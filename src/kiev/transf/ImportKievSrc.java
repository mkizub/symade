package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;
import java.io.*;

import kiev.be.java15.JFileUnit;
import kiev.fmt.JavaSyntax;
import kiev.fmt.TextFormatter;
import kiev.fmt.TextPrinter;
import kiev.fmt.Drawable;

import static kiev.stdlib.Debug.*;
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
	private KievFE_Pass1() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Syntax" }
	
	public void process(ASTNode:ASTNode node) {
		return;
	}
	
	public void process(FileUnit:ASTNode astn) {
		FileUnit fu = astn;
		processFileHeader(fu);
		foreach (ASTNode n; astn.members) {
			try {
				processSyntax(n);
			} catch(Exception e ) { Kiev.reportError(n,e); }
		}
	}

	public void process(Struct:ASTNode astn) {
		processSyntax(astn);
	}
	
	private void processFileHeader(FileUnit fu) {
		// Process file imports...
		boolean java_lang_found = false;
		String java_lang_name = "java.lang";
		boolean kiev_stdlib_found = false;
		String kiev_stdlib_name = "kiev.stdlib";
		boolean kiev_stdlib_meta_found = false;
		String kiev_stdlib_meta_name = "kiev.stdlib.meta";

		foreach (SNode n; fu.members) {
			try {
				processSyntax(n);
				if (n instanceof Import && n.resolved instanceof Struct) {
					String name = ((Struct)n.resolved).qname();
					if( n.mode == Import.ImportMode.IMPORT_CLASS && name == java_lang_name)
						java_lang_found = true;
					else if( n.mode == Import.ImportMode.IMPORT_CLASS && name == kiev_stdlib_name)
						kiev_stdlib_found = true;
					else if( n.mode == Import.ImportMode.IMPORT_CLASS && name == kiev_stdlib_meta_name)
						kiev_stdlib_meta_found = true;
				}
				trace(Kiev.debugResolve,"Add "+n);
			} catch(Exception e ) {
				Kiev.reportError(n,e);
			}
		}
		// Add standard imports, if they were not defined
		if( !Kiev.javaMode && !kiev_stdlib_found ) {
			Import imp = new Import(Env.newPackage(kiev_stdlib_name),true);
			imp.setHidden(true);
			fu.members.add(imp);
		}
		if( !Kiev.javaMode && !kiev_stdlib_meta_found ) {
			Import imp = new Import(Env.newPackage(kiev_stdlib_meta_name),true);
			imp.setHidden(true);
			fu.members.add(imp);
		}
		if( !java_lang_found ) {
			Import imp = new Import(Env.newPackage(java_lang_name),true);
			imp.setHidden(true);
			fu.members.add(imp);
		}
	}
	
	public void processSyntax(ASTNode:ASTNode node) {
		return;
	}

	public void processSyntax(Import:ASTNode astn) {
		String name = astn.name.name;
		Struct scope = Env.root;
		DNode n;
		int dot;
		do {
			dot = name.indexOf('.');
			String head;
			if (dot > 0) {
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1).intern();
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
		String image = astn.image;
		String decl = null;
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
		Operator op = Operator.getOperator(name);
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

	public void processSyntax(Struct:ASTNode astn) {
		Struct me = astn;
		if (me.isAnnotation() || me.isEnum() || me.isSyntax()) {
			if( me.args.length > 0 ) {
				Kiev.reportError(me,"Type parameters are not allowed for "+me);
				me.args.delAll();
			}
			me.setTypeUnerasable(false);
		}
		else if!(me.parent() instanceof FileUnit) {
			if (!me.isStatic()) {
				Struct pkg = me.package_clazz;
				if (me.isClazz() && pkg.isClazz()) {
					int n = 0;
					for(Struct p=pkg; p.isClazz() && !p.isStatic(); p=p.package_clazz) n++;
					String fldName = (nameThis+"$"+n).intern();
					boolean found = false;
					foreach (Field f; me.members; f.id.equals(fldName))
						found = true;
					if (!found) {
						TypeDef td = new TypeAssign(
							new Symbol(me.pos,"outer$"+n+"$type"),
							new TypeRef(pkg.xtype));
						td.setSynthetic(true);
						me.members.append(td);
						OuterMetaType.instance(me,td);
						Field f = new Field(fldName,td.getAType(),ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC);
						f.pos = me.pos;
						me.members.append(f);
					}
				}
			}
		}
		if (me.isTypeUnerasable()) {
			foreach (TypeDef a; me.args)
				a.setTypeUnerasable(true);
		}
		
		foreach (ASTNode dn; me.members)
			processSyntax(dn);
	}
	
	public void processSyntax(TypeDecl:ASTNode astn) {
		foreach (ASTNode dn; astn.members)
			processSyntax(dn);
	}

	public void processSyntax(Method:ASTNode astn) {
		Method me = astn;
		if (me.isMacro() && me.isNative()) {
			if !(me instanceof CoreMethod) {
				CoreMethod cm = new CoreMethod();
				cm.pos = me.pos;
				cm.compileflags = me.compileflags;
				cm.flags = me.flags;
				if (me.meta != null) cm.meta = ~me.meta;
				cm.id = ~me.id;
				cm.acc = me.acc;
				cm.targs.addAll(me.targs.delToArray());
				if (me.type_ret != null) cm.type_ret = ~me.type_ret;
				if (me.dtype_ret != null) cm.dtype_ret = ~me.dtype_ret;
				cm.params.addAll(me.params.delToArray());
				cm.aliases.addAll(me.aliases.delToArray());
				if (me.body != null) cm.body = ~me.body;
				cm.conditions.addAll(me.conditions.delToArray());
				me.replaceWithNode(cm);
				return; 
			}
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
	private KievFE_Pass2() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Class types" }

	public void process(ASTNode:ASTNode node) {
		return;
	}
	
	public void process(FileUnit:ASTNode astn) {
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
			process(n);
	}

	public void process(TypeDecl:ASTNode astn) {
		try {
			TypeDecl td = (TypeDecl)astn;
			// Verify meta-data to the new structure
			if (td.meta != null)
				td.meta.verify();
			foreach (DNode dn; td.members; dn.meta != null)
				dn.meta.verify();
			getStructType(td, new Stack<TypeDecl>());
			foreach (TypeDecl s; td.members)
				process(s);
		} catch(Exception e ) { Kiev.reportError(astn,e); }
	}
	
	private Type getStructType(TypeDecl tdecl, Stack<TypeDecl> path) {
		if (tdecl.isTypeResolved()) {
			if (!tdecl.isArgsResolved())
				throw new CompilerException(tdecl, "Recursive type declaration for class "+tdecl+" via "+path);
			return tdecl.xtype;
		}
		path.push(tdecl);
		
		tdecl.setTypeResolved(true);
		
		if (tdecl instanceof Struct) {
			for (Struct p = tdecl.package_clazz; p != null; p = p.package_clazz)
				getStructType(p, path);
		}

		if (tdecl.parent() instanceof FileUnit)
			tdecl.setStatic(true);

		if (tdecl instanceof Struct) {
			Struct clazz = (Struct)tdecl;
			if (clazz.isAnnotation()) {
				clazz.super_types.add(new TypeRef(Type.tpObject));
				clazz.super_types.add(new TypeRef(Type.tpAnnotation));
			}
			else if (tdecl.isEnum()) {
				clazz.setStatic(true);
				clazz.super_types.insert(0, new TypeRef(Type.tpEnum));
				// assign type of enum fields
				if (clazz.isEnum()) {
					foreach (Field f; clazz.members; f.isEnumField())
						f.ftype = new TypeRef(clazz.xtype);
				}
			}
			else if (clazz.isPizzaCase()) {
				clazz.setStatic(true);
				Struct p = clazz.ctx_tdecl;
				p.addCase(clazz);
				getStructType(p, path);
				TypeWithArgsRef sup_ref = new TypeWithArgsRef(null, new SymbolRef(clazz.pos,p));
			next_case_arg:
				for(int i=0; i < p.args.length; i++) {
					for(int j=0; j < clazz.args.length; j++) {
						if (p.args[i].id.uname == clazz.args[j].id.uname) {
							sup_ref.args.add(new TypeRef(clazz.args[j].getAType()));
							continue next_case_arg;
						}
					}
					sup_ref.args.add(new TypeRef(p.args[i].getAType()));
				}
				clazz.super_types.insert(0, sup_ref);
			}
			else if (clazz.isPackage()) {
				clazz.setAbstract(true);
				clazz.setMembersGenerated(true);
				clazz.setStatementsGenerated(true);
				clazz.super_types.delAll();
			}
			else if (clazz.isSyntax()) {
				clazz.setAbstract(true);
				clazz.setMembersGenerated(true);
				clazz.setStatementsGenerated(true);
				foreach(TypeRef tr; clazz.super_types) {
					Struct s = tr.getType().getStruct();
					if (s != null) {
						getStructType(s, path);
						if (!s.isSyntax())
							Kiev.reportError(clazz,"Syntax "+clazz+" extends non-syntax "+s);
					}
				}
			}
			else if( clazz.isInterface() ) {
				if (clazz.super_types.length == 0 || clazz.super_types[0].getType() ≉ Type.tpObject)
					clazz.super_types.insert(0, new TypeRef(Type.tpObject));
				foreach(TypeRef tr; clazz.super_types) {
					Struct s = tr.getType().getStruct();
					if (s != null)
						getStructType(s, path);
				}
			}
			else {
				if (clazz.view_of != null)
					clazz.view_of.getType();
				foreach(TypeRef tr; clazz.super_types) {
					Struct s = tr.getType().getStruct();
					if (s != null)
						getStructType(s, path);
				}
				if (clazz.super_types.length == 0) {
					if (clazz != Type.tpObject.clazz)
						clazz.super_types.insert(0, new TypeRef(Type.tpObject));
				}
				else if (clazz.super_types[0].getStruct().isInterface())
					clazz.super_types.insert(0, new TypeRef(Type.tpObject));
			}
		}
		else {
			foreach(TypeRef tr; tdecl.super_types) {
				Struct s = tr.getType().getStruct();
				if (s != null)
					getStructType(s, path);
			}
		}
		
		tdecl.type_decl_version++;
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
				if (!clazz.instanceOf(Type.tpTypeInfoInterface.clazz))
					clazz.super_types.append(new TypeRef(Type.tpTypeInfoInterface));
			}
		}

		tdecl.setArgsResolved(true);
		path.pop();
		
		return tdecl.xtype;
	}
}

////////////////////////////////////////////////////
//												   //
//	   PASS 3- struct members					   //
//												   //
////////////////////////////////////////////////////

@singleton
public final class KievFE_Pass3 extends TransfProcessor {
	private KievFE_Pass3() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Class members" }

	public void process(ASTNode:ASTNode astn) {
	}

	public void process(FileUnit:ASTNode astn) {
		foreach (ASTNode n; astn.members)
			process(n);
	}


	public void process(TypeDecl:ASTNode astn) {
		int pos = astn.pos;
		TypeDecl me = astn;
		int next_enum_val = 0;
		trace(Kiev.debugResolve,"Pass 3 for class "+me);
		if (me.isSyntax()) {
			return;
		}
		// Process members
		ASTNode[] members = astn.members;
		for(int i=0; i < members.length; i++) {
			if( members[i] instanceof Initializer ) {
				Initializer init = (Initializer)members[i];
				// TODO: check flags for initialzer
				if( me.isPackage() ) init.setStatic(true);
			}
			else if (members[i] instanceof CoreMethod) {
				CoreMethod cm = (CoreMethod)members[i];
				cm.pass3();
				cm.attachToCompiler();
				Access.verifyDecl(cm);
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
				if( me.isPackage() )
					m.setStatic(true);
				if( m.isPrivate() )
					m.setFinal(false);
				if( me.isClazz() && me.isFinal() ) {
					m.setFinal(true);
				}
				else if( me.isInterface() ) {
					m.setPublic();
					m.setFinal(false);
					m.setAbstract(true);
				}
				if( m.id.equals(nameInit) ) {
					m.setAbstract(false);
					m.setSynchronized(false);
					m.setFinal(false);
				}
				Access.verifyDecl(m);
			}
			else if (members[i] instanceof Field && ((Field)members[i]).isEnumField()) {
				Field f = (Field)members[i];
				String text = f.id.sname;
				MetaAlias al = f.getMetaAlias();
				if (al != null) {
					foreach (ConstStringExpr n; al.getAliases()) {
						String nm = n.value;
						if (nm.length() > 2 && nm.charAt(0) == '\"') {
							f.id.addAlias(nm.intern());
							text = nm.substring(1,nm.length()-1).toString();
							break;
						}
					}
				}
				f.init = new NewExpr(f.pos,me.xtype,new ENode[]{
							new ConstStringExpr(f.id.sname),
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
						f.delNodeData(MetaPacked.ATTR);
					}
					else if( !ftype.isIntegerInCode() ) {
						if( ftype.getStruct() != null && ftype.getStruct().isEnum() ) {
							Kiev.reportError(fdecl,"Packing of enum is not implemented yet");
						} else {
							Kiev.reportError(fdecl,"Packing of reference type is not allowed");
						}
						f.delNodeData(MetaPacked.ATTR);
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
							f.delNodeData(MetaPacked.ATTR);
						}
						else if( pack.getOffset() >= 0 && pack.getSize()+pack.getOffset() > 32) {
							Kiev.reportError(fdecl,"Size+offset "+(pack.getSize()+pack.getOffset())+" do not fit in 32 bit boundary");
							f.delNodeData(MetaPacked.ATTR);
						}
					}
				}
				if( f.getMetaPacked() != null )
					f.setPackedField(true);
			}
			else if( members[i] instanceof WBCCondition ) {
				WBCCondition inv = (WBCCondition)members[i];
				assert(inv.cond == WBCType.CondInvariant);
				// TODO: check flags for fields
				Method m = new Method(inv.id.uname,Type.tpVoid,inv.flags);
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
		if( !me.isPackage() ) {
			foreach (TypeDecl n; me.members)
				process(n);
		}
	}
}

////////////////////////////////////////////////////
//	   PASS Meta 1 - resolve meta decls           //
////////////////////////////////////////////////////

@singleton
public final class KievFE_MetaDecls extends TransfProcessor {
	private KievFE_MetaDecls() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Annotation's declaration" }

	public void process(ASTNode:ASTNode node) {
	}
	public void process(FileUnit:ASTNode fu) {
		foreach(Struct n; fu.members)
			process(n);
	}
	public void process(Struct:ASTNode clazz) {
		if (clazz.isAnnotation()) {
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
					if (m.type.ret() ≡ Type.tpVoid || m.type.ret() ≡ Type.tpRule)
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
		foreach (Struct sub; clazz.sub_decls)
			process(sub);
	}
}

////////////////////////////////////////////////////
//	   PASS Meta 2 - resolve meta defaults        //
////////////////////////////////////////////////////

@singleton
public final class KievFE_MetaDefaults extends TransfProcessor {
	private KievFE_MetaDefaults() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Annotation's defaults" }

	public void process(ASTNode:ASTNode node) {
	}
	public void process(FileUnit:ASTNode fu) {
		foreach(Struct n; fu.members)
			process(n);
	}
	public void process(Struct:ASTNode clazz) {
		clazz.resolveMetaDefaults();
	}
}

////////////////////////////////////////////////////
//	   PASS Meta 3 - resolve meta annotations     //
////////////////////////////////////////////////////

@singleton
public final class KievFE_MetaValues extends TransfProcessor {
	private KievFE_MetaValues() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Annotation values" }

	public void process(ASTNode:ASTNode node) {
	}
	public void process(FileUnit:ASTNode fu) {
		foreach(Struct n; fu.members)
			process(n);
	}
	public void process(Struct:ASTNode clazz) {
		clazz.resolveMetaValues();
	}
}

////////////////////////////////////////////////////
//	   PASS 5 - parse method bodies               //
////////////////////////////////////////////////////

@singleton
public final class KievFE_SrcParse extends TransfProcessor {
	private KievFE_SrcParse() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Full Parser" }

	public void process(FileUnit:ASTNode fu) {
		if (fu.scanned_for_interface_only)
			return;
		long curr_time = System.currentTimeMillis();
		try {
			Kiev.parseFile(fu);
			Kiev.curFile = "";
		} catch (Exception ioe) {
			Kiev.reportParserError(0,ioe);
		}
		long diff_time = System.currentTimeMillis() - curr_time;
		if( Kiev.verbose )
			Kiev.reportInfo("Parsed file    "+fu,diff_time);
	}
}

////////////////////////////////////////////////////
//	   PASS 4 - resolve meta and generate members //
////////////////////////////////////////////////////

@singleton
public final class KievFE_GenMembers extends TransfProcessor {
	private KievFE_GenMembers() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Members generation" }

	public void process(ASTNode:ASTNode node) {
	}
	public void process(FileUnit:ASTNode fu) {
		foreach(Struct n; fu.members)
			process(n);
	}
	public void process(Struct:ASTNode clazz) {
		clazz.autoGenerateMembers();
	}
}

////////////////////////////////////////////////////
//	   PASS 5 - pre-resolve                       //
////////////////////////////////////////////////////

@singleton
public final class KievFE_PreResolve extends TransfProcessor {
	private KievFE_PreResolve() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Kiev pre-resolve" }

	public void process(ASTNode node) {
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
	private KievFE_MainResolve() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Kiev main-resolve" }

	public void process(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return n.mainResolveIn(); return false; }
			public void post_exec(ANode n) { if (n instanceof ASTNode) n.mainResolveOut(); }
		});
		return;
	}
}

////////////////////////////////////////////////////
//	   PASS 5 - verify                            //
////////////////////////////////////////////////////

@singleton
public final class KievFE_Verify extends TransfProcessor {
	private KievFE_Verify() { super(Kiev.Ext.JavaOnly); }
	public String getDescr() { "Kiev verify" }

	public void process(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return n.preVerify(); return false; }
			//public void post_exec(ANode n) { if (n instanceof ASTNode) n.postVerify(); }
		});
		return;
	}
}


////////////////////////////////////////////////////
//	   PASS - backend pre-generation              //
////////////////////////////////////////////////////

@singleton
public final class KievME_PreGenartion extends BackendProcessor {
	private KievME_PreGenartion() { super(Kiev.Backend.Java15); }
	public String getDescr() { "Kiev pre-generation" }

	public void process(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return n.preGenerate(); return false; }
		});
	}
}


////////////////////////////////////////////////////
//	   PASS - backend resolve                     //
////////////////////////////////////////////////////

@singleton
public final class KievBE_Resolve extends BackendProcessor {
	private KievBE_Resolve() { super(Kiev.Backend.Java15); }
	public String getDescr() { "Kiev resolve" }

	public void process(ASTNode node) {
		if (node instanceof ENode)
			node.resolve(null);
		else if (node instanceof DNode)
			node.resolveDecl();
	}
}

@singleton
public final class KievBE_Lock extends BackendProcessor {
	private KievBE_Lock() { super(Kiev.Backend.Generic); }
	public String getDescr() { "Lock nodes" }

	public void process(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { n.locked = true; return true; }
		});
	}
}


////////////////////////////////////////////////////
//	   PASS - backend generate                    //
////////////////////////////////////////////////////

@singleton
public final class KievBE_Generate extends BackendProcessor {
	private KievBE_Generate() { super(Kiev.Backend.Java15); }
	public String getDescr() { "Class generation" }

	public void process(ASTNode node) {
		if (node instanceof FileUnit) {
			Transaction tr = Transaction.open();
			try {
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
						((JFileUnit)(FileUnit)node).generate();
					} catch (Exception rte) { Kiev.reportError(rte); }
				}
			} finally {
				tr.close();
			}
		}
	}
}

////////////////////////////////////////////////////
//	   PASS - backend generate                    //
////////////////////////////////////////////////////

@singleton
public final class ExportBE_Generate extends BackendProcessor {
	private ExportBE_Generate() { super(Kiev.Backend.VSrc); }
	public String getDescr() { "Source generation" }

	public void process(ASTNode node) {
		StringBuffer sb = new StringBuffer(1024);
		TextFormatter f = new TextFormatter(new JavaSyntax());
		try {
			Drawable dr = f.format(node);
			TextPrinter pr = new TextPrinter(sb);
			pr.draw(dr);
		} finally {
			cleanFormatting(node, f.getAttr());
		}
		if (node instanceof FileUnit) {
			try {
				dumpSrc((FileUnit)node, sb.toString());
			} catch (Exception rte) { Kiev.reportError(rte); }
		} else {
			System.out.println(sb.toString());
		}
	}

	private void cleanFormatting(ASTNode node, AttrSlot attr) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) {
				if (n instanceof ASTNode)
					n.delNodeData(attr);
				return true;
			}
		});
	}
	
	public void dumpSrc(FileUnit fu, String text) {
		String output_dir = Kiev.output_dir;
		if( output_dir==null ) output_dir = "classes";
		if( Kiev.verbose ) System.out.println("Dumping to source file "+fu+" into '"+output_dir+"' dir");

		try {
			File f;
			String out_file = fu.id.toString();
			make_output_dir(output_dir,out_file);
			f = new File(output_dir,out_file);
			FileOutputStream out;
			try {
				out = new FileOutputStream(f);
			} catch( java.io.FileNotFoundException e ) {
				System.gc();
				System.runFinalization();
				System.gc();
				System.runFinalization();
				out = new FileOutputStream(f);
			}
			//out.write("\uFEFF".getBytes("UTF-8"));
			out.write(text.getBytes("UTF-8"));
			out.close();
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-Src exporting: "+e);
		}
	}

	private static void make_output_dir(String top_dir, String filename) throws IOException {
		File dir;
		dir = new File(top_dir,filename);
		dir = new File(dir.getParent());
		dir.mkdirs();
		if( !dir.exists() || !dir.isDirectory() ) throw new RuntimeException("Can't create output dir "+dir);
	}
}

