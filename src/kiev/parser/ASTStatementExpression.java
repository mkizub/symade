/* Generated By:JJTree: Do not edit this line. ASTStatementExpression.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTStatementExpression.java,v 1.3 1998/10/26 23:47:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTStatementExpression extends Statement {
	public Expr	expr;
    
	public ASTStatementExpression(int id) {
		super(id,null);
	}

	public ASTStatementExpression(Expr e) {
		super(0,null);
		expr = e;
		this.pos = e.getPos();
	}

	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0: expr=(Expr)n; pos = n.getPos(); break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public ASTNode resolve(Type reqType) {
		return new ExprStat(pos,parent,expr).resolve(Type.tpVoid);
	}

	public Dumper toJava(Dumper dmp) {
		return expr.toJava(dmp).append(';');
	}
}
