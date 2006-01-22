package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import kiev.be.java.JBaseTypeProvider;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final class TVarSet extends AType {

	public static final TVarSet emptySet = new TVarSet();

	private TVarSet() {
		super(DummyTypeProvider.instance, 0, TVar.emptyArray, TArg.emptyArray);
	}
	
	TVarSet(TVarBld bld) {
		super(DummyTypeProvider.instance, 0, bld);
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

public class DummyTypeProvider extends TypeProvider {
	static final DummyTypeProvider instance = new DummyTypeProvider();
	private DummyTypeProvider() {}
	public Type make(TVSet bindings) {
		throw new RuntimeException("make() in DummyType");
	}
	public Type bind(Type t, TVSet bindings) {
		throw new RuntimeException("bind() in DummyType");
	}
	public Type rebind(Type t, TVSet bindings) {
		throw new RuntimeException("rebind() in DummyType");
	}
	public Type applay(Type t, TVSet bindings) {
		throw new RuntimeException("applay() in DummyType");
	}
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
		return new CompaundType(this, getTemplBindings().bind_bld(bindings));
	}

	public Type bind(Type t, TVSet bindings) {
		if (!t.isAbstract()) return t;
		return new CompaundType(this, t.bindings().bind_bld(bindings));
	}
	
	public Type rebind(Type t, TVSet bindings) {
		return new CompaundType(this, t.bindings().rebind_bld(bindings));
	}
	
	public Type applay(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0) return t;
		return new CompaundType(this, t.bindings().applay_bld(bindings));
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
		templ_bindings = new TVarSet(vs.close());
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
		mt = new CallType(mt.bindings().bind_bld(bindings),mt.arity,mt.isReference());
		return mt;
	}
	public Type rebind(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0 || t.bindings().tvars.length == 0) return t;
		if!(t instanceof CallType) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().rebind_bld(bindings),mt.arity,mt.isReference());
		return mt;
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isAbstract() || bindings.getTVars().length == 0 ) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().applay_bld(bindings),mt.arity,mt.isReference());
		return mt;
	}
}


