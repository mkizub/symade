/* Generated By:JJTree: Do not edit this line. ASTReparseExpression.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTReparseExpression.java,v 1.3 1998/10/26 23:47:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTReparseExpression extends Expr {

	public String	ref;

	public ASTReparseExpression() {
	}

	public ASTReparseExpression(int id) {
	}

  	public void set(Token t) {
		this.ref = t.image;
		ASTNode n = Kiev.parserAddresses.get(ref.substring(2));
		if( n != null )
			kiev.Kiev.k.jj_input_stream.adjustBeginLineColumn(n.getPosLine(),n.getPosColumn());
	}
	
	public ASTNode resolve(Type reqType) {
		ASTNode n = Kiev.parserAddresses.get(ref.substring(2));
		if( n==null ) {
			throw new RuntimeException("Reparse node "+ref+" not found");
		}
		if( !(n instanceof Expr) ) {
			throw new RuntimeException("Reparse node "+ref+" is not an expression");
		}
		return n;
	}
  
	public int		getPriority() { return 256; }

	public String toString() { return Kiev.parserAddresses.get(ref.substring(2)).toString(); }

}
