/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/ClazzName.java,v 1.3.4.1 1999/02/15 21:45:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

public class ClazzName implements Constants {

	/** Full-qualified name of class/package/interface
		for example - java.lang.Object or kiev.Type.tMethod
	 */
	public KString		name;

	/** Unqualified name of class/package/interface
		for java bytecode example - Object or tMethod
	 */
	public KString			short_name;

	/** Full-qualified name of class/package/interface
		for java bytecode example - java/lang/Object or kiev/Type$tMethod
	 */
	public KString			bytecode_name;

	/** Full-qualified name of class/package/interface
		for cpp code example - ::java::lang::Object or ::kiev::Type::tMethod,
		if namespaces for cpp enabled
	 */
	public KString			cpp_name;

	/** Class is an argument */
	public boolean			isArgument = false;

	public static ClazzName		Empty = new ClazzName(KString.Empty,KString.Empty,KString.Empty);

	public String toString() { return name.toString(); }

	public ClazzName(KString name, KString short_name, KString bytecode_name) {
		this.name = name;
		this.short_name = short_name;
		this.bytecode_name = bytecode_name;
	}

	public void make_cpp_name() {
		if (cpp_name != null)
			return;
		StringBuffer nm = new StringBuffer();
		int i = 0;
		if (!Kiev.gen_cpp_namespace) {
			i = name.lastIndexOf('.');
			if (i < 0) i = 0;
		} else {
			nm.append("::");
		}
		for (; i < name.length(); i++) {
			byte b = name.byteAt(i);
			if (b=='.' || b=='$') {
				nm.append("::");
			} else {
				nm.append((char)b);
			}
		}
		cpp_name = KString.from(nm.toString());
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

	public KString package_cpp_namespace_name() {
		int i;
		if( (i=cpp_name.lastIndexOf(':')) > 1 )
			return cpp_name.substr(0,i-1);
		else
			return Constants.GlobalCppNamespace;
	}

	public KString signature() {
		KStringBuffer ksb = new KStringBuffer(bytecode_name.len+2);
		if( isArgument )
			ksb.append_fast((byte)'A');
		else
			ksb.append_fast((byte)'L');
		return ksb.append_fast(bytecode_name).append_fast((byte)';').toKString();
	}

	public static ClazzName fromSignature(KString signature) {
		if(signature.equals(KString.Empty)) return Empty;
		KString bytecode_name = signature.substr(1,signature.length()-1);
		KString name = bytecode_name.replace('/','.');
		name = replaceDollars(name);
		int i;
		KString short_name;
		if( (i=name.lastIndexOf('.')) >= 0 ) {
			short_name = name.substr(i+1);
		} else {
			short_name = name;
		}
		ClazzName clname = new ClazzName(name,short_name,bytecode_name);
		if( signature.byteAt(0) == 'A' ) clname.isArgument = true;
		return clname;
	}

	public static ClazzName fromBytecodeName(KString bytecode_name) {
		if(bytecode_name.equals(KString.Empty)) return Empty;
		KString name = bytecode_name.replace('/','.');
		name = replaceDollars(name);
		int i;
		KString short_name;
		if( (i=name.lastIndexOf('.')) >= 0 ) {
			short_name = name.substr(i+1);
		} else {
			short_name = name;
		}
		return new ClazzName(name,short_name,bytecode_name);
	}

	public static ClazzName fromToplevelName(KString name) {
		if(name.equals(KString.Empty)) return Empty;
		KString bytecode_name = name.replace('.','/');
		int i;
		KString short_name;
		name = fixLocalName(name);
		if( (i=name.lastIndexOf('.')) >= 0 ) {
			short_name = name.substr(i+1);
		} else {
			short_name = name;
		}
		return new ClazzName(name,short_name,bytecode_name);
	}

	public static ClazzName fromOuterAndName(Struct outer, KString short_name) {
		if(short_name.equals(KString.Empty)) return Empty;
		KString bytecode_name;
		try {
			if( outer.isPackage() ) {
				if( !outer.name.name.equals(KString.Empty) )
					bytecode_name = KString.from(outer.name.bytecode_name+"/"+short_name);
				else
					bytecode_name = short_name;
			} else {
				bytecode_name = KString.from(outer.name.bytecode_name+"$"+short_name);
			}
		} catch(Exception e) {
			Kiev.reportError(0,e);
			bytecode_name = KString.from(outer.name.bytecode_name+"."+short_name);
		}
		KString name = replaceDollars(bytecode_name.replace('/','.'));
		return new ClazzName(name,short_name,bytecode_name);
	}

	public int hashCode() { return name.hashCode(); }

	public boolean equals(ClazzName nm) { return name.equals(nm.name); }

	public static KString fixLocalName(KString name) {
		int i=0;
		boolean fix_it = false;
		while( (i=name.indexOf((byte)'.',i)) >= 0
				&& Character.isDigit((char)name.byteAt(i+1)) ) {
			fix_it = true;
			break;
		}
		if( !fix_it)
			return name;
		KString.KStringScanner sc = new KString.KStringScanner(name);
		KStringBuffer sb = new KStringBuffer(name.len);
		fix_it = false;
		while(sc.hasMoreChars()) {
			char ch = sc.nextChar();
			if( ch == '.' ) {
				char pc = sc.peekChar();
				if( pc == '.' ) {
					// Case aaa..bbb -> aaa$$bbb
					sc.nextChar();
					sb.append_fast((byte)'$').append_fast((byte)'$');
				}
				else if( fix_it ) {
					// Case aaa.0.bbb -> aaa$0$bbb
					sb.append_fast((byte)'$');
					fix_it = false;
				}
				else if( Character.isDigit(pc) ) {
					// Case aaa.0 -> aaa$0, also flag previous case
					sb.append_fast((byte)'$');
					fix_it = true;
				}
				else
					sb.append_fast((byte)'.');
				continue;
			}
			if( ch == '$' )
				fix_it = false;
			sb.append(ch);
		}
		return sb.toKString();
	}

	public static KString replaceDollars(KString str) {
		KString.KStringScanner sc = new KString.KStringScanner(str);
		KStringBuffer sb = new KStringBuffer(str.len);
		while(sc.hasMoreChars()) {
			char ch = sc.nextChar();
			if( ch == '$' ) {
				if( sc.peekChar() != '$' && !Character.isDigit(sc.peekChar()) )
					sb.append_fast((byte)'.');
				else sb.append(ch).append(sc.nextChar());
				continue;
			}
			sb.append(ch);
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

	rule public equals(NodeName nm)
		pvar KString n;
	{
		{
			n ?= name;
			n @= aliases
		},
		nm.equals(n.$var)
	}

	rule public equals(KString nm)
		pvar KString n;
	{
		{
			n ?= name;
			n @= aliases
		},
		nm.equals(n.$var)
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
