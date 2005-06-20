/* Generated By:JJTree: Do not edit this line. ASTIdentifier.java */

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
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTIdentifier.java,v 1.4.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.4.1 $
 *
 */

public class ASTIdentifier extends Expr {
	public KString name;

	public ASTIdentifier(int id) {
		super(0);
	}

	public ASTIdentifier(int pos, KString name) {
		super(0);
		this.pos = pos;
		this.name = name;
	}

	public void set(Token t) {
		this.name = new KToken(t).image;
		pos = t.getPos();
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
    }

	public ASTNode resolve(Type reqType) {
		if( name == Constants.nameFILE )
			return new ConstExpr(pos,Kiev.curFile);
		else if( name == Constants.nameLINENO )
			return new ConstExpr(pos,Kiev.newInteger(pos>>>11));
		else if( name == Constants.nameMETHOD ) {
			if( PassInfo.method != null )
				return new ConstExpr(pos,PassInfo.method.name.name);
			else
				return new ConstExpr(pos,nameInit);
		}
		else if( name == Constants.nameDEBUG )
			return new ConstExpr(pos,Kiev.debugOutputA ? Boolean.TRUE : Boolean.FALSE);
		else if( name == Constants.nameReturnVar ) {
			Kiev.reportWarning(pos,"Keyword '$return' is deprecated. Replace with 'Result', please");
			name = Constants.nameResultVar;
		}
		PVar<ASTNode> v = new PVar<ASTNode>();
		ResInfo info = new ResInfo();
		if( !PassInfo.resolveNameR(v,info,name,null,0) ) {
			// May be a function
			if( reqType instanceof MethodType ) {
				Expr[] args = new Expr[reqType.args.length];
				for(int i=0; i < args.length; i++) {
					args[i] = new VarAccessExpr(pos,this,new Var(pos,this,KString.from("arg"+1),reqType.args[i],0));
				}
				if( PassInfo.resolveMethodR(v,null,name,args,((MethodType)reqType).ret,null,0) ) {
//					System.out.println("First-order function "+v);
					ASTAnonymouseClosure ac = new ASTAnonymouseClosure(kiev020TreeConstants.JJTANONYMOUSECLOSURE);
					ac.pos = pos;
					ac.parent = parent;
					ac.type = ((MethodType)reqType).ret;
					ac.params = new ASTNode[args.length];
					for(int i=0; i < ac.params.length; i++)
						ac.params[i] = new Var(pos,KString.from("arg"+(i+1)),reqType.args[i],0);
					BlockStat bs = new BlockStat(pos,ac,ASTNode.emptyArray);
					Expr[] cargs = new Expr[ac.params.length];
					for(int i=0; i < cargs.length; i++)
						cargs[i] = new VarAccessExpr(pos,this,(Var)ac.params[i]);
					args = cargs;
					ASTCallExpression ace = new ASTCallExpression(0);
					ace.ident = new ASTIdentifier(pos,name);
					ace.args = cargs;
					if( ac.type == Type.tpVoid ) {
						bs.addStatement(new ExprStat(pos,bs,ace));
						bs.addStatement(new ReturnStat(pos,bs,null));
					} else {
						bs.addStatement(new ReturnStat(pos,bs,ace));
					}
					ac.body = bs;
					return ac.resolve(reqType);
				}
			}
			if( name.startsWith(Constants.nameDEF) ) {
				String prop = name.toString().substring(2);
				String val = Env.getProperty(prop);
				if( val == null ) val = Env.getProperty(prop.replace('_','.'));
				if( val != null ) {
					if( reqType == null || reqType == Type.tpString)
						return new ConstExpr(pos,KString.from(val));
					if( reqType.isBoolean() )
						if( val == "" )
							return new ConstExpr(pos,Boolean.TRUE);
						else
							return new ConstExpr(pos,Boolean.valueOf(val));
					if( reqType.isInteger() )
						return new ConstExpr(pos,Integer.valueOf(val));
					if( reqType.isNumber() )
						return new ConstExpr(pos,Double.valueOf(val));
					return new ConstExpr(pos,KString.from(val));
				}
				if( reqType.isBoolean() )
					return new ConstExpr(pos,Boolean.FALSE);
				return new ConstExpr(pos,null);
			}
			throw new CompilerException(pos,"Unresolved identifier "+name);
		}
		Expr e = null;
		if( v instanceof Var ) {
			if( v.isLocalRuleVar() )
				e = new LocalPrologVarAccessExpr(pos,this,(Var)v);
			else
				e = new VarAccessExpr(pos,this,(Var)v);
		}
		else if( v instanceof Field ) {
			Field f = (Field)v;
			if( f.isStatic() ) return new StaticFieldAccessExpr(pos,PassInfo.clazz,f).resolve(reqType);
			if( info.path.length() == 0 )
				e = new FieldAccessExpr(pos,f);
			else {
				List<ASTNode> acc = info.path.toList();
				if (acc.head() instanceof Field)
					e = new FieldAccessExpr(pos,(Field)acc.head());
				else if (acc.head() instanceof Var)
					e = new VarAccessExpr(pos,(Var)acc.head());
				acc = acc.tail();
				foreach(ASTNode n; acc) {
					e = new AccessExpr(pos,this,e,(Field)n);
				}
				e = new AccessExpr(pos,this,e,(Field)f);
			}
		}
		else if( v instanceof Struct ) {
			if( reqType != null && reqType.equals(Type.tpInt) ) {
				Struct s = (Struct)v;
				if( s.isPizzaCase() ) {
					PizzaCaseAttr case_attr = (PizzaCaseAttr)s.getAttr(attrPizzaCase);
					if( case_attr == null ) return s;
					return new ConstExpr(pos,Kiev.newInteger(case_attr.caseno)).resolve(reqType);
				}
			}
			return v;
		}
		else if( v instanceof Type ) {
			return ((Type)v).clazz;
		} else {
			throw new CompilerException(pos,"Identifier "+name+" must be local var, class's field or class name");
		}
		e.parent = parent;
		return e.resolve(reqType);
	}

	public int		getPriority() { return 256; }

	public String toString() {
		return name.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}
}
