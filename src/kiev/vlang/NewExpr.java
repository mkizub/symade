/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.Instr.*;
import kiev.vlang.Operator.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Instr.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/NewExpr.java,v 1.6.2.1.2.1 1999/02/15 21:45:13 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.1 $
 *
 */

public class NewExpr extends Expr {

	public Type		type;
	public Expr[]	args;
	public Expr		outer;
	public Expr		tif_expr;	// TypeInfo field access expression

	public Method	func;

	public NewExpr(int pos, Type type, Expr[] args) {
		super(pos);
		this.type = type;
		this.args = args;
	}

	public NewExpr(int pos, Type type, Expr[] args, Expr outer) {
		this(pos,type,args);
		this.outer = outer;
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

	public Type getType() {
		return type;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			if( type.clazz.isAnonymouse() ) {
				type.clazz.resolve(null);
			}
			if( !type.clazz.isArgument() && (type.clazz.isAbstract() || !type.clazz.isClazz()) ) {
				throw new CompilerException(pos,"Abstract class "+type+" instantiation");
			}
			if( outer != null ) outer = (Expr)outer.resolve(null);
			else if( (!type.clazz.isStatic() && type.clazz.isLocal())
				  || (!type.clazz.isStatic() && !((Struct)type.clazz.package_clazz).isPackage()) ) {
				if( PassInfo.method==null || PassInfo.method.isStatic() )
					throw new CompilerException(pos,"'new' for inner class requares outer instance specification");
				Var th = PassInfo.method.params[0];
				outer = (Expr)new VarAccessExpr(pos,this,th).resolve(null);
			}
			for(int i=0; i < args.length; i++)
				args[i] = (Expr)args[i].resolve(null);
			Expr[] outer_args;
			if( outer != null ) {
				outer_args = new Expr[args.length+1];
				outer_args[0] = outer;
				for(int i=0; i < args.length; i++) outer_args[i+1] = args[i];
			} else {
				outer_args = args;
			}
			if( type.clazz.isLocal() ) {
				Struct cl = type.clazz;
				for(int i=0; i < cl.fields.length; i++) {
					if( !cl.fields[i].isNeedProxy() ) continue;
					outer_args = (Expr[])Arrays.append(outer_args,new FieldAccessExpr(pos,cl.fields[i]));
				}
			}
			if( !Kiev.kaffe && type.args.length > 0 ) {
				// Create static field for this type typeinfo
				tif_expr = PassInfo.clazz.accessTypeInfoField(pos,this,type);
				args = (Expr[])Arrays.insert(args,tif_expr,0);
				outer_args = (Expr[])Arrays.insert(outer_args,tif_expr,(outer!=null?1:0));
			}
			// Don't try to find constructor of argument type
			if( !type.clazz.isArgument() ) {
				PVar<Method> m = new PVar<Method>();
				// First try overloaded 'new', than real 'new'
				if( (PassInfo.method==null || !PassInfo.method.name.equals(nameNewOp))
				 &&	type.clazz.resolveMethodR(m,null,nameNewOp,outer_args,
			 		type,type,ResolveFlags.NoForwards | ResolveFlags.NoSuper)
				) {
				 	ASTNode n = new CallExpr(pos,parent,(Method)m,m.makeArgs(args,type));
					n.type_of_static = type;
				 	n.setResolved(true);
				 	return n;
				}
				else if( !type.clazz.resolveMethodR(m,null,nameInit,outer_args,
					Type.tpVoid,type,ResolveFlags.NoForwards | ResolveFlags.NoSuper)
				) {
					throw new RuntimeException("Can't find apropriative initializer for "
						+Method.toString(nameInit,outer_args,Type.tpVoid)+" for "+type);
				} else {
					func = m;
					outer_args = m.makeArgs(outer_args,type);
					int po = 0;
					int pa = 0;
					if( outer != null ) outer = outer_args[po++];
					while( pa < args.length ) args[pa++] = outer_args[po++];
				}
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewExpr: "+this);
		PassInfo.push(this);
		try {
			if( type.clazz.isArgument() ) {
				if( outer != null || args.length > 0 ) {
					Kiev.reportError(pos,"Constructor with arguments for type argument is not supported");
					return;
				} else {
					// If we have primitive type
					Type t = Type.getRealType(Kiev.argtype,type);
					Expr e;
					if( !t.isReference() ) {
						e = (Expr)new ConstExpr(pos,null).resolve(null);
						e.generate(t);
						return;
					}
					int i;
					for(i=0; i < PassInfo.clazz.type.args.length; i++)
						if( type.string_equals(PassInfo.clazz.type.args[i]) ) break;
					if( i >= PassInfo.clazz.type.args.length )
						throw new CompilerException(pos,"Can't create an instance of argument type "+type);
					Expr tie = new FieldAccessExpr(pos,PassInfo.clazz.resolveField(nameTypeInfo));
					e = new CastExpr(pos,type,
						new CallAccessExpr(pos,parent,tie,
							Type.tpTypeInfo.clazz.resolveMethod(
								KString.from("newInstance"),
								KString.from("(I)Ljava/lang/Object;")
								),
								new Expr[]{new ConstExpr(pos,Kiev.newInteger(i))}),
						true).resolveExpr(reqType);
					e.generate(t);
					return;
				}
			}
			Code.addInstr(op_new,Type.getRealType(Kiev.argtype,type));
			// First arg ('this' pointer) is generated by 'op_dup'
			if( reqType != Type.tpVoid )
				Code.addInstr(op_dup);
			// Constructor call args (first args 'this' skipped)
			if( outer != null )
				outer.generate(null);
			for(int i=0; i < args.length; i++)
				args[i].generate(null);
			// Now, fill proxyed fields (vars)
			if( type.clazz.isLocal() ) {
				Struct cl = type.clazz;
				for(int i=0; i < cl.fields.length; i++) {
					if( !cl.fields[i].isNeedProxy() ) continue;
					Var v = ((VarAccessExpr)cl.fields[i].init).var;
					Code.addInstr(Instr.op_load,v);
				}
			}
			if( Kiev.kaffe )
				Code.addInstr(op_call,func,false,type);
			else
				Code.addInstr(op_call,func,false,type);
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public void cleanup() {
		parent=null;
		type = null;
		foreach(ASTNode n; args; n!=null) n.cleanup();
		args = null;
		if( outer != null ) {
			outer.cleanup();
			outer = null;
		}
		tif_expr = null;
		func = null;
	}

	public Dumper toJava(Dumper dmp) {
		Type tp = Type.getRealType(Kiev.argtype,type);
		if( !tp.isReference() ) {
			return dmp.append('0');
		}
		if( !tp.clazz.isAnonymouse() ) {
			dmp.append("new ").append(tp).append('(');
		} else {
			if( tp.clazz.interfaces.length > 0 )
				dmp.append("new ").append(tp.clazz.interfaces[0].clazz.name).append('(');
			else
				dmp.append("new ").append(tp.clazz.super_clazz.clazz.name).append('(');
		}
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		dmp.append(')');
		if( tp.clazz.isAnonymouse() ) {
			Struct cl = type.clazz;
			dmp.space().append('{').newLine(1);
			if( cl.isClazz() ) {
				for(int i=0; cl.sub_clazz!=null && i < cl.sub_clazz.length; i++) {
					if( cl.sub_clazz[i].isLocal() ) continue;
					cl.sub_clazz[i].toJavaDecl(dmp).newLine();
				}
			}
			for(int i=0; cl.fields!=null && i < cl.fields.length; i++) {
				cl.fields[i].toJavaDecl(dmp).newLine();
			}
			for(int i=0; cl.methods!=null && i < cl.methods.length; i++) {
				cl.methods[i].toJavaDecl(dmp).newLine();
			}
			dmp.newLine(-1).append('}').newLine();
		}
		return dmp;
	}
}

public class NewArrayExpr extends Expr {

	public Type		type;
	public Expr[]	args;
	public int		dim;
	private Type	arrtype;
	private Expr	create_via_reflection;

	public NewArrayExpr(int pos, Type type, Expr[] args, int dim) {
		super(pos);
		this.type = type;
		this.args = args;
		this.dim = dim;
		arrtype = Type.newArrayType(type);
		for(int i=1; i < dim; i++) arrtype = Type.newArrayType(arrtype);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(type.toString());
		for(int i=0; i < dim; i++) {
			sb.append('[');
			if( i < args.length && args[i] != null ) sb.append(args[i].toString());
			sb.append(']');
		}
		return sb.toString();
	}

	public Type getType() { return arrtype; }

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			for(int i=0; i < args.length; i++)
				if( args[i] != null )
					args[i] = args[i].resolveExpr(Type.tpInt);
			if( !Kiev.kaffe && type.clazz.isArgument() ) {
				if( PassInfo.method==null || PassInfo.method.isStatic() )
					throw new CompilerException(pos,"Access to argument "+type+" from static method");
				int i;
				for(i=0; i < PassInfo.clazz.type.args.length; i++)
					if( type.string_equals(PassInfo.clazz.type.args[i]) ) break;
				if( i >= PassInfo.clazz.type.args.length )
					throw new CompilerException(pos,"Can't create an array of argument type "+type);
				Expr tie = new FieldAccessExpr(pos,PassInfo.clazz.resolveField(nameTypeInfo));
				if( dim == 1 ) {
					create_via_reflection = (Expr)new CastExpr(pos,arrtype,
						new CallAccessExpr(pos,parent,tie,
							Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),KString.from("(II)Ljava/lang/Object;")),
							new Expr[]{new ConstExpr(pos,Kiev.newInteger(i)),args[0]}
						),true).resolve(reqType);
				} else {
					create_via_reflection = (Expr)new CastExpr(pos,arrtype,
						new CallAccessExpr(pos,parent,tie,
							Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),KString.from("(I[I)Ljava/lang/Object;")),
							new Expr[]{
								new ConstExpr(pos,Kiev.newInteger(i)),
								new NewInitializedArrayExpr(pos,Type.tpInt,1,args)
							}
						),true).resolve(reqType);
				}
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewArrayExpr: "+this);
		PassInfo.push(this);
		try {
			if( create_via_reflection != null
			 && Type.getRealType(Kiev.argtype,type).isArgumented()
			) {
				create_via_reflection.generate(reqType);
			} else {
				if( dim == 1 ) {
					args[0].generate(null);
					Code.addInstr(Instr.op_newarray,Type.getRealType(Kiev.argtype,type));
				} else {
					for(int i=0; i < args.length; i++)
						args[i].generate(null);
					Code.addInstr(Instr.op_multianewarray,Type.getRealType(Kiev.argtype,arrtype),args.length);
				}
				if( reqType == Type.tpVoid ) Code.addInstr(Instr.op_pop);
			}
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public void cleanup() {
		parent=null;
		type = null;
		arrtype = null;
		foreach(ASTNode n; args; n!=null) n.cleanup();
		args = null;
	}

	public Dumper toJava(Dumper dmp) {
		if( create_via_reflection != null
		 && Type.getRealType(Kiev.argtype,type).isArgumented()
		) {
			create_via_reflection.toJava(dmp);
		} else {
			dmp.append("new ").append(Type.getRealType(Kiev.argtype,type));
			for(int i=0; i < dim; i++) {
				dmp.append('[');
				if( i < args.length && args[i] != null ) args[i].toJava(dmp);
				dmp.append(']');
			}
		}
		return dmp;
	}
}

public class NewInitializedArrayExpr extends Expr {

	public Type			type;
	public int			dim;
	public int[]		dims;
	public Expr[]		args;
	private Type		arrtype;

	public NewInitializedArrayExpr(int pos, Type type, int dim, Expr[] args) {
		super(pos);
		this.type = type;
		this.dim = dim;
		this.args = args;
		arrtype = Type.newArrayType(type);
		for(int i=1; i < dim; i++) arrtype = Type.newArrayType(arrtype);
		dims = new int[dim];
		dims[0] = args.length;
	}

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

	public Type getType() { return arrtype; }

	public int getElementsNumber(int i) { return dims[i]; }

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			for(int i=0; i < args.length; i++)
				args[i] = args[i].resolveExpr(arrtype.args[0]);
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
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewInitializedArrayExpr: "+this);
		PassInfo.push(this);
		try {
			if( dim == 1 ) {
				Code.addConst(args.length);
				Code.addInstr(Instr.op_newarray,type);
			} else {
				for(int i=0; i < dim; i++)
					Code.addConst(dims[i]);
				Code.addInstr(Instr.op_multianewarray,arrtype,dim);
			}
			for(int i=0; i < args.length; i++) {
				Code.addInstr(Instr.op_dup);
				Code.addConst(i);
				args[i].generate(null);
				Code.addInstr(Instr.op_arr_store);
			}
			if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public void cleanup() {
		parent=null;
		type = null;
		arrtype = null;
		foreach(ASTNode n; args; n!=null) n.cleanup();
		args = null;
	}

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

public class NewClosure extends Expr {

	public Type		type;
	public Expr[]	args = Expr.emptyArray;

	public Method	func;

	public NewClosure(int pos, Type type) {
		super(pos);
		this.type = type;
	}

	public NewClosure(int pos, Method func) {
		super(pos);
		this.func = func;
		this.type = MethodType.newMethodType(Type.tpClosureClazz,null,func.type.args,func.type.ret);
	}

	public NewClosure(int pos, Method func, Expr[] args) {
		super(pos);
		this.func = func;
		if(args.length==0)
			this.type = MethodType.newMethodType(Type.tpClosureClazz,func.type.fargs,func.type.args,func.type.ret);
		else {
			Type[] targs = new Type[func.type.args.length-args.length];
			for(int i=args.length, j=0; i < func.type.args.length; i++, j++)
				targs[j] = func.type.args[i];
			this.type = MethodType.newMethodType(Type.tpClosureClazz,func.type.fargs,targs,func.type.ret);
		}
		this.args = args;
		for(int i=0; i < args.length; i++) {
			args[i].parent = this;
		}
	}

	public String toString() {
		return "fun "+type;
	}

	public Type getType() { return type; }

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		if( Kiev.passLessThen(TopLevelPass.passResolveImports) ) return this;
		PassInfo.push(this);
		try {
			if( Kiev.kaffe ) {
				if( (PassInfo.method==null || PassInfo.method.isStatic()) && !func.isStatic())
					throw new CompilerException(pos,"Non-static method reference in static method");
				for(int i=0; i < args.length; i++)
					args[i] = args[i].resolveExpr(func.type.args[i]);
				boolean updated = false;
				for(int i=0; i < func.params.length; i++) {
					if( func.params[i].isClosureProxy() ) {
						KString name = func.params[i].name.name;
						PVar<ASTNode> v = new PVar<ASTNode>();
						if( !PassInfo.resolveNameR(v,null,name,null,0) ) {
							Kiev.reportError(pos,"Internal error: can't find var "+name);
						}
						Expr vae = new VarAccessExpr(pos,this,(Var)v)
							.resolveExpr(func.params[i].type);
						vae.setUseNoProxy(true);
						args = (Expr[])Arrays.insert(args,vae,func.isStatic()?i:i-1);
						updated = true;
					}
				}
				if(updated) {
					Type[] targs = new Type[func.type.args.length-args.length];
					for(int i=args.length, j=0; i < func.type.args.length; i++, j++)
						targs[j] = func.type.args[i];
					this.type = MethodType.newMethodType(Type.tpClosureClazz,func.type.fargs,targs,func.type.ret);
				}
			} else {
				if( Env.getStruct(Type.tpClosureClazz.name) == null )
					throw new RuntimeException("Core class "+Type.tpClosureClazz.name+" not found");
				type.clazz.autoProxyMethods();
				type.clazz.resolve(null);
				Struct cl = type.clazz;
				KStringBuffer sign = new KStringBuffer().append('(');
				if( PassInfo.method!=null && !PassInfo.method.isStatic() ) {
					sign.append(((Struct)PassInfo.method.parent).type.signature);
				}
				sign.append('I');
				for(int i=0; i < cl.fields.length; i++) {
					if( !cl.fields[i].isNeedProxy() ) continue;
					sign.append(cl.fields[i].type.signature);
				}
				sign.append(")V");
				func = type.clazz.resolveMethod(nameInit,sign.toKString());
				if( !((Struct)func.parent).equals(type.clazz) )
					throw new RuntimeException("Can't find apropriative initializer for "+nameInit+sign+" for "+type+" class "+type.clazz);
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewClosure: "+this);
		PassInfo.push(this);
		try {
			if( !Kiev.kaffe ) {
				Code.addInstr(op_new,type);
				// First arg ('this' pointer) is generated by 'op_dup'
				if( reqType != Type.tpVoid && !Kiev.kaffe
//				|| (
//			 		PassInfo.method.isHasProxy()
////				 && !type.equals(PassInfo.method.frame_proxy_var.type)
//				   )
				)
				Code.addInstr(op_dup);
				// Constructor call args (first args 'this' skipped)
				if( PassInfo.method!=null && !PassInfo.method.isStatic() )
					Code.addInstr(Instr.op_load,PassInfo.method.params[0]);
				if( type.args == null )
					Code.addConst(0);
				else
					Code.addConst(type.args.length);
				// Now, fill proxyed fields (vars)
				Struct cl = type.clazz;
				for(int i=0; i < cl.fields.length; i++) {
					if( !cl.fields[i].isNeedProxy() ) continue;
					Var v = ((VarAccessExpr)cl.fields[i].init).var;
					Code.addInstr(Instr.op_load,v);
				}
				Code.addInstr(op_call,func,false);
			} else {
				int nargs = args.length;
				if (!func.isStatic()) {
					Code.addInstr(Instr.op_load,PassInfo.method.params[0]);
					nargs++;
				}
				for(int i=0; i < args.length; i++)
					args[i].generate(func.type.args[i]);
				Code.addInstr(op_new,func,nargs,type);
			}
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public void cleanup() {
		parent=null;
		type = null;
		func = null;
	}

	public Dumper toJava(Dumper dmp) {
		Struct cl = type.clazz;
		dmp.append("new ").append(cl.super_clazz.clazz.name).append('(')
			.append(String.valueOf(type.args.length)).append(')');
		dmp.space().append('{').newLine(1);
		if( cl.isClazz() ) {
			for(int i=0; cl.sub_clazz!=null && i < cl.sub_clazz.length; i++) {
				if( cl.sub_clazz[i].isLocal() ) continue;
				cl.sub_clazz[i].toJavaDecl(dmp).newLine();
			}
		}
		for(int i=0; cl.fields!=null && i < cl.fields.length; i++) {
			cl.fields[i].toJavaDecl(dmp).newLine();
		}
		for(int i=0; cl.methods!=null && i < cl.methods.length; i++) {
			cl.methods[i].toJavaDecl(dmp).newLine();
		}
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

