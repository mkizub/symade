/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.transf;

import kiev.ir.java15.RNode;

import java.io.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

/////////////////////////////////////////////////////
//													//
//		   PASS 1 - process file syntax				//
//													//
/////////////////////////////////////////////////////
	
@singleton
public final class KievFE_Pass1 extends TransfProcessor {
	private KievFE_Pass1() { super(KievExt.JavaOnly); }
	public String getDescr() { "Syntax" }
	
	public void process(ASTNode node, Transaction tr) {
		processSyntax(node);
	}
	
	public void processSyntax(ASTNode:ASTNode node) {
		return;
	}
	
	public void processSyntax(FileUnit:ASTNode node) {
		FileUnit fu = node;
		fu.getPackage();
		boolean any_syntax = false;
		foreach (Import n; fu.members; n.mode == Import.ImportMode.IMPORT_SYNTAX) {
			any_syntax = true;
			break;
		}
		if (!any_syntax) {
			Import imp = new Import();
			imp.name.qualified = true;
			imp.name.name = "kiev\u001fstdlib\u001fSyntax";
			imp.mode = Import.ImportMode.IMPORT_SYNTAX;
			imp.setAutoGenerated(true);
			fu.members.insert(0,imp);
		}
		foreach (ASTNode n; node.members) {
			try {
				processSyntax(n);
			} catch(Exception e ) { Kiev.reportError(n,e); }
		}
	}

	public void processSyntax(NameSpace:ASTNode node) {
		node.getPackage();
		foreach (ASTNode n; node.members) {
			try {
				processSyntax(n);
			} catch(Exception e ) { Kiev.reportError(n,e); }
		}
	}
	
	public void processSyntax(ComplexTypeDecl:ASTNode node) {
		foreach (ASTNode n; node.members) {
			try {
				processSyntax(n);
			} catch(Exception e ) { Kiev.reportError(n,e); }
		}
	}

	public void processSyntax(Import:ASTNode astn) {
		if (astn.of_method)
			return;
		String name = astn.name.name.replace('.','\u001f');
		ScopeOfNames scope = (ScopeOfNames)Env.getRoot();
		DNode n;
		int dot;
		do {
			dot = name.indexOf('\u001f');
			String head;
			if (dot > 0) {
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1).intern();
			} else {
				head = name;
			}
			DNode@ node;
			if!(scope.resolveNameR(node,new ResInfo(astn,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext))) {
				Kiev.reportError(astn,"Unresolved identifier "+head+" in "+scope);
				return;
			}
			n = node;
			if (n instanceof ScopeOfNames)
				scope = (ScopeOfNames)n;
		} while (dot > 0);
		if		(astn.mode == Import.ImportMode.IMPORT_CLASS && !(n instanceof TypeDecl || n instanceof KievPackage))
			Kiev.reportError(astn,"Identifier "+name+" is not a type or package");
		else if (astn.mode == Import.ImportMode.IMPORT_STATIC && !(astn.star || (n instanceof Field)))
			Kiev.reportError(astn,"Identifier "+name+" is not a field");
		else if (astn.mode == Import.ImportMode.IMPORT_SYNTAX && !(n instanceof KievSyntax))
			Kiev.reportError(astn,"Identifier "+name+" is not a syntax");
		else {
			assert (n != null);
			astn.name.symbol = n;
		}
	}

	
	public void processSyntax(Opdef:ASTNode astn) {
		int prior = astn.prior;
		int opmode = astn.opmode;
		String image = astn.image;
		String decl = astn.decl;
		if (decl != null) {
			Operator op = Operator.getOperatorByDecl(decl);
			if (op != null) {
				if (prior != op.priority)
					throw new CompilerException(astn,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
				astn.resolved = op;
				return;
			}
			op = Operator.newOperator(prior, decl);
			if( Kiev.verbose ) System.out.println("Declared operator "+op+" with priority "+op.priority);
			astn.resolved = op;
			return;
		}
		switch(opmode) {
		case Opdef.LFY:	decl = "X "+image+" Y";	break;
		case Opdef.XFX:
			if (image == "instanceof")
				decl = "X "+image+" T";
			else
				decl = "X "+image+" X";
			break;
		case Opdef.YFX:	decl = "Y "+image+" X";	break;
		case Opdef.XFY:	decl = "X "+image+" Y";	break;
		case Opdef.YFY: decl = "Y "+image+" Y"; break;
		case Opdef.FX:	decl = image+" X";	break;
		case Opdef.FY:	decl = image+" Y";	break;
		case Opdef.XF:	decl = "X "+image;	break;
		case Opdef.YF:	decl = "Y "+image;	break;
		case Opdef.XFXFY:
			throw new CompilerException(astn,"Multioperators are not supported yet");
		default:
			throw new CompilerException(astn,"Unknown operator mode "+opmode);
		}
		String name = OpArg.toOpName(OpArg.fromOpString(prior,decl));
		{
			Operator op = Operator.getOperatorByName(name);
			if (op != null) {
				if (prior != op.priority)
					throw new CompilerException(astn,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
			} else {
				op = Operator.newOperator(prior, decl);
				if( Kiev.verbose ) System.out.println("Declared operator "+op+" with priority "+op.priority);
			}
			astn.resolved = op;
			astn.decl = op.decl;
		}
		return;
	}

	public void processSyntax(KievSyntax:ASTNode astn) {
	next_super_syntax:
		foreach(SymbolRef sr; astn.super_syntax) {
			String name = sr.name;
			KievPackage scope = null;
			int dot = name.indexOf('\u001f');
			while (dot > 0) {
				String head;
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1).intern();
				if (scope == null)
					scope = Env.getRoot();
				KievPackage@ pkg;
				if!(scope.resolveNameR(pkg,new ResInfo(astn,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext))) {
					Kiev.reportError(sr,"Unresolved package "+head+" in "+scope);
					continue next_super_syntax;
				}
				scope = (KievPackage)pkg;
				dot = name.indexOf('\u001f');
			}
			KievSyntax@ stx;
			if!(scope.resolveNameR(stx,new ResInfo(astn,name,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext))) {
				Kiev.reportError(sr,"Unresolved syntax "+name+" in "+scope);
				continue next_super_syntax;
			}
			sr.symbol = (KievSyntax)stx;
		}
		foreach (ASTNode n; astn.members) {
			try {
				processSyntax(n);
			} catch(Exception e ) { Kiev.reportError(n,e); }
		}
	}

	public void processSyntax(Struct:ASTNode astn) {
		Struct me = astn;
		if (me.isAnnotation() || me.isEnum()) {
			if( me.args.length > 0 ) {
				Kiev.reportError(me,"Type parameters are not allowed for "+me);
				me.args.delAll();
			}
			me.setTypeUnerasable(false);
		}
		else if !(me.parent() instanceof KievPackage) {
			if (me.isStructInner() && !me.isStatic() && me.isClazz()) {
				ComplexTypeDecl pkg = me.ctx_tdecl;
				if (pkg.sname == nameIFaceImpl)
					pkg = pkg.ctx_tdecl;
				int n = 0;
				for(ComplexTypeDecl p=pkg; p.isStructInner() && !p.isStatic(); p=p.ctx_tdecl) n++;
				String fldName = (nameThisDollar+n).intern();
				boolean found = false;
				foreach (Field f; me.members; f.sname == fldName)
					found = true;
				if (!found) {
					TypeAssign td = new TypeAssign("outer$"+n+"$type", new TypeRef(pkg.xtype));
					td.setSynthetic(true);
					me.members.append(td);
					me.ometa_tdef = td;
					Field f = new Field(fldName,td.getAType(),ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC);
					f.pos = me.pos;
					me.members.append(f);
				}
			}
		}
		if (me.isTypeUnerasable()) {
			foreach (TypeDef a; me.args)
				a.setTypeUnerasable(true);
		}
		
		foreach (ASTNode n; me.members) {
			try {
				processSyntax(n);
			} catch(Exception e ) { Kiev.reportError(n,e); }
		}
	}
}

/////////////////////////////////////////////////////
//													//
//		   PASS 2 - create types for structures	//
//													//
/////////////////////////////////////////////////////
@singleton
public final class KievFE_Pass2 extends TransfProcessor {
	private KievFE_Pass2() { super(KievExt.JavaOnly); }
	public String getDescr() { "Class types" }

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode astn) {
		FileUnit fu = astn;

		foreach (ASTNode n; astn.members) {
			try {
				if (n instanceof TypeDef)
					n.getType();
				else if (n instanceof TypeOpDef)
					n.getType();
			} catch(Exception e ) {
				Kiev.reportError(n,e);
			}
		}

		foreach (ASTNode n; astn.members)
			doProcess(n);
	}

	public void doProcess(NameSpace:ASTNode astn) {
		foreach (ASTNode n; astn.members)
			doProcess(n);
	}

	public void doProcess(TypeDecl:ASTNode astn) {
		try {
			TypeDecl tdecl = astn;
			tdecl.verifyMetas();
			if (tdecl.isTypeResolved())
				return;
			tdecl.setTypeResolved(true);
			foreach(TypeRef tr; tdecl.super_types) {
				TypeDecl td = tr.getTypeDecl();
				if (td instanceof ComplexTypeDecl)
					getStructType((ComplexTypeDecl)td, new Stack<TypeDecl>());
			}
			tdecl.setArgsResolved(true);
		} catch(Exception e ) { Kiev.reportError(astn,e); }
	}
	
	public void doProcess(ComplexTypeDecl:ASTNode astn) {
		try {
			ComplexTypeDecl td = astn;
			// Verify meta-data to the new structure
			td.verifyMetas();
			foreach (DNode dn; td.members)
				dn.verifyMetas();
			getStructType(td, new Stack<TypeDecl>());
			foreach (TypeDecl s; td.members)
				doProcess(s);
		} catch(Exception e ) { Kiev.reportError(astn,e); }
	}
	
	private Type getStructType(ComplexTypeDecl tdecl, Stack<TypeDecl> path) {
		tdecl.checkResolved();
		if (tdecl.isTypeResolved())
			return tdecl.xtype;
		path.push(tdecl);
		
		tdecl.setTypeResolved(true);
		
		for (ComplexTypeDecl p = tdecl.ctx_tdecl; p != null; p = p.ctx_tdecl)
			getStructType(p, path);

		if (tdecl instanceof Struct) {
			Struct clazz = (Struct)tdecl;
			if (clazz.isAnnotation()) {
				if (clazz.super_types.length == 0) {
					clazz.super_types.insert(0, new TypeRef(Type.tpObject));
					clazz.super_types.insert(1, new TypeRef(Type.tpAnnotation));
				}
			}
			else if (tdecl.isEnum()) {
				if (tdecl.isStructInner())
					clazz.setStatic(true);
				if (clazz.super_types.length == 0) {
					TypeRef tr = new TypeRef(Type.tpEnum);
					tr.setAutoGenerated(true);
					clazz.super_types.insert(0, tr);
				}
			}
			else if (clazz instanceof PizzaCase) {
				clazz.setStatic(true);
				Struct p = (Struct)clazz.ctx_tdecl;
				p.addCase((PizzaCase)clazz);
				getStructType(p, path);
				if (clazz.super_types.length == 0) {
					TypeNameArgsRef sup_ref = new TypeNameArgsRef(p.qname());
					sup_ref.symbol = p;
				next_case_arg:
					for(int i=0; i < p.args.length; i++) {
						for(int j=0; j < clazz.args.length; j++) {
							if (p.args[i].sname == clazz.args[j].sname) {
								sup_ref.args.add(new TypeRef(clazz.args[j].getAType()));
								continue next_case_arg;
							}
						}
						sup_ref.args.add(new TypeRef(StdTypes.tpNull));
					}
					sup_ref.setAutoGenerated(true);
					clazz.super_types.insert(0, sup_ref);
				}
			}
			else if (clazz.isInterface()) {
				if (clazz.super_types.length == 0) {
					TypeRef tr = new TypeRef(Type.tpObject);
					tr.setAutoGenerated(true);
					clazz.super_types.insert(0, tr);
				}
				foreach(TypeRef tr; clazz.super_types) {
					Struct s = tr.getType().getStruct();
					if (s != null)
						getStructType(s, path);
				}
			}
			else if (clazz.isStructView() || clazz.isClazz() || clazz instanceof MetaTypeDecl) {
				if (clazz.isStructView()) {
					KievView kview = (KievView)clazz;
					if (kview.view_of != null)
						kview.view_of.getType();
				}
				foreach(TypeRef tr; clazz.super_types) {
					Struct s = tr.getType().getStruct();
					if (s != null)
						getStructType(s, path);
				}
				if (clazz.super_types.length == 0) {
					if (clazz != Type.tpObject.tdecl) {
						TypeRef tr = new TypeRef(Type.tpObject);
						tr.setAutoGenerated(true);
						clazz.super_types.insert(0, tr);
					}
				}
				//else if (clazz.super_types[0].getStruct().isInterface()) {
				//	TypeRef tr = new TypeRef(Type.tpObject);
				//	tr.setAutoGenerated(true);
				//	clazz.super_types.insert(0, tr);
				//}
			}
		}
		else {
			foreach(TypeRef tr; tdecl.super_types) {
				Struct s = tr.getType().getStruct();
				if (s != null)
					getStructType(s, path);
			}
		}
		
		tdecl.callbackTypeVersionChanged();
		if (tdecl instanceof Struct) {
			Struct clazz = (Struct)tdecl;
			clazz.xtype.bindings(); // update the type
			if (clazz.xtype.isUnerasable()) {
				Struct clazz = (Struct)tdecl;
				if (!clazz.isTypeUnerasable()) {
					Kiev.reportWarning(clazz,"Type "+clazz+" must be annotated as @unerasable");
					clazz.setTypeUnerasable(true);
					foreach (TypeDef a; clazz.args)
						a.setTypeUnerasable(true);
				}
				if (!clazz.instanceOf(Type.tpTypeInfoInterface.tdecl))
					clazz.super_types.append(new TypeRef(Type.tpTypeInfoInterface));
			}
		}

		tdecl.setArgsResolved(true);
		path.pop();
		
		return tdecl.xtype;
	}
}


@singleton
public final class KievFE_CheckStdTypes extends TransfProcessor {
	
	private boolean done;
	
	private KievFE_CheckStdTypes() { super(KievExt.JavaOnly); }
	public String getDescr() { "Check StdTypes are resolved" }

	public boolean isEnabled() {
		return !done;
	}
	public boolean isDisabled() {
		return done;
	}

	public void process(ASTNode node, Transaction tr) {
		if (done)
			return;
		StdTypes.tpAny.checkResolved();
		StdTypes.tpVoid.checkResolved();
		StdTypes.tpBoolean.checkResolved();
		StdTypes.tpChar.checkResolved();
		StdTypes.tpByte.checkResolved();
		StdTypes.tpShort.checkResolved();
		StdTypes.tpInt.checkResolved();
		StdTypes.tpLong.checkResolved();
		StdTypes.tpFloat.checkResolved();
		StdTypes.tpDouble.checkResolved();
		StdTypes.tpNull.checkResolved();
		StdTypes.tpRule.checkResolved();
		StdTypes.tpBooleanRef.checkResolved();
		StdTypes.tpByteRef.checkResolved();
		StdTypes.tpCharRef.checkResolved();
		StdTypes.tpNumberRef.checkResolved();
		StdTypes.tpShortRef.checkResolved();
		StdTypes.tpIntRef.checkResolved();
		StdTypes.tpLongRef.checkResolved();
		StdTypes.tpFloatRef.checkResolved();
		StdTypes.tpDoubleRef.checkResolved();
		StdTypes.tpVoidRef.checkResolved();
		StdTypes.tpObject.checkResolved();
		StdTypes.tpClass.checkResolved();
		StdTypes.tpDebug.checkResolved();
		StdTypes.tpTypeInfo.checkResolved();
		StdTypes.tpTypeInfoInterface.checkResolved();
		StdTypes.tpCloneable.checkResolved();
		StdTypes.tpString.checkResolved();
		StdTypes.tpThrowable.checkResolved();
		StdTypes.tpError.checkResolved();
		StdTypes.tpException.checkResolved();
		StdTypes.tpCastException.checkResolved();
		StdTypes.tpJavaEnumeration.checkResolved();
		StdTypes.tpKievEnumeration.checkResolved();
		StdTypes.tpArrayEnumerator.checkResolved();
		StdTypes.tpRuntimeException.checkResolved();
		StdTypes.tpAssertException.checkResolved();
		StdTypes.tpEnum.checkResolved();
		StdTypes.tpAnnotation.checkResolved();
		StdTypes.tpClosure.checkResolved();
		StdTypes.tpPrologVar.checkResolved();
		StdTypes.tpRefProxy.checkResolved();
		StdTypes.tpTypeSwitchHash.checkResolved();
		done = true;
	}
}

////////////////////////////////////////////////////
//												   //
//	   PASS 3- struct members					   //
//												   //
////////////////////////////////////////////////////

@singleton
public final class KievFE_Pass3 extends TransfProcessor {
	private KievFE_Pass3() { super(KievExt.JavaOnly); }
	public String getDescr() { "Class members" }

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode astn) {
	}

	public void doProcess(FileUnit:ASTNode astn) {
		foreach (ASTNode n; astn.members)
			doProcess(n);
	}

	public void doProcess(NameSpace:ASTNode astn) {
		foreach (ASTNode n; astn.members)
			doProcess(n);
	}

	public void doProcess(ComplexTypeDecl:ASTNode astn) {
		int pos = astn.pos;
		TypeDecl me = astn;
		trace(Kiev.debug && Kiev.debugResolve,"Pass 3 for class "+me);
		// Process members
		for(int i=0; i < me.members.length; i++) {
			if( me.members[i] instanceof Initializer ) {
				Initializer init = (Initializer)me.members[i];
				// TODO: check flags for initialzer
			}
			else if( me.members[i] instanceof RuleMethod ) {
				RuleMethod m = (RuleMethod)me.members[i];
				m.pass3();
				if( m.isPrivate() ) m.setFinal(true);
				if( me.isClazz() && me.isFinal() ) m.setFinal(true);
				else if( me.isInterface() ) 	m.setPublic();
				MetaAccess.verifyDecl(m);
			}
			else if( me.members[i] instanceof Method ) {
				Method m = (Method)me.members[i];
				m.pass3();
				if( m.isPrivate() )
					m.setFinal(false);
				if( me.isClazz() && me.isFinal() ) {
					m.setFinal(true);
				}
				else if( me.isInterface() && !me.isStructView() ) {
					m.setPublic();
					m.setFinal(false);
					if (m.body == null)
						m.setAbstract(true);
				}
				if( m instanceof Constructor ) {
					m.setAbstract(false);
					m.setSynchronized(false);
					m.setFinal(false);
				}
				MetaAccess.verifyDecl(m);
			}
			else if( me.members[i] instanceof Field ) {
				Field fdecl = (Field)me.members[i];
				Field f = fdecl;
				f.verifyMetas();
				// TODO: check flags for fields
				if( me.isStructView() && !f.isStatic() && f.sname != nameImpl) {
					//f.setFinal(true);
					f.setAbstract(true);
					f.setVirtual(true);
				}
				if( me.isInterface() ) {
					if (f.isVirtual()) {
						f.setAbstract(true);
					} else {
						if (me.isMixin()) {
							if !(f.isPrivate())
								f.setVirtual(true);
						} else {
							f.setStatic(true);
							f.setFinal(true);
						}
					}
					if (!me.isMixin()) {
						f.setPublic();
					}
					else if !(f.isPublic() || f.isPrivate()) {
						Kiev.reportWarning(f,"Mixin fields must be either public or private");
						f.setPublic();
					}
				}
				MetaAccess.verifyDecl(f); // recheck access
				Type ftype = fdecl.getType();
				MetaPacked pack = f.getMetaPacked();
				if( pack != null ) {
					if( f.isStatic() ) {
						Kiev.reportWarning(fdecl,"Packing of static field(s) ignored");
						~pack;
					}
					else if( !ftype.isIntegerInCode() ) {
						if( ftype.getStruct() != null && ftype.getStruct().isEnum() ) {
							Kiev.reportError(fdecl,"Packing of enum is not implemented yet");
						} else {
							Kiev.reportError(fdecl,"Packing of reference type is not allowed");
						}
						~pack;
					} else {
						int max_pack_size = 32;
						if( ftype ≡ Type.tpShort || ftype ≡ Type.tpChar ) {
							max_pack_size = 16;
							if( pack.size <= 0 ) pack.size = 16;
						}
						else if( ftype ≡ Type.tpByte ) {
							max_pack_size = 8;
							if( pack.size <= 0 ) pack.size = 8;
						}
						else if( ftype ≡ Type.tpBoolean) {
							max_pack_size = 1;
							if( pack.size <= 0 ) pack.size = 1;
						}
						if( pack.size < 0 || pack.size > max_pack_size ) {
							Kiev.reportError(fdecl,"Bad size "+pack.size+" of packed field");
							~pack;
						}
						else if( pack.offset >= 0 && pack.size+pack.offset > 32) {
							Kiev.reportError(fdecl,"Size+offset "+(pack.size+pack.offset)+" do not fit in 32 bit boundary");
							~pack;
						}
					}
				}
				if( f.getMetaPacked() != null )
					f.setPackedField(true);
			}
			else if( me.members[i] instanceof WBCCondition ) {
				WBCCondition inv = (WBCCondition)me.members[i];
				assert(inv.cond == WBCType.CondInvariant);
				// TODO: check flags for fields
				Method m = new MethodImpl(inv.sname,Type.tpVoid,inv.mflags);
				m.setInvariantMethod(true);
				m.body = new Block();
				inv.replaceWithNode(m);
				m.conditions += inv;
			}
			// Inner classes and cases after all methods and fields, skip now
		}
		
		if (me.isSingleton()) {
			me.setFinal(true);
			if (me.resolveField(nameInstance, false) == null) {
				Field inst = new Field(nameInstance, me.xtype, ACC_STATIC|ACC_FINAL|ACC_PUBLIC|ACC_SYNTHETIC);
				inst.pos = me.pos;
				inst.init = new NewExpr(me.pos, me.xtype, ENode.emptyArray);
				me.members += inst;
			}
		}

		// Process inner classes and cases
		foreach (TypeDecl n; me.members)
			doProcess(n);
	}
}

////////////////////////////////////////////////////
//	   PASS Meta 1 - resolve meta decls           //
////////////////////////////////////////////////////

@singleton
public final class KievFE_MetaDecls extends TransfProcessor {
	private KievFE_MetaDecls() { super(KievExt.JavaOnly); }
	public String getDescr() { "Annotation's declaration" }

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
	}
	public void doProcess(FileUnit:ASTNode node) {
		foreach(ASTNode n; node.members)
			doProcess(n);
	}
	public void doProcess(NameSpace:ASTNode node) {
		foreach(ASTNode n; node.members)
			doProcess(n);
	}
	public void doProcess(Struct:ASTNode node) {
		foreach (Struct sub; node.members)
			doProcess(sub);
	}
	public void doProcess(JavaAnnotation:ASTNode clazz) {
		foreach(ASTNode n; clazz.members) {
			if( n instanceof Method ) {
				Method m = (Method)n;
				if (m.params.length != 0)
					Kiev.reportError(m, "Annotation methods may not have arguments");
				if (m.body != null && !(m.body instanceof MetaValue))
					Kiev.reportError(m, "Annotation methods may not have bodies");
				if (m.conditions.length > 0)
					Kiev.reportError(m, "Annotation methods may not have work-by-contruct conditions");
				m.setPublic();
				m.setAbstract(true);
				m.pass3();
				if (m.mtype.ret() ≡ Type.tpVoid || m.mtype.ret() ≡ Type.tpRule)
					Kiev.reportError(m, "Annotation methods must return a value");
			}
			else if( n instanceof Field )
				;
			else if( n instanceof Struct )
				;
			else if( n instanceof Comment )
				;
			else
				Kiev.reportError(n, "Annotations may only have methods and final fields");
		}
	}
}

////////////////////////////////////////////////////
//	   PASS Meta 2 - resolve meta defaults        //
////////////////////////////////////////////////////

@singleton
public final class KievFE_MetaDefaults extends TransfProcessor {
	private KievFE_MetaDefaults() { super(KievExt.JavaOnly); }
	public String getDescr() { "Annotation's defaults" }

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
	}

	public void doProcess(NameSpace:ASTNode fu) {
		foreach(ASTNode n; fu.members)
			doProcess(n);
	}

	public void doProcess(ComplexTypeDecl:ASTNode clazz) {
		if (clazz instanceof JavaAnnotation)
			clazz.resolveMetaDefaults();
		foreach(ASTNode n; clazz.members)
			doProcess(n);
	}
}

////////////////////////////////////////////////////
//	   PASS Meta 3 - resolve meta annotations     //
////////////////////////////////////////////////////

@singleton
public final class KievFE_MetaValues extends TransfProcessor {
	private KievFE_MetaValues() { super(KievExt.JavaOnly); }
	public String getDescr() { "Annotation values" }

	public void process(ASTNode node, Transaction tr) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) {
				if !(n instanceof ASTNode)
					return false;
				if (n instanceof MNode)
					n.resolve(null);
				return true;
			}
		});
	}
}

////////////////////////////////////////////////////
//	   PASS 5 - pre-resolve                       //
////////////////////////////////////////////////////

@singleton
public final class KievFE_PreResolve extends TransfProcessor {
	private KievFE_PreResolve() { super(KievExt.JavaOnly); }
	public String getDescr() { "Kiev pre-resolve" }

	public void process(ASTNode node, Transaction tr) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return n.preResolveIn(); return false; }
			public void post_exec(ANode n) { if (n instanceof ASTNode) n.preResolveOut(); }
		});
		return;
	}
}

////////////////////////////////////////////////////
//	   PASS 5 - main-resolve                      //
////////////////////////////////////////////////////

@singleton
public final class KievFE_MainResolve extends TransfProcessor {
	private KievFE_MainResolve() { super(KievExt.JavaOnly); }
	public String getDescr() { "Kiev main-resolve" }

	public void process(ASTNode node, Transaction tr) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return n.mainResolveIn(); return false; }
			public void post_exec(ANode n) { if (n instanceof ASTNode) n.mainResolveOut(); }
		});
		return;
	}
}

////////////////////////////////////////////////////
//	   PASS - midend dump API as XML files        //
////////////////////////////////////////////////////

@singleton
public final class KievME_DumpAPI extends BackendProcessor {

	//private ATextSyntax stx;

	private KievME_DumpAPI() { super(KievBackend.Generic); }
	public String getDescr() { "Dump API files" }

	public boolean isEnabled() {
		return Kiev.enabled(KievExt.DumpAPI);
	}

	public void process(ASTNode node, Transaction tr) {
		if!(node instanceof FileUnit)
			return;
		FileUnit fu = (FileUnit)node;
		if (fu.scanned_for_interface_only)
			return;
		//if (this.stx == null)
		//	this.stx = new XmlDumpSyntax("api");
		try {
			dumpAPI(fu);
		} catch (Exception rte) { Kiev.reportError(rte); }
	}

	public void dumpAPI(FileUnit fu) {
		if( Kiev.verbose ) System.out.println("Dumping API of source file "+fu);

		foreach (ASTNode n; fu.members) {
			if (n instanceof TypeDecl)
				dumpAPI((TypeDecl)n);
			else if (n instanceof NameSpace)
				dumpAPI((NameSpace)n);
			else if (n instanceof DumpSerialized)
				dumpAPI((DumpSerialized)n);
		}
	}
	public void dumpAPI(NameSpace ns) {
		foreach (ASTNode n; ns.members) {
			if (n instanceof TypeDecl)
				dumpAPI((TypeDecl)n);
			else if (n instanceof NameSpace)
				dumpAPI((NameSpace)n);
			else if (n instanceof DumpSerialized)
				dumpAPI((DumpSerialized)n);
		}
	}
	public void dumpAPI(TypeDecl td) {
		if (td.isPrivate() || (td instanceof TypeDef))
			return;
		String output_dir = Kiev.output_dir;
		if( output_dir==null ) output_dir = "classes";
		try {
			String out_file = td.qname().replace('\u001f',File.separatorChar)+".xml";
			File f = new File(output_dir,out_file);
			DumpUtils.dumpToXMLFile("api", td, f);
		} catch (IOException e) {
			System.out.println("Create/write error while API dump: "+e);
		}
	}
	public void dumpAPI(DumpSerialized stx) {
		String output_dir = Kiev.output_dir;
		if( output_dir==null ) output_dir = "classes";
		FileOutputStream fo = null;
		try {
			String out_file = stx.qname().replace('\u001f',File.separatorChar)+".ser";
			File f = new File(output_dir,out_file);
			File dir = f.getParentFile();
			if (dir != null) {
				dir.mkdirs();
				if( !dir.exists() || !dir.isDirectory() ) throw new IOException("Can't create output dir "+dir);
			}
			FileOutputStream fo = new FileOutputStream(f);
			ObjectOutput so = new ObjectOutputStream(fo);
			so.writeObject(stx.getDataToSerialize());
			so.flush();
		} catch (IOException e) {
			System.out.println("Create/write error while API dump: "+e);
		} finally {
			if (fo != null)
				fo.close();
		}
	}
}

////////////////////////////////////////////////////
//	   PASS - backend pre-generation              //
////////////////////////////////////////////////////

@singleton
public final class KievME_PreGenartion extends BackendProcessor {
	private KievME_PreGenartion() { super(KievBackend.Java15); }
	public String getDescr() { "Kiev pre-generation" }

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"KievME_PreGenartion");
		try {
			node.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) {
					if (n instanceof ASTNode) {
						ASTNode astn = (ASTNode)n;
						return ((RNode)astn).preGenerate();
					}
					return false;
				}
			});
		} finally { tr.leave(); }
	}
}


////////////////////////////////////////////////////
//	   PASS - backend resolve                     //
////////////////////////////////////////////////////

@singleton
public final class KievBE_Resolve extends BackendProcessor {
	private KievBE_Resolve() { super(KievBackend.Java15); }
	public String getDescr() { "Kiev resolve" }

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"KievBE_Resolve");
		try {
			if (node instanceof ENode)
				node.resolve(null);
			else if (node instanceof DNode)
				node.resolveDecl();
			else if (node instanceof SNode)
				node.resolveDecl();
		} finally { tr.leave(); }
	}
}

////////////////////////////////////////////////////
//	   PASS - backend generate                    //
////////////////////////////////////////////////////

@singleton
public final class KievBE_Generate extends BackendProcessor {
	private KievBE_Generate() { super(KievBackend.Java15); }
	public String getDescr() { "Class generation" }

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof FileUnit) {
			FileUnit fu = (FileUnit)node;
			if (fu.scanned_for_interface_only)
				return;
			tr = Transaction.enter(tr,"KievBE_Generate");
			try {
				try {
					Env.getRoot().getBackendEnv().generateFile(fu);
				} catch (Exception rte) { Kiev.reportError(rte); }
			} finally { tr.leave(); }
		}
	}
}

////////////////////////////////////////////////////
//	   PASS - backend generate                    //
////////////////////////////////////////////////////
/*
@singleton
public final class ExportBE_Generate extends BackendProcessor {
	private ExportBE_Generate() { super(KievBackend.VSrc); }
	public String getDescr() { "Source generation" }

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof FileUnit) {
			FileUnit fu = (FileUnit)node;
			if (fu.scanned_for_interface_only)
				return;
			try {
				dumpSrc(fu);
			} catch (Exception rte) { Kiev.reportError(rte); }
		}
	}

	public void dumpSrc(FileUnit fu) {
		String output_dir = Kiev.output_dir;
		if( output_dir==null ) output_dir = "classes";
		if( Kiev.verbose ) System.out.println("Dumping to source file "+fu+" into '"+output_dir+"' dir");

		try {
			String out_file = fu.pname();
			File f = new File(output_dir,out_file);
			ATextSyntax stx;
			if (fu.fname.toLowerCase().endsWith(".xml"))
				stx = new XmlDumpSyntax("full");
			else
				stx = (ATextSyntax)Env.getRoot().resolveGlobalDNode("stx-fmt\u001fsyntax-for-java");
			Env.getRoot().dumpTextFile(fu, f, stx.getCompiled().init());
		} catch (IOException e) {
			System.out.println("Create/write error while Kiev-to-Src exporting: "+e);
		}
	}
}
*/
////////////////////////////////////////////////////
//	   PASS - cleanup after backend               //
////////////////////////////////////////////////////

@singleton
public final class KievBE_Cleanup extends BackendProcessor {
	private KievBE_Cleanup() { super(KievBackend.Generic); }
	public String getDescr() { "Kiev cleanup after backend" }

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"KievBE_Cleanup");
		try {
			try {
				Env.getRoot().getBackendEnv().backendCleanup(node);
			} catch (Exception rte) { Kiev.reportError(rte); }
		} finally { tr.leave(); }
	}
}

