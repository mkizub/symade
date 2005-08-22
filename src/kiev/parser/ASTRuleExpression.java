/* Generated By:JJTree: Do not edit this line. ASTRuleExpression.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTRuleExpression.java,v 1.3 1998/10/26 23:47:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTRuleExpression extends ASTRuleNode {

	@att public Expr	expr;
	@att public Expr	bt_expr;
	boolean				while_mode;

    public void resolve(Type reqType) {
    	expr.resolve(null);
    	if (bt_expr != null) bt_expr.resolve(null);
    	if (while_mode)
   			replaceWithResolve(new RuleWhileExpr(expr,bt_expr), null);
   		else
    		replaceWithResolve(new RuleExpr(expr,bt_expr), null);
    }

	public void	createText(StringBuffer sb) { throw new CompilerException(getPos(),"Internal error"); }
	public void	resolve1(JumpNodes jn) { throw new CompilerException(getPos(),"Internal error"); }

}
