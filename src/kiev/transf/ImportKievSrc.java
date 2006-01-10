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

@singleton
public final class ImportKievSrc extends TransfProcessor implements Constants {

	private ImportKievSrc() {
		super(Kiev.Ext.JavaOnly);
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
		KString name = astn.name.name;
		Struct scope = Env.root;
		DNode n;
		int dot;
		do {
			dot = name.indexOf('.');
			KString head;
			if (dot > 0) {
				head = name.substr(0,dot);
				name = name.substr(dot+1);
			} else {
				head = name;
			}
			DNode@ node;
			if!(scope.resolveNameR(node,new ResInfo(astn,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports),head)) {
				Kiev.reportError(astn,"Unresolved identifier "+name+" in "+scope);
				return;
			}
			n = node;
			if (node instanceof Struct)
				scope = (Struct)node;
		} while (dot > 0);
		if		(astn.mode == Import.ImportMode.IMPORT_CLASS && !(n instanceof Struct))
			Kiev.reportError(astn,"Identifier "+name+" is not a class or package");
		else if (astn.mode == Import.ImportMode.IMPORT_PACKAGE && !(n instanceof Struct && ((Struct)n).isPackage()))
			Kiev.reportError(astn,"Identifier "+name+" is not a package");
		else if (astn.mode == Import.ImportMode.IMPORT_STATIC && !(astn.star || (n instanceof Field)))
			Kiev.reportError(astn,"Identifier "+name+" is not a field");
		else if (astn.mode == Import.ImportMode.IMPORT_SYNTAX && !(n instanceof Struct && ((Struct)n).isSyntax()))
			Kiev.reportError(astn,"Identifier "+name+" is not a syntax");
		else {
			assert (n != null);
			astn.resolved = n;
		}
	}

	
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
		Struct me = astn;
		if (me.isAnnotation() || me.isEnum() || me.isSyntax()) {
			if( me.args.length > 0 ) {
				Kiev.reportError(me,"Type parameters are not allowed for "+me);
				me.args.delAll();
			}
			me.setTypeUnerasable(false);
		}
		else if!(me.parent instanceof FileUnit) {
			if (!me.isStatic()) {
				Struct pkg = me.package_clazz;
				if (me.isClazz() && pkg.isClazz()) {
					int n = 0;
					for(Struct p=pkg; p.isClazz() && !p.isStatic(); p=p.package_clazz) n++;
					TypeDef td = new TypeDef(
						new NameRef(me.pos,KString.from("outer$"+n+"$type")),
						new TypeRef(pkg.imeta_type.templ_type));
					me.members.append(td);
					OuterTypeProvider.instance(me,td);
					Field f = new Field(KString.from(nameThis+"$"+n),td.getAType(),ACC_FORWARD|ACC_FINAL);
					f.pos = me.pos;
					me.members.append(f);
				}
			}
		}
		if (me.isTypeUnerasable()) {
			foreach (TypeDef a; me.args)
				a.setTypeUnerasable(true);
		}
		
		if (me.isTypeUnerasable()) {
			foreach (TypeDef a; me.args)
				a.setTypeUnerasable(true);
		}

		if (me.isSyntax()) {
			trace(Kiev.debugResolve,"Pass 1 for syntax "+me);
			for (int i=0; i < me.members.length; i++) {
				ASTNode n = me.members[i];
				try {
					if (n instanceof TypeOpDef) {
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

		foreach (DNode dn; me.members; dn instanceof Struct)
			processSyntax(dn);
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
				if (n instanceof TypeDef)
					n.getType();
				else if (n instanceof TypeOpDef)
					n.getType();
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
			// Verify meta-data to the new structure
			clazz.meta.verify();
			foreach (DNode dn; clazz.members; dn.meta != null)
				dn.meta.verify();
			getStructType(clazz, new Stack<Struct>());
			if( !clazz.isPackage() ) {
				foreach (DNode s; clazz.members; s instanceof Struct)
					pass2(s);
			}
		} catch(Exception e ) { Kiev.reportError(astn,e); }
	}
	
	private ConcreteType getStructType(Struct clazz, Stack<Struct> path) {
		if (clazz.isTypeResolved()) {
			if (!clazz.isArgsResolved())
				throw new CompilerException(clazz, "Recursive type declaration for class "+clazz+" via "+path);
			return clazz.concr_type;
		}
		path.push(clazz);
		
		clazz.setTypeResolved(true);
		
		for (Struct p = clazz.package_clazz; p != null; p = p.package_clazz)
			getStructType(p, path);

		if (clazz.parent instanceof FileUnit)
			clazz.setStatic(true);

		if (clazz.isAnnotation()) {
			clazz.super_type = Type.tpObject.toTypeWithLowerBound(clazz.concr_type);
			clazz.interfaces.add(new TypeRef(Type.tpAnnotation));
		}
		else if (clazz.isEnum()) {
			clazz.setStatic(true);
			clazz.super_type = Type.tpEnum.toTypeWithLowerBound(clazz.concr_type);
			// assign type of enum fields
			if (clazz.isEnum()) {
				foreach (DNode n; clazz.members; n instanceof Field && ((Field)n).isEnumField()) {
					Field f = (Field)n;
					f.ftype = new TypeRef(clazz.concr_type);
				}
			}
		}
		else if (clazz.isPizzaCase()) {
			clazz.setStatic(true);
			Struct p = clazz.ctx_clazz;
			p.addCase(clazz);
			getStructType(p, path);
			TypeWithArgsRef sup_ref = new TypeWithArgsRef(new TypeRef(p.concr_type));
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
		}
		else if (clazz.isSyntax() || clazz.isPackage()) {
			clazz.setAbstract(true);
			clazz.setMembersGenerated(true);
			clazz.setStatementsGenerated(true);
			clazz.super_type = null;
		}
		else if( clazz.isInterface() ) {
			clazz.super_type = Type.tpObject.toTypeWithLowerBound(clazz.concr_type);
			foreach(TypeRef tr; clazz.interfaces) {
				Struct s = tr.getType().getStruct();
				if (s != null)
					getStructType(s, path);
			}
		}
		else {
			if (clazz.view_of != null)
				clazz.view_of.getType();
			Type sup = clazz.super_bound == null ? null : clazz.super_bound.getType();
			if (sup == null && !clazz.name.name.equals(Type.tpObject.clazz.name.name))
				clazz.super_type = sup = Type.tpObject.toTypeWithLowerBound(clazz.concr_type);
			if (sup != null) {
				Struct s = sup.getStruct();
				if (s != null)
					getStructType(s, path);
			}
			foreach(TypeRef tr; clazz.interfaces) {
				Struct s = tr.getType().getStruct();
				if (s != null)
					getStructType(s, path);
			}
		}
		
		clazz.imeta_type.version++;
		clazz.concr_type.bindings(); // update the type
		if (clazz.concr_type.isUnerasable()) {
			if (!clazz.isTypeUnerasable()) {
				Kiev.reportWarning(clazz,"Type "+clazz+" must be annotated as @unerasable");
				clazz.setTypeUnerasable(true);
				foreach (TypeDef a; clazz.args)
					a.setTypeUnerasable(true);
			}
			clazz.interfaces.append(new TypeRef(Type.tpTypeInfoInterface));
		}

		clazz.setArgsResolved(true);
		path.pop();
		
		return clazz.concr_type;
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
				else if( me.isInterface() ) 	m.setPublic();
				Access.verifyDecl(m);
			}
			else if( members[i] instanceof Method ) {
				Method m = (Method)members[i];
				m.pass3();
				if( me.isPackage() ) m.setStatic(true);
				if( m.isPrivate() ) m.setFinal(false);
				else if( me.isClazz() && me.isFinal() ) m.setFinal(true);
				else if( me.isInterface() ) {
					m.setPublic();
				}
				if( m.name.equals(nameInit) ) {
					m.setAbstract(false);
					m.setSynchronized(false);
					m.setFinal(false);
				}
				Access.verifyDecl(m);
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
				f.init = new NewExpr(f.pos,me.concr_type,new ENode[]{
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
				if( me.isStructView() && !f.isStatic()) {
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
					f.setPublic();
				}
				Access.verifyDecl(f); // recheck access
				Type ftype = fdecl.type;
				MetaPacked pack = f.getMetaPacked();
				if( pack != null ) {
					if( f.isStatic() ) {
						Kiev.reportWarning(fdecl,"Packing of static field(s) ignored");
						f.delNodeData(MetaPacked.ID);
					}
					else if( !ftype.isIntegerInCode() ) {
						if( ftype.isEnum() ) {
							Kiev.reportError(fdecl,"Packing of enum is not implemented yet");
						} else {
							Kiev.reportError(fdecl,"Packing of reference type is not allowed");
						}
						f.delNodeData(MetaPacked.ID);
					} else {
						int max_pack_size = 32;
						if( ftype ≡ Type.tpShort || ftype ≡ Type.tpChar ) {
							max_pack_size = 16;
							if( pack.getSize() <= 0 ) pack.setSize(16);
						}
						else if( ftype ≡ Type.tpByte ) {
							max_pack_size = 8;
							if( pack.getSize() <= 0 ) pack.setSize(8);
						}
						else if( ftype ≡ Type.tpBoolean) {
							max_pack_size = 1;
							if( pack.getSize() <= 0 ) pack.setSize(1);
						}
						if( pack.getSize() < 0 || pack.getSize() > max_pack_size ) {
							Kiev.reportError(fdecl,"Bad size "+pack.getSize()+" of packed field");
							f.delNodeData(MetaPacked.ID);
						}
						else if( pack.getOffset() >= 0 && pack.getSize()+pack.getOffset() > 32) {
							Kiev.reportError(fdecl,"Size+offset "+(pack.getSize()+pack.getOffset())+" do not fit in 32 bit boundary");
							f.delNodeData(MetaPacked.ID);
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
				Method m = new Method(inv.name.name,Type.tpVoid,inv.flags);
				m.setInvariantMethod(true);
				m.body = new BlockStat();
				inv.replaceWithNode(m);
				m.conditions += inv;
			}
			// Inner classes and cases after all methods and fields, skip now
			else if( members[i] instanceof TypeDef );
			else if( members[i] instanceof Struct );
			else if( members[i] instanceof Import ) {
				me.imported.add((Import)members[i]);
			}
			else {
				throw new CompilerException(members[i],"Unknown type of structure member: "+members[i]);
			}
		}
		
		if (me.isSingleton()) {
			me.setFinal(true);
			if (me.resolveField(nameInstance, false) == null) {
				Field inst = new Field(nameInstance, me.concr_type, ACC_STATIC|ACC_FINAL|ACC_PUBLIC);
				inst.pos = me.pos;
				inst.init = new NewExpr(me.pos, me.concr_type, ENode.emptyArray);
				me.addField(inst);
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
					m.setPublic();
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

	public void verify(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { return n.preVerify(); }
			public void post_exec(ASTNode n) { n.postVerify(); }
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
			return JavaBackend;
		return null;
	}
	
}

@singleton
class JavaBackend extends BackendProcessor {

	private JavaBackend() {
		super(Kiev.Backend.Java15);
	}
	
	public void preGenerate() {
		foreach (FileUnit fu; Kiev.files) {
			fu.walkTree(new TreeWalker() {
				public boolean pre_exec(ASTNode n) { return n.preGenerate(); }
			});
		}
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


