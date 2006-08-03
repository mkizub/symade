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
	public				String		sname; // source code name, may be null for anonymouse symbols
	
	@access:n,n,r,rw	D			dnode;
	private				SymbolRef	refs;
	
	public Object copyTo(Object to$node)
	{
		Symbol node = (Symbol)super.copyTo(to$node);
		node.sname = this.sname;
		node.dnode = this.dnode;
		//node.refs = refs; // don't copy!
		return node;
	}

	public void callbackAttached() {
		ANode p = parent();
		if (p instanceof D)
			dnode = (D)p;
		for (SymbolRef sr = refs; sr != null; sr = sr.next)
			sr.callbackSymbolChanged();
		super.callbackAttached();
	}

	public void callbackDetached() {
		dnode = null;
		for (SymbolRef sr = refs; sr != null; sr = sr.next)
			sr.callbackSymbolChanged();
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
			if (attr.name == "sname") {
				parent().callbackChildChanged(pslot());
				for (SymbolRef sr = refs; sr != null; sr = sr.next)
					sr.callbackSymbolChanged();
			}
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
	
	void link(SymbolRef sr) {
		assert (sr.symbol == null && sr.next == null);
		if (this.refs == null) {
			this.refs = sr;
		} else {
			sr.next = this.refs;
			this.refs = sr;
		}
	}
	void unlink(SymbolRef sr) {
		assert (sr.symbol == this);
		if (this.refs == sr) {
			this.refs = sr.next;
			sr.next = null;
			return;
		}
		SymbolRef prev = refs;
		while (prev != null && prev.next != sr)
			prev = prev.next;
		assert (prev != null && prev.next == sr);
		prev.next = sr.next;
		sr.next = null;
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
	     public:r,r,rw,rw	D			dnode; // resolved dnode (symbol.parent())
	     public:r,r,rw,rw	SymbolRef	next; // next SymbolRef which refers the same Symbol
		 
	public Object copyTo(Object to$node)
	{
		SymbolRef<D> node = (SymbolRef<D>)super.copyTo(to$node);
		node.name = this.name;
		node.symbol = this.symbol;
		//node.dnode = this.dnode; // don't copy!
		//node.next = next; // don't copy!
		return node;
	}

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
	
	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
		if (this.symbol != null && this.symbol.sname != value)
			this.symbol = null;
	}
	
	@setter
	public void set$symbol(Symbol<D> value) {
		if (value == null) {
			if (this.symbol != null) {
				this.dnode = null;
				this.symbol.unlink(this);
				assert (next == null);
				this.symbol = null;
			}
		}
		else if (this.symbol != value) {
			if (this.symbol != null) {
				this.dnode = null;
				this.symbol.unlink(this);
				assert (next == null);
				this.symbol = null;
			}
			value.link(this);
			this.symbol = value;
			this.dnode = value.dnode;
		}
	}
	
	public String toString() { return name; }

	public void callbackDetached() {
		this.symbol = null;
		super.callbackDetached();
	}

	void callbackSymbolChanged() {
		this.dnode = this.symbol.dnode;
		if (this.name != this.symbol.sname)
			this.name = this.symbol.sname;
	}
	
	public DNode[] findForResolve(boolean by_equals) {
		ANode parent = parent();
		if (parent instanceof ASTNode)
			return parent.findForResolve(name, pslot(), by_equals);
		return null;
	}
}


