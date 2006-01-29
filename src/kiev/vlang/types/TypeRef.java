package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;
import kiev.transf.*;

import kiev.vlang.ENode.ENodeImpl;
import kiev.vlang.ENode.ENodeView;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.be.java.JTypeRef;
import kiev.be.java.JType;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeset
public class TypeRef extends ENode {

	public static final TypeRef dummyNode = new TypeRef();
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeRef;
	@virtual typedef NImpl = TypeRefImpl;
	@virtual typedef VView = TypeRefView;
	@virtual typedef JView = JTypeRef;

	@nodeimpl
	public static class TypeRefImpl extends ENodeImpl {
		@virtual typedef ImplOf = TypeRef;
		@ref public Type	lnk;
	}
	@nodeview
	public static view TypeRefView of TypeRefImpl extends ENodeView {
		public Type	lnk;

		public boolean preResolveIn() {
			getNode().getType(); // calls resolving
			return false;
		}
	
		public boolean mainResolveIn() {
			getNode().getType(); // calls resolving
			return false;
		}
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public TypeRef() {
		super(new TypeRefImpl());
	}
	
	public TypeRef(TypeRefImpl $view) {
		super($view);
	}

	public TypeRef(Type tp) {
		this();
		this.lnk = tp;
		
	}
	public TypeRef(int pos) {
		this();
		this.pos = pos;
	}
	public TypeRef(int pos, Type tp) {
		this();
		this.pos = pos;
		this.lnk = tp;
	}
	
	public ASTNode getDummyNode() {
		return TypeRef.dummyNode;
	}
	
	public boolean isBound() {
		return lnk != null;
	}
	
	public boolean isArray() { return getType().isArray(); }
	public boolean checkResolved() { return getType().checkResolved(); } 
	public Struct getStruct() { if (lnk == null) return null; return lnk.getStruct(); }
	public JType getJType() { return getType().getJType(); }

	public Type getType()
		alias operator(210,fy,$cast)
	{
		return lnk;
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
	
	public Dumper toJava(Dumper dmp) {
		return lnk.toJava(dmp);
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
				ENode expr = new ConstIntExpr(meta.getTag());
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
	
	public static Enumeration<Type> linked_elements(NArr<TypeRef> arr) {
		Vector<Type> tmp = new Vector<Type>();
		foreach (TypeRef tr; arr) { if (tr.lnk != null) tmp.append(tr.lnk); }
		return tmp.elements();
	}
}


