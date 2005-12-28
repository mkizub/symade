package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

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
	//													//
	//		   PASS 1 - process file syntax				//
	//													//
	/////////////////////////////////////////////////////
	
	public void pass1(ASTNode:ASTNode node) {
		return;
	}
	
	public void pass1(FileUnit:ASTNode astn) {
		FileUnit fu = astn;
		processFileHeader(fu);
		foreach (ASTNode n; astn.members) {
			try {
				processSyntax((Struct)n);
			} catch(Exception e ) { Kiev.reportError(n,e); }
		}
	}

	public void pass1(Struct:ASTNode astn) {
		processSyntax(astn);
	}
	
	private void processFileHeader(FileUnit fu) {
		// Process file imports...
		boolean java_lang_found = false;
		KString java_lang_name = KString.from("java.lang");
		boolean kiev_stdlib_found = false;
		KString kiev_stdlib_name = KString.from("kiev.stdlib");
		boolean kiev_stdlib_meta_found = false;
		KString kiev_stdlib_meta_name = KString.from("kiev.stdlib.meta");

		foreach (DNode n; fu.syntax) {
			try {
				if (n instanceof Import && ((Import)n).mode == Import.ImportMode.IMPORT_STATIC && !((Import)n).star)
					continue; // process later
				processSyntax(n);
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
			fu.syntax.add(new Import(Env.newPackage(kiev_stdlib_name),Import.ImportMode.IMPORT_CLASS,true));
		if( !Kiev.javaMode && !kiev_stdlib_meta_found )
			fu.syntax.add(new Import(Env.newPackage(kiev_stdlib_meta_name),Import.ImportMode.IMPORT_CLASS,true));
		if( !java_lang_found )
			fu.syntax.add(new Import(Env.newPackage(java_lang_name),Import.ImportMode.IMPORT_CLASS,true));
	}
	
	public void processSyntax(ASTNode:ASTNode node) {
		return;
	}

	public void processSyntax(Import:ASTNode astn) {
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
		else if (astn.mode == Import.ImportMode.IMPORT_SYNTAX && !(n instanceof Struct && ((Struct)n).isSyntax()))
			Kiev.reportError(astn,"Identifier "+name+" is not a syntax");
		else
			astn.resolved = n;
	}

//	public void processSyntax(TypeDefOp:ASTNode astn) {
//		try {
//			if (astn.typearg != null) {
//				astn.type = new TypeRef(astn.type.getType().getInitialType());
//			} else {
//				astn.type = new TypeRef(astn.type.getType());
//			}
//		} catch (RuntimeException e) { /* ignore */ }
//	}

	
	public void processSyntax(Opdef:ASTNode astn) {
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

	public void processSyntax(Struct:ASTNode astn) {
		// Verify meta-data to the new structure
		Struct me = astn;
		me.meta.verify();
		
		if (me.isSyntax()) {
			trace(Kiev.debugResolve,"Pass 1 for syntax "+me);
			for (int i=0; i < me.members.length; i++) {
				ASTNode n = me.members[i];
				try {
					if (n instanceof TypeDefOp) {
						processSyntax(n);
						me.imported.add(me.members[i]);
						trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
					}
					else if (n instanceof Opdef) {
						processSyntax(n);
						me.imported.add(me.members[i]);
						trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
					}
				} catch(Exception e ) {
					Kiev.reportError(n,e);
				}
			}
		}
	}
	
	/////////////////////////////////////////////////////
	//													//
	//		   PASS 2 - create types for structures	//
	//													//
	/////////////////////////////////////////////////////


	public void pass2(ASTNode:ASTNode node) {
		return;
	}
	
	public void pass2(FileUnit:ASTNode astn) {
		FileUnit fu = astn;

		foreach (DNode n; astn.syntax) {
			try {
				if (n instanceof TypeDefOp) {
					TypeDefOp tdop = (TypeDefOp)n;
					if (tdop.typearg != null) {
						tdop.type = new TypeRef(getStructType(tdop.type));
					} else {
						getStructType(tdop.type);
						tdop.type.getType();
					}
				}
			} catch(Exception e ) {
				Kiev.reportError(n,e);
			}
		}

		foreach (DNode n; astn.members)
			pass2(n);
	}

	public void pass2(Struct:ASTNode astn) {
		try {
			Struct clazz = (Struct)astn;
			getStructType(clazz);
			setStructArgTypes(clazz);
			if( !clazz.isPackage() ) {
				foreach (DNode s; clazz.members; s instanceof Struct) {
					getStructType((Struct)s);
					setStructArgTypes((Struct)s);
				}
			}
		} catch(Exception e ) { Kiev.reportError(astn,e); }
	}
	
	private BaseType getStructType(Struct clazz) {
		if (clazz.isTypeResolved()) {
			if (clazz.type == null)
				throw new CompilerException(clazz, "Recursive type declaration for class "+clazz);
			return clazz.type;
		}
		
		if (clazz.isPackage())
			throw new RuntimeException("Unassigned type for a package: "+clazz);

		clazz.setTypeResolved(true);
		
		for (Struct p = clazz.package_clazz; p != null; p = p.package_clazz)
			getStructType(p);

		if (clazz.parent instanceof FileUnit)
			clazz.setStatic(true);

		if (clazz.isAnnotation()) {
			clazz.super_type = Type.tpObject;
			clazz.interfaces.add(new TypeRef(Type.tpAnnotation));
			setupStructType(clazz, false);
		}
		else if (clazz.isEnum()) {
			clazz.setStatic(true);
			clazz.super_type = Type.tpEnum;
			setupStructType(clazz, false);
			// assign type of enum fields
			if (clazz.isEnum()) {
				foreach (DNode n; clazz.members; n instanceof Field && ((Field)n).isEnumField()) {
					Field f = (Field)n;
					f.ftype = new TypeRef(clazz.type);
				}
			}
		}
		else if (clazz.isPizzaCase()) {
			clazz.setStatic(true);
			Struct p = clazz.ctx_clazz;
			p.addCase(clazz);
			getStructType(p);
			TypeWithArgsRef sup_ref = new TypeWithArgsRef(new TypeRef(p.type));
		next_case_arg:
			for(int i=0; i < p.args.length; i++) {
				for(int j=0; j < clazz.args.length; j++) {
					if (p.args[i].name.name == clazz.args[j].name.name) {
						sup_ref.args.add(new TypeRef(clazz.args[j].getAType()));
						continue next_case_arg;
					}
				}
				sup_ref.args.add(new TypeRef(p.args[i].getAType()));
			}
			clazz.super_bound = sup_ref;
			getStructType(sup_ref);
			setupStructType(clazz, true);
		}
		else if (clazz.isSyntax()) {
			clazz.setPrivate(true);
			clazz.setAbstract(true);
			clazz.setMembersGenerated(true);
			clazz.setStatementsGenerated(true);
			clazz.super_type = null;
			setupStructType(clazz, false);
		}
		else if( clazz.isInterface() ) {
			clazz.super_type = Type.tpObject;
			foreach(TypeRef tr; clazz.interfaces)
				getStructType(tr);
			setupStructType(clazz, true);
		}
		else {
			if (clazz.view_of != null)
				getStructType(clazz.view_of);
			Type sup = getStructType(clazz.super_bound);
			if (sup == null && !clazz.name.name.equals(Type.tpObject.clazz.name.name))
				clazz.super_type = Type.tpObject;
			foreach(TypeRef tr; clazz.interfaces)
				getStructType(tr);
			setupStructType(clazz, true);
		}
		
		return clazz.type;
	}

	private void setupStructType(Struct clazz, boolean canHaveArgs) {
		if (!canHaveArgs) {
			if( clazz.args.length > 0 ) {
				Kiev.reportError(clazz,"Type parameters are not allowed for "+clazz);
				clazz.args.delAll();
			}
		}
		else if (clazz.parent instanceof Struct) {
			if (!clazz.isStatic())
				clazz.args.delAll();
		}

		TVarSet vs = new TVarSet();
		// add own class arguments
		for (int i=0; i < clazz.args.length; i++)
			vs.append(clazz.args[i].getAType(), null);
		// add super-class bindings
		if (clazz.super_bound != null && clazz.super_bound.getType() != null)
			vs.append(clazz.super_type.bindings());
		// add super-interfaces bindings
		foreach(TypeRef tr; clazz.interfaces)
			vs.append(tr.getType().bindings());
		// add outer class bindings
		for (Struct s = clazz; !s.isStatic() && !s.package_clazz.isPackage(); s = s.package_clazz)
			vs.append(s.package_clazz.type.bindings());

		// Generate type for this structure
		clazz.type = Type.createRefType(clazz, vs);
	}

	private BaseType setStructArgTypes(Struct clazz) {
		if (clazz.isArgsResolved())
			return clazz.type;

		// Process inheritance of class's arguments
		for (int i=0; i < clazz.args.length; i++) {
			TypeArgDef arg = clazz.args[i];
			if( arg.super_bound != null ) {
				Type sup = getStructType(arg.super_bound);
				if (sup == null)
					sup = Type.tpObject;
				if( !sup.isReference() )
					Kiev.reportError(clazz,"Argument extends bad type "+sup);
				arg.getAType().super_type = sup;
			}
		}

		// check super-class bindings
		if (clazz.super_type != null) {
			setStructArgTypes(clazz.super_type.clazz);
			clazz.type.bind(clazz.super_type.bindings());
		}
		// add super-interfaces bindings
		foreach(TypeRef tr; clazz.interfaces) {
			setStructArgTypes(tr.getType().getStruct());
			clazz.type.bind(tr.getType().bindings());
		}
		// add outer class bindings
		for (Struct s = clazz; !s.isStatic() && !s.package_clazz.isPackage(); s = s.package_clazz) {
			setStructArgTypes(s.package_clazz);
			clazz.type.bind(s.package_clazz.type.bindings());
		}

		foreach (TVar tv; clazz.type.bindings().tvars) {
			if (!tv.isBound()) {
				clazz.setRuntimeArgTyped(true);
				break;
			}
		}
		if (clazz.isRuntimeArgTyped())
			clazz.interfaces.append(new TypeRef(Type.tpTypeInfoInterface));

		clazz.setArgsResolved(true);
		return clazz.type;
	}

	private Type getStructType(TypeRef tr) {
		if (tr instanceof TypeNameRef) {
			TypeNameRef tnr = (TypeNameRef)tr;
			KString nm = tnr.name.name;
			DNode@ v;
			if (!PassInfo.resolveQualifiedNameR(tnr,v,new ResInfo(tnr,ResInfo.noForwards),nm))
				throw new CompilerException(tnr,"Unresolved identifier "+nm);
			if (v instanceof Struct)
				getStructType((Struct)v);
		}
		else if (tr instanceof TypeWithArgsRef) {
			TypeWithArgsRef twar = (TypeWithArgsRef)tr;
			getStructType(twar.base_type);
			foreach (TypeRef tr; twar.args)
				getStructType(tr);
		}
		return tr.getType();
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
			else if (members[i] instanceof Field && ((Field)members[i]).isEnumField()) {
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
				f.init = new NewExpr(f.pos,me.type,new ENode[]{
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
				if( me.isView() && !f.isStatic()) {
					f.setFinal(true);
					f.setAbstract(true);
					f.setVirtual(true);
				}
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
						f.init = new NewExpr(fdecl.pos,ftype,ENode.emptyArray);
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
				MethodType mt = MethodType.newMethodType(Type.emptyArray,Type.tpVoid);
				Method m = new Method(inv.name.name,mt,inv.flags);
				m.setInvariantMethod(true);
				m.body = new BlockStat();
				inv.replaceWithNode(m);
				m.conditions += inv;
			}
			// Inner classes and cases after all methods and fields, skip now
			else if( members[i] instanceof Struct );
			else if( members[i] instanceof Import ) {
				me.imported.add((Import)members[i]);
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
					if (m.type.ret ≡ Type.tpVoid || m.type.ret ≡ Type.tpRule)
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
//		JPackage jroot = (JPackage)new TreeMapper().mapStruct(Env.root);
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
					node.getJFileUnitView().generate();
				} catch (Exception rte) { Kiev.reportError(rte); }
			}
		}
	}
}


