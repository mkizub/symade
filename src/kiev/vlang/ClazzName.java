package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class ClazzName extends NodeName implements Constants {

	/** Unqualified name of class/package/interface
		for java bytecode example - Object or tMethod
	 */
	public KString			short_name;

	/** Full-qualified name of class/package/interface
		for java bytecode example - java/lang/Object or kiev/Type$tMethod
	 */
	public KString			bytecode_name;

	/** Class is an argument */
	public boolean			isInner = false;

	public static ClazzName		Empty = new ClazzName(KString.Empty,KString.Empty,KString.Empty,false);

	public String toString() {
		return name.toString();
	}

	public ClazzName(KString name, KString short_name, KString bytecode_name, boolean isInn) {
		super(name);
		this.short_name = short_name;
		this.bytecode_name = bytecode_name;
		this.isInner = isInn;
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
		boolean isInn;
		if( (i=name.lastIndexOf('.')) >= 0 ) {
			isInn = (bytecode_name.byteAt(i) == (byte)'$');
			short_name = name.substr(i+1);
		} else {
			isInn = false;
			short_name = name;
		}
		return new ClazzName(name,short_name,bytecode_name,isInn);
	}

	public static ClazzName fromOuterAndName(Struct outer, KString short_name, boolean isInn) {
		if(short_name.equals(KString.Empty)) return Empty;
		String delim = isInn ? "$" : "/" ;
		KString bytecode_name;
		KString name;
		if( outer.isPackage() ) {
			assert(!isInn,"fromOuterAndName("+outer+","+short_name+","+isInn+")");
			if( !outer.name.name.equals(KString.Empty) ) {
				bytecode_name = KString.from(outer.name.bytecode_name+delim+short_name);
				name = KString.from(outer.name.name+"."+short_name);
			} else {
				bytecode_name = short_name;
				name = short_name;
			}
		} else {
			assert(isInn,"fromOuterAndName("+outer+","+short_name+","+isInn+")");
			bytecode_name = KString.from(outer.name.bytecode_name+delim+short_name);
			name = KString.from(outer.name.name+"."+short_name);
		}
		return new ClazzName(name,short_name,bytecode_name,isInn);
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


public class NodeName {

	public KString			name;
	public List<KString>	aliases = List.Nil;

	public NodeName(KString name) {
		this.name = name;
	}
	
	public KString	toKString()	alias operator(210,fy,$cast) { return name; }

	public List<KString> getAllNames() {
		return new List<KString>.Cons(name, aliases);
	}
	
	public void addAlias(KString al) {
		// Check we do not have this alias already
		foreach(KString n; aliases)
			if( n.equals(al) )
				return;
		aliases = aliases.concat(al);
	}

	public boolean equals(Object nm) {
		if( nm instanceof NodeName ) return equals((NodeName)nm);
		if( nm instanceof KString ) return equals((KString)nm);
		return false;
	}

/* This are rules, but they cause a greate overhead,
   since this operation is one of most used.
   These methods are rewritten in pure java.

	public rule equals(NodeName nm)
		KString@ n;
	{
		{
			n ?= name;
			n @= aliases
		},
		nm.equals(n)
	}

	public rule equals(KString nm)
		KString@ n;
	{
		{
			n ?= name;
			n @= aliases
		},
		nm.equals(n)
	}
*/

	public boolean equals(NodeName nm) {
		if( nm.name.equals(name) ) return true;
		if( nm.aliases != List.Nil ) {
			if( nm.equals(name) ) return true;
			foreach(KString al; aliases; nm.equals(al) )
				return true;
		}
		else if( aliases != List.Nil ) {
			foreach(KString al; aliases; al.equals(nm.name) )
				return true;
		}
		else
			return name.equals(nm.name);
		return false;
	}

	public boolean equals(KString nm) {
		if( name.equals(nm) ) return true;
		if( aliases != List.Nil ) {
			foreach(KString al; aliases; al.equals(nm) )
				return true;
		}
		return false;
	}

	public String toString() {
		return name.toString();
	}

	public int hashCode() {
		return name.hashCode();
	}
}
