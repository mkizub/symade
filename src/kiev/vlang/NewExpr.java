package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.Operator.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.be.java.JNewExpr;
import kiev.be.java.JNewArrayExpr;
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
	@virtual typedef VView = NewExprView;
	@virtual typedef JView = JNewExpr;

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
	public static final view NewExprView of NewExprImpl extends ENodeView {
		public				TypeRef			type;
		public:ro	NArr<ENode>		args;
		public				ENode			outer;
		public				ENode			temp_expr;
		public				Struct			clazz;
		public				Method			func;

		public int		getPriority() { return Constants.opAccessPriority; }

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
			if( sup.isInterface() ) {
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

	public void resolve(Type reqType) {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		CompaundType type;
		if (this.clazz != null) {
			type = this.clazz.ctype;
		} else {
			Type t = this.type.getType();
			if (t instanceof CTimeType)
				type = t.getEnclosedType();
			else if (t instanceof ArgType)
				type = (CompaundType)t.getSuperType();
			else
				type = (CompaundType)t;
		}
		if!(type instanceof CompaundType)
			Kiev.reportWarning(this,"Instantiation of non-concrete type "+type+" ???");
		if( type.isAnonymouseClazz() ) {
			type.getStruct().resolveDecl();
		}
//		if( !type.isArgument() && (type.isAbstract() || !type.isClazz()) ) {
//			if (type.isUnerasable())
//				/*throw new CompilerException*/ Kiev.reportWarning(this,"Abstract unerasable class "+type+" instantiation");
//			else
//				Kiev.reportWarning(this,"Abstract erasable class "+type+" instantiation");
//		}
		if (outer == null && type.clazz.ometa_type != null) {
			if( ctx_method==null || ctx_method.isStatic() )
				throw new CompilerException(this,"'new' for inner class requares outer instance specification");
			outer = new ThisExpr(pos);
		}
		if( outer != null ) {
			outer.resolve(null);
			type = (CompaundType)type.bind(new TVarBld(type.clazz.ometa_type.tdef.getAType(), outer.getType()));
		}
		for(int i=0; i < args.length; i++)
			args[i].resolve(null);
		if( type.clazz.isTypeUnerasable() )
			ctx_clazz.getRView().accessTypeInfoField(this,type,false); // Create static field for this type typeinfo
		// Don't try to find constructor of argument type
		if( type.isArgument() ) {
			if( !type.isUnerasable())
				throw new CompilerException(this,"Can't create an instance of erasable argument type "+type);
			setResolved(true);
			return;
		}
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		CallType mt = (CallType)Type.getRealType(type,new CallType(ta,type));
		Method@ m;
		// First try overloaded 'new', than real 'new'
		if( this.clazz == null && (ctx_method==null || !ctx_method.name.equals(nameNewOp)) ) {
			ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports);
			if (PassInfo.resolveBestMethodR(type,m,info,nameNewOp,mt)) {
				CallExpr n = new CallExpr(pos,new TypeRef(type),(Method)m,args.delToArray());
				replaceWithNodeResolve(n);
				return;
			}
		}
		mt = (CallType)Type.getRealType(type,new CallType(ta,Type.tpVoid));
		ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports|ResInfo.noStatic);
		if( PassInfo.resolveBestMethodR(type,m,info,nameInit,mt) ) {
			func = m;
			m.makeArgs(args,type);
			for(int i=0; i < args.length; i++)
				args[i].resolve(mt.arg(i));
		}
		else {
			throw new CompilerException(this,"Can't find apropriative initializer for "+
				Method.toString(nameInit,args,Type.tpVoid)+" for "+type);
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}

	public Dumper toJava(Dumper dmp) {
		Type tp = type.getType();
		if( !tp.isReference() ) {
			return dmp.append('0');
		}
		if( !tp.isAnonymouseClazz() ) {
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
		if( tp.isAnonymouseClazz() ) {
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
	@virtual typedef VView = NewArrayExprView;
	@virtual typedef JView = JNewArrayExpr;

	@nodeimpl
	public static final class NewArrayExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewArrayExpr;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		@att public int					dim;
		@ref public ArrayType			arrtype;
	}
	@nodeview
	public static final view NewArrayExprView of NewArrayExprImpl extends ENodeView {
		public				TypeRef			type;
		public:ro	NArr<ENode>		args;
		public				int				dim;
		public				ArrayType		arrtype;

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
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

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

	public Type getType() {
		return arrtype;
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		Type type = this.type.getType();
		ArrayType art = this.arrtype;
		for(int i=0; i < args.length; i++)
			if( args[i] != null )
				args[i].resolve(Type.tpInt);
		if( type.isArgument() ) {
			if( !type.isUnerasable())
				throw new CompilerException(this,"Can't create an array of erasable argument type "+type);
			if( ctx_method==null || ctx_method.isStatic() )
				throw new CompilerException(this,"Access to argument "+type+" from static method");
			ENode ti = ctx_clazz.getRView().accessTypeInfoField(this,type,false);
			if( dim == 1 ) {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,ti,
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),Type.tpObject,Type.tpInt),
						new ENode[]{~args[0]}
					)));
				return;
			} else {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,ti,
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),Type.tpObject,new ArrayType(Type.tpInt)),
						new ENode[]{
							new NewInitializedArrayExpr(pos,new TypeRef(Type.tpInt),1,args.delToArray())
						}
					)));
				return;
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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
	@virtual typedef VView = NewInitializedArrayExprView;
	@virtual typedef JView = JNewInitializedArrayExpr;

	@nodeimpl
	public static final class NewInitializedArrayExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewInitializedArrayExpr;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		@att public int[]				dims;
		@ref public Type				arrtype;
	}
	@nodeview
	public static final view NewInitializedArrayExprView of NewInitializedArrayExprImpl extends ENodeView {
		public				TypeRef			type;
		public:ro			NArr<ENode>		args;
		public				int[]			dims;
		public				ArrayType		arrtype;
		
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
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

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

	public Type getType() { return arrtype; }

	public int getElementsNumber(int i) { return dims[i]; }

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		Type type;
		if( this.type == null ) {
			if( !reqType.isArray() )
				throw new CompilerException(this,"Type "+reqType+" is not an array type");
			type = reqType;
			this.arrtype = (ArrayType)reqType;
			Type art = reqType;
			int dim = 0;
			while (art instanceof ArrayType) { dim++; art = art.arg; }
			this.type = new TypeRef(art);
			this.dims = new int[dim];
			this.dims[0] = args.length;
		} else {
			type = this.type.getType();
			for (int dim = this.dim; dim > 0; dim--)
				type = new ArrayType(type);
		}
		if( !type.isArray() )
			throw new CompilerException(this,"Type "+type+" is not an array type");
		for(int i=0; i < args.length; i++)
			args[i].resolve(arrtype.arg);
		for(int i=1; i < dims.length; i++) {
			int n;
			for(int j=0; j < args.length; j++) {
				if( args[j] instanceof NewInitializedArrayExpr )
					n = ((NewInitializedArrayExpr)args[j]).getElementsNumber(i-1);
				else
					n = 1;
				if( dims[i] < n ) dims[i] = n;
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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
		@att public Block			body;
		@att public Struct				clazz;
		@ref public CallType			ctype;
	}
	@nodeview
	public static abstract view NewClosureView of NewClosureImpl extends ENodeView {
		public TypeRef			type_ret;
		public NArr<FormPar>	params;
		public Block		body;
		public Struct			clazz;
		public CallType			ctype;

		public int		getPriority() { return Constants.opAccessPriority; }
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
		return "fun "+getType();
	}

	public Type getType() {
		if (ctype != null)
			return ctype;
		Vector<Type> args = new Vector<Type>();
		foreach (FormPar fp; params)
			args.append(fp.getType());
		ctype = new CallType(args.toArray(), type_ret.getType(), true);
		return ctype;
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
		Var@ p;
	{
		p @= params,
		p.name.equals(name),
		node ?= p
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		clazz.resolveDecl();
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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

