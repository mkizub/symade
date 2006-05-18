package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.Operator.*;
import kiev.vlang.types.*;
import kiev.transf.*;

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

import static kiev.stdlib.Debug.*;
import static kiev.be.java15.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class NewExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewExpr;
	@virtual typedef VView = VNewExpr;
	@virtual typedef JView = JNewExpr;
	@virtual typedef RView = RNewExpr;

	@att public TypeRef				type;
	@att public NArr<ENode>			args;
	@att public ENode				outer;
	@att public Struct				clazz; // if this new expression defines new class
	@ref public Method				func;

	@nodeview
	public static final view VNewExpr of NewExpr extends VENode {
		public		TypeRef				type;
		public:ro	NArr<ENode>			args;
		public		ENode				outer;
		public		Struct				clazz;
		public		Method				func;

		public boolean preResolveIn() {
			if( clazz == null )
				return true;
			Type tp = type.getType();
			tp.checkResolved();
			// Local anonymouse class
			CompaundType sup  = (CompaundType)tp;
			clazz.setResolved(true);
			clazz.setLocal(true);
			clazz.setAnonymouse(true);
			clazz.setStatic(ctx_method==null || ctx_method.isStatic());
			clazz.super_types.delAll();
			TypeRef sup_tr = this.type.ncopy();
			if( sup.clazz.isInterface() ) {
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
						init.params.append(new FormPar(pos,"arg$"+i,args[i].getType(),FormPar.PARAM_LVAR_PROXY,ACC_FINAL|ACC_SYNTHETIC));
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
		if (outer == null && type.getStruct() != null && type.getStruct().ometa_type != null) {
			if (ctx_method != null || !ctx_method.isStatic())
				outer = new ThisExpr(pos);
		}
		if (outer == null)
			return type;
		TVarBld vset = new TVarBld(
			type.getStruct().ometa_type.tdef.getAType(),
			new OuterType(type.getStruct(),outer.getType()) );
		return type.rebind(vset);
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

	public Dumper toJava(Dumper dmp) {
		Type tp = type.getType();
		if( !tp.isReference() ) {
			return dmp.append('0');
		}
		if( !tp.getStruct().isAnonymouse() ) {
			dmp.append("new ").append(tp).append('(');
		} else {
			dmp.append("new ").append(tp.getStruct().super_types[0].getStruct().qname()).append('(');
		}
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		dmp.append(')');
		if( tp.getStruct().isAnonymouse() ) {
			Struct cl = tp.getStruct();
			dmp.space().append('{').newLine(1);
			foreach (DNode n; cl.members)
				n.toJavaDecl(dmp).newLine();
			dmp.newLine(-1).append('}').newLine();
		}
		return dmp;
	}
}

@node
public final class NewArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewArrayExpr;
	@virtual typedef VView = VNewArrayExpr;
	@virtual typedef JView = JNewArrayExpr;
	@virtual typedef RView = RNewArrayExpr;

	@att public TypeRef				type;
	@att public NArr<ENode>			args;
	     public ArrayType			arrtype;

	@nodeview
	public static final view VNewArrayExpr of NewArrayExpr extends VENode {
		public		TypeRef				type;
		public:ro	NArr<ENode>			args;
		public		ArrayType			arrtype;
	}

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
		for(int i=1; i < args.size(); i++) art = new ArrayType(art);
		this.arrtype = art;
		return art;
	}

	public int getPriority() { return Constants.opAccessPriority; }

	public Type getType() { return arrtype; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(type.toString());
		for(int i=0; i < args.size(); i++) {
			sb.append('[');
			ENode arg = args[i];
			sb.append(arg.toString());
			sb.append(']');
		}
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("new ").append(type);
		for(int i=0; i < args.size(); i++) {
			dmp.append('[');
			ENode arg = args[i];
			arg.toJava(dmp);
			dmp.append(']');
		}
		return dmp;
	}
}

@node
public final class NewInitializedArrayExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = NewInitializedArrayExpr;
	@virtual typedef VView = VNewInitializedArrayExpr;
	@virtual typedef JView = JNewInitializedArrayExpr;
	@virtual typedef RView = RNewInitializedArrayExpr;

	@att public TypeRef				type;
	@att public NArr<ENode>			args;
	@att public int[]				dims;
	@ref public ArrayType			arrtype;

	@nodeview
	public static final view VNewInitializedArrayExpr of NewInitializedArrayExpr extends VENode {
		public		TypeRef				type;
		public:ro	NArr<ENode>			args;
		public		int[]				dims;
		public		ArrayType			arrtype;
		
		@getter public final int	get$dim();

		@getter public final Type	get$arrtype();
	}

	public NewInitializedArrayExpr() {}

	public NewInitializedArrayExpr(int pos, TypeRef type, int dim, ENode[] args) {
		this.pos = pos;
		this.type = type;
		dims = new int[dim];
		dims[0] = args.length;
		this.args.addAll(args);
	}
		
	@getter public final int	get$dim()	{ return this.dims.length; }

	@getter
	public ArrayType get$arrtype() {
		ArrayType art = ((NewInitializedArrayExpr)this).arrtype;
		if (art != null)
			return art;
		art = new ArrayType(type.getType());
		for(int i=1; i < dim; i++) art = new ArrayType(art);
		((NewInitializedArrayExpr)this).arrtype = art;
		return art;
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Type getType() { return arrtype; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(type.toString());
		for(int i=0; i < dim; i++) sb.append("[]");
		sb.append('{');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]+",");
		}
		sb.append('}');
		return sb.toString();
	}

	public int getElementsNumber(int i) { return dims[i]; }

	public Dumper toJava(Dumper dmp) {
		dmp.append("new ").append(arrtype);
		dmp.append('{');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 ) dmp.append(',').space();
		}
		dmp.append('}');
		return dmp;
	}
}

@node
public final class NewClosure extends ENode implements ScopeOfNames {
	
	@dflow(out="this:in") private static class DFI {
	@dflow(in="this:in")	ENode		body;
	}


	@virtual typedef This  = NewClosure;
	@virtual typedef VView = VNewClosure;
	@virtual typedef JView = JNewClosure;
	@virtual typedef RView = RNewClosure;

	@att public TypeRef				type_ret;
	@att public NArr<FormPar>		params;
	@att public ENode				body;
	@att public Struct				clazz;
	@ref public CallType			xtype;

	@nodeview
	public static final view VNewClosure of NewClosure extends VENode {
		public TypeRef			type_ret;
		public NArr<FormPar>	params;
		public ENode			body;
		public Struct			clazz;
		public CallType			xtype;
	}

	public NewClosure() {}

	public NewClosure(int pos) {
		this.pos = pos;
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Type getType() {
		if (xtype != null)
			return xtype;
		Vector<Type> args = new Vector<Type>();
		foreach (FormPar fp; params)
			args.append(fp.getType());
		xtype = new CallType(args.toArray(), type_ret.getType(), true);
		return xtype;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("fun (");
		for (int i=0; i < params.length; i++) {
			if (i > 0) sb.append(",");
			sb.append(params[i].vtype).append(' ').append(params[i].id);
		}
		sb.append(")->").append(type_ret).append(" {...}");
		return sb.toString();
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, String name)
		Var@ p;
	{
		p @= params,
		p.id.equals(name),
		node ?= p
	}
	
	public Dumper toJava(Dumper dmp) {
		CallType type = (CallType)this.getType();
		Struct cl = clazz;
		dmp.append("new ").append(cl.super_types[0].getStruct().qname()).append('(')
			.append(String.valueOf(type.arity)).append(')');
		dmp.space().append('{').newLine(1);
		foreach (DNode n; cl.members)
			n.toJavaDecl(dmp).newLine();
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

