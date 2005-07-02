/* Generated By:JJTree: Do not edit this line. ASTCastOperator.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTCastOperator.java,v 1.3 1998/10/26 23:47:02 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTCastOperator extends ASTNode {

	@ref public ASTNode	type;
	public boolean  reinterp;

	public ASTCastOperator(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0: type=n; break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public Operator resolveOperator() {
		Type tp = ((ASTType)type).getType();
	    return CastOperator.newCastOperator(tp,reinterp);
	}

	public int		getPriority() { return Constants.opCastPriority; }

	public String toString() { return (reinterp?"($reinterp ":"($cast ")+type+")"; }

    public Dumper toJava(Dumper dmp) {
    	dmp.append("(").append(type).append(')');
        return dmp;
    }

}
