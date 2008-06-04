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

import kiev.be.java15.JBaseMetaType;
import kiev.be.java15.JStruct;

import syntax kiev.Syntax;

public abstract class TVSet {
	public abstract TVar[] getTVars();
	public abstract Type resolve(ArgType arg);
	public abstract int getArgsLength();
	public abstract ArgType getArg(int i);
	public abstract Type resolveArg(int i);
	public abstract boolean isAliasArg(int i);
}

public final class TemplateTVarSet extends TVSet {

	public static final TemplateTVarSet emptySet = new TemplateTVarSet(TVarBld.emptySet);

	@access:no,no,ro,rw		TVar[]		tvars;
	private					ArgType[]	appls;
	private					int			flags;

	TemplateTVarSet(TVarBld bld) {
		TVar[] bld_tvars = bld.getTVars();
		int n = bld_tvars.length;
		if (n > 0) {
			this.tvars = new TVar[n];
			for (int i=0; i < n; i++)
				this.tvars[i] = bld_tvars[i];
		} else {
			this.tvars = TVar.emptyArray;
		}

		foreach(TVar tv; this.tvars; !tv.isAlias()) {
			Type r = tv.result();
			ArgType v = tv.var;
			if (tv.isFree()) flags |= StdTypes.flBindable;
			if (r.isAbstract()) flags |= StdTypes.flAbstract;
			if (v.isUnerasable()) flags |= StdTypes.flUnerasable;
			if (v.isArgAppliable() && r.isValAppliable()) flags |= StdTypes.flValAppliable;
		}
	}
	
	private ArgType[] getTArgs() {
		if (this.appls == null)
			buildApplayables();
		return this.appls;
	}
	
	private void buildApplayables() {
		appls = ArgType.emptyArray;
		foreach (TVar tv; tvars; !tv.isAlias())
			addApplayables(tv.result());
	}
	
	private void addApplayables(Type t) {
		if (t instanceof ArgType) {
			addApplayable((ArgType)t);
		} else {
			ArgType[] tappls = t.bindings().getTArgs();
			for (int i=0; i < tappls.length; i++)
				addApplayable(tappls[i]);
		}
	}
	private void addApplayable(ArgType at) {
		int sz = this.appls.length;
		for (int i=0; i < sz; i++) {
			if (this.appls[i] ≡ at)
				return;
		}
		ArgType[] tmp = new ArgType[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = this.appls[i];
		tmp[sz] = at;
		this.appls = tmp;
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
						sr.set(i, vs.resolveArg(j));
						continue next_my;
					}
				}
				// bind to itself
				sr.set(i, sr.resolveArg(i));
				continue next_my;
			}
			// bind virtual aliases
			if (x.isAlias() && x.var.isVirtual() && unaliasTVar(x).isFree()) {
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(i, vs.resolveArg(j));
						continue next_my;
					}
				}
			}
		}
		return sr.close();
	}
	private TVar unaliasTVar(TVar r) {
		while (r.ref >= 0) r = this.tvars[r.ref];
		return r;
	}
	


	// TVSet interface methods
	
	public TVar[] getTVars() { this.tvars }
	
	// find a bound type of an argument
	public final Type resolve(ArgType arg) {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for(int i=0; i < n; i++) {
			TVar x = tvars[i];
			if (x.var ≡ arg) {
				while (x.ref >= 0) x = tvars[x.ref];
				return x.result();
			}
		}
		return arg;
	}
	
	public final int getArgsLength() { return this.tvars.length; }
	public final ArgType getArg(int i) { return this.tvars[i].var; }
	public final Type resolveArg(int i)  {
		TVar[] tvars = this.tvars;
		while (tvars[i].ref >= 0) i = tvars[i].ref;
		return tvars[i].result();
	}
	public final boolean isAliasArg(int i) { return this.tvars[i].isAlias(); }

	final TVar unaliasTVar(TVar r) {
		while (r.ref >= 0) r = this.tvars[r.ref];
		return r;
	}
}

public final class TVarBld extends TVSet {

	private static final boolean ASSERT_MORE = true;

	public static final TVarBld emptySet = new TVarBld().close();

	@access:no,no,ro,rw		TVar[]		tvars;
	private					ArgType[]	appls;
	private					boolean		closed;

	public TVarBld() {
		tvars = TVar.emptyArray;
	}
	
	public TVarBld(ArgType var, Type bnd) {
		if (bnd == null)
			this.tvars = new TVar[]{ new TVar(var, bnd, TVar.MODE_FREE) };
		else
			this.tvars = new TVar[]{ new TVar(var, bnd, TVar.MODE_BOUND) };
		if (ASSERT_MORE) checkIntegrity(false);
	}
	
	public TVarBld(AType vset) {
		this.tvars = TVar.emptyArray;
		TVar[] tvars = vset.getTVars();
		int n = tvars.length;
		if (n > 0) {
			this.tvars = new TVar[n];
			for (int i=0; i < n; i++)
				this.tvars[i] = tvars[i];
		}
	}
	
	public TVarBld(TemplateTVarSet vset) {
		if (vset.tvars.length > 0)
			this.tvars = (TVar[])vset.tvars.clone();
		else
			this.tvars = TVar.emptyArray;
	}
	
	public TVarBld close() {
		if (!closed) {
			//this.buildApplayables();
			//if (ASSERT_MORE) this.checkIntegrity(true);
			closed = true;
		}
		return this;
	}
	
	// TVSet interface methods
	
	public TVar[] getTVars() {
		return this.tvars;
	}
	
	// find a bound type of an argument
	public final Type resolve(ArgType arg) {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for(int i=0; i < n; i++) {
			TVar x = tvars[i];
			if (x.var ≡ arg) {
				while (x.ref >= 0) x = tvars[x.ref];
				return x.result();
			}
		}
		return arg;
	}
	
	public final int getArgsLength() { return this.tvars.length; }
	public final ArgType getArg(int i) { return this.tvars[i].var; }
	public final Type resolveArg(int i)  {
		TVar[] tvars = this.tvars;
		while (tvars[i].ref >= 0) i = tvars[i].ref;
		return tvars[i].result();
	}
	public final boolean isAliasArg(int i) { return this.tvars[i].isAlias(); }

	// Operations on the type

	public void append(TVSet set)
	{
		foreach (TVar v; set.getTVars())
			append(v.var, v.val);
	}
	
	public void append(ArgType var, Type value)
		require var != null;
	{
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for (int i=0; i < n; i++) {
			if (tvars[i].var ≡ var)
				return; // ignore duplicated var
		}
		TVar[] tmp = new TVar[n+1];
		for (int i=0; i < n; i++)
			tmp[i] = tvars[i];
		if (var.isVirtual()) {
			for (int i=0; i < n; i++) {
				if (tmp[i].var.isVirtual() && tmp[i].var.name == var.name) {
					tmp[n] = new TVar(var, tmp[i].var, i);
					value = null;
					break;
				}
			}
		}
		if (tmp[n] == null)
			tmp[n] = new TVar(var, null, TVar.MODE_FREE);
		// fix aliases
		for (int i=0; i < n; i++) {
			TVar v = tmp[i];
			if (!v.isAlias() && v.val ≡ var)
				tmp[i] = new TVar(v.var, var, n);
		}
		this.tvars = tmp;
		
		if (ASSERT_MORE) checkIntegrity(false);
		if (var ≢ value && value ≢ null)
			set(n, value);
		if (ASSERT_MORE) checkIntegrity(false);
	}
	
	void set(int v_idx, Type bnd)
		require { bnd != null; }
	{
		TVar[] tvars = this.tvars;
		TVar v = tvars[v_idx];
		if (v.val ≡ bnd)
			return; // ignore duplicated alias
		while (v.isAlias()) {
			// alias of another var, must point to
			v_idx = v.ref;
			v = tvars[v_idx];
			if (v.val ≡ bnd)
				return; // ignore duplicated alias
		}
		// non-aliased var, just bind or alias it
		final int n = tvars.length;
		if (bnd instanceof ArgType) {
			for (int i=0; i < n; i++) {
				TVar av = tvars[i];
				if (av.var ≡ bnd) {
					// set v as alias of av
					int av_idx = i;
					while (av.ref >= 0) {
						av_idx = av.ref;
						av = tvars[av_idx];
					}
					if (v == av)
						break; // don't alias a var to itself 
					assert (i < n && av_idx < n);
					tvars[v_idx] = new TVar(v.var, av.var, av_idx);
					if (ASSERT_MORE) checkIntegrity(false);
					return;
				}
			}
		}
		// not an alias, just bind
		tvars[v_idx] = new TVar(v.var,bnd,TVar.MODE_BOUND);
		if (ASSERT_MORE) checkIntegrity(false);
		return;
	}
	
	private ArgType[] getTArgs() {
		if (this.appls == null)
			buildApplayables();
		return this.appls;
	}
	
	private void buildApplayables() {
		appls = ArgType.emptyArray;
		foreach (TVar tv; tvars; !tv.isAlias())
			addApplayables(tv.result());
	}
	
	private void addApplayables(Type t) {
		if (t instanceof ArgType) {
			addApplayable((ArgType)t);
		} else {
			ArgType[] tappls = t.bindings().getTArgs();
			for (int i=0; i < tappls.length; i++)
				addApplayable(tappls[i]);
		}
	}
	private void addApplayable(ArgType at) {
		int sz = this.appls.length;
		for (int i=0; i < sz; i++) {
			if (this.appls[i] ≡ at)
				return;
		}
		ArgType[] tmp = new ArgType[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = this.appls[i];
		tmp[sz] = at;
		this.appls = tmp;
	}

	private void checkIntegrity(boolean check_appls) {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for (int i=0; i < n; i++) {
			TVar v = tvars[i];
			for (int j=0; j < n; j++)
				assert(i==j || tvars[j].var ≢ v.var);
			if (v.isAlias()) {
				assert(v.val != null);
				assert(v.ref >= 0 && v.ref < n);
				assert(tvars[v.ref].var == v.val);
				assert(v.ref != i);
				for (TVar x=tvars[v.ref]; x.isAlias(); x=tvars[x.ref])
					assert(v != x);
			} else {
				if (check_appls) {
					if (v.val == null) {
						int j=0;
						for (; j < this.appls.length; j++) {
							if (this.appls[j] ≡ v.var)
								break;
						}
						assert (j < this.appls.length);
					}
					else if (v.val instanceof ArgType) {
						int j=0;
						for (; j < this.appls.length; j++) {
							if (this.appls[j] ≡ v.val)
								break;
						}
						assert (j < this.appls.length);
					}
					else {
						foreach (ArgType at; v.val.bindings().getTArgs()) {
							int j=0;
							for (; j < this.appls.length; j++) {
								if (this.appls[j] ≡ at)
									break;
							}
							assert (j < this.appls.length);
						}
					}
				}
			}
		}
		if (check_appls) {
			final int m = this.appls.length;
			for (int i=0; i < m; i++) {
				ArgType at = this.appls[i];
				int j = 0;
			next_tvar:
				for (; j < n; j++) {
					if (!tvars[j].isAlias()) {
						TVar tv = tvars[j];
						if (tv.val == null) {
							if (tv.var ≡ at)
								break next_tvar;
						}
						else if (tv.val instanceof ArgType) {
							if (tv.val ≡ at)
								break next_tvar;
						}
						else {
							ArgType[] tappls = tv.val.bindings().getTArgs();
							for (int k=0; k < tappls.length; k++) {
								if (at ≡ tappls[k])
									break next_tvar;
							}
						}
					}
				}
				assert (j < n);
			}
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("TVarBld{\n");
		for (int i=0; i < tvars.length; i++)
			sb.append(i).append(": ").append(tvars[i]).append('\n');
		sb.append("}");
		return sb.toString();
	}
}

