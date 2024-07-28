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
public final view RCoreExpr of CoreExpr extends RENode {

	public void resolveENode(Type reqType, Env env) {
		setResolved(true);
	}
}

@ViewOf(vcast=true)
public final view RShadow of Shadow extends RENode {
	public ASTNode		rnode;

	public void resolveENode(Type reqType, Env env) {
		if (rnode instanceof ENode)
			resolveENode(rnode,reqType,env);
		else
			resolveDNode(rnode,env);
		setResolved(true);
	}
}

@ViewOf(vcast=true)
public final view RTypeClassExpr of TypeClassExpr extends RENode {
	public TypeRef		ttype;

	public void resolveENode(Type reqType, Env env) {
		Type tp = ttype.getType(env);
		if (!tp.isReference()) {
			Type rt = ((CoreType)tp).getRefTypeForPrimitive();
			Field f = rt.tdecl.resolveField(env,"TYPE");
			replaceWithNodeResolve(env, reqType,new SFldExpr(pos,f));
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public final view RTypeInfoExpr of TypeInfoExpr extends RENode {
	public		TypeRef				ttype;
	public		ENode				cl_expr;
	public:ro	ENode[]				cl_args;

	public void resolveENode(Type reqType, Env env) {
		if (isResolved())
			return;
		Type ttype = this.ttype.getType(env);
		Struct clazz = ttype.getStruct();
		if (clazz == null)
			clazz = ttype.getErasedType().getStruct();
		if (clazz == null) {
			if (ttype instanceof CoreType || ttype.isArray()) {
				cl_expr = new TypeClassExpr(pos,new TypeRef(ttype));
				resolveENode(cl_expr,env.tenv.tpClass,env);
			} else {
				Kiev.reportError(this,"Cannot make typeinfo expression for type "+ttype);
			}
		} else {
			if (clazz.isTypeUnerasable()) {
				if (clazz.typeinfo_clazz == null)
					((RStruct)clazz).autoGenerateTypeinfoClazz(env);
			}
			cl_expr = new TypeClassExpr(pos,new TypeRef(clazz.getType(env)));
			resolveENode(cl_expr,env.tenv.tpClass,env);
			((TypeInfoExpr)this).cl_args.delAll();
			foreach (ArgType at; ((RStruct)clazz).getTypeInfoArgs(env)) {
				ENode tie = new TypeInfoExpr(ttype.resolve(at));
				((TypeInfoExpr)this).cl_args.add(tie);
			}
			foreach (ENode tie; cl_args)
				resolveENode(tie,null,env);
		}
		ANode parent = parent();
		if !(parent instanceof Field && parent.isStatic())
			resolveTypeInfo(env, (Struct)ctx_tdecl, (TypeInfoExpr)this, ttype, reqType);
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}

	private static void resolveTypeInfo(Env env, Struct clz, TypeInfoExpr tie, Type t, Type reqType) {
		while (t instanceof CTimeType)
			t = t.getEnclosedType();
		if (t.getStruct() != null && clz == t.getStruct().typeinfo_clazz && t.getStruct().getType(env) ≈ t)
			return ((RENode)tie).replaceWithNodeResolve(env, reqType, new ThisExpr(tie.pos));
		Method ctx_method = Env.ctxMethod(tie);
		if (t.isUnerasable()) {
			if (ctx_method != null && ctx_method.isTypeUnerasable() && t instanceof ArgType) {
				foreach (TypeDef td; ctx_method.targs; td.getAType(env) == t)
					return ((RENode)tie).replaceWithNodeResolve(env, reqType, new LVarExpr(tie.pos, ctx_method.getMethodTypeInfoParam(td.sname)));
			}
			if (clz.instanceOf(env.tenv.tpTypeInfo.tdecl) && ctx_method != null && ctx_method instanceof Constructor && !ctx_method.isStatic()) {
				if (t instanceof ArgType)
					return ((RENode)tie).replaceWithNodeResolve(env, reqType, new EToken(tie.pos,t.name.toString(),ETokenKind.EXPL_IDENTIFIER));
			}
			if (clz.isTypeUnerasable()) {
				ENode ti_access;
				if (ctx_method != null && ctx_method.isStatic()) {
					// check we have $typeinfo as first argument
					if (ctx_method.getClassTypeInfoParam() == null)
						goto make_typeinfo; //throw new CompilerException(from,"$typeinfo cannot be accessed from "+ctx_method);
					else
						ti_access = new LVarExpr(tie.pos,ctx_method.getClassTypeInfoParam());
				}
				else if (ctx_method == null)
					goto make_typeinfo;
				else {
					ti_access = new CastExpr(tie.pos, clz.typeinfo_clazz.getType(env),
						new CallExpr(clz.pos,
							new ThisExpr(tie.pos),
							clz.resolveMethod(env,nameGetTypeInfo,env.tenv.tpTypeInfo),
							ENode.emptyArray
						)
					);
				}
				// Check that we need our $typeinfo
				if (clz.getType(env) ≈ t)
					return ((RENode)tie).replaceWithNodeResolve(env, reqType, ti_access);
	
				if (t instanceof ArgType) {
					// Get corresponded type argument
					ArgType at = (ArgType)t;
					String fnm = (nameTypeInfo+'$'+at.name).intern();
					Field ti_arg = clz.typeinfo_clazz.resolveField(env,fnm);
					if (ti_arg == null)
						throw new RuntimeException("Field "+fnm+" not found in "+clz.typeinfo_clazz+" from method "+Env.ctxMethod(tie));
					ti_access = new IFldExpr(tie.pos,ti_access,ti_arg);
					return ((RENode)tie).replaceWithNodeResolve(env, reqType, ti_access);
				}
			}
		}

		make_typeinfo:;

		// Special case for interfaces, that cannot have private fields,
		// but need typeinfo in <clinit>
		if ((Env.ctxMethod(tie) == null || Env.ctxMethod(tie) instanceof Constructor && Env.ctxMethod(tie).isStatic()) && Env.ctxTDecl(tie).isInterface())
			return;
		
		// Lookup and create if need as $typeinfo$N
		foreach(Field f; clz.members; f.isStatic()) {
			if (f.init == null || !f.sname.startsWith(nameTypeInfo) || f.sname.equals(nameTypeInfo))
				continue;
			if (((TypeInfoExpr)f.init).ttype.getType(env) ≈ t)
				return ((RENode)tie).replaceWithNodeResolve(env, reqType, new SFldExpr(tie.pos,f));
		}
		
		foreach (ENode ti_arg; tie.cl_args; !(ti_arg instanceof SFldExpr))
			return; // oops, cannot make it a static field
		int i = 0;
		foreach(Field f; clz.members; f.isStatic()) {
			if (f.init == null || !f.sname.startsWith(nameTypeInfo) || f.sname.equals(nameTypeInfo))
				continue;
			i++;
		}
		Field f = new Field(nameTypeInfo+"$"+i,tie.getType(env),ACC_SYNTHETIC|ACC_STATIC|ACC_FINAL); // package-private for inner classes
		f.init = new Copier().copyFull(tie);
		clz.addField(f);
		resolveDNode(f,env);
		TypeInfoExpr finit = (TypeInfoExpr)new Copier().copyFull(f.init);
		finit.setResolved(true);
		// Add initialization in <clinit>
		Constructor class_init = clz.getClazzInitMethod();
		if( ctx_method != null && ctx_method instanceof Constructor && ctx_method.isStatic() ) {
			class_init.addstats.append(
				new ExprStat(f.init.pos,
					new AssignExpr(f.init.pos,new SFldExpr(f.pos,f),finit)
				)
			);
			Kiev.runProcessorsOn(class_init.addstats[class_init.addstats.length-1]);
		} else {
			class_init.addstats.append(
				new ExprStat(f.init.pos,
					new AssignExpr(f.init.pos,new SFldExpr(f.pos,f),finit)
				)
			);
			Kiev.runProcessorsOn(class_init.addstats[class_init.addstats.length-1]);
		}
		f.setAddedToInit(true);
		return ((RENode)tie).replaceWithNodeResolve(env, reqType, new SFldExpr(tie.pos,f));
	}

}

@ViewOf(vcast=true)
public final view RAssertEnabledExpr of AssertEnabledExpr extends RENode {

	public void resolveENode(Type reqType, Env env) {
		if (!Kiev.debugOutputA) {
			replaceWithNodeResolve(env, reqType, new ConstBoolExpr(false));
			return;
		}
		// get top-level class
		TypeDecl clazz = ctx_tdecl;
		while !(clazz.parent() instanceof KievPackage || Env.ctxTDecl(clazz).isInterface())
			clazz = Env.ctxTDecl(clazz);
		// find $assertionsEnabled
		foreach (Field f; clazz.members; f.sname == "$assertionsEnabled") {
			replaceWithNodeResolve(env, reqType, new SFldExpr(pos,f));
			return;
		}
		Field f = new Field("$assertionsEnabled", env.tenv.tpBoolean, ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC);
		clazz.members.add(f);
		f.init = new CallExpr(0,
			new TypeClassExpr(0,new TypeRef(clazz.getType(env))),
			env.tenv.tpClass.tdecl.resolveMethod(env,"desiredAssertionStatus", env.tenv.tpBoolean),
			ENode.emptyArray
			);
		resolveDNode(f,env);
		if !(f.isAddedToInit()) {
			// Add initialization in <clinit>
			Constructor class_init = ((Struct)clazz).getClazzInitMethod();
			class_init.addstats.insert(0,
				new ExprStat(f.init.pos,
					new AssignExpr(f.init.pos,new SFldExpr(f.pos,f),new Copier().copyFull(f.init))
				)
			);
			f.setAddedToInit(true);
			Kiev.runProcessorsOn(class_init.addstats[0]);
		}
		replaceWithNodeResolve(env, reqType, new SFldExpr(pos,f));
	}
}

@ViewOf(vcast=true)
public static final view RAssignExpr of AssignExpr extends RENode {
	public ENode			lval;
	public ENode			value;

	public void resolveENode(Type reqType, Env env) {
		if (lval.getLvalArity() < 0)
			Kiev.reportError(this,"Assigning to a non-lvalue "+lval);
		if( isResolved() ) {
			if (isAutoReturnable())
				RReturnStat.autoReturn(reqType, this, env);
			return;
		}

		Method m = resolveMethodAndNormalize(env);
		if (m == null)
			return; // error already reported
		if !(m instanceof CoreOperation) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,null,m,new ENode[]{~lval,~value}));
			else if (lval instanceof ContainerAccessExpr) {
				ContainerAccessExpr cae = (ContainerAccessExpr)lval;
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~cae.obj,m,new ENode[]{~cae.index, ~value}));
			}
			else
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~lval,m,new ENode[]{~value}));
			return;
		}
		CallType ct = m.makeType((AssignExpr)this);
		Type tval = m.isStatic() ? ct.arg(1) : ct.arg(0);
		resolveENode(lval,lval.getType(env),env);
		if !(value.getType(env).isInstanceOf(tval))
			CastExpr.autoCast(env,value,tval,this,AssignExpr.nodeattr$value);
		resolveENode(value,tval,env);

		Type t1 = lval.getType(env);
		Type t2 = value.getType(env);
		if( !t2.isInstanceOf(t1) ) {
			if (t2 ≡ env.tenv.tpNull && t1.isReference() || m instanceof CoreOperation)
				;
			else if (t2.getErasedType().isInstanceOf(t1.getErasedType()))
				;
			else if (t2.getAutoCastTo(t1) != null) {
				value = new CastExpr(pos,t1,~value);
				resolveENode(value,t1,env);
			}
			else if( t2.isCastableTo(t1) ) {
				Kiev.reportWarning(this, "Unsafe casting from "+t2+" to type "+t1);
				value = new CastExpr(pos,t1,~value);
				resolveENode(value,t1,env);
			} else {
				Kiev.reportError(this, "Value of type "+t2+" can't be assigned to "+lval);
			}
		}

		// Set violation of the field
		if( lval instanceof SFldExpr
		 || (
				lval instanceof IFldExpr
			 && ((IFldExpr)lval).obj instanceof LVarExpr
			 &&	((LVarExpr)((IFldExpr)lval).obj).ident.equals(nameThis)
			)
		) {
			if( ctx_method != null && ctx_method.isInvariantMethod() )
				Kiev.reportError(this,"Side-effect in invariant condition");
			if( ctx_method != null && !ctx_method.isInvariantMethod() ) {
				if( lval instanceof SFldExpr )
					ctx_method.addViolatedField( ((SFldExpr)lval).var );
				else
					ctx_method.addViolatedField( ((IFldExpr)lval).var );
			}
		}

		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
		if (lval.getLvalArity() < 0)
			Kiev.reportError(this,"Assigning to a non-lvalue "+lval);
	}
}

@ViewOf(vcast=true)
public static final view RModifyExpr of ModifyExpr extends RENode {
	public ENode			lval;
	public ENode			value;

	public void resolveENode(Type reqType, Env env) {
		if (lval.getLvalArity() < 0)
			Kiev.reportError(this,"Assigning/modify of a non-lvalue "+lval);
		if( isResolved() ) {
			if (isAutoReturnable())
				RReturnStat.autoReturn(reqType, this, env);
			return;
		}

		Method m = resolveMethodAndNormalize(env);
		if (m == null)
			return; // error already reported
		if !(m instanceof CoreOperation) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,null,m,new ENode[]{~lval,~value}));
			else if (lval instanceof ContainerAccessExpr) {
				ContainerAccessExpr cae = (ContainerAccessExpr)lval;
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~cae.obj,m,new ENode[]{~cae.index, ~value}));
			}
			else
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~lval,m,new ENode[]{~value}));
			return;
		}
		CallType ct = m.makeType((ModifyExpr)this);
		Type tval = m.isStatic() ? ct.arg(1) : ct.arg(0);
		resolveENode(lval,lval.getType(env),env);
		if !(value.getType(env).isInstanceOf(tval))
			CastExpr.autoCast(env,value,tval,this,ModifyExpr.nodeattr$value);
		resolveENode(value,tval,env);

		Type t1 = lval.getType(env);
		Type t2 = value.getType(env);
		if( !t2.isInstanceOf(t1) ) {
			if (t2 ≡ env.tenv.tpNull && t1.isReference() || m instanceof CoreOperation)
				;
			else if (t2.getErasedType().isInstanceOf(t1.getErasedType()))
				;
			else if (t2.getAutoCastTo(t1) != null) {
				value = new CastExpr(pos,t1,~value);
				resolveENode(value,t1,env);
			}
			else if( t2.isCastableTo(t1) ) {
				Kiev.reportWarning(this, "Unsafe casting from "+t2+" to type "+t1);
				value = new CastExpr(pos,t1,~value);
				resolveENode(value,t1,env);
			} else {
				Kiev.reportError(this, "Value of type "+t2+" can't be assigned to "+lval);
			}
		}

		// Set violation of the field
		if( lval instanceof SFldExpr
		 || (
				lval instanceof IFldExpr
			 && ((IFldExpr)lval).obj instanceof LVarExpr
			 &&	((LVarExpr)((IFldExpr)lval).obj).ident.equals(nameThis)
			)
		) {
			if( ctx_method != null && ctx_method.isInvariantMethod() )
				Kiev.reportError(this,"Side-effect in invariant condition");
			if( ctx_method != null && !ctx_method.isInvariantMethod() ) {
				if( lval instanceof SFldExpr )
					ctx_method.addViolatedField( ((SFldExpr)lval).var );
				else
					ctx_method.addViolatedField( ((IFldExpr)lval).var );
			}
		}

		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
		if (lval.getLvalArity() < 0)
			Kiev.reportError(this,"Assigning/modify of a non-lvalue "+lval);
	}
}

@ViewOf(vcast=true)
public static final view RBinaryExpr of BinaryExpr extends RENode {
	public ENode			expr1;
	public ENode			expr2;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) {
			if (isAutoReturnable())
				RReturnStat.autoReturn(reqType, this, env);
			return;
		}
		
		Method m = resolveMethodAndNormalize(env);
		if (m == null)
			return; // error already reported
		if !(m instanceof CoreOperation) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,null,m,new ENode[]{~expr1,~expr2}));
			else
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~expr1,m,new ENode[]{~expr2}));
			return;
		}
		CallType ct = m.makeType((BinaryExpr)this);
		if (m.isStatic()) {
			m.makeArgs(getEArgs(),reqType);
			resolveENode(expr1,ct.arg(0),env);
			resolveENode(expr2,ct.arg(1),env);
		} else {
			m.makeArgs(new ENode[]{expr2},reqType);
			resolveENode(expr1,((TypeDecl)m.parent()).getType(env),env);
			resolveENode(expr2,ct.arg(0),env);
		}
		// Check if both expressions are constant
		if (isConstantExpr(env)) {
			replaceWithNodeResolve(env, reqType, ((CoreOperation)m).calc(this));
			return;
		}
		
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}

}

@ViewOf(vcast=true)
public static view RUnaryExpr of UnaryExpr extends RENode {
	public ENode			expr;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) {
			if (isAutoReturnable())
				RReturnStat.autoReturn(reqType, this, env);
			return;
		}
		
		Method m = resolveMethodAndNormalize(env);
		if (m == null)
			return; // error already reported
		if !(m instanceof CoreOperation) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,null,m,new ENode[]{~expr}));
			else
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~expr,m,ENode.emptyArray));
			return;
		}
		CallType ct = m.makeType((UnaryExpr)this);
		if (m.isStatic()) {
			m.makeArgs(getEArgs(),reqType);
			resolveENode(expr,ct.arg(0),env);
		} else {
			m.makeArgs(ENode.emptyArray,reqType);
			resolveENode(expr,((TypeDecl)m.parent()).getType(env),env);
		}
		// Check if expression is a constant
		if (isConstantExpr(env)) {
			replaceWithNodeResolve(env, reqType, ((CoreOperation)m).calc(this));
			return;
		}
		
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}


@ViewOf(vcast=true)
public static final view RStringConcatExpr of StringConcatExpr extends RENode {
	public:ro	ENode[]			args;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) return;
		foreach (ENode e; args)
			resolveENode(e,null,env);
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RCommaExpr of CommaExpr extends RENode {
	public:ro	ENode[]		exprs;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) return;
		for(int i=0; i < exprs.length; i++) {
			if( i < exprs.length-1) {
				resolveENode(exprs[i],env.tenv.tpVoid,env);
				exprs[i].setGenVoidExpr(true);
			} else {
				resolveENode(exprs[i],reqType,env);
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static view RBlock of Block extends RENode {
	public:ro	ASTNode[]		stats;

	public void resolveENode(Type reqType, Env env) {
		RBlock.resolveStats(reqType, Env.getSpacePtr(this,"stats"), env);
	}

	public static void resolveStats(Type reqType, SpacePtr stats, Env env) {
		ENode self = (ENode)stats.node;
		boolean directFlowReachable = self.isDirectFlowReachable();
		if (self instanceof SwitchStat)
			directFlowReachable = false;
		int sz = stats.length;
		for (int i=0; i < sz; i++) {
			ASTNode st = (ASTNode)stats[i];
			if (st instanceof ENode)
				st.setDirectFlowReachable(directFlowReachable);
			try {
				if( (i == sz-1) && self.isAutoReturnable() && st instanceof ENode)
					st.setAutoReturnable(true);
				if( self.isAbrupted() && (st instanceof LabeledStat || st instanceof CaseLabel) )
					self.setAbrupted(false);
				//if( self.isAbrupted() )
				//	; //Kiev.reportWarning(stats[i].pos,"Possible unreachable statement");
				if (st instanceof ENode) {
					if (i < sz || reqType == env.tenv.tpVoid) {
						st.setGenVoidExpr(true);
						resolveENode(st,env.tenv.tpVoid,env);
					} else {
						resolveENode(st,reqType,env);
					}
				}
				else if (st instanceof DNode) {
					resolveDNode(st,env);
				}
				else if (st instanceof SNode) {
					resolveSNode(st,env);
				}
				st = (ASTNode)stats[i];
				if( st instanceof ENode && st.isAbrupted() && !self.isBreaked() ) self.setAbrupted(true);
				if( st instanceof ENode && st.isMethodAbrupted() && !self.isBreaked() ) self.setMethodAbrupted(true);
				if( st instanceof ENode && st.isAbrupted() ) directFlowReachable = false;
				if( st instanceof LabeledStat || st instanceof CaseLabel ) directFlowReachable = true;
			} catch(Exception e ) {
				Kiev.reportError((ASTNode)stats[i],e);
			}
		}
	}
}

@ViewOf(vcast=true)
public static final view RIncrementExpr of IncrementExpr extends RENode {
	public ENode		lval;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) {
			if (isAutoReturnable())
				RReturnStat.autoReturn(reqType, this, env);
			return;
		}
		
		Method m = resolveMethodAndNormalize(env);
		if (m == null)
			return; // error already reported
		if (m.isStatic()) {
			m.makeArgs(getEArgs(),reqType);
			resolveENode(lval,m.params[0].getType(env),env);
		} else {
			m.makeArgs(ENode.emptyArray,reqType);
			resolveENode(lval,((TypeDecl)m.parent()).getType(env),env);
		}
		if !(m instanceof CoreOperation) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,null,m,new ENode[]{~lval}));
			else
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~lval,m,ENode.emptyArray));
			return;
		}
		
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RConditionalExpr of ConditionalExpr extends RENode {
	public ENode		cond;
	public ENode		expr1;
	public ENode		expr2;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) return;
		resolveENode(cond,env.tenv.tpBoolean,env);
		resolveENode(expr1,reqType,env);
		resolveENode(expr2,reqType,env);

		if( expr1.getType(env) ≉ getType(env) ) {
			expr1 = new CastExpr(expr1.pos,getType(env),~expr1);
			resolveENode(expr1,getType(env),env);
		}
		if( expr2.getType(env) ≉ getType(env) ) {
			expr2 = new CastExpr(expr2.pos,getType(env),~expr2);
			resolveENode(expr2,getType(env),env);
		}
		setResolved(true);
	}
}

@ViewOf(vcast=true)
public static final view RCastExpr of CastExpr extends RENode {
	public ENode	expr;
	public TypeRef	ctype;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) return;
		Type ctype = this.ctype.getType(env);
		resolveENode(expr,ctype,env);
		if (expr instanceof TypeRef)
			((RTypeRef)(TypeRef)expr).toExpr(ctype,env);
		Type extp = Type.getRealType(ctype,expr.getType(env));
		if( ctype ≡ env.tenv.tpBoolean && extp ≡ env.tenv.tpRule ) {
			replaceWithNodeResolve(env, reqType,~expr);
			return;
		}
		// Try to find $cast method
		if (extp.getAutoCastTo(ctype) == null) {
			if( tryOverloadedCast(extp, env) )
				return;
			if (extp instanceof CTimeType) {
				ENode e = extp.makeUnboxedExpr(expr);
				if (e != expr) {
					expr = e;
					resolveENode(reqType,env);
					return;
				}
			}
		}
		else if (extp instanceof CTimeType && extp.getUnboxedType().getAutoCastTo(ctype) != null) {
			if( tryOverloadedCast(extp, env) )
				return;
			ENode e = extp.makeUnboxedExpr(expr);
			if (e != expr) {
				expr = e;
				resolveENode(reqType,env);
				return;
			}
		}
		else if (!extp.isInstanceOf(ctype) && extp.getStruct() != null && extp.getStruct().isStructView()
				&& ((KievView)extp.getStruct()).view_of.getType(env).getAutoCastTo(ctype) != null)
		{
			if( tryOverloadedCast(extp, env) )
				return;
			this.resolve2(ctype,env);
			return;
		}
		else {
			this.resolve2(ctype,env);
			return;
		}
		if( extp.isCastableTo(ctype) ) {
			this.resolve2(ctype,env);
			return;
		}
		throw new CompilerException(this,"Expression "+expr+" of type "+extp+" is not castable to "+ctype);
	}

	public final boolean tryOverloadedCast(Type et, Env env) {
		ResInfo<Method> info = new ResInfo<Method>(env,this,nameCastOp,ResInfo.noStatic|ResInfo.noForwards|ResInfo.noSyntaxContext);
		CallType mt = new CallType(et,null,null,this.ctype.getType(env),false);
		if( PassInfo.resolveBestMethodR(et,info,mt) ) {
			Method m = info.resolvedDNode();
			TypeRef[] targs = new TypeRef[m.targs.length];
			for (int i=0; i < targs.length; i++) {
				ArgType at = m.targs[i].getAType(env);
				Type tp = info.resolved_type.resolve(at);
				targs[i] = new TypeRef(tp);
			}
			ENode call = info.buildCall((ASTNode)this,~expr,targs,ENode.emptyArray).closeBuild();
			if (this.ctype.getType(env).isReference())
				call.setCastCall(true);
			replaceWithNodeResolve(env, ctype.getType(env),call);
			return true;
		}
		info = new ResInfo<Method>(env,this,nameCastOp,ResInfo.noForwards|ResInfo.noSyntaxContext);
		mt = new CallType(null,null,new Type[]{expr.getType(env)},this.ctype.getType(env),false);
		if( PassInfo.resolveMethodR(this,info,mt)
			|| this.ctype.getTypeDecl(env).resolveMethodR(info,mt)
			|| expr.getType(env).meta_type.tdecl.resolveMethodR(info,mt)
		) {
			Method m = info.resolvedDNode();
			TypeRef[] targs = new TypeRef[m.targs.length];
			for (int i=0; i < targs.length; i++) {
				ArgType at = m.targs[i].getAType(env);
				Type tp = info.resolved_type.resolve(at);
				targs[i] = new TypeRef(tp);
			}
			assert(m.isStatic());
			ENode call = new CallExpr(pos,null,info.resolvedSymbol(),targs,new ENode[]{~expr});
			replaceWithNodeResolve(env, ctype.getType(env),call);
			return true;
		}
		return false;
	}
	
	public final void tryTypeArgCast(Env env, Type reqType, Type tp, Type et) {
		if (tp instanceof ArgType && tp.isUnerasable()) {
			replaceWithNodeResolve(env, reqType,
				new ReinterpExpr(pos,tp,
					new CallExpr(pos,
						new TypeInfoExpr(tp),
						env.tenv.tpTypeInfo.tdecl.resolveMethod(env,"$checkcast",env.tenv.tpObject,env.tenv.tpObject),
						new ENode[]{~expr}
					)
				)
			);
			return;
		}
//		if (tp instanceof CompaundType && tp.isUnerasable()) {
//			expr.replaceWithNodeResolve(env, env.tenv.tpObject,
//				new CallExpr(pos,
//					new TypeInfoExpr(tp),
//					env.tenv.tpTypeInfo.clazz.resolveMethod("$checkcast",env.tenv.tpObject,env.tenv.tpObject),
//					new ENode[]{~expr}
//				)
//			);
//			return;
//		}
	}

	public final void resolve2(Type reqType, Env env) {
		Type ctype = this.ctype.getType(env);
		resolveENode(expr,ctype,env);
		if (reqType ≡ env.tenv.tpVoid) {
			setResolved(true);
		}
		Type et = Type.getRealType(ctype,expr.getType(env));
		// Try wrapped field
		if (et instanceof CTimeType && et.getUnboxedType().equals(ctype)) {
			expr = et.makeUnboxedExpr(expr);
			resolveENode(reqType,env);
			return;
		}
		// try null to something...
		if (et ≡ env.tenv.tpNull && reqType.isReference())
			return;
		if( ctype ≡ env.tenv.tpBoolean && et ≡ env.tenv.tpRule ) {
			replaceWithNodeResolve(env, ctype, new BinaryBoolExpr(pos,env.coreFuncs.fObjectBoolNE,expr,new ConstNullExpr()));
			return;
		}
		if( ctype.isBoolean() && et.isBoolean() )
			return;
		if( ctype.isInstanceOf(env.tenv.tpEnum) && et.isIntegerInCode() ) {
			if (ctype.isIntegerInCode())
				return;
			Method cm = ((CompaundType)ctype).tdecl.resolveMethod(env,nameCastOp,ctype,env.tenv.tpInt);
			replaceWithNodeResolve(env, reqType, new CallExpr(pos,null,cm,new ENode[]{~expr}));
			return;
		}
		if( ctype.isIntegerInCode() && et.isInstanceOf(env.tenv.tpEnum) ) {
			if (et.isIntegerInCode())
				return;
			Method cf = env.tenv.tpEnum.tdecl.resolveMethod(env,nameEnumOrdinal, env.tenv.tpInt);
			replaceWithNodeResolve(env, reqType, new CallExpr(pos,~expr,cf,ENode.emptyArray));
			return;
		}
		// Try to find $cast method
		if( et.getAutoCastTo(ctype) == null && tryOverloadedCast(et, env))
			return;

		if( et.isReference() != ctype.isReference() && !(expr instanceof ClosureCallExpr) )
			if( !et.isReference() && ctype instanceof ArgType )
				Kiev.reportWarning(this,"Cast of argument to primitive type - ensure 'generate' of this type and wrapping in if( A instanceof type ) statement");
			else if (et.getStruct() == null || !et.getStruct().isEnum())
				throw new CompilerException(this,"Expression "+expr+" of type "+et+" cannot be casted to type "+ctype);
		if( !et.isCastableTo(ctype) ) {
			throw new RuntimeException("Expression "+expr+" cannot be casted to type "+ctype);
		}
		if( Kiev.verify && expr.getType(env) ≉ et ) {
			tryTypeArgCast(env,reqType,ctype,et);
			setResolved(true);
			return;
		}
		if( et.isReference() && et.isInstanceOf(ctype) ) {
			tryTypeArgCast(env,reqType,ctype,et);
			setResolved(true);
			return;
		}
		if( et.isReference() && ctype.isReference() && et.getStruct() != null
		 && et.getStruct().isStructInner()
		 && !et.getStruct().isStatic()
		 && !(et instanceof ArgType)
		 && Env.ctxTDecl(et.getStruct()).isClazz()
		 && Env.ctxTDecl(et.getStruct()).getType(env).getAutoCastTo(ctype) != null
		) {
			replaceWithNodeResolve(env, reqType,
				new CastExpr(pos,ctype,
					new IFldExpr(pos,~expr,OuterThisAccessExpr.outerOf((Struct)et.getStruct()))
				));
			return;
		}
		if( expr.isConstantExpr(env) ) {
			Object val = expr.getConstValue(env);
			Type t = ctype;
			if( val instanceof Number ) {
				Number num = (Number)val;
				if     ( t ≡ env.tenv.tpDouble ) { replaceWithNodeResolve(env, new ConstDoubleExpr ((double)num.doubleValue())); return; }
				else if( t ≡ env.tenv.tpFloat )  { replaceWithNodeResolve(env, new ConstFloatExpr  ((float) num.floatValue())); return; }
				else if( t ≡ env.tenv.tpLong )   { replaceWithNodeResolve(env, new ConstLongExpr   ((long)  num.longValue())); return; }
				else if( t ≡ env.tenv.tpInt )    { replaceWithNodeResolve(env, new ConstIntExpr    ((int)   num.intValue())); return; }
				else if( t ≡ env.tenv.tpShort )  { replaceWithNodeResolve(env, new ConstShortExpr  ((short) num.intValue())); return; }
				else if( t ≡ env.tenv.tpByte )   { replaceWithNodeResolve(env, new ConstByteExpr   ((byte)  num.intValue())); return; }
				else if( t ≡ env.tenv.tpChar )   { replaceWithNodeResolve(env, new ConstCharExpr   ((char)  num.intValue())); return; }
			}
			else if( val instanceof Character ) {
				char num = ((Character)val).charValue();
				if     ( t ≡ env.tenv.tpDouble ) { replaceWithNodeResolve(env, new ConstDoubleExpr ((double)(int)num)); return; }
				else if( t ≡ env.tenv.tpFloat )  { replaceWithNodeResolve(env, new ConstFloatExpr  ((float) (int)num)); return; }
				else if( t ≡ env.tenv.tpLong )   { replaceWithNodeResolve(env, new ConstLongExpr   ((long)  (int)num)); return; }
				else if( t ≡ env.tenv.tpInt )    { replaceWithNodeResolve(env, new ConstIntExpr    ((int)   (int)num)); return; }
				else if( t ≡ env.tenv.tpShort )  { replaceWithNodeResolve(env, new ConstShortExpr  ((short) (int)num)); return; }
				else if( t ≡ env.tenv.tpByte )   { replaceWithNodeResolve(env, new ConstByteExpr   ((byte)  (int)num)); return; }
				else if( t ≡ env.tenv.tpChar )   { replaceWithNodeResolve(env, new ConstCharExpr   ((char)  num)); return; }
			}
			else if( val instanceof Boolean ) {
				int num = ((Boolean)val).booleanValue() ? 1 : 0;
				if     ( t ≡ env.tenv.tpDouble ) { replaceWithNodeResolve(env, new ConstDoubleExpr ((double)num)); return; }
				else if( t ≡ env.tenv.tpFloat )  { replaceWithNodeResolve(env, new ConstFloatExpr  ((float) num)); return; }
				else if( t ≡ env.tenv.tpLong )   { replaceWithNodeResolve(env, new ConstLongExpr   ((long)  num)); return; }
				else if( t ≡ env.tenv.tpInt )    { replaceWithNodeResolve(env, new ConstIntExpr    ((int)   num)); return; }
				else if( t ≡ env.tenv.tpShort )  { replaceWithNodeResolve(env, new ConstShortExpr  ((short) num)); return; }
				else if( t ≡ env.tenv.tpByte )   { replaceWithNodeResolve(env, new ConstByteExpr   ((byte)  num)); return; }
				else if( t ≡ env.tenv.tpChar )   { replaceWithNodeResolve(env, new ConstCharExpr   ((char)  num)); return; }
			}
		}
		if( !et.equals(ctype) && expr instanceof ClosureCallExpr && ((ClosureCallExpr)expr).isKindAuto() && et instanceof CallType ) {
			if( et.getAutoCastTo(ctype) != null ) {
				((ClosureCallExpr)expr).kind = ClosureCallKind.CALL;
				return;
			}
			else if( et.isCastableTo(ctype) ) {
				((ClosureCallExpr)expr).kind = ClosureCallKind.CALL;
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}

}

