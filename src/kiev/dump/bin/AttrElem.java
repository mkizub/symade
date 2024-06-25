package kiev.dump.bin;

import java.lang.reflect.Field;

import kiev.vtree.AttrSlot;
import kiev.vlang.Env;

public class AttrElem extends Elem {
	
	public static final int IS_CHILD		= 1 << 0;		// otherwice it's a primitive value of cross-ref to another node
	public static final int IS_OPTIONAL		= 1 << 1;		// otherwice it's a required value
	public static final int IS_SPACE		= 1 << 2;		// otherwice it's a scalar attribute
	public static final int IS_EXTERNAL		= 1 << 3;		// global, otherwice it's declared withing a type
	public static final int IS_NO_COPY		= 1 << 4;		// don't copy when cloning a node
	public static final int IS_LEADING		= 1 << 5;		// required to be read before new node creation

	// type of data (type of value)
	public TypeElem				vtype;
	// attribute names
	public String				name;
	// declaration type (in which the attribute was declared), if not external
	public TypeElem				intype;
	
	private AttrSlot			attr_slot;
	private Enum				enum_value;

	public AttrElem(int id, TypeElem vtype, int flags, String name) {
		super(id);
		this.vtype = vtype;
		this.flags = flags;
		this.name = name;
	}
	
	public AttrElem(int id, TypeElem vtype, int flags, AttrSlot attr_slot) {
		super(id);
		this.vtype = vtype;
		this.flags = flags;
		this.name = attr_slot.name;
		this.attr_slot = attr_slot;
	}
	
	public AttrElem(int id, int addr) {
		super(id, addr);
	}
	
	public AttrSlot getAttrSlot() {
		return attr_slot;
	}
	public Enum getEnumVal() {
		return enum_value;
	}

	public boolean isChild()	{ return (flags & IS_CHILD) != 0; }
	public boolean isOptional()	{ return (flags & IS_OPTIONAL) != 0; }
	public boolean isSpace()	{ return (flags & IS_SPACE) != 0; }
	//public boolean isExternal()	{ return (flags & IS_EXTERNAL) != 0; }
	public boolean isNoCopy()	{ return (flags & IS_NO_COPY) != 0; }
	public boolean isLeading()	{ return (flags & IS_LEADING) != 0; }

	public void build(Env env) {
		if (intype == null)
			return;
		try {
			Field vals = intype.typeinfo.clazz.getDeclaredField("$values");
			vals.setAccessible(true);
			for (AttrSlot slot : (AttrSlot[])vals.get(null)) {
				if (name == slot.name) {
					this.attr_slot = slot;
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Attribute "+name+" not found in type "+intype.name);
	}
}
