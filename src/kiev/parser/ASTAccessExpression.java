/* Generated By:JJTree: Do not edit this line. ASTAccessExpression.java */

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

import kiev.*;
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class ASTAccessExpression extends Expr {
	private static KString nameWrapperSelf = KString.from("$self");
	
	@att public ENode			obj;
	@att public ASTIdentifier	ident;

	public boolean preResolve() {
		PassInfo.push(this);
		try {
			ASTNode[] res;
			Type[] tps;

			// pre-resolve access
			obj.preResolve();
			// pre-resolve result
			if( obj instanceof TypeRef ) {
				tps = new Type[]{ ((TypeRef)obj).getType() };
				res = new ASTNode[1];
				if( ident.name.equals(nameThis) )
					res[0] = new OuterThisAccessExpr(pos,tps[0].getStruct());
			}
			else {
				ENode e = obj;
				tps = new Type[]{e.getType()};
				res = new ASTNode[tps.length];
				for (int si=0; si < tps.length; si++) {
					Type tp = tps[si];
					if( ident.name.equals(nameWrapperSelf) && tp.isReference() ) {
						if (tp.isWrapper()) {
							tps[si] = ((WrapperType)tp).getUnwrappedType();
							res[si] = obj;
						}
					}
					else if (ident.name.byteAt(0) == '$') {
						while (tp.isWrapper())
							tps[si] = tp = ((WrapperType)tp).getUnwrappedType();
					}
					else if( ident.name.equals("length") ) {
						if( tp.isArray() ) {
							tps[si] = Type.tpInt;
							res[si] = new ArrayLengthAccessExpr(pos,(ENode)e.copy());
						}
					}
				}
				// fall down
			}
			for (int si=0; si < tps.length; si++) {
				if (res[si] != null)
					continue;
				Type tp = tps[si];
				ASTNode@ v;
				ResInfo info = new ResInfo(ResInfo.noStatic | ResInfo.noImports);
				if (obj instanceof Expr && tp.resolveNameAccessR(v,info,ident.name) ) {
					res[si] = makeExpr(v,info,obj);
				}
				else if (tp.resolveStaticNameR(v,info=new ResInfo(),ident.name)) {
					res[si] = makeExpr(v,info,tp.getStruct());
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
				StringBuffer msg = new StringBuffer("Umbigous access:\n");
				for(int si=0; si < res.length; si++) {
					if (res[si] == null)
						continue;
					msg.append("\t").append(res).append('\n');
				}
				msg.append("while resolving ").append(this);
				throw new CompilerException(pos, msg.toString());
			}
			if (cnt == 0) {
				//StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
				//for(int si=0; si < res.length; si++) {
				//	if (tps[si] == null)
				//		continue;
				//	msg.append("\t").append(tps[si]).append('\n');
				//}
				//msg.append("while resolving ").append(this);
				//throw new CompilerException(pos, msg.toString());
				//Kiev.reportWarning(pos, "Cannot pre-resolve "+this);
				obj = obj;
				return false;
			}
			this.replaceWithNode(res[idx]);
		} finally { PassInfo.pop(this); }
		return false;
	}
	
	public void resolve(Type reqType) throws CompilerException {
		PassInfo.push(this);
		try {
			ENode[] res;
			Type[] tps;

			// resolve access
			obj.resolve(null);

		try_static:
			if( obj instanceof TypeRef ) {
				tps = new Type[]{ ((TypeRef)obj).getType() };
				res = new ENode[1];
				if( ident.name.equals(nameThis) )
					res[0] = new OuterThisAccessExpr(pos,tps[0].getStruct());
			}
			else {
				Expr e = (Expr)obj;
				tps = e.getAccessTypes();
				res = new ENode[tps.length];
				for (int si=0; si < tps.length; si++) {
					Type tp = tps[si];
					if( ident.name.equals(nameWrapperSelf) && tp.isReference() ) {
						if (tp.isWrapper()) {
							tps[si] = ((WrapperType)tp).getUnwrappedType();
							res[si] = obj;
						}
						else if (tp.isInstanceOf(Type.tpPrologVar)) {
							tps[si] = tp;
							res[si] = obj;
						}
					}
					else if (ident.name.byteAt(0) == '$') {
						while (tp.isWrapper())
							tps[si] = tp = ((WrapperType)tp).getUnwrappedType();
					}
					else if( ident.name.equals("length") ) {
						if( tp.isArray() ) {
							tps[si] = Type.tpInt;
							res[si] = new ArrayLengthAccessExpr(pos,(Expr)e.copy());
						}
					}
				}
				// fall down
			}
			for (int si=0; si < tps.length; si++) {
				if (res[si] != null)
					continue;
				Type tp = tps[si];
				ASTNode@ v;
				ResInfo info;
				if (obj instanceof Expr &&
					tp.resolveNameAccessR(v,info=new ResInfo(ResInfo.noStatic|ResInfo.noImports),ident.name) )
				{
					res[si] = makeExpr(v,info,obj);
				}
				else if (tp.resolveStaticNameR(v,info=new ResInfo(),ident.name))
				{
					res[si] = makeExpr(v,info,tp.getStruct());
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
				StringBuffer msg = new StringBuffer("Umbigous access:\n");
				for(int si=0; si < res.length; si++) {
					if (res[si] == null)
						continue;
					msg.append("\t").append(res).append('\n');
				}
				msg.append("while resolving ").append(this);
				throw new CompilerException(pos, msg.toString());
			}
			if (cnt == 0) {
				StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
				for(int si=0; si < res.length; si++) {
					if (tps[si] == null)
						continue;
					msg.append("\t").append(tps[si]).append('\n');
				}
				msg.append("while resolving ").append(this);
				throw new CompilerException(pos, msg.toString());
				obj = obj;
				return;
			}
			this.replaceWithNodeResolve(reqType,res[idx]);
		} finally { PassInfo.pop(this); }
	}

	private ENode makeExpr(ASTNode v, ResInfo info, ASTNode o) {
		if( v instanceof Field ) {
			return info.buildAccess(pos, o, v);
		}
		else if( v instanceof Struct ) {
			TypeRef tr = new TypeRef(((Struct)v).type);
			return tr;
		}
//		else if( v instanceof Method ) {
//			if( v.isStatic() ) {
//				return new CallExpr(pos,(Method)v,Expr.emptyArray);
//			}
//			if( info.isEmpty() ) {
//				if( o instanceof Struct )
//					throw new CompilerException(pos,"Static access to non-static method "+v);
//				return new CallExpr(pos,(Expr)obj,(Method)v,Expr.emptyArray);
//			} else {
//				ENode e = info.buildCall(pos, (Expr)obj, v, Expr.emptyArray);
//				return e;
//			}
//		}
		else {
			throw new CompilerException(pos,"Identifier "+ident+" must be a class's field");
		}
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
    	return obj+"."+ident;
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('.').append(ident.name);
		return dmp;
	}
}
