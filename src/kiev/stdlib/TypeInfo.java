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

import java.util.StringTokenizer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectInput;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface TypeInfoInterface {
	public TypeInfo getTypeInfoField();
}

public class TypeInfo implements Externalizable {

	private static TypeInfo[] hashTable = new TypeInfo[1777];
	private static int        hashTableCount;
	
	final static class TypeInfoSerialized implements Externalizable {
		static final long serialVersionUID = 7933549803914250163L;
		Class ti_clazz;
		Class clazz;
		TypeInfo[] args;
		public TypeInfoSerialized() {}
		TypeInfoSerialized(Class ti_clazz, Class clazz, TypeInfo[] args) {
			this.ti_clazz = ti_clazz;
			this.clazz = clazz;
			this.args = args;
		}
		public Object readResolve() {
			//if (ti_clazz != TypeInfo.class)
			//	System.out.println("Serialization readResolve "+ti_clazz);
			Method m = ti_clazz.getDeclaredMethod("newTypeInfo", Class.class, TypeInfo[].class);
			return m.invoke(null,new Object[]{this.clazz,this.args});
		}
		public final void writeExternal(ObjectOutput out) throws IOException {
			//System.out.println("Serialization writeExternal "+ti_clazz);
			out.writeObject(this.ti_clazz.getName());
			out.writeObject(this.clazz.getName());
			out.writeObject(this.args);
		}
		
		public final void readExternal(ObjectInput in) throws IOException {
			ClassLoader cl = this.getClass().getClassLoader();
			String ti_clazz_name = (String)in.readObject();
			//System.out.println("Serialization readExternal "+ti_clazz_name);
			String clazz_name = (String)in.readObject();
			this.args = (TypeInfo[])in.readObject();
			this.ti_clazz = Class.forName(ti_clazz_name, false, cl);
			this.clazz = Class.forName(clazz_name, false, cl);
		}
	}

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
	
	public static TypeInfo newTypeInfo(String ti_str) {
		StringTokenizer st = new StringTokenizer(ti_str, "«;»", true);
		TypeInfo ti = parseTypeInfo(st, st.nextToken().trim(), ti_str);
		if (st.hasMoreTokens())
			throw new RuntimeException("Bad TypeInfo signature: "+ti_str);
		return ti;
	}
	
	public static TypeInfo makeTypeInfo(Class clazz, TypeInfo[] args) {
		Class ti_clazz;
		try {
			ti_clazz = Class.forName(clazz.getName() + "$__ti__");
		} catch (ClassNotFoundException e) {
			return TypeInfo.newTypeInfo(clazz, null);
		}
		if (args == null)
			return (TypeInfo)ti_clazz.getDeclaredMethod("newTypeInfo", new Class[0]).invoke(null,new Object[0]);
		else
			return (TypeInfo)ti_clazz.getDeclaredMethod("newTypeInfo", Class.class, TypeInfo[].class).invoke(null,clazz,args);
	}
	
	private static TypeInfo parseTypeInfo(StringTokenizer st, String first_token, String ti_str) {
		Class clazz = Class.forName(first_token);
		if (!st.hasMoreTokens())
			return makeTypeInfo(clazz,null);
		String t = st.nextToken().trim();
		if (t.equals(";"))
			return makeTypeInfo(clazz,null);
		if (!t.equals("«"))
			throw new RuntimeException("Bad TypeInfo signature: "+ti_str);
		TypeInfo[] args = null;
		do {
			t = st.nextToken().trim();
			if (t.equals("»")) {
				if (st.hasMoreTokens()) {
					t = st.nextToken().trim();
					if (!t.equals(";"))
						throw new RuntimeException("Bad TypeInfo signature: "+ti_str);
				}
				return makeTypeInfo(clazz,args);
			}
			TypeInfo arg = parseTypeInfo(st, t, ti_str);
			if (args == null)
				args = new TypeInfo[]{arg};
			else
				args = (TypeInfo[])Arrays.append(args, arg);
		} while (st.hasMoreTokens());
		throw new RuntimeException("Bad TypeInfo signature: "+ti_str);
	}
	
	public TypeInfo[] getTopArgs() {
		return null;
	}

	public final int hashCode() {
		return hash;
	}

	public String toString() {
		TypeInfo[] args = getTopArgs();
		if (args == null || args.length == 0)
			return clazz.getName();
		StringBuffer sb = new StringBuffer();
		sb.append(clazz.getName());
		sb.append("«");
		for (int i=0; i < args.length; i++) {
			sb.append(args[i].toString());
			sb.append(";");
		}
		sb.append("»");
		return sb.toString();
	}
	
	public final void writeExternal(ObjectOutput out) throws IOException {
		//System.out.println("Serialization writeExternal "+this.getClass());
		throw new IOException("TypeInfo.writeExternal()");
	}
	
	public final void readExternal(ObjectInput in) throws IOException {
		//System.out.println("Serialization readExternal "+this.getClass());
		throw new IOException("TypeInfo.readExternal()");
	}
	
	public final Object writeReplace() {
		//if (this.getClass() != TypeInfo.class)
		//	System.out.println("Serialization writeReplace "+this.getClass());
		return new TypeInfoSerialized(this.getClass(), this.clazz, this.getTopArgs());
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
		//if( obj == null ) return false;
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


