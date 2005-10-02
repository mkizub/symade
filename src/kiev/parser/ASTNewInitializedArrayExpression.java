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
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class ASTNewInitializedArrayExpression extends Expr {
	@att
	public TypeRef					type;
	
	@att
	@dflow(in="", seq="true")
	public final NArr<ENode>		args;
	
	public int dim;
	
	public void resolve(Type reqType) {
		Type tp;
		if( type == null ) {
			tp = reqType;
		} else {
			tp = type.getType();
			while( this.dim > 0 ) { tp = Type.newArrayType(tp); this.dim--; }
		}
		if( !tp.isArray() )
			throw new CompilerException(pos,"Type "+type+" is not an array type");
    	for(int i=0; i < args.length; i++) {
        	try {
				args[i].resolve(tp.args[0]);
            } catch(Exception e) {
            	Kiev.reportError(pos,e);
            }
        }
        int dim = 0;
        while( tp.isArray() ) { dim++; tp = tp.args[0]; }
		replaceWithNodeResolve(reqType, new NewInitializedArrayExpr(pos,new TypeRef(tp),dim,args.toArray()));
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
