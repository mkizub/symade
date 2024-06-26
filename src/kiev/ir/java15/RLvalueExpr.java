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
 * @version $Revision: 296 $
 *
 */

@ViewOf(vcast=true)
public abstract static view RLvalueExpr of LvalueExpr extends RENode {
}

@ViewOf(vcast=true)
public static final view RAccessExpr of AccessExpr extends RLvalueExpr {
	public ENode		obj;
}

@ViewOf(vcast=true)
public static final view RIFldExpr of IFldExpr extends RLvalueExpr {
	public		ENode		obj;
	public:ro	Field		var;

	public void resolveENode(Type reqType, Env env) throws RuntimeException {
		resolveENode(obj,null,env);

		// Set violation of the field
		if( ctx_method != null
		 && obj instanceof LVarExpr && ((LVarExpr)obj).ident.equals(nameThis)
		)
			ctx_method.addViolatedField(var);

		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RContainerAccessExpr of ContainerAccessExpr extends RLvalueExpr {
	public ENode		obj;
	public ENode		index;

	public void resolveENode(Type reqType, Env env) throws RuntimeException {
		if( isResolved() ) return;
		resolveENode(obj,null,env);
		resolveENode(index,null,env);
		// Resolve overloaded access method
		CallType mt = new CallType(obj.getType(env),null,new Type[]{index.getType(env)},env.tenv.tpAny,false);
		ResInfo<Method> info = new ResInfo<Method>(env,(ASTNode)this,nameArrayGetOp,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic);
		if( !PassInfo.resolveBestMethodR(obj.getType(env),info,mt) )
			throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayGetOp,mt));
		Method m = info.resolvedDNode();
		if !(m.isMacro() && m.isNative()) {
			// Not a standard operator
			if( m.dnode.isStatic() )
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,null,info.resolvedSymbol(),new ENode[]{~obj,~index}));
			else
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~obj,info.resolvedSymbol(),new ENode[]{~index}));
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RThisExpr of ThisExpr extends RLvalueExpr {

	public void resolveENode(Type reqType, Env env) throws RuntimeException {
		if( isResolved() ) return;
		ANode p = parent();
		while !(p instanceof Method || p instanceof Initializer || p instanceof Field)
			p = p.parent();
		DNode decl = (DNode)p;
		if (decl.isStatic() && ctx_tdecl.sname != nameIFaceImpl)
			Kiev.reportError(this,"Access '"+parent()+"' in static context");
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RSuperExpr of SuperExpr extends RENode {

	public void resolveENode(Type reqType, Env env) throws RuntimeException {
		if( isResolved() ) return;
		ANode p = parent();
		while !(p instanceof Method || p instanceof Initializer || p instanceof Field)
			p = p.parent();
		DNode decl = (DNode)p;
		if (decl.isStatic() && ctx_tdecl.sname != nameIFaceImpl)
			Kiev.reportError(this,"Access '"+parent()+"' in static context");
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public final view RLVarExpr of LVarExpr extends RLvalueExpr {

	public:ro	Var			var;

	public Var getVarSafe();

	public void resolveENode(Type reqType, Env env) throws RuntimeException {
		// Check if we try to access this var from local inner/anonymouse class
		ComplexTypeDecl ctx_tdecl = this.ctx_tdecl;
		if !(ctx_tdecl.parent() instanceof KievPackage || ctx_tdecl.parent() instanceof ComplexTypeDecl) {
			if (Env.ctxTDecl(getVarSafe()) != ctx_tdecl) {
				var.setNeedProxy(true);
				setAsField(true);
				// Now we need to add this var as a fields to
				// local class and to initializer of this class
				Field vf;
				if ((vf = ctx_tdecl.resolveField(env,this.ident,false)) == null) {
					// Add field
					vf = new Field(this.ident,var.getType(env),ACC_PUBLIC);
					ctx_tdecl.members.add(vf);
					vf.setNeedProxy(true);
					vf.init = new Copier().copyFull((ENode)this);
				}
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RSFldExpr of SFldExpr extends RLvalueExpr {
	public		TypeRef		obj;
	public:ro	Field		var;

	public void resolveENode(Type reqType, Env env) throws RuntimeException {
		if( isResolved() ) return;
		// Set violation of the field
		if( ctx_method != null )
			ctx_method.addViolatedField(var);
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view ROuterThisAccessExpr of OuterThisAccessExpr extends RENode {
	public		TypeRef			outer;
	public:ro	Var[]			outer_refs;

	public void setupOuterFields();
	
	public void resolveENode(Type reqType, Env env) throws RuntimeException {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving "+this);
		setupOuterFields();
		if( Kiev.debug && Kiev.debugResolve ) {
			StringBuffer sb = new StringBuffer("Outer 'this' resolved as this");
			for(int i=0; i < outer_refs.length; i++)
				sb.append("->").append(outer_refs[i].sname);
			System.out.println(sb.toString());
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public static final view RReinterpExpr of ReinterpExpr extends RLvalueExpr {
	public TypeRef		ctype;
	public ENode		expr;

	public void resolveENode(Type reqType, Env env) {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving "+this);
		resolveENode(expr,null,env);
		Type ctype = this.getType(env);
		Type extp = expr.getType(env);
		if (ctype ≈ extp) {
			replaceWithNodeResolve(env, reqType,~expr);
			return;
		}
		if (ctype.isIntegerInCode() && extp.isIntegerInCode())
			;
		else if (extp.isInstanceOf(ctype))
			;
		else if (extp.getErasedType().isInstanceOf(ctype.getErasedType()))
			;
		else if (ctype instanceof CTimeType && ctype.getEnclosedType() ≈ extp)
			;
		else if (extp instanceof CTimeType && extp.getEnclosedType() ≈ ctype)
			;
		else
			Kiev.reportError(this, "Cannot reinterpret "+extp+" as "+ctype);
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

