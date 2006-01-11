package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.Operator.*;
import kiev.transf.BackendProcessor;

import kiev.be.java.JNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JNewExprView;
import kiev.be.java.JNewArrayExprView;
import kiev.be.java.JNewInitializedArrayExprView;
import kiev.be.java.JNewClosureView;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final class NewExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef NImpl = NewExprImpl;
	@virtual typedef VView = NewExprView;
	@virtual typedef JView = JNewExprView;

	@node
	public static final class NewExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewExpr;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		@att public ENode				outer;
		@att public ENode				temp_expr;
		@att public Struct				clazz; // if this new expression defines new class
		@ref public Method				func;

		public NewExprImpl() {}
		public NewExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view NewExprView of NewExprImpl extends ENodeView {
		public				TypeRef			type;
		public access:ro	NArr<ENode>		args;
		public				ENode			outer;
		public				ENode			temp_expr;
		public				Struct			clazz;
		public				Method			func;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public NewExpr() {
		super(new NewExprImpl());
	}

	public NewExpr(int pos, Type type, ENode[] args) {
		super(new NewExprImpl(pos));
		this.type = new TypeRef(type);
		foreach (ENode e; args) this.args.append(e);
	}

	public NewExpr(int pos, TypeRef type, ENode[] args) {
		super(new NewExprImpl(pos));
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
		Type type = this.type.getType();
		if (outer == null) return type;
		return type.bind(new TVarSet(type.getOuterArg(), outer.getType()));
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		BaseType type;
		{
			Type t = this.type.getType();
			if (t.isWrapper())
				type = ((WrapperType)t).getUnwrappedType();
			else if (t instanceof ArgumentType)
				type = (BaseType)((ArgumentType)t).super_type;
			else
				type = (BaseType)t;
		}
		if( type.isAnonymouseClazz() ) {
			type.getStruct().resolveDecl();
		}
		if( !type.isArgument() && (type.isAbstract() || !type.isClazz()) ) {
			throw new CompilerException(this,"Abstract class "+type+" instantiation");
		}
		if( outer == null &&
			( (!type.isStaticClazz() && type.isLocalClazz())
			|| (!type.isStaticClazz() && !type.getStruct().package_clazz.isPackage()) )
		)
		{
			if( ctx_method==null || ctx_method.isStatic() )
				throw new CompilerException(this,"'new' for inner class requares outer instance specification");
			outer = new ThisExpr(pos);
		}
		if( outer != null ) {
			outer.resolve(null);
			type = (BaseType)type.bind(new TVarSet(type.getOuterArg(), outer.getType()));
		}
		for(int i=0; i < args.length; i++)
			args[i].resolve(null);
		if( type.clazz.isTypeUnerasable() )
			ctx_clazz.accessTypeInfoField(this,type); // Create static field for this type typeinfo
		// Don't try to find constructor of argument type
		if( !type.isArgument() ) {
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();
			MethodType mt = (MethodType)Type.getRealType(type,new MethodType(ta,type));
			Method@ m;
			// First try overloaded 'new', than real 'new'
			if( (ctx_method==null || !ctx_method.name.equals(nameNewOp)) ) {
				ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports);
				if (PassInfo.resolveBestMethodR(type,m,info,nameNewOp,mt)) {
					CallExpr n = new CallExpr(pos,new TypeRef(type),(Method)m,args.delToArray());
					replaceWithNode(n);
					m.makeArgs(n.args,type);
					for(int i=0; i < n.args.length; i++)
						n.args[i].resolve(null);
					n.setResolved(true);
					return;
				}
			}
			mt = (MethodType)Type.getRealType(type,new MethodType(ta,Type.tpVoid));
			ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports|ResInfo.noStatic);
			if( PassInfo.resolveBestMethodR(type,m,info,nameInit,mt) ) {
				func = m;
				m.makeArgs(args,type);
				for(int i=0; i < args.length; i++)
					args[i].resolve(null);
			}
			else {
				throw new CompilerException(this,"Can't find apropriative initializer for "+
					Method.toString(nameInit,args,Type.tpVoid)+" for "+type);
			}
		} else {
			if( !type.isRtArgumented())
				throw new CompilerException(this,"Can't create an instance of erasable argument type "+type);
		}
		setResolved(true);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

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

public final class NewArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef NImpl = NewArrayExprImpl;
	@virtual typedef VView = NewArrayExprView;
	@virtual typedef JView = JNewArrayExprView;

	@node
	public static final class NewArrayExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewArrayExpr;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		@att public int					dim;
		@ref public Type				arrtype;

		public NewArrayExprImpl() {}
		public NewArrayExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view NewArrayExprView of NewArrayExprImpl extends ENodeView {
		public				TypeRef			type;
		public access:ro	NArr<ENode>		args;
		public				int				dim;
		public				Type			arrtype;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public NewArrayExpr() {
		super(new NewArrayExprImpl());
	}

	public NewArrayExpr(int pos, TypeRef type, ENode[] args, int dim) {
		super(new NewArrayExprImpl(pos));
		this.type = type;
		foreach (ENode e; args) this.args.append(e);
		this.dim = dim;
		arrtype = new ArrayType(type.getType());
		for(int i=1; i < dim; i++) arrtype = new ArrayType(arrtype);
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

	public Type getType() { return arrtype; }

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		Type type = this.type.getType();
		for(int i=0; i < args.length; i++)
			if( args[i] != null )
				args[i].resolve(Type.tpInt);
		if( type.isArgument() ) {
			if( !type.isRtArgumented())
				throw new CompilerException(this,"Can't create an array of erasable argument type "+type);
			if( ctx_method==null || ctx_method.isStatic() )
				throw new CompilerException(this,"Access to argument "+type+" from static method");
			int i;
			for(i=0; i < ctx_clazz.args.length; i++)
				if (type ≈ ctx_clazz.args[i].getAType()) break;
			if( i >= ctx_clazz.args.length )
				throw new CompilerException(this,"Can't create an array of argument type "+type);
			ENode tie = new IFldExpr(pos,new ThisExpr(0),ctx_clazz.resolveField(nameTypeInfo));
			if( dim == 1 ) {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,tie,
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),Type.tpObject,Type.tpInt,Type.tpInt),
						new ENode[]{new ConstIntExpr(i),(ENode)~args[0]}
					),true));
				return;
			} else {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,tie,
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),Type.tpObject,Type.tpInt,new ArrayType(Type.tpInt)),
						new ENode[]{
							new ConstIntExpr(i),
							new NewInitializedArrayExpr(pos,new TypeRef(Type.tpInt),1,args.delToArray())
						}
					),true));
				return;
			}
		}
		setResolved(true);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

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

public final class NewInitializedArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef NImpl = NewInitializedArrayExprImpl;
	@virtual typedef VView = NewInitializedArrayExprView;
	@virtual typedef JView = JNewInitializedArrayExprView;

	@node
	public static final class NewInitializedArrayExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewInitializedArrayExpr;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		@att public int[]				dims;
		@ref public Type				arrtype;

		public NewInitializedArrayExprImpl() {}
		public NewInitializedArrayExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view NewInitializedArrayExprView of NewInitializedArrayExprImpl extends ENodeView {
		public				TypeRef			type;
		public access:ro	NArr<ENode>		args;
		public				int[]			dims;
		public				ArrayType		arrtype;
		
		@getter public final int	get$dim()	{ return this.$view.dims.length; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public NewInitializedArrayExpr() {
		super(new NewInitializedArrayExprImpl());
	}

	public NewInitializedArrayExpr(int pos, TypeRef type, int dim, ENode[] args) {
		super(new NewInitializedArrayExprImpl(pos));
		this.type = type;
		dims = new int[dim];
		dims[0] = args.length;
		foreach (ENode e; args) this.args.append(e);
		arrtype = new ArrayType(type.getType());
		for(int i=1; i < dim; i++) arrtype = new ArrayType(arrtype);
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
		if( isResolved() ) return;
		Type type = this.type.getType();
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
	}

	public int		getPriority() { return Constants.opAccessPriority; }

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

public final class NewClosure extends ENode {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = NewClosureImpl;
	@virtual typedef VView = NewClosureView;
	@virtual typedef JView = JNewClosureView;

	@node
	public static final class NewClosureImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewClosure;
		@att public TypeClosureRef		type;
		@att public Struct				clazz;
		@ref public Method				func;

		public NewClosureImpl() {}
		public NewClosureImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view NewClosureView of NewClosureImpl extends ENodeView {
		public TypeClosureRef	type;
		public Struct			clazz;
		public Method			func;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public NewClosure() {
		super(new NewClosureImpl());
	}

	public NewClosure(int pos, TypeClosureRef type, Struct clazz) {
		super(new NewClosureImpl(pos));
		this.type = type;
		this.clazz = clazz;
	}

//	public NewClosure(int pos, Method func) {
//		super(new NewClosureImpl(pos));
//		this.func = func;
//		this.type = new TypeClosureRef(new ClosureType(Type.tpClosureClazz,func.type.args,func.type.ret));
//	}

	public String toString() {
		return "fun "+type;
	}

	public Type getType() { return type.getType(); }

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		if( Kiev.passLessThen(TopLevelPass.passResolveImports) ) return;
		ClosureType type = (ClosureType)this.type.getType();
		if( Env.getStruct(Type.tpClosureClazz.name) == null )
			throw new RuntimeException("Core class "+Type.tpClosureClazz.name+" not found");
		Struct clazz = this.clazz;
		Kiev.runBackends(fun (BackendProcessor bep)->void { bep.preGenerate(clazz); });
		Kiev.runBackends(fun (BackendProcessor bep)->void { bep.resolve(clazz); });
		func = clazz.resolveMethod(nameInit,Type.tpVoid,Type.tpInt);
		setResolved(true);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Dumper toJava(Dumper dmp) {
		ClosureType type = (ClosureType)this.type.getType();
		Struct cl = clazz;
		dmp.append("new ").append(cl.super_type.clazz.name).append('(')
			.append(String.valueOf(type.args.length)).append(')');
		dmp.space().append('{').newLine(1);
		foreach (DNode n; cl.members)
			n.toJavaDecl(dmp).newLine();
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

