/* Generated By:JJTree: Do not edit this line. ASTRuleIstheExpression.java */

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
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
@cfnode
public class ASTRuleIstheExpression extends ASTRuleNode {

	@att public ASTIdentifier	name;
	@att public ENode			expr;

	public void preResolve() {
		PassInfo.push(this);
		try {
			// don't pre-resolve 'name'
			expr.preResolve();
		} finally { PassInfo.pop(this); }
	}
	
    public void resolve(Type reqType) {
		ASTNode@ v;
		if( !PassInfo.resolveNameR(v,new ResInfo(),name.name) )
			throw new CompilerException(pos,"Unresolved identifier "+name.name);
		if( !(v instanceof Var) )
    		throw new CompilerException(name.getPos(),"Identifier is not a var");
		expr.resolve(((Var)v).type.args[0]);
    	replaceWithNode(new RuleIstheExpr(getPos(), (Var)v, expr));
    }

	public void	createText(StringBuffer sb) { throw new CompilerException(name.getPos(),"Internal error"); }
	public void	resolve1(JumpNodes jn) { throw new CompilerException(name.getPos(),"Internal error"); }

}
