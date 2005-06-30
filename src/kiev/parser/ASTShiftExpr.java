/* Generated By:JJTree: Do not edit this line. ASTShiftExpr.java */

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
import kiev.vlang.*;
import kiev.stdlib.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTShiftExpr.java,v 1.3 1998/10/26 23:47:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTShiftExpr extends Expr {
	public Expr expr1;
	public Expr expr2;
	public KString op;

	ASTShiftExpr(int id) {
		super(kiev.Kiev.k.getToken(0)==null?0:kiev.Kiev.k.getToken(0).getPos());
	}
  
	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0: expr1=(Expr)n; break;
        case 1: expr2=(Expr)n; break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

  	public void set(Token t) {
		op = new KToken(t).image;
        pos = t.getPos();
	}
  
	public ASTNode resolve(Type reqType) {
		throw new RuntimeException("Class expired");
//		Operator o = Operator.resolve(op,Operator.binaryOp);
//		return new BinaryExpr(pos,o,expr1,expr2).resolve(reqType);
	}
    
    public Dumper toJava(Dumper dmp) {
    	dmp.append(expr1).space().append(op).space().append(expr2);
        return dmp;
    }
}
