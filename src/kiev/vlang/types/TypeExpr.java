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

	public static final Hashtable<String,Struct>	AllNodes = new Hashtable<String,Struct>(256);

	@virtual typedef This  = TypeExpr;

	@att public TypeRef			arg;
	@ref public Operator		op;

	public TypeExpr() {}

	public TypeExpr(TypeRef arg, Operator op) {
		this.pos = arg.pos;
		this.arg = arg;
		this.ident.name = op.name;
	}

	public TypeExpr(TypeRef arg, Token op) {
		this.arg = arg;
		if (op.kind == ParserConstants.OPERATOR_LRBRACKETS) {
			this.op = Operator.PostTypeArray;
			this.ident.name = this.op.name;
		} else {
			this.ident.name = ("T "+op.image).intern();
		}
		this.pos = op.getPos();
	}

	public Operator getOp() { return op; }

	public ENode[] getArgs() { return new ENode[]{arg}; }

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		if (this.op == null) {
			Operator op = Operator.getOperator(ident.name);
			if (op == null)
				throw new CompilerException(this, "Cannot find type operator: "+ident.name);
			this.op = op;
		}
		if (op == Operator.PostTypeAST) {
			Struct s = AllNodes.get(arg.toString());
			if (s != null) {
				arg.lnk = s.xtype;
				this.lnk = new ASTNodeType(s);
				return this.lnk;
			}
		}
		Type tp = arg.getType();
		DNode@ v;
		if (op == Operator.PostTypeArray) {
			tp = new ArrayType(tp);
		}
		else if (op == Operator.PostTypeWrapper) {
			tp = new WrapperType((CompaundType)tp);
		}
		else if (op == Operator.PostTypeAST) {
			tp = new ASTNodeType(tp.getStruct());
		}
		else {
			Type t;
			if (!PassInfo.resolveNameR(((TypeExpr)this),v,new ResInfo(this,ident.name))) {
				if (op == Operator.PostTypePVar) {
					t = WrapperType.tpWrappedPrologVar;
				}
				else if (op == Operator.PostTypeVararg) {
					t = StdTypes.tpVararg;
				}
				else if (op == Operator.PostTypeRef) {
					Kiev.reportWarning(this, "Typedef for "+op+" not found, assuming wrapper of "+Type.tpRefProxy);
					t = WrapperType.tpWrappedRefProxy;
				}
				else
					throw new CompilerException(this,"Typedef for type operator "+ident+" not found");
			} else {
				if (v instanceof TypeDecl)
					t = ((TypeDecl)v).getType();
				else
					throw new CompilerException(this,"Expected to find type for "+ident+", but found "+v);
			}
			t.checkResolved();
			TVarBld set = new TVarBld();
			if (t.meta_type.tdecl.args.length != 1)
				throw new CompilerException(this,"Type '"+t+"' of type operator "+ident+" must have 1 argument");
			set.append(t.meta_type.tdecl.args[0].getAType(), tp);
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
		if (ident.name == Operator.PostTypeArray.name || ident.name == Operator.PostTypeVararg.name)
			return null;
		DNode@ v;
		if (!PassInfo.resolveNameR(this,v,new ResInfo(this,ident.name))) {
			if (op == Operator.PostTypePVar)
				return WrapperType.tpWrappedPrologVar.getStruct();
			else if (op == Operator.PostTypeRef)
				return WrapperType.tpWrappedRefProxy.getStruct();
			else if (op == Operator.PostTypeAST)
				return arg.getStruct();
			else
				throw new CompilerException(this,"Typedef for type operator "+ident+" not found");
		}
		if (v instanceof TypeDecl)
			return ((TypeDecl)v).getStruct();
		throw new CompilerException(this,"Expected to find type for "+ident+", but found "+v);
	}
	public TypeDecl getTypeDecl() {
		if (this.lnk != null)
			return this.lnk.meta_type.tdecl;
		if (ident.name == Operator.PostTypeArray.name)
			return ArrayMetaType.instance.tdecl;
		if (ident.name == Operator.PostTypeVararg.name)
			return (TypeDecl)Env.resolveStruct("kiev.stdlib._Vararg_");
		DNode@ v;
		if (!PassInfo.resolveNameR(this,v,new ResInfo(this,ident.name))) {
			if (op == Operator.PostTypePVar)
				return WrapperType.tpWrappedPrologVar.meta_type.tdecl;
			else if (op == Operator.PostTypeRef)
				return WrapperType.tpWrappedRefProxy.meta_type.tdecl;
			else if (op == Operator.PostTypeAST)
				return ASTNodeMetaType.instance(arg.getStruct()).tdecl;
			else
				throw new CompilerException(this,"Typedef for type operator "+ident+" not found");
		}
		if (v instanceof TypeDecl)
			return (TypeDecl)v;
		throw new CompilerException(this,"Expected to find type for "+ident+", but found "+v);
	}

	public String toString() {
		if (this.lnk != null)
			return this.lnk.toString();
		if (op != null)
			return op.toString(this);
		return String.valueOf(arg)+ident.name.substring(2);
	}
}

