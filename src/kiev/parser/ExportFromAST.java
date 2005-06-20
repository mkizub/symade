/* Generated By:JJTree: Do not edit this line. ASTNonArrayType.java */

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
import kiev.vlang.ASTNode;
import kiev.vlang.CompilerException;
import kiev.vlang.BinaryOperator;
import kiev.tree.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTNonArrayType.java,v 1.3 1998/10/26 23:47:04 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public final class ExportFromAST {
	
	public final class ExportASTCreateInfo extends CreateInfo {
		public ASTNode src_node;
		
		public ExportASTCreateInfo(ASTNode src_node) {
			this.src_node = src_node;
		}
	}

	public final VersionBranch from;
	public final VersionBranch into;
	
	public ExportFromAST(VersionBranch from, VersionBranch into)
	{
		this.from = from;
		this.into = into;
	}
	
	private CreateInfo createInfo(ASTNode node) {
		return new ExportASTCreateInfo(node);
	}
	
	public VNode toVlang(ASTNode node) {
		if (node == null)
			return null;
		throw new CompilerException(node.getPos(), "Unknown ASTNode to import: "+node.getClass());
	}
	
	public VNode toVlang(ASTIdentifier node) {
		Identifier i = new Identifier(createInfo(node), node.name);
		node.setImpl(into, i);
		return node;
	}
	
	public VNode toVlang(ASTAccessExpression node) {
		VNode obj   = toVlang(node.obj);
		VNode ident = toVlang(node.ident);
		BinaryExpr ae = new BinaryExpr(createInfo(node), BinaryOperator.DotAccess, obj, ident);
		node.setImpl(into, ae);
		return node;
	}
	
	public VNode toVlang(ASTArrayElementAccessExpression node) {
		VNode obj   = toVlang(node.obj);
		VNode index = toVlang(node.index);
		BinaryExpr ae = new BinaryExpr(createInfo(node), BinaryOperator.ElemAccess, obj, index);
		node.setImpl(into, ae);
		return node;
	}
	
	public VNode toVlang(ASTAnonymouseClosure node) {
		VNode[] params = new VNode[node.params.length];
		for (int i=0; i < node.params.length; i++)
			params[i] = toVlang(node.params[i]);
		VNode rtype = toVlang(node.type);
		VNode body  = toVlang(node.body);
		Closure c = new Closure(createInfo(node), params, rtype, body);
		node.setImpl(into, c);
		return node;
	}
	
	public VNode toVlang(ASTArgumentDeclaration node) {
		VNode ident = toVlang(node.ident);
		VNode stype = toVlang(node.type);
		//TypeArgDecl tad = new TypeArgDecl(createInfo(node), ident, stype);
		//node.setImpl(into, tad);
		return node;
	}
	
	public VNode toVlang(ASTBlock node) {
		VNode[] stats = new VNode[node.stats.length];
		for (int i=0; i < node.stats.length; i++)
			stats[i] = toVlang(node.stats[i]);
		BlockSt bs = new BlockSt(createInfo(node), stats);
		node.setImpl(into, bs);
		return node;
	}
	
	public VNode toVlang(ASTBreakStatement node) {
		VNode target = toVlang(node.ident);
		BreakSt bs = new BreakSt(createInfo(node), target);
		node.setImpl(into, bs);
		return node;
	}
	
	
	public VNode toVlang(ASTCallAccessExpression node) {
		VNode obj   = toVlang(node.obj);
		VNode ident = toVlang(node.ident);
		VNode[] args = new VNode[node.args.length];
		for (int i=0; i < node.args.length; i++)
			args[i] = toVlang(node.args[i]);
		CallExpr c = new CallExpr(createInfo(node), obj, ident, args);
		node.setImpl(into, c);
		return node;
	}
	
	public VNode toVlang(ASTCallExpression node) {
		VNode ident = toVlang(node.ident);
		VNode[] args = new VNode[node.args.length];
		for (int i=0; i < node.args.length; i++)
			args[i] = toVlang(node.args[i]);
		CallExpr c = new CallExpr(createInfo(node), null, ident, args);
		node.setImpl(into, c);
		return node;
	}
	
	public VNode toVlang(ASTCastExpression node) {
		VNode type = toVlang(node.type);
		VNode expr = toVlang(node.expr);
		CastExpr c = new CastExpr(createInfo(node), type, expr);
		node.setImpl(into, c);
		return node;
	}
	
	public VNode toVlang(ASTCatchInfo node) {
		VNode par = toVlang(node.par);
		VNode body = toVlang(node.body);
		Catch c = new Catch(createInfo(node), par, body);
		node.setImpl(into, c);
		return node;
	}
	
}

	
