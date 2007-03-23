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
package kiev.stdlib;

import syntax kiev.stdlib.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final class KStringBuffer {

	public byte[]	buf;
	public int		pos;
	
	public KStringBuffer() {
		buf = new byte[32];
		pos = 0;
	}
	
	public KStringBuffer(int i) {
		buf = new byte[i];
		pos = 0;
	}
	
	public String toString() { return toKString().toString(); }
	
	public KString toKString() { return KString.from(buf,0,pos); }
	
	private void ensure_buf(int i) {
		if( buf.length < i ) {
			int len = ((i / 32)+1)*32;
			byte[] newbuf = new byte[len];
			System.arraycopy(buf,0,newbuf,0,pos);
			buf = newbuf;
		}
	}
	
	public KStringBuffer append(char b) {
		if ((b >= 0x0001) && (b <= 0x007F)) {
			ensure_buf(pos+1);
			buf[pos++] = (byte)(b & 0xFF);
		} else if (b > 0x07FF) {
			ensure_buf(pos+3);
			buf[pos++] = (byte)(0xE0 | ((b >> 12) & 0x0F));
			buf[pos++] = (byte)(0x80 | ((b >>  6) & 0x3F));
			buf[pos++] = (byte)(0x80 | (b & 0x3F));
		} else {
			ensure_buf(pos+2);
			buf[pos++] = (byte)(0xC0 | ((b >>  6) & 0x1F));
			buf[pos++] = (byte)(0x80 | (b & 0x3F));
		}
		return this;
	}
	
	public KStringBuffer append(byte b) {
		ensure_buf(pos+1);
		buf[pos++] = b;
		return this;
	}
	public KStringBuffer append_fast(byte b) {
		buf[pos++] = b;
		return this;
	}

	public KStringBuffer append(KString k) {
		ensure_buf(pos+k.len);
		System.arraycopy(KString.buffer,k.offset,buf,pos,k.len);
		pos += k.len;
		return this;
	}
	public KStringBuffer append_fast(KString k) {
		System.arraycopy(KString.buffer,k.offset,buf,pos,k.len);
		pos += k.len;
		return this;
	}

	// Append array of bytes from start to (non-inclusive) end positions
	public KStringBuffer append(byte[] k, int start_pos, int end_pos) {
		int len = end_pos-start_pos;
		if( len > 0 ) {
			ensure_buf(pos+len);
			System.arraycopy(k,start_pos,buf,pos,len);
			pos += len;
		}
		return this;
	}
	public KStringBuffer append_fast(byte[] k, int start_pos, int end_pos) {
		int len = end_pos-start_pos;
		if( len > 0 ) {
			System.arraycopy(k,start_pos,buf,pos,len);
			pos += len;
		}
		return this;
	}

	public KStringBuffer append(byte[] k) {
		return append(k,0,k.length);
	}
	public KStringBuffer append_fast(byte[] k) {
		int len = k.length;
		if( len > 0 ) {
			System.arraycopy(k,0,buf,pos,len);
			pos += len;
		}
		return this;
	}
		
	public KStringBuffer append(String str) {
		// Optimized version of append(char)
		int slen = str.length();
		int len = 0;
		for(int i=0; i < slen; i++) {
			int b = str.charAt(i);
			if ((b >= 0x0001) && (b <= 0x007F)) len += 1;
			else if (b > 0x07FF) len += 3;
			else len += 2;
		}
		ensure_buf(pos+len);

		for(int i=0; i < slen; i++) {
			int b = str.charAt(i);
			if ((b >= 0x0001) && (b <= 0x007F)) {
				buf[pos++] = (byte)(b & 0xFF);
			} else if (b > 0x07FF) {
				buf[pos++] = (byte)(0xE0 | ((b >> 12) & 0x0F));
				buf[pos++] = (byte)(0x80 | ((b >>  6) & 0x3F));
				buf[pos++] = (byte)(0x80 | (b & 0x3F));
			} else {
				buf[pos++] = (byte)(0xC0 | ((b >>  6) & 0x1F));
				buf[pos++] = (byte)(0x80 | (b & 0x3F));
			}
		}
		return this;
	}

	public KStringBuffer append(int i) {
		return append(String.valueOf(i));
	}
	public KStringBuffer append(float f) {
		return append(String.valueOf(f));
	}
	public KStringBuffer append(double d) {
		return append(String.valueOf(d));
	}
	public KStringBuffer append(boolean bool) {
		return append(String.valueOf(bool));
	}
	public KStringBuffer append(Object o) {
		return append(String.valueOf(o));
	}
}

