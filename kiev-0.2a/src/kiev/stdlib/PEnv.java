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

	/** Method's states vector, it's logically splitted by frames' max_states values */
	public int[]			state_stack;
	
	/** Currently executed rule frame index */
	public int				bp;

	/** Currently exceuted rule state index */
	public int				pc;
	
	/** Total amunt of allocated states (sum of sizes of all frames */
	public int				allocated_states;
	
	/** Method's local vars vector, it's logically splitted by frames' max_vars values */
	public Object[]			local_vars;
	
	/** Total amunt of allocated local vars (sum of numbers of vars all frames (rules) */
	public int				allocated_vars;
	
	/** Convinient access to current rule state */
	public abstract virtual int state;
	
	public static final int FRAME_SIZE	= 0;
	public static final int LVARS_BASE	= 1;
	public static final int LVARS_NUMB	= 2;
	public static final int PREV_BP		= 3;
	public static final int PREV_PC		= 4;
	public static final int SAVED_PC	= 5;
	public static final int FIRST_STATE	= 6;
	public static final int SIZEOF_FRAME= 6;


	public static final int CALL_STATE	= 0;
	public static final int CALL_BP		= 1;
	public static final int CALL_END	= 2;
	public static final int CALL_STATE2	= 3;

	/** Construct a new PEnv object.
	 *  after construction it's in "initialized" state
	 */	
	public PEnv() {
		state_stack = new int[32];
		local_vars = new Object[8];
	}
	
	/** Reinitialize PEnv object */
	public PEnv init() {
		// Cleanup rule states
		int len = state_stack.length;
		for(int i=0; i < len; i++)	state_stack[i] = 0;
		// Cleanup local vars
		len = allocated_vars;
		for(int i=0; i < len; i++)
			local_vars[i] = null;

		// Init initial frame
		bp = 0;
		pc = 0;
		
		// Set allocated amounts
		allocated_states = 0; // index of next free state index
		allocated_vars = 0; // No vars was allocated
		
		if( debug ) System.out.println("PlDB: init: "+dumpStack());
		return this;
	}

	public int get$state() {
		return state_stack[pc];
	}

	public void set$state(int i) {
		state_stack[pc] = i;
		if( debug ) System.out.println("PlDB: set$state: pc:"+pc+" :"+i+" :"+dumpStack());
	}
	
	public void push(int i) {
		state_stack[++pc] = i;
		if( debug ) System.out.println("PlDB: push: pc:"+pc+" :"+i+" :"+dumpStack());
	}
	
	/** Pop rule state, return new state */
	public int pop() {
		state_stack[pc--] = 0;
		// Check we at call frame
		if( state_stack[pc] < 0 ) pc -= CALL_END;
		if( debug ) System.out.println("PlDB: pop: "+state_stack[pc]+" :"+dumpStack());
		return state_stack[pc];
	}
	
	/** Make unification call structure {0 0 -1} */
	public void unif_call() {
		// Fill unification entry
		state_stack[pc+CALL_BP]	=  allocated_states;
		state_stack[pc+CALL_END]= -1;
		if( debug ) System.out.println("PlDB: unification of rule");
	}

	/** Enter into rule
	 *  save state of previous rule in it's frame
	 *  create new frame if need
	 */
	public void enter(int max_states, int max_vars) {
		int newbp = state_stack[pc+CALL_BP];
		int newpc;
		if( newbp == 0 ) {
			if( state_stack[SAVED_PC] == 0 ) {
				// First enter aftre init
				newpc = 0;
			} else {
				if( pc == 0 ) {
					// Re-enter from 'foreach' statement
					newpc = state_stack[SAVED_PC];
				} else {
					// Call rule in non-reentrant mode
					newpc = 0;
				}
			}
		} else {
			newpc = state_stack[newbp+SAVED_PC];
		}
		int oldbp = bp;
		int oldpc = pc;
		if( debug ) System.out.println("PlDB: enter: "+max_states+"/"+max_vars+" - "+(newpc==0?"unification":"backtracking"));
		if( debug ) System.out.println("PlDB: enter: old bp:"+oldbp+",pc:"+oldpc+" -> new bp:"+newbp+",pc:"+newpc);
		// If pc in call frame is 0 - this is an unification entry
		if( newpc == 0 ) {
			// Allocate new frame
			newbp = allocated_states;
			newpc = allocated_states+SIZEOF_FRAME;
			allocated_states += max_states+SIZEOF_FRAME;
			if( allocated_states+SIZEOF_FRAME >= state_stack.length )
				state_stack = (int[])Arrays.ensureSize(state_stack,allocated_states+SIZEOF_FRAME);
			allocated_vars += max_vars;
			if( allocated_vars >= local_vars.length )
				local_vars = (Object[])Arrays.ensureSize(local_vars,allocated_vars);
			// Initialize new frame
			state_stack[newbp+FRAME_SIZE]	= max_states+SIZEOF_FRAME;	// Initial frame size + sizeof(frame)
			state_stack[newbp+LVARS_BASE]	= allocated_vars-max_vars;	// Index of local vars
			state_stack[newbp+LVARS_NUMB]	= max_vars;		// Number of local vars
			state_stack[newbp+PREV_BP]		= oldbp;		// Previous rule 'bp'
			state_stack[newbp+PREV_PC]		= oldpc;		// Previous rule 'pc'
			state_stack[newbp+SAVED_PC]		= newpc;		// Current rule 'pc'
			state_stack[newbp+FIRST_STATE]	= 0;			// Rule unification state
			// Save this frame info in previous call frame
			if( oldpc != 0 )
				state_stack[oldpc+CALL_BP]		= newbp;
		}
		bp = newbp;
		pc = newpc;
		if( debug ) System.out.println("PlDB: enter: "+dumpStack());
	}

	/** Debug version of enter
	 *  @param - rule's name and arity
	 */	
	public void enter(String msg, int max_states, int max_vars) {
		if( debug ) System.out.println(msg);
		enter(max_states, max_vars);
	}

	/** Inform the PEnv that last call to rule returns true */
	public void info_call_true() {
		// Resore previous frame, and save info about current frame
		int oldbp = bp;
		int oldpc = pc;
		bp = state_stack[oldbp+PREV_BP];
		pc = state_stack[oldbp+PREV_PC];
		state_stack[oldbp+SAVED_PC] = oldpc;
		if( debug ) System.out.println("PlDB: info true: old bp:"+oldbp+",pc:"+oldpc+" -> new bp:"+bp+",pc:"+pc);
		// Check that we were called in re-entrant mode
		if( state_stack[pc+CALL_END] < 0 ) {
			state_stack[pc+CALL_BP] = oldbp;
			// Set next state (state_stack[pc]%5==1 -> 2) Duplicate rule state over {bp pc -1}
			if( state_stack[pc]%5 == 1 ) state_stack[pc]++;
			state_stack[pc+CALL_END+1] = state_stack[pc];
			pc += CALL_END+1;
		} else {
			// Set next state (state_stack[pc]%5==1 -> 2)
			if( state_stack[pc]%5 == 1 ) state_stack[pc]++;
		}
		if( debug ) System.out.println("PlDB: info true: "+dumpStack());
	}

	/** Inform the PEnv that last call to rule returns false */
	public void info_call_false() {
		// Resore previous frame, and cleanup unsuccessfull frames
		int oldbp = bp;
		int oldpc = pc;
		bp = state_stack[oldbp+PREV_BP];
		pc = state_stack[oldbp+PREV_PC];
		state_stack[oldbp+SAVED_PC] = oldpc;
		if( debug ) System.out.println("PlDB: info false: old bp:"+oldbp+",pc:"+oldpc+" -> new bp:"+bp+",pc:"+pc);
		// Cleanup local vars
		for(int i=state_stack[oldbp+LVARS_BASE]; i < allocated_vars; i++)
			local_vars[i] = null;
		allocated_vars = state_stack[oldbp+LVARS_BASE];
		// Cleanup state stack
		for(int i=oldbp; i < allocated_states; i++)
			state_stack[i] = 0;
		allocated_states = oldbp;
		// Check that we were called in re-entrant mode
		if( state_stack[pc+CALL_END] < 0 ) {
			// Cleanup call frame
			state_stack[pc+1] = 0;
			state_stack[pc+2] = 0;
		}
		if( debug ) System.out.println("PlDB: info false: "+dumpStack());
	}
	
	public Object getVar(int i) {
		Object v = local_vars[i+state_stack[bp+LVARS_BASE]];
		if( debug ) System.out.println("PlDB: getvar: "+i+" : "+(i+state_stack[bp+LVARS_BASE])+": "+v);
		return v;
	}
	
	public void setVar(int i, Object v) {
		local_vars[i+state_stack[bp+LVARS_BASE]] = v;
		if( debug ) System.out.println("PlDB: setvar: "+i+" : "+(i+state_stack[bp+LVARS_BASE])+": "+v);
	}
	
	private String dumpStack() {
		StringBuffer sb = new StringBuffer();
		sb.append("\nstack is {");
		int fr = 0;
		while( fr < allocated_states ) {
			dumpFrame(sb,fr);
			fr += state_stack[fr];
			if( allocated_states <= fr ) break;
			if( state_stack[fr] == 0 ) break;
		}
		sb.append('}').append("/").append(allocated_states).append("/").append(allocated_vars);
		sb.append('{');
		while( fr  < state_stack.length ) sb.append(' ').append(state_stack[fr++]);
		sb.append('}');
		return sb.toString();
	}
	
	private void dumpFrame(StringBuffer sb, int frame_bp) {
		sb.append('{');
		sb.append(" sz:").append(state_stack[frame_bp+FRAME_SIZE]);
		sb.append(" lv:").append(state_stack[frame_bp+LVARS_BASE]);
		sb.append(" nv:").append(state_stack[frame_bp+LVARS_NUMB]);
		sb.append(" bp:").append(state_stack[frame_bp+PREV_BP]);
		sb.append(" pc:").append(state_stack[frame_bp+PREV_PC]);
		sb.append(" sv:").append(state_stack[frame_bp+SAVED_PC]);
		sb.append(" :");
		for(int i=FIRST_STATE; i < state_stack[frame_bp]; i++) {
			sb.append(state_stack[frame_bp+i]);
			if( frame_bp+i == pc )
				sb.append('!');
			else
				sb.append(' ');
		}
		sb.append('}');
	}

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

