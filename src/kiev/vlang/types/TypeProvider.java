package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import kiev.be.java.JBaseTypeProvider;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final class TVarSet extends TVSet {
	public static final TVarSet emptySet = new TVarSet();

	private final boolean ASSERT_MORE = true;

	public final TVar[]		tvars;
	public final TArg[]		appls;

	private TVarSet() {
		this.tvars = TVar.emptyArray;
		this.appls = TArg.emptyArray;
	}
	
	TVarSet(TVarBld bld) {
		int n = bld.tvars.length;
		if (n > 0) {
			this.tvars = new TVar[n];
			for (int i=0; i < n; i++)
				this.tvars[i] = bld.tvars[i].copy(this);
			for (int i=0; i < n; i++)
				this.tvars[i].resolve(i);
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
	}
	
	public TVar[] getTVars() {
		return this.tvars;
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

	public TVarSet bind(TVSet vs) {
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.getTVars();
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarBld sr = new TVarBld(this);

	next_my:
		for(int i=0; i < my_size; i++) {
			TVar x = my_vars[i];
			// TVarBound already bound
			if (x instanceof TVarBound)
				continue;
			// bind TVar
			if!(x instanceof TVarAlias) {
				// try known bind
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(sr.tvars[i], y.result());
						continue next_my;
					}
				}
				// bind to itself
				sr.set(sr.tvars[i], sr.tvars[i].result());
			}
			// bind virtual aliases
			if (x.var.isVirtual()) {
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(sr.tvars[i], y.result());
						continue next_my;
					}
				}
			}
		}
		return sr.close();
	}

	// change bound types, for virtual args, outer args, etc
	public TVarSet rebind(TVSet vs) {
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.getTVars();
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarBld sr = new TVarBld(this);

	next_my:
		for(int i=0; i < my_size; i++) {
			TVar x = my_vars[i];
			// TVarBound already bound
			if (x instanceof TVarBound) {
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(sr.tvars[i], y.result());
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
						sr.set(sr.tvars[i], y.result());
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
	
	public TVarSet applay(TVSet vs)
	{
		if (!this.hasApplayables(vs))
			return this;
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.getTVars();
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarBld sr = new TVarBld(this);

	next_my:
		for(int i=0; i < my_size; i++) {
			TVar x = my_vars[i];
			Type bnd = x.bound();
			if (bnd == null || !bnd.isAbstract())
				continue;
			if (bnd instanceof ArgType) {
				for(int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (bnd ≡ y.var) {
						// re-bind
						sr.set(sr.tvars[i], y.result());
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
		sb.append("TVarSet{\n");
		foreach (TVar v; tvars)
			sb.append(v).append('\n');
		sb.append("}");
		return sb.toString();
	}
}


public abstract class TVar {
	public static final TVar[] emptyArray = new TVar[0];

	public final TVSet			set;	// the set this TVar belongs to
	public final int			idx;	// position in the set (set.tvars[idx] == this)
	public final ArgType		var;	// variable

	TVar(TVSet set, int idx, ArgType var) {
		this.set = set;
		this.idx = idx;
		this.var = var;
	}

	public abstract boolean isBound();
	public abstract boolean isAlias();
	public abstract Type    value()	;
	public abstract Type    result();
	public abstract Type    bound()	;
	public abstract TVar copy(TVSet set);
	public abstract void resolve(int i);
}

public final class TVarFree extends TVar {
	TVarFree(TVSet set, int idx, ArgType var) {
		super(set,idx,var);
	}

	public boolean isBound() { return false; }
	public boolean isAlias() { return false; }
	public Type    value()	  { return null; }
	public Type    result()	  { return var; }
	public Type    bound()	  { return null; }
	public TVar copy(TVSet set) {
		return new TVarFree(set, idx, var);
	}
	void resolve(int i) {
		assert(i == idx && set.getTVars()[idx] == this);
	}
	public String toString() {
		return idx+": free  "+var.definer+"."+var.name;
	}
}

public final class TVarBound extends TVar {

	access:no,no,ro,rw Type				bnd;

	TVarBound(TVSet set, int idx, ArgType var, Type bnd) {
		super(set, idx, var);
		this.bnd = bnd;
	}

	public boolean isBound() { return true; }
	public boolean isAlias() { return false; }
	public Type    value()	  { return bnd; }
	public Type    result()	  { return bnd; }
	public Type    bound()	  { return bnd; }
	public TVar copy(TVSet set) {
		return new TVarBound(set, idx, var, bnd);
	}
	void resolve(int i) {
		assert(i == idx && set.getTVars()[idx] == this);
	}
	public String toString() {
		return idx+": bound "+var.definer+"."+var.name+" = "+bnd;
	}
}

public final class TVarAlias extends TVar {

	access:no,no,ro,rw TVar				bnd;

	TVarAlias(TVSet set, int idx, ArgType var, TVar bnd) {
		super(set, idx, var);
		this.bnd = bnd;
	}

	public boolean isBound() {
		if (var.isVirtual())
			return true;
		return bnd.isBound();
	}
	public boolean isAlias() { return true; }
	public Type    value()	  { return bnd.var; }
	public Type    result()	  { return bnd.result(); }
	public Type    bound()	  { return bnd.bound(); }
	public TVar copy(TVSet set) {
		return new TVarAlias(set, idx, var, bnd);
	}
	void resolve(int i) {
		assert(i == idx && set.getTVars()[idx] == this);
		TVar[] tvars = set.getTVars();
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
	public String toString() {
		return idx+": alias "+var.definer+"."+var.name+" > "+bnd;
	}
}

public final class TArg {
	public static final TArg[] emptyArray = new TArg[0];

	public final TVSet		set;	// the set this TVar belongs to
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



public abstract class TypeProvider {
	public final static TypeProvider[] emptyArray = new TypeProvider[0];
	public int version;
	public TypeProvider() {}
	public abstract Type make(TVSet bindings);
	public abstract Type bind(Type t, TVSet bindings);
	public abstract Type rebind(Type t, TVSet bindings);
	public abstract Type applay(Type t, TVSet bindings);
	public TVarSet getTemplBindings() { return TVarSet.emptySet; }
}

public class CoreTypeProvider extends TypeProvider {
	CoreType core_type;
	
	CoreTypeProvider() {}
	
	public Type make(TVSet bindings) {
		return core_type;
	}
	public Type bind(Type t, TVSet bindings) {
		throw new RuntimeException("bind() in CoreType");
	}
	public Type rebind(Type t, TVSet bindings) {
		throw new RuntimeException("rebind() in CoreType");
	}
	public Type applay(Type t, TVSet bindings) {
		return t;
	}
}

public final class CompaundTypeProvider extends TypeProvider {
	public final Struct			clazz;
	
	private TVarSet			templ_bindings;
	private int				templ_version;
	
	public CompaundTypeProvider(Struct clazz) {
		this.clazz = clazz;
		if (this.clazz == Env.root) ((Struct.StructImpl)Env.root.$v_impl).imeta_type = this;
		this.templ_bindings = TVarSet.emptySet;
		this.templ_version = -1;
	}
	
	public Type make(TVSet bindings) {
		return new CompaundType(this, getTemplBindings().bind(bindings));
	}

	public Type bind(Type t, TVSet bindings) {
		if (!t.isAbstract()) return t;
		return new CompaundType(this, t.bindings().bind(bindings));
	}
	
	public Type rebind(Type t, TVSet bindings) {
		return new CompaundType(this, t.bindings().rebind(bindings));
	}
	
	public Type applay(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0) return t;
		return new CompaundType(this, t.bindings().applay(bindings));
	}
	
	public TVarSet getTemplBindings() {
		if (this.version != this.templ_version)
			makeTemplBindings();
		return templ_bindings;
	}
	
	private void makeTemplBindings() {
		TVarBld vs = new TVarBld();
		foreach (TypeDef ad; clazz.args)
			vs.append(ad.getAType(), null);
		foreach (DNode d; clazz.members; d instanceof TypeDef) {
			TypeDef td = (TypeDef)d;
			vs.append(td.getAType(), null /*td.getAType().getSuperType()*/);
		}
		TypeRef st = clazz.super_bound;
		if (st.getType() ≢ null) {
			vs.append(st.getType().bindings());
			foreach (TypeRef it; clazz.interfaces)
				vs.append(it.getType().bindings());
		}
		templ_bindings = vs.close();
		templ_version = version;
	}
}

public class ArrayTypeProvider extends TypeProvider {
	public static final ArrayTypeProvider instance = new ArrayTypeProvider();
	private ArrayTypeProvider() {}

	public Type make(TVSet bindings) {
		return ArrayType.newArrayType(bindings.resolve(StdTypes.tpArrayArg));
	}
	public Type bind(Type t, TVSet bindings) {
		throw new RuntimeException("bind() in ArrayType");
	}
	public Type rebind(Type t, TVSet bindings) {
		throw new RuntimeException("rebind() in ArrayType");
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isAbstract() || bindings.getTVars().length == 0 ) return t;
		return ArrayType.newArrayType(((ArrayType)t).arg.applay(bindings));
	}
}

public class ArgTypeProvider extends TypeProvider {
	public static final ArgTypeProvider instance = new ArgTypeProvider();
	private ArgTypeProvider() {}

	public Type make(TVSet bindings) {
		throw new RuntimeException("make() in ArgType");
	}
	public Type bind(Type t, TVSet bindings) {
		return t; //throw new RuntimeException("bind() in ArgType");
	}
	public Type rebind(Type t, TVSet bindings) {
		return t; //throw new RuntimeException("bind() in ArgType");
	}
	public Type applay(Type t, TVSet bindings) {
		ArgType at = (ArgType)t;
		foreach (TVar v; bindings.getTVars()) {
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
	public Type make(TVSet bindings) {
		return WrapperType.newWrapperType(bindings.resolve(StdTypes.tpWrapperArg));
	}
	public Type bind(Type t, TVSet bindings) {
		return WrapperType.newWrapperType(((WrapperType)t).getUnwrappedType().bind(bindings));
	}
	public Type rebind(Type t, TVSet bindings) {
		return WrapperType.newWrapperType(((WrapperType)t).getUnwrappedType().rebind(bindings));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0) return t;
		return WrapperType.newWrapperType(((WrapperType)t).getUnwrappedType().applay(bindings));
	}
}

public class OuterTypeProvider extends TypeProvider {
	public final Struct		clazz;
	public final TypeDef	tdef;
	public static OuterTypeProvider instance(Struct clazz, TypeDef tdef) {
		if (clazz.ometa_type == null)
			clazz.ometa_type = new OuterTypeProvider(clazz, tdef);
		return clazz.ometa_type;
	}
	private OuterTypeProvider(Struct clazz, TypeDef tdef) {
		this.clazz = clazz;
		this.tdef = tdef;
	}

	public Type make(TVSet bindings) {
		return OuterType.newOuterType(clazz,bindings.resolve(tdef.getAType()));
	}
	public Type bind(Type t, TVSet bindings) {
		return OuterType.newOuterType(clazz,((OuterType)t).outer.bind(bindings));
	}
	public Type rebind(Type t, TVSet bindings) {
		return OuterType.newOuterType(clazz,((OuterType)t).outer.rebind(bindings));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0) return t;
		return OuterType.newOuterType(clazz,((OuterType)t).outer.applay(bindings));
	}
}

public class CallTypeProvider extends TypeProvider {
	public static final CallTypeProvider instance = new CallTypeProvider();
	private CallTypeProvider() {}
	public Type bind(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0 || t.bindings().getTVars().length == 0) return t;
		if!(t instanceof CallType) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().bind(bindings),mt.arity,mt.isReference());
		return mt;
	}
	public Type rebind(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0 || t.bindings().tvars.length == 0) return t;
		if!(t instanceof CallType) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().rebind(bindings),mt.arity,mt.isReference());
		return mt;
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isAbstract() || bindings.getTVars().length == 0 ) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().applay(bindings),mt.arity,mt.isReference());
		return mt;
	}
}


