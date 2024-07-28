/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.ir.java15;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ViewOf(vcast=true)
public static final view RNewExpr of NewExpr extends RENode {
	public		Method				func;
	public		TypeRef				ntype;
	public		ENode				outer;
	public:ro	ENode[]				args;
	public		ENode				tpinfo;
	public		Struct				clazz;

	public boolean preGenerate(Env env) {
		if (this.clazz == null)
			return true;
		// Create default initializer, if not exists
		foreach (Constructor ctor; this.clazz.members; !ctor.isStatic())
			return true;
		Constructor init = new Constructor(ACC_PUBLIC);
		for(int i=0; i < args.length; i++) {
			resolveENode(args[i],null,env);
			init.params.append(new LVar(pos,"arg$"+i,args[i].getType(env),Var.PARAM_LVAR_PROXY,ACC_FINAL|ACC_SYNTHETIC));
		}
		init.pos = pos;
		init.body = new Block(pos);
		init.setPublic();
		clazz.addMethod(init);
		this.func = init;
		return true;
	}

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) {
			assert (func != null);
			if (isAutoReturnable())
				RReturnStat.autoReturn(reqType, this, env);
			return;
		}
		Type ntype;
		if (this.clazz != null) {
			resolveDNode(this.clazz,env);
			ntype = this.clazz.getType(env);
		} else {
			ntype = this.ntype.getType(env);
		}
		Struct s = ntype.getStruct();
		if (s == null || (s.isInterface() && !s.isMixin() && clazz == null))
			Kiev.reportWarning(this,"Instantiation of non-concrete type "+this.ntype+" ???");
		if (s.isInterface() && s.isMixin() && clazz == null) {
			s = s.iface_impl;
			this.ntype = new TypeInnerNameRef(~this.ntype, s.sname);
			ntype = this.ntype.getType(env);
		}
		if (s.isEnum())
			throw new CompilerException(this,"Forbidden enum value instantiation");
		if (outer == null && s.isStructInner() && !s.isStatic() && s.ometa_tdef != null) {
			if( ctx_method==null || ctx_method.isStatic() )
				throw new CompilerException(this,"'new' for inner class requares outer instance specification");
			outer = new ThisExpr(pos);
		}
		if (outer != null) {
			resolveENode(outer,null,env);
			TVarBld vs = new TVarBld(s.ometa_tdef.getAType(env), outer.getType(env));
			vs.append(ntype);
			ntype = ntype.make(vs);
		}
		if (s.isTypeUnerasable() && ((RStruct)s).getTypeInfoArgs(env).length > 0) {
			tpinfo = new TypeInfoExpr(ntype);
			resolveENode(tpinfo,null,env);
		}
		else if (tpinfo != null) {
			tpinfo = null;
		}
		for(int i=0; i < args.length; i++)
			resolveENode(args[i],null,env);
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType(env);
		if (this.clazz == null || this.func == null) {
			CallType mt = new CallType(null,null,ta,ntype,false); //(CallType)Type.getRealType(ntype,new CallType(null,null,ta,ntype,false));
			// First try overloaded 'new', than real 'new'
			if( this.clazz == null && (ctx_method==null || !ctx_method.hasName(nameNewOp)) ) {
				ResInfo<Method> info = new ResInfo<Method>(env,this,nameNewOp,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
				if (PassInfo.resolveBestMethodR(ntype,info,mt)) {
					CallExpr n = new CallExpr(pos,new TypeRef(ntype),info.resolvedSymbol(),((NewExpr)this).args.delToArray());
					replaceWithNodeResolve(env, n);
					return;
				}
			}
			mt = new CallType(ntype,null,ta,env.tenv.tpVoid,false); //(CallType)Type.getRealType(ntype,new CallType(ntype,null,ta,env.tenv.tpVoid,false));
			ResInfo<Method> info = new ResInfo<Method>(env,this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noStatic);
			if( PassInfo.resolveBestMethodR(ntype,info,mt) ) {
				this.symbol = info.resolvedSymbol();
				info.resolvedDNode().makeArgs(args,ntype);
				for(int i=0; i < args.length; i++)
					resolveENode(args[i],mt.arg(i),env);
			}
			else {
				throw new CompilerException(this,"Can't find apropriative initializer for "+
					Method.toString("<constructor>",args,env.tenv.tpVoid)+" for "+ntype);
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RNewEnumExpr of NewEnumExpr extends RENode {
	public:ro	Method				func;
	public:ro	ENode[]				args;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) {
			assert (func != null);
			if (isAutoReturnable())
				RReturnStat.autoReturn(reqType, this, env);
			return;
		}
		Type ntype = this.getType(env);
		for(int i=0; i < args.length; i++)
			resolveENode(args[i],null,env);
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType(env);
		CallType mt = (CallType)Type.getRealType(ntype,new CallType(null,null,ta,ntype,false));
		mt = (CallType)Type.getRealType(ntype,new CallType(ntype,null,ta,env.tenv.tpVoid,false));
		ResInfo<Constructor> info = new ResInfo<Constructor>(env,this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noStatic);
		if( PassInfo.resolveBestMethodR(ntype,info,mt) ) {
			this.symbol = info.resolvedSymbol();
			info.resolvedDNode().makeArgs(args,ntype);
			for(int i=0; i < args.length; i++)
				resolveENode(args[i],mt.arg(i),env);
		}
		else {
			throw new CompilerException(this,"Can't find apropriative initializer for "+
				Method.toString("<constructor>",args,env.tenv.tpVoid)+" for "+ntype);
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RNewArrayExpr of NewArrayExpr extends RENode {
	public		TypeRef				ntype;
	public:ro	ENode[]				args;
	public		ArrayType			arrtype;

	@getter public final Type	get$arrtype();

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) {
			if (isAutoReturnable())
				RReturnStat.autoReturn(reqType, this, env);
			return;
		}
		Type ntype = this.ntype.getType(env);
		ArrayType art = this.arrtype;
		for(int i=0; i < args.length; i++)
			if( args[i] != null )
				resolveENode(args[i],env.tenv.tpInt,env);
		if( ntype instanceof ArgType ) {
			if( !ntype.isUnerasable())
				throw new CompilerException(this,"Can't create an array of erasable argument type "+ntype);
			ENode ti = new TypeInfoExpr(ntype);
			if( args.length == 1 ) {
				this.replaceWithNodeResolve(env, reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,ti,
						env.tenv.tpTypeInfo.tdecl.resolveMethod(env,"newArray",env.tenv.tpObject,env.tenv.tpInt),
						new ENode[]{~args[0]}
					)));
				return;
			} else {
				this.replaceWithNodeResolve(env, reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,ti,
						env.tenv.tpTypeInfo.tdecl.resolveMethod(env,"newArray",env.tenv.tpObject,new ArrayType(env.tenv.tpInt)),
						new ENode[]{
							new NewInitializedArrayExpr(pos,new TypeExpr(env.tenv.tpInt,Operator.PostTypeArray,new ArrayType(env.tenv.tpInt)),((NewArrayExpr)this).args.delToArray())
						}
					)));
				return;
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RNewInitializedArrayExpr of NewInitializedArrayExpr extends RENode {
	public		TypeRef				ntype;
	public:ro	ENode[]				args;
	
	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) {
			if (isAutoReturnable())
				RReturnStat.autoReturn(reqType, this, env);
			return;
		}
		Type ntype;
		if( this.ntype == null ) {
			if( !reqType.isArray() )
				throw new CompilerException(this,"Type "+reqType+" is not an array type");
			ntype = reqType;
			Type art = reqType;
			int dim = 0;
			while (art instanceof ArrayType) { dim++; art = art.arg; }
			TypeRef tp = new TypeRef(art);
			for (int i=0; i < dim; i++)
				tp = new TypeExpr(tp, Operator.PostTypeArray, new ArrayType(tp.getType(env)));
			this.ntype = (TypeExpr)tp;
		} else {
			ntype = this.getType(env);
		}
		if( !ntype.isArray() )
			throw new CompilerException(this,"Type "+ntype+" is not an array type");
		for(int i=0; i < args.length; i++)
			resolveENode(args[i],((ArrayType)ntype).arg,env);
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public final view RNewClosure of NewClosure extends RENode {
	public		TypeRef			type_ret;
	public:ro	Var[]			params;
	public		Block			body;
	public		Struct			clazz;

	public boolean preGenerate(Env env) {
		if (clazz != null)
			return true;
		clazz = env.newStruct(null,null,0,new JavaAnonymouseClass(),null);
		if (ctx_method==null || ctx_method.isStatic())
			clazz.setStatic(true);
		if (env.loadTypeDecl(env.tenv.tpClosure.getStruct()).isTypeDeclNotLoaded())
			throw new RuntimeException("Core class "+env.tenv.tpClosure.getStruct()+" not found");
		clazz.super_types.insert(0, new TypeRef(env.tenv.tpClosure));
		Kiev.runProcessorsOn(clazz);
		((NewClosure)this).getType(env);

		// scan the body, and replace ThisExpr with OuterThisExpr
		Struct clz = (Struct)this.ctx_tdecl;
		body.walkTree(null,null,new ITreeWalker() {
			public void post_exec(INode n, INode parent, AttrSlot slot) {
				if (n instanceof ThisExpr)
					n.replaceWithNode(new OuterThisAccessExpr(n.pos, new TypeRef(clz.getType(env))), parent, slot);
			}
		});

		Block body = ~this.body;
		Type ret = type_ret.getType(env);
		if( ret ≢ env.tenv.tpRule ) {
			String call_name;
			if( ret.isReference() ) {
				ret = env.tenv.tpObject;
				call_name = "call_Object";
			} else {
				call_name = ("call_"+ret).intern();
			}
			Method md = new MethodImpl(call_name, ret, ACC_PUBLIC);
			md.pos = pos;
			md.body = body;
			clazz.members.add(md);
		} else {
			String call_name = "call_rule";
			RuleMethod md = new RuleMethod(call_name,ACC_PUBLIC);
			md.pos = pos;
			md.body = body;
			clazz.members.add(md);
		}

		Var[] params = ((NewClosure)this).params.delToArray();
		for(int i=0; i < params.length; i++) {
			Var v = params[i];
			((NewClosure)this).params += new Copier().copyFull(v);
			ENode val = new ContainerAccessExpr(pos,
				new IFldExpr(pos,new ThisExpr(pos),env.tenv.tpClosure.getStruct().resolveField(env,nameClosureArgs)),
				new ConstIntExpr(i));
			if( v.getType(env).isReference() )
				val = new CastExpr(v.pos,v.getType(env),val);
			else
				val = new CastExpr(v.pos,((CoreType)v.getType(env)).getRefTypeForPrimitive(),val);
			v.init = val;
			body.insertSymbol(v,i);
			if( !v.getType(env).isReference() )
				 CastExpr.autoCastToPrimitive(env, val, (CoreType)v.getType(env), v, Var.nodeattr$init);
		}

		return true;
	}
	
	public void resolveENode(Type reqType, Env env) {
		resolveDNode(clazz,env);
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

