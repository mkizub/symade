/* Generated By:JJTree: Do not edit this line. ASTForStatement.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTForStatement.java,v 1.3 1998/10/26 23:47:03 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public class ASTForStatement extends Statement {
	public ASTNode	init;
	public Expr		cond;
	public Expr		iter;
	public Statement	body;

	public ASTForStatement(int id) {
		super(kiev.Kiev.k.getToken(0)==null?0:kiev.Kiev.k.getToken(0).getPos(),null);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0: body=(Statement)n; break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public ASTNode resolve(Type reqType) {
		if( cond != null && !(cond instanceof BooleanExpr) )
			return new ForStat(pos,parent,init,new BooleanWrapperExpr(cond.getPos(),cond),iter,body)
				.resolve(Type.tpVoid);
		return new ForStat(pos,parent,init,(BooleanExpr)cond,iter,body).resolve(Type.tpVoid);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("for").space().append('(');
		if( init != null ) dmp.append(init);
		dmp.append(';');

		if( cond != null ) dmp.append(cond);
		dmp.append(';');

		if( iter != null ) dmp.append(iter);
		dmp.space().append(')').space();

		dmp.append(body).newLine();
		return dmp;
	}
}
