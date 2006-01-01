/* Generated By:JJTree: Do not edit this line. ASTInitializer.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTInitializer.java,v 1.3 1998/10/26 23:47:03 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTInitializer extends ASTNode implements PreScanneable {
	@att public ASTModifiers	modifiers;
	@att public Statement	body;
	public virtual PrescannedBody pbody;
    
	public PrescannedBody get$pbody() { return pbody; }
	public void set$pbody(PrescannedBody p) { pbody = p; }
	
	public ASTInitializer() {
	}
    
	public ASTInitializer(int id) {
	}
    
	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTModifiers) {
			modifiers = (ASTModifiers)n;
		}
        else if( n instanceof Statement ) {
			body = (Statement)n;
            pos = n.getPos();
		}
        else {
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public Dumper toJava(Dumper dmp) {
		modifiers.toJava(dmp);
		body.toJava(dmp).newLine();
		return dmp;
	}
}
