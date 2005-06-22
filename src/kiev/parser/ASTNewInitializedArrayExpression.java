/* Generated By:JJTree: Do not edit this line. ASTNewInitializedArrayExpression.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTNewInitializedArrayExpression.java,v 1.3 1998/10/26 23:47:04 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public class ASTNewInitializedArrayExpression extends ASTNode {
	public ASTNode		type;
    public ASTExpr[]	args = ASTExpr.emptyArray;
	public int dim;
  
	ASTNewInitializedArrayExpression(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if(i==0 && n instanceof ASTNonArrayType ) {
			type=n;
		} else {
			args = (Expr[])Arrays.append(args,n);
        }
    }

	public Node resolve(Type reqType) {
		if( type == null ) type = reqType;
		else if( type instanceof Type );
		else {
			type = ((ASTNonArrayType)type).pass2();
			while( dim > 0 ) { type = Type.newArrayType((Type)type); dim--; }
		}
		if( !((Type)type).isArray() )
			throw new CompilerException(pos,"Type "+type+" is not an array type");
    	for(int i=0; i < args.length; i++) {
        	try {
				args[i] = args[i].resolveExpr(((Type)type).args[0]);
            } catch(Exception e) {
            	Kiev.reportError(pos,e);
            }
        }
        Type tp = (Type)type;
        dim = 0;
        while( tp.isArray() ) { dim++; tp = tp.args[0]; }
		return new NewInitializedArrayExpr(pos,tp,dim,args).resolve(reqType);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Dumper toJava(Dumper dmp) {
		dmp.append("new ").append(type);
		for(int i=0; i < dim; i++) dmp.append("[]");
		dmp.append('{');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 ) dmp.append(',').space();
		}
		dmp.append('}');
		return dmp;
	}
}
