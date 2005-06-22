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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/stdlib/ArrayEnumerator.java,v 1.2.4.2 1999/05/29 21:03:10 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.2.4.2 $
 *
 */

public class ArrayEnumerator<A> implements Enumeration<A>, Cloneable
	$generate <boolean>,<byte>,<char>,<short>,<int>,<long>,<float>,<double>
{

	private A[]		arr;
	private int		top;
	
	public ArrayEnumerator(A[] arr) {
		this.arr = arr;
	}
	
	public boolean	hasMoreElements() {
		return arr != null && top < arr.length;
	}
	public A		nextElement() {
		return arr[top++];
	}
	
	public static boolean contains(A[] ar, A val) {
		for(int i=0; i < ar.length; i++) {
			if( A instanceof Object )
				if( val!=null && (val == ar[i] || val.equals(ar[i])) )
					return true;
			else
				if( val.equals(ar[i]) )
					return true;
		}
		return false;
	}
/*
	public static boolean contains(int[] ar, int val) {
		for(int i=0; i < ar.length; i++)
			if( val == ar[i] )
				return true;
		return false;
	}

	public static boolean contains(long[] ar, long val) {
		for(int i=0; i < ar.length; i++)
			if( val == ar[i] )
				return true;
		return false;
	}

	public static boolean contains(float[] ar, float val) {
		for(int i=0; i < ar.length; i++)
			if( val == ar[i] )
				return true;
		return false;
	}

	public static boolean contains(double[] ar, double val) {
		for(int i=0; i < ar.length; i++)
			if( val == ar[i] )
				return true;
		return false;
	}

	public static boolean contains(boolean[] ar, boolean val) {
		for(int i=0; i < ar.length; i++)
			if( val == ar[i] )
				return true;
		return false;
	}

	public static boolean contains(byte[] ar, byte val) {
		for(int i=0; i < ar.length; i++)
			if( val == ar[i] )
				return true;
		return false;
	}

	public static boolean contains(char[] ar, char val) {
		for(int i=0; i < ar.length; i++)
			if( val == ar[i] )
				return true;
		return false;
	}

	public static boolean contains(short[] ar, short val) {
		for(int i=0; i < ar.length; i++)
			if( val == ar[i] )
				return true;
		return false;
	}
*/
}

