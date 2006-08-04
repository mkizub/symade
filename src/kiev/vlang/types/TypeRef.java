package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;
import kiev.transf.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.be.java15.JTypeRef;
import kiev.be.java15.JType;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeRef extends ENode {

	public static final TypeRef[] emptyArray = new TypeRef[0];
	public static final TypeRef dummyNode = new TypeRef();
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  ≤ TypeRef;
	@virtual typedef JView = JTypeRef;
	//@virtual typedef TypeOfIdent = TypeDecl;

	@ref public Type	lnk;

	public TypeRef() {}
	
	private TypeRef(CoreType tp) {
		this.ident.name = tp.name;
		this.ident.symbol = tp.meta_type.tdecl.id;
		this.lnk = tp;
	}
	
	private TypeRef(ArgType tp) {
		this.ident.name = tp.name;
		this.ident.symbol = tp.meta_type.tdecl.id;
		this.lnk = tp;
	}
	
	public static TypeRef newTypeRef(Type tp)
		alias lfy operator new
	{
		if (tp instanceof CoreType)
			return new TypeRef((CoreType)tp);
		if (tp instanceof ASTNodeType)
			return new TypeExpr(newTypeRef(tp.getStruct().xtype),Operator.PostTypeAST);
		if (tp instanceof ArgType)
			return new TypeRef((ArgType)tp);
		if (tp instanceof ArrayType)
			return new TypeExpr(newTypeRef(tp.arg), Operator.PostTypeArray);
		if (tp instanceof WrapperType)
			return new TypeExpr(newTypeRef(tp.getEnclosedType()), Operator.PostTypeWrapper);
		if (tp instanceof CompaundType || tp instanceof XType)
			return new TypeNameRef(tp);
		if (tp instanceof CallType)
			return new TypeClosureRef((CallType)tp);
		throw new RuntimeException("Unknow type for TypeRef: "+tp.getClass());
	}
	
	public ASTNode getDummyNode() {
		return TypeRef.dummyNode;
	}

	public Type getType()
		alias fy operator $cast
	{
		return lnk;
	}
	
	public boolean isBound() {
		return lnk != null;
	}
	
	public boolean isArray() { return getType().isArray(); }
	public boolean checkResolved() { return getType().checkResolved(); } 
	public Struct getStruct() { if (lnk == null) return null; return lnk.getStruct(); }
	public TypeDecl getTypeDecl() { if (lnk == null) return null; return lnk.meta_type.tdecl; }
	public JType getJType() { return getType().getJType(); }

	public boolean preResolveIn() {
		((TypeRef)this).getType(); // calls resolving
		return false;
	}

	public boolean mainResolveIn() {
		((TypeRef)this).getType(); // calls resolving
		return false;
	}

	public void resolve(Type reqType) {
		if (reqType ≢ null && reqType ≉ Type.tpClass)
			toExpr(reqType);
		else
			getType(); // calls resolving
	}
	
	public boolean equals(Object o) {
		if (o instanceof Type) return this.lnk ≡ (Type)o;
		return this == o;
	}
	
	public String toString() {
		return String.valueOf(lnk);
	}
	
	public void toExpr(Type reqType) {
		Type st = getType();
		Struct s = st.getStruct();
		if (s != null && s.isPizzaCase()) {
			// Pizza case may be casted to int or to itself or super-class
			MetaPizzaCase meta = s.getMetaPizzaCase();
			if (meta == null)
				throw new RuntimeException("Internal error - can't find pizza case meta attr");
			Type tp = Type.getRealType(reqType,st);
			if !(reqType.isInteger() || tp.isInstanceOf(reqType))
				throw new CompilerException(this,"Pizza case "+tp+" cannot be casted to type "+reqType);
			if (meta.getFields().length != 0)
				throw new CompilerException(this,"Empty constructor for pizza case "+tp+" not found");
			if (reqType.isInteger()) {
				ENode expr = new ConstIntExpr(meta.tag);
				if( reqType ≢ Type.tpInt )
					expr = new CastExpr(pos,reqType,expr);
				replaceWithNodeResolve(reqType, expr);
			}
			else if (s.isSingleton()) {
				replaceWithNodeResolve(reqType, new SFldExpr(pos, s.resolveField(nameInstance)));
			}
			else {
				replaceWithResolve(reqType, fun ()->ENode {return new NewExpr(pos,tp,ENode.emptyArray);});
			}
			return;
		}
		if (s != null && s.isSingleton()) {
			replaceWithNodeResolve(reqType, new SFldExpr(pos, s.resolveField(nameInstance)));
			return;
		}
		throw new CompilerException(this,"Type "+this+" is not a singleton");
	}

	public Object doRewrite(RewriteContext ctx) {
		return this;
	}
}


