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
public class Symbol<D extends DNode> extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  ≤ Symbol;

	public static final Symbol[] emptyArray = new Symbol[0];

	@att
	public String		sname; // source code name, may be null for anonymouse symbols
	
	@ref
	public D			dnode;
	
	@ref
	public SymbolRef	refs;
	
	public void callbackAttached() {
		ANode p = parent();
		if (p instanceof D)
			dnode = (D)p;
		super.callbackAttached();
	}

	public void callbackDetached() {
		dnode = null;
		super.callbackDetached();
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
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.sname = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.sname = t.image;
	}
	
	@setter
	public void set$sname(String value)
		alias operator(5, lfy, =)
	{
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
public class SymbolRef<D extends DNode> extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SymbolRef;

	public static final SymbolRef[] emptyArray = new SymbolRef[0];

	@att public String		name; // unresolved name
	@ref public D			symbol; // resolved symbol
	@ref public SymbolRef	next; // next SymbolRef which refers the same Symbol

	public SymbolRef() {}

	public SymbolRef(String name) {
		this.name = name;
	}

	public SymbolRef(int pos, String name) {
		this.pos = pos;
		this.name = name;
	}

	public SymbolRef(int pos, D symbol) {
		this.pos = pos;
		this.name = symbol.id.sname;
		this.symbol = symbol;
	}

	public SymbolRef(String name, D symbol) {
		this.name = name;
		this.symbol = symbol;
	}

	public boolean equals(Object nm) {
		if (nm instanceof Symbol) return nm.equals(this.name);
		if (nm instanceof SymbolRef) return nm.name == this.name;
		if (nm instanceof String) return nm == this.name;
		if (nm instanceof DNode) return nm.hasName(this.name,true);
		return false;
	}

	public void set(D symbol)
		alias operator(5, lfy, =)
	{
		if (this.name == null && symbol != null)
			this.name = symbol.id.sname;
		this.symbol = (D)symbol;
	}
	
	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = t.image;
	}
	
	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public String toString() { return name; }
	
	public DNode[] findForResolve(boolean by_equals) {
		ANode parent = parent();
		if (parent instanceof ASTNode)
			return parent.findForResolve(name, pslot(), by_equals);
		return null;
	}
}


