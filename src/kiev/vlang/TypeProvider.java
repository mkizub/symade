package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final class TVarSet {
	public static final TVarSet emptySet = new TVarSet();

	public access:ro,ro,ro,rw	TVar[] tvars = TVar.emptyArray;

	public TVarSet copy() {
		TVarSet tvs = new TVarSet();
		if (tvars.length == 0) return tvs;
		tvs.tvars = new TVar[this.tvars.length];
		for (int i=0; i < this.tvars.length; i++) {
			TVar v = this.tvars[i];
			tvs.tvars[i] = new TVar(v.kind, tvs, v.var, v.bnd);
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
	
	public void append(ArgumentType var)
		require { var != null; }
	{
		final int n = tvars.length;
		for (int i=0; i < n; i++) {
			if (tvars[i].var == var)
				return; // ignore duplicated var
		}
		append(new TVar(TVar.TVAR_UNBOUND, this, var, null));
		normalize();
	}
	
	public void append(ArgumentType var, Type bnd)
		require { var != null; }
	{
		if (bnd == var || bnd == null) {
			append(var);
			return;
		}
		for (int i=0; i < tvars.length; i++) {
			TVar v = tvars[i];
			if (v.var == var) {
				if (v.bnd == bnd)
					return; // ignore duplicated alias
				if (v.kind >= 0) {
					i = v.kind;
					// alias of another var, must point to 
					assert (tvars.length < i);
					assert (tvars[i].kind < 0); // must not be another alias
					assert (v.bnd == tvars[i].var);
					v = tvars[i];
					if (v.bnd == bnd)
						return; // ignore duplicated alias
				}
				if (v.bnd == null) {
					// unbound var, just bind it
					assert (v.kind == TVar.TVAR_UNBOUND);
					tvars[i] = new TVar(TVar.TVAR_BOUND,this,var,bnd);
					normalize();
					return;
				}
				throw new RuntimeException("TVar rebinding");
			}
		}
		append(new TVar(TVar.TVAR_BOUND, this, var, bnd));
		normalize();
	}

	public void set(int i, Type bnd)
		require { bnd != null; }
	{
		TVar v = tvars[i];
		if (v.bnd == bnd)
			return; // ignore duplicated alias
		if (v.kind >= 0) {
			i = v.kind;
			// alias of another var, must point to 
			assert (tvars.length < i);
			assert (tvars[i].kind < 0); // must not be another alias
			assert (v.bnd == tvars[i].var);
			v = tvars[i];
			if (v.bnd == bnd)
				return; // ignore duplicated alias
		}
		if (v.bnd == null) {
			// unbound var, just bind it
			assert (v.kind == TVar.TVAR_UNBOUND);
			tvars[i] = new TVar(TVar.TVAR_BOUND,this,v.var,bnd);
			normalize();
			return;
		}
		tvars[i] = new TVar(TVar.TVAR_BOUND,this,v.var,bnd);
		normalize();
		return;
	}

	public void append(TVarSet set) {
		foreach (TVar v; set.tvars)
			append(v.var, v.bnd);
	}
	
	private void append(TVar v) {
		final int n = tvars.length;
		TVar[] tmp = new TVar[n+1];
		for (int i=0; i < n; i++)
			tmp[i] = tvars[i];
		tmp[n] = v;
		this.tvars = tmp;
	}
	
	private void normalize() {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for (int i=0; i < n; i++) {
			TVar vi = tvars[i];
			for (int j=i+1; j < n; j++) {
				TVar vj = tvars[j];
				if (vj.bnd == vi.var) {
					if (vj.kind == i) continue; // already the alias
					tvars[j] = new TVar(i, this, vj.var, vj.bnd);
				}
			}
		}
	}
	
	// Bind free (unbound) variables of current type to values
	// from a set of var=value pairs, returning a new set.
	//
	// This operation is used in type extension/specification:
	//
	// class Bar<B> :- defines a free variable B
	// class Foo<F> extends Bar<F> :- binds Bar.B to Foo.F
	// new Foo<String> :- binds Foo.F to String
	// new Foo<Bar<Foo<N>>> :- binds Foo.F with Bar<Foo<N>>
	public TVarSet bind(TVarSet vs) {
		int n = this.tvars.length;
		TVarSet sr = this.copy();
		return sr;
	}

	// Re-bind type set, replace all abstract types in current set
	// with results of another set. It binds unbound vars, and re-binds
	// (changes) vars bound to abstract types, i.e. it changes only 'TVar.bnd' field.
	//
	// having a re-bind pair A -> V, will re-bind
	// A ? -> A V
	// B A -> B V
	//
	// This operation is used in access expressions:
	//
	// class Bar<B> { B b; }
	// class Foo<F> { F f; Foo<Bar<F>> fbf; }
	// Foo<String> a;
	// a.* :- binds Foo.F with String, and rebinds Bar.B (bound to Foo.F) with String
	//        producing: a.f = String; a.fbf = Foo<Bar<String>>; a.b = String 
	// Foo<Bar<Foo<N>>> x;
	// a.* :- binds Foo.F with Bar<Foo<N>>, and rebinds Bar.B (bound to Foo.F) with Bar<Foo<N>>,
	//        producing: a.f = Bar<Foo<N>>; a.fbf = Foo<Bar<Bar<Foo<N>>>>; a.b = Bar<Foo<N>>
	// a.fbf.* :- a.fbf.f = 
	public TVarSet rebind(TVarSet vs) {
		int n = this.tvars.length;
		TVarSet sr = this.copy();
	next:
		for(int i=0; i < vs.length; i++) {
			TVar v = vs[i];
			if (v.bnd == null)
				continue next;
			for(int r=0; r < n; r++) {
				TVar x = tvars[r];
				if (x.var == v.var || x.bnd == v.var || x.bnd == v.bnd || x.bnd == v.bnd) {
					if (x.isAlias())
						r = x.kind;
					sr.set(r, v.result());
					continue next;
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
			if (tvars[i].var == arg)
				return tvars[i].result();
		}
		return arg;
	}
}


public final class TVar {
	public static final TVar[] emptyArray = new TVar[0];
	
	static final int TVAR_UNBOUND = -1;
	static final int TVAR_BOUND   = -2;
	
	public final int			kind;
	public final TVarSet		set;
	public final ArgumentType	var;
	public final Type			bnd;
	
	TVar(int kind, TVarSet set, ArgumentType var, Type bnd)
		require { set != null && var != null; }
	{
		this.kind = kind;
		this.set = set;
		this.var = var;
		this.bnd = bnd;
		assert (
			(kind == TVar.TVAR_UNBOUND && bnd == null) ||
			(kind == TVar.TVAR_BOUND && bnd != null) ||
			(kind >= 0 && set.tvars[kind].var == bnd)
		);
	}
	
	public boolean isBound() {
		if (this.kind >= 0)
			return set.tvars[this.kind].isBound();
		return this.bnd != null;
	}
	
	public boolean isAlias() {
		return this.kind >= 0;
	}
	
	public Type result() {
		if (this.kind >= 0) // alias
			return set.tvars[this.kind].result();
		if (bnd == null) return var;
		return bnd;
	}
}

public abstract class TypeProvider {
	TypeProvider() {}
	public abstract Type rebind(Type t, TVarSet bindings);
	public abstract TVarSet bindings(Type tp);
}

public class BaseTypeProvider extends TypeProvider {
	final Struct clazz;
	
	final JBaseTypeProvider java_meta_type;

	BaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
		java_meta_type = new JBaseTypeProvider(clazz);
	}
	
	public Type rebind(Type t, TVarSet bindings) {
		if( !t.isArgumented() || bindings.length == 0 ) return t;
		return Type.newRefType(((BaseType)t).clazz, t.bindings().rebind(bindings));
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
	public Type rebind(Type t, TVarSet bindings) {
		if( bindings.length == 0 ) return t;
		ArgumentType at = (ArgumentType)t;
		for(int i=0; i < bindings.length; i++) {
			TVar v = bindings[i];
			if (v.bnd != null && (v.var == at || v.bnd == at))
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
	public Type rebind(Type t, TVarSet bindings) {
		if( !t.isArgumented() || bindings.length == 0 ) return t;
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

public class JBaseTypeProvider extends TypeProvider {
	public final Struct clazz;
	JBaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
	}
}


