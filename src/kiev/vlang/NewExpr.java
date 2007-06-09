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

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RNewExpr;
import kiev.be.java15.JNewExpr;
import kiev.ir.java15.RNewArrayExpr;
import kiev.be.java15.JNewArrayExpr;
import kiev.ir.java15.RNewInitializedArrayExpr;
import kiev.be.java15.JNewInitializedArrayExpr;
import kiev.be.java15.JNewClosure;
import kiev.ir.java15.RNewClosure;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="New")
public final class NewExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")			ENode		outer;
	@dflow(in="outer")				TypeRef		type;
	@dflow(in="type")				ENode		tpinfo;
	@dflow(in="tpinfo", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewExpr;
	@virtual typedef JView = JNewExpr;
	@virtual typedef RView = RNewExpr;

	@att				public TypeRef				type;
	@att				public ENode				outer;
	@att(ext_data=true)	public ENode				tpinfo;
	@att				public ENode[]				args;
	@att				public Struct				clazz; // if this new expression defines new class

	@getter public Method get$func() {
		DNode sym = this.dnode;
		if (sym instanceof Method)
			return (Method)sym;
		return null;
	}
	@setter public void set$func(Method m) {
		this = this.open();
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

@node(name="NewArr")
public final class NewArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewArrayExpr;
	@virtual typedef JView = JNewArrayExpr;
	@virtual typedef RView = RNewArrayExpr;

	@att public TypeRef				type;
	@att public ENode[]				args;
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

@node(name="NewArrInitialized")
public final class NewInitializedArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewInitializedArrayExpr;
	@virtual typedef JView = JNewInitializedArrayExpr;
	@virtual typedef RView = RNewInitializedArrayExpr;

	@att public TypeExpr			type;
	@att public ENode[]				args;
	@ref public int[]				dims;

	public NewInitializedArrayExpr() {}

	public NewInitializedArrayExpr(int pos, TypeExpr type, int dim, ENode[] args) {
		this.pos = pos;
		this.type = type;
		this.dims = new int[dim];
		if (args != null) {
			this.dims[0] = args.length;
			this.args.addAll(args);
		}
	}

	@getter public final int	get$dim()	{ return this.dims.length; }

	public int		getPriority() { return Constants.opAccessPriority; }

	public Type getType() { return type.getType(); }

	public void setType(ArrayType reqType) {
		assert (this.type == null);
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

	public int getElementsNumber(int i) { return dims[i]; }
}

@node(name="NewClosure")
public final class NewClosure extends ENode implements ScopeOfNames {
	
	@dflow(out="this:in") private static class DFI {
	@dflow(in="this:in")	ENode		body;
	}


	@virtual typedef This  = NewClosure;
	@virtual typedef JView = JNewClosure;
	@virtual typedef RView = RNewClosure;

	@att public TypeRef				type_ret;
	@att public Var[]				params;
	@att public ENode				body;
	@att public Struct				clazz;
	@ref public CallType			xtype;

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

