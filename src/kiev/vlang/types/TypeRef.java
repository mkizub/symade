package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;
import kiev.transf.*;

import kiev.vlang.ENode.ENodeImpl;
import kiev.vlang.ENode.ENodeView;

import kiev.be.java.JNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JTypeRefView;
import kiev.be.java.JType;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeRef extends ENode {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static class TypeRefImpl extends ENodeImpl {
		public TypeRefImpl() {}
		public TypeRefImpl(int pos, Type tp) { super(pos); this.lnk = tp; }
		@ref public Type	lnk;
	}
	@nodeview
	public static view TypeRefView of TypeRefImpl extends ENodeView {
		public Type	lnk;
	}

	@ref public abstract virtual forward Type	lnk;
	
	public NodeView			getNodeView()		{ return new TypeRefView((TypeRefImpl)this.$v_impl); }
	public ENodeView		getENodeView()		{ return new TypeRefView((TypeRefImpl)this.$v_impl); }
	public TypeRefView		getTypeRefView()	{ return new TypeRefView((TypeRefImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		{ return new JTypeRefView((TypeRefImpl)this.$v_impl); }
	public JENodeView		getJENodeView()		{ return new JTypeRefView((TypeRefImpl)this.$v_impl); }
	public JTypeRefView		getJTypeRefView()	{ return new JTypeRefView((TypeRefImpl)this.$v_impl); }

	@getter public Type		get$lnk()					{ return this.getTypeRefView().lnk; }
	@setter public void		set$lnk(Type val)			{ this.getTypeRefView().lnk = val; }
	
	public TypeRef() {
		super(new TypeRefImpl());
	}
	
	public TypeRef(TypeRefImpl $view) {
		super($view);
	}

	public TypeRef(Type tp) {
		super(new TypeRefImpl(0, tp));
	}
	public TypeRef(int pos) {
		super(new TypeRefImpl(pos, null));
	}
	public TypeRef(int pos, Type tp) {
		super(new TypeRefImpl(pos, tp));
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
	
	public boolean preResolveIn(TransfProcessor proc) {
		getType(); // calls resolving
		return false;
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		getType(); // calls resolving
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


