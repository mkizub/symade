/* Generated By:JJTree: Do not edit this line. ASTTypeClassExpression.java */

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

import kiev.*;
import kiev.stdlib.*;
import kiev.vlang.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTTypeClassExpression.java,v 1.3 1998/10/26 23:47:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
@cfnode
public class ASTTypeClassExpression extends Expr {
	
	@att public ASTType		type;
	
	public void jjtAddChild(ASTNode n, int i) {
    	switch(i) {
        case 0:	type = (ASTType)n; break;
        default: throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }
    
    public Type getType() { return Type.tpClass; }

	public ASTNode resolve(Type reqType) throws CompilerException {
		Type tp = type.getType();
		if( !tp.isReference() ) {
			Type rt = Type.getRefTypeForPrimitive(tp);
			Field f = (Field)rt.clazz.resolveName(KString.from("TYPE"));
			if( f == null || !f.isStatic() )
				throw new CompilerException(pos,"Static final field TYPE not found in "+rt);
			return new StaticFieldAccessExpr(pos,(Struct)rt.clazz,f).resolve(reqType);
		}
		KString name;
		if( tp.isArray() )
			name = tp.java_signature.replace('/','.');
		else
			name = tp.clazz.name.bytecode_name.replace('/','.');
		return new CallExpr(pos,
				Type.tpClass.clazz.resolveMethod(
					KString.from("forName"),
					KString.from("(Ljava/lang/String;)Ljava/lang/Class;")
				),
				new Expr[]{new ConstExpr(pos,name)}
			).resolve(reqType);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
    	return type+".class";
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(type).append(".class");
		return dmp;
	}
}
