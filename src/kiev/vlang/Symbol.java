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

	public KString		sname; // source code name, may be null for anonymouse symbols
	public KString		uname; // unique name in scope, never null, usually equals to name
	public KString[]	aliases;

	@nodeview
	public static view VSymbol of Symbol extends NodeView {
		public:ro KString	sname;
		public:ro KString	uname;
		public:ro KString[]	aliases;
	}

	public Symbol() {}
	public Symbol(int pos, KString sname) {
		this.pos = pos;
		this.sname = sname;
		this.uname = sname;
	}
	
	public Symbol(KString sname) {
		this.sname = sname;
		this.uname = sname;
	}
	
	public Symbol(KString sname, KString uname) {
		this.sname = sname;
		this.uname = uname;
	}
	
	public void addAlias(KString al) {
		if (al == null || al == sname || al == uname)
			return;
		// Check we do not have this alias already
		if (aliases == null) {
			aliases = new KString[]{ al };
		} else {
			foreach(KString n; aliases; n == al)
				return;
			aliases = (KString[])Arrays.append(aliases, al);
		}
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.sname = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.sname = KString.from(t.image);
		this.uname = this.sname;
	}
	
	public boolean equals(Object:Object nm) {
		return false;
	}

	public boolean equals(Symbol:Object nm) {
		if (this.equals(nm.sname)) return true;
		if (this.equals(nm.uname)) return true;
		if (nm.aliases != null) {
			foreach(KString n; nm.aliases; this.equals(n))
				return true;
		}
		return false;
	}

	public boolean equals(KString:Object nm) {
		if (sname == nm) return true;
		if (uname == nm) return true;
		if (aliases != null) {
			foreach(KString n; aliases; n == nm)
				return true;
		}
		return false;
	}

	public String toString() {
		return (sname != null) ? sname.toString() : uname.toString();
	}
}
