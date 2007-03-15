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

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.be.java15.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public abstract class BEndFunc {
	private static Hashtable<String,BEndFunc> coreFuncs;
	static {
		coreFuncs = new Hashtable<String,BEndFunc>(1024);

//		coreFuncs.put("kiev.stdlib.any:_instanceof_",       AnyInstanceOf);

		coreFuncs.put("kiev.vlang.Globals:ref_assign",      new AssignFunc());
		coreFuncs.put("kiev.vlang.Globals:ref_assign2",     new AssignFunc());
		coreFuncs.put("kiev.vlang.Globals:ref_pvar_init",   new AssignFunc());
//		coreFuncs.put("kiev.stdlib.any:ref_eq",             ObjectBoolEQ);
//		coreFuncs.put("kiev.stdlib.any:ref_neq",            ObjectBoolNE);

		coreFuncs.put("kiev.stdlib.boolean:assign",         new AssignFunc());
		coreFuncs.put("kiev.stdlib.boolean:assign_bit_or",  new AssignWithOpFunc(Instr.op_ior));
		coreFuncs.put("kiev.stdlib.boolean:assign_bit_xor", new AssignWithOpFunc(Instr.op_ixor));
		coreFuncs.put("kiev.stdlib.boolean:assign_bit_and", new AssignWithOpFunc(Instr.op_iand));
		coreFuncs.put("kiev.stdlib.boolean:bit_or",         new BinaryOpFunc(Instr.op_ior));
		coreFuncs.put("kiev.stdlib.boolean:bit_xor",        new BinaryOpFunc(Instr.op_ixor));
		coreFuncs.put("kiev.stdlib.boolean:bit_and",        new BinaryOpFunc(Instr.op_iand));
//		coreFuncs.put("kiev.stdlib.boolean:bool_or",        BoolBoolOR);
//		coreFuncs.put("kiev.stdlib.boolean:bool_and",       BoolBoolAND);
//		coreFuncs.put("kiev.stdlib.boolean:bool_eq",        BoolBoolEQ);
//		coreFuncs.put("kiev.stdlib.boolean:bool_neq",       BoolBoolNE);
//		coreFuncs.put("kiev.stdlib.boolean:bool_not",       BoolBoolNOT);

		coreFuncs.put("kiev.stdlib.char:assign",            new AssignFunc());
		
		coreFuncs.put("kiev.stdlib.byte:assign",            new AssignFunc());
		coreFuncs.put("kiev.stdlib.byte:positive",          new UnaryOpFunc(Instr.op_nop));
		coreFuncs.put("kiev.stdlib.byte:negative",          new UnaryOpFunc(Instr.op_ineg));
		
		coreFuncs.put("kiev.stdlib.short:assign",           new AssignFunc());
		coreFuncs.put("kiev.stdlib.short:positive",         new UnaryOpFunc(Instr.op_nop));
		coreFuncs.put("kiev.stdlib.short:negative",         new UnaryOpFunc(Instr.op_ineg));
		
		coreFuncs.put("kiev.stdlib.int:assign",             new AssignFunc());
		coreFuncs.put("kiev.stdlib.int:assign_bit_or",      new AssignWithOpFunc(Instr.op_ior));
		coreFuncs.put("kiev.stdlib.int:assign_bit_xor",     new AssignWithOpFunc(Instr.op_ixor));
		coreFuncs.put("kiev.stdlib.int:assign_bit_and",     new AssignWithOpFunc(Instr.op_iand));
		coreFuncs.put("kiev.stdlib.int:assign_left_shift",             new AssignWithOpFunc(Instr.op_ishl));
		coreFuncs.put("kiev.stdlib.int:assign_right_shift",            new AssignWithOpFunc(Instr.op_ishr));
		coreFuncs.put("kiev.stdlib.int:assign_unsigned_right_shift",   new AssignWithOpFunc(Instr.op_iushr));
		coreFuncs.put("kiev.stdlib.int:assign_add",         new AssignIntOpFunc(Instr.op_iadd));
		coreFuncs.put("kiev.stdlib.int:assign_sub",         new AssignIntOpFunc(Instr.op_isub));
		coreFuncs.put("kiev.stdlib.int:assign_mul",         new AssignWithOpFunc(Instr.op_imul));
		coreFuncs.put("kiev.stdlib.int:assign_div",         new AssignWithOpFunc(Instr.op_idiv));
		coreFuncs.put("kiev.stdlib.int:assign_mod",         new AssignWithOpFunc(Instr.op_irem));
		coreFuncs.put("kiev.stdlib.int:bit_or",             new BinaryOpFunc(Instr.op_ior));
		coreFuncs.put("kiev.stdlib.int:bit_xor",            new BinaryOpFunc(Instr.op_ixor));
		coreFuncs.put("kiev.stdlib.int:bit_and",            new BinaryOpFunc(Instr.op_iand));
		coreFuncs.put("kiev.stdlib.int:bit_not",            new IntBitNOT());
//		coreFuncs.put("kiev.stdlib.int:bool_eq",            IntBoolEQ);
//		coreFuncs.put("kiev.stdlib.int:bool_neq",           IntBoolNE);
//		coreFuncs.put("kiev.stdlib.int:bool_ge",            IntBoolGE);
//		coreFuncs.put("kiev.stdlib.int:bool_le",            IntBoolLE);
//		coreFuncs.put("kiev.stdlib.int:bool_gt",            IntBoolGT);
//		coreFuncs.put("kiev.stdlib.int:bool_lt",            IntBoolLT);
		coreFuncs.put("kiev.stdlib.int:left_shift",               new BinaryOpFunc(Instr.op_ishl));
		coreFuncs.put("kiev.stdlib.int:right_shift",              new BinaryOpFunc(Instr.op_ishr));
		coreFuncs.put("kiev.stdlib.int:unsigned_right_shift",     new BinaryOpFunc(Instr.op_iushr));
		coreFuncs.put("kiev.stdlib.int:add",                new BinaryOpFunc(Instr.op_iadd));
		coreFuncs.put("kiev.stdlib.int:sub",                new BinaryOpFunc(Instr.op_isub));
		coreFuncs.put("kiev.stdlib.int:mul",                new BinaryOpFunc(Instr.op_imul));
		coreFuncs.put("kiev.stdlib.int:div",                new BinaryOpFunc(Instr.op_idiv));
		coreFuncs.put("kiev.stdlib.int:mod",                new BinaryOpFunc(Instr.op_irem));
		coreFuncs.put("kiev.stdlib.int:positive",           new UnaryOpFunc(Instr.op_nop));
		coreFuncs.put("kiev.stdlib.int:negative",           new UnaryOpFunc(Instr.op_ineg));
		coreFuncs.put("kiev.stdlib.int:pre_incr",           new IntPreIncrFunc(Instr.op_iadd, 1));
		coreFuncs.put("kiev.stdlib.int:pre_decr",           new IntPreIncrFunc(Instr.op_iadd, -1));
		coreFuncs.put("kiev.stdlib.int:post_incr",          new IntPostIncrFunc(Instr.op_iadd, 1));
		coreFuncs.put("kiev.stdlib.int:post_decr",          new IntPostIncrFunc(Instr.op_iadd, -1));
		
		coreFuncs.put("kiev.stdlib.long:assign",            new AssignFunc());
		coreFuncs.put("kiev.stdlib.long:assign_bit_or",     new AssignWithOpFunc(Instr.op_lor));
		coreFuncs.put("kiev.stdlib.long:assign_bit_xor",    new AssignWithOpFunc(Instr.op_lxor));
		coreFuncs.put("kiev.stdlib.long:assign_bit_and",    new AssignWithOpFunc(Instr.op_land));
		coreFuncs.put("kiev.stdlib.long:assign_left_shift",            new AssignWithOpFunc(Instr.op_lshl));
		coreFuncs.put("kiev.stdlib.long:assign_right_shift",           new AssignWithOpFunc(Instr.op_lshr));
		coreFuncs.put("kiev.stdlib.long:assign_unsigned_right_shift",  new AssignWithOpFunc(Instr.op_lushr));
		coreFuncs.put("kiev.stdlib.long:assign_add",        new AssignWithOpFunc(Instr.op_ladd));
		coreFuncs.put("kiev.stdlib.long:assign_sub",        new AssignWithOpFunc(Instr.op_lsub));
		coreFuncs.put("kiev.stdlib.long:assign_mul",        new AssignWithOpFunc(Instr.op_lmul));
		coreFuncs.put("kiev.stdlib.long:assign_div",        new AssignWithOpFunc(Instr.op_ldiv));
		coreFuncs.put("kiev.stdlib.long:assign_mod",        new AssignWithOpFunc(Instr.op_lrem));
		coreFuncs.put("kiev.stdlib.long:bit_or",            new BinaryOpFunc(Instr.op_lor));
		coreFuncs.put("kiev.stdlib.long:bit_xor",           new BinaryOpFunc(Instr.op_lxor));
		coreFuncs.put("kiev.stdlib.long:bit_and",           new BinaryOpFunc(Instr.op_land));
		coreFuncs.put("kiev.stdlib.long:bit_not",           new LongBitNOT());
//		coreFuncs.put("kiev.stdlib.long:bool_eq",           LongBoolEQ);
//		coreFuncs.put("kiev.stdlib.long:bool_neq",          LongBoolNE);
//		coreFuncs.put("kiev.stdlib.long:bool_ge",           LongBoolGE);
//		coreFuncs.put("kiev.stdlib.long:bool_le",           LongBoolLE);
//		coreFuncs.put("kiev.stdlib.long:bool_gt",           LongBoolGT);
//		coreFuncs.put("kiev.stdlib.long:bool_lt",           LongBoolLT);
		coreFuncs.put("kiev.stdlib.long:left_shift",              new BinaryOpFunc(Instr.op_lshl));
		coreFuncs.put("kiev.stdlib.long:right_shift",             new BinaryOpFunc(Instr.op_lshr));
		coreFuncs.put("kiev.stdlib.long:unsigned_right_shift",    new BinaryOpFunc(Instr.op_lushr));
		coreFuncs.put("kiev.stdlib.long:add",               new BinaryOpFunc(Instr.op_ladd));
		coreFuncs.put("kiev.stdlib.long:sub",               new BinaryOpFunc(Instr.op_lsub));
		coreFuncs.put("kiev.stdlib.long:mul",               new BinaryOpFunc(Instr.op_lmul));
		coreFuncs.put("kiev.stdlib.long:div",               new BinaryOpFunc(Instr.op_ldiv));
		coreFuncs.put("kiev.stdlib.long:mod",               new BinaryOpFunc(Instr.op_lrem));
		coreFuncs.put("kiev.stdlib.long:positive",          new UnaryOpFunc(Instr.op_nop));
		coreFuncs.put("kiev.stdlib.long:negative",          new UnaryOpFunc(Instr.op_lneg));
		coreFuncs.put("kiev.stdlib.long:pre_incr",          new LongPreIncrFunc(Instr.op_ladd, 1));
		coreFuncs.put("kiev.stdlib.long:pre_decr",          new LongPreIncrFunc(Instr.op_ladd, -1));
		coreFuncs.put("kiev.stdlib.long:post_incr",         new LongPostIncrFunc(Instr.op_ladd, 1));
		coreFuncs.put("kiev.stdlib.long:post_decr",         new LongPostIncrFunc(Instr.op_ladd, -1));
		
		coreFuncs.put("kiev.stdlib.float:assign",           new AssignFunc());
		coreFuncs.put("kiev.stdlib.float:assign_add",       new AssignWithOpFunc(Instr.op_fadd));
		coreFuncs.put("kiev.stdlib.float:assign_sub",       new AssignWithOpFunc(Instr.op_fsub));
		coreFuncs.put("kiev.stdlib.float:assign_mul",       new AssignWithOpFunc(Instr.op_fmul));
		coreFuncs.put("kiev.stdlib.float:assign_div",       new AssignWithOpFunc(Instr.op_fdiv));
		coreFuncs.put("kiev.stdlib.float:assign_mod",       new AssignWithOpFunc(Instr.op_frem));
//		coreFuncs.put("kiev.stdlib.float:bool_eq",          FloatBoolEQ);
//		coreFuncs.put("kiev.stdlib.float:bool_neq",         FloatBoolNE);
//		coreFuncs.put("kiev.stdlib.float:bool_ge",          FloatBoolGE);
//		coreFuncs.put("kiev.stdlib.float:bool_le",          FloatBoolLE);
//		coreFuncs.put("kiev.stdlib.float:bool_gt",          FloatBoolGT);
//		coreFuncs.put("kiev.stdlib.float:bool_lt",          FloatBoolLT);
		coreFuncs.put("kiev.stdlib.float:add",              new BinaryOpFunc(Instr.op_fadd));
		coreFuncs.put("kiev.stdlib.float:sub",              new BinaryOpFunc(Instr.op_fsub));
		coreFuncs.put("kiev.stdlib.float:mul",              new BinaryOpFunc(Instr.op_fmul));
		coreFuncs.put("kiev.stdlib.float:div",              new BinaryOpFunc(Instr.op_fdiv));
		coreFuncs.put("kiev.stdlib.float:mod",              new BinaryOpFunc(Instr.op_frem));
		coreFuncs.put("kiev.stdlib.float:positive",         new UnaryOpFunc(Instr.op_nop));
		coreFuncs.put("kiev.stdlib.float:negative",         new UnaryOpFunc(Instr.op_fneg));
		
		coreFuncs.put("kiev.stdlib.double:assign",          new AssignFunc());
		coreFuncs.put("kiev.stdlib.double:assign_add",      new AssignWithOpFunc(Instr.op_dadd));
		coreFuncs.put("kiev.stdlib.double:assign_sub",      new AssignWithOpFunc(Instr.op_dsub));
		coreFuncs.put("kiev.stdlib.double:assign_mul",      new AssignWithOpFunc(Instr.op_dmul));
		coreFuncs.put("kiev.stdlib.double:assign_div",      new AssignWithOpFunc(Instr.op_ddiv));
		coreFuncs.put("kiev.stdlib.double:assign_mod",      new AssignWithOpFunc(Instr.op_drem));
//		coreFuncs.put("kiev.stdlib.double:bool_eq",         DoubleBoolEQ);
//		coreFuncs.put("kiev.stdlib.double:bool_neq",        DoubleBoolNE);
//		coreFuncs.put("kiev.stdlib.double:bool_ge",         DoubleBoolGE);
//		coreFuncs.put("kiev.stdlib.double:bool_le",         DoubleBoolLE);
//		coreFuncs.put("kiev.stdlib.double:bool_gt",         DoubleBoolGT);
//		coreFuncs.put("kiev.stdlib.double:bool_lt",         DoubleBoolLT);
		coreFuncs.put("kiev.stdlib.double:add",             new BinaryOpFunc(Instr.op_dadd));
		coreFuncs.put("kiev.stdlib.double:sub",             new BinaryOpFunc(Instr.op_dsub));
		coreFuncs.put("kiev.stdlib.double:mul",             new BinaryOpFunc(Instr.op_dmul));
		coreFuncs.put("kiev.stdlib.double:div",             new BinaryOpFunc(Instr.op_ddiv));
		coreFuncs.put("kiev.stdlib.double:mod",             new BinaryOpFunc(Instr.op_drem));
		coreFuncs.put("kiev.stdlib.double:positive",        new UnaryOpFunc(Instr.op_nop));
		coreFuncs.put("kiev.stdlib.double:negative",        new UnaryOpFunc(Instr.op_dneg));
	}

	public static void attachToBackend(CoreMethod cm) {
		String name = ((TypeDecl)cm.parent()).qname()+":"+cm.id;
		BEndFunc cf = coreFuncs.get(name);
		if (cf == null) {
			cf = UnimplementedFunc; //Kiev.reportWarning(cm,"Backend function "+name+" not found");
			return;
		}
		cf.core_method = cm;
		cm.bend_func = cf;
	}

	public CoreMethod core_method;

	public abstract void generate(Code code, Type reqType, JENode expr);
	
	final JENode[] getJArgs(JENode expr) {
		ENode[] args = ((ENode)expr).getArgs();
		JENode[] jargs = new JENode[args.length];
		for (int i=0; i < args.length; i++)
			jargs[i] = (JENode)args[i];
		return jargs;
	}
	
}

@singleton
final class UnimplementedFunc extends BEndFunc {
	public void generate(Code code, Type reqType, JENode expr) {
		Kiev.reportError(expr, "Unsupported core function "+this+" for java backend");
	}
}

final class AssignFunc extends BEndFunc {
	public void generate(Code code, Type reqType, JENode expr) {
		JENode[] args = this.getJArgs(expr);
		JLvalueExpr lval = (JLvalueExpr)args[0];
		JENode value = args[1];
		lval.generateAccess(code);
		value.generate(code,null);
		if( reqType ≢ Type.tpVoid )
			lval.generateStoreDupValue(code);
		else
			lval.generateStore(code);
	}
}

final class AssignWithOpFunc extends BEndFunc {
	private final Instr instr;
	AssignWithOpFunc(Instr instr) { this.instr = instr; }
	public void generate(Code code, Type reqType, JENode expr) {
		JENode[] args = this.getJArgs(expr);
		JLvalueExpr lval = (JLvalueExpr)args[0];
		JENode value = args[1];
		lval.generateLoadDup(code);
		value.generate(code,null);
		code.addInstr(instr);
		if( reqType ≢ Type.tpVoid )
			lval.generateStoreDupValue(code);
		else
			lval.generateStore(code);
	}
}

final class AssignIntOpFunc extends BEndFunc {
	private final Instr instr;
	AssignIntOpFunc(Instr instr) { this.instr = instr; }
	public void generate(Code code, Type reqType, JENode expr) {
		JENode[] args = this.getJArgs(expr);
		JLvalueExpr lval = (JLvalueExpr)args[0];
		JENode value = args[1];
		if( lval instanceof JLVarExpr && value instanceof JConstExpr) {
			JLVarExpr va = (JLVarExpr)lval;
			if( !va.var.isNeedProxy() ) {
				int val = ((Number)((JConstExpr)value).getConstValue()).intValue();
				if (instr == Instr.op_isub) val = -val;
				if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
					IntPreIncrFunc.genVarIncr(code, reqType, va, val);
					return;
				}
			}
		}
		// like in AssignWithOpFunc
		lval.generateLoadDup(code);
		value.generate(code,null);
		code.addInstr(instr);
		if( reqType ≢ Type.tpVoid )
			lval.generateStoreDupValue(code);
		else
			lval.generateStore(code);
	}
}

final class BinaryOpFunc extends BEndFunc {
	private final Instr instr;
	BinaryOpFunc(Instr instr) { this.instr = instr; }
	public void generate(Code code, Type reqType, JENode expr) {
		JENode[] args = this.getJArgs(expr);
		JENode expr1 = args[0];
		JENode expr2 = args[1];
		expr1.generate(code,null);
		expr2.generate(code,null);
		code.addInstr(instr);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}
}

final class UnaryOpFunc extends BEndFunc {
	private final Instr instr;
	UnaryOpFunc(Instr instr) { this.instr = instr; }
	public void generate(Code code, Type reqType, JENode expr) {
		JENode[] args = this.getJArgs(expr);
		JENode expr = args[0];
		expr.generate(code,null);
		if (instr != Instr.op_nop)
			code.addInstr(instr);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}
}

final class IntBitNOT extends BEndFunc {
	public void generate(Code code, Type reqType, JENode expr) {
		JENode[] args = this.getJArgs(expr);
		JENode expr = args[0];
		expr.generate(code,null);
		code.addConst(-1);
		code.addInstr(Instr.op_ixor);
	}
}

final class LongBitNOT extends BEndFunc {
	public void generate(Code code, Type reqType, JENode expr) {
		JENode[] args = this.getJArgs(expr);
		JENode expr = args[0];
		expr.generate(code,null);
		code.addConst(-1L);
		code.addInstr(Instr.op_lxor);
	}
}


abstract class IncrFunc extends BEndFunc {
	final Instr instr;
	IncrFunc(Instr instr) { this.instr = instr; }
	abstract void genIncr(Code code, Type reqType, JLvalueExpr lval);
	abstract void pushProperConstant(Code code);
	public void generate(Code code, Type reqType, JENode expr) {
		JLvalueExpr lval = (JLvalueExpr)this.getJArgs(expr)[0];
		genIncr(code, reqType, lval);
	}
}

abstract class PreIncrFunc extends IncrFunc {
	PreIncrFunc(Instr instr) { super(instr); }
	public void genIncr(Code code, Type reqType, JLvalueExpr lval) {
		if( reqType ≢ Type.tpVoid ) {
			lval.generateLoadDup(code);
			pushProperConstant(code);
			code.addInstr(instr);
			lval.generateStoreDupValue(code);
		} else {
			lval.generateLoadDup(code);
			pushProperConstant(code);
			code.addInstr(instr);
			lval.generateStore(code);
		}
	}
}

abstract class PostIncrFunc extends IncrFunc {
	PostIncrFunc(Instr instr) { super(instr); }
	abstract JVar makeTempVar();
	public void genIncr(Code code, Type reqType, JLvalueExpr lval) {
		if( reqType ≢ Type.tpVoid ) {
			lval.generateLoadDup(code);
			code.addInstr(Instr.op_dup);
			JVar tmp_var = makeTempVar();
			code.addVar(tmp_var);
			code.addInstr(Instr.op_store,tmp_var);
			pushProperConstant(code);
			code.addInstr(instr);
			lval.generateStore(code);
			code.addInstr(Instr.op_load,tmp_var);
			code.removeVar(tmp_var);
		} else {
			lval.generateLoadDup(code);
			pushProperConstant(code);
			code.addInstr(instr);
			lval.generateStore(code);
		}
	}
}

final class IntPreIncrFunc extends PreIncrFunc {
	private final int val;
	IntPreIncrFunc(Instr instr, int val) { super(instr); this.val = val; }
	void pushProperConstant(Code code) { code.addConst(val); }
	public void generate(Code code, Type reqType, JENode expr) {
		JLvalueExpr lval = (JLvalueExpr)this.getJArgs(expr)[0];
		if( lval instanceof JLVarExpr ) {
			JLVarExpr va = (JLVarExpr)lval;
			if( !va.var.isNeedProxy() ) {
				genVarIncr(code, reqType, va, val);
				return;
			}
		}
		genIncr(code, reqType, lval);
	}
	public static void genVarIncr(Code code, Type reqType, JLVarExpr va, int val) {
		code.addInstrIncr(va.var,val);
		if( reqType ≢ Type.tpVoid )
			code.addInstr(op_load,va.var);
	}
}

final class IntPostIncrFunc extends PostIncrFunc {
	private final int val;
	IntPostIncrFunc(Instr instr, int val) { super(instr); this.val = val; }
	void pushProperConstant(Code code) { code.addConst(val); }
	JVar makeTempVar() { return (JVar)new LVar(0,"",Type.tpInt,Var.VAR_LOCAL,0); }
	public void generate(Code code, Type reqType, JENode expr) {
		JLvalueExpr lval = (JLvalueExpr)this.getJArgs(expr)[0];
		if( lval instanceof JLVarExpr ) {
			JLVarExpr va = (JLVarExpr)lval;
			if( !va.var.isNeedProxy() ) {
				genVarIncr(code, reqType, va, val);
				return;
			}
		}
		genIncr(code, reqType, lval);
	}
	public static void genVarIncr(Code code, Type reqType, JLVarExpr va, int val) {
		if( reqType ≢ Type.tpVoid )
			code.addInstr(op_load,va.var);
		code.addInstrIncr(va.var,val);
	}
}

final class LongPreIncrFunc extends PreIncrFunc {
	private final long val;
	LongPreIncrFunc(Instr instr, long val) { super(instr); this.val = val; }
	void pushProperConstant(Code code) { code.addConst(val); }
}

final class LongPostIncrFunc extends PostIncrFunc {
	private final long val;
	LongPostIncrFunc(Instr instr, long val) { super(instr); this.val = val; }
	void pushProperConstant(Code code) { code.addConst(val); }
	JVar makeTempVar() { return (JVar)new LVar(0,"",Type.tpLong,Var.VAR_LOCAL,0); }
}


