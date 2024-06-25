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
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 254 $
 *
 */

class ClazzName implements Constants {

	/** Unqualified name of class/package/interface
		for java bytecode example - Object or tMethod
	 */
	public String			src_name;

	/** Full-qualified name of class/package/interface
		for java bytecode example - java/lang/Object or kiev/Type$tMethod
	 */
	public String			bytecode_name;

	public String			name; // source code name, may be null for anonymouse symbols
	public String			unq_name; // unique name in scope, never null, usually equals to name

	public static ClazzName		Empty = new ClazzName("","","","");

	public String toString() {
		return name;
	}

	public ClazzName() {}
	public ClazzName(String qualified_name, String src_name, String bytecode_name, String uniq_name) {
		this.name = qualified_name;
		this.src_name = src_name;
		this.bytecode_name = bytecode_name;
		this.unq_name = uniq_name;
	}

	public String package_name() {
		int i;
		if( (i=name.lastIndexOf('.')) >= 0 )
			return name.substring(0,i);
		else
			return "";
	}

	public String package_bytecode_name() {
		int i;
		if( (i=name.lastIndexOf('.')) >= 0 )
			return bytecode_name.substring(0,i);
		else
			return "";
	}

	public static ClazzName fromSignature(JEnv jenv, String signature) {
		if(signature.equals("")) return Empty;
		String bytecode_name = signature.substring(1,signature.length()-2);
		return fromBytecodeName(jenv,bytecode_name);
	}

	public static ClazzName fromToplevelName(JEnv jenv, String name) {
		if(name.equals("")) return Empty;
		String bytecode_name = name.replace('.','/');
		return fromBytecodeName(jenv,bytecode_name);
	}

	public static ClazzName fromBytecodeName(JEnv jenv, String bytecode_name) {
		if(bytecode_name.equals("")) return Empty;
		String name = bytecode_name.replace('/','.');
		name = fixName(jenv, name);
		int i;
		String short_name;
		if( (i=name.lastIndexOf('.')) >= 0 )
			short_name = name.substring(i+1);
		else
			short_name = name;
		return new ClazzName(name,short_name,bytecode_name,short_name);
	}

	public static ClazzName fromOuterAndName(DNode outer, String short_name) {
		if(short_name.equals("")) return Empty;
		String delim = (outer instanceof KievPackage) ? "/" : "$" ;
		String bytecode_name;
		String name;
		if (outer instanceof KievPackage) {
			if (outer.qname() != "") {
				bytecode_name = outer.qname().replace('·','/')+delim+short_name;
				name = outer.qname().replace('·','.')+"."+short_name;
			} else {
				bytecode_name = short_name;
				name = short_name;
			}
		} else {
			outer = (ComplexTypeDecl)outer;
			if (outer.bytecode_name == null)
				bytecode_name = outer.qname().replace('·','/')+delim+short_name;
			else
				bytecode_name = outer.bytecode_name+delim+short_name;
			name = outer.qname().replace('·','.')+"."+short_name;
		}
		return new ClazzName(name,short_name,bytecode_name,short_name);
	}

	public int hashCode() { return name.hashCode(); }

	public boolean equals(ClazzName nm) { return name == nm.name; }

	private static String fixName(JEnv jenv, String str) {
		if (str.indexOf('$') < 0)
			return str;
		StringBuffer sb = new StringBuffer(str.length());
		for (int i=0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if( ch == '$' ) {
				String tmp = str.substring(0,i).replace('.','·');
				ch = (jenv.env.existsTypeDecl(tmp)?'.':'$');
			}
			sb.append(ch);
		}
		return sb.toString();
	}
}

