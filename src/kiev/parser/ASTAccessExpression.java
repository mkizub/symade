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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTAccessExpression.java,v 1.3.2.1 1999/02/12 18:47:02 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1 $
 *
 */

public class ASTAccessExpression extends Expr {
	public Expr		obj;
	public KString	name;
	public boolean  in_wrapper;

	public ASTAccessExpression(int id) {
		super(kiev.Kiev.k.getToken(0)==null?0:kiev.Kiev.k.getToken(0).getPos());
	}

	public ASTAccessExpression(int pos,Expr obj, KString name) {
		super(pos);
		this.obj = obj;
		this.name = name;
		this.obj.parent = this;
	}

	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0:	obj = (Expr)n; break;
		case 1:	name = ((ASTIdentifier)n).name; break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public ASTNode resolve(Type reqType) throws CompilerException {
		PassInfo.push(this);
		try {
			ASTNode o = obj.resolve(null);
			if( o == null ) throw new CompilerException(obj.getPos(),"Unresolved object "+obj);
			Struct cl;
			Type tp = null;
			Type[] snitps = null;
			int snitps_index = 0;
			if( o instanceof Struct ) cl = (Struct)o;
			else {
				obj = (Expr)o;
				snitps = ((Expr)o).getAccessTypes();
				tp = snitps[snitps_index++];
				if (in_wrapper) {
					if (!tp.clazz.isWrapper())
						throw new CompilerException(obj.getPos(),"Class "+tp+" is not a wrapper");
				}
				else if (tp.clazz.isWrapper() && name.byteAt(0) != '$') {
					o = (Expr)new AccessExpr(obj.pos,obj,tp.clazz.wrapped_field).resolve(null);
					tp = o.getType();
				}
				if( tp.isArray() ) {
					if( name.equals("length") ) {
						return new ArrayLengthAccessExpr(pos,(Expr)o).resolve(reqType);
					}
					else throw new CompilerException(obj.getPos(),"Arrays "+tp+" has only one member 'length'");
				}
				else if( name.equals("$self") && tp.isReference()/*.clazz.equals(Type.tpPrologVar.clazz)*/ )
					return new SelfAccessExpr(pos,(LvalueExpr)o).resolve(reqType);
				else if( tp.isReference() ) cl = (Struct)tp.clazz;
				else
					throw new CompilerException(obj.getPos(),"Resolved object "+o+" of type "+tp+" is not a scope");
			}
			if( o instanceof Struct && name.equals(nameThis) ) {
				return new OuterThisAccessExpr(pos,(Struct)o).resolve(null);
			}
	retry_resolving:;
			PVar<ASTNode> v = new PVar<ASTNode>();
			PVar<List<ASTNode>> path = new PVar<List<ASTNode>>(List.Nil);
			if( !cl.resolveNameR(v,path,name,tp, in_wrapper? ResolveFlags.NoForwards : 0) ) {
				if( o instanceof Expr && snitps != null ) {
					if( snitps_index < snitps.length ) {
						tp = snitps[snitps_index++];
						cl = (Struct)tp.clazz;
						goto retry_resolving;
					}
				}
				throw new CompilerException(pos,"Unresolved identifier "+name+" in class "+cl+" for type "
					+(snitps==null?tp.toString():Arrays.toString(snitps)) );
			}
			if( v.$var instanceof Field ) {
				if( ((Field)v.$var).isStatic() ) {
					return new StaticFieldAccessExpr(pos,cl,(Field)v.$var).resolve(reqType);
				} else {
					if( path.$var == List.Nil ) {
						if( o instanceof Struct )
							throw new CompilerException(pos,"Static access to non-static field "+v.$var);
						return new AccessExpr(pos,(Expr)o,(Field)v.$var).resolve(reqType);
					} else {
						if( o instanceof Struct && path.$var.head().isStatic() )
							throw new CompilerException(pos,"Static access to non-static field "+v.$var);

						Expr expr;
						expr = new AccessExpr(pos,(Expr)o,(Field)path.head());
						expr.parent = parent;
						path.$var = path.$var.tail();
						foreach(ASTNode n; path.$var)
							expr = new AccessExpr(pos,expr,(Field)n);
						return new AccessExpr(pos,expr,(Field)v.$var).resolve(reqType);
					}
				}
			}
			else if( v.$var instanceof Struct ) {
				return (Struct)v.$var;
			}
			else if( v.$var instanceof Method ) {
				return new CallAccessExpr(pos,parent,(Expr)o,(Method)v.$var,Expr.emptyArray).resolve(reqType);
			} else {
				throw new CompilerException(pos,"Identifier "+name+" must be a class's field");
			}
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
    	return obj+"."+name;
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('.').append(name);
		return dmp;
	}
}
