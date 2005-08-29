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
 * @version $Revision$
 *
 */

public final class KString {
	
	private final static int		tablesize	= 0x10000;
	private final static int		tablemask	= 0xffff;
	private static KString[]		table		= new KString[tablesize];

	/** UTF8 encoded names (currently just ascii strings supported only) */
	public static byte[]			buffer		= new byte[tablesize];
	
	/** High water mark for buffer */
	public static int				bn			= 0;

	public static int				count;

	public final static KString		Empty		= from(new byte[]{},0,0);

	/** Offset in buffer of KString in table */
	public int		offset;
	
	/** Length of the string */
	public int		len;
	
	/** Hash code of the KString (low 2 bytes) plus position in list
		of strings with the same hash code (high 2 bytes)
		This gives unique index for each KString
	 */
	public int		index;
	
	/** Next name with the same table index (i.e. with the same hash 
		code in low 2 bytes) 
	 */
	public KString	next;

	public static int calcIndex(byte[] name, int start, int end) {
		int index = 0;
		int len = end-start;
		if( len < 8 ) {
			if( len % 2 == 0 ) {
				for(int i=start; i < end; i+=2) {
					index = (index + (name[i] << 8) + name[i+1]) * 37;
				}
			} else {
				for(int i=start; i < end-1; i+=2) {
					index = (index + (name[i] << 8) + name[i+1]) * 37;
				}
				index = (index + name[end-1] << 8) * 37;
			}
		}
		else {
			int incr;
			if( len < 32 ) incr = len >>> 2;
			else incr = len >>> 4;
			end--;
			for(int i=start; i < end; i+=incr) {
				index = (index + (name[i] << 8) + name[i+1]) * 37;
			}
		}
		index &= tablemask;
		return index;
	}

	public static int calcFullIndex(byte[] name, int start, int end) {
		int index = 0;
		int len = end-start;
		if( len % 2 == 0 ) {
			for(int i=start; i < end; i+=2) {
				index = (index + (name[i] << 8) + name[i+1]) * 37;
			}
		} else {
			for(int i=start; i < end-1; i+=2) {
				index = (index + (name[i] << 8) + name[i+1]) * 37;
			}
			index = (index + name[end-1] << 8) * 37;
		}
		index &= 0x7FFFFFFF;
		return index;
	}
	
	public int calcFullIndex() {
		return calcFullIndex(buffer, offset, offset+len);
	}

	private KString(int ind, byte[] name, KString prev, int start, int end) {
//		synchronize( buffer ) {
			index = ind;
			next = null;
			if( prev != null )
				prev.next = this;
			else
				table[ ind & tablemask ] = this;
			len = end - start;
			if( name == buffer ) {
				offset = start;
			} else {
				if( (buffer.length - bn) <= len) {
					int incr;
					if( len < tablesize ) incr = tablesize;
					else incr = tablesize*( len/tablesize + 1 );
					byte[] newbuf = new byte[buffer.length + incr];
					System.arraycopy(buffer,0,newbuf,0,bn);
					buffer = newbuf;
				}
				offset = bn;
				System.arraycopy(name,start,buffer,bn,len);
				bn += len;
			}
			// Debug code
//			int debugIndex = calcIndex(buffer,offset,offset+len);
//			if( debugIndex != (index & tablemask) )
//				throw new Error("Name \""+this+"\" has index that differ from calculation: "+debugIndex+" != "+(index & tablemask));
//		}
		count++;
	}
	
	public int length() { return len; }
	
	public String toString() {
		StringBuffer sb = new StringBuffer(len);
		for(KStringScanner sc=new KStringScanner(this); sc.hasMoreChars();) {
			char ch = sc.nextChar();
			sb.append(ch);
		}
		return sb.toString();
	}

	public byte[] getBytes() {
		byte[] bytes = new byte[len];
		System.arraycopy(buffer,offset,bytes,0,len);
		return bytes;
	}
	
	public byte byteAt(int i) { return buffer[offset+i]; }
	
	public char charAt(int i) {
		int j = 0;
		for(KStringScanner sc=new KStringScanner(this); sc.hasMoreChars(); j++) {
			char ch = sc.nextChar();
			if( j==i ) return ch;
		}
		return (char)0;
	}
	
	public static KString from(String s)
		alias operator (210,fy,~)
	{
		byte[] name = Convert.string2ascii(s);
		return from(name,0,name.length);
	}

    public static KString fromSource(String str) {
    	byte[] ascii = new byte[str.length()*2];
    	int len = Convert.source2ascii(str.getBytes(),0,str.length(),ascii);
    	return from(ascii,0,len);
    }

	public static KString from(byte[] name) {
		return from(name,0,name.length);
	}

	public static KString from(byte[] name, int start, int end) {
		int index = calcIndex(name,start,end);
		if( table[index] == null )
			return new KString(index,name,null,start,end);
		else {
			KString tn = table[index];
			int ext = 0;
			if( tn.equals(name,start,end) ) return tn;
			while( tn.next != null ) {
				tn = tn.next;
				ext++;
				if( tn.equals(name,start,end) ) return tn;
			}
			return new KString(index+(++ext<<16),name,tn,start,end);
		}
	}
	
	public final boolean equals(byte[] nm, int start, int end) {
		int nlen = end-start;
		if( len != nlen ) return false;
		for(int i=0; i < nlen; i++)
			if( buffer[offset+i] != nm[start+i] ) return false;
		return true;
	}
	
	public final boolean equals(byte[] nm) {
		return equals(nm,0,nm.length);
	}
	
	public final boolean equals(char[] nm) {
		int nlen = nm.length;
		if( len != nlen ) return false;
		for(int i=0; i < nlen; i++)
			if( buffer[offset+i] != nm[i] ) return false;
		return true;
	}
	
	public final boolean equals(String nm) {
		int nlen = nm.length();
		if( len != nlen ) return false;
		for(int i=0; i < nlen; i++)
			if( buffer[offset+i] != nm.charAt(i) ) return false;
		return true;
	}
	
	public final boolean equals(KString nm) {
		return	this.index == nm.index /* this.offset == nm.offset && this.len == nm.len */;
	}

	public final boolean equals(Object nm) {
		if( nm instanceof KString ) return equals((KString)nm);
		else if( nm instanceof String ) return equals((String)nm);
		else if( nm instanceof byte[] ) return equals((byte[])nm);
		else if( nm instanceof char[] ) return equals((char[])nm);
		else return false;
	}

	public final int hashCode() {
		return index;
	}
	
	public int indexOf(char b) {
		return indexOf((byte)b);
	}

	public int indexOf(byte b) {
		int max_offset = offset+len;
		for(int i=offset; i < max_offset; i++ )
			if( buffer[i] == b ) return i-offset;
		return -1;
	}

	public int indexOf(char b, int offs) {
		return indexOf((byte)b, offs);
	}

	public int indexOf(byte b, int offs) {
		int max_offset = offset+len;
		for(int i=offset+offs; i < max_offset; i++ )
			if( buffer[i] == b ) return i-offset;
		return -1;
	}

	public int lastIndexOf(char b) {
		return lastIndexOf((byte)b);
	}

	public int lastIndexOf(byte b) {
		int max_offset = offset+len-1;
		for(int i=max_offset; i >= offset; i-- )
			if( buffer[i] == b ) return i-offset;
		return -1;
	}
	
	public boolean startsWith(KString kstr) {
		int len2 = kstr.len;
		if( len2 > len ) return false;
		if( len2 == len ) return equals(kstr);
		int i1 = offset;
		int i2 = kstr.offset;
		len2 += i1;
		for(; i1 < len2; i1++, i2++ )
			if( buffer[i1] != buffer[i2] ) return false;
		return true;
	}

	public KString substr(int start) {
		if( start >= len ) return Empty;
		return from(buffer,offset+start,offset+len);
	}

	public KString substr(int start, int end) {
		if( start >= len ) return Empty;
		if( end >= len ) end = len;
		return from(buffer,offset+start,offset+end);
	}
	
	public KString replace(char b1, char b2) {
		return replace((byte)b1,(byte)b2);
	}

	public KString replace(byte b1, byte b2) {
		if( indexOf(b1) == -1 ) return this;
		byte[] newbuf = new byte[len];
		System.arraycopy(buffer,offset,newbuf,0,len);
		for(int i=0; i < len; i++ )
			if( newbuf[i] == b1 ) newbuf[i] = b2;
		return from(newbuf,0,newbuf.length);
	}

	/** KString class test/debug */
	public static void main(String[] args) {
		KString aaa = KString.from("aaa");
		System.out.println("Create name "+aaa);
		aaa.dumpKString();
		System.out.println("aaa.equals(aaa)="+aaa.equals(aaa));
		KString bbb = KString.from("bbb");
		bbb.dumpKString();
		System.out.println("Create name "+bbb);
		System.out.println("bbb.equals(bbb)="+bbb.equals(bbb));
		System.out.println("aaa.equals(bbb)="+aaa.equals(bbb));
		System.out.println("bbb.equals(aaa)="+bbb.equals(aaa));
		KString aaa1 = KString.from("aaa");
		aaa1.dumpKString();
		System.out.println("Create another name "+aaa1);
		System.out.println("aaa.equals(aaa')="+aaa.equals(aaa1));
		System.out.println("bbb.equals(aaa')="+bbb.equals(aaa1));
		
		KString t1 = KString.from("@");
		KString t2 = KString.from("()S");
		System.out.println("'@'=='()S' = "+t1.equals(t2));
		System.out.println("(Object)'@'==(Object)'()S' = "+((Object)t1).equals((Object)t2));
		t1.dumpKString();
		t2.dumpKString();

		KString xxx = KString.from("---/---/---/---/-");
		System.out.println("Index of '/' in "+xxx+" is "+xxx.indexOf('/'));
		System.out.println("Index of '/' in "+xxx+" from offset 4 is "+xxx.indexOf('/',4));
		System.out.println("Index of '/' in "+xxx+" from offset 5 is "+xxx.indexOf('/',5));
		System.out.println("Last Index of '/' in "+xxx+" is "+xxx.lastIndexOf('/'));
		System.out.println("Substring from offset 3 of "+xxx+" is "+xxx.substr(3));
		System.out.println("Substring from offset 3 to 7 of "+xxx+" is "+xxx.substr(3,7));
		System.out.println("Replace of '/' into '.' in "+xxx+" is "+xxx.replace('/','.'));

		KString yyy = KString.from("1/22/333/4444/-");
		KStringTokenizer yyyt = new KStringTokenizer(yyy,'/');
		System.out.println("Tokens in "+yyy+" containce "+yyyt.countTokens()+" tokens separated by '/', tokens are:");
		while( yyyt.hasMoreTokens() ) 
			System.out.println("\t"+yyyt.nextToken());

		KStringBuffer ksb = new KStringBuffer();
		System.out.println("Test KStringBuffer");
		System.out.println("\""+ksb+"\""+".append('a')="+ksb.append('a'));
		System.out.println("\""+ksb+"\""+".append(\" hello \")="+ksb.append(" hello "));
		System.out.println("\""+ksb+"\""+".append(ksb.toKString())="+ksb.append(ksb.toKString()));
		
		KString kfs = KString.fromSource("abcd");
		System.out.println("KString.fromSource(\"abcd\") = "+kfs);
		kfs = KString.fromSource("a\\tbcd");
		System.out.println("KString.fromSource(\"a\\tbcd\") = "+kfs);
		kfs = KString.fromSource("a\\u0033tbcd");
		System.out.println("KString.fromSource(\"a\\u0033tbcd\") = "+kfs);
		kfs = KString.fromSource("a\\088tbcd");
		System.out.println("KString.fromSource(\"a\\088tbcd\") = "+kfs);
	}

	private void dumpKString() {
		System.out.println("KString '"+toString()+"' has index "+index+", length "+len+", offset "+offset);
	}
//	*/


	public static class KStringScanner {
	
		public KString	str;
		public int		pos;
		
		public KStringScanner(KString str) {
			this.str = str;
			pos = 0;
		}
		
		public String toString() { return str.toString(); }
		
		public boolean hasMoreChars() {
			return pos < str.len;
		}

		public char nextChar() {
			int ptr = str.offset;
			int b = buffer[ptr+pos++] & 0xFF;
			if (b >= 0xE0) {
				b = (b & 0x0F) << 12;
				b = b | (buffer[ptr+pos++] & 0x3F) << 6;
				b = b | (buffer[ptr+pos++] & 0x3F);
			}
			else if (b >= 0xC0) {
				b = (b & 0x1F) << 6;
				b = b | (buffer[ptr+pos++] & 0x3F);
			}
			return (char)b;
		}

		public char peekChar() {
			int p = pos;
			char ch = nextChar();
			pos = p;
			return ch;
		}
	}

}

/* ************************************************************************
 * Pizza name manager
 * Author     : Martin Odersky
 *
 * Copyright (C) 1996,97 Martin Odersky. All rights reserved.
 * Permission is hereby granted to modify and use this software for research
 * and teaching purposes. Modification for commercial purposes requires
 * prior written permission by the author.
 * The software, or modifications thereof, may be redistributed only
 * if this copyright notice stays attached.
 *************************************************************************/

/** Conversion routines:
 */
public class Convert {

    public static int digit2int(byte ch, int base) {
        if ('0' <= ch && ch <= '9' && ch < '0' + base)
            return ch - '0';
        else if ('A' <= ch && ch < 'A' + base - 10)
            return ch - 'A' + 10;
        else if ('a' <= ch && ch < 'a' + base - 10)
            return ch - 'a' + 10;
        else
            return -1;
    }

    public static byte int2digit(int x) {
        if (x <= 9) return (byte)(x + '0');
        else return (byte)(x - 10 + 'A');
    }

/* the next 4 functions convert between three fundamental name
 *representations:
 *  - string   each character 16 bit,
 *  - source   characters outside 0..127 are represented by
 *             unicode escapes, \ u X X X X
 *  - ascii    characters outside 0..127 are represented by two or three
 *             byte sequences with high bit set (as in class file format).
 */

/** convert source bytes in source[offset..offset+len-1] to ascii.
 */
    public static int source2ascii(byte source[], int offset, int len,
                            byte ascii[]) {
        int j = 0;
        int i = 0;
        while (i < len) {
            if (source[offset + i] == '\\' && i + 1 < len) {
                i++;
                switch (source[offset + i]) {
                case 'n':
                    ascii[j++] = (byte)'\n'; i++; continue;
                case 't':
                    ascii[j++] = (byte)'\t'; i++; continue;
                case 'b':
                    ascii[j++] = (byte)'\b'; i++; continue;
                case 'r':
                    ascii[j++] = (byte)'\r'; i++; continue;
                case 'f':
                    ascii[j++] = (byte)'\f'; i++; continue;
                case '0': case '1': case '2': case '3': case '4': case '5': 
                case '6': case '7': case '8': case '9':
                	{
                	int code = 0;
                	for(int k=0; k < 3 && i < len && source[offset+i] >='0' && source[offset+i] <='8'; k++, i++) {
                		code = code*8 + (source[offset+i] - '0');
                	}
                    ascii[j++] = (byte)code;
                    continue;
                	}
                case 'u':
                    if (i + 4 < len) {
                        int code = 0;
                        int k = 1;
                        int d = 0;
                        while (k <= 4 && d >= 0) {
                            d = digit2int(source[offset + i + k], 16);
                            code = code * 16 + d;
                            k++;
                        }
                        if (d >= 0) {
                            if (code <= 0x7F) {
                                ascii[j++] = (byte)code;
                            } else if (code <= 0x3FF) {
                                ascii[j++] = (byte)(0xC0 | (code >> 6));
                                ascii[j++] = (byte)(0x80 | (code & 0x3F));
                            } else {
                                ascii[j++] = (byte)(0xE0 | (code >> 12));
                                ascii[j++] = (byte)(0x80 |
                                                    ((code >> 6) & 0x3F));
                                ascii[j++] = (byte)(0x80 | (code & 0x3F));
                            }
                            i = i + 5;
                            continue;
                        }
                    }
                }
            }
            byte b = source[offset + i++];
            if (b >= 0)
                ascii[j++] = b;
            else {
                ascii[j++] = (byte)(0xC0 | ((b >> 6) & 0x3));
                ascii[j++] = (byte)(0x80 | (b & 0x3F));
            }
        }
        return j;
    }

/** convert ascii bytes in ascii[offset..offset+len-1] to a string.
 */
    public static String ascii2string(byte ascii[], int offset, int len) {
        char cs[] = new char[len];
        int i = 0;
        int j = 0;
        while (i < len) {
            int b = ascii[offset + i++] & 0xFF;
            if (b >= 0xE0) {
                b = (b & 0x0F) << 12;
                b = b | (ascii[offset + i++] & 0x3F) << 6;
                b = b | (ascii[offset + i++] & 0x3F);
            } else if (b >= 0xC0) {
                b = (b & 0x1F) << 6;
                b = b | (ascii[offset + i++] & 0x3F);
            }
            cs[j++] = (char)b;
        }
        return new String(cs, 0, j);
    }

/** convert string to array of source bytes.
 */
    public static byte[] string2source(String s) {
        byte[] source = new byte[s.length() * 6];
        int j = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
            case '\n':
                source[j++] = (byte)'\\';
                source[j++] = (byte)'n';
                break;
            case '\t':
                source[j++] = (byte)'\\';
                source[j++] = (byte)'t';
                break;
            case '\b':
                source[j++] = (byte)'\\';
                source[j++] = (byte)'b';
                break;
            case '\r':
                source[j++] = (byte)'\\';
                source[j++] = (byte)'r';
                break;
            case '\f':
                source[j++] = (byte)'\\';
                source[j++] = (byte)'f';
                break;
            case '\"':
                source[j++] = (byte)'\\';
                source[j++] = (byte)'\"';
                break;
            case '\'':
                source[j++] = (byte)'\\';
                source[j++] = (byte)'\'';
                break;
            case '\\':
                source[j++] = (byte)'\\';
                source[j++] = (byte)'\\';
                break;
            default:
                if (' ' <= ch && ch <= 127)
                    source[j++] = (byte)ch;
                else {
                    source[j++] = (byte)'\\';
                    source[j++] = (byte)'u';
                    source[j++] = int2digit((ch >> 12) & 0xF);
                    source[j++] = int2digit((ch >> 8) & 0xF);
                    source[j++] = int2digit((ch >> 4) & 0xF);
                    source[j++] = int2digit(ch & 0xF);
                }
            }
        }
        byte[] res = new byte[j];
        System.arraycopy(source, 0, res, 0, j);
        return res;
    }

/** convert string to array of ascii bytes.
 */
    public static byte[] string2ascii(String s) {
        byte[] source = string2source(s);
        byte[] ascii = new byte[source.length * 2];
        int alen = source2ascii(source, 0, source.length, ascii);
        byte[] res = new byte[alen];
        System.arraycopy(ascii, 0, res, 0, alen);
        return res;
    }

/** escape all characters outside 32..127 in string s.
 */
    public static String escape(String s) {
        return new String(string2source(s), 0);
    }

/** escape character c, if outside 32..127.
 */
    public static String escape(char c) {
        char[] s = new char[]{c};
        return escape(new String(s));
    }
}


