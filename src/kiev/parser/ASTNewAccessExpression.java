/* Generated By:JJTree: Do not edit this line. ASTNewAccessExpression.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTNewAccessExpression.java,v 1.3.4.1 1999/02/15 21:45:09 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

public class ASTNewAccessExpression extends ASTNode {
	public ASTExpr		obj;
	public ASTNode		type;
    public ASTExpr[]	args = ASTExpr.emptyArray;

	public ASTNewAccessExpression(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if(i==0) {
			obj=(ASTExpr)n;
		} else {
			type = ((ASTNewExpression)n).type;
			args = ((ASTNewExpression)n).args;
            pos = n.getPos();
        }
    }

	public Node resolve(Type reqType) {
    	for(int i=0; i < args.length; i++) {
        	try {
            	args[i] = (Expr)args[i].resolve(null);
            } catch(Exception e) {
            	Kiev.reportError(pos,e);
            }
        }
		return new NewExpr(pos,((ASTNonArrayType)type).pass2(),args,obj).resolve(reqType);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('.').append("new").space().append(type).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		return dmp.append(')');
	}
}
