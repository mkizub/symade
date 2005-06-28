/* Generated By:JJTree: Do not edit this line. ASTEnumFieldDeclaration.java */

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

/**
 * @author Maxim Kizub
 *
 */

public class ASTEnumFieldDeclaration extends ASTNode {

	public ASTModifiers modifiers;
	public ASTIdentifier name;
	public ASTConstExpression val;
	public ASTConstExpression text;
	
	public ASTEnumFieldDeclaration(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTModifiers) {
			modifiers = (ASTModifiers)n;
		}
        else if( n instanceof ASTIdentifier ) {
			name = (ASTIdentifier)n;
			pos = n.getPos();
		}
        else if( n instanceof ASTConstExpression ) {
			ASTConstExpression ce = (ASTConstExpression)n;
			if (n.val instanceof KString) {
				text = (ASTConstExpression)n;
			}
			else {
				val = (ASTConstExpression)n; 
			}
		}
		else {
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n+" ("+n.getClass()+")");
		}
    }

}
