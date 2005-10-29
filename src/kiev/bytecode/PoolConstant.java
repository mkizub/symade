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

/**
 * @author Maxim Kizub
 * @version $Revision: 182 $
 *
 */

public abstract class PoolConstant implements BytecodeFileConstants, BytecodeElement {

	public int				constant_type;

	public abstract void	read(ReadContext cont);
	public abstract void	write(ReadContext cont);
	public abstract int		size();
	public boolean			double_slot() { return false; }

	public PoolConstant(int constant_type) {
		this.constant_type = constant_type;
	}

	public static PoolConstant[] readConstantPool(ReadContext cont) {
		int len = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": Pool containce "+len+" constants");
		assert(len > 0,"Null or negative number of pool constants "+len);
		PoolConstant[] pool = new PoolConstant[len];
		pool[0] = new VoidPoolConstant();
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

	public static void fillConstantPool(ReadContext cont, PoolConstant[] pool, int pool_offset) {
		int len = pool.length;
		for(int i=pool_offset; i < len; i++) {
			int ctype = cont.readByte();
			switch(ctype) {
			case CONSTANT_UTF8:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_UTF8");
				pool[i] = new Utf8PoolConstant();
				break;
			case CONSTANT_UNICODE:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_UNICODE");
				pool[i] = new UnicodePoolConstant();
				break;
			case CONSTANT_INTEGER:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_INTEGER");
				pool[i] = new IntegerPoolConstant();
				break;
			case CONSTANT_FLOAT:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_FLOAT");
				pool[i] = new FloatPoolConstant();
				break;
			case CONSTANT_LONG:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_LONG");
				pool[i] = new LongPoolConstant();
				break;
			case CONSTANT_DOUBLE:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_DOUBLE");
				pool[i] = new DoublePoolConstant();
				break;
			case CONSTANT_CLASS:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_CLASS");
				pool[i] = new ClazzPoolConstant();
				break;
			case CONSTANT_STRING:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_STRING");
				pool[i] = new StringPoolConstant();
				break;
			case CONSTANT_FIELD:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_FIELD");
				pool[i] = new FieldPoolConstant();
				break;
			case CONSTANT_METHOD:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_METHOD");
				pool[i] = new MethodPoolConstant();
				break;
			case CONSTANT_INTERFACEMETHOD:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_INTERFACEMETHOD");
				pool[i] = new InterfaceMethodPoolConstant();
				break;
			case CONSTANT_NAMEANDTYPE:
				trace(Clazz.traceRead,cont.offset+": const "+i+" CONSTANT_NAMEANDTYPE");
				pool[i] = new NameAndTypePoolConstant();
				break;
			default:
				assert(false,"Bad pool constant type "+ctype+" for constant "+i);
			}
			pool[i].read(cont);
			if( pool[i].double_slot() ) {
				assert(i < pool.length-1,"Double-slotted pool constant type at the end of constants");
				pool[++i] = new VoidPoolConstant();
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

public class VoidPoolConstant extends PoolConstant {

	public VoidPoolConstant() { super(0); }

	public void read(ReadContext cont) {
		assert(false,"VoidPoolConstant read");
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": void constant ");
	}
	public int size()	{ return 0; }
}

public class Utf8PoolConstant extends PoolConstant {

	public KString			value;

	public Utf8PoolConstant() { super(CONSTANT_UTF8); }

	public void read(ReadContext cont) {
		int len = cont.readShort();
		assert(cont.data.length-cont.offset >= len,"Too big length "+len+" specified for UTF8 string ");
		value = KString.from(cont.data,cont.offset,cont.offset+len);
		trace(Clazz.traceRead,cont.offset+": value = "+value);
		cont.offset += len;
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_UTF8 len="+value.length()+", value="+value);
		cont.writeByte(constant_type);
		cont.writeShort(value.length());
		cont.write(KString.buffer,value.offset,value.offset+value.len);
	}
	public int size()	{ return 1+2+value.len; }
}

public class UnicodePoolConstant extends PoolConstant {

	public UnicodePoolConstant() { super(CONSTANT_UNICODE); }

	public void read(ReadContext cont) {
		assert(false,"UnicodePoolConstant read");
	}
	public void write(ReadContext cont) {
		assert(false,"UnicodePoolConstant read");
	}
	public int size()	{ return 0; }
}

public abstract class NumberPoolConstant extends PoolConstant {

	public NumberPoolConstant(int constant_type) { super(constant_type); }

	public abstract	Number getValue();
}

public class IntegerPoolConstant extends NumberPoolConstant {

	public int				value;

	public IntegerPoolConstant() { super(CONSTANT_INTEGER); }

	public void read(ReadContext cont) {
		value = cont.readInt();
		trace(Clazz.traceRead,cont.offset+": value = "+value);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_INTEGER value="+value);
		cont.writeByte(constant_type);
		cont.writeInt(value);
	}
	public int size()	{ return 1+4; }
	public Number getValue() { return Integer.valueOf(value); }
}

public class FloatPoolConstant extends NumberPoolConstant {

	public float			value;

	public FloatPoolConstant() { super(CONSTANT_FLOAT); }

	public void read(ReadContext cont) {
		value = cont.readFloat();
		trace(Clazz.traceRead,cont.offset+": value = "+value);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_FLOAT value="+value);
		cont.writeByte(constant_type);
		cont.writeFloat(value);
	}
	public int size()	{ return 1+4; }
	public Number getValue() { return Float.valueOf(value); }
}

public class LongPoolConstant extends NumberPoolConstant {

	public long				value;

	public LongPoolConstant() { super(CONSTANT_LONG); }

	public void read(ReadContext cont) {
		value = cont.readLong();
		trace(Clazz.traceRead,cont.offset+": value = "+value);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_LONG value="+value);
		cont.writeByte(constant_type);
		cont.writeLong(value);
	}
	public int size()	{ return 1+8; }
	public boolean double_slot() { return true; }
	public Number getValue() { return Long.valueOf(value); }
}

public class DoublePoolConstant extends NumberPoolConstant {

	public double			value;

	public DoublePoolConstant() { super(CONSTANT_DOUBLE); }

	public void read(ReadContext cont) {
		value = cont.readDouble();
		trace(Clazz.traceRead,cont.offset+": value = "+value);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_DOUBLE value="+value);
		cont.writeByte(constant_type);
		cont.writeDouble(value);
	}
	public int size()	{ return 1+8; }
	public boolean double_slot() { return true; }
	public Number getValue() { return Double.valueOf(value); }
}

public abstract class RefPoolConstant extends PoolConstant {

	public int				ref;

	public RefPoolConstant(int constant_type) {
		super(constant_type);
	}

	public void read(ReadContext cont) {
		ref = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": ref = "+ref);
	}
	public void write(ReadContext cont) {
		cont.writeByte(constant_type);
		assert(ref > 0 && ref < cont.clazz.pool.length ,"Reference to UTF8 constant "+ref+" out of range");
		assert(cont.clazz.pool[ref].constant_type == CONSTANT_UTF8 ,"Reference to UTF8 constant "+ref+" does not points to CONSTANT_UTF8");
		trace(Clazz.traceWrite,cont.offset+": constant "+(constant_type==CONSTANT_CLASS?"CONSTANT_CLASS":"CONSTANT_STRING")+" ref="+ref+", value="+((Utf8PoolConstant)cont.clazz.pool[ref]).value);
		cont.writeShort(ref);
	}
	public int size()	{ return 1+2; }
}

public class ClazzPoolConstant extends RefPoolConstant {

	public ClazzPoolConstant() { super(CONSTANT_CLASS); }

}

public class StringPoolConstant extends RefPoolConstant {

	public StringPoolConstant() { super(CONSTANT_STRING); }

}

public abstract class ClazzNameTypePoolConstant extends PoolConstant {

	public int				ref_clazz;
	public int				ref_nametype;

	public ClazzNameTypePoolConstant(int constant_type) {
		super(constant_type);
	}

	public void read(ReadContext cont) {
		ref_clazz = cont.readShort();
		ref_nametype = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": ref_clazz = "+ref_clazz+", ref_nametype="+ref_nametype);
	}
	public void write(ReadContext cont) {
		cont.writeByte(constant_type);
		assert(ref_clazz > 0 && ref_clazz < cont.clazz.pool.length ,"Reference to clazz constant "+ref_clazz+" out of range");
		assert(cont.clazz.pool[ref_clazz].constant_type == CONSTANT_CLASS ,"Reference to clazz constant "+ref_clazz+" does not points to CONSTANT_CLASS");
		cont.writeShort(ref_clazz);
		assert(ref_nametype > 0 && ref_nametype < cont.clazz.pool.length ,"Reference to UTF8 constant "+ref_nametype+" out of range");
		assert(cont.clazz.pool[ref_nametype].constant_type == CONSTANT_NAMEANDTYPE ,"Reference to name&type constant "+ref_nametype+" does not points to CONSTANT_NAMEANDTYPE");
		trace(Clazz.traceWrite,cont.offset+": constant "+(constant_type==CONSTANT_FIELD?"CONSTANT_FIELD":constant_type==CONSTANT_METHOD?"CONSTANT_METHOD":"CONSTANT_INTERFACEMETHOD")
			+" ref_clazz="+ref_clazz+", clazz="+((Utf8PoolConstant)cont.clazz.pool[((ClazzPoolConstant)cont.clazz.pool[ref_clazz]).ref]).value
			+", ref_nametype="+ref_nametype
			+", name="+((Utf8PoolConstant)cont.clazz.pool[((NameAndTypePoolConstant)cont.clazz.pool[ref_nametype]).ref_name]).value
			+", signature="+((Utf8PoolConstant)cont.clazz.pool[((NameAndTypePoolConstant)cont.clazz.pool[ref_nametype]).ref_type]).value);
		cont.writeShort(ref_nametype);
	}
	public int size()	{ return 1+4; }
}

public class FieldPoolConstant extends ClazzNameTypePoolConstant {

	public FieldPoolConstant() { super(CONSTANT_FIELD); }

}

public class MethodPoolConstant extends ClazzNameTypePoolConstant {

	public MethodPoolConstant() { super(CONSTANT_METHOD); }

}

public class InterfaceMethodPoolConstant extends ClazzNameTypePoolConstant {

	public InterfaceMethodPoolConstant() { super(CONSTANT_INTERFACEMETHOD); }

}

public class NameAndTypePoolConstant extends PoolConstant {

	public int				ref_name;
	public int				ref_type;

	public NameAndTypePoolConstant() { super(CONSTANT_NAMEANDTYPE); }

	public void read(ReadContext cont) {
		ref_name = cont.readShort();
		ref_type = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": ref_name = "+ref_name+", ref_type="+ref_type);
	}
	public void write(ReadContext cont) {
		cont.writeByte(constant_type);
		assert(ref_name > 0 && ref_name < cont.clazz.pool.length ,"Reference to UTF8 constant "+ref_name+" out of range");
		assert(cont.clazz.pool[ref_name].constant_type == CONSTANT_UTF8 ,"Reference to name constant "+ref_name+" does not points to CONSTANT_UTF8");
		cont.writeShort(ref_name);
		assert(ref_type > 0 && ref_type < cont.clazz.pool.length ,"Reference to clazz constant "+ref_type+" out of range");
		assert(cont.clazz.pool[ref_type].constant_type == CONSTANT_UTF8 ,"Reference to type constant "+ref_type+" does not points to CONSTANT_UTF8");
		trace(Clazz.traceWrite,cont.offset+": constant CONSTANT_NAMEANDTYPE "
			+" ref_name="+ref_name+", name="+((Utf8PoolConstant)cont.clazz.pool[ref_name]).value
			+" ref_type="+ref_type+", signature="+((Utf8PoolConstant)cont.clazz.pool[ref_type]).value);
		cont.writeShort(ref_type);
	}
	public int size()	{ return 1+4; }
}

