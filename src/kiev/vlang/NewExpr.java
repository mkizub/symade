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
import kiev.parser.*;
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

@node
@cfnode
public class NewExpr extends Expr {

	@att public TypeRef				type;
	@att public final NArr<ENode>	args;
	@att public ENode				outer;
	@att public ENode				tif_expr;	// TypeInfo field access expression
	@att public Struct				clazz; // if this new expression defines new class

	@ref public Method	func;

	public NewExpr() {
	}

	public NewExpr(int pos, Type type, ENode[] args) {
		super(pos);
		this.type = new TypeRef(type);
		foreach (Expr e; args) this.args.append(e);
	}

	public NewExpr(int pos, TypeRef type, ENode[] args) {
		super(pos);
		this.type = type;
		foreach (Expr e; args) this.args.append(e);
	}

	public NewExpr(int pos, Type type, ENode[] args, ENode outer) {
		this(pos,type,args);
		this.outer = outer;
	}

	public NewExpr(int pos, TypeRef type, ENode[] args, ENode outer) {
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
		return type.getType();
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			if( type.isAnonymouseClazz() ) {
				type.getStruct().resolveDecl();
			}
			if( !type.isArgument() && (type.isAbstract() || !type.isClazz()) ) {
				throw new CompilerException(pos,"Abstract class "+type+" instantiation");
			}
			if( outer != null ) outer.resolve(null);
			else if( (!type.isStaticClazz() && type.isLocalClazz())
				  || (!type.isStaticClazz() && !type.getStruct().package_clazz.isPackage()) ) {
				if( PassInfo.method==null || PassInfo.method.isStatic() )
					throw new CompilerException(pos,"'new' for inner class requares outer instance specification");
				Var th = PassInfo.method.getThisPar();
				outer = new VarAccessExpr(pos,this,th);
				outer.resolve(null);
			}
			for(int i=0; i < args.length; i++)
				args[i].resolve(null);
			ENode[] outer_args;
			if( outer != null ) {
				outer_args = new ENode[args.length+1];
				outer_args[0] = outer;
				for(int i=0; i < args.length; i++)
					outer_args[i+1] = args[i];
			} else {
				outer_args = args.toArray();
			}
			if( type.isLocalClazz() ) {
				Struct cl = (Struct)type.clazz;
				foreach (ASTNode n; cl.members; n instanceof Field) {
					Field f = (Field)n;
					if( !f.isNeedProxy() ) continue;
					outer_args = (Expr[])Arrays.append(outer_args,new AccessExpr(pos,new ThisExpr(pos),f));
				}
			}
			if( type.args.length > 0 ) {
				// Create static field for this type typeinfo
				tif_expr = PassInfo.clazz.accessTypeInfoField(pos,this,type.getType());
				args.insert(0,tif_expr);
				outer_args = (Expr[])Arrays.insert(outer_args,tif_expr,(outer!=null?1:0));
			}
			// Don't try to find constructor of argument type
			if( !type.isArgument() ) {
				Type[] ta = new Type[outer_args.length];
				for (int i=0; i < ta.length; i++)
					ta[i] = outer_args[i].getType();
				MethodType mt = MethodType.newMethodType(null,ta,type.getType());
				Method@ m;
				// First try overloaded 'new', than real 'new'
				if( (PassInfo.method==null || !PassInfo.method.name.equals(nameNewOp)) ) {
					ResInfo info = new ResInfo(ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports);
					if (PassInfo.resolveBestMethodR(type,m,info,nameNewOp,mt)) {
						CallExpr n = new CallExpr(pos,parent,(Method)m,m.makeArgs(args,type));
						n.type_of_static = type;
						n.setResolved(true);
						replaceWith(n);
						return;
					}
				}
				mt = MethodType.newMethodType(null,ta,Type.tpVoid);
				ResInfo info = new ResInfo(ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports|ResInfo.noStatic);
				if( PassInfo.resolveBestMethodR(type,m,info,nameInit,mt) ) {
					func = m;
					outer_args = m.makeArgs(outer_args,type.getType());
					int po = 0;
					int pa = 0;
					if( outer != null ) outer = outer_args[po++];
					while( pa < args.length ) args[pa++] = outer_args[po++];
				}
				else {
					throw new RuntimeException("Can't find apropriative initializer for "+
						Method.toString(nameInit,outer_args,Type.tpVoid)+" for "+type);
				}
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewExpr: "+this);
		PassInfo.push(this);
		try {
			if( type.isArgument() ) {
				if( outer != null || args.length > 0 ) {
					Kiev.reportError(pos,"Constructor with arguments for type argument is not supported");
					return;
				} else {
					// If we have primitive type
					Type t = type;
					Expr e;
					if( !t.isReference() ) {
						e = (Expr)new ConstNullExpr().resolve(null);
						e.generate(t);
						return;
					}
					int i;
					for(i=0; i < PassInfo.clazz.type.args.length; i++)
						if( type.string_equals(PassInfo.clazz.type.args[i]) ) break;
					if( i >= PassInfo.clazz.type.args.length )
						throw new CompilerException(pos,"Can't create an instance of argument type "+type);
					Expr tie = new AccessExpr(pos,new ThisExpr(pos),PassInfo.clazz.resolveField(nameTypeInfo));
					e = new CastExpr(pos,type,
						new CallAccessExpr(pos,parent,tie,
							Type.tpTypeInfo.clazz.resolveMethod(
								KString.from("newInstance"),
								KString.from("(I)Ljava/lang/Object;")
								),
								new Expr[]{new ConstIntExpr(i)}),
						true).resolveExpr(reqType);
					e.generate(t);
					return;
				}
			}
			Code.addInstr(op_new,type);
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
				Struct cl = (Struct)type.clazz;
				foreach (ASTNode n; cl.members; n instanceof Field) {
					Field f = (Field)n;
					if( !f.isNeedProxy() ) continue;
					Var v = ((VarAccessExpr)f.init).var;
					Code.addInstr(Instr.op_load,v);
				}
			}
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
		Type tp = type;
		if( !tp.isReference() ) {
			return dmp.append('0');
		}
		if( !tp.clazz.isAnonymouse() ) {
			dmp.append("new ").append(tp).append('(');
		} else {
			if( tp.clazz.interfaces.length > 0 )
				dmp.append("new ").append(tp.clazz.interfaces[0].clazz.name).append('(');
			else
				dmp.append("new ").append(tp.clazz.super_type.clazz.name).append('(');
		}
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		dmp.append(')');
		if( tp.clazz.isAnonymouse() ) {
			Struct cl = (Struct)type.clazz;
			dmp.space().append('{').newLine(1);
			foreach (ASTNode n; cl.members)
				((TopLevelDecl)n).toJavaDecl(dmp).newLine();
			dmp.newLine(-1).append('}').newLine();
		}
		return dmp;
	}
}

@node
@cfnode
public class NewArrayExpr extends Expr {

	@att public TypeRef				type;
	@att public final NArr<ENode>	args;
	public int						dim;
	private Type					arrtype;

	public NewArrayExpr() {
	}

	public NewArrayExpr(int pos, TypeRef type, Expr[] args, int dim) {
		super(pos);
		this.type = type;
		foreach (Expr e; args) this.args.append(e);
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

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			for(int i=0; i < args.length; i++)
				if( args[i] != null )
					args[i] = args[i].resolveExpr(Type.tpInt);
			if( type.isArgument() ) {
				if( PassInfo.method==null || PassInfo.method.isStatic() )
					throw new CompilerException(pos,"Access to argument "+type+" from static method");
				int i;
				for(i=0; i < PassInfo.clazz.type.args.length; i++)
					if( type.string_equals(PassInfo.clazz.type.args[i]) ) break;
				if( i >= PassInfo.clazz.type.args.length )
					throw new CompilerException(pos,"Can't create an array of argument type "+type);
				Expr tie = new AccessExpr(pos,new ThisExpr(0),PassInfo.clazz.resolveField(nameTypeInfo));
				if( dim == 1 ) {
					return (Expr)new CastExpr(pos,arrtype,
						new CallAccessExpr(pos,parent,tie,
							Type.tpTypeInfo.resolveMethod(KString.from("newArray"),KString.from("(II)Ljava/lang/Object;")),
							new Expr[]{new ConstIntExpr(i),args[0]}
						),true).resolve(reqType);
				} else {
					return (Expr)new CastExpr(pos,arrtype,
						new CallAccessExpr(pos,parent,tie,
							Type.tpTypeInfo.clazz.resolveMethod(KString.from("newArray"),KString.from("(I[I)Ljava/lang/Object;")),
							new Expr[]{
								new ConstIntExpr(i),
								new NewInitializedArrayExpr(pos,Type.tpInt,1,args.toArray())
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
			if( dim == 1 ) {
				args[0].generate(null);
				Code.addInstr(Instr.op_newarray,type);
			} else {
				for(int i=0; i < args.length; i++)
					args[i].generate(null);
				Code.addInstr(Instr.op_multianewarray,arrtype,args.length);
			}
			if( reqType == Type.tpVoid ) Code.addInstr(Instr.op_pop);
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
		dmp.append("new ").append(type);
		for(int i=0; i < dim; i++) {
			dmp.append('[');
			if( i < args.length && args[i] != null ) args[i].toJava(dmp);
			dmp.append(']');
		}
		return dmp;
	}
}

@node
@cfnode
public class NewInitializedArrayExpr extends Expr {

	@att public TypeRef				type;
	@att public final NArr<ENode>	args;
	public int						dim;
	public int[]					dims;
	private Type				arrtype;

	public NewInitializedArrayExpr() {
	}

	public NewInitializedArrayExpr(int pos, TypeRef type, int dim, ENode[] args) {
		super(pos);
		this.type = type;
		this.dim = dim;
		foreach (Expr e; args) this.args.append(e);
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

	public void resolve(Type reqType) throws RuntimeException {
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

@node
@cfnode
public class NewClosure extends Expr {

	@att public TypeClosureRef		type;
	@att public final NArr<Expr>	args;
	@att public Struct				clazz; // if this new expression defines new class

	@ref public Method	func;

	public NewClosure() {
	}

	public NewClosure(int pos, TypeClosureRef type) {
		super(pos);
		this.type = type;
	}

	public NewClosure(int pos, Method func) {
		super(pos);
		this.func = func;
		this.type = ClosureType.newClosureType(Type.tpClosureClazz,func.type.args,func.type.ret);
	}

	public NewClosure(int pos, Method func, ENode[] args) {
		super(pos);
		this.func = func;
		if(args.length==0)
			this.type = ClosureType.newClosureType(Type.tpClosureClazz,func.type.args,func.type.ret);
		else {
			Type[] targs = new Type[func.type.args.length-args.length];
			for(int i=args.length, j=0; i < func.type.args.length; i++, j++)
				targs[j] = func.type.args[i];
			this.type = ClosureType.newClosureType(Type.tpClosureClazz,targs,func.type.ret);
		}
		foreach (Expr e; args) this.args.append(e);
	}

	public String toString() {
		return "fun "+type;
	}

	public Type getType() { return type; }

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		if( Kiev.passLessThen(TopLevelPass.passResolveImports) ) return this;
		PassInfo.push(this);
		try {
			if( Env.getStruct(Type.tpClosureClazz.name) == null )
				throw new RuntimeException("Core class "+Type.tpClosureClazz.name+" not found");
			type.getStruct().autoProxyMethods();
			type.getStruct().resolve(null);
			Struct cl = type.getStruct();
			KStringBuffer sign = new KStringBuffer().append('(');
			if( PassInfo.method!=null && !PassInfo.method.isStatic() ) {
				sign.append(((Struct)PassInfo.method.parent).type.signature);
			}
			sign.append('I');
			foreach (ASTNode n; cl.members; n instanceof Field) {
				Field f = (Field)n;
				if( !f.isNeedProxy() ) continue;
				sign.append(f.type.signature);
			}
			sign.append(")V");
			func = type.resolveMethod(nameInit,sign.toKString());
			if( !((Struct)func.parent).equals(type.getStruct()) )
				throw new RuntimeException("Can't find apropriative initializer for "+nameInit+sign+" for "+type+" class "+type);
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewClosure: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_new,type);
			// First arg ('this' pointer) is generated by 'op_dup'
			if( reqType != Type.tpVoid )
				Code.addInstr(op_dup);
			// Constructor call args (first args 'this' skipped)
			if( PassInfo.method!=null && !PassInfo.method.isStatic() )
				Code.addInstr(Instr.op_load,PassInfo.method.getThisPar());
			if( type.args == null )
				Code.addConst(0);
			else
				Code.addConst(type.args.length);
			// Now, fill proxyed fields (vars)
			Struct cl = (Struct)type.clazz;
			foreach (ASTNode n; cl.members; n instanceof Field) {
				Field f = (Field)n;
				if( !f.isNeedProxy() ) continue;
				Var v = ((VarAccessExpr)f.init).var;
				Code.addInstr(Instr.op_load,v);
			}
			Code.addInstr(op_call,func,false);
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public void cleanup() {
		parent=null;
		type = null;
		func = null;
	}

	public Dumper toJava(Dumper dmp) {
		Struct cl = (Struct)type.clazz;
		dmp.append("new ").append(cl.super_type.clazz.name).append('(')
			.append(String.valueOf(type.args.length)).append(')');
		dmp.space().append('{').newLine(1);
		foreach (ASTNode n; cl.members)
			((TopLevelDecl)n).toJavaDecl(dmp).newLine();
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

