/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
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

	@att
	public MNode[]		metas;

	// public just because of bit-mapped fields implementation
	public int			mflags;

	public @packed:3,mflags, 0 int     is_access;

	public @packed:1,mflags, 3 boolean is_static;
	public @packed:1,mflags, 4 boolean is_final;
	public @packed:1,mflags, 5 boolean is_mth_synchronized;	// method
	public @packed:1,mflags, 5 boolean is_struct_super;		// struct
	public @packed:1,mflags, 6 boolean is_fld_volatile;		// field
	public @packed:1,mflags, 6 boolean is_mth_bridge;		// method
	public @packed:1,mflags, 7 boolean is_fld_transient;	// field
	public @packed:1,mflags, 7 boolean is_mth_varargs;		// method
	public @packed:1,mflags, 8 boolean is_native;			// native method, backend operation/field/struct
	public @packed:1,mflags, 9 boolean is_struct_interface;
	public @packed:1,mflags,10 boolean is_abstract;
	public @packed:1,mflags,11 boolean is_math_strict;		// strict math
	public @packed:1,mflags,12 boolean is_synthetic;		// any decl that was generated (not in sources)
	public @packed:1,mflags,13 boolean is_struct_annotation;
	public @packed:1,mflags,14 boolean is_enum;				// struct/decl group/fields
		
	// Flags temporary used with java flags
	public @packed:1,mflags,16 boolean is_forward;			// var/field/method, type is wrapper
	public @packed:1,mflags,17 boolean is_virtual;			// var/field, method is 'static virtual', struct is 'view'
	public @packed:1,mflags,18 boolean is_type_unerasable;	// typedecl, method/struct as parent of typedef

	public @packed:1,mflags,19 boolean is_macro;			// macro-declarations for fields, methods, etc
	public @packed:1,mflags,20 boolean is_struct_singleton;
	
	public @packed:1,mflags,21 boolean is_tdecl_loaded;		// TypeDecl was fully loaded (from src or bytecode) 

	public @packed:1,mflags,22 boolean is_has_aliases;
	public @packed:1,mflags,23 boolean is_has_throws;

	
	@getter
	public DeclGroup get$group() {
		ANode p = parent();
		if (p instanceof DNode)
			return p.group;
		return null;
	}

	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if (attr.name == "metas") parent().callbackChildChanged(pslot());
		}
	}

	public MetaSet() {}
	
	public boolean hasRuntimeVisibles() {
		foreach (MNode m; metas; m.isRuntimeVisible())
			return true;
		if (group != null) {
			foreach (MNode m; group.meta.metas; m.isRuntimeVisible())
				return true;
		}
		return false;
	}
	public boolean hasRuntimeInvisibles() {
		foreach (MNode m; metas; m.isRuntimeInvisible())
			return true;
		if (group != null) {
			foreach (MNode m; group.meta.metas; m.isRuntimeInvisible())
				return true;
		}
		return false;
	}

	public void resolve() {
		if (group != null)
			foreach (MNode m; group.meta.metas)
				m.resolve(null);
		foreach (MNode m; metas)
			m.resolve(null);
	}

	public void verify() {
		foreach (MNode m; metas) {
			try {
				m.verify();
			} catch (CompilerException e) {
				Kiev.reportError(m, e);
				continue;
			}
		}
	}
	
	public MNode getMeta(String name) {
		int sz = metas.length;
		foreach (MNode m; metas) {
			if (m.qname() == name)
				return m;
		}
		if (group != null) {
			foreach (MNode m; group.meta.metas) {
				if (m.qname() == name)
					return m;
			}
		}
		return null;
	}
	
	public MNode setMeta(MNode meta)  alias add alias lfy operator +=
	{
		String qname = meta.qname();
		foreach (MNode m; metas) {
			if (m.qname() == qname) {
				if (meta != m)
					m.replaceWithNode(meta);
				return meta;
			}
		}
		metas.append(meta);
		return meta;
	}
}

@node
public abstract class MNode extends ASTNode {
	@virtual typedef This  ≤ MNode;
	
	public static final MNode[] emptyArray = new MNode[0];

	public abstract String qname();
	public abstract TypeDecl getTypeDecl();
	public void resolve(Type reqType) {}
	public void verify() {}
	public boolean isRuntimeVisible() { return false; }
	public boolean isRuntimeInvisible() { return false; }
}

@node(name="UserMeta")
public class UserMeta extends MNode {
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

	public final TypeDecl getTypeDecl() { return type.getType().meta_type.tdecl; }
	
	public boolean isRuntimeVisible() {
		TypeDecl tdecl = getTypeDecl();
		UserMeta retens = tdecl.getMeta("java.lang.annotation.Retention");
		if (retens == null)
			return false;
		MetaValue val = retens.get("value");
		if (val instanceof MetaValueScalar && val.value instanceof SFldExpr) {
			Field f = ((SFldExpr)val.value).var;
			if (f.u_name == "RUNTIME")
				return true;
		}
		return false;
	}

	public boolean isRuntimeInvisible() {
		TypeDecl tdecl = getTypeDecl();
		UserMeta retens = tdecl.getMeta("java.lang.annotation.Retention");
		if (retens == null)
			return true;
		MetaValue val = retens.get("value");
		if (val instanceof MetaValueScalar && val.value instanceof SFldExpr) {
			Field f = ((SFldExpr)val.value).var;
			if (f.u_name == "CLASS")
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
		String name = ((CompaundType)mt).tdecl.qname();
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
				if( sm.hasName(v.ident,true)) {
					m = sm;
					break;
				}
			}
			if (m == null)
				throw new CompilerException(v, "Unresolved method "+v.ident+" in class "+s);
			v = v.open();
			v.symbol = m;
			Type t = m.type.ret();
			if (t instanceof ArrayType) {
				if (v instanceof MetaValueScalar) {
					ASTNode val = ((MetaValueScalar)v).value;
					MetaValueArray mva = new MetaValueArray(new SymbolRef(v.pos,v.ident)); 
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
				if (values[j].symbol != null)
					continue next_method;
			}
			// value not specified - does the method has a default meta-value?
			if !(m.body instanceof MetaValue)
				Kiev.reportError(this, "Annotation value "+m.sname+" is not specified");
		}
	}
	
	public MetaValue get(String name) {
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident == name) {
				MetaValue v = values[i];
				return v;
			}
		}
		TypeDecl td = getType().meta_type.tdecl;
		foreach (Method m; td.members; m.hasName(name,true))
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
			if (values[i].ident == value.ident) {
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
			if (values[i].ident == name) {
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
			if (values[i].ident == name) {
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
			if (values[i].ident == name) {
				values[i].open();
				((MetaValueScalar)values[i]).value = new ConstStringExpr(val);
				return values[i];
			}
		}
		MetaValueScalar mv = new MetaValueScalar(new SymbolRef<DNode>(name), new ConstStringExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue unset(MetaValue value) alias del alias lfy operator -=
	{
		return unset(value.ident);
	}
	public MetaValue unset(String name) alias del alias lfy operator -=
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident == name) {
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
}

@node
public abstract class MetaValue extends ENode {
	public static final MetaValue[] emptyArray = new MetaValue[0];

	@virtual typedef This  ≤ MetaValue;

	public MetaValue() {}

	public MetaValue(SymbolRef<DNode> ident) {
		if (ident != null) {
			this.pos = ident.pos;
			this.ident = ident.name;
		}
	}

	public abstract boolean valueEquals(Object mv);

	public void verify() {
		if (parent() instanceof Method && pslot().name == "body") {
			Method m = (Method)parent();
			if (this.dnode != m) {
				this = this.open();
				this.symbol = m;
			}
		}
		else if (ident == null) {
			if (ident != "value") {
				this = this.open();
				this.ident = "value";
			}
		}
	}
	
	boolean checkValue(Type reqType, ASTNode value) {
		if (value instanceof TypeRef) {
			if (reqType ≈ Type.tpClass) {
				((TypeRef)value).getType();
				return false;
			} else {
				throw new CompilerException(this, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+
					value+" ("+value.getClass()+")");
			}
		}
		if !(value instanceof ENode)
			return false;
		ENode v = (ENode)value;
		if (v instanceof SFldExpr && ((SFldExpr)v).var.isEnumField()) {
			return false;
		}
		else if (!v.isConstantExpr())
			throw new CompilerException(this, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+v+" ("+v.getClass()+")");
		Type vt = value.getType();
		if (vt ≉ reqType) {
			v.replaceWith(fun ()->ASTNode {return new CastExpr(v.pos, reqType, v);});
			return true;
		}
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

	@att public ASTNode			value;

	public MetaValueScalar() {}

	public MetaValueScalar(SymbolRef<DNode> ident) {
		super(ident);
	}

	public MetaValueScalar(SymbolRef<DNode> ident, ASTNode value) {
		super(ident);
		this.value = value;
	}

	public boolean valueEquals(Object mv) {
		if (mv instanceof MetaValueScalar && this.ident == mv.ident) {
			ASTNode v1 = this.value;
			ASTNode v2 = mv.value;
			if (v1 instanceof ENode && v2 instanceof ENode)
				return v1.valueEquals(v2);
		}
		return false;
	}

	public void verify() {
		super.verify();
		if (value instanceof MNode)
			((MNode)value).verify();
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

	@att public ASTNode[]				values;

	public MetaValueArray() {}

	public MetaValueArray(SymbolRef<DNode> ident) {
		super(ident);
	}

	public MetaValueArray(SymbolRef<DNode> ident, ASTNode[] values) {
		super(ident);
		this.values.addAll(values);
	}

	public boolean valueEquals(Object mv) {
		if (mv instanceof MetaValueArray && this.ident == mv.ident) {
			MetaValueArray mva = (MetaValueArray)mv;
			if (values.length != mva.values.length)
				return false;
			for (int i=0; i < values.length; i++) {
				ASTNode v1 = values[i];
				ASTNode v2 = mva.values[i];
				if (v1 instanceof ENode && v2 instanceof ENode && !v1.valueEquals(v2))
					return false;
			}
			return true;
		}
		return false;
	}

	public void verify() {
		super.verify();
		for (int i=0; i < values.length; i++) {
			if (values[i] instanceof MNode)
				((MNode)values[i]).verify();
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


