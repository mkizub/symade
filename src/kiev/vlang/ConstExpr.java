package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
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
	@virtual typedef NImpl = ConstBoolExprImpl;
	@virtual typedef VView = ConstBoolExprView;
	@virtual typedef JView = JConstBoolExpr;

	@nodeimpl
	public static class ConstBoolExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstBoolExpr;
		@att public boolean value;
	}
	@nodeview
	public static view ConstBoolExprView of ConstBoolExprImpl extends ConstExprView {
		public boolean	value;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstBoolExpr() { super(new ConstBoolExprImpl()); }
	public ConstBoolExpr(boolean value) { super(new ConstBoolExprImpl()); this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
	public Object	getConstValue()		{ return value ? Boolean.TRUE: Boolean.FALSE; }
	public Type		getType()			{ return Type.tpBoolean; }
	
	public boolean valueEquals(Object o) {
		if (o instanceof ConstBoolExpr)
			return o.value == this.value;
		return false;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(String.valueOf(value)).space();
	}
}

@nodeset
public final class ConstNullExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstNullExpr;
	@virtual typedef NImpl = ConstNullExprImpl;
	@virtual typedef VView = ConstNullExprView;
	@virtual typedef JView = JConstNullExpr;

	@nodeimpl
	public static class ConstNullExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstNullExpr;
	}
	@nodeview
	public static view ConstNullExprView of ConstNullExprImpl extends ConstExprView {
		public ConstNullExprView(ConstNullExprImpl $view) {
			super($view);
		}
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstNullExpr() { super(new ConstNullExprImpl()); }

	public String	toString()			{ return "null"; }
	public Object	getConstValue()		{ return null; }
	public Type		getType()			{ return Type.tpNull; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstNullExpr)
			return true;
		return false;
	}
}

@nodeset
public final class ConstByteExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstByteExpr;
	@virtual typedef NImpl = ConstByteExprImpl;
	@virtual typedef VView = ConstByteExprView;
	@virtual typedef JView = JConstByteExpr;

	@nodeimpl
	public static class ConstByteExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstByteExpr;
		@att public byte value;
	}
	@nodeview
	public static view ConstByteExprView of ConstByteExprImpl extends ConstExprView {
		public byte		value;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstByteExpr() { super(new ConstByteExprImpl()); }
	public ConstByteExpr(byte value) { super(new ConstByteExprImpl()); this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
	public Object	getConstValue()		{ return Byte.valueOf(value); }
	public Type		getType()			{ return Type.tpByte; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstByteExpr)
			return o.value == this.value;
		return false;
	}
}

@nodeset
public final class ConstShortExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstShortExpr;
	@virtual typedef NImpl = ConstShortExprImpl;
	@virtual typedef VView = ConstShortExprView;
	@virtual typedef JView = JConstShortExpr;

	@nodeimpl
	public static class ConstShortExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstShortExpr;
		@att public short value;
	}
	@nodeview
	public static view ConstShortExprView of ConstShortExprImpl extends ConstExprView {
		public short		value;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstShortExpr() { super(new ConstShortExprImpl()); }
	public ConstShortExpr(short value) { super(new ConstShortExprImpl()); this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
	public Object	getConstValue()		{ return Short.valueOf(value); }
	public Type		getType()			{ return Type.tpShort; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstShortExpr)
			return o.value == this.value;
		return false;
	}
}

@nodeset
public final class ConstIntExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstIntExpr;
	@virtual typedef NImpl = ConstIntExprImpl;
	@virtual typedef VView = ConstIntExprView;
	@virtual typedef JView = JConstIntExpr;

	@nodeimpl
	public static class ConstIntExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstIntExpr;
		@att public int value;
	}
	@nodeview
	public static view ConstIntExprView of ConstIntExprImpl extends ConstExprView {
		public int		value;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstIntExpr() { super(new ConstIntExprImpl()); }
	public ConstIntExpr(int value) { super(new ConstIntExprImpl()); this.value = value; }

	public String	toString()			{ return String.valueOf(value); }
	public Object	getConstValue()		{ return Integer.valueOf(value); }
	public Type		getType()			{ return Type.tpInt; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstIntExpr)
			return o.value == this.value;
		return false;
	}
}

@nodeset
public final class ConstLongExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstLongExpr;
	@virtual typedef NImpl = ConstLongExprImpl;
	@virtual typedef VView = ConstLongExprView;
	@virtual typedef JView = JConstLongExpr;

	@nodeimpl
	public static class ConstLongExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstLongExpr;
		@att public long value;
	}
	@nodeview
	public static view ConstLongExprView of ConstLongExprImpl extends ConstExprView {
		public long		value;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstLongExpr() { super(new ConstLongExprImpl()); }
	public ConstLongExpr(long value) { super(new ConstLongExprImpl()); this.value = value; }

	public String	toString()			{ return String.valueOf(value)+"L"; }
	public Object	getConstValue()		{ return Long.valueOf(value); }
	public Type		getType()			{ return Type.tpLong; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstLongExpr)
			return o.value == this.value;
		return false;
	}
}

@nodeset
public final class ConstCharExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstCharExpr;
	@virtual typedef NImpl = ConstCharExprImpl;
	@virtual typedef VView = ConstCharExprView;
	@virtual typedef JView = JConstCharExpr;

	@nodeimpl
	public static class ConstCharExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstCharExpr;
		@att public char value;
	}
	@nodeview
	public static view ConstCharExprView of ConstCharExprImpl extends ConstExprView {
		public char		value;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstCharExpr() { super(new ConstCharExprImpl()); }
	public ConstCharExpr(char value) { super(new ConstCharExprImpl()); this.value = value; }

	public String	toString()			{ return "'"+Convert.escape(value)+"'"; }
	public Object	getConstValue()		{ return Character.valueOf(value); }
	public Type		getType()			{ return Type.tpChar; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstCharExpr)
			return o.value == this.value;
		return false;
	}
}


@nodeset
public final class ConstFloatExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstFloatExpr;
	@virtual typedef NImpl = ConstFloatExprImpl;
	@virtual typedef VView = ConstFloatExprView;
	@virtual typedef JView = JConstFloatExpr;

	@nodeimpl
	public static class ConstFloatExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstFloatExpr;
		@att public float value;
	}
	@nodeview
	public static view ConstFloatExprView of ConstFloatExprImpl extends ConstExprView {
		public float		value;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstFloatExpr() { super(new ConstFloatExprImpl()); }
	public ConstFloatExpr(float value) { super(new ConstFloatExprImpl()); this.value = value; }

	public String	toString()			{ return String.valueOf(value)+"F"; }
	public Object	getConstValue()		{ return Float.valueOf(value); }
	public Type		getType()			{ return Type.tpFloat; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstFloatExpr)
			return o.value == this.value;
		return false;
	}
}


@nodeset
public final class ConstDoubleExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstDoubleExpr;
	@virtual typedef NImpl = ConstDoubleExprImpl;
	@virtual typedef VView = ConstDoubleExprView;
	@virtual typedef JView = JConstDoubleExpr;

	@nodeimpl
	public static class ConstDoubleExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstDoubleExpr;
		@att public double value;
	}
	@nodeview
	public static view ConstDoubleExprView of ConstDoubleExprImpl extends ConstExprView {
		public double		value;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstDoubleExpr() { super(new ConstDoubleExprImpl()); }
	public ConstDoubleExpr(double value) { super(new ConstDoubleExprImpl()); this.value = value; }

	public String	toString()			{ return String.valueOf(value)+"D"; }
	public Object	getConstValue()		{ return Double.valueOf(value); }
	public Type		getType()			{ return Type.tpDouble; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstDoubleExpr)
			return o.value == this.value;
		return false;
	}
}

@nodeset
public final class ConstStringExpr extends ConstExpr {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = ConstStringExpr;
	@virtual typedef NImpl = ConstStringExprImpl;
	@virtual typedef VView = ConstStringExprView;
	@virtual typedef JView = JConstStringExpr;

	@nodeimpl
	public static class ConstStringExprImpl extends ConstExprImpl {
		@virtual typedef ImplOf = ConstStringExpr;
		@att public KString value;
	}
	@nodeview
	public static view ConstStringExprView of ConstStringExprImpl extends ConstExprView {
		public KString		value;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ConstStringExpr() { super(new ConstStringExprImpl()); }
	public ConstStringExpr(KString value) { super(new ConstStringExprImpl()); this.value = value; }

	public String	toString()			{ return '\"'+value.toString()+'\"'; }
	public Object	getConstValue()		{ return value; }
	public Type		getType()			{ return Type.tpString; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstStringExpr)
			return o.value == this.value;
		return false;
	}
}


@nodeset
public abstract class ConstExpr extends ENode {

	@virtual typedef This  = ConstExpr;
	@virtual typedef NImpl = ConstExprImpl;
	@virtual typedef VView = ConstExprView;
	@virtual typedef JView = JConstExpr;

	@nodeimpl
	public abstract static class ConstExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = ConstExpr;
	}
	@nodeview
	public abstract static view ConstExprView of ConstExprImpl extends ENodeView {
		public ConstExprView(ConstExprImpl $view) {
			super($view);
		}

		public int		getPriority() { return 255; }

		public final boolean mainResolveIn() {
			// already fully resolved
			setResolved(true);
			return false;
		}
	}

	public abstract VView getVView() alias operator(210,fy,$cast);
	public abstract JView getJView() alias operator(210,fy,$cast);

	public ConstExpr(ConstExprImpl impl) {
		super(impl);
		setResolved(true);
	}

	public abstract Type getType();
	public abstract Object getConstValue();

	public boolean	isConstantExpr() { return true; }
	
	public final void resolve(Type reqType) {
		setResolved(true);
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


