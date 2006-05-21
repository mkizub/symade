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

public metatype any {}

public metatype void {}

public metatype boolean extends any {
	@macro @native
	public boolean assign(boolean val) alias lfy operator = ;

	@macro @native
	public boolean bool_or(boolean val) alias yfx operator || ;

	@macro @native
	public boolean bool_and(boolean val) alias yfx operator && ;

	@macro @native
	public boolean bool_eq(boolean val) alias xfx operator == ;

	@macro @native
	public boolean bool_neq(boolean val) alias xfx operator != ;

	@macro @native
	public boolean bool_not() alias fy operator ! ;
}

public metatype char extends any {
	@macro @native
	public char assign(char val) alias lfy operator = ;
}

public metatype byte extends any {
	@macro @native
	public byte assign(byte val) alias lfy operator = ;
}

public metatype short extends any {
	@macro @native
	public short assign(short val) alias lfy operator = ;
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
	public int assign_rem(int val) alias lfy operator %= ;

	@macro @native
	public int bit_or(int val) alias yfx operator | ;

	@macro @native
	public int bit_xor(int val) alias yfx operator ^ ;

	@macro @native
	public int bit_and(int val) alias yfx operator & ;

	@macro @native
	public boolean bool_eq(int val) alias xfx operator == ;

	@macro @native
	public boolean bool_neq(int val) alias xfx operator != ;

	@macro @native
	public boolean bool_ge(int val) alias xfx operator >= ;

	@macro @native
	public boolean bool_le(int val) alias xfx operator <= ;

	@macro @native
	public boolean bool_gt(int val) alias xfx operator > ;

	@macro @native
	public boolean bool_lt(int val) alias xfx operator < ;

	@macro @native
	public int left_shift(int val) alias xfx operator << ;

	@macro @native
	public int right_shift(int val) alias xfx operator >> ;

	@macro @native
	public int unsigned_right_shift(int val) alias xfx operator >>> ;

	@macro @native
	public int add(int val) alias yfx operator + ;

	@macro @native
	public int sub(int val) alias yfx operator - ;

	@macro @native
	public int mul(int val) alias yfx operator * ;

	@macro @native
	public int div(int val) alias yfx operator / ;

	@macro @native
	public int rem(int val) alias yfx operator % ;

	@macro @native
	public int positive() alias fy operator + ;

	@macro @native
	public int negative() alias fy operator - ;

	@macro @native
	public int pre_incr() alias fx operator ++ ;

	@macro @native
	public int pre_decr() alias fx operator -- ;

	@macro @native
	public int bit_not() alias fy operator ~ ;

	@macro @native
	public int post_incr() alias xf operator ++ ;

	@macro @native
	public int post_decr() alias xf operator -- ;

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
	public long assign_rem(long val) alias lfy operator %= ;

	@macro @native
	public long bit_or(long val) alias yfx operator | ;

	@macro @native
	public long bit_xor(long val) alias yfx operator ^ ;

	@macro @native
	public long bit_and(long val) alias yfx operator & ;

	@macro @native
	public boolean bool_eq(long val) alias xfx operator == ;

	@macro @native
	public boolean bool_neq(long val) alias xfx operator != ;

	@macro @native
	public boolean bool_ge(long val) alias xfx operator >= ;

	@macro @native
	public boolean bool_le(long val) alias xfx operator <= ;

	@macro @native
	public boolean bool_gt(long val) alias xfx operator > ;

	@macro @native
	public boolean bool_lt(long val) alias xfx operator < ;

	@macro @native
	public long left_shift(int val) alias xfx operator << ;

	@macro @native
	public long right_shift(int val) alias xfx operator >> ;

	@macro @native
	public long unsigned_right_shift(int val) alias xfx operator >>> ;

	@macro @native
	public long add(long val) alias yfx operator + ;

	@macro @native
	public long sub(long val) alias yfx operator - ;

	@macro @native
	public long mul(long val) alias yfx operator * ;

	@macro @native
	public long div(int val) alias yfx operator / ;

	@macro @native
	public long rem(long val) alias yfx operator % ;

	@macro @native
	public long positive() alias fy operator + ;

	@macro @native
	public long negative() alias fy operator - ;

	@macro @native
	public long pre_incr() alias fx operator ++ ;

	@macro @native
	public long pre_decr() alias fx operator -- ;

	@macro @native
	public long bit_not() alias fy operator ~ ;

	@macro @native
	public long post_incr() alias xf operator ++ ;

	@macro @native
	public long post_decr() alias xf operator -- ;

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
	public float assign_rem(float val) alias lfy operator %= ;

	@macro @native
	public boolean bool_eq(float val) alias xfx operator == ;

	@macro @native
	public boolean bool_neq(float val) alias xfx operator != ;

	@macro @native
	public boolean bool_ge(float val) alias xfx operator >= ;

	@macro @native
	public boolean bool_le(float val) alias xfx operator <= ;

	@macro @native
	public boolean bool_gt(float val) alias xfx operator > ;

	@macro @native
	public boolean bool_lt(float val) alias xfx operator < ;

	@macro @native
	public float add(float val) alias yfx operator + ;

	@macro @native
	public float sub(float val) alias yfx operator - ;

	@macro @native
	public float mul(float val) alias yfx operator * ;

	@macro @native
	public float div(float val) alias yfx operator / ;

	@macro @native
	public float rem(float val) alias yfx operator % ;

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
	public double assign_rem(double val) alias lfy operator %= ;

	@macro @native
	public boolean bool_eq(double val) alias xfx operator == ;

	@macro @native
	public boolean bool_neq(double val) alias xfx operator != ;

	@macro @native
	public boolean bool_ge(double val) alias xfx operator >= ;

	@macro @native
	public boolean bool_le(double val) alias xfx operator <= ;

	@macro @native
	public boolean bool_gt(double val) alias xfx operator > ;

	@macro @native
	public boolean bool_lt(double val) alias xfx operator < ;

	@macro @native
	public double add(double val) alias yfx operator + ;

	@macro @native
	public double sub(double val) alias yfx operator - ;

	@macro @native
	public double mul(double val) alias yfx operator * ;

	@macro @native
	public double div(double val) alias yfx operator / ;

	@macro @native
	public double rem(double val) alias yfx operator % ;

}

public metatype _array_<_elem_ extends any> extends Object {
	@macro @native
	public:ro final int length;

	@macro @native
	public _elem_ get(int idx) alias operator(210, xfy, [] );

	@macro @native
	public _elem_ set(int idx, _elem_ val) alias operator(210, lfy, [] );
}

