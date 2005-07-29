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

package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

public final class ProcessFixParent implements Constants {

	// fix nodes
	private void fixupNode(ASTNode node) {
		foreach (String name; node.values()) {
			Object val = node.getVal(name);
			if (val instanceof ASTNode) {
				ASTNode n = (ASTNode)val;
				n.parent = node;
			}
			fixup(val, name);
		}
	}
	
	public void fixup(ASTNode:Object node, String id) {
		fixupNode(node);
	}
	
	public void fixup(NArr<ASTNode>:Object arr, String id) {
		foreach (ASTNode n; arr) {
			n.parent = arr.getParent();
			fixupNode(n);
		}
	}

	public void fixup(Object:Object o, String id) {
		return;
	}

	
	
	// verify after the fix
	private void verifyNode(ASTNode node) {
		foreach (String name; node.values()) {
			Object val = node.getVal(name);
			if (val instanceof ASTNode) {
				ASTNode n = (ASTNode)val;
				n.parent = node;
			}
			verify(val, name);
		}
	}
	
	public void verify(ASTNode:Object node, String id) {
		verifyNode(node);
	}
	
	public void verify(NArr<ASTNode>:Object arr, String id) {
		foreach (ASTNode n; arr) {
			if !(n.parent == arr.getParent())
				Kiev.reportError(n.pos, "Node in "+arr.getParent()+" attached to multiple @att fields, one is "+n.parent+" and another is "+arr.getParent()+"."+id);
			verifyNode(n);
		}
	}

	public void verify(Object:Object o, String id) {
		return;
	}

}