/* Generated By:JJTree: Do not edit this line. ASTOpdef.java */

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

import static kiev.stdlib.Debug.*;
import static kiev.vlang.OpTypes.*;
import static kiev.vlang.Operator.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTOperatorAlias.java,v 1.3.4.2 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.2 $
 *
 */

@node
public class Opdef extends ASTNode implements TopLevelDecl {
	public int					prior;
	public int					opmode;
	public KString				image;
	
	@ref public transient Operator		resolved;

	public void jjtAddChild(ASTNode n, int i) {
		switch(i) {
		case 2:
			if( n instanceof ConstExpr ) {
				Object val = ((ConstExpr)n).getConstValue();
				if( val == null || !( val instanceof Number) )
					throw new CompilerException(n.getPos(),"Priority must be of int type, but found "+n);
				prior = ((Number)val).intValue();
				if( prior < 0 || prior > 255 )
					throw new CompilerException(n.getPos(),"Operator priority must have value from 0 to 255");
				pos = n.getPos();
				return;
			}
			break;
		case 1:
			opmode = -1;
			if( n instanceof ASTIdentifier ) {
				KString optype = ((ASTIdentifier)n).name;
				for(int i=0; i < Operator.orderAndArityNames.length; i++) {
					if( Operator.orderAndArityNames[i].equals(optype) ) {
						opmode = i;
						break;
					}
				}
				if( opmode < 0 )
					throw new CompilerException(n.getPos(),"Operator mode must be one of "+Arrays.toString(Operator.orderAndArityNames));
				return;
			}
			break;
		case 0:
			this.pos = n.pos;
			if( n instanceof ASTOperator ) {
				image = ((ASTOperator)n).image;
				return;
			}
			else if( n instanceof ASTIdentifier ) {
				image = ((ASTIdentifier)n).name;
				return;
			}
			break;
		}
		throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
	}

	public String toString() {
		return image.toString();
	}

	public ASTNode resolve(Type reqType) {
		return this;
	}
	
	public Dumper toJavaDecl(Dumper dmp) {
		return toJava(dmp);
	}
	
	public Dumper toJava(Dumper dmp) {
		return dmp.space().append("/* operator ")
			.append(Integer.toString(prior)).forsed_space()
			.append(Operator.orderAndArityNames[opmode]).forsed_space()
			.append(image).append(" */").space();
	}

}

