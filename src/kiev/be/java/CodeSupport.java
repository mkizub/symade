package kiev.be.java;

import kiev.Kiev;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class CodeVar {

	public JVarView		var;
	public int			stack_pos = -1;
	public int			start_pc = -1;
	public int			end_pc = -1;
	public int			index = 0;

	public CodeVar(JVarView var) {
		this.var = var;
		stack_pos = var.bcpos;
	}

	public String toString() {
		return "("+stack_pos+","+index+","+start_pc+","+end_pc+")";
	}

}

public class CodeLabel implements JConstants {
	public static CodeLabel[] emptyArray = new CodeLabel[0];

	public final Code	code;
	
	/** PC position of this label */
	public int			pc = -1;

	/** List of instructions that needs this label be setted up */
//	public short[]		instrs;

	/** Stack state expected at this label */
	public JType[]	stack;

	/** If check state does not allowed */
	public boolean	check = true;

	public CodeLabel(Code code) {
		this.code = code;
		this.stack = null;
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
//			instrs = new short[]{(short)code.pc};
//		} else {
//			short[] newinstrs = new short[instrs.length+1];
//			for(int j=0; j < instrs.length; j++) newinstrs[j] = instrs[j];
//			newinstrs[instrs.length] = (short)code.pc;
//			instrs = newinstrs;
//		}
		if( !check ) return;
		if( stack == null ) {
			if( code.top == 0 )
				stack = JType.emptyArray;
			else
				stack = (JType[])Arrays.cloneToSize(code.stack,code.top);
		} else {
			if( stack.length != code.top )
				throw new RuntimeException("Stack depth at "+this+" does not match stack depth of instruction ("+stack.length+" != "+code.top+")");
			for(int i=0; i < stack.length; i++) {
				if( stack[stack.length-i-1] != code.stack_at(i) ) {
					JType tl = stack[stack.length-i-1];
					JType tc = code.stack_at(i);
					// Check for object/null, or one is child of other
					if( tl.isReference() && tc.isReference() ) {
						if( tl == JType.tpNull || tl.isInstanceOf(tc) ) continue;
						if( tc == JType.tpNull || tc.isInstanceOf(tl) ) {
							code.set_stack_at(tl,i);
							continue;
						}
					}
					throw new RuntimeException("Stack contentce at "+this+" does not match stack contentce of instruction at stack pos "+i+" ("+stack[stack.length-i-1]+" != "+code.stack_at(i)+")");
				}
			}
		}
	}

	public void attachPosition() {
		pc = code.pc;
		if( !check ) return;
		// Check if we at unreachable Code (after goto, return, throw)
		if( !code.reachable ) {
			// This point is unreachable by normal flow control,
			// just setup stack to be correct
			if( stack == null ) {
				trace(Kiev.debugInstrGen,"Stack state at unreacheable code is undefined, assuming current");
				stack = (JType[])Arrays.cloneToSize(code.stack,code.top);
			} else {
				trace(Kiev.debugInstrGen,"Attaching label at unreachable code, stack depth is "+stack.length);
				code.top = stack.length;
				System.arraycopy(stack,0,code.stack,0,stack.length);
				for(int i=code.top; i < code.stack.length; i++) {
					if( code.stack[i] == null ) break;
					code.stack[i] = null;
				}
			}
		} else {
			// This point is reachable by normal flow control, check stack state
			if( stack == null ) {
				stack = (JType[])Arrays.cloneToSize(code.stack,code.top);
			} else {
				if( stack.length != code.top )
					throw new RuntimeException("Stack depth at "+this+" does not match stack depth of instruction ("+stack.length+" != "+code.top+")");
				for(int i=0; i < stack.length; i++) {
					if( !stack[stack.length-i-1].equals(code.stack_at(i)) ) {
						JType tl = stack[stack.length-i-1];
						JType tc = code.stack_at(i);
						// Check for object/null, or one is child of other
						if( tl.isReference() && tc.isReference() ) {
							if( tl == JType.tpNull || tl.isInstanceOf(tc) ) continue;
							if( tc == JType.tpNull || tc.isInstanceOf(tl) ) {
								code.set_stack_at(tl,i);
								continue;
							}
							JType lct = JType.leastCommonType(tl,tc);
							if( lct != null ) {
								code.set_stack_at(lct,i);
								continue;
							}
						}
						else if( tl.isIntegerInCode() && tc.isIntegerInCode() ) {
							code.set_stack_at(JType.tpInt,i);
							continue;
						}
						throw new RuntimeException("Stack contentce at "+this+" does not match stack contentce of instruction at stack pos "+i+" ("+stack[stack.length-i-1]+" != "+code.stack_at(i)+")");
					}
				}
			}
		}
	}
}

public abstract class CodeSwitch {
	public final Code code;
	CodeSwitch(Code code) { this.code = code; }
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

	public CodeTableSwitch(Code code, int lo, int hi) {
		super(code);
		this.lo = lo;
		this.hi = hi;
		cases = new CodeLabel[hi-lo+1];
		end_label = code.newLabel();
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
		int code_pc = code.pc;
		end_label.pc = code_pc;
		code.pc = def_pc;
		code.add_code_int( def_case.pc-this.pc );
		code.add_code_int( lo );
		code.add_code_int( hi );
		trace(Kiev.debugAsmGen,"\ttable switch: from "+lo+" to "+hi+" default pc="+def_case.pc);
		for(int i=0; i <= hi-lo; i++)
			if( cases[i] != null )
				code.add_code_int( cases[i].pc-this.pc );
			else
				code.add_code_int( def_case.pc-this.pc );
		code.pc = code_pc;
	}
}

public class CodeLookupSwitch extends CodeSwitch {
	public int	pc;	// the PC of opc_tableswitch
	public int	def_pc;	// the PC of default label
	public int[]	tags;	// number of tags in table
	public CodeLabel[] cases;
	public CodeLabel   def_case;	// default case
	public CodeLabel   end_label;	// end of switch

	public CodeLookupSwitch(Code code, int[] tags) {
		super(code);
		this.tags = tags;
		cases = new CodeLabel[tags.length];
		end_label = code.newLabel();
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
		int code_pc = code.pc;
		end_label.pc = code_pc;
		code.pc = def_pc;
		code.add_code_int( def_case.pc-this.pc );
		code.add_code_int( tags.length );
		for(int i=0; i < tags.length; i++) {
			code.add_code_int(tags[i]);
			if( cases[i] != null )
				code.add_code_int( cases[i].pc-this.pc );
			else
				code.add_code_int( def_case.pc-this.pc );
		}
		code.pc = code_pc;
	}
}

public class CodeCatchInfo {
	public static CodeCatchInfo[] emptyArray = new CodeCatchInfo[0];

	public int			start_pc;
	public int			end_pc;
	public CodeLabel	handler;
	public JType		type;

	public CodeCatchInfo(CodeLabel handler, JType type) {
		this.handler = handler;
		this.type = type;
	}
}

