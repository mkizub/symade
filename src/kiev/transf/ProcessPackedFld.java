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
public final class PackedFldFE_Verify extends VerifyProcessor {
	private PackedFldFE_Verify() { super(KievExt.PackedFields); }

	public void verify(ASTNode node) {
		if (node instanceof Field && !node.isInterfaceOnly())
			verifyField((Field)node);
	}
	private void verifyField(Field f) {
		MetaPacked mp = f.getMetaPacked();
		if (mp == null)
			return;
		if (!f.isAbstract()) {
			Kiev.reportWarning(f,"Packed field "+f+" must be abstract");
			f.setAbstract(true);
		}
		TypeDecl s = f.ctx_tdecl;
		String mp_in = mp.getS("in");
		if( mp_in != null && mp_in.length() > 0 ) {
			Field p = s.resolveField(mp_in.intern(),false);
			if( p == null ) {
				Kiev.reportError(f,"Packer field "+mp_in+" not found");
				return;
			}
			if( p.getType() ≢ Type.tpInt ) {
				Kiev.reportError(f,"Packer field "+p+" is not of 'int' type");
				return;
			}
			mp.fld.symbol = p.symbol;
			assert( mp.offset >= 0 && mp.offset+mp.size <= 32 );
		}
	}
}

@singleton
public class PackedFldME_PreGenerate extends BackendProcessor {
	private PackedFldME_PreGenerate() { super(KievBackend.Java15); }
	public String getDescr() { "Packed fields pre-generation" }

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

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"PackedFldME_PreGenerate");
		try {
			doProcess(node);
		} finally { tr.leave(); }
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode s) {
		// Setup packed/packer fields
		MetaPacked mp;
		foreach(Field f; s.members; (mp=f.getMetaPacked()) != null) {
			Field@ packer;
			// Locate or create nearest packer field that can hold this one
			if (!f.isInterfaceOnly() && mp.fld.dnode == null) {
				String mp_in = mp.getS("in");
				if( mp_in != null && mp_in.length() > 0 ) {
					Field p = s.resolveField(mp_in,false);
					if( p == null ) {
						Kiev.reportError(f,"Packer field "+mp_in+" not found");
						~mp;
						continue;
					}
					if( p.getType() ≢ Type.tpInt ) {
						Kiev.reportError(f,"Packer field "+p+" is not of 'int' type");
						~mp;
						continue;
					}
					mp.fld.symbol = p.symbol;
					assert( mp.offset >= 0 && mp.offset+mp.size <= 32 );
				}
				else if (locatePackerField(packer,mp.size,s)) {
					// Found
					mp.fld.symbol = packer.symbol;
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
					mp.fld.symbol = p.symbol;
					mp.offset = 0;
					mpr.size += mp.size;
				}
			}
			f.setVirtual(true);
			String set_name = (nameSet+f.sname).intern();
			String get_name = (nameGet+f.sname).intern();
			// setter
			if (!f.isFinal() && MetaAccess.writeable(f)) {
				Method set_var = new MethodSetter(f);
				set_var.setFinal(true);
				set_var.setAbstract(false);
				if (s.isInterface())
					set_var.setFinal(false);
				s.addMethod(set_var);
				Var value = set_var.params[0];
				if (!f.isInterfaceOnly()) {
					Block body = new Block(f.pos);
					set_var.body = body;
	
					Field mpfld = (Field)mp.fld.dnode;
					Var fval = new LVar(0,"tmp$fldval",Type.tpInt,Var.VAR_LOCAL,0);
					if (mpfld.isStatic())
						fval.init = new SFldExpr(f.pos,mpfld);
					else
						fval.init = new IFldExpr(f.pos,new ThisExpr(0),mpfld);
					body.addSymbol(fval);
					Var tmp = new LVar(0,"tmp$val",Type.tpInt,Var.VAR_LOCAL,0);
					if (f.getType() ≡ Type.tpBoolean)
						tmp.init = new ReinterpExpr(f.pos, Type.tpInt, new LVarExpr(f.pos,value));
					else
						tmp.init = new LVarExpr(f.pos,value);
					body.addSymbol(tmp);
	
					ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
					ENode expr_l = new BinaryExpr(f.pos, Operator.BitAnd, new LVarExpr(f.pos,tmp), mexpr);
					if (mp.offset > 0) {
						ConstExpr sexpr = new ConstIntExpr(mp.offset);
						expr_l = new BinaryExpr(f.pos, Operator.LeftShift, expr_l, sexpr);
					}
					ConstExpr clear = new ConstIntExpr(~(masks[mp.size]<<mp.offset));
					ENode expr_r = new BinaryExpr(f.pos, Operator.BitAnd, new LVarExpr(f.pos,fval), clear);
					ENode expr = new BinaryExpr(f.pos, Operator.BitOr, expr_r, expr_l);
					if (mpfld.isStatic())
						expr = new AssignExpr(f.pos, Operator.Assign, new SFldExpr(f.pos,mpfld), expr);
					else
						expr = new AssignExpr(f.pos, Operator.Assign, new IFldExpr(f.pos,new ThisExpr(0),mpfld), expr);
					body.stats.add(new ExprStat(f.pos, expr));
				}

				f.setter = new SymbolRef<Method>(set_var);
			}
			// getter
			if(MetaAccess.readable(f)) {
				Method get_var = new MethodGetter(f);
				get_var.setFinal(true);
				get_var.setAbstract(false);
				if (s.isInterface())
					get_var.setFinal(false);
				s.addMethod(get_var);
				if (!f.isInterfaceOnly()) {
					Block body = new Block(f.pos);
					get_var.body = body;
					
					ConstExpr mexpr = new ConstIntExpr(masks[mp.size]);
					Field mpfld = (Field)mp.fld.dnode;
					ENode expr;
					if (mpfld.isStatic())
						expr = new SFldExpr(f.pos,mpfld);
					else
						expr = new IFldExpr(f.pos,new ThisExpr(0),mpfld);
					if (mp.offset > 0) {
						ConstExpr sexpr = new ConstIntExpr(mp.offset);
						expr = new BinaryExpr(f.pos, Operator.UnsignedRightShift, expr, sexpr);
					}
					expr = new BinaryExpr(f.pos, Operator.BitAnd, expr, mexpr);
					if( mp.size == 8 && f.getType() ≡ Type.tpByte )
						expr = new CastExpr(f.pos, Type.tpByte, expr);
					else if( mp.size == 16 && f.getType() ≡ Type.tpShort )
						expr = new CastExpr(f.pos, Type.tpShort, expr);
					else if( mp.size == 16 && f.getType() ≡ Type.tpChar )
						expr = new ReinterpExpr(f.pos, Type.tpChar, expr);
					else if( mp.size == 1 && f.getType() ≡ Type.tpBoolean )
						expr = new ReinterpExpr(f.pos, Type.tpBoolean, expr);
					
					body.stats.add(new ReturnStat(f.pos,expr));
				}
				
				f.getter = new SymbolRef<Method>(get_var);
			}
		}
		foreach(Struct n; s.members)
			this.doProcess(n);
	}

	private int countPackerFields(Struct s) {
		int i = 0;
		foreach (Field f; s.members; f.getMetaPacker() != null) i++;
		return i;
	}

	private rule locatePackerField(Field@ f, int size, Struct s)
		ASTNode@ n;
		Field ff;
	{
		s.super_types.length > 0,
		locatePackerField(f,size,s.super_types[0].getStruct())
	;	n @= s.members,
		n instanceof Field && ((Field)n).getMetaPacker() != null,
		ff = (Field)n : ff = null,
		(32-ff.getMetaPacked().size) >= size,
		f ?= ff
	}

}
