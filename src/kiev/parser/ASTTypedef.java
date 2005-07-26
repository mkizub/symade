/* Generated By:JJTree: Do not edit this line. ASTTypedef.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTTypedef.java,v 1.3 1998/10/26 23:47:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTTypedef extends SimpleNode implements TopLevelDecl {
	public KString	name;
	@att public ASTNode	type;
	@ref public Typedef td;
	public boolean opdef = false;

	ASTTypedef() {
	}

	ASTTypedef(int id) {
	}

	public void jjtAddChild(ASTNode n, int i) {
		switch(i) {
    	case 0:
    		if (!opdef)
				type = (ASTType)n;
			else
				name = ((ASTOperator)n).image;
			pos = n.getPos();
    		break;
    	case 1:
    		if (!opdef)
    			name = ((ASTIdentifier)n).name;
    		else
    			type = (ASTQName)n;
    		break;
    	default:
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public String toString() {
		if (opdef)
			return "typedef type"+name+" "+type+"<type>;";
		else
    		return "typedef "+type+" "+name+";";
    }

	public Dumper toJava(Dumper dmp) {
    	return dmp.append("/* ").append(toString()).append(" */").newLine();
    }
}
