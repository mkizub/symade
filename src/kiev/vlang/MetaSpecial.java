package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.TypeNameRef;
import kiev.parser.ASTIdentifier;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

@node
public class MetaVirtual extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.virtual");

	/** Getter/setter methods for this field */
	@ref public Method		get;
	@ref public Method		set;

	public MetaVirtual() {
		super(new TypeNameRef(NAME));
	}

	public MetaVirtual(TypeRef type) {
		super(type);
	}
	
}

@node
public class MetaPacked extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.packed");
	public static final KString nameSize   = KString.from("size");
	public static final KString nameOffset = KString.from("offset");
	public static final KString nameIn     = KString.from("in");

	@ref
	public Field			 packer;
	
	@virtual
	public virtual abstract int size;
	@virtual
	public virtual abstract int offset;
	@virtual
	public virtual abstract int in;

	public MetaPacked() {
		super(new TypeNameRef(NAME));
	}

	public MetaPacked(TypeRef type) {
		super(type);
	}
	
	@getter public int get$size() { return getI(nameSize); }
	@setter public void set$size(int val) { setI(nameSize, val); }
	@getter public int get$offset() { return getI(nameOffset); }
	@setter public void set$offset(int val) { setI(nameOffset, val); }
	@getter public KString get$fld() { return getS(nameIn); }
	@setter public void set$fld(KString val) { setS(nameIn, val); }
}

@node
public class MetaPacker extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.packer");
	public static final KString nameSize = KString.from("size");

	@virtual
	public virtual abstract int size;

	public MetaPacker() {
		super(new TypeNameRef(NAME));
	}

	public MetaPacker(TypeRef type) {
		super(type);
	}
	
	@getter public int get$size() { return getI(nameSize); }
	@setter public void set$size(int val) { setI(nameSize, val); }
}

@node
public class MetaAlias extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.alias");
	public static final KString VALUE = KString.from("value");

	public MetaAlias() {
		super(new TypeNameRef(NAME));
	}

	public MetaAlias(ConstStringExpr name) {
		super(new TypeNameRef(NAME));
		MetaValueType mvt = new MetaValueType(VALUE, new ArrayType(Type.tpString));
		MetaValueArray mv = new MetaValueArray(mvt, new ENode[]{name});
		set(mv);
	}

	public MetaAlias(TypeRef type) {
		super(type);
	}
	
	public ASTNode[] getAliases() {
		MetaValueArray mv = (MetaValueArray)get(VALUE);
		if (mv == null)
			return new ASTNode[0];
		return mv.values.toArray();
	}
}

@node
public class MetaThrows extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.throws");
	public static final KString VALUE = KString.from("value");

	public MetaThrows() {
		super(new TypeNameRef(NAME));
	}

	public MetaThrows(TypeRef type) {
		super(type);
	}
	
	public void add(NameRef thr) {
		if (size() == 0) {
			MetaValueType mvt = new MetaValueType(VALUE, new ArrayType(Type.tpClass));
			MetaValueArray mv = new MetaValueArray(mvt, new ENode[]{new TypeNameRef(thr)});
			set(mv);
		} else {
			MetaValueArray mv = (MetaValueArray)get(VALUE);
			mv.values.append(new TypeNameRef(thr));
		}
	}
	
	public ASTNode[] getThrowns() {
		MetaValueArray mv = (MetaValueArray)get(VALUE);
		if (mv == null)
			return new TypeRef[0];
		return mv.values.toArray();
	}
}

@node
public class MetaPizzaCase extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.pcase");
	public static final KString TAG = KString.from("tag");
	public static final KString FIELDS = KString.from("fields");

	public MetaPizzaCase() {
		this(new TypeNameRef(NAME));
	}

	public MetaPizzaCase(TypeRef type) {
		super(type);
		setI(TAG, 0);
		MetaValueType mvt = new MetaValueType(FIELDS, new ArrayType(Type.tpString));
		MetaValueArray mv = new MetaValueArray(mvt, ENode.emptyArray);
		set(mv);
	}
	
	public void add(Field f) {
		MetaValueArray mv = (MetaValueArray)get(FIELDS);
		mv.values.append(new ConstStringExpr(f.name.name));
	}
	
	public ENode[] getFields() {
		MetaValueArray mv = (MetaValueArray)get(FIELDS);
		if (mv == null)
			return ENode.emptyArray;
		return mv.values.toArray();
	}
	
	public int getTag() { return getI(TAG); }
	public void setTag(int tag) { setI(TAG, tag); }
}

@node
public class MetaUnerasable extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.unerasable");

	public MetaUnerasable() {
		super(new TypeNameRef(NAME));
	}

	public MetaUnerasable(TypeRef type) {
		super(type);
	}
}

@node
public class MetaSingleton extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.singleton");

	public MetaSingleton() {
		super(new TypeNameRef(NAME));
	}

	public MetaSingleton(TypeRef type) {
		super(type);
	}
}


