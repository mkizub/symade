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
	
	public @packed:1,mflags,21 boolean is_tdecl_not_loaded;		// TypeDecl was fully loaded (from src or bytecode) 

	public @packed:1,mflags,22 boolean is_has_aliases;
	public @packed:1,mflags,23 boolean is_has_throws;
	public @packed:1,mflags,24 boolean is_has_uuid;

	
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
			if (m.qname == name)
				return m;
		}
		if (group != null) {
			foreach (MNode m; group.meta.metas) {
				if (m.qname == name)
					return m;
			}
		}
		return null;
	}
	
	public MNode setMeta(MNode meta)  alias add alias lfy operator +=
	{
		String qname = meta.qname;
		foreach (MNode m; metas) {
			if (m.qname == qname) {
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

	@getter
	public abstract String get$qname();
	public abstract TypeDecl getTypeDecl();
	public void resolve(Type reqType) {}
	public void verify() {}
	public boolean isRuntimeVisible() { return false; }
	public boolean isRuntimeInvisible() { return false; }
}

@node(name="UserMeta")
public class UserMeta extends MNode {
	@virtual typedef This  ≤ UserMeta;

	@abstract
	@att public String					qname;
	@att public SymbolRef<Struct>		decl;
	@att public MetaValue[]				values;

	public boolean equals(Object o) {
		if!(o instanceof UserMeta)
			return false;
		UserMeta meta = (UserMeta)o;
		if (qname != o.qname)
			return false;
		foreach (Method m; getTypeDecl().members) {
			MetaValue v1 = this.get(m.sname);
			MetaValue v2 = meta.get(m.sname);
			if (v1 == null && v2 == null)
				continue;
			if (v1 == null || v2 == null)
				return false;
			if (!v1.equals(v2))
				return false;
		}
		return true;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api") {
			if (attr.name == "decl")
				return false;
			if (attr.name == "qname")
				return true;
		}
		return super.includeInDump(dump, attr, val);
	}

	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if      (attr.name == "decl")
				parent().callbackChildChanged(pslot());
			else if (attr.name == "values")
				parent().callbackChildChanged(pslot());
		}
	}

	public UserMeta() {
		this.decl = new SymbolRef<Struct>("");
	}

	public UserMeta(Struct decl) {
		this.decl = new SymbolRef<Struct>(decl);
	}
	
	public UserMeta(String name) {
		this.decl = new SymbolRef<Struct>(name);
	}
	
	@getter
	public String get$qname() {
		TypeDecl s = decl.dnode;
		if (s != null)
			return s.qname();
		return decl.name;
	}

	@setter
	public void set$qname(String val) {
		decl.name = val;
	}

	public final TypeDecl getTypeDecl() {
		TypeDecl td = decl.dnode;
		if (td != null)
			return td;
		String name = decl.name;
		if (name.indexOf('\u001f') < 0) {
			Struct@ node;
			if( !PassInfo.resolveNameR(this,node,new ResInfo(this,name,ResInfo.noForwards)) )
				Kiev.reportError(this,"Unresolved annotation name "+name);
			this.decl.symbol = (Struct)node;
			node.checkResolved();
			return (Struct)node;
		}
		Struct scope = Env.getRoot();
		int dot;
		do {
			dot = name.indexOf('\u001f');
			String head;
			if (dot > 0) {
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1).intern();
			} else {
				head = name;
			}
			Struct@ node;
			if!(scope.resolveNameR(node,new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports))) {
				Kiev.reportError(this,"Unresolved identifier "+head+" in "+scope);
				return null;
			}
			scope = (Struct)node;
		} while (dot > 0);
		this.decl.symbol = scope;
		scope.checkResolved();
		return scope;
	}
	
	public boolean isRuntimeVisible() {
		TypeDecl tdecl = getTypeDecl();
		UserMeta retens = tdecl.getMeta("java\u001flang\u001fannotation\u001fRetention");
		if (retens == null)
			return false;
		MetaValue val = retens.get("value");
		if (val instanceof MetaValueScalar && val.value instanceof SFldExpr) {
			Field f = ((SFldExpr)val.value).var;
			if (f.sname == "RUNTIME")
				return true;
		}
		return false;
	}

	public boolean isRuntimeInvisible() {
		TypeDecl tdecl = getTypeDecl();
		UserMeta retens = tdecl.getMeta("java\u001flang\u001fannotation\u001fRetention");
		if (retens == null)
			return true;
		MetaValue val = retens.get("value");
		if (val instanceof MetaValueScalar && val.value instanceof SFldExpr) {
			Field f = ((SFldExpr)val.value).var;
			if (f.sname == "CLASS")
				return true;
		}
		return false;
	}

	public Type getType() {
		TypeDecl td = getTypeDecl();
		if (td == null)
			return Type.tpVoid;
		return td.xtype;
	}
	
	public int size() alias length {
		return values.length;
	}
	public boolean isEmpty() {
		return values.length == 0;
	}
	
	public void verify() {
		TypeDecl tdecl = getTypeDecl();
		if (tdecl == null || !tdecl.isAnnotation()) {
			throw new CompilerException(this, "Annotation name expected");
		}
//		String name = this.qname();
//		UserMeta m = this;
//		if (m != this) {
//			this.replaceWithNode(m);
//			foreach (MetaValue v; values)
//				m.set(v.ncopy());
//			m.verify();
//		}
		foreach (MetaValue v; values)
			v.verify();
		return;
	}
	
	public void resolve(Type reqType) {
		TypeDecl tdecl = getTypeDecl();
		tdecl.checkResolved();
		for (int n=0; n < values.length; n++) {
			MetaValue v = values[n];
			Method m = null;
			foreach (Method sm; tdecl.members) {
				if( sm.hasName(v.ident,true)) {
					m = sm;
					break;
				}
			}
			if (m == null)
				throw new CompilerException(v, "Unresolved method "+v.ident+" in class "+tdecl);
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
		foreach (Method m; tdecl.members) {
			for(int j=0; j < values.length; j++) {
				if (values[j].symbol != null)
					continue next_method;
			}
			// value not specified - does the method has a default meta-value?
			if !(m.body instanceof MetaValue)
				Kiev.reportError(this, "Annotation value "+m.sname+" is not specified");
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "decl") {
			TypeDecl scope;
			String head;
			int dot = name.indexOf('\u001f');
			if (dot > 0) {
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1);
			} else {
				head = name;
				name = "";
			}
			if (dot < 0) {
				int flags = ResInfo.noForwards;
				if (!by_equals)
					flags |= ResInfo.noEquals;
				Vector<TypeDecl> vect = new Vector<TypeDecl>();
				TypeDecl@ td;
				ResInfo info = new ResInfo(this,head,flags);
				foreach (PassInfo.resolveNameR(this,td,info)) {
					if ((td instanceof KievPackage || td instanceof JavaAnnotation) && !vect.contains(td))
						vect.append(td);
				}
				return vect.toArray();
			} else {
				TypeDecl@ td;
				if( !PassInfo.resolveNameR(this,td,new ResInfo(this,head,ResInfo.noForwards)) )
					return new TypeDecl[0];
				scope = (TypeDecl)td;
			}
			while (dot >= 0) {
				dot = name.indexOf('\u001f');
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
				} else {
					head = name.intern();
					name = "";
				}
				if (dot < 0) {
					int flags = ResInfo.noForwards;
					if (!by_equals)
						flags |= ResInfo.noEquals;
					Vector<TypeDecl> vect = new Vector<TypeDecl>();
					TypeDecl@ td;
					ResInfo info = new ResInfo(this,head,flags);
					foreach (scope.resolveNameR(td,info)) {
						if ((td instanceof KievPackage || td instanceof JavaAnnotation) && !vect.contains(td))
							vect.append(td);
					}
					return vect.toArray();
				} else {
					TypeDecl@ td;
					if!(scope.resolveNameR(td,new ResInfo(this,head,ResInfo.noForwards)))
						return new TypeDecl[0];
					scope = td;
				}
			}
		}
		return super.findForResolve(name,slot,by_equals);
	}

	public MetaValue get(String name) {
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident == name) {
				MetaValue v = values[i];
				return v;
			}
		}
		TypeDecl td = getTypeDecl();
		foreach (Method m; td.members; m.hasName(name,true))
			return (MetaValue)m.body;
		throw new RuntimeException("Value "+name+" not found in "+decl+" annotation");
	}
	
	public boolean getZ(String name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return false;
		if (v instanceof ConstBoolExpr)
			return ((ConstBoolExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+decl+" is not a boolean constant, but "+v);
	}
	
	public int getI(String name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return 0;
		if (v instanceof ConstIntExpr)
			return ((ConstIntExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+decl+" is not an int constant, but "+v);
	}
	
	public String getS(String name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return null;
		if (v instanceof ConstStringExpr)
			return ((ConstStringExpr)v).value;
		throw new RuntimeException("Value "+name+" in annotation "+decl+" is not a String constant, but "+v);
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
			if (this.dnode != m)
				this.symbol = m;
		}
		else if (ident == null) {
			if (ident != "value")
				this.ident = "value";
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


