/* Generated By:JJTree: Do not edit this line. ASTRequareDeclaration.java */

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

import static kiev.stdlib.Debug.*;
import static kiev.vlang.WorkByContractCondition.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTRequareDeclaration.java,v 1.3.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

@node
public abstract class ASTCondDeclaration extends ASTNode implements PreScanneable {
	public KString		name;
    @att public Statement	body;
	@virtual
	@att public virtual PrescannedBody pbody;

	@getter public PrescannedBody get$pbody() { return pbody; }
	@setter public void set$pbody(PrescannedBody p) { pbody = p; }

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTIdentifier ) {
        	name = ((ASTIdentifier)n).name;
        	pos = n.getPos();
		}
		else if( n instanceof ASTBlock ) {
			body = (Statement)n;
			if( pos == 0 ) pos = n.pos;
        }
        else {
        	throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

    public abstract ASTNode pass3();

}

@node
public class ASTRequareDeclaration extends ASTCondDeclaration {

    public ASTNode pass3() {
		WorkByContractCondition cond = new WorkByContractCondition(pos,CondRequire,name,body,pbody);
		return cond;
    }
}

