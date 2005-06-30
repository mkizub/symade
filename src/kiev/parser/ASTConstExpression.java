/* Generated By:JJTree: Do not edit this line. ASTConstExpression.java */

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

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTConstExpression.java,v 1.3 1998/10/26 23:47:02 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTConstExpression extends Expr implements kiev020Constants {
	public Object	val;

	ASTConstExpression(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
    }

	public void set(Token t) throws ParseException {
        pos = t.getPos();
		switch(t.kind) {
		case INTEGER_LITERAL:
		{
			long i;
			String image;
			int radix;
			if( t.image.startsWith("0x") || t.image.startsWith("0X") ) { image = t.image.substring(2); radix = 16; }
			else if( t.image.startsWith("0") && t.image.length() > 1 ) { image = t.image.substring(1); radix = 8; }
			else { image = t.image; radix = 10; }
			try {
				i = ConstExpr.parseLong(image,radix);
			} catch( NumberFormatException e ) {
				throw new ParseException("Integer literal "+t.image+" parsed as "+image+" and radix "+radix+" throws NumberFormatException");
			}
			Number n;
			int ii = (int)i;
			//if( ii >= Byte.MIN_VALUE && ii <= Byte.MAX_VALUE )
			//	val = Kiev.newByte(ii);
			//else if( ii >= Short.MIN_VALUE && ii <= Short.MAX_VALUE )
			//	val = Kiev.newShort(ii);
			//else
				val = Kiev.newInteger(ii);
			return;
		}
		case LONG_INTEGER_LITERAL:
		{
			long i;
			String image;
			int radix;
			if( t.image.startsWith("0x") || t.image.startsWith("0X") ) { image = t.image.substring(2,t.image.length()-1); radix = 16; }
			else if( t.image.startsWith("0") && !t.image.equals("0") && !t.image.equals("0L") ) { image = t.image.substring(1,t.image.length()-1); radix = 8; }
			else { image = t.image.substring(0,t.image.length()-1); radix = 10; }
			try {
				i = ConstExpr.parseLong(image,radix);
			} catch( NumberFormatException e ) {
				throw new ParseException("Long literal "+t.image+" parsed as "+image+" and radix "+radix+" throws NumberFormatException");
			}
			val = Kiev.newLong(i);
			return;
		}
		case FLOATING_POINT_LITERAL:
		{
			Float f;
			String image;
			if( t.image.endsWith("f") || t.image.endsWith("F") ) image = t.image.substring(0,t.image.length()-1);
			else image = t.image;
			try {
				val = Kiev.newFloat(Float.valueOf(image).floatValue());
			} catch( NumberFormatException e ) {
				throw new ParseException("Float literal "+t.image+" parsed as "+image+" throws NumberFormatException");
			}
			return;
		}
		case DOUBLE_POINT_LITERAL:
		{
			Double d;
			String image;
			if( t.image.endsWith("d") || t.image.endsWith("D") ) image = t.image.substring(0,t.image.length()-1);
			else image = t.image;
			try {
				val = Kiev.newDouble(Double.valueOf(t.image).doubleValue());
			} catch( NumberFormatException e ) {
				throw new ParseException("Double literal "+t.image+" parsed as "+image+" throws NumberFormatException");
			}
			return;
		}
		case CHARACTER_LITERAL:
		{
			if( t.image.length() == 3 )
				val = Kiev.newCharacter( t.image.charAt(1) );
			else {
				val = Kiev.newCharacter( source2ascii(t.image.substring(1,t.image.length()-1)).charAt(0) );
			}
			return;
		}
		case STRING_LITERAL:
		{
			val = source2ascii(t.image.substring(1,t.image.length()-1));
			return;
		}
		case TRUE:
		{
			val = Boolean.TRUE;
			return;
		}
		case FALSE:
		{
			val = Boolean.FALSE;
			return;
		}
		case NULL:
		{
			val = null;
			return;
		}
		default:
			throw new CompilerException(pos,"Unknown term "+t.image);
		}
	}

    public static long parseLong(String s, int radix)
              throws NumberFormatException
    {
        if (s == null) {
            throw new NumberFormatException("null");
        }
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
		if (digit < 0) {
		    throw new NumberFormatException(s);
		} else {
		    result = -digit;
		}
	    }
	    while (i < max) {
		// Accumulating negatively avoids surprises near MAX_VALUE
		digit = Character.digit(s.charAt(i++),radix);
		if (digit < 0) {
		    throw new NumberFormatException(s);
		}
//		if (result < multmin) {
//		    throw new NumberFormatException(s);
//		}
		result *= radix;
//		if (result < limit + digit) {
//		    throw new NumberFormatException(s);
//		}
		result -= digit;
	    }
	} else {
	    throw new NumberFormatException(s);
	}
	if (negative) {
	    if (i > 1) {
		return result;
	    } else {	/* Only got "-" */
		throw new NumberFormatException(s);
	    }
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


	public ASTNode resolve(Type reqType) {
		if( val instanceof Boolean )
			return new ConstBooleanExpr(pos,((Boolean)val).booleanValue()).resolve(reqType);
		return new ConstExpr(pos,val);
	}

	public int		getPriority() { return 255; }

    public String toString() {
		return new ConstExpr(pos,val).toString();
    }

    public Dumper toJava(Dumper dmp) {
		return new ConstExpr(pos,val).toJava(dmp);
    }
}
