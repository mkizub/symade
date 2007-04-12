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
package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.types.*;
import java.io.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.be.java15.JTypeDecl;
import kiev.be.java15.JStruct;
import kiev.ir.java15.RStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */


@node
public class TypeDeclVariant extends ASTNode {
}

@node
public final class JavaPackage extends TypeDeclVariant {
}

@node
public final class KievSyntax extends TypeDeclVariant {
}

@node
public final class JavaClass extends TypeDeclVariant {
}

@node
public final class JavaAnonymouseClass extends TypeDeclVariant {
}

@node
public final class JavaInterface extends TypeDeclVariant {
}

@node
public final class KievView extends TypeDeclVariant {
	@att public TypeRef						view_of;
}

@node
public final class JavaAnnotation extends TypeDeclVariant {
}

@node
public final class PizzaCase extends TypeDeclVariant {
	public int tag;
	@ref public DeclGroupCaseFields		group;

	public Field[] getCaseFields() {
		DeclGroupCaseFields cases = this.group;
		Field[] cflds = new Field[cases.decls.length];
		for (int i=0; i < cflds.length; i++)
			cflds[i] = (Field)cases.decls[i];
		return cflds;
	}
}

@node
public final class JavaEnum extends TypeDeclVariant {
	@ref public DeclGroupEnumFields		group;
	public Field[] getEnumFields() {
		DeclGroupEnumFields enums = this.group;
		Field[] eflds = new Field[enums.decls.length];
		for (int i=0; i < eflds.length; i++)
			eflds[i] = (Field)enums.decls[i];
		return eflds;
	}

	public int getIndexOfEnumField(Field f) {
		DeclGroupEnumFields enums = this.group;
		for (int i=0; i < enums.decls.length; i++) {
			if (f == enums.decls[i])
				return i;
		}
		throw new RuntimeException("Enum value for field "+f+" not found in "+this);
	}

}

@node
public class Struct extends TypeDecl {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="false")	DNode[]		members;
	}

	@virtual typedef This  = Struct;
	@virtual typedef JView = JStruct;
	@virtual typedef RView = RStruct;

	@att public TypeDeclVariant				variant;
	@abstract
	@att public String						uniq_name;
	@ref public DNode[]						sub_decls;

	@ref(ext_data=true) public Struct				typeinfo_clazz;
	@ref(ext_data=true) public Struct				iface_impl;
	@ref(ext_data=true) public WrapperMetaType		wmeta_type;
	@ref(ext_data=true) public ASTNodeMetaType		ameta_type;
	@ref(ext_data=true) public TypeAssign			ometa_tdef;

	@att
	@getter public final String get$uniq_name() { return u_name; }
	@setter public final void set$uniq_name(String val) { this.u_name = val; }

	public void callbackChildChanged(AttrSlot attr) {
		if (attr.name == "package_clazz")
			this.callbackSuperTypeChanged(this);
		else if (attr.name == "sname")
			resetNames();
		else
			super.callbackChildChanged(attr);
	}
	
	private void resetNames() {
		if (u_name != null) { // initialized!
			q_name = null;
			b_name = null;
			foreach (Struct s; sub_decls)
				s.resetNames();
		}
	}
	
	public final void setPackage() {
		assert (variant == null || variant instanceof JavaPackage);
		if (this.meta.is_access != MASK_ACC_NAMESPACE) {
			assert(!locked);
			this.meta.is_access = MASK_ACC_NAMESPACE;
		}
		if !(variant instanceof JavaPackage) variant = new JavaPackage();
	}
	public final void setSyntax() {
		assert (variant == null || variant instanceof KievSyntax);
		if (this.meta.is_access != MASK_ACC_SYNTAX) {
			assert(!locked);
			this.meta.is_access = MASK_ACC_SYNTAX;
		}
		if !(variant instanceof KievSyntax) variant = new KievSyntax();
	}
	public void setInterface() {
		assert (variant == null || variant instanceof JavaInterface || variant instanceof KievView);
		if (!this.meta.is_struct_interface) {
			assert(!locked);
			this.meta.is_struct_interface = true;
		}
		if !(variant instanceof JavaInterface) {
			if (variant instanceof KievView) {
				; // temporary do nothing
			} else {
				variant = new JavaInterface();
			}
		}
	}
	public void setStructView() {
		assert (variant == null || variant instanceof KievView);
		if (!this.meta.is_virtual) {
			assert(!locked);
			this.meta.is_virtual = true;
		}
		if !(variant instanceof KievView) variant = new KievView();
	}
	public final void setPizzaCase() {
		assert (variant == null || variant instanceof PizzaCase);
		if (!this.is_struct_pizza_case) {
			assert(!locked);
			this.is_struct_pizza_case = true;
		}
		if !(variant instanceof PizzaCase) variant = new PizzaCase();
	}
	public final void setAnnotation() {
		assert (variant == null || variant instanceof JavaAnnotation);
		if (!this.meta.is_struct_annotation) {
			this.meta.is_struct_annotation = true;
			this.meta.is_struct_interface = true;
		}
		if !(variant instanceof JavaAnnotation) variant = new JavaAnnotation();
	}
	public final void setEnum() {
		assert (variant == null || variant instanceof JavaEnum);
		if (!this.meta.is_enum) {
			assert(!locked);
			this.meta.is_enum = true;
		}
		if !(variant instanceof JavaEnum) variant = new JavaEnum();
	}

	public boolean isClazz() {
		return !isPackage() && !isInterface() && !isSyntax();
	}
	
	// a pizza case	
	public final boolean isPizzaCase() {
		return this.is_struct_pizza_case;
	}
	// has pizza cases
	public final boolean isHasCases() {
		return this.is_struct_has_pizza_cases;
	}
	public final void setHasCases(boolean on) {
		if (this.is_struct_has_pizza_cases != on) {
			assert(!locked);
			this.is_struct_has_pizza_cases = on;
		}
	}
	// indicates that structure members were generated
	public final boolean isMembersGenerated() {
		return this.is_struct_fe_passed || this.is_struct_members_generated;
	}
	public final void setMembersGenerated(boolean on) {
		assert (!this.is_struct_fe_passed);
		if (this.is_struct_members_generated != on) {
			this.is_struct_members_generated = on;
		}
	}
	// indicates that structure members were pre-generated
	public final boolean isMembersPreGenerated() {
		return this.is_struct_pre_generated;
	}
	public final void setMembersPreGenerated(boolean on) {
		if (this.is_struct_pre_generated != on) {
			this.is_struct_pre_generated = on;
		}
	}
	// a pizza case	
	public final boolean isCompilerNode() {
		return this.is_struct_compiler_node;
	}
	public final void setCompilerNode(boolean on) {
		if (this.is_struct_compiler_node != on) {
			assert(!locked);
			this.is_struct_compiler_node = on;
		}
	}
	
	/** Add information about new sub structure, this class (package) containes */
	public Struct addSubStruct(Struct sub) {
		// Check we already have this sub-class
		for(int i=0; i < sub_decls.length; i++) {
			if( sub_decls[i].equals(sub) ) {
				// just ok
				return sub;
			}
		}
		// Check package class is null or equals to this
		if( sub.package_clazz == null ) sub.package_clazz = this;
		else if( sub.package_clazz != this ) {
			throw new RuntimeException("Sub-structure "+sub+" already has package class "
				+sub.package_clazz+" that differs from "+this);
		}

		sub_decls.append(sub);

		trace(Kiev.debug && Kiev.debugMembers,"Sub-class "+sub+" added to class "+this);
		if (sub.sname == nameClTypeInfo) {
			typeinfo_clazz = sub;
			trace(Kiev.debug && Kiev.debugMembers,"Sub-class "+sub+" is the typeinfo class of "+this);
		}
		return sub;
	}

	/** Add information about new method that belongs to this class */
	public Method addMethod(Method m) {
		// Check we already have this method
		members.append(m);
		trace(Kiev.debug && Kiev.debugMembers,"Method "+m+" added to class "+this);
		if (m instanceof Constructor) {
			foreach (Constructor mm; members; mm != m) {
				if (mm.type.equals(m.type))
					Kiev.reportError(m,"Constructor "+m+" already exists in class "+this);
			}
		} else {
			foreach (Method mm; members; mm != m) {
				if (mm.u_name == m.u_name && mm.type.equals(m.type))
					Kiev.reportError(m,"Method "+m+" already exists in class "+this);
			}
		}
		return m;
	}

	/** Add information about new field that belongs to this class */
	public Field addField(Field f) {
		// Check we already have this field
		foreach (Field ff; getAllFields()) {
			if( ff.equals(f) ) {
				throw new RuntimeException("Field "+f+" already exists in class "+this);
			}
		}
		members.append(f);
		trace(Kiev.debug && Kiev.debugMembers,"Field "+f+" added to class "+this);
		return f;
	}

	/** Add information about new pizza case of this class */
	public Struct addCase(Struct cas) {
		setHasCases(true);
		int caseno = 0;
		foreach (Struct s; members; s.isPizzaCase()) {
			PizzaCase pcase = (PizzaCase)s.variant;
			if (pcase.tag > caseno)
				caseno = pcase.tag;
		}
		((PizzaCase)cas.variant).tag = caseno + 1;
		trace(Kiev.debug && Kiev.debugMembers,"Class's case "+cas+" added to class "	+this+" as case # "+(caseno+1));
		return cas;
	}
		
	public Constructor getClazzInitMethod() {
		foreach(Constructor n; members; n.u_name == nameClassInit)
			return n;
		Constructor class_init = new Constructor(ACC_STATIC);
		class_init.pos = pos;
		class_init.setAutoGenerated(true);
		addMethod(class_init);
		class_init.body = new Block(pos);
		return class_init;
	}

	public final String qname() {
		if (q_name != null)
			return q_name;
		Struct pkg = package_clazz;
		if (pkg == null || pkg == Env.root)
			q_name = u_name;
		else
			q_name = (pkg.qname()+"."+u_name).intern();
		return q_name;
	}

	public Struct() {
		super("");
		this.u_name = "";
		this.q_name = "";
		this.b_name = KString.Empty;
		if !(this instanceof Env) {
			this.xmeta_type = new CompaundMetaType(this);
			this.xtype = new CompaundType((CompaundMetaType)this.xmeta_type, TVarBld.emptySet);
		}
	}
	
	public Struct(String name, String u_name, Struct outer, int flags, TypeDeclVariant variant) {
		super(name);
		this.u_name = u_name;
		this.xmeta_type = new CompaundMetaType(this);
		this.xtype = new CompaundType((CompaundMetaType)this.xmeta_type, TVarBld.emptySet);
		this.package_clazz = outer;
		if (flags != 0) {
			if ((flags & ACC_PUBLIC) == ACC_PUBLIC) setMeta(new MetaAccess("public"));
			if ((flags & ACC_PROTECTED) == ACC_PROTECTED) setMeta(new MetaAccess("protected"));
			if ((flags & ACC_PRIVATE) == ACC_PRIVATE) setMeta(new MetaAccess("private"));
			if ((flags & ACC_STATIC) == ACC_STATIC) setMeta(new MetaStatic());
			if ((flags & ACC_FINAL) == ACC_FINAL) setMeta(new MetaFinal());
			if ((flags & ACC_ABSTRACT) == ACC_ABSTRACT) setMeta(new MetaAbstract());
			if ((flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) setMeta(new MetaSynthetic());
			if ((flags & ACC_MACRO) == ACC_MACRO) setMeta(new MetaMacro());
			if ((flags & ACC_TYPE_UNERASABLE) == ACC_TYPE_UNERASABLE) setMeta(new MetaUnerasable());
			if ((flags & ACC_SINGLETON) == ACC_SINGLETON) setMeta(new MetaSingleton());
			this.meta.mflags = flags;
		}
		this.variant = variant;
		trace(Kiev.debug && Kiev.debugCreation,"New clazz created: "+qname() +" as "+u_name+", member of "+outer);
	}

	public Struct getStruct() { return this; }

	public int countAnonymouseInnerStructs() {
		int i=0;
		foreach(Struct s; sub_decls; s.isAnonymouse() || s.isLocal()) i++;
		return i;
	}

	public boolean preResolveIn() {
		if (this.isLoadedFromBytecode())
			return false;
		if (parent() instanceof Struct || parent() instanceof FileUnit)
			return true;
		if (ctx_method==null || ctx_method.isStatic())
			this.setStatic(true);
		this.setTypeDeclLoaded(true);
		this.setLocal(true);
		this.setLoadedFromBytecode(true);
		try {
			Kiev.runProcessorsOn(this);
		} finally { this.setLoadedFromBytecode(false); }
		return true;
	}

	public void mainResolveOut() {
		((Struct)this).cleanDFlow();
	}

	// verify resolved tree
	public boolean preVerify() {
		setFrontEndPassed();
		foreach (TypeRef i; super_types) {
			if (i.getStruct().isFinal())
				Kiev.reportError(this, "Struct "+this+" extends final struct "+i);
		}
		if (isInterface() && !isStructView()) {
			foreach (ASTNode n; members; n instanceof Field || n instanceof Initializer || n instanceof DeclGroup) {
				if (n instanceof DeclGroup) {
					foreach (Field f; n.decls; !f.isAbstract())
						verifyFieldInIface(f);
				}
				else if (n instanceof Field && !n.isAbstract()) {
					verifyFieldInIface((Field)n);
				}
				else if (n instanceof Initializer) {
					verifyInitializerInIface((Initializer)n);
				}
			}
		}
		return true;
	}
	
	private void verifyFieldInIface(Field n) {
		if (!n.isStatic()) {
			Kiev.reportError(n,"Non-static field "+n+" in interface "+this);
			n = n.open();
			n.setStatic(true);
		}
		if (!n.isFinal()) {
			Kiev.reportError(n,"Non-final field "+n+" in interface "+this);
			n = n.open();
			n.setFinal(true);
		}
	}
	private void verifyInitializerInIface(Initializer n) {
		if (!n.isStatic()) {
			Kiev.reportError(n,"Non-static initializer in interface "+this);
			n = n.open();
			n.setStatic(true);
		}
	}

	public final rule resolveNameR(ASTNode@ node, ResInfo info)
	{
		info.isStaticAllowed(),
		{
			super.resolveNameR(node, info), $cut
		;
			isPackage(),
			node @= sub_decls,
			info.checkNodeName(node)
		;
			isPackage(),
			info.isCmpByEquals(),
			tryLoad(node,info.getName()), $cut
		}
	}

	public boolean tryLoad(ASTNode@ node, String name) {
		if( isPackage() ) {
			trace(Kiev.debug && Kiev.debugResolve,"Struct: trying to load in package "+this);
			TypeDecl cl;
			String qn = name;
			if (this.equals(Env.root))
				cl = Env.loadTypeDecl(qn);
			else
				cl = Env.loadTypeDecl(qn=(this.qname()+"."+name).intern());
			if( cl != null ) {
				trace(Kiev.debug && Kiev.debugResolve,"TypeDecl "+cl+" found in "+this);
				node = cl;
				return true;
			} else {
				trace(Kiev.debug && Kiev.debugResolve,"TypeDecl "+qn+" not found in "+this);
			}
		}
		return false;
	}

	public void autoGenerateMembers() {
		checkResolved();
		if( isMembersGenerated() ) return;
		if( isPackage() ) return;

		foreach (TypeRef tr; super_types; !tr.getStruct().isMembersGenerated())
			tr.getStruct().autoGenerateMembers();

		if( Kiev.debug ) System.out.println("AutoGenerating members for "+this);

		String oldfn = Kiev.getCurFile();
		boolean[] old_exts = Kiev.getExtSet();
		{
			ANode fu = parent();
			while( fu != null && !(fu instanceof FileUnit))
				fu = fu.parent();
			if( fu != null ) {
				Kiev.setCurFile(((FileUnit)fu).name);
				Kiev.setExtSet(((FileUnit)fu).disabled_extensions);
			}
		}

		try {
			((RStruct)this).autoGenerateTypeinfoClazz();
	
			if( !isInterface() && !isPackage() ) {
				// Default <init> method, if no one is declared
				boolean init_found = false;
				// Add outer hidden parameter to constructors for inner and non-static classes
				foreach (Constructor m; members; m.u_name == nameInit) {
					init_found = true;
					package_clazz.checkResolved();
					if (!isInterface() && isTypeUnerasable())
						m.params.insert(0,new LVar(m.pos,nameTypeInfo,typeinfo_clazz.xtype,Var.PARAM_TYPEINFO,ACC_FINAL|ACC_SYNTHETIC));
					if (package_clazz.isClazz() && !isStatic())
						m.params.insert(0,new LVar(m.pos,nameThisDollar,package_clazz.xtype,Var.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
				}
				if( !init_found ) {
					trace(Kiev.debug && Kiev.debugResolve,nameInit+" not found in class "+this);
					Constructor init = new Constructor(ACC_PUBLIC);
					init.setAutoGenerated(true);
					if (this != Type.tpClosureClazz && this.instanceOf(Type.tpClosureClazz)) {
						if( !isStatic() ) {
							init.params.append(new LVar(pos,nameThisDollar,package_clazz.xtype,Var.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
							init.params.append(new LVar(pos,"max$args",Type.tpInt,Var.PARAM_NORMAL,ACC_SYNTHETIC));
						} else {
							init.params.append(new LVar(pos,"max$args",Type.tpInt,Var.PARAM_NORMAL,ACC_SYNTHETIC));
						}
					} else {
						if( package_clazz.isClazz() && !isStatic() ) {
							init.params.append(new LVar(pos,nameThisDollar,package_clazz.xtype,Var.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
						}
						if (!isInterface() && isTypeUnerasable()) {
							init.params.append(new LVar(pos,nameTypeInfo,typeinfo_clazz.xtype,Var.PARAM_TYPEINFO,ACC_FINAL|ACC_SYNTHETIC));
						}
						if( isEnum() ) {
							init.params.append(new LVar(pos,"name",Type.tpString,Var.PARAM_NORMAL,ACC_SYNTHETIC));
							init.params.append(new LVar(pos,nameEnumOrdinal,Type.tpInt,Var.PARAM_NORMAL,ACC_SYNTHETIC));
							//init.params.append(new LVar(pos,"text",Type.tpString,Var.PARAM_NORMAL,ACC_SYNTHETIC));
						}
						if (isStructView()) {
							KievView kview = (KievView)this.variant;
							init.params.append(new LVar(pos,nameImpl,kview.view_of.getType(),Var.PARAM_NORMAL,ACC_FINAL|ACC_SYNTHETIC));
						}
					}
					init.pos = pos;
					init.body = new Block(pos);
					if (isEnum() || isSingleton())
						init.setPrivate();
					else
						init.setPublic();
					addMethod(init);
				}
			}
		} finally { Kiev.setExtSet(old_exts); Kiev.setCurFile(oldfn); }

		setMembersGenerated(true);
		foreach(Struct s; members)
			s.autoGenerateMembers();
	}

	static class StructDFFunc extends DFFunc {
		final int res_idx;
		StructDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			res = DFState.makeNewState();
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new StructDFFunc(dfi);
	}
}


