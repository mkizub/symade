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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */


@ThisIsANode(lang=CoreLang)
public class JavaClass extends Struct {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}
}

@ThisIsANode(lang=CoreLang)
public final class JavaAnonymouseClass extends JavaClass {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}
}

@ThisIsANode(lang=CoreLang)
public class JavaInterface extends Struct {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}
	public JavaInterface() {
		this.mflags_is_struct_interface = true;
	}
	public void cleanupOnReload() {
		super.cleanupOnReload();
		this.mflags_is_struct_interface = true;
	}
}

@ThisIsANode(lang=CoreLang)
public final class KievView extends Struct {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}

	@nodeAttr public TypeRef						view_of;

	public KievView() {
		this.mflags_is_virtual = true;
	}
	public void cleanupOnReload() {
		super.cleanupOnReload();
		this.mflags_is_virtual = true;
	}
	
	public Struct getViewImpl() {
		UserMeta view_meta = (UserMeta)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fViewOf");
		if (view_meta != null && view_meta.getZ("iface"))
			return this.iface_impl;
		return this;
	}
}

@ThisIsANode(lang=CoreLang)
public final class JavaAnnotation extends JavaInterface {
	public JavaAnnotation() {
		this.mflags_is_struct_annotation = true;
	}
	public void cleanupOnReload() {
		super.cleanupOnReload();
		this.mflags_is_struct_annotation = true;
	}

	public void resolveMetaDefaults() {
		foreach(Method m; members) {
			try {
				m.resolveMetaDefaults();
			} catch(Exception e) {
				Kiev.reportError(m,e);
			}
		}
	}

}

@ThisIsANode(lang=CoreLang)
public final class PizzaCase extends Struct {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}

	// declare NodeAttr_case_fields to be an attribute for ANode.nodeattr$syntax_parent
	static final class NodeAttr_case_fields extends SpaceAttAttrSlot<Field> {
		public final ANode[] get(ANode parent) { return ((PizzaCase)parent).case_fields; }
		public final void set(ANode parent, Object narr) { ((PizzaCase)parent).case_fields = (Field∅)narr; }
		NodeAttr_case_fields(String name, TypeInfo typeinfo) {
			super(name, ANode.nodeattr$syntax_parent, typeinfo);
		}
	}

	@nodeAttr public Field∅	case_fields;

	public int tag;

	public PizzaCase() {}
	
	public Field[] getCaseFields() {
		return this.case_fields;
	}

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (ct == ChildChangeType.ATTACHED) {
			if (attr.name == "case_fields") {
				Field f = (Field)data;
				if (f.parent() == null)
					this.members += f;
				assert (f.parent() == this);
			}
		}
		else if (ct == ChildChangeType.DETACHED) {
			if (attr.name == "case_fields") {
				Field f = (Field)data;
				if (f.parent() == this)
					~f;
				assert (f.parent() == null);
			}
		}
		super.callbackChildChanged(ct, attr, data);
	}

}

@ThisIsANode(lang=CoreLang)
public final class JavaEnum extends JavaClass {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}

	// declare NodeAttr_enum_fields to be an attribute for ANode.nodeattr$syntax_parent
	static final class NodeAttr_enum_fields extends SpaceAttAttrSlot<Field> {
		public final ANode[] get(ANode parent) { return ((JavaEnum)parent).enum_fields; }
		public final void set(ANode parent, Object narr) { ((JavaEnum)parent).enum_fields = (Field∅)narr; }
		NodeAttr_enum_fields(String name, TypeInfo typeinfo) {
			super(name, ANode.nodeattr$syntax_parent, typeinfo);
		}
	}

	// is a syntax_parent
	@nodeAttr public		Field∅			enum_fields;

	public JavaEnum() {
		this.mflags_is_enum = true;
	}
	public void cleanupOnReload() {
		super.cleanupOnReload();
		this.mflags_is_enum = true;
	}
	public Field[] getEnumFields() {
		return enum_fields;
	}

	public int getIndexOfEnumField(Field f) {
		Field[] enum_fields = this.enum_fields;
		for (int i=0; i < enum_fields.length; i++) {
			if (f == enum_fields[i])
				return i;
		}
		throw new RuntimeException("Enum value for field "+f+" not found in "+this);
	}

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (ct == ChildChangeType.ATTACHED) {
			if (attr.name == "enum_fields" && data instanceof Field) {
				Field f = (Field)data;
				if (f.parent() == null)
					this.members += f;
				assert (f.parent() == this);
				f.mflags_is_enum = true;
				f.setPublic();
				f.setStatic(true);
				f.setFinal(true);
				f.vtype = new TypeRef(this.xtype);
				f.vtype.setAutoGenerated(true);
			}
		}
		else if (ct == ChildChangeType.DETACHED) {
			if (attr.name == "enum_fields" && data instanceof Field) {
				Field f = (Field)data;
				if (f.parent() == this)
					~f;
				assert (f.parent() == null);
				f.mflags_is_enum = false;
			}
		}
		super.callbackChildChanged(ct, attr, data);
	}
}

@ThisIsANode(lang=void)
public final class InnerStructInfo extends ASTNode {
	@nodeAttr public int			inner_count; 
	@nodeData public DNode∅		inners;	// structures, static fields in methods, etc
}

@ThisIsANode(lang=CoreLang)
public abstract class Struct extends ComplexTypeDecl {
	
	@nodeData(ext_data=true)		public InnerStructInfo		inner_info;
	@nodeData(ext_data=true)		public Struct				typeinfo_clazz;
	@nodeData(ext_data=true)		public Struct				iface_impl;

	public Object copy(CopyContext cc) {
		Struct obj = (Struct)super.copy(cc);
		if (this == obj)
			return this;
		obj.xmeta_type = new CompaundMetaType(obj);
		obj.xtype = new CompaundType((CompaundMetaType)obj.xmeta_type, null, null);
		obj.callbackTypeVersionChanged();
		return obj;
	}

	public boolean isClazz() {
		return !isInterface();
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
	
	/** Add information about new method that belongs to this class */
	public Method addMethod(Method m) {
		// Check we already have this method
		members.append(m);
		trace(Kiev.debug && Kiev.debugMembers,"Method "+m+" added to class "+this);
		if (m instanceof Constructor) {
			foreach (Constructor mm; members; mm != m) {
				if (mm.mtype.equals(m.mtype))
					Kiev.reportError(m,"Constructor "+m+" already exists in class "+this);
			}
		} else {
			foreach (Method mm; members; mm != m) {
				if (mm.sname == m.sname && mm.mtype.equals(m.mtype))
					Kiev.reportError(m,"Method "+m+" already exists in class "+this);
			}
		}
		return m;
	}

	/** Add information about new field that belongs to this class */
	public Field addField(Field f) {
		// Check we already have this field
		foreach (Field ff; this.members) {
			if( ff.equals(f) ) {
				throw new RuntimeException("Field "+f+" already exists in class "+this);
			}
		}
		members.append(f);
		trace(Kiev.debug && Kiev.debugMembers,"Field "+f+" added to class "+this);
		return f;
	}

	/** Add information about new pizza case of this class */
	public PizzaCase addCase(PizzaCase cas) {
		setHasCases(true);
		int caseno = 0;
		foreach (PizzaCase pcase; members; pcase.tag > caseno)
			caseno = pcase.tag;
		cas.tag = caseno + 1;
		trace(Kiev.debug && Kiev.debugMembers,"Class's case "+cas+" added to class "	+this+" as case # "+(caseno+1));
		return cas;
	}
		
	public Constructor getClazzInitMethod() {
		foreach(Constructor n; members; n.isStatic())
			return n;
		Constructor class_init = new Constructor(ACC_STATIC);
		class_init.pos = pos;
		class_init.setAutoGenerated(true);
		addMethod(class_init);
		class_init.body = new Block(pos);
		return class_init;
	}

	public Struct() {
		super(null);
	}

	public void initStruct(String name, int flags) {
		this.sname = name;
		if (flags != 0) {
			if!(this instanceof KievPackage) {
				if ((flags & ACC_PUBLIC) == ACC_PUBLIC) setMeta(new MetaAccess("public"));
				if ((flags & ACC_PROTECTED) == ACC_PROTECTED) setMeta(new MetaAccess("protected"));
				if ((flags & ACC_PRIVATE) == ACC_PRIVATE) setMeta(new MetaAccess("private"));
			}
			if ((flags & ACC_STATIC) == ACC_STATIC) setMeta(new MetaStatic());
			if ((flags & ACC_FINAL) == ACC_FINAL) setMeta(new MetaFinal());
			if ((flags & ACC_ABSTRACT) == ACC_ABSTRACT) setMeta(new MetaAbstract());
			if ((flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) setMeta(new MetaSynthetic());
			if ((flags & ACC_MACRO) == ACC_MACRO) setMeta(new MetaMacro());
			if ((flags & ACC_TYPE_UNERASABLE) == ACC_TYPE_UNERASABLE) setMeta(new MetaUnerasable());
			this.nodeflags |= flags;
		}
		if (this.xmeta_type == null) {
			CompaundMetaType cmt = new CompaundMetaType(this);
			this.xmeta_type = cmt;
			this.xtype = new CompaundType(cmt, null, null);
		} else {
			CompaundMetaType.checkNotDeferred(this);
		}
		callbackTypeVersionChanged();
	}

	public Struct getStruct() { return this; }

	public void cleanupOnReload() {
		this.metas.delAll();
		this.nodeflags = 0;
		this.typeinfo_clazz = null;
		super.cleanupOnReload();
	}

	public boolean preResolveIn() {
		if (this.isInterfaceOnly())
			return false;
		return true;
	}

	// verify resolved tree
	public boolean preVerify() {
		setFrontEndPassed();
		foreach (TypeRef tr; super_types; !(tr instanceof MacroSubstTypeRef)) {
			TypeDecl td = tr.getTypeDecl();
			if (td == null)
				Kiev.reportError(this, "Struct "+this+" extends unresolved type "+tr);
			else if (td.isFinal())
				Kiev.reportError(this, "Struct "+this+" extends final type "+tr);
		}
		if (isInterface() && !isStructView() && !isMixin()) {
			foreach (ASTNode n; members; n instanceof Field || n instanceof Initializer) {
				if (n instanceof Field && !n.isAbstract()) {
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
			n.setStatic(true);
		}
		if (!n.isFinal()) {
			Kiev.reportError(n,"Non-final field "+n+" in interface "+this);
			n.setFinal(true);
		}
	}
	private void verifyInitializerInIface(Initializer n) {
		if (!n.isStatic()) {
			Kiev.reportError(n,"Non-static initializer in interface "+this);
			n.setStatic(true);
		}
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


