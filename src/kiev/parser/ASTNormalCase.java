/* Generated By:JJTree: Do not edit this line. ASTNormalCase.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTNormalCase.java,v 1.3 1998/10/26 23:47:04 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTNormalCase extends ASTNode {
	@att public Expr					val;
	@att public final NArr<ASTNode>		stats;

	public ASTNormalCase() {
	}

	public ASTNormalCase(int id) {
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if( i==0 && n instanceof Expr )
			val = (Expr)n;
        else
			stats.append(n);
    }
    
    public ASTNode resolve(Type reqType) {
    	try {
			ASTNode n = null;
    		if( val != null ) {
		    	n = val.resolve(null);
				if (n instanceof Struct)
					n = new WrapedExpr(val.pos, n);
			}
			CaseLabel cl = new CaseLabel(pos,parent,(Expr)n,stats.toArray());
			cl.parent = parent;
			return cl /*.resolve(Type.tpVoid)*/;
	    } catch(Exception e ) {
	    	Kiev.reportError(val.getPos(),e);
			return this;
	    }
    }

	public Dumper toJava(Dumper dmp) {
		if( val == null )
			dmp.newLine(-1).append("default:").newLine(1);
		else
			dmp.newLine(-1).append("case ").append(val).append(':').newLine(1);
		for(int i=0; i < stats.length; i++) {
			dmp.append(stats[i]).newLine();
		}
		return dmp;
	}
}

