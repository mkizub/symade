/* Generated By:JJTree: Do not edit this line. ASTType.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTType.java,v 1.3 1998/10/26 23:47:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTType extends ASTNode {

	public static ASTType[]	emptyArray = new ASTType[0];

	public int			dim;
    @ref public ASTNode	type;
	
	ASTType(int id) {
		super(0);
	}

	ASTType(int pos, int dim, ASTNode type) {
		super(pos);
		this.dim = dim;
		this.type = type;
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( i==0 ) {
			type = n;
            pos = n.getPos();
		}
        else {
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }
    
	public Type getType() {
		if( type instanceof Type )
			return (Type)type;
		Type tp = Type.tpVoid;
		if( type instanceof ASTNonArrayType ) {
			try {
				type = tp = ((ASTNonArrayType)type).getType();
				for(int i=0; i < dim; i++)
					tp = Type.newArrayType(tp);
			} catch(Exception e ) {
				Kiev.reportError(type.getPos(),e);
			}
		}
		else if( type instanceof ASTClosureType ) {
			type = tp = ((ASTClosureType)type).getType();
		} else {
			throw new CompilerException(type.getPos(),"Bad type node "+type);
		}
		return tp;
	}
	
	public String toString() {
		String s = type.toString();
    	for(int i=0; i < dim; i++) s += "[]";
        return s;
	}

	public Dumper toJava(Dumper dmp) {
		type.toJava(dmp);
    	for(int i=0; i < dim; i++) dmp.append("[]");
        return dmp.space();
	}
}

