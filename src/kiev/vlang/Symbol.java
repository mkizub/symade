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
import kiev.parser.Token;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 455 $
 *
 */

@node
@unerasable
public class Symbol<D extends DNode> extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  ≤ Symbol;

	public static final Symbol[] emptyArray = new Symbol[0];

	@att
	public	String				sname; // source code name, may be null for anonymouse symbols
	
	@getter D get$dnode() {
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
	
	@setter
	public void set$sname(String value) {
		this.sname = (value == null) ? null : value.intern();
	}
	
	public boolean equals(Object:Object nm) {
		return false;
	}

	public boolean equals(Symbol:Object nm) {
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

@node
//@unerasable
public final class SymbolRef<D extends DNode> extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SymbolRef;

	public static final SymbolRef[] emptyArray = new SymbolRef[0];

	@att public				String		name; // unresolved name
	@ref public				Symbol<D>	symbol; // resolved symbol
	@abstract public:ro		D			dnode; // resolved dnode (symbol.parent())
		 
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
		this.name = symbol.sname;
		this.symbol = symbol;
	}

	public SymbolRef(Symbol<D> symbol) {
		this.name = symbol.sname;
		this.symbol = symbol;
	}

	@getter public final D get$dnode() {
		if (symbol != null)
			return symbol.dnode;
		return null;
	}
	
	public boolean equals(Object nm) {
		if (nm instanceof Symbol) return nm.equals(this.name);
		if (nm instanceof SymbolRef) return nm.name == this.name;
		if (nm instanceof String) return nm == this.name;
		if (nm instanceof DNode) return nm.hasName(this.name,true);
		return false;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = t.image;
	}
	
	@getter
	public String get$name() {
		if (this.symbol != null)
			return this.symbol.sname;
		return name;
	}
	
	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
		if (this.symbol != null && this.symbol.sname != value) {
			this.symbol = null;
		}
	}
	
	@setter
	public void set$symbol(Symbol<D> value) {
		if (value == null) {
			if (this.symbol != null) {
				if (this.symbol.sname != this.name)
					this.name = this.symbol.sname;
				this.symbol = null;
			}
		}
		else if (this.symbol != value) {
			this.symbol = value;
			this.name = value.sname;
		}
	}
	
	public String toString() { return name; }

	public void callbackDetached() {
		this = ANode.getVersion(this).open();
		this.symbol = null;
		super.callbackDetached();
	}

	public DNode[] findForResolve(boolean by_equals) {
		ANode parent = parent();
		if (parent instanceof ASTNode)
			return parent.findForResolve(name, pslot(), by_equals);
		return null;
	}
}


