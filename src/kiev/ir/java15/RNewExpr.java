package kiev.ir.java15;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public static final view RNewExpr of NewExpr extends RENode {
	public		TypeRef				type;
	public:ro	NArr<ENode>			args;
	public		ENode				outer;
	public		Struct				clazz;
	public		Method				func;

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
			while (t != null && !(t instanceof CompaundType))
				t = t.getMetaSuper();
			type = (CompaundType)t;
		}
		if!(type instanceof CompaundType)
			Kiev.reportWarning(this,"Instantiation of non-concrete type "+this.type+" ???");
		if( type.getStruct().isAnonymouse() ) {
			type.getStruct().resolveDecl();
		}
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
			((RStruct)ctx_clazz).accessTypeInfoField((NewExpr)this,type,false); // Create static field for this type typeinfo
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
}

@nodeview
public static final view RNewArrayExpr of NewArrayExpr extends RENode {
	public		TypeRef				type;
	public:ro	NArr<ENode>			args;
	public		ArrayType			arrtype;

	@getter public final Type	get$arrtype();

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
		if( type instanceof ArgType ) {
			if( !type.isUnerasable())
				throw new CompilerException(this,"Can't create an array of erasable argument type "+type);
			if( ctx_method==null || ctx_method.isStatic() )
				throw new CompilerException(this,"Access to argument "+type+" from static method");
			ENode ti = ((RStruct)ctx_clazz).accessTypeInfoField((NewArrayExpr)this,type,false);
			if( args.size() == 1 ) {
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
}

@nodeview
public static final view RNewInitializedArrayExpr of NewInitializedArrayExpr extends RENode {
	public		TypeRef				type;
	public:ro	NArr<ENode>			args;
	public		int[]				dims;
	public		ArrayType			arrtype;
	
	@getter public final int	get$dim();

	@getter public final Type	get$arrtype();

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
}

@nodeview
public final view RNewClosure of NewClosure extends RENode {
	public TypeRef			type_ret;
	public NArr<FormPar>	params;
	public Block			body;
	public Struct			clazz;
	public CallType			ctype;

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
		((NewClosure)this).getType();

		// scan the body, and replace ThisExpr with OuterThisExpr
		Struct clz = this.ctx_clazz;
		body.walkTree(new TreeWalker() {
			public void post_exec(NodeData n) {
				if (n instanceof ThisExpr) n.replaceWithNode(new OuterThisAccessExpr(n.pos, clz));
			}
		});

		Block body = ~this.body;
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
				val = new CastExpr(v.pos,v.type,val);
			else
				val = new CastExpr(v.pos,((CoreType)v.type).getRefTypeForPrimitive(),val);
			v.init = val;
			body.insertSymbol(v,i);
			if( !v.type.isReference() )
				 CastExpr.autoCastToPrimitive(val);
		}

		return true;
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		clazz.resolveDecl();
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

