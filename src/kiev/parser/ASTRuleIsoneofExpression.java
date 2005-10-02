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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class ASTRuleIsoneofExpression extends ASTRuleNode {

	@att
	public final NArr<ASTIdentifier>	names;
	
	@att
	@dflow(in="", seq="false")
	public final NArr<ENode>			exprs;

	public boolean preResolve() {
		PassInfo.push(this);
		try {
			// don't pre-resolve 'names'
			foreach (ENode e; exprs) e.preResolve();
		} finally { PassInfo.pop(this); }
		return false;
	}
	
    public void resolve(Type reqType) {
    	Var[] vars = new Var[names.length];
    	for(int i=0; i < vars.length; i++ ) {
			ASTNode@ v;
			if( !PassInfo.resolveNameR(v,new ResInfo(),names[i].name) )
				throw new CompilerException(pos,"Unresolved identifier "+names[i].name);
			if( !(v instanceof Var) )
	    		throw new CompilerException(names[i].getPos(),"Identifier is not a var");
			vars[i] = (Var)v;
			exprs[i].resolve(null);
		}
    	replaceWithNode(new RuleIsoneofExpr(getPos(),vars,exprs.toArray()));
    }

	public void	createText(StringBuffer sb) { throw new CompilerException(pos,"Internal error"); }
	public void	resolve1(JumpNodes jn) { throw new CompilerException(pos,"Internal error"); }

}

