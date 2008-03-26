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
public final class VirtFldFE_GenMembers extends TransfProcessor {

	private static final String PROP_BASE		= "symade.transf.virtfld";
	public static final String nameMetaGetter	= getPropS(PROP_BASE,"nameMetaGetter","kiev\u001fstdlib\u001fmeta\u001fgetter"); 
	public static final String nameMetaSetter	= getPropS(PROP_BASE,"nameMetaSetter","kiev\u001fstdlib\u001fmeta\u001fsetter"); 
	
	private VirtFldFE_GenMembers() { super(KievExt.VirtualFields); }
	public String getDescr() { "Virtual fields members generation" }

	////////////////////////////////////////////////////
	//	   PASS - autoGenerateMembers                 //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
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
		addAbstractFields(s);
		foreach(Struct sub; s.sub_decls)
			doProcess(sub);
	}
	
	public void addAbstractFields(Struct s) {
		foreach(Method m; s.members; m.sname != null) {
			if (m.sname.startsWith(nameSet))
				addSetterForAbstractField(s, m.sname.substring(nameSet.length()), m);
			foreach (Symbol a; m.aliases; a.sname.startsWith(nameSet)) {
				addSetterForAbstractField(s, a.sname.substring(nameSet.length()), m);
			}
			if (m.sname.startsWith(nameGet))
				addGetterForAbstractField(s, m.sname.substring(nameGet.length()), m);
			foreach (Symbol a; m.aliases; a.sname.startsWith(nameGet)) {
				addGetterForAbstractField(s, a.sname.substring(nameGet.length()), m);
			}
		}
	}
	
	private void addSetterForAbstractField(Struct s, String name, Method m) {
		name = name.intern();
		Field f = s.resolveField( name, false );
		MetaAccess acc;
		if( f != null ) {
			if (f.parent() != m.parent())
				return;
			Method setter = f.setter;
			if (setter != null && setter != m)
				return;
			acc = f.getMetaAccess();
			if (acc == null) f.setMeta(acc = new MetaAccess());
			if (acc.flags == -1) acc.setFlags(MetaAccess.getFlags(f));
		} else {
			s.addField(f=new Field(name,m.type.arg(0),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC));
			if (f.isFinal()) f.setFinal(false);
			acc = f.getMetaAccess();
			if (acc == null) f.setMeta(acc = new MetaAccess());
			acc.setFlags(0);
		}
		f.setVirtual(true);
		f.setter = m;
		if (m.getMeta(nameMetaSetter) == null) {
			Kiev.reportWarning(m,"Method looks to be a setter, but @setter is not specified");
		}
		if( m.isPublic() ) {
			if !(acc.w_public & acc.w_protected & acc.w_default & acc.w_private) {
				acc.w_public = true;
				acc.w_protected = true;
				acc.w_default = true;
				acc.w_private = true;
			}
		}
		else if( m.isPrivate() ) {
			if !(!acc.w_public & !acc.w_protected & !acc.w_default & acc.w_private) {
				acc.w_public = false;
				acc.w_protected = false;
				acc.w_default = false;
				acc.w_private = true;
			}
		}
		else if( m.isProtected() ) {
			if !(!acc.w_public & acc.w_protected & acc.w_default & acc.w_private) {
				acc.w_public = false;
				acc.w_protected = true;
				acc.w_default = true;
				acc.w_private = true;
			}
		}
		else {
			if !(!acc.w_public & !acc.w_protected & acc.w_default & acc.w_private) {
				acc.w_public = false;
				acc.w_protected = false;
				acc.w_default = true;
				acc.w_private = true;
			}
		}
		MetaAccess.verifyDecl(f);
	}
	
	private void addGetterForAbstractField(Struct s, String name, Method m) {
		name = name.intern();
		Field f = s.resolveField( name, false );
		MetaAccess acc;
		if( f != null ) {
			if (f.parent() != m.parent())
				return;
			Method getter = f.getter;
			if (getter != null && getter != m)
				return;
			acc = f.getMetaAccess();
			if (acc == null) f.setMeta(acc = new MetaAccess());
			if (acc.flags == -1) acc.setFlags(MetaAccess.getFlags(f));
		} else {
			s.addField(f=new Field(name,m.type.ret(),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC));
			if (f.isFinal()) f.setFinal(false);
			acc = f.getMetaAccess();
			if (acc == null) f.setMeta(acc = new MetaAccess());
			acc.setFlags(0);
		}
		f.setVirtual(true);
		f.getter = m;
		if (m.getMeta(nameMetaGetter) == null) {
			Kiev.reportWarning(m,"Method looks to be a getter, but @getter is not specified");
		}
		if( m.isPublic() ) {
			if !(acc.r_public & acc.r_protected & acc.r_default & acc.r_private) {
				acc.r_public = true;
				acc.r_protected = true;
				acc.r_default = true;
				acc.r_private = true;
			}
		}
		else if( m.isPrivate() ) {
			if !(!acc.r_public & !acc.r_protected & !acc.r_default & acc.r_private) {
				acc.r_public = false;
				acc.r_protected = false;
				acc.r_default = false;
				acc.r_private = true;
			}
		}
		else if( m.isProtected() ) {
			if !(!acc.r_public & acc.r_protected & acc.r_default & acc.r_private) {
				acc.r_public = false;
				acc.r_protected = true;
				acc.r_default = true;
				acc.r_private = true;
			}
		}
		else {
			if !(!acc.r_public & !acc.r_protected & acc.r_default & acc.r_private) {
				acc.r_public = false;
				acc.r_protected = false;
				acc.r_default = true;
				acc.r_private = true;
			}
		}
		MetaAccess.verifyDecl(f);
	}
}

////////////////////////////////////////////////////
//	   PASS - preGenerate                         //
////////////////////////////////////////////////////

@singleton
public class VirtFldME_PreGenerate extends BackendProcessor implements Constants {

	public static final String nameMetaGetter = VirtFldFE_GenMembers.nameMetaGetter; 
	public static final String nameMetaSetter = VirtFldFE_GenMembers.nameMetaSetter; 
	
	private VirtFldME_PreGenerate() { super(KievBackend.Java15); }
	public String getDescr() { "Virtual fields pre-generation" }

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"VirtFldME_PreGenerate");
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
		foreach(Field f; s.getAllFields())
			addMethodsForVirtualField(s, f);
		foreach(Field f; s.getAllFields()) {
			if (!f.isVirtual())
				continue;
			if (s.isInterface() && !f.isAbstract())
				f.setAbstract(true);
		}
		foreach(Struct sub; s.sub_decls)
			this.doProcess(sub);
	}
	
	private static void addMethodsForVirtualField(Struct s, Field f) {
		if( f.isStatic() && f.isVirtual() ) {
			Kiev.reportError(f,"Static fields can't be virtual");
			f.setVirtual(false);
		}
		if( s.isInterface() && f.isVirtual() ) f.setAbstract(true);

		if( !f.isVirtual() ) return;

		// Check set$/get$ methods
		boolean set_found = false;
		boolean get_found = false;

		String set_name = (nameSet+f.sname).intern();
		String get_name = (nameGet+f.sname).intern();

		foreach(Method m; s.members) {
			if( m.hasName(set_name,true) ) {
				set_found = true;
				if( get_found ) break;
			}
			else if( m.hasName(get_name,true) ) {
				get_found = true;
				if( set_found ) break;
			}
		}
		if( !set_found && !f.isFinal() && MetaAccess.writeable(f) ) {
			Method set_var = new MethodImpl(set_name,Type.tpVoid,f.getJavaFlags() | ACC_SYNTHETIC);
			if (s.isInterface())
				set_var.setFinal(false);
			else if (f.getMeta(VNode_Base.mnAtt) != null)
				set_var.setFinal(true);
			s.addMethod(set_var);
			set_var.setMeta(new UserMeta(nameMetaSetter)).resolve(null);
			LVar value;
			if (f.isStatic()) {
				value = new LVar(f.pos,"value",f.type,Var.PARAM_NORMAL,0);
				set_var.params.add(value);
			} else {
				value = new LVar(f.pos,"value",f.type,Var.PARAM_NORMAL,0);
				set_var.params.add(value);
			}
			if( !f.isAbstract() ) {
				Block body = new Block(f.pos);
				set_var.body = body;
				ENode fa;
				if (f.isStatic())
					fa = new SFldExpr(f.pos,f);
				else
					fa = new IFldExpr(f.pos,new ThisExpr(0),f);
				fa.setAsField(true);
				ENode ass_st = new ExprStat(f.pos,
					new AssignExpr(f.pos,Operator.Assign,fa,new LVarExpr(f.pos,value))
				);

				body.stats.append(ass_st);
			}
			f.setter = set_var;
		}
		else if( set_found && (f.isFinal() || !MetaAccess.writeable(f)) ) {
			Kiev.reportError(f,"Virtual set$ method for non-writeable field "+f);
		}

		if (!f.isVirtual())
			return;		// no need to generate getter
		if( !get_found && MetaAccess.readable(f)) {
			Method get_var = new MethodImpl(get_name,f.type,f.getJavaFlags() | ACC_SYNTHETIC);
			if (s.isInterface())
				get_var.setFinal(false);
			if (f.getMeta(VNode_Base.mnAtt) != null)
				get_var.setFinal(true);
			s.addMethod(get_var);
			get_var.setMeta(new UserMeta(nameMetaGetter)).resolve(null);
			if( !f.isAbstract() ) {
				Block body = new Block(f.pos);
				get_var.body = body;
				body.stats.add(new ReturnStat(f.pos,new IFldExpr(f.pos,new ThisExpr(0),f,true)));
			}
			f.getter = get_var;
		}
		else if( get_found && !MetaAccess.readable(f) ) {
			Kiev.reportError(f,"Virtual get$ method for non-readable field "+f);
		}
	}
}
	
////////////////////////////////////////////////////
//	   PASS - rewrite code                        //
////////////////////////////////////////////////////

@singleton
public class VirtFldBE_Rewrite extends BackendProcessor implements Constants {

	public static final String nameMetaGetter = VirtFldFE_GenMembers.nameMetaGetter; 
	public static final String nameMetaSetter = VirtFldFE_GenMembers.nameMetaSetter; 
	
	private VirtFldBE_Rewrite() { super(KievBackend.Java15); }
	public String getDescr() { "Virtual fields rewrite" }

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"VirtFldBE_Rewrite");
		try {
			node.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return VirtFldBE_Rewrite.this.rewrite((ASTNode)n); return false; }
			});
		} finally { tr.leave(); }
	}
	
	boolean rewrite(ASTNode:ASTNode o) {
		//System.out.println("ProcessVirtFld: rewrite "+(o==null?"null":o.getClass().getName())+" in "+id);
		return true;
	}

	boolean rewrite(DNode:ASTNode dn) {
		if (dn.isMacro())
			return false;
		return true;
	}

	boolean rewrite(IFldExpr:ASTNode fa) {
		//System.out.println("ProcessVirtFld: rewrite "+fa.getClass().getName()+" "+fa+" in "+id);
		Field f = fa.var;
		if( !f.isVirtual() || fa.isAsField() )
			return true;
		String get_name = (nameGet+f.sname).intern();

		if (fa.ctx_method != null && fa.ctx_method.hasName(get_name,true) && fa.ctx_tdecl.instanceOf(f.ctx_tdecl)) {
			fa.setAsField(true);
			return true;
		}
		// We rewrite by get$ method. set$ method is rewritten by AssignExpr
		Method getter = f.getter;
		if (getter == null) {
			Kiev.reportError(fa, "Getter method for virtual field "+f+" not found");
			fa.setAsField(true);
			return true;
		}
		ENode ce = new CallExpr(fa.pos, ~fa.obj, getter, ENode.emptyArray);
		fa.replaceWithNodeReWalk(ce);
		throw new Error();
	}
	
	boolean rewrite(AssignExpr:ASTNode ae) {
		//System.out.println("ProcessVirtFld: rewrite "+ae.getClass().getName()+" "+ae+" in "+id);
		if (ae.lval instanceof IFldExpr) {
			IFldExpr fa = (IFldExpr)ae.lval;
			Field f = fa.var;
			if( !f.isVirtual() || fa.isAsField() )
				return true;
			String set_name = (nameSet+f.sname).intern();
	
			if (ae.ctx_method != null && ae.ctx_method.hasName(set_name,true) && ae.ctx_tdecl.instanceOf(f.ctx_tdecl)) {
				fa.setAsField(true);
				return true;
			}
			// Rewrite by set$ method
			Method getter = f.getter;
			Method setter = f.setter;
			if (setter == null) {
				Kiev.reportWarning(fa, "Setter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			if (getter == null && (!ae.isGenVoidExpr() || !(ae.op == Operator.Assign || ae.op == Operator.Assign2))) {
				Kiev.reportWarning(fa, "Getter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
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
				expr = new CallExpr(ae.pos, ~fa.obj, setter, new ENode[]{~ae.value});
			}
			else {
				Block be = new Block(ae.pos);
				Object acc;
				if (fa.obj instanceof ThisExpr || fa.obj instanceof SuperExpr) {
					acc = ~fa.obj;
				}
				else if (fa.obj instanceof LVarExpr) {
					acc = ((LVarExpr)fa.obj).getVar();
				}
				else {
					Var var = new LVar(0,"tmp$virt",fa.obj.getType(),Var.VAR_LOCAL,0);
					var.init = ~fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				ENode g;
				if !(ae.op == Operator.Assign || ae.op == Operator.Assign2) {
					g = new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray);
					g = new BinaryExpr(ae.pos, op, g, ~ae.value);
				} else {
					g = ~ae.value;
				}
				g = new CallExpr(ae.pos, mkAccess(acc), setter, new ENode[]{g});
				be.stats.add(new ExprStat(0, g));
				if (!ae.isGenVoidExpr()) {
					g = new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray);
					be.stats.add(g);
				}
				expr = be;
			}
			expr.setGenVoidExpr(ae.isGenVoidExpr());
			ae.replaceWithNodeReWalk(expr);
		}
		return true;
	}
	
	boolean rewrite(IncrementExpr:ASTNode ie) {
		//System.out.println("ProcessVirtFld: rewrite "+ie.getClass().getName()+" "+ie+" in "+id);
		if (ie.lval instanceof IFldExpr) {
			IFldExpr fa = (IFldExpr)ie.lval;
			Field f = fa.var;
			if( !f.isVirtual() || fa.isAsField() )
				return true;
			String set_name = (nameSet+f.sname).intern();
			String get_name = (nameGet+f.sname).intern();
	
			if (ie.ctx_method != null
			&& (ie.ctx_method.hasName(set_name,true) || ie.ctx_method.hasName(get_name,true))
			&& ie.ctx_tdecl.instanceOf(f.ctx_tdecl) )
			{
				fa.setAsField(true);
				return true;
			}
			// Rewrite by set$ method
			Method getter = f.getter;
			Method setter = f.setter;
			if (setter == null) {
				Kiev.reportError(fa, "Setter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			if (getter == null) {
				Kiev.reportError(fa, "Getter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
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
					acc = ((LVarExpr)fa.obj).getVar();
				}
				else {
					Var var = new LVar(0,"tmp$virt",fa.obj.getType(),Var.VAR_LOCAL,0);
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
				g = new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray);
				if (ie.op == Operator.PostIncr || ie.op == Operator.PostDecr)
					g = new AssignExpr(ie.pos, Operator.Assign, mkAccess(res), g);
				g = new BinaryExpr(ie.pos, Operator.Add, ce, g);
				g = new CallExpr(ie.pos, mkAccess(acc), setter, new ENode[]{g});
				be.stats.add(new ExprStat(0, g));
				if (ie.op == Operator.PostIncr || ie.op == Operator.PostDecr)
					be.stats.add(mkAccess(res));
				else
					be.stats.add(new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray));
				expr = be;
			}
			expr.setGenVoidExpr(ie.isGenVoidExpr());
			ie.replaceWithNodeReWalk(expr);
		}
		return true;
	}
	
	private ENode mkAccess(Object o) {
		if (o instanceof Var) return new LVarExpr(0,(Var)o);
		if (o instanceof LVarExpr) return new LVarExpr(0,o.getVar());
		if (o instanceof ThisExpr) return new ThisExpr(0);
		if (o instanceof SuperExpr) return new SuperExpr(0);
		throw new RuntimeException("Unknown accessor "+o);
	}

}
