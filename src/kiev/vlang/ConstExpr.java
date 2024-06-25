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

public enum IntRadix {
	RADIX_DEC,
	RADIX_HEX,
	RADIX_OCT
}

@ThisIsANode(lang=CoreLang)
public final class ConstBoolExpr extends ConstExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public boolean value;

	public ConstBoolExpr() {}
	public ConstBoolExpr(boolean value) { this.value = value; }
	
	public Type		getType(Env env)			{ return env.tenv.tpBoolean; }

	public Object	getConstValue(Env env)		{ return value ? Boolean.TRUE: Boolean.FALSE; }
	
	public boolean valueEquals(Object o) {
		if (o instanceof ConstBoolExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString() { return String.valueOf(value); }
}

@ThisIsANode(lang=CoreLang)
public final class ConstNullExpr extends ConstExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	public ConstNullExpr() {}

	public Type		getType(Env env)			{ return env.tenv.tpNull; }

	public Object	getConstValue(Env env)		{ return null; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstNullExpr)
			return true;
		return false;
	}

	public String	toString()			{ return "null"; }
}

@ThisIsANode(lang=CoreLang)
public abstract class ConstRadixExpr extends ConstExpr {
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public IntRadix	radix;

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "radix")
			return this.radix != null && this.radix != IntRadix.RADIX_DEC;
		return super.includeInDump(dump, attr, val);
	}

}

@ThisIsANode(lang=CoreLang)
public final class ConstByteExpr extends ConstRadixExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public byte		value;

	public ConstByteExpr() {}
	public ConstByteExpr(byte value) { this.value = value; }

	public Type		getType(Env env)			{ return env.tenv.tpByte; }

	public Object	getConstValue(Env env)		{ return Byte.valueOf(value); }

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

@ThisIsANode(lang=CoreLang)
public final class ConstShortExpr extends ConstRadixExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public short		value;

	public ConstShortExpr() {}
	public ConstShortExpr(short value) { this.value = value; }

	public Type		getType(Env env)			{ return env.tenv.tpShort; }

	public Object	getConstValue(Env env)		{ return Short.valueOf(value); }

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

@ThisIsANode(lang=CoreLang)
public final class ConstIntExpr extends ConstRadixExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public int		value;

	public ConstIntExpr() {}
	public ConstIntExpr(int value) { this.value = value; }

	public Type		getType(Env env)			{ return env.tenv.tpInt; }

	public Object	getConstValue(Env env)		{ return Integer.valueOf(value); }

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

@ThisIsANode(lang=CoreLang)
public final class ConstLongExpr extends ConstRadixExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public long		value;

	public ConstLongExpr() {}
	public ConstLongExpr(long value) { this.value = value; }

	public Type		getType(Env env)			{ return env.tenv.tpLong; }

	public Object	getConstValue(Env env)		{ return Long.valueOf(value); }

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

@ThisIsANode(lang=CoreLang)
public final class ConstCharExpr extends ConstExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public char value;

	public ConstCharExpr() {}
	public ConstCharExpr(char value) { this.value = value; }

	public Type		getType(Env env)			{ return env.tenv.tpChar; }

	public Object	getConstValue(Env env)		{ return Character.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstCharExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString()			{ return "'"+value+"'"; }
}


@ThisIsANode(lang=CoreLang)
public final class ConstFloatExpr extends ConstExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public float value;

	public ConstFloatExpr() {}
	public ConstFloatExpr(float value) { this.value = value; }

	public Type		getType(Env env)			{ return env.tenv.tpFloat; }

	public Object	getConstValue(Env env)		{ return Float.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstFloatExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString()			{ return String.valueOf(value)+"F"; }
}


@ThisIsANode(lang=CoreLang)
public final class ConstDoubleExpr extends ConstExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public double value;

	public ConstDoubleExpr() {}
	public ConstDoubleExpr(double value) { this.value = value; }

	public Type		getType(Env env)			{ return env.tenv.tpDouble; }

	public Object	getConstValue(Env env)		{ return Double.valueOf(value); }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstDoubleExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString()			{ return String.valueOf(value)+"D"; }
}

@ThisIsANode(lang=CoreLang)
public final class ConstStringExpr extends ConstExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	public static final ConstStringExpr[] emptyArray = new ConstStringExpr[0];
	
	@nodeAttr public String value;

	public ConstStringExpr() {
		this.value = "";
	}
	public ConstStringExpr(String value) {
		if (value == null)
			this.value = "";
		else
			this.value = value;
	}

	public Type		getType(Env env)			{ return env.tenv.tpString; }

	public Object	getConstValue(Env env)		{ return value; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstStringExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString()			{ return '\"'+value.toString()+'\"'; }
}

@unerasable
@ThisIsANode(lang=CoreLang)
public final class ConstEnumExpr<E extends Enum> extends ConstExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public E value;

	public ConstEnumExpr() {}
	public ConstEnumExpr(E value) { this.value = value; }

	public Type getType(Env env) {
		if (value == null)
			return new ASTNodeType(this.getTypeInfoField().getTopArgs()[0].clazz);
		return new ASTNodeType(value.getClass());
	}

	public Object	getConstValue(Env env)		{ return value; }

	public boolean valueEquals(Object o) {
		if (o instanceof ConstEnumExpr)
			return o.value == this.value;
		return false;
	}

	public String	toString()			{ return String.valueOf(value); }

	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot == ConstEnumExpr.nodeattr$value) {
			AutoCompleteResult result = new AutoCompleteResult(true);
			foreach (Enum e; (Enum[])this.getTypeInfoField().getTopArgs()[0].clazz.getDeclaredMethod(Constants.nameEnumValues).invoke(null)) {
				result.append(e.toString(), e.getClass().getName(), null, e); 
			}
			result.append("null", "null", null, null);
			return result;
		}
		return null;
	}
}


@ThisIsANode(lang=CoreLang)
public abstract class ConstExpr extends ENode {

	public ConstExpr() {
		setResolved(true);
	}

	public int		getPriority(Env env) { return 255; }

	public abstract Object getConstValue(Env env);

	public boolean	isConstantExpr(Env env) { return true; }

	public final boolean mainResolveIn(Env env, INode parent, AttrSlot slot) {
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
							d = source.charAt(i+k);
							if (d >= '0' && d <= '9') d -= '0';
							else if (d >= 'a' && d <= 'f') d = d - 'a' + 10;
							else if (d >= 'A' && d <= 'F') d = d - 'A' + 10;
							else d = 0;
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


