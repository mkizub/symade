/* Generated By:JJTree: Do not edit this line. ASTCallExpression.java */

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

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTCallExpression.java,v 1.4.2.1.2.1 1999/02/15 21:45:08 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.2.1.2.1 $
 *
 */

@node
@cfnode
public class ASTCallExpression extends Expr {
	@att public ASTIdentifier			func;
    @att public final NArr<Expr>		args;

	public ASTCallExpression() {
	}

	public ASTCallExpression(int pos, KString func, Expr[] args) {
		super(pos);
		this.func = new ASTIdentifier(pos, func);
		foreach (Expr e; args) {
			this.args.append(e);
		}
	}

	public ASTCallExpression(int pos, KString func, NArr<Expr> args) {
		super(pos);
		this.func = new ASTIdentifier(pos, func);
		this.args = args;
		foreach (Expr e; args) this.args.append(e);
	}

	public ASTNode resolve(Type reqType) {
    	for(int i=0; i < args.length; i++) {
//			try {
				args[i] = (Expr)args[i].resolve(null);
//			} catch(Exception e) {
//				Kiev.reportError(args[i].getPos(),e);
//			}
        }
		// method of current class or first-order function
		PVar<ASTNode> m;
		Type tp = PassInfo.clazz.type;
		Type ret = reqType;
	retry_with_null_ret:;
		if( func.name.equals(nameThis) ) {
			Method mmm = PassInfo.method;
			if( mmm.name.equals(nameInit) && PassInfo.clazz.type.args.length > 0 ) {
				// Insert our-generated typeinfo, or from childs class?
				if( mmm.type.args.length > 0 && mmm.type.args[0].isInstanceOf(Type.tpTypeInfo) )
					args.insert(new VarAccessExpr(pos,this,mmm.params[0]),0);
				else
					args.insert(PassInfo.clazz.accessTypeInfoField(pos,this,PassInfo.clazz.type),0);
			}
			ResInfo info = new ResInfo(ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if( !PassInfo.resolveBestMethodR(PassInfo.clazz.type,m,info,PassInfo.method.name.name,args.toArray(),ret,tp) )
				throw new CompilerException(pos,"Method "+Method.toString(func.name,args)+" unresolved");
            if( info.isEmpty() )
				return new CallExpr(pos,parent,(Method)m,((Method)m).makeArgs(args,PassInfo.clazz.super_type),false).resolve(ret);
			else
				throw new CompilerException(getPos(),"Constructor call via forwarding is not allowed");
			//Expr e = info.buildCall(pos,null,m,((Method)m).makeArgs(args,tp));
			//return e.resolve(ret);
		}
		else if( func.name.equals(nameSuper) ) {
			Method mmm = PassInfo.method;
			if( mmm.name.equals(nameInit) && PassInfo.clazz.super_type.args.length > 0 ) {
				// no // Insert our-generated typeinfo, or from childs class?
				if( mmm.type.args.length > 0 && mmm.type.args[0].isInstanceOf(Type.tpTypeInfo) )
					args.insert(new VarAccessExpr(pos,this,mmm.params[0]),0);
				else if( mmm.type.args.length > 1 && mmm.type.args[1].isInstanceOf(Type.tpTypeInfo) )
					args.insert(new VarAccessExpr(pos,this,mmm.params[1]),0);
				else
					args.insert(PassInfo.clazz.accessTypeInfoField(pos,this,PassInfo.clazz.super_type),0);
			}
			// If we extend inner non-static class - pass this$N as first argument
			if( ((Struct)PassInfo.clazz.super_type.clazz).package_clazz.isClazz()
			 && !PassInfo.clazz.super_type.clazz.isStatic()
			) {
				if( PassInfo.clazz.isStatic() )
					throw new CompilerException(pos,"Non-static inner super-class of static class");
				args.insert(new VarAccessExpr(pos,(Var)PassInfo.method.params[0]),0);
			}
			ResInfo info = new ResInfo(ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if( !PassInfo.resolveBestMethodR(PassInfo.clazz.super_type,
					m,info,PassInfo.method.name.name,args.toArray(),ret,PassInfo.clazz.super_type) )
				throw new CompilerException(pos,"Method "+Method.toString(func.name,args)+" unresolved");
            if( info.isEmpty() )
				return new CallExpr(pos,parent,(Method)m,((Method)m).makeArgs(args,PassInfo.clazz.super_type),true).resolve(ret);
			else
				throw new CompilerException(getPos(),"Super-constructor call via forwarding is not allowed");
		} else {
			Expr[] args1 = args.toArray();
			if( reqType instanceof MethodType && reqType.args.length > 0 ) {
				for(int i=0; i < reqType.args.length; i++) {
					args1 = (Expr[])Arrays.append(args1,new VarAccessExpr(pos,this,new Var(pos,KString.Empty,reqType.args[i],0)));
				}
			}
			ResInfo info = new ResInfo();
			if( !PassInfo.resolveMethodR(m,info,func.name,args1,ret,tp) ) {
				// May be a closure
				PVar<ASTNode> closure = new PVar<ASTNode>();
				ResInfo info = new ResInfo();
				if( !PassInfo.resolveNameR(closure,info,func.name,tp) ) {
					if( ret != null ) { ret = null; goto retry_with_null_ret; }
					throw new CompilerException(pos,"Unresolved method "+Method.toString(func.name,args,ret));
				}
				try {
					if( closure instanceof Var && Type.getRealType(tp,((Var)closure).type) instanceof MethodType
					||  closure instanceof Field && Type.getRealType(tp,((Field)closure).type) instanceof MethodType
					) {
//						Expr e = info.buildCall(pos,null,m,args.toArray());
//						return e.resolve(ret);
						if( info.isEmpty() )
							return new ClosureCallExpr(pos,parent,closure,args.toArray()).resolve(ret);
						else {
							return new ClosureCallExpr(pos,parent,info.buildAccess(pos,null),closure,args.toArray()).resolve(ret);
						}
					}
				} catch(Exception eee) {
					Kiev.reportError(pos,eee);
				}
				if( ret != null ) { ret = null; goto retry_with_null_ret; }
				throw new CompilerException(pos,"Unresolved method "+Method.toString(func.name,args));
			}
			if( reqType instanceof MethodType ) {
				ASTAnonymouseClosure ac = new ASTAnonymouseClosure();
				ac.pos = pos;
				ac.parent = parent;
				ac.rettype = new TypeRef(pos, ((MethodType)reqType).ret);
				for (int i=0; i < ac.params.length; i++)
					ac.params.append(new FormPar(pos,KString.from("arg"+(i+1)),((Method)m).type.args[i],0));
				BlockStat bs = new BlockStat(pos,ac,ASTNode.emptyArray);
				Expr[] oldargs = args.toArray();
				Expr[] cargs = new Expr[ac.params.length];
				for(int i=0; i < cargs.length; i++)
					cargs[i] = new VarAccessExpr(pos,this,(Var)ac.params[i]);
				args.delAll();
				foreach (Expr e; cargs)
					args.add(e);
				if( ac.rettype.getType() == Type.tpVoid ) {
					bs.addStatement(new ExprStat(pos,bs,this));
					bs.addStatement(new ReturnStat(pos,bs,null));
				} else {
					bs.addStatement(new ReturnStat(pos,bs,this));
				}
				ac.body = bs;
				if( oldargs.length > 0 )
					return new ClosureCallExpr(pos,ac.resolve(reqType),oldargs).resolve(reqType);
				else
					return ac.resolve(reqType);
			} else {
				if( m.isStatic() )
					assert (info.isEmpty());
				Expr e = info.buildCall(pos,null,m,((Method)m).makeArgs(args,tp));
				return e.resolve(ret);
			}
		}
	}

	public int		getPriority() { return Constants.opCallPriority; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
    	sb.append(func).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		return sb.append(')').toString();
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(func).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		return dmp.append(')');
	}
}
