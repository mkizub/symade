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

interface TVSet {
	public TVar[] getTVars();
	public ArgType[] getTArgs();
	public Type resolve(ArgType arg);
}

public final class TVarBld implements TVSet {

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
		tvars = TVar.emptyArray;
		int n = vset.tvars.length;
		if (n > 0) {
			this.tvars = new TVar[n];
			for (int i=0; i < n; i++)
				this.tvars[i] = vset.tvars[i];
		}
		//if (vset.appls != null && vset.appls.length > 0) {
		//	n = vset.appls.length;
		//	this.appls = new ArgType[n];
		//	for (int i=0; i < n; i++)
		//		this.appls[i] = vset.appls[i].copy(this);
		//}
	}
	
	public TVarBld close() {
		if (!closed) {
			//this.buildApplayables();
			//if (ASSERT_MORE) this.checkIntegrity(true);
			closed = true;
		}
		return this;
	}
	
	public TVar[] getTVars() {
		return this.tvars;
	}
	
	public ArgType[] getTArgs() {
		if (this.appls == null)
			buildApplayables();
		return this.appls;
	}
	
	@getter
	public int size()
		alias length
		alias get$length
	{
		return tvars.length;
	}

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
					int av_idx = av.unalias_idx(this,i);
					av = tvars[av_idx];
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

	// find a bound type of an argument
	public Type resolve(ArgType arg) {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for(int i=0; i < n; i++) {
			if (tvars[i].var ≡ arg)
				return tvars[i].unalias(this).result();
		}
		return arg;
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

