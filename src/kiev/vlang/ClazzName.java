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

@node
public class ClazzName extends Symbol implements Constants {

	/** Unqualified name of class/package/interface
		for java bytecode example - Object or tMethod
	 */
	public KString			short_name;

	/** Full-qualified name of class/package/interface
		for java bytecode example - java/lang/Object or kiev/Type$tMethod
	 */
	public KString			bytecode_name;

	public static ClazzName		Empty = new ClazzName(KString.Empty,KString.Empty,KString.Empty);

	public String toString() {
		return name.toString();
	}

	public ClazzName() {}
	public ClazzName(KString name, KString short_name, KString bytecode_name) {
		super(name);
		this.short_name = short_name;
		this.bytecode_name = bytecode_name;
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
		return new ClazzName(name,short_name,bytecode_name);
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
		return new ClazzName(name,short_name,bytecode_name);
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

	public KString		name; // source code name, may be null for anonymouse symbols
	public KString		unq_name; // unique name in scope, never null, usually equals to name
	public KString[]	aliases;

	@nodeview
	public static view VSymbol of Symbol extends NodeView {
		public:ro KString	name;
		public:ro KString	unq_name;
		public:ro KString[]	aliases;
	}

	public Symbol() {}
	public Symbol(int pos, KString name) {
		this.pos = pos;
		this.name = name;
		this.unq_name = name;
	}
	
	public Symbol(KString name) {
		this.name = name;
		this.unq_name = name;
	}
	
	public Symbol(KString name, KString unq_name) {
		this.name = name;
		this.unq_name = unq_name;
	}
	
	public void addAlias(KString al) {
		if (al == null || al == name || al == unq_name)
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
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = KString.from(t.image);
	}
	
	public boolean equals(Object:Object nm) {
		return false;
	}

	public boolean equals(Symbol:Object nm) {
		if (this.equals(nm.name)) return true;
		if (this.equals(nm.unq_name)) return true;
		if (nm.aliases != null) {
			foreach(KString n; nm.aliases; this.equals(n))
				return true;
		}
		return false;
	}

	public boolean equals(KString:Object nm) {
		if (name == nm) return true;
		if (unq_name == nm) return true;
		if (aliases != null) {
			foreach(KString n; aliases; n == nm)
				return true;
		}
		return false;
	}

	public String toString() {
		return (name != null) ? name.toString() : unq_name.toString();
	}
}
