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
	
	public Type result() {
		if (res != null) return res;
		if (bound == null) return at;
		if (bound instanceof ArgumentType)
			res = Type.getRealType(owner,bound);
		else
			res = bound;
		return res;
	}
}

public abstract class TypeProvider {
	TypeProvider() {}
	public abstract Type newType(Type[] args);
	public abstract TVar[] getBindings(Type tp);
}

public class BaseTypeProvider extends TypeProvider {
	final Struct clazz;
	
	final JBaseTypeProvider java_meta_type;

	BaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
		java_meta_type = new JBaseTypeProvider(clazz);
	}
	
	public TVar[] getBindings(Type owner) {
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
	static final ArrayTypeProvider instance = new ArrayTypeProvider();
	private ArrayTypeProvider() {}
}

public class ArgumentTypeProvider extends TypeProvider {
	static final ArgumentTypeProvider instance = new ArgumentTypeProvider();
	private ArgumentTypeProvider() {}
}

public class WrapperTypeProvider extends TypeProvider {
	final Struct	clazz;
	final Field		field;
	public static WrapperTypeProvider instance(Struct clazz) {
		if (clazz.wmeta_type == null)
			clazz.wmeta_type = new WrapperTypeProvider(clazz);
		return clazz.wmeta_type;
	}
	private WrapperTypeProvider(Struct clazz) {
		this.clazz = clazz;
		this.field = clazz.getWrappedField(true);
	}
	public Type newType(Type[] args) {
		assert (args.length == 1);
		return WrapperType.newWrapperType(Type.newRefType(clazz,args));
	}
}

public class CallTypeProvider extends TypeProvider {
	static final CallTypeProvider instance = new CallTypeProvider();
	private CallTypeProvider() {}
}

public class JBaseTypeProvider extends TypeProvider {
	final Struct clazz;
	JBaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
	}
}


