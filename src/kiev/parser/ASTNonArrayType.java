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

import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTNonArrayType.java,v 1.3 1998/10/26 23:47:04 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTNonArrayType extends ASTType {
	private static KString opArray = KString.from("[");
	private static KString opPVar  = KString.from("@");
	private static KString opRef   = KString.from("&");
	
	static private KString[] noops = new KString[0];
	
	@att public final NArr<ASTNode>		children;
	public KString[] ops = noops;
	

	public ASTNonArrayType() {
	}

	public ASTNonArrayType(KString nm) {
		children.append(new ASTIdentifier(nm));
	}

	public ASTNonArrayType(ASTIdentifier nm) {
		pos = nm.getPos();
		children.append(nm);
	}

	public ASTNonArrayType(ASTPrimitiveType tp) {
		pos = tp.getPos();
		children.append(tp);
	}

    public void addOperation(Token t) {
    	ops = (KString[])Arrays.append(ops,KString.from(t.image));
    }

	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
	    Type tp = null;
		if( children[0] instanceof ASTPrimitiveType ) {
			tp = (Type)(ASTPrimitiveType)children[0];
		} else {
    		KString nm;
			nm = ((ASTIdentifier)children[0]).name;
	    	ASTNode@ v;
		    if( !PassInfo.resolveNameR(v,new ResInfo(),nm,null) )
			    throw new CompilerException(pos,"Unresolved identifier "+nm);
    		if( v instanceof Type ) {
    		    tp = (Type)v;
    		} else {
        		if( !(v instanceof BaseStruct) )
		        	throw new CompilerException(pos,"Type name "+nm+" is not a structure, but "+v);
				BaseStruct s = (BaseStruct)v;
				Type[] atypes = new Type[children.length-1];
				for(int i=0; i < atypes.length; i++) {
					ASTNode ct = children[i+1];
					atypes[i] = ct.getType();
				}
				tp = Type.newRefType(s,atypes);
		    }
		}
		for (int i=0; i < ops.length; i++) {
			ASTNode@ v;
			if (ops[i] == opArray) {
				tp = Type.newArrayType(tp);
			} else {
				if (!PassInfo.resolveNameR(v,new ResInfo(),ops[i],null)) {
					if (ops[i] == opPVar) {
						Kiev.reportWarning(pos, "Typedef for "+ops[i]+" not found, assuming "+Type.tpPrologVar);
						v = Type.tpPrologVar;
					}
					else if (ops[i] == KString.from("&")) {
						Kiev.reportWarning(pos, "Typedef for "+ops[i]+" not found, assuming "+Type.tpRefProxy);
						v = Type.tpRefProxy;
					}
					else
						throw new CompilerException(pos,"Typedef for type operator "+ops[i]+" not found");
				}
				if (v instanceof TypeRef)
					v = ((TypeRef)v).getType();
				if !(v instanceof Type)
					throw new CompilerException(pos,"Expected to find type for "+ops[i]+", but found "+v);
				Type t = (Type)v;
				if (t.args.length != 1)
					throw new CompilerException(pos,"Type '"+t+"' of type operator "+ops[i]+" must have 1 argument");
				Env.getStruct(t.clazz.name);
				tp = Type.newRefType(t.clazz,new Type[]{tp});
			}
		}
		this.lnk = tp;
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
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}
