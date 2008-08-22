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
	
////////////////////////////////////////////////////
//	   PASS - rewrite code                        //
////////////////////////////////////////////////////

@singleton
public class InnerBE_Rewrite extends BackendProcessor implements Constants {

	private InnerBE_Rewrite() { super(KievBackend.Java15); }
	public String getDescr() { "Inner classes access rewrite" }

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"InnerBE_Rewrite");
		try {
			node.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return InnerBE_Rewrite.this.rewrite((ASTNode)n); return false; }
			});
		} finally { tr.leave(); }
	}
	
	boolean rewrite(ASTNode:ASTNode o) {
		return true;
	}

	boolean rewrite(DNode:ASTNode dn) {
		if (dn.isMacro())
			return false;
		return true;
	}

	boolean rewrite(IFldExpr:ASTNode fa) {
		Field f = fa.var;
		if (!MetaAccess.accessedFromInner(fa,f))
			return true;
		Method getter = f.makeReadAccessor();
		ENode ce = new CallExpr(fa.pos, new TypeRef(getter.ctx_tdecl.xtype), getter, new ENode[]{ ~fa.obj });
		fa.replaceWithNodeReWalk(ce);
		throw new Error();
	}
	
	boolean rewrite(SFldExpr:ASTNode fa) {
		Field f = fa.var;
		if (!MetaAccess.accessedFromInner(fa,f))
			return true;
		Method getter = f.makeReadAccessor();
		ENode ce = new CallExpr(fa.pos, new TypeRef(getter.ctx_tdecl.xtype), getter, ENode.emptyArray);
		fa.replaceWithNodeReWalk(ce);
		throw new Error();
	}
	
	boolean rewrite(CallExpr:ASTNode ce) {
		Method func = ce.func;
		if (func == null || !MetaAccess.accessedFromInner(ce,func))
			return true;
		Method m = func.makeAccessor();
		ce.symbol = m.symbol;
		if (!func.isStatic()) {
			ce.args.insert(0, ~ce.obj);
			ce.obj = new TypeRef(m.ctx_tdecl.xtype);
		}
		return true;
	}
	
	boolean rewrite(NewExpr:ASTNode ne) {
		Method func = ne.func;
		if (func == null || !MetaAccess.accessedFromInner(ne,func))
			return true;
		Method m = func.makeAccessor();
		CallExpr ce = new CallExpr(ne.pos, new TypeRef(m.ctx_tdecl.xtype), m, ne.args.delToArray());
		ce.setGenVoidExpr(ne.isGenVoidExpr());
		ne.replaceWithNodeReWalk(ce);
		return true;
	}
	
	boolean rewrite(AssignExpr:ASTNode ae) {
		if (ae.lval instanceof IFldExpr) {
			IFldExpr fa = (IFldExpr)ae.lval;
			Field f = fa.var;
			if (!MetaAccess.accessedFromInner(fa,f))
				return true;

			Type ae_tp = ae.isGenVoidExpr() ? Type.tpVoid : ae.getType();
			Operator op = null;
			if      (ae.op == Operator.AssignAdd)                  op = Operator.Add;
			else if (ae.op == Operator.AssignSub)                  op = Operator.Sub;
			else if (ae.op == Operator.AssignMul)                  op = Operator.Mul;
			else if (ae.op == Operator.AssignDiv)                  op = Operator.Div;
			else if (ae.op == Operator.AssignMod)                  op = Operator.Mod;
			else if (ae.op == Operator.AssignLeftShift)            op = Operator.LeftShift;
			else if (ae.op == Operator.AssignRightShift)           op = Operator.RightShift;
			else if (ae.op == Operator.AssignUnsignedRightShift)   op = Operator.UnsignedRightShift;
			else if (ae.op == Operator.AssignBitOr)                op = Operator.BitOr;
			else if (ae.op == Operator.AssignBitXor)               op = Operator.BitXor;
			else if (ae.op == Operator.AssignBitAnd)               op = Operator.BitAnd;
			ENode expr;
			if (ae.isGenVoidExpr() && (ae.op == Operator.Assign || ae.op == Operator.Assign2)) {
				expr = new CallExpr(ae.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeWriteAccessor(), new ENode[]{~fa.obj, ~ae.value});
			}
			else {
				Block be = new Block(ae.pos);
				Object acc;
				if (fa.obj instanceof ThisExpr || fa.obj instanceof SuperExpr) {
					acc = ~fa.obj;
				}
				else if (fa.obj instanceof LVarExpr) {
					acc = ((LVarExpr)fa.obj).getVarSafe();
				}
				else {
					Var var = new LVar(0,"tmp$access",fa.obj.getType(),Var.VAR_LOCAL,0);
					var.init = ~fa.obj;
					be.stats += var;
					acc = var;
				}
				ENode g;
				if !(ae.op == Operator.Assign || ae.op == Operator.Assign2) {
					g = new CallExpr(ae.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeReadAccessor(), new ENode[]{mkAccess(acc)});
					g = new BinaryExpr(ae.pos, op, g, ~ae.value);
				} else {
					g = ~ae.value;
				}
				g = new CallExpr(ae.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeWriteAccessor(), new ENode[]{mkAccess(acc), g});
				be.stats += new ExprStat(0, g);
				if (!ae.isGenVoidExpr()) {
					g = new CallExpr(ae.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeReadAccessor(), new ENode[]{mkAccess(acc)});
					be.stats += g;
				}
				expr = be;
			}
			expr.setGenVoidExpr(ae.isGenVoidExpr());
			ae.replaceWithNodeReWalk(expr);
		}
		else if (ae.lval instanceof SFldExpr) {
			SFldExpr fa = (SFldExpr)ae.lval;
			Field f = fa.var;
			if (!MetaAccess.accessedFromInner(fa,f))
				return true;

			Type ae_tp = ae.isGenVoidExpr() ? Type.tpVoid : ae.getType();
			Operator op = null;
			if      (ae.op == Operator.AssignAdd)                  op = Operator.Add;
			else if (ae.op == Operator.AssignSub)                  op = Operator.Sub;
			else if (ae.op == Operator.AssignMul)                  op = Operator.Mul;
			else if (ae.op == Operator.AssignDiv)                  op = Operator.Div;
			else if (ae.op == Operator.AssignMod)                  op = Operator.Mod;
			else if (ae.op == Operator.AssignLeftShift)            op = Operator.LeftShift;
			else if (ae.op == Operator.AssignRightShift)           op = Operator.RightShift;
			else if (ae.op == Operator.AssignUnsignedRightShift)   op = Operator.UnsignedRightShift;
			else if (ae.op == Operator.AssignBitOr)                op = Operator.BitOr;
			else if (ae.op == Operator.AssignBitXor)               op = Operator.BitXor;
			else if (ae.op == Operator.AssignBitAnd)               op = Operator.BitAnd;
			ENode expr;
			if (ae.isGenVoidExpr() && (ae.op == Operator.Assign || ae.op == Operator.Assign2)) {
				expr = new CallExpr(ae.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeWriteAccessor(), new ENode[]{~ae.value});
			}
			else {
				Block be = new Block(ae.pos);
				ENode g;
				if !(ae.op == Operator.Assign || ae.op == Operator.Assign2) {
					g = new CallExpr(ae.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeReadAccessor(), ENode.emptyArray);
					g = new BinaryExpr(ae.pos, op, g, ~ae.value);
				} else {
					g = ~ae.value;
				}
				g = new CallExpr(ae.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeWriteAccessor(), new ENode[]{g});
				be.stats += new ExprStat(0, g);
				if (!ae.isGenVoidExpr()) {
					g = new CallExpr(ae.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeReadAccessor(), ENode.emptyArray);
					be.stats += g;
				}
				expr = be;
			}
			expr.setGenVoidExpr(ae.isGenVoidExpr());
			ae.replaceWithNodeReWalk(expr);
		}
		return true;
	}
	
	boolean rewrite(IncrementExpr:ASTNode ie) {
		if (ie.lval instanceof IFldExpr) {
			IFldExpr fa = (IFldExpr)ie.lval;
			Field f = fa.var;
			if (!MetaAccess.accessedFromInner(fa,f))
				return true;
			ENode expr;
			Type ie_tp = ie.isGenVoidExpr() ? Type.tpVoid : ie.getType();
			if (ie.isGenVoidExpr()) {
				if (ie.op == Operator.PreIncr || ie.op == Operator.PostIncr) {
					expr = new AssignExpr(ie.pos, Operator.AssignAdd, ~ie.lval, new ConstIntExpr(1));
				} else {
					expr = new AssignExpr(ie.pos, Operator.AssignAdd, ~ie.lval, new ConstIntExpr(-1));
				}
			}
			else {
				Block be = new Block(ie.pos);
				Object acc;
				if (fa.obj instanceof ThisExpr || fa.obj instanceof SuperExpr) {
					acc = fa.obj;
				}
				else if (fa.obj instanceof LVarExpr) {
					acc = ((LVarExpr)fa.obj).getVarSafe();
				}
				else {
					Var var = new LVar(0,"tmp$access",fa.obj.getType(),Var.VAR_LOCAL,0);
					var.init = ~fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				Var res = null;
				if (ie.op == Operator.PostIncr || ie.op == Operator.PostDecr) {
					res = new LVar(0,"tmp$res",f.getType(),Var.VAR_LOCAL,0);
					be.addSymbol(res);
				}
				ConstExpr ce;
				if (ie.op == Operator.PreIncr || ie.op == Operator.PostIncr)
					ce = new ConstIntExpr(1);
				else
					ce = new ConstIntExpr(-1);
				ENode g;
				g = new CallExpr(ie.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeReadAccessor(), new ENode[]{mkAccess(acc)});
				if (ie.op == Operator.PostIncr || ie.op == Operator.PostDecr)
					g = new AssignExpr(ie.pos, Operator.Assign, mkAccess(res), g);
				g = new BinaryExpr(ie.pos, Operator.Add, ce, g);
				g = new CallExpr(ie.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeWriteAccessor(), new ENode[]{mkAccess(acc),g});
				be.stats.add(new ExprStat(0, g));
				if (ie.op == Operator.PostIncr || ie.op == Operator.PostDecr)
					be.stats.add(mkAccess(res));
				else
					be.stats.add(new CallExpr(ie.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeReadAccessor(), new ENode[]{mkAccess(acc)}));
				expr = be;
			}
			expr.setGenVoidExpr(ie.isGenVoidExpr());
			ie.replaceWithNodeReWalk(expr);
		}
		else if (ie.lval instanceof SFldExpr) {
			SFldExpr fa = (SFldExpr)ie.lval;
			Field f = fa.var;
			if (!MetaAccess.accessedFromInner(fa,f))
				return true;
			ENode expr;
			Type ie_tp = ie.isGenVoidExpr() ? Type.tpVoid : ie.getType();
			if (ie.isGenVoidExpr()) {
				if (ie.op == Operator.PreIncr || ie.op == Operator.PostIncr) {
					expr = new AssignExpr(ie.pos, Operator.AssignAdd, ~ie.lval, new ConstIntExpr(1));
				} else {
					expr = new AssignExpr(ie.pos, Operator.AssignAdd, ~ie.lval, new ConstIntExpr(-1));
				}
			}
			else {
				Block be = new Block(ie.pos);
				Var res = null;
				if (ie.op == Operator.PostIncr || ie.op == Operator.PostDecr) {
					res = new LVar(0,"tmp$res",f.getType(),Var.VAR_LOCAL,0);
					be.addSymbol(res);
				}
				ConstExpr ce;
				if (ie.op == Operator.PreIncr || ie.op == Operator.PostIncr)
					ce = new ConstIntExpr(1);
				else
					ce = new ConstIntExpr(-1);
				ENode g;
				g = new CallExpr(ie.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeReadAccessor(), ENode.emptyArray);
				if (ie.op == Operator.PostIncr || ie.op == Operator.PostDecr)
					g = new AssignExpr(ie.pos, Operator.Assign, mkAccess(res), g);
				g = new BinaryExpr(ie.pos, Operator.Add, ce, g);
				g = new CallExpr(ie.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeWriteAccessor(), new ENode[]{g});
				be.stats.add(new ExprStat(0, g));
				if (ie.op == Operator.PostIncr || ie.op == Operator.PostDecr)
					be.stats.add(mkAccess(res));
				else
					be.stats.add(new CallExpr(ie.pos, new TypeRef(f.ctx_tdecl.xtype), f.makeReadAccessor(), ENode.emptyArray));
				expr = be;
			}
			expr.setGenVoidExpr(ie.isGenVoidExpr());
			ie.replaceWithNodeReWalk(expr);
		}
		return true;
	}
	
	private ENode mkAccess(Object o) {
		if (o instanceof Var) return new LVarExpr(0,(Var)o);
		if (o instanceof LVarExpr) return new LVarExpr(0,o.getVarSafe());
		if (o instanceof ThisExpr) return new ThisExpr(0);
		if (o instanceof SuperExpr) return new SuperExpr(0);
		throw new RuntimeException("Unknown accessor "+o);
	}

	boolean rewrite(Struct:ASTNode s) {
		addToOuter(s);
		cleanupMixins(s);
		return true;
	}
	
	private GlobalDNode getOuterStruct(Struct s) {
		ANode n = s.parent();
		while (n != null && !(n instanceof NameSpace || n instanceof KievPackage || n instanceof ComplexTypeDecl))
			n = n.parent();
		if (n == null)
			return Env.getRoot();
		if (n instanceof KievPackage)
			return (KievPackage)n;
		if (n instanceof NameSpace)
			return n.getPackage();
		return (ComplexTypeDecl)n;
	}
	
	private void addToOuter(Struct s) {
		GlobalDNode outer = getOuterStruct(s);
		if (outer instanceof KievPackage || !(outer instanceof Struct)) {
			// already top-level class, just make bytecode name
			if (s.bytecode_name != null)
				return;
			String pkg_name = outer.qname().replace('·','/');
			String bc_name = pkg_name + '/' + s.sname;
			s.bytecode_name = KString.from(bc_name);
			return;
		}
		outer = (Struct)outer;
		InnerStructInfo inf = outer.inner_info;
		if (inf == null) {
			inf = new InnerStructInfo();
			outer.inner_info = inf;
		}
		if (inf.inners.indexOf(s) >= 0) {
			assert (s.bytecode_name != null);
			return;
		}
		String pkg_name = outer.bytecode_name.toString();
		String bc_name;
		if (s.parent() == outer) {
			bc_name = s.sname;
		} else {
			int idx = inf.inner_count++;
			if (s instanceof JavaAnonymouseClass)
				bc_name = String.valueOf(idx);
			else
				bc_name = String.valueOf(idx) + '$' + s.sname;
		}
		s.bytecode_name = KString.from(pkg_name + '$' + bc_name);
		inf.inners.append(s);
	}
	
	private void cleanupMixins(Struct s) {
		if (!s.isInterface() || s.isAnnotation())
			return;
		if (s.super_types.length > 0 && s.super_types[0].getType() ≉ StdTypes.tpObject) {
			TypeRef tr = new TypeRef(Type.tpObject);
			tr.setAutoGenerated(true);
			s.super_types[0] = tr;
		}
		foreach (DNode dn; s.members) {
			if (dn instanceof Constructor) {
				if (!dn.isStatic())
					~dn;
			}
			else if (dn.isPrivate())
				~dn;
			else if (!dn.isAbstract()) {
				if (dn instanceof Method) {
					dn.setAbstract(true);
					((Method)dn).body = null;
				}
				else if (dn instanceof Field) {
					if !(dn.isStatic() && dn.isFinal())
						dn.setAbstract(true);
				}
			}
		}
	}
}
