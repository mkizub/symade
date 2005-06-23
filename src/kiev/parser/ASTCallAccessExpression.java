/* Generated By:JJTree: Do not edit this line. ASTCallAccessExpression.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTCallAccessExpression.java,v 1.3.2.1.2.1 1999/02/15 21:45:08 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1.2.1 $
 *
 */

public class ASTCallAccessExpression extends ASTExpr {
	public ASTExpr			obj;
	public ASTIdentifier	ident;
    public ASTExpr[]		args = ASTExpr.emptyArray;
	public boolean  in_wrapper;

	public ASTCallAccessExpression(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if(i==0) {
        	obj = (ASTExpr)n;
		} else {
        	ident = ((ASTCallExpression)n).ident;
			args = ((ASTCallExpression)n).args;
            pos = n.getPos();
        }
    }

	public Node resolve(Type reqType) {
		KString func = ident.name;
		for(int i=0; i < args.length; i++) {
			args[i] = (ASTExpr)args[i].resolveExpr(null);
		}
		ASTNode o;
		Struct cl;
		Type tp = null;
		Type ret = reqType;
	retry_with_null_ret:;
		Node@ m;
		ResInfo info = new ResInfo();
		if( obj instanceof ASTIdentifier
		&& ((ASTIdentifier)obj).name.equals(Constants.nameSuper)
		&& !PassInfo.method.isStatic() ) {
			if( !PassInfo.resolveBestMethodR(PassInfo.clazz.super_clazz.clazz,m,info,func,args,ret,tp,0) ) {
				if( ret != null ) { ret = null; goto retry_with_null_ret; }
				throw new CompilerException(obj.getPos(),"Unresolved method "+Method.toString(func,args,ret));
			}
			if( info.path.length() == 0 )
				return new CallExpr(pos,parent,(Method)m,((Method)m).makeArgs(args,tp),true).resolve(ret);
			else
				throw new CompilerException(obj.getPos(),"Super-call via forwarding is not allowed");
		} else {
			o = obj.resolve(null);
			if( o == null )
				throw new CompilerException(obj.getPos(),"Unresolved object "+obj);
			if( o instanceof Struct ) {
				cl = (Struct)o;
				if( !PassInfo.resolveBestMethodR(cl,m,info,func,args,ret,tp,0) ) {
					// May be a closure
					PVar<ASTNode> closure = new PVar<ASTNode>();
					info = new ResInfo();
					if( !cl.resolveNameR(closure,info,func,tp,0) ) {
						if( ret != null ) { ret = null; goto retry_with_null_ret; }
						throw new CompilerException(pos,"Unresolved method "+Method.toString(func,args,ret));
					}
					try {
						if( closure instanceof Var && Type.getRealType(tp,((Var)closure).type) instanceof MethodType
						||  closure instanceof Field && Type.getRealType(tp,((Field)closure).type) instanceof MethodType
						) {
							if( info.path.length() == 0 )
								return new ClosureCallExpr(pos,parent,closure,args).resolve(ret);
							else {
								return new ClosureCallExpr(pos,parent,Method.getAccessExpr(info),closure,args).resolve(ret);
							}
						}
					} catch(Exception eee) {
						Kiev.reportError(pos,eee);
					}
					if( ret != null ) { ret = null; goto retry_with_null_ret; }
					throw new CompilerException(pos,"Method "+Method.toString(func,args,ret)+" unresolved in "+cl);
				}
				if( !m.isStatic() )
					throw new CompilerException(pos,"Static call to non-static method");
				return new CallExpr(pos,parent,(Method)m,((Method)m).makeArgs(args,tp)).resolve(ret);
			}
			else if( o instanceof Expr) {
				Type[] snitps = null;
				int snitps_index = 0;
				snitps = ((Expr)o).getAccessTypes();
				tp = snitps[snitps_index++];
				if (in_wrapper) {
					if (!tp.clazz.isWrapper())
						throw new CompilerException(o.getPos(),"Class "+tp+" is not a wrapper");
				}
				else if (tp.clazz.isWrapper() && func.byteAt(0) != '$') {
					o = (Expr)new AccessExpr(o.pos,(Expr)o,tp.clazz.wrapped_field).resolve(null);
					tp = o.getType();
				}
				if( reqType instanceof MethodType ) ret = null;
				if( tp.isReference() ) {
			retry_resolving:;
					cl = (Struct)tp.clazz;
					if( !PassInfo.resolveBestMethodR(cl,m,info,func,args,ret,tp,0) ) {
						// May be a closure
						PVar<ASTNode> closure = new PVar<ASTNode>();
						info = new ResInfo();
						if( !cl.resolveNameR(closure,info,func,tp,0) ) {
							if( o instanceof Expr && snitps != null ) {
								if( snitps_index < snitps.length ) {
									tp = snitps[snitps_index++];
//									cl = (Struct)tp.clazz;
									goto retry_resolving;
								}
							}
							if( ret != null ) { ret = null; goto retry_with_null_ret; }
							throw new CompilerException(pos,"Unresolved method "+Method.toString(func,args,ret)+" in "
								+(snitps==null?tp.toString():Arrays.toString(snitps)) );
						}
						try {
							if( closure instanceof Var && Type.getRealType(tp,((Var)closure).type) instanceof MethodType
							||  closure instanceof Field && Type.getRealType(tp,((Field)closure).type) instanceof MethodType
							) {
								if( info.path.length() == 0 )
									return new ClosureCallExpr(pos,parent,(Expr)o,closure,args).resolve(reqType);
								else {
									return new ClosureCallExpr(pos,parent,Method.getAccessExpr(info,(Expr)o),closure,args).resolve(reqType);
								}
							}
						} catch(Exception eee) {
							Kiev.reportError(pos,eee);
						}
						if( ret != null ) { ret = null; goto retry_with_null_ret; }
						throw new CompilerException(pos,"Method "+Method.toString(func,args,reqType)+" unresolved in "+tp);
					}
					if( reqType instanceof MethodType ) {
						ASTAnonymouseClosure ac = new ASTAnonymouseClosure(kiev020TreeConstants.JJTANONYMOUSECLOSURE);
						ac.pos = pos;
						ac.parent = parent;
						ac.type = ((MethodType)reqType).ret;
						ac.params = new ASTNode[((Method)m).type.args.length];
						for(int i=0; i < ac.params.length; i++)
							ac.params[i] = new Var(pos,KString.from("arg"+(i+1)),((Method)m).type.args[i],0);
						BlockStat bs = new BlockStat(pos,ac);
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
					} else {
						obj = (Expr)o;
						if( m.isStatic() )
							return new CallExpr(pos,parent,(Method)m,args).resolve(reqType);
						else
							return new CallAccessExpr(pos,parent,Method.getAccessExpr(info,obj),
								(Method)m,((Method)m).makeArgs(args,tp)).resolve(reqType);
					}
				} else {
					throw new CompilerException(obj.getPos(),"Resolved object "+obj+" of type "+tp+" is not a scope");
				}
			} else {
				throw new CompilerException(o.getPos(),"Resolved object "+o+" is not an object");
			}
		}
	}

	public int		getPriority() { return Constants.opCallPriority; }

	public String toString() {
		KString func = ident.name;
		StringBuffer sb = new StringBuffer();
    	sb.append(obj).append('.').append(func).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		return sb.append(')').toString();
	}

	public Dumper toJava(Dumper dmp) {
		KString func = ident.name;
    	dmp.append(obj).append('.').append(func).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		return dmp.append(')');
	}
}
