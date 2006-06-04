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

package kiev.bytecode;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public abstract class PoolConstant implements BytecodeFileConstants, BytecodeElement {

	public int				idx;

	public abstract void	write(ReadContext cont);
	public abstract int		size();
	public abstract int		constant_type();
	public boolean			double_slot() { return false; }

	public PoolConstant(int idx) {
		this.idx = idx;
	}

	public static PoolConstant[] readConstantPool(ReadContext cont) {
		int len = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": Pool containce "+len+" constants");
		assert(len > 0,"Null or negative number of pool constants "+len);
		PoolConstant[] pool = new PoolConstant[len];
		pool[0] = new VoidPoolConstant(0);
		fillConstantPool(cont,pool,1);
		return pool;
	}

	public static PoolConstant[] readKievConstantPool(ReadContext cont, PoolConstant[] oldpool) {
		int len = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": Pool containce "+len+" constants, inluding "+oldpool.length+" from class' pool");
		assert(len > 0,"Null or negative number of pool constants "+len);
		PoolConstant[] pool = new PoolConstant[len];
		System.arraycopy(oldpool,0,pool,0,oldpool.length);
		fillConstantPool(cont,pool,oldpool.length);
		return pool;
	}

	private static Utf8PoolConstant checkUtf8Ref(PoolConstant[] pool, int ref) {
		assert(ref > 0 && ref < pool.length ,"Reference to UTF8 constant "+ref+" out of range");
		assert(pool[ref].constant_type() == CONSTANT_UTF8 ,"Reference to UTF8 constant "+ref+" does not points to CONSTANT_UTF8");
		return (Utf8PoolConstant)pool[ref];
	}
	
	private static ClazzPoolConstant checkClsRef(PoolConstant[] pool, int ref) {
		assert(ref > 0 && ref < pool.length ,"Reference to CLASS constant "+ref+" out of range");
		assert(pool[ref].constant_type() == CONSTANT_CLASS ,"Reference to CLASS constant "+ref+" does not points to CONSTANT_CLASS");
		return (ClazzPoolConstant)pool[ref];
	}
	
	private static NameAndTypePoolConstant checkNmTpRef(PoolConstant[] pool, int ref) {
		assert(ref > 0 && ref < pool.length ,"Reference to constant "+ref+" out of range");
		assert(pool[ref].constant_type() == CONSTANT_NAMEANDTYPE ,"Reference to constant "+ref+" does not points to CONSTANT_NAMEANDTYPE");
		return (NameAndTypePoolConstant)pool[ref];
	}
	
	public static void fillConstantPool(ReadContext cont, PoolConstant[] pool, int pool_offset) {
		int len = pool.length;
		for(int i=pool_offset; i < len; i++) {
			int xtype = cont.readByte();
			switch(xtype) {
			case CONSTANT_UTF8:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_UTF8");
				pool[i] = new Utf8PoolConstant(i, cont);
				break;
			case CONSTANT_UNICODE:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_UNICODE");
				pool[i] = new UnicodePoolConstant(i, cont);
				break;
			case CONSTANT_INTEGER:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_INTEGER");
				pool[i] = new IntegerPoolConstant(i, cont);
				break;
			case CONSTANT_FLOAT:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_FLOAT");
				pool[i] = new FloatPoolConstant(i, cont);
				break;
			case CONSTANT_LONG:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_LONG");
				pool[i] = new LongPoolConstant(i, cont);
				break;
			case CONSTANT_DOUBLE:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_DOUBLE");
				pool[i] = new DoublePoolConstant(i, cont);
				break;
			case CONSTANT_CLASS:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_CLASS");
				pool[i] = new ClazzPoolConstant(i, cont);
				break;
			case CONSTANT_STRING:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_STRING");
				pool[i] = new StringPoolConstant(i, cont);
				break;
			case CONSTANT_FIELD:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_FIELD");
				pool[i] = new FieldPoolConstant(i, cont);
				break;
			case CONSTANT_METHOD:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_METHOD");
				pool[i] = new MethodPoolConstant(i, cont);
				break;
			case CONSTANT_INTERFACEMETHOD:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_INTERFACEMETHOD");
				pool[i] = new InterfaceMethodPoolConstant(i, cont);
				break;
			case CONSTANT_NAMEANDTYPE:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_NAMEANDTYPE");
				pool[i] = new NameAndTypePoolConstant(i, cont);
				break;
			default:
				assert(false,"Bad pool constant type "+xtype+" for constant "+i);
			}
			if( pool[i].double_slot() ) {
				assert(i < pool.length-1,"Double-slotted pool constant type at the end of constants");
				i++;
				pool[i] = new VoidPoolConstant(i);
			}
		}
		for(int i=pool_offset; i < len; i++) {
			int ref1, ref2;
			switch(pool[i].constant_type()) {
			case CONSTANT_CLASS:
				ref1 = ((ClazzPoolConstant)pool[i]).ref.idx;
				pool[i] = new ClazzPoolConstant(i, checkUtf8Ref(pool,ref1));
				break;
			case CONSTANT_STRING:
				ref1 = ((StringPoolConstant)pool[i]).ref.idx;
				pool[i] = new StringPoolConstant(i, checkUtf8Ref(pool,ref1));
				break;
			case CONSTANT_NAMEANDTYPE:
				ref1 = ((NameAndTypePoolConstant)pool[i]).ref_name.idx;
				ref2 = ((NameAndTypePoolConstant)pool[i]).ref_type.idx;
				pool[i] = new NameAndTypePoolConstant(i, checkUtf8Ref(pool,ref1), checkUtf8Ref(pool,ref2));
				break;
			}
		}
		for(int i=pool_offset; i < len; i++) {
			int cp, nt;
			switch(pool[i].constant_type()) {
			case CONSTANT_FIELD:
				cp = ((FieldPoolConstant)pool[i]).ref_clazz.idx;
				nt = ((FieldPoolConstant)pool[i]).ref_nametype.idx;
				pool[i] = new FieldPoolConstant(i, checkClsRef(pool, cp), checkNmTpRef(pool, nt));
				break;
			case CONSTANT_METHOD:
				cp = ((MethodPoolConstant)pool[i]).ref_clazz.idx;
				nt = ((MethodPoolConstant)pool[i]).ref_nametype.idx;
				pool[i] = new MethodPoolConstant(i, checkClsRef(pool, cp), checkNmTpRef(pool, nt));
				break;
			case CONSTANT_INTERFACEMETHOD:
				cp = ((InterfaceMethodPoolConstant)pool[i]).ref_clazz.idx;
				nt = ((InterfaceMethodPoolConstant)pool[i]).ref_nametype.idx;
				pool[i] = new InterfaceMethodPoolConstant(i, checkClsRef(pool, cp), checkNmTpRef(pool, nt));
				break;
			}
		}
	}

	public static void writeConstantPool(ReadContext cont, PoolConstant[] pool) {
		int len = pool.length;
		trace(Clazz.traceWrite,cont.offset+": Pool containce "+len+" constants");
		cont.writeShort(pool.length);
		for(int i=1; i < len; i++) {
			assert(cont.data.length-cont.offset >= pool[i].size(),"Too short buffer to write pool constant "+i);
			trace(Clazz.traceWrite,cont.offset+": constant "+i);
			pool[i].write(cont);
		}
	}

	public static void writeKievConstantPool(ReadContext cont, PoolConstant[] pool, int pool_offset) {
		int len = pool.length;
		trace(Clazz.traceWrite,cont.offset+": Pool containce "+len+" constants, starting from "+pool_offset);
		cont.writeShort(pool.length);
		for(int i=pool_offset; i < len; i++) {
			assert(cont.data.length-cont.offset >= pool[i].size(),"Too short buffer to write pool constant "+i);
			trace(Clazz.traceWrite,cont.offset+": constant "+i);
			pool[i].write(cont);
		}
	}
}

public final class VoidPoolConstant extends PoolConstant {

	public VoidPoolConstant(int idx) { super(idx); }

	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": void constant ");
	}
	public int constant_type() { return 0; }
	public int size()	{ return 0; }
}

public final class Utf8PoolConstant extends PoolConstant {

	public final KString			value;

	public Utf8PoolConstant(int idx, KString value) {
		super(idx);
		this.value = value;
	}
	public Utf8PoolConstant(int idx, ReadContext cont) {
		super(idx);
		int len = cont.readShort();
		assert(cont.data.length-cont.offset >= len,"Too big length "+len+" specified for UTF8 string ");
		value = KString.from(cont.data,cont.offset,cont.offset+len);
		trace(Clazz.traceRead,cont.offset+": value = "+value);
		cont.offset += len;
	}

	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_UTF8 len="+value.length()+", value="+value);
		cont.writeByte(CONSTANT_UTF8);
		cont.writeShort(value.length());
		cont.write(KString.buffer,value.offset,value.offset+value.len);
	}
	public int constant_type() { return CONSTANT_UTF8; }
	public int size()	{ return 1+2+value.len; }
}

public final class UnicodePoolConstant extends PoolConstant {

	public UnicodePoolConstant(int idx) {
		super(idx);
	}
	public UnicodePoolConstant(int idx, ReadContext cont) {
		super(idx);
		assert(false,"UnicodePoolConstant read");
	}
	public void write(ReadContext cont) {
		assert(false,"UnicodePoolConstant read");
	}
	public int constant_type() { return CONSTANT_UNICODE; }
	public int size()	{ return 0; }
}

public abstract class NumberPoolConstant extends PoolConstant {

	public NumberPoolConstant(int idx) { super(idx); }

	public abstract	Number getValue();
}

public final class IntegerPoolConstant extends NumberPoolConstant {

	public final int				value;

	public IntegerPoolConstant(int idx, int value) {
		super(idx);
		this.value = value;
	}
	public IntegerPoolConstant(int idx, ReadContext cont) {
		super(idx);
		value = cont.readInt();
		trace(Clazz.traceRead,cont.offset+": value = "+value);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_INTEGER value="+value);
		cont.writeByte(CONSTANT_INTEGER);
		cont.writeInt(value);
	}
	public int size()	{ return 1+4; }
	public int constant_type() { return CONSTANT_INTEGER; }
	public Number getValue() { return Integer.valueOf(value); }
}

public final class FloatPoolConstant extends NumberPoolConstant {

	public final float			value;

	public FloatPoolConstant(int idx, float value) {
		super(idx);
		this.value = value;
	}
	public FloatPoolConstant(int idx, ReadContext cont) {
		super(idx);
		value = cont.readFloat();
		trace(Clazz.traceRead,cont.offset+": value = "+value);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_FLOAT value="+value);
		cont.writeByte(CONSTANT_FLOAT);
		cont.writeFloat(value);
	}
	public int size()	{ return 1+4; }
	public int constant_type() { return CONSTANT_FLOAT; }
	public Number getValue() { return Float.valueOf(value); }
}

public final class LongPoolConstant extends NumberPoolConstant {

	public final long				value;

	public LongPoolConstant(int idx, long value) {
		super(idx);
		this.value = value;
	}
	public LongPoolConstant(int idx, ReadContext cont) {
		super(idx);
		value = cont.readLong();
		trace(Clazz.traceRead,cont.offset+": value = "+value);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_LONG value="+value);
		cont.writeByte(CONSTANT_LONG);
		cont.writeLong(value);
	}
	public int size()	{ return 1+8; }
	public int constant_type() { return CONSTANT_LONG; }
	public boolean double_slot() { return true; }
	public Number getValue() { return Long.valueOf(value); }
}

public class DoublePoolConstant extends NumberPoolConstant {

	public final double			value;

	public DoublePoolConstant(int idx, double value) {
		super(idx);
		this.value = value;
	}
	public DoublePoolConstant(int idx, ReadContext cont) {
		super(idx);
		value = cont.readDouble();
		trace(Clazz.traceRead,cont.offset+": value = "+value);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_DOUBLE value="+value);
		cont.writeByte(CONSTANT_DOUBLE);
		cont.writeDouble(value);
	}
	public int size()	{ return 1+8; }
	public int constant_type() { return CONSTANT_DOUBLE; }
	public boolean double_slot() { return true; }
	public Number getValue() { return Double.valueOf(value); }
}

public abstract class RefPoolConstant extends PoolConstant {

	public final Utf8PoolConstant		ref;

	public RefPoolConstant(int idx, Utf8PoolConstant ref) {
		super(idx);
		this.ref = ref;
	}
	public RefPoolConstant(int idx, ReadContext cont) {
		super(idx);
		ref = new Utf8PoolConstant(cont.readShort(), KString.Empty);
		trace(Clazz.traceRead,cont.offset+": ref = "+ref);
	}
	public void write(ReadContext cont) {
		cont.writeByte(constant_type());
		assert(ref.idx > 0 && ref.idx < cont.clazz.pool.length ,"Reference to UTF8 constant "+ref.idx+" out of range");
		assert(cont.clazz.pool[ref.idx].constant_type() == CONSTANT_UTF8 ,"Reference to UTF8 constant "+ref.idx+" does not points to CONSTANT_UTF8");
		trace(Clazz.traceWrite,cont.offset+": constant "+(constant_type()==CONSTANT_CLASS?"CONSTANT_CLASS":"CONSTANT_STRING")+" ref="+ref.idx+", value="+ref.value);
		cont.writeShort(ref.idx);
	}
	public int size()	{ return 1+2; }
}

public final class ClazzPoolConstant extends RefPoolConstant {

	public ClazzPoolConstant(int idx, Utf8PoolConstant ref) {
		super(idx,ref);
	}
	public ClazzPoolConstant(int idx, ReadContext cont) {
		super(idx,cont);
	}
	public int constant_type() { return CONSTANT_CLASS; }

}

public final class StringPoolConstant extends RefPoolConstant {

	public StringPoolConstant(int idx, Utf8PoolConstant ref) {
		super(idx,ref);
	}
	public StringPoolConstant(int idx, ReadContext cont) {
		super(idx,cont);
	}
	public int constant_type() { return CONSTANT_STRING; }

}

public final class NameAndTypePoolConstant extends PoolConstant {

	public final Utf8PoolConstant		ref_name;
	public final Utf8PoolConstant		ref_type;

	public NameAndTypePoolConstant(int idx, Utf8PoolConstant ref_name, Utf8PoolConstant ref_type) {
		super(idx);
		this.ref_name = ref_name;
		this.ref_type = ref_type;
	}
	public NameAndTypePoolConstant(int idx, ReadContext cont) {
		super(idx);
		ref_name = new Utf8PoolConstant(cont.readShort(), KString.Empty);
		ref_type = new Utf8PoolConstant(cont.readShort(), KString.Empty);
		trace(Clazz.traceRead,cont.offset+": ref_name = "+ref_name+", ref_type="+ref_type);
	}
	public void write(ReadContext cont) {
		cont.writeByte(CONSTANT_NAMEANDTYPE);
		assert(ref_name.idx > 0 && ref_name.idx < cont.clazz.pool.length ,"Reference to UTF8 constant "+ref_name.idx+" out of range");
		assert(cont.clazz.pool[ref_name.idx].constant_type() == CONSTANT_UTF8 ,"Reference to name constant "+ref_name+" does not points to CONSTANT_UTF8");
		cont.writeShort(ref_name.idx);
		assert(ref_type.idx > 0 && ref_type.idx < cont.clazz.pool.length ,"Reference to clazz constant "+ref_type.idx+" out of range");
		assert(cont.clazz.pool[ref_type.idx].constant_type() == CONSTANT_UTF8 ,"Reference to type constant "+ref_type+" does not points to CONSTANT_UTF8");
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_NAMEANDTYPE "
			+" ref_name="+ref_name.idx+", name="+ref_name.value
			+" ref_type="+ref_type.idx+", signature="+ref_type.value);
		cont.writeShort(ref_type.idx);
	}
	public int size()	{ return 1+4; }
	public int constant_type() { return CONSTANT_NAMEANDTYPE; }
}

public abstract class ClazzNameTypePoolConstant extends PoolConstant {

	public final ClazzPoolConstant			ref_clazz;
	public final NameAndTypePoolConstant	ref_nametype;

	public ClazzNameTypePoolConstant(int idx, ClazzPoolConstant ref_clazz, NameAndTypePoolConstant ref_nametype) {
		super(idx);
		this.ref_clazz = ref_clazz;
		this.ref_nametype = ref_nametype;
	}
	public ClazzNameTypePoolConstant(int idx, ReadContext cont) {
		super(idx);
		ref_clazz = new ClazzPoolConstant(cont.readShort(), (Utf8PoolConstant)null);
		ref_nametype = new NameAndTypePoolConstant(cont.readShort(),null,null);
		trace(Clazz.traceRead,cont.offset+": ref_clazz = "+ref_clazz+", ref_nametype="+ref_nametype);
	}
	public void write(ReadContext cont) {
		cont.writeByte(constant_type());
		assert(ref_clazz.idx > 0 && ref_clazz.idx < cont.clazz.pool.length ,"Reference to clazz constant "+ref_clazz.idx+" out of range");
		assert(cont.clazz.pool[ref_clazz.idx].constant_type() == CONSTANT_CLASS ,"Reference to clazz constant "+ref_clazz+" does not points to CONSTANT_CLASS");
		cont.writeShort(ref_clazz.idx);
		assert(ref_nametype.idx > 0 && ref_nametype.idx < cont.clazz.pool.length ,"Reference to UTF8 constant "+ref_nametype.idx+" out of range");
		assert(cont.clazz.pool[ref_nametype.idx].constant_type() == CONSTANT_NAMEANDTYPE ,"Reference to name&type constant "+ref_nametype+" does not points to CONSTANT_NAMEANDTYPE");
		trace(Clazz.traceWrite,cont.offset+": constant "+(constant_type()==CONSTANT_FIELD?"CONSTANT_FIELD":constant_type()==CONSTANT_METHOD?"CONSTANT_METHOD":"CONSTANT_INTERFACEMETHOD")
			+" ref_clazz="+ref_clazz.idx+", clazz="+ref_clazz.ref.value
			+", ref_nametype="+ref_nametype.idx
			+", name="+ref_nametype.ref_name.value
			+", signature="+ref_nametype.ref_type.value);
		cont.writeShort(ref_nametype.idx);
	}
	public int size()	{ return 1+4; }
}

public final class FieldPoolConstant extends ClazzNameTypePoolConstant {

	public FieldPoolConstant(int idx, ClazzPoolConstant ref_clazz, NameAndTypePoolConstant ref_nametype) {
		super(idx,ref_clazz,ref_nametype);
	}
	public FieldPoolConstant(int idx, ReadContext cont) {
		super(idx,cont);
	}
	public int constant_type() { return CONSTANT_FIELD; }

}

public final class MethodPoolConstant extends ClazzNameTypePoolConstant {

	public MethodPoolConstant(int idx, ClazzPoolConstant ref_clazz, NameAndTypePoolConstant ref_nametype) {
		super(idx,ref_clazz,ref_nametype);
	}
	public MethodPoolConstant(int idx, ReadContext cont) {
		super(idx,cont);
	}
	public int constant_type() { return CONSTANT_METHOD; }

}

public final class InterfaceMethodPoolConstant extends ClazzNameTypePoolConstant {

	public InterfaceMethodPoolConstant(int idx, ClazzPoolConstant ref_clazz, NameAndTypePoolConstant ref_nametype) {
		super(idx,ref_clazz,ref_nametype);
	}
	public InterfaceMethodPoolConstant(int idx, ReadContext cont) {
		super(idx,cont);
	}
	public int constant_type() { return CONSTANT_INTERFACEMETHOD; }

}


