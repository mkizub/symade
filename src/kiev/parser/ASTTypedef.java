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

public class ASTTypedef extends SimpleNode implements TopLevelDecl {
	KString	name;
	ASTNode	type;
	Typedef td;
	boolean opdef = false;

	ASTTypedef(int id) {
		super(0);
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

	public ASTNode pass1_1() {
		if (opdef) {
			ASTQName qn = (ASTQName)type;
			PVar<ASTNode> v = new PVar<ASTNode>();
			if( !PassInfo.resolveNameR(v,new PVar<List<ASTNode>>(List.Nil),qn.toKString(),null,0) )
				throw new CompilerException(pos,"Unresolved identifier "+qn.toKString());
			if( !(v.$var instanceof Struct) )
				throw new CompilerException(qn.getPos(),"Type name "+qn.toKString()+" is not a structure, but "+v.$var);
			Struct s = (Struct)v.$var;
			if (s.type.args.length != 1)
				throw new CompilerException(qn.getPos(),"Type "+s.type+" must have 1 argument");
			return td = new Typedef(pos,parent,name,s.type);
		} else {
			type = ((ASTType)type).pass2();
			return td = new Typedef(pos,parent,name,(Type)type);
		}
	}

	public ASTNode pass2() {
		if (td != null) return td;
		return pass1_1();
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
