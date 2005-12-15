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
public class TypeCallRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@att public final NArr<TypeRef>		args;
	@att public TypeRef					ret;

	public TypeCallRef() {
	}

	public TypeCallRef(MethodType mt) {
		this.ret = new TypeRef(mt.ret);
		foreach (Type a; mt.args)
			this.args += new TypeRef(a);
		this.lnk = mt;
	}

	public boolean isBound() {
		return true;
	}

	public void callbackChildChanged(AttrSlot attr) {
		this.lnk = null;
		if (parent != null && pslot != null) {
			parent.callbackChildChanged(pslot);
		}
	}

	public MethodType getMType() {
		if (this.lnk != null)
			return (MethodType)this.lnk;
		Type rt = ret.getType();
		Type[] atypes = new Type[args.length];
		for(int i=0; i < atypes.length; i++) {
			atypes[i] = args[i].getType();
		}
		this.lnk = MethodType.newMethodType(null,atypes,rt);
		return (MethodType)this.lnk;
	}
	public Type getType() {
		return getMType();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1)
				sb.append(',');
		}
		sb.append(")->").append(ret);
		return sb.toString();
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}
