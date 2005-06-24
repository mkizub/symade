/* Generated By:JJTree: Do not edit this line. ASTPragma.java */

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

public class ASTPragma extends SimpleNode implements TopLevelDecl {

	public boolean				enable;
	public ASTConstExpression[]	options = new ASTConstExpression[0];

	public ASTPragma(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
		ASTConstExpression opt = (ASTConstExpression)n;
		options = (ASTConstExpression[])Arrays.append(options,opt);
    }

	public Dumper toJava(Dumper dmp) {
		dmp.append("/* pragma ").append(enable?"enable":"disable").space();
		foreach (ASTConstExpression e; options)
			dmp.forsed_space().append(e);
		return dmp.append("; */").newLine();
	}
}
