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

import java.io.*;

import kiev.be.java15.JFileUnit;
import kiev.fmt.XmlDumpSyntax;
import kiev.fmt.ATextSyntax;
import kiev.fmt.TextFormatter;
import kiev.fmt.TextPrinter;
import kiev.fmt.Drawable;

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
	
	public void processSyntax(TypeDecl:ASTNode node) {
		node.updatePackageClazz();
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
		Struct scope = Env.getRoot();
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
			if!(scope.resolveNameR(node,new ResInfo(astn,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports))) {
				Kiev.reportError(astn,"Unresolved identifier "+head+" in "+scope);
				return;
			}
			n = node;
			if (n instanceof Struct)
				scope = (Struct)n;
		} while (dot > 0);
		if		(astn.mode == Import.ImportMode.IMPORT_CLASS && !(n instanceof Struct))
			Kiev.reportError(astn,"Identifier "+name+" is not a class or package");
		else if (astn.mode == Import.ImportMode.IMPORT_PACKAGE)
			Kiev.reportError(astn,"Import of packages is not supported");
		else if (astn.mode == Import.ImportMode.IMPORT_STATIC && !(astn.star || (n instanceof Field)))
			Kiev.reportError(astn,"Identifier "+name+" is not a field");
		else if (astn.mode == Import.ImportMode.IMPORT_SYNTAX && !(n instanceof Struct && ((Struct)n).isSyntax()))
			Kiev.reportError(astn,"Identifier "+name+" is not a syntax");
		else {
			assert (n != null);
			astn.name.open().symbol = n;
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
				astn = astn.open();
				astn.resolved = op;
				return;
			}
			op = Operator.newOperator(prior, decl);
			if( Kiev.verbose ) System.out.println("Declared operator "+op+" with priority "+op.priority);
			astn = astn.open();
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
			astn = astn.open();
			astn.resolved = op;
			astn.decl = op.decl;
		}
		return;
	}

	public void processSyntax(Struct:ASTNode astn) {
		Struct me = astn;
		me.updatePackageClazz();
		if (me.isAnnotation() || me.isEnum() || me.isSyntax()) {
			if( me.args.length > 0 ) {
				Kiev.reportError(me,"Type parameters are not allowed for "+me);
				me.args.delAll();
			}
			me.setTypeUnerasable(false);
		}
		else if!(me.parent() instanceof NameSpace) {
			if (me.isStructInner() && !me.isStatic() && me.isClazz()) {
				TypeDecl pkg = me.package_clazz.dnode;
				if (pkg.sname == nameIFaceImpl)
					pkg = pkg.package_clazz.dnode;
				int n = 0;
				for(TypeDecl p=pkg; p.isStructInner() && !p.isStatic(); p=p.package_clazz.dnode) n++;
				String fldName = (nameThisDollar+n).intern();
				boolean found = false;
				foreach (Field f; me.getAllFields(); f.sname == fldName)
					found = true;
				if (!found) {
					TypeAssign td = new TypeAssign(	"outer$"+n+"$type", new TypeRef(pkg.xtype));
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
			TypeDecl td = (TypeDecl)astn;
			td.updatePackageClazz();
			// Verify meta-data to the new structure
			if (td.meta != null)
				td.meta.verify();
			foreach (DNode dn; td.members)
				dn.meta.verify();
			foreach (DeclGroup dn; td.members) {
				dn.meta.verify();
				foreach (DNode d; dn.decls)
					d.meta.verify();
			}
			td = ANode.getVersion(td);
			getStructType(td, new Stack<TypeDecl>());
			foreach (TypeDecl s; td.members)
				doProcess(s);
		} catch(Exception e ) { Kiev.reportError(astn,e); }
	}
	
	private Type getStructType(TypeDecl tdecl, Stack<TypeDecl> path) {
		if (tdecl.isTypeResolved()) {
			//if (!tdecl.isArgsResolved())
			//	throw new CompilerException(tdecl, "Recursive type declaration for class "+tdecl+" via "+path);
			return tdecl.xtype;
		}
		path.push(tdecl);
		
		tdecl = tdecl.open();
		tdecl.setTypeResolved(true);
		
		for (TypeDecl p = tdecl.package_clazz.dnode; p != null; p = p.package_clazz.dnode)
			getStructType(ANode.getVersion(p), path);

		if (tdecl instanceof Struct) {
			Struct clazz = (Struct)tdecl;
			if (clazz.isAnnotation()) {
				clazz.super_types.add(new TypeRef(Type.tpObject));
				clazz.super_types.add(new TypeRef(Type.tpAnnotation));
			}
			else if (tdecl.isEnum()) {
				if (tdecl.isStructInner())
					clazz.setStatic(true);
				clazz.super_types.insert(0, new TypeRef(Type.tpEnum));
			}
			else if (clazz instanceof PizzaCase) {
				clazz.setStatic(true);
				Struct p = clazz.ctx_tdecl;
				p.addCase((PizzaCase)clazz);
				getStructType(p, path);
				TypeNameRef sup_ref = new TypeNameRef(p.qname());
				sup_ref.symbol = p;
			next_case_arg:
				for(int i=0; i < p.args.length; i++) {
					for(int j=0; j < clazz.args.length; j++) {
						if (p.args[i].sname == clazz.args[j].sname) {
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
				clazz.super_types.delAll();
			}
			else if (clazz.isSyntax()) {
				clazz.setAbstract(true);
				clazz.setMembersGenerated(true);
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
			else if( clazz.isStructView() || clazz.isClazz() || clazz instanceof MetaTypeDecl) {
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
					if (clazz != Type.tpObject.tdecl)
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
				if (!clazz.instanceOf(Type.tpTypeInfoInterface.tdecl))
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

	public void doProcess(TypeDecl:ASTNode astn) {
		int pos = astn.pos;
		TypeDecl me = astn;
		trace(Kiev.debug && Kiev.debugResolve,"Pass 3 for class "+me);
		if (me.isSyntax()) {
			return;
		}
		// Process members
		for(int i=0; i < me.members.length; i++) {
			if( me.members[i] instanceof Initializer ) {
				Initializer init = (Initializer)me.members[i];
				// TODO: check flags for initialzer
				if( me.isPackage() ) init.setStatic(true);
			}
			else if( me.members[i] instanceof RuleMethod ) {
				RuleMethod m = (RuleMethod)me.members[i];
				m.pass3();
				if( me.isPackage() ) m.setStatic(true);
				if( m.isPrivate() ) m.setFinal(true);
				if( me.isClazz() && me.isFinal() ) m.setFinal(true);
				else if( me.isInterface() ) 	m.setPublic();
				MetaAccess.verifyDecl(m);
			}
			else if( me.members[i] instanceof Method ) {
				Method m = (Method)me.members[i];
				m.pass3();
				if( me.isPackage() )
					m.setStatic(true);
				if( m.isPrivate() )
					m.setFinal(false);
				if( me.isClazz() && me.isFinal() ) {
					m.setFinal(true);
				}
				else if( me.isInterface() && !me.isStructView() ) {
					m.setPublic();
					m.setFinal(false);
					m.setAbstract(true);
				}
				if( m instanceof Constructor ) {
					m.setAbstract(false);
					m.setSynchronized(false);
					m.setFinal(false);
				}
				MetaAccess.verifyDecl(m);
			}
			else if (me.members[i] instanceof DeclGroup) {
				DeclGroup dg = (DeclGroup)me.members[i];
				dg.meta.verify();
				if( me.isStructView() && !dg.meta.is_static) {
					//dg.setFinal(true);
					dg.setAbstract(true);
					dg.setVirtual(true);
				}
				if( me.isInterface() ) {
					if (dg.meta.is_virtual) {
						dg.setAbstract(true);
					} else {
						dg.setStatic(true);
						dg.setFinal(true);
					}
					dg.setPublic();
				}
				foreach (Field f; dg.decls)
					f.meta.verify();
			}
			else if( me.members[i] instanceof Field ) {
				Field fdecl = (Field)me.members[i];
				Field f = fdecl;
				f.meta.verify();
				// TODO: check flags for fields
				if( me.isPackage() )
					f.setStatic(true);
				if( me.isStructView() && !f.isStatic()) {
					//f.setFinal(true);
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
				MetaAccess.verifyDecl(f); // recheck access
				Type ftype = fdecl.type;
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
				Method m = new MethodImpl(inv.sname,Type.tpVoid,inv.meta.mflags);
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
				doProcess(n);
		}
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
	public void doProcess(FileUnit:ASTNode fu) {
		foreach(ASTNode n; fu.members)
			doProcess(n);
	}
	public void doProcess(NameSpace:ASTNode fu) {
		foreach(ASTNode n; fu.members)
			doProcess(n);
	}
	public void doProcess(Struct:ASTNode clazz) {
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
			doProcess(sub);
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
	public void doProcess(FileUnit:ASTNode fu) {
		foreach(ASTNode n; fu.members)
			doProcess(n);
	}
	public void doProcess(NameSpace:ASTNode fu) {
		foreach(ASTNode n; fu.members)
			doProcess(n);
	}
	public void doProcess(TypeDecl:ASTNode clazz) {
		clazz.resolveMetaDefaults();
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
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
	}
	public void doProcess(FileUnit:ASTNode fu) {
		foreach(ASTNode n; fu.members)
			doProcess(n);
	}
	public void doProcess(NameSpace:ASTNode fu) {
		foreach(ASTNode n; fu.members)
			doProcess(n);
	}
	public void doProcess(TypeDecl:ASTNode clazz) {
		clazz.resolveMetaValues();
	}
}

////////////////////////////////////////////////////
//	   PASS 5 - parse method bodies               //
////////////////////////////////////////////////////

@singleton
public final class KievFE_SrcParse extends TransfProcessor {
	private KievFE_SrcParse() { super(KievExt.JavaOnly); }
	public String getDescr() { "Full Parser" }

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof FileUnit) {
			doProcess((FileUnit)node);
		}
	}
	
	public void doProcess(FileUnit fu) {
		if (fu.scanned_for_interface_only || fu.bodies.length == 0)
			return;
		long curr_time = System.currentTimeMillis();
		try {
			Kiev.parseFile(fu);
			Kiev.setCurFile("");
		} catch (Exception ioe) {
			Kiev.reportParserError(0,ioe);
		}
		long diff_time = System.currentTimeMillis() - curr_time;
		if( Kiev.verbose )
			Kiev.reportInfo("Parsed file    "+fu,diff_time);
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
//	   PASS 5 - verify                            //
////////////////////////////////////////////////////

@singleton
public final class KievFE_Verify extends TransfProcessor {
	private KievFE_Verify() { super(KievExt.JavaOnly); }
	public String getDescr() { "Kiev verify" }

	public void process(ASTNode node, Transaction tr) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return n.preVerify(); return false; }
			public void post_exec(ANode n) { if (n instanceof ASTNode) n.postVerify(); }
		});
		return;
	}
}

/*
@singleton
public final class KievBE_Lock extends BackendProcessor {
	private KievBE_Lock() { super(KievBackend.Generic); }
	public String getDescr() { "Lock nodes" }

	public void process(ASTNode node, Transaction tr) {
		if (Transaction.get() != null)
			return;
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { n.locked = true; return true; }
		});
	}
}


@singleton
public final class KievBE_CheckLock extends BackendProcessor {
	private KievBE_CheckLock() { super(KievBackend.Generic); }
	public String getDescr() { "Check nodes' locks" }

	public void process(ASTNode node, Transaction tr) {
		if (Transaction.get() != null)
			return;
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (!n.locked && n instanceof ASTNode) reportError(n); return true; }
		});
	}
	
	final static void reportError(ANode n) {
		System.out.println("Unlocked node "+n);
		assert(false);
	}
}
*/

////////////////////////////////////////////////////
//	   PASS - midend dump API as XML files        //
////////////////////////////////////////////////////

@singleton
public final class KievME_DumpAPI extends BackendProcessor {

	private ATextSyntax stx = new XmlDumpSyntax("api");

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
		if (this.stx == null)
			this.stx = new XmlDumpSyntax("api");
		try {
			dumpSrc(fu);
		} catch (Exception rte) { Kiev.reportError(rte); }
	}

	public void dumpSrc(FileUnit fu) {
		if( Kiev.verbose ) System.out.println("Dumping API of source file "+fu);

		foreach (ASTNode n; fu.members) {
			if (n instanceof TypeDecl)
				dumpSrc((TypeDecl)n);
			else if (n instanceof NameSpace)
				dumpSrc((NameSpace)n);
		}
	}
	public void dumpSrc(NameSpace ns) {
		foreach (ASTNode n; ns.members) {
			if (n instanceof TypeDecl)
				dumpSrc((TypeDecl)n);
			else if (n instanceof NameSpace)
				dumpSrc((NameSpace)n);
		}
	}
	public void dumpSrc(TypeDecl td) {
		if (td.isPrivate() || (td instanceof TypeDef))
			return;
		String output_dir = Kiev.output_dir;
		if( output_dir==null ) output_dir = "classes";
		try {
			String out_file = td.qname().replace('\u001f',File.separatorChar)+".xml";
			File f = new File(output_dir,out_file);
			Env.dumpTextFile(td, f, stx);
		} catch (IOException e) {
			System.out.println("Create/write error while API dump: "+e);
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
		tr = Transaction.enter(tr);
		try {
			node.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return n.preGenerate(); return false; }
			});
		} finally { tr.leave(); }
	}
}


////////////////////////////////////////////////////
//	   PASS - bytecode names generation           //
////////////////////////////////////////////////////

@singleton
public final class KievME_GenBytecodeNames extends BackendProcessor {
	private KievME_GenBytecodeNames() { super(KievBackend.Java15); }
	public String getDescr() { "Kiev bytecode name generation" }

	public void process(ASTNode node, Transaction tr) {
		if!(node instanceof FileUnit)
			return;
		FileUnit fu = (FileUnit)node;
		try {
			doProcess(fu);
		} catch (Exception rte) { Kiev.reportError(rte); }
	}

	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode s) {
		makeBytecodeName(s);
		foreach (ASTNode dn; s.sub_decls)
			this.doProcess(dn);
	}
	
	private KString makeBytecodeName(TypeDecl s) {
		if (s.bytecode_name != null)
			return s.bytecode_name;
		TypeDecl pkg = s.package_clazz.dnode;
		KString pkg_bc_name = KString.Empty;
		if!(pkg instanceof Env)
			pkg_bc_name = makeBytecodeName(pkg);
		String name = s.sname;
		String bc_name = null;
		if (s instanceof JavaAnonymouseClass) {
			name = makeInnerIndex(pkg);
		}
		else if (!(s.parent() instanceof TypeDecl) && !(s.parent() instanceof NameSpace)) {
			name = makeInnerIndex(pkg) + '$' + name;
		}
		
		if (pkg instanceof Env) {
			bc_name = name;
		}
		else if (pkg instanceof KievPackage) {
			bc_name = pkg_bc_name + "/" + name;
		}
		else {
			bc_name = pkg_bc_name + "$" + name;
		}
		s = s.open();
		s.bytecode_name = KString.from(bc_name);
		return s.bytecode_name;
	}
	
	private String makeInnerIndex(TypeDecl pkg) {
		while !(pkg.package_clazz.dnode instanceof KievPackage)
			pkg = pkg.package_clazz.dnode;
		Integer i = pkg.inner_counter;
		if (i == null)
			i = new Integer(0);
		else
			i = new Integer(i.intValue() + 1);
		pkg.inner_counter = i;
		return String.valueOf(i.intValue());
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
		tr = Transaction.enter(tr);
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
			tr = Transaction.enter(tr);
			try {
				try {
					((JFileUnit)fu).generate();
				} catch (Exception rte) { Kiev.reportError(rte); }
			} finally { tr.leave(); }
		}
	}
}

////////////////////////////////////////////////////
//	   PASS - backend generate                    //
////////////////////////////////////////////////////

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
				stx = (ATextSyntax)Env.resolveGlobalDNode("stx-fmt\u001fsyntax-for-java");
			Env.dumpTextFile(fu, f, stx);
		} catch (IOException e) {
			System.out.println("Create/write error while Kiev-to-Src exporting: "+e);
		}
	}
}

////////////////////////////////////////////////////
//	   PASS - cleanup after backend               //
////////////////////////////////////////////////////

@singleton
public final class KievBE_Cleanup extends BackendProcessor {
	private KievBE_Cleanup() { super(KievBackend.Generic); }
	public String getDescr() { "Kiev cleanup after backend" }

	public void process(ASTNode node, Transaction tr) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { return n.backendCleanup(); }
			public void post_exec(ANode n) {}
		});
		return;
	}
}

