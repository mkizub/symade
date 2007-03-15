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
package kiev.be.java15;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 703 $
 *
 */

class ClazzName implements Constants {

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
			if (outer.qname() != "") {
				bytecode_name = KString.from(((JStruct)outer).bname()+delim+short_name);
				name = KString.from(outer.qname()+"."+short_name);
			} else {
				bytecode_name = short_name;
				name = short_name;
			}
		} else {
			assert(isInn,"fromOuterAndName("+outer+","+short_name+","+isInn+")");
			bytecode_name = KString.from(((JStruct)outer).bname()+delim+short_name);
			name = KString.from(outer.qname()+"."+short_name);
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
				String tmp = str.substr(0,sc.pos-1).toString().intern();
				byte b = (byte)(Env.existsStruct(tmp)?'.':'$');
				sb.append_fast(b);
			} else {
				sb.append(ch);
			}
		}
		return sb.toKString();
	}
}

