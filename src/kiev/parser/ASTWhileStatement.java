/* Generated By:JJTree: Do not edit this line. ASTWhileStatement.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTWhileStatement.java,v 1.3 1998/10/26 23:47:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTWhileStatement extends Statement {
    public boolean			not;
	@att public Expr		cond;
    @att public Statement	body;

	public ASTWhileStatement(int id) {
		super(kiev.Kiev.k.getToken(0)==null?0:kiev.Kiev.k.getToken(0).getPos(),null);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0: cond=(Expr)n; break;
        case 1: body=(Statement)n; break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public ASTNode resolve(Type reqType) {
		if (not) {
			ASTOperator op = new ASTOperator(0);
			op.image = KString.from("!");
			ASTExpression e = new ASTExpression(cond.pos);
			e.nodes.append(op);
			e.nodes.append(cond);
			cond = e;
			not = false;
		}
		if( !(cond instanceof BooleanExpr) )
			return new WhileStat(pos,parent,new BooleanWrapperExpr(cond.getPos(),cond),body).resolve(Type.tpVoid);
		return new WhileStat(pos,parent,(BooleanExpr)cond,body).resolve(Type.tpVoid);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("while").space().append('(').space().append(cond)
			.space().append(')').space().append(body).newLine();
		return dmp;
	}
}
