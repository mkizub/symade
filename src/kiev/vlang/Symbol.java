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
public class Symbol extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = Symbol;

	@att
	public String		sname; // source code name, may be null for anonymouse symbols
	public String		uname; // unique name in scope, never null, usually equals to name
	public String[]		aliases;

	public Symbol() {}
	public Symbol(int pos, String sname) {
		this.pos = pos;
		this.sname = sname;
	}
	
	public Symbol(String sname) {
		this.sname = sname;
	}
	
	public Symbol(String sname, String uname) {
		this.sname = sname;
		this.uname = uname.intern();
	}
	
	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if (attr.name == "sname")
				parent().callbackChildChanged(pslot());
		}
	}

	public void addAlias(String al) {
		if (al == null || al == sname || al == uname)
			return;
		// Check we do not have this alias already
		if (aliases == null) {
			aliases = new String[]{ al };
		} else {
			foreach(String n; aliases; n == al)
				return;
			aliases = (String[])Arrays.append(aliases, al);
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
		if (value != null) {
			this.sname = value.intern();
			this.uname = this.sname;
		} else {
			this.sname = null;
		}
	}
	
	public boolean equals(Object:Object nm) {
		return false;
	}

	public boolean equals(Symbol:Object nm) {
		if (this.equals(nm.sname)) return true;
		if (this.equals(nm.uname)) return true;
		if (nm.aliases != null) {
			foreach(String n; nm.aliases; this.equals(n))
				return true;
		}
		return false;
	}

	public boolean equals(String:Object nm) {
		if (sname == nm) return true;
		if (uname == nm) return true;
		if (aliases != null) {
			foreach(String n; aliases; n == nm)
				return true;
		}
		return false;
	}

	public String toString() {
		return (sname != null) ? sname : uname;
	}
}

@node
public class SymbolRef<D extends DNode> extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SymbolRef;

	public static final SymbolRef[] emptyArray = new SymbolRef[0];

	@att public String		name; // unresolved name
	@ref public D			symbol; // resolved symbol

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
		if (nm instanceof DNode) return nm.id.equals(this.name);
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
	}
	
	public String toString() { return name; }
	
	public DNode[] findForResolve(boolean by_equals) {
		ANode parent = parent();
		if (parent instanceof ASTNode)
			return parent.findForResolve(name, pslot(), by_equals);
		return null;
	}
}


