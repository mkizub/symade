package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

// Meta information about a node
@nodeset
public final class MetaSet extends ASTNode {
	
	@virtual typedef This  = MetaSet;
	@virtual typedef VView = MetaSetView;

	@att public NArr<Meta>			metas;

	public void callbackChildChanged(AttrSlot attr) {
		if (parent != null && pslot != null) {
			if (attr.name == "metas") parent.callbackChildChanged(pslot);
		}
	}

	@nodeview
	public static final view MetaSetView of MetaSet extends NodeView {
		public:ro	NArr<Meta>			metas;
	}

	public MetaSet() {}
	
	public int size() alias length {
		return metas.length;
	}
	public boolean isEmpty() {
		return metas.length == 0;
	}
	
	public void verify() {
		foreach (Meta m; metas) {
			try {
				m.verify();
			} catch (CompilerException e) {
				Kiev.reportError(m, e);
				continue;
			}
		}
	}
	
	public Meta get(KString name) {
		int sz = metas.length;
		for (int i=0; i < sz; i++) {
			if (metas[i].type.getType().getStruct().name.name == name)
				return metas[i];
		}
		return null;
	}
	
	public Meta set(Meta meta) alias add alias operator (5,lfy,+=)
	{
		if (meta == null)
			throw new NullPointerException();
		int sz = metas.length;
		for (int i=0; i < sz; i++) {
			if (metas[i].type == meta.type) {
				metas[i] = meta;
				return meta;
			}
		}
		metas.append(meta);
		return meta;
	}

	public Meta unset(Meta meta) alias del alias operator (5,lfy,-=)
	{
		return unset(meta.type.getType().getStruct().name.name);
	}
	public Meta unset(KString name) alias del alias operator (5,lfy,-=)
	{
		if (name == null)
			throw new NullPointerException();
		int sz = metas.length;
		for (int i=0; i < sz; i++) {
			if (((CompaundType)metas[i].type.getType()).clazz.name.name == name) {
				Meta m = metas[i];
				metas.del(i);
				return m;
			}
		}
		return null;
	}

	public boolean contains(Meta meta) {
		for (int i = 0 ; i >= 0 ; i--) {
			if (metas[i].equals(meta))
				return true;
		}
		return false;
	}

	public Enumeration<Meta> elements() {
		return new Enumeration<Meta>() {
			int current;
			public boolean hasMoreElements() { return current < MetaSet.this.size(); }
			public Meta nextElement() {
				if ( current < MetaSet.this.size() ) return MetaSet.this.metas[current++];
				throw new NoSuchElementException(Integer.toString(MetaSet.this.size()));
			}
		};
	}
	
}

public final class MetaValueType {
	public KString	name;
	public Type		ret;
	public MetaValueType(KString name) {
		this.name = name;
	}
	public MetaValueType(KString name, Type ret) {
		this.name = name;
		this.ret = ret;
	}
}

@nodeset
public class Meta extends ENode {
	public final static Meta[] emptyArray = new Meta[0];
	
	public static final Meta dummyNode = new Meta();
	
	@virtual typedef This  = Meta;
	@virtual typedef VView = MetaView;

	@att public TypeRef					type;
	@att public NArr<MetaValue>			values;

	public void callbackChildChanged(AttrSlot attr) {
		if (parent != null && pslot != null) {
			if      (attr.name == "type")
				parent.callbackChildChanged(pslot);
			else if (attr.name == "values")
				parent.callbackChildChanged(pslot);
		}
	}

	@nodeview
	public static view MetaView of Meta extends ENodeView {
		public		TypeRef					type;
		public:ro	NArr<MetaValue>			values;
	}

	public Meta() {}

	public Meta(TypeRef type) {
		this.type = type;
	}
	
	public static Meta newMeta(KString name)
		alias operator(210,lfy,new)
	{
		return new Meta(new TypeNameRef(name));
	}
	
	public ASTNode getDummyNode() {
		return Meta.dummyNode;
	}
	
	public int size() alias length {
		return values.length;
	}
	public boolean isEmpty() {
		return values.length == 0;
	}
	
	public void verify() {
		Type mt = type.getType();
		if (mt == null || mt.getStruct() == null || !mt.getStruct().isAnnotation()) {
			throw new CompilerException(this, "Annotation name expected");
		}
		KString name = ((CompaundType)mt).clazz.name.name;
		Meta m = this;
		if (m != this) {
			this.replaceWithNode(m);
			foreach (MetaValue v; values)
				m.set(v.ncopy());
			m.verify();
		}
		foreach (MetaValue v; values)
			v.verify();
		return;
	}
	
	public Meta resolve() {
		Struct s = type.getType().getStruct();
		s.checkResolved();
		for (int n=0; n < values.length; n++) {
			MetaValue v = values[n];
			Method m = null;
			foreach (ASTNode sn; s.members; sn instanceof Method) {
				Method sm = (Method)sn;
				if( sm.name.equals(v.type.name)) {
					m = sm;
					break;
				}
			}
			if (m == null)
				throw new CompilerException(v, "Unresolved method "+v.type.name+" in class "+s);
			Type tp = m.type.ret();
			v.type.ret = tp;
			Type t = tp;
			if (t.isArray()) {
				ArrayType at = (ArrayType)t;
				if (v instanceof MetaValueScalar) {
					ENode val = ((MetaValueScalar)v).value;
					MetaValueArray mva = new MetaValueArray(v.type); 
					values[n] = v = mva;
					mva.values.add(~val);
				}
				
				t = at.arg;
			}
			if (t.isReference()) {
				t.checkResolved();
				if (t.getStruct() == null || !(t ≈ Type.tpString || t ≈ Type.tpClass || t.getStruct().isAnnotation() || t.getStruct().isEnum()))
					throw new CompilerException(m, "Bad annotation value type "+tp);
			}
			v.resolve(t);
		}
		// check that all non-default values are specified, and add default values
	next_method:
		foreach (ASTNode n; s.members; n instanceof Method) {
			Method m = (Method)n;
			for(int j=0; j < values.length; j++) {
				if (values[j].type.name == m.name.name)
					continue next_method;
			}
			// value not specified - does the method has a default meta-value?
			if (m.annotation_default != null) {
				MetaValueType mvt = new MetaValueType(m.name.name);
				mvt.ret = m.type.ret();
				if (!m.type.ret().isArray()) {
					MetaValueScalar mvs = (MetaValueScalar)m.annotation_default;
					ENode v = mvs.value.ncopy();
					values.append(new MetaValueScalar(mvt, v));
				} else {
					ENode[] arr = ((MetaValueArray)m.annotation_default).values.toArray();
					for(int j=0; j < arr.length; j++)
						arr[j] = arr[j].ncopy();
					values.append(new MetaValueArray(mvt, arr));
				}
				continue;
			}
			throw new CompilerException(m, "Annotation value "+m.name.name+" is not specified");
		}
		return this;
	}
	
	public MetaValue get(KString name) {
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type.name == name) {
				MetaValue v = values[i];
				return v;
			}
		}
		throw new RuntimeException("Value "+name+" not found in "+type+" annotation");
	}
	
	public boolean getZ(KString name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return false;
		if (v instanceof ConstBoolExpr)
			return ((ConstBoolExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+type+" is not a boolean constant, but "+v);
	}
	
	public int getI(KString name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return 0;
		if (v instanceof ConstIntExpr)
			return ((ConstIntExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+type+" is not an int constant, but "+v);
	}
	
	public KString getS(KString name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return null;
		if (v instanceof ConstStringExpr)
			return ((ConstStringExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+type+" is not a String constant, but "+v);
	}
	
	public MetaValue set(MetaValue value)
	{
		if (value == null)
			throw new NullPointerException();
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type.name == value.type.name) {
				values[i] = value;
				return value;
			}
		}
		values.append(value);
		return value;
	}

	public MetaValue setZ(KString name, boolean val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type.name == name) {
				((MetaValueScalar)values[i]).value = new ConstBoolExpr(val);
				return values[i];
			}
		}
		MetaValueType mvt = new MetaValueType(name, Type.tpBoolean);
		MetaValueScalar mv = new MetaValueScalar(mvt, new ConstBoolExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue setI(KString name, int val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type.name == name) {
				((MetaValueScalar)values[i]).value = new ConstIntExpr(val);
				return values[i];
			}
		}
		MetaValueType mvt = new MetaValueType(name, Type.tpInt);
		MetaValueScalar mv = new MetaValueScalar(mvt, new ConstIntExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue setS(KString name, KString val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type.name == name) {
				((MetaValueScalar)values[i]).value = new ConstStringExpr(val);
				return values[i];
			}
		}
		MetaValueType mvt = new MetaValueType(name, Type.tpString);
		MetaValueScalar mv = new MetaValueScalar(mvt, new ConstStringExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue unset(MetaValue value) alias del alias operator (5,lfy,-=)
	{
		return unset(value.type.name);
	}
	public MetaValue unset(KString name) alias del alias operator (5,lfy,-=)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type.name == name) {
				MetaValue v = values[i];
				values.del(i);
				return v;
			}
		}
		return null;
	}

	public boolean contains(MetaValue value) {
		for (int i = 0 ; i >= 0 ; i--) {
			if (values[i].equals(value))
				return true;
		}
		return false;
	}

	public Enumeration<MetaValue> elements() {
		return new Enumeration<MetaValue>() {
			int current;
			public boolean hasMoreElements() { return current < Meta.this.size(); }
			public MetaValue nextElement() {
				if ( current < Meta.this.size() ) return Meta.this.values[current++];
				throw new NoSuchElementException(Integer.toString(Meta.this.size()));
			}
		};
	}

	public Dumper toJavaDecl(Dumper dmp) {
		return this.toJava(dmp).newLine();
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append('@').append(type.getType().getStruct().name.short_name);
		boolean need_lp = true;
		boolean need_comma = false;
		if (values.length != 0) {
			Struct s = type.getType().getStruct();
			s.checkResolved();
			foreach (ASTNode n; s.members; n instanceof Method) {
				Method m = (Method)n;
				MetaValue v = get(m.name.name);
				if (v.valueEquals(m.annotation_default))
					continue;
				if (need_lp) {
					dmp.append('(');
					need_lp = false;
				}
				else if (need_comma) {
					dmp.append(',');
				}
				dmp.append(v.type.name).append('=');
				v.toJava(dmp);
				need_comma = true;
			}
			if (!need_lp)
				dmp.append(')');
		}
		return dmp;
	}
}

@nodeset
public abstract class MetaValue extends ASTNode {
	public final static MetaValue[] emptyArray = new MetaValue[0];

	@virtual typedef This  = MetaValue;
	@virtual typedef VView = MetaValueView;

	@att public MetaValueType			type;

	@nodeview
	public static abstract view MetaValueView of MetaValue extends NodeView {
		public MetaValueType			type;
		public abstract boolean valueEquals(MetaValue mv);
	}

	public MetaValue() {}

	public MetaValue(MetaValueType type) {
		this.type  = type;
	}

	public abstract void resolve(Type reqType);
	
	public void verify() {
		if (type == null)
			type = new MetaValueType(KString.from("value"));
	}
	
	void resolveValue(Type reqType, ENode value) {
		if (value instanceof Meta) {
			((Meta)value).resolve();
			return;
		}
		value.resolve(reqType);
	}

	boolean checkValue(Type reqType, ENode value) {
		if (value instanceof TypeRef) {
			if (reqType ≈ Type.tpClass) {
				((TypeRef)value).getType();
				return false;
			} else {
				throw new CompilerException(this, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+
					value+" ("+value.getClass()+")");
			}
		}
		ENode v = value;
		if (v instanceof SFldExpr && ((SFldExpr)v).var.isEnumField()) {
			return false;
		}
		else if (!v.isConstantExpr())
			throw new CompilerException(this, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+v+" ("+v.getClass()+")");
		Type vt = value.getType();
		if (vt ≉ reqType) {
			v.replaceWith(fun ()->ENode {return new CastExpr(v.pos, reqType, v);});
			return true;
		}
		ENode v = value;
		if (!v.isConstantExpr())
			throw new CompilerException(this, "Annotation value must be a constant, but found "+v+" ("+v.getClass()+")");
		Type vt = v.getType();
		if (vt ≉ reqType)
			throw new CompilerException(this, "Wrong annotation value type "+vt+", type "+reqType+" is expected for value "+type.name);
		return false;
	}

	public abstract Dumper toJavaDecl(Dumper dmp);
	public abstract Dumper toJava(Dumper dmp);
}

@nodeset
public final class MetaValueScalar extends MetaValue {

	@virtual typedef This  = MetaValueScalar;
	@virtual typedef VView = MetaValueScalarView;

	@att public ENode			value;

	@nodeview
	public static final view MetaValueScalarView of MetaValueScalar extends MetaValueView {
		public ENode			value;

		public boolean valueEquals(MetaValue mv) {
			if (mv instanceof MetaValueScalar) {
				return this.value.valueEquals(mv.value);
			}
			return false;
		}
	}

	public MetaValueScalar() {}

	public MetaValueScalar(MetaValueType type) {
		super(type);
	}

	public MetaValueScalar(MetaValueType type, ENode value) {
		super(type);
		this.value = value;
	}

	public void verify() {
		super.verify();
		if (value instanceof Meta)
			((Meta)value).verify();
	}
	
	public void resolve(Type reqType) {
		resolveValue(reqType, this.value);
		while (checkValue(reqType, this.value))
			resolveValue(reqType, this.value);
	}

	public Dumper toJavaDecl(Dumper dmp) {
		return value.toJava(dmp);
	}
	public Dumper toJava(Dumper dmp) {
		return value.toJava(dmp);
	}
}

@nodeset
public final class MetaValueArray extends MetaValue {

	@virtual typedef This  = MetaValueArray;
	@virtual typedef VView = MetaValueArrayView;

	@att public NArr<ENode>			values;

	@nodeview
	public static final view MetaValueArrayView of MetaValueArray extends MetaValueView {
		public:ro	NArr<ENode>			values;

		public boolean valueEquals(MetaValue mv) {
			if (mv instanceof MetaValueArray) {
				MetaValueArray mva = (MetaValueArray)mv;
				if (values.length != mva.values.length)
					return false;
				for (int i=0; i < values.length; i++) {
					if (!values[i].valueEquals(mva.values[i]))
						return false;
				}
				return true;
			}
			return false;
		}
	}

	public MetaValueArray() {}

	public MetaValueArray(MetaValueType type) {
		super(type);
	}

	public MetaValueArray(MetaValueType type, ENode[] values) {
		super(type);
		this.values.addAll(values);
	}

	public void verify() {
		super.verify();
		for (int i=0; i < values.length; i++) {
			if (values[i] instanceof Meta)
				((Meta)values[i]).verify();
		}
	}
	
	public void resolve(Type reqType) {
		for (int i=0; i < values.length; i++) {
			resolveValue(reqType, this.values[i]);
			while (checkValue(reqType, this.values[i]))
				resolveValue(reqType, this.values[i]);
		}
	}

	public Dumper toJavaDecl(Dumper dmp) {
		return toJava(dmp);
	}
	public Dumper toJava(Dumper dmp) {
		dmp.append('{');
		for (int i=0; i < values.length; i++) {
			ENode v = values[i];
			v.toJava(dmp);
			if (i < values.length-1)
				dmp.append(',');
		}
		dmp.append('}');
		return dmp;
	}
}


