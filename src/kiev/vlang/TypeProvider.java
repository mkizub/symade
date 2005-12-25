package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final class TVar {
	public static final TVar[] emptyArray = new TVar[0];
	
	public			Type			owner;
	public  final	ArgumentType	at;
	private final	Type			bound;
	private			Type			res;
	
	public TVar(Type owner, ArgumentType at, Type bound) {
		this.owner = owner;
		this.at = at;
		this.bound = bound==at ? null : bound;
	}
	
	public TVar copy(Type owner) {
		return new TVar(owner, this.at, this.bound);
	}
	
	public boolean match(TVar v) {
		if (this.at == v.at) return true;
		if (this.at == v.bound) return true;
		if (this.bound == null) return false;
		return this.bound == v.at || this.bound == v.bound;
	}
	
	public boolean match(ArgumentType at) {
		if (this.at == at) return true;
		if (this.bound == at) return true;
		return false;
	}
	
	public Type result()
		require { owner != null; }
	{
		if (res != null) return res;
		if (bound == null) return at;
		if (bound instanceof ArgumentType) {
			TVar[] bindings = owner.bindings();
			for (int i=0; i < bindings.length; i++) {
				if (bindings[i].at == bound) {
					res = bindings[i].result();
					return res;
				}
			}
		}
		res = bound;
		return res;
	}

	public Type result(TVar[] bindings) {
		if (res != null) return res;
		if (bound == null) return at;
		if (bound instanceof ArgumentType) {
			for (int i=0; i < bindings.length; i++) {
				if (bindings[i].at == bound)
					return bindings[i].result(bindings);
			}
		}
		return bound;
	}
}

public abstract class TypeProvider {
	TypeProvider() {}
	public abstract Type rebind(Type t, TVar[] bindings);
	public abstract TVar[] bindings(Type tp);
}

public class BaseTypeProvider extends TypeProvider {
	final Struct clazz;
	
	final JBaseTypeProvider java_meta_type;

	BaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
		java_meta_type = new JBaseTypeProvider(clazz);
	}
	
	public Type rebind(Type t, TVar[] bindings) {
		if( !t.isArgumented() || bindings.length == 0 ) return t;
		BaseType bt = (BaseType)t;
		assert(bt.args.length != 0);
		TVar[] bi = bt.bindings();
		TVar[] br = new TVar[bi.length];
	next:
		for(int i=0; i < bi.length; i++) {
			for(int b=0; b < bindings.length; b++) {
				if (bi[i].match(bindings[b])) {
					br[i] = new TVar(null,bi[i].at,bindings[b].result());
					continue next;
				}
			}
			br[i] = bi[i].copy(null);
		}
		return Type.newRefType(bt.clazz,br);
	}
	
	public TVar[] bindings(Type owner) {
		BaseType t = (BaseType)owner;
		Vector<TVar> v = new Vector<TVar>();
		for (int i=0; i < clazz.args.length; i++)
			v.append(new TVar(owner, clazz.args[i].getAType(), t.args[i]));
		foreach (Type st; t.getDirectSuperTypes()) {
			TVar[] svars = ((BaseType)st).bindings();
	next_sv:foreach (TVar sv; svars) {
				// check it's not already added
				foreach (TVar tv; v) {
					if (tv.at == sv.at)
						continue next_sv;
				}
				v.append(sv.copy(owner));
			}
		}
		return v.toArray();
	}
}

public class ArrayTypeProvider extends TypeProvider {
	public static final ArrayTypeProvider instance = new ArrayTypeProvider();
	private ArrayTypeProvider() {}
	public Type rebind(Type t, TVar[] bindings) {
		if( !t.isArgumented() || bindings.length == 0 ) return t;
		return ArrayType.newArrayType(((ArrayType)t).arg.rebind(bindings));
	}
	public TVar[] bindings(Type owner) {
		ArrayType a = (ArrayType)owner;
		return a.arg.bindings();
	}
}

public class ArgumentTypeProvider extends TypeProvider {
	public static final ArgumentTypeProvider instance = new ArgumentTypeProvider();
	private ArgumentTypeProvider() {}
	public Type rebind(Type t, TVar[] bindings) {
		if( bindings.length == 0 ) return t;
		ArgumentType at = (ArgumentType)t;
		for(int i=0; i < bindings.length; i++) {
			if (bindings[i].match(at))
				return bindings[i].result();
		}
		// Not found, return itself
		return t;
	}
	public TVar[] bindings(Type owner) {
		return TVar.emptyArray;
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
	public Type rebind(Type t, TVar[] bindings) {
		if( !t.isArgumented() || bindings.length == 0 ) return t;
		return WrapperType.newWrapperType(((WrapperType)t).getUnwrappedType().rebind(bindings));
	}
	public TVar[] bindings(Type owner) {
		WrapperType w = (WrapperType)owner;
		return w.getUnwrappedType().bindings();
	}
}

public class CallTypeProvider extends TypeProvider {
	public static final CallTypeProvider instance = new CallTypeProvider();
	private CallTypeProvider() {}
	public Type rebind(Type t, TVar[] bindings) {
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
	public TVar[] bindings(Type owner) {
		return TVar.emptyArray;
	}
}

public class JBaseTypeProvider extends TypeProvider {
	public final Struct clazz;
	JBaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
	}
}


