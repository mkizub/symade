package kiev.stdlib;

import kiev.vlang.NewExpr;
import kiev.vlang.CallExpr;
import syntax kiev.stdlib.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 0 $
 *
 */

public metatype any {
	@macro @native @CompilerNode("InstanceOf")
	public static boolean _instanceof_(any val, any type) alias xfx operator operator instanceof ;

	@macro @native @CompilerNode("Cmp")
	public static boolean ref_eq(Object o1, Object o2) alias xfx operator == ;

	@macro @native @CompilerNode("Cmp")
	public static boolean ref_neq(Object o1, Object o2) alias xfx operator != ;

}

public metatype void {}

public metatype boolean extends any {
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
		case CallExpr# self(): new #BinaryBoolExpr(expr1=self.obj,op="V == V",expr2=val)
	}

	@macro
	public Class getClass()
	{
		case CallExpr# self(): new #SFldExpr(obj=java.lang.Boolean,ident="TYPE")
	}

	@macro
	public boolean clone()
	{
		case CallExpr# self(): self.obj
	}

	@macro
	public String toString()
	{
		case CallExpr# self(): new #CallExpr(obj=java.lang.String,ident="valueOf",args={self.obj})
	}

	@macro
	public int hashCode()
	{
		case CallExpr# self(): new #CallExpr(obj=new #NewExpr(type=java.lang.Boolean,args={self.obj}),ident="hashCode")
	}
}

public metatype char extends any {
	@macro @native @CompilerNode("Set")
	public char assign(char val) alias lfy operator = ;

	@macro @CompilerNode("Cmp")
	public boolean equals(char val)
	{
		case CallExpr# self(): new #BinaryBoolExpr(expr1=self.obj,op="V == V",expr2=val)
	}

	@macro
	public Class getClass()
	{
		case CallExpr# self(): new #SFldExpr(obj=java.lang.Character,ident="TYPE")
	}

	@macro
	public char clone()
	{
		case CallExpr# self(): self.obj
	}

	@macro
	public String toString()
	{
		case CallExpr# self(): new #CallExpr(obj=java.lang.String,ident="valueOf",args={self.obj})
	}

	@macro
	public int hashCode()
	{
		case CallExpr# self(): new #CallExpr(obj=new #NewExpr(type=java.lang.Character,args={self.obj}),ident="hashCode")
	}
}

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
		case CallExpr# self(): new #BinaryBoolExpr(expr1=self.obj,op="V == V",expr2=val)
	}

	@macro
	public Class getClass()
	{
		case CallExpr# self(): new #SFldExpr(obj=java.lang.Byte,ident="TYPE")
	}

	@macro
	public byte clone()
	{
		case CallExpr# self(): self.obj
	}

	@macro
	public String toString()
	{
		case CallExpr# self(): new #CallExpr(obj=java.lang.String,ident="valueOf",args={self.obj})
	}

	@macro
	public int hashCode()
	{
		case CallExpr# self(): new #CallExpr(obj=new #NewExpr(type=java.lang.Byte,args={self.obj}),ident="hashCode")
	}
}

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
		case CallExpr# self(): new #BinaryBoolExpr(expr1=self.obj,op="V == V",expr2=val)
	}

	@macro
	public Class getClass()
	{
		case CallExpr# self(): new #SFldExpr(obj=java.lang.Short,ident="TYPE")
	}

	@macro
	public short clone()
	{
		case CallExpr# self(): self.obj
	}

	@macro
	public String toString()
	{
		case CallExpr# self(): new #CallExpr(obj=java.lang.String,ident="valueOf",args={self.obj})
	}

	@macro
	public int hashCode()
	{
		case CallExpr# self(): new #CallExpr(obj=new #NewExpr(type=java.lang.Short,args={self.obj}),ident="hashCode")
	}
}

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
		case CallExpr# self(): new #BinaryBoolExpr(expr1=self.obj,op="V == V",expr2=val)
	}

	@macro
	public Class getClass()
	{
		case CallExpr# self(): new #SFldExpr(obj=java.lang.Integer,ident="TYPE")
	}

	@macro
	public int clone()
	{
		case CallExpr# self(): self.obj
	}

	@macro
	public String toString()
	{
		case CallExpr# self(): new #CallExpr(obj=java.lang.String,ident="valueOf",args={self.obj})
	}

	@macro
	public int hashCode()
	{
		case CallExpr# self(): new #CallExpr(obj=new #NewExpr(type=java.lang.Integer,args={self.obj}),ident="hashCode")
	}
}

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
	public long assign_left_shift(long val) alias lfy operator <<= ;

	@macro @native @CompilerNode("Set")
	public long assign_right_shift(long val) alias lfy operator >>= ;

	@macro @native @CompilerNode("Set")
	public long assign_unsigned_right_shift(long val) alias lfy operator >>>= ;

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
		case CallExpr# self(): new #BinaryBoolExpr(expr1=self.obj,op="V == V",expr2=val)
	}

	@macro
	public Class getClass()
	{
		case CallExpr# self(): new #SFldExpr(obj=java.lang.Long,ident="TYPE")
	}

	@macro
	public long clone()
	{
		case CallExpr# self(): self.obj
	}

	@macro
	public String toString()
	{
		case CallExpr# self(): new #CallExpr(obj=java.lang.String,ident="valueOf",args={self.obj})
	}

	@macro
	public int hashCode()
	{
		case CallExpr# self(): new #CallExpr(obj=new #NewExpr(type=java.lang.Long,args={self.obj}),ident="hashCode")
	}
}

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
		case CallExpr# self(): new #BinaryBoolExpr(expr1=self.obj,op="V == V",expr2=val)
	}

	@macro
	public Class getClass()
	{
		case CallExpr# self(): new #SFldExpr(obj=java.lang.Float,ident="TYPE")
	}

	@macro
	public float clone()
	{
		case CallExpr# self(): self.obj
	}

	@macro
	public String toString()
	{
		case CallExpr# self(): new #CallExpr(obj=java.lang.String,ident="valueOf",args={self.obj})
	}

	@macro
	public int hashCode()
	{
		case CallExpr# self(): new #CallExpr(obj=new #NewExpr(type=java.lang.Float,args={self.obj}),ident="hashCode")
	}
}

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
		case CallExpr# self(): new #BinaryBoolExpr(expr1=self.obj,op="V == V",expr2=val)
	}

	@macro
	public Class getClass()
	{
		case CallExpr# self(): new #SFldExpr(obj=java.lang.Double,ident="TYPE")
	}

	@macro
	public double clone()
	{
		case CallExpr# self(): self.obj
	}

	@macro
	public String toString()
	{
		case CallExpr# self(): new #CallExpr(obj=java.lang.String,ident="valueOf",args={self.obj})
	}

	@macro
	public int hashCode()
	{
		case CallExpr# self(): new #CallExpr(obj=new #NewExpr(type=java.lang.Double,args={self.obj}),ident="hashCode")
	}
}
/*
public metatype _array_<_elem_ extends any> extends Object {
	@macro @native
	public:ro final int length;

	@macro @native
	public lvalue element(int idx) alias xfy operator []
	{
		@macro @native @getter
		public _elem_ get$element(int idx);

		@macro @native @setter
		public <R extends _elem_> R set$element(int idx, R val);
	}
}
*/
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

