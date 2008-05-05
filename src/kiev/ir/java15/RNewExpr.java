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

public static final view RNewExpr of NewExpr extends RENode {
	public:ro	Method				func;
	public		TypeRef				type;
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
		Type type;
		if (this.clazz != null) {
			this.clazz.resolveDecl();
			type = this.clazz.xtype;
		} else {
			type = this.type.getType();
		}
		Struct s = type.getStruct();
		if (s == null || (s.isInterface() && !s.isMixin() && clazz == null))
			Kiev.reportWarning(this,"Instantiation of non-concrete type "+this.type+" ???");
		if (s.isInterface() && s.isMixin() && clazz == null) {
			s = s.iface_impl;
			this.type = new TypeNameRef(~this.type, s.sname);
			type = this.type.getType();
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
			type = type.bind(new TVarBld(s.ometa_tdef.getAType(), outer.getType()));
		}
		if (s.isTypeUnerasable()) {
			tpinfo = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((NewExpr)this,type,false); // Create static field for this type typeinfo
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
		CallType mt = (CallType)Type.getRealType(type,new CallType(null,null,ta,type,false));
		Method@ m;
		// First try overloaded 'new', than real 'new'
		if( this.clazz == null && (ctx_method==null || !ctx_method.hasName(nameNewOp,true)) ) {
			ResInfo info = new ResInfo(this,nameNewOp,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports);
			if (PassInfo.resolveBestMethodR(type,m,info,mt)) {
				CallExpr n = new CallExpr(pos,new TypeRef(type),(Method)m,((NewExpr)this).args.delToArray());
				replaceWithNodeResolve(n);
				return;
			}
		}
		mt = (CallType)Type.getRealType(type,new CallType(type,null,ta,Type.tpVoid,false));
		ResInfo info = new ResInfo(this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports|ResInfo.noStatic);
		if( PassInfo.resolveBestMethodR(type,m,info,mt) ) {
			this.symbol = m;
			m.makeArgs(args,type);
			for(int i=0; i < args.length; i++)
				args[i].resolve(mt.arg(i));
		}
		else {
			throw new CompilerException(this,"Can't find apropriative initializer for "+
				Method.toString("<constructor>",args,Type.tpVoid)+" for "+type);
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

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
		Type type = this.getType();
		for(int i=0; i < args.length; i++)
			args[i].resolve(null);
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		CallType mt = (CallType)Type.getRealType(type,new CallType(null,null,ta,type,false));
		Constructor@ m;
		mt = (CallType)Type.getRealType(type,new CallType(type,null,ta,Type.tpVoid,false));
		ResInfo info = new ResInfo(this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports|ResInfo.noStatic);
		if( PassInfo.resolveBestMethodR(type,m,info,mt) ) {
			this.symbol = m;
			m.makeArgs(args,type);
			for(int i=0; i < args.length; i++)
				args[i].resolve(mt.arg(i));
		}
		else {
			throw new CompilerException(this,"Can't find apropriative initializer for "+
				Method.toString("<constructor>",args,Type.tpVoid)+" for "+type);
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

public static final view RNewArrayExpr of NewArrayExpr extends RENode {
	public		TypeRef				type;
	public:ro	ENode[]				args;
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
			//if( ctx_method==null || ctx_method.isStatic() )
			//	throw new CompilerException(this,"Access to argument "+type+" from static method");
			ENode ti = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((NewArrayExpr)this,type,false);
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
							new NewInitializedArrayExpr(pos,new TypeExpr(Type.tpInt,Operator.PostTypeArray),1,((NewArrayExpr)this).args.delToArray())
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

public static final view RNewInitializedArrayExpr of NewInitializedArrayExpr extends RENode {
	public		TypeRef				type;
	public:ro	ENode[]				args;
	public		int[]				dims;
	
	@getter public final int	get$dim();

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
			Type art = reqType;
			int dim = 0;
			while (art instanceof ArrayType) { dim++; art = art.arg; }
			this.dims = new int[dim];
			this.dims[0] = args.length;
			{
				TypeRef tp = new TypeRef(art);
				for (int i=0; i < dim; i++)
					tp = new TypeExpr(tp, Operator.PostTypeArray);
				this.type = (TypeExpr)tp;
			}
		} else {
			type = this.getType();
		}
		if( !type.isArray() )
			throw new CompilerException(this,"Type "+type+" is not an array type");
		for(int i=0; i < args.length; i++)
			args[i].resolve(((ArrayType)type).arg);
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

public final view RNewClosure of NewClosure extends RENode {
	public		TypeRef			type_ret;
	public:ro	Var[]			params;
	public		Block			body;
	public		Struct			clazz;
	public		CallType		xtype;

	public boolean preGenerate() {
		if (clazz != null)                                                    
			return true;
		clazz = Env.getRoot().newStruct(null,false,(Struct)ctx_tdecl,0,new JavaAnonymouseClass(),true,null);
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
			LVar v = params[i];
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

