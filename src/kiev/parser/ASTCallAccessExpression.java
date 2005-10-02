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
 * @author Maxim Kizub
 *
 */

@node
@dflow(out="args")
public class ASTCallAccessExpression extends Expr {
	@dflow
	@att public ENode					obj;

	@att public ASTIdentifier			func;

	@dflow(in="obj", seq="true")
    @att public final NArr<ENode>		args;

	public ASTCallAccessExpression() {}
	
	public ASTCallAccessExpression(int pos, ENode obj, KString func, ENode[] args) {
		super(pos);
		this.obj = obj;
		this.func = new ASTIdentifier(pos, func);
		this.args.addAll(args);
	}

	public boolean preResolve() {
		PassInfo.push(this);
		try {
			// pre-resolve 'obj', but check it's not 'super.'
			if (obj != null) {
				if !( obj instanceof ASTIdentifier && ((ASTIdentifier)obj).name.equals(Constants.nameSuper) )
					obj.preResolve();
			}
			// don't pre-resolve 'func'
			;
			// pre-resolve arguments
			foreach (ENode e; args) e.preResolve();
		} finally { PassInfo.pop(this); }
		return false;
	}
	
	public void resolve(Type reqType) {
		for(int i=0; i < args.length; i++) {
			args[i].resolve(null);
		}
		if( obj instanceof ASTIdentifier
		&& ((ASTIdentifier)obj).name.equals(Constants.nameSuper)
		&& !PassInfo.method.isStatic() ) {
			Type ret = reqType;
			ASTNode@ m;
	retry_with_null_ret:;
			Type tp = null;
			ResInfo info = new ResInfo();
			ThisExpr sup = new ThisExpr();
			info.enterForward(sup);
			info.enterSuper();
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();
			MethodType mt = MethodType.newMethodType(null,ta,ret);
			if( !PassInfo.resolveBestMethodR(PassInfo.clazz.super_type,m,info,func.name,mt) ) {
				if( ret != null ) { ret = null; goto retry_with_null_ret; }
				throw new CompilerException(obj.getPos(),"Unresolved method "+Method.toString(func.name,args,ret));
			}
			info.leaveSuper();
			info.leaveForward(sup);
			if( info.isEmpty() ) {
				Method meth = (Method)m;
				CallExpr cae = new CallExpr(pos,sup,meth,args);
				cae.super_flag = true;
				replaceWithNode(cae);
				meth.makeArgs(cae.args, tp);
				cae.resolve(ret);
				return;
			}
			throw new CompilerException(obj.getPos(),"Super-call via forwarding is not allowed");
		}
		
		obj.resolve(null);
		
		if !(obj instanceof Expr || obj instanceof TypeRef)
			throw new CompilerException(obj.getPos(),"Resolved object "+obj+" is not an expression or type name");

		MethodType mt = null;
		{
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();
			mt = MethodType.newMethodType(null,ta,null);
		}
		int res_flags = ResInfo.noStatic | ResInfo.noImports;
		ENode[] res;
		Type[] tps;
	try_static:;
		if( obj instanceof TypeRef ) {
			Type tp = ((TypeRef)obj).getType();
			tps = new Type[]{tp};
			res = new ENode[1];
			res_flags = 0;
		} else {
			tps = ((Expr)obj).getAccessTypes();
			res = new ENode[tps.length];
			for (int si=0; si < tps.length; si++) {
				Type tp = tps[si];
				if (func.name.byteAt(0) == '$') {
					while (tp.isWrapper())
						tps[si] = tp = ((WrapperType)tp).getUnwrappedType();
				}
			}
			// fall down
		}
		for (int si=0; si < tps.length; si++) {
			Type tp = tps[si];
			ASTNode@ m;
			ResInfo info = new ResInfo(res_flags);
			if (PassInfo.resolveBestMethodR(tp,m,info,func.name,mt)) {
				if (tps.length == 1 && res_flags == 0)
					res[si] = info.buildCall(pos, obj, m, args.toArray());
				else if (res_flags == 0)
					res[si] = info.buildCall(pos, new TypeRef(tps[si]), m, args.toArray());
				else
					res[si] = info.buildCall(pos, (ENode)obj.copy(), m, args.toArray());
			}
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
			StringBuffer msg = new StringBuffer("Umbigous methods:\n");
			for(int si=0; si < res.length; si++) {
				if (res[si] == null)
					continue;
				msg.append("\t").append(res[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(pos, msg.toString());
		}
		if (cnt == 0 && res_flags != 0) {
			res_flags = 0;
			goto try_static;
		}
		if (cnt == 0) {
			StringBuffer msg = new StringBuffer("Unresolved method '"+Method.toString(func.name,mt)+"' in:\n");
			for(int si=0; si < res.length; si++) {
				if (tps[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(pos, msg.toString());
			return;
		}
		this.replaceWithNodeResolve( reqType, res[idx] );
	}

	public int		getPriority() { return Constants.opCallPriority; }

	public String toString() {
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
    	dmp.append(obj).append('.').append(func).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		return dmp.append(')');
	}
}
