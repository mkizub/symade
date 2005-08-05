/* Generated By:JJTree: Do not edit this line. ASTIfStatement.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTIfStatement.java,v 1.3 1998/10/26 23:47:03 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
@cfnode
public class ASTIfStatement extends Statement {
    public boolean		not;
	@att public Expr		cond;
    @att public Statement	thenSt;
    @att public Statement	elseSt;

	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0: cond=(Expr)n; break;
        case 1: thenSt=(Statement)n; break;
        case 2: elseSt=(Statement)n; break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public ASTNode resolve(Type reqType) {
		if (not) {
			ASTOperator op = new ASTOperator();
			op.image = KString.from("!");
			ASTExpression e = new ASTExpression();
			e.pos = cond.pos;
			e.nodes.append(op);
			e.nodes.append(cond);
			cond = e;
			not = false;
		}
		if( cond instanceof BooleanExpr )
			return new IfElseStat(pos,parent,(BooleanExpr)cond,thenSt,elseSt).resolve(Type.tpVoid);
		else
			return new IfElseStat(pos,parent,new BooleanWrapperExpr(cond.getPos(),cond),thenSt,elseSt).resolve(Type.tpVoid);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("if(").space().append(cond).space()
			.append(')').space().append(thenSt).newLine();
		if( elseSt != null )
			dmp.append("else").space().append(elseSt).newLine();
		return dmp;
	}
}
