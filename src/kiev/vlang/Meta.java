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
import kiev.parser.ASTNonArrayType;

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
	}
	
	public MetaSet(ASTNode owner) {
		super(0,owner);
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
				Meta n = m.verify();
				if (n != m)
					set(n);
			} catch (CompilerException e) {
				Kiev.reportError(pos, e);
				continue;
			}
		}
	}
	
	public Meta get(KString name) {
		int sz = metas.length;
		for (int i=0; i < sz; i++) {
			if (metas[i].type.getType().clazz.name.name == name)
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
		return unset(meta.type.getType().clazz.name.name);
	}
	public Meta unset(KString name) alias del alias operator (5,lfy,-=)
	{
		if (name == null)
			throw new NullPointerException();
		int sz = metas.length;
		for (int i=0; i < sz; i++) {
			if (metas[i].type.getType().clazz.name.name == name) {
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

public class MetaValueType {
	public KString name;
	public KString signature;
	public MetaValueType(KString name) {
		this.name = name;
	}
	public MetaValueType(KString name, KString sign) {
		this.name = name;
		this.signature = sign;
	}
}

@node
public class Meta extends ASTNode {
	public final static Meta[] emptyArray = new Meta[0];
	
	@att public TypeRef					type;
	@att public final NArr<MetaValue>	values;
	
	public Meta() {
	}

	public Meta(TypeRef type) {
		this.type = type;
	}
	
	public static Meta newMeta(KString name)
		alias operator(210,lfy,new)
	{
		if (name == MetaVirtual.NAME)
			return new MetaVirtual(new ASTNonArrayType(name));
		if (name == MetaPacked.NAME)
			return new MetaPacked(new ASTNonArrayType(name));
		if (name == MetaPacker.NAME)
			return new MetaPacker(new ASTNonArrayType(name));
		return new Meta(new ASTNonArrayType(name));
	}

	public int size() alias length {
		return values.length;
	}
	public boolean isEmpty() {
		return values.length == 0;
	}
	
	public Meta verify() {
		Type mt = type.getType();
		if (mt == null || !mt.clazz.isAnnotation()) {
			throw new CompilerException(pos, "Annotation name expected");
		}
		Meta m = this;
		if (mt.clazz.name.name == MetaVirtual.NAME && !(this instanceof MetaVirtual))
			m = new MetaVirtual(new TypeRef(mt));
		if (mt.clazz.name.name == MetaPacked.NAME && !(this instanceof MetaPacked))
			m = new MetaPacked(new TypeRef(mt));
		if (mt.clazz.name.name == MetaPacker.NAME && !(this instanceof MetaPacker))
			m = new MetaPacker(new TypeRef(mt));
		if (m != this) {
			m.pos          = this.pos;
			m.flags        = this.flags;
			m.compileflags = this.compileflags;
			m.type         = this.type;
		}
		for (int i=0; i < values.length; i++) {
			MetaValue v1 = values[i];
			MetaValue v2 = v1.verify();
			if (v1 != v2)
				values[i] = v2;
		}
		return m;
	}
	
	public Meta resolve() {
		Struct s = (Struct)type.getType().clazz;
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
		foreach (ASTNode n; s.members; n instanceof Method) {
			Method m = (Method)n;
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
		if (v instanceof ConstExpr && ((ConstExpr)v).value instanceof Boolean)
			return ((Boolean)((ConstExpr)v).value).booleanValue();
		if (v instanceof ConstBooleanExpr)
			return ((ConstBooleanExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+type+" is not a boolean constant, but "+v);
	}
	
	public int getI(KString name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return 0;
		if (v instanceof ConstExpr && ((ConstExpr)v).value instanceof Integer)
			return ((Integer)((ConstExpr)v).value).intValue();
		throw new RuntimeException("Value "+name+" in annotation "+type+" is not an int constant, but "+v);
	}
	
	public KString getS(KString name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return null;
		if (v instanceof ConstExpr && ((ConstExpr)v).value instanceof KString)
			return (KString)((ConstExpr)v).value;
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
				((MetaValueScalar)values[i]).value = new ConstBooleanExpr(0, val);
				return values[i];
			}
		}
		MetaValueType mvt = new MetaValueType(name, Type.tpBoolean.signature);
		MetaValueScalar mv = new MetaValueScalar(mvt, new ConstBooleanExpr(0, val));
		values.append(mv);
		return mv;
	}

	public MetaValue setI(KString name, int val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type.name == name) {
				((MetaValueScalar)values[i]).value = new ConstExpr(0, new Integer(val));
				return values[i];
			}
		}
		MetaValueType mvt = new MetaValueType(name, Type.tpInt.signature);
		MetaValueScalar mv = new MetaValueScalar(mvt, new ConstExpr(0, new Integer(val)));
		values.append(mv);
		return mv;
	}

	public MetaValue setS(KString name, KString val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].type.name == name) {
				((MetaValueScalar)values[i]).value = new ConstExpr(0, val);
				return values[i];
			}
		}
		MetaValueType mvt = new MetaValueType(name, Type.tpString.signature);
		MetaValueScalar mv = new MetaValueScalar(mvt, new ConstExpr(0, val));
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
	
	public MetaValue verify() {
		if (type == null)
			type = new MetaValueType(KString.from("value"));
		return this;
	}
	
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
				throw new CompilerException(pos, "Wrong annotation value type "+vt+", type "+reqType+" is expected for value "+type.name);
			v = new CastExpr(v.pos, reqType, (Expr)v).resolve(reqType);
			if (!((Expr)v).isConstantExpr())
				throw new CompilerException(pos, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+v+" ("+v.getClass()+")");
			vt = v.getType();
			if (vt != reqType)
				throw new CompilerException(pos, "Wrong annotation value type "+vt+", type "+reqType+" is expected for value "+type.name);
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

	public MetaValue verify() {
		super.verify();
		if (value instanceof Meta)
			value = ((Meta)value).verify();
		return this;
	}
	
	public void resolve(Type reqType) {
		value = resolveValue(reqType, value);
	}
}

@node
public class MetaValueArray extends MetaValue {

	@att public final NArr<ASTNode>      values;
	
	public MetaValueArray() {
	}

	public MetaValueArray(MetaValueType type) {
		super(type);
	}

	public MetaValueArray(MetaValueType type, ASTNode[] values) {
		super(type);
		this.values.addAll(values);
	}

	public MetaValue verify() {
		super.verify();
		for (int i=0; i < values.length; i++) {
			if (values[i] instanceof Meta)
				values[i] = ((Meta)values[i]).verify();
		}
		return this;
	}
	
	public void resolve(Type reqType) {
		for (int i=0; i < values.length; i++)
			values[i] = resolveValue(reqType, values[i]);
	}
}


