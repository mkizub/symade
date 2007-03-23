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
