/* Generated By:JJTree: Do not edit this line. ASTDoStatement.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTDoStatement.java,v 1.3 1998/10/26 23:47:02 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public class ASTDoStatement extends Statement {
	public Expr			cond;
    public Statement	body;

	public ASTDoStatement(int id) {
		super(kiev.Kiev.k.getToken(0)==null?0:kiev.Kiev.k.getToken(0).getPos(),null);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0: body=(Statement)n; break;
        case 1: cond=(Expr)n; break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public ASTNode resolve(Type reqType) {
		if( !(cond instanceof BooleanExpr) )
			return new DoWhileStat(pos,parent,new BooleanWrapperExpr(cond.getPos(),cond),body).resolve(Type.tpVoid);
		return new DoWhileStat(pos,parent,(BooleanExpr)cond,body).resolve(Type.tpVoid);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("do").space().append(body).newLine().append("while")
			.space().append('(').space().append(cond).space().append(");").newLine();
		return dmp;
	}
}
