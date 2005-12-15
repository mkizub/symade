package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeWithArgsRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@att public TypeRef					base_type;
	@att public final NArr<TypeRef>		args;

	public TypeWithArgsRef() {
	}

	public TypeWithArgsRef(TypeRef base) {
		this.pos = base.pos;
		this.base_type = base;
	}

	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type tp = base_type.getType();
		if (tp == null || !(tp instanceof BaseType))
			throw new CompilerException(this,"Type "+base_type+" is not found");
		Type[] atypes = new Type[args.length];
		for(int i=0; i < atypes.length; i++) {
			atypes[i] = args[i].getType();
			if (atypes[i] == null)
				throw new CompilerException(this,"Type "+args[i]+" is not found");
		}
		this.lnk = Type.newRefType((BaseType)tp,atypes);
		return this.lnk;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (this.lnk != null)
			sb.append(this.lnk.getClazzName());
		else
			sb.append(base_type);
		sb.append('<');
		for (int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if (i < args.length-1) sb.append(',');
		}
		return sb.append('>').toString();
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}
