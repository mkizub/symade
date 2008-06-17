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
	@DataFlowDefinition(in="this:in")			ENode		outer;
	@DataFlowDefinition(in="outer")				TypeRef		type;
	@DataFlowDefinition(in="type")				ENode		tpinfo;
	@DataFlowDefinition(in="tpinfo", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewExpr;

	@nodeAttr				public TypeRef				type;
	@nodeAttr				public ENode				outer;
	@nodeAttr(ext_data=true)	public ENode				tpinfo;
	@nodeAttr				public ENode[]				args;
	@nodeAttr				public Struct				clazz; // if this new expression defines new class

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

	public NewExpr(int pos, Type type, ENode[] args) {
		this.pos = pos;
		this.type = new TypeRef(type);
		foreach (ENode e; args) this.args.append(e);
	}

	public NewExpr(int pos, TypeRef type, ENode[] args) {
		this.pos = pos;
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

	public int		getPriority() { return Constants.opAccessPriority; }

	public Type getType() {
		if (this.clazz != null)
			return this.clazz.xtype;
		Type type = this.type.getType();
		Struct clazz = type.getStruct();
		if (outer == null && type.getStruct() != null && type.getStruct().ometa_tdef != null) {
			if (ctx_method != null || !ctx_method.isStatic())
				outer = new ThisExpr(pos);
		}
		if (outer == null)
			return type;
		TVarBld vset = new TVarBld(
			type.getStruct().ometa_tdef.getAType(),
			outer.getType() );
		return type.rebind(vset);
	}

	public boolean preResolveIn() {
		if( clazz == null )
			return true;
		Type tp = type.getType();
		tp.checkResolved();
		// Local anonymouse class
		CompaundType sup  = (CompaundType)tp;
		clazz.setLocal(true);
		clazz.setAnonymouse(true);
		clazz.setStatic(ctx_method==null || ctx_method.isStatic());
		clazz.super_types.delAll();
		TypeRef sup_tr = this.type.ncopy();
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
		if (type instanceof MacroSubstTypeRef)
			return;
		Type type;
		if (this.clazz != null)
			type = this.clazz.xtype;
		else
			type = this.type.getType();
		Struct s = type.getStruct();
		if (s == null) {
			Kiev.reportError(this,"Instantiation of non-concrete type "+this.type+" ???");
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
			type = type.rebind(new TVarBld(s.ometa_tdef.getAType(), outer.getType()));
		}
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		{
			CallType mt = (CallType)new CallType(null,null,ta,type,false);
			Method@ m;
			// First try overloaded 'new', than real 'new'
			if( this.clazz == null && (ctx_method==null || !ctx_method.hasName(nameNewOp)) ) {
				ResInfo info = new ResInfo(this,nameNewOp,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports);
				if (PassInfo.resolveBestMethodR(type,m,info,mt)) {
					CallExpr n = new CallExpr(pos,new TypeRef(type),(Method)m,((NewExpr)this).args.delToArray());
					replaceWithNodeReWalk(n);
					return;
				}
			}
		}
		// try to find a constructor
		{
			CallType mt = (CallType)new CallType(type,null,ta,Type.tpVoid,false);
			Constructor@ c;
			ResInfo info = new ResInfo(this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports|ResInfo.noStatic);
			if( PassInfo.resolveBestMethodR(type,c,info,mt) ) {
				this.symbol = c;
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
			Method.toString("<constructor>",args,Type.tpVoid)+" for "+type);
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
}

@ThisIsANode(name="NewEnum", lang=CoreLang)
public final class NewEnumExpr extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewEnumExpr;

	@nodeAttr public ENode[]				args;

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
		Type type = this.getType();
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		CallType mt = (CallType)Type.getRealType(type,new CallType(type,null,ta,Type.tpVoid,false));
		Constructor@ c;
		ResInfo info = new ResInfo(this,null,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports|ResInfo.noStatic);
		if( PassInfo.resolveBestMethodR(type,c,info,mt) ) {
			this.symbol = c;
			return;
		}
		// try to bind to a class with no constructors
		if (args.length == 0) {
			boolean ok = true;
			foreach(Constructor n; type.meta_type.tdecl.getMembers(); !n.isStatic())
				ok = false;
			if (ok)
				return;
		}
		Kiev.reportWarning(this,"Can't find apropriative initializer for "+Method.toString("<constructor>",args,Type.tpVoid)+" for "+type);
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

	@virtual typedef This  = NewArrayExpr;

	@nodeAttr public TypeRef				type;
	@nodeAttr public ENode[]				args;
	     public ArrayType			arrtype;

	public NewArrayExpr() {}

	public NewArrayExpr(int pos, TypeRef type, ENode[] args) {
		this.pos = pos;
		this.type = type;
		foreach (ENode e; args) this.args.append(e);
	}

	@getter
	public ArrayType get$arrtype() {
		ArrayType art = this.arrtype;
		if (art != null)
			return art;
		art = new ArrayType(type.getType());
		for(int i=1; i < args.length; i++) art = new ArrayType(art);
		this.arrtype = art;
		return art;
	}

	public int getPriority() { return Constants.opAccessPriority; }

	public Type getType() { return arrtype; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(type.toString());
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

	@virtual typedef This  = NewInitializedArrayExpr;

	@nodeAttr public TypeExpr			type;
	@nodeAttr public ENode[]			args;

	public NewInitializedArrayExpr() {}

	public NewInitializedArrayExpr(int pos, TypeExpr type, ENode[] args) {
		this.pos = pos;
		this.type = type;
		if (args != null)
			this.args.addAll(args);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Type getType() { return type.getType(); }
	
	public void setType(ArrayType reqType) {
		assert (this.type == null);
		Type art = reqType;
		int dim = 0;
		while (art instanceof ArrayType) { dim++; art = art.arg; }
		TypeRef tp = new TypeRef(art);
		for (int i=0; i < dim; i++)
			tp = new TypeExpr(tp, Operator.PostTypeArray);
		this.type = (TypeExpr)tp;

		foreach (NewInitializedArrayExpr arg; args; arg.type == null) {
			Type tp = reqType.arg;
			if!(tp instanceof ArrayType)
				Kiev.reportError(this,"Wrong dimension of array initializer");
			else
				arg.setType((ArrayType)tp);
		}
	}

	public boolean preResolveIn() {
		if (type == null)
			return true;
		Type tp = getType();
		if!(tp instanceof ArrayType)
			throw new CompilerException(this,"Wrong dimension of array initializer");
		tp = ((ArrayType)tp).arg;
		foreach (NewInitializedArrayExpr arg; args; arg.type == null) {
			if!(tp instanceof ArrayType)
				Kiev.reportError(this,"Wrong dimension of array initializer");
			else
				arg.setType((ArrayType)tp);
		}
		return true;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(type);
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


	@virtual typedef This  = NewClosure;

	@nodeAttr public TypeRef				type_ret;
	@nodeAttr public Var[]				params;
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

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		Var@ p;
	{
		p @= params,
		path.checkNodeName(p),
		node ?= p
	}
}

