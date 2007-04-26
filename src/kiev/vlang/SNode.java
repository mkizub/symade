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
@node
public class SNode extends ASTNode {

	@virtual typedef This  ≤ SNode;
	@virtual typedef JView ≤ JSNode;
	@virtual typedef RView ≤ RSNode;

	@dflow(out="this:in") private static class DFI {}

	public static final SNode dummyNode = new SNode();

	public SNode() {}

	public ASTNode getDummyNode() {
		return SNode.dummyNode;
	}
	
	public final void resolveDecl() { ((RView)this).resolveDecl(); }

}

@node(name="Comment")
public final class Comment extends SNode {

	@virtual typedef This  = Comment;

    public static final AttrSlot ATTR_BEFORE = new ExtAttrSlot("comment before", true, false, TypeInfo.newTypeInfo(Comment.class,null));
    public static final AttrSlot ATTR_AFTER  = new ExtAttrSlot("comment after",  true, false, TypeInfo.newTypeInfo(Comment.class,null));

	@dflow(out="this:in") private static class DFI {}

	@att public boolean eol_form;
	@att public boolean multiline;
	@att public boolean doc_form;
	@att public boolean nl_before;
	@att public boolean nl_after;
	@att public String  text;
	
	public Comment() {}
}

@node
public abstract class DeclGroup extends SNode implements ScopeOfNames, ScopeOfMethods {
	
	@virtual typedef This  ≤ DeclGroup;
	@virtual typedef JView = JDeclGroup;
	@virtual typedef RView = RDeclGroup;

	@att public final	MetaSet			meta;
	@att public			TypeRef			dtype;
	@att public			DNode[]			decls;

	@getter public final Type	get$type() { return this.dtype.getType(); }

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
		n @= new SymbolIterator(this.decls, info.space_prev),
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
		dn @= decls,
		dn.isForward(),
		info.enterForward(dn) : info.leaveForward(dn),
		dn.getType().resolveCallAccessR(node,info,mt)
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

@node
public final class DeclGroupFields extends DeclGroup {
	@virtual typedef This  = DeclGroupFields;

	@dflow(out="decls") private static class DFI {
	@dflow(in="", seq="true")	DNode[]		decls;
	}

	public DeclGroupFields() {}
}

@node
public final class DeclGroupVars extends DeclGroup {
	@virtual typedef This  = DeclGroupVars;

	@dflow(out="decls") private static class DFI {
	@dflow(in="", seq="true")	DNode[]		decls;
	}

	public DeclGroupVars() {}
}

@node
public final class DeclGroupEnumFields extends DeclGroup {
	
	@virtual typedef This  = DeclGroupEnumFields;

	@dflow(out="decls") private static class DFI {
	@dflow(in="", seq="true")	DNode[]		decls;
	}

	public DeclGroupEnumFields() {
		this.meta.is_enum = true;
		setPublic();
		setStatic(true);
		setFinal(true);
	}

	public void callbackAttached() {
		ANode p = parent();
		if (p instanceof Struct) {
			this.dtype = new TypeRef(p.xtype);
			((JavaEnum)p.variant).group = this;
		}
	}
}

@node
public final class DeclGroupCaseFields extends DeclGroup {
	
	@virtual typedef This  = DeclGroupCaseFields;

	@dflow(out="decls") private static class DFI {
	@dflow(in="", seq="true")	DNode[]		decls;
	}

	public DeclGroupCaseFields() {}

	public void callbackAttached() {
		ANode p = parent();
		if (p instanceof Struct)
			((PizzaCase)p.variant).group = this;
	}
}

