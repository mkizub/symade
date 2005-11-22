/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import kiev.vlang.Instr.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */


@node
@dflow(out="this:in")
public final class ConstBoolExpr extends ConstExpr implements IBoolExpr {
	public boolean value;

	public ConstBoolExpr() {}
	public ConstBoolExpr(boolean value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
	public Object	getConstValue()		{ return value ? Boolean.TRUE: Boolean.FALSE; }
	public Type		getType()			{ return Type.tpBoolean; }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: "+this);
		code.setLinePos(this.getPosLine());
		if( reqType != Type.tpVoid ) {
			if( value )
				code.addConst(1);
			else
				code.addConst(0);
		}
	}

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: if_true "+this);
		code.setLinePos(this.getPosLine());
		if( value ) code.addInstr(op_goto,label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: if_false "+this);
		code.setLinePos(this.getPosLine());
		if( !value ) code.addInstr(op_goto,label);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(String.valueOf(value)).space();
	}
}

@node
@dflow(out="this:in")
public final class ConstNullExpr extends ConstExpr {
	public byte		value;

	public ConstNullExpr() {}

	public String	toString()			{ return "null"; }
	public Object	getConstValue()		{ return null; }
	public Type		getType()			{ return Type.tpNull; }
}

@node
@dflow(out="this:in")
public final class ConstByteExpr extends ConstExpr {
	public byte		value;

	public ConstByteExpr() {}
	public ConstByteExpr(byte value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
	public Object	getConstValue()		{ return Byte.valueOf(value); }
	public Type		getType()			{ return Type.tpByte; }
}

@node
@dflow(out="this:in")
public final class ConstShortExpr extends ConstExpr {
	public short		value;

	public ConstShortExpr() {}
	public ConstShortExpr(short value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
	public Object	getConstValue()		{ return Short.valueOf(value); }
	public Type		getType()			{ return Type.tpShort; }
}

@node
@dflow(out="this:in")
public final class ConstIntExpr extends ConstExpr {
	public int			value;

	public ConstIntExpr() {}
	public ConstIntExpr(int value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
	public Object	getConstValue()		{ return Integer.valueOf(value); }
	public Type		getType()			{ return Type.tpInt; }
}

@node
@dflow(out="this:in")
public final class ConstLongExpr extends ConstExpr {
	public long			value;

	public ConstLongExpr() {}
	public ConstLongExpr(long value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value)+"L"; }
	public Object	getConstValue()		{ return Long.valueOf(value); }
	public Type		getType()			{ return Type.tpLong; }
}

@node
@dflow(out="this:in")
public final class ConstCharExpr extends ConstExpr {
	public char			value;

	public ConstCharExpr() {}
	public ConstCharExpr(char value) { this.value = value; }

	public String	toString()			{ return "'"+Convert.escape(value)+"'"; }
	public Object	getConstValue()		{ return Character.valueOf(value); }
	public Type		getType()			{ return Type.tpChar; }
}


@node
@dflow(out="this:in")
public final class ConstFloatExpr extends ConstExpr {
	public float			value;

	public ConstFloatExpr() {}
	public ConstFloatExpr(float value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value)+"F"; }
	public Object	getConstValue()		{ return Float.valueOf(value); }
	public Type		getType()			{ return Type.tpFloat; }
}


@node
@dflow(out="this:in")
public final class ConstDoubleExpr extends ConstExpr {
	public double			value;

	public ConstDoubleExpr() {}
	public ConstDoubleExpr(double value) { this.value = value; }

	public String	toString()			{ return String.valueOf(value)+"D"; }
	public Object	getConstValue()		{ return Double.valueOf(value); }
	public Type		getType()			{ return Type.tpDouble; }
}

@node
@dflow(out="this:in")
public final class ConstStringExpr extends ConstExpr {
	public KString			value;

	public ConstStringExpr() {}

	public ConstStringExpr(KString value) { this.value = value; }

	public String	toString()			{ return '\"'+value.toString()+'\"'; }
	public Object	getConstValue()		{ return value; }
	public Type		getType()			{ return Type.tpString; }

}



@node
public abstract class ConstExpr extends Expr {

	public KString text_name;
	
	public ConstExpr() {
		setResolved(true);
	}

	public abstract Type getType();
	public abstract Object getConstValue();

	public boolean	isConstantExpr() { return true; }
	public int		getPriority() { return 255; }

	public final boolean mainResolveIn(TransfProcessor proc) {
		// already fully resolved
		setResolved(true);
		return false;
	}
	
	public final void resolve(Type reqType) {
		setResolved(true);
	}

	public void generate(Code code, Type reqType) {
		Object value = getConstValue();
		trace(Kiev.debugStatGen,"\t\tgenerating ConstExpr: "+value);
		code.setLinePos(this.getPosLine());
		if( value == null ) {
			// Special case for generation of parametriezed
			// with primitive types classes
			if( reqType != null && !reqType.isReference() ) {
				switch(reqType.signature.byteAt(0)) {
				case 'Z': case 'B': case 'S': case 'I': case 'C':
					code.addConst(0);
					break;
				case 'J':
					code.addConst(0L);
					break;
				case 'F':
					code.addConst(0.F);
					break;
				case 'D':
					code.addConst(0.D);
					break;
				default:
					code.addNullConst();
					break;
				}
			}
			else
				code.addNullConst();
		}
		else if( value instanceof Byte ) {
			code.addConst(((Byte)value).intValue());
		}
		else if( value instanceof Short ) {
			code.addConst(((Short)value).intValue());
		}
		else if( value instanceof Integer ) {
			code.addConst(((Integer)value).intValue());
		}
		else if( value instanceof Character ) {
			code.addConst((int)((Character)value).charValue());
		}
		else if( value instanceof Long ) {
			code.addConst(((Long)value).longValue());
		}
		else if( value instanceof Float ) {
			code.addConst(((Float)value).floatValue());
		}
		else if( value instanceof Double ) {
			code.addConst(((Double)value).doubleValue());
		}
		else if( value instanceof KString ) {
			code.addConst((KString)value);
		}
		else throw new RuntimeException("Internal error: unknown type of constant "+value.getClass());
		if( reqType == Type.tpVoid ) code.addInstr(op_pop);
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


