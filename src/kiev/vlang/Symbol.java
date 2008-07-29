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
 * @version $Revision$
 *
 */

@ThisIsANode(lang=void)
public interface ISymbol extends ASTNode {
	@getter public String	get$sname(); // source code name, may be null for anonymouse symbols
	@getter public String	get$qname(); // quilifies source code name, default is sname
	@getter public DNode	get$dnode();
	@getter public String	get$UUID(); // UUID of this symbol (auto-generated, if not exists)
}

@ThisIsANode(lang=CoreLang)
public class Symbol extends ASTNode implements ISymbol {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final Symbol[] emptyArray = new Symbol[0];
	
	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr public	String		sname; // source code name, may be null for anonymouse symbols

	@AttrXMLDumpInfo(attr=true)
	@UnVersioned
	@nodeAttr(copyable=false)
	public	String					uuid; // source code name, may be null for anonymouse symbols
	
	@getter public ANode get$node() {
		return parent();
	}
	
	@getter public DNode get$dnode() {
		ANode p = parent();
		if (p instanceof DNode)
			return (DNode)p;
		return null;
	}
	
	@getter final public String get$UUID() {
		String u = this.uuid;
		if (u == null) {
			u = java.util.UUID.randomUUID().toString();
			this.uuid = u;
		}
		return u;
	}
	@setter final public void set$uuid(String value) {
		assert (this.uuid == null);
		value = value.intern();
		Env.getRoot().registerISymbol(value,this);
		this.uuid = value;
	}

	public Symbol() {}
	public Symbol(int pos, String sname) {
		this.pos = pos;
		this.sname = sname;
	}
	
	public Symbol(String sname) {
		this.sname = sname;
	}
	
	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (isAttached()) {
			if (attr.name == "sname")
				parent().callbackChildChanged(ChildChangeType.MODIFIED, pslot(), this);
		}
		super.callbackChildChanged(ct, attr, data);
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.sname = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.sname = t.image;
	}
	
	@getter public final String get$sname() {
		return sname;
	}
	@setter public final void set$sname(String value) {
		this.sname = (value == null) ? null : value.intern();
	}
	
	@getter public final String get$qname() {
		ANode p = parent();
		if (p instanceof GlobalDNode) {
			String qn = ((GlobalDNode)p).qname();
			if (qn == null)
				return sname;
			int dot = qn.lastIndexOf('\u001f');
			if (dot < 0)
				return sname;
			return qn.substring(0,dot+1) + sname;
		}
		return sname;
	}

	public boolean equals(Object:Object nm) {
		return false;
	}

	public boolean equals(Symbol:Object nm) {
		if (this.equals(nm.sname)) return true;
		return false;
	}

	public boolean equals(DNode:Object nm) {
		if (this.equals(nm.sname)) return true;
		return false;
	}

	public boolean equals(String:Object nm) {
		if (sname == nm) return true;
		return false;
	}

	public String toString() {
		return sname;
	}

}

@ThisIsANode(lang=CoreLang)
public final class SymbolRef<D extends DNode> extends ASTNode {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final SymbolRef[] emptyArray = new SymbolRef[0];

	private Object	ident_or_symbol_or_type;
	
	@AttrXMLDumpInfo(attr=true)
	@abstract @nodeAttr public		String		name; // unresolved name
	@AttrXMLDumpInfo(attr=true, name="full")
	@abstract @nodeAttr public		boolean		qualified; // stored name may be qualified name
	@AttrXMLDumpInfo(ignore=true)
	@abstract @nodeData public		ISymbol		symbol; // resolved symbol
	@abstract           public:ro	D			dnode; // resolved dnode (symbol.parent())
		 
	public SymbolRef() {}

	public SymbolRef(String name) {
		this.name = name;
	}

	public SymbolRef(int pos, String name) {
		this.pos = pos;
		this.name = name;
	}

	public SymbolRef(int pos, ISymbol symbol) {
		this.pos = pos;
		this.symbol = symbol;
	}

	public SymbolRef(int pos, D symbol) {
		this.pos = pos;
		this.symbol = symbol;
	}

	public SymbolRef(Symbol symbol) {
		this.symbol = symbol;
	}

	public SymbolRef(D symbol) {
		this.symbol = symbol;
	}

	@getter public final boolean get$qualified() {
		return is_qualified;
	}

	@setter public final void set$qualified(boolean val) {
		is_qualified = val;
	}

	public boolean equals(Object nm) {
		if (nm instanceof DNode) return nm.sname == this.name;
		if (nm instanceof Symbol) return nm.equals(this.name);
		if (nm instanceof SymbolRef) return nm.name == this.name;
		if (nm instanceof String) return nm == this.name;
		return false;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = t.image;
	}

	@getter public final String get$name() {
		Object id = ident_or_symbol_or_type;
		if (id instanceof String)
			return (String)id;
		if (id instanceof ISymbol) {
			if (qualified)
				return ((ISymbol)id).qname;
			return ((ISymbol)id).sname;
		}
		if (id instanceof Type) {
			if (qualified)
				return ((Type)id).meta_type.qname();
			return ((Type)id).meta_type.tdecl.sname;
		}
		return null;
	}

	@getter public final ISymbol get$symbol() {
		Object id = ident_or_symbol_or_type;
		if (id instanceof ISymbol)
			return (ISymbol)id;
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl;
		return null;
	}
	
	@getter public final D get$dnode() {
		Object id = ident_or_symbol_or_type;
		if (id instanceof DNode)
			return (DNode)id;
		if (id instanceof ISymbol)
			return ((ISymbol)id).dnode;
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl;
		return null;
	}
	
	@setter public final void set$name(String val) {
		if (val != null) {
			val = val.intern();
			if (val.indexOf('\u001f') >= 0)
				qualified = true;
		}
		ident_or_symbol_or_type = val;
	}
	
	@setter public final void set$symbol(ISymbol val) {
		ident_or_symbol_or_type = val;
	}
	
	public String toString() { return name; }

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "qualified")
			return this.qualified; // do not dump <qualified>false</qualified>
		return super.includeInDump(dump, attr, val);
	}

	public DNode[] resolveAutoComplete(String name, AttrSlot slot) {
		if (slot.name == "name") {
			ANode parent = parent();
			if (parent instanceof ASTNode)
				return parent.resolveAutoComplete(name, pslot());
			return null;
		}
		return super.resolveAutoComplete(name,slot);
	}
	
	@unerasable
	public static DNode[] autoCompleteSymbol(ASTNode resolve_in, String str, (DNode)->boolean check) {
		if (str == null || str.length() == 0)
			return new DNode[0];
		ResInfo info = new ResInfo(resolve_in, str, ResInfo.noEquals);
		Vector<DNode> vect = new Vector<DNode>();
		DNode@ a;
		foreach (PassInfo.resolveNameR(resolve_in,a,info)) {
			DNode dn = (DNode)a;
			if (vect.contains(dn) || !check(dn))
				continue;
			vect.append(dn);
		}
		return vect.toArray();
	}

	@unerasable
	public static <A extends DNode> A[] autoCompleteSymbol(SymbolRef<A> self, String str) {
		if (self == null || str == null || str.length() == 0)
			return new A[0];
		ResInfo info = new ResInfo(self, str, ResInfo.noEquals);
		Vector<A> vect = new Vector<A>();
		A@ a;
		foreach (PassInfo.resolveNameR(self,a,info))
			if (!vect.contains(a)) vect.append(a);
		return vect.toArray();
	}

	@unerasable
	public static <A extends DNode> void resolveSymbol(SeverError sever, SymbolRef<A> self, (DNode)->boolean check) {
		if (self == null)
			return;
		String sname = self.name;
		if (sname == null || sname == "")
			return;
		A@ a;
		if (!PassInfo.resolveNameR(self,a,new ResInfo(self,sname,ResInfo.noForwards))) {
			Kiev.reportAs(sever, self,"Unresolved "+self);
			return;
		}
		A dn = (A)a;
		if (!check(dn)) {
			Kiev.reportAs(sever, self, "Resolved "+self+" does not match required constraints");
			return;
		}
		if (self.symbol != dn)
			self.symbol = dn;
	}

	@unerasable
	public static <A extends DNode> void resolveSymbol(SeverError sever, SymbolRef<A> self) {
		if (self == null)
			return;
		String sname = self.name;
		if (sname == null || sname == "")
			return;
		A@ a;
		if (!PassInfo.resolveNameR(self,a,new ResInfo(self,sname,ResInfo.noForwards)))
			Kiev.reportAs(sever,self,"Unresolved "+self);
		A dn = (A)a;
		if (self.symbol != a)
			self.symbol = a;
	}
}


