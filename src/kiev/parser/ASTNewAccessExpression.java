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

@node
public class ASTNewAccessExpression extends Expr {
	@att public Expr				obj;
	@att public ASTNonArrayType		type;
	@att public final NArr<Expr>	args;

	public ASTNewAccessExpression() {
	}

	public ASTNewAccessExpression(int id) {
	}
	
	public void jjtAddChild(ASTNode n, int i) {
    	if(i==0) {
			obj=(Expr)n;
		} else {
			ASTNewExpression ne = (ASTNewExpression)n;
			type = ne.type;
			foreach (Expr e; ne.args)
				args.add(e);
            pos = n.getPos();
        }
    }

	public ASTNode resolve(Type reqType) {
    	for(int i=0; i < args.length; i++) {
        	try {
            	args[i] = (Expr)args[i].resolve(null);
            } catch(Exception e) {
            	Kiev.reportError(pos,e);
            }
        }
		return new NewExpr(pos,type.getType(),args.toArray(),obj).resolve(reqType);
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
