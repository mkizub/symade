/* Generated By:JJTree: Do not edit this line. ASTPizzaCase.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTPizzaCase.java,v 1.3 1998/10/26 23:47:04 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public class ASTPizzaCase extends ASTNode {
	public ASTNode		val;
	public ASTNode[]	params = ASTNode.emptyArray;
	public ASTNode[]	stats = ASTNode.emptyArray;

	public ASTPizzaCase(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if( i==0 )
			val = n;
		else if( n instanceof ASTFormalParameter ) {
			params = (ASTNode[])Arrays.append(params,n);
		}
        else
			stats = (ASTNode[])Arrays.append(stats,n);
    }

    public ASTNode resolve(Type reqType) {
    	Var[] pattern = new Var[params.length];
    	try {
	    	KString n = ((ASTQName)val).toKString();
			PVar<ASTNode> v = new PVar<ASTNode>();
			if( !PassInfo.resolveNameR(v,new PVar<List<ASTNode>>(List.Nil),n,null,0) )
				throw new CompilerException(val.pos,"Unresolved class "+n);
	    	val = v;
	    	if( !(val instanceof Struct) || !((Struct)val).isPizzaCase() )
	    		throw new CompilerException(val.getPos(),"Class "+n+" is not a class case");
	    	for(int i=0; i < params.length; i++) {
    			params[i] = pattern[i] = ((ASTFormalParameter)params[i]).pass3();
	    	}
	    } catch(Exception e ) {
	    	Kiev.reportError(val.getPos(),e);
	    }
    	CaseLabel cl = new CaseLabel(pos,parent,val,stats);
    	cl.parent = parent;
    	cl.pattern = pattern;
    	return cl.resolve(Type.tpVoid);
    }

	public Dumper toJava(Dumper dmp) {
		dmp.newLine(-1).append("case ").append(val);
		if( params.length > 0 ) {
			dmp.append('(');
			for(int i=0; i < params.length; i++) {
				dmp.append(params[i]);
				if( i < params.length-1 ) dmp.append(',').space();
			}
			dmp.append(')');
		}
		dmp.append(':').newLine(1);
		for(int i=0; i < stats.length; i++) {
			dmp.append(stats[i]).newLine();
		}
		dmp.newLine(-1);
		return dmp;
	}
}
