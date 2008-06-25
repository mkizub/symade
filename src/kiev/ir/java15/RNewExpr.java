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

@ViewOf(vcast=true, iface=true)
public static final view RNewExpr of NewExpr extends RENode {
	public:ro	Method				func;
	public		TypeRef				ntype;
	public		ENode				outer;
	public:ro	ENode[]				args;
	public		ENode				tpinfo;
	public		Struct				clazz;

	public void resolve(Type reqType) {
		if( isResolved() ) {
			assert (func != null);
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		Type ntype;
		if (this.clazz != null) {
			this.clazz.resolveDecl();
			ntype = this.clazz.xtype;
		} else {
			ntype = this.ntype.getType();
		}
		Struct s = ntype.getStruct();
		if (s == null || (s.isInterface() && !s.isMixin() && clazz == null))
			Kiev.reportWarning(this,"Instantiation of non-concrete type "+this.ntype+" ???");
		if (s.isInterface() && s.isMixin() && clazz == null) {
			s = s.iface_impl;
			this.ntype = new TypeInnerNameRef(~this.ntype, s.sname);
			ntype = this.ntype.getType();
		}
		if (s.isEnum())
			throw new CompilerException(this,"Forbidden enum value instantiation");
		if (outer == null && s.isStructInner() && !s.isStatic() && s.ometa_tdef != null) {
			if( ctx_method==null || ctx_method.isStatic() )
				throw new CompilerException(this,"'new' for inner class requares outer instance specification");
			outer = new ThisExpr(pos);
		}
		if (outer != null) {
			outer.resolve(null);
			ntype = ntype.rebind(new TVarBld(s.ometa_tdef.getAType(), outer.getType()));
		}
		if (s.isTypeUnerasable()) {
			tpinfo = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((NewExpr)this,ntype,false); // Create static field for this type typeinfo
			tpinfo.resolve(null);
		}
		else if (tpinfo != null) {
			tpinfo = null;
		}
		for(int i=0; i < args.length; i++)
			args[i].resolve(null);
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		CallType mt = new CallType(null,null,ta,ntype,false); //(CallType)Type.getRealType(ntype,new CallType(null,null,ta,ntype,false));
		Method@ m;
		// First try overloaded 'new', than real 'new'
		if( this.clazz == null && (ctx_method==null || !ctx_method.hasName(nameNewOp)) ) {
			ResInfo info = new ResInfo(this,nameNewOp,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
			if (PassInfo.resolveBestMethodR(ntype,m,info,mt)) {
				CallExpr n = new CallExpr(pos,new TypeRef(ntype),(Method)m,((NewExpr)this).args.delToArray());
				replaceWithNodeResolve(n);
				return;
			}
		}
		mt = new CallType(ntype,null,ta,Type.tpVoid,false); //(CallType)Type.getRealType(ntype,new CallType(ntype,null,ta,Type.tpVoid,false));
		ResInfo info = new ResInfo(this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noStatic);
		if( PassInfo.resolveBestMethodR(ntype,m,info,mt) ) {
			this.symbol = m;
			m.makeArgs(args,ntype);
			for(int i=0; i < args.length; i++)
				args[i].resolve(mt.arg(i));
		}
		else {
			throw new CompilerException(this,"Can't find apropriative initializer for "+
				Method.toString("<constructor>",args,Type.tpVoid)+" for "+ntype);
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RNewEnumExpr of NewEnumExpr extends RENode {
	public:ro	Method				func;
	public:ro	ENode[]				args;

	public void resolve(Type reqType) {
		if( isResolved() ) {
			assert (func != null);
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		Type ntype = this.getType();
		for(int i=0; i < args.length; i++)
			args[i].resolve(null);
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		CallType mt = (CallType)Type.getRealType(ntype,new CallType(null,null,ta,ntype,false));
		Constructor@ m;
		mt = (CallType)Type.getRealType(ntype,new CallType(ntype,null,ta,Type.tpVoid,false));
		ResInfo info = new ResInfo(this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noStatic);
		if( PassInfo.resolveBestMethodR(ntype,m,info,mt) ) {
			this.symbol = m;
			m.makeArgs(args,ntype);
			for(int i=0; i < args.length; i++)
				args[i].resolve(mt.arg(i));
		}
		else {
			throw new CompilerException(this,"Can't find apropriative initializer for "+
				Method.toString("<constructor>",args,Type.tpVoid)+" for "+ntype);
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RNewArrayExpr of NewArrayExpr extends RENode {
	public		TypeRef				ntype;
	public:ro	ENode[]				args;
	public		ArrayType			arrtype;

	@getter public final Type	get$arrtype();

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		Type ntype = this.ntype.getType();
		ArrayType art = this.arrtype;
		for(int i=0; i < args.length; i++)
			if( args[i] != null )
				args[i].resolve(Type.tpInt);
		if( ntype instanceof ArgType ) {
			if( !ntype.isUnerasable())
				throw new CompilerException(this,"Can't create an array of erasable argument type "+ntype);
			//if( ctx_method==null || ctx_method.isStatic() )
			//	throw new CompilerException(this,"Access to argument "+ntype+" from static method");
			ENode ti = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((NewArrayExpr)this,ntype,false);
			if( args.length == 1 ) {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,ti,
						Type.tpTypeInfo.tdecl.resolveMethod("newArray",Type.tpObject,Type.tpInt),
						new ENode[]{~args[0]}
					)));
				return;
			} else {
				this.replaceWithNodeResolve(reqType, new CastExpr(pos,arrtype,
					new CallExpr(pos,ti,
						Type.tpTypeInfo.tdecl.resolveMethod("newArray",Type.tpObject,new ArrayType(Type.tpInt)),
						new ENode[]{
							new NewInitializedArrayExpr(pos,new TypeExpr(Type.tpInt,Operator.PostTypeArray),((NewArrayExpr)this).args.delToArray())
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

@ViewOf(vcast=true, iface=true)
public static final view RNewInitializedArrayExpr of NewInitializedArrayExpr extends RENode {
	public		TypeRef				ntype;
	public:ro	ENode[]				args;
	
	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
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
				tp = new TypeExpr(tp, Operator.PostTypeArray);
			this.ntype = (TypeExpr)tp;
		} else {
			ntype = this.getType();
		}
		if( !ntype.isArray() )
			throw new CompilerException(this,"Type "+ntype+" is not an array type");
		for(int i=0; i < args.length; i++)
			args[i].resolve(((ArrayType)ntype).arg);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public final view RNewClosure of NewClosure extends RENode {
	public		TypeRef			type_ret;
	public:ro	Var[]			params;
	public		Block			body;
	public		Struct			clazz;
	public		CallType		xtype;

	public boolean preGenerate() {
		if (clazz != null)                                                    
			return true;
		clazz = Env.getRoot().newStruct(null,false,null,0,new JavaAnonymouseClass(),true,null);
		clazz.setLocal(true);
		clazz.setAnonymouse(true);
		if (ctx_method==null || ctx_method.isStatic())
			clazz.setStatic(true);
		if (Env.getRoot().loadTypeDecl(Type.tpClosureClazz).isTypeDeclNotLoaded())
			throw new RuntimeException("Core class "+Type.tpClosureClazz+" not found");
		clazz.super_types.insert(0, new TypeRef(Type.tpClosureClazz.xtype));
		Kiev.runProcessorsOn(clazz);
		((NewClosure)this).getType();

		// scan the body, and replace ThisExpr with OuterThisExpr
		Struct clz = (Struct)this.ctx_tdecl;
		body.walkTree(new TreeWalker() {
			public void post_exec(ANode n) {
				if (n instanceof ThisExpr) n.replaceWithNode(new OuterThisAccessExpr(n.pos, new TypeRef(clz.xtype)));
			}
		});

		Block body = ~this.body;
		Type ret = xtype.ret();
		if( ret ≢ Type.tpRule ) {
			String call_name;
			if( ret.isReference() ) {
				ret = Type.tpObject;
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
			ENode val = new ContainerAccessExpr(pos,
				new IFldExpr(pos,new ThisExpr(pos),Type.tpClosureClazz.resolveField(nameClosureArgs)),
				new ConstIntExpr(i));
			if( v.getType().isReference() )
				val = new CastExpr(v.pos,v.getType(),val);
			else
				val = new CastExpr(v.pos,((CoreType)v.getType()).getRefTypeForPrimitive(),val);
			v.init = val;
			body.insertSymbol(v,i);
			if( !v.getType().isReference() )
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

