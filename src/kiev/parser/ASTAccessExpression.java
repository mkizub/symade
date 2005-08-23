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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTAccessExpression.java,v 1.3.2.1 1999/02/12 18:47:02 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1 $
 *
 */

@node
@cfnode
public class ASTAccessExpression extends Expr {
	@att public ENode			obj;
	@att public ASTIdentifier	ident;

	public void preResolve() {
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
//			else if( obj instanceof Struct ) {
//				((Struct)obj).checkResolved();
//				tps = new Type[]{ ((Struct)obj).type };
//				res = new ASTNode[1];
//				if( ident.name.equals(nameThis) )
//					res[0] = new OuterThisAccessExpr(pos,tps[0].getStruct());
//			}
			else {
				Expr e = (Expr)obj;
				tps = e.getAccessTypes();
				res = new ASTNode[tps.length];
				for (int si=0; si < tps.length; si++) {
					Type tp = tps[si];
					if( ident.name.equals("$self") && tp.isReference() ) {
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
				obj = obj;
				return;
			}
			this.replaceWith(res[idx]);
		} finally { PassInfo.pop(this); }
	}
	
	public void resolve(Type reqType) throws CompilerException {
		PassInfo.push(this);
		try {
			obj.resolve(null);
			//if( o == null ) throw new CompilerException(obj.getPos(),"Unresolved object "+obj);
			Type tp = null;
			Type[] snitps = null;
			int snitps_index = 0;
		try_static:
			if( obj instanceof TypeRef ) {
				tp = ((TypeRef)obj).getType();
			}
//			else if( obj instanceof Struct ) {
//				((Struct)o).checkResolved();
//				tp = ((Struct)obj).type;
//			}
			else {
				snitps = ((Expr)obj).getAccessTypes();
				tp = snitps[snitps_index++];
				if (tp.isWrapper() && ident.name.byteAt(0) != '$') {
					obj.replaceWithResolve(tp.makeWrappedAccess(obj), null);
					tp = obj.getType();
				}
				if( tp.isArray() ) {
					if( ident.name.equals("length") ) {
						replaceWithResolve(new ArrayLengthAccessExpr(pos,obj), reqType);
						return;
					}
					else throw new CompilerException(obj.getPos(),"Arrays "+tp+" has only one member 'length'");
				}
				else if( ident.name.equals("$self") && tp.isReference() ) {
					replaceWithResolve(new SelfAccessExpr(pos,(LvalueExpr)obj), reqType);
				}
				else if( tp.isReference() )
					;
				else
					throw new CompilerException(obj.getPos(),"Resolved object "+obj+" of type "+tp+" is not a scope");
			}
			if( obj instanceof TypeRef && ident.name.equals(nameThis) ) {
				replaceWithResolve(new OuterThisAccessExpr(pos,obj.getType().getStruct()), null);
				return;
			}
			ListBuffer<ENode> res = new ListBuffer<ENode>();
			ASTNode@ v;
			ResInfo info;
			int min_transforms = 8096;
			if( obj instanceof Expr && snitps != null && snitps.length > 1) {
				snitps_index = 0;
				while (snitps_index < snitps.length) {
					v.$unbind();
					info = new ResInfo(ResInfo.noStatic | ResInfo.noImports);
					tp = snitps[snitps_index++];
					foreach(tp.resolveNameAccessR(v,info,ident.name) ) {
						if (info.getTransforms() > min_transforms)
							continue;
						ENode e = makeExpr(v,info,obj);
						if (info.getTransforms() < min_transforms) {
							res.setLength(0);
							min_transforms = info.getTransforms();
						}
						res.append(e);
					}
				}
			} else {
					v.$unbind();
					if (obj instanceof Expr) {
						info = new ResInfo(ResInfo.noStatic | ResInfo.noImports);
						foreach(tp.resolveNameAccessR(v,info,ident.name) ) {
							if (info.getTransforms() > min_transforms)
								continue;
							ENode e = makeExpr(v,info,obj);
							if (info.getTransforms() < min_transforms) {
								res.setLength(0);
								min_transforms = info.getTransforms();
							}
							res.append(e);
						}
					} else {
						info = new ResInfo();
						foreach(tp.resolveStaticNameR(v,info,ident.name) ) {
							if (info.getTransforms() > min_transforms)
								continue;
							ENode e = makeExpr(v,info,obj);
							if (info.getTransforms() < min_transforms) {
								res.setLength(0);
								min_transforms = info.getTransforms();
							}
							res.append(e);
						}
					}
			}
			if (res.length() == 0) {
				if (obj instanceof Expr) {
					obj = new TypeRef(obj.getType());
					goto try_static;
				}
				//resolve(reqType);
				throw new CompilerException(pos,"Unresolved identifier "+ident+" in class "+tp+" for type(s) "
					+(snitps==null?tp.toString():Arrays.toString(snitps)) );
			}
			if (res.length() > 1) {
				String msg = "Umbigous identifier "+ident+" in class "+tp+" for type(s) "
					+(snitps==null?tp.toString():Arrays.toString(snitps));
				Dumper dmp = new Dumper();
				dmp.newLine(1);
				foreach (ASTNode r; res.toList()) r.toJava(dmp).newLine();
				dmp.newLine(-1);
				throw new CompilerException(pos,msg+dmp);
			}
			ENode n = res.getAt(0);
			replaceWithResolve(n, reqType);
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
		else if( v instanceof Method ) {
			if( v.isStatic() )
				return new CallExpr(pos,(Method)v,Expr.emptyArray);
			if( info.isEmpty() ) {
				if( o instanceof Struct )
					throw new CompilerException(pos,"Static access to non-static method "+v);
				return new CallAccessExpr(pos,(Expr)obj,(Method)v,Expr.emptyArray);
			} else {
				ENode e = info.buildCall(pos, (Expr)obj, v, Expr.emptyArray);
				return e;
			}
		} else {
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
