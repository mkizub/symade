/* Generated By:JJTree: Do not edit this line. ASTBlock.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTBlock.java,v 1.3 1998/10/26 23:47:02 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTBlock extends Statement {
	public ASTNode[] stats = ASTNode.emptyArray;

	public ASTBlock(int id) {
		super(0,null);
	}

	public void jjtAddChild(ASTNode n, int i)
	{
		stats = (ASTNode[])Arrays.append(stats,n);
	}

	public ASTNode resolve(Type reqType) {
		BlockStat bs = new BlockStat(pos,parent,stats);
		if( parent instanceof Method )
			((Method)parent).body = bs;
		return bs.resolve(reqType);
	}

    public Dumper toJava(Dumper dmp) {
		dmp.space().append('{').newLine(1);
		for(int i=0; i < stats.length; i++)
			stats[i].toJava(dmp).newLine();
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

}
