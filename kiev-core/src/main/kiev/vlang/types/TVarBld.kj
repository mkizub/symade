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

import syntax kiev.Syntax;

public abstract class TVSet {
	public abstract Type resolve(ArgType arg);
	public abstract int getArgsLength();
	public abstract ArgType getArg(int i);
	public abstract Type resolveArg(int i);
	public abstract boolean isAliasArg(int i);
}

public final class TemplateTVarSet extends TVSet {

	public static final TemplateTVarSet emptySet = new TemplateTVarSet(-1, TVarBld.emptySet);

	@access:no,no,ro,rw		TVar[]		tvars;
	private					int			flags;
	public final			int			n_free;

	TemplateTVarSet(int n_free, TVarBld bld) {
		bld.close(n_free);
		if (n_free < 0)
			this.n_free = bld.getArgsLength();
		else
			this.n_free = n_free;
		TVar[] bld_tvars = bld.getTVars();
		int n = bld_tvars.length;
		if (n > 0)
			this.tvars = (TVar[])bld_tvars.clone();
		else
			this.tvars = TVar.emptyArray;

		foreach(TVar tv; this.tvars; !tv.isAlias()) {
			Type r = tv.result();
			ArgType v = tv.var;
			if (r.isAbstract()) flags |= StdTypes.flAbstract;
			if (v.isUnerasable()) flags |= StdTypes.flUnerasable;
			if (v.isArgAppliable() && r.isValAppliable()) flags |= StdTypes.flValAppliable;
		}
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

	public String toDump() {
		StringBuffer sb = new StringBuffer();
		sb.append("template free "+n_free+" {\n");
		for (int i=0; i < tvars.length; i++)
			sb.append(i).append(": ").append(tvars[i]).append('\n');
		sb.append("}");
		return sb.toString();
	}
}

public final class TVarBld extends TVSet {

	private static final boolean ASSERT_MORE = Kiev.debug;

	public static final TVarBld emptySet = new TVarBld().close(-1);

	@access:no,no,ro,rw		TVar[]		tvars;
							boolean		closed;

	public TVarBld() {
		tvars = TVar.emptyArray;
	}
	
	public TVarBld(ArgType var, Type bnd) {
		if (bnd == null)
			this.tvars = new TVar[]{ new TVar(var, bnd, TVar.MODE_FREE) };
		else
			this.tvars = new TVar[]{ new TVar(var, bnd, TVar.MODE_BOUND) };
		if (ASSERT_MORE) checkIntegrity();
	}
	
	public TVarBld(AType vset) {
		this.tvars = TVar.emptyArray;
		this.append(vset);
	}
	
	public TVarBld(TemplateTVarSet vset) {
		if (vset.tvars.length > 0)
			this.tvars = (TVar[])vset.tvars.clone();
		else
			this.tvars = TVar.emptyArray;
	}
	
	// close the TVarBld, self-bind TVar-s with index >= n_free; or do not self-bind vars if n_free < 0
	public TVarBld close(int n_free) {
		if (this.tvars.length == 0) {
			closed = true;
			return this;
		}
		assert (!closed);
		closed = true;
		if (n_free >= 0) {
			TVar[] tvars = this.tvars;
			for (int i=n_free; i < tvars.length; i++) {
				TVar tv = tvars[i];
				if (tv.isFree())
					tvars[i] = new TVar(tv.var, tv.var, TVar.MODE_BOUND);
			}
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
		int n = set.getArgsLength();
		for (int i=0; i < n; i++)
			append(set.getArg(i), set.resolveArg(i));
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
		
		if (ASSERT_MORE) checkIntegrity();
		if (var ≢ value && value ≢ null)
			set(n, value);
		if (ASSERT_MORE) checkIntegrity();
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
					if (ASSERT_MORE) checkIntegrity();
					return;
				}
			}
		}
		// not an alias, just bind
		tvars[v_idx] = new TVar(v.var,bnd,TVar.MODE_BOUND);
		if (ASSERT_MORE) checkIntegrity();
		return;
	}
	
	private void checkIntegrity() {
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

