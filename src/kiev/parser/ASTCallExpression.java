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

public class ASTCallExpression extends Expr {
	public KString	func;
    public Expr[]	args = Expr.emptyArray;

	public ASTCallExpression(int id) {
		super(0);
	}

	public ASTCallExpression(int pos, KString func, Expr[] args) {
		super(pos);
		this.func = func;
		this.args = args;
		for (int i=0; i < args.length; i++)
			args[i].parent = this;
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if(i==0) {
			func=((ASTIdentifier)n).name;
            pos = n.getPos();
		} else {
			args = (Expr[])Arrays.append(args,n);
        }
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
		PVar<ASTNode> m = new PVar<ASTNode>();
		ResPath path = new ResPath();
		Type tp = PassInfo.clazz.type;
		Type ret = reqType;
	retry_with_null_ret:;
		if( func.equals(nameThis) ) {
			Method mmm = PassInfo.method;
			if( !Kiev.kaffe && mmm.name.equals(nameInit) && PassInfo.clazz.type.args.length > 0 ) {
				// Insert our-generated typeinfo, or from childs class?
				if( mmm.type.args.length > 0 && mmm.type.args[0].isInstanceOf(Type.tpTypeInfo) )
					args = (Expr[])Arrays.insert(args,new VarAccessExpr(pos,this,mmm.params[1]),0);
				else
					args = (Expr[])Arrays.insert(args,
						PassInfo.clazz.accessTypeInfoField(pos,this,PassInfo.clazz.type),0);
			}
			if( !PassInfo.resolveBestMethodR(PassInfo.clazz,m,path,PassInfo.method.name.name,args,ret,tp,0) )
				throw new CompilerException(pos,"Method "+Method.toString(func,args)+" unresolved");
            if( path.length() == 0 )
				return new CallExpr(pos,parent,(Method)m,((Method)m).makeArgs(args,tp),false).resolve(ret);
			else
				return new CallAccessExpr(pos,parent,Method.getAccessExpr(path),(Method)m,((Method)m).makeArgs(args,tp)).resolve(ret);
		}
		else if( func.equals(nameSuper) ) {
			Method mmm = PassInfo.method;
			if( !Kiev.kaffe && mmm.name.equals(nameInit) && PassInfo.clazz.super_clazz.args.length > 0 ) {
				// no // Insert our-generated typeinfo, or from childs class?
				if( mmm.type.args.length > 0 && mmm.type.args[0].isInstanceOf(Type.tpTypeInfo) )
					args = (Expr[])Arrays.insert(args,new VarAccessExpr(pos,this,mmm.params[1]),0);
				else if( mmm.type.args.length > 1 && mmm.type.args[1].isInstanceOf(Type.tpTypeInfo) )
					args = (Expr[])Arrays.insert(args,new VarAccessExpr(pos,this,mmm.params[2]),0);
				else
					args = (Expr[])Arrays.insert(args,
						PassInfo.clazz.accessTypeInfoField(pos,this,PassInfo.clazz.super_clazz),0);
			}
			// If we extend inner non-static class - pass this$N as first argument
			if( PassInfo.clazz.super_clazz.clazz.package_clazz.isClazz()
			 && !PassInfo.clazz.super_clazz.clazz.isStatic()
			) {
				if( PassInfo.clazz.isStatic() )
					throw new CompilerException(pos,"Non-static inner super-class of static class");
				args = (Expr[])Arrays.insert(args,new VarAccessExpr(pos,(Var)PassInfo.method.params[1]),0);
			}
			if( !PassInfo.resolveBestMethodR(PassInfo.clazz.super_clazz.clazz,
					m,path,PassInfo.method.name.name,args,ret,PassInfo.clazz.super_clazz,0) )
				throw new CompilerException(pos,"Method "+Method.toString(func,args)+" unresolved");
            if( path.length() == 0 )
				return new CallExpr(pos,parent,(Method)m,((Method)m).makeArgs(args,PassInfo.clazz.super_clazz),true).resolve(ret);
			else
				throw new CompilerException(getPos(),"Super-call via forwarding is not allowed");
		} else {
			Expr[] args1 = args;
			if( reqType instanceof MethodType && reqType.args.length > 0 ) {
				for(int i=0; i < reqType.args.length; i++) {
					args1 = (Expr[])Arrays.append(args1,new VarAccessExpr(pos,this,new Var(pos,this,KString.Empty,reqType.args[i],0)));
				}
			}
			if( !PassInfo.resolveMethodR(m,path,func,args1,ret,tp,0) ) {
				// May be a closure
				PVar<ASTNode> closure = new PVar<ASTNode>();
				ResPath path = new ResPath();
				if( !PassInfo.resolveNameR(closure,path,func,tp,0) ) {
					if( ret != null ) { ret = null; goto retry_with_null_ret; }
					throw new CompilerException(pos,"Unresolved method "+Method.toString(func,args,ret));
				}
				try {
					if( closure instanceof Var && Type.getRealType(tp,((Var)closure).type) instanceof MethodType
					||  closure instanceof Field && Type.getRealType(tp,((Field)closure).type) instanceof MethodType
					) {
						if( path.length()  == 0 )
							return new ClosureCallExpr(pos,parent,closure,args).resolve(ret);
						else {
							return new ClosureCallExpr(pos,parent,Method.getAccessExpr(path),closure,args).resolve(ret);
						}
					}
				} catch(Exception eee) {
					Kiev.reportError(pos,eee);
				}
				throw new CompilerException(pos,"Unresolved method "+Method.toString(func,args));
			}
			if( reqType instanceof MethodType ) {
				if( Kiev.kaffe ) {
					return new NewClosure(pos,(Method)m,args).resolve(reqType);
				} else {
					ASTAnonymouseClosure ac = new ASTAnonymouseClosure(kiev020TreeConstants.JJTANONYMOUSECLOSURE);
					ac.pos = pos;
					ac.parent = parent;
					ac.type = ((MethodType)reqType).ret;
					ac.params = new ASTNode[((Method)m).type.args.length];
					for(int i=0; i < ac.params.length; i++)
						ac.params[i] = new Var(pos,KString.from("arg"+(i+1)),((Method)m).type.args[i],0);
					BlockStat bs = new BlockStat(pos,ac,ASTNode.emptyArray);
					Expr[] oldargs = args;
					Expr[] cargs = new Expr[ac.params.length];
					for(int i=0; i < cargs.length; i++)
						cargs[i] = new VarAccessExpr(pos,this,(Var)ac.params[i]);
					args = cargs;
					if( ac.type == Type.tpVoid ) {
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
				}
			} else {
				if( m.isStatic() )
					path.setLength(0);
	            if( path.length() == 0 )
					return new CallExpr(pos,parent,(Method)m,((Method)m).makeArgs(args,tp)).resolve(reqType);
				else
					return new CallAccessExpr(pos,parent,Method.getAccessExpr(path),(Method)m,((Method)m).makeArgs(args,tp)).resolve(reqType);
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
