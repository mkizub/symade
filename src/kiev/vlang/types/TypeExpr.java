package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeExpr extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	static String opPVar   = "@";
	static String opRef    = "&";
	static String opAST    = "#";
	static String opWraper = "\u229b"; // ⊛
	
	@virtual typedef This  = TypeExpr;
	@virtual typedef VView = VTypeExpr;

	@att public TypeRef					arg;
	@att public String					op;

	@setter
	public void set$op(String value) {
		this.op = (value != null) ? value.intern() : null;
	}
	
	@nodeview
	public static final view VTypeExpr of TypeExpr extends VTypeRef {
		public TypeRef				arg;
		public String				op;
	}

	public TypeExpr() {}

	public TypeExpr(TypeRef arg, String op) {
		this.pos = arg.pos;
		this.arg = arg;
		this.op = op;
	}

	public TypeExpr(TypeRef arg, Token op) {
		this.arg = arg;
		if (op.kind == ParserConstants.OPERATOR_LRBRACKETS)
			this.op = Constants.nameArrayOp;
		else
			this.op = op.image;
		this.pos = op.getPos();
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type tp = arg.getType();
		DNode@ v;
		if (op == Constants.nameArrayOp) {
			tp = new ArrayType(tp);
		}
		else if (op == opWraper) {
			tp = new WrapperType((CompaundType)tp);
		}
		else if (op == opAST) {
			tp = new ASTNodeType(tp.getStruct());
		}
		else {
			Type t;
			if (!PassInfo.resolveNameR(((TypeExpr)this),v,new ResInfo(this),op)) {
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
			TVarBld set = new TVarBld();
			if (t.getStruct().args.length != 1)
				throw new CompilerException(this,"Type '"+t+"' of type operator "+op+" must have 1 argument");
			set.append(t.getStruct().args[0].getAType(), tp);
			tp = t.applay(set);
		}
		this.lnk = tp;
		return tp;
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
			else if (op == opAST)
				return arg.getStruct();
			else
				throw new CompilerException(this,"Typedef for type operator "+op+" not found");
		}
		if (v instanceof TypeDecl)
			return ((TypeDecl)v).getStruct();
		throw new CompilerException(this,"Expected to find type for "+op+", but found "+v);
	}
	public TypeDecl getTypeDecl() {
		if (this.lnk != null)
			return this.lnk.meta_type.tdecl;
		if (op == Constants.nameArrayOp)
			return ArrayMetaType.instance.tdecl;
		DNode@ v;
		if (!PassInfo.resolveNameR(this,v,new ResInfo(this),op)) {
			if (op == opPVar)
				return WrapperType.tpWrappedPrologVar.meta_type.tdecl;
			else if (op == opRef)
				return WrapperType.tpWrappedRefProxy.meta_type.tdecl;
			else if (op == opAST)
				return ASTNodeMetaType.instance(arg.getStruct()).tdecl;
			else
				throw new CompilerException(this,"Typedef for type operator "+op+" not found");
		}
		if (v instanceof TypeDecl)
			return (TypeDecl)v;
		throw new CompilerException(this,"Expected to find type for "+op+", but found "+v);
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

