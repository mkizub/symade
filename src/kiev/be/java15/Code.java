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
package kiev.be.java15;

import static kiev.be.java15.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final class Code implements JConstants {
	
	/** Current class we are generating */
	public JStruct		clazz;
	
	/** Current method we are generating */
	public JMethod		method;
	
	/** Current ConstPool of the class we are generating */
	public ConstPool		constPool;
	
	/** Current position (for error reporting) from setLinePos */
	public int				last_lineno;
	
	/** Max stack deepness */
	public int				max_stack;

	/** Code (JVM bytecode) - for code generation only */
	public byte[]			bcode;

	/** Variables of this code (method args & locals) */
	public CodeVar[]		vars;

	/** Current number of local vars (method args & local vars) */
	private int				cur_locals;

	/** Max locals (method args & local vars) */
	private int				max_locals;

	/** Stack of code - for code generation only */
	public JType[]			stack;

	/** Top of stack - for code generation only */
	public int				top;

	/** Max stack top of stack - for code generation only
		double & long types increment/decrement it by 2
	 */
	private int				max_stack_top;

	/** PC - current code position - for code generation only */
	public int				pc;

	/** Labels of code */
	public CodeLabel[]		labels;
	public int[]			labels_pc;
	public int				labels_top = 0;

	/** ConstPool pointers */
	public CP[]				constants;
	public int[]			constants_pc;
	public int				constants_top = 0;

	/** Catch info */
	public CodeCatchInfo[]	catchers = CodeCatchInfo.emptyArray;

	/** Code attributes (local var table, line number info, etc. */
	public Attr[]			attrs = Attr.emptyArray;

	/** Line number table. Each element containce a pair of
		((pc & 0xFFFF) << 16) | (line_no & 0xFFFF)
	 */
	public int[]			linetable;

	/** Index of last filled linetable entry */
	public int				line_top = -1;

	/** Local var table (attribute) */
	public LocalVarTableAttr	lvta;

	public boolean			reachable = true;

	public boolean			generation;

	public boolean			cond_generation;
	
	public boolean			need_to_gen_post_cond;

	public Code(JStruct s, JMethod m, ConstPool cp) {
		clazz = s;
		method = m;
		constPool = cp;
		attrs = Attr.emptyArray;
		cur_locals = 0;
		max_locals = 0;
		vars = new CodeVar[255];

		// Initialize stack
		stack = new JType[256];
		top = 0;
		max_stack = 0;
		max_stack_top = 0;

		// Initialize code
		bcode = new byte[1024];
		pc = 0;

		// Initialize local var table
		lvta = new LocalVarTableAttr();
		// Initialize line number table
		linetable = new int[256];
		line_top = -1;

		// Initialize labels
		labels = new CodeLabel[256];
		labels_pc = new int[256];
		labels_top = 0;

		// Initialize constants
		constants = new CP[256];
		constants_pc = new int[256];
		constants_top = 0;

		catchers = CodeCatchInfo.emptyArray;

		reachable = true;

	}

	public void pushStackPos() {};

	public void popStackPos() {};

	public void setLinePos(JNode nv) {
		this.setLinePos(nv.getPosLine());
	}
	
	public void setLinePos(int lineno) {
		if( !generation ) return;
		if( lineno <= 0 || lineno > 0xFFFF) return;
		last_lineno = lineno;
		lineno = lineno & 0xFFFF;
		if( line_top != -1 && ( linetable[line_top] & 0xFFFF ) == lineno )
			return;
		if( linetable.length <= line_top+3 ) {
			int[] lt = new int[linetable.length*2];
			System.arraycopy(linetable,0,lt,0,linetable.length);
			linetable = lt;
		}
		linetable[++line_top] = ((pc & 0xFFFF) << 16) | lineno;
		return;
	};

	/** Push value into stack.
		Automatically calculates max_stack value
		Automatically expand stack array if needed
	 */
	public void stack_push(JType type) {
		if( stack.length <= (top + 2) ) stack = (JType[])Arrays.cloneToSize(stack,stack.length*2);
		if( type==JType.tpLong || type==JType.tpDouble ) max_stack_top += 2;
		else max_stack_top++;
		stack[top++] = type;
		if( max_stack < max_stack_top ) max_stack = max_stack_top;
		if( Kiev.debug && Kiev.debugInstrGen ) {
			StringBuffer sb = new StringBuffer(); sb.append("stack is:");
			for(int i=0; i < top; i++) sb.append(' ').append(stack[i]);
			System.out.println(sb.toString());
		}
	}

	/** Pop value from stack.
	 */
	public void stack_pop() {
		JType type = stack[--top];
		if( type==JType.tpLong || type==JType.tpDouble ) max_stack_top -= 2;
		else max_stack_top--;
		stack[top+1] = null;
		if( top < 0 ) throw new RuntimeException("Top of stack is < 0 for pc="+pc);
		if( Kiev.debug && Kiev.debugInstrGen ) {
			StringBuffer sb = new StringBuffer(); sb.append("stack is:");
			for(int i=0; i < top; i++) sb.append(' ').append(stack[i]);
			System.out.println(sb.toString());
		}
	}

	public JType stack_at(int i) {
		try {
			return stack[top-i-1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new RuntimeException("Access beyond bottom of stack");
		}
	}

	public void set_stack_at(JType tp, int i) {
		try {
			stack[top-i-1] = tp;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new RuntimeException("Access beyond bottom of stack");
		}
	}

	/** Add 1 byte to bytecode */
	public void add_code_byte(int b) {
		if( bcode.length <= pc+1 ) bcode = (byte[])Arrays.cloneToSize(bcode, bcode.length+256);
		bcode[pc++] = (byte)b;
	}
	/** Add 2 byte to bytecode */
	public void add_code_short(int s) {
		if( bcode.length <= pc+2 ) bcode = (byte[])Arrays.cloneToSize(bcode, bcode.length+256);
		bcode[pc++] = (byte)(s >>> 8);
		bcode[pc++] = (byte)(s & 0xFF);
	}
	/** Add 4 byte to bytecode */
	public void add_code_int(int i) {
		if( bcode.length <= pc+4 ) bcode = (byte[])Arrays.cloneToSize(bcode, bcode.length+256);
		bcode[pc++] = (byte)(i >>> 24);
		bcode[pc++] = (byte)(i >>> 16);
		bcode[pc++] = (byte)(i >>> 8);
		bcode[pc++] = (byte)(i & 0xFF);
	}

	/** Add java native opcode to bytecode
		verify and update stack info
	 */
	private void add_opcode(int op) {
		trace(Kiev.debug && Kiev.debugInstrGen,"\tadd opcode "+opcNames[op]);
		JType[] types = OpCodeRules.op_input_types[op];	// get argument types
		for(int i=0,j=types.length-1; i < types.length; i++,j--) {
			JType t1 = stack_at(j);
			JType t2 = types[i];
			try {
				if( !(	t1.isAutoCastableTo(t2)
				     ||(t1.isIntegerInCode() && t2.isIntegerInCode())
				     ||(t1.isArray() && t2.isArray())
					 ||(t1.isReference() && t2 == JType.tpObject)
					 )
				)
					throw new RuntimeException("Type of value in stack pos "+j+" is "+stack_at(j)+" but type "+types[i]+" expected for opcode "+opcNames[op]);
			} catch(Exception e) {
				throw new RuntimeException("Unresolved type at generation phase: "+e);
			}
		}
		for(int i=0; i < types.length; i++) stack_pop();
		types = OpCodeRules.op_output_types[op];		// get return types
		if( types.length > 0 ) {
			JType t = types[0];
			try {
				if( !(t.isReference() || t==JType.tpVoid) )
					stack_push(t);
			} catch(Exception e) {
				throw new RuntimeException("Unresolved type at generation phase: "+e);
			}
		}
		add_code_byte(op);
	}

	/** Add java native opcode with one-byte argument
	 */
	private void add_opcode_and_byte(int op, int arg) {
		add_opcode(op);
		add_code_byte(arg);
		trace(Kiev.debug && Kiev.debugInstrGen,"\t\topcode arg is byte "+arg);
	}

	/** Add java native opcode with two-byte argument
	 */
	private void add_opcode_and_short(int op, int arg) {
		add_opcode(op);
		add_code_short(arg);
		trace(Kiev.debug && Kiev.debugInstrGen,"\t\topcode arg is short "+arg);
	}

	/** Add java native opcode with four-byte argument
	 */
	private void add_opcode_and_int(int op, int arg) {
		add_opcode(op);
		add_code_int(arg);
		trace(Kiev.debug && Kiev.debugInstrGen,"\t\topcode arg is int "+arg);
	}

	/** Add java native opcode with ContPool constant pointer
	 */
	private void add_opcode_and_CP(int op, CP arg) {
		if( !cond_generation && (op == opc_ldc || op == opc_ldc_w) ) {
			if( arg.pos<=0 && constPool.hwm < 256 && (arg instanceof NumberCP || arg instanceof StringCP) ) {
				arg.pos = constPool.hwm;
				constPool.pool[constPool.hwm++] = arg;
			}
			if( arg.pos >0 && arg.pos < 256 ) {
				add_opcode(op=opc_ldc);
				add_code_byte(arg.pos);
			} else {
				add_opcode(op=opc_ldc_w);
				add_code_short(arg.pos);
			}
		} else {
			if( op == opc_ldc ) op = opc_ldc_w;
			add_opcode(op);
			add_code_short(arg.pos);
		}
		if( arg.pos <= 0 ) {
			if( constants.length < constants_top+2 ) {
				constants = (CP[])Arrays.ensureSize(constants,constants.length*2);
				constants_pc = (int[])Arrays.ensureSize(constants_pc,constants.length);
			}
			constants[constants_top] = arg;
			constants_pc[constants_top] = pc - (op==opc_ldc?1:2);
			constants_top++;
		}
		trace(Kiev.debug && Kiev.debugInstrGen,"\t\topcode arg is CP "+arg);
	}

	/** Add jump/opcode (cond & uncond) with label it use to jump to
	 */
	private void add_opcode_and_label(int op, CodeLabel arg) {
		add_opcode(op);
		arg.addInstr();
		if( arg.pc >= 0 )
			add_code_short(arg.pc-pc+1);
		else {
			if( labels.length < labels_top+2 ) {
				labels = (CodeLabel[])Arrays.ensureSize(labels,labels.length*2);
				labels_pc = (int[])Arrays.ensureSize(labels_pc,labels.length);
			}
			labels[labels_top] = arg;
			labels_pc[labels_top] = pc;
			labels_top++;
			add_code_short(0);
		}
		trace(Kiev.debug && Kiev.debugInstrGen,"\t\topcode arg is label "+arg);
	}

	/** Add jump/opcode (cond & uncond) with label it use to jump to
	 */
	private void add_opcode_dump(byte[] dump) {
		assert( top == 0 , "Add code dump while stack is not empty");
		while( bcode.length <= pc+dump.length )
			bcode = (byte[])Arrays.cloneToSize(bcode, bcode.length+256);
		System.arraycopy(dump,0,bcode,pc,dump.length);
		pc += dump.length;
		trace(Kiev.debug && Kiev.debugInstrGen,"\t\tadd opcode dump of length "+dump.length);
	}

	/** Add java native opcode
		automatically choose one or two bytes reference to
		structure's constant pool, then selects apropriative
		java bytecode from array
	 */
//	private void add_opcode(int[] ops, CP c) {
//		short index = c.pos;
//		if( index >= Byte.MIN_VALUE && index <= Byte.MAX_VALUE ) {
//			add_opcode_and_byte(ops[0],index);
//		}
//		else if( index >= Short.MIN_VALUE && index <= Short.MAX_VALUE ) {
//			add_opcode_and_short(ops[1],index);
//		}
//	}

	/** Push constant into stack
		optimize integer and numeric constants
		if possible, otherwise put constant in the structure's pool
		and load it by reference
	 */
	private void generatePushConst(Object value) {
		if( value == null ) {
			add_opcode(opc_aconst_null);
			stack_push(JType.tpNull);
		}
		else if( value instanceof java.lang.Character ) {
			add_opcode_and_short(opc_sipush,((java.lang.Character)value).charValue());
		}
		else if( value instanceof Long ) {
			long val = ((Long)value).longValue();
			if( val == 0L )			add_opcode(opc_lconst_0);
			else if( val == 1L )	add_opcode(opc_lconst_1);
			else {
				CP c = constPool.addNumberCP((Number)value);
				add_opcode_and_CP(opc_ldc2_w,c);
				stack_push(JType.tpLong);
			}
		}
		else if( value instanceof Float ) {
			float val = ((Float)value).floatValue();
			if( val == 0.0f )		add_opcode(opc_fconst_0);
			else if( val == 1.0f )	add_opcode(opc_fconst_1);
			else if( val == 2.0f )	add_opcode(opc_fconst_1);
			else {
				CP c = constPool.addNumberCP((Number)value);
				add_opcode_and_CP(opc_ldc,c);
				stack_push(JType.tpFloat);
			}
		}
		else if( value instanceof Double ) {
			double val = ((Double)value).doubleValue();
			if( val == 0.0D )		add_opcode(opc_dconst_0);
			else if( val == 1.0D )	add_opcode(opc_dconst_1);
			else {
				CP c = constPool.addNumberCP((Number)value);
				add_opcode_and_CP(opc_ldc2_w,c);
				stack_push(JType.tpDouble);
			}
		}
		else if( value instanceof KString ) {
			CP c = constPool.addStringCP((KString)value);
			add_opcode_and_CP(opc_ldc,c);
			stack_push(JType.tpString);
		}
		else if( value instanceof Number ) {
			int val = ((Number)value).intValue();
			switch(val) {
			case -1:	add_opcode(opc_iconst_m1); break;
			case 0:		add_opcode(opc_iconst_0); break;
			case 1:		add_opcode(opc_iconst_1); break;
			case 2:		add_opcode(opc_iconst_2); break;
			case 3:		add_opcode(opc_iconst_3); break;
			case 4:		add_opcode(opc_iconst_4); break;
			case 5:		add_opcode(opc_iconst_5); break;
			default:
				if( val >=  Byte.MIN_VALUE && val <= Byte.MAX_VALUE ) {
					add_opcode_and_byte(opc_bipush,val);
				}
				else if( val >= Short.MIN_VALUE && val <= Short.MAX_VALUE ) {
					add_opcode_and_short(opc_sipush,val);
				}
				else {
					CP c = constPool.addNumberCP((Number)value);
					add_opcode_and_CP(opc_ldc,c);
					stack_push(JType.tpInt);
				}
			}
		}
		else
	        throw new RuntimeException("Adding constant of undefined type "+value.getClass());
	}

	/** Pop out value from
		Check whether two-word or one-word value is popped
	 */
	private void generatePop() {
		JType t = stack_at(0);
		if( t.isDoubleSize() ) add_opcode(opc_pop2);
		else add_opcode(opc_pop);
		stack_pop();
	}

	/** Duplicate value on the top of stack
		This version is restricted to duplicate only
		one var. No (int1,int2)->(int1,int2,int1,in2),
		only (long1)->(long1,long1) for two-word duplication
	 */
	private void generateDup() {
		JType t = stack_at(0);
		if( t.isDoubleSize() ) add_opcode(opc_dup2);
		else add_opcode(opc_dup);
		stack_push(t);
	}

	/** Duplicate value on the top of stack with exchange
		This version is restricted to duplicate only
		one var (like in generateDup).
	 */
	private void generateDupX() {
		JType type1 = stack_at(0);
		JType type2 = stack_at(1);
		if( type1.isDoubleSize() ) {
			if( type2.isDoubleSize() ) add_opcode(opc_dup2_x2);
			else add_opcode(opc_dup2_x1);
		} else {
			if( type2.isDoubleSize() ) add_opcode(opc_dup_x2);
			else add_opcode(opc_dup_x1);
		}
		stack_pop();
		stack_pop();
		stack_push(type1);
		stack_push(type2);
		stack_push(type1);
	}

	/** Duplicate value on the top of stack with exchange over 2 values
		This version is restricted to duplicate only over 2 vars of int/object type
		(to dup over long/double var use generateDupX).
	 */
	private void generateDupX2() {
		JType type1 = stack_at(0);
		JType type2 = stack_at(1);
		JType type3 = stack_at(2);
		if( type2.isDoubleSize() || type3.isDoubleSize() )
			throw new RuntimeException("Dup_X2 over long/double value");
		if( type1.isDoubleSize() )
			add_opcode(opc_dup2_x2);
		else
			add_opcode(opc_dup_x2);
		stack_pop();
		stack_pop();
		stack_pop();
		stack_push(type1);
		stack_push(type3);
		stack_push(type2);
		stack_push(type1);
	}

	/** Duplicate 2 values both of 4 bytes length
	 */
	private void generateDup2() {
		JType t1 = stack_at(0);
		JType t2 = stack_at(1);
		if( t1.isDoubleSize() || t2.isDoubleSize() )
			throw new RuntimeException("Dup2 on long/double type");
		add_opcode(opc_dup2);
		stack_push(t2);
		stack_push(t1);
	}

	/** Swap values on the top of stack (both must be 4 byte length)
	 */
	private void generateSwap() {
		JType t1 = stack_at(0);
		JType t2 = stack_at(1);
		if( t1.isDoubleSize() || t2.isDoubleSize() )
			throw new RuntimeException("Swap on long/double type");
		add_opcode(opc_swap);
		stack_pop(); stack_pop();
		stack_push(t1);
		stack_push(t2);
	}

	/** Call method. Find out args from method, check args's types
	 */
	private void generateCall(JMethod m, boolean super_flag, Type tp) {
		boolean call_static;
		if( !m.isStatic() ) {
			trace(Kiev.debug && Kiev.debugInstrGen,"\t\tgenerating non-static call to method: "+m);
			call_static = false;
		} else {
			trace(Kiev.debug && Kiev.debugInstrGen,"\t\tgenerating static call to method: "+m);
			call_static = true;
		}
		JMethodType mtype = (JMethodType)Type.getRealType(tp,m.dtype).getJType();
		for(int i=0; i < mtype.jargs.length; i++) {
			try {
				JType t1 = stack_at(mtype.jargs.length-i-1);
				JType t2 = mtype.jargs[i];
//				if( t1.isArgument() || t2.isArgument() ) continue;
				if( t1 == JType.tpNull && t2.isReference() ) continue;
//				if( !t1.isInstanceOf(t2) && !(t1.isIntegerInCode() && t2.isIntegerInCode()) )
//					throw new RuntimeException("Type of call argument in stack pos "+(m.params.length-i-1)
//						+" is "+stack_at(m.params.length-i-1)+" but method "+m+" expects "+m.params[i].type.getJType());
			} catch(Exception e) {
				throw new RuntimeException("Unresolved type at generation phase: "+e);
			}
		}
		KString sign;
		JType ttt = Type.getRealType(tp,((JTypeDecl)m.jparent).xtype).getJType();
		sign = m.etype.getJType().java_signature;
		CP cpm;
		if( m.jctx_tdecl.isInterface() )
			cpm = constPool.addInterfaceMethodCP(ttt.java_signature,KString.from(m.sname),sign);
		else if (m.isConstructor())
			cpm = constPool.addMethodCP(ttt.java_signature,knameInit,sign);
		else
			cpm = constPool.addMethodCP(ttt.java_signature,KString.from(m.sname),sign);
		if( call_static ) {
			add_opcode_and_CP(opc_invokestatic,cpm);
		}
		else if( ((JTypeDecl)m.jparent).isInterface() ) {
			add_opcode_and_CP(opc_invokeinterface,cpm);
			int argslen = 1;
			foreach(JType t; mtype.jargs) {
				if( t.isDoubleSize() ) argslen+=2;
				else argslen++;
			}
			add_code_byte(argslen);
			add_code_byte(0);
			trace(Kiev.debug && Kiev.debugInstrGen,"call has "+mtype.jargs.length+" params");
		}
		else if(super_flag || m.isPrivate()) {
			add_opcode_and_CP(opc_invokespecial,cpm);
		}
		else {
			assert (!m.isConstructor());
			add_opcode_and_CP(opc_invokevirtual,cpm);
		}
		for(int i=0; i < mtype.jargs.length; i++) stack_pop();
		if( !call_static) stack_pop();
		if( mtype.jret != JType.tpVoid )
			stack_push(mtype.jret);
	}

	public void generateReturn() {
		try {
			JType t = ((JMethodType)this.method.etype.getJType()).jret;
			if( t == JType.tpVoid )				add_opcode(opc_return);
			else if( t.isIntegerInCode() )		add_opcode(opc_ireturn);
			else if( t == JType.tpLong )		add_opcode(opc_lreturn);
			else if( t == JType.tpFloat )		add_opcode(opc_freturn);
			else if( t == JType.tpDouble )		add_opcode(opc_dreturn);
			else if( t == JType.tpLong )		add_opcode(opc_lreturn);
			else if( t.isReference() )			add_opcode(opc_areturn);
			else
				throw new RuntimeException("Unknown return type "+this.method.type.ret()+" of method");
		} catch(Exception e) {
			throw new RuntimeException("Unresolved type at generation phase: "+e);
		}
       	reachable = false;
	}

	/** Get (load, push in stack) length of array. */
	public void generateLengthOfArray() {
		JType tp1 = stack_at(0);

		if( !tp1.isArray() )
			throw new RuntimeException("Length of non-array object "+tp1);

		add_opcode(opc_arraylength);
	}

	/** Get (load, push in stack) element of array. */
	public void generateLoadElement() {
		JType tp1 = stack_at(1);
		JType tp2 = stack_at(0);

		if( !tp1.isArray() )
			throw new RuntimeException("Index of non-array object "+tp1);
		if( !tp2.isIntegerInCode() )
			throw new RuntimeException("Index of array element must be of integer type, but found "+tp2);

		JType t = ((JArrayType)tp1).jarg;
		if( t == JType.tpVoid )
			throw new RuntimeException("Array of elements of 'void' type is not allowed");
		else if( t == JType.tpBoolean )		add_opcode(opc_baload);
		else if( t == JType.tpByte )		add_opcode(opc_baload);
		else if( t == JType.tpChar )		add_opcode(opc_caload);
		else if( t == JType.tpShort )		add_opcode(opc_saload);
		else if( t == JType.tpInt )			add_opcode(opc_iaload);
		else if( t == JType.tpLong )		add_opcode(opc_laload);
		else if( t == JType.tpFloat )		add_opcode(opc_faload);
		else if( t == JType.tpDouble)		add_opcode(opc_daload);
		else if( t.isInstanceOf(JType.tpObject) )	{
			add_opcode(opc_aaload);
			stack_push(t);
		}
		else
			throw new RuntimeException("Unknown type of array element "+t);
	}

	/** Get (load, push in stack) element of array. */
	public void generateStoreElement() {
		JType tp1 = stack_at(2);
		JType tp2 = stack_at(1);
		JType tp3 = stack_at(0);

		if( !tp1.isArray() )
			throw new RuntimeException("Index of non-array object "+tp1);
		JType t = ((JArrayType)tp1).jarg;
		if( !tp2.isIntegerInCode() )
			throw new RuntimeException("Index of array element must be of integer type, but found "+tp2);

		if( t == JType.tpVoid )
			throw new RuntimeException("Array of elements of 'void' type is not allowed");
		else if( t == JType.tpBoolean )		add_opcode(opc_bastore);
		else if( t == JType.tpByte )		add_opcode(opc_bastore);
		else if( t == JType.tpChar )		add_opcode(opc_castore);
		else if( t == JType.tpShort )		add_opcode(opc_sastore);
		else if( t == JType.tpInt )			add_opcode(opc_iastore);
		else if( t == JType.tpLong )		add_opcode(opc_lastore);
		else if( t == JType.tpFloat )		add_opcode(opc_fastore);
		else if( t == JType.tpDouble)		add_opcode(opc_dastore);
		else if( t.isInstanceOf(JType.tpObject) ) {
			stack_pop();
			stack_pop();
			stack_pop();
			add_opcode(opc_aastore);
		}
		else
			throw new RuntimeException("Unknown type of array element "+t);
	}

	/** Push var (argument or one of local vars) into stack (load it)
		automatically optimaze to short instruction, if possible
	 */
	private static final int[] iload_ops = {opc_iload_0,opc_iload_1,opc_iload_2,opc_iload_3,opc_iload};
	private static final int[] lload_ops = {opc_lload_0,opc_lload_1,opc_lload_2,opc_lload_3,opc_lload};
	private static final int[] fload_ops = {opc_fload_0,opc_fload_1,opc_fload_2,opc_fload_3,opc_fload};
	private static final int[] dload_ops = {opc_dload_0,opc_dload_1,opc_dload_2,opc_dload_3,opc_dload};
	private static final int[] aload_ops = {opc_aload_0,opc_aload_1,opc_aload_2,opc_aload_3,opc_aload};

	/** This method pushes var (loads) from locals (args and auto vars)
	 */
	private void generateLoadVar(int vv) {
		CodeVar v = vars[vv];
		int[] opcodes;
		JType t = v.var.getType().getJType();
		if( t == JType.tpVoid )
			throw new RuntimeException("Can't load variable of type "+t);
		else if( t == JType.tpLong )	opcodes = lload_ops;
		else if( t == JType.tpFloat )	opcodes = fload_ops;
		else if( t == JType.tpDouble)	opcodes = dload_ops;
		else if( t.isIntegerInCode() ) opcodes = iload_ops;
		else if( t.isReference() )		opcodes = aload_ops;
		else
			throw new RuntimeException("Can't load variable of unknown type with signature "+t.java_signature);
		if( v.stack_pos >= 0 && v.stack_pos <= 3 )
			add_opcode(opcodes[v.stack_pos]);
		else if( v.stack_pos >=0 && v.stack_pos < 256 )
			add_opcode_and_byte(opcodes[4],v.stack_pos);
		else
			throw new RuntimeException("Var "+v+" has illegal offset "+v.stack_pos+" in method");
		if( t.isReference() )
			stack_push(t);
	}

	/** Pop value from stack into var (argument or one of local vars)
		automatically optimaze to short instruction, if possible
	 */
	private static final int[] istore_ops = {opc_istore_0,opc_istore_1,opc_istore_2,opc_istore_3,opc_istore};
	private static final int[] lstore_ops = {opc_lstore_0,opc_lstore_1,opc_lstore_2,opc_lstore_3,opc_lstore};
	private static final int[] fstore_ops = {opc_fstore_0,opc_fstore_1,opc_fstore_2,opc_fstore_3,opc_fstore};
	private static final int[] dstore_ops = {opc_dstore_0,opc_dstore_1,opc_dstore_2,opc_dstore_3,opc_dstore};
	private static final int[] astore_ops = {opc_astore_0,opc_astore_1,opc_astore_2,opc_astore_3,opc_astore};

	/** This method pops var (stores) into locals (args and auto vars)
	 */
	private void generateStoreVar(int vv) {
		CodeVar v = vars[vv];
		int[] opcodes;
		JType t = v.var.getType().getJType();
		if( t == JType.tpVoid )
			throw new RuntimeException("Can't store variable of type "+t);
		else if( t == JType.tpLong )	opcodes = lstore_ops;
		else if( t == JType.tpFloat )	opcodes = fstore_ops;
		else if( t == JType.tpDouble)	opcodes = dstore_ops;
		else if( t.isIntegerInCode() ) opcodes = istore_ops;
		else if( t.isReference() )		opcodes = astore_ops;
		else
			throw new RuntimeException("Can't store variable of unknown type with type "+t);
		if( v.stack_pos >= 0 && v.stack_pos <= 3 )
			add_opcode(opcodes[v.stack_pos]);
		else if( v.stack_pos >=0 && v.stack_pos < 256 )
			add_opcode_and_byte(opcodes[4],(byte)v.stack_pos);
		else
			throw new RuntimeException("Var "+v+" has illegal offset "+v.stack_pos+" in method");
	}

	/** Binary operation instanceof.
	 */
	private void generateInstanceofOp(JType type) {
		JType pt1 = stack_at(0);
		JType pt2 = type;

		if( !pt1.isReference() )
            throw new RuntimeException("Instanceof operation on primitive type: "+pt1);
		if( !pt2.isReference() )
            throw new RuntimeException("Type of instanceof operation is primtive type: "+pt2);
		CP cpi = constPool.addClazzCP(type.java_signature);
		add_opcode_and_CP(opc_instanceof,cpi);
	}

	/** Boolean operations.
	 */
	private void generateCompareOp(Instr instr, CodeLabel l) {
		JType pt1 = stack_at(1);
		JType pt2 = stack_at(0);
		int op;

		if( pt1.isIntegerInCode() ) {
			if( !pt2.isIntegerInCode() )
	            throw new RuntimeException("Bad boolean operation "+instr+" on types "+pt1+" and "+pt2);
			switch(instr) {
			case Instr.op_ifcmpeq:	add_opcode_and_label(opc_if_icmpeq,l); break;
			case Instr.op_ifcmpne:	add_opcode_and_label(opc_if_icmpne,l); break;
			case Instr.op_ifcmple:	add_opcode_and_label(opc_if_icmple,l); break;
			case Instr.op_ifcmplt:	add_opcode_and_label(opc_if_icmplt,l); break;
			case Instr.op_ifcmpge:	add_opcode_and_label(opc_if_icmpge,l); break;
			case Instr.op_ifcmpgt:	add_opcode_and_label(opc_if_icmpgt,l); break;
			default:
	            throw new RuntimeException("Bad boolean compare operation "+instr+" on types "+pt1+" and "+pt2);
			}
//			l.addInstr();
			return;
		}
		else if( pt1.isReference() ) {
			if( !pt2.isReference() )
	            throw new RuntimeException("Bad boolean operation "+instr+" on types "+pt1+" and "+pt2);
			switch(instr) {
			case Instr.op_ifcmpeq:	add_opcode_and_label(opc_if_acmpeq,l); break;
			case Instr.op_ifcmpne:	add_opcode_and_label(opc_if_acmpne,l); break;
			default:
	            throw new RuntimeException("Bad boolean compare operation "+instr+" on types "+pt1+" and "+pt2);
			}
//			l.addInstr();
			return;
		}
		else if( pt1 == JType.tpVoid )
            throw new RuntimeException("Bad boolean operation "+instr+" on types "+pt1+" and "+pt2);
		else if( pt1 == JType.tpLong ) {
			if( pt2 != JType.tpLong )
	            throw new RuntimeException("Bad boolean operation "+instr+" on types "+pt1+" and "+pt2);
			op = opc_lcmp;
		}
		// TODO opc_fcmpg & opc_fcmpl
		else if( pt1 == JType.tpFloat ) {
			if( pt2 != JType.tpFloat )
	            throw new RuntimeException("Bad boolean operation "+instr+" on types "+pt1+" and "+pt2);
			op = opc_fcmpg;
		}
		// TODO opc_dcmpg & opc_dcmpl
		else if( pt1 == JType.tpDouble ) {
			if( pt2 != JType.tpDouble )
	            throw new RuntimeException("Bad boolean operation "+instr+" on types "+pt1+" and "+pt2);
			op = opc_dcmpg;
		}
		else
            throw new RuntimeException("Type of compare operation is wrong type: "+pt1);
		add_opcode(op);
		switch(instr) {
		case Instr.op_ifcmpeq: add_opcode_and_label(opc_ifeq,l); break;
		case Instr.op_ifcmpne: add_opcode_and_label(opc_ifne,l); break;
		case Instr.op_ifcmple: add_opcode_and_label(opc_ifle,l); break;
		case Instr.op_ifcmplt: add_opcode_and_label(opc_iflt,l); break;
		case Instr.op_ifcmpge: add_opcode_and_label(opc_ifge,l); break;
		case Instr.op_ifcmpgt: add_opcode_and_label(opc_ifgt,l); break;
		default:
            throw new RuntimeException("Bad boolean compare operation "+instr+" on types "+pt1+" and "+pt2);
		}
//		l.addInstr();
	}

	/** New one-dimension array */
	public void generateNewArray(JType type) {
		JType dim = stack_at(0);
		if( !dim.isIntegerInCode() )
			throw new RuntimeException("Array dimention must be of integer type, but "+dim+" found");
		if( type == JType.tpBoolean )		add_opcode_and_byte(opc_newarray,4);
		else if( type == JType.tpByte )		add_opcode_and_byte(opc_newarray,8);
		else if( type == JType.tpChar )		add_opcode_and_byte(opc_newarray,5);
		else if( type == JType.tpShort )	add_opcode_and_byte(opc_newarray,9);
		else if( type == JType.tpInt )		add_opcode_and_byte(opc_newarray,10);
		else if( type == JType.tpLong )		add_opcode_and_byte(opc_newarray,11);
		else if( type == JType.tpFloat )	add_opcode_and_byte(opc_newarray,6);
		else if( type == JType.tpDouble )	add_opcode_and_byte(opc_newarray,7);
		else if( type.isReference() ) {
			ClazzCP cpc = constPool.addClazzCP(type.java_signature);
			add_opcode_and_CP(opc_anewarray,cpc);
		}
		stack_push(new JArrayType(type));
	}


	/** New multi-dimension array */
	public void generateNewMultiArray(int dim, JType arrtype) {
		for(int i=0; i < dim; i++ )
			if( !stack_at(i).isIntegerInCode() )
				throw new RuntimeException("Array dimention must be of integer type, but "
					+stack_at(i)+" found at "+(dim-i)+" dimension of multidimension array");
		ClazzCP cpc = constPool.addClazzCP(arrtype.java_signature);
		add_opcode_and_CP(opc_multianewarray,cpc);
		add_code_byte(dim);
		for(int i=0; i < dim; i++ ) stack_pop();
		stack_push(arrtype);
	}

	/** Casts (converts) primitive bytecode types */
	public void generatePrimitiveCast(JType to) {
		JType from = stack_at(0);
		if( to.equals(from) ) return;
		if( to == JType.tpBoolean || to == JType.tpByte ) {
			if( from == JType.tpLong )
				{ add_opcode(opc_l2i); add_opcode(opc_i2b); }
			else if( from == JType.tpFloat )
				{ add_opcode(opc_f2i); add_opcode(opc_i2b); }
			else if( from == JType.tpDouble )
				{ add_opcode(opc_d2i); add_opcode(opc_i2b); }
			else if( from.isIntegerInCode() )
				add_opcode(opc_i2b);
		}
		if( to == JType.tpChar ) {
			if( from == JType.tpLong )
				{ add_opcode(opc_l2i); add_opcode(opc_i2c); }
			else if( from == JType.tpFloat )
				{ add_opcode(opc_f2i); add_opcode(opc_i2c); }
			else if( from == JType.tpDouble )
				{ add_opcode(opc_d2i); add_opcode(opc_i2c); }
			else if( from.isIntegerInCode() )
				add_opcode(opc_i2c);
		}
		if( to == JType.tpShort ) {
			if( from == JType.tpLong )
				{ add_opcode(opc_l2i); add_opcode(opc_i2s); }
			else if( from == JType.tpFloat )
				{ add_opcode(opc_f2i); add_opcode(opc_i2s); }
			else if( from == JType.tpDouble )
				{ add_opcode(opc_d2i); add_opcode(opc_i2s); }
			else if( from.isIntegerInCode() )
				add_opcode(opc_i2s);
		}
		if( to == JType.tpInt ) {
			if( from == JType.tpLong )		add_opcode(opc_l2i);
			else if( from == JType.tpFloat )	add_opcode(opc_f2i);
			else if( from == JType.tpDouble )	add_opcode(opc_d2i);
		}
		if( to == JType.tpLong ) {
			if( from == JType.tpFloat )		add_opcode(opc_f2l);
			else if( from == JType.tpDouble )	add_opcode(opc_d2l);
			else if( from.isIntegerInCode() )	add_opcode(opc_i2l);
		}
		if( to == JType.tpFloat ) {
			if( from == JType.tpLong )		add_opcode(opc_l2f);
			else if( from == JType.tpDouble )	add_opcode(opc_d2f);
			else if( from.isIntegerInCode() )	add_opcode(opc_i2f);
		}
		if( to == JType.tpDouble ) {
			if( from == JType.tpLong )		add_opcode(opc_l2d);
			else if( from == JType.tpFloat )	add_opcode(opc_f2d);
			else if( from.isIntegerInCode() )	add_opcode(opc_i2d);
		}
	}

	/** Add local var for this code.
		For debug version automatically generates debug info
	 */
	public CodeVar addVar(JVar v) {
		trace(Kiev.debug && Kiev.debugInstrGen,"Code add var "+v);
		int pos = cur_locals;
		v.bcpos = pos;
		vars[pos] = new CodeVar(v);
		vars[pos].start_pc = pc;
		cur_locals++;
		JType t = v.type.getJType();
		if( t==JType.tpLong || t==JType.tpDouble ) cur_locals++;
		if( cur_locals > max_locals ) max_locals = cur_locals;
		if( v.sname != "" && v.sname != null) {
			lvta.addVar(vars[pos]);
		}
		trace(Kiev.debug && Kiev.debugInstrGen,"Code var "+v+" added to bc pos "+pos+" "+vars[pos]);
		return vars[pos];
	}

	/** Remove local var for this code.
	 */
	public void removeVar(JVar v) {
		trace(Kiev.debug && Kiev.debugInstrGen,"Code remove var "+v+" from bc pos "+v.bcpos+" "+vars[v.bcpos]);
		JType t = v.type.getJType();
		if( v.sname != "" && v.sname != null ) {
			lvta.vars[vars[v.bcpos].index].end_pc = pc-1;
		}
		if( t==JType.tpLong || t==JType.tpDouble ) {
			if( v.bcpos != cur_locals-2 )
				throw new RuntimeException("Removing var "+v+" at pos "+v.bcpos+" but last inserted var was "
					+(vars[cur_locals-1]!=null?vars[cur_locals-1].var:vars[cur_locals-2].var)+" at pos "
					+(vars[cur_locals-1]!=null?(cur_locals-2):(cur_locals-1)));
			vars[--cur_locals] = null;
			vars[--cur_locals] = null;
		} else {
			if( v.bcpos != cur_locals-1 )
				throw new RuntimeException("Removing var "+v+" at pos "+v.bcpos+" but last inserted var was "
					+(vars[cur_locals-1]!=null?vars[cur_locals-1].var:vars[cur_locals-2].var)+" at pos "
					+(vars[cur_locals-1]!=null?(cur_locals-2):(cur_locals-1)));
			vars[--cur_locals] = null;
		}
	}

	/** Add local vars for this code.
		For debug version automatically generates debug info
	 */
	public void addVars(JVar[] v) {
		for(int i=0; i < v.length; i++) addVar(v[i]);
	}

	/** Remove local vars for this code (i.e. on block end)
	 */
	public void removeVars(JVar[] v) {
		for(int i=v.length-1; i >= 0; i--) removeVar(v[i]);
	}

	/** Create new label and return it */
	public CodeLabel newLabel() {
		CodeLabel l = new CodeLabel(this);
//		labels = (CodeLabel[])Arrays.append(labels,l);
		return l;
	}

	/** Create new catcher and return it */
	public CodeCatchInfo newCatcher(CodeLabel handler, JType type) {
		CodeCatchInfo catcher = new CodeCatchInfo(handler,type);
		catchers = (CodeCatchInfo[])Arrays.insert(catchers,catcher,0);
		return catcher;
	}

	public CodeTableSwitch newTableSwitch(int lo, int hi) {
		return new CodeTableSwitch(this,lo,hi);
	}

	public CodeLookupSwitch newLookupSwitch(int[] tags) {
		return new CodeLookupSwitch(this,tags);
	}

	/** Create new switch table */
	public void generateTableSwitch(CodeTableSwitch sw) {
		sw.pc = pc;
		add_opcode(opc_tableswitch);
		while( (pc % 4) != 0 ) add_code_byte(0);
		sw.def_pc = pc;
		add_code_int(0);	// default label
		add_code_int(sw.lo);
		add_code_int(sw.hi);
		for(int i=sw.lo; i <= sw.hi; i++) add_code_int(0);
	}

	/** Create new lookup switch table */
	public void generateLookupSwitch(CodeLookupSwitch sw) {
		sw.pc = pc;
		add_opcode(opc_lookupswitch);
		while( (pc % 4) != 0 ) add_code_byte(0);
		sw.def_pc = pc;
		add_code_int(0);	// default label
		add_code_int( sw.tags.length );
		for(int i=0; i < sw.tags.length; i++) {
			add_code_int( sw.tags[i] );
			add_code_int(0);
		}
	}

	public boolean isInfoInstr(Instr instr) {
		switch( instr ) {
		case set_lineno:
		case set_label:
		case switch_close:
		case add_var:
		case remove_var:
		case start_catcher:
		case stop_catcher:
		case enter_catch_handler:
		case exit_catch_handler:
		case set_CP:
			return true;
		default:
			return false;
		}
	}

	/** Add pseude-instruction for this code.
	 */
	public void addInstr(Instr i) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
	    switch(i) {
        case op_nop:					add_opcode(opc_nop);				break;
        case op_pop:					generatePop();						break;
		case op_arr_load:				generateLoadElement();				break;
		case op_arr_store:				generateStoreElement();				break;
        case op_dup:					generateDup();						break;
        case op_dup_x:					generateDupX();						break;
        case op_dup_x2:					generateDupX2();					break;
        case op_dup2:					generateDup2();						break;
        case op_swap:					generateSwap();						break;
        case op_iadd:					add_opcode(opc_iadd);				break;
        case op_ladd:					add_opcode(opc_ladd);				break;
        case op_fadd:					add_opcode(opc_fadd);				break;
        case op_dadd:					add_opcode(opc_dadd);				break;
        case op_isub:					add_opcode(opc_isub);				break;
        case op_lsub:					add_opcode(opc_lsub);				break;
        case op_fsub:					add_opcode(opc_fsub);				break;
        case op_dsub:					add_opcode(opc_dsub);				break;
        case op_imul:					add_opcode(opc_imul);				break;
        case op_lmul:					add_opcode(opc_lmul);				break;
        case op_fmul:					add_opcode(opc_fmul);				break;
        case op_dmul:					add_opcode(opc_dmul);				break;
        case op_idiv:					add_opcode(opc_idiv);				break;
        case op_ldiv:					add_opcode(opc_ldiv);				break;
        case op_fdiv:					add_opcode(opc_fdiv);				break;
        case op_ddiv:					add_opcode(opc_ddiv);				break;
        case op_irem:					add_opcode(opc_irem);				break;
        case op_lrem:					add_opcode(opc_lrem);				break;
        case op_frem:					add_opcode(opc_frem);				break;
        case op_drem:					add_opcode(opc_drem);				break;
        case op_ineg:					add_opcode(opc_ineg);				break;
        case op_lneg:					add_opcode(opc_lneg);				break;
        case op_fneg:					add_opcode(opc_fneg);				break;
        case op_dneg:					add_opcode(opc_dneg);				break;
        case op_ishl:					add_opcode(opc_ishl);				break;
        case op_lshl:					add_opcode(opc_lshl);				break;
        case op_ishr:					add_opcode(opc_ishr);				break;
        case op_lshr:					add_opcode(opc_lshr);				break;
        case op_iushr:					add_opcode(opc_iushr);				break;
        case op_lushr:					add_opcode(opc_lushr);				break;
        case op_iand:					add_opcode(opc_iand);				break;
        case op_land:					add_opcode(opc_land);				break;
        case op_ior:					add_opcode(opc_ior);				break;
        case op_lor:					add_opcode(opc_lor);				break;
        case op_ixor:					add_opcode(opc_ixor);				break;
        case op_lxor:					add_opcode(opc_lxor);				break;
        case op_return:					generateReturn();
        	reachable = false;
        	break;
        case op_arrlength:				generateLengthOfArray();			break;
        case op_throw:			       	add_opcode(opc_athrow);
        	reachable = false;
			break;
		case op_monitorenter:			add_opcode(opc_monitorenter);		break;
		case op_monitorexit:			add_opcode(opc_monitorexit);		break;
		default:
        	throw new RuntimeException("Bad simple instruction "+i);
	    }
	}

	/** Add pseude-instruction for this code.
	 */
	public void addInstrIncr(JVar vv, int val) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": op_incr "+vv+" "+val);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\"op_incr\" ingnored as unreachable");
			return;
		}
		CodeVar v = vars[vv.bcpos];
		add_opcode_and_short(opc_iinc, v.stack_pos<<8 | ((byte)val & 0xFF) );
	}


	/** Add pseude-instruction with label for this code.
	 */
	public void addInstr(Instr i, CodeLabel l) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i+" -> "+l);
		if( !reachable && !isInfoInstr(i) ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
	    switch(i) {
        case op_ifcmpeq:	generateCompareOp(i,l);			break;
        case op_ifcmpne:	generateCompareOp(i,l);			break;
        case op_ifcmple:	generateCompareOp(i,l);			break;
        case op_ifcmplt:	generateCompareOp(i,l);			break;
        case op_ifcmpge:	generateCompareOp(i,l);			break;
        case op_ifcmpgt:	generateCompareOp(i,l);			break;
		case op_ifeq:     	add_opcode_and_label(opc_ifeq,l);		break;
		case op_ifne:     	add_opcode_and_label(opc_ifne,l);		break;
		case op_ifle:     	add_opcode_and_label(opc_ifle,l);		break;
		case op_ifge:     	add_opcode_and_label(opc_ifge,l);		break;
		case op_iflt:     	add_opcode_and_label(opc_iflt,l);		break;
		case op_ifgt:     	add_opcode_and_label(opc_ifgt,l);		break;
		case op_ifnull:		add_opcode_and_label(opc_ifnull,l);	break;
		case op_ifnonnull:	add_opcode_and_label(opc_ifnonnull,l);	break;
        case op_goto:     	add_opcode_and_label(opc_goto,l);
        	reachable = false;
        	break;
        case op_jsr:		add_opcode_and_label(opc_jsr,l);		break;
		case set_label:
			trace(Kiev.debug && Kiev.debugInstrGen,"Attach label to position "+pc);
			l.attachPosition();
			reachable = true;
			break;
		default:
        	throw new RuntimeException("Bad label-use instruction");
	    }
	}

	public void addInstrLoadThis() {
       	if( method.isStatic() )
       		throw new RuntimeException("Generation of load 'this' in a static method");
		generateLoadVar(0);
	}

	public void addInstrStoreThis() {
       	if( method.isStatic() )
       		throw new RuntimeException("Generation of load 'this' in a static method");
		generateStoreVar(0);
	}

	/** Add pseude-instruction with var for this code.
	 */
	public void addInstr(Instr i, JVar v) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i+" -> "+vars[v.bcpos].var);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
	    switch(i) {
        case op_load:
        	if( vars[v.bcpos] == null )
        		throw new RuntimeException("Generation of unplaced var "+v);
        	generateLoadVar(v.bcpos);
        	break;
        case op_store:
        	if( vars[v.bcpos] == null )
        		throw new RuntimeException("Generation of unplaced var "+v);
        	generateStoreVar(v.bcpos);
        	break;
        case op_ret:
        	add_opcode_and_byte(opc_ret,vars[v.bcpos].stack_pos);
        	reachable = false;
        	break;
		default:
        	throw new RuntimeException("Bad var-use instruction");
	    }
	}

	/** Add pseude-instruction with field for this code.
	 */
	public void addInstr(Instr i, JField f, Type tp) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i+" -> "+f);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
		JType ttt = Type.getRealType(tp.getErasedType(),f.jctx_tdecl.xtype).getJType();
		KString struct_sig = ttt.java_signature;
		KString field_sig = Type.getRealType(f.jctx_tdecl.xtype,f.type).getJType().java_signature;
		FieldCP cpf = constPool.addFieldCP(struct_sig,KString.from(f.sname),field_sig);
	    switch(i) {
        case op_getstatic:
			add_opcode_and_CP(opc_getstatic,cpf);
			stack_push(Type.getRealType(tp,f.type).getJType());
			break;
        case op_putstatic:
			add_opcode_and_CP(opc_putstatic,cpf);
			stack_pop();
			break;
        case op_getfield:
			add_opcode_and_CP(opc_getfield,cpf);
			stack_pop();
			stack_push(Type.getRealType(tp,f.type).getJType());
			break;
        case op_putfield:
			add_opcode_and_CP(opc_putfield,cpf);
			stack_pop();
			stack_pop();
			break;
		default:
        	throw new RuntimeException("Bad field-use instruction");
	    }
	}


	/** Add pseude-instruction with type for this code.
	 */
	public void addInstr(Instr i, Type type) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i+" -> "+type);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
	    switch(i) {
		case op_x2y:
			generatePrimitiveCast(type.getJType());
			break;
		case op_new:
			if( !type.isReference() )
				throw new RuntimeException("New on non-reference type "+type);
			add_opcode_and_CP(opc_new,constPool.getClazzCP(type.getJType().java_signature));
			stack_push(type.getJType());
			break;
		case op_newarray:
			generateNewArray(type.getJType());
			break;
		case op_checkcast:
			if( !type.isReference() )
				throw new RuntimeException("Type "+type+" must be a reference for cast checking");
			if( !type.isReference() )
				break;
			add_opcode_and_CP(opc_checkcast,constPool.addClazzCP(type.getJType().java_signature));
			stack_push(type.getJType());
			break;
		case op_instanceof:
			generateInstanceofOp(type.getJType());
			break;
		default:
			throw new RuntimeException("Bad type-use instruction");
	    }
	}

	/** Add pseude-instruction with type for this code.
	 */
	public void addInstr(Instr i, Type type, int dim) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i+" "+dim+" -> "+type);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
		switch(i) {
		case op_multianewarray:
			generateNewMultiArray(dim,type.getJType());
			break;
		default:
        	throw new RuntimeException("Bad type-use instruction");
	    }
	}

	/** Add pseude-instruction with method call for this code.
	 */
	public void addInstr(Instr i, JMethod method, boolean super_flag) {
		addInstr(i,method,super_flag,method.type);
	}
	/** Add pseude-instruction with method call for this code.
	 */
	public void addInstr(Instr i, JMethod method, boolean super_flag, Type tp) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i+" -> "+(super_flag?"super.":"")+method);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
	    switch(i) {
        case op_call:
        	generateCall(method,super_flag, tp);
        	break;
		default:
        	throw new RuntimeException("Bad call-use instruction");
	    }
	}

	/** Add pseude-instruction with method reference for this code.
	 */
	public void addInstr(Instr i, JMethod method, int nargs, Type tp) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i+" -> "+method);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
       	throw new RuntimeException("Bad call-use instruction");
	}

	/** Add pseude-instruction with switch table for this code.
	 */
	public void addInstr(Instr i, CodeSwitch sw) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
	    switch(i) {
		case op_tableswitch:
			generateTableSwitch((CodeTableSwitch)sw);
			break;
		case op_lookupswitch:
			generateLookupSwitch((CodeLookupSwitch)sw);
			break;
		case switch_close:
			sw.close();
			break;
		default:
        	throw new RuntimeException("Bad switch-use instruction");
	    }
	}

	/** Add pseude-instruction with try/catch for this code.
	 */
	public void addInstr(Instr i, CodeCatchInfo catcher) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+i);
		if( !reachable && !isInfoInstr(i) ) {
			Kiev.reportCodeWarning(this,"\""+i+"\" ingnored as unreachable");
			return;
		}
	    switch(i) {
		case start_catcher:
			catcher.start_pc = pc;
			break;
		case stop_catcher:
			catcher.end_pc = pc;
			break;
		case enter_catch_handler:
			if( catcher != null ) {
				catcher.handler.attachPosition();
				stack_push(catcher.type);
			} else {
				stack_push(JType.tpThrowable);
			}
			reachable = true;
			break;
		case exit_catch_handler:
//			stack_pop();
			break;
		default:
        	throw new RuntimeException("Bad try/catch-use instruction");
	    }
	}


	public void addConst(int val) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+Instr.op_push_iconst+" "+val);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+Instr.op_push_iconst+"\" ingnored as unreachable");
			return;
		}
		switch(val) {
		case -1:	add_opcode(opc_iconst_m1); break;
		case 0:		add_opcode(opc_iconst_0); break;
		case 1:		add_opcode(opc_iconst_1); break;
		case 2:		add_opcode(opc_iconst_2); break;
		case 3:		add_opcode(opc_iconst_3); break;
		case 4:		add_opcode(opc_iconst_4); break;
		case 5:		add_opcode(opc_iconst_5); break;
		default:
			if( val >=  Byte.MIN_VALUE && val <= Byte.MAX_VALUE ) {
				add_opcode_and_byte(opc_bipush,val);
			}
			else if( val >= Short.MIN_VALUE && val <= Short.MAX_VALUE ) {
				add_opcode_and_short(opc_sipush,val);
			}
			else {
				CP c = constPool.addNumberCP(Integer.valueOf(val));
				add_opcode_and_CP(opc_ldc,c);
				stack_push(JType.tpInt);
			}
		}
	}

	public void addConst(long val) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+Instr.op_push_lconst+" "+val);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+Instr.op_push_lconst+"\" ingnored as unreachable");
			return;
		}
		if( val == 0L )			add_opcode(opc_lconst_0);
		else if( val == 1L )	add_opcode(opc_lconst_1);
		else {
			CP c = constPool.addNumberCP(Long.valueOf(val));
			add_opcode_and_CP(opc_ldc2_w,c);
			stack_push(JType.tpLong);
		}
	}

	public void addConst(float val) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+Instr.op_push_fconst+" "+val);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+Instr.op_push_fconst+"\" ingnored as unreachable");
			return;
		}
		if( val == 0.0f )		add_opcode(opc_fconst_0);
		else if( val == 1.0f )	add_opcode(opc_fconst_1);
		else if( val == 2.0f )	add_opcode(opc_fconst_2);
		else {
			CP c = constPool.addNumberCP(Float.valueOf(val));
			add_opcode_and_CP(opc_ldc,c);
			stack_push(JType.tpFloat);
		}
	}

	public void addConst(double val) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+Instr.op_push_dconst+" "+val);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+Instr.op_push_dconst+"\" ingnored as unreachable");
			return;
		}
		if( val == 0.0D )		add_opcode(opc_dconst_0);
		else if( val == 1.0D )	add_opcode(opc_dconst_1);
		else {
			CP c = constPool.addNumberCP(Double.valueOf(val));
			add_opcode_and_CP(opc_ldc2_w,c);
			stack_push(JType.tpDouble);
		}
	}

	public void addConst(KString val) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+Instr.op_push_sconst+" \""+val+"\"");
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+Instr.op_push_sconst+"\" ingnored as unreachable");
			return;
		}
		CP c = constPool.addStringCP(val);
		add_opcode_and_CP(opc_ldc,c);
		stack_push(JType.tpString);
	}

	public void addConst(JType val) {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+Instr.op_push_tconst+" \""+val+"\"");
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+Instr.op_push_tconst+"\" ingnored as unreachable");
			return;
		}
		CP c = constPool.addClazzCP(val.java_signature);
		add_opcode_and_CP(opc_ldc,c);
		stack_push(JType.tpClass);
	}

	public void addNullConst() {
		trace(Kiev.debug && Kiev.debugInstrGen,pc+": "+Instr.op_push_null);
		if( !reachable ) {
			Kiev.reportCodeWarning(this,"\""+Instr.op_push_null+"\" ingnored as unreachable");
			return;
		}
		add_opcode(opc_aconst_null);
		stack_push(JType.tpNull);
	}


	public void generateCode() {
		this.method.addAttr(generateCodeAttr(0));
	}

	public void generateCode(JWBCCondition wbc) {
		wbc.code_attr = generateCodeAttr(wbc.cond);
	}

	private CodeAttr generateCodeAttr(int cond) {
		bcode = (byte[])Arrays.cloneToSize(bcode,pc);
		for(int i=0; i < cur_locals; i++) {
			if( vars[i] == null ) continue;
			if( vars[i].end_pc == -1) vars[i].end_pc = pc;
		}

		if( !cond_generation && !(cond == 1 || cond == 2) ) {
			// Initialize local var table
			if( Kiev.debugOutputV )
				attrs = (Attr[])Arrays.append(attrs,lvta);
			// Initialize line number table
			if( Kiev.debugOutputL ) {
				LinenoTableAttr lnta = new LinenoTableAttr();
				if( line_top >= 0 ) {
					while( line_top >= 0 && (linetable[line_top] >>> 16) >= pc )
						line_top--;
					lnta.table = (int[])Arrays.cloneToSize(linetable,line_top+1);
					attrs = (Attr[])Arrays.append(attrs,lnta);
				}
			}
		}

		CodeAttr ca;
		if( cond == 1 || cond == 2 )
			ca = new ContractAttr(cond, max_stack,(max_locals+1),bcode,attrs);
		else
			ca = new CodeAttr(method, max_stack,(max_locals+1),bcode,catchers,attrs);
		ca.constants = (CP[])Arrays.cloneToSize(constants,constants_top);
		ca.constants_pc = (int[])Arrays.cloneToSize(constants_pc,constants_top);

		// Process all labels
		for(int i=0; i < labels_top; i++) {
			CodeLabel l = labels[i];
			if( l.pc == -1 )
				throw new RuntimeException("Label referenced, but not placed in code");
			pc = labels_pc[i];
			add_code_short(labels[i].pc-pc+1);
		}
		return ca;
	}

	public void importCode(CodeAttr ca) {
		while( constants.length < constants_top+2+ca.constants.length ) {
			constants = (CP[])Arrays.ensureSize(constants,constants.length*2);
			constants_pc = (int[])Arrays.ensureSize(constants_pc,constants.length);
		}
		for(int i=0; i < ca.constants.length; i++) {
			constants[constants_top] = ca.constants[i];
			constants_pc[constants_top] = pc + ca.constants_pc[i];
			constants_top++;
		}
		if( max_locals < ca.max_locals ) max_locals = ca.max_locals;
		if( max_stack < ca.max_stack ) max_stack = ca.max_stack;
		add_opcode_dump(ca.bcode);
		return;
	}

	public static void patchCodeConstants(CodeAttr ca) {
		final byte[] bcode = ca.bcode;
		final CP[]   constants = ca.constants;
		final int[]  constants_pc = ca.constants_pc;
		// Process all constants
		for(int i=0; i < constants.length; i++) {
			CP cp = constants[i];
			int cppos = cp.pos;
			if( cppos < 1 )
				throw new RuntimeException("Constant referenced, but not generated");
			int pc = constants_pc[i];
			trace(Kiev.debug && Kiev.debugInstrGen,pc+": ref CP to pos "+cppos+": "+cp);
			bcode[pc+0] = (byte)(cppos >>> 8);
			bcode[pc+1] = (byte)cppos;
		}
	}

}

