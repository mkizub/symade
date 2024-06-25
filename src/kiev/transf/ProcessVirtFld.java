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

public final class VirtFldPlugin implements PluginFactory {
	public PluginDescr getPluginDescr(String name) {
		PluginDescr pd = null;
		if (name.equals("virt-fld")) {
			pd = new PluginDescr("virt-fld").depends("kiev");
			pd.proc(new ProcessorDescr("gen-members", "fe", 0, VirtFldFE_GenMembers.class).after("kiev:fe:pass3").before("kiev:fe:pre-resolve"));
			pd.proc(new ProcessorDescr("pre-generate", "me", 0, VirtFldME_PreGenerate.class));
			pd.proc(new ProcessorDescr("rewrite", "be", 0, VirtFldBE_Rewrite.class).before("kiev:be:generate"));
		}
		return pd;
	}
}

public final class VirtFldFE_GenMembers extends TransfProcessor {

	private static final String PROP_BASE	= "symade.transf.virtfld";
	public final String nameMetaGetter		= getPropS(PROP_BASE,"nameMetaGetter","kiev·stdlib·meta·getter"); 
	public final String nameMetaSetter		= getPropS(PROP_BASE,"nameMetaSetter","kiev·stdlib·meta·setter"); 
	
	public VirtFldFE_GenMembers(Env env, int id) { super(env,id,KievExt.VirtualFields); }
	public String getDescr() { "Virtual fields members generation" }

	////////////////////////////////////////////////////
	//	   PASS - autoGenerateMembers                 //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			if (wthg.setProcessorRun(cu,this))
				return;
			tr = Transaction.enter(tr,"VirtFldFE_GenMembers");
			try {
				node.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) {
						if !(n instanceof ASTNode)
							return false;
						if (n instanceof ComplexTypeDecl)
							addAbstractFields((ComplexTypeDecl)n);
						return true;
					}
				});
			} finally { tr.leave(); }
		}
	}
	
	public void addAbstractFields(ComplexTypeDecl s) {
		foreach(Method m; s.members; m.sname != null) {
			if (m.sname.startsWith(nameSet))
				addSetterForAbstractField(s, m.sname.substring(nameSet.length()), m);
			foreach (Alias a; m.aliases; a.symbol.sname.startsWith(nameSet))
				addSetterForAbstractField(s, a.symbol.sname.substring(nameSet.length()), m);
			if (m.sname.startsWith(nameGet))
				addGetterForAbstractField(s, m.sname.substring(nameGet.length()), m);
			foreach (Alias a; m.aliases; a.symbol.sname.startsWith(nameGet))
				addGetterForAbstractField(s, a.symbol.sname.substring(nameGet.length()), m);
		}
	}
	
	private void addSetterForAbstractField(ComplexTypeDecl s, String name, Method m) {
		name = name.intern();
		Field f = s.resolveField(env, name, false );
		MetaAccess acc;
		if( f != null ) {
			if (f.parent() != m.parent())
				return;
			Method setter = f.getSetterMethod();
			if (setter != null && setter != m)
				return;
			acc = f.getMetaAccess();
			if (acc == null) {
				acc = new MetaAccess();
				if      (f.isPublic())		acc.simple = "public";
				else if (f.isProtected())	acc.simple = "protected";
				else if (f.isPrivate())		acc.simple = "private";
				f.setMeta(acc);
			}
			if (acc.flags == -1) acc.flags = MetaAccess.getFlags(f);
		} else {
			Kiev.reportWarning(m, "Creating @virtuial @abstract field "+name);
			f = new Field(name,m.mtype.arg(0),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC);
			s.members += f;
			if (f.isFinal()) f.setFinal(false);
			acc = f.getMetaAccess();
			if (acc == null) {
				acc = new MetaAccess();
				if      (f.isPublic())		acc.simple = "public";
				else if (f.isProtected())	acc.simple = "protected";
				else if (f.isPrivate())		acc.simple = "private";
				f.setMeta(acc);
			}
			acc.flags = 0;
		}
		f.setVirtual(true);
		f.setter = new SymbolRef<Method>(m);
		if (!(m instanceof MethodSetter) && m.getMeta(nameMetaSetter) == null) {
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
	
	private void addGetterForAbstractField(ComplexTypeDecl s, String name, Method m) {
		name = name.intern();
		Field f = s.resolveField(env, name, false );
		MetaAccess acc;
		if( f != null ) {
			if (f.parent() != m.parent())
				return;
			Method getter = f.getGetterMethod();
			if (getter != null && getter != m)
				return;
			acc = f.getMetaAccess();
			if (acc == null) {
				acc = new MetaAccess();
				if      (f.isPublic())		acc.simple = "public";
				else if (f.isProtected())	acc.simple = "protected";
				else if (f.isPrivate())		acc.simple = "private";
				f.setMeta(acc);
			}
			if (acc.flags == -1) acc.flags = MetaAccess.getFlags(f);
		} else {
			Kiev.reportWarning(m, "Creating @virtuial @abstract field "+name);
			f = new Field(name,m.mtype.ret(),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC);
			s.members += f;
			if (f.isFinal()) f.setFinal(false);
			acc = f.getMetaAccess();
			if (acc == null) {
				acc = new MetaAccess();
				if      (f.isPublic())		acc.simple = "public";
				else if (f.isProtected())	acc.simple = "protected";
				else if (f.isPrivate())		acc.simple = "private";
				f.setMeta(acc);
			}
			acc.flags = 0;
		}
		f.setVirtual(true);
		f.getter = new SymbolRef<Method>(m);
		if (!(m instanceof MethodGetter) && m.getMeta(nameMetaGetter) == null) {
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

public final class VirtFldME_PreGenerate extends BackendProcessor implements Constants {

	public VirtFldME_PreGenerate(Env env, int id) { super(env,id,KievBackend.Java15); }
	public String getDescr() { "Virtual fields pre-generation" }

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			if (wthg.setProcessorRun(cu,this))
				return;
			tr = Transaction.enter(tr,"VirtFldME_PreGenerate");
			try {
				node.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) {
						if !(n instanceof ASTNode)
							return false;
						if (n instanceof ComplexTypeDecl)
							doProcess((ComplexTypeDecl)n);
						return true;
					}
				});
			} finally { tr.leave(); }
		}
	}
	
	private void doProcess(ComplexTypeDecl s) {
		foreach(Field f; s.members)
			addMethodsForVirtualField(s, f);
		foreach(Field f; s.members; f.isVirtual()) {
			if (s.isInterface() && !f.isAbstract())
				f.setAbstract(true);
		}
		foreach(MethodGetter getter; s.members) {
			// change the name of the getter to backend-specific value
			if (getter.sname.startsWith(nameGet)) {
				String name = getter.sname.substring(4);
				name = Character.toUpperCase(name.charAt(0))+name.substring(1);
				if (getter.mtype.ret() ≡ this.env.getTypeEnv().tpBoolean)
					name = ("is"+name).intern();
				else
					name = ("get"+name).intern();
				if (!getter.isMethodBridge()) {
					foreach (Method m; s.members; m.sname == name && m.params.length == 0)
						Kiev.reportError(getter, "Getter method name conflicts with method "+m);
				}
				getter.sname = name;
			}
		}
		foreach(MethodSetter setter; s.members) {
			if (setter.sname.startsWith(nameSet)) {
				String name = setter.sname.substring(4);
				name = Character.toUpperCase(name.charAt(0))+name.substring(1);
				name = ("set"+name).intern();
				if (!setter.isMethodBridge()) {
					foreach (Method m; s.members; m.sname == name && m.params.length == 1)
						Kiev.reportError(setter, "Setter method name conflicts with method "+m);
				}
				setter.sname = name;
			}
		}
	}
	
	private void addMethodsForVirtualField(ComplexTypeDecl s, Field f) {
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

		if (f.getSetterMethod() != null)
			set_found = true;
		if (f.getGetterMethod() != null)
			get_found = true;
		
		foreach(Method m; s.members) {
			if (set_found && get_found)
				break;
			if (!set_found && m.hasName(set_name))
				set_found = true;
			if (!get_found && m.hasName(get_name))
				get_found = true;
		}
		if( !set_found && !f.isFinal() && MetaAccess.writeable(f) ) {
			Method set_var = new MethodSetter(f);
			if (s.isInterface())
				set_var.setFinal(false);
			else if (f.getMeta(VNodeUtils.mnAtt) != null || f.getMeta(VNodeUtils.mnRef) != null)
				set_var.setFinal(true);
			s.members += set_var;
			Var value = set_var.params[0];
			if( !f.isAbstract() ) {
				Block body = new Block(f.pos);
				body.setAutoGenerated(true);
				set_var.body = body;
				ENode fa;
				if (f.isStatic())
					fa = new SFldExpr(f.pos,f);
				else
					fa = new IFldExpr(f.pos,new ThisExpr(0),f);
				fa.setAsField(true);
				ENode ass_st = new ExprStat(f.pos,
					new AssignExpr(f.pos,fa,new LVarExpr(f.pos,value))
				);

				body.stats.append(ass_st);
			}
			f.setter = new SymbolRef<Method>(set_var);
		}
		else if( set_found && (f.isFinal() || !MetaAccess.writeable(f)) ) {
			Kiev.reportError(f,"Virtual set$ method for non-writeable field "+f);
		}

		if( !get_found && MetaAccess.readable(f)) {
			Method get_var = new MethodGetter(f);
			if (s.isInterface())
				get_var.setFinal(false);
			if (f.getMeta(VNodeUtils.mnAtt) != null || f.getMeta(VNodeUtils.mnRef) != null)
				get_var.setFinal(true);
			s.members += get_var;
			if( !f.isAbstract() ) {
				Block body = new Block(f.pos);
				body.setAutoGenerated(true);
				get_var.body = body;
				body.stats.add(new ReturnStat(f.pos,new IFldExpr(f.pos,new ThisExpr(0),f,true)));
			}
			f.getter = new SymbolRef<Method>(get_var);
		}
		else if( get_found && !MetaAccess.readable(f) ) {
			Kiev.reportError(f,"Virtual get$ method for non-readable field "+f);
		}

		if (!f.isAbstract() && !f.isPrivate())
			f.setPkgPrivateKeepAccess();
	}
}
	
////////////////////////////////////////////////////
//	   PASS - rewrite code                        //
////////////////////////////////////////////////////

public final class VirtFldBE_Rewrite extends BackendProcessor implements Constants {

	public VirtFldBE_Rewrite(Env env, int id) { super(env,id,KievBackend.Java15); }
	public String getDescr() { "Virtual fields rewrite" }

	public void process(ASTNode node, Transaction tr) {
		boolean need_run = false;
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			need_run = !wthg.setProcessorRun(cu,this);
		}
		if (!need_run)
			return;
		String debug_msg = need_run ? "" : "Unexpected virtual field rewrite";
		tr = Transaction.enter(tr,"VirtFldBE_Rewrite");
		try {
			node.walkTree(node.parent(), node.pslot(), new ITreeWalker() {
				public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
					if (n instanceof ASTNode)
						return VirtFldBE_Rewrite.this.rewrite((ASTNode)n, debug_msg, parent, slot);
					return false;
				}
			});
		} finally { tr.leave(); }
	}
	
	boolean rewrite(ASTNode:ASTNode o, String debug_msg, INode parent, AttrSlot slot) {
		//System.out.println("ProcessVirtFld: rewrite "+(o==null?"null":o.getClass().getName())+" in "+id);
		return true;
	}

	boolean rewrite(DNode:ASTNode dn, String debug_msg, INode parent, AttrSlot slot) {
		if (dn.isMacro())
			return false;
		return true;
	}

	boolean rewrite(IFldExpr:ASTNode fa, String debug_msg, INode parent, AttrSlot slot) {
		//System.out.println("ProcessVirtFld: rewrite "+fa.getClass().getName()+" "+fa+" in "+id);
		Field f = fa.var;
		if( !f.isVirtual() || fa.isAsField() )
			return true;
		if (debug_msg != "")
			System.out.println("Unexpected virtual field rewrite");

		Method getter = f.getGetterMethod();
		// We rewrite by get$ method. set$ method is rewritten by AssignExpr
		if (getter == null) {
			Kiev.reportError(fa, "Getter method for virtual field "+f+" not found");
			fa.setAsField(true);
			return true;
		}
		Method ctx_method = Env.ctxMethod(fa);
		if (getter == ctx_method) {
			fa.setAsField(true);
			return true;
		}
		ENode ce = new CallExpr(fa.pos, ~fa.obj, getter, ENode.emptyArray);
		fa.replaceWithNodeReWalk(ce,parent,slot);
		throw new Error();
	}
	
	boolean rewrite(AssignExpr:ASTNode ae, String debug_msg, INode parent, AttrSlot slot) {
		//System.out.println("ProcessVirtFld: rewrite "+ae.getClass().getName()+" "+ae+" in "+id);
		if (ae.lval instanceof IFldExpr) {
			IFldExpr fa = (IFldExpr)ae.lval;
			Field f = fa.var;
			if( !f.isVirtual() || fa.isAsField() )
				return true;
			if (debug_msg != "")
				System.out.println("Unexpected virtual field rewrite");

			// Rewrite by set$ method
			Method getter = f.getGetterMethod();
			Method setter = f.getSetterMethod();
			Method ctx_method = Env.ctxMethod(fa);
			if (ctx_method != null && ctx_method == setter) {
				fa.setAsField(true);
				return true;
			}
			if (setter == null) {
				if (!f.isFinal() && MetaAccess.writeable(f))
					Kiev.reportWarning(fa, "Setter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			if (getter == null && !ae.isGenVoidExpr()) {
				Kiev.reportWarning(fa, "Getter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			Type ae_tp = ae.isGenVoidExpr() ? this.env.getTypeEnv().tpVoid : ae.getType(env);
			Operator op = ae.getOper();
			ENode expr;
			if (ae.isGenVoidExpr()) {
				expr = new CallExpr(ae.pos, ~fa.obj, setter, new ENode[]{~ae.value});
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
					Var var = new LVar(0,"tmp$virt",fa.obj.getType(env),Var.VAR_LOCAL,0);
					var.init = ~fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				ENode g = ~ae.value;
				g = new CallExpr(ae.pos, mkAccess(acc), setter, new ENode[]{g});
				be.stats.add(new ExprStat(0, g));
				if (!ae.isGenVoidExpr()) {
					g = new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray);
					be.stats.add(g);
				}
				expr = be;
			}
			expr.setGenVoidExpr(ae.isGenVoidExpr());
			ae.replaceWithNodeReWalk(expr,parent,slot);
		}
		return true;
	}
	
	boolean rewrite(ModifyExpr:ASTNode ae, String debug_msg, INode parent, AttrSlot slot) {
		//System.out.println("ProcessVirtFld: rewrite "+ae.getClass().getName()+" "+ae+" in "+id);
		if (ae.lval instanceof IFldExpr) {
			IFldExpr fa = (IFldExpr)ae.lval;
			Field f = fa.var;
			if( !f.isVirtual() || fa.isAsField() )
				return true;
			if (debug_msg != "")
				System.out.println("Unexpected virtual field rewrite");
	
			// Rewrite by set$ method
			Method getter = f.getGetterMethod();
			Method setter = f.getSetterMethod();
			Method ctx_method = Env.ctxMethod(fa);
			if (ctx_method != null && ctx_method == setter) {
				fa.setAsField(true);
				return true;
			}
			if (setter == null) {
				if (!f.isFinal() && MetaAccess.writeable(f))
					Kiev.reportWarning(fa, "Setter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			if (getter == null && !ae.isGenVoidExpr()) {
				Kiev.reportWarning(fa, "Getter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			Type ae_tp = ae.isGenVoidExpr() ? this.env.getTypeEnv().tpVoid : ae.getType(env);
			Operator op = ae.getOper();
			if      (op == Operator.AssignAdd)                  op = Operator.Add;
			else if (op == Operator.AssignSub)                  op = Operator.Sub;
			else if (op == Operator.AssignMul)                  op = Operator.Mul;
			else if (op == Operator.AssignDiv)                  op = Operator.Div;
			else if (op == Operator.AssignMod)                  op = Operator.Mod;
			else if (op == Operator.AssignLeftShift)            op = Operator.LeftShift;
			else if (op == Operator.AssignRightShift)           op = Operator.RightShift;
			else if (op == Operator.AssignUnsignedRightShift)   op = Operator.UnsignedRightShift;
			else if (op == Operator.AssignBitOr)                op = Operator.BitOr;
			else if (op == Operator.AssignBitXor)               op = Operator.BitXor;
			else if (op == Operator.AssignBitAnd)               op = Operator.BitAnd;
			ENode expr;
			Block be = new Block(ae.pos);
			Object acc;
			if (fa.obj instanceof ThisExpr || fa.obj instanceof SuperExpr) {
				acc = ~fa.obj;
			}
			else if (fa.obj instanceof LVarExpr) {
				acc = ((LVarExpr)fa.obj).getVarSafe();
			}
			else {
				Var var = new LVar(0,"tmp$virt",fa.obj.getType(env),Var.VAR_LOCAL,0);
				var.init = ~fa.obj;
				be.addSymbol(var);
				acc = var;
			}
			ENode g;
			g = new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray);
			g = new BinaryExpr(ae.pos, op, g, ~ae.value);
			g = new CallExpr(ae.pos, mkAccess(acc), setter, new ENode[]{g});
			be.stats.add(new ExprStat(0, g));
			if (!ae.isGenVoidExpr()) {
				g = new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray);
				be.stats.add(g);
			}
			expr = be;
			expr.setGenVoidExpr(ae.isGenVoidExpr());
			ae.replaceWithNodeReWalk(expr,parent,slot);
		}
		return true;
	}
	
	boolean rewrite(IncrementExpr:ASTNode ie, String debug_msg, INode parent, AttrSlot slot) {
		//System.out.println("ProcessVirtFld: rewrite "+ie.getClass().getName()+" "+ie+" in "+id);
		if (ie.lval instanceof IFldExpr) {
			IFldExpr fa = (IFldExpr)ie.lval;
			Field f = fa.var;
			if( !f.isVirtual() || fa.isAsField() )
				return true;
			if (debug_msg != "")
				System.out.println("Unexpected virtual field rewrite");

			// Rewrite by set$ method
			Method getter = f.getGetterMethod();
			Method setter = f.getSetterMethod();
			Method ctx_method = Env.ctxMethod(ie);
			if (ctx_method != null && ctx_method == getter)
			{
				fa.setAsField(true);
				return true;
			}
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
			Type ie_tp = ie.isGenVoidExpr() ? this.env.getTypeEnv().tpVoid : ie.getType(env);
			Operator ieop = ie.getOper();
			if (ie.isGenVoidExpr()) {
				if (ieop == Operator.PreIncr || ieop == Operator.PostIncr) {
					expr = new ModifyExpr(ie.pos, Operator.AssignAdd, ~ie.lval, new ConstIntExpr(1));
				} else {
					expr = new ModifyExpr(ie.pos, Operator.AssignAdd, ~ie.lval, new ConstIntExpr(-1));
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
					Var var = new LVar(0,"tmp$virt",fa.obj.getType(env),Var.VAR_LOCAL,0);
					var.init = ~fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				Var res = null;
				if (ieop == Operator.PostIncr || ieop == Operator.PostDecr) {
					res = new LVar(0,"tmp$res",f.getType(env),Var.VAR_LOCAL,0);
					be.addSymbol(res);
				}
				ConstExpr ce;
				if (ieop == Operator.PreIncr || ieop == Operator.PostIncr)
					ce = new ConstIntExpr(1);
				else
					ce = new ConstIntExpr(-1);
				ENode g;
				g = new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray);
				if (ieop == Operator.PostIncr || ieop == Operator.PostDecr)
					g = new AssignExpr(ie.pos, mkAccess(res), g);
				g = new BinaryExpr(ie.pos, Operator.Add, ce, g);
				g = new CallExpr(ie.pos, mkAccess(acc), setter, new ENode[]{g});
				be.stats.add(new ExprStat(0, g));
				if (ieop == Operator.PostIncr || ieop == Operator.PostDecr)
					be.stats.add(mkAccess(res));
				else
					be.stats.add(new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray));
				expr = be;
			}
			expr.setGenVoidExpr(ie.isGenVoidExpr());
			ie.replaceWithNodeReWalk(expr,parent,slot);
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

}
