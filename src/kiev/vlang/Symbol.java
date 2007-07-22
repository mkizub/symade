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

@node(lang=void)
public interface ISymbol extends INode {
	@virtual typedef This  ≤ ISymbol;

	@getter public String	get$sname(); // source code name, may be null for anonymouse symbols
	@getter public String	get$qname(); // quilifies source code name, default is sname
	@getter public DNode	get$dnode();
}

@node(lang=CoreLang)
@unerasable
public class Symbol<D extends DNode> extends ASTNode implements ISymbol {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  ≤ Symbol;

	public static final Symbol[] emptyArray = new Symbol[0];

	@att public	String		sname; // source code name, may be null for anonymouse symbols
	
	@getter public D get$dnode() {
		ANode p = parent();
		if (p instanceof D)
			return (D)p;
		return null;
	}
	
	public Symbol() {}
	public Symbol(int pos, String sname) {
		this.pos = pos;
		this.sname = sname;
	}
	
	public Symbol(String sname) {
		this.sname = sname;
	}
	
	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if (attr.name == "sname")
				parent().callbackChildChanged(pslot());
		}
		super.callbackChildChanged(attr);
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
		if (nm.hasName(this.sname,true)) return true;
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

@node(lang=CoreLang)
public final class SymbolRef<D extends DNode> extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SymbolRef;

	public static final SymbolRef[] emptyArray = new SymbolRef[0];

	private Object	ident_or_symbol_or_type;
	
	@abstract @att public		String		name; // unresolved name
	@abstract @att public		boolean		qualified; // stored name may be qualified name
	@abstract @ref public		ISymbol		symbol; // resolved symbol
	@abstract      public:ro	D			dnode; // resolved dnode (symbol.parent())
		 
	public SymbolRef() {}

	public SymbolRef(String name) {
		this.name = name;
	}

	public SymbolRef(int pos, String name) {
		this.pos = pos;
		this.name = name;
	}

	public SymbolRef(int pos, Symbol<D> symbol) {
		this.pos = pos;
		this.symbol = symbol;
	}

	public SymbolRef(int pos, D symbol) {
		this.pos = pos;
		this.symbol = symbol;
	}

	public SymbolRef(Symbol<D> symbol) {
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
		if (nm instanceof DNode) return nm.hasName(this.name,true);
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

	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "name") {
			ANode parent = parent();
			if (parent instanceof ASTNode)
				return parent.findForResolve(name, pslot(), by_equals);
			return null;
		}
		return super.findForResolve(name,slot,by_equals);
	}
}


