/* Generated By:JJTree: Do not edit this line. ASTCatchInfo.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTCatchInfo.java,v 1.3 1998/10/26 23:47:02 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

@node
public class ASTCatchInfo extends ASTNode {
	@att public ASTFormalParameter	par;
    @att public Statement			body;

	public ASTNode resolve(Type reqType) {
		CatchInfo ci = new CatchInfo(pos,parent,par.pass3(),body);
		return ci.resolve(Type.tpVoid);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("catch").space().append('(').space();
		par.toJava(dmp).space().append(')').space().append(body);
		return dmp;
	}
}
