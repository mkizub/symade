/* Generated By:JJTree: Do not edit this line. ASTRuleIsoneofExpression.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTRuleIsoneofExpression.java,v 1.3.4.1 1999/02/15 21:45:09 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

public class ASTRuleIsoneofExpression extends ASTRuleNode {

	public ASTIdentifier[]	names = new ASTIdentifier[0];
	public Expr[]			exprs = Expr.emptyArray;

	public ASTRuleIsoneofExpression(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	switch(i % 2) {
        case 0:
        	if( n instanceof ASTIdentifier ) {
	        	names = (ASTIdentifier[])Arrays.append(names,n);
	        	if( i == 0 ) setPos(n.getPos());
	        	break;
	        }
	        throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        case 1:
        	if( n instanceof Expr ) {
	        	exprs = (Expr[])Arrays.append(exprs,n);
	        	break;
	        }
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
		}
    }

    public ASTNode resolve(Type reqType) {
    	Var[] vars = new Var[names.length];
    	for(int i=0; i < vars.length; i++ ) {
			PVar<ASTNode> v = new PVar<ASTNode>();
			if( !PassInfo.resolveNameR(v,null,names[i].name,null,0) )
				throw new CompilerException(pos,"Unresolved identifier "+names[i].name);
			if( !(v instanceof Var) )
	    		throw new CompilerException(names[i].getPos(),"Identifier is not a var");
			vars[i] = (Var)v;
			exprs[i] = (Expr)exprs[i].resolve(null);
		}
    	return new RuleIsoneofExpr(getPos(),vars,exprs);
    }

	public void	createText(StringBuffer sb) { throw new CompilerException(pos,"Internal error"); }
	public void	resolve1(JumpNodes jn) { throw new CompilerException(pos,"Internal error"); }

}

