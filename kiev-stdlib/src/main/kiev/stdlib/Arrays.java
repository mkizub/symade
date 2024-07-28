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

public final class Arrays {

	public static Object cloneToSize(Object arr, int i) {
//		int len = java.lang.reflect.Array.getLength(arr);
         	Object tmp = java.lang.reflect.Array.newInstance(
					arr.getClass().getComponentType(),i);
		return copy(arr,tmp);
	}

	public static Object incr(Object arr, int i) {
         	return cloneToSize(arr,java.lang.reflect.Array.getLength(arr)+i);
	}

	public static Object incr(Object arr) {
         	return cloneToSize(arr,java.lang.reflect.Array.getLength(arr)+1);
	}

	public static Object ensureSize(Object arr, int i) {
		int len = java.lang.reflect.Array.getLength(arr);
         	if( len >= i ) return arr;
		return cloneToSize(arr,i);
	}

	public static Object copy(Object src, Object tag) {
		int len_src = java.lang.reflect.Array.getLength(src);
		int len_tag = java.lang.reflect.Array.getLength(tag);
		int len = len_src > len_tag ? len_tag: len_src;
		System.arraycopy(src,0,tag,0,len);
		return tag;
	}

	public static boolean contains(Object arr, Object val) {
		return indexOf(arr,val) != -1;
	}

	public static int indexOf(Object arr, Object val) {
		int len = java.lang.reflect.Array.getLength(arr);
		for(int i=0; i < len; i++) {
			Object o = java.lang.reflect.Array.get(arr,i);
			if( o==val || (o!=null && o.equals(val)) )
				return i;
		}
		return -1;
	}

	public static Object append(Object arr, Object val) {
		arr = incr(arr);
		java.lang.reflect.Array.set(arr,java.lang.reflect.Array.getLength(arr)-1,val);
		return arr;
	}

	public static Object appendUniq(Object arr, Object val) {
		if( contains(arr,val) ) return arr;
		return append(arr,val);
	}

	public static Object insert(Object arr, Object val, int pos) {
		int len = java.lang.reflect.Array.getLength(arr);
		if( pos > len )
			throw new ArrayIndexOutOfBoundsException(String.valueOf(pos));
		Object tmp = java.lang.reflect.Array.newInstance(
			arr.getClass().getComponentType(),len+1);
		if( pos > 0 )
			System.arraycopy(arr,0,tmp,0,pos);
		java.lang.reflect.Array.set(tmp,pos,val);
		if( pos < len )
			System.arraycopy(arr,pos,tmp,pos+1,len-pos);
		return tmp;
	}
	
	public static Object remove(Object arr, int pos) {
		int len = java.lang.reflect.Array.getLength(arr);
		if( pos >= len )
			throw new ArrayIndexOutOfBoundsException(String.valueOf(pos));
		Object tmp = java.lang.reflect.Array.newInstance(
			arr.getClass().getComponentType(),len-1);
		if( pos > 0 )
			System.arraycopy(arr,0,tmp,0,pos);
		if( pos < len-1 )
			System.arraycopy(arr,pos+1,tmp,pos,len-pos-1);
		return tmp;
	}
	
	public static String toString(Object arr) {
		if( arr == null ) return String.valueOf(arr);
		int len = java.lang.reflect.Array.getLength(arr);
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		for(int i=0; i < len; i++) {
			sb.append(java.lang.reflect.Array.get(arr,i));
			if( i < len-1 ) sb.append(',');
		}
		sb.append('}');
		return sb.toString();
	}

}