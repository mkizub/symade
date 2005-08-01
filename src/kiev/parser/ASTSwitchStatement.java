/* Generated By:JJTree: Do not edit this line. ASTSwitchStatement.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTSwitchStatement.java,v 1.3 1998/10/26 23:47:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTSwitchStatement extends Statement {
	@att public Expr					sel;
	@att public final NArr<ASTNode>		cases;

    public ASTSwitchStatement() {
		cases = new NArr<ASTNode>(this, new AttrSlot("cases", true, true));
	}

    public ASTSwitchStatement(int id) {
		super(kiev.Kiev.k.getToken(0)==null?0:kiev.Kiev.k.getToken(0).getPos(),null);
		cases = new NArr<ASTNode>(this, new AttrSlot("cases", true, true));
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if( i==0 ) sel = (Expr)n;
        else {
        	cases.append(n);
        }
    }
    
    public ASTNode resolve(Type reqType) {
		return new SwitchStat(pos,parent,sel,cases.toArray()).resolve(Type.tpVoid);
    }

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("switch").space().append('(')
			.append(sel).space().append(')').space().append('{').newLine(1);
		for(int i=0; i < cases.length; i++) dmp.append(cases[i]);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

}
