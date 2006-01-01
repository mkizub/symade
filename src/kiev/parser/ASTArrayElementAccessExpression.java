/* Generated By:JJTree: Do not edit this line. ASTArrayElementAccessExpression.java */

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

import kiev.*;
import kiev.stdlib.*;
import kiev.vlang.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTArrayElementAccessExpression.java,v 1.3 1998/10/26 23:47:01 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTArrayElementAccessExpression extends Expr {
	@att public Expr		obj;
	@att public Expr		index;

	public ASTArrayElementAccessExpression() {}

	public ASTArrayElementAccessExpression(int id) {
		super(kiev.Kiev.k.getToken(0)==null?0:kiev.Kiev.k.getToken(0).getPos());
	}

	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0:	obj = (Expr)n; break;
		case 1:	index = (Expr)n; break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public ASTNode resolve(Type reqType) {
		return new ContainerAccessExpr(pos,obj,index).resolve(reqType);
	}
    
	public int		getPriority() { return Constants.opContainerElementPriority; }

    public String toString() {
    	return obj+"["+index+']';
    }
    
    public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('[').append(index).append(']');
        return dmp;
    }
}