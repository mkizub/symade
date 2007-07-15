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


// UUID name-based generated as:
// URL is http://www.symade.com/ and UUID is generated as:
// java -jar jug-lgpl-2.0.0.jar --name http://www.symade.com/ --namespace URL name-based => 6189bf45-88e8-3a27-8ebf-0a14795e29a7
// UUID for names in this package were geberated using this UUID as a base, for instance:
// java -jar jug-lgpl-2.0.0.jar --name kiev.stdlib.any --namespace 6189bf45-88e8-3a27-8ebf-0a14795e29a7 name-based

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@uuid("be8bba7f-b4f9-3991-8834-6552dcb237a0")
public metatype any {
	@macro @native @CompilerNode("InstanceOf")
	public static boolean _instanceof_(any val, any type) operator "V instanceof T" ;

	@macro @native @CompilerNode("Cmp")
	public static boolean ref_eq(Object o1, Object o2) operator "V == V" ;

	@macro @native @CompilerNode("Cmp")
	public static boolean ref_neq(Object o1, Object o2) operator "V != V" ;

	@macro @native @CompilerNode("Set")
	public static <L extends Object, R extends L> R ref_assign(L lval, R val) operator "V = V" continue "kiev.vlang.Globals:ref_assign";

	@macro @native @CompilerNode("Set")
	public static <L extends Object, R extends L> R ref_assign2(L lval, R val) operator "V := V" continue "kiev.vlang.Globals:ref_assign2";

	@macro @native @CompilerNode("Set")
	public static <L extends Object, R extends L> R@ ref_pvar_init(L@ lval, R@ val) operator "V := V" continue "kiev.vlang.Globals:ref_pvar_init";

	@macro @CompilerNode("RuleIstheExpr")
	public static boolean ref_pvar_is_the(Object@ lval, Object val) operator "V ?= V" continue "kiev.vlang.Globals:ref_pvar_is_the";

	@macro @CompilerNode("RuleIsoneofExpr")
	public static boolean ref_pvar_is_one_of(Object@ lval, Object val) operator "V @= V" continue "kiev.vlang.Globals:ref_pvar_is_one_of";

	@macro @CompilerNode("Set")
	public static <L extends Object, R extends L> R ref_assign_pvar(L lval, R@ val) operator "V = V"
	{
		case Set# self():
			self.lval = (self.value).get$$var()
		case Call# self():
			lval = (val).get$$var()
	}

	@macro @CompilerNode("Set")
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R val) operator "V = V"
	{
		case Call# self():
			(lval).$bind(val)
		case Set# self():
			(self.lval).$bind(self.value)
	}

	@macro @CompilerNode("Set")
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R@ val) operator "V = V"
	{
		case Call# self():
			(lval).$bind(val)
		case Set# self():
			(self.lval).$bind(self.value)
	}

}

@uuid("ec98468f-75f6-3811-ab77-6b0a8458b3ad")
public metatype void {}

@uuid("6c8cef01-5c38-36c3-aab0-bd16c23e817d")
public metatype #id"null"# extends Object {

	public static final #id"null"# #id"null"#;
	
}

@uuid("9c517365-318e-307c-acdf-6682cf309b3f")
public metatype boolean extends any {
	
	public static final boolean #id"false"# = ($reinterp boolean)0;
	
	public static final boolean #id"true"# = ($reinterp boolean)1;
	
	@macro @native @CompilerNode("Set")
	public boolean assign(boolean val) alias lfy operator = ;

	@macro @native @CompilerNode("Set")
	public int assign_bit_or(boolean val) alias lfy operator |= ;

	@macro @native @CompilerNode("Set")
	public int assign_bit_and(boolean val) alias lfy operator &= ;

	@macro @native @CompilerNode("Set")
	public int assign_bit_xor(boolean val) alias lfy operator ^= ;

	@macro @native @CompilerNode("BinOp")
	public static boolean bit_or(boolean b1, boolean b2) alias yfx operator | ;

	@macro @native @CompilerNode("BinOp")
	public static boolean bit_and(boolean b1, boolean b2) alias yfx operator & ;

	@macro @native @CompilerNode("BinOp")
	public static boolean bit_xor(boolean b1, boolean b2) alias yfx operator ^ ;

	@macro @native @CompilerNode("Or")
	public static boolean bool_or(boolean b1, boolean b2) alias yfx operator || ;

	@macro @native @CompilerNode("And")
	public static boolean bool_and(boolean b1, boolean b2) alias yfx operator && ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_eq(boolean b1, boolean b2) alias xfx operator == ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_neq(boolean b1, boolean b2) alias xfx operator != ;

	@macro @native @CompilerNode("Not")
	public static boolean bool_not(boolean b1) alias fy operator ! ;

	@macro @CompilerNode("Cmp")
	public boolean equals(boolean val)
	{
		case Call# self(): this == val
	}

	@macro
	public Class getClass()
	{
		case Call# self(): Boolean.TYPE
	}

	@macro
	public boolean clone()
	{
		case Call# self(): this
	}

	@macro
	public String toString()
	{
		case Call# self(): String.valueOf(this)
	}

	@macro
	public int hashCode()
	{
		case Call# self(): new Boolean(this).hashCode()
	}
}

@uuid("7713311e-809c-30f7-964a-3d28beb7aab3")
public metatype char extends any {
	@macro @native @CompilerNode("Set")
	public char assign(char val) alias lfy operator = ;

	@macro @CompilerNode("Cmp")
	public boolean equals(char val)
	{
		case Call# self(): this == val
	}

	@macro
	public Class getClass()
	{
		case Call# self(): Character.TYPE
	}

	@macro
	public char clone()
	{
		case Call# self(): this
	}

	@macro
	public String toString()
	{
		case Call# self(): String.valueOf(this)
	}

	@macro
	public int hashCode()
	{
		case Call# self(): new Character(this).hashCode()
	}
}

@uuid("89ed44f6-f9a6-3ef7-b396-d2248d5f69db")
public metatype byte extends any {
	@macro @native @CompilerNode("Set")
	public byte assign(byte val) alias lfy operator = ;

	@macro @native @CompilerNode("UnaryOp")
	public byte positive() alias fy operator + ;

	@macro @native @CompilerNode("UnaryOp")
	public byte negative() alias fy operator - ;

	@macro @CompilerNode("Cmp")
	public boolean equals(byte val)
	{
		case Call# self(): this == val
	}

	@macro
	public Class getClass()
	{
		case Call# self(): Byte.TYPE
	}

	@macro
	public byte clone()
	{
		case Call# self(): this
	}

	@macro
	public String toString()
	{
		case Call# self(): String.valueOf(this)
	}

	@macro
	public int hashCode()
	{
		case Call# self(): new Byte(this).hashCode()
	}
}

@uuid("f9bb2439-c397-3930-b36c-5b1565ec7841")
public metatype short extends any {
	@macro @native @CompilerNode("Set")
	public short assign(short val) alias lfy operator = ;

	@macro @native @CompilerNode("UnaryOp")
	public short positive() alias fy operator + ;

	@macro @native @CompilerNode("UnaryOp")
	public short negative() alias fy operator - ;

	@macro @CompilerNode("Cmp")
	public boolean equals(short val)
	{
		case Call# self(): this == val
	}

	@macro
	public Class getClass()
	{
		case Call# self(): Short.TYPE
	}

	@macro
	public short clone()
	{
		case Call# self(): this
	}

	@macro
	public String toString()
	{
		case Call# self(): String.valueOf(this)
	}

	@macro
	public int hashCode()
	{
		case Call# self(): new Short(this).hashCode()
	}
}

@uuid("d50f9a1a-2e09-3313-8a64-6b58b300579e")
public metatype int extends any {
	@macro @native @CompilerNode("Set")
	public int assign(int val) alias lfy operator = ;

	@macro @native @CompilerNode("Set")
	public int assign_bit_or(int val) alias lfy operator |= ;

	@macro @native @CompilerNode("Set")
	public int assign_bit_and(int val) alias lfy operator &= ;

	@macro @native @CompilerNode("Set")
	public int assign_bit_xor(int val) alias lfy operator ^= ;

	@macro @native @CompilerNode("Set")
	public int assign_left_shift(int val) alias lfy operator <<= ;

	@macro @native @CompilerNode("Set")
	public int assign_right_shift(int val) alias lfy operator >>= ;

	@macro @native @CompilerNode("Set")
	public int assign_unsigned_right_shift(int val) alias lfy operator >>>= ;

	@macro @native @CompilerNode("Set")
	public int assign_add(int val) alias lfy operator += ;

	@macro @native @CompilerNode("Set")
	public int assign_sub(int val) alias lfy operator -= ;

	@macro @native @CompilerNode("Set")
	public int assign_mul(int val) alias lfy operator *= ;

	@macro @native @CompilerNode("Set")
	public int assign_div(int val) alias lfy operator /= ;

	@macro @native @CompilerNode("Set")
	public int assign_mod(int val) alias lfy operator %= ;

	@macro @native @CompilerNode("BinOp")
	public static int bit_or(int i1, int i2) alias yfx operator | ;

	@macro @native @CompilerNode("BinOp")
	public static int bit_xor(int i1, int i2) alias yfx operator ^ ;

	@macro @native @CompilerNode("BinOp")
	public static int bit_and(int i1, int i2) alias yfx operator & ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_eq(int i1, int i2) alias xfx operator == ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_neq(int i1, int i2) alias xfx operator != ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_ge(int i1, int i2) alias xfx operator >= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_le(int i1, int i2) alias xfx operator <= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_gt(int i1, int i2) alias xfx operator > ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_lt(int i1, int i2) alias xfx operator < ;

	@macro @native @CompilerNode("BinOp")
	public static int left_shift(int i1, int shft) alias xfx operator << ;

	@macro @native @CompilerNode("BinOp")
	public static int right_shift(int i1, int shft) alias xfx operator >> ;

	@macro @native @CompilerNode("BinOp")
	public static int unsigned_right_shift(int i1, int shft) alias xfx operator >>> ;

	@macro @native @CompilerNode("BinOp")
	public static int add(int i1, int i2) alias yfx operator + ;

	@macro @native @CompilerNode("BinOp")
	public static int sub(int i1, int i2) alias yfx operator - ;

	@macro @native @CompilerNode("BinOp")
	public static int mul(int i1, int i2) alias yfx operator * ;

	@macro @native @CompilerNode("BinOp")
	public static int div(int i1, int i2) alias yfx operator / ;

	@macro @native @CompilerNode("BinOp")
	public static int mod(int i1, int i2) alias yfx operator % ;

	@macro @native @CompilerNode("UnaryOp")
	public int positive() alias fy operator + ;

	@macro @native @CompilerNode("UnaryOp")
	public int negative() alias fy operator - ;

	@macro @native @CompilerNode("IncrOp")
	public static int pre_incr(int lval) alias fx operator ++ ;

	@macro @native @CompilerNode("IncrOp")
	public static int pre_decr(int lval) alias fx operator -- ;

	@macro @native @CompilerNode("UnaryOp")
	public static int bit_not(int i1) alias fy operator ~ ;

	@macro @native @CompilerNode("IncrOp")
	public static int post_incr(int lval) alias xf operator ++ ;

	@macro @native @CompilerNode("IncrOp")
	public static int post_decr(int lval) alias xf operator -- ;

	@macro @CompilerNode("Cmp")
	public boolean equals(int val)
	{
		case Call# self(): this == val
	}

	@macro
	public Class getClass()
	{
		case Call# self(): Integer.TYPE
	}

	@macro
	public int clone()
	{
		case Call# self(): this
	}

	@macro
	public String toString()
	{
		case Call# self(): String.valueOf(this)
	}

	@macro
	public int hashCode()
	{
		case Call# self(): new Integer(this).hashCode()
	}
}

@uuid("2d6eef81-2c5e-36e4-ab9d-136dfec1dc6b")
public metatype long extends any {
	@macro @native @CompilerNode("Set")
	public long assign(long val) alias lfy operator = ;

	@macro @native @CompilerNode("Set")
	public long assign_bit_or(long val) alias lfy operator |= ;

	@macro @native @CompilerNode("Set")
	public long assign_bit_and(long val) alias lfy operator &= ;

	@macro @native @CompilerNode("Set")
	public long assign_bit_xor(long val) alias lfy operator ^= ;

	@macro @native @CompilerNode("Set")
	public long assign_left_shift(int val) alias lfy operator <<= ;

	@macro @native @CompilerNode("Set")
	public long assign_right_shift(int val) alias lfy operator >>= ;

	@macro @native @CompilerNode("Set")
	public long assign_unsigned_right_shift(int val) alias lfy operator >>>= ;

	@macro @native @CompilerNode("Set")
	public long assign_add(long val) alias lfy operator += ;

	@macro @native @CompilerNode("Set")
	public long assign_sub(long val) alias lfy operator -= ;

	@macro @native @CompilerNode("Set")
	public long assign_mul(long val) alias lfy operator *= ;

	@macro @native @CompilerNode("Set")
	public long assign_div(long val) alias lfy operator /= ;

	@macro @native @CompilerNode("Set")
	public long assign_mod(long val) alias lfy operator %= ;

	@macro @native @CompilerNode("BinOp")
	public static long bit_or(long l1, long l2) alias yfx operator | ;

	@macro @native @CompilerNode("BinOp")
	public static long bit_xor(long l1, long l2) alias yfx operator ^ ;

	@macro @native @CompilerNode("BinOp")
	public static long bit_and(long l1, long l2) alias yfx operator & ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_eq(long l1, long l2) alias xfx operator == ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_neq(long l1, long l2) alias xfx operator != ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_ge(long l1, long l2) alias xfx operator >= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_le(long l1, long l2) alias xfx operator <= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_gt(long l1, long l2) alias xfx operator > ;

	@macro @native @CompilerNode("BinOp")
	public static boolean bool_lt(long l1, long l2) alias xfx operator < ;

	@macro @native @CompilerNode("BinOp")
	public static long left_shift(long val, int shft) alias xfx operator << ;

	@macro @native @CompilerNode("BinOp")
	public static long right_shift(long val, int shft) alias xfx operator >> ;

	@macro @native @CompilerNode("BinOp")
	public static long unsigned_right_shift(long val, int shft) alias xfx operator >>> ;

	@macro @native @CompilerNode("BinOp")
	public static long add(long l1, long l2) alias yfx operator + ;

	@macro @native @CompilerNode("BinOp")
	public static long sub(long l1, long l2) alias yfx operator - ;

	@macro @native @CompilerNode("BinOp")
	public static long mul(long l1, long l2) alias yfx operator * ;

	@macro @native @CompilerNode("BinOp")
	public static long div(long l1, long l2) alias yfx operator / ;

	@macro @native @CompilerNode("BinOp")
	public static long mod(long l1, long l2) alias yfx operator % ;

	@macro @native @CompilerNode("UnaryOp")
	public long positive() alias fy operator + ;

	@macro @native @CompilerNode("UnaryOp")
	public long negative() alias fy operator - ;

	@macro @native @CompilerNode("IncrOp")
	public static long pre_incr(long lval) alias fx operator ++ ;

	@macro @native @CompilerNode("IncrOp")
	public static long pre_decr(long lval) alias fx operator -- ;

	@macro @native @CompilerNode("UnaryOp")
	public static long bit_not(long val) alias fy operator ~ ;

	@macro @native @CompilerNode("IncrOp")
	public static long post_incr(long lval) alias xf operator ++ ;

	@macro @native @CompilerNode("IncrOp")
	public static long post_decr(long lval) alias xf operator -- ;

	@macro @CompilerNode("Cmp")
	public boolean equals(long val)
	{
		case Call# self(): this == val
	}

	@macro
	public Class getClass()
	{
		case Call# self(): Long.TYPE
	}

	@macro
	public long clone()
	{
		case Call# self(): this
	}

	@macro
	public String toString()
	{
		case Call# self(): String.valueOf(this)
	}

	@macro
	public int hashCode()
	{
		case Call# self(): new Long(this).hashCode()
	}
}

@uuid("a02d23b3-8055-3c87-b331-2b242964a7f1")
public metatype float extends any {
	@macro @native @CompilerNode("Set")
	public float assign(float val) alias lfy operator = ;

	@macro @native @CompilerNode("Set")
	public float assign_add(float val) alias lfy operator += ;

	@macro @native @CompilerNode("Set")
	public float assign_sub(float val) alias lfy operator -= ;

	@macro @native @CompilerNode("Set")
	public float assign_mul(float val) alias lfy operator *= ;

	@macro @native @CompilerNode("Set")
	public float assign_div(float val) alias lfy operator /= ;

	@macro @native @CompilerNode("Set")
	public float assign_mod(float val) alias lfy operator %= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_eq(float f1, float f2) alias xfx operator == ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_neq(float f1, float f2) alias xfx operator != ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_ge(float f1, float f2) alias xfx operator >= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_le(float f1, float f2) alias xfx operator <= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_gt(float f1, float f2) alias xfx operator > ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_lt(float f1, float f2) alias xfx operator < ;

	@macro @native @CompilerNode("BinOp")
	public static float add(float f1, float f2) alias yfx operator + ;

	@macro @native @CompilerNode("BinOp")
	public static float sub(float f1, float f2) alias yfx operator - ;

	@macro @native @CompilerNode("BinOp")
	public static float mul(float f1, float f2) alias yfx operator * ;

	@macro @native @CompilerNode("BinOp")
	public static float div(float f1, float f2) alias yfx operator / ;

	@macro @native @CompilerNode("BinOp")
	public static float mod(float f1, float f2) alias yfx operator % ;

	@macro @native @CompilerNode("UnaryOp")
	public float positive() alias fy operator + ;

	@macro @native @CompilerNode("UnaryOp")
	public float negative() alias fy operator - ;

	@macro @CompilerNode("Cmp")
	public boolean equals(float val)
	{
		case Call# self(): this == val
	}

	@macro
	public Class getClass()
	{
		case Call# self(): Float.TYPE
	}

	@macro
	public float clone()
	{
		case Call# self(): this
	}

	@macro
	public String toString()
	{
		case Call# self(): String.valueOf(this)
	}

	@macro
	public int hashCode()
	{
		case Call# self(): new Float(this).hashCode()
	}
}

@uuid("d741575d-769c-3108-810e-6c0e57a4b03e")
public metatype double extends any {
	@macro @native @CompilerNode("Set")
	public double assign(double val) alias lfy operator = ;

	@macro @native @CompilerNode("Set")
	public double assign_add(double val) alias lfy operator += ;

	@macro @native @CompilerNode("Set")
	public double assign_sub(double val) alias lfy operator -= ;

	@macro @native @CompilerNode("Set")
	public double assign_mul(double val) alias lfy operator *= ;

	@macro @native @CompilerNode("Set")
	public double assign_div(double val) alias lfy operator /= ;

	@macro @native @CompilerNode("Set")
	public double assign_mod(double val) alias lfy operator %= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_eq(double d1, double d2) alias xfx operator == ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_neq(double d1, double d2) alias xfx operator != ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_ge(double d1, double d2) alias xfx operator >= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_le(double d1, double d2) alias xfx operator <= ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_gt(double d1, double d2) alias xfx operator > ;

	@macro @native @CompilerNode("Cmp")
	public static boolean bool_lt(double d1, double d2) alias xfx operator < ;

	@macro @native @CompilerNode("BinOp")
	public static double add(double d1, double d2) alias yfx operator + ;

	@macro @native @CompilerNode("BinOp")
	public static double sub(double d1, double d2) alias yfx operator - ;

	@macro @native @CompilerNode("BinOp")
	public static double mul(double d1, double d2) alias yfx operator * ;

	@macro @native @CompilerNode("BinOp")
	public static double div(double d1, double d2) alias yfx operator / ;

	@macro @native @CompilerNode("BinOp")
	public static double mod(double d1, double d2) alias yfx operator % ;

	@macro @native @CompilerNode("UnaryOp")
	public double positive() alias fy operator + ;

	@macro @native @CompilerNode("UnaryOp")
	public double negative() alias fy operator - ;

	@macro @CompilerNode("Cmp")
	public boolean equals(double val)
	{
		case Call# self(): this == val
	}

	@macro
	public Class getClass()
	{
		case Call# self(): Double.TYPE
	}

	@macro
	public double clone()
	{
		case Call# self(): this
	}

	@macro
	public String toString()
	{
		case Call# self(): String.valueOf(this)
	}

	@macro
	public int hashCode()
	{
		case Call# self(): new Double(this).hashCode()
	}
}

public metatype GString extends java.lang.String {

	@macro @native @CompilerNode("StrConcat")
	public static String str_concat_ss(String s1, String s2) alias yfx operator + ;

	@macro @native @CompilerNode("StrConcat")
	public static String str_concat_as(any s1, String s2) alias yfx operator + ;

	@macro @native @CompilerNode("StrConcat")
	public static String str_concat_sa(String s1, any s2) alias yfx operator + ;

	@macro @native @CompilerNode("Set")
	public static String str_assign_add(String s1, any s2) alias lfy operator += ;
}

