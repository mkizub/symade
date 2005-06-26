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

package kiev.vlang;

import kiev.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Resolving.java,v 1.3 1998/10/26 23:47:22 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public interface ResolveFlags {

	// Result must be unique
	public static final int		Unique	= 1;
	// Result must exists
	public static final int		Exists	= 2;
	// Resolving may not use class-level imports
	public static final int		NoImports	= 4;
	// Resolving may not use forwarding
	public static final int		NoForwards	= 8;
	// Resolving may not check super classes
	public static final int		NoSuper		= 16;
	// Result may be a static member
	public static final int		Static	= 32;

	// Both unique & exists requared
	public static final int		UniqEx	= 3;
}

public class MatchNode implements ResolveFlags {
	public ASTNode	path[] = ASTNode.emptyArray;
	public ASTNode	node;

	public MatchNode() {
	}

	public MatchNode(ASTNode node) {
		this.node = node;
	}

	public static void checkFlags(List<MatchNode> objs, int resfl) {
		if( (resfl & Exists) != 0 && objs == List.Nil )
			throw new RuntimeException("Unresolved identifier");
		if( (resfl & Unique) != 0 && objs.length() > 1 )
			throw new RuntimeException("Ambigous identifier");
	}

	public Method method() {
		return (Method)node;
	}

}

public class ResInfo {
	public int					transforms;
	public ListBuffer<ASTNode>	path = new ListBuffer<ASTNode>();

	public ResInfo() {
	}
	public ResInfo(ASTNode through) {
		enterForward(through);
	}

	public void enterForward(ASTNode node) {
		path.append(node);
		transforms++;
	}
	public void leaveForward(ASTNode node) {
		assert(path.length() > 0 && path.getAt(path.length()-1) == node);
		path.setLength(path.length()-1);
		transforms--;
	}
	public void enterSuper() {
		transforms++;
	}
	public void leaveSuper() {
		transforms++;
	}

	public ResInfo copy() {
		ResInfo ri = new ResInfo();
		ri.transforms = transforms;
		foreach (ASTNode n; path.toList()) ri.path.append(n);
		return ri;
	}

	public void set(ResInfo info)
	{
		if (this == info) return;
		this.transforms = info.transforms;
		this.path.setLength(0);
		foreach (ASTNode n; info.path.toList()) this.path.append(n);
	}
};

public interface Scope {
	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name, Type type, int resfl);
	public rule resolveMethodR(ASTNode@ node, ResInfo path, KString name, Expr[] args, Type ret, Type type, int resfl);
}

public interface ScopeOfOperators {
	public rule resolveOperatorR(ASTNode@ op);
}



