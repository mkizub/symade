/* Generated By:JJTree: Do not edit this line. ASTFieldDecl.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/Attic/ASTFieldDecl.java,v 1.1.2.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.1.2.1 $
 *
 */

public class ASTFieldDecl extends ASTNode {

	public ASTNode[]	modifier = ASTNode.emptyArray;
	public ASTPack		pack;
	public ASTAccess	acc;
	public ASTNode		type;
	public ASTNode[]	vars = ASTNode.emptyArray;

	public ASTFieldDecl(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
		switch( n ) {
		case ASTModifier:
			modifier = (ASTNode[])Arrays.append(modifier,n);
			break;
		case ASTPack:
			if( pack != null )
				throw new CompilerException(n.getPos(),"Duplicate 'packed' specified");
			pack = (ASTPack)n;
			break;
		case ASTAccess:
			if( acc != null )
				throw new CompilerException(n.getPos(),"Duplicate 'access' specified");
			acc = (ASTAccess)n;
			break;
		case ASTType:
			type = n;
			break;
		case ASTVarDecl:
			if( vars.length == 0 ) pos = n.pos;
			vars = (ASTNode[])Arrays.append(vars,n);
			break;
        default:
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public ASTNode resolve(Type reqType) {
		return null;
	}
    
    public Dumper toJava(Dumper dmp) {
    	for(int i=0; i < modifier.length; i++)
        	modifier[i].toJava(dmp);
		type.toJava(dmp).space();
    	for(int i=0; i < vars.length; i++) {
        	vars[i].toJava(dmp);
            if( i < vars.length - 1 ) dmp.append(',').space();
        }
		return dmp;
    }
}
