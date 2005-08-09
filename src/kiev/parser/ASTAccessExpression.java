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
	@att public Expr			obj;
	@att public ASTIdentifier	ident;

	public ASTNode resolve(Type reqType) throws CompilerException {
		PassInfo.push(this);
		try {
			ASTNode o = obj.resolve(null);
			if( o == null ) throw new CompilerException(obj.getPos(),"Unresolved object "+obj);
			BaseStruct cl;
			Type tp = null;
			Type[] snitps = null;
			int snitps_index = 0;
			if( o instanceof Struct ) {
				cl = (Struct)o;
				tp = cl.type;
			} else {
				obj = (Expr)o;
				snitps = ((Expr)o).getAccessTypes();
				tp = snitps[snitps_index++];
				if (tp.clazz.isWrapper() && ident.name.byteAt(0) != '$') {
					o = (Expr)new AccessExpr(obj.pos,obj,((Struct)tp.clazz).wrapped_field).resolve(null);
					tp = o.getType();
				}
				if( tp.isArray() ) {
					if( ident.name.equals("length") ) {
						return new ArrayLengthAccessExpr(pos,(Expr)o).resolve(reqType);
					}
					else throw new CompilerException(obj.getPos(),"Arrays "+tp+" has only one member 'length'");
				}
				else if( ident.name.equals("$self") && tp.isReference()/*.clazz.equals(Type.tpPrologVar.clazz)*/ )
					return new SelfAccessExpr(pos,(LvalueExpr)o).resolve(reqType);
				else if( tp.isReference() ) cl = tp.clazz;
				else
					throw new CompilerException(obj.getPos(),"Resolved object "+o+" of type "+tp+" is not a scope");
			}
			if( o instanceof Struct && ident.name.equals(nameThis) ) {
				return new OuterThisAccessExpr(pos,(Struct)o).resolve(null);
			}
			ListBuffer<ASTNode> res = new ListBuffer<ASTNode>();
			PVar<ASTNode> v = new PVar<ASTNode>();
			ResInfo info;
			int min_transforms = 8096;
			if( o instanceof Expr && snitps != null && snitps.length > 1) {
				snitps_index = 0;
				while (snitps_index < snitps.length) {
					v.$unbind();
					info = new ResInfo();
					tp = snitps[snitps_index++];
					cl = tp.clazz;
					foreach(cl.resolveNameR(v,info,ident.name,tp, 0) ) {
						if (info.transforms > min_transforms)
							continue;
						ASTNode e = makeExpr(v,info,o,cl);
						if (info.transforms < min_transforms) {
							res.setLength(0);
						}
						res.append(e);
					}
				}
			} else {
					v.$unbind();
					info = new ResInfo();
					foreach(cl.resolveNameR(v,info,ident.name,tp, 0) ) {
						if (info.transforms > min_transforms)
							continue;
						ASTNode e = makeExpr(v,info,o,cl);
						if (info.transforms < min_transforms) {
							res.setLength(0);
						}
						res.append(e);
					}
			}
			if (res.length() == 0) {
				//resolve(reqType);
				throw new CompilerException(pos,"Unresolved identifier "+ident+" in class "+cl+" for type(s) "
					+(snitps==null?tp.toString():Arrays.toString(snitps)) );
			}
			if (res.length() > 1) {
				String msg = "Umbigous identifier "+ident+" in class "+cl+" for type(s) "
					+(snitps==null?tp.toString():Arrays.toString(snitps));
				Dumper dmp = new Dumper();
				dmp.newLine(1);
				foreach (ASTNode r; res.toList()) r.toJava(dmp).newLine();
				dmp.newLine(-1);
				throw new CompilerException(pos,msg+dmp);
			}
			ASTNode n = res.getAt(0);
			if (n instanceof Expr)	return ((Expr)n).resolve(reqType);
			return n;
		} finally { PassInfo.pop(this); }
	}

	private ASTNode makeExpr(ASTNode v, ResInfo info, ASTNode o, BaseStruct cl) {
		if( v instanceof Field ) {
			if( v.isStatic() )
				return new StaticFieldAccessExpr(pos,(Struct)cl,(Field)v);
			if( info.path.length() == 0 ) {
				if( o instanceof BaseStruct )
					throw new CompilerException(pos,"Static access to non-static field "+v);
				return new AccessExpr(pos,(Expr)o,(Field)v);
			} else {
				List<ASTNode> acc = info.path.toList();
				if( o instanceof Struct && acc.head().isStatic() )
					throw new CompilerException(pos,"Static access to non-static field "+v);

				Expr expr;
				expr = new AccessExpr(pos,(Expr)o,(Field)acc.head());
				acc = acc.tail();
				foreach(ASTNode f; acc)
					expr = new AccessExpr(pos,expr,(Field)f);
				return new AccessExpr(pos,expr,(Field)v);
			}
		}
		else if( v instanceof BaseStruct ) {
			return (BaseStruct)v;
		}
		else if( v instanceof Method ) {
			if( v.isStatic() )
				return new CallExpr(pos,parent,(Method)v,Expr.emptyArray);
			if( info.path.length() == 0 ) {
				if( o instanceof Struct )
					throw new CompilerException(pos,"Static access to non-static method "+v);
				return new CallAccessExpr(pos,parent,(Expr)o,(Method)v,Expr.emptyArray);
			} else {
				List<ASTNode> acc = info.path.toList();
				if( o instanceof Struct && acc.head().isStatic() )
					throw new CompilerException(pos,"Static access to non-static method "+v);

				Expr expr;
				expr = new AccessExpr(pos,(Expr)o,(Field)acc.head());
				acc = acc.tail();
				foreach(ASTNode f; info.path)
					expr = new AccessExpr(pos,expr,(Field)f);
				return new CallAccessExpr(pos,parent,expr,(Method)v,Expr.emptyArray);
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
