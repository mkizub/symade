package kiev.dump.bin;

import kiev.stdlib.TypeInfo;
import kiev.vlang.Env;

public class TypeElem extends Elem {

	public static final int IS_SPECIAL		= 1 << 0;	// this type is a special type or a type for primitive values
	public static final int IS_ENUM		= 1 << 1;	// this type is enumeration, it's attributes are enum values
	
	public static final TypeElem teBOOL	= new TypeElem('B', IS_SPECIAL, "<BOOL>");
	public static final TypeElem teINT		= new TypeElem('I', IS_SPECIAL, "<INT>");
	public static final TypeElem teFLOAT	= new TypeElem('F', IS_SPECIAL, "<FLOAT>");
	public static final TypeElem teCHAR	= new TypeElem('C', IS_SPECIAL, "<CHAR>");
	public static final TypeElem teSTRING	= new TypeElem('S', IS_SPECIAL, "<STRING>");
	public static final TypeElem teDATA	= new TypeElem('D', IS_SPECIAL, "<DATA>");
	public static final TypeElem teSYMBOL= new TypeElem('Y', IS_SPECIAL, "<SYMBOL>");
	public static final TypeElem teSYMREF	= new TypeElem('R', IS_SPECIAL, "<SYMREF>");
	public static final TypeElem tePARENT= new TypeElem('P', IS_SPECIAL, "<PARENT>");
	
	public static final TypeElem[] SPACIAL_TYPES = {
		teBOOL, teINT, teFLOAT, teCHAR, teSTRING, teDATA, teSYMBOL, teSYMREF, tePARENT
	};
	
	// qualified type name
	public String			name;
	// super-types
	public TypeElem[]		super_types;
	// attributes
	public AttrElem[]		attrs;
	// constants
	public ConstElem[]		consts;
	
	// type info
	public TypeInfo			typeinfo;
	// leading arguments (to create the node when all leading args are read)
	public int				leading_attrs;

	public TypeElem(int id, int flags, String name) {
		super(id);
		this.flags = flags;
		this.name = name;
		this.super_types = new TypeElem[0];
		this.attrs = new AttrElem[0];
		this.consts = new ConstElem[0];
	}

	public TypeElem(int id, int addr) {
		super(id, addr);
		this.super_types = new TypeElem[0];
		this.attrs = new AttrElem[0];
		this.consts = new ConstElem[0];
	}
	
	public boolean isSpecial() {
		return (flags & IS_SPECIAL) != 0;
	}
	
	public boolean isEnum() {
		return (flags & IS_ENUM) != 0;
	}
	
	public void build(Env env) {
		//this.typeinfo = TypeInfo.newTypeInfo(this.name.replace('·', '.'));
		for (AttrElem ae : attrs) {
			ae.intype = this;
			ae.build(env);
		}
		for (int i=0; i < attrs.length; i++) {
			if (!attrs[i].isLeading())
				break;
			leading_attrs = i+1;
		}
	}
}
