package kiev.stdlib;

// assign operators
operator  =    , lfy,   5;
operator  :=   , lfy,   5;
operator  |=   , lfy,   5;
operator  &=   , lfy,   5;
operator  ^=   , lfy,   5;
operator  <<=  , lfy,   5;
operator  >>=  , lfy,   5;
operator  >>>= , lfy,   5;
operator  +=   , lfy,   5;
operator  -=   , lfy,   5;
operator  *=   , lfy,   5;
operator  /=   , lfy,   5;
operator  %=   , lfy,   5;

// infix operators
operator  ||         , yfx,  10;
operator  &&         , yfx,  20;
operator  |          , yfx,  30;
operator  ^          , yfx,  40;
operator  &          , yfx,  50;
operator  ==         , xfx,  60;
operator  !=         , xfx,  60;
operator  instanceof , xfx,  70;
operator  >=         , xfx,  80;
operator  <=         , xfx,  80;
operator  >          , xfx,  80;
operator  <          , xfx,  80;
operator  <<         , xfx,  90;
operator  >>         , xfx,  90;
operator  >>>        , xfx,  90;
operator  +          , yfx, 100;
operator  -          , yfx, 100;
operator  *          , yfx, 150;
operator  /          , yfx, 150;
operator  %          , yfx, 150;

// prefix operators
operator  +  ,  fy, 200;
operator  -  ,  fy, 200;
operator  ++ ,  fx, 210; // fl
operator  -- ,  fx, 210; // fl
operator  ~  ,  fy, 210;
operator  !  ,  fy, 210;

// postfix operators
operator  ++ ,  xf, 210; // lf
operator  -- ,  xf, 210; // lf



/**
 * @author Maxim Kizub
 * @version $Revision: 0 $
 *
 */

public metatype any {
	@macro @native
	public static boolean _instanceof_(any val, any type) alias xfx operator operator instanceof ;

	@macro @native
	public static boolean ref_eq(Object o1, Object o2) alias xfx operator == ;

	@macro @native
	public static boolean ref_neq(Object o1, Object o2) alias xfx operator != ;

}

public metatype void {}

public metatype boolean extends any {
	@macro @native
	public boolean assign(boolean val) alias lfy operator = ;

	@macro @native
	public int assign_bit_or(boolean val) alias lfy operator |= ;

	@macro @native
	public int assign_bit_and(boolean val) alias lfy operator &= ;

	@macro @native
	public int assign_bit_xor(boolean val) alias lfy operator ^= ;

	@macro @native
	public static boolean bit_or(boolean b1, boolean b2) alias yfx operator | ;

	@macro @native
	public static boolean bit_and(boolean b1, boolean b2) alias yfx operator & ;

	@macro @native
	public static boolean bit_xor(boolean b1, boolean b2) alias yfx operator ^ ;

	@macro @native
	public static boolean bool_or(boolean b1, boolean b2) alias yfx operator || ;

	@macro @native
	public static boolean bool_and(boolean b1, boolean b2) alias yfx operator && ;

	@macro @native
	public static boolean bool_eq(boolean b1, boolean b2) alias xfx operator == ;

	@macro @native
	public static boolean bool_neq(boolean b1, boolean b2) alias xfx operator != ;

	@macro @native
	public static boolean bool_not(boolean b1) alias fy operator ! ;
}

public metatype char extends any {
	@macro @native
	public char assign(char val) alias lfy operator = ;
}

public metatype byte extends any {
	@macro @native
	public byte assign(byte val) alias lfy operator = ;

	@macro @native
	public byte positive() alias fy operator + ;

	@macro @native
	public byte negative() alias fy operator - ;
}

public metatype short extends any {
	@macro @native
	public short assign(short val) alias lfy operator = ;

	@macro @native
	public short positive() alias fy operator + ;

	@macro @native
	public short negative() alias fy operator - ;
}

public metatype int extends any {
	@macro @native
	public int assign(int val) alias lfy operator = ;

	@macro @native
	public int assign_bit_or(int val) alias lfy operator |= ;

	@macro @native
	public int assign_bit_and(int val) alias lfy operator &= ;

	@macro @native
	public int assign_bit_xor(int val) alias lfy operator ^= ;

	@macro @native
	public int assign_left_shift(int val) alias lfy operator <<= ;

	@macro @native
	public int assign_right_shift(int val) alias lfy operator >>= ;

	@macro @native
	public int assign_unsigned_right_shift(int val) alias lfy operator >>>= ;

	@macro @native
	public int assign_add(int val) alias lfy operator += ;

	@macro @native
	public int assign_sub(int val) alias lfy operator -= ;

	@macro @native
	public int assign_mul(int val) alias lfy operator *= ;

	@macro @native
	public int assign_div(int val) alias lfy operator /= ;

	@macro @native
	public int assign_mod(int val) alias lfy operator %= ;

	@macro @native
	public static int bit_or(int i1, int i2) alias yfx operator | ;

	@macro @native
	public static int bit_xor(int i1, int i2) alias yfx operator ^ ;

	@macro @native
	public static int bit_and(int i1, int i2) alias yfx operator & ;

	@macro @native
	public static boolean bool_eq(int i1, int i2) alias xfx operator == ;

	@macro @native
	public static boolean bool_neq(int i1, int i2) alias xfx operator != ;

	@macro @native
	public static boolean bool_ge(int i1, int i2) alias xfx operator >= ;

	@macro @native
	public static boolean bool_le(int i1, int i2) alias xfx operator <= ;

	@macro @native
	public static boolean bool_gt(int i1, int i2) alias xfx operator > ;

	@macro @native
	public static boolean bool_lt(int i1, int i2) alias xfx operator < ;

	@macro @native
	public static int left_shift(int i1, int shft) alias xfx operator << ;

	@macro @native
	public static int right_shift(int i1, int shft) alias xfx operator >> ;

	@macro @native
	public static int unsigned_right_shift(int i1, int shft) alias xfx operator >>> ;

	@macro @native
	public static int add(int i1, int i2) alias yfx operator + ;

	@macro @native
	public static int sub(int i1, int i2) alias yfx operator - ;

	@macro @native
	public static int mul(int i1, int i2) alias yfx operator * ;

	@macro @native
	public static int div(int i1, int i2) alias yfx operator / ;

	@macro @native
	public static int mod(int i1, int i2) alias yfx operator % ;

	@macro @native
	public int positive() alias fy operator + ;

	@macro @native
	public int negative() alias fy operator - ;

	@macro @native
	public static int pre_incr(int lval) alias fx operator ++ ;

	@macro @native
	public static int pre_decr(int lval) alias fx operator -- ;

	@macro @native
	public static int bit_not(int i1) alias fy operator ~ ;

	@macro @native
	public static int post_incr(int lval) alias xf operator ++ ;

	@macro @native
	public static int post_decr(int lval) alias xf operator -- ;

}

public metatype long extends any {
	@macro @native
	public long assign(long val) alias lfy operator = ;

	@macro @native
	public long assign_bit_or(long val) alias lfy operator |= ;

	@macro @native
	public long assign_bit_and(long val) alias lfy operator &= ;

	@macro @native
	public long assign_bit_xor(long val) alias lfy operator ^= ;

	@macro @native
	public long assign_left_shift(long val) alias lfy operator <<= ;

	@macro @native
	public long assign_right_shift(long val) alias lfy operator >>= ;

	@macro @native
	public long assign_unsigned_right_shift(long val) alias lfy operator >>>= ;

	@macro @native
	public long assign_add(long val) alias lfy operator += ;

	@macro @native
	public long assign_sub(long val) alias lfy operator -= ;

	@macro @native
	public long assign_mul(long val) alias lfy operator *= ;

	@macro @native
	public long assign_div(long val) alias lfy operator /= ;

	@macro @native
	public long assign_mod(long val) alias lfy operator %= ;

	@macro @native
	public static long bit_or(long l1, long l2) alias yfx operator | ;

	@macro @native
	public static long bit_xor(long l1, long l2) alias yfx operator ^ ;

	@macro @native
	public static long bit_and(long l1, long l2) alias yfx operator & ;

	@macro @native
	public static boolean bool_eq(long l1, long l2) alias xfx operator == ;

	@macro @native
	public static boolean bool_neq(long l1, long l2) alias xfx operator != ;

	@macro @native
	public static boolean bool_ge(long l1, long l2) alias xfx operator >= ;

	@macro @native
	public static boolean bool_le(long l1, long l2) alias xfx operator <= ;

	@macro @native
	public static boolean bool_gt(long l1, long l2) alias xfx operator > ;

	@macro @native
	public static boolean bool_lt(long l1, long l2) alias xfx operator < ;

	@macro @native
	public static long left_shift(long val, int shft) alias xfx operator << ;

	@macro @native
	public static long right_shift(long val, int shft) alias xfx operator >> ;

	@macro @native
	public static long unsigned_right_shift(long val, int shft) alias xfx operator >>> ;

	@macro @native
	public static long add(long l1, long l2) alias yfx operator + ;

	@macro @native
	public static long sub(long l1, long l2) alias yfx operator - ;

	@macro @native
	public static long mul(long l1, long l2) alias yfx operator * ;

	@macro @native
	public static long div(long l1, long l2) alias yfx operator / ;

	@macro @native
	public static long mod(long l1, long l2) alias yfx operator % ;

	@macro @native
	public long positive() alias fy operator + ;

	@macro @native
	public long negative() alias fy operator - ;

	@macro @native
	public static long pre_incr(long lval) alias fx operator ++ ;

	@macro @native
	public static long pre_decr(long lval) alias fx operator -- ;

	@macro @native
	public static long bit_not(long val) alias fy operator ~ ;

	@macro @native
	public static long post_incr(long lval) alias xf operator ++ ;

	@macro @native
	public static long post_decr(long lval) alias xf operator -- ;

}

public metatype float extends any {
	@macro @native
	public float assign(float val) alias lfy operator = ;

	@macro @native
	public float assign_add(float val) alias lfy operator += ;

	@macro @native
	public float assign_sub(float val) alias lfy operator -= ;

	@macro @native
	public float assign_mul(float val) alias lfy operator *= ;

	@macro @native
	public float assign_div(float val) alias lfy operator /= ;

	@macro @native
	public float assign_mod(float val) alias lfy operator %= ;

	@macro @native
	public static boolean bool_eq(float f1, float f2) alias xfx operator == ;

	@macro @native
	public static boolean bool_neq(float f1, float f2) alias xfx operator != ;

	@macro @native
	public static boolean bool_ge(float f1, float f2) alias xfx operator >= ;

	@macro @native
	public static boolean bool_le(float f1, float f2) alias xfx operator <= ;

	@macro @native
	public static boolean bool_gt(float f1, float f2) alias xfx operator > ;

	@macro @native
	public static boolean bool_lt(float f1, float f2) alias xfx operator < ;

	@macro @native
	public static float add(float f1, float f2) alias yfx operator + ;

	@macro @native
	public static float sub(float f1, float f2) alias yfx operator - ;

	@macro @native
	public static float mul(float f1, float f2) alias yfx operator * ;

	@macro @native
	public static float div(float f1, float f2) alias yfx operator / ;

	@macro @native
	public static float mod(float f1, float f2) alias yfx operator % ;

	@macro @native
	public float positive() alias fy operator + ;

	@macro @native
	public float negative() alias fy operator - ;

}

public metatype double extends any {
	@macro @native
	public double assign(double val) alias lfy operator = ;

	@macro @native
	public double assign_add(double val) alias lfy operator += ;

	@macro @native
	public double assign_sub(double val) alias lfy operator -= ;

	@macro @native
	public double assign_mul(double val) alias lfy operator *= ;

	@macro @native
	public double assign_div(double val) alias lfy operator /= ;

	@macro @native
	public double assign_mod(double val) alias lfy operator %= ;

	@macro @native
	public static boolean bool_eq(double d1, double d2) alias xfx operator == ;

	@macro @native
	public static boolean bool_neq(double d1, double d2) alias xfx operator != ;

	@macro @native
	public static boolean bool_ge(double d1, double d2) alias xfx operator >= ;

	@macro @native
	public static boolean bool_le(double d1, double d2) alias xfx operator <= ;

	@macro @native
	public static boolean bool_gt(double d1, double d2) alias xfx operator > ;

	@macro @native
	public static boolean bool_lt(double d1, double d2) alias xfx operator < ;

	@macro @native
	public static double add(double d1, double d2) alias yfx operator + ;

	@macro @native
	public static double sub(double d1, double d2) alias yfx operator - ;

	@macro @native
	public static double mul(double d1, double d2) alias yfx operator * ;

	@macro @native
	public static double div(double d1, double d2) alias yfx operator / ;

	@macro @native
	public static double mod(double d1, double d2) alias yfx operator % ;

	@macro @native
	public double positive() alias fy operator + ;

	@macro @native
	public double negative() alias fy operator - ;

}

public metatype _array_<_elem_ extends any> extends Object {
	@macro @native
	public:ro final int length;

	@macro @native
	public _elem_ get(int idx) alias operator(210, xfy, [] );

	@macro @native
	public <R extends _elem_> R set(int idx, R val) alias operator(210, lfy, [] );
}

public metatype GString extends java.lang.String {

	@macro @native
	public static String str_concat_ss(String s1, String s2) alias yfx operator + ;

	@macro @native
	public static String str_concat_as(any s1, String s2) alias yfx operator + ;

	@macro @native
	public static String str_concat_sa(String s1, any s2) alias yfx operator + ;

	@macro @native
	public static String str_assign_add(String s1, any s2) alias lfy operator += ;
}

