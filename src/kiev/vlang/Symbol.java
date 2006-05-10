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
	@virtual typedef VView = VSymbol;

	@att
	public String		sname; // source code name, may be null for anonymouse symbols
	public String		uname; // unique name in scope, never null, usually equals to name
	public String[]		aliases;

	@nodeview
	public static view VSymbol of Symbol extends NodeView {
		public:ro String	sname;
		public:ro String	uname;
		public:ro String[]	aliases;
	}

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
	public void set$sname(String value) {
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