package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.ir.java.RConstExpr;
import kiev.be.java.JConstExpr;
import kiev.be.java.JConstBoolExpr;
import kiev.be.java.JConstNullExpr;
import kiev.be.java.JConstByteExpr;
import kiev.be.java.JConstShortExpr;
import kiev.be.java.JConstIntExpr;
import kiev.be.java.JConstLongExpr;
import kiev.be.java.JConstCharExpr;
import kiev.be.java.JConstFloatExpr;
import kiev.be.java.JConstDoubleExpr;
import kiev.be.java.JConstStringExpr;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */


@nodeset
public final class ConstBoolExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstBoolExpr;
	@virtual typedef VView = ConstBoolExprView;
	@virtual typedef JView = JConstBoolExpr;

	@att public boolean value;

	@nodeview
	public static view ConstBoolExprView of ConstBoolExpr extends ConstExprView {
		public boolean	value;
		public Type		getType()			{ return Type.tpBoolean; }

		public Object	getConstValue()		{ return value ? Boolean.TRUE: Boolean.FALSE; }
		
		public boolean valueEquals(Object o) {
			if (o instanceof ConstBoolExpr)
				return o.value == this.value;
			return false;
		}
	}
	
	public ConstBoolExpr() {}
	public ConstBoolExpr(boolean value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value); }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(String.valueOf(value)).space();
	}
}

@nodeset
public final class ConstNullExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstNullExpr;
	@virtual typedef VView = ConstNullExprView;
	@virtual typedef JView = JConstNullExpr;

	@nodeview
	public static view ConstNullExprView of ConstNullExpr extends ConstExprView {
		public Type		getType()			{ return Type.tpNull; }

		public Object	getConstValue()		{ return null; }

		public boolean valueEquals(Object o) {
			if (o instanceof ConstNullExpr)
				return true;
			return false;
		}
	}
	
	public ConstNullExpr() {}

	public String	toString()			{ return "null"; }
}

@nodeset
public final class ConstByteExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstByteExpr;
	@virtual typedef VView = ConstByteExprView;
	@virtual typedef JView = JConstByteExpr;

	@att public byte value;

	@nodeview
	public static view ConstByteExprView of ConstByteExpr extends ConstExprView {
		public byte		value;
		public Type		getType()			{ return Type.tpByte; }

		public Object	getConstValue()		{ return Byte.valueOf(value); }

		public boolean valueEquals(Object o) {
			if (o instanceof ConstByteExpr)
				return o.value == this.value;
			return false;
		}
	}
	
	public ConstByteExpr() {}
	public ConstByteExpr(byte value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
}

@nodeset
public final class ConstShortExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstShortExpr;
	@virtual typedef VView = ConstShortExprView;
	@virtual typedef JView = JConstShortExpr;

	@att public short value;

	@nodeview
	public static view ConstShortExprView of ConstShortExpr extends ConstExprView {
		public short		value;
		public Type		getType()			{ return Type.tpShort; }

		public Object	getConstValue()		{ return Short.valueOf(value); }

		public boolean valueEquals(Object o) {
			if (o instanceof ConstShortExpr)
				return o.value == this.value;
			return false;
		}
	}
	
	public ConstShortExpr() {}
	public ConstShortExpr(short value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
}

@nodeset
public final class ConstIntExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstIntExpr;
	@virtual typedef VView = ConstIntExprView;
	@virtual typedef JView = JConstIntExpr;

	@att public int value;

	@nodeview
	public static view ConstIntExprView of ConstIntExpr extends ConstExprView {
		public int		value;
		public Type		getType()			{ return Type.tpInt; }

		public Object	getConstValue()		{ return Integer.valueOf(value); }

		public boolean valueEquals(Object o) {
			if (o instanceof ConstIntExpr)
				return o.value == this.value;
			return false;
		}
	}
	
	public ConstIntExpr() {}
	public ConstIntExpr(int value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
}

@nodeset
public final class ConstLongExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstLongExpr;
	@virtual typedef VView = ConstLongExprView;
	@virtual typedef JView = JConstLongExpr;

	@att public long value;

	@nodeview
	public static view ConstLongExprView of ConstLongExpr extends ConstExprView {
		public long		value;
		public Type		getType()			{ return Type.tpLong; }

		public Object	getConstValue()		{ return Long.valueOf(value); }

		public boolean valueEquals(Object o) {
			if (o instanceof ConstLongExpr)
				return o.value == this.value;
			return false;
		}
	}
	
	public ConstLongExpr() {}
	public ConstLongExpr(long value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value)+"L"; }
}

@nodeset
public final class ConstCharExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstCharExpr;
	@virtual typedef VView = ConstCharExprView;
	@virtual typedef JView = JConstCharExpr;

	@att public char value;

	@nodeview
	public static view ConstCharExprView of ConstCharExpr extends ConstExprView {
		public char		value;
		public Type		getType()			{ return Type.tpChar; }

		public Object	getConstValue()		{ return Character.valueOf(value); }

		public boolean valueEquals(Object o) {
			if (o instanceof ConstCharExpr)
				return o.value == this.value;
			return false;
		}
	}
	
	public ConstCharExpr() {}
	public ConstCharExpr(char value) { this.value = value; }

	public String	toString()			{ return "'"+Convert.escape(value)+"'"; }
}


@nodeset
public final class ConstFloatExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstFloatExpr;
	@virtual typedef VView = ConstFloatExprView;
	@virtual typedef JView = JConstFloatExpr;

	@att public float value;

	@nodeview
	public static view ConstFloatExprView of ConstFloatExpr extends ConstExprView {
		public float		value;
		public Type		getType()			{ return Type.tpFloat; }

		public Object	getConstValue()		{ return Float.valueOf(value); }

		public boolean valueEquals(Object o) {
			if (o instanceof ConstFloatExpr)
				return o.value == this.value;
			return false;
		}
	}
	
	public ConstFloatExpr() {}
	public ConstFloatExpr(float value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value)+"F"; }
}


@nodeset
public final class ConstDoubleExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstDoubleExpr;
	@virtual typedef VView = ConstDoubleExprView;
	@virtual typedef JView = JConstDoubleExpr;

	@att public double value;

	@nodeview
	public static view ConstDoubleExprView of ConstDoubleExpr extends ConstExprView {
		public double		value;
		public Type		getType()			{ return Type.tpDouble; }

		public Object	getConstValue()		{ return Double.valueOf(value); }

		public boolean valueEquals(Object o) {
			if (o instanceof ConstDoubleExpr)
				return o.value == this.value;
			return false;
		}
	}
	
	public ConstDoubleExpr() {}
	public ConstDoubleExpr(double value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value)+"D"; }
}

@nodeset
public final class ConstStringExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstStringExpr;
	@virtual typedef VView = ConstStringExprView;
	@virtual typedef JView = JConstStringExpr;

	@att public KString value;

	@nodeview
	public static view ConstStringExprView of ConstStringExpr extends ConstExprView {
		public KString		value;
		public Type		getType()			{ return Type.tpString; }

		public Object	getConstValue()		{ return value; }

		public boolean valueEquals(Object o) {
			if (o instanceof ConstStringExpr)
				return o.value == this.value;
			return false;
		}
	}
	
	public ConstStringExpr() {}
	public ConstStringExpr(KString value) { this.value = value; }

	public String	toString()			{ return '\"'+value.toString()+'\"'; }
}


@nodeset
public abstract class ConstExpr extends ENode {

	@virtual typedef This  = ConstExpr;
	@virtual typedef VView = ConstExprView;
	@virtual typedef JView = JConstExpr;
	@virtual typedef RView = RConstExpr;

	@nodeview
	public abstract static view ConstExprView of ConstExpr extends ENodeView {

		public int		getPriority() { return 255; }

		public abstract Object getConstValue();

		public boolean	isConstantExpr() { return true; }

		public final boolean mainResolveIn() {
			// already fully resolved
			setResolved(true);
			return false;
		}
	}

	public ConstExpr() {
		setResolved(true);
	}
	
	public final void resolve(Type reqType) {
		((RView)this).resolve(reqType);
	}

	public Dumper	toJava(Dumper dmp) {
		Object value = getConstValue();
		if( value == null ) dmp.space().append("null").space();
		else if( value instanceof Number ) {
			if( value instanceof Long ) dmp.append(value).append('L');
			else if( value instanceof Float ) dmp.append(value).append('F');
			else if( value instanceof Double ) dmp.append(value).append('D');
			else dmp.append(value);
		}
		else if( value instanceof KString ) {
			dmp.append('\"');
			byte[] val = Convert.string2source(value.toString());
			dmp.append(new String(val,0));
			dmp.append('\"');
		}
		else if( value instanceof java.lang.Boolean )
			if( ((Boolean)value).booleanValue() )
				dmp.space().append("true").space();
			else
				dmp.space().append("false").space();
		else if( value instanceof java.lang.Character ) {
			char ch = ((java.lang.Character)value).charValue();
			return dmp.append('\'').append(Convert.escape(ch)).append('\'');
		}
		else throw new RuntimeException("Internal error: unknown type of constant "+value.getClass());
		return dmp;
	}

	public static ConstExpr fromConst(Object o) {
		if (o == null)              return new ConstNullExpr   ();
		if (o instanceof Integer)   return new ConstIntExpr    (((Integer)  o).intValue());
		if (o instanceof KString)   return new ConstStringExpr (((KString)  o));
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

    public static KString source2ascii(String source) {
    	KStringBuffer ksb = new KStringBuffer(source.length()*2);
        int i = 0;
        int len = source.length();
        while (i < len) {
            if (source.charAt(i) == '\\' && i + 1 < len) {
                i++;
                switch (source.charAt(i)) {
                case 'n':	ksb.append((byte)'\n'); i++; continue;
                case 't':	ksb.append((byte)'\t'); i++; continue;
                case 'b':	ksb.append((byte)'\b'); i++; continue;
                case 'r':	ksb.append((byte)'\r'); i++; continue;
                case 'f':	ksb.append((byte)'\f'); i++; continue;
                case '0': case '1': case '2': case '3': case '4': case '5':
                case '6': case '7': case '8': case '9':
                	{
                	int code = 0;
                	for(int k=0; k < 3 && i < len && source.charAt(i) >='0' && source.charAt(i) <='8'; k++, i++) {
                		code = code*8 + (source.charAt(i) - '0');
                	}
                    ksb.append((byte)code);
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
	                        ksb.append((char)code);
                            i = i + 5;
                            continue;
                        }
                    }
                }
            }
            ksb.append(source.charAt(i++));
        }
        return ksb.toKString();
    }

}


