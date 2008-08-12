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
package kiev.vlang;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ThisIsANode(name="New", lang=CoreLang)
public final class NewExpr extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in")				ENode		outer;
	@DataFlowDefinition(in="outer")					TypeRef		ntype;
	@DataFlowDefinition(in="ntype")					ENode		tpinfo;
	@DataFlowDefinition(in="tpinfo", seq="true")	ENode[]		args;
	}

	@nodeAttr				public TypeRef				ntype;
	@nodeAttr				public ENode				outer;
	@nodeAttr(ext_data=true)public ENode				tpinfo;
	@nodeAttr				public ENode∅				args;
	@nodeAttr				public Struct				clazz; // if this new expression defines new class

	@virtual @abstract
	public Method		func;

	@getter public Method get$func() {
		DNode sym = this.dnode;
		if (sym instanceof Method)
			return (Method)sym;
		return null;
	}
	@setter public void set$func(Method m) {
		this.symbol = m;
	}

	public NewExpr() {}

	public NewExpr(int pos, Type ntype, ENode[] args) {
		this.pos = pos;
		this.ntype = new TypeRef(ntype);
		foreach (ENode e; args) this.args.append(e);
	}

	public NewExpr(int pos, TypeRef ntype, ENode[] args) {
		this.pos = pos;
		this.ntype = ntype;
		foreach (ENode e; args) this.args.append(e);
	}

	public NewExpr(int pos, Type ntype, ENode[] args, ENode outer) {
		this(pos,ntype,args);
		this.outer = outer;
	}

	public NewExpr(int pos, TypeRef ntype, ENode[] args, ENode outer) {
		this(pos,ntype,args);
		this.outer = outer;
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Type getType() {
		if (this.clazz != null)
			return this.clazz.xtype;
		Type ntype = this.ntype.getType();
		Struct clazz = ntype.getStruct();
		if (outer == null && clazz != null && clazz.ometa_tdef != null) {
			if (ctx_method != null || !ctx_method.isStatic())
				outer = new ThisExpr(pos);
		}
		if (outer == null)
			return ntype;
		TVarBld vset = new TVarBld(clazz.ometa_tdef.getAType(), outer.getType());
		return ntype.rebind(vset);
	}

	public boolean preResolveIn() {
		if( clazz == null )
			return true;
		Type tp = ntype.getType();
		tp.checkResolved();
		// Local anonymouse class
		CompaundType sup  = (CompaundType)tp;
		clazz.setStatic(ctx_method==null || ctx_method.isStatic());
		clazz.super_types.delAll();
		TypeRef sup_tr = this.ntype.ncopy();
		if( sup.tdecl.isInterface() ) {
			clazz.super_types.insert(0, new TypeRef(Type.tpObject));
			clazz.super_types.add(sup_tr);
		} else {
			clazz.super_types.insert(0, sup_tr);
		}

		{
			// Create default initializer, if number of arguments > 0
			if( args.length > 0 ) {
				Constructor init = new Constructor(ACC_PUBLIC);
				for(int i=0; i < args.length; i++) {
					args[i].resolve(null);
					init.params.append(new LVar(pos,"arg$"+i,args[i].getType(),Var.PARAM_LVAR_PROXY,ACC_FINAL|ACC_SYNTHETIC));
				}
				init.pos = pos;
				init.body = new Block(pos);
				init.setPublic();
				clazz.addMethod(init);
			}
		}

		// Process inner classes and cases
		Kiev.runProcessorsOn(clazz);
		return true;
	}
	
	public void mainResolveOut() {
		if (ntype instanceof MacroSubstTypeRef)
			return;
		Type ntype;
		if (this.clazz != null)
			ntype = this.clazz.xtype;
		else
			ntype = this.ntype.getType();
		Struct s = ntype.getStruct();
		if (s == null) {
			Kiev.reportError(this,"Instantiation of non-concrete type "+this.ntype+" ???");
			return;
		}
		if (s.isEnum()) {
			Kiev.reportError(this,"Forbidden enum value instantiation");
			return;
		}
		if (outer == null && s.isStructInner() && !s.isStatic() && s.ometa_tdef != null) {
			if (ctx_method==null || ctx_method.isStatic()) {
				Kiev.reportError(this,"'new' for inner class requares outer instance specification");
				return;
			}
			outer = new ThisExpr(pos);
			outer.setAutoGenerated(true);
		}
		if (outer != null) {
			outer.resolve(null);
			ntype = ntype.rebind(new TVarBld(s.ometa_tdef.getAType(), outer.getType()));
		}
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		{
			CallType mt = (CallType)new CallType(null,null,ta,ntype,false);
			// First try overloaded 'new', than real 'new'
			if( this.clazz == null && (ctx_method==null || !ctx_method.hasName(nameNewOp)) ) {
				ResInfo<Method> info = new ResInfo<Method>(this,nameNewOp,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
				if (PassInfo.resolveBestMethodR(ntype,info,mt)) {
					CallExpr n = new CallExpr(pos,new TypeRef(ntype),info.resolvedSymbol(),((NewExpr)this).args.delToArray());
					replaceWithNodeReWalk(n);
					return;
				}
			}
		}
		// try to find a constructor
		{
			CallType mt = (CallType)new CallType(ntype,null,ta,Type.tpVoid,false);
			ResInfo<Constructor> info = new ResInfo<Constructor>(this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noStatic);
			if( PassInfo.resolveBestMethodR(ntype,info,mt) ) {
				this.symbol = info.resolvedSymbol();
				return;
			}
		}
		// try to bind to a class with no constructors
		if (args.length == 0) {
			boolean ok = true;
			foreach(Constructor n; s.members; !n.isStatic())
				ok = false;
			if (ok)
				return;
		}
		Kiev.reportWarning(this,"Can't find apropriative initializer for "+
			Method.toString("<constructor>",args,Type.tpVoid)+" for "+ntype);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(ntype).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
}

@ThisIsANode(name="NewEnum", lang=CoreLang)
public final class NewEnumExpr extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]		args;
	}

	@nodeAttr public ENode∅				args;

	@virtual @abstract
	public Method		func;

	@getter public Method get$func() {
		DNode sym = this.dnode;
		if (sym instanceof Method)
			return (Method)sym;
		return null;
	}
	@setter public void set$func(Method m) {
		this.symbol = m;
	}

	public NewEnumExpr() {}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Type getType() {
		return ctx_tdecl.xtype;
	}

	public void mainResolveOut() {
		Type ntype = this.getType();
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		CallType mt = (CallType)Type.getRealType(ntype,new CallType(ntype,null,ta,Type.tpVoid,false));
		ResInfo<Constructor> info = new ResInfo<Constructor>(this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noStatic);
		if( PassInfo.resolveBestMethodR(ntype,info,mt) ) {
			this.symbol = info.resolvedSymbol();
			return;
		}
		// try to bind to a class with no constructors
		if (args.length == 0) {
			boolean ok = true;
			foreach(Constructor n; ntype.meta_type.tdecl.getMembers(); !n.isStatic())
				ok = false;
			if (ok)
				return;
		}
		Kiev.reportWarning(this,"Can't find apropriative initializer for "+Method.toString("<constructor>",args,Type.tpVoid)+" for "+ntype);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
}

@ThisIsANode(name="NewArr", lang=CoreLang)
public final class NewArrayExpr extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]		args;
	}

	@nodeAttr public TypeRef				ntype;
	@nodeAttr public ENode∅				args;
	          public ArrayType				arrtype;

	public NewArrayExpr() {}

	public NewArrayExpr(int pos, TypeRef ntype, ENode[] args) {
		this.pos = pos;
		this.ntype = ntype;
		foreach (ENode e; args) this.args.append(e);
	}

	@getter
	public ArrayType get$arrtype() {
		ArrayType art = this.arrtype;
		if (art != null)
			return art;
		art = new ArrayType(ntype.getType());
		for(int i=1; i < args.length; i++) art = new ArrayType(art);
		this.arrtype = art;
		return art;
	}

	public int getPriority() { return Constants.opAccessPriority; }

	public Type getType() { return arrtype; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(ntype.toString());
		for(int i=0; i < args.length; i++) {
			sb.append('[');
			ENode arg = args[i];
			sb.append(arg.toString());
			sb.append(']');
		}
		return sb.toString();
	}
}

@ThisIsANode(name="NewArrInitialized", lang=CoreLang)
public final class NewInitializedArrayExpr extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]		args;
	}

	@nodeAttr public TypeExpr			ntype;
	@nodeAttr public ENode∅			args;

	public NewInitializedArrayExpr() {}

	public NewInitializedArrayExpr(int pos, TypeExpr ntype, ENode[] args) {
		this.pos = pos;
		this.ntype = ntype;
		if (args != null)
			this.args.addAll(args);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Type getType() { return ntype.getType(); }
	
	public void setType(ArrayType reqType) {
		assert (this.ntype == null);
		Type art = reqType;
		int dim = 0;
		while (art instanceof ArrayType) { dim++; art = art.arg; }
		TypeRef tp = new TypeRef(art);
		for (int i=0; i < dim; i++)
			tp = new TypeExpr(tp, Operator.PostTypeArray);
		this.ntype = (TypeExpr)tp;

		foreach (NewInitializedArrayExpr arg; args; arg.ntype == null) {
			Type tp = reqType.arg;
			if!(tp instanceof ArrayType)
				Kiev.reportError(this,"Wrong dimension of array initializer");
			else
				arg.setType((ArrayType)tp);
		}
	}

	public boolean preResolveIn() {
		if (ntype == null)
			return true;
		Type tp = getType();
		if!(tp instanceof ArrayType)
			throw new CompilerException(this,"Wrong dimension of array initializer");
		tp = ((ArrayType)tp).arg;
		foreach (NewInitializedArrayExpr arg; args; arg.ntype == null) {
			if!(tp instanceof ArrayType)
				Kiev.reportError(this,"Wrong dimension of array initializer");
			else
				arg.setType((ArrayType)tp);
		}
		return true;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(ntype);
		sb.append('{');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]+",");
		}
		sb.append('}');
		return sb.toString();
	}
}

@ThisIsANode(name="NewClosure", lang=CoreLang)
public final class NewClosure extends ENode implements ScopeOfNames {
	
	@DataFlowDefinition(out="this:in") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		body;
	}


	@nodeAttr public TypeRef			type_ret;
	@nodeAttr public Var∅				params;
	@nodeAttr public ENode				body;
	@nodeAttr public Struct				clazz;
	@nodeData public CallType			xtype;

	public NewClosure() {}

	public NewClosure(int pos) {
		this.pos = pos;
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Type getType() {
		if (xtype != null)
			return xtype;
		Vector<Type> args = new Vector<Type>();
		foreach (Var fp; params)
			args.append(fp.getType());
		xtype = new CallType(null, null, args.toArray(), type_ret.getType(), true);
		return xtype;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("fun (");
		for (int i=0; i < params.length; i++) {
			if (i > 0) sb.append(",");
			sb.append(params[i].vtype).append(' ').append(params[i].sname);
		}
		sb.append(")->").append(type_ret).append(" {...}");
		return sb.toString();
	}

	public rule resolveNameR(ResInfo path)
	{
		path @= params
	}
}

