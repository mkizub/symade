package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.vlang.TypeProvider.Slot;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public abstract class TypeProvider {
	
	public static final class Slot {
		public final String name;
		public final int arity;
		public Slot(String name, int arity) {
			this.name = name.intern();
			this.arity = arity;
		}
	}
	
	public final Slot[] slots;
	
	TypeProvider(Slot[] slots) {
		this.slots = slots;
	}
	
	public abstract Type newType(Type[] args);
}

public class BaseTypeProvider extends TypeProvider {
	final Struct clazz;
	
	final JBaseTypeProvider java_meta_type;

	BaseTypeProvider(Struct clazz) {
		super(new Slot[]{
			new Slot("args", clazz.args.length)
		});
		this.clazz = clazz;
		java_meta_type = new JBaseTypeProvider(clazz);
	}
	
}

public class ArrayTypeProvider extends TypeProvider {
	static final ArrayTypeProvider instance = new ArrayTypeProvider();
	private ArrayTypeProvider() {
		super(new Slot[]{
			new Slot("arg", 1)
		});
	}
}

public class ArgumentTypeProvider extends TypeProvider {
	public ArgumentTypeProvider() { super(new Slot[0]); }
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
		super(new Slot[]{
			new Slot("arg", 1)
		});
		this.clazz = clazz;
		this.field = clazz.getWrappedField(true);
	}
	public Type newType(Type[] args) {
		assert (args.length == 1);
		return WrapperType.newWrapperType(Type.newRefType(clazz,args));
	}
}

public class CallTypeProvider extends TypeProvider {
	static final CallTypeProvider[] instancies;
	static {
		instancies = new CallTypeProvider[16];
		for (int i=0; i < 16; i++)
			instancies[i] = new CallTypeProvider(i);
	}
	private CallTypeProvider(int arity) {
		super(new Slot[]{
			new Slot("args", arity),
			new Slot("ret", 1)
		});
	}
}

public class JBaseTypeProvider extends TypeProvider {
	final Struct clazz;
	JBaseTypeProvider(Struct clazz) {
		super(new Slot[0]);
		this.clazz = clazz;
	}
}


