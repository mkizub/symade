/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.transf;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@singleton
public final class PackedFldFE_Verify extends TransfProcessor {
	private PackedFldFE_Verify() { super(KievExt.PackedFields); }
	public String getDescr() { "Packed fields verification" }

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (Struct n; fu.members)
			doProcess(n);
	}
	
	public void doProcess(Struct:ASTNode s) {
		foreach (Field n; s.getAllFields())
			doProcess(n);
		foreach (Struct sub; s.sub_decls)
			doProcess(sub);
	}
	
	public void doProcess(Field:ASTNode f) {
		MetaPacked mp = f.getMetaPacked();
		if !(f.isPackedField() ) {
			if (mp != null)
				Kiev.reportError(f, "Non-packed field has @packed attribute");
			return;
		}
		if (mp == null) {
			if (mp != null)
				Kiev.reportError(f, "Packed field has no @packed attribute");
			return;
		}
		Struct s = f.ctx_tdecl;
		String mp_in = mp.getS("in");
		if( mp_in != null && mp_in.length() > 0 ) {
			Field p = s.resolveField(mp_in.intern(),false);
			if( p == null ) {
				Kiev.reportError(f,"Packer field "+mp_in+" not found");
				return;
			}
			if( p.type ≢ Type.tpInt ) {
				Kiev.reportError(f,"Packer field "+p+" is not of 'int' type");
				return;
			}
			mp.fld.open();
			mp.fld.symbol = p;
			assert( mp.offset >= 0 && mp.offset+mp.size <= 32 );
		}
	}
	
}

@singleton
public class PackedFldME_PreGenerate extends BackendProcessor {
	private PackedFldME_PreGenerate() { super(KievBackend.Java15); }
	public String getDescr() { "Packed fields pre-generation" }

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr);
		try {
			doProcess(node);
		} finally { tr.leave(); }
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode s) {
		// Setup packed/packer fields
		foreach(Field f; s.getAllFields(); f.isPackedField() ) {
			Field@ packer;
			// Locate or create nearest packer field that can hold this one
			MetaPacked mp = f.getMetaPacked();
			if( mp.fld.dnode == null ) {
				mp.open();
				String mp_in = mp.getS("in");
				if( mp_in != null && mp_in.length() > 0 ) {
					Field p = s.resolveField(mp_in,false);
					if( p == null ) {
						Kiev.reportError(f,"Packer field "+mp_in+" not found");
						~mp;
						continue;
					}
					if( p.type ≢ Type.tpInt ) {
						Kiev.reportError(f,"Packer field "+p+" is not of 'int' type");
						~mp;
						continue;
					}
					mp.fld.open();
					mp.fld.symbol = p;
					assert( mp.offset >= 0 && mp.offset+mp.size <= 32 );
				}
				else if( locatePackerField(packer,mp.size,s) ) {
					// Found
					mp.fld.open();
					mp.fld.symbol = packer;
					MetaPacker mpr = packer.getMetaPacker();
					mp.offset = mpr.size;
					mpr.size += mpr.size;
				} else {
					// Create
					Field p = new Field("$pack$"+countPackerFields(s),Type.tpInt,ACC_PUBLIC|ACC_SYNTHETIC);
					p.pos = s.pos;
					MetaPacker mpr = new MetaPacker();
					p.setMeta(mpr);
					s.addField(p);
					mp.fld.open();
					mp.fld.symbol = p;
					mp.offset = 0;
					mpr.size += mp.size;
				}
			}
			foreach(Struct n; s.members)
				this.doProcess(n);
		}
	}

	private int countPackerFields(Struct s) {
		int i = 0;
		foreach (Field f; s.getAllFields(); f.isPackerField()) i++;
		return i;
	}

	private rule locatePackerField(Field@ f, int size, Struct s)
		ASTNode@ n;
		Field ff;
	{
		s.super_types.length > 0,
		locatePackerField(f,size,s.super_types[0].getStruct())
	;	n @= s.members,
		n instanceof Field && ((Field)n).isPackerField(),
		ff = (Field)n : ff = null,
		(32-ff.getMetaPacked().size) >= size,
		f ?= ff
	}

}

@singleton
public class PackedFldBE_Rewrite extends BackendProcessor {
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

	private PackedFldBE_Rewrite() { super(KievBackend.Java15); }
	public String getDescr() { "Packed fields rewrite" }

	public void process(ASTNode fu, Transaction tr) {
		tr = Transaction.enter(tr);
		try {
			fu.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) { PackedFldBE_Rewrite.this.rewrite(n); return true; }
			});
		} finally { tr.leave(); }
	}
	
	void rewrite(ANode:ANode n) {
		//System.out.println("ProcessPackedFld: rewrite "+(o==null?"null":o.getClass().getName())+" in "+id);
	}

	void rewrite(IFldExpr:ANode fa) {
		//System.out.println("ProcessPackedFld: rewrite "+fa.getClass().getName()+" "+fa+" in "+id);
		Field f = fa.var;
		if( !f.isPackedField() )
			return;
		MetaPacked mp = f.getMetaPacked();
		if( mp == null || mp.fld.dnode == null ) {
			Kiev.reportError(fa, "Internal error: packed field "+f+" has no packer");
			return;
		}
		ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
		IFldExpr ae = fa.ncopy();
		ae = ae.open();
		ae.symbol = mp.fld.symbol;
		ENode expr = ae;
		if (mp.offset > 0) {
			ConstExpr sexpr = new ConstIntExpr(mp.offset);
			expr = new BinaryExpr(fa.pos, Operator.UnsignedRightShift, expr, sexpr);
		}
		expr = new BinaryExpr(fa.pos, Operator.BitAnd, expr, mexpr);
		if( mp.size == 8 && f.type ≡ Type.tpByte )
			expr = new CastExpr(fa.pos, Type.tpByte, expr);
		else if( mp.size == 16 && f.type ≡ Type.tpShort )
			expr = new CastExpr(fa.pos, Type.tpShort, expr);
		else if( mp.size == 16 && f.type ≡ Type.tpChar )
			expr = new ReinterpExpr(fa.pos, Type.tpChar, expr);
		else if( mp.size == 1 && f.type ≡ Type.tpBoolean )
			expr = new ReinterpExpr(fa.pos, Type.tpBoolean, expr);

		fa.replaceWithNodeReWalk(expr);
	}
	
	void rewrite(AssignExpr:ANode ae) {
		//System.out.println("ProcessPackedFld: rewrite "+ae.getClass().getName()+" "+ae+" in "+id);
		if !(ae.lval instanceof IFldExpr)
			return;
		IFldExpr fa = (IFldExpr)ae.lval;
		Field f = fa.var;
		if( !f.isPackedField() )
			return;
		ae = ae.open();
		fa = fa.open();
		Block be = new Block(ae.pos);
		Object acc;
		if (fa.obj instanceof ThisExpr || fa.obj instanceof SuperExpr) {
			acc = fa.obj;
		}
		else if (fa.obj instanceof LVarExpr) {
			acc = ((LVarExpr)fa.obj).getVar();
		}
		else {
			Var var = new LVar(0,"tmp$acc",fa.obj.getType(),Var.VAR_LOCAL,0);
			var.init = ~fa.obj;
			be.addSymbol(var);
			acc = var;
		}
		Var fval = new LVar(0,"tmp$fldval",Type.tpInt,Var.VAR_LOCAL,0);
		MetaPacked mp = f.getMetaPacked();
		fval.init = new IFldExpr(fa.pos, mkAccess(acc), (Field)mp.fld.dnode);
		be.addSymbol(fval);
		Var tmp = new LVar(0,"tmp$val",Type.tpInt,Var.VAR_LOCAL,0);
		be.addSymbol(tmp);
		if !(ae.op == Operator.Assign || ae.op == Operator.Assign2) {
			ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
			ENode expr = new BinaryExpr(fa.pos, Operator.BitAnd, mkAccess(fval), mexpr);
			if (mp.offset > 0) {
				ConstExpr sexpr = new ConstIntExpr(mp.offset);
				expr = new BinaryExpr(fa.pos, Operator.UnsignedRightShift, expr, sexpr);
			}
			if( mp.size == 8 && f.type ≡ Type.tpByte )
				expr = new CastExpr(fa.pos, Type.tpByte, expr);
			else if( mp.size == 16 && f.type ≡ Type.tpShort )
				expr = new CastExpr(fa.pos, Type.tpShort, expr);
			tmp.init = expr;
			be.stats.add(new ExprStat(new AssignExpr(fa.pos, ae.op, mkAccess(tmp), ~ae.value)));
		}
		else if (ae.value.getType() ≡ Type.tpBoolean) {
			tmp.init = new ReinterpExpr(ae.value.pos, Type.tpInt, ~ae.value);
		}
		else {
			tmp.init = ~ae.value;
		}
		
		{
			ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
			ENode expr_l = new BinaryExpr(fa.pos, Operator.BitAnd, mkAccess(tmp), mexpr);
			if (mp.offset > 0) {
				ConstExpr sexpr = new ConstIntExpr(mp.offset);
				expr_l = new BinaryExpr(fa.pos, Operator.LeftShift, expr_l, sexpr);
			}
			ConstExpr clear = new ConstIntExpr(~(masks[mp.size]<<mp.offset));
			ENode expr_r = new BinaryExpr(fa.pos, Operator.BitAnd, mkAccess(fval), clear);
			ENode expr = new BinaryExpr(fa.pos, Operator.BitOr, expr_r, expr_l);
			expr = new AssignExpr(fa.pos, Operator.Assign,
				new IFldExpr(fa.pos, mkAccess(acc), (Field)mp.fld.dnode),
				expr);
			be.stats.add(new ExprStat(fa.pos, expr));
		}
		if (!ae.isGenVoidExpr()) {
			be.stats.add(mkAccess(tmp));
		}
		ae.replaceWithNodeReWalk(be);
	}
	
	void rewrite(IncrementExpr:ANode ie) {
		//System.out.println("ProcessPackedFld: rewrite "+ie.getClass().getName()+" "+ie+" in "+id);
		if !(ie.lval instanceof IFldExpr)
			return;
		IFldExpr fa = (IFldExpr)ie.lval;
		Field f = fa.var;
		if( !f.isPackedField() )
			return;
		ie = ie.open();
		MetaPacked mp = f.getMetaPacked();
		ENode expr;
		if (ie.isGenVoidExpr()) {
			if (ie.op == Operator.PreIncr || ie.op == Operator.PostIncr) {
				expr = new AssignExpr(ie.pos, Operator.AssignAdd, ~ie.lval, new ConstIntExpr(1));
			} else {
				expr = new AssignExpr(ie.pos, Operator.AssignAdd, ~ie.lval, new ConstIntExpr(-1));
			}
			expr.setGenVoidExpr(true);
		}
		else {
			Block be = new Block(ie.pos);
			Object acc;
			if (fa.obj instanceof ThisExpr || fa.obj instanceof SuperExpr) {
				acc = fa.obj;
			}
			else if (fa.obj instanceof LVarExpr) {
				acc = ((LVarExpr)fa.obj).getVar();
			}
			else {
				Var var = new LVar(0,"tmp$acc",fa.obj.getType(),Var.VAR_LOCAL,0);
				var.init = fa.obj;
				be.addSymbol(var);
				acc = var;
			}
			Var fval = new LVar(0,"tmp$fldval",Type.tpInt,Var.VAR_LOCAL,0);
			fval.init = new IFldExpr(fa.pos, mkAccess(acc), (Field)mp.fld.dnode);
			be.addSymbol(fval);
			Var tmp = new LVar(0,"tmp$val",Type.tpInt,Var.VAR_LOCAL,0);
			be.addSymbol(tmp);
			{
				ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
				ENode expr = new BinaryExpr(fa.pos, Operator.BitAnd, mkAccess(fval), mexpr);
				if (mp.offset > 0) {
					ConstExpr sexpr = new ConstIntExpr(mp.offset);
					expr = new BinaryExpr(fa.pos, Operator.UnsignedRightShift, expr, sexpr);
				}
				if( mp.size == 8 && f.type ≡ Type.tpByte )
					expr = new CastExpr(fa.pos, Type.tpByte, expr);
				else if( mp.size == 16 && f.type ≡ Type.tpShort )
					expr = new CastExpr(fa.pos, Type.tpShort, expr);
				ConstExpr ce;
				if (ie.op == Operator.PreIncr)
					tmp.init = new BinaryExpr(0, Operator.Add, expr, new ConstIntExpr(1));
				else if (ie.op == Operator.PreDecr)
					tmp.init = new BinaryExpr(0, Operator.Sub, expr, new ConstIntExpr(1));
				else
					tmp.init = expr;
			}

			{
				ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
				ENode expr_l;
				if (ie.op == Operator.PostIncr)
					expr_l = new BinaryExpr(fa.pos, Operator.BitAnd, new BinaryExpr(0,Operator.Add,mkAccess(tmp),new ConstIntExpr(1)), mexpr);
				else if (ie.op == Operator.PostDecr)
					expr_l = new BinaryExpr(fa.pos, Operator.BitAnd, new BinaryExpr(0,Operator.Sub,mkAccess(tmp),new ConstIntExpr(1)), mexpr);
				else
					expr_l = new BinaryExpr(fa.pos, Operator.BitAnd, mkAccess(tmp), mexpr);
				if (mp.offset > 0) {
					ConstExpr sexpr = new ConstIntExpr(mp.offset);
					expr_l = new BinaryExpr(fa.pos, Operator.LeftShift, expr_l, sexpr);
				}
				ConstExpr clear = new ConstIntExpr(~(masks[mp.size]<<mp.offset));
				ENode expr_r = new BinaryExpr(fa.pos, Operator.BitAnd, mkAccess(fval), clear);
				ENode expr = new BinaryExpr(fa.pos, Operator.BitOr, expr_r, expr_l);
				expr = new AssignExpr(fa.pos, Operator.Assign,
					new IFldExpr(fa.pos, mkAccess(acc), (Field)mp.fld.dnode),
					expr);
				be.stats.add(new ExprStat(fa.pos, expr));
			}
			if (!ie.isGenVoidExpr()) {
				be.stats.add(mkAccess(tmp));
			}
			expr = be;
		}
		ie.replaceWithNodeReWalk(expr);
	}
	
	private ENode mkAccess(Object o) {
		if (o instanceof Var) return new LVarExpr(0,(Var)o);
		if (o instanceof LVarExpr) return new LVarExpr(0,o.getVar());
		if (o instanceof ThisExpr) return new ThisExpr(0);
		if (o instanceof SuperExpr) return new SuperExpr(0);
		throw new RuntimeException("Unknown accessor "+o);
	}

}
