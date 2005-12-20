package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.Operator.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JNewExprView;
import kiev.be.java.JNewArrayExprView;
import kiev.be.java.JNewInitializedArrayExprView;
import kiev.be.java.JNewClosureView;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class NewExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@node
	public static final class NewExprImpl extends ENodeImpl {
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
	
	@att public abstract virtual			TypeRef				type;
	@att public abstract virtual access:ro	NArr<ENode>			args;
	@att public abstract virtual			ENode				outer;
	@att public abstract virtual			ENode				temp_expr;
	@att public abstract virtual			Struct				clazz;
	@ref public abstract virtual			Method				func;
	
	@getter public TypeRef			get$type()				{ return this.getNewExprView().type; }
	@getter public NArr<ENode>		get$args()				{ return this.getNewExprView().args; }
	@getter public ENode			get$outer()				{ return this.getNewExprView().outer; }
	@getter public ENode			get$temp_expr()			{ return this.getNewExprView().temp_expr; }
	@getter public Struct			get$clazz()				{ return this.getNewExprView().clazz; }
	@getter public Method			get$func()				{ return this.getNewExprView().func; }
	
	@setter public void		set$type(TypeRef val)			{ this.getNewExprView().type = val; }
	@setter public void		set$outer(ENode val)			{ this.getNewExprView().outer = val; }
	@setter public void		set$temp_expr(ENode val)		{ this.getNewExprView().temp_expr = val; }
	@setter public void		set$clazz(Struct val)			{ this.getNewExprView().clazz = val; }
	@setter public void		set$func(Method val)			{ this.getNewExprView().func = val; }

	public NodeView				getNodeView()		{ return new NewExprView((NewExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()		{ return new NewExprView((NewExprImpl)this.$v_impl); }
	public NewExprView			getNewExprView()	{ return new NewExprView((NewExprImpl)this.$v_impl); }
	public JNodeView			getJNodeView()		{ return new JNewExprView((NewExprImpl)this.$v_impl); }
	public JENodeView			getJENodeView()		{ return new JNewExprView((NewExprImpl)this.$v_impl); }
	public JNewExprView			getJNewExprView()	{ return new JNewExprView((NewExprImpl)this.$v_impl); }


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
		return type.getType();
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		Type type = this.type.getType();
		if( type.isAnonymouseClazz() ) {
			type.getStruct().resolveDecl();
		}
		if( !type.isArgument() && (type.isAbstract() || !type.isClazz()) ) {
			throw new CompilerException(this,"Abstract class "+type+" instantiation");
		}
		if( outer != null )
			outer.resolve(null);
		else if( (!type.isStaticClazz() && type.isLocalClazz())
			  || (!type.isStaticClazz() && !type.getStruct().package_clazz.isPackage()) )
		{
			if( ctx_method==null || ctx_method.isStatic() )
				throw new CompilerException(this,"'new' for inner class requares outer instance specification");
			outer = new ThisExpr(pos);
			outer.resolve(null);
		}
		for(int i=0; i < args.length; i++)
			args[i].resolve(null);
		if( ctx_clazz.args.length > 0 )
			ctx_clazz.accessTypeInfoField(this,type); // Create static field for this type typeinfo
		// Don't try to find constructor of argument type
		if( !type.isArgument() ) {
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();
			MethodType mt = MethodType.newMethodType(null,ta,type);
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
			mt = MethodType.newMethodType(null,ta,Type.tpVoid);
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

@node
public final class NewArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@node
	public static final class NewArrayExprImpl extends ENodeImpl {
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
	
	@att public abstract virtual			TypeRef				type;
	@att public abstract virtual access:ro	NArr<ENode>			args;
	@att public abstract virtual			int					dim;
	@att public abstract virtual			Type				arrtype;
	
	@getter public TypeRef			get$type()				{ return this.getNewArrayExprView().type; }
	@getter public NArr<ENode>		get$args()				{ return this.getNewArrayExprView().args; }
	@getter public int				get$dim()				{ return this.getNewArrayExprView().dim; }
	@getter public Type				get$arrtype()			{ return this.getNewArrayExprView().arrtype; }
	
	@setter public void		set$type(TypeRef val)			{ this.getNewArrayExprView().type = val; }
	@setter public void		set$dim(int val)				{ this.getNewArrayExprView().dim = val; }
	@setter public void		set$arrtype(Type val)			{ this.getNewArrayExprView().arrtype = val; }

	public NodeView				getNodeView()			{ return new NewArrayExprView((NewArrayExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new NewArrayExprView((NewArrayExprImpl)this.$v_impl); }
	public NewArrayExprView		getNewArrayExprView()	{ return new NewArrayExprView((NewArrayExprImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JNewArrayExprView((NewArrayExprImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JNewArrayExprView((NewArrayExprImpl)this.$v_impl); }
	public JNewArrayExprView	getJNewArrayExprView()	{ return new JNewArrayExprView((NewArrayExprImpl)this.$v_impl); }

	public NewArrayExpr() {
		super(new NewArrayExprImpl());
	}

	public NewArrayExpr(int pos, TypeRef type, ENode[] args, int dim) {
		super(new NewArrayExprImpl(pos));
		this.type = type;
		foreach (ENode e; args) this.args.append(e);
		this.dim = dim;
		arrtype = Type.newArrayType(type.getType());
		for(int i=1; i < dim; i++) arrtype = Type.newArrayType(arrtype);
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
			if( ctx_method==null || ctx_method.isStatic() )
				throw new CompilerException(this,"Access to argument "+type+" from static method");
			int i;
			for(i=0; i < ctx_clazz.args.length; i++)
				if( type.string_equals(ctx_clazz.args[i].getType()) ) break;
			if( i >= ctx_clazz.args.length )
				throw new CompilerException(this,"Can't create an array of argument type "+type);
			ENode tie = new IFldExpr(pos,new ThisExpr(0),ctx_clazz.resolveField(nameTypeInfo));
			if( dim == 1 ) {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,tie,
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),KString.from("(II)Ljava/lang/Object;")),
						new ENode[]{new ConstIntExpr(i),(ENode)~args[0]}
					),true));
				return;
			} else {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,tie,
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),KString.from("(I[I)Ljava/lang/Object;")),
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

@node
public final class NewInitializedArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@node
	public static final class NewInitializedArrayExprImpl extends ENodeImpl {
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
		public				Type			arrtype;
		
		@getter public final int	get$dim()	{ return this.$view.dims.length; }
	}
	
	@att public abstract virtual			TypeRef				type;
	@att public abstract virtual access:ro	NArr<ENode>			args;
	     public abstract virtual access:ro	int					dim;
	@att public abstract virtual			int[]				dims;
	@ref public abstract virtual			Type				arrtype;
	
	@getter public TypeRef			get$type()				{ return this.getNewInitializedArrayExprView().type; }
	@getter public NArr<ENode>		get$args()				{ return this.getNewInitializedArrayExprView().args; }
	@getter public int				get$dim()				{ return this.getNewInitializedArrayExprView().dim; }
	@getter public int[]			get$dims()				{ return this.getNewInitializedArrayExprView().dims; }
	@getter public Type				get$arrtype()			{ return this.getNewInitializedArrayExprView().arrtype; }
	
	@setter public void		set$type(TypeRef val)			{ this.getNewInitializedArrayExprView().type = val; }
	@setter public void		set$dims(int[] val)				{ this.getNewInitializedArrayExprView().dims = val; }
	@setter public void		set$arrtype(Type val)			{ this.getNewInitializedArrayExprView().arrtype = val; }

	public NodeView							getNodeView()						{ return new NewInitializedArrayExprView((NewInitializedArrayExprImpl)this.$v_impl); }
	public ENodeView						getENodeView()						{ return new NewInitializedArrayExprView((NewInitializedArrayExprImpl)this.$v_impl); }
	public NewInitializedArrayExprView		getNewInitializedArrayExprView()	{ return new NewInitializedArrayExprView((NewInitializedArrayExprImpl)this.$v_impl); }
	public JNodeView						getJNodeView()						{ return new JNewInitializedArrayExprView((NewInitializedArrayExprImpl)this.$v_impl); }
	public JENodeView						getJENodeView()						{ return new JNewInitializedArrayExprView((NewInitializedArrayExprImpl)this.$v_impl); }
	public JNewInitializedArrayExprView	getJNewInitializedArrayExprView()	{ return new JNewInitializedArrayExprView((NewInitializedArrayExprImpl)this.$v_impl); }

	public NewInitializedArrayExpr() {
		super(new NewInitializedArrayExprImpl());
	}

	public NewInitializedArrayExpr(int pos, TypeRef type, int dim, ENode[] args) {
		super(new NewInitializedArrayExprImpl(pos));
		this.type = type;
		dims = new int[dim];
		dims[0] = args.length;
		foreach (ENode e; args) this.args.append(e);
		arrtype = Type.newArrayType(type.getType());
		for(int i=1; i < dim; i++) arrtype = Type.newArrayType(arrtype);
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
			args[i].resolve(arrtype.bindings[0]);
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

@node
public final class NewClosure extends ENode {
	
	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class NewClosureImpl extends ENodeImpl {
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
	
	@att public abstract virtual			TypeClosureRef		type;
	@att public abstract virtual			Struct				clazz;
	@ref public abstract virtual			Method				func;
	
	@getter public TypeClosureRef	get$type()				{ return this.getNewClosureView().type; }
	@getter public Struct			get$clazz()				{ return this.getNewClosureView().clazz; }
	@getter public Method			get$func()				{ return this.getNewClosureView().func; }
	
	@setter public void		set$type(TypeClosureRef val)	{ this.getNewClosureView().type = val; }
	@setter public void		set$clazz(Struct val)			{ this.getNewClosureView().clazz = val; }
	@setter public void		set$func(Method val)			{ this.getNewClosureView().func = val; }

	public NodeView				getNodeView()			{ return new NewClosureView((NewClosureImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new NewClosureView((NewClosureImpl)this.$v_impl); }
	public NewClosureView		getNewClosureView()		{ return new NewClosureView((NewClosureImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JNewClosureView((NewClosureImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JNewClosureView((NewClosureImpl)this.$v_impl); }
	public JNewClosureView		getJNewClosureView()	{ return new JNewClosureView((NewClosureImpl)this.$v_impl); }


	public NewClosure() {
		super(new NewClosureImpl());
	}

	public NewClosure(int pos, TypeClosureRef type) {
		super(new NewClosureImpl(pos));
		this.type = type;
	}

	public NewClosure(int pos, Method func) {
		super(new NewClosureImpl(pos));
		this.func = func;
		this.type = new TypeClosureRef(ClosureType.newClosureType(Type.tpClosureClazz,func.type.args,func.type.ret));
	}

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
		type.getStruct().autoProxyMethods();
		type.getStruct().resolveDecl();
		Method@ m;
		MethodType mt = MethodType.newMethodType(null,new Type[]{Type.tpInt},Type.tpVoid);
		ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports|ResInfo.noStatic);
		if !(PassInfo.resolveBestMethodR(type,m,info,nameInit,mt))
			throw new CompilerException(this,"Can't find apropriative initializer for "+
					Method.toString(nameInit,mt)+" for "+type);
		func = m;
		setResolved(true);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Dumper toJava(Dumper dmp) {
		ClosureType type = (ClosureType)this.type.getType();
		Struct cl = type.clazz;
		dmp.append("new ").append(cl.super_type.clazz.name).append('(')
			.append(String.valueOf(type.args.length)).append(')');
		dmp.space().append('{').newLine(1);
		foreach (DNode n; cl.members)
			n.toJavaDecl(dmp).newLine();
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

