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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/stdlib/PEnv.java,v 1.2 1998/10/21 19:44:45 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.2 $
 *
 */

/**
 *  PEnv emulates a virtual Prolog machine
 *	For rules it has a special stack of rule states
 *  This stack is holding rules searching (executing) tree
 *  Each rule allocates a frame for itself by makeframe(int max_states, int max_vars)
 *  call. A frame has next structure:
 *  bp -> frame size
 *        index of local vars in local_vars vector
 *        number of local vars
 *        previous rule bp
 *        previous rule pc
 *        saved my rule pc
 *        state 1
 *        state 2
 *        ...
 *  pc -> state N
 *        ....
 *
 *  Where:
 *  'bp' - base pointer (index) of frame (stack)
 *  'pc' - stack pointer (index) of current rule state
 *
 *  To enter into a rule interpreter does:
 *  1) for unification (first) enter:
 *     - allocates a frame state_stack vector
 *     - allocates a frame in local_vars vector
 *     - stores information about previous rule state
 *     - push information about new rule 'bp' and 'pc' into old frame
 *     - starts execution from state 0
 *  2) for backtracking (re-enter):
 *     - setup 'bp' and 'pc'
 *     - starts execution from state 'pc'
 *  To leave a rule interpreter does:
 *  1) Non-destructive leave (when rule returns 'true' and may be re-entered)
 *     - restores previous rule 'bp' and 'pc'
 *     - saves information about current (being leaved) rule in own rule frame
 *     - continues execution of previous rule
 *  2) Destructive leave (when rule returns 'false' and may not be re-entered)
 *     - restores previous rule 'bp' and 'pc'
 *     - clears and frees all frames and local vars down (inclusive) from unsuccesful one
 *     - continues execution of previous rule
 *  To call a rule state stack must hold this info:
 *        ...  previous states of current rule
 *  pc -> state N (a current rule state)
 *        'bp' of rule to call
 *        -1   a special pseudo rule-state that means end of call info
 *        ...  next states of current rule
 */

public final class PEnv {

	/** Debug mode flag */
	public static boolean	debug = false;

	public static boolean contains(Enumeration<Object> e, Object value) {
		foreach(Object val; e; val!=null && (val==value || val.equals(value)) )
			return true;
		return false;
	}

	public static boolean jcontains(java.util.Enumeration e, Object value) {
		foreach(Object val; e; val!=null && (val==value || val.equals(value)) )
			return true;
		return false;
	}
}

