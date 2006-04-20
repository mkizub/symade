package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@singleton
public final class ProcessPackedFld extends TransfProcessor implements Constants {
	
	private ProcessPackedFld() {
		super(Kiev.Ext.PackedFields);
	}
	
	public void verify(ASTNode:ASTNode node) {
	}
	
	public void verify(FileUnit:ASTNode fu) {
		foreach (ASTNode n; fu.members; n instanceof Struct)
			verify(n);
	}
	
	public void verify(Struct:ASTNode s) {
		foreach (DNode n; s.members; n instanceof Field)
			verify(n);
		foreach (Struct sub; s.sub_clazz)
			verify(sub);
	}
	
	public void verify(Field:ASTNode f) {
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
		Struct s = f.ctx_clazz;
		KString mp_in = mp.getFld();
		if( mp_in != null && mp_in.len > 0 ) {
			Field p = s.resolveField(mp_in,false);
			if( p == null ) {
				Kiev.reportError(f,"Packer field "+mp_in+" not found");
				return;
			}
			if( p.type ≢ Type.tpInt ) {
				Kiev.reportError(f,"Packer field "+p+" is not of 'int' type");
				return;
			}
			mp.packer = p;
			assert( mp.getOffset() >= 0 && mp.getOffset()+mp.getSize() <= 32 );
		}
	}
	
	public BackendProcessor getBackend(Kiev.Backend backend) {
		if (backend == Kiev.Backend.Java15)
			return JavaPackedFldBackend;
		return null;
	}
	
}

@singleton
class JavaPackedFldBackend extends BackendProcessor implements Constants {
	
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

	private JavaPackedFldBackend() {
		super(Kiev.Backend.Java15);
	}
	
	public void preGenerate(ASTNode:ASTNode node) {
		return;
	}
	
	public void preGenerate(FileUnit:ASTNode fu) {
		foreach (ASTNode dn; fu.members; dn instanceof Struct)
			this.preGenerate(dn);
	}
	
	public void preGenerate(Struct:ASTNode s) {
		// Setup packed/packer fields
		foreach(DNode n; s.members; n instanceof Field && ((Field)n).isPackedField() ) {
			Field f = (Field)n;
			Field@ packer;
			// Locate or create nearest packer field that can hold this one
			MetaPacked mp = f.getMetaPacked();
			if( mp.packer == null ) {
				KString mp_in = mp.getFld();
				if( mp_in != null && mp_in.len > 0 ) {
					Field p = s.resolveField(mp_in,false);
					if( p == null ) {
						Kiev.reportError(f,"Packer field "+mp_in+" not found");
						f.delNodeData(MetaPacked.ATTR);
						f.setPackedField(false);
						continue;
					}
					if( p.type ≢ Type.tpInt ) {
						Kiev.reportError(f,"Packer field "+p+" is not of 'int' type");
						f.delNodeData(MetaPacked.ATTR);
						f.setPackedField(false);
						continue;
					}
					mp.packer = p;
					assert( mp.getOffset() >= 0 && mp.getOffset()+mp.getSize() <= 32 );
				}
				else if( locatePackerField(packer,mp.getSize(),s) ) {
					// Found
					mp.packer = packer;
					mp.setFld(packer.name.name);
					MetaPacker mpr = packer.getMetaPacker();
					mp.setOffset(mpr.getSize());
					mpr.setSize(mpr.getSize() + mp.getSize());
				} else {
					// Create
					Field p = new Field(KString.from("$pack$"+countPackerFields(s)),Type.tpInt,ACC_PUBLIC|ACC_SYNTHETIC);
					p.pos = s.pos;
					MetaPacker mpr = new MetaPacker();
					p.addNodeData(mpr, MetaPacker.ATTR);
					p.setPackerField(true);
					s.addField(p);
					mp.packer = p;
					mp.setFld(p.name.name);
					mp.setOffset(0);
					mpr.setSize(mpr.getSize() + mp.getSize());
				}
			}
			foreach(ASTNode n; s.members; n instanceof Struct)
				this.preGenerate(n);
		}
	}

	private int countPackerFields(Struct s) {
		int i = 0;
		foreach (DNode n; s.members; n instanceof Field && ((Field)n).isPackerField()) i++;
		return i;
	}

	private rule locatePackerField(Field@ f, int size, Struct s)
		ASTNode@ n;
		Field ff;
	{
		s.super_type != null,
		locatePackerField(f,size,(Struct)s.super_type.clazz)
	;	n @= s.members,
		n instanceof Field && ((Field)n).isPackerField(),
		ff = (Field)n : ff = null,
		(32-ff.getMetaPacked().getSize()) >= size,
		f ?= ff
	}

	public void rewriteNode(ASTNode fu) {
		fu.walkTree(new TreeWalker() {
			public boolean pre_exec(NodeData n) { if (n instanceof ASTNode) return JavaPackedFldBackend.this.rewrite((ASTNode)n); return false; }
		});
	}
	
	boolean rewrite(ASTNode:ASTNode n) {
		//System.out.println("ProcessPackedFld: rewrite "+(o==null?"null":o.getClass().getName())+" in "+id);
		return true;
	}

	boolean rewrite(IFldExpr:ASTNode fa) {
		//System.out.println("ProcessPackedFld: rewrite "+fa.getClass().getName()+" "+fa+" in "+id);
		Field f = fa.var;
		if( !f.isPackedField() )
			return true;
		MetaPacked mp = f.getMetaPacked();
		if( mp == null || mp.packer == null ) {
			Kiev.reportError(fa, "Internal error: packed field "+f+" has no packer");
			return true;
		}
		ConstExpr mexpr = new ConstIntExpr(masks[mp.getSize()]);
		IFldExpr ae = fa.ncopy();
		ae.var = mp.packer;
		ENode expr = ae;
		if (mp.getOffset() > 0) {
			ConstExpr sexpr = new ConstIntExpr(mp.getOffset());
			expr = new BinaryExpr(fa.pos, BinaryOperator.UnsignedRightShift, expr, sexpr);
		}
		expr = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, expr, mexpr);
		if( mp.getSize() == 8 && f.type ≡ Type.tpByte )
			expr = new CastExpr(fa.pos, Type.tpByte, expr);
		else if( mp.getSize() == 16 && f.type ≡ Type.tpShort )
			expr = new CastExpr(fa.pos, Type.tpShort, expr);
		else if( mp.getSize() == 16 && f.type ≡ Type.tpChar )
			expr = new ReinterpExpr(fa.pos, Type.tpChar, expr);
		else if( mp.getSize() == 1 && f.type ≡ Type.tpBoolean )
			expr = new ReinterpExpr(fa.pos, Type.tpBoolean, expr);

		fa.replaceWithNode(expr);
		rewriteNode(expr);
		return false;
	}
	
	boolean rewrite(AssignExpr:ASTNode ae) {
		//System.out.println("ProcessPackedFld: rewrite "+ae.getClass().getName()+" "+ae+" in "+id);
		if !(ae.lval instanceof IFldExpr)
			return true;
		IFldExpr fa = (IFldExpr)ae.lval;
		Field f = fa.var;
		if( !f.isPackedField() )
			return true;
		Block be = new Block(ae.pos);
		Object acc;
		if (fa.obj instanceof ThisExpr) {
			acc = fa.obj;
		}
		else if (fa.obj instanceof LVarExpr) {
			acc = ((LVarExpr)fa.obj).getVar();
		}
		else {
			Var var = new Var(0,KString.from("tmp$acc"),fa.obj.getType(),0);
			var.init = ~fa.obj;
			be.addSymbol(var);
			acc = var;
		}
		Var fval = new Var(0,KString.from("tmp$fldval"),Type.tpInt,0);
		MetaPacked mp = f.getMetaPacked();
		fval.init = new IFldExpr(fa.pos, mkAccess(acc), mp.packer);
		be.addSymbol(fval);
		Var tmp = new Var(0,KString.from("tmp$val"),Type.tpInt,0);
		be.addSymbol(tmp);
		if !(ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2) {
			ConstExpr mexpr = new ConstIntExpr(masks[mp.getSize()]);
			ENode expr = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(fval), mexpr);
			if (mp.getOffset() > 0) {
				ConstExpr sexpr = new ConstIntExpr(mp.getOffset());
				expr = new BinaryExpr(fa.pos, BinaryOperator.UnsignedRightShift, expr, sexpr);
			}
			if( mp.getSize() == 8 && f.type ≡ Type.tpByte )
				expr = new CastExpr(fa.pos, Type.tpByte, expr);
			else if( mp.getSize() == 16 && f.type ≡ Type.tpShort )
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
			ConstExpr mexpr = new ConstIntExpr(masks[mp.getSize()]);
			ENode expr_l = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(tmp), mexpr);
			if (mp.getOffset() > 0) {
				ConstExpr sexpr = new ConstIntExpr(mp.getOffset());
				expr_l = new BinaryExpr(fa.pos, BinaryOperator.LeftShift, expr_l, sexpr);
			}
			ConstExpr clear = new ConstIntExpr(~(masks[mp.getSize()]<<mp.getOffset()));
			ENode expr_r = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(fval), clear);
			ENode expr = new BinaryExpr(fa.pos, BinaryOperator.BitOr, expr_r, expr_l);
			expr = new AssignExpr(fa.pos, AssignOperator.Assign,
				new IFldExpr(fa.pos, mkAccess(acc), mp.packer),
				expr);
			be.stats.add(new ExprStat(fa.pos, expr));
		}
		if (!ae.isGenVoidExpr()) {
			be.stats.add(mkAccess(tmp));
		}
		ae.replaceWithNode(be);
		be.resolve(ae.isGenVoidExpr() ? Type.tpVoid : ae.getType());
		rewriteNode(be);
		return false;
	}
	
	boolean rewrite(IncrementExpr:ASTNode ie) {
		//System.out.println("ProcessPackedFld: rewrite "+ie.getClass().getName()+" "+ie+" in "+id);
		if !(ie.lval instanceof IFldExpr)
			return true;
		IFldExpr fa = (IFldExpr)ie.lval;
		Field f = fa.var;
		if( !f.isPackedField() )
			return true;
		MetaPacked mp = f.getMetaPacked();
		ENode expr;
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
			Block be = new Block(ie.pos);
			Object acc;
			if (fa.obj instanceof ThisExpr) {
				acc = fa.obj;
			}
			else if (fa.obj instanceof LVarExpr) {
				acc = ((LVarExpr)fa.obj).getVar();
			}
			else {
				Var var = new Var(0,KString.from("tmp$acc"),fa.obj.getType(),0);
				var.init = fa.obj;
				be.addSymbol(var);
				acc = var;
			}
			Var fval = new Var(0,KString.from("tmp$fldval"),Type.tpInt,0);
			fval.init = new IFldExpr(fa.pos, mkAccess(acc), mp.packer);
			be.addSymbol(fval);
			Var tmp = new Var(0,KString.from("tmp$val"),Type.tpInt,0);
			be.addSymbol(tmp);
			{
				ConstExpr mexpr = new ConstIntExpr(masks[mp.getSize()]);
				ENode expr = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(fval), mexpr);
				if (mp.getOffset() > 0) {
					ConstExpr sexpr = new ConstIntExpr(mp.getOffset());
					expr = new BinaryExpr(fa.pos, BinaryOperator.UnsignedRightShift, expr, sexpr);
				}
				if( mp.getSize() == 8 && f.type ≡ Type.tpByte )
					expr = new CastExpr(fa.pos, Type.tpByte, expr);
				else if( mp.getSize() == 16 && f.type ≡ Type.tpShort )
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
				ConstExpr mexpr = new ConstIntExpr(masks[mp.getSize()]);
				ENode expr_l;
				if (ie.op == PostfixOperator.PostIncr)
					expr_l = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, new BinaryExpr(0,BinaryOperator.Add,mkAccess(tmp),new ConstIntExpr(1)), mexpr);
				else if (ie.op == PostfixOperator.PostDecr)
					expr_l = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, new BinaryExpr(0,BinaryOperator.Sub,mkAccess(tmp),new ConstIntExpr(1)), mexpr);
				else
					expr_l = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(tmp), mexpr);
				if (mp.getOffset() > 0) {
					ConstExpr sexpr = new ConstIntExpr(mp.getOffset());
					expr_l = new BinaryExpr(fa.pos, BinaryOperator.LeftShift, expr_l, sexpr);
				}
				ConstExpr clear = new ConstIntExpr(~(masks[mp.getSize()]<<mp.getOffset()));
				ENode expr_r = new BinaryExpr(fa.pos, BinaryOperator.BitAnd, mkAccess(fval), clear);
				ENode expr = new BinaryExpr(fa.pos, BinaryOperator.BitOr, expr_r, expr_l);
				expr = new AssignExpr(fa.pos, AssignOperator.Assign,
					new IFldExpr(fa.pos, mkAccess(acc), mp.packer),
					expr);
				be.stats.add(new ExprStat(fa.pos, expr));
			}
			if (!ie.isGenVoidExpr()) {
				be.stats.add(mkAccess(tmp));
			}
			expr = be;
			expr.resolve(ie.isGenVoidExpr() ? Type.tpVoid : ie.getType());
		}
		ie.replaceWithNode(expr);
		rewriteNode(expr);
		return false;
	}
	
	private ENode mkAccess(Object o) {
		if (o instanceof Var) return new LVarExpr(0,(Var)o);
		if (o instanceof LVarExpr) return new LVarExpr(0,o.getVar());
		if (o instanceof ThisExpr) return new ThisExpr(0);
		throw new RuntimeException("Unknown accessor "+o);
	}

}
