/* Generated By:JJTree: Do not edit this line. ASTNonArrayType.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTNonArrayType.java,v 1.3 1998/10/26 23:47:04 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public class ASTNonArrayType extends SimpleNode {
	static private KString[] noops = new KString[0];
	public KString[] ops = noops;

	public ASTNonArrayType(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if( i==0 ) pos = n.getPos();
        super.jjtAddChild(n,i);
    }

    public void addOperation(Token t) {
    	ops = (KString[])Arrays.append(ops,KString.from(t.image));
    }

	public Type pass2() {
	    Type tp= null;
		if( children[0] instanceof ASTPrimitiveType ) {
			tp = ((ASTPrimitiveType)children[0]).type;
		} else {
    		ASTQName qn = (ASTQName)children[0];
	    	PVar<ASTNode> v = new PVar<ASTNode>();
		    if( !PassInfo.resolveNameR(v,new PVar<List<ASTNode>>(List.Nil),qn.toKString(),null,0) )
			    throw new CompilerException(pos,"Unresolved identifier "+qn.toKString());
    		if( v.$var instanceof Type ) {
    		    tp = (Type)v.$var;
    		} else {
        		if( !(v.$var instanceof Struct) )
		        	throw new CompilerException(qn.getPos(),"Type name "+qn.toKString()+" is not a structure, but "+v.$var);
        		Type[] atypes = new Type[children.length-1];
		        for(int i=0; i < atypes.length; i++) {
        			atypes[i] = ((ASTType)children[i+1]).pass2();
		        }
		        tp = Type.newRefType((Struct)v.$var,atypes);
		    }
		}
		for (int i=0; i < ops.length; i++) {
			PVar<ASTNode> v = new PVar<ASTNode>();
			if (!PassInfo.resolveNameR(v,new PVar<List<ASTNode>>(List.Nil),ops[i],null,0)) {
				if (ops[i] == KString.from("@"))
					v.$var = Type.tpPrologVar;
				else if (ops[i] == KString.from("&"))
					v.$var = Type.tpRefProxy;
				else
					throw new CompilerException(pos,"Typedef for type operator "+ops[i]+" not found");
			}
			if (!(v.$var instanceof Type))
				throw new CompilerException(pos,"Expected to find type for "+ops[i]+", but found "+v);
			Type t = (Type)v.$var;
			if (t.args.length != 1)
				throw new CompilerException(pos,"Type '"+t+"' of type operator "+ops[i]+" must have 1 argument");
			Env.getStruct(t.clazz.name);
			tp = Type.newRefType(t.clazz,new Type[]{tp});
		}
		return tp;
	}

	public String toString() {
		if( children.length == 1 ) return String.valueOf(children[0]);
		StringBuffer sb = new StringBuffer();
		sb.append(children[0]).append('<');
		for(int i=1; i < children.length; i++) {
			sb.append(children[i]);
			if( i < children.length-1 ) sb.append(',');
		}
		return sb.append('>').toString();
	}
}
