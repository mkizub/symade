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
public final class VirtFldFE_GenMembers extends TransfProcessor {

	public static final String nameMetaGetter = "kiev.stdlib.meta.getter"; 
	public static final String nameMetaSetter = "kiev.stdlib.meta.setter"; 
	
	private VirtFldFE_GenMembers() { super(Kiev.Ext.VirtualFields); }
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
		foreach (Struct dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode s) {
		addAbstractFields(s);
		foreach(Struct sub; s.sub_decls)
			doProcess(sub);
	}
	
	public void addAbstractFields(Struct s) {
		foreach(Method m; s.members) {
			//trace(Kiev.debugCreation,"check "+m.name.name+" to be a setter");
			if (m.id.sname.startsWith(nameSet))
				addSetterForAbstractField(s, m.id.sname.substring(nameSet.length()), m);
			if (m.id.aliases != null) {
				foreach (String name; m.id.aliases) {
					//trace(Kiev.debugCreation,"check "+name+" to be a setter");
					if (name.startsWith(nameSet))
						addSetterForAbstractField(s, name.substring(nameSet.length()), m);
				}
			}
			//trace(Kiev.debugCreation,"check "+m.name.name+" to be a getter");
			if (m.id.sname.startsWith(nameGet))
				addGetterForAbstractField(s, m.id.sname.substring(nameGet.length()), m);
			if (m.id.aliases != null) {
				foreach (String name; m.id.aliases) {
					//trace(Kiev.debugCreation,"check "+name+" to be a getter");
					if (name.startsWith(nameGet))
						addGetterForAbstractField(s, name.substring(nameGet.length()), m);
				}
			}
		}
	}
	
	private void addSetterForAbstractField(Struct s, String name, Method m) {
		name = name.intern();
		Field f = s.resolveField( name, false );
		MetaAccess acc;
		if( f != null ) {
			trace(Kiev.debugCreation,"method "+m+" has field "+f);
			if (f.parent() != m.parent())
				return;
			Method setter = (Method)Field.SETTER_ATTR.get(f);
			if (setter != null && setter != m)
				return;
			acc = f.getMetaAccess();
			if (acc == null) f.meta.setF(acc = new MetaAccess());
			if (acc.flags == -1) acc.setFlags(MetaAccess.getFlags(f));
		} else {
			s.addField(f=new Field(name,m.type.arg(0),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC));
			acc = f.getMetaAccess();
			if (acc == null) f.meta.setF(acc = new MetaAccess());
			acc.setFlags(0);
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		f.setVirtual(true);
		Field.SETTER_ATTR.set(f, m);
		if (m.meta.getU(nameMetaSetter) == null) {
			Kiev.reportWarning(m,"Method looks to be a setter, but @setter is not specified");
		}
		if( m.isPublic() ) {
			acc.w_public = true;
			acc.w_protected = true;
			acc.w_default = true;
			acc.w_private = true;
		}
		else if( m.isPrivate() ) {
			acc.w_public = false;
			acc.w_protected = false;
			acc.w_default = false;
			acc.w_private = true;
		}
		else if( m.isProtected() ) {
			acc.w_public = false;
			acc.w_private = true;
		}
		else {
			acc.w_public = false;
			acc.w_default = true;
			acc.w_private = true;
		}
		MetaAccess.verifyDecl(f);
	}
	
	private void addGetterForAbstractField(Struct s, String name, Method m) {
		name = name.intern();
		Field f = s.resolveField( name, false );
		MetaAccess acc;
		if( f != null ) {
			trace(Kiev.debugCreation,"method "+m+" has field "+f);
			if (f.parent() != m.parent())
				return;
			Method getter = (Method)Field.GETTER_ATTR.get(f);
			if (getter != null && getter != m)
				return;
			acc = f.getMetaAccess();
			if (acc == null) f.meta.setF(acc = new MetaAccess());
			if (acc.flags == -1) acc.setFlags(MetaAccess.getFlags(f));
		} else {
			s.addField(f=new Field(name,m.type.ret(),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC));
			acc = f.getMetaAccess();
			if (acc == null) f.meta.setF(acc = new MetaAccess());
			acc.setFlags(0);
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		f.setVirtual(true);
		Field.GETTER_ATTR.set(f, m);
		if (m.meta.getU(nameMetaGetter) == null) {
			Kiev.reportWarning(m,"Method looks to be a getter, but @getter is not specified");
		}
		if( m.isPublic() ) {
			acc.r_public = true;
			acc.r_protected = true;
			acc.r_default = true;
			acc.r_private = true;
		}
		else if( m.isPrivate() ) {
			acc.r_public = false;
			acc.r_protected = false;
			acc.r_default = false;
			acc.r_private = true;
		}
		else if( m.isProtected() ) {
			acc.r_public = false;
			acc.r_private = true;
		}
		else {
			acc.r_public = false;
			acc.r_default = true;
			acc.r_private = true;
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
	
	private VirtFldME_PreGenerate() { super(Kiev.Backend.Java15); }
	public String getDescr() { "Virtual fields pre-generation" }

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
		foreach(Field f; s.members)
			addMethodsForVirtualField(s, f);
		foreach(Field f; s.members) {
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

		String set_name = (nameSet+f.id.sname).intern();
		String get_name = (nameGet+f.id.sname).intern();

		foreach(Method m; s.members) {
			if( m.id.equals(set_name) ) {
				set_found = true;
				if( get_found ) break;
			}
			else if( m.id.equals(get_name) ) {
				get_found = true;
				if( set_found ) break;
			}
		}
		if( !set_found && MetaAccess.writeable(f) ) {
			Method set_var = new Method(set_name,Type.tpVoid,f.getJavaFlags() | ACC_SYNTHETIC);
			if (s.isInterface())
				set_var.setFinal(false);
			else if (f.meta.getU(VNode_Base.mnAtt) != null)
				set_var.setFinal(true);
			s.addMethod(set_var);
			set_var.meta.setU(new UserMeta(nameMetaSetter)).resolve(null);
			FormPar value;
			if (f.isStatic()) {
				value = new FormPar(f.pos,"value",f.type,FormPar.PARAM_NORMAL,0);
				set_var.params.add(value);
			} else {
				value = new FormPar(f.pos,"value",f.type,FormPar.PARAM_NORMAL,0);
				set_var.params.add(value);
			}
			if( !f.isAbstract() ) {
				Block body = new Block(f.pos);
				set_var.open();
				set_var.body = body;
				ENode ass_st = new ExprStat(f.pos,
					new AssignExpr(f.pos,Operator.Assign,
						f.isStatic()? new SFldExpr(f.pos,f,true)
									: new IFldExpr(f.pos,new ThisExpr(0),f,true),
						new LVarExpr(f.pos,value)
					)
				);
				body.stats.append(ass_st);
			}
			Field.SETTER_ATTR.set(f, set_var);
		}
		else if( set_found && !MetaAccess.writeable(f) ) {
			Kiev.reportError(f,"Virtual set$ method for non-writeable field "+f);
		}

		if (!f.isVirtual())
			return;		// no need to generate getter
		if( !get_found && MetaAccess.readable(f)) {
			Method get_var = new Method(get_name,f.type,f.getJavaFlags() | ACC_SYNTHETIC);
			if (s.isInterface())
				get_var.setFinal(false);
			if (f.meta.getU(VNode_Base.mnAtt) != null)
				get_var.setFinal(true);
			s.addMethod(get_var);
			get_var.meta.setU(new UserMeta(nameMetaGetter)).resolve(null);
			if( !f.isAbstract() ) {
				Block body = new Block(f.pos);
				get_var.open();
				get_var.body = body;
				body.stats.add(new ReturnStat(f.pos,new IFldExpr(f.pos,new ThisExpr(0),f,true)));
			}
			Field.GETTER_ATTR.set(f, get_var);
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
	
	private VirtFldBE_Rewrite() { super(Kiev.Backend.Java15); }
	public String getDescr() { "Virtual fields rewrite" }

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr);
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
		String get_name = (nameGet+f.id.sname).intern();

		if (fa.ctx_method != null && fa.ctx_method.id.equals(get_name) && fa.ctx_tdecl.instanceOf(f.ctx_tdecl)) {
			fa.setAsField(true);
			return true;
		}
		// We rewrite by get$ method. set$ method is rewritten by AssignExpr
		Method getter = (Method)Field.GETTER_ATTR.get(f);
		if (getter == null) {
			Kiev.reportError(fa, "Getter method for virtual field "+f+" not found");
			fa.setAsField(true);
			return true;
		}
		fa.open();
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
			String set_name = (nameSet+f.id.sname).intern();
	
			if (ae.ctx_method != null && ae.ctx_method.id.equals(set_name) && ae.ctx_tdecl.instanceOf(f.ctx_tdecl)) {
				fa.setAsField(true);
				return true;
			}
			// Rewrite by set$ method
			Method getter = (Method)Field.GETTER_ATTR.get(f);
			Method setter = (Method)Field.SETTER_ATTR.get(f);
			if (setter == null) {
				Kiev.reportError(fa, "Setter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			if (getter == null && (!ae.isGenVoidExpr() || !(ae.op == Operator.Assign || ae.op == Operator.Assign2))) {
				Kiev.reportError(fa, "Getter method for virtual field "+f+" not found");
				fa.setAsField(true);
				return true;
			}
			ae.open();
			fa.open();
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
				if (fa.obj instanceof ThisExpr) {
					acc = ~fa.obj;
				}
				else if (fa.obj instanceof LVarExpr) {
					acc = ((LVarExpr)fa.obj).getVar();
				}
				else {
					Var var = new Var(0,"tmp$virt",fa.obj.getType(),0);
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
			String set_name = (nameSet+f.id.sname).intern();
			String get_name = (nameGet+f.id.sname).intern();
	
			if (ie.ctx_method != null
			&& (ie.ctx_method.id.equals(set_name) || ie.ctx_method.id.equals(get_name))
			&& ie.ctx_tdecl.instanceOf(f.ctx_tdecl) )
			{
				fa.setAsField(true);
				return true;
			}
			// Rewrite by set$ method
			Method getter = (Method)Field.GETTER_ATTR.get(f);
			Method setter = (Method)Field.SETTER_ATTR.get(f);
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
			ie.open();
			fa.open();
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
				if (fa.obj instanceof ThisExpr) {
					acc = fa.obj;
				}
				else if (fa.obj instanceof LVarExpr) {
					acc = ((LVarExpr)fa.obj).getVar();
				}
				else {
					Var var = new Var(0,"tmp$virt",fa.obj.getType(),0);
					var.init = ~fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				Var res = null;
				if (ie.op == Operator.PostIncr || ie.op == Operator.PostDecr) {
					res = new Var(0,"tmp$res",f.getType(),0);
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
		throw new RuntimeException("Unknown accessor "+o);
	}

}
