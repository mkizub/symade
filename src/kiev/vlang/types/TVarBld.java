package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import kiev.be.java.JBaseTypeProvider;
import kiev.be.java.JStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

interface TVSet {
	public TVar[] getTVars();
	public Type resolve(ArgType arg);
}

public final class TVarBld implements TVSet {

	private static final boolean ASSERT_MORE = true;

	public static final TVarBld emptySet = new TVarBld().close();

	public access:ro,rw,ro,rw	TVar[]		tvars;
	public access:ro,ro,ro,rw	TArg[]		appls;
	private						boolean		closed;

	public TVarBld() {
		tvars = TVar.emptyArray;
	}
	
	public TVarBld(ArgType var, Type bnd) {
		this.tvars = new TVar[]{ new TVar(this, 0, var, bnd) };
		if (ASSERT_MORE) checkIntegrity(false);
	}
	
	public TVarBld(AType vset) {
		tvars = TVar.emptyArray;
		int n = vset.tvars.length;
		if (n > 0) {
			this.tvars = new TVar[n];
			for (int i=0; i < n; i++)
				this.tvars[i] = vset.tvars[i].copy(this);
		}
		if (vset.appls != null && vset.appls.length > 0) {
			n = vset.appls.length;
			this.appls = new TArg[n];
			for (int i=0; i < n; i++)
				this.appls[i] = vset.appls[i].copy(this);
		}
	}
	
	public TVarBld close() {
		if (!closed) {
			this.buildApplayables();
			if (ASSERT_MORE) this.checkIntegrity(true);
			closed = true;
		}
		return this;
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
					tmp[n] = new TVar(this, n, var, tmp[i].var, i);
					value = null;
					break;
				}
			}
		}
		if (tmp[n] == null)
			tmp[n] = new TVar(this, n, var);
		// fix aliases
		for (int i=0; i < n; i++) {
			TVar v = tmp[i];
			if (!v.isAlias() && v.val ≡ var)
				tmp[i] = new TVar(this, i, v.var, var, n);
		}
		this.tvars = tmp;
		
		if (ASSERT_MORE) checkIntegrity(false);
		if (var ≢ value && value ≢ null)
			set(this.tvars[n], value);
		if (ASSERT_MORE) checkIntegrity(false);
	}
	
	public void set(TVar v, Type bnd)
		require { v.set == this && bnd != null; }
	{
		TVar[] tvars = this.tvars;
		if (v.val ≡ bnd)
			return; // ignore duplicated alias
		while (v.isAlias()) {
			// alias of another var, must point to 
			v = tvars[v.ref];
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
					av = av.unalias();
					if (v == av)
						break; // don't alias a var to itself 
					tvars[v.idx] = new TVar(this, v.idx, v.var, av.var, av.idx);
					assert (i < n);
					if (ASSERT_MORE) checkIntegrity(false);
					return;
				}
			}
		}
		// not an alias, just bind
		tvars[v.idx] = new TVar(this,v.idx,v.var,bnd);
		if (ASSERT_MORE) checkIntegrity(false);
		return;
	}
	
	private void buildApplayables() {
		appls = TArg.emptyArray;
		foreach (TVar tv; tvars; !tv.isAlias())
			addApplayables(tv.result());
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
				return tvars[i].unalias().result();
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
							if (this.appls[j].var ≡ v.var)
								break;
						}
						assert (j < this.appls.length);
					}
					else if (v.val instanceof ArgType) {
						int j=0;
						for (; j < this.appls.length; j++) {
							if (this.appls[j].var ≡ v.val)
								break;
						}
						assert (j < this.appls.length);
					}
					else {
						foreach (TArg at; v.val.bindings().appls) {
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
					if (!tvars[j].isAlias()) {
						TVar tv = tvars[j];
						if (tv.val == null) {
							if (tv.var ≡ at.var)
								break next_tvar;
						}
						else if (tv.val instanceof ArgType) {
							if (tv.val ≡ at.var)
								break next_tvar;
						}
						else {
							TArg[] tappls = tv.val.bindings().appls;
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

