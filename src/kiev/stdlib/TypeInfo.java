/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev library.

 The Kiev library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public License as
 published by the Free Software Foundation.

 The Kiev library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU Library General Public
 License along with the Kiev compiler; see the file License.  If not,
 write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.stdlib;

import java.util.StringTokenizer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import syntax kiev.stdlib.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface TypeInfoInterface {
	public TypeInfo getTypeInfoField();
}

public class TypeInfo {

	private static TypeInfo[] hashTable = new TypeInfo[1777];
	private static int        hashTableCount;
	
	public final Class			clazz;
	public final int			hash;
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


