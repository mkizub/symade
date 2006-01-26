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
			TypeRef sup_tr = (TypeRef)this.type.copy();
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
					init.body = new BlockStat(pos);
					init.setPublic();
					clazz.addMethod(init);
				}
			}
	
			// Process inner classes and cases
			Kiev.runProcessorsOn(clazz);
			return true;
		}
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
		if( isResolved() ) return;
		CompaundType type;
		if (this.clazz != null) {
			type = this.clazz.ctype;
		} else {
			Type t = this.type.getType();
			if (t.isWrapper())
				type = ((WrapperType)t).getUnwrappedType();
			else if (t instanceof ArgType)
				type = (CompaundType)((ArgType)t).getSuperType();
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
			ctx_clazz.accessTypeInfoField(this,type,false); // Create static field for this type typeinfo
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

		public NewArrayExprImpl() {}
		public NewArrayExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view NewArrayExprView of NewArrayExprImpl extends ENodeView {
		public				TypeRef			type;
		public access:ro	NArr<ENode>		args;
		public				int				dim;
		public				ArrayType		arrtype;

		public Type get$arrtype() {
			ArrayType art = this.$view.arrtype;
			if (art != null)
				return art;
			art = new ArrayType(type.getType());
			for(int i=1; i < dim; i++) art = new ArrayType(art);
			this.$view.arrtype = art;
			return art;
		}

		public int		getPriority() { return Constants.opAccessPriority; }
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
		if( isResolved() ) return;
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
			ENode ti = ctx_clazz.accessTypeInfoField(this,type,false);
			if( dim == 1 ) {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,ti,
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),Type.tpObject,Type.tpInt),
						new ENode[]{(ENode)~args[0]}
					),true));
				return;
			} else {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,ti,
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),Type.tpObject,new ArrayType(Type.tpInt)),
						new ENode[]{
							new NewInitializedArrayExpr(pos,new TypeRef(Type.tpInt),1,args.delToArray())
						}
					),true));
				return;
			}
		}
		setResolved(true);
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

		public Type get$arrtype() {
			ArrayType art = this.$view.arrtype;
			if (art != null)
				return art;
			art = new ArrayType(type.getType());
			for(int i=1; i < dim; i++) art = new ArrayType(art);
			this.$view.arrtype = art;
			return art;
		}

		public int		getPriority() { return Constants.opAccessPriority; }
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
		if( isResolved() ) return;
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
	@dflow(in="this:in")	BlockStat		body;
	}


	@virtual typedef NImpl = NewClosureImpl;
	@virtual typedef VView = NewClosureView;
	@virtual typedef JView = JNewClosure;

	@nodeimpl
	public static final class NewClosureImpl extends ENodeImpl {
		@virtual typedef ImplOf = NewClosure;
		@att public TypeRef				type_ret;
		@att public NArr<FormPar>		params;
		@att public BlockStat			body;
		@att public Struct				clazz;
		@ref public CallType			ctype;

		public NewClosureImpl() {}
		public NewClosureImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view NewClosureView of NewClosureImpl extends ENodeView {
		public TypeRef			type_ret;
		public NArr<FormPar>	params;
		public BlockStat		body;
		public Struct			clazz;
		public CallType			ctype;

		public int		getPriority() { return Constants.opAccessPriority; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public NewClosure() {
		super(new NewClosureImpl());
	}

	public NewClosure(int pos) {
		super(new NewClosureImpl(pos));
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
	
	public boolean preGenerate() {
		if (clazz != null)
			return true;
		ClazzName clname = ClazzName.fromBytecodeName(
			new KStringBuffer(ctx_clazz.name.bytecode_name.len+8)
				.append_fast(ctx_clazz.name.bytecode_name)
				.append_fast((byte)'$')
				.append(ctx_clazz.countAnonymouseInnerStructs())
				.toKString(),
			false
		);
		clazz = Env.newStruct(clname,ctx_clazz,0,true);
		clazz.setResolved(true);
		clazz.setLocal(true);
		clazz.setAnonymouse(true);
		if( ctx_method==null || ctx_method.isStatic() ) clazz.setStatic(true);
		if( Env.getStruct(Type.tpClosureClazz.name) == null )
			throw new RuntimeException("Core class "+Type.tpClosureClazz.name+" not found");
		clazz.super_type = Type.tpClosureClazz.ctype;
		Kiev.runProcessorsOn(clazz);
		this.getType();

		// scan the body, and replace ThisExpr with OuterThisExpr
		Struct clz = this.ctx_clazz;
		body.walkTree(new TreeWalker() {
			public void post_exec(ASTNode n) {
				if (n instanceof ThisExpr) n.replaceWithNode(new OuterThisAccessExpr(n.pos, clz));
			}
		});

		BlockStat body = (BlockStat)~this.body;
		Type ret = ctype.ret();
		if( ret ≢ Type.tpRule ) {
			KString call_name;
			if( ret.isReference() ) {
				ret = Type.tpObject;
				call_name = KString.from("call_Object");
			} else {
				call_name = KString.from("call_"+ret);
			}
			Method md = new Method(call_name, ret, ACC_PUBLIC);
			md.pos = pos;
			md.body = body;
			clazz.members.add(md);
		} else {
			KString call_name = KString.from("call_rule");
			RuleMethod md = new RuleMethod(call_name,ACC_PUBLIC);
			md.pos = pos;
			md.body = body;
			clazz.members.add(md);
		}

		FormPar[] params = this.params.delToArray();
		for(int i=0; i < params.length; i++) {
			FormPar v = params[i];
			ENode val = new ContainerAccessExpr(pos,
				new IFldExpr(pos,new ThisExpr(pos),Type.tpClosureClazz.resolveField(nameClosureArgs)),
				new ConstIntExpr(i));
			if( v.type.isReference() )
				val = new CastExpr(v.pos,v.type,val,true);
			else
				val = new CastExpr(v.pos,((CoreType)v.type).getRefTypeForPrimitive(),val,true);
			v.init = val;
			body.insertSymbol(v,i);
			if( !v.type.isReference() )
				 CastExpr.autoCastToPrimitive(val);
		}

		return true;
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		//if( isResolved() ) return;
		//CallType type = (CallType)this.type.getType();
		//if( Env.getStruct(Type.tpClosureClazz.name) == null )
		//	throw new RuntimeException("Core class "+Type.tpClosureClazz.name+" not found");
		//Struct clazz = this.clazz;
		//Kiev.runBackends(fun (BackendProcessor bep)->void { bep.preGenerate(clazz); });
		//Kiev.runBackends(fun (BackendProcessor bep)->void { bep.resolve(clazz); });
		//func = clazz.resolveMethod(nameInit,Type.tpVoid,Type.tpInt);
		clazz.resolveDecl();
		setResolved(true);
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

