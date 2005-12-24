package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final class TVar {
	public static final TVar[] emptyArray = new TVar[0];
	
	public final ArgumentType	at;
	public final Type			bound;
	
	public TVar(ArgumentType at, Type bound) {
		this.at = at;
		this.bound = bound;
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
	
	public TVar[] getBindings(Type _t) {
		BaseType t = (BaseType)_t;
		Vector<TVar> v = new Vector<TVar>();
		for (int i=0; i < clazz.args.length; i++)
			v.append(new TVar((ArgumentType)clazz.args[i].getType(), t.args[i]));
		t.bindings = v.toArray();
		foreach (Type st; t.getDirectSuperTypes())
			addSuperBindings(t, (BaseType)st, v);
		return v.toArray();
	}
	private void addSuperBindings(BaseType t, BaseType st, Vector<TVar> v) {
		st = (BaseType)Type.getRealType(t, st);
	next_arg:
		for (int i=0; i < st.clazz.args.length; i++) {
			ArgumentType at = (ArgumentType)st.clazz.args[i].getType();
			Type bound = st.args[i];
			for (int k=0; k < v.length; k++) {
				if (v[k].at == at) {
					//assert(v[k].bound == bound);
					continue next_arg;
				}
			}
			v.append(new TVar(at, bound));
		}
		foreach (Type sst; st.getDirectSuperTypes())
			addSuperBindings(st, (BaseType)sst, v);
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


