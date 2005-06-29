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
public final class MetaSet extends ASTNode {
	private final ASTNode owner;
	private Meta[] metas = Meta.emptyArray;
	
	public MetaSet(ASTNode owner) {
		super(0,owner);
		this.owner = owner;
	}
	
	public int size() alias length {
		return metas.length;
	}
	public boolean isEmpty() {
		return metas.length == 0;
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
		Meta[] tmp = new Meta[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = metas[i];
		metas = tmp;
		metas[sz] = meta;
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
				if (sz == 1) {
					metas = Meta.emptyArray;
				} else {
					Meta[] tmp = new Meta[sz-1];
					int k;
					for (k=0; k < i; k++)
						tmp[k] = metas[k];
					for (k++; k < sz; k++)
						tmp[k-1] = metas[k];
					metas = tmp;
				}
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
	public final KString name;
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
	public MetaValue default_value;
	public MetaValueType(KString name) {
		this.name = name;
	}
}

public class Meta extends ASTNode {
	public final static Meta[] emptyArray = new Meta[0];
	
	public final MetaType    type;
	public       MetaValue[] values = MetaValue.emptyArray;
	
	public Meta(MetaType type) {
		super(0);
		this.type = type;
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
			Type t = tp;
			if (t.isArray()) {
				if (v instanceof MetaValueScalar)
					values[n] = v = new MetaValueArray(v.type, new ASTNode[]{((MetaValueScalar)v).value});
				t = t.args[0];
			}
			if (t.isReference()) {
				t.clazz.checkResolved();
				if (!(t == Type.tpString || t == Type.tpClass || t.clazz.isAnnotation() || t.clazz.isJavaEnum()))
					throw new CompilerException(m.pos, "Bad annotation value type "+tp);
			}
			v.resolve(t);
		}
		return this;
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
		MetaValue[] tmp = new MetaValue[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = values[i];
		values = tmp;
		values[sz] = value;
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
				if (sz == 1) {
					values = MetaValue.emptyArray;
				} else {
					MetaValue[] tmp = new MetaValue[sz-1];
					int k;
					for (k=0; k < i; k++)
						tmp[k] = values[k];
					for (k++; k < sz; k++)
						tmp[k-1] = values[k];
					values = tmp;
				}
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

public abstract class MetaValue extends ASTNode {
	public final static MetaValue[] emptyArray = new MetaValue[0];

	public final MetaValueType type;

	public MetaValue(MetaValueType type) {
		super(0);
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
				return v;
			else
				throw new CompilerException(pos, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+v+" ("+v.getClass()+")");
		}
		else if (v instanceof StaticFieldAccessExpr && ((StaticFieldAccessExpr)v).obj.isJavaEnum() && ((StaticFieldAccessExpr)v).var.isEnumField())
			return ((StaticFieldAccessExpr)v).var;
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

public class MetaValueScalar extends MetaValue {

	public       ASTNode       value;
	
	public MetaValueScalar(MetaValueType type, ASTNode value) {
		super(type);
		this.value = value;
	}

	public void resolve(Type reqType) {
		value = resolveValue(reqType, value);
	}
}

public class MetaValueArray extends MetaValue {

	public final MetaValueType type;
	public       ASTNode[]     values;
	
	public MetaValueArray(MetaValueType type, ASTNode[] values) {
		super(type);
		this.values = values;
	}

	public void resolve(Type reqType) {
		for (int i=0; i < values.length; i++)
			values[i] = resolveValue(reqType, values[i]);
	}
}


