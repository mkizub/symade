/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

// Meta information about a node
@node
public final class MetaSet extends ASTNode {
	@att private final NArr<Meta> metas;
	
	public MetaSet() {
		metas = new NArr<Meta>(this, new AttrSlot("metas", true, true));
	}
	
	public MetaSet(ASTNode owner) {
		super(0,owner);
		metas = new NArr<Meta>(this, new AttrSlot("metas", true, true));
	}
	
	public int size() alias length {
		return metas.length;
	}
	public boolean isEmpty() {
		return metas.length == 0;
	}
	
	public Meta get(KString name) {
		int sz = metas.length;
		for (int i=0; i < sz; i++) {
			if (metas[i].type.name == name)
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
		return unset(meta.type.name);
	}
	public Meta unset(KString name) alias del alias operator (5,lfy,-=)
	{
		if (name == null)
			throw new NullPointerException();
		int sz = metas.length;
		for (int i=0; i < sz; i++) {
			if (metas[i].type.name == name) {
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
			public A nextElement() {
				if ( current < MetaSet.this.size() ) return MetaSet.this.metas[current++];
				throw new NoSuchElementException(Integer.toString(MetaSet.this.size()));
			}
		};
	}
	
}

public class MetaType {
	public /*final*/ KString name;
	public MetaType(KString name) {
		this.name = name;
	}
	public KString signature() {
		return KString.from('L'+String.valueOf(name).replace('.','/')+';');
	}
}

public class MetaValueType {
	public KString name;
	public KString signature;
	public MetaValueType(KString name) {
		this.name = name;
	}
}

@node
public class Meta extends ASTNode {
	public final static Meta[] emptyArray = new Meta[0];
	
	public /*final*/  MetaType        type;
	@att public final NArr<MetaValue> values;
	
	public Meta() {
		values = new NArr<MetaValue>(this, new AttrSlot("values", true, true));
	}

	public Meta(MetaType type) {
		super(0);
		this.type = type;
		values = new NArr<MetaValue>(this, new AttrSlot("values", true, true));
	}

	public int size() alias length {
		return values.length;
	}
	public boolean isEmpty() {
		return values.length == 0;
	}
	
	public Meta resolve() {
		Struct s = Env.getStruct(type.name);
		for (int n=0; n < values.length; n++) {
			MetaValue v = values[n];
			Method m = null;
			for(int i=0; i < s.methods.length; i++) {
				if( s.methods[i].name.equals(v.type.name)) {
					m = s.methods[i];
					break;
				}
			}
			if (m == null)
				throw new CompilerException(v.pos, "Unresolved method "+v.type.name+" in class "+s);
			Type tp = m.type.ret;
			v.type.signature = tp.signature;
			Type t = tp;
			if (t.isArray()) {
				if (v instanceof MetaValueScalar) {
					ASTNode val = ((MetaValueScalar)v).value;
					MetaValueArray mva = new MetaValueArray(v.type); 
					values[n] = v = mva;
					mva.values.add(val);
				}
				
				t = t.args[0];
			}
			if (t.isReference()) {
				t.clazz.checkResolved();
				if (!(t == Type.tpString || t == Type.tpClass || t.clazz.isAnnotation() || t.clazz.isJavaEnum()))
					throw new CompilerException(m.pos, "Bad annotation value type "+tp);
			}
			v.resolve(t);
		}
		// check that all non-default values are specified, and add default values
	next_method:
		for(int i=0; i < s.methods.length; i++) {
			Method m = s.methods[i];
			for(int j=0; j < values.length; j++) {
				if (values[j].type.name == m.name.name)
					continue next_method;
			}
			// value not specified - does the method has a default meta-value?
			if (m.annotation_default != null) {
				MetaValueType mvt = new MetaValueType(m.name.name);
				mvt.signature = m.type.ret.signature;
				if (!m.type.ret.isArray()) {
					MetaValueScalar mvs = (MetaValueScalar)m.annotation_default;
					ASTNode v = (ASTNode)mvs.value.copy();
					values.append(new MetaValueScalar(mvt, v));
				} else {
					ASTNode[] arr = ((MetaValueArray)m.annotation_default).values.toArray();
					for(int j=0; j < arr.length; j++)
						arr[j] = (ASTNode)arr[j].copy();
					values.append(new MetaValueArray(mvt, arr));
				}
				continue;
			}
			throw new CompilerException(m.pos, "Annotation value "+m.name.name+" is not specified");
		}
		return this;
	}
	
	public MetaValue get(KString name) {
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type.name == name) {
				return values[i];
			}
		}
		throw new RuntimeException("Value "+name+" not found in "+type.name+" annotation");
	}
	
	public boolean getZ(KString name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return false;
		if (v instanceof ConstExpr && ((ConstExpr)v).value instanceof Boolean)
			return ((Boolean)((ConstExpr)v).value).booleanValue();
		if (v instanceof ConstBooleanExpr)
			return ((ConstBooleanExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+type.name+" is not a boolean constant, but "+v);
	}
	
	public MetaValue set(MetaValue value) alias add alias operator (5,lfy,+=)
	{
		if (value == null)
			throw new NullPointerException();
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type == value.type) {
				values[i] = value;
				return value;
			}
		}
		values.append(value);
		return value;
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
			public A nextElement() {
				if ( current < Meta.this.size() ) return Meta.this.values[current++];
				throw new NoSuchElementException(Integer.toString(Meta.this.size()));
			}
		};
	}

}

@node
public abstract class MetaValue extends ASTNode {
	public final static MetaValue[] emptyArray = new MetaValue[0];

	public /*final*/ MetaValueType type;

	public MetaValue() {
	}

	public MetaValue(MetaValueType type) {
		this.type  = type;
	}

	public abstract void resolve(Type reqType);
	
	ASTNode resolveValue(Type reqType, ASTNode value) {
		if (value instanceof Meta) {
			return ((Meta)value).resolve();
		}
		ASTNode v = ((Expr)value).resolve(reqType);
		if (!(v instanceof Expr)) {
			if (reqType == Type.tpClass)
				return new WrapedExpr(value.pos, v);
			else
				throw new CompilerException(pos, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+v+" ("+v.getClass()+")");
		}
		else if (v instanceof StaticFieldAccessExpr && ((StaticFieldAccessExpr)v).obj.isJavaEnum() && ((StaticFieldAccessExpr)v).var.isEnumField())
			return new WrapedExpr(value.pos, ((StaticFieldAccessExpr)v).var);
		else if (!((Expr)v).isConstantExpr())
			throw new CompilerException(pos, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+v+" ("+v.getClass()+")");
		Type vt = v.getType();
		if (vt != reqType) {
			if (!vt.isCastableTo(reqType))
				throw new CompilerException(pos, "Wrong annotation value type "+vt+", type "+reqType+" is expected");
			v = new CastExpr(v.pos, reqType, (Expr)v).resolve(reqType);
			if (!((Expr)v).isConstantExpr())
				throw new CompilerException(pos, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+v+" ("+v.getClass()+")");
			vt = v.getType();
			if (vt != reqType)
				throw new CompilerException(pos, "Wrong annotation value type "+vt+", type "+reqType+" is expected");
		}
		return v;
	}
}

@node
public class MetaValueScalar extends MetaValue {

	@att public       ASTNode       value;
	
	public MetaValueScalar() {
	}

	public MetaValueScalar(MetaValueType type) {
		super(type);
	}

	public MetaValueScalar(MetaValueType type, ASTNode value) {
		super(type);
		this.value = value;
	}

	public void resolve(Type reqType) {
		value = resolveValue(reqType, value);
	}
}

@node
public class MetaValueArray extends MetaValue {

	@att public final NArr<ASTNode>      values;
	
	public MetaValueArray() {
		values = new NArr<ASTNode>(this, new AttrSlot("values", true, true)); 
	}

	public MetaValueArray(MetaValueType type) {
		super(type);
		values = new NArr<ASTNode>(this, new AttrSlot("values", true, true)); 
	}

	public MetaValueArray(MetaValueType type, ASTNode[] values) {
		super(type);
		this.values.addAll(values);
	}

	public void resolve(Type reqType) {
		for (int i=0; i < values.length; i++)
			values[i] = resolveValue(reqType, values[i]);
	}
}


