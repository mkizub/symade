package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.Token;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class ClazzName implements Constants {

	/** Unqualified name of class/package/interface
		for java bytecode example - Object or tMethod
	 */
	public KString			src_name;

	/** Full-qualified name of class/package/interface
		for java bytecode example - java/lang/Object or kiev/Type$tMethod
	 */
	public KString			bytecode_name;

	public KString			name; // source code name, may be null for anonymouse symbols
	public KString			unq_name; // unique name in scope, never null, usually equals to name

	public static ClazzName		Empty = new ClazzName(KString.Empty,KString.Empty,KString.Empty,KString.Empty);

	public String toString() {
		return name.toString();
	}

	public ClazzName() {}
	public ClazzName(KString qualified_name, KString src_name, KString bytecode_name, KString uniq_name) {
		this.name = qualified_name;
		this.src_name = src_name;
		this.bytecode_name = bytecode_name;
		this.unq_name = uniq_name;
	}

	public KString package_name() {
		int i;
		if( (i=name.lastIndexOf('.')) >= 0 )
			return name.substr(0,i);
		else
			return KString.Empty;
	}

	public KString package_bytecode_name() {
		int i;
		if( (i=name.lastIndexOf('.')) >= 0 )
			return bytecode_name.substr(0,i);
		else
			return KString.Empty;
	}

	public static ClazzName fromSignature(KString signature) {
		if(signature.equals(KString.Empty)) return Empty;
		KString bytecode_name = signature.substr(1,signature.length()-1);
		return fromBytecodeName(bytecode_name);
	}

	public static ClazzName fromToplevelName(KString name) {
		if(name.equals(KString.Empty)) return Empty;
		KString bytecode_name = name.replace('.','/');
		return fromBytecodeName(bytecode_name);
	}

	public static ClazzName fromBytecodeName(KString bytecode_name) {
		if(bytecode_name.equals(KString.Empty)) return Empty;
		KString name = bytecode_name.replace('/','.');
		name = fixName(name);
		int i;
		KString short_name;
		if( (i=name.lastIndexOf('.')) >= 0 )
			short_name = name.substr(i+1);
		else
			short_name = name;
		return new ClazzName(name,short_name,bytecode_name,short_name);
	}

	public static ClazzName fromOuterAndName(Struct outer, KString short_name, boolean isInn) {
		if(short_name.equals(KString.Empty)) return Empty;
		String delim = isInn ? "$" : "/" ;
		KString bytecode_name;
		KString name;
		if( outer.isPackage() ) {
			assert(!isInn,"fromOuterAndName("+outer+","+short_name+","+isInn+")");
			if (outer.qname != KString.Empty) {
				bytecode_name = KString.from(outer.bname+delim+short_name);
				name = KString.from(outer.qname+"."+short_name);
			} else {
				bytecode_name = short_name;
				name = short_name;
			}
		} else {
			assert(isInn,"fromOuterAndName("+outer+","+short_name+","+isInn+")");
			bytecode_name = KString.from(outer.bname+delim+short_name);
			name = KString.from(outer.qname+"."+short_name);
		}
		return new ClazzName(name,short_name,bytecode_name,short_name);
	}

	public int hashCode() { return name.hashCode(); }

	public boolean equals(ClazzName nm) { return name == nm.name; }

	public static KString fixName(KString str) {
		KString.KStringScanner sc = new KString.KStringScanner(str);
		KStringBuffer sb = new KStringBuffer(str.len);
		while(sc.hasMoreChars()) {
			char ch = sc.nextChar();
			if( ch == '$' ) {
				KString tmp = str.substr(0,sc.pos-1);
				byte b = (byte)(Env.existsStruct(tmp)?'.':'$');
				sb.append_fast(b);
			} else {
				sb.append(ch);
			}
		}
		return sb.toKString();
	}
}


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
