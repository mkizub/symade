/* Generated By:JJTree: Do not edit this line. ASTForEachStatement.java */

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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTForEachStatement.java,v 1.3 1998/10/26 23:47:03 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
@cfnode
public class ASTForEachStatement extends Statement {
	@att public ASTFormalParameter	var;
	@att public Expr				container;
	@att public Expr				cond;
	@att public Statement			body;

	public ASTForEachStatement() {
	}

	public ASTForEachStatement(int id) {
		super(kiev.Kiev.k.getToken(0)==null?0:kiev.Kiev.k.getToken(0).getPos(),null);
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTFormalParameter ) {
			var=(ASTFormalParameter)n;
		}
		else if( n instanceof Expr ) {
			if( container == null ) container = (Expr)n;
			else cond = (Expr)n;
		}
		else if( n instanceof Statement ) {
			body=(Statement)n;
		}
		else
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
    }

	public ASTNode resolve(Type reqType) {
		Var v = null;
		if( var != null )
			v = var.pass3();
		if( cond != null && !(cond instanceof BooleanExpr) )
			return new ForEachStat(pos,parent,v,container,new BooleanWrapperExpr(cond.getPos(),cond),body)
				.resolve(Type.tpVoid);
		return new ForEachStat(pos,parent,v,container,(BooleanExpr)cond,body).resolve(Type.tpVoid);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("foreach").space().append('(');
		if( var != null ) {
			dmp.append(var);
			dmp.append(';').space();
		}
		dmp.append(container);
		if( cond != null ) {
			dmp.append(';').space();
			dmp.append(cond);
		}
		dmp.space().append(')').space();

		dmp.append(body).newLine();
		return dmp;
	}
}
