package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.Operator.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.ir.java.RNewExpr;
import kiev.be.java.JNewExpr;
import kiev.ir.java.RNewArrayExpr;
import kiev.be.java.JNewArrayExpr;
import kiev.ir.java.RNewInitializedArrayExpr;
import kiev.be.java.JNewInitializedArrayExpr;
import kiev.be.java.JNewClosure;
import kiev.ir.java.RNewClosure;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public final class NewExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewExpr;
	@virtual typedef NImpl = NewExprImpl;
	@virtual typedef VView = VNewExpr;
	@virtual typedef JView = JNewExpr;
	@virtual typedef RView = RNewExpr;

	@nodeimpl
	public static final class NewExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewExpr;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		@att public ENode				outer;
		@att public ENode				temp_expr;
		@att public Struct				clazz; // if this new expression defines new class
		@ref public Method				func;
	}
	@nodeview
	public static view NewExprView of NewExprImpl extends ENodeView {
		public		TypeRef				type;
		public:ro	NArr<ENode>			args;
		public		ENode				outer;
		public		ENode				temp_expr;
		public		Struct				clazz;
		public		Method				func;

		public int		getPriority() { return Constants.opAccessPriority; }

		public Type getType() {
			if (this.clazz != null)
				return this.clazz.ctype;
			Type type = this.type.getType();
			Struct clazz = type.getStruct();
			if (outer == null && type.getStruct() != null && type.getStruct().ometa_type != null) {
				if (ctx_method != null || !ctx_method.isStatic())
					outer = new ThisExpr(pos);
			}
			if (outer == null)
				return type;
			TVarBld vset = new TVarBld(
				type.getStruct().ometa_type.tdef.getAType(),
				new OuterType(type.getStruct(),outer.getType()) );
			return type.rebind(vset);
		}
	}

	@nodeview
	public static final view VNewExpr of NewExprImpl extends NewExprView {
		public boolean preResolveIn() {
			if( clazz == null )
				return true;
			Type tp = type.getType();
			tp.checkResolved();
			// Local anonymouse class
			CompaundType sup  = (CompaundType)tp;
			clazz.setResolved(true);
			clazz.setLocal(true);
			clazz.setAnonymouse(true);
			clazz.setStatic(ctx_method==null || ctx_method.isStatic());
			TypeRef sup_tr = this.type.ncopy();
			if( sup.clazz.isInterface() ) {
				clazz.super_type = Type.tpObject;
				clazz.interfaces.add(sup_tr);
			} else {
				clazz.super_bound = sup_tr;
			}
	
			{
				// Create default initializer, if number of arguments > 0
				if( args.length > 0 ) {
					Constructor init = new Constructor(ACC_PUBLIC);
					for(int i=0; i < args.length; i++) {
						args[i].resolve(null);
						init.params.append(new FormPar(pos,KString.from("arg$"+i),args[i].getType(),FormPar.PARAM_LVAR_PROXY,ACC_FINAL));
					}
					init.pos = pos;
					init.body = new Block(pos);
					init.setPublic();
					clazz.addMethod(init);
				}
			}
	
			// Process inner classes and cases
			Kiev.runProcessorsOn(clazz);
			return true;
		}
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public NewExpr() {
		super(new NewExprImpl());
	}

	public NewExpr(int pos, Type type, ENode[] args) {
		this();
		this.pos = pos;
		this.type = new TypeRef(type);
		foreach (ENode e; args) this.args.append(e);
	}

	public NewExpr(int pos, TypeRef type, ENode[] args) {
		this();
		this.pos = pos;
		this.type = type;
		foreach (ENode e; args) this.args.append(e);
	}

	public NewExpr(int pos, Type type, ENode[] args, ENode outer) {
		this(pos,type,args);
		this.outer = outer;
	}

	public NewExpr(int pos, TypeRef type, ENode[] args, ENode outer) {
		this(pos,type,args);
		this.outer = outer;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(type).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		Type tp = type.getType();
		if( !tp.isReference() ) {
			return dmp.append('0');
		}
		if( !tp.getStruct().isAnonymouse() ) {
			dmp.append("new ").append(tp).append('(');
		} else {
			if( tp.getStruct().interfaces.length > 0 )
				dmp.append("new ").append(tp.getStruct().interfaces[0].getStruct().name).append('(');
			else
				dmp.append("new ").append(tp.getStruct().super_type.clazz.name).append('(');
		}
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		dmp.append(')');
		if( tp.getStruct().isAnonymouse() ) {
			Struct cl = tp.getStruct();
			dmp.space().append('{').newLine(1);
			foreach (DNode n; cl.members)
				n.toJavaDecl(dmp).newLine();
			dmp.newLine(-1).append('}').newLine();
		}
		return dmp;
	}
}

@nodeset
public final class NewArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewArrayExpr;
	@virtual typedef NImpl = NewArrayExprImpl;
	@virtual typedef VView = VNewArrayExpr;
	@virtual typedef JView = JNewArrayExpr;
	@virtual typedef RView = RNewArrayExpr;

	@nodeimpl
	public static final class NewArrayExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewArrayExpr;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		@att public int					dim;
		@ref public ArrayType			arrtype;
	}
	@nodeview
	public static  view NewArrayExprView of NewArrayExprImpl extends ENodeView {
		public		TypeRef				type;
		public:ro	NArr<ENode>			args;
		public		int					dim;
		public		ArrayType			arrtype;

		public Type get$arrtype() {
			ArrayType art = ((NewArrayExprImpl)this).arrtype;
			if (art != null)
				return art;
			art = new ArrayType(type.getType());
			for(int i=1; i < dim; i++) art = new ArrayType(art);
			((NewArrayExprImpl)this).arrtype = art;
			return art;
		}

		public int		getPriority() { return Constants.opAccessPriority; }

		public Type getType() { return arrtype; }
	}
	@nodeview
	public static final view VNewArrayExpr of NewArrayExprImpl extends NewArrayExprView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public NewArrayExpr() {
		super(new NewArrayExprImpl());
	}

	public NewArrayExpr(int pos, TypeRef type, ENode[] args, int dim) {
		this();
		this.pos = pos;
		this.type = type;
		foreach (ENode e; args) this.args.append(e);
		this.dim = dim;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(type.toString());
		for(int i=0; i < dim; i++) {
			sb.append('[');
			if( i < args.length && args[i] != null ) sb.append(args[i].toString());
			sb.append(']');
		}
		return sb.toString();
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("new ").append(type);
		for(int i=0; i < dim; i++) {
			dmp.append('[');
			if( i < args.length && args[i] != null ) args[i].toJava(dmp);
			dmp.append(']');
		}
		return dmp;
	}
}

@nodeset
public final class NewInitializedArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewInitializedArrayExpr;
	@virtual typedef NImpl = NewInitializedArrayExprImpl;
	@virtual typedef VView = VNewInitializedArrayExpr;
	@virtual typedef JView = JNewInitializedArrayExpr;
	@virtual typedef RView = RNewInitializedArrayExpr;

	@nodeimpl
	public static final class NewInitializedArrayExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewInitializedArrayExpr;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		@att public int[]				dims;
		@ref public Type				arrtype;
	}
	@nodeview
	public static view NewInitializedArrayExprView of NewInitializedArrayExprImpl extends ENodeView {
		public		TypeRef				type;
		public:ro	NArr<ENode>			args;
		public		int[]				dims;
		public		ArrayType			arrtype;
		
		@getter public final int	get$dim()	{ return this.dims.length; }

		public Type get$arrtype() {
			ArrayType art = ((NewInitializedArrayExprImpl)this).arrtype;
			if (art != null)
				return art;
			art = new ArrayType(type.getType());
			for(int i=1; i < dim; i++) art = new ArrayType(art);
			((NewInitializedArrayExprImpl)this).arrtype = art;
			return art;
		}

		public int		getPriority() { return Constants.opAccessPriority; }

		public Type getType() { return arrtype; }
	}
	@nodeview
	public static final view VNewInitializedArrayExpr of NewInitializedArrayExprImpl extends NewInitializedArrayExprView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public NewInitializedArrayExpr() {
		super(new NewInitializedArrayExprImpl());
	}

	public NewInitializedArrayExpr(int pos, TypeRef type, int dim, ENode[] args) {
		this();
		this.pos = pos;
		this.type = type;
		dims = new int[dim];
		dims[0] = args.length;
		this.args.addAll(args);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(type.toString());
		for(int i=0; i < dim; i++) sb.append("[]");
		sb.append('{');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]+",");
		}
		sb.append('}');
		return sb.toString();
	}

	public int getElementsNumber(int i) { return dims[i]; }

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("new ").append(arrtype);
		dmp.append('{');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 ) dmp.append(',').space();
		}
		dmp.append('}');
		return dmp;
	}
}

@nodeset
public final class NewClosure extends ENode implements ScopeOfNames {
	
	@dflow(out="this:in") private static class DFI {
	@dflow(in="this:in")	Block		body;
	}


	@virtual typedef This  = NewClosure;
	@virtual typedef NImpl = NewClosureImpl;
	@virtual typedef VView = VNewClosure;
	@virtual typedef JView = JNewClosure;
	@virtual typedef RView = RNewClosure;

	@nodeimpl
	public static final class NewClosureImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewClosure;
		@att public TypeRef				type_ret;
		@att public NArr<FormPar>		params;
		@att public Block				body;
		@att public Struct				clazz;
		@ref public CallType			ctype;
	}
	@nodeview
	public static abstract view NewClosureView of NewClosureImpl extends ENodeView {
		public TypeRef			type_ret;
		public NArr<FormPar>	params;
		public Block			body;
		public Struct			clazz;
		public CallType			ctype;

		public int		getPriority() { return Constants.opAccessPriority; }

		public Type getType() {
			if (ctype != null)
				return ctype;
			Vector<Type> args = new Vector<Type>();
			foreach (FormPar fp; params)
				args.append(fp.getType());
			ctype = new CallType(args.toArray(), type_ret.getType(), true);
			return ctype;
		}
	}
	@nodeview
	public static final view VNewClosure of NewClosureImpl extends NewClosureView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public NewClosure() {
		super(new NewClosureImpl());
	}

	public NewClosure(int pos) {
		this();
		this.pos = pos;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("fun (");
		for (int i=0; i < params.length; i++) {
			if (i > 0) sb.append(",");
			sb.append(params[i].vtype).append(' ').append(params[i].name);
		}
		sb.append(")->").append(type_ret).append(" {...}");
		return sb.toString();
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
		Var@ p;
	{
		p @= params,
		p.name.equals(name),
		node ?= p
	}
	
	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		CallType type = (CallType)this.getType();
		Struct cl = clazz;
		dmp.append("new ").append(cl.super_type.clazz.name).append('(')
			.append(String.valueOf(type.arity)).append(')');
		dmp.space().append('{').newLine(1);
		foreach (DNode n; cl.members)
			n.toJavaDecl(dmp).newLine();
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

