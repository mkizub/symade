package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JBaseTypeProvider;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final class TVarSet {
	public static final TVarSet emptySet = new TVarSet();

	private final boolean ASSERT_MORE = false;	

	public access:ro,ro,ro,rw	TVar[] tvars = TVar.emptyArray;

	public TVarSet copy() {
		TVarSet tvs = new TVarSet();
		if (tvars.length == 0) return tvs;
		tvs.tvars = new TVar[this.tvars.length];
		for (int i=0; i < this.tvars.length; i++) {
			TVar v = this.tvars[i];
			tvs.tvars[i] = new TVar(v.ref, tvs, v.var, v.bnd);
		}
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
	
	public void append(TVarSet set) {
		foreach (TVar v; set.tvars)
			append(v.var, v.bnd);
	}
	
	public void append(ArgumentType var, Type bnd)
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
		tmp[n] = new TVar(-1, this, var, null);
		// fix aliases
		for (int i=0; i < n; i++) {
			TVar v = tmp[i];
			if (v.bnd ≡ var) {
				assert (v.ref < 0);
				tmp[i] = new TVar(n, this, v.var, var);
			}
		}
		this.tvars = tmp;
		if (ASSERT_MORE) checkIntegrity();
		if (var ≢ bnd && bnd ≢ null)
			set(n, var, bnd);
		if (ASSERT_MORE) checkIntegrity();
	}
	
	public void set(int i, ArgumentType var, Type bnd)
		require { bnd != null; }
	{
		TVar[] tvars = this.tvars;
		TVar v = tvars[i];
		assert (v.var ≡ var);
		if (v.bnd ≡ bnd)
			return; // ignore duplicated alias
		while (v.ref >= 0) {
			i = v.ref;
			// alias of another var, must point to 
			assert (i < tvars.length);
			assert (v.bnd ≡ tvars[i].var);
			v = tvars[i];
			if (v.bnd ≡ bnd)
				return; // ignore duplicated alias
		}
		// non-aliased var, just bind or alias it
		if (bnd instanceof ArgumentType) {
			final int n = tvars.length;
			for (int j=0; j < n; j++) {
				if (tvars[j].var ≡ bnd) {
					// alias of tvars[j]
					while (tvars[j].ref >= 0) j = tvars[j].ref;
					if (i == j)
						return; // don't bind a var to itself
					tvars[i] = new TVar(j,this,v.var,tvars[j].var);
					if (ASSERT_MORE) checkIntegrity();
					return;
				}
			}
		}
		// not an alias, just bind
		tvars[i] = new TVar(-1,this,v.var,bnd);
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
		for(int i=0; i < vs_size; i++) {
			TVar v = vs_vars[i];
			if (v.bnd == null) continue;
			Type r = v.result();
			for(int j=0; j < my_size; j++) {
				TVar x = my_vars[j];
				if (x.var ≡ v.var) {
					// bind
					sr.set(j, v.var, r);
					break;
				}
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
			if (v.bnd == null) continue;
			Type r = v.result();
			for(int j=0; j < my_size; j++) {
				TVar x = my_vars[j];
				if (x.var ≡ v.var) {
					// bind
					sr.set(j, v.var, r);
					continue;
				}
				if (x.bnd == null || x.isAlias())
					continue;
				if (x.bnd instanceof ArgumentType) {
					if (x.bnd ≡ v.var) {
						// re-bind
						sr.set(j, x.var, r);
					}
				} else {
					if (x.bnd.isArgumented()) {
						// recursive
						Type t = x.bnd.rebind(vs);
						if (t ≢ x.bnd)
							sr.set(j, x.var, t);
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
			for (int j=0; j < n; j++)
				assert(i==j || tvars[j].var ≢ v.var);
			if (v.ref >= 0) {
				assert(v.bnd instanceof ArgumentType);
				assert(v.ref < n);
				assert(tvars[v.ref].var ≡ v.bnd);
				assert(v.ref != i);
				for (int j=v.ref; tvars[j].ref >= 0; j=tvars[j].ref)
					assert(j != i);
			}
			else if (v.bnd instanceof ArgumentType) {
				for (int j=0; j < n; j++)
					assert(tvars[j].var ≢ v.bnd);
			}
		}
	}
}


public final class TVar {
	public static final TVar[] emptyArray = new TVar[0];
	
	public final int			ref;	// if ref >= 0 then it's alias reference 
	public final TVarSet		set;	// the set this TVar belongs to
	public final ArgumentType	var;	// variable
	public final Type			bnd;	// bnd == null for unbound, bnd == set[ref].var, bnd = value (if ref < 0)
	
	TVar(int ref, TVarSet set, ArgumentType var, Type bnd)
		require { set != null && var != null; }
	{
		this.ref = ref;
		this.set = set;
		this.var = var;
		this.bnd = bnd;
	}
	
	public boolean isBound() {
		if (ref >= 0)
			return set.tvars[ref].isBound();
		return bnd != null;
	}
	
	public boolean isAlias() {
		return ref >= 0;
	}
	
	public Type result() {
		if (ref >= 0) // alias
			return set.tvars[ref].result();
		if (bnd == null) return var;
		return bnd;
	}
}

public abstract class TypeProvider {
	public TypeProvider() {}
	public abstract Type bind(Type t, TVarSet bindings);
	public abstract Type rebind(Type t, TVarSet bindings);
	public abstract TVarSet bindings(Type tp);
}

public class BaseTypeProvider extends TypeProvider {
	final Struct clazz;
	
	BaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
	}
	
	public Type bind(Type t, TVarSet bindings) {
		if (!t.isArgumented() || bindings.length == 0) return t;
		return Type.newRefType(this.clazz, t.bindings().bind(bindings));
	}
	
	public Type rebind(Type t, TVarSet bindings) {
		if (!t.isArgumented() || bindings.length == 0) return t;
		return Type.newRefType(this.clazz, t.bindings().rebind(bindings));
	}
	
	public TVarSet bindings(Type owner) {
		BaseType t = (BaseType)owner;
		TVarSet vs = new TVarSet();
		for (int i=0; i < clazz.args.length; i++)
			vs.append(clazz.args[i].getAType(), t.args[i]);
		foreach (Type st; t.getDirectSuperTypes())
			vs.append(st.bindings());
		;
		for (Struct s = clazz; !s.isStatic() && !s.package_clazz.isPackage(); s = s.package_clazz)
			vs.append(s.package_clazz.type.bindings());
		return vs;
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
	public TVarSet bindings(Type owner) {
		ArrayType a = (ArrayType)owner;
		return a.arg.bindings();
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
			if (v.bnd != null && (v.var ≡ at || v.bnd ≡ at))
				return v.result();
		}
		// Not found, return itself
		return t;
	}
	public TVarSet bindings(Type owner) {
		return TVarSet.emptySet;
	}
}

public class WrapperTypeProvider extends TypeProvider {
	public final Struct	clazz;
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
	public TVarSet bindings(Type owner) {
		WrapperType w = (WrapperType)owner;
		return w.getUnwrappedType().bindings();
	}
}

public class CallTypeProvider extends TypeProvider {
	public static final CallTypeProvider instance = new CallTypeProvider();
	private CallTypeProvider() {}
	public Type bind(Type t, TVarSet bindings) {
		throw new RuntimeException("bind() in CallableType");
	}
	public Type rebind(Type t, TVarSet bindings) {
		if( !t.isArgumented() || bindings.length == 0 ) return t;
		CallableType ct = (CallableType)t;
		Type[] tpargs = new Type[ct.args.length];
		for(int i=0; i < tpargs.length; i++)
			tpargs[i] = ct.args[i].rebind(bindings);
		Type ret = ct.ret.rebind(bindings);
		if (t instanceof MethodType)
			return MethodType.newMethodType(tpargs,ret);
		else if (t instanceof ClosureType)
			return ClosureType.newClosureType(tpargs,ret);
		assert (false, "Unrecognized type "+t+" ("+t.getClass()+")");
		return t;
	}
	public TVarSet bindings(Type owner) {
		return TVarSet.emptySet;
	}
}


