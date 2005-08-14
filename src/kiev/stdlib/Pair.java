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

/** a class for pairs
 * $Header: /home/CVSROOT/forestro/kiev/kiev/stdlib/Pair.java,v 1.2.4.2 1999/05/29 21:03:10 max Exp $
 * @author      Martin Odersky
 * @author      Maxim Kizub
 * @version $Revision: 1.2.4.2 $
 *
 */

public class Pair<A,B>
	$generate
		<int,int>,<int,long>,<int,float>,<int,double>,<int,B>,
		<long,int>,<long,long>,<long,float>,<long,double>,<long,B>,
		<float,int>,<float,long>,<float,float>,<float,double>,<float,B>,
		<double,int>,<double,long>,<double,float>,<double,double>,<double,B>,
		<A,int>,<A,long>,<A,float>,<A,double>
{

	public A fst;
	public B snd;
    
	public Pair(A fst, B snd) {
		this.fst = fst;
		this.snd = snd;
	}

/**
 * a hashcode for this pair.
 */
	public int hashCode() {
		int f, s;

		if( A instanceof Object ) f = fst == null ? 1 : fst.hashCode();
		else f = fst.hashCode();

		if( B instanceof Object ) s = snd == null ? 1 : snd.hashCode();
		else s = snd.hashCode();

		return f * s;
	}

/** a binary equality method that also works for `null' operands
 */
	private static boolean equals(Object x, Object y) {
		return (x == null && y == null) || (x != null && x.equals(y));
	}

/** is this pair (structurally) equal to another pair?
 */
	public boolean equals(Object other) {
		if (other instanceof Pair<A,B>) {
			Pair<A,B> o = (Pair<A,B>)other;
			if( A instanceof Object ) {
				if( !equals(fst,o.fst) ) return false;
			} else {
				if( fst != o.fst ) return false;
			}
			if( B instanceof Object ) {
				if( !equals(snd,o.snd) ) return false;
			} else {
				if( snd != o.snd ) return false;
			}
			return true;
		} else {
			return false;
		}
	}

/** a string representation of this pair
 */
	public String toString() {
		return "Pair(" + fst + ", " + snd + ")";
	}
}
