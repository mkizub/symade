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
 * @version $Revision$
 *
 */

@ViewOf(vcast=true, iface=true)
public abstract static view RLvalueExpr of LvalueExpr extends RENode {
}

@ViewOf(vcast=true, iface=true)
public static final view RAccessExpr of AccessExpr extends RLvalueExpr {
	public ENode		obj;

	public final ENode makeExpr(ASTNode v, ResInfo info, ASTNode o);

	public void resolve(Type reqType) throws CompilerException {
		ENode[] res;
		Type[] tps;

		// resolve access
		obj.resolve(null);

	try_static:
		if( obj instanceof TypeRef ) {
			tps = new Type[]{ ((TypeRef)obj).getType() };
			res = new ENode[1];
			if( this.ident == nameThis )
				this.replaceWithNodeResolve(reqType,new OuterThisAccessExpr(pos,(TypeRef)~obj));
		}
		else {
			ENode e = obj;
			tps = e.getAccessTypes();
			res = new ENode[tps.length];
			// fall down
		}
		for (int si=0; si < tps.length; si++) {
			if (res[si] != null)
				continue;
			Type tp = tps[si];
			DNode@ v;
			ResInfo info;
			if (!(obj instanceof TypeRef) &&
				tp.resolveNameAccessR(v,info=new ResInfo(this,this.ident,ResInfo.noStatic|ResInfo.noImports)) )
				res[si] = makeExpr(v,info,obj);
			else if (tp.meta_type.tdecl.resolveNameR(v,info=new ResInfo(this,this.ident)))
				res[si] = makeExpr(v,info,tp.getStruct());
		}
		int cnt = 0;
		int idx = -1;
		for (int si=0; si < res.length; si++) {
			if (res[si] != null) {
				cnt ++;
				if (idx < 0) idx = si;
			}
		}
		if (cnt > 1) {
			StringBuffer msg = new StringBuffer("Umbigous access:\n");
			for(int si=0; si < res.length; si++) {
				if (res[si] == null)
					continue;
				msg.append("\t").append(res).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
		if (cnt == 0) {
			StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
			for(int si=0; si < res.length; si++) {
				if (tps[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
			//return;
		}
		this.replaceWithNodeResolve(reqType,res[idx].closeBuild());
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RIFldExpr of IFldExpr extends RLvalueExpr {
	public		ENode		obj;
	public:ro	Field		var;

	public void resolve(Type reqType) throws RuntimeException {
		obj.resolve(null);

		// Set violation of the field
		if( ctx_method != null
		 && obj instanceof LVarExpr && ((LVarExpr)obj).ident.equals(nameThis)
		)
			ctx_method.addViolatedField(var);

		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RContainerAccessExpr of ContainerAccessExpr extends RLvalueExpr {
	public ENode		obj;
	public ENode		index;

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		obj.resolve(null);
		index.resolve(null);
		// Resolve overloaded access method
		Method@ m;
		CallType mt = new CallType(obj.getType(),null,new Type[]{index.getType()},Type.tpAny,false);
		ResInfo info = new ResInfo((ASTNode)this,nameArrayGetOp,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
		if( !PassInfo.resolveBestMethodR(obj.getType(),m,info,mt) )
			throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayGetOp,mt));
		if !(m.isMacro() && m.isNative()) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(reqType, new CallExpr(pos,null,m,new ENode[]{~obj,~index}));
			else
				replaceWithNodeResolve(reqType, new CallExpr(pos,~obj,m,new ENode[]{~index}));
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RThisExpr of ThisExpr extends RLvalueExpr {

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		ANode p = parent();
		while !(p instanceof Method || p instanceof Initializer || p instanceof Field)
			p = p.parent();
		DNode decl = (DNode)p;
		if (decl.isStatic() && ctx_tdecl.sname != nameIFaceImpl)
			Kiev.reportError(this,"Access '"+parent()+"' in static context");
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RSuperExpr of SuperExpr extends RENode {

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		ANode p = parent();
		while !(p instanceof Method || p instanceof Initializer || p instanceof Field)
			p = p.parent();
		DNode decl = (DNode)p;
		if (decl.isStatic() && ctx_tdecl.sname != nameIFaceImpl)
			Kiev.reportError(this,"Access '"+parent()+"' in static context");
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public final view RLVarExpr of LVarExpr extends RLvalueExpr {

	static final String namePEnv = "$env";

	public:ro	Var			var;

	public Var getVar();

	public boolean preGenerate() {
		if (getVar().kind == Var.VAR_RULE) {
			RuleMethod rm = (RuleMethod)ctx_method;
			assert(rm.params[0].type ≡ Type.tpRule);
			Var pEnv = null;
		lookup_penv:
			foreach (ASTNode dn; rm.block.stats) {
				if (dn instanceof DeclGroup) {
					foreach (Var vd; dn.getDecls(); vd.sname == namePEnv) {
						pEnv = vd;
						break lookup_penv;
					}
				}
				else if (dn instanceof Var) {
					Var vd = (Var)dn;
					if (vd.sname == namePEnv) {
						pEnv = vd;
						break lookup_penv;
					}
				}
			}
			if (pEnv == null) {
				Kiev.reportError(this, "Cannot find "+namePEnv);
				return false;
			}
			assert(pEnv.type.isInstanceOf(Type.tpRule));
			Struct s = (Struct)rm.block.stats[0];
			Field f = s.resolveField(this.ident);
			replaceWithNode(new IFldExpr(pos, new LVarExpr(pos, pEnv), f));
		}
		return true;
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		// Check if we try to access this var from local inner/anonymouse class
		if( ctx_tdecl.isLocal() ) {
			if( getVar().ctx_tdecl != this.ctx_tdecl ) {
				var.setNeedProxy(true);
				setAsField(true);
				// Now we need to add this var as a fields to
				// local class and to initializer of this class
				Field vf;
				if( (vf = ctx_tdecl.resolveField(this.ident,false)) == null ) {
					// Add field
					vf = (Field)ctx_tdecl.members.add(new Field(this.ident,var.getType(),ACC_PUBLIC));
					vf.setNeedProxy(true);
					vf.init = ((ENode)this).ncopy();
				}
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RSFldExpr of SFldExpr extends RLvalueExpr {
	public		TypeRef		obj;
	public:ro	Field		var;

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		// Set violation of the field
		if( ctx_method != null )
			ctx_method.addViolatedField(var);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view ROuterThisAccessExpr of OuterThisAccessExpr extends RENode {
	public		TypeRef			outer;
	public:ro	Var[]			outer_refs;

	public void setupOuterFields();
	
	public void resolve(Type reqType) throws RuntimeException {
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
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RReinterpExpr of ReinterpExpr extends RLvalueExpr {
	public TypeRef		type;
	public ENode		expr;

	public void resolve(Type reqType) {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving "+this);
		expr.resolve(null);
		Type type = this.getType();
		Type extp = expr.getType();
		if (type ≈ extp) {
			replaceWithNodeResolve(reqType,~expr);
			return;
		}
		if (type.isIntegerInCode() && extp.isIntegerInCode())
			;
		else if (extp.isInstanceOf(type))
			;
		else if (extp.getErasedType().isInstanceOf(type.getErasedType()))
			;
		else if (type instanceof CTimeType && type.getEnclosedType() ≈ extp)
			;
		else if (extp instanceof CTimeType && extp.getEnclosedType() ≈ type)
			;
		else
			Kiev.reportError(this, "Cannot reinterpret "+extp+" as "+type);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

