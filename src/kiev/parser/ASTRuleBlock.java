/* Generated By:JJTree: Do not edit this line. ASTRuleBlock.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTRuleBlock.java,v 1.3 1998/10/26 23:47:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public class ASTRuleBlock extends ASTBlock {

	ASTRuleNode	expr;

	public ASTRuleBlock(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i)
	{
		if( n instanceof ASTRuleNode )
			expr = (ASTRuleNode)n;
		else
			throw new CompilerException(pos,"Bad child node "+n.getClass());
	}

	public ASTNode resolve(Type reqType) {
		RuleBlock rb = new RuleBlock(pos,parent,expr,stats);
		return rb.resolve(reqType);
	}

    public Dumper toJava(Dumper dmp) {
		dmp.space().append('{').newLine(1);
		for(int i=0; i < stats.length; i++)
			stats[i].toJava(dmp).newLine();
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}
