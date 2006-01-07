package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JBaseTypeProvider;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final class TVarSet {
	public static final TVarSet emptySet = new TVarSet();

	private final boolean ASSERT_MORE = true;	

	public access:ro,ro,ro,rw	TVar[] tvars;

	public TVarSet() {
		tvars = TVar.emptyArray;
	}
	
	public TVarSet(ArgumentType var, Type bnd) {
		tvars = new TVar[]{ new TVarBound(this, 0, var, bnd) };
		if (ASSERT_MORE) checkIntegrity();
	}
	
	public TVarSet copy() {
		TVarSet tvs = new TVarSet();
		int n = this.tvars.length;
		if (n == 0) return tvs;
		tvs.tvars = new TVar[n];
		for (int i=0; i < n; i++)
			tvs.tvars[i] = this.tvars[i].copy(tvs);
		for (int i=0; i < n; i++)
			tvs.tvars[i].resolve(i);
		return tvs;
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
	
	boolean eq(TVarSet vset) {
		TVar[] b1 = this.tvars;
		TVar[] b2 = vset.tvars;
		final int n = b1.length;
		for (int i=0; i < n; i++) {
			if (b1[i].result() ≉ b2[i].result())
				return false;
		}
		return true;
	}

	public void append(TVarSet set) {
		foreach (TVar v; set.tvars)
			append(v.var, v.value());
	}
	
	public void append(ArgumentType var, Type value)
		require { var != null; }
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
		if (var.isArgVirtual()) {
			for (int i=0; i < n; i++) {
				if (tmp[i].var.isArgVirtual() && tmp[i].var.name == var.name) {
					tmp[n] = new TVarAlias(this, n, var, tmp[i]);
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
			if (!v.isAlias() && v.isBound() && v.value() ≡ var)
				tmp[i] = new TVarAlias(this, i, v.var, tmp[n]);
		}
		this.tvars = tmp;
		for (int i=0; i < n; i++)
			this.tvars[i].resolve(i);
		
		if (ASSERT_MORE) checkIntegrity();
		if (var ≢ value && value ≢ null)
			set(this.tvars[n], value);
		if (ASSERT_MORE) checkIntegrity();
	}
	
	public void set(TVar v, Type bnd)
		require { v.set == this && bnd != null; }
	{
		TVar[] tvars = this.tvars;
		if (v.value() ≡ bnd || v.result() ≡ bnd)
			return; // ignore duplicated alias
		while (v.isAlias()) {
			TVarAlias va = (TVarAlias)v;
			// alias of another var, must point to 
			v = va.bnd;
			if (v.value() ≡ bnd || v.result() ≡ bnd)
				return; // ignore duplicated alias
		}
		// non-aliased var, just bind or alias it
		final int n = tvars.length;
		if (bnd instanceof ArgumentType) {
			for (int i=0; i < n; i++) {
				TVar av = tvars[i];
				if (av.var ≡ bnd) {
					// set v as alias of av
					while (av.isAlias()) av = ((TVarAlias)av).bnd;
					if (v == av)
						return; // don't bind a var to itself
					tvars[v.idx] = new TVarAlias(this, v.idx, v.var, av);
					assert (i < n);
					for (i=0; i < n; i++)
						this.tvars[i].resolve(i);
					if (ASSERT_MORE) checkIntegrity();
					return;
				}
			}
		}
		// not an alias, just bind
		tvars[v.idx] = new TVarBound(this,v.idx,v.var,bnd);
		for (int i=0; i < n; i++)
			this.tvars[i].resolve(i);
		if (ASSERT_MORE) checkIntegrity();
		return;
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

	public TVarSet bind(TVarSet vs) {
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.tvars;
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarSet sr = this.copy();
		// bind known vars
		for(int i=0; i < vs_size; i++) {
			TVar v = vs_vars[i];
			if (!v.isBound()) continue;
			Type r = v.result();
			for(int j=0; j < my_size; j++) {
				TVar x = my_vars[j];
				if (x.var ≡ v.var) {
					// bind
					sr.set(sr.tvars[i], r);
					break;
				}
			}
		}
		// bind free vars
		for(int i=0; i < vs_size; i++) {
			TVar v = vs_vars[i];
			if (v.var.definer != null)
				continue;
			Type r = v.result();
			for(int j=0; j < my_size; j++) {
				TVar x = my_vars[j];
				if (x.isAlias() || x.isBound())
					continue;
				// bind free var
				sr.set(sr.tvars[i], r);
				break;
			}
		}
		return sr;
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
	// a.* :- binds Foo.F with String, and rebinds Bar.B (bound to Foo.F) with String
	//        producing: a.f = String; a.fbf = Foo<Bar<String>>; a.b = String 
	// Foo<Bar<Foo<F>>> x;
	// a.* :- binds Foo.F with Bar<Foo<F>>, and rebinds Bar.B (bound to Foo.F) with Bar<Foo<F>>,
	//        producing: a.f = Bar<Foo<F>>; a.fbf = Foo<Bar<Bar<Foo<F>>>>; a.b = Bar<Foo<F>>
	// a.fbf.* :- a.fbf.f = 
	public TVarSet rebind(TVarSet vs) {
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.tvars;
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarSet sr = this.copy();
		for(int i=0; i < vs_size; i++) {
			TVar v = vs_vars[i];
			if (!v.isBound()) continue;
			Type r = v.result();
			for(int j=0; j < my_size; j++) {
				TVar x = my_vars[j];
				if (x.var ≡ v.var) {
					// bind
					sr.set(sr.tvars[j], r);
					continue;
				}
				if!(x instanceof TVarBound)
					continue;
				TVarBound b = (TVarBound)x;
				if (b.bnd instanceof ArgumentType) {
					if (b.bnd ≡ v.var) {
						// re-bind
						sr.set(sr.tvars[j], r);
					}
				} else {
					if (b.bnd.isArgumented()) {
						// recursive
						Type t = b.bnd.rebind(vs);
						if (t ≢ b.bnd)
							sr.set(sr.tvars[j], t);
					}
				}
			}
		}
		return sr;
	}
	
	// find a bound type of an argument
	public Type resolve(ArgumentType arg) {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for(int i=0; i < n; i++) {
			if (tvars[i].var ≡ arg)
				return tvars[i].result();
		}
		return arg;
	}
	
	private void checkIntegrity() {
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
				if (bv.bnd instanceof ArgumentType) {
					for (int j=0; j < n; j++)
						assert(tvars[j].var ≢ bv.bnd);
				}
			}
		}
	}
}


public class TVar {
	public static final TVar[] emptyArray = new TVar[0];

	public final TVarSet		set;	// the set this TVar belongs to
	public final int			idx;	// position in the set (set.tvars[idx] == this)
	public final ArgumentType	var;	// variable

	TVar(TVarSet set, int idx, ArgumentType var) {
		this.set = set;
		this.idx = idx;
		this.var = var;
	}

	public boolean isBound() { return false; }
	public boolean isAlias() { return false; }
	public Type    value()	  { return null; }
	public Type    result()	  { return var; }
	public TVar copy(TVarSet set) {
		return new TVar(set, idx, var);
	}
	void resolve(int i) {
		assert(i == idx && set.tvars[idx] == this);
	}
}

public class TVarBound extends TVar {

	access:no,no,ro,rw Type				bnd;

	TVarBound(TVarSet set, int idx, ArgumentType var, Type bnd) {
		super(set, idx, var);
		this.bnd = bnd;
	}

	public boolean isBound() { return true; }
	public boolean isAlias() { return false; }
	public Type    value()	  { return bnd; }
	public Type    result()	  { return bnd; }
	public TVar copy(TVarSet set) {
		return new TVarBound(set, idx, var, bnd);
	}
	void resolve(int i) {
		assert(i == idx && set.tvars[idx] == this);
	}
}

public class TVarAlias extends TVar {

	access:no,no,ro,rw TVar				bnd;

	TVarAlias(TVarSet set, int idx, ArgumentType var, TVar bnd) {
		super(set, idx, var);
		this.bnd = bnd;
	}

	public boolean isBound() { return bnd.isBound(); }
	public boolean isAlias() { return true; }
	public Type    value()	  { return bnd.var; }
	public Type    result()	  { return bnd.result(); }
	public TVar copy(TVarSet set) {
		return new TVarAlias(set, idx, var, bnd);
	}
	void resolve(int i) {
		assert(i == idx && set.tvars[idx] == this);
		TVar[] tvars = set.tvars;
		if (tvars[bnd.idx] == bnd)
			return;
		for (i=0; i < tvars.length; i++) {
			if (this.bnd.var == tvars[i].var) {
				this.bnd = tvars[i];
				return;
			}
		}
		throw new RuntimeException("Cannot resolve TVarAlias");
	}
}



public abstract class TypeProvider {
	public int version;
	public TypeProvider() {}
	public abstract Type bind(Type t, TVarSet bindings);
	public abstract Type rebind(Type t, TVarSet bindings);
}

public class CoreTypeProvider extends TypeProvider {
	public static final CoreTypeProvider instance = new CoreTypeProvider();
	private CoreTypeProvider() {}
	public Type bind(Type t, TVarSet bindings) {
		throw new RuntimeException("bind() in CoreType");
	}
	public Type rebind(Type t, TVarSet bindings) {
		return t;
	}
}

public class BaseTypeProvider extends TypeProvider {
	final Struct clazz;
	
	BaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
	}
	
	public Type bind(Type t, TVarSet bindings) {
		if (!t.isArgumented() || bindings.length == 0) return t;
		return new BaseType(this, t.bindings().bind(bindings));
	}
	
	public Type rebind(Type t, TVarSet bindings) {
		if (!t.isArgumented() || bindings.length == 0) return t;
		return new BaseType(this, t.bindings().rebind(bindings));
	}
	
}

public class ArrayTypeProvider extends TypeProvider {
	public static final ArrayTypeProvider instance = new ArrayTypeProvider();
	private ArrayTypeProvider() {}
	public Type bind(Type t, TVarSet bindings) {
		throw new RuntimeException("bind() in ArrayType");
	}
	public Type rebind(Type t, TVarSet bindings) {
		if( !t.isArgumented() || bindings.length == 0 ) return t;
		return ArrayType.newArrayType(((ArrayType)t).arg.rebind(bindings));
	}
}

public class ArgumentTypeProvider extends TypeProvider {
	public static final ArgumentTypeProvider instance = new ArgumentTypeProvider();
	private ArgumentTypeProvider() {}
	public Type bind(Type t, TVarSet bindings) {
		throw new RuntimeException("bind() in ArgumentType");
	}
	public Type rebind(Type t, TVarSet bindings) {
		if (bindings.length == 0) return t;
		ArgumentType at = (ArgumentType)t;
		for(int i=0; i < bindings.length; i++) {
			TVar v = bindings[i];
			if (v.value() != null && (v.var ≡ at || v.value() ≡ at))
				return v.result();
		}
		// Not found, return itself
		return t;
	}
}

public class WrapperTypeProvider extends TypeProvider {
	public final Struct		clazz;
	public final Field		field;
	public static WrapperTypeProvider instance(Struct clazz) {
		if (clazz.wmeta_type == null)
			clazz.wmeta_type = new WrapperTypeProvider(clazz);
		return clazz.wmeta_type;
	}
	private WrapperTypeProvider(Struct clazz) {
		this.clazz = clazz;
		this.field = clazz.getWrappedField(true);
	}
	public Type bind(Type t, TVarSet bindings) {
		throw new RuntimeException("bind() in WrapperType");
	}
	public Type rebind(Type t, TVarSet bindings) {
		if (!t.isArgumented() || bindings.length == 0) return t;
		return WrapperType.newWrapperType(((WrapperType)t).getUnwrappedType().rebind(bindings));
	}
}

public class CallTypeProvider extends TypeProvider {
	public static final CallTypeProvider instance = new CallTypeProvider();
	private CallTypeProvider() {}
	public Type bind(Type t, TVarSet bindings) {
		if (!t.isArgumented() || bindings.length == 0 || t.bindings().length == 0) return t;
		if!(t instanceof MethodType) return t;
		MethodType mt = (MethodType)t;
		mt = new MethodType(mt.bindings().bind(bindings),mt.args,mt.ret);
		mt = (MethodType)this.rebind(mt,mt.bindings());
		return mt;
	}
	public Type rebind(Type t, TVarSet bindings) {
		if( !t.isArgumented() || bindings.length == 0 ) return t;
		CallableType ct = (CallableType)t;
		Type[] tpargs = new Type[ct.args.length];
		for(int i=0; i < tpargs.length; i++)
			tpargs[i] = ct.args[i].rebind(bindings);
		Type ret = ct.ret.rebind(bindings);
		if (t instanceof MethodType)
			return new MethodType(tpargs,ret);
		else if (t instanceof ClosureType)
			return new ClosureType(tpargs,ret);
		assert (false, "Unrecognized type "+t+" ("+t.getClass()+")");
		return t;
	}
}


