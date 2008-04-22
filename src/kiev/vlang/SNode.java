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

import kiev.ir.java15.RSNode;
import kiev.be.java15.JSNode;
import kiev.ir.java15.RDeclGroup;
import kiev.be.java15.JDeclGroup;

import syntax kiev.Syntax;

/**
 * A node that is a syntax modifier: import, operator decl, separators, comments, etc.
 */
@ThisIsANode(lang=CoreLang)
public class SNode extends ASTNode {

	@virtual typedef This  ≤ SNode;
	@virtual typedef JView ≤ JSNode;
	@virtual typedef RView ≤ RSNode;

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final SNode dummyNode = new SNode();

	public SNode() {}

	public ASTNode getDummyNode() {
		return SNode.dummyNode;
	}
	
	public final void resolveDecl() { ((RView)this).resolveDecl(); }

}

@ThisIsANode(name="Comment", lang=CoreLang)
public final class Comment extends SNode {

	@virtual typedef This  = Comment;

    public static final AttrSlot ATTR_BEFORE = new ExtAttrSlot("comment before", ANode.nodeattr$parent, false, TypeInfo.newTypeInfo(Comment.class,null));
    public static final AttrSlot ATTR_AFTER  = new ExtAttrSlot("comment after",  ANode.nodeattr$parent, false, TypeInfo.newTypeInfo(Comment.class,null));

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@nodeAttr public boolean eol_form;
	@nodeAttr public boolean multiline;
	@nodeAttr public boolean doc_form;
	@nodeAttr public boolean nl_before;
	@nodeAttr public boolean nl_after;
	@nodeAttr public String  text;
	
	public Comment() {}
}

@ThisIsANode(lang=CoreLang)
public abstract class DeclGroup extends SNode implements ScopeOfNames, ScopeOfMethods {
	
	@virtual typedef This  ≤ DeclGroup;
	@virtual typedef JView = JDeclGroup;
	@virtual typedef RView = RDeclGroup;

	@nodeAttr public final	MetaSet			meta;
	@nodeAttr public		TypeRef			dtype;

	@getter public final Type	get$type() { return this.dtype.getType(); }

	public abstract DNode[] getDecls();

	public DeclGroup() {
		this.meta = new MetaSet();
	}

	public Type	getType() { return type; }

	public final MetaAccess getMetaAccess() {
		return (MetaAccess)this.getMeta("kiev\u001fstdlib\u001fmeta\u001faccess");
	}

	public final MNode getMeta(String name) {
		return this.meta.getMeta(name);
	}
	public final MNode setMeta(MNode meta)  alias add alias lfy operator +=
	{
		return this.meta.setMeta(meta);
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
		ASTNode@ n;
	{
		n @= new SymbolIterator(this.getDecls(), info.space_prev),
		{
			info.checkNodeName(n),
			node ?= n
		;	info.isForwardsAllowed(),
			((DNode)n).isForward(),
			info.enterForward(n) : info.leaveForward(n),
			n.getType().resolveNameAccessR(node,info)
		}
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		DNode@ dn;
	{
		info.isForwardsAllowed(),
		dn @= getDecls(),
		dn.isForward(),
		info.enterForward(dn) : info.leaveForward(dn),
		dn.getType().resolveCallAccessR(node,info,mt)
	}

	public boolean isPublic() {
		MetaAccess m = getMetaAccess();
		return (m != null && m.simple == "public");
	}
	public boolean isPrivate() {
		MetaAccess m = getMetaAccess();
		return (m != null && m.simple == "private");
	}
	public void setPublic() {
		MetaAccess m = getMetaAccess();
		if (m == null)
			this.setMeta(new MetaAccess("public"));
		else
			m.setSimple("public");
	}
	public void setStatic(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fstatic");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaStatic());
		}
	}
	public void setFinal(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001ffinal");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaFinal());
		}
	}
	public void setAbstract(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fabstract");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaAbstract());
		}
	}
	public final void setVirtual(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fvirtual");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVirtual());
		}
	}
}

@ThisIsANode(lang=CoreLang)
public final class DeclGroupVars extends DeclGroup {
	@virtual typedef This  = DeclGroupVars;

	@DataFlowDefinition(out="decls") private static class DFI {
	@DataFlowDefinition(in="", seq="true")	DNode[]		decls;
	}

	@nodeAttr public		DNode[]			decls;

	public DNode[] getDecls() { return decls; }

	public DeclGroupVars() {}
}

@ThisIsANode(lang=CoreLang)
public abstract class ADeclGroupFields extends DeclGroup {
	@virtual typedef This  ≤ ADeclGroupFields;

	// declare NodeAttr_decls to be an attribute for ANode.nodeattr$syntax_parent
	static final class NodeAttr_decls extends SpaceAttAttrSlot<Field> {
		public final ANode[] get(ANode parent) { return ((ADeclGroupFields)parent).decls; }
		public final void set(ANode parent, Object narr) { ((ADeclGroupFields)parent).decls = (Field[])narr; }
		NodeAttr_decls(String name, TypeInfo typeinfo) {
			super(name, ANode.nodeattr$syntax_parent, typeinfo);
		}
	}

	// is a syntax_parent
	@nodeAttr public		Field[]			decls;

	public DNode[] getDecls() { return decls; }

	public ADeclGroupFields() {}

	private void checkFieldsAreAttached() {
		ANode p = parent();
		if (p instanceof TypeDecl) {
			// attach all my fields to the parent TypeDecl
			TypeDecl td = (TypeDecl)p;
			foreach (Field f; decls) {
				if (f.parent() == null)
					td.members += f;
				assert (f.parent() == td);
			}
		}
	}

	public void callbackAttached(ParentInfo pi) {
		if (pi.isSemantic())
			checkFieldsAreAttached();
		super.callbackAttached(pi);
	}

	public void callbackDetached(ANode parent, AttrSlot slot) {
		if (slot.isSemantic()) {
			if (parent instanceof TypeDecl) {
				// detach all my fields from the parent TypeDecl
				TypeDecl td = (TypeDecl)parent;
				foreach (Field f; decls) {
					if (f.parent() == td)
						~f;
					assert (f.parent() == null);
				}
			}
		}
	}

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "decls")
			checkFieldsAreAttached();
		super.callbackChildChanged(ct, attr, data);
	}
	
}

@ThisIsANode(lang=CoreLang)
public final class DeclGroupFields extends ADeclGroupFields {
	@virtual typedef This  = DeclGroupFields;

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public DeclGroupFields() {}
}

@ThisIsANode(lang=CoreLang)
public final class DeclGroupEnumFields extends ADeclGroupFields {
	
	@virtual typedef This  = DeclGroupEnumFields;

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public DeclGroupEnumFields() {
		this.meta.is_enum = true;
		setPublic();
		setStatic(true);
		setFinal(true);
	}

	public void callbackAttached(ParentInfo pi) {
		if (pi.isSemantic()) {
			ANode p = parent();
			if (p instanceof Struct) {
				this.dtype = new TypeRef(p.xtype);
				((JavaEnum)p).group = this;
			}
		}
		super.callbackAttached(pi);
	}
}

@ThisIsANode(lang=CoreLang)
public final class DeclGroupCaseFields extends ADeclGroupFields {
	
	@virtual typedef This  = DeclGroupCaseFields;

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public DeclGroupCaseFields() {}

	public void callbackAttached(ParentInfo pi) {
		if (pi.isSemantic()) {
			ANode p = parent();
			if (p instanceof Struct)
				((PizzaCase)p).group = this;
		}
		super.callbackAttached(pi);
	}
}

