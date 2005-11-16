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

public final class ProcessVirtFld extends TransfProcessor implements Constants {
	
	public ProcessVirtFld(Kiev.Ext ext) {
		super(ext);
	}

	////////////////////////////////////////////////////
	//	   PASS - autoGenerateMembers                 //
	////////////////////////////////////////////////////

	public void autoGenerateMembers(ASTNode:ASTNode node) {
		return;
	}
	
	public void autoGenerateMembers(FileUnit:ASTNode fu) {
		foreach (DNode dn; fu.members; dn instanceof Struct)
			this.autoGenerateMembers(dn);
	}
	
	public void autoGenerateMembers(Struct:ASTNode s) {
		addAbstractFields(s);
		foreach(Struct sub; s.sub_clazz)
			autoGenerateMembers(sub);
	}
	
	public void addAbstractFields(Struct s) {
		foreach(ASTNode n; s.members; n instanceof Method) {
			Method m = (Method)n;
			//trace(Kiev.debugCreation,"check "+m.name.name+" to be a setter");
			if (m.name.name.startsWith(nameSet))
				addSetterForAbstractField(s, m.name.name.substr(nameSet.length()), m);
			foreach (KString name; m.name.aliases) {
				//trace(Kiev.debugCreation,"check "+name+" to be a setter");
				if (name.startsWith(nameSet))
					addSetterForAbstractField(s, name.substr(nameSet.length()), m);
			}
			//trace(Kiev.debugCreation,"check "+m.name.name+" to be a getter");
			if (m.name.name.startsWith(nameGet))
				addGetterForAbstractField(s, m.name.name.substr(nameGet.length()), m);
			foreach (KString name; m.name.aliases) {
				//trace(Kiev.debugCreation,"check "+name+" to be a getter");
				if (name.startsWith(nameGet))
					addGetterForAbstractField(s, name.substr(nameGet.length()), m);
			}
		}
	}
	
	private void addSetterForAbstractField(Struct s, KString name, Method m) {
		Field f = s.resolveField( name, false );
		if( f != null ) {
			trace(Kiev.debugCreation,"method "+m+" has field "+f);
			if (f.parent != m.parent)
				return;
			MetaVirtual mv = f.getMetaVirtual();
			if (mv != null && mv.set != null && mv.set != m)
				return;
		} else {
			s.addField(f=new Field(name,m.type.args[0],m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT));
			f.acc.flags = 0;
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		if (f.getMetaVirtual() == null)
			f.meta.set(new MetaVirtual());
		f.getMetaVirtual().set = m;
		if( m.isPublic() ) {
			f.acc.w_public = true;
			f.acc.w_protected = true;
			f.acc.w_default = true;
			f.acc.w_private = true;
		}
		else if( m.isPrivate() ) {
			f.acc.w_public = false;
			f.acc.w_protected = false;
			f.acc.w_default = false;
			f.acc.w_private = true;
		}
		else if( m.isProtected() ) {
			f.acc.w_public = false;
			f.acc.w_private = true;
		}
		else {
			f.acc.w_public = false;
			f.acc.w_default = true;
			f.acc.w_private = true;
		}
		f.acc.flags |= f.acc.flags << 16;
		f.acc.verifyAccessDecl(f);
	}
	
	private void addGetterForAbstractField(Struct s, KString name, Method m) {
		Field f = s.resolveField( name, false );
		if( f != null ) {
			trace(Kiev.debugCreation,"method "+m+" has field "+f);
			if (f.parent != m.parent)
				return;
			MetaVirtual mv = f.getMetaVirtual();
			if (mv != null && mv.get != null && mv.get != m)
				return;
		} else {
			s.addField(f=new Field(name,m.type.ret,m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT));
			f.acc.flags = 0;
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		if (f.getMetaVirtual() == null)
			f.meta.set(new MetaVirtual());
		f.getMetaVirtual().get = m;
		if( m.isPublic() ) {
			f.acc.r_public = true;
			f.acc.r_protected = true;
			f.acc.r_default = true;
			f.acc.r_private = true;
		}
		else if( m.isPrivate() ) {
			f.acc.r_public = false;
			f.acc.r_protected = false;
			f.acc.r_default = false;
			f.acc.r_private = true;
		}
		else if( m.isProtected() ) {
			f.acc.r_public = false;
			f.acc.r_private = true;
		}
		else {
			f.acc.r_public = false;
			f.acc.r_default = true;
			f.acc.r_private = true;
		}
		f.acc.flags |= f.acc.flags << 16;
		f.acc.verifyAccessDecl(f);
	}
	
	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void preGenerate(ASTNode:ASTNode node) {
		return;
	}
	
	public void preGenerate(FileUnit:ASTNode fu) {
		foreach (DNode dn; fu.members; dn instanceof Struct)
			this.preGenerate(dn);
	}
	
	public void preGenerate(Struct:ASTNode s) {
		foreach(ASTNode n; s.members; n instanceof Field)
			addMethodsForVirtualField(s, (Field)n);
		foreach(ASTNode n; s.members; n instanceof Field) {
			Field f = (Field)n;
			if (!f.isVirtual())
				continue;
			if (s.isInterface() && !n.isAbstract())
				f.setAbstract(true);
		}
		foreach(Struct sub; s.sub_clazz)
			preGenerate(sub);
	}
	
	private static void addMethodsForVirtualField(Struct s, Field f) {
		if( f.isStatic() && f.isVirtual() ) {
			Kiev.reportError(f,"Static fields can't be virtual");
			f.setVirtual(false);
		}
		if( s.isInterface() && f.isVirtual() ) f.setAbstract(true);

		if( !f.isVirtual() ) return;

		if (f.getMetaVirtual() == null)
			f.meta.set(new MetaVirtual());

		// Check set$/get$ methods
		boolean set_found = false;
		boolean get_found = false;

		KString set_name = new KStringBuffer(nameSet.length()+f.name.name.length()).
			append_fast(nameSet).append_fast(f.name.name).toKString();
		KString get_name = new KStringBuffer(nameGet.length()+f.name.name.length()).
			append_fast(nameGet).append_fast(f.name.name).toKString();

		foreach(ASTNode n; s.members; n instanceof Method) {
			Method m = (Method)n;
			if( m.name.equals(set_name) ) {
				set_found = true;
				if( get_found ) break;
			}
			else if( m.name.equals(get_name) ) {
				get_found = true;
				if( set_found ) break;
			}
		}
		if( !set_found && f.acc.writeable() ) {
			Method set_var = new Method(set_name,
				MethodType.newMethodType(null,new Type[]{f.type},Type.tpVoid),
				f.getJavaFlags()
			);
			if (f.meta.get(ProcessVNode.mnAtt) != null)
				set_var.setFinal(true);
			s.addMethod(set_var);
			FormPar self;
			FormPar value;
			if (f.isStatic()) {
				self = null;
				value = new FormPar(f.pos,KString.from("value"),f.type,0);
				set_var.params.add(value);
			} else {
				self = set_var.getThisPar();
				value = new FormPar(f.pos,KString.from("value"),f.type,0);
				set_var.params.add(value);
			}
			if( !f.isAbstract() ) {
				BlockStat body = new BlockStat(f.pos,ENode.emptyArray);
				Type astT = Type.fromSignature(KString.from("Lkiev/vlang/ASTNode;"));
				if (f.meta.get(ProcessVNode.mnAtt) != null && f.type.isInstanceOf(astT)) {
					Statement p_st = new IfElseStat(0,
							new BinaryBoolExpr(0, BinaryOperator.NotEquals,
								new AccessExpr(0,new ThisExpr(0),f,true),
								new ConstNullExpr()
							),
							new BlockStat(0,new Statement[]{
								new ExprStat(0,
									new ASTCallAccessExpression(0,
										new AccessExpr(0,new ThisExpr(0),f,true),
										KString.from("callbackDetached"),
										ENode.emptyArray
									)
								)
							}),
							null
						);
					body.stats.append(p_st);
				}
				Statement ass_st = new ExprStat(f.pos,
					new AssignExpr(f.pos,AssignOperator.Assign,
						f.isStatic()? new StaticFieldAccessExpr(f.pos,f,true)
									: new AccessExpr(f.pos,new ThisExpr(0),f,true),
						new VarExpr(f.pos,value)
					)
				);
				body.stats.append(ass_st);
				if (f.meta.get(ProcessVNode.mnAtt) != null && f.type.isInstanceOf(astT)) {
					KString fname = new KStringBuffer().append("nodeattr$").append(f.name.name).toKString();
					Field fatt = ((Struct)f.parent).resolveField(fname);
					Statement p_st = new IfElseStat(0,
							new BinaryBoolExpr(0, BinaryOperator.NotEquals,
								new VarExpr(0, value),
								new ConstNullExpr()
							),
							new ExprStat(0,
								new ASTCallAccessExpression(0,
									new VarExpr(0, value),
									KString.from("callbackAttached"),
									new ENode[] {
										new ThisExpr(),
										new StaticFieldAccessExpr(f.pos, fatt)
									}
								)
							),
							null
						);
					body.stats.append(p_st);
				}
				body.stats.append(new ReturnStat(f.pos,null));
				set_var.body = body;
			}
			f.getMetaVirtual().set = set_var;
		}
		else if( set_found && !f.acc.writeable() ) {
			Kiev.reportError(f,"Virtual set$ method for non-writeable field");
		}

		if (!f.isVirtual())
			return;		// no need to generate getter
		if( !get_found && f.acc.readable()) {
			Method get_var = new Method(get_name,
				MethodType.newMethodType(null,Type.emptyArray,f.type),
				f.getJavaFlags()
			);
			if (f.meta.get(ProcessVNode.mnAtt) != null)
				get_var.setFinal(true);
			s.addMethod(get_var);
			FormPar self = get_var.getThisPar();
			if( !f.isAbstract() ) {
				BlockStat body = new BlockStat(f.pos,ENode.emptyArray);
				body.stats.add(new ReturnStat(f.pos,new AccessExpr(f.pos,new ThisExpr(0),f,true)));
				get_var.body = body;
			}
			f.getMetaVirtual().get = get_var;
		}
		else if( get_found && !f.acc.readable() ) {
			Kiev.reportError(f,"Virtual get$ method for non-readable field");
		}
	}

	
	////////////////////////////////////////////////////
	//	   PASS - rewrite code                        //
	////////////////////////////////////////////////////

	public void rewriteNode(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { return ProcessVirtFld.this.rewrite(n); }
		});
	}
	
	boolean rewrite(ASTNode:ASTNode o) {
		//System.out.println("ProcessVirtFld: rewrite "+(o==null?"null":o.getClass().getName())+" in "+id);
		return true;
	}

	boolean rewrite(AccessExpr:ASTNode fa) {
		//System.out.println("ProcessVirtFld: rewrite "+fa.getClass().getName()+" "+fa+" in "+id);
		Field f = fa.var;
		if( !f.isVirtual() || fa.isAsField() )
			return true;
		KString get_name = new KStringBuffer(nameGet.length()+f.name.name.length()).
			append_fast(nameGet).append_fast(f.name.name).toKString();

		if (fa.pctx.method != null && fa.pctx.method.name.equals(get_name)) {
			fa.setAsField(true);
			return true;
		}
		// We rewrite by get$ method. set$ method is rewritten by AssignExpr
		if (f.getMetaVirtual().get == null) {
			Kiev.reportError(fa, "Getter method for virtual field "+f+" not found");
			fa.setAsField(true);
			return true;
		}
		Expr ce = new CallExpr(fa.pos, (ENode)~fa.obj, f.getMetaVirtual().get, Expr.emptyArray);
		//ce = ce.resolveExpr(fa.getType());
		fa.replaceWithNode(ce);
		rewriteNode(ce);
		return false;
	}
	
	boolean rewrite(AssignExpr:ASTNode ae) {
		//System.out.println("ProcessVirtFld: rewrite "+ae.getClass().getName()+" "+ae+" in "+id);
		if (ae.lval instanceof AccessExpr) {
			AccessExpr fa = (AccessExpr)ae.lval;
			Field f = fa.var;
			if( !f.isVirtual() || fa.isAsField() )
				return true;
			KString set_name = new KStringBuffer(nameSet.length()+f.name.name.length()).
				append_fast(nameSet).append_fast(f.name.name).toKString();
	
			if (ae.pctx.method != null && ae.pctx.method.name.equals(set_name)) {
				fa.setAsField(true);
				return true;
			}
			// Rewrite by set$ method
			if (f.getMetaVirtual().set == null) {
				Kiev.reportError(fa, "Setter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			if (f.getMetaVirtual().get == null && (!ae.isGenVoidExpr() || !(ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2))) {
				Kiev.reportError(fa, "Getter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			BinaryOperator op = null;
			if      (ae.op == AssignOperator.AssignAdd)                  op = BinaryOperator.Add;
			else if (ae.op == AssignOperator.AssignSub)                  op = BinaryOperator.Sub;
			else if (ae.op == AssignOperator.AssignMul)                  op = BinaryOperator.Mul;
			else if (ae.op == AssignOperator.AssignDiv)                  op = BinaryOperator.Div;
			else if (ae.op == AssignOperator.AssignMod)                  op = BinaryOperator.Mod;
			else if (ae.op == AssignOperator.AssignLeftShift)            op = BinaryOperator.LeftShift;
			else if (ae.op == AssignOperator.AssignRightShift)           op = BinaryOperator.RightShift;
			else if (ae.op == AssignOperator.AssignUnsignedRightShift)   op = BinaryOperator.UnsignedRightShift;
			else if (ae.op == AssignOperator.AssignBitOr)                op = BinaryOperator.BitOr;
			else if (ae.op == AssignOperator.AssignBitXor)               op = BinaryOperator.BitXor;
			else if (ae.op == AssignOperator.AssignBitAnd)               op = BinaryOperator.BitAnd;
			Expr expr;
			if (ae.isGenVoidExpr() && (ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2)) {
				expr = new CallExpr(ae.pos, (ENode)~fa.obj, f.getMetaVirtual().set, new ENode[]{(ENode)~ae.value});
			}
			else {
				BlockExpr be = new BlockExpr(ae.pos);
				Object acc;
				if (fa.obj instanceof ThisExpr) {
					acc = (ENode)~fa.obj;
				}
				else if (fa.obj instanceof VarExpr) {
					acc = ((VarExpr)fa.obj).getVar();
				}
				else {
					Var var = new Var(0,KString.from("tmp$virt"),fa.obj.getType(),0);
					var.init = (ENode)~fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				ENode g;
				if !(ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2) {
					g = new CallExpr(0, mkAccess(acc), f.getMetaVirtual().get, Expr.emptyArray);
					g = new BinaryExpr(ae.pos, op, g, (ENode)~ae.value);
				} else {
					g = (ENode)~ae.value;
				}
				g = new CallExpr(ae.pos, mkAccess(acc), f.getMetaVirtual().set, new ENode[]{g});
				be.addStatement(new ExprStat(0, g));
				if (!ae.isGenVoidExpr()) {
					g = new CallExpr(0, mkAccess(acc), f.getMetaVirtual().get, ENode.emptyArray);
					be.setExpr(g);
				}
				expr = be;
			}
			ae.replaceWithNode(expr);
			expr.resolve(ae.isGenVoidExpr() ? Type.tpVoid : ae.getType());
			rewriteNode(expr);
			return false;
		}
		return true;
	}
	
	boolean rewrite(IncrementExpr:ASTNode ie) {
		//System.out.println("ProcessVirtFld: rewrite "+ie.getClass().getName()+" "+ie+" in "+id);
		if (ie.lval instanceof AccessExpr) {
			AccessExpr fa = (AccessExpr)ie.lval;
			Field f = fa.var;
			if( !f.isVirtual() || fa.isAsField() )
				return true;
			KString set_name = new KStringBuffer(nameSet.length()+f.name.name.length()).
				append_fast(nameSet).append_fast(f.name.name).toKString();
			KString get_name = new KStringBuffer(nameGet.length()+f.name.name.length()).
				append_fast(nameGet).append_fast(f.name.name).toKString();
	
			if (ie.pctx.method != null && (ie.pctx.method.name.equals(set_name) || ie.pctx.method.name.equals(get_name))) {
				fa.setAsField(true);
				return true;
			}
			// Rewrite by set$ method
			if (f.getMetaVirtual().set == null) {
				Kiev.reportError(fa, "Setter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			if (f.getMetaVirtual().get == null) {
				Kiev.reportError(fa, "Getter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			Expr expr;
			if (ie.isGenVoidExpr()) {
				if (ie.op == PrefixOperator.PreIncr || ie.op == PostfixOperator.PostIncr) {
					expr = new AssignExpr(ie.pos, AssignOperator.AssignAdd, ie.lval, new ConstIntExpr(1));
				} else {
					expr = new AssignExpr(ie.pos, AssignOperator.AssignAdd, ie.lval, new ConstIntExpr(-1));
				}
			}
			else {
				BlockExpr be = new BlockExpr(ie.pos);
				Object acc;
				if (fa.obj instanceof ThisExpr) {
					acc = fa.obj;
				}
				else if (fa.obj instanceof VarExpr) {
					acc = ((VarExpr)fa.obj).getVar();
				}
				else {
					Var var = new Var(0,KString.from("tmp$virt"),fa.obj.getType(),0);
					var.init = fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				Var res = null;
				if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr) {
					res = new Var(0,KString.from("tmp$res"),f.getType(),0);
					be.addSymbol(res);
				}
				ConstExpr ce;
				if (ie.op == PrefixOperator.PreIncr || ie.op == PostfixOperator.PostIncr)
					ce = new ConstIntExpr(1);
				else
					ce = new ConstIntExpr(-1);
				Expr g;
				g = new CallExpr(0, mkAccess(acc), f.getMetaVirtual().get, Expr.emptyArray);
				if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr)
					g = new AssignExpr(ie.pos, AssignOperator.Assign, mkAccess(res), g);
				g = new CallExpr(ie.pos, mkAccess(acc), f.getMetaVirtual().set, new Expr[]{g});
				be.addStatement(new ExprStat(0, g));
				if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr)
					be.setExpr(mkAccess(res));
				else
					be.setExpr(new CallExpr(0, mkAccess(acc), f.getMetaVirtual().get, Expr.emptyArray));
				expr = be;
			}
			ie.replaceWithNode(expr);
			expr.setGenVoidExpr(ie.isGenVoidExpr());
			expr.resolve(ie.isGenVoidExpr() ? Type.tpVoid : ie.getType());
			rewriteNode(expr);
			return false;
		}
		return true;
	}
	
	private Expr mkAccess(Object o) {
		if (o instanceof Var) return new VarExpr(0,(Var)o);
		if (o instanceof VarExpr) return new VarExpr(0,o.getVar());
		if (o instanceof ThisExpr) return new ThisExpr(0);
		throw new RuntimeException("Unknown accessor "+o);
	}
}