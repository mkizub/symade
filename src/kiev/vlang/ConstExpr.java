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

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RConstExpr;
import kiev.be.java15.JConstExpr;
import kiev.be.java15.JConstBoolExpr;
import kiev.be.java15.JConstNullExpr;
import kiev.be.java15.JConstByteExpr;
import kiev.be.java15.JConstShortExpr;
import kiev.be.java15.JConstIntExpr;
import kiev.be.java15.JConstLongExpr;
import kiev.be.java15.JConstCharExpr;
import kiev.be.java15.JConstFloatExpr;
import kiev.be.java15.JConstDoubleExpr;
import kiev.be.java15.JConstStringExpr;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

enum IntRadix {
	RADIX_DEC,
	RADIX_HEX,
	RADIX_OCT
}

@node
public final class ConstBoolExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstBoolExpr;
	@virtual typedef JView = JConstBoolExpr;

	@att public boolean value;

	public ConstBoolExpr() {}
	public ConstBoolExpr(boolean value) { this.value = value; }
	
	public Type		getType()			{ return Type.tpBoolean; }

	public Object	getConstValue()		{ return value ? Boolean.TRUE: Boolean.FALSE; }
	
	public boolean valueEquals(Object o) {
		if (o instanceof ConstBoolExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString() { return String.valueOf(value); }
}

@node
public final class ConstNullExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstNullExpr;
	@virtual typedef JView = JConstNullExpr;

	public ConstNullExpr() {}

	public Type		getType()			{ return Type.tpNull; }

	public Object	getConstValue()		{ return null; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstNullExpr)
			return true;
		return false;
	}

	public String	toString()			{ return "null"; }
}

@node
public final class ConstByteExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstByteExpr;
	@virtual typedef JView = JConstByteExpr;

	@att public byte		value;
	@att public IntRadix	radix;

	public ConstByteExpr() {}
	public ConstByteExpr(byte value) { this.value = value; }

	public Type		getType()			{ return Type.tpByte; }

	public Object	getConstValue()		{ return Byte.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstByteExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString() {
		if (radix == IntRadix.RADIX_OCT)
			return "0"+Integer.toOctalString(value);
		if (radix == IntRadix.RADIX_HEX)
			return "0x"+Integer.toHexString(value);
		return Integer.toString(value);
	}
}

@node
public final class ConstShortExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstShortExpr;
	@virtual typedef JView = JConstShortExpr;

	@att public short		value;
	@att public IntRadix	radix;

	public ConstShortExpr() {}
	public ConstShortExpr(short value) { this.value = value; }

	public Type		getType()			{ return Type.tpShort; }

	public Object	getConstValue()		{ return Short.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstShortExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString() {
		if (radix == IntRadix.RADIX_OCT)
			return "0"+Integer.toOctalString(value);
		if (radix == IntRadix.RADIX_HEX)
			return "0x"+Integer.toHexString(value);
		return Integer.toString(value);
	}
}

@node
public final class ConstIntExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstIntExpr;
	@virtual typedef JView = JConstIntExpr;

	@att public int			value;
	@att public IntRadix	radix;

	public ConstIntExpr() {}
	public ConstIntExpr(int value) { this.value = value; }

	public Type		getType()			{ return Type.tpInt; }

	public Object	getConstValue()		{ return Integer.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstIntExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString() {
		if (radix == IntRadix.RADIX_HEX || value == Integer.MIN_VALUE)
			return "0x"+Integer.toHexString(value);
		if (radix == IntRadix.RADIX_OCT)
			return "0"+Integer.toOctalString(value);
		return Integer.toString(value);
	}
}

@node
public final class ConstLongExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstLongExpr;
	@virtual typedef JView = JConstLongExpr;

	@att public long		value;
	@att public IntRadix	radix;

	public ConstLongExpr() {}
	public ConstLongExpr(long value) { this.value = value; }

	public Type		getType()			{ return Type.tpLong; }

	public Object	getConstValue()		{ return Long.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstLongExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString() {
		if (radix == IntRadix.RADIX_HEX || value == Long.MIN_VALUE)
			return "0x"+Long.toHexString(value)+"L";
		if (radix == IntRadix.RADIX_OCT)
			return "0"+Long.toOctalString(value)+"L";
		return Long.toString(value)+"L";
	}
}

@node
public final class ConstCharExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstCharExpr;
	@virtual typedef JView = JConstCharExpr;

	@att public char value;

	public ConstCharExpr() {}
	public ConstCharExpr(char value) { this.value = value; }

	public Type		getType()			{ return Type.tpChar; }

	public Object	getConstValue()		{ return Character.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstCharExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString()			{ return "'"+Convert.escape(value)+"'"; }
}


@node
public final class ConstFloatExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstFloatExpr;
	@virtual typedef JView = JConstFloatExpr;

	@att public float value;

	public ConstFloatExpr() {}
	public ConstFloatExpr(float value) { this.value = value; }

	public Type		getType()			{ return Type.tpFloat; }

	public Object	getConstValue()		{ return Float.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstFloatExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString()			{ return String.valueOf(value)+"F"; }
}


@node
public final class ConstDoubleExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstDoubleExpr;
	@virtual typedef JView = JConstDoubleExpr;

	@att public double value;

	public ConstDoubleExpr() {}
	public ConstDoubleExpr(double value) { this.value = value; }

	public Type		getType()			{ return Type.tpDouble; }

	public Object	getConstValue()		{ return Double.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstDoubleExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString()			{ return String.valueOf(value)+"D"; }
}

@node
public final class ConstStringExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	public static final ConstStringExpr[] emptyArray = new ConstStringExpr[0];
	
	@virtual typedef This  = ConstStringExpr;
	@virtual typedef JView = JConstStringExpr;

	@att public String value;

	public ConstStringExpr() {}
	public ConstStringExpr(String value) { this.value = value; }

	public Type		getType()			{ return Type.tpString; }

	public Object	getConstValue()		{ return value; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstStringExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString()			{ return '\"'+value.toString()+'\"'; }
}


@node
public abstract class ConstExpr extends ENode {

	@virtual typedef This  ≤ ConstExpr;
	@virtual typedef JView ≤ JConstExpr;
	@virtual typedef RView = RConstExpr;

	public ConstExpr() {
		setResolved(true);
	}

	public int		getPriority() { return 255; }

	public abstract Object getConstValue();

	public boolean	isConstantExpr() { return true; }

	public final boolean mainResolveIn() {
		// already fully resolved
		setResolved(true);
		return false;
	}

	public static ConstExpr fromConst(Object o) {
		if (o == null)              return new ConstNullExpr   ();
		if (o instanceof Integer)   return new ConstIntExpr    (((Integer)  o).intValue());
		if (o instanceof String)    return new ConstStringExpr (((String)   o));
		if (o instanceof Byte)      return new ConstByteExpr   (((Byte)     o).byteValue());
		if (o instanceof Short)     return new ConstShortExpr  (((Short)    o).shortValue());
		if (o instanceof Long)      return new ConstLongExpr   (((Long)     o).longValue());
		if (o instanceof Character) return new ConstCharExpr   (((Character)o).charValue());
		if (o instanceof Boolean)   return new ConstBoolExpr   (((Boolean)  o).booleanValue());
		if (o instanceof Float)     return new ConstFloatExpr  (((Float)    o).floatValue());
		if (o instanceof Double)    return new ConstDoubleExpr (((Double)   o).doubleValue());
		throw new RuntimeException("Bad constant object "+o+" ("+o.getClass()+")");
	}
	
	public static ConstExpr fromSource(Token t) throws ParseException {
		ConstExpr ce = null;
		try
		{
			switch(t.kind) {
			case ParserConstants.INTEGER_LITERAL:
			{
				String image;
				int radix;
				if( t.image.startsWith("0x") || t.image.startsWith("0X") ) { image = t.image.substring(2); radix = 16; }
				else if( t.image.startsWith("0") && t.image.length() > 1 ) { image = t.image.substring(1); radix = 8; }
				else { image = t.image; radix = 10; }
				long i = ConstExpr.parseLong(image,radix);
				ce = new ConstIntExpr((int)i);
				switch (radix) {
				case 16: ce.radix = IntRadix.RADIX_HEX; break;
				case  8: ce.radix = IntRadix.RADIX_OCT; break;
				default: ce.radix = IntRadix.RADIX_DEC; break;
				}
				break;
			}
			case ParserConstants.LONG_INTEGER_LITERAL:
			{
				String image;
				int radix;
				if( t.image.startsWith("0x") || t.image.startsWith("0X") ) { image = t.image.substring(2,t.image.length()-1); radix = 16; }
				else if( t.image.startsWith("0") && !t.image.equals("0") && !t.image.equals("0L") ) { image = t.image.substring(1,t.image.length()-1); radix = 8; }
				else { image = t.image.substring(0,t.image.length()-1); radix = 10; }
				long l = ConstExpr.parseLong(image,radix);
				ce = new ConstLongExpr(l);
				switch (radix) {
				case 16: ce.radix = IntRadix.RADIX_HEX; break;
				case  8: ce.radix = IntRadix.RADIX_OCT; break;
				default: ce.radix = IntRadix.RADIX_DEC; break;
				}
				break;
			}
			case ParserConstants.FLOATING_POINT_LITERAL:
			{
				String image;
				if( t.image.endsWith("f") || t.image.endsWith("F") ) image = t.image.substring(0,t.image.length()-1);
				else image = t.image;
				float f = Float.valueOf(image).floatValue();
				ce = new ConstFloatExpr(f);
				break;
			}
			case ParserConstants.DOUBLE_POINT_LITERAL:
			{
				String image;
				if( t.image.endsWith("d") || t.image.endsWith("D") ) image = t.image.substring(0,t.image.length()-1);
				else image = t.image;
				double d = Double.valueOf(t.image).doubleValue();
				ce = new ConstDoubleExpr(d);
				break;
			}
			case ParserConstants.CHARACTER_LITERAL:
			{
				char c;
				if( t.image.length() == 3 )
					c = t.image.charAt(1);
				else
					c = source2ascii(t.image.substring(1,t.image.length()-1)).charAt(0);
				ce = new ConstCharExpr(c);
				break;
			}
			case ParserConstants.STRING_LITERAL:
				ce = new ConstStringExpr(source2ascii(t.image.substring(1,t.image.length()-1)));
				break;
			case ParserConstants.TRUE:
				ce = new ConstBoolExpr(true);
				break;
			case ParserConstants.FALSE:
				ce = new ConstBoolExpr(false);
				break;
			case ParserConstants.NULL:
				ce = new ConstNullExpr();
				break;
			}
		} catch( NumberFormatException e ) {
			throw new ParseException(t.image);
		}
		if (ce == null) {
			Kiev.reportParserError(t.getPos(), "Unknown term "+t.image);
			ce = new ConstNullExpr();
		}
		ce.pos = t.getPos();
		return ce;
	}

    public static long parseLong(String s, int radix)
              throws NumberFormatException
    {
        if (s == null)
            throw new NumberFormatException("null");
		long result = 0;
		boolean negative = false;
		int i = 0, max = s.length();
		long limit;
		long multmin;
		int digit;
	
		if (max > 0) {
			if (s.charAt(0) == '-') {
				negative = true;
				i++;
			}
			limit = Long.MIN_VALUE;
			multmin = limit / radix;
			if (i < max) {
				digit = Character.digit(s.charAt(i++),radix);
				if (digit < 0)
					throw new NumberFormatException(s);
				else
					result = -digit;
			}
			while (i < max) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(s.charAt(i++),radix);
				if (digit < 0)
					throw new NumberFormatException(s);
				result *= radix;
				result -= digit;
			}
		} else {
			throw new NumberFormatException(s);
		}
		if (negative) {
			if (i > 1)
				return result;
			else
				throw new NumberFormatException(s);
		} else {
			return -result;
		}
    }

    public static String source2ascii(String source) {
    	StringBuffer sb = new StringBuffer(source.length());
        int i = 0;
        int len = source.length();
        while (i < len) {
            if (source.charAt(i) == '\\' && i + 1 < len) {
                i++;
                switch (source.charAt(i)) {
                case 'n':	sb.append('\n'); i++; continue;
                case 't':	sb.append('\t'); i++; continue;
                case 'b':	sb.append('\b'); i++; continue;
                case 'r':	sb.append('\r'); i++; continue;
                case 'f':	sb.append('\f'); i++; continue;
                case '0': case '1': case '2': case '3': case '4': case '5':
                case '6': case '7': case '8': case '9':
                	{
                	int code = 0;
                	for(int k=0; k < 3 && i < len && source.charAt(i) >='0' && source.charAt(i) <='8'; k++, i++) {
                		code = code*8 + (source.charAt(i) - '0');
                	}
                    sb.append((char)code);
                    continue;
                	}
                case 'u':
                    if (i + 4 < len) {
                        int code = 0;
                        int k = 1;
                        int d = 0;
                        while (k <= 4 && d >= 0) {
                            d = Convert.digit2int((byte)source.charAt(i+k), 16);
                            code = code * 16 + d;
                            k++;
                        }
                        if (d >= 0) {
	                        sb.append((char)code);
                            i = i + 5;
                            continue;
                        }
                    }
                }
            }
            sb.append(source.charAt(i++));
        }
        return sb.toString();
    }

}


