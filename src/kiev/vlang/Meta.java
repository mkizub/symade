package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.TypeNameRef;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

// Meta information about a node
@node
public final class MetaSet extends ASTNode {
	
	@node
	public static final class MetaSetImpl extends NodeImpl {
		@att public NArr<Meta>			metas;
		public MetaSetImpl() {}
	}
	@nodeview
	public static final view MetaSetView of MetaSetImpl extends NodeView {
		public access:ro	NArr<Meta>			metas;
	}

	@att public abstract virtual access:ro NArr<Meta>			metas;
	
	public NodeView				getNodeView()			{ return new MetaSetView((MetaSetImpl)this.$v_impl); }
	public MetaSetView			getMetaSetView()		{ return new MetaSetView((MetaSetImpl)this.$v_impl); }

	@getter public NArr<Meta>		get$metas()		{ return this.getMetaSetView().metas; }

	public MetaSet() {
		super(new MetaSetImpl());
	}
	
	public void callbackChildChanged(AttrSlot attr) {
		if (parent != null && pslot != null) {
			if (attr.name == "metas") parent.callbackChildChanged(pslot);
		}
	}
	
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
			if (((BaseType)metas[i].type.getType()).clazz.name.name == name) {
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
			/// BUG BUG BUG ///
			public Object nextElement() {
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

@node
public class Meta extends ENode {
	public final static Meta[] emptyArray = new Meta[0];
	
	@node
	public static class MetaImpl extends ENodeImpl {
		@att public TypeRef					type;
		@att public NArr<MetaValue>			values;
		public MetaImpl() {}
	}
	@nodeview
	public static view MetaView of MetaImpl extends ENodeView {
		public				TypeRef					type;
		public access:ro	NArr<MetaValue>			values;
	}

	@att public abstract virtual			TypeRef					type;
	@att public abstract virtual access:ro NArr<MetaValue>			values;
	
	public NodeView				getNodeView()			{ return new MetaView((MetaImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new MetaView((MetaImpl)this.$v_impl); }
	public MetaView				getMetaView()			{ return new MetaView((MetaImpl)this.$v_impl); }

	@getter public TypeRef				get$type()		{ return this.getMetaView().type; }
	@getter public NArr<MetaValue>		get$values()	{ return this.getMetaView().values; }
	@setter public void		set$type(TypeRef val)		{ this.getMetaView().type = val; }

	public Meta() {
		super(new MetaImpl());
	}

	public Meta(TypeRef type) {
		super(new MetaImpl());
		this.type = type;
	}
	
	public static Meta newMeta(KString name)
		alias operator(210,lfy,new)
	{
		if (name == MetaVirtual.NAME)
			return new MetaVirtual(new TypeNameRef(name));
		if (name == MetaPacked.NAME)
			return new MetaPacked(new TypeNameRef(name));
		if (name == MetaPacker.NAME)
			return new MetaPacker(new TypeNameRef(name));
		return new Meta(new TypeNameRef(name));
	}

	public void callbackChildChanged(AttrSlot attr) {
		if (parent != null && pslot != null) {
			if      (attr.name == "type")
				parent.callbackChildChanged(pslot);
			else if (attr.name == "values")
				parent.callbackChildChanged(pslot);
		}
	}
	
	public int size() alias length {
		return values.length;
	}
	public boolean isEmpty() {
		return values.length == 0;
	}
	
	public void verify() {
		Type mt = type.getType();
		if (mt == null || !mt.isAnnotation()) {
			throw new CompilerException(this, "Annotation name expected");
		}
		KString name = ((BaseType)mt).clazz.name.name;
		Meta m = this;
		if (name == MetaVirtual.NAME    && !(this instanceof MetaVirtual))
			m = (Meta)this.copyTo(new MetaVirtual());
		if (name == MetaPacked.NAME     && !(this instanceof MetaPacked))
			m = (Meta)this.copyTo(new MetaPacked());
		if (name == MetaPacker.NAME     && !(this instanceof MetaPacker))
			m = (Meta)this.copyTo(new MetaPacker());
		if (name == MetaUnerasable.NAME && !(this instanceof MetaUnerasable))
			m = (Meta)this.copyTo(new MetaUnerasable());
		if (name == MetaForward.NAME    && !(this instanceof MetaForward))
			m = (Meta)this.copyTo(new MetaForward());
		if (m != this) {
			this.replaceWithNode(m);
			foreach (MetaValue v; values)
				m.set((MetaValue)v.copy());
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
			Type tp = m.type.ret;
			v.type.ret = tp;
			Type t = tp;
			if (t.isArray()) {
				ArrayType at = (ArrayType)t;
				if (v instanceof MetaValueScalar) {
					ENode val = ((MetaValueScalar)v).value;
					MetaValueArray mva = new MetaValueArray(v.type); 
					values[n] = v = mva;
					mva.values.add((ENode)~val);
				}
				
				t = at.arg;
			}
			if (t.isReference()) {
				t.checkResolved();
				if (!(t ≈ Type.tpString || t ≈ Type.tpClass || t.isAnnotation() || t.isEnum()))
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
				mvt.ret = m.type.ret;
				if (!m.type.ret.isArray()) {
					MetaValueScalar mvs = (MetaValueScalar)m.annotation_default;
					ENode v = (ENode)mvs.value.copy();
					values.append(new MetaValueScalar(mvt, v));
				} else {
					ENode[] arr = ((MetaValueArray)m.annotation_default).values.toArray();
					for(int j=0; j < arr.length; j++)
						arr[j] = (ENode)arr[j].copy();
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
			/// BUG BUG BUG ///
			public Object nextElement() {
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

@node
public abstract class MetaValue extends ASTNode {
	public final static MetaValue[] emptyArray = new MetaValue[0];

	@node
	public static abstract class MetaValueImpl extends NodeImpl {
		@att public MetaValueType			type;
		public MetaValueImpl() {}
	}
	@nodeview
	public static abstract view MetaValueView of MetaValueImpl extends NodeView {
		public MetaValueType			type;
	}

	@att public abstract virtual MetaValueType			type;
	
	public abstract MetaValueView		getMetaValueView();

	@getter public MetaValueType	get$type()					{ return this.getMetaValueView().type; }
	@setter public void				set$type(MetaValueType val)	{ this.getMetaValueView().type = val; }

	public MetaValue(MetaValueImpl v_impl) {
		super(v_impl);
	}

	public MetaValue(MetaValueImpl v_impl, MetaValueType type) {
		super(v_impl);
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
	public abstract boolean valueEquals(MetaValue mv);
}

@node
public final class MetaValueScalar extends MetaValue {

	@node
	public static final class MetaValueScalarImpl extends MetaValueImpl {
		@att public ENode			value;
		public MetaValueScalarImpl() {}
	}
	@nodeview
	public static final view MetaValueScalarView of MetaValueScalarImpl extends MetaValueView {
		public ENode			value;
	}

	@att public abstract virtual ENode			value;
	
	public NodeView				getNodeView()				{ return new MetaValueScalarView((MetaValueScalarImpl)this.$v_impl); }
	public MetaValueView		getMetaValueView()			{ return new MetaValueScalarView((MetaValueScalarImpl)this.$v_impl); }
	public MetaValueScalarView	getMetaValueScalarView()	{ return new MetaValueScalarView((MetaValueScalarImpl)this.$v_impl); }

	@getter public ENode		get$value()				{ return this.getMetaValueScalarView().value; }
	@setter public void			set$value(ENode val)	{ this.getMetaValueScalarView().value = val; }

	public MetaValueScalar() {
		super(new MetaValueScalarImpl());
	}

	public MetaValueScalar(MetaValueType type) {
		super(new MetaValueScalarImpl(),type);
	}

	public MetaValueScalar(MetaValueType type, ENode value) {
		super(new MetaValueScalarImpl(),type);
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
	public boolean valueEquals(MetaValue mv) {
		if (mv instanceof MetaValueScalar) {
			return this.value.valueEquals(mv.value);
		}
		return false;
	}
}

@node
public final class MetaValueArray extends MetaValue {

	@node
	public static final class MetaValueArrayImpl extends MetaValueImpl {
		@att public NArr<ENode>			values;
		public MetaValueArrayImpl() {}
	}
	@nodeview
	public static final view MetaValueArrayView of MetaValueArrayImpl extends MetaValueView {
		public access:ro	NArr<ENode>			values;
	}

	@att public abstract virtual access:ro NArr<ENode>			values;
	
	public NodeView				getNodeView()			{ return new MetaValueArrayView((MetaValueArrayImpl)this.$v_impl); }
	public MetaValueView		getMetaValueView()		{ return new MetaValueArrayView((MetaValueArrayImpl)this.$v_impl); }
	public MetaValueArrayView	getMetaValueArrayView()	{ return new MetaValueArrayView((MetaValueArrayImpl)this.$v_impl); }

	@getter public NArr<ENode>		get$values()	{ return this.getMetaValueArrayView().values; }

	public MetaValueArray() {
		super(new MetaValueArrayImpl());
	}

	public MetaValueArray(MetaValueType type) {
		super(new MetaValueArrayImpl(),type);
	}

	public MetaValueArray(MetaValueType type, ENode[] values) {
		super(new MetaValueArrayImpl(),type);
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


