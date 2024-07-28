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
package kiev.stdlib;

import syntax kiev.stdlib.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 213 $
 *
 */

class TypeSwitchHashEntry {

	TypeSwitchHashEntry		next;		// Next entry with the same hash
	Class					clazz;		// Class (Type)
	int						index;		// index in switch statement for this clazz;

	TypeSwitchHashEntry(Class clazz, int index) {
		this.clazz = clazz;
		this.index = index;
	}
}

public class TypeSwitchHash {

	private TypeSwitchHashEntry[]	table;
	private String[]				signs;
	private Class[]					types;
	private int						defindex;

	public TypeSwitchHash(String[] signs, int defindex) {
		this.signs = signs;
		this.defindex = defindex;
	}

	public TypeSwitchHash(Class[] types, int defindex) {
		this.types = types;
		this.defindex = defindex;
	}

	public static int index(Object obj, TypeSwitchHash tsh) {
		return tsh.index(obj);
	}
	public int index(Object obj) {
		if( obj == null ) return defindex;
		Class clazz = obj.getClass();
		int hash = clazz.hashCode() & 0x7FFFFFFF;
		if( table == null ) makeClazzHash();
		TypeSwitchHashEntry he = table[hash % table.length];
		for(; he != null; he = he.next )
			if( he.clazz == clazz ) return he.index;
		// Not found, search super-classes
		for(;;) {
			clazz = clazz.getSuperclass();
			if( clazz == null ) return addClazz(obj.getClass(),defindex);
			hash = clazz.hashCode() & 0x7FFFFFFF;
			for( he = table[hash % table.length]; he != null; he = he.next)
				if( he.clazz == clazz ) return addClazz(obj.getClass(), he.index);
		}
	}
	
	private int addClazz(Class clazz, int ind) {
		int hash = clazz.hashCode() & 0x7FFFFFFF;
		TypeSwitchHashEntry he = table[hash % table.length];
		if( he == null ) {
			he = new TypeSwitchHashEntry(clazz,ind);
			table[hash % table.length] = he;
		} else {
			for(; he.next != null; he = he.next );
			he.next = new TypeSwitchHashEntry(clazz,ind);
			he = he.next;
		}
		return he.index;
	}
	
	private void makeClazzHash() {
		if (signs != null) {
			int n = signs.length*2+1;
			if( n < 17 ) n = 17;
			table = new TypeSwitchHashEntry[n];
			for(int i=0; i < signs.length; i++) {
				Class clazz = Class.forName(signs[i], false, this.getClass().getClassLoader());
				addClazz(clazz,i);
			}
			signs = null;
		} else {
			int n = types.length*2+1;
			if( n < 17 ) n = 17;
			table = new TypeSwitchHashEntry[n];
			for(int i=0; i < types.length; i++)
				addClazz(types[i],i);
			types = null;
		}
	}
}

