package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import kiev.be.java.JBaseTypeProvider;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

abstract class TVSet {
	TVSet() {}
	public abstract TVar[] getTVars();
	public abstract Type resolve(ArgType arg);
}

public final class TVarBld extends TVSet {

	private final boolean ASSERT_MORE = true;

	public access:ro,rw,ro,rw	TVar[]		tvars;
	public access:ro,ro,ro,rw	TArg[]		appls;

	public TVarBld() {
		tvars = TVar.emptyArray;
	}
	
	public TVarBld(ArgType var, Type bnd) {
		this.tvars = new TVar[]{ new TVarBound(this, 0, var, bnd) };
		if (ASSERT_MORE) checkIntegrity(false);
	}
	
	public TVarBld(TVarSet vset) {
		tvars = TVar.emptyArray;
		int n = vset.tvars.length;
		if (n > 0) {
			this.tvars = new TVar[n];
			for (int i=0; i < n; i++)
				this.tvars[i] = vset.tvars[i].copy(this);
			for (int i=0; i < n; i++)
				this.tvars[i].resolve(i);
		}
		if (vset.appls != null && vset.appls.length > 0) {
			n = vset.appls.length;
			this.appls = new TArg[n];
			for (int i=0; i < n; i++)
				this.appls[i] = vset.appls[i].copy(this);
		}
	}
	
	public TVarSet close() {
		this.buildApplayables();
		if (ASSERT_MORE) this.checkIntegrity(true);
		return new TVarSet(this);
	}
	
	public TVar[] getTVars() {
		return this.tvars;
	}
	
	public int size()
		alias length
		alias get$length
	{
		return tvars.length;
	}

	public TVar get(int idx)
		alias at
		alias operator(210,xfy,[])
	{
		return tvars[idx];
	}
	
	public void append(TVSet set)
	{
		foreach (TVar v; set.getTVars())
			append(v.var, v.value());
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
					tmp[n] = new TVarAlias(this, n, var, tmp[i]);
					value = null;
					break;
				}
			}
		}
		if (tmp[n] == null)
			tmp[n] = new TVarFree(this, n, var);
		// fix aliases
		for (int i=0; i < n; i++) {
			TVar v = tmp[i];
			if (!v.isAlias() && v.isBound() && v.value() ≡ var)
				tmp[i] = new TVarAlias(this, i, v.var, tmp[n]);
		}
		this.tvars = tmp;
		for (int i=0; i < n; i++)
			this.tvars[i].resolve(i);
		
		if (ASSERT_MORE) checkIntegrity(false);
		if (var ≢ value && value ≢ null)
			set(this.tvars[n], value);
		if (ASSERT_MORE) checkIntegrity(false);
	}
	
	public void set(TVar v, Type bnd)
		require { v.set == this && bnd != null; }
	{
		TVar[] tvars = this.tvars;
		if (v.bound() ≡ bnd)
			return; // ignore duplicated alias
		while (v.isAlias()) {
			TVarAlias va = (TVarAlias)v;
			// alias of another var, must point to 
			v = va.bnd;
			if (v.bound() ≡ bnd)
				return; // ignore duplicated alias
		}
		// non-aliased var, just bind or alias it
		final int n = tvars.length;
		if (bnd instanceof ArgType) {
			for (int i=0; i < n; i++) {
				TVar av = tvars[i];
				if (av.var ≡ bnd) {
					// set v as alias of av
					while (av.isAlias()) av = ((TVarAlias)av).bnd;
					if (v == av)
						break; // don't alias a var to itself 
					tvars[v.idx] = new TVarAlias(this, v.idx, v.var, av);
					assert (i < n);
					for (i=0; i < n; i++)
						this.tvars[i].resolve(i);
					if (ASSERT_MORE) checkIntegrity(false);
					return;
				}
			}
		}
		// not an alias, just bind
		tvars[v.idx] = new TVarBound(this,v.idx,v.var,bnd);
		for (int i=0; i < n; i++)
			this.tvars[i].resolve(i);
		if (ASSERT_MORE) checkIntegrity(false);
		return;
	}
	
	private void buildApplayables() {
		appls = TArg.emptyArray;
		foreach (TVar tv; tvars; tv instanceof TVarBound)
			addApplayables(tv.bnd);
	}
	
	private void addApplayables(Type t) {
		if (t instanceof ArgType) {
			addApplayable((ArgType)t);
		} else {
			TArg[] tappls = t.bindings().appls;
			for (int i=0; i < tappls.length; i++)
				addApplayable(tappls[i].var);
		}
	}
	private void addApplayable(ArgType at) {
		int sz = this.appls.length;
		for (int i=0; i < sz; i++) {
			if (this.appls[i].var ≡ at)
				return;
		}
		TArg[] tmp = new TArg[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = this.appls[i];
		tmp[sz] = new TArg(this,sz,at);
		this.appls = tmp;
	}

	// find a bound type of an argument
	public Type resolve(ArgType arg) {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for(int i=0; i < n; i++) {
			if (tvars[i].var ≡ arg)
				return tvars[i].result();
		}
		return arg;
	}
	
	private void checkIntegrity(boolean check_appls) {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for (int i=0; i < n; i++) {
			TVar v = tvars[i];
			assert(v.idx == i);
			for (int j=0; j < n; j++)
				assert(i==j || tvars[j].var ≢ v.var);
			if (v.isAlias()) {
				TVarAlias av = (TVarAlias)v;
				assert(av.bnd != null);
				assert(av.bnd.idx < n);
				assert(tvars[av.bnd.idx] == av.bnd);
				assert(av.bnd.idx != i);
				for (TVar x=av.bnd; x.isAlias(); x=((TVarAlias)x).bnd)
					assert(av != x);
			}
			else if (v.isBound()) {
				TVarBound bv = (TVarBound)v;
				assert(bv.bnd ≢ null);
				if (check_appls) {
					if (bv.bnd instanceof ArgType) {
						int j=0;
						for (; j < this.appls.length; j++) {
							if (this.appls[j].var ≡ bv.bnd)
								break;
						}
						assert (j < this.appls.length);
					} else {
						foreach (TArg at; bv.bnd.bindings().appls) {
							int j=0;
							for (; j < this.appls.length; j++) {
								if (this.appls[j].var ≡ at.var)
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
				TArg at = this.appls[i];
				int j = 0;
			next_tvar:
				for (; j < n; j++) {
					if (tvars[j] instanceof TVarBound) {
						Type bnd = tvars[j].bound();
						if (bnd instanceof ArgType) {
							if (bnd ≡ at.var)
								break next_tvar;
						} else {
							TArg[] tappls = bnd.bindings().appls;
							for (int k=0; k < tappls.length; k++) {
								if (at.var ≡ tappls[k].var)
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
		foreach (TVar v; tvars)
			sb.append(v).append('\n');
		sb.append("}");
		return sb.toString();
	}
}

