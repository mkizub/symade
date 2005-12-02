/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;
import kiev.backend.java15.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public final class ImportKievSrc extends TransfProcessor implements Constants {

	private JavaBackend javaBackend = new JavaBackend();
	
	public ImportKievSrc(Kiev.Ext ext) {
		super(ext);
	}
	/////////////////////////////////////////////////////
	//												   //
	//		   PASS 1 - create top structures		   //
	//												   //
	/////////////////////////////////////////////////////
	
	public void pass1(ASTNode:ASTNode node) {
		return;
	}
	
	public void pass1(FileUnit:ASTNode astn) {
		FileUnit fu = astn;
		foreach (ASTNode n; astn.members) {
			try {
				pass1(n);
			} catch(Exception e ) { Kiev.reportError(n,e); }
		}
	}

	private void setSourceFile(Struct me) {
		if( !me.isLocal() )
			Env.setProjectInfo(me.name, Kiev.curFile);
		SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
		me.addAttr(sfa);
	}
	
	private void setupStructType(Struct me, boolean canHaveArgs) {
		/* Then may be class arguments - they are proceed here, but their
		   inheritance - at pass2()
		*/
		Type[] targs = Type.emptyArray;
		if (!canHaveArgs) {
			if( me.args.length > 0 ) {
				Kiev.reportError(me,"Type parameters are not allowed for "+me);
				me.args.delAll();
			}
		}
		else if( me.parent instanceof Struct && ((Struct)me.parent).args.length > 0 ) {
			Struct astnp = (Struct)me.parent;
			// Inner classes's arguments have to be arguments of outer classes
			// BUG BUG BUG - need to follow java scheme
			for(int i=0; i < me.args.length; i++) {
				TypeArgRef arg = me.args[i];
				Type[] outer_args = astnp.type.args;
				if( outer_args == null || outer_args.length <= i)
					throw new CompilerException(arg,"Inner class arguments must match outer class arguments");
				if !(outer_args[i].getClazzName().short_name.equals(arg.name.name))
					throw new CompilerException(arg,"Inner class arguments must match outer class argument,"
						+" but arg["+i+"] is "+arg
						+" and have to be "+outer_args[i].getClazzName().short_name);
			}
			/* Create type for class's arguments, if any */
			if( me.args.length > 0 ) {
				targs = astnp.type.args;
			}
		} else {
			for(int i=0; i < me.args.length; i++)
				targs = (Type[])Arrays.append(targs,me.args[i].getType());
		}

		/* Generate type for this structure */
		me.type = Type.newRefType(me,targs);
	}

	public void pass1(Struct:ASTNode astn) {
		trace(Kiev.debugResolve,"Pass 1 for struct "+astn);

		Struct me = astn;
		me.setResolved(true);
		setSourceFile(me);
		if (me.isEnum()) {
			if !(astn.parent instanceof FileUnit)
				me.setStatic(true);
			setupStructType(me, false);
		}
		else if (me.isPizzaCase()) {
			me.setStatic(true);
			Struct p = (Struct)me.parent;
			p.addCase(me);
			me.super_type = p.type;
			setupStructType(me, true);
		}
		else if (me.isSyntax()) {
			me.setPrivate(true);
			me.setAbstract(true);
			me.setMembersGenerated(true);
			me.setStatementsGenerated(true);
			setupStructType(me, false);
		}
		else if (me.type != null)
			;
		else
			setupStructType(me, true);

		// assign type of enum fields
		if (me.isEnum()) {
			foreach (ASTNode n; me.members; n instanceof Field && n.isEnumField()) {
				Field f = (Field)n;
				f.ftype = new TypeRef(me.type);
			}
		}
		
		if( !me.isPackage() ) {
			// Process inner classes and cases
			foreach (ASTNode n; me.members) {
				pass1(n);
			}
		}
	}





	/////////////////////////////////////////////////////
	//												   //
	//	   PASS 1_1 - process syntax declarations	   //
	//												   //
	/////////////////////////////////////////////////////


	public void pass1_1(ASTNode:ASTNode node) {
		return;
	}

	public void pass1_1(FileUnit:ASTNode astn) {
		// Process file imports...
		boolean java_lang_found = false;
		KString java_lang_name = KString.from("java.lang");
		boolean kiev_stdlib_found = false;
		KString kiev_stdlib_name = KString.from("kiev.stdlib");
		boolean kiev_stdlib_meta_found = false;
		KString kiev_stdlib_meta_name = KString.from("kiev.stdlib.meta");

		foreach (ASTNode n; astn.syntax) {
			try {
				if (n instanceof Import && ((Import)n).mode == Import.ImportMode.IMPORT_STATIC && !((Import)n).star)
					continue; // process later
				pass1_1(n);
				if (n instanceof Import) {
					if( n.mode == Import.ImportMode.IMPORT_CLASS && ((Struct)n.resolved).name.name.equals(java_lang_name))
						java_lang_found = true;
					else if( n.mode == Import.ImportMode.IMPORT_CLASS && ((Struct)n.resolved).name.name.equals(kiev_stdlib_name))
						kiev_stdlib_found = true;
					else if( n.mode == Import.ImportMode.IMPORT_CLASS && ((Struct)n.resolved).name.name.equals(kiev_stdlib_meta_name))
						kiev_stdlib_meta_found = true;
				}
				trace(Kiev.debugResolve,"Add "+n);
			} catch(Exception e ) {
				Kiev.reportError(n,e);
			}
		}
		// Add standard imports, if they were not defined
		if( !Kiev.javaMode && !kiev_stdlib_found )
			astn.syntax.add(new Import(Env.newPackage(kiev_stdlib_name),Import.ImportMode.IMPORT_CLASS,true));
		if( !Kiev.javaMode && !kiev_stdlib_meta_found )
			astn.syntax.add(new Import(Env.newPackage(kiev_stdlib_meta_name),Import.ImportMode.IMPORT_CLASS,true));
		if( !java_lang_found )
			astn.syntax.add(new Import(Env.newPackage(java_lang_name),Import.ImportMode.IMPORT_CLASS,true));

		// Process members - pass1_1()
		foreach (ASTNode n; astn.members) {
			pass1_1(n);
		}
	}

	public void pass1_1(Import:ASTNode astn) {
		if (astn.of_method || (astn.mode==Import.ImportMode.IMPORT_STATIC && !astn.star)) return;
		KString name = astn.name.name;
		DNode@ v;
		if( !PassInfo.resolveQualifiedNameR(astn,v,new ResInfo(astn,ResInfo.noForwards),name) ) {
			Kiev.reportError(astn,"Unresolved identifier "+name);
		}
		DNode n = v;
		if		(astn.mode == Import.ImportMode.IMPORT_CLASS && !(n instanceof Struct))
			Kiev.reportError(astn,"Identifier "+name+" is not a class or package");
		else if (astn.mode == Import.ImportMode.IMPORT_PACKAGE && !(n instanceof Struct && ((Struct)n).isPackage()))
			Kiev.reportError(astn,"Identifier "+name+" is not a package");
		else if (astn.mode == Import.ImportMode.IMPORT_STATIC && !(astn.star || (n instanceof Field)))
			Kiev.reportError(astn,"Identifier "+name+" is not a field");
		else if (astn.mode == Import.ImportMode.IMPORT_SYNTAX && !(n instanceof Struct && n.isSyntax()))
			Kiev.reportError(astn,"Identifier "+name+" is not a syntax");
		else
			astn.resolved = n;
	}

	public void pass1_1(Typedef:ASTNode astn) {
		try {
			if (astn.typearg != null) {
				astn.type = new TypeRef(astn.type.getType().getInitialType());
			} else {
				astn.type = new TypeRef(astn.type.getType());
			}
		} catch (RuntimeException e) { /* ignore */ }
	}

	public void pass1_1(Struct:ASTNode astn) {
		// Verify meta-data to the new structure
		Struct me = astn;
		me.meta.verify();
		
		if (me.isSyntax()) {
			trace(Kiev.debugResolve,"Pass 1_1 for syntax "+me);
			for (int i=0; i < me.members.length; i++) {
				ASTNode n = me.members[i];
				try {
					if (n instanceof Typedef) {
						pass1_1(n);
						me.imported.add(me.members[i]);
						trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
					}
					else if (n instanceof Opdef) {
						pass1_1(n);
						me.imported.add(me.members[i]);
						trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
					}
				} catch(Exception e ) {
					Kiev.reportError(n,e);
				}
			}
		}
	}
	
	public void pass1_1(Opdef:ASTNode astn) {
		int prior = astn.prior;
		int opmode = astn.opmode;
		KString image = astn.image;
		switch(opmode) {
		case Operator.LFY:
			{
				AssignOperator op = AssignOperator.getOperator(image);
				if (op != null) {
					if (prior != op.priority)
						throw new CompilerException(astn,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
					if (opmode != op.mode)
						throw new CompilerException(astn,"Operator declaration conflict: "+Operator.orderAndArityNames[opmode]+" and "+Operator.orderAndArityNames[op.mode]+" are different");
					astn.resolved = op;
					return;
				}
				op = AssignOperator.newAssignOperator(image,null,null,false);
				if( Kiev.verbose ) System.out.println("Declared assign operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return;
			}
		case Operator.XFX:
		case Operator.YFX:
		case Operator.XFY:
		case Operator.YFY:
			{
				BinaryOperator op = BinaryOperator.getOperator(image);
				if (op != null) {
					if (prior != op.priority)
						throw new CompilerException(astn,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
					if (opmode != op.mode)
						throw new CompilerException(astn,"Operator declaration conflict: "+Operator.orderAndArityNames[opmode]+" and "+Operator.orderAndArityNames[op.mode]+" are different");
					astn.resolved = op;
					return;
				}
				op = BinaryOperator.newBinaryOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared infix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return;
			}
		case Operator.FX:
		case Operator.FY:
			{
				PrefixOperator op = PrefixOperator.getOperator(image);
				if (op != null) {
					if (prior != op.priority)
						throw new CompilerException(astn,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
					if (opmode != op.mode)
						throw new CompilerException(astn,"Operator declaration conflict: "+Operator.orderAndArityNames[opmode]+" and "+Operator.orderAndArityNames[op.mode]+" are different");
					astn.resolved = op;
					return;
				}
				op = PrefixOperator.newPrefixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared prefix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return;
			}
		case Operator.XF:
		case Operator.YF:
			{
				PostfixOperator op = PostfixOperator.getOperator(image);
				if (op != null) {
					if (prior != op.priority)
						throw new CompilerException(astn,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
					if (opmode != op.mode)
						throw new CompilerException(astn,"Operator declaration conflict: "+Operator.orderAndArityNames[opmode]+" and "+Operator.orderAndArityNames[op.mode]+" are different");
					astn.resolved = op;
					return;
				}
				op = PostfixOperator.newPostfixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared postfix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return;
			}
		case Operator.XFXFY:
			throw new CompilerException(astn,"Multioperators are not supported yet");
		default:
			throw new CompilerException(astn,"Unknown operator mode "+opmode);
		}
	}






	/////////////////////////////////////////////////////
	//													//
	//	   PASS 2 - struct args inheritance			//
	//													//
	/////////////////////////////////////////////////////


	public void pass2(ASTNode:ASTNode astn) {
		return;
	}

	public void pass2(FileUnit:ASTNode astn) {
		foreach (ASTNode n; astn.members)
			pass2(n);
	}

	public void pass2(Struct:ASTNode astn) {
		Struct me = astn;
		int pos = astn.pos;
		trace(Kiev.debugResolve,"Pass 2 for class "+me);
		/* Process inheritance of class's arguments, if any */
		Type[] targs = me.type.args;
		for(int i=0; i < astn.args.length; i++) {
			TypeArgRef arg = astn.args[i];
			if( arg.super_bound != null ) {
				Type sup = arg.super_bound.getType();
				if( !sup.isReference() ) {
					Kiev.reportError(astn,"Argument extends bad type "+sup);
				} else {
					((ArgumentType)arg.getType()).super_type = sup;
				}
				targs[i].checkJavaSignature();
			}
		}

		// Process inner classes and cases
		if( !me.isPackage() ) {
			foreach (ASTNode m; astn.members)
				pass2(m);
		}
	}







	/////////////////////////////////////////////////////
	//												   //
	//	   PASS 2_2 - struct inheritance			   //
	//												   //
	/////////////////////////////////////////////////////


	public void pass2_2(ASTNode:ASTNode astn) {
	}

	public void pass2_2(FileUnit:ASTNode astn) {
		foreach (ASTNode n; astn.syntax) {
			try {
				if (n instanceof Import && ((Import)n).mode == Import.ImportMode.IMPORT_STATIC && !((Import)n).star)
					continue; // process later
				pass2_2(n);
			} catch(Exception e ) {
				Kiev.reportError(n,e);
			}
		}
		foreach (ASTNode n; astn.members)
			pass2_2(n);
	}

	public void pass2_2(Typedef:ASTNode astn) {
		if (astn.type == null) {
			if (astn.typearg != null) {
				astn.type = new TypeRef(((BaseType)astn.type.getType()).clazz.type);
			} else {
				astn.type = new TypeRef(astn.type.getType());
			}
		}
	}

	public void pass2_2(Struct:ASTNode astn) {
		int pos = astn.pos;
		Struct me = astn;
		trace(Kiev.debugResolve,"Pass 2_2 for class "+me);
		/* Now, process 'extends' and 'implements' clauses */
		if( me.isAnnotation() ) {
			me.super_type = Type.tpObject;
			me.interfaces.add(new TypeRef(Type.tpAnnotation));
		}
		else if( me.isInterface() ) {
			me.super_type = Type.tpObject;
			foreach(TypeRef tr; me.interfaces)
				tr.getType();
		}
		else if( me.isEnum() ) {
			me.super_type = Type.tpEnum;
		}
		else if( me.isSyntax() ) {
			me.super_type = null;
		}
		else if( me.isPizzaCase() ) {
			// already set
			//me.super_clazz = ((Struct)me.parent).type;
			assert (me.super_type == ((Struct)me.parent).type);
		}
		else {
			Type sup = me.super_bound.getType();
			if (sup == null && !me.name.name.equals(Type.tpObject.clazz.name.name))
				me.super_type = Type.tpObject;
			foreach(TypeRef tr; me.interfaces)
				tr.getType();
			if( me.type.args.length > 0 && !(me.type instanceof ClosureType) ) {
				me.interfaces.append(new TypeRef(Type.tpTypeInfoInterface));
			}
		}

		// Process inner classes and cases
		if( !me.isPackage() ) {
			foreach (ASTNode m; astn.members)
				pass2_2(m);
		}

	}






	////////////////////////////////////////////////////
	//												   //
	//	   PASS 3- struct members					   //
	//												   //
	////////////////////////////////////////////////////


	public void pass3(ASTNode:ASTNode astn) {
	}

	public void pass3(FileUnit:ASTNode astn) {
		foreach (ASTNode n; astn.members)
			pass3(n);
	}


	public void pass3(Struct:ASTNode astn) {
		int pos = astn.pos;
		Struct me = astn;
		int next_enum_val = 0;
		trace(Kiev.debugResolve,"Pass 3 for class "+me);
		if (me.isSyntax()) {
			return;
		}
		// Process members
		ASTNode[] members = astn.members.toArray();
		for(int i=0; i < members.length; i++) {
			if( members[i] instanceof Initializer ) {
				Initializer init = (Initializer)members[i];
				// TODO: check flags for initialzer
				if( me.isPackage() ) init.setStatic(true);
			}
			else if( members[i] instanceof RuleMethod ) {
				RuleMethod m = (RuleMethod)members[i];
				m.pass3();
				if( me.isPackage() ) m.setStatic(true);
				if( m.isPrivate() ) m.setFinal(true);
				if( me.isClazz() && me.isFinal() ) m.setFinal(true);
				else if( me.isInterface() ) 	m.setPublic(true);
				m.acc.verifyAccessDecl(m);
			}
			else if( members[i] instanceof Method ) {
				Method m = (Method)members[i];
				m.pass3();
				if( me.isPackage() ) m.setStatic(true);
				if( m.isPrivate() ) m.setFinal(false);
				else if( me.isClazz() && me.isFinal() ) m.setFinal(true);
				else if( me.isInterface() ) {
					m.setPublic(true);
				}
				if( m.name.equals(nameInit) ) {
					m.setNative(false);
					m.setAbstract(false);
					m.setSynchronized(false);
					m.setFinal(false);
				}
				m.acc.verifyAccessDecl(m);
			}
			else if (members[i] instanceof Field && members[i].isEnumField()) {
				Field f = (Field)members[i];
				KString text = f.name.name;
				MetaAlias al = f.getMetaAlias();
				if (al != null) {
					foreach (ASTNode n; al.getAliases(); n instanceof ConstStringExpr) {
						KString nm = ((ConstStringExpr)n).value;
						if (nm.len > 2 && nm.byteAt(0) == '\"') {
							f.name.addAlias(nm);
							text = nm.substr(1,nm.len-2);
							break;
						}
					}
				}
				f.init = new NewExpr(f.pos,me.type,new Expr[]{
							new ConstStringExpr(f.name.name),
							new ConstIntExpr(next_enum_val)
							//new ConstStringExpr(text)
				});
				next_enum_val++;
			}
			else if( members[i] instanceof Field ) {
				Field fdecl = (Field)members[i];
				Field f = fdecl;
				f.meta.verify();
				// TODO: check flags for fields
				if( me.isPackage() )
					f.setStatic(true);
				if( me.isInterface() ) {
					if (f.isVirtual()) {
						f.setAbstract(true);
					} else {
						f.setStatic(true);
						f.setFinal(true);
					}
					f.setPublic(true);
				}
				f.acc.verifyAccessDecl(f); // recheck access
				Type ftype = fdecl.type;
				MetaPacked pack = f.getMetaPacked();
				if( pack != null ) {
					if( f.isStatic() ) {
						Kiev.reportWarning(fdecl,"Packing of static field(s) ignored");
						f.meta.unset(pack);
					}
					else if( !ftype.isIntegerInCode() ) {
						if( ftype.isEnum() ) {
							Kiev.reportError(fdecl,"Packing of enum is not implemented yet");
						} else {
							Kiev.reportError(fdecl,"Packing of reference type is not allowed");
						}
						f.meta.unset(pack);
					} else {
						int max_pack_size = 32;
						if( ftype == Type.tpShort || ftype == Type.tpChar ) {
							max_pack_size = 16;
							if( pack.size <= 0 ) pack.size = 16;
						}
						else if( ftype == Type.tpByte ) {
							max_pack_size = 8;
							if( pack.size <= 0 ) pack.size = 8;
						}
						else if( ftype == Type.tpBoolean) {
							max_pack_size = 1;
							if( pack.size <= 0 ) pack.size = 1;
						}
						if( pack.size < 0 || pack.size > max_pack_size ) {
							Kiev.reportError(fdecl,"Bad size "+pack.size+" of packed field");
							f.meta.unset(pack);
						}
						else if( pack.offset >= 0 && pack.size+pack.offset > 32) {
							Kiev.reportError(fdecl,"Size+offset "+(pack.size+pack.offset)+" do not fit in 32 bit boundary");
							f.meta.unset(pack);
						}
					}
				}
				if( f.getMetaPacked() != null )
					f.setPackedField(true);
				if (fdecl.init == null && !ftype.isArray()) {
					if(ftype.isWrapper()) {
						f.init = new NewExpr(fdecl.pos,ftype,Expr.emptyArray);
						f.setInitWrapper(true);
					}
				} else {
					if( ftype.isWrapper()) {
						if (fdecl.isInitWrapper())
							f.init = fdecl.init;
						else
							f.init = new NewExpr(fdecl.pos,ftype, (fdecl.init==null)? ENode.emptyArray : new ENode[]{fdecl.init});
						f.setInitWrapper(true);
					} else {
						f.init = fdecl.init;
						f.setInitWrapper(false);
					}
				}
			}
			else if( members[i] instanceof WBCCondition ) {
				WBCCondition inv = (WBCCondition)members[i];
				assert(inv.cond == WBCType.CondInvariant);
				// TODO: check flags for fields
				MethodType mt = MethodType.newMethodType(null,Type.emptyArray,Type.tpVoid);
				Method m = new Method(inv.name.name,mt,inv.flags);
				m.setInvariantMethod(true);
				m.body = new BlockStat();
				inv.replaceWithNode(m);
				m.conditions += inv;
			}
			// Inner classes and cases after all methods and fields, skip now
			else if( members[i] instanceof Struct );
			else if( members[i] instanceof Import ) {
				me.imported.add(members[i]);
			}
			else {
				throw new CompilerException(members[i],"Unknown type if structure member: "+members[i]);
			}
		}

		// Process inner classes and cases
		if( !me.isPackage() ) {
			foreach (ASTNode n; me.members; n instanceof Struct)
				pass3(n);
		}
	}




	////////////////////////////////////////////////////
	//	   PASS Meta 1 - resolve meta decls           //
	////////////////////////////////////////////////////

	public void resolveMetaDecl(ASTNode:ASTNode node) {
	}
	public void resolveMetaDecl(FileUnit:ASTNode fu) {
		foreach(ASTNode n; fu.members; n instanceof Struct)
			resolveMetaDecl(n);
	}
	public void resolveMetaDecl(Struct:ASTNode clazz) {
		if (clazz.isAnnotation()) {
			foreach(ASTNode n; clazz.members) {
				if( n instanceof Method ) {
					Method m = (Method)n;
					if (m.params.length != 0)
						Kiev.reportError(m, "Annotation methods may not have arguments");
					if (m.body != null || m.pbody != null)
						Kiev.reportError(m, "Annotation methods may not have bodies");
					if (m.conditions.length > 0)
						Kiev.reportError(m, "Annotation methods may not have work-by-contruct conditions");
					m.setPublic(true);
					m.setAbstract(true);
					m.pass3();
					if (m.type.ret == Type.tpVoid || m.type.ret == Type.tpRule)
						Kiev.reportError(m, "Annotation methods must return a value");
				}
				else if( n instanceof Field )
					;
				else if( n instanceof Struct )
					;
				else
					Kiev.reportError(n, "Annotations may only have methods and final fields");
			}
		}
		foreach (Struct sub; clazz.sub_clazz)
			resolveMetaDecl(sub);
	}

	////////////////////////////////////////////////////
	//	   PASS Meta 2 - resolve meta defaults        //
	////////////////////////////////////////////////////

	public void resolveMetaDefaults(ASTNode:ASTNode node) {
	}
	public void resolveMetaDefaults(FileUnit:ASTNode fu) {
		foreach(ASTNode n; fu.members; n instanceof Struct)
			resolveMetaDefaults(n);
	}
	public void resolveMetaDefaults(Struct:ASTNode clazz) {
		clazz.resolveMetaDefaults();
	}

	////////////////////////////////////////////////////
	//	   PASS Meta 3 - resolve meta annotations     //
	////////////////////////////////////////////////////

	public void resolveMetaValues(ASTNode:ASTNode node) {
	}
	public void resolveMetaValues(FileUnit:ASTNode fu) {
		foreach(ASTNode n; fu.members; n instanceof Struct)
			resolveMetaValues(n);
	}
	public void resolveMetaValues(Struct:ASTNode clazz) {
		clazz.resolveMetaValues();
	}

	////////////////////////////////////////////////////
	//												   //
	//	   PASS 4 - resolve meta and generate members //
	//												   //
	////////////////////////////////////////////////////

	public void autoGenerateMembers(ASTNode:ASTNode node) {
	}
	public void autoGenerateMembers(FileUnit:ASTNode fu) {
		foreach(DNode n; fu.members; n instanceof Struct)
			autoGenerateMembers(n);
	}
	public void autoGenerateMembers(Struct:ASTNode clazz) {
		clazz.autoGenerateMembers();
	}



	////////////////////////////////////////////////////
	//												   //
	//	   PASS 5 - pre-resolve                       //
	//												   //
	////////////////////////////////////////////////////

	public void preResolve(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { return n.preResolveIn(TransfProcessor.this); }
			public void post_exec(ASTNode n) { n.preResolveOut(); }
		});
		return;
	}

	public void mainResolve(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { return n.mainResolveIn(TransfProcessor.this); }
			public void post_exec(ASTNode n) { n.mainResolveOut(); }
		});
		return;
	}

	////////////////////////////////////////////////////
	//												   //
	//	   PASS - pre-generation, auto-proxy methods  //
	//												   //
	////////////////////////////////////////////////////

	public BackendProcessor getBackend(Kiev.Backend backend) {
		if (backend == Kiev.Backend.Java15)
			return javaBackend;
		return null;
	}
	
}

final class JavaBackend extends BackendProcessor {
	
	public JavaBackend() {
		super(Kiev.Backend.Java15);
	}
	
	public void preGenerate() {
		JPackage jroot = (JPackage)new TreeMapper().mapStruct(Env.root);
		foreach (FileUnit fu; Kiev.files) {
//			foreach (DNode d; fu.members; d instanceof Struct) {
//				new JStruct((Struct)d).preGenerate();
//			}
			fu.walkTree(new TreeWalker() {
				public boolean pre_exec(ASTNode n) { return n.preGenerate(); }
			});
		}
//		jroot.toJavaDecl("jsrc");
	}

	public void preGenerate(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { return n.preGenerate(); }
		});
	}

	// resolve back-end
	public void resolve(ASTNode node) {
		if (node instanceof ENode)
			node.resolve(null);
		else if (node instanceof DNode)
			node.resolveDecl();
	}

	// generate back-end
	public void generate(ASTNode node) {
		if (node instanceof FileUnit) {
			if( Kiev.source_only ) {
				if( Kiev.output_dir == null )
					if( Kiev.verbose ) System.out.println("Dumping to Java source file "+node);
				else
					if( Kiev.verbose ) System.out.println("Dumping to Java source file "+node+" into "+Kiev.output_dir+" dir");
				try {
					node.toJava(Kiev.output_dir);
				} catch (Exception rte) { Kiev.reportError(rte); }
			} else {
				try {
					node.generate();
				} catch (Exception rte) { Kiev.reportError(rte); }
			}
		}
	}
}


