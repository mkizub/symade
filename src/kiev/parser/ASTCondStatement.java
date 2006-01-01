/* Generated By:JJTree: Do not edit this line. ASTCondStatement.java */

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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTCondStatement.java,v 1.3.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

@node
public class ASTCondStatement extends Statement {
	
	@att public Expr		cond;
	@att public Expr		message;
	
	public ASTCondStatement() {
	}

	public ASTCondStatement(int id) {
		this();
	}

	public void jjtAddChild(ASTNode n, int i)
	{
		if( i==0 && n instanceof ASTExpression )
			cond = (ASTExpression)n;
		else if( i==1 && n instanceof Expr )
			message = (Expr)n;
		else
			throw new CompilerException(pos,"Bad child node "+n.getClass());
	}

	public ASTNode resolve(Type reqType) {
		if( cond instanceof BooleanExpr )
			return new CondStat(pos,parent,(BooleanExpr)cond,message).resolve(Type.tpVoid);
		else
			return new CondStat(pos,parent,new BooleanWrapperExpr(cond.getPos(),cond),message).resolve(Type.tpVoid);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("if( !(").append(cond)
			.append(") ) throw new kiev.stdlib.AssertionFailedException(")
			.append(message).append(");").newLine();
		return dmp;
	}
}