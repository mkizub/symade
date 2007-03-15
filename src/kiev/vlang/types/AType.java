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
package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.be.java15.JType;
import kiev.be.java15.JBaseType;
import kiev.be.java15.JArrayType;
import kiev.be.java15.JMethodType;
import kiev.be.java15.JStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class AType implements StdTypes, TVSet {
	
	public final			MetaType	meta_type;
	public:ro,ro,ro,rw		TVar[]		tvars;
	public:ro,ro,ro,rw		TArg[]		appls;
	public					int			flags;
	private					int			version;
	
	protected AType(MetaType meta_type, int flags, TVar[] tvars, TArg[] appls) {
		this.meta_type = meta_type;
		this.flags = flags;
		this.tvars = tvars;
		this.appls = appls;
	}
	
	protected AType(MetaType meta_type, int flags, TVarBld bld)
	{
		this.meta_type = meta_type;
		this.flags = flags;
		this.setFromBld(bld);
	}
	
	private void setFromBld(TVarBld bld) {
		bld.close();
		int n = bld.tvars.length;
		if (n > 0) {
			this.tvars = new TVar[n];
			for (int i=0; i < n; i++)
				this.tvars[i] = bld.tvars[i].copy(this);
		} else {
			this.tvars = TVar.emptyArray;
		}
		if (bld.appls != null && bld.appls.length > 0) {
			n = bld.appls.length;
			this.appls = new TArg[n];
			for (int i=0; i < n; i++)
				this.appls[i] = bld.appls[i].copy(this);
		} else {
			this.appls = TArg.emptyArray;
		}

		flags &= ~(flAbstract|flValAppliable|flBindable);
		foreach(TVar tv; this.tvars; !tv.isAlias()) {
			Type r = tv.result();
			ArgType v = tv.var;
			if (tv.isFree()) flags |= flBindable;
			if (r.isAbstract()) flags |= flAbstract;
			if (v.isUnerasable()) flags |= flUnerasable;
			if (v.isArgAppliable() && r.isValAppliable()) flags |= flValAppliable;
		}
	}
	
	public final AType bindings() {
		if (!this.meta_type.checkTypeVersion(this.version)) {
			this.setFromBld(meta_type.getTemplBindings().bind_bld(this));
			this.version = this.meta_type.version;
		}
		return this;
	}
	
	public static boolean identity(AType t1, AType t2) alias xfx operator ≡ {
		return t1 == t2;
	}

	public static boolean not_identity(AType t1, AType t2) alias xfx operator ≢ {
		return t1 != t2;
	}

	public static boolean type_not_equals(AType t1, AType t2) alias xfx operator ≉ {
		if (t1 == null || t2 == null) return true;
		return !(t1 ≈ t2);
	}
	
	public static boolean type_equals(AType t1, AType t2) alias xfx operator ≈ {
		if (t1 == null || t2 == null) return false;
		if (t1 == t2) return true;
		if (t1.meta_type != t2.meta_type) return false;
		TVar[] b1 = t1.bindings().tvars;
		TVar[] b2 = t2.bindings().tvars;
		if (b1.length != b2.length) return false;
		final int n = b1.length;
		for (int i=0; i < n; i++) {
			TVar tv1 = b1[i];
			TVar tv2 = b2[i];
			if (tv1.var != tv2.var) return false;
			if (tv1.unalias().result() ≉ tv2.unalias().result())
				return false;
		}
		return true;
	}

	public final boolean equals(Object to) {
		if (to instanceof AType) return AType.type_equals(this,(AType)to);
		return false;
	}

	public TVar[] getTVars() {
		return this.tvars;
	}

	// find bound value for an abstract type
	public final Type resolve(ArgType arg) {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for(int i=0; i < n; i++) {
			if (tvars[i].var ≡ arg)
				return tvars[i].unalias().result();
		}
		return arg;
	}
	
	// Bind free (unbound) variables of current type to values
	// from a set of var=value pairs, returning a new set.
	//
	// having a bind pair A -> V, will bind
	// A:?      -> A:V			; bind
	// B:A      -> B:A			; alias remains
	// C:X<A:?> -> C X<A:?>		; non-recursive
	//
	// This operation is used in type extension/specification:
	//
	// class Bar<B> :- defines a free variable B
	// class Foo<F> extends Bar<F> :- binds Bar.B to Foo.F
	// new Foo<String> :- binds Foo.F to String
	// new Foo<Bar<Foo<F>>> :- binds Foo.F with Bar<Foo<F>>
	//
	// my.var ≡ vs.var -> (my.var, vs.result())

	public TVarBld bind_bld(TVSet vs) {
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.getTVars();
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarBld sr = new TVarBld(this);

	next_my:
		for(int i=0; i < my_size; i++) {
			TVar x = my_vars[i];
			
			// bind TVar
			if (x.isFree()) {
				// try known bind
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(sr.tvars[i], y.unalias().result());
						continue next_my;
					}
				}
				// bind to itself
				sr.set(sr.tvars[i], sr.tvars[i].unalias().result());
				continue next_my;
			}
			// bind virtual aliases
			if (x.isAlias() && x.var.isVirtual() && x.unalias().isFree()) {
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(sr.tvars[i], y.unalias().result());
						continue next_my;
					}
				}
			}
		}
		return sr.close();
	}

	// change bound types, for virtual args, outer args, etc
	public TVarBld rebind_bld(TVSet vs) {
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.getTVars();
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarBld sr = new TVarBld(this);

	next_my:
		for(int i=0; i < my_size; i++) {
			TVar x = my_vars[i];
			// TVarBound already bound
			if (!x.isAlias()) {
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(sr.tvars[i], y.unalias().result());
						continue next_my;
					}
				}
				continue next_my;
			}
			// bind virtual aliases
			if (x.var.isVirtual()) {
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(sr.tvars[i], y.unalias().result());
						continue next_my;
					}
				}
				continue next_my;
			}
		}
		return sr.close();
	}

	// Re-bind type set, replace all abstract types in current set
	// with results of another set. It binds unbound vars, and re-binds
	// (changes) vars bound to abstract types, i.e. it changes only 'TVar.bnd' field.
	//
	// having a re-bind pair A -> V, will re-bind
	// A:?      -> A:V			; bind
	// B:A      -> B:V			; re-bind 
	// C:X<A:?> -> C X<A:V>		; recursive
	//
	// This operation is used in access expressions:
	//
	// class Bar<B> { B b; }
	// class Foo<F> { F f; Foo<Bar<F>> fbf; }
	// Foo<String> a;
	// a.* :- binds Foo.F with String, and applays Bar.B (bound to Foo.F) with String
	//        producing: a.f = String; a.fbf = Foo<Bar<String>>; a.b = String 
	// Foo<Bar<Foo<F>>> x;
	// a.* :- binds Foo.F with Bar<Foo<F>>, and applays Bar.B (bound to Foo.F) with Bar<Foo<F>>,
	//        producing: a.f = Bar<Foo<F>>; a.fbf = Foo<Bar<Bar<Foo<F>>>>; a.b = Bar<Foo<F>>
	// a.fbf.* :- a.fbf.f = 
	//
	// my.bnd ≡ vs.var -> (my.var, vs.result())
	
	public TVarBld applay_bld(TVSet vs)
	{
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.getTVars();
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarBld sr = new TVarBld(this);
		if (!this.hasApplayables(vs))
			return sr.close();

	next_my:
		for(int i=0; i < my_size; i++) {
			TVar x = my_vars[i].unalias();
			Type bnd = x.val;
			if (x.isFree() || !x.var.isArgAppliable())
				continue;
			if (bnd instanceof ArgType) {
				for(int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (bnd ≡ y.var) {
						// re-bind
						sr.set(sr.tvars[i], y.unalias().result());
						continue next_my;
					}
				}
			}
			else if (bnd.bindings().hasApplayables(vs)) {
				// recursive
				Type t = bnd.applay(vs);
				if (t ≉ bnd)
					sr.set(sr.tvars[i], t);
			}
		}
		return sr.close();
	}
	
	private boolean hasApplayables(TVSet vs) {
		final int my_size = this.appls.length;
		if (my_size == 0)
			return false;
		TVar[] vs_vars = vs.getTVars();
		final int tp_size = vs_vars.length;
		if (tp_size == 0)
			return false;
		for (int i=0; i < my_size; i++) {
			for (int j=0; j < tp_size; j++) {
				if (this.appls[i].var ≡ vs_vars[j].var)
					return true;
			}
		}
		return false;
	}
	
	public boolean hasApplayable(ArgType at) {
		final int my_size = this.appls.length;
		if (my_size == 0)
			return false;
		for (int i=0; i < my_size; i++) {
			if (this.appls[i].var ≡ at)
				return true;
		}
		return false;
	}
	
	public String toDump() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getClass());
		sb.append(" {\n");
		foreach (TVar v; tvars)
			sb.append(v).append('\n');
		sb.append("}");
		return sb.toString();
	}
}

public final class TVarSet extends AType {

	public static final TVarSet emptySet = new TVarSet();

	private TVarSet() {
		super(MetaType.dummy, 0, TVar.emptyArray, TArg.emptyArray);
	}
	
	TVarSet(TVarBld bld) {
		super(MetaType.dummy, 0, bld);
	}
	
}


public final class TVar {
	public static final TVar[] emptyArray = new TVar[0];

	public final TVSet			set;	// the set this TVar belongs to
	public final int			idx;	// position in the set (set.tvars[idx] == this)
	public final ArgType		var;	// variable
	public final Type			val;	// value of the TVar (null for free vars, ArgType for aliases) 
	public final int			ref;	// reference to actual TVar, for aliases

	// copy
	private TVar(TVSet set, int idx, ArgType var, Type val, int ref) {
		this.set = set;
		this.idx = idx;
		this.var = var;
		this.val = val;
		this.ref = ref;
	}
	
	// free vars
	TVar(TVSet set, int idx, ArgType var) {
		this.set = set;
		this.idx = idx;
		this.var = var;
		this.ref = -1;
	}

	// bound vars
	TVar(TVSet set, int idx, ArgType var, Type val) {
		this.set = set;
		this.idx = idx;
		this.var = var;
		this.val = val;
		this.ref = -1;
	}

	// aliases vars
	TVar(TVSet set, int idx, ArgType var, ArgType val, int ref) {
		this.set = set;
		this.idx = idx;
		this.var = var;
		this.val = val;
		this.ref = ref;
	}

	public Type result() {
		return val == null? var : val;
	}
	
	public TVar copy(TVSet set) {
		return new TVar(set, idx, var, val, ref);
	}

	public TVar unalias() {
		TVar r = this;
		while (r.ref >= 0) r = set.getTVars()[r.ref];
		return r;
	}
	
	public boolean isFree() { return val == null; }
	
	public boolean isAlias() { return ref >= 0; }

	public String toString() {
		if (isFree())
			return idx+": free  "+var.definer.parent()+"."+var.definer+"."+var.name;
		else if (isAlias())
			return idx+": alias "+var.definer.parent()+"."+var.definer+"."+var.name+" > "+set.getTVars()[this.ref];
		else
			return idx+": bound "+var.definer.parent()+"."+var.definer+"."+var.name+" = "+val;
	}
}

public final class TArg {
	public static final TArg[] emptyArray = new TArg[0];

	public final TVSet			set;	// the set this TVar belongs to
	public final int			idx;	// position in the set (set.appls[idx] == this)
	public final ArgType		var;	// variable

	TArg(TVSet set, int idx, ArgType var) {
		this.set = set;
		this.idx = idx;
		this.var = var;
	}

	public TArg copy(TVSet set) {
		return new TArg(set, idx, var);
	}
}



