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

import java.util.StringTokenizer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.io.Serializable;

import syntax kiev.stdlib.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface TypeInfoInterface {
	public TypeInfo getTypeInfoField();
}

public class TypeInfo implements Serializable {

	private static TypeInfo[] hashTable = new TypeInfo[1777];
	private static int        hashTableCount;
	
	public final Class			clazz;
	public final int			hash;

	transient
	private      TypeInfo		next; // for hashTable

	protected TypeInfo(int hash, Class clazz) {
		this.clazz = clazz;
		this.hash = hash;
		put(hash, this);
	}

	public static TypeInfo newTypeInfo(Class clazz, TypeInfo[] args) {
		int hash = hashCode(clazz, args);
		TypeInfo ti = get(hash,clazz,null);
		if (ti == null)
			ti = new TypeInfo(hash, clazz);
		return ti;
	}

	public final int hashCode() {
		return hash;
	}

	public String toString() {
		return clazz.getName();
	}

	public Object readResolve() {
		return TypeInfo.newTypeInfo(this.clazz, null);
	}

	public boolean eq(Class clazz, TypeInfo[] args) {
		return this.clazz == clazz && args == null;
	}

	// ti instanceof List<A>.type
	public boolean $assignableFrom(TypeInfo ti) {
		return this.clazz.isAssignableFrom(ti.clazz);
	}

	// obj instanceof List<A>.type
	public boolean $instanceof(Object obj) {
		if( obj == null ) return false;
		return clazz.isInstance(obj);
	}

	// ($cast List<A>)obj
	public final Object $checkcast(Object obj) {
		if( obj == null ) return obj;
		if ($instanceof(obj))
			return obj;
		throw new ClassCastException(obj.getClass().getName());
	}

	public Object newInstance() {
		return clazz.newInstance();
	}

	public Object newArray(int size) {
		return java.lang.reflect.Array.newInstance(clazz,size);
	}

	public Object newArray(int[] dims) {
		return java.lang.reflect.Array.newInstance(clazz,dims);
	}
	
	public static int hashCode(Class clazz, TypeInfo[] args) {
		int hash  = clazz.hashCode();
		if (args != null) {
			for (int i=0; i < args.length; i++)
				hash = hash * 37 + args[i].hash;
		}
		return hash;
	}

	public static TypeInfo get(int hash, Class clazz, TypeInfo[] args) {
		int index = (hash & 0x7FFFFFFF) % hashTable.length;
		for (TypeInfo ti = hashTable[index]; ti != null; ti = ti.next) {
			if (ti.eq(clazz, args))
				return ti;
		}
		return null;
	}
	
	public static void put(int hash, TypeInfo ti) {
		int index = (hash & 0x7FFFFFFF) % hashTable.length;
		ti.next = hashTable[index];
		hashTable[index] = ti;
	}
}


