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
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public interface BEndOperation {
}

@ThisIsANode(lang=CoreLang, copyable=false)
public final class CoreOperation extends Method {
	
	public final CoreFunc		core_func;
	public BEndOperation		bend_func;
	
	public CoreOperation(Env env, String sname, CallType otype, CoreFunc core_func) {
		this.sname = sname;
		this.core_func = core_func;
		this.setPublic();
		this.setStatic(true);
		this.setNative(true);
		this.setMacro(true);
		this.type_ret = new TypeRef(otype.ret());
		for (int i=0; i < otype.arity; i++)
			this.params += new LVar(0,"arg"+i,otype.arg(i),Var.VAR_LOCAL,0);
		env.root.pkg_members += this;
	}
	
	public ConstExpr calc(ENode expr) {
		return core_func.calc(expr);
	}
	
	public Operator getOperator() {
		return core_func.getOperator();
	}

	public boolean preVerify(Env env, INode parent, AttrSlot slot) {
		return false;
	}

	public void normilizeExpr(Env env, ENode expr, Symbol sym, INode parent, AttrSlot slot) {
		assert (sym.dnode == this);
		this.core_func.normilizeExpr(expr,parent,slot);
	}

	public CallType makeType(ENode expr) {
		CallType ct = this.core_func.makeType(expr);
		if (ct != null)
			return ct;
		return mtype;
	}
}

@ThisIsANode(lang=CoreLang)
public final class CoreExpr extends ENode {
	
	public CoreExpr() {}
	
	public CoreOperation getCoreOperation(Env env) {
		CoreFunc cf = env.coreFuncs.get(ident);
		if (cf == null)
			return null;
		return cf.operation;
	}
	
}

public final class CoreFuncs {
	public final Hashtable<String,CoreFunc> coreFuncs;
	
	public final CoreFunc fAnyInstanceOf;
	public final CoreFunc fObjectAssign;
	public final CoreFunc fObjectBoolEQ;
	public final CoreFunc fObjectBoolNE;
	public final CoreFunc fBoolAssign;
	public final CoreFunc fBoolAssignBitOR;
	public final CoreFunc fBoolAssignBitXOR;
	public final CoreFunc fBoolAssignBitAND;
	public final CoreFunc fBoolBitOR;
	public final CoreFunc fBoolBitXOR;
	public final CoreFunc fBoolBitAND;
	public final CoreFunc fBoolBoolOR;
	public final CoreFunc fBoolBoolAND;
	public final CoreFunc fBoolBoolEQ;
	public final CoreFunc fBoolBoolNE;
	public final CoreFunc fBoolBoolNOT;
	public final CoreFunc fCharAssign;
	public final CoreFunc fByteAssign;
	public final CoreFunc fBytePOS;
	public final CoreFunc fByteNEG;
	public final CoreFunc fShortAssign;
	public final CoreFunc fShortPOS;
	public final CoreFunc fShortNEG;
	
	public final CoreFunc fIntAssign;
	public final CoreFunc fIntAssignBitOR;
	public final CoreFunc fIntAssignBitXOR;
	public final CoreFunc fIntAssignBitAND;
	public final CoreFunc fIntAssignLShift;
	public final CoreFunc fIntAssignRShift;
	public final CoreFunc fIntAssignUShift;
	public final CoreFunc fIntAssignADD;
	public final CoreFunc fIntAssignSUB;
	public final CoreFunc fIntAssignMUL;
	public final CoreFunc fIntAssignDIV;
	public final CoreFunc fIntAssignMOD;
	public final CoreFunc fIntBitOR;
	public final CoreFunc fIntBitXOR;
	public final CoreFunc fIntBitAND;
	public final CoreFunc fIntBitNOT;
	public final CoreFunc fIntBoolEQ;
	public final CoreFunc fIntBoolNE;
	public final CoreFunc fIntBoolGE;
	public final CoreFunc fIntBoolLE;
	public final CoreFunc fIntBoolGT;
	public final CoreFunc fIntBoolLT;
	public final CoreFunc fIntLShift;
	public final CoreFunc fIntRShift;
	public final CoreFunc fIntUShift;
	public final CoreFunc fIntADD;
	public final CoreFunc fIntSUB;
	public final CoreFunc fIntMUL;
	public final CoreFunc fIntDIV;
	public final CoreFunc fIntMOD;
	public final CoreFunc fIntPOS;
	public final CoreFunc fIntNEG;
	public final CoreFunc fIntPreINCR;
	public final CoreFunc fIntPreDECR;
	public final CoreFunc fIntPostINCR;
	public final CoreFunc fIntPostDECR;

	public final CoreFunc fLongAssign;
	public final CoreFunc fLongAssignBitOR;
	public final CoreFunc fLongAssignBitXOR;
	public final CoreFunc fLongAssignBitAND;
	public final CoreFunc fLongAssignLShift;
	public final CoreFunc fLongAssignRShift;
	public final CoreFunc fLongAssignUShift;
	public final CoreFunc fLongAssignADD;
	public final CoreFunc fLongAssignSUB;
	public final CoreFunc fLongAssignMUL;
	public final CoreFunc fLongAssignDIV;
	public final CoreFunc fLongAssignMOD;
	public final CoreFunc fLongBitOR;
	public final CoreFunc fLongBitXOR;
	public final CoreFunc fLongBitAND;
	public final CoreFunc fLongBitNOT;
	public final CoreFunc fLongBoolEQ;
	public final CoreFunc fLongBoolNE;
	public final CoreFunc fLongBoolGE;
	public final CoreFunc fLongBoolLE;
	public final CoreFunc fLongBoolGT;
	public final CoreFunc fLongBoolLT;
	public final CoreFunc fLongLShift;
	public final CoreFunc fLongRShift;
	public final CoreFunc fLongUShift;
	public final CoreFunc fLongADD;
	public final CoreFunc fLongSUB;
	public final CoreFunc fLongMUL;
	public final CoreFunc fLongDIV;
	public final CoreFunc fLongMOD;
	public final CoreFunc fLongPOS;
	public final CoreFunc fLongNEG;
	public final CoreFunc fLongPreINCR;
	public final CoreFunc fLongPreDECR;
	public final CoreFunc fLongPostINCR;
	public final CoreFunc fLongPostDECR;
	
	public final CoreFunc fFloatAssign;
	public final CoreFunc fFloatAssignADD;
	public final CoreFunc fFloatAssignSUB;
	public final CoreFunc fFloatAssignMUL;
	public final CoreFunc fFloatAssignDIV;
	public final CoreFunc fFloatAssignMOD;
	public final CoreFunc fFloatBoolEQ;
	public final CoreFunc fFloatBoolNE;
	public final CoreFunc fFloatBoolGE;
	public final CoreFunc fFloatBoolLE;
	public final CoreFunc fFloatBoolGT;
	public final CoreFunc fFloatBoolLT;
	public final CoreFunc fFloatADD;
	public final CoreFunc fFloatSUB;
	public final CoreFunc fFloatMUL;
	public final CoreFunc fFloatDIV;
	public final CoreFunc fFloatMOD;
	public final CoreFunc fFloatPOS;
	public final CoreFunc fFloatNEG;
	
	public final CoreFunc fDoubleAssign;
	public final CoreFunc fDoubleAssignADD;
	public final CoreFunc fDoubleAssignSUB;
	public final CoreFunc fDoubleAssignMUL;
	public final CoreFunc fDoubleAssignDIV;
	public final CoreFunc fDoubleAssignMOD;
	public final CoreFunc fDoubleBoolEQ;
	public final CoreFunc fDoubleBoolNE;
	public final CoreFunc fDoubleBoolGE;
	public final CoreFunc fDoubleBoolLE;
	public final CoreFunc fDoubleBoolGT;
	public final CoreFunc fDoubleBoolLT;
	public final CoreFunc fDoubleADD;
	public final CoreFunc fDoubleSUB;
	public final CoreFunc fDoubleMUL;
	public final CoreFunc fDoubleDIV;
	public final CoreFunc fDoubleMOD;
	public final CoreFunc fDoublePOS;
	public final CoreFunc fDoubleNEG;
	
	public final CoreFunc fStringConcatSS;
	public final CoreFunc fStringConcatAS;
	public final CoreFunc fStringConcatSA;
	public final CoreFunc fStringAssignADD;
	public final CoreFunc fConditional;
	public final CoreFunc fIFldAccess;
	public final CoreFunc fSFldAccess;
	public final CoreFunc fOuterThisAccess;
	public final CoreFunc fMacroAccess;
	public final CoreFunc fClassAccess;
	public final CoreFunc fTypeinfoAccess;
	public final CoreFunc fCast;
	public final CoreFunc fReinterp;
	public final CoreFunc fRuleIsThe;
	public final CoreFunc fRuleIsOneOf;
	public final CoreFunc fPathTypeAccess;
	
	public CoreFuncs(Env env) {
		coreFuncs = new Hashtable<String,CoreFunc>(1024);

		fAnyInstanceOf = new CoreFunc.AnyInstanceOf(env);			coreFuncs.put("kiev.stdlib.any:_instanceof_",       fAnyInstanceOf);

		fObjectAssign = new CoreFunc.ObjectAssign(env);				coreFuncs.put("kiev.vlang.Globals:ref_assign",      fObjectAssign);
		fObjectBoolEQ = new CoreFunc.ObjectBoolEQ(env);				coreFuncs.put("kiev.stdlib.any:ref_eq",             fObjectBoolEQ);
		fObjectBoolNE = new CoreFunc.ObjectBoolNE(env);				coreFuncs.put("kiev.stdlib.any:ref_neq",            fObjectBoolNE);

		fBoolAssign = new CoreFunc.BoolAssign(env);					coreFuncs.put("kiev.stdlib.boolean:assign",         fBoolAssign);
		fBoolAssignBitOR = new CoreFunc.BoolAssignBitOR(env);		coreFuncs.put("kiev.stdlib.boolean:assign_bit_or",  fBoolAssignBitOR);
		fBoolAssignBitXOR = new CoreFunc.BoolAssignBitXOR(env);		coreFuncs.put("kiev.stdlib.boolean:assign_bit_xor", fBoolAssignBitXOR);
		fBoolAssignBitAND = new CoreFunc.BoolAssignBitAND(env);		coreFuncs.put("kiev.stdlib.boolean:assign_bit_and", fBoolAssignBitAND);
		fBoolBitOR = new CoreFunc.BoolBitOR(env);					coreFuncs.put("kiev.stdlib.boolean:bit_or",         fBoolBitOR);
		fBoolBitXOR = new CoreFunc.BoolBitXOR(env);					coreFuncs.put("kiev.stdlib.boolean:bit_xor",        fBoolBitXOR);
		fBoolBitAND = new CoreFunc.BoolBitAND(env);					coreFuncs.put("kiev.stdlib.boolean:bit_and",        fBoolBitAND);
		fBoolBoolOR = new CoreFunc.BoolBoolOR(env);					coreFuncs.put("kiev.stdlib.boolean:bool_or",        fBoolBoolOR);
		fBoolBoolAND = new CoreFunc.BoolBoolAND(env);				coreFuncs.put("kiev.stdlib.boolean:bool_and",       fBoolBoolAND);
		fBoolBoolEQ = new CoreFunc.BoolBoolEQ(env);					coreFuncs.put("kiev.stdlib.boolean:bool_eq",        fBoolBoolEQ);
		fBoolBoolNE = new CoreFunc.BoolBoolNE(env);					coreFuncs.put("kiev.stdlib.boolean:bool_neq",       fBoolBoolNE);
		fBoolBoolNOT = new CoreFunc.BoolBoolNOT(env);				coreFuncs.put("kiev.stdlib.boolean:bool_not",       fBoolBoolNOT);

		fCharAssign = new CoreFunc.CharAssign(env);					coreFuncs.put("kiev.stdlib.char:assign",            fCharAssign);
		
		fByteAssign = new CoreFunc.ByteAssign(env);					coreFuncs.put("kiev.stdlib.byte:assign",            fByteAssign);
		fBytePOS = new CoreFunc.BytePOS(env);						coreFuncs.put("kiev.stdlib.byte:positive",          fBytePOS);
		fByteNEG = new CoreFunc.ByteNEG(env);						coreFuncs.put("kiev.stdlib.byte:negative",          fByteNEG);
		
		fShortAssign = new CoreFunc.ShortAssign(env);				coreFuncs.put("kiev.stdlib.short:assign",           fShortAssign);
		fShortPOS = new CoreFunc.ShortPOS(env);						coreFuncs.put("kiev.stdlib.short:positive",         fShortPOS);
		fShortNEG = new CoreFunc.ShortNEG(env);						coreFuncs.put("kiev.stdlib.short:negative",         fShortNEG);
		
		fIntAssign = new CoreFunc.IntAssign(env);					coreFuncs.put("kiev.stdlib.int:assign",             fIntAssign);
		fIntAssignBitOR = new CoreFunc.IntAssignBitOR(env);			coreFuncs.put("kiev.stdlib.int:assign_bit_or",      fIntAssignBitOR);
		fIntAssignBitXOR = new CoreFunc.IntAssignBitXOR(env);		coreFuncs.put("kiev.stdlib.int:assign_bit_xor",     fIntAssignBitXOR);
		fIntAssignBitAND = new CoreFunc.IntAssignBitAND(env);		coreFuncs.put("kiev.stdlib.int:assign_bit_and",     fIntAssignBitAND);
		fIntAssignLShift = new CoreFunc.IntAssignLShift(env);		coreFuncs.put("kiev.stdlib.int:assign_left_shift",  fIntAssignLShift);
		fIntAssignRShift = new CoreFunc.IntAssignRShift(env);		coreFuncs.put("kiev.stdlib.int:assign_right_shift", fIntAssignRShift);
		fIntAssignUShift = new CoreFunc.IntAssignUShift(env);		coreFuncs.put("kiev.stdlib.int:assign_unsigned_right_shift",     fIntAssignUShift);
		fIntAssignADD = new CoreFunc.IntAssignADD(env);				coreFuncs.put("kiev.stdlib.int:assign_add",         fIntAssignADD);
		fIntAssignSUB = new CoreFunc.IntAssignSUB(env);				coreFuncs.put("kiev.stdlib.int:assign_sub",         fIntAssignSUB);
		fIntAssignMUL = new CoreFunc.IntAssignMUL(env);				coreFuncs.put("kiev.stdlib.int:assign_mul",         fIntAssignMUL);
		fIntAssignDIV = new CoreFunc.IntAssignDIV(env);				coreFuncs.put("kiev.stdlib.int:assign_div",         fIntAssignDIV);
		fIntAssignMOD = new CoreFunc.IntAssignMOD(env);				coreFuncs.put("kiev.stdlib.int:assign_mod",         fIntAssignMOD);
		fIntBitOR = new CoreFunc.IntBitOR(env);						coreFuncs.put("kiev.stdlib.int:bit_or",             fIntBitOR);
		fIntBitXOR = new CoreFunc.IntBitXOR(env);					coreFuncs.put("kiev.stdlib.int:bit_xor",            fIntBitXOR);
		fIntBitAND = new CoreFunc.IntBitAND(env);					coreFuncs.put("kiev.stdlib.int:bit_and",            fIntBitAND);
		fIntBitNOT = new CoreFunc.IntBitNOT(env);					coreFuncs.put("kiev.stdlib.int:bit_not",            fIntBitNOT);
		fIntBoolEQ = new CoreFunc.IntBoolEQ(env);					coreFuncs.put("kiev.stdlib.int:bool_eq",            fIntBoolEQ);
		fIntBoolNE = new CoreFunc.IntBoolNE(env);					coreFuncs.put("kiev.stdlib.int:bool_neq",           fIntBoolNE);
		fIntBoolGE = new CoreFunc.IntBoolGE(env);					coreFuncs.put("kiev.stdlib.int:bool_ge",            fIntBoolGE);
		fIntBoolLE = new CoreFunc.IntBoolLE(env);					coreFuncs.put("kiev.stdlib.int:bool_le",            fIntBoolLE);
		fIntBoolGT = new CoreFunc.IntBoolGT(env);					coreFuncs.put("kiev.stdlib.int:bool_gt",            fIntBoolGT);
		fIntBoolLT = new CoreFunc.IntBoolLT(env);					coreFuncs.put("kiev.stdlib.int:bool_lt",            fIntBoolLT);
		fIntLShift = new CoreFunc.IntLShift(env);					coreFuncs.put("kiev.stdlib.int:left_shift",         fIntLShift);
		fIntRShift = new CoreFunc.IntRShift(env);					coreFuncs.put("kiev.stdlib.int:right_shift",        fIntRShift);
		fIntUShift = new CoreFunc.IntUShift(env);					coreFuncs.put("kiev.stdlib.int:unsigned_right_shift", fIntUShift);
		fIntADD = new CoreFunc.IntADD(env);							coreFuncs.put("kiev.stdlib.int:add",                fIntADD);
		fIntSUB = new CoreFunc.IntSUB(env);							coreFuncs.put("kiev.stdlib.int:sub",                fIntSUB);
		fIntMUL = new CoreFunc.IntMUL(env);							coreFuncs.put("kiev.stdlib.int:mul",                fIntMUL);
		fIntDIV = new CoreFunc.IntDIV(env);							coreFuncs.put("kiev.stdlib.int:div",                fIntDIV);
		fIntMOD = new CoreFunc.IntMOD(env);							coreFuncs.put("kiev.stdlib.int:mod",                fIntMOD);
		fIntPOS = new CoreFunc.IntPOS(env);							coreFuncs.put("kiev.stdlib.int:positive",           fIntPOS);
		fIntNEG = new CoreFunc.IntNEG(env);							coreFuncs.put("kiev.stdlib.int:negative",           fIntNEG);
		fIntPreINCR = new CoreFunc.IntPreINCR(env);					coreFuncs.put("kiev.stdlib.int:pre_incr",           fIntPreINCR);
		fIntPreDECR = new CoreFunc.IntPreDECR(env);					coreFuncs.put("kiev.stdlib.int:pre_decr",           fIntPreDECR);
		fIntPostINCR = new CoreFunc.IntPostINCR(env);				coreFuncs.put("kiev.stdlib.int:post_incr",          fIntPostINCR);
		fIntPostDECR = new CoreFunc.IntPostDECR(env);				coreFuncs.put("kiev.stdlib.int:post_decr",          fIntPostDECR);
		
		fLongAssign = new CoreFunc.LongAssign(env);					coreFuncs.put("kiev.stdlib.long:assign",            fLongAssign);
		fLongAssignBitOR = new CoreFunc.LongAssignBitOR(env);		coreFuncs.put("kiev.stdlib.long:assign_bit_or",     fLongAssignBitOR);
		fLongAssignBitXOR = new CoreFunc.LongAssignBitXOR(env);		coreFuncs.put("kiev.stdlib.long:assign_bit_xor",    fLongAssignBitXOR);
		fLongAssignBitAND = new CoreFunc.LongAssignBitAND(env);		coreFuncs.put("kiev.stdlib.long:assign_bit_and",    fLongAssignBitAND);
		fLongAssignLShift = new CoreFunc.LongAssignLShift(env);		coreFuncs.put("kiev.stdlib.long:assign_left_shift", fLongAssignLShift);
		fLongAssignRShift = new CoreFunc.LongAssignRShift(env);		coreFuncs.put("kiev.stdlib.long:assign_right_shift",fLongAssignRShift);
		fLongAssignUShift = new CoreFunc.LongAssignUShift(env);		coreFuncs.put("kiev.stdlib.long:assign_unsigned_right_shift",   fLongAssignUShift);
		fLongAssignADD = new CoreFunc.LongAssignADD(env);			coreFuncs.put("kiev.stdlib.long:assign_add",        fLongAssignADD);
		fLongAssignSUB = new CoreFunc.LongAssignSUB(env);			coreFuncs.put("kiev.stdlib.long:assign_sub",        fLongAssignSUB);
		fLongAssignMUL = new CoreFunc.LongAssignMUL(env);			coreFuncs.put("kiev.stdlib.long:assign_mul",        fLongAssignMUL);
		fLongAssignDIV = new CoreFunc.LongAssignDIV(env);			coreFuncs.put("kiev.stdlib.long:assign_div",        fLongAssignDIV);
		fLongAssignMOD = new CoreFunc.LongAssignMOD(env);			coreFuncs.put("kiev.stdlib.long:assign_mod",        fLongAssignMOD);
		fLongBitOR = new CoreFunc.LongBitOR(env);					coreFuncs.put("kiev.stdlib.long:bit_or",            fLongBitOR);
		fLongBitXOR = new CoreFunc.LongBitXOR(env);					coreFuncs.put("kiev.stdlib.long:bit_xor",           fLongBitXOR);
		fLongBitAND = new CoreFunc.LongBitAND(env);					coreFuncs.put("kiev.stdlib.long:bit_and",           fLongBitAND);
		fLongBitNOT = new CoreFunc.LongBitNOT(env);					coreFuncs.put("kiev.stdlib.long:bit_not",           fLongBitNOT);
		fLongBoolEQ = new CoreFunc.LongBoolEQ(env);					coreFuncs.put("kiev.stdlib.long:bool_eq",           fLongBoolEQ);
		fLongBoolNE = new CoreFunc.LongBoolNE(env);					coreFuncs.put("kiev.stdlib.long:bool_neq",          fLongBoolNE);
		fLongBoolGE = new CoreFunc.LongBoolGE(env);					coreFuncs.put("kiev.stdlib.long:bool_ge",           fLongBoolGE);
		fLongBoolLE = new CoreFunc.LongBoolLE(env);					coreFuncs.put("kiev.stdlib.long:bool_le",           fLongBoolLE);
		fLongBoolGT = new CoreFunc.LongBoolGT(env);					coreFuncs.put("kiev.stdlib.long:bool_gt",           fLongBoolGT);
		fLongBoolLT = new CoreFunc.LongBoolLT(env);					coreFuncs.put("kiev.stdlib.long:bool_lt",           fLongBoolLT);
		fLongLShift = new CoreFunc.LongLShift(env);					coreFuncs.put("kiev.stdlib.long:left_shift",        fLongLShift);
		fLongRShift = new CoreFunc.LongRShift(env);					coreFuncs.put("kiev.stdlib.long:right_shift",       fLongRShift);
		fLongUShift = new CoreFunc.LongUShift(env);					coreFuncs.put("kiev.stdlib.long:unsigned_right_shift", fLongUShift);
		fLongADD = new CoreFunc.LongADD(env);						coreFuncs.put("kiev.stdlib.long:add",               fLongADD);
		fLongSUB = new CoreFunc.LongSUB(env);						coreFuncs.put("kiev.stdlib.long:sub",               fLongSUB);
		fLongMUL = new CoreFunc.LongMUL(env);						coreFuncs.put("kiev.stdlib.long:mul",               fLongMUL);
		fLongDIV = new CoreFunc.LongDIV(env);						coreFuncs.put("kiev.stdlib.long:div",               fLongDIV);
		fLongMOD = new CoreFunc.LongMOD(env);						coreFuncs.put("kiev.stdlib.long:mod",               fLongMOD);
		fLongPOS = new CoreFunc.LongPOS(env);						coreFuncs.put("kiev.stdlib.long:positive",          fLongPOS);
		fLongNEG = new CoreFunc.LongNEG(env);						coreFuncs.put("kiev.stdlib.long:negative",          fLongNEG);
		fLongPreINCR = new CoreFunc.LongPreINCR(env);				coreFuncs.put("kiev.stdlib.long:pre_incr",          fLongPreINCR);
		fLongPreDECR = new CoreFunc.LongPreDECR(env);				coreFuncs.put("kiev.stdlib.long:pre_decr",          fLongPreDECR);
		fLongPostINCR = new CoreFunc.LongPostINCR(env);				coreFuncs.put("kiev.stdlib.long:post_incr",         fLongPostINCR);
		fLongPostDECR = new CoreFunc.LongPostDECR(env);				coreFuncs.put("kiev.stdlib.long:post_decr",         fLongPostDECR);
		
		fFloatAssign = new CoreFunc.FloatAssign(env);				coreFuncs.put("kiev.stdlib.float:assign",           fFloatAssign);
		fFloatAssignADD = new CoreFunc.FloatAssignADD(env);			coreFuncs.put("kiev.stdlib.float:assign_add",       fFloatAssignADD);
		fFloatAssignSUB = new CoreFunc.FloatAssignSUB(env);			coreFuncs.put("kiev.stdlib.float:assign_sub",       fFloatAssignSUB);
		fFloatAssignMUL = new CoreFunc.FloatAssignMUL(env);			coreFuncs.put("kiev.stdlib.float:assign_mul",       fFloatAssignMUL);
		fFloatAssignDIV = new CoreFunc.FloatAssignDIV(env);			coreFuncs.put("kiev.stdlib.float:assign_div",       fFloatAssignDIV);
		fFloatAssignMOD = new CoreFunc.FloatAssignMOD(env);			coreFuncs.put("kiev.stdlib.float:assign_mod",       fFloatAssignMOD);
		fFloatBoolEQ = new CoreFunc.FloatBoolEQ(env);				coreFuncs.put("kiev.stdlib.float:bool_eq",          fFloatBoolEQ);
		fFloatBoolNE = new CoreFunc.FloatBoolNE(env);				coreFuncs.put("kiev.stdlib.float:bool_neq",         fFloatBoolNE);
		fFloatBoolGE = new CoreFunc.FloatBoolGE(env);				coreFuncs.put("kiev.stdlib.float:bool_ge",          fFloatBoolGE);
		fFloatBoolLE = new CoreFunc.FloatBoolLE(env);				coreFuncs.put("kiev.stdlib.float:bool_le",          fFloatBoolLE);
		fFloatBoolGT = new CoreFunc.FloatBoolGT(env);				coreFuncs.put("kiev.stdlib.float:bool_gt",          fFloatBoolGT);
		fFloatBoolLT = new CoreFunc.FloatBoolLT(env);				coreFuncs.put("kiev.stdlib.float:bool_lt",          fFloatBoolLT);
		fFloatADD = new CoreFunc.FloatADD(env);						coreFuncs.put("kiev.stdlib.float:add",              fFloatADD);
		fFloatSUB = new CoreFunc.FloatSUB(env);						coreFuncs.put("kiev.stdlib.float:sub",              fFloatSUB);
		fFloatMUL = new CoreFunc.FloatMUL(env);						coreFuncs.put("kiev.stdlib.float:mul",              fFloatMUL);
		fFloatDIV = new CoreFunc.FloatDIV(env);						coreFuncs.put("kiev.stdlib.float:div",              fFloatDIV);
		fFloatMOD = new CoreFunc.FloatMOD(env);						coreFuncs.put("kiev.stdlib.float:mod",              fFloatMOD);
		fFloatPOS = new CoreFunc.FloatPOS(env);						coreFuncs.put("kiev.stdlib.float:positive",         fFloatPOS);
		fFloatNEG = new CoreFunc.FloatNEG(env);						coreFuncs.put("kiev.stdlib.float:negative",         fFloatNEG);
		
		fDoubleAssign = new CoreFunc.DoubleAssign(env);				coreFuncs.put("kiev.stdlib.double:assign",          fDoubleAssign);
		fDoubleAssignADD = new CoreFunc.DoubleAssignADD(env);		coreFuncs.put("kiev.stdlib.double:assign_add",      fDoubleAssignADD);
		fDoubleAssignSUB = new CoreFunc.DoubleAssignSUB(env);		coreFuncs.put("kiev.stdlib.double:assign_sub",      fDoubleAssignSUB);
		fDoubleAssignMUL = new CoreFunc.DoubleAssignMUL(env);		coreFuncs.put("kiev.stdlib.double:assign_mul",      fDoubleAssignMUL);
		fDoubleAssignDIV = new CoreFunc.DoubleAssignDIV(env);		coreFuncs.put("kiev.stdlib.double:assign_div",      fDoubleAssignDIV);
		fDoubleAssignMOD = new CoreFunc.DoubleAssignMOD(env);		coreFuncs.put("kiev.stdlib.double:assign_mod",      fDoubleAssignMOD);
		fDoubleBoolEQ = new CoreFunc.DoubleBoolEQ(env);				coreFuncs.put("kiev.stdlib.double:bool_eq",         fDoubleBoolEQ);
		fDoubleBoolNE = new CoreFunc.DoubleBoolNE(env);				coreFuncs.put("kiev.stdlib.double:bool_neq",        fDoubleBoolNE);
		fDoubleBoolGE = new CoreFunc.DoubleBoolGE(env);				coreFuncs.put("kiev.stdlib.double:bool_ge",         fDoubleBoolGE);
		fDoubleBoolLE = new CoreFunc.DoubleBoolLE(env);				coreFuncs.put("kiev.stdlib.double:bool_le",         fDoubleBoolLE);
		fDoubleBoolGT = new CoreFunc.DoubleBoolGT(env);				coreFuncs.put("kiev.stdlib.double:bool_gt",         fDoubleBoolGT);
		fDoubleBoolLT = new CoreFunc.DoubleBoolLT(env);				coreFuncs.put("kiev.stdlib.double:bool_lt",         fDoubleBoolLT);
		fDoubleADD = new CoreFunc.DoubleADD(env);					coreFuncs.put("kiev.stdlib.double:add",             fDoubleADD);
		fDoubleSUB = new CoreFunc.DoubleSUB(env);					coreFuncs.put("kiev.stdlib.double:sub",             fDoubleSUB);
		fDoubleMUL = new CoreFunc.DoubleMUL(env);					coreFuncs.put("kiev.stdlib.double:mul",             fDoubleMUL);
		fDoubleDIV = new CoreFunc.DoubleDIV(env);					coreFuncs.put("kiev.stdlib.double:div",             fDoubleDIV);
		fDoubleMOD = new CoreFunc.DoubleMOD(env);					coreFuncs.put("kiev.stdlib.double:mod",             fDoubleMOD);
		fDoublePOS = new CoreFunc.DoublePOS(env);					coreFuncs.put("kiev.stdlib.double:positive",        fDoublePOS);
		fDoubleNEG = new CoreFunc.DoubleNEG(env);					coreFuncs.put("kiev.stdlib.double:negative",        fDoubleNEG);

		fStringConcatSS = new CoreFunc.StringConcatSS(env);			coreFuncs.put("kiev.stdlib.GString:str_concat_ss",  fStringConcatSS);
		fStringConcatAS = new CoreFunc.StringConcatAS(env);			coreFuncs.put("kiev.stdlib.GString:str_concat_as",  fStringConcatAS);
		fStringConcatSA = new CoreFunc.StringConcatSA(env);			coreFuncs.put("kiev.stdlib.GString:str_concat_sa",  fStringConcatSA);
		fStringAssignADD = new CoreFunc.StringAssignADD(env);		coreFuncs.put("kiev.stdlib.GString:str_assign_add", fStringAssignADD);

		fConditional = new CoreFunc.Conditional(env);				coreFuncs.put("kiev.stdlib.any:conditional",        fConditional);
		fIFldAccess = new CoreFunc.IFldAccess(env);					coreFuncs.put("kiev.stdlib.any:access-ifld",        fIFldAccess);
		fSFldAccess = new CoreFunc.SFldAccess(env);					coreFuncs.put("kiev.stdlib.any:access-sfld",        fSFldAccess);
		fOuterThisAccess = new CoreFunc.OuterThisAccess(env);		coreFuncs.put("kiev.stdlib.any:access-this",        fOuterThisAccess);
		fMacroAccess = new CoreFunc.MacroAccess(env);				coreFuncs.put("kiev.stdlib.any:access-macro",       fMacroAccess);
		fClassAccess = new CoreFunc.ClassAccess(env);				coreFuncs.put("kiev.stdlib.any:access-class",       fClassAccess);
		fTypeinfoAccess = new CoreFunc.TypeinfoAccess(env);			coreFuncs.put("kiev.stdlib.any:access-typeinfo",    fTypeinfoAccess);
		fCast = new CoreFunc.Cast(env);								coreFuncs.put("kiev.stdlib.any:cast",               fCast);
		fReinterp = new CoreFunc.Reinterp(env);						coreFuncs.put("kiev.stdlib.any:reinterp",           fReinterp);
		fRuleIsThe = new CoreFunc.RuleIsThe(env);					coreFuncs.put("kiev.stdlib.any:rule-is-the",        fRuleIsThe);
		fRuleIsOneOf = new CoreFunc.RuleIsOneOf(env);				coreFuncs.put("kiev.stdlib.any:rule-is-one-of",     fRuleIsOneOf);
		fPathTypeAccess = new CoreFunc.PathTypeAccess(env);			coreFuncs.put("kiev.stdlib.any:path-type-access",   fPathTypeAccess);

		CoreFunc fASTNodeEQ;
		CoreFunc fASTNodeNE;
		fASTNodeEQ = new CoreFunc.ASTNodeEQ(env);					coreFuncs.put("kiev.vlang.Globals:node_ref_eq",       fASTNodeEQ);
		fASTNodeNE = new CoreFunc.ASTNodeNE(env);					coreFuncs.put("kiev.vlang.Globals:node_ref_neq",      fASTNodeNE);
		fASTNodeEQ = new CoreFunc.ASTNodeEQ(env);					coreFuncs.put("kiev.vlang.Globals:node_ref_eq_null",  fASTNodeEQ);
		fASTNodeNE = new CoreFunc.ASTNodeNE(env);					coreFuncs.put("kiev.vlang.Globals:node_ref_neq_null", fASTNodeNE);
	}
	
	public CoreFunc get(String key) {
		return coreFuncs.get(key);
	}
	
}

public abstract class CoreFunc {
	
	static CallType mkType(Type ret, Type... args) {
		return new CallType(null, null, args, ret, false);
	}
	
	public final CoreOperation	operation;
	public final Class			node_clazz;
	
	CoreFunc(Env env, String opname, Class node_clazz, CallType otype) {
		this.operation = new CoreOperation(env, opname, otype, this);
		this.node_clazz = node_clazz;
	}
	
	public abstract void normilizeExpr(ENode expr, INode parent, AttrSlot slot);
	public abstract ConstExpr calc(ENode expr);
	public Operator getOperator() { return null; }
	public CallType makeType(ENode expr) { return null; }

public static abstract class BinaryFunc extends CoreFunc {
	BinaryFunc(Env env, String opname, Class node_clazz, CallType otype) {
		super(env, opname, node_clazz, otype);
	}
	
	public void normilizeExpr(ENode expr, INode parent, AttrSlot slot) {
		Symbol symbol = this.operation.symbol;
		if (expr.getClass() == this.node_clazz) {
			if (expr.dnode != symbol.dnode)
				expr.symbol = symbol;
			return;
		}
		ENode[] args = expr.getEArgs();
		if (args == null || args.length != 2) {
			Kiev.reportError(expr, "Don't know how to normalize "+expr.getClass()+" into "+this.node_clazz);
			return;
		}
		ENode en = (ENode)this.node_clazz.newInstance();
		for (int i=0; i < args.length; i++)
			args[i] = args[i].detach();
		en.initFrom(expr, symbol, args);
		expr.replaceWithNodeReWalk(en,parent,slot);
	}
	public ConstExpr calc(ENode expr) {
		ENode[] args = expr.getEArgs();
		Env env = Env.getEnv();
		return doCalc(args[0].getConstValue(env), args[1].getConstValue(env));
	}
	protected ConstExpr doCalc(Object:Object arg1, Object:Object arg2) {
		throw new RuntimeException("Cannot calculate a const from "+arg1+" and "+arg2);
	}
}

public static abstract class UnaryFunc extends CoreFunc {
	UnaryFunc(Env env, String opname, Class node_clazz, CallType otype) {
		super(env, opname, node_clazz, otype);
	}
	
	public void normilizeExpr(ENode expr, INode parent, AttrSlot slot) {
		Symbol symbol = this.operation.symbol;
		if (expr.getClass() == this.node_clazz) {
			if (expr.dnode != symbol.dnode)
				expr.symbol = symbol;
			return;
		}
		ENode[] args = expr.getEArgs();
		if (args == null || args.length != 1) {
			Kiev.reportError(expr, "Don't know how to normalize "+expr.getClass()+" into "+this.node_clazz);
			return;
		}
		ENode en = (ENode)this.node_clazz.newInstance();
		for (int i=0; i < args.length; i++)
			args[i] = args[i].detach();
		en.initFrom(expr, symbol, args);
		expr.replaceWithNodeReWalk(en,parent,slot);
	}
	public ConstExpr calc(ENode expr) {
		ENode[] args = expr.getEArgs();
		return doCalc(args[0].getConstValue(Env.getEnv()));
	}
	protected ConstExpr doCalc(Object:Object arg) {
		throw new RuntimeException("Cannot calculate a const from "+arg);
	}
}


/////////////////////////////////////////////////
//         any                                 //
/////////////////////////////////////////////////

public static class AnyInstanceOf extends BinaryFunc {
	protected AnyInstanceOf(Env env) { super(env, "%instanceof(any,type)→bool", InstanceofExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpAny, env.tenv.tpTypeInfo)); }
	public Operator getOperator() { Operator.InstanceOf }
}


public static class ObjectAssign extends BinaryFunc {
	protected ObjectAssign(Env env) { super(env, "%assign(obj,obj)→obj", AssignExpr.class, mkType(env.tenv.tpObject, env.tenv.tpObject, env.tenv.tpObject)); }
	public Operator getOperator() { Operator.Assign }
	public CallType makeType(ENode expr) {
		AssignExpr ae = (AssignExpr)expr;
		Type ret = ae.lval.getType(Env.getEnv());
		return mkType(ret, ret, ret);
	}
}

public static class ObjectBoolEQ extends BinaryFunc {
	protected ObjectBoolEQ(Env env) { super(env,"%equals(obj,obj)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpObject, env.tenv.tpObject)); }
	public Operator getOperator() { Operator.Equals }
}

public static class ObjectBoolNE extends BinaryFunc {
	protected ObjectBoolNE(Env env) { super(env,"%not-equals(obj,obj)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpObject, env.tenv.tpObject)); }
	public Operator getOperator() { Operator.NotEquals }
}

/////////////////////////////////////////////////
//         boolean                             //
/////////////////////////////////////////////////

public static class BoolAssign extends BinaryFunc {
	protected BoolAssign(Env env) { super(env,"%assign(bool,bool)→bool", AssignExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg2.booleanValue()) }
	public Operator getOperator() { Operator.Assign }
}

public static class BoolAssignBitOR extends BinaryFunc {
	protected BoolAssignBitOR(Env env) { super(env,"%assign-bit-or(bool,bool)→bool", ModifyExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() | arg2.booleanValue()) }
	public Operator getOperator() { Operator.AssignBitOr }
}

public static class BoolAssignBitXOR extends BinaryFunc {
	protected BoolAssignBitXOR(Env env) { super(env,"%assign-bit-xor(bool,bool)→bool", ModifyExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() ^ arg2.booleanValue()) }
	public Operator getOperator() { Operator.AssignBitXor }
}

public static class BoolAssignBitAND extends BinaryFunc {
	protected BoolAssignBitAND(Env env) { super(env,"%assign-bit-and(bool,bool)→bool", ModifyExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() & arg2.booleanValue()) }
	public Operator getOperator() { Operator.AssignBitAnd }
}

public static class BoolBitOR extends BinaryFunc {
	protected BoolBitOR(Env env) { super(env,"%bit-or(bool,bool)→bool", BinaryExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() | arg2.booleanValue()) }
	public Operator getOperator() { Operator.BitOr }
}

public static class BoolBitXOR extends BinaryFunc {
	protected BoolBitXOR(Env env) { super(env,"%bit-xor(bool,bool)→bool", BinaryExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() ^ arg2.booleanValue()) }
	public Operator getOperator() { Operator.BitXor }
}

public static class BoolBitAND extends BinaryFunc {
	protected BoolBitAND(Env env) { super(env,"%bit-and(bool,bool)→bool", BinaryExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() & arg2.booleanValue()) }
	public Operator getOperator() { Operator.BitAnd }
}

public static class BoolBoolOR extends BinaryFunc {
	protected BoolBoolOR(Env env) { super(env,"%bool-or(bool,bool)→bool", BinaryBooleanOrExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() | arg2.booleanValue()) }
	public Operator getOperator() { Operator.BooleanOr }
}

public static class BoolBoolAND extends BinaryFunc {
	protected BoolBoolAND(Env env) { super(env,"%bool-and(bool,bool)→bool", BinaryBooleanAndExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() & arg2.booleanValue()) }
	public Operator getOperator() { Operator.BooleanAnd }
}

public static class BoolBoolEQ extends BinaryFunc {
	protected BoolBoolEQ(Env env) { super(env,"%equals(bool,bool)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() == arg2.booleanValue()) }
	public Operator getOperator() { Operator.Equals }
}

public static class BoolBoolNE extends BinaryFunc {
	protected BoolBoolNE(Env env) { super(env,"%not-equals(bool,bool)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg1, Boolean:Object arg2) { new ConstBoolExpr(arg1.booleanValue() != arg2.booleanValue()) }
	public Operator getOperator() { Operator.NotEquals }
}

public static class BoolBoolNOT extends UnaryFunc {
	protected BoolBoolNOT(Env env) { super(env,"%bool-not(bool)→bool", BooleanNotExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpBoolean)); }
	protected ConstExpr doCalc(Boolean:Object arg) { new ConstBoolExpr( !arg.booleanValue()) }
	public Operator getOperator() { Operator.BooleanNot }
}


/////////////////////////////////////////////////
//         char                                //
/////////////////////////////////////////////////

public static class CharAssign extends BinaryFunc {
	protected CharAssign(Env env) { super(env,"%assign(char,char)→char", AssignExpr.class, mkType(env.tenv.tpChar, env.tenv.tpChar, env.tenv.tpChar)); }
	protected ConstExpr doCalc(Character:Object arg1, Character:Object arg2) { new ConstCharExpr(arg2.charValue()) }
	public Operator getOperator() { Operator.Assign }
}

/////////////////////////////////////////////////
//         byte                                //
/////////////////////////////////////////////////

public static class ByteAssign extends BinaryFunc {
	protected ByteAssign(Env env) { super(env,"%assign(byte,byte)→byte", AssignExpr.class, mkType(env.tenv.tpByte, env.tenv.tpByte, env.tenv.tpByte)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstByteExpr((byte)arg2.intValue()) }
	public Operator getOperator() { Operator.Assign }
}

public static class BytePOS extends UnaryFunc {
	protected BytePOS(Env env) { super(env,"%positive(byte)→byte", UnaryExpr.class, mkType(env.tenv.tpByte, env.tenv.tpByte)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstByteExpr( ($cast byte) + arg.intValue()) }
	public Operator getOperator() { Operator.Pos }
}

public static class ByteNEG extends UnaryFunc {
	protected ByteNEG(Env env) { super(env,"%negative(byte)→byte", UnaryExpr.class, mkType(env.tenv.tpByte, env.tenv.tpByte)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstByteExpr( ($cast byte) - arg.intValue()) }
	public Operator getOperator() { Operator.Neg }
}

/////////////////////////////////////////////////
//         short                               //
/////////////////////////////////////////////////

public static class ShortAssign extends BinaryFunc {
	protected ShortAssign(Env env) { super(env,"%assign(short,short)→short", AssignExpr.class, mkType(env.tenv.tpShort, env.tenv.tpShort, env.tenv.tpShort)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstShortExpr((short)arg2.intValue()) }
	public Operator getOperator() { Operator.Assign }
}

public static class ShortPOS extends UnaryFunc {
	protected ShortPOS(Env env) { super(env,"%positive(short)→short", UnaryExpr.class, mkType(env.tenv.tpShort, env.tenv.tpShort)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstShortExpr( ($cast short) + arg.intValue()) }
	public Operator getOperator() { Operator.Pos }
}

public static class ShortNEG extends UnaryFunc {
	protected ShortNEG(Env env) { super(env,"%negative(short)→short", UnaryExpr.class, mkType(env.tenv.tpShort, env.tenv.tpShort)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstShortExpr( ($cast short) - arg.intValue()) }
	public Operator getOperator() { Operator.Neg }
}

/////////////////////////////////////////////////
//         int                                 //
/////////////////////////////////////////////////

public static class IntAssign extends BinaryFunc {
	protected IntAssign(Env env) { super(env,"%assign(int,int)→int", AssignExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg2.intValue()) }
	public Operator getOperator() { Operator.Assign }
}

public static class IntAssignBitOR extends BinaryFunc {
	protected IntAssignBitOR(Env env) { super(env,"%assign-bit-or(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() | arg2.intValue()) }
	public Operator getOperator() { Operator.AssignBitOr }
}

public static class IntAssignBitXOR extends BinaryFunc {
	protected IntAssignBitXOR(Env env) { super(env,"%assign-bit-xor(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() ^ arg2.intValue()) }
	public Operator getOperator() { Operator.AssignBitXor }
}

public static class IntAssignBitAND extends BinaryFunc {
	protected IntAssignBitAND(Env env) { super(env,"%assign-bit-and(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() & arg2.intValue()) }
	public Operator getOperator() { Operator.AssignBitAnd }
}

public static class IntAssignLShift extends BinaryFunc {
	protected IntAssignLShift(Env env) { super(env,"%assign-lshift(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() << arg2.intValue()) }
	public Operator getOperator() { Operator.AssignLeftShift }
}

public static class IntAssignRShift extends BinaryFunc {
	protected IntAssignRShift(Env env) { super(env,"%assign-rshift(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() >> arg2.intValue()) }
	public Operator getOperator() { Operator.AssignRightShift }
}

public static class IntAssignUShift extends BinaryFunc {
	protected IntAssignUShift(Env env) { super(env,"%assign-ushift(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() >>> arg2.intValue()) }
	public Operator getOperator() { Operator.AssignUnsignedRightShift }
}

public static class IntAssignADD extends BinaryFunc {
	protected IntAssignADD(Env env) { super(env,"%assign-add(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() + arg2.intValue()) }
	public Operator getOperator() { Operator.AssignAdd }
}

public static class IntAssignSUB extends BinaryFunc {
	protected IntAssignSUB(Env env) { super(env,"%assign-sub(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() - arg2.intValue()) }
	public Operator getOperator() { Operator.AssignSub }
}

public static class IntAssignMUL extends BinaryFunc {
	protected IntAssignMUL(Env env) { super(env,"%assign-mul(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() * arg2.intValue()) }
	public Operator getOperator() { Operator.AssignMul }
}

public static class IntAssignDIV extends BinaryFunc {
	protected IntAssignDIV(Env env) { super(env,"%assign-div(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() / arg2.intValue()) }
	public Operator getOperator() { Operator.AssignDiv }
}

public static class IntAssignMOD extends BinaryFunc {
	protected IntAssignMOD(Env env) { super(env,"%assign-mod(int,int)→int", ModifyExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() % arg2.intValue()) }
	public Operator getOperator() { Operator.AssignMod }
}

public static class IntBitOR extends BinaryFunc {
	protected IntBitOR(Env env) { super(env,"%bit-or(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() | arg2.intValue()) }
	public Operator getOperator() { Operator.BitOr }
}

public static class IntBitXOR extends BinaryFunc {
	protected IntBitXOR(Env env) { super(env,"%bit-xor(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() ^ arg2.intValue()) }
	public Operator getOperator() { Operator.BitXor }
}

public static class IntBitAND extends BinaryFunc {
	protected IntBitAND(Env env) { super(env,"%bit-and(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() & arg2.intValue()) }
	public Operator getOperator() { Operator.BitAnd }
}

public static class IntBitNOT extends UnaryFunc {
	protected IntBitNOT(Env env) { super(env,"%bit-not(int)→int", UnaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstIntExpr( ~ arg.intValue()) }
	public Operator getOperator() { Operator.BitNot }
}

public static class IntBoolEQ extends BinaryFunc {
	protected IntBoolEQ(Env env) { super(env,"%equals(int,int)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() == arg2.intValue()) }
	public Operator getOperator() { Operator.Equals }
}

public static class IntBoolNE extends BinaryFunc {
	protected IntBoolNE(Env env) { super(env,"%not-equals(int,int)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() != arg2.intValue()) }
	public Operator getOperator() { Operator.NotEquals }
}

public static class IntBoolGE extends BinaryFunc {
	protected IntBoolGE(Env env) { super(env,"%greater-equals(int,int)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() >= arg2.intValue()) }
	public Operator getOperator() { Operator.GreaterEquals }
}

public static class IntBoolLE extends BinaryFunc {
	protected IntBoolLE(Env env) { super(env,"%less-equals(int,int)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() <= arg2.intValue()) }
	public Operator getOperator() { Operator.LessEquals }
}

public static class IntBoolGT extends BinaryFunc {
	protected IntBoolGT(Env env) { super(env,"%greater-then(int,int)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() > arg2.intValue()) }
	public Operator getOperator() { Operator.GreaterThen }
}

public static class IntBoolLT extends BinaryFunc {
	protected IntBoolLT(Env env) { super(env,"%less-then(int,int)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.intValue() < arg2.intValue()) }
	public Operator getOperator() { Operator.LessThen }
}

public static class IntLShift extends BinaryFunc {
	protected IntLShift(Env env) { super(env,"%lshift(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() << arg2.intValue()) }
	public Operator getOperator() { Operator.LeftShift }
}

public static class IntRShift extends BinaryFunc {
	protected IntRShift(Env env) { super(env,"%rshift(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() >> arg2.intValue()) }
	public Operator getOperator() { Operator.RightShift }
}

public static class IntUShift extends BinaryFunc {
	protected IntUShift(Env env) { super(env,"%ushift(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() >>> arg2.intValue()) }
	public Operator getOperator() { Operator.UnsignedRightShift }
}

public static class IntADD extends BinaryFunc {
	protected IntADD(Env env) { super(env,"%add(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() + arg2.intValue()) }
	public Operator getOperator() { Operator.Add }
}

public static class IntSUB extends BinaryFunc {
	protected IntSUB(Env env) { super(env,"%sub(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() - arg2.intValue()) }
	public Operator getOperator() { Operator.Sub }
}

public static class IntMUL extends BinaryFunc {
	protected IntMUL(Env env) { super(env,"%mul(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() * arg2.intValue()) }
	public Operator getOperator() { Operator.Mul }
}

public static class IntDIV extends BinaryFunc {
	protected IntDIV(Env env) { super(env,"%div(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() / arg2.intValue()) }
	public Operator getOperator() { Operator.Div }
}

public static class IntMOD extends BinaryFunc {
	protected IntMOD(Env env) { super(env,"%mod(int,int)→int", BinaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstIntExpr(arg1.intValue() % arg2.intValue()) }
	public Operator getOperator() { Operator.Mod }
}

public static class IntPOS extends UnaryFunc {
	protected IntPOS(Env env) { super(env,"%positive(int)→int", UnaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstIntExpr( + arg.intValue()) }
	public Operator getOperator() { Operator.Pos }
}

public static class IntNEG extends UnaryFunc {
	protected IntNEG(Env env) { super(env,"%negative(int)→int", UnaryExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstIntExpr( - arg.intValue()) }
	public Operator getOperator() { Operator.Neg }
}

public static class IntPreINCR extends UnaryFunc {
	protected IntPreINCR(Env env) { super(env,"%incr-pre(int)→int", IncrementExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg) { null }
	public Operator getOperator() { Operator.PreIncr }
}

public static class IntPreDECR extends UnaryFunc {
	protected IntPreDECR(Env env) { super(env,"%decr-pre(int)→int", IncrementExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg) { null }
	public Operator getOperator() { Operator.PreDecr }
}

public static class IntPostINCR extends UnaryFunc {
	protected IntPostINCR(Env env) { super(env,"%incr-post(int)→int", IncrementExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg) { null }
	public Operator getOperator() { Operator.PostIncr }
}

public static class IntPostDECR extends UnaryFunc {
	protected IntPostDECR(Env env) { super(env,"%decr-post(int)→int", IncrementExpr.class, mkType(env.tenv.tpInt, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg) { null }
	public Operator getOperator() { Operator.PostDecr }
}


/////////////////////////////////////////////////
//         long                                //
/////////////////////////////////////////////////

public static class LongAssign extends BinaryFunc {
	protected LongAssign(Env env) { super(env,"%assign(long,long)→long", AssignExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg2.longValue()) }
	public Operator getOperator() { Operator.Assign }
}

public static class LongAssignBitOR extends BinaryFunc {
	protected LongAssignBitOR(Env env) { super(env,"%assign-bit-or(long,long)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() | arg2.longValue()) }
	public Operator getOperator() { Operator.AssignBitOr }
}

public static class LongAssignBitXOR extends BinaryFunc {
	protected LongAssignBitXOR(Env env) { super(env,"%assign-bit-xor(long,long)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() ^ arg2.longValue()) }
	public Operator getOperator() { Operator.AssignBitXor }
}

public static class LongAssignBitAND extends BinaryFunc {
	protected LongAssignBitAND(Env env) { super(env,"%assign-bit-and(long,long)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() & arg2.longValue()) }
	public Operator getOperator() { Operator.AssignBitAnd }
}

public static class LongAssignLShift extends BinaryFunc {
	protected LongAssignLShift(Env env) { super(env,"%assign-lshift(long,int)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpInt, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() << arg2.intValue()) }
	public Operator getOperator() { Operator.AssignLeftShift }
}

public static class LongAssignRShift extends BinaryFunc {
	protected LongAssignRShift(Env env) { super(env,"%assign-rshift(long,int)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpInt, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() >> arg2.intValue()) }
	public Operator getOperator() { Operator.AssignRightShift }
}

public static class LongAssignUShift extends BinaryFunc {
	protected LongAssignUShift(Env env) { super(env,"%assign-ushift(long,int)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpInt, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() >>> arg2.intValue()) }
	public Operator getOperator() { Operator.AssignUnsignedRightShift }
}

public static class LongAssignADD extends BinaryFunc {
	protected LongAssignADD(Env env) { super(env,"%assign-and(long,long)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() + arg2.longValue()) }
	public Operator getOperator() { Operator.AssignAdd }
}

public static class LongAssignSUB extends BinaryFunc {
	protected LongAssignSUB(Env env) { super(env,"%assign-sub(long,long)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() - arg2.longValue()) }
	public Operator getOperator() { Operator.AssignSub }
}

public static class LongAssignMUL extends BinaryFunc {
	protected LongAssignMUL(Env env) { super(env,"%assign-mul(long,long)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() * arg2.longValue()) }
	public Operator getOperator() { Operator.AssignMul }
}

public static class LongAssignDIV extends BinaryFunc {
	protected LongAssignDIV(Env env) { super(env,"%assign-div(long,long)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() / arg2.longValue()) }
	public Operator getOperator() { Operator.AssignDiv }
}

public static class LongAssignMOD extends BinaryFunc {
	protected LongAssignMOD(Env env) { super(env,"%assign-mod(long,long)→long", ModifyExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() % arg2.longValue()) }
	public Operator getOperator() { Operator.AssignMod }
}

public static class LongBitOR extends BinaryFunc {
	protected LongBitOR(Env env) { super(env,"%bit-or(long,long)→long", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() | arg2.longValue()) }
	public Operator getOperator() { Operator.BitOr }
}

public static class LongBitXOR extends BinaryFunc {
	protected LongBitXOR(Env env) { super(env,"%bit-xor(long,long)→long", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() ^ arg2.longValue()) }
	public Operator getOperator() { Operator.BitXor }
}

public static class LongBitAND extends BinaryFunc {
	protected LongBitAND(Env env) { super(env,"%bit-and(long,long)→long", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() & arg2.longValue()) }
	public Operator getOperator() { Operator.BitAnd }
}

public static class LongBitNOT extends UnaryFunc {
	protected LongBitNOT(Env env) { super(env,"%bit-not(long)→long", UnaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstLongExpr( ~ arg.longValue()) }
	public Operator getOperator() { Operator.BitNot }
}

public static class LongBoolEQ extends BinaryFunc {
	protected LongBoolEQ(Env env) { super(env,"%equals(long,long)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() == arg2.longValue()) }
	public Operator getOperator() { Operator.Equals }
}

public static class LongBoolNE extends BinaryFunc {
	protected LongBoolNE(Env env) { super(env,"%not-equals(long,long)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() != arg2.longValue()) }
	public Operator getOperator() { Operator.NotEquals }
}

public static class LongBoolGE extends BinaryFunc {
	protected LongBoolGE(Env env) { super(env,"%greater-equals(long,long)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() >= arg2.longValue()) }
	public Operator getOperator() { Operator.GreaterEquals }
}

public static class LongBoolLE extends BinaryFunc {
	protected LongBoolLE(Env env) { super(env,"%less-equals(long,long)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() <= arg2.longValue()) }
	public Operator getOperator() { Operator.LessEquals }
}

public static class LongBoolGT extends BinaryFunc {
	protected LongBoolGT(Env env) { super(env,"%greater-then(long,long)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() > arg2.longValue()) }
	public Operator getOperator() { Operator.GreaterThen }
}

public static class LongBoolLT extends BinaryFunc {
	protected LongBoolLT(Env env) { super(env,"%less-then(long,long)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.longValue() < arg2.longValue()) }
	public Operator getOperator() { Operator.LessThen }
}

public static class LongLShift extends BinaryFunc {
	protected LongLShift(Env env) { super(env,"%lshift(long,int)→bool", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() << arg2.intValue()) }
	public Operator getOperator() { Operator.LeftShift }
}

public static class LongRShift extends BinaryFunc {
	protected LongRShift(Env env) { super(env,"%rshift(long,int)→bool", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() >> arg2.intValue()) }
	public Operator getOperator() { Operator.RightShift }
}

public static class LongUShift extends BinaryFunc {
	protected LongUShift(Env env) { super(env,"%ushift(long,int)→bool", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpInt)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() >>> arg2.intValue()) }
	public Operator getOperator() { Operator.UnsignedRightShift }
}

public static class LongADD extends BinaryFunc {
	protected LongADD(Env env) { super(env,"%add(long,long)→long", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() + arg2.longValue()) }
	public Operator getOperator() { Operator.Add }
}

public static class LongSUB extends BinaryFunc {
	protected LongSUB(Env env) { super(env,"%sub(long,long)→long", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() - arg2.longValue()) }
	public Operator getOperator() { Operator.Sub }
}

public static class LongMUL extends BinaryFunc {
	protected LongMUL(Env env) { super(env,"%mul(long,long)→long", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() * arg2.longValue()) }
	public Operator getOperator() { Operator.Mul }
}

public static class LongDIV extends BinaryFunc {
	protected LongDIV(Env env) { super(env,"%div(long,long)→long", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() / arg2.longValue()) }
	public Operator getOperator() { Operator.Div }
}

public static class LongMOD extends BinaryFunc {
	protected LongMOD(Env env) { super(env,"%mod(long,long)→long", BinaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstLongExpr(arg1.longValue() % arg2.longValue()) }
	public Operator getOperator() { Operator.Mod }
}

public static class LongPOS extends UnaryFunc {
	protected LongPOS(Env env) { super(env,"%positive(long)→long", UnaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstLongExpr( + arg.longValue()) }
	public Operator getOperator() { Operator.Pos }
}

public static class LongNEG extends UnaryFunc {
	protected LongNEG(Env env) { super(env,"%negarive(long)→long", UnaryExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstLongExpr( - arg.longValue()) }
	public Operator getOperator() { Operator.Neg }
}

public static class LongPreINCR extends UnaryFunc {
	protected LongPreINCR(Env env) { super(env,"%incr-pre(long)→long", IncrementExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg) { null }
	public Operator getOperator() { Operator.PreIncr }
}

public static class LongPreDECR extends UnaryFunc {
	protected LongPreDECR(Env env) { super(env,"%decr-pre(long)→long", IncrementExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg) { null }
	public Operator getOperator() { Operator.PreDecr }
}

public static class LongPostINCR extends UnaryFunc {
	protected LongPostINCR(Env env) { super(env,"%incr-post(long)→long", IncrementExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg) { null }
	public Operator getOperator() { Operator.PostIncr }
}

public static class LongPostDECR extends UnaryFunc {
	protected LongPostDECR(Env env) { super(env,"%decr-post(long)→long", IncrementExpr.class, mkType(env.tenv.tpLong, env.tenv.tpLong)); }
	protected ConstExpr doCalc(Number:Object arg) { null }
	public Operator getOperator() { Operator.PostDecr }
}


/////////////////////////////////////////////////
//         float                               //
/////////////////////////////////////////////////

public static class FloatAssign extends BinaryFunc {
	protected FloatAssign(Env env) { super(env,"%assign(float)→float", AssignExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg2.floatValue()) }
	public Operator getOperator() { Operator.Assign }
}

public static class FloatAssignADD extends BinaryFunc {
	protected FloatAssignADD(Env env) { super(env,"%assign-add(float)→float", ModifyExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() + arg2.floatValue()) }
	public Operator getOperator() { Operator.AssignAdd }
}

public static class FloatAssignSUB extends BinaryFunc {
	protected FloatAssignSUB(Env env) { super(env,"%assign-sub(float)→float", ModifyExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() - arg2.floatValue()) }
	public Operator getOperator() { Operator.AssignSub }
}

public static class FloatAssignMUL extends BinaryFunc {
	protected FloatAssignMUL(Env env) { super(env,"%assign-mul(float)→float", ModifyExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() * arg2.floatValue()) }
	public Operator getOperator() { Operator.AssignMul }
}

public static class FloatAssignDIV extends BinaryFunc {
	protected FloatAssignDIV(Env env) { super(env,"%assign-div(float)→float", ModifyExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() / arg2.floatValue()) }
	public Operator getOperator() { Operator.AssignDiv }
}

public static class FloatAssignMOD extends BinaryFunc {
	protected FloatAssignMOD(Env env) { super(env,"%assign-mod(float)→float", ModifyExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() % arg2.floatValue()) }
	public Operator getOperator() { Operator.AssignMod }
}

public static class FloatBoolEQ extends BinaryFunc {
	protected FloatBoolEQ(Env env) { super(env,"%equals(float,float)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() == arg2.floatValue()) }
	public Operator getOperator() { Operator.Equals }
}

public static class FloatBoolNE extends BinaryFunc {
	protected FloatBoolNE(Env env) { super(env,"%not-equals(float,float)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() != arg2.floatValue()) }
	public Operator getOperator() { Operator.NotEquals }
}

public static class FloatBoolGE extends BinaryFunc {
	protected FloatBoolGE(Env env) { super(env,"%greater-equals(float,float)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() >= arg2.floatValue()) }
	public Operator getOperator() { Operator.GreaterEquals }
}

public static class FloatBoolLE extends BinaryFunc {
	protected FloatBoolLE(Env env) { super(env,"%less-equals(float,float)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() <= arg2.floatValue()) }
	public Operator getOperator() { Operator.LessEquals }
}

public static class FloatBoolGT extends BinaryFunc {
	protected FloatBoolGT(Env env) { super(env,"%greater-then(float,float)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() > arg2.floatValue()) }
	public Operator getOperator() { Operator.GreaterThen }
}

public static class FloatBoolLT extends BinaryFunc {
	protected FloatBoolLT(Env env) { super(env,"%less-then(float,float)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.floatValue() < arg2.floatValue()) }
	public Operator getOperator() { Operator.LessThen }
}

public static class FloatADD extends BinaryFunc {
	protected FloatADD(Env env) { super(env,"%add(float,float)→float", BinaryExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() + arg2.floatValue()) }
	public Operator getOperator() { Operator.Add }
}

public static class FloatSUB extends BinaryFunc {
	protected FloatSUB(Env env) { super(env,"%sub(float,float)→float", BinaryExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() - arg2.floatValue()) }
	public Operator getOperator() { Operator.Sub }
}

public static class FloatMUL extends BinaryFunc {
	protected FloatMUL(Env env) { super(env,"%mul(float,float)→float", BinaryExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() * arg2.floatValue()) }
	public Operator getOperator() { Operator.Mul }
}

public static class FloatDIV extends BinaryFunc {
	protected FloatDIV(Env env) { super(env,"%div(float,float)→float", BinaryExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() / arg2.floatValue()) }
	public Operator getOperator() { Operator.Div }
}

public static class FloatMOD extends BinaryFunc {
	protected FloatMOD(Env env) { super(env,"%mod(float,float)→float", BinaryExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstFloatExpr(arg1.floatValue() % arg2.floatValue()) }
	public Operator getOperator() { Operator.Mod }
}

public static class FloatPOS extends UnaryFunc {
	protected FloatPOS(Env env) { super(env,"%positive(float)→float", UnaryExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstFloatExpr( + arg.floatValue()) }
	public Operator getOperator() { Operator.Pos }
}

public static class FloatNEG extends UnaryFunc {
	protected FloatNEG(Env env) { super(env,"%negative(float)→float", UnaryExpr.class, mkType(env.tenv.tpFloat, env.tenv.tpFloat)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstFloatExpr( - arg.floatValue()) }
	public Operator getOperator() { Operator.Neg }
}



/////////////////////////////////////////////////
//         double                              //
/////////////////////////////////////////////////

public static class DoubleAssign extends BinaryFunc {
	protected DoubleAssign(Env env) { super(env,"%assign(double)→double", AssignExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg2.doubleValue()) }
	public Operator getOperator() { Operator.Assign }
}

public static class DoubleAssignADD extends BinaryFunc {
	protected DoubleAssignADD(Env env) { super(env,"%assign-add(double)→double", ModifyExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() + arg2.doubleValue()) }
	public Operator getOperator() { Operator.AssignAdd }
}

public static class DoubleAssignSUB extends BinaryFunc {
	protected DoubleAssignSUB(Env env) { super(env,"%assign-sub(double)→double", ModifyExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() - arg2.doubleValue()) }
	public Operator getOperator() { Operator.AssignSub }
}

public static class DoubleAssignMUL extends BinaryFunc {
	protected DoubleAssignMUL(Env env) { super(env,"%assign-mul(double)→double", ModifyExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() * arg2.doubleValue()) }
	public Operator getOperator() { Operator.AssignMul }
}

public static class DoubleAssignDIV extends BinaryFunc {
	protected DoubleAssignDIV(Env env) { super(env,"%assign-div(double)→double", ModifyExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() / arg2.doubleValue()) }
	public Operator getOperator() { Operator.AssignDiv }
}

public static class DoubleAssignMOD extends BinaryFunc {
	protected DoubleAssignMOD(Env env) { super(env,"%assign-mod(double)→double", ModifyExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() % arg2.doubleValue()) }
	public Operator getOperator() { Operator.AssignMod }
}

public static class DoubleBoolEQ extends BinaryFunc {
	protected DoubleBoolEQ(Env env) { super(env,"%equals(double,double)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() == arg2.doubleValue()) }
	public Operator getOperator() { Operator.Equals }
}

public static class DoubleBoolNE extends BinaryFunc {
	protected DoubleBoolNE(Env env) { super(env,"%not-equals(double,double)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() != arg2.doubleValue()) }
	public Operator getOperator() { Operator.NotEquals }
}

public static class DoubleBoolGE extends BinaryFunc {
	protected DoubleBoolGE(Env env) { super(env,"%greater-equals(double,double)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() >= arg2.doubleValue()) }
	public Operator getOperator() { Operator.GreaterEquals }
}

public static class DoubleBoolLE extends BinaryFunc {
	protected DoubleBoolLE(Env env) { super(env,"%less-equals(double,double)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() <= arg2.doubleValue()) }
	public Operator getOperator() { Operator.LessEquals }
}

public static class DoubleBoolGT extends BinaryFunc {
	protected DoubleBoolGT(Env env) { super(env,"%greater-then(double,double)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() > arg2.doubleValue()) }
	public Operator getOperator() { Operator.GreaterThen }
}

public static class DoubleBoolLT extends BinaryFunc {
	protected DoubleBoolLT(Env env) { super(env,"%less-then(double,double)→bool", BinaryBoolExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstBoolExpr(arg1.doubleValue() < arg2.doubleValue()) }
	public Operator getOperator() { Operator.LessThen }
}

public static class DoubleADD extends BinaryFunc {
	protected DoubleADD(Env env) { super(env,"%add(double,double)→double", BinaryExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() + arg2.doubleValue()) }
	public Operator getOperator() { Operator.Add }
}

public static class DoubleSUB extends BinaryFunc {
	protected DoubleSUB(Env env) { super(env,"%sub(double,double)→double", BinaryExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() - arg2.doubleValue()) }
	public Operator getOperator() { Operator.Sub }
}

public static class DoubleMUL extends BinaryFunc {
	protected DoubleMUL(Env env) { super(env,"%mul(double,double)→double", BinaryExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() * arg2.doubleValue()) }
	public Operator getOperator() { Operator.Mul }
}

public static class DoubleDIV extends BinaryFunc {
	protected DoubleDIV(Env env) { super(env,"%div(double,double)→double", BinaryExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() / arg2.doubleValue()) }
	public Operator getOperator() { Operator.Div }
}

public static class DoubleMOD extends BinaryFunc {
	protected DoubleMOD(Env env) { super(env,"%mod(double,double)→double", BinaryExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg1, Number:Object arg2) { new ConstDoubleExpr(arg1.doubleValue() % arg2.doubleValue()) }
	public Operator getOperator() { Operator.Mod }
}

public static class DoublePOS extends UnaryFunc {
	protected DoublePOS(Env env) { super(env,"%positive(double)→double", UnaryExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstDoubleExpr( + arg.doubleValue()) }
	public Operator getOperator() { Operator.Pos }
}

public static class DoubleNEG extends UnaryFunc {
	protected DoubleNEG(Env env) { super(env,"%negative(double)→double", UnaryExpr.class, mkType(env.tenv.tpDouble, env.tenv.tpDouble)); }
	protected ConstExpr doCalc(Number:Object arg) { new ConstDoubleExpr( - arg.doubleValue()) }
	public Operator getOperator() { Operator.Neg }
}



/////////////////////////////////////////////////
//         String                              //
/////////////////////////////////////////////////

public static abstract class StringConcat extends BinaryFunc {
	StringConcat(Env env, String opname, Class node_clazz, CallType otype) {
		super(env, opname, node_clazz, otype);
	}
	
	protected ConstExpr doCalc(Object arg1, Object arg2) { new ConstStringExpr(String.valueOf(arg1) + String.valueOf(arg2)) }
	public Operator getOperator() { Operator.Add }
}

public static class StringConcatSS extends StringConcat {
	protected StringConcatSS(Env env) { super(env,"%concat-string(string,string)→string", StringConcatExpr.class, mkType(env.tenv.tpString, env.tenv.tpString, env.tenv.tpString)); }
}
public static class StringConcatAS extends StringConcat {
	protected StringConcatAS(Env env) { super(env,"%concat-string(any,string)→string", StringConcatExpr.class, mkType(env.tenv.tpString, env.tenv.tpAny, env.tenv.tpString)); }
}
public static class StringConcatSA extends StringConcat {
	protected StringConcatSA(Env env) { super(env,"%concat-string(string,any)→string", StringConcatExpr.class, mkType(env.tenv.tpString, env.tenv.tpString, env.tenv.tpAny)); }
}

public static class StringAssignADD extends BinaryFunc {
	protected StringAssignADD(Env env) { super(env,"%assign-add(string,any)→string", ModifyExpr.class, mkType(env.tenv.tpString, env.tenv.tpString, env.tenv.tpAny)); }
	public void normilizeExpr(ENode expr, INode parent, AttrSlot slot) {
		ENode[] args = expr.getEArgs();
		if (args == null || args.length != 2) {
			Kiev.reportError(expr, "Don't know how to normalize "+expr.getClass());
			return;
		}
		ENode lval = args[0];
		ENode value = args[1];
		ENode en = new AssignExpr(expr.pos,
			~lval,
			new BinaryExpr(expr.pos,Operator.Add,new Copier().copyFull(lval),~value)
			);
		expr.replaceWithNodeReWalk(en,parent,slot);
	}
	public Operator getOperator() { Operator.AssignAdd }
}


public static class Conditional extends CoreFunc {
	protected Conditional(Env env) { super(env,"%conditional(bool,any,any)→any", ConditionalExpr.class, mkType(env.tenv.tpAny, env.tenv.tpBoolean, env.tenv.tpAny, env.tenv.tpAny)); }
	public void normilizeExpr(ENode expr, INode parent, AttrSlot slot) {
		if (expr.getClass() == ConditionalExpr.class) {
			if (expr.dnode != this.operation)
				expr.symbol = this.operation.symbol;
			return;
		}
		ENode[] args = expr.getEArgs();
		if (args == null || args.length != 3) {
			Kiev.reportError(expr, "Don't know how to normalize "+expr.getClass()+" into "+ConditionalExpr.class);
			return;
		}
		for (int i=0; i < args.length; i++)
			args[i] = args[i].detach();
		ConditionalExpr ce = new ConditionalExpr(expr.pos, args[0], args[1], args[2]);
		ce.symbol = this.operation.symbol;
		expr.replaceWithNodeReWalk(ce,parent,slot);
	}
	public ConstExpr calc(ENode expr) {
		ENode[] args = expr.getEArgs();
		Boolean cond = (Boolean)args[0].getConstValue(Env.getEnv());
		ConstExpr ce1 = (ConstExpr)args[1];
		ConstExpr ce2 = (ConstExpr)args[2];
		if (cond.booleanValue())
			return ce1;
		else
			return ce2;
	}
	public Operator getOperator() { Operator.Conditional }
}

/////////////////////////////////////////////////
//         Access/Type operations              //
/////////////////////////////////////////////////


public static class IFldAccess extends BinaryFunc {
	protected IFldAccess(Env env) { super(env,"%access-ifld(any,ident)→any", IFldExpr.class, mkType(env.tenv.tpAny, env.tenv.tpAny, env.tenv.tpVoid)); }
	public Operator getOperator() { Operator.Access }
}

public static class SFldAccess extends BinaryFunc {
	protected SFldAccess(Env env) { super(env,"%access-sfld(type,ident)→any", SFldExpr.class, mkType(env.tenv.tpAny, env.tenv.tpAny, env.tenv.tpVoid)); }
	public Operator getOperator() { Operator.Access }
}

public static class OuterThisAccess extends BinaryFunc {
	protected OuterThisAccess(Env env) { super(env,"%access-outer-this(type,this)→any", OuterThisAccessExpr.class, mkType(env.tenv.tpAny, env.tenv.tpAny, env.tenv.tpVoid)); }
	public Operator getOperator() { Operator.Access }
}

public static class MacroAccess extends BinaryFunc {
	protected MacroAccess(Env env) { super(env,"%access-macro(any,ident)→any", MacroAccessExpr.class, mkType(env.tenv.tpAny, env.tenv.tpAny, env.tenv.tpVoid)); }
	public Operator getOperator() { Operator.MacroAccess }
}

public static class ClassAccess extends UnaryFunc {
	protected ClassAccess(Env env) { super(env,"%class-of(any)→class", TypeClassExpr.class, mkType(env.tenv.tpClass, env.tenv.tpAny)); }
	public Operator getOperator() { Operator.ClassAccess }
}

public static class TypeinfoAccess extends UnaryFunc {
	protected TypeinfoAccess(Env env) { super(env,"%type-of(any)→type", TypeInfoExpr.class, mkType(env.tenv.tpClass, env.tenv.tpAny)); }
	public Operator getOperator() { Operator.PathTypeAccess }
}

public static class Cast extends BinaryFunc {
	protected Cast(Env env) { super(env,"%cast(any,type)→any", CastExpr.class, mkType(env.tenv.tpAny, env.tenv.tpAny, env.tenv.tpVoid)); }
	public Operator getOperator() { Operator.CastForce }
}

public static class Reinterp extends BinaryFunc {
	protected Reinterp(Env env) { super(env,"%reinterp(any,type)→any", ReinterpExpr.class, mkType(env.tenv.tpAny, env.tenv.tpAny, env.tenv.tpVoid)); }
	public Operator getOperator() { Operator.Reinterp }
}

public static class RuleIsThe extends BinaryFunc {
	protected RuleIsThe(Env env) { super(env,"%rule-is-the(var,obj)→bool", RuleIstheExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpPrologVar, env.tenv.tpObject)); }
	public Operator getOperator() { Operator.RuleIsThe }
}

public static class RuleIsOneOf extends BinaryFunc {
	protected RuleIsOneOf(Env env) { super(env,"%rule-is-one_of(var,obj)→bool", RuleIsoneofExpr.class, mkType(env.tenv.tpBoolean, env.tenv.tpPrologVar, env.tenv.tpObject)); }
	public Operator getOperator() { Operator.RuleIsOneOf }
}

public static class PathTypeAccess extends UnaryFunc {
	protected PathTypeAccess(Env env) { super(env,"%type-of-path(any)→type", PathTypeRef.class, mkType(env.tenv.tpVoid, env.tenv.tpAny)); }
	public Operator getOperator() { Operator.PathTypeAccess }
}


/////////////////////////////////////////////////
//         ASTNodeType                         //
/////////////////////////////////////////////////


public static class ASTNodeEQ extends BinaryFunc {
	protected ASTNodeEQ(Env env) { super(env,"%equals(node,node)→bool", MacroBinaryBoolExpr.class, mkType(env.tenv.tpBoolean, new ASTNodeType(ANode.class), new ASTNodeType(ANode.class))); }
	protected ConstExpr doCalc(Object arg1, Object arg2) { new ConstBoolExpr(arg1 == arg2) }
	public Operator getOperator() { Operator.Equals }
}

public static class ASTNodeNE extends BinaryFunc {
	protected ASTNodeNE(Env env) { super(env,"%not-equals(node,node)→bool", MacroBinaryBoolExpr.class, mkType(env.tenv.tpBoolean, new ASTNodeType(ANode.class), new ASTNodeType(ANode.class))); }
	protected ConstExpr doCalc(Object arg1, Object arg2) { new ConstBoolExpr(arg1 != arg2) }
	public Operator getOperator() { Operator.NotEquals }
}

} // CoreFunc
