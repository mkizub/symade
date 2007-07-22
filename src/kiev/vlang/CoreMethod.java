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
package kiev.vlang;

import kiev.ir.java15.RCoreExpr;
import kiev.be.java15.BEndFunc;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node(lang=CoreLang)
public final class CoreExpr extends ENode {
	
	@virtual typedef This  = CoreExpr;
	@virtual typedef RView = RCoreExpr;

	public CoreFunc core_func;
	public BEndFunc bend_func;
	
	public CoreExpr() {}
	
	public void callbackChildChanged(AttrSlot attr) {
		if (attr.name == "ident") {
			String id = this.ident;
			if (id != null) {
				this.core_func = CoreFunc.coreFuncs.get(id);
				this.bend_func = BEndFunc.coreFuncs.get(id);
			} else {
				this.core_func = null;
				this.bend_func = null;
			}
		}
	}

	public Method getMethod() {
		ANode p = parent();
		if (p instanceof Method)
			return (Method)p;
		return null;
	}
	
	public ConstExpr calc(ENode expr) {
		return core_func.calc(expr);
	}
	
	public static CoreExpr makeInstance(int pos, String name) {
		CoreExpr ce = new CoreExpr();
		ce.pos = pos;
		ce.ident = name;
		return ce;
	}
}

public abstract class CoreFunc {
	public static Hashtable<String,CoreFunc> coreFuncs;
	static {
		coreFuncs = new Hashtable<String,CoreFunc>(1024);

		coreFuncs.put("kiev.stdlib.any:_instanceof_",       AnyInstanceOf);

		coreFuncs.put("kiev.vlang.Globals:ref_assign",      ObjectAssign);
		coreFuncs.put("kiev.vlang.Globals:ref_assign2",     ObjectAssign2);
		coreFuncs.put("kiev.vlang.Globals:ref_pvar_init",   ObjectPvarINIT);
		coreFuncs.put("kiev.stdlib.any:ref_eq",             ObjectBoolEQ);
		coreFuncs.put("kiev.stdlib.any:ref_neq",            ObjectBoolNE);

		coreFuncs.put("kiev.stdlib.boolean:assign",         BoolAssign);
		coreFuncs.put("kiev.stdlib.boolean:assign_bit_or",  BoolAssignBitOR);
		coreFuncs.put("kiev.stdlib.boolean:assign_bit_xor", BoolAssignBitXOR);
		coreFuncs.put("kiev.stdlib.boolean:assign_bit_and", BoolAssignBitAND);
		coreFuncs.put("kiev.stdlib.boolean:bit_or",         BoolBitOR);
		coreFuncs.put("kiev.stdlib.boolean:bit_xor",        BoolBitXOR);
		coreFuncs.put("kiev.stdlib.boolean:bit_and",        BoolBitAND);
		coreFuncs.put("kiev.stdlib.boolean:bool_or",        BoolBoolOR);
		coreFuncs.put("kiev.stdlib.boolean:bool_and",       BoolBoolAND);
		coreFuncs.put("kiev.stdlib.boolean:bool_eq",        BoolBoolEQ);
		coreFuncs.put("kiev.stdlib.boolean:bool_neq",       BoolBoolNE);
		coreFuncs.put("kiev.stdlib.boolean:bool_not",       BoolBoolNOT);

		coreFuncs.put("kiev.stdlib.char:assign",            CharAssign);
		
		coreFuncs.put("kiev.stdlib.byte:assign",            ByteAssign);
		coreFuncs.put("kiev.stdlib.byte:positive",          BytePOS);
		coreFuncs.put("kiev.stdlib.byte:negative",          ByteNEG);
		
		coreFuncs.put("kiev.stdlib.short:assign",           ShortAssign);
		coreFuncs.put("kiev.stdlib.short:positive",         ShortPOS);
		coreFuncs.put("kiev.stdlib.short:negative",         ShortNEG);
		
		coreFuncs.put("kiev.stdlib.int:assign",             IntAssign);
		coreFuncs.put("kiev.stdlib.int:assign_bit_or",      IntAssignBitOR);
		coreFuncs.put("kiev.stdlib.int:assign_bit_xor",     IntAssignBitXOR);
		coreFuncs.put("kiev.stdlib.int:assign_bit_and",     IntAssignBitAND);
		coreFuncs.put("kiev.stdlib.int:assign_left_shift",  IntAssignLShift);
		coreFuncs.put("kiev.stdlib.int:assign_right_shift", IntAssignRShift);
		coreFuncs.put("kiev.stdlib.int:assign_unsigned_right_shift",     IntAssignUShift);
		coreFuncs.put("kiev.stdlib.int:assign_add",         IntAssignADD);
		coreFuncs.put("kiev.stdlib.int:assign_sub",         IntAssignSUB);
		coreFuncs.put("kiev.stdlib.int:assign_mul",         IntAssignMUL);
		coreFuncs.put("kiev.stdlib.int:assign_div",         IntAssignDIV);
		coreFuncs.put("kiev.stdlib.int:assign_mod",         IntAssignMOD);
		coreFuncs.put("kiev.stdlib.int:bit_or",             IntBitOR);
		coreFuncs.put("kiev.stdlib.int:bit_xor",            IntBitXOR);
		coreFuncs.put("kiev.stdlib.int:bit_and",            IntBitAND);
		coreFuncs.put("kiev.stdlib.int:bit_not",            IntBitNOT);
		coreFuncs.put("kiev.stdlib.int:bool_eq",            IntBoolEQ);
		coreFuncs.put("kiev.stdlib.int:bool_neq",           IntBoolNE);
		coreFuncs.put("kiev.stdlib.int:bool_ge",            IntBoolGE);
		coreFuncs.put("kiev.stdlib.int:bool_le",            IntBoolLE);
		coreFuncs.put("kiev.stdlib.int:bool_gt",            IntBoolGT);
		coreFuncs.put("kiev.stdlib.int:bool_lt",            IntBoolLT);
		coreFuncs.put("kiev.stdlib.int:left_shift",         IntLShift);
		coreFuncs.put("kiev.stdlib.int:right_shift",        IntRShift);
		coreFuncs.put("kiev.stdlib.int:unsigned_right_shift", IntUShift);
		coreFuncs.put("kiev.stdlib.int:add",                IntADD);
		coreFuncs.put("kiev.stdlib.int:sub",                IntSUB);
		coreFuncs.put("kiev.stdlib.int:mul",                IntMUL);
		coreFuncs.put("kiev.stdlib.int:div",                IntDIV);
		coreFuncs.put("kiev.stdlib.int:mod",                IntMOD);
		coreFuncs.put("kiev.stdlib.int:positive",           IntPOS);
		coreFuncs.put("kiev.stdlib.int:negative",           IntNEG);
		coreFuncs.put("kiev.stdlib.int:pre_incr",           IntPreINCR);
		coreFuncs.put("kiev.stdlib.int:pre_decr",           IntPreDECR);
		coreFuncs.put("kiev.stdlib.int:post_incr",          IntPostINCR);
		coreFuncs.put("kiev.stdlib.int:post_decr",          IntPostDECR);
		
		coreFuncs.put("kiev.stdlib.long:assign",            LongAssign);
		coreFuncs.put("kiev.stdlib.long:assign_bit_or",     LongAssignBitOR);
		coreFuncs.put("kiev.stdlib.long:assign_bit_xor",    LongAssignBitXOR);
		coreFuncs.put("kiev.stdlib.long:assign_bit_and",    LongAssignBitAND);
		coreFuncs.put("kiev.stdlib.long:assign_left_shift", LongAssignLShift);
		coreFuncs.put("kiev.stdlib.long:assign_right_shift",LongAssignRShift);
		coreFuncs.put("kiev.stdlib.long:assign_unsigned_right_shift",   LongAssignUShift);
		coreFuncs.put("kiev.stdlib.long:assign_add",        LongAssignADD);
		coreFuncs.put("kiev.stdlib.long:assign_sub",        LongAssignSUB);
		coreFuncs.put("kiev.stdlib.long:assign_mul",        LongAssignMUL);
		coreFuncs.put("kiev.stdlib.long:assign_div",        LongAssignDIV);
		coreFuncs.put("kiev.stdlib.long:assign_mod",        LongAssignMOD);
		coreFuncs.put("kiev.stdlib.long:bit_or",            LongBitOR);
		coreFuncs.put("kiev.stdlib.long:bit_xor",           LongBitXOR);
		coreFuncs.put("kiev.stdlib.long:bit_and",           LongBitAND);
		coreFuncs.put("kiev.stdlib.long:bit_not",           LongBitNOT);
		coreFuncs.put("kiev.stdlib.long:bool_eq",           LongBoolEQ);
		coreFuncs.put("kiev.stdlib.long:bool_neq",          LongBoolNE);
		coreFuncs.put("kiev.stdlib.long:bool_ge",           LongBoolGE);
		coreFuncs.put("kiev.stdlib.long:bool_le",           LongBoolLE);
		coreFuncs.put("kiev.stdlib.long:bool_gt",           LongBoolGT);
		coreFuncs.put("kiev.stdlib.long:bool_lt",           LongBoolLT);
		coreFuncs.put("kiev.stdlib.long:left_shift",        LongLShift);
		coreFuncs.put("kiev.stdlib.long:right_shift",       LongRShift);
		coreFuncs.put("kiev.stdlib.long:unsigned_right_shift", LongUShift);
		coreFuncs.put("kiev.stdlib.long:add",               LongADD);
		coreFuncs.put("kiev.stdlib.long:sub",               LongSUB);
		coreFuncs.put("kiev.stdlib.long:mul",               LongMUL);
		coreFuncs.put("kiev.stdlib.long:div",               LongDIV);
		coreFuncs.put("kiev.stdlib.long:mod",               LongMOD);
		coreFuncs.put("kiev.stdlib.long:positive",          LongPOS);
		coreFuncs.put("kiev.stdlib.long:negative",          LongNEG);
		coreFuncs.put("kiev.stdlib.long:pre_incr",          LongPreINCR);
		coreFuncs.put("kiev.stdlib.long:pre_decr",          LongPreDECR);
		coreFuncs.put("kiev.stdlib.long:post_incr",         LongPostINCR);
		coreFuncs.put("kiev.stdlib.long:post_decr",         LongPostDECR);
		
		coreFuncs.put("kiev.stdlib.float:assign",           FloatAssign);
		coreFuncs.put("kiev.stdlib.float:assign_add",       FloatAssignADD);
		coreFuncs.put("kiev.stdlib.float:assign_sub",       FloatAssignSUB);
		coreFuncs.put("kiev.stdlib.float:assign_mul",       FloatAssignMUL);
		coreFuncs.put("kiev.stdlib.float:assign_div",       FloatAssignDIV);
		coreFuncs.put("kiev.stdlib.float:assign_mod",       FloatAssignMOD);
		coreFuncs.put("kiev.stdlib.float:bool_eq",          FloatBoolEQ);
		coreFuncs.put("kiev.stdlib.float:bool_neq",         FloatBoolNE);
		coreFuncs.put("kiev.stdlib.float:bool_ge",          FloatBoolGE);
		coreFuncs.put("kiev.stdlib.float:bool_le",          FloatBoolLE);
		coreFuncs.put("kiev.stdlib.float:bool_gt",          FloatBoolGT);
		coreFuncs.put("kiev.stdlib.float:bool_lt",          FloatBoolLT);
		coreFuncs.put("kiev.stdlib.float:add",              FloatADD);
		coreFuncs.put("kiev.stdlib.float:sub",              FloatSUB);
		coreFuncs.put("kiev.stdlib.float:mul",              FloatMUL);
		coreFuncs.put("kiev.stdlib.float:div",              FloatDIV);
		coreFuncs.put("kiev.stdlib.float:mod",              FloatMOD);
		coreFuncs.put("kiev.stdlib.float:positive",         FloatPOS);
		coreFuncs.put("kiev.stdlib.float:negative",         FloatNEG);
		
		coreFuncs.put("kiev.stdlib.double:assign",          DoubleAssign);
		coreFuncs.put("kiev.stdlib.double:assign_add",      DoubleAssignADD);
		coreFuncs.put("kiev.stdlib.double:assign_sub",      DoubleAssignSUB);
		coreFuncs.put("kiev.stdlib.double:assign_mul",      DoubleAssignMUL);
		coreFuncs.put("kiev.stdlib.double:assign_div",      DoubleAssignDIV);
		coreFuncs.put("kiev.stdlib.double:assign_mod",      DoubleAssignMOD);
		coreFuncs.put("kiev.stdlib.double:bool_eq",         DoubleBoolEQ);
		coreFuncs.put("kiev.stdlib.double:bool_neq",        DoubleBoolNE);
		coreFuncs.put("kiev.stdlib.double:bool_ge",         DoubleBoolGE);
		coreFuncs.put("kiev.stdlib.double:bool_le",         DoubleBoolLE);
		coreFuncs.put("kiev.stdlib.double:bool_gt",         DoubleBoolGT);
		coreFuncs.put("kiev.stdlib.double:bool_lt",         DoubleBoolLT);
		coreFuncs.put("kiev.stdlib.double:add",             DoubleADD);
		coreFuncs.put("kiev.stdlib.double:sub",             DoubleSUB);
		coreFuncs.put("kiev.stdlib.double:mul",             DoubleMUL);
		coreFuncs.put("kiev.stdlib.double:div",             DoubleDIV);
		coreFuncs.put("kiev.stdlib.double:mod",             DoubleMOD);
		coreFuncs.put("kiev.stdlib.double:positive",        DoublePOS);
		coreFuncs.put("kiev.stdlib.double:negative",        DoubleNEG);

		coreFuncs.put("kiev.stdlib.GString:str_concat_ss",  StringConcatSS);
		coreFuncs.put("kiev.stdlib.GString:str_concat_as",  StringConcatAS);
		coreFuncs.put("kiev.stdlib.GString:str_concat_sa",  StringConcatSA);
		coreFuncs.put("kiev.stdlib.GString:str_assign_add", StringAssignADD);

		coreFuncs.put("kiev.vlang.Globals:node_ref_eq",       ASTNodeEQ);
		coreFuncs.put("kiev.vlang.Globals:node_ref_neq",      ASTNodeNE);
		coreFuncs.put("kiev.vlang.Globals:node_ref_eq_null",  ASTNodeEQ);
		coreFuncs.put("kiev.vlang.Globals:node_ref_neq_null", ASTNodeNE);
	}
	
	public abstract void normilizeExpr(Method core_method, ENode expr);
	public abstract ConstExpr calc(ENode expr);
}

abstract class BinaryFunc extends CoreFunc {
	public void normilizeExpr(Method core_method, ENode expr, Class cls, Operator op) {
		if (expr.getClass() == cls) {
			expr.symbol = core_method;
			return;
		}
		ENode[] args = expr.getArgs();
		if (args == null || args.length != 2) {
			Kiev.reportError(expr, "Don't know how to normalize "+expr.getClass()+" into "+cls);
			return;
		}
		ENode en = (ENode)cls.newInstance();
		for (int i=0; i < args.length; i++)
			args[i] = args[i].detach();
		en.initFrom(expr, op, core_method, args);
		expr.replaceWithNodeReWalk(en);
	}
	public ConstExpr calc(ENode expr) {
		ENode[] args = expr.getArgs();
		return doCalc(args[0].getConstValue(), args[1].getConstValue());
	}
	protected ConstExpr doCalc(Object:Object arg1, Object:Object arg2) {
		throw new RuntimeException("Cannot calculate a const from "+arg1+" and "+arg2);
	}
}

abstract class UnaryFunc extends CoreFunc {
	public void normilizeExpr(Method core_method, ENode expr, Class cls, Operator op) {
		if (expr.getClass() == cls) {
			expr.symbol = core_method;
			return;
		}
		ENode[] args = expr.getArgs();
		if (args == null || args.length != 1) {
			Kiev.reportError(expr, "Don't know how to normalize "+expr.getClass()+" into "+cls);
			return;
		}
		ENode en = (ENode)cls.newInstance();
		for (int i=0; i < args.length; i++)
			args[i] = args[i].detach();
		en.initFrom(expr, op, core_method, args);
		expr.replaceWithNodeReWalk(en);
	}
	public ConstExpr calc(ENode expr) {
		ENode[] args = expr.getArgs();
		return doCalc(args[0].getConstValue());
	}
	protected ConstExpr doCalc(Object:Object arg) {
		throw new RuntimeException("Cannot calculate a const from "+arg);
	}
}


/////////////////////////////////////////////////
//         any                                 //
/////////////////////////////////////////////////

@singleton
class AnyInstanceOf extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, InstanceofExpr.class, Operator.InstanceOf); }
}


@singleton
class ObjectAssign extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign); }
}

@singleton
class ObjectAssign2 extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign2); }
}

@singleton
class ObjectPvarINIT extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign2); }
}

@singleton
class ObjectBoolEQ extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.Equals); }
}

@singleton
class ObjectBoolNE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.NotEquals); }
}

/////////////////////////////////////////////////
//         boolean                             //
/////////////////////////////////////////////////

@singleton
class BoolAssign extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue()) }
}

@singleton
class BoolAssignBitOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignBitOr); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue() | arg2.booleanValue()) }
}

@singleton
class BoolAssignBitXOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignBitXor); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue() ^ arg2.booleanValue()) }
}

@singleton
class BoolAssignBitAND extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignBitAnd); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue() & arg2.booleanValue()) }
}

@singleton
class BoolBitOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.BitOr); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue() | arg2.booleanValue()) }
}

@singleton
class BoolBitXOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.BitXor); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue() ^ arg2.booleanValue()) }
}

@singleton
class BoolBitAND extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.BitAnd); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue() & arg2.booleanValue()) }
}

@singleton
class BoolBoolOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBooleanOrExpr.class, Operator.BooleanOr); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue() | arg2.booleanValue()) }
}

@singleton
class BoolBoolAND extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBooleanAndExpr.class, Operator.BooleanAnd); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue() & arg2.booleanValue()) }
}

@singleton
class BoolBoolEQ extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.Equals); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() == arg2.booleanValue()) }
}

@singleton
class BoolBoolNE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.NotEquals); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() != arg2.booleanValue()) }
}

@singleton
class BoolBoolNOT extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BooleanNotExpr.class, Operator.BooleanNot); }
	protected ConstExpr doCalc(Boolean:Object arg) { new ConstBoolExpr( !arg.booleanValue()) }
}


/////////////////////////////////////////////////
//         char                                //
/////////////////////////////////////////////////

@singleton
class CharAssign extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign); }
	protected ConstExpr doCalc(Character:Object arg1, Character:Object arg2) { new ConstCharExpr(arg2.charValue()) }
}

/////////////////////////////////////////////////
//         byte                                //
/////////////////////////////////////////////////

@singleton
class ByteAssign extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstByteExpr((byte)arg2.intValue()) }
}

@singleton
class BytePOS extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Pos); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstByteExpr( ($cast byte) + arg.intValue()) }
}

@singleton
class ByteNEG extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Neg); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstByteExpr( ($cast byte) - arg.intValue()) }
}

/////////////////////////////////////////////////
//         short                               //
/////////////////////////////////////////////////

@singleton
class ShortAssign extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstShortExpr((short)arg2.intValue()) }
}

@singleton
class ShortPOS extends UnaryFunc {
	public void normilizeExpr(Method core_method, Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Pos); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstShortExpr( ($cast short) + arg.intValue()) }
}

@singleton
class ShortNEG extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Neg); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstShortExpr( ($cast short) - arg.intValue()) }
}

/////////////////////////////////////////////////
//         int                                 //
/////////////////////////////////////////////////

@singleton
class IntAssign extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue()) }
}

@singleton
class IntAssignBitOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignBitOr); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() | arg2.intValue()) }
}

@singleton
class IntAssignBitXOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignBitXor); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() ^ arg2.intValue()) }
}

@singleton
class IntAssignBitAND extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignBitAnd); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() & arg2.intValue()) }
}

@singleton
class IntAssignLShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignLeftShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() << arg2.intValue()) }
}

@singleton
class IntAssignRShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignRightShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() >> arg2.intValue()) }
}

@singleton
class IntAssignUShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignUnsignedRightShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() >>> arg2.intValue()) }
}

@singleton
class IntAssignADD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignAdd); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() + arg2.intValue()) }
}

@singleton
class IntAssignSUB extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignSub); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() - arg2.intValue()) }
}

@singleton
class IntAssignMUL extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignMul); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() * arg2.intValue()) }
}

@singleton
class IntAssignDIV extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignDiv); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() / arg2.intValue()) }
}

@singleton
class IntAssignMOD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignMod); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue() % arg2.intValue()) }
}

@singleton
class IntBitOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.BitOr); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() | arg2.intValue()) }
}

@singleton
class IntBitXOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.BitXor); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() ^ arg2.intValue()) }
}

@singleton
class IntBitAND extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.BitAnd); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() & arg2.intValue()) }
}

@singleton
class IntBitNOT extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.BitNot); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstIntExpr( ~ arg.intValue()) }
}

@singleton
class IntBoolEQ extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.Equals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() == arg2.intValue()) }
}

@singleton
class IntBoolNE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.NotEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() != arg2.intValue()) }
}

@singleton
class IntBoolGE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.GreaterEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() >= arg2.intValue()) }
}

@singleton
class IntBoolLE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.LessEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() <= arg2.intValue()) }
}

@singleton
class IntBoolGT extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.GreaterThen); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() > arg2.intValue()) }
}

@singleton
class IntBoolLT extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.LessThen); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() < arg2.intValue()) }
}

@singleton
class IntLShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.LeftShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() << arg2.intValue()) }
}

@singleton
class IntRShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.RightShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() >> arg2.intValue()) }
}

@singleton
class IntUShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.UnsignedRightShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() >>> arg2.intValue()) }
}

@singleton
class IntADD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Add); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() + arg2.intValue()) }
}

@singleton
class IntSUB extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Sub); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() - arg2.intValue()) }
}

@singleton
class IntMUL extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Mul); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() * arg2.intValue()) }
}

@singleton
class IntDIV extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Div); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() / arg2.intValue()) }
}

@singleton
class IntMOD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Mod); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() % arg2.intValue()) }
}

@singleton
class IntPOS extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Pos); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstIntExpr( + arg.intValue()) }
}

@singleton
class IntNEG extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Neg); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstIntExpr( - arg.intValue()) }
}

@singleton
class IntPreINCR extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, IncrementExpr.class, Operator.PreIncr); }
	protected ConstExpr doCalc(Number:Object arg) { null }
}

@singleton
class IntPreDECR extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, IncrementExpr.class, Operator.PreDecr); }
	protected ConstExpr doCalc(Number:Object arg) { null }
}

@singleton
class IntPostINCR extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, IncrementExpr.class, Operator.PostIncr); }
	protected ConstExpr doCalc(Number:Object arg) { null }
}

@singleton
class IntPostDECR extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, IncrementExpr.class, Operator.PostDecr); }
	protected ConstExpr doCalc(Number:Object arg) { null }
}


/////////////////////////////////////////////////
//         long                                //
/////////////////////////////////////////////////

@singleton
class LongAssign extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue()) }
}

@singleton
class LongAssignBitOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignBitOr); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() | arg2.longValue()) }
}

@singleton
class LongAssignBitXOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignBitXor); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() ^ arg2.longValue()) }
}

@singleton
class LongAssignBitAND extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignBitAnd); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() & arg2.longValue()) }
}

@singleton
class LongAssignLShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignLeftShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() << arg2.intValue()) }
}

@singleton
class LongAssignRShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignRightShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() >> arg2.intValue()) }
}

@singleton
class LongAssignUShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignUnsignedRightShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() >>> arg2.intValue()) }
}

@singleton
class LongAssignADD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignAdd); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() + arg2.longValue()) }
}

@singleton
class LongAssignSUB extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignSub); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() - arg2.longValue()) }
}

@singleton
class LongAssignMUL extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignMul); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() * arg2.longValue()) }
}

@singleton
class LongAssignDIV extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignDiv); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() / arg2.longValue()) }
}

@singleton
class LongAssignMOD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignMod); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue() % arg2.longValue()) }
}

@singleton
class LongBitOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.BitOr); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() | arg2.longValue()) }
}

@singleton
class LongBitXOR extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.BitXor); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() ^ arg2.longValue()) }
}

@singleton
class LongBitAND extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.BitAnd); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() & arg2.longValue()) }
}

@singleton
class LongBitNOT extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.BitNot); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstLongExpr( ~ arg.longValue()) }
}

@singleton
class LongBoolEQ extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.Equals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() == arg2.longValue()) }
}

@singleton
class LongBoolNE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.NotEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() != arg2.longValue()) }
}

@singleton
class LongBoolGE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.GreaterEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() >= arg2.longValue()) }
}

@singleton
class LongBoolLE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.LessEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() <= arg2.longValue()) }
}

@singleton
class LongBoolGT extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.GreaterThen); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() > arg2.longValue()) }
}

@singleton
class LongBoolLT extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.LessThen); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() < arg2.longValue()) }
}

@singleton
class LongLShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.LeftShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() << arg2.intValue()) }
}

@singleton
class LongRShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.RightShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() >> arg2.intValue()) }
}

@singleton
class LongUShift extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.UnsignedRightShift); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() >>> arg2.intValue()) }
}

@singleton
class LongADD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Add); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() + arg2.longValue()) }
}

@singleton
class LongSUB extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Sub); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() - arg2.longValue()) }
}

@singleton
class LongMUL extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Mul); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() * arg2.longValue()) }
}

@singleton
class LongDIV extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Div); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() / arg2.longValue()) }
}

@singleton
class LongMOD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Mod); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() % arg2.longValue()) }
}

@singleton
class LongPOS extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Pos); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstLongExpr( + arg.longValue()) }
}

@singleton
class LongNEG extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Neg); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstLongExpr( - arg.longValue()) }
}

@singleton
class LongPreINCR extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, IncrementExpr.class, Operator.PreIncr); }
	protected ConstExpr doCalc(Number:Object arg) { null }
}

@singleton
class LongPreDECR extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, IncrementExpr.class, Operator.PreDecr); }
	protected ConstExpr doCalc(Number:Object arg) { null }
}

@singleton
class LongPostINCR extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, IncrementExpr.class, Operator.PostIncr); }
	protected ConstExpr doCalc(Number:Object arg) { null }
}

@singleton
class LongPostDECR extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, IncrementExpr.class, Operator.PostDecr); }
	protected ConstExpr doCalc(Number:Object arg) { null }
}


/////////////////////////////////////////////////
//         float                               //
/////////////////////////////////////////////////

@singleton
class FloatAssign extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg2.floatValue()) }
}

@singleton
class FloatAssignADD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignAdd); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg2.floatValue() + arg2.floatValue()) }
}

@singleton
class FloatAssignSUB extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignSub); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg2.floatValue() - arg2.floatValue()) }
}

@singleton
class FloatAssignMUL extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignMul); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg2.floatValue() * arg2.floatValue()) }
}

@singleton
class FloatAssignDIV extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignDiv); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg2.floatValue() / arg2.floatValue()) }
}

@singleton
class FloatAssignMOD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignMod); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg2.floatValue() % arg2.floatValue()) }
}

@singleton
class FloatBoolEQ extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.Equals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() == arg2.floatValue()) }
}

@singleton
class FloatBoolNE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.NotEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() != arg2.floatValue()) }
}

@singleton
class FloatBoolGE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.GreaterEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() >= arg2.floatValue()) }
}

@singleton
class FloatBoolLE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.LessEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() <= arg2.floatValue()) }
}

@singleton
class FloatBoolGT extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.GreaterThen); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() > arg2.floatValue()) }
}

@singleton
class FloatBoolLT extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.LessThen); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() < arg2.floatValue()) }
}

@singleton
class FloatADD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Add); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() + arg2.floatValue()) }
}

@singleton
class FloatSUB extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Sub); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() - arg2.floatValue()) }
}

@singleton
class FloatMUL extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Mul); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() * arg2.floatValue()) }
}

@singleton
class FloatDIV extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Div); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() / arg2.floatValue()) }
}

@singleton
class FloatMOD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Mod); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() % arg2.floatValue()) }
}

@singleton
class FloatPOS extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Pos); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstFloatExpr( + arg.floatValue()) }
}

@singleton
class FloatNEG extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Neg); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstFloatExpr( - arg.floatValue()) }
}



/////////////////////////////////////////////////
//         double                              //
/////////////////////////////////////////////////

@singleton
class DoubleAssign extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.Assign); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg2.doubleValue()) }
}

@singleton
class DoubleAssignADD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignAdd); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg2.doubleValue() + arg2.doubleValue()) }
}

@singleton
class DoubleAssignSUB extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignSub); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg2.doubleValue() - arg2.doubleValue()) }
}

@singleton
class DoubleAssignMUL extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignMul); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg2.doubleValue() * arg2.doubleValue()) }
}

@singleton
class DoubleAssignDIV extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignDiv); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg2.doubleValue() / arg2.doubleValue()) }
}

@singleton
class DoubleAssignMOD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignMod); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg2.doubleValue() % arg2.doubleValue()) }
}

@singleton
class DoubleBoolEQ extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.Equals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() == arg2.doubleValue()) }
}

@singleton
class DoubleBoolNE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.NotEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() != arg2.doubleValue()) }
}

@singleton
class DoubleBoolGE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.GreaterEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() >= arg2.doubleValue()) }
}

@singleton
class DoubleBoolLE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.LessEquals); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() <= arg2.doubleValue()) }
}

@singleton
class DoubleBoolGT extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.GreaterThen); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() > arg2.doubleValue()) }
}

@singleton
class DoubleBoolLT extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryBoolExpr.class, Operator.LessThen); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() < arg2.doubleValue()) }
}

@singleton
class DoubleADD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Add); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() + arg2.doubleValue()) }
}

@singleton
class DoubleSUB extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Sub); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() - arg2.doubleValue()) }
}

@singleton
class DoubleMUL extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Mul); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() * arg2.doubleValue()) }
}

@singleton
class DoubleDIV extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Div); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() / arg2.doubleValue()) }
}

@singleton
class DoubleMOD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, BinaryExpr.class, Operator.Mod); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() % arg2.doubleValue()) }
}

@singleton
class DoublePOS extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Pos); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstDoubleExpr( + arg.doubleValue()) }
}

@singleton
class DoubleNEG extends UnaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, UnaryExpr.class, Operator.Neg); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstDoubleExpr( - arg.doubleValue()) }
}



/////////////////////////////////////////////////
//         String                              //
/////////////////////////////////////////////////

abstract class StringConcat extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, StringConcatExpr.class, Operator.Add); }
	protected ConstExpr doCalc(Object arg1, Object arg2) { new ConstStringExpr(String.valueOf(arg1) + String.valueOf(arg2)) }
}

@singleton
class StringConcatSS extends StringConcat {}
@singleton
class StringConcatAS extends StringConcat {}
@singleton
class StringConcatSA extends StringConcat {}

@singleton
class StringAssignADD extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) {
		super.normilizeExpr(core_method, expr, AssignExpr.class, Operator.AssignAdd);
		ENode[] args = expr.getArgs();
		if (args == null || args.length != 2) {
			Kiev.reportError(expr, "Don't know how to normalize "+expr.getClass());
			return;
		}
		ENode lval = args[0];
		ENode value = args[1];
		ENode en = new AssignExpr(expr.pos, Operator.Assign,
			~lval,
			new BinaryExpr(expr.pos,Operator.Add,lval.ncopy(),~value)
			);
		expr.replaceWithNodeReWalk(en);
	}
}



/////////////////////////////////////////////////
//         ASTNodeType                         //
/////////////////////////////////////////////////


@singleton
class ASTNodeEQ extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, MacroBinaryBoolExpr.class, Operator.Equals); }
	protected ConstExpr doCalc(Object arg1, Object arg2) { new ConstBoolExpr(arg1 == arg2) }
}

@singleton
class ASTNodeNE extends BinaryFunc {
	public void normilizeExpr(Method core_method, ENode expr) { super.normilizeExpr(core_method, expr, MacroBinaryBoolExpr.class, Operator.NotEquals); }
	protected ConstExpr doCalc(Object arg1, Object arg2) { new ConstBoolExpr(arg1 != arg2) }
}
