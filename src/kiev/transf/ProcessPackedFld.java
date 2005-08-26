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

public final class ProcessPackedFld extends TransfProcessor implements Constants {
	
	private static final int[] masks =
		{	0,
			0x1       ,0x3       ,0x7       ,0xF       ,
			0x1F      ,0x3F      ,0x7F      ,0xFF      ,
			0x1FF     ,0x3FF     ,0x7FF     ,0xFFF     ,
			0x1FFF    ,0x3FFF    ,0x7FFF    ,0xFFFF    ,
			0x1FFFF   ,0x3FFFF   ,0x7FFFF   ,0xFFFFF   ,
			0x1FFFFF  ,0x3FFFFF  ,0x7FFFFF  ,0xFFFFFF  ,
			0x1FFFFFF ,0x3FFFFFF ,0x7FFFFFF ,0xFFFFFFF ,
			0x1FFFFFFF,0x3FFFFFFF,0x7FFFFFFF,0xFFFFFFFF
		};

	public ProcessPackedFld(Kiev.Ext ext) {
		super(ext);
	}
	
	private void rewriteNode(ASTNode node, String id) {
		foreach (AttrSlot attr; node.values(); attr.is_attr) {
			Object val = node.getVal(attr.name);
			rewrite(val, attr.name);
		}
	}
	
	public void rewrite(ASTNode:Object node, String id) {
		//System.out.println("ProcessPackedFld: rewrite "+node.getClass().getName()+" in "+id);
		PassInfo.push(node);
		try {
			rewriteNode(node, id);
		} finally { PassInfo.pop(node); }
	}
	
	public void rewrite(NArr<ASTNode>:Object arr, String id) {
		//System.out.println("ProcessPackedFld: rewrite "+arr.getClass().getName()+" in "+id);
		foreach (ASTNode n; arr) {
			rewrite(n, id);
		}
	}

	public void rewrite(Object:Object o, String id) {
		//System.out.println("ProcessPackedFld: rewrite "+(o==null?"null":o.getClass().getName())+" in "+id);
		return;
	}

	public void rewrite(FileUnit:Object node, String id) {
		//System.out.println("ProcessPackedFld: rewrite "+node.getClass().getName()+" in "+id);
		NodeInfoPass.init();
		ScopeNodeInfoVector state = NodeInfoPass.pushState();
		state.guarded = true;
		PassInfo.push(node);
		try {
			rewriteNode(node, id);
		} finally { PassInfo.pop(node); NodeInfoPass.close(); }
	}
	
	public void rewrite(AccessExpr:Object fa, String id) {
		//System.out.println("ProcessPackedFld: rewrite "+fa.getClass().getName()+" "+fa+" in "+id);
		PassInfo.push(fa);
		try {
			Field f = fa.var;
			if( !f.isPackedField() ) {
				rewriteNode(fa, id);
				return;
			}
			MetaPacked mp = f.getMetaPacked();
		if( mp == null || mp.packer == null ) {
				Kiev.reportError(fa.pos, "Internal error: packed field "+f+" has no packer");
				rewriteNode(fa, id);
				return;
			}
			ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
			AccessExpr ae = (AccessExpr)fa.copy();
			ae.var = mp.packer;
			Expr expr = ae;
			if (mp.offset > 0) {
				ConstExpr sexpr = new ConstIntExpr(mp.offset);
				expr = new BinaryExpr(fa.pos, BinaryOperator.UnsignedRightShift, expr, sexpr);
			}
			expr = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, expr, mexpr);
			if( mp.size == 8 && f.type == Type.tpByte )
				expr = new CastExpr(fa.pos, Type.tpByte, expr);
			else if( mp.size == 16 && f.type == Type.tpShort )
				expr = new CastExpr(fa.pos, Type.tpShort, expr);
			else if( mp.size == 16 && f.type == Type.tpChar )
				expr = new CastExpr(fa.pos, Type.tpChar, expr, true);
			else if( mp.size == 1 && f.type == Type.tpBoolean )
				expr = new CastExpr(fa.pos, Type.tpBoolean, expr, true);

			fa.parent.replaceVal(id, fa, expr);
			rewriteNode(expr, id);
		} finally { PassInfo.pop(fa); }
	}
	
	public void rewrite(AssignExpr:Object ae, String id) {
		//System.out.println("ProcessPackedFld: rewrite "+ae.getClass().getName()+" "+ae+" in "+id);
		PassInfo.push(ae);
		try {
			if !(ae.lval instanceof AccessExpr) {
				rewriteNode(ae, id);
				return;
			}
			AccessExpr fa = (AccessExpr)ae.lval;
			Field f = fa.var;
			if( !f.isPackedField() ) {
				rewriteNode(ae, id);
				return;
			}
			BlockExpr be = new BlockExpr(ae.pos, ae.parent);
			Object acc;
			if (fa.obj instanceof ThisExpr) {
				acc = fa.obj;
			}
			else if (fa.obj instanceof VarAccessExpr) {
				acc = ((VarAccessExpr)fa.obj).var;
			}
			else {
				Var var = new Var(0,KString.from("tmp$acc"),fa.obj.getType(),0);
				var.init = fa.obj;
				be.addSymbol(var);
				acc = var;
			}
			Var fval = new Var(0,KString.from("tmp$fldval"),Type.tpInt,0);
			MetaPacked mp = f.getMetaPacked();
			fval.init = new AccessExpr(fa.pos, mkAccess(acc), mp.packer);
			be.addSymbol(fval);
			Var tmp = new Var(0,KString.from("tmp$val"),Type.tpInt,0);
			be.addSymbol(tmp);
			if !(ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2) {
				ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
				Expr expr = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(fval), mexpr);
				if (mp.offset > 0) {
					ConstExpr sexpr = new ConstIntExpr(mp.offset);
					expr = new BinaryExpr(fa.pos, BinaryOperator.UnsignedRightShift, expr, sexpr);
				}
				if( mp.size == 8 && f.type == Type.tpByte )
					expr = new CastExpr(fa.pos, Type.tpByte, expr);
				else if( mp.size == 16 && f.type == Type.tpShort )
					expr = new CastExpr(fa.pos, Type.tpShort, expr);
				tmp.init = expr;
				be.addStatement(new ExprStat(new AssignExpr(fa.pos, ae.op, mkAccess(tmp), ae.value)));
			}
			else if (ae.value.getType() == Type.tpBoolean) {
				tmp.init = new CastExpr(ae.value.pos, Type.tpInt, ae.value, true);
			}
			else {
				tmp.init = ae.value;
			}
			
			{
				ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
				Expr expr_l = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(tmp), mexpr);
				if (mp.offset > 0) {
					ConstExpr sexpr = new ConstIntExpr(mp.offset);
					expr_l = new BinaryExpr(fa.pos, BinaryOperator.LeftShift, expr_l, sexpr);
				}
				ConstExpr clear = new ConstIntExpr(~(masks[mp.size]<<mp.offset));
				Expr expr_r = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(fval), clear);
				Expr expr = new BinaryExpr(fa.pos, BinaryOperator.BitOr, expr_r, expr_l);
				expr = new AssignExpr(fa.pos, AssignOperator.Assign,
					new AccessExpr(fa.pos, mkAccess(acc), mp.packer),
					expr);
				be.addStatement(new ExprStat(fa.pos, be, expr));
			}
			if (!ae.isGenVoidExpr()) {
				be.setExpr(mkAccess(tmp));
			}
			ae.replaceWithNode(be);
			be.resolve(ae.isGenVoidExpr() ? Type.tpVoid : ae.getType());
			rewrite(be, id);
		} finally { PassInfo.pop(ae); }
	}
	
	public void rewrite(IncrementExpr:Object ie, String id) {
		//System.out.println("ProcessPackedFld: rewrite "+ie.getClass().getName()+" "+ie+" in "+id);
		PassInfo.push(ie);
		try {
			if !(ie.lval instanceof AccessExpr) {
				rewriteNode(ie, id);
				return;
			}
			AccessExpr fa = (AccessExpr)ie.lval;
			Field f = fa.var;
			if( !f.isPackedField() ) {
				rewriteNode(ie, id);
				return;
			}
			MetaPacked mp = f.getMetaPacked();
			Expr expr;
			if (ie.isGenVoidExpr()) {
				if (ie.op == PrefixOperator.PreIncr || ie.op == PostfixOperator.PostIncr) {
					expr = new AssignExpr(ie.pos, AssignOperator.AssignAdd, ie.lval, new ConstIntExpr(1));
				} else {
					expr = new AssignExpr(ie.pos, AssignOperator.AssignAdd, ie.lval, new ConstIntExpr(-1));
				}
				expr.resolve(Type.tpVoid);
				expr.setGenVoidExpr(true);
			}
			else {
				BlockExpr be = new BlockExpr(ie.pos, ie.parent);
				Object acc;
				if (fa.obj instanceof ThisExpr) {
					acc = fa.obj;
				}
				else if (fa.obj instanceof VarAccessExpr) {
					acc = ((VarAccessExpr)fa.obj).var;
				}
				else {
					Var var = new Var(0,KString.from("tmp$acc"),fa.obj.getType(),0);
					var.init = fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				Var fval = new Var(0,KString.from("tmp$fldval"),Type.tpInt,0);
				fval.init = new AccessExpr(fa.pos, mkAccess(acc), mp.packer);
				be.addSymbol(fval);
				Var tmp = new Var(0,KString.from("tmp$val"),Type.tpInt,0);
				be.addSymbol(tmp);
				{
					ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
					Expr expr = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(fval), mexpr);
					if (mp.offset > 0) {
						ConstExpr sexpr = new ConstIntExpr(mp.offset);
						expr = new BinaryExpr(fa.pos, BinaryOperator.UnsignedRightShift, expr, sexpr);
					}
					if( mp.size == 8 && f.type == Type.tpByte )
						expr = new CastExpr(fa.pos, Type.tpByte, expr);
					else if( mp.size == 16 && f.type == Type.tpShort )
						expr = new CastExpr(fa.pos, Type.tpShort, expr);
					ConstExpr ce;
					if (ie.op == PrefixOperator.PreIncr)
						tmp.init = new BinaryExpr(0, BinaryOperator.Add, expr, new ConstIntExpr(1));
					else if (ie.op == PrefixOperator.PreDecr)
						tmp.init = new BinaryExpr(0, BinaryOperator.Sub, expr, new ConstIntExpr(1));
					else
						tmp.init = expr;
				}

				{
					ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
					Expr expr_l;
					if (ie.op == PostfixOperator.PostIncr)
						expr_l = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, new BinaryExpr(0,BinaryOperator.Add,mkAccess(tmp),new ConstIntExpr(1)), mexpr);
					else if (ie.op == PostfixOperator.PostDecr)
						expr_l = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, new BinaryExpr(0,BinaryOperator.Sub,mkAccess(tmp),new ConstIntExpr(1)), mexpr);
					else
						expr_l = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(tmp), mexpr);
					if (mp.offset > 0) {
						ConstExpr sexpr = new ConstIntExpr(mp.offset);
						expr_l = new BinaryExpr(fa.pos, BinaryOperator.LeftShift, expr_l, sexpr);
					}
					ConstExpr clear = new ConstIntExpr(~(masks[mp.size]<<mp.offset));
					Expr expr_r = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(fval), clear);
					Expr expr = new BinaryExpr(fa.pos, BinaryOperator.BitOr, expr_r, expr_l);
					expr = new AssignExpr(fa.pos, AssignOperator.Assign,
						new AccessExpr(fa.pos, mkAccess(acc), mp.packer),
						expr);
					be.addStatement(new ExprStat(fa.pos, be, expr));
				}
				if (!ie.isGenVoidExpr()) {
					be.setExpr(mkAccess(tmp));
				}
				expr = be;
				expr.resolve(ie.isGenVoidExpr() ? Type.tpVoid : ie.getType());
			}
			ie.parent.replaceVal(id, ie, expr);
			rewrite(expr, id);
		} finally { PassInfo.pop(ie); }
	}
	
	private Expr mkAccess(Object o) {
		if (o instanceof Var) return new VarAccessExpr(0,null,(Var)o);
		if (o instanceof ThisExpr) return new ThisExpr(0,null);
		throw new RuntimeException("Unknown accessor "+o);
	}
}