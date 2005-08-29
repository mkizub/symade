/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.*;
import kiev.stdlib.*;
import kiev.vlang.Instr.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class CodeLabel implements Constants {
	public static CodeLabel[] emptyArray = new CodeLabel[0];

	/** PC position of this label */
	public int			pc = -1;

	/** List of instructions that needs this label be setted up */
//	public short[]		instrs;

	/** Stack state expected at this label */
	public Type[]	stack;

	/** If check state does not allowed */
	public boolean	check = true;

	public CodeLabel() {
		stack = null;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("label at ").append(pc);
//		if( instrs != null ) {
//			sb.append("<-");
//			for(int i=0; i < instrs.length; i++) sb.append(instrs[i]).append(',');
//		}
		return sb.toString();
	}

	public void addInstr() {
		// Instruction is added AFTER it was placed in stack
		// so, we must set instruction's PC at current PC-2-1
		// (short argument of offset to jump plus the instruction itself)
//		if( instrs == null ) {
//			instrs = new short[]{(short)Code.pc};
//		} else {
//			short[] newinstrs = new short[instrs.length+1];
//			for(int j=0; j < instrs.length; j++) newinstrs[j] = instrs[j];
//			newinstrs[instrs.length] = (short)Code.pc;
//			instrs = newinstrs;
//		}
		if( !check ) return;
		if( stack == null ) {
			if( Code.top == 0 )
				stack = Type.emptyArray;
			else
				stack = (Type[])Arrays.cloneToSize(Code.stack,Code.top);
		} else {
			if( stack.length != Code.top )
				throw new RuntimeException("Stack depth at "+this+" does not match stack depth of instruction ("+stack.length+" != "+Code.top+")");
			for(int i=0; i < stack.length; i++) {
				if( stack[stack.length-i-1] != Code.stack_at(i) ) {
					Type tl = stack[stack.length-i-1];
					Type tc = Code.stack_at(i);
					// Check for object/null, or one is child of other
					if( tl.isReference() && tc.isReference() ) {
						if( tl == Type.tpNull || tl.isInstanceOf(tc) ) continue;
						if( tc == Type.tpNull || tc.isInstanceOf(tl) ) {
							Code.set_stack_at(tl,i);
							continue;
						}
					}
					throw new RuntimeException("Stack contentce at "+this+" does not match stack contentce of instruction at stack pos "+i+" ("+stack[stack.length-i-1]+" != "+Code.stack_at(i)+")");
				}
			}
		}
	}

	public void attachPosition() {
		pc = Code.pc;
		if( !check ) return;
		// Check if we at unreachable Code (after goto, return, throw)
		if( !Code.reachable ) {
			// This point is unreachable by normal flow control,
			// just setup stack to be correct
			if( stack == null ) {
				trace(Kiev.debugInstrGen,"Stack state at unreacheable code is undefined, assuming current");
				stack = (Type[])Arrays.cloneToSize(Code.stack,Code.top);
			} else {
				trace(Kiev.debugInstrGen,"Attaching label at unreachable code, stack depth is "+stack.length);
				Code.top = stack.length;
				System.arraycopy(stack,0,Code.stack,0,stack.length);
				for(int i=Code.top; i < Code.stack.length; i++) {
					if( Code.stack[i] == null ) break;
					Code.stack[i] = null;
				}
			}
		} else {
			// This point is reachable by normal flow control, check stack state
			if( stack == null ) {
				stack = (Type[])Arrays.cloneToSize(Code.stack,Code.top);
			} else {
				if( stack.length != Code.top )
					throw new RuntimeException("Stack depth at "+this+" does not match stack depth of instruction ("+stack.length+" != "+Code.top+")");
				for(int i=0; i < stack.length; i++) {
					if( !stack[stack.length-i-1].equals(Code.stack_at(i)) ) {
						Type tl = stack[stack.length-i-1];
						Type tc = Code.stack_at(i);
						// Check for object/null, or one is child of other
						if( tl.isReference() && tc.isReference() ) {
							if( tl == Type.tpNull || tl.isInstanceOf(tc) ) continue;
							if( tc == Type.tpNull || tc.isInstanceOf(tl) ) {
								Code.set_stack_at(tl,i);
								continue;
							}
							Type lct = Type.leastCommonType(tl,tc);
							if( lct != null ) {
								Code.set_stack_at(lct,i);
								continue;
							}
						}
						else if( tl.isIntegerInCode() && tc.isIntegerInCode() ) {
							Code.set_stack_at(Type.tpInt,i);
							continue;
						}
						throw new RuntimeException("Stack contentce at "+this+" does not match stack contentce of instruction at stack pos "+i+" ("+stack[stack.length-i-1]+" != "+Code.stack_at(i)+")");
					}
				}
			}
		}
	}
}

public abstract class CodeSwitch {
	public abstract void addCase(int val, CodeLabel l) throws RuntimeException;
	public abstract void addDefault(CodeLabel l) throws RuntimeException;
	public abstract void close() ;
//	public abstract void close2(short[] pos_map) ;
}


public class CodeTableSwitch extends CodeSwitch {
	public int	pc;	// the PC of opc_tableswitch
	public int	def_pc;	// the PC of default label
	public int	lo;	// bottom value
	public int	hi;	// top value
	public CodeLabel[] cases;
	public CodeLabel   def_case;	// default case
	public CodeLabel   end_label;	// end of switch

	public CodeTableSwitch(int lo, int hi) {
		this.lo = lo;
		this.hi = hi;
		cases = new CodeLabel[hi-lo+1];
		end_label = Code.newLabel();
	}

	public void addCase(int val, CodeLabel l) {
		if( val < lo || val > hi )
			throw new RuntimeException("Case with value "+val+" added, but tableswitch accepts only values in range from "+lo+" to "+hi);
		if( cases[val-lo] != null )
			throw new RuntimeException("Case with value "+val+" added, but this case already defined for tableswitch");
		cases[val-lo] = l;

	}

	public void addDefault(CodeLabel l) {
		if( def_case != null )
			throw new RuntimeException("Default Case added, but this case already defined for tableswitch");
		def_case = l;
	}

	public void close() {
		end_label.attachPosition();
		if( def_case == null ) {
			def_case = end_label;
		}
		int code_pc = Code.pc;
		end_label.pc = code_pc;
		Code.pc = def_pc;
		Code.add_code_int( def_case.pc-this.pc );
		Code.add_code_int( lo );
		Code.add_code_int( hi );
		trace(Kiev.debugAsmGen,"\ttable switch: from "+lo+" to "+hi+" default pc="+def_case.pc);
		for(int i=0; i <= hi-lo; i++)
			if( cases[i] != null )
				Code.add_code_int( cases[i].pc-this.pc );
			else
				Code.add_code_int( def_case.pc-this.pc );
		Code.pc = code_pc;
	}
}

public class CodeLookupSwitch extends CodeSwitch {
	public int	pc;	// the PC of opc_tableswitch
	public int	def_pc;	// the PC of default label
	public int[]	tags;	// number of tags in table
	public CodeLabel[] cases;
	public CodeLabel   def_case;	// default case
	public CodeLabel   end_label;	// end of switch

	public CodeLookupSwitch(int[] tags) {
		this.tags = tags;
		cases = new CodeLabel[tags.length];
		end_label = Code.newLabel();
	}

	public void addCase(int val, CodeLabel l) {
		for(int i=0; i <tags.length; i++) {
			if( tags[i] == val ) {
				cases[i] = l;
				return;
			}
		}
		throw new RuntimeException("Tag with value "+val+" not found in lookup table");
	}

	public void addDefault(CodeLabel l) {
		if( def_case != null )
			throw new RuntimeException("Default Case added, but this case already defined for lookupswitch");
		def_case = l;
	}

	public void close() {
		end_label.attachPosition();
		if( def_case == null ) def_case = end_label;
		int code_pc = Code.pc;
		end_label.pc = code_pc;
		Code.pc = def_pc;
		Code.add_code_int( def_case.pc-this.pc );
		Code.add_code_int( tags.length );
		for(int i=0; i < tags.length; i++) {
			Code.add_code_int(tags[i]);
			if( cases[i] != null )
				Code.add_code_int( cases[i].pc-this.pc );
			else
				Code.add_code_int( def_case.pc-this.pc );
		}
		Code.pc = code_pc;
	}
}

public class CodeCatchInfo {
	public static CodeCatchInfo[] emptyArray = new CodeCatchInfo[0];

	public int			start_pc;
	public int			end_pc;
	public CodeLabel	handler;
	public Type			type;

	public CodeCatchInfo(CodeLabel handler, Type type) {
		this.handler = handler;
		this.type = type;
	}
}


