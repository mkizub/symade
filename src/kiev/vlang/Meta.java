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
@node(name="MetaSet")
public final class MetaSet extends ASTNode {
	
	@virtual typedef This  = MetaSet;

	@att public ANode[]				metas;

	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if (attr.name == "metas") parent().callbackChildChanged(pslot());
		}
	}

	public MetaSet() {}
	
	public boolean hasRuntimeVisibles() {
		foreach (UserMeta m; metas; m.isRuntimeVisible())
			return true;
		return false;
	}
	public boolean hasRuntimeInvisibles() {
		foreach (UserMeta m; metas; m.isRuntimeInvisible())
			return true;
		return false;
	}

	public int size() alias length {
		return metas.length;
	}
	public boolean isEmpty() {
		return metas.length == 0;
	}
	
	public void verify() {
		foreach (UserMeta m; metas) {
			try {
				m.verify();
			} catch (CompilerException e) {
				Kiev.reportError(m, e);
				continue;
			}
		}
	}
	
	public MetaFlag getF(String name) {
		int sz = metas.length;
		foreach (MetaFlag m; metas) {
			if (m.qname() == name)
				return m;
		}
		return null;
	}
	
	public MetaFlag setF(MetaFlag meta) {
		String qname = meta.qname();
		foreach (MetaFlag m; metas) {
			if (m.qname() == qname) {
				if (meta != m)
					m.replaceWithNode(meta);
				return meta;
			}
		}
		metas.append(meta);
		return meta;
	}

	public UserMeta getU(String name) {
		int sz = metas.length;
		foreach (UserMeta m; metas) {
			if (m.qname() == name)
				return m;
		}
		return null;
	}
	
	public UserMeta setU(UserMeta meta) alias add alias operator (5,lfy,+=)
	{
		String qname = meta.qname();
		foreach (UserMeta m; metas) {
			if (m.qname() == qname) {
				if (meta != m)
					m.replaceWithNode(meta);
				return meta;
			}
		}
		metas.append(meta);
		return meta;
	}

	public Enumeration<ANode> elements() {
		return new Enumeration<ANode>() {
			int current;
			public boolean hasMoreElements() { return current < MetaSet.this.size(); }
			public ANode nextElement() {
				if ( current < MetaSet.this.size() ) return MetaSet.this.metas[current++];
				throw new NoSuchElementException(Integer.toString(MetaSet.this.size()));
			}
		};
	}
	
}

@node(name="UserMeta")
public class UserMeta extends ENode {
	@virtual typedef This  ≤ UserMeta;

	@att public TypeNameRef				type;
	@att public MetaValue[]				values;

	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if      (attr.name == "type")
				parent().callbackChildChanged(pslot());
			else if (attr.name == "values")
				parent().callbackChildChanged(pslot());
		}
	}

	public UserMeta() {}

	public UserMeta(TypeNameRef type) {
		this.type = type;
	}
	
	public UserMeta(String name) {
		this.type = new TypeNameRef(name);
	}
	
	public String qname() {
		return this.type.qname();
	}

	public TypeDecl getTypeDecl() { return type.getType().meta_type.tdecl; }
	
	public boolean isRuntimeVisible() {
		TypeDecl tdecl = getTypeDecl();
		UserMeta retens = tdecl.meta.getU("java.lang.annotation.Retention");
		if (retens == null)
			return false;
		MetaValue val = retens.get("value");
		if (val instanceof MetaValueScalar && val.value instanceof SFldExpr) {
			Field f = ((SFldExpr)val.value).var;
			if (f.id.uname == "RUNTIME")
				return true;
		}
		return false;
	}

	public boolean isRuntimeInvisible() {
		TypeDecl tdecl = getTypeDecl();
		UserMeta retens = tdecl.meta.getU("java.lang.annotation.Retention");
		if (retens == null)
			return true;
		MetaValue val = retens.get("value");
		if (val instanceof MetaValueScalar && val.value instanceof SFldExpr) {
			Field f = ((SFldExpr)val.value).var;
			if (f.id.uname == "CLASS")
				return true;
		}
		return false;
	}

	public Type getType() { return type.getType(); }
	
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
		String name = ((CompaundType)mt).clazz.qname();
		UserMeta m = this;
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
	
	public void resolve(Type reqType) {
		Struct s = type.getType().getStruct();
		s.checkResolved();
		for (int n=0; n < values.length; n++) {
			MetaValue v = values[n];
			Method m = null;
			foreach (Method sm; s.members) {
				if( sm.id.equals(v.ident.name)) {
					m = sm;
					break;
				}
			}
			if (m == null)
				throw new CompilerException(v, "Unresolved method "+v.ident+" in class "+s);
			v.ident.symbol = m;
			Type t = m.type.ret();
			if (t instanceof ArrayType) {
				if (v instanceof MetaValueScalar) {
					ENode val = ((MetaValueScalar)v).value;
					MetaValueArray mva = new MetaValueArray(~v.ident); 
					mva.values.add(~val);
					values[n] = v = mva;
				}
				t = t.arg;
			}
			if (t.isReference()) {
				t.checkResolved();
				if (t.getStruct() == null || !(t ≈ Type.tpString || t ≈ Type.tpClass || t.getStruct().isAnnotation() || t.getStruct().isEnum()))
					throw new CompilerException(m, "Bad annotation value type "+t);
			}
			v.resolve(t);
		}
		// check that all non-default values are specified, and add default values
	next_method:
		foreach (Method m; s.members) {
			for(int j=0; j < values.length; j++) {
				if (values[j].ident.symbol != null)
					continue next_method;
			}
			// value not specified - does the method has a default meta-value?
			if !(m.body instanceof MetaValue)
				Kiev.reportError(this, "Annotation value "+m.id+" is not specified");
		}
	}
	
	public MetaValue get(String name) {
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident.name == name) {
				MetaValue v = values[i];
				return v;
			}
		}
		TypeDecl td = getType().meta_type.tdecl;
		foreach (Method m; td.members; m.id.equals(name))
			return (MetaValue)m.body;
		throw new RuntimeException("Value "+name+" not found in "+type+" annotation");
	}
	
	public boolean getZ(String name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return false;
		if (v instanceof ConstBoolExpr)
			return ((ConstBoolExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+type+" is not a boolean constant, but "+v);
	}
	
	public int getI(String name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return 0;
		if (v instanceof ConstIntExpr)
			return ((ConstIntExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+type+" is not an int constant, but "+v);
	}
	
	public String getS(String name) {
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
			if (values[i].ident.name == value.ident.name) {
				values[i] = value;
				return value;
			}
		}
		values.append(value);
		return value;
	}

	public MetaValue setZ(String name, boolean val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident.name == name) {
				values[i].open();
				((MetaValueScalar)values[i]).value = new ConstBoolExpr(val);
				return values[i];
			}
		}
		MetaValueScalar mv = new MetaValueScalar(new SymbolRef<DNode>(name), new ConstBoolExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue setI(String name, int val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident.name == name) {
				values[i].open();
				((MetaValueScalar)values[i]).value = new ConstIntExpr(val);
				return values[i];
			}
		}
		MetaValueScalar mv = new MetaValueScalar(new SymbolRef<DNode>(name), new ConstIntExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue setS(String name, String val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident.name == name) {
				values[i].open();
				((MetaValueScalar)values[i]).value = new ConstStringExpr(val);
				return values[i];
			}
		}
		MetaValueScalar mv = new MetaValueScalar(new SymbolRef<DNode>(name), new ConstStringExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue unset(MetaValue value) alias del alias operator (5,lfy,-=)
	{
		return unset(value.ident.name);
	}
	public MetaValue unset(String name) alias del alias operator (5,lfy,-=)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident.name == name) {
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
			public boolean hasMoreElements() { return current < UserMeta.this.size(); }
			public MetaValue nextElement() {
				if ( current < UserMeta.this.size() ) return UserMeta.this.values[current++];
				throw new NoSuchElementException(Integer.toString(UserMeta.this.size()));
			}
		};
	}
}

@node
public abstract class MetaValue extends ENode {
	public final static MetaValue[] emptyArray = new MetaValue[0];

	@virtual typedef This  ≤ MetaValue;

	public MetaValue() {}

	public MetaValue(SymbolRef<DNode> ident) {
		this.ident  = ident;
	}

	public abstract boolean valueEquals(MetaValue mv);

	public void verify() {
		if (parent() instanceof Method && pslot().name == "body") {
			Method m = (Method)parent();
			ident = new SymbolRef<DNode>(pos, m);
		}
		else if (ident.name == null) {
			ident.name = "value";
		}
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
			throw new CompilerException(this, "Wrong annotation value type "+vt+", type "+reqType+" is expected for value "+ident);
		return false;
	}
}

@node(name="MetaVal")
public final class MetaValueScalar extends MetaValue {

	@virtual typedef This  = MetaValueScalar;

	@att public ENode			value;

	public MetaValueScalar() {}

	public MetaValueScalar(SymbolRef<DNode> ident) {
		super(ident);
	}

	public MetaValueScalar(SymbolRef<DNode> ident, ENode value) {
		super(ident);
		this.value = value;
	}

	public boolean valueEquals(MetaValue mv) {
		if (mv instanceof MetaValueScalar) {
			return this.value.valueEquals(mv.value);
		}
		return false;
	}

	public void verify() {
		super.verify();
		if (value instanceof UserMeta)
			((UserMeta)value).verify();
	}
	
	public void resolve(Type reqType) {
		boolean ok;
		do {
			ok = true;
			try {
				Kiev.runFrontEndProcessorsOn(value);
			} catch (ReWalkNodeException e) { ok = false; }
		} while (ok && checkValue(reqType, value));
	}
}

@node(name="MetaArr")
public final class MetaValueArray extends MetaValue {

	@virtual typedef This  = MetaValueArray;

	@att public ENode[]				values;

	public MetaValueArray() {}

	public MetaValueArray(SymbolRef<DNode> ident) {
		super(ident);
	}

	public MetaValueArray(SymbolRef<DNode> ident, ENode[] values) {
		super(ident);
		this.values.addAll(values);
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

	public void verify() {
		super.verify();
		for (int i=0; i < values.length; i++) {
			if (values[i] instanceof UserMeta)
				((UserMeta)values[i]).verify();
		}
	}
	
	public void resolve(Type reqType) {
		for (int i=0; i < values.length; i++) {
			boolean ok;
			do {
				ok = true;
				try {
					Kiev.runFrontEndProcessorsOn(this.values[i]);
				} catch (ReWalkNodeException e) { ok = false; }
			} while (ok && checkValue(reqType, this.values[i]));
		}
	}
}


