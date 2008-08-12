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
public final view RCoreExpr of CoreExpr extends RENode {

	public void resolve(Type reqType) {
		setResolved(true);
	}
}

@ViewOf(vcast=true, iface=true)
public final view RShadow of Shadow extends RENode {
	public ASTNode		rnode;

	public void resolve(Type reqType) {
		if (rnode instanceof ENode)
			((ENode)rnode).resolve(reqType);
		else
			((Initializer)rnode).resolveDecl();
		setResolved(true);
	}
}

@ViewOf(vcast=true, iface=true)
public final view RTypeClassExpr of TypeClassExpr extends RENode {
	public TypeRef		ttype;

	public void resolve(Type reqType) {
		Type tp = ttype.getType();
		if (!tp.isReference()) {
			Type rt = ((CoreType)tp).getRefTypeForPrimitive();
			Field f = rt.tdecl.resolveField("TYPE");
			replaceWithNodeResolve(reqType,new SFldExpr(pos,f));
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public final view RTypeInfoExpr of TypeInfoExpr extends RENode {
	public		TypeRef				ttype;
	public		ENode				cl_expr;
	public:ro	ENode[]				cl_args;

	public void resolve(Type reqType) {
		if (isResolved())
			return;
		Type ttype = this.ttype.getType();
		Struct clazz = ttype.getStruct();
		if (clazz == null)
			clazz = ttype.getErasedType().getStruct();
		if (clazz == null) {
			if (ttype instanceof CoreType || ttype.isArray()) {
				cl_expr = new TypeClassExpr(pos,new TypeRef(ttype));
				cl_expr.resolve(Type.tpClass);
			} else {
				Kiev.reportError(this,"Cannot make typeinfo expression for type "+ttype);
			}
		} else {
			if (clazz.isTypeUnerasable()) {
				if (clazz.typeinfo_clazz == null)
					((RStruct)clazz).autoGenerateTypeinfoClazz();
			}
			cl_expr = new TypeClassExpr(pos,new TypeRef(clazz.xtype));
			cl_expr.resolve(Type.tpClass);
			((TypeInfoExpr)this).cl_args.delAll();
			foreach (ArgType at; ((RStruct)clazz).getTypeInfoArgs()) {
				ENode tie = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((TypeInfoExpr)this, ttype.resolve(at),false);
				((TypeInfoExpr)this).cl_args.add(tie);
				Kiev.runProcessorsWithRewalk(tie);
			}
			foreach (ENode tie; cl_args)
				tie.resolve(null);
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public final view RAssertEnabledExpr of AssertEnabledExpr extends RENode {

	public void resolve(Type reqType) {
		if (!Kiev.debugOutputA) {
			replaceWithNodeResolve(reqType, new ConstBoolExpr(false));
			return;
		}
		// get top-level class
		TypeDecl clazz = ctx_tdecl;
		while !(clazz.parent() instanceof KievPackage || clazz.ctx_tdecl.isInterface())
			clazz = clazz.ctx_tdecl;
		// find $assertionsEnabled
		foreach (Field f; clazz.members; f.sname == "$assertionsEnabled") {
			replaceWithNodeResolve(reqType, new SFldExpr(pos,f));
			return;
		}
		Field f = new Field("$assertionsEnabled", Type.tpBoolean, ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC);
		clazz.members.add(f);
		f.init = new CallExpr(0,
			new TypeClassExpr(0,new TypeRef(clazz.xtype)),
			Type.tpClass.tdecl.resolveMethod("desiredAssertionStatus", Type.tpBoolean),
			ENode.emptyArray
			);
		f.resolveDecl();
		if !(f.isAddedToInit()) {
			// Add initialization in <clinit>
			Constructor class_init = ((Struct)clazz).getClazzInitMethod();
			class_init.addstats.insert(0,
				new ExprStat(f.init.pos,
					new AssignExpr(f.init.pos,Operator.Assign
						,new SFldExpr(f.pos,f),f.init.ncopy())
				)
			);
			f.setAddedToInit(true);
			Kiev.runProcessorsOn(class_init.addstats[0]);
		}
		replaceWithNodeResolve(reqType, new SFldExpr(pos,f));
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RAssignExpr of AssignExpr extends RENode {
	public Operator			op;
	public ENode			lval;
	public ENode			value;

	public void resolve(Type reqType) {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}

		Method m = resolveMethodAndNormalize();
		if (m == null)
			return; // error already reported
		CallType ct = m.makeType(null,getArgs());
		if (m.isStatic()) {
			m.makeArgs(getArgs(),reqType);
			ENode[] args = getArgs();
			for (int i=0; i < args.length; i++)
				args[i].resolve(ct.arg(i));
		} else {
			ENode[] args = getArgs();
			ENode[] tmp = new ENode[args.length-1];
			for (int i=0; i < tmp.length; i++)
				tmp[i] = args[i+1];
			m.makeArgs(tmp,reqType);
			args = getArgs();
			args[0].resolve(((TypeDecl)m.parent()).xtype);
			for (int i=1; i < args.length; i++)
				args[i].resolve(ct.arg(i-1));
		}
		if !(m.body instanceof CoreExpr) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(reqType, new CallExpr(pos,null,m,new ENode[]{~lval,~value}));
			else
				replaceWithNodeResolve(reqType, new CallExpr(pos,~lval,m,new ENode[]{~value}));
			return;
		}

		Type t1 = lval.getType();
		Type t2 = value.getType();
		if( !t2.isInstanceOf(t1) ) {
			if (t2 ≡ StdTypes.tpNull && t1.isReference() || m.body instanceof CoreExpr)
				;
			else if (t2.getErasedType().isInstanceOf(t1.getErasedType()))
				;
			else if (t2.getAutoCastTo(t1) != null) {
				value = new CastExpr(pos,t1,~value);
				value.resolve(t1);
			}
			else if( t2.isCastableTo(t1) ) {
				Kiev.reportWarning(this, "Unsafe casting from "+t2+" to type "+t1);
				value = new CastExpr(pos,t1,~value);
				value.resolve(t1);
			} else {
				Kiev.reportError(this, "Value of type "+t2+" can't be assigned to "+lval);
			}
		}
		DataFlowInfo.getDFlow((AssignExpr)this).out();

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
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RBinaryExpr of BinaryExpr extends RENode {
	public Operator			op;
	public ENode			expr1;
	public ENode			expr2;

	public void resolve(Type reqType) {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		
		Method m = resolveMethodAndNormalize();
		if (m == null)
			return; // error already reported
		CallType ct = m.makeType(null,getArgs());
		if (m.isStatic()) {
			m.makeArgs(getArgs(),reqType);
			expr1.resolve(ct.arg(0));
			expr2.resolve(ct.arg(1));
		} else {
			m.makeArgs(new ENode[]{expr2},reqType);
			expr1.resolve(((TypeDecl)m.parent()).xtype);
			expr2.resolve(ct.arg(0));
		}
		if !(m.body instanceof CoreExpr) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(reqType, new CallExpr(pos,null,m,new ENode[]{~expr1,~expr2}));
			else
				replaceWithNodeResolve(reqType, new CallExpr(pos,~expr1,m,new ENode[]{~expr2}));
			return;
		}
		// Check if both expressions are constant
		if( expr1.isConstantExpr() && expr2.isConstantExpr() ) {
			ConstExpr ce = ((CoreExpr)m.body).calc(this);
			replaceWithNodeResolve(reqType, ce);
			return;
		}
		
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}

}

@ViewOf(vcast=true, iface=true)
public static view RUnaryExpr of UnaryExpr extends RENode {
	public Operator			op;
	public ENode			expr;

	public void resolve(Type reqType) {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		
		Method m = resolveMethodAndNormalize();
		if (m == null)
			return; // error already reported
		CallType ct = m.makeType(null,getArgs());
		if (m.isStatic()) {
			m.makeArgs(getArgs(),reqType);
			expr.resolve(ct.arg(0));
		} else {
			m.makeArgs(ENode.emptyArray,reqType);
			expr.resolve(((TypeDecl)m.parent()).xtype);
		}
		if !(m.body instanceof CoreExpr) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(reqType, new CallExpr(pos,null,m,new ENode[]{~expr}));
			else
				replaceWithNodeResolve(reqType, new CallExpr(pos,~expr,m,ENode.emptyArray));
			return;
		}
		// Check if expression is a constant
		if (expr.isConstantExpr()) {
			ConstExpr ce = ((CoreExpr)m.body).calc(this);
			replaceWithNodeResolve(reqType, ce);
			return;
		}
		
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}


@ViewOf(vcast=true, iface=true)
public static final view RStringConcatExpr of StringConcatExpr extends RENode {
	public:ro	ENode[]			args;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		foreach (ENode e; args)
			e.resolve(null);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RCommaExpr of CommaExpr extends RENode {
	public:ro	ENode[]		exprs;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		for(int i=0; i < exprs.length; i++) {
			if( i < exprs.length-1) {
				exprs[i].resolve(Type.tpVoid);
				exprs[i].setGenVoidExpr(true);
			} else {
				exprs[i].resolve(reqType);
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static view RBlock of Block extends RENode {
	public:ro	ASTNode[]		stats;

	public void resolve(Type reqType) {
		RBlock.resolveStats(reqType, getSpacePtr("stats"));
	}

	public static void resolveStats(Type reqType, SpacePtr stats) {
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
					if (i < sz || reqType == Type.tpVoid) {
						st.setGenVoidExpr(true);
						st.resolve(Type.tpVoid);
					} else {
						st.resolve(reqType);
					}
				}
				else if (st instanceof DNode) {
					st.resolveDecl();
				}
				else if (st instanceof SNode) {
					st.resolveDecl();
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

@ViewOf(vcast=true, iface=true)
public static final view RIncrementExpr of IncrementExpr extends RENode {
	public Operator		op;
	public ENode		lval;

	public void resolve(Type reqType) {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		
		Method m = resolveMethodAndNormalize();
		if (m == null)
			return; // error already reported
		if (m.isStatic()) {
			m.makeArgs(getArgs(),reqType);
			lval.resolve(m.params[0].getType());
		} else {
			m.makeArgs(ENode.emptyArray,reqType);
			lval.resolve(((TypeDecl)m.parent()).xtype);
		}
		if !(m.body instanceof CoreExpr) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(reqType, new CallExpr(pos,null,m,new ENode[]{~lval}));
			else
				replaceWithNodeResolve(reqType, new CallExpr(pos,~lval,m,ENode.emptyArray));
			return;
		}
		
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RConditionalExpr of ConditionalExpr extends RENode {
	public ENode		cond;
	public ENode		expr1;
	public ENode		expr2;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		cond.resolve(Type.tpBoolean);
		expr1.resolve(reqType);
		expr2.resolve(reqType);

		if( expr1.getType() ≉ getType() ) {
			expr1 = new CastExpr(expr1.pos,getType(),~expr1);
			expr1.resolve(getType());
		}
		if( expr2.getType() ≉ getType() ) {
			expr2 = new CastExpr(expr2.pos,getType(),~expr2);
			expr2.resolve(getType());
		}
		setResolved(true);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RCastExpr of CastExpr extends RENode {
	public ENode	expr;
	public TypeRef	ctype;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		Type ctype = this.ctype.getType();
		expr.resolve(ctype);
		if (expr instanceof TypeRef)
			((TypeRef)expr).toExpr(ctype);
		Type extp = Type.getRealType(ctype,expr.getType());
		if( ctype ≡ Type.tpBoolean && extp ≡ Type.tpRule ) {
			replaceWithNodeResolve(reqType,~expr);
			return;
		}
		// Try to find $cast method
		if (extp.getAutoCastTo(ctype) == null) {
			if( tryOverloadedCast(extp) )
				return;
			if (extp instanceof CTimeType) {
				ENode e = extp.makeUnboxedExpr(expr);
				if (e != expr) {
					expr = e;
					resolve(reqType);
					return;
				}
			}
		}
		else if (extp instanceof CTimeType && extp.getUnboxedType().getAutoCastTo(ctype) != null) {
			if( tryOverloadedCast(extp) )
				return;
			ENode e = extp.makeUnboxedExpr(expr);
			if (e != expr) {
				expr = e;
				resolve(reqType);
				return;
			}
		}
		else if (!extp.isInstanceOf(ctype) && extp.getStruct() != null && extp.getStruct().isStructView()
				&& ((KievView)extp.getStruct()).view_of.getType().getAutoCastTo(ctype) != null)
		{
			if( tryOverloadedCast(extp) )
				return;
			this.resolve2(ctype);
			return;
		}
		else {
			this.resolve2(ctype);
			return;
		}
		if( extp.isCastableTo(ctype) ) {
			this.resolve2(ctype);
			return;
		}
		throw new CompilerException(this,"Expression "+expr+" of type "+extp+" is not castable to "+ctype);
	}

	public final boolean tryOverloadedCast(Type et) {
		ResInfo<Method> info = new ResInfo<Method>(this,nameCastOp,ResInfo.noStatic|ResInfo.noForwards|ResInfo.noSyntaxContext);
		CallType mt = new CallType(et,null,null,this.ctype.getType(),false);
		if( PassInfo.resolveBestMethodR(et,info,mt) ) {
			Method m = info.resolvedDNode();
			TypeRef[] targs = new TypeRef[m.targs.length];
			for (int i=0; i < targs.length; i++) {
				ArgType at = m.targs[i].getAType();
				Type tp = info.resolved_type.resolve(at);
				targs[i] = new TypeRef(tp);
			}
			ENode call = info.buildCall((ASTNode)this,~expr,targs,ENode.emptyArray).closeBuild();
			if (this.ctype.getType().isReference())
				call.setCastCall(true);
			replaceWithNodeResolve(ctype.getType(),call);
			return true;
		}
		info = new ResInfo<Method>(this,nameCastOp,ResInfo.noForwards|ResInfo.noSyntaxContext);
		mt = new CallType(null,null,new Type[]{expr.getType()},this.ctype.getType(),false);
		if( PassInfo.resolveMethodR(this,info,mt) ) {
			Method m = info.resolvedDNode();
			TypeRef[] targs = new TypeRef[m.targs.length];
			for (int i=0; i < targs.length; i++) {
				ArgType at = m.targs[i].getAType();
				Type tp = info.resolved_type.resolve(at);
				targs[i] = new TypeRef(tp);
			}
			assert(m.isStatic());
			ENode call = new CallExpr(pos,null,info.resolvedSymbol(),targs,new ENode[]{~expr});
			replaceWithNodeResolve(ctype.getType(),call);
			return true;
		}
		return false;
	}
	
	public final void tryTypeArgCast(Type reqType, Type tp, Type et) {
		if (tp instanceof ArgType && tp.isUnerasable()) {
			replaceWithNodeResolve(reqType,
				new ReinterpExpr(pos,tp,
					new CallExpr(pos,
						((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((CastExpr)this,tp,false),
						Type.tpTypeInfo.tdecl.resolveMethod("$checkcast",Type.tpObject,Type.tpObject),
						new ENode[]{~expr}
					)
				)
			);
			return;
		}
//		if (tp instanceof CompaundType && tp.isUnerasable()) {
//			expr.replaceWithNodeResolve(Type.tpObject,
//				new CallExpr(pos,
//					((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((CastExpr)this,tp,false),
//					Type.tpTypeInfo.clazz.resolveMethod("$checkcast",Type.tpObject,Type.tpObject),
//					new ENode[]{~expr}
//				)
//			);
//			return;
//		}
	}

	public final void resolve2(Type reqType) {
		Type ctype = this.ctype.getType();
		expr.resolve(ctype);
		if (reqType ≡ Type.tpVoid) {
			setResolved(true);
		}
		Type et = Type.getRealType(ctype,expr.getType());
		// Try wrapped field
		if (et instanceof CTimeType && et.getUnboxedType().equals(ctype)) {
			expr = et.makeUnboxedExpr(expr);
			resolve(reqType);
			return;
		}
		// try null to something...
		if (et ≡ Type.tpNull && reqType.isReference())
			return;
		if( ctype ≡ Type.tpBoolean && et ≡ Type.tpRule ) {
			replaceWithNodeResolve(ctype, new BinaryBoolExpr(pos,Operator.NotEquals,expr,new ConstNullExpr()));
			return;
		}
		if( ctype.isBoolean() && et.isBoolean() )
			return;
		if( ctype.isInstanceOf(Type.tpEnum) && et.isIntegerInCode() ) {
			if (ctype.isIntegerInCode())
				return;
			Method cm = ((CompaundType)ctype).tdecl.resolveMethod(nameCastOp,ctype,Type.tpInt);
			replaceWithNodeResolve(reqType, new CallExpr(pos,null,cm,new ENode[]{~expr}));
			return;
		}
		if( ctype.isIntegerInCode() && et.isInstanceOf(Type.tpEnum) ) {
			if (et.isIntegerInCode())
				return;
			Method cf = Type.tpEnum.tdecl.resolveMethod(nameEnumOrdinal, Type.tpInt);
			replaceWithNodeResolve(reqType, new CallExpr(pos,~expr,cf,ENode.emptyArray));
			return;
		}
		// Try to find $cast method
		if( et.getAutoCastTo(ctype) == null && tryOverloadedCast(et))
			return;

		if( et.isReference() != ctype.isReference() && !(expr instanceof ClosureCallExpr) )
			if( !et.isReference() && ctype instanceof ArgType )
				Kiev.reportWarning(this,"Cast of argument to primitive type - ensure 'generate' of this type and wrapping in if( A instanceof type ) statement");
			else if (et.getStruct() == null || !et.getStruct().isEnum())
				throw new CompilerException(this,"Expression "+expr+" of type "+et+" cannot be casted to type "+ctype);
		if( !et.isCastableTo(ctype) ) {
			throw new RuntimeException("Expression "+expr+" cannot be casted to type "+ctype);
		}
		if( Kiev.verify && expr.getType() ≉ et ) {
			tryTypeArgCast(reqType,ctype,et);
			setResolved(true);
			return;
		}
		if( et.isReference() && et.isInstanceOf(ctype) ) {
			tryTypeArgCast(reqType,ctype,et);
			setResolved(true);
			return;
		}
		if( et.isReference() && ctype.isReference() && et.getStruct() != null
		 && et.getStruct().isStructInner()
		 && !et.getStruct().isStatic()
		 && !(et instanceof ArgType)
		 && et.getStruct().ctx_tdecl.isClazz()
		 && et.getStruct().ctx_tdecl.xtype.getAutoCastTo(ctype) != null
		) {
			replaceWithNodeResolve(reqType,
				new CastExpr(pos,ctype,
					new IFldExpr(pos,~expr,OuterThisAccessExpr.outerOf((Struct)et.getStruct()))
				));
			return;
		}
		if( expr.isConstantExpr() ) {
			Object val = expr.getConstValue();
			Type t = ctype;
			if( val instanceof Number ) {
				Number num = (Number)val;
				if     ( t ≡ Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)num.doubleValue())); return; }
				else if( t ≡ Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) num.floatValue())); return; }
				else if( t ≡ Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  num.longValue())); return; }
				else if( t ≡ Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   num.intValue())); return; }
				else if( t ≡ Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) num.intValue())); return; }
				else if( t ≡ Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  num.intValue())); return; }
				else if( t ≡ Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num.intValue())); return; }
			}
			else if( val instanceof Character ) {
				char num = ((Character)val).charValue();
				if     ( t ≡ Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)(int)num)); return; }
				else if( t ≡ Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) (int)num)); return; }
				else if( t ≡ Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  (int)num)); return; }
				else if( t ≡ Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   (int)num)); return; }
				else if( t ≡ Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) (int)num)); return; }
				else if( t ≡ Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  (int)num)); return; }
				else if( t ≡ Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num)); return; }
			}
			else if( val instanceof Boolean ) {
				int num = ((Boolean)val).booleanValue() ? 1 : 0;
				if     ( t ≡ Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)num)); return; }
				else if( t ≡ Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) num)); return; }
				else if( t ≡ Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  num)); return; }
				else if( t ≡ Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   num)); return; }
				else if( t ≡ Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) num)); return; }
				else if( t ≡ Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  num)); return; }
				else if( t ≡ Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num)); return; }
			}
		}
		if( !et.equals(ctype) && expr instanceof ClosureCallExpr && et instanceof CallType ) {
			if( et.getAutoCastTo(ctype) != null ) {
				((ClosureCallExpr)expr).is_a_call = Boolean.TRUE;
				return;
			}
			else if( et.isCastableTo(ctype) ) {
				((ClosureCallExpr)expr).is_a_call = Boolean.TRUE;
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}

}

