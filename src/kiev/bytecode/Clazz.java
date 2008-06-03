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
package kiev.bytecode;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class Clazz implements BytecodeElement,BytecodeFileConstants {
	public static boolean	traceRead = false;
	public static boolean	traceWrite = false;
	public static boolean	traceRules = false;

	public PoolConstant[]			pool;
	public int						flags;
	public ClazzPoolConstant		cp_clazz;
	public ClazzPoolConstant		cp_super_clazz;
	public ClazzPoolConstant[]		cp_interfaces;
	public Field[]					fields;
	public Method[]					methods;
	public Attribute[]				attrs;

	public KString getClazzName() {
		return cp_clazz.ref.value;
	}

	public KString getSuperClazzName() {
		if( cp_super_clazz == null ) return null;
		return cp_super_clazz.ref.value;
	}

	public KString[] getInterfaceNames() {
		KString[] names = new KString[cp_interfaces.length];
		for(int i=0; i < names.length; i++)
			names[i] = cp_interfaces[i].ref.value;
		return names;
	}

	public KString getClazzSignature() {
		foreach (GenericsSignatureAttribute a; attrs)
			return a.getSignature(this);
		return null;
	}

	public void readClazz(byte[] data) {
		assert(data != null && data.length > 8 ,"Null bytecode");
		ReadContext cont = new ReadContext();
		cont.clazz = this;
		cont.data = data;
		cont.offset = 0;
		read(cont);
		assert(cont.offset == data.length ,"Read "+cont.offset+" of "+data.length);
	}

	public byte[] writeClazz() {
		byte[] data = new byte[size()];
		assert(data != null && data.length > 8 ,"Null bytecode");
		ReadContext cont = new ReadContext();
		cont.clazz = this;
		cont.data = data;
		cont.offset = 0;
		write(cont);
		assert(cont.offset == data.length ,"Write "+cont.offset+" of "+data.length);
		return data;
	}

	public int size() {
		int size = 8;	// magic(int)+magor+minor
		assert(pool != null ,"Null pool");
		size += poolSize();
		assert(cp_interfaces != null ,"Null interfaces");
		size += 8+cp_interfaces.length*2;	// flags+name+super_name+interfaces.length+interfaces.length*2
		size += 6;	// fields.length+methods.length+attrs.length
		assert(fields != null ,"Null fields");
		for(int i=0; i < fields.length; i++) {
			assert(fields[i] != null ,"Null field "+i);
			size += fields[i].size();
		}
		assert(methods != null ,"Null methods");
		for(int i=0; i < methods.length; i++) {
			assert(methods[i] != null ,"Null method "+i);
			size += methods[i].size();
		}
		assert(attrs != null ,"Null attrs");
		for(int i=0; i < attrs.length; i++) {
			assert(attrs[i] != null ,"Null attribute "+i);
//			assert(attrs[i].data != null ,"Null data in attribute "+i+": "+attrs[i].getName(this));
			size += attrs[i].size();
		}
		return size;
	}
	public void read(ReadContext cont) {
		int idx;
		assert(cont.offset == 0, "Read class not from offset 0");
		int magic = cont.readInt();
		trace(traceRead,cont.offset+": JAVA_MAGIC = 0x"+Integer.toHexString(magic));
		assert(magic == JAVA_MAGIC ,"Bad JAVA_MAGIC 0x"+Integer.toHexString(magic)+", should be 0x"+Integer.toHexString(JAVA_MAGIC));
		int minor = cont.readShort();
		trace(traceRead,cont.offset+": JAVA_MINOR_VERSION = "+minor);
		//assert(minor == JAVA_MINOR_VERSION ,"Bad JAVA_MINOR_VERSION "+minor+", should be "+JAVA_MINOR_VERSION);
		int magor = cont.readShort();
		trace(traceRead,cont.offset+": JAVA_VERSION = "+magor);
		//assert(magor == JAVA_VERSION ,"Bad JAVA_VERSION "+magor+", should be "+JAVA_VERSION);

		readConstantPool(cont);

		flags = cont.readShort();
		trace(traceRead,cont.offset+": class flags = 0x"+Integer.toHexString(flags));
		
		// class
		idx = cont.readShort();
		assert(idx > 0 && idx < pool.length ,"Class name index "+idx+" out of range");
		assert(pool[idx].constant_type() == CONSTANT_CLASS ,"Class name index "+idx+" does not points to CONSTANT_CLASS");
		cp_clazz = (ClazzPoolConstant)pool[idx];
		trace(traceRead,cont.offset+": class name "+idx+" = "+cp_clazz.ref.value);
		
		// super-class
		idx = cont.readShort();
		assert(idx >= 0 && idx < pool.length ,"Super-Class name index "+idx+" out of range");
		assert(idx==0 || pool[idx].constant_type() == CONSTANT_CLASS ,"Super-Class name index "+idx+" does not points to CONSTANT_CLASS");
		if (idx == 0)
			cp_super_clazz = null;
		else
			cp_super_clazz = (ClazzPoolConstant)pool[idx];
		trace(traceRead,cont.offset+": super-class name "+idx+" = "+(idx==0?"":cp_super_clazz.ref.value));

		int num = cont.readShort();
		trace(traceRead,cont.offset+": number of interfaces is "+num);
		assert(num*2 <= cont.data.length+cont.offset ,"Too big number of interfaces "+num);
		cp_interfaces = new ClazzPoolConstant[num];
		for(int i=0; i < num; i++) {
			idx = cont.readShort();
			assert(idx > 0 && idx < pool.length ,"Interface "+i+" name index "+idx+" out of range");
			assert(pool[idx].constant_type() == CONSTANT_CLASS ,"Interface "+i+" name index "+idx+" does not points to CONSTANT_CLASS");
			cp_interfaces[i] = (ClazzPoolConstant)pool[idx];
			trace(traceRead,cont.offset+": inetrface "+i+" name "+idx+" = "+cp_interfaces[i].ref.value);
		}

		num = cont.readShort();
		trace(traceRead,cont.offset+": number of fields is "+num);
		assert(num*8 <= cont.data.length+cont.offset ,"Too big number of fields "+num);
		if( num == 0 )
			fields = Field.emptyArray;
		else
			fields = new Field[num];
		for(int i=0; i < num; i++) {
			fields[i] = new Field();
			fields[i].read(cont);
		}

		num = cont.readShort();
		trace(traceRead,cont.offset+": number of methods is "+num);
		assert(num*8 <= cont.data.length+cont.offset ,"Too big number of methods "+num);
		if( num == 0 )
			methods = Method.emptyArray;
		else
			methods = new Method[num];
		for(int i=0; i < num; i++) {
			methods[i] = new Method();
			methods[i].read(cont);
		}

		attrs = Attribute.readAttributes(cont);
	}

	public void readConstantPool(ReadContext cont) {
		pool = PoolConstant.readConstantPool(cont);
	}

	public void write(ReadContext cont) {
		assert(cont.offset == 0, "Write class not from offset 0");
		assert(cont.data.length > 8, "Write into too small buffer "+cont.data.length);
		trace(traceWrite,cont.offset+": Bytecode byffer size "+cont.data.length);
		trace(traceWrite,cont.offset+": JAVA_MAGIC = 0x"+Integer.toHexString(JAVA_MAGIC));
		cont.writeInt(JAVA_MAGIC);
		trace(traceWrite,cont.offset+": JAVA_MINOR_VERSION = "+JAVA_MINOR_VERSION);
		cont.writeShort(JAVA_MINOR_VERSION);
		trace(traceWrite,cont.offset+": JAVA_VERSION = "+JAVA_VERSION);
		cont.writeShort(JAVA_VERSION);

		writeConstantPool(cont);

		assert(cont.data.length-cont.offset > 8, "Write into too small buffer");
		trace(traceWrite,cont.offset+": class flags = 0x"+Integer.toHexString(flags));
		cont.writeShort(flags);
		
		// class
		assert(cp_clazz.idx > 0 && cp_clazz.idx < pool.length ,"Class name index "+cp_clazz.idx+" out of range");
		assert(pool[cp_clazz.idx].constant_type() == CONSTANT_CLASS ,"Class name index "+cp_clazz+" does not points to CONSTANT_CLASS");
		trace(traceWrite,cont.offset+": class name "+cp_clazz+" = "+cp_clazz.ref.value);
		cont.writeShort(cp_clazz.idx);
		
		// super-class
		assert(cp_super_clazz == null || (cp_super_clazz.idx > 0 && cp_super_clazz.idx < pool.length) ,"Super-Class name index "+cp_super_clazz.idx+" out of range");
		assert(cp_super_clazz == null || pool[cp_super_clazz.idx].constant_type() == CONSTANT_CLASS ,"Super-Class name index "+cp_super_clazz+" does not points to CONSTANT_CLASS");
		trace(traceWrite,cont.offset+": super-class name "+cp_super_clazz+" = "+(cp_super_clazz==null?"":cp_super_clazz.ref.value));
		cont.writeShort(cp_super_clazz==null ? 0 : cp_super_clazz.idx);

		int num = cp_interfaces.length;
		cont.writeShort(num);
		for(int i=0; i < num; i++) {
			assert(cp_interfaces[i].idx > 0 && cp_interfaces[i].idx < pool.length ,"Interface "+i+" name index "+cp_interfaces[i].idx+" out of range");
			assert(pool[cp_interfaces[i].idx].constant_type() == CONSTANT_CLASS ,"Interface "+i+" name index "+cp_interfaces[i].idx+" does not points to CONSTANT_CLASS");
			cont.writeShort(cp_interfaces[i].idx);
		}

		num = fields.length;
		trace(traceWrite,cont.offset+": number of fields is "+num);
		cont.writeShort(num);
		for(int i=0; i < num; i++)
			fields[i].write(cont);

		num = methods.length;
		trace(traceWrite,cont.offset+": number of method is "+num);
		cont.writeShort(num);
		for(int i=0; i < num; i++)
			methods[i].write(cont);

		num = attrs.length;
		trace(traceWrite,cont.offset+": number of attrs is "+num);
		cont.writeShort(num);
		for(int i=0; i < num; i++)
			attrs[i].write(cont);
	}

	public void writeConstantPool(ReadContext cont) {
		PoolConstant.writeConstantPool(cont,pool);
	}

	public int poolSize() {
		int size = 2;
		int len = pool.length;
		for(int i=1; i < len; i++) {
			assert( pool[i] != null, "PoolConstant "+i+" is null" );
			size += pool[i].size();
		}
		return size;
	}

}

public interface BytecodeElement {
	public int				size();
	public void			read(ReadContext cont);
	public void			write(ReadContext cont);
}

public class ReadContext {
	public Clazz			clazz;
	public byte[]			data;
	public int				offset;

	public void read(byte[] b) {
		System.arraycopy(data,offset,b,0,b.length);
		offset += b.length;
	}

	public byte readByte() {
		return data[offset++];
	}

	public short readShort() {
		int ch1 = data[offset++];
		int ch2 = data[offset++];
		return (short)(((ch1 & 0xFF) << 8 ) | (ch2 & 0xFF));
	}

	public int readUShort() {
		int ch1 = data[offset++];
		int ch2 = data[offset++];
		return ((ch1 & 0xFF) << 8 ) | (ch2 & 0xFF);
	}

	public int readInt() {
		int ch1 = data[offset++];
		int ch2 = data[offset++];
		int ch3 = data[offset++];
		int ch4 = data[offset++];
		return ((ch1 & 0xFF) << 24 ) | ((ch2 & 0xFF) << 16 ) | ((ch3 & 0xFF) << 8 ) | (ch4 & 0xFF);
	}

	public long readLong() {
		int i1 = readInt();
		int i2 = readInt();
		return (((long)i1) << 32 ) | (long)( i2 & 0xFFFFFFFFL);
	}

	public float readFloat() {
		return Float.intBitsToFloat(readInt());
	}

	public double readDouble() {
		return Double.longBitsToDouble(readLong());
	}


	public void write(byte[] b, int start, int end) {
		System.arraycopy(b,start,data,offset,end-start);
		offset += end - start;
	}

	public void write(byte[] b) {
		System.arraycopy(b,0,data,offset,b.length);
		offset += b.length;
	}

	public void writeByte(int i) {
		data[offset++] = (byte)i;
	}

	public void writeShort(int i) {
		data[offset++] = (byte)(i>>8);
		data[offset++] = (byte)i;
	}

	public void writeInt(int i) {
		data[offset++] = (byte)(i>>24);
		data[offset++] = (byte)(i>>16);
		data[offset++] = (byte)(i>>8);
		data[offset++] = (byte)i;
	}

	public void writeLong(long l) {
		writeInt((int)(l>>32));
		writeInt((int)l);
	}

	public void writeFloat(float f) {
		writeInt(Float.floatToIntBits(f));
	}

	public void writeDouble(double d) {
		writeLong(Double.doubleToLongBits(d));
	}

}

