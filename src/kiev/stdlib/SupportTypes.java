/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev library.
 
 The Kiev library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public License as
 published by the Free Software Foundation.

 The Kiev library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU Library General Public
 License along with the Kiev compiler; see the file License.  If not,
 write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/
  
package kiev.stdlib;

/**
 * @author Maxim Kizub
 * @version $Revision: 1.2 $
 *
 */

public abstract class Cell {
	public boolean	to_boolean()	{ throw new Error("Can't convert to boolean"); }
	public byte		to_byte()		{ throw new Error("Can't convert to byte"); }
	public char		to_char()		{ throw new Error("Can't convert to char"); }
	public short	to_short()		{ throw new Error("Can't convert to short"); }
	public int		to_int()		{ throw new Error("Can't convert to int"); }
	public long		to_long()		{ throw new Error("Can't convert to long"); }
	public float	to_float()		{ throw new Error("Can't convert to float"); }
	public double	to_double()		{ throw new Error("Can't convert to double"); }
	public Object	toObject()		{ throw new Error("Can't convert to Object"); }
}

public class Cell_boolean extends Cell {
	public boolean $val;
	
	public Cell_boolean(boolean $val){ this.$val = $val; }
	public boolean	to_boolean()	{ return $val; }
	public byte		to_byte()		{ return $val? (byte)1 : (byte)0; }
	public char		to_char()		{ return $val? 'T' : 'F'; }
	public short	to_short()		{ return $val? (short)1 : (short)0; }
	public int		to_int()		{ return $val? 1 : 0; }
	public long		to_long()		{ return $val? 1L : 0L; }
	public float	to_float()		{ return $val? 1.f : 0.f; }
	public double	to_double()		{ return $val? 1.d : 0.d; }
	public Object	toObject()		{ return $val? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE; }
	
	public static boolean opNot(boolean b) { return !b; }
	public static boolean opBoolOr(boolean b1, boolean b2) { return b1 || b2; }
	public static boolean opBoolAnd(boolean b1, boolean b2) { return b1 && b2; }
}

public abstract class Cell_number extends Cell {
	public static int opAdd(int i1, int i2) { return i1 + i2; }
	public static long opAdd(long i1, long i2) { return i1 + i2; }
	public static float opAdd(float i1, float i2) { return i1 + i2; }
	public static double opAdd(double i1, double i2) { return i1 + i2; }

	public static int opSub(int i1, int i2) { return i1 - i2; }
	public static long opSub(long i1, long i2) { return i1 - i2; }
	public static float opSub(float i1, float i2) { return i1 - i2; }
	public static double opSub(double i1, double i2) { return i1 - i2; }

	public static int opMul(int i1, int i2) { return i1 * i2; }
	public static long opMul(long i1, long i2) { return i1 * i2; }
	public static float opMul(float i1, float i2) { return i1 * i2; }
	public static double opMul(double i1, double i2) { return i1 * i2; }

	public static int opDiv(int i1, int i2) { return i1 / i2; }
	public static long opDiv(long i1, long i2) { return i1 / i2; }
	public static float opDiv(float i1, float i2) { return i1 / i2; }
	public static double opDiv(double i1, double i2) { return i1 / i2; }

	public static int opMod(int i1, int i2) { return i1 % i2; }
	public static long opMod(long i1, long i2) { return i1 % i2; }
	public static float opMod(float i1, float i2) { return i1 % i2; }
	public static double opMod(double i1, double i2) { return i1 % i2; }

	public static int opNeg(int i) { return -i; }
	public static long opNeg(long i) { return -i; }
	public static float opNeg(float i) { return -i; }
	public static double opNeg(double i) { return -i; }
}

public abstract class Cell_integer extends Cell_number {
	public static int opOr(int i1, int i2) { return i1 | i2; }
	public static long opOr(long i1, long i2) { return i1 | i2; }

	public static int opXor(int i1, int i2) { return i1 ^ i2; }
	public static long opXor(long i1, long i2) { return i1 ^ i2; }

	public static int opAnd(int i1, int i2) { return i1 & i2; }
	public static long opAnd(long i1, long i2) { return i1 & i2; }
}

public class Cell_byte extends Cell_integer {
	public byte $val;

	public Cell_byte(byte $val)		{ this.$val = $val; }
	public boolean	to_boolean()	{ return $val==0; }
	public byte		to_byte()		{ return (byte)$val; }
	public char		to_char()		{ return (char)$val; }
	public short	to_short()		{ return (short)$val; }
	public int		to_int()		{ return (int)$val; }
	public long		to_long()		{ return (long)$val; }
	public float	to_float()		{ return (float)$val; }
	public double	to_double()		{ return (double)$val; }
	public Object	toObject()		{ return new java.lang.Byte($val); }
}
		
public class Cell_char extends Cell_integer {
	public char $val;

	public Cell_char(char $val)		{ this.$val = $val; }
	public boolean	to_boolean()	{ return $val=='\0'; }
	public byte		to_byte()		{ return (byte)$val; }
	public char		to_char()		{ return (char)$val; }
	public short	to_short()		{ return (short)$val; }
	public int		to_int()		{ return (int)$val; }
	public long		to_long()		{ return (long)$val; }
	public float	to_float()		{ return (float)$val; }
	public double	to_double()		{ return (double)$val; }
	public Object	toObject()		{ return new java.lang.Character($val); }
}
		
public class Cell_short extends Cell_integer {
	public short $val;

	public Cell_short(short $val)	{ this.$val = $val; }
	public boolean	to_boolean()	{ return $val=='\0'; }
	public byte		to_byte()		{ return (byte)$val; }
	public char		to_char()		{ return (char)$val; }
	public short	to_short()		{ return (short)$val; }
	public int		to_int()		{ return (int)$val; }
	public long		to_long()		{ return (long)$val; }
	public float	to_float()		{ return (float)$val; }
	public double	to_double()		{ return (double)$val; }
	public Object	toObject()		{ return new java.lang.Short($val); }
}
		
public class Cell_int extends Cell_integer {
	public int $val;

	public Cell_int(int $val)		{ this.$val = $val; }
	public boolean	to_boolean()	{ return $val=='\0'; }
	public byte		to_byte()		{ return (byte)$val; }
	public char		to_char()		{ return (char)$val; }
	public short	to_short()		{ return (short)$val; }
	public int		to_int()		{ return (int)$val; }
	public long		to_long()		{ return (long)$val; }
	public float	to_float()		{ return (float)$val; }
	public double	to_double()		{ return (double)$val; }
	public Object	toObject()		{ return new java.lang.Integer($val); }
}
		
public class Cell_long extends Cell_integer {
	public long $val;

	public Cell_long(long $val)		{ this.$val = $val; }
	public boolean	to_boolean()	{ return $val=='\0'; }
	public byte		to_byte()		{ return (byte)$val; }
	public char		to_char()		{ return (char)$val; }
	public short	to_short()		{ return (short)$val; }
	public int		to_int()		{ return (int)$val; }
	public long		to_long()		{ return (long)$val; }
	public float	to_float()		{ return (float)$val; }
	public double	to_double()		{ return (double)$val; }
	public Object	toObject()		{ return new java.lang.Long($val); }
}
		
public class Cell_float extends Cell_number {
	public float $val;

	public Cell_float(float $val)	{ this.$val = $val; }
	public boolean	to_boolean()	{ return $val=='\0'; }
	public byte		to_byte()		{ return (byte)$val; }
	public char		to_char()		{ return (char)$val; }
	public short	to_short()		{ return (short)$val; }
	public int		to_int()		{ return (int)$val; }
	public long		to_long()		{ return (long)$val; }
	public float	to_float()		{ return (float)$val; }
	public double	to_double()		{ return (double)$val; }
	public Object	toObject()		{ return new java.lang.Float($val); }
}
		
public class Cell_double extends Cell_number {
	public double $val;

	public Cell_double(double $val)	{ this.$val = $val; }
	public boolean	to_boolean()	{ return $val=='\0'; }
	public byte		to_byte()		{ return (byte)$val; }
	public char		to_char()		{ return (char)$val; }
	public short	to_short()		{ return (short)$val; }
	public int		to_int()		{ return (int)$val; }
	public long		to_long()		{ return (long)$val; }
	public float	to_float()		{ return (float)$val; }
	public double	to_double()		{ return (double)$val; }
	public Object	toObject()		{ return new java.lang.Double($val); }
}
		
public class Cell_void extends Cell {
	public Cell_void()				{}
	public boolean	to_boolean()	{ return false; }
	public byte		to_byte()		{ return (byte)0; }
	public char		to_char()		{ return '\0'; }
	public short	to_short()		{ return (short)0; }
	public int		to_int()		{ return 0; }
	public long		to_long()		{ return 0L; }
	public float	to_float()		{ return 0.f; }
	public double	to_double()		{ return 0.d; }
	public Object	toObject()		{ return null; }
}
		
public class Cell_Object extends Cell {
	public Object $val;
	public Cell_Object(Object $val)	{ this.$val = $val; }
}
		
public class Array<elem> extends java.lang.Object {
	public final int length = 0;
}

public abstract class Null extends java.lang.Object {
}

public abstract class closure implements Cloneable {

	public static Object[]	$noargs = new Object[0];

	public Object[]		$args;
	public int			max$args;	// = 0
	public int			top$arg;	// = 0
	
	public closure(int max$args) {
		this.max$args = max$args;
		$args = max$args==0? $noargs: new Object[max$args];
	}
	
	protected void throwException(String msg) {
		throw new RuntimeException("This closure "+getClass()+" "+msg);
	}
	
	public void		call_void()		{ throwException("does not returns void"); }
	public rule		call_rule()		{ throwException("is not a rule") }
	public boolean	call_boolean()	{ throwException("does not returns boolean"); return false; }
	public byte		call_byte()		{ throwException("does not returns byte"); return (byte)0; }
	public char		call_char()		{ throwException("does not returns char"); return '\000'; }
	public short	call_short()	{ throwException("does not returns short"); return (short)0; }
	public int		call_int()		{ throwException("does not returns int"); return 0; }
	public long		call_long()		{ throwException("does not returns long"); return 0L; }
	public float	call_float()	{ throwException("does not returns float"); return 0.F; }
	public double	call_double()	{ throwException("does not returns double"); return 0.D; }
	public Object	call_Object()	{ throwException("does not returns Object"); return null; }
	
	public Object clone() {
		closure cl;
		try {
			cl = (closure)super.clone();
			if( cl.$args.length != 0 )
				cl.$args = (Object[])$args.clone();
		} catch( java.lang.CloneNotSupportedException e) {
			throw new RuntimeException("internal error: "+e.getMessage());
		}
		return cl;
	}
	
	public closure addArg(boolean arg) {
		try {
			$args[top$arg++] = new Cell_boolean(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(byte arg) {
		try {
			$args[top$arg++] = new Cell_byte(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(char arg) {
		try {
			$args[top$arg++] = new Cell_char(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(short arg) {
		try {
			$args[top$arg++] = new Cell_short(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(int arg) {
		try {
			$args[top$arg++] = new Cell_int(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(long arg) {
		try {
			$args[top$arg++] = new Cell_long(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(float arg) {
		try {
			$args[top$arg++] = new Cell_float(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(double arg) {
		try {
			$args[top$arg++] = new Cell_double(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(Object arg) {
		try {
			$args[top$arg++] = arg;
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
}
