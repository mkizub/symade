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
 * @author      Martin Odersky
 * @author      Maxim Kizub
 * @version $Revision$
 *
 */

public class Pair<A,B>
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

		f = fst == null ? 1 : fst.hashCode();

		s = snd == null ? 1 : snd.hashCode();

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
			if( !equals(fst,o.fst) ) return false;
			if( !equals(snd,o.snd) ) return false;
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
