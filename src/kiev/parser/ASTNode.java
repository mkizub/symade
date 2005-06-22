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
import kiev.tree.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTNewInitializedArrayExpression.java,v 1.3 1998/10/26 23:47:04 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public class ASTNode extends Node {

    public ASTNode() {
		super(0,null);
	}

    public ASTNode(int pos) {
		super(pos,null);
	}

    public ASTNode(Node# parent) {
		super(0,parent);
	}

	public ASTNode(int pos, Node# parent) {
		super(pos,parent);
	}

	public ASTNode(int pos, Node parent) {
		super(pos,parent.vnode);
	}

	public void jjtSetParent(ASTNode n) { parent = n; }
	public ASTNode jjtGetParent() { return (ASTNode)parent; }
	public void jjtAddChild(ASTNode n, int i) {
		Kiev.reportError(pos,"jjtAddChild not implemented for this class: "+getClass());
	}

}

public class ASTExpr extends ASTNode {
	public static final ASTExpr[] emptyArray = new ASTExpr[0];
}
public class ASTStatement extends ASTNode {
	public static final ASTStatement[] emptyArray = new ASTStatement[0];
}
public class ASTRuleNode extends ASTNode {
	public static final ASTRuleNode[] emptyArray = new ASTRuleNode[0];
}

