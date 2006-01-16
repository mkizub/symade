package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import syntax kiev.Syntax;

import kiev.vlang.types.TypeRef.TypeRefImpl;
import kiev.vlang.types.TypeRef.TypeRefView;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeExpr extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	private static KString opPVar  = KString.from("@");
	private static KString opRef   = KString.from("&");
	
	@node
	public static final class TypeExprImpl extends TypeRefImpl {
		@att public TypeRef					arg;
		@att public KString					op;
		public TypeExprImpl() {}
		public TypeExprImpl(int pos) { super(pos, null); }
	}
	@nodeview
	public static final view TypeExprView of TypeExprImpl extends TypeRefView {
		public TypeRef				arg;
		public KString				op;
	}

	@att public abstract virtual TypeRef				arg;
	@att public abstract virtual KString				op;
	
	public NodeView			getNodeView()			{ return new TypeExprView((TypeExprImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new TypeExprView((TypeExprImpl)this.$v_impl); }
	public TypeRefView		getTypeRefView()		{ return new TypeExprView((TypeExprImpl)this.$v_impl); }
	public TypeExprView		getTypeExprView()		{ return new TypeExprView((TypeExprImpl)this.$v_impl); }

	@getter public TypeRef		get$arg()			{ return this.getTypeExprView().arg; }
	@getter public KString		get$op()			{ return this.getTypeExprView().op; }
	@setter public void		set$arg(TypeRef val)	{ this.getTypeExprView().arg = val; }
	@setter public void		set$op(KString val)		{ this.getTypeExprView().op = val; }
	
	public TypeExpr() {
		super(new TypeExprImpl());
	}

	public TypeExpr(TypeRef arg, KString op) {
		super(new TypeExprImpl());
		this.pos = arg.pos;
		this.arg = arg;
		this.op = op;
	}

	public TypeExpr(TypeRef arg, Token op) {
		super(new TypeExprImpl());
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
	public Struct getStruct() {
		if (this.lnk != null)
			return this.lnk.getStruct();
		if (op == Constants.nameArrayOp)
			return null;
		DNode@ v;
		if (!PassInfo.resolveNameR(this,v,new ResInfo(this),op)) {
			if (op == opPVar)
				return WrapperType.tpWrappedPrologVar.getStruct();
			else if (op == opRef)
				return WrapperType.tpWrappedRefProxy.getStruct();
			else
				throw new CompilerException(this,"Typedef for type operator "+op+" not found");
		}
		if (v instanceof TypeDecl)
			return ((TypeDecl)v).getStruct();
		throw new CompilerException(this,"Expected to find type for "+op+", but found "+v);
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type tp = arg.getType();
		DNode@ v;
		if (op == Constants.nameArrayOp) {
			tp = new ArrayType(tp);
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
				if (v instanceof TypeDecl)
					t = ((TypeDecl)v).getType();
				else
					throw new CompilerException(this,"Expected to find type for "+op+", but found "+v);
			}
			t.checkResolved();
			TVarSet set = new TVarSet();
			if (t.getStruct().args.length != 1)
				throw new CompilerException(this,"Type '"+t+"' of type operator "+op+" must have 1 argument");
			set.append(t.getStruct().args[0].getAType(), tp);
			tp = t.applay(set);
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

