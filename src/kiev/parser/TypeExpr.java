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
public class TypeExpr extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	private static KString opPVar  = KString.from("@");
	private static KString opRef   = KString.from("&");
	
	@att public TypeRef		arg;
	@att public KString		op;

	public TypeExpr() {
	}

	public TypeExpr(TypeRef arg, KString op) {
		this.pos = arg.pos;
		this.arg = arg;
		this.op = op;
	}

	public TypeExpr(TypeRef arg, Token op) {
		this.arg = arg;
		if (op.kind == ParserConstants.OPERATOR_LRBRACKETS)
			this.op = Constants.nameArrayOp;
		else
			this.op = KString.from(op.image);
		this.pos = op.getPos();
	}

	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
	    Type tp = arg.getType();
		DNode@ v;
		if (op == Constants.nameArrayOp) {
			tp = Type.newArrayType(tp);
		} else {
			Type t;
			if (!PassInfo.resolveNameR(this,v,new ResInfo(this),op)) {
				if (op == opPVar) {
					t = WrapperType.tpWrappedPrologVar;
				}
				else if (op == opRef) {
					Kiev.reportWarning(this, "Typedef for "+op+" not found, assuming wrapper of "+Type.tpRefProxy);
					t = WrapperType.tpWrappedRefProxy;
				}
				else
					throw new CompilerException(this,"Typedef for type operator "+op+" not found");
			} else {
				if (v instanceof TypeDef)
					t = ((TypeDef)v).getType();
				else
					throw new CompilerException(this,"Expected to find type for "+op+", but found "+v);
			}
			if (t.args.length != 1)
				throw new CompilerException(this,"Type '"+t+"' of type operator "+op+" must have 1 argument");
			t.checkResolved();
			if (t.isWrapper()) {
				BaseType ut = Type.newRefType(((WrapperType)t).getUnwrappedType(),new Type[]{tp});
				tp = WrapperType.newWrapperType(ut);
			} else {
				tp = Type.newRefType((BaseType)t,new Type[]{tp});
			}
		}
		this.lnk = tp;
		return tp;
	}

	public String toString() {
		if (this.lnk != null)
			return this.lnk.toString();
		return String.valueOf(arg)+op;
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

