package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */
@singleton
public final class ProcessVirtFld extends TransfProcessor implements Constants {
	
	private ProcessVirtFld() {
		super(Kiev.Ext.VirtualFields);
	}

	////////////////////////////////////////////////////
	//	   PASS - autoGenerateMembers                 //
	////////////////////////////////////////////////////

	public void autoGenerateMembers(ASTNode:ASTNode node) {
		return;
	}
	
	public void autoGenerateMembers(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.autoGenerateMembers(dn);
	}
	
	public void autoGenerateMembers(Struct:ASTNode s) {
		addAbstractFields(s);
		foreach(Struct sub; s.sub_clazz)
			autoGenerateMembers(sub);
	}
	
	public void addAbstractFields(Struct s) {
		foreach(Method m; s.members) {
			//trace(Kiev.debugCreation,"check "+m.name.name+" to be a setter");
			if (m.id.sname.startsWith(nameSet))
				addSetterForAbstractField(s, m.id.sname.substr(nameSet.length()), m);
			if (m.id.aliases != null) {
				foreach (KString name; m.id.aliases) {
					//trace(Kiev.debugCreation,"check "+name+" to be a setter");
					if (name.startsWith(nameSet))
						addSetterForAbstractField(s, name.substr(nameSet.length()), m);
				}
			}
			//trace(Kiev.debugCreation,"check "+m.name.name+" to be a getter");
			if (m.id.sname.startsWith(nameGet))
				addGetterForAbstractField(s, m.id.sname.substr(nameGet.length()), m);
			if (m.id.aliases != null) {
				foreach (KString name; m.id.aliases) {
					//trace(Kiev.debugCreation,"check "+name+" to be a getter");
					if (name.startsWith(nameGet))
						addGetterForAbstractField(s, name.substr(nameGet.length()), m);
				}
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
			if (f.acc == null) f.acc = new Access(0);
		} else {
			s.addField(f=new Field(name,m.type.arg(0),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC));
			f.acc = new Access(0);
			f.acc.flags = 0;
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		if (f.getMetaVirtual() == null)
			f.addNodeData(new MetaVirtual(), MetaVirtual.ATTR);
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
		Access.verifyDecl(f);
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
			if (f.acc == null) f.acc = new Access(0);
		} else {
			s.addField(f=new Field(name,m.type.ret(),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC));
			f.acc = new Access(0);
			f.acc.flags = 0;
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		if (f.getMetaVirtual() == null)
			f.addNodeData(new MetaVirtual(), MetaVirtual.ATTR);
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
		Access.verifyDecl(f);
	}
	
	public BackendProcessor getBackend(Kiev.Backend backend) {
		if (backend == Kiev.Backend.Java15)
			return JavaVirtFldBackend;
		return null;
	}
	
}

@singleton
class JavaVirtFldBackend extends BackendProcessor implements Constants {

	public static final KString nameNode			= KString.from("kiev.vlang.ASTNode"); 

	private static Type tpNode;

	private JavaVirtFldBackend() {
		super(Kiev.Backend.Java15);
	}
	

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void preGenerate(ASTNode:ASTNode node) {
		return;
	}
	
	public void preGenerate(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.preGenerate(dn);
	}
	
	public void preGenerate(Struct:ASTNode s) {
		foreach(Field f; s.members)
			addMethodsForVirtualField(s, f);
		foreach(Field f; s.members) {
			if (!f.isVirtual())
				continue;
			if (s.isInterface() && !f.isAbstract())
				f.setAbstract(true);
		}
		foreach(Struct sub; s.sub_clazz)
			preGenerate(sub);
	}
	
	private static void addMethodsForVirtualField(Struct s, Field f) {
		if (tpNode == null)
			tpNode = Env.loadStruct(nameNode).ctype;
		if (tpNode == null) {
			Kiev.reportError("Cannot find class "+nameNode);
			return;
		}

		if( f.isStatic() && f.isVirtual() ) {
			Kiev.reportError(f,"Static fields can't be virtual");
			f.setVirtual(false);
		}
		if( s.isInterface() && f.isVirtual() ) f.setAbstract(true);

		if( !f.isVirtual() ) return;

		if (f.getMetaVirtual() == null)
			f.addNodeData(new MetaVirtual(), MetaVirtual.ATTR);

		// Check set$/get$ methods
		boolean set_found = false;
		boolean get_found = false;

		KString set_name = new KStringBuffer(nameSet.length()+f.id.sname.length()).
			append_fast(nameSet).append_fast(f.id.sname).toKString();
		KString get_name = new KStringBuffer(nameGet.length()+f.id.sname.length()).
			append_fast(nameGet).append_fast(f.id.sname).toKString();

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
		if( !set_found && Access.writeable(f) ) {
			Method set_var = new Method(set_name,Type.tpVoid,f.getJavaFlags() | ACC_SYNTHETIC);
			if (s.isInterface())
				set_var.setFinal(false);
			else if (f.meta.get(ProcessVNode.mnAtt) != null)
				set_var.setFinal(true);
			s.addMethod(set_var);
			FormPar value;
			if (f.isStatic()) {
				value = new FormPar(f.pos,KString.from("value"),f.type,FormPar.PARAM_NORMAL,0);
				set_var.params.add(value);
			} else {
				value = new FormPar(f.pos,KString.from("value"),f.type,FormPar.PARAM_NORMAL,0);
				set_var.params.add(value);
			}
			if( !f.isAbstract() ) {
				Block body = new Block(f.pos);
				set_var.body = body;
				if (f.meta.get(ProcessVNode.mnAtt) != null && f.type.isInstanceOf(tpNode)) {
					ENode p_st = new IfElseStat(0,
							new BinaryBoolExpr(0, BinaryOperator.NotEquals,
								new IFldExpr(0,new ThisExpr(0),f,true),
								new ConstNullExpr()
							),
							new Block(0,new ENode[]{
								new ExprStat(0,
									new ASTCallAccessExpression(0,
										new IFldExpr(0,new ThisExpr(0),f,true),
										KString.from("callbackDetached"),
										ENode.emptyArray
									)
								)
							}),
							null
						);
					body.stats.append(p_st);
				}
				ENode ass_st = new ExprStat(f.pos,
					new AssignExpr(f.pos,AssignOperator.Assign,
						f.isStatic()? new SFldExpr(f.pos,f,true)
									: new IFldExpr(f.pos,new ThisExpr(0),f,true),
						new LVarExpr(f.pos,value)
					)
				);
				body.stats.append(ass_st);
				if (f.meta.get(ProcessVNode.mnAtt) != null && f.type.isInstanceOf(tpNode)) {
					KString fname = new KStringBuffer().append("nodeattr$").append(f.id.sname).toKString();
					Field fatt = f.ctx_clazz.resolveField(fname);
					ENode p_st = new IfElseStat(0,
							new BinaryBoolExpr(0, BinaryOperator.NotEquals,
								new LVarExpr(0, value),
								new ConstNullExpr()
							),
							new ExprStat(0,
								new ASTCallAccessExpression(0,
									new LVarExpr(0, value),
									KString.from("callbackAttached"),
									new ENode[] {
										new ThisExpr(),
										new SFldExpr(f.pos, fatt)
									}
								)
							),
							null
						);
					body.stats.append(p_st);
				}
				body.stats.append(new ReturnStat(f.pos,null));
			}
			f.getMetaVirtual().set = set_var;
		}
		else if( set_found && !Access.writeable(f) ) {
			Kiev.reportError(f,"Virtual set$ method for non-writeable field "+f);
		}

		if (!f.isVirtual())
			return;		// no need to generate getter
		if( !get_found && Access.readable(f)) {
			Method get_var = new Method(get_name,f.type,f.getJavaFlags() | ACC_SYNTHETIC);
			if (s.isInterface())
				get_var.setFinal(false);
			if (f.meta.get(ProcessVNode.mnAtt) != null)
				get_var.setFinal(true);
			s.addMethod(get_var);
			if( !f.isAbstract() ) {
				Block body = new Block(f.pos);
				get_var.body = body;
				body.stats.add(new ReturnStat(f.pos,new IFldExpr(f.pos,new ThisExpr(0),f,true)));
			}
			f.getMetaVirtual().get = get_var;
		}
		else if( get_found && !Access.readable(f) ) {
			Kiev.reportError(f,"Virtual get$ method for non-readable field "+f);
		}
	}

	
	////////////////////////////////////////////////////
	//	   PASS - rewrite code                        //
	////////////////////////////////////////////////////

	public void rewriteNode(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(NodeData n) { if (n instanceof ASTNode) return JavaVirtFldBackend.this.rewrite((ASTNode)n); return false; }
		});
	}
	
	boolean rewrite(ASTNode:ASTNode o) {
		//System.out.println("ProcessVirtFld: rewrite "+(o==null?"null":o.getClass().getName())+" in "+id);
		return true;
	}

	boolean rewrite(IFldExpr:ASTNode fa) {
		//System.out.println("ProcessVirtFld: rewrite "+fa.getClass().getName()+" "+fa+" in "+id);
		Field f = fa.var;
		if( !f.isVirtual() || fa.isAsField() )
			return true;
		KString get_name = new KStringBuffer(nameGet.length()+f.id.sname.length()).
			append_fast(nameGet).append_fast(f.id.sname).toKString();

		if (fa.ctx_method != null && fa.ctx_method.id.equals(get_name) && fa.ctx_clazz.instanceOf(f.ctx_clazz)) {
			fa.setAsField(true);
			return true;
		}
		// We rewrite by get$ method. set$ method is rewritten by AssignExpr
		if (f.getMetaVirtual().get == null) {
			Kiev.reportError(fa, "Getter method for virtual field "+f+" not found");
			fa.setAsField(true);
			return true;
		}
		ENode ce = new CallExpr(fa.pos, ~fa.obj, f.getMetaVirtual().get, ENode.emptyArray);
		//ce = ce.resolveExpr(fa.getType());
		fa.replaceWithNode(ce);
		rewriteNode(ce);
		return false;
	}
	
	boolean rewrite(AssignExpr:ASTNode ae) {
		//System.out.println("ProcessVirtFld: rewrite "+ae.getClass().getName()+" "+ae+" in "+id);
		if (ae.lval instanceof IFldExpr) {
			IFldExpr fa = (IFldExpr)ae.lval;
			Field f = fa.var;
			if( !f.isVirtual() || fa.isAsField() )
				return true;
			KString set_name = new KStringBuffer(nameSet.length()+f.id.sname.length()).
				append_fast(nameSet).append_fast(f.id.sname).toKString();
	
			if (ae.ctx_method != null && ae.ctx_method.id.equals(set_name) && ae.ctx_clazz.instanceOf(f.ctx_clazz)) {
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
			Type ae_tp = ae.isGenVoidExpr() ? Type.tpVoid : ae.getType();
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
			ENode expr;
			if (ae.isGenVoidExpr() && (ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2)) {
				expr = new CallExpr(ae.pos, ~fa.obj, f.getMetaVirtual().set, new ENode[]{~ae.value});
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
					Var var = new Var(0,KString.from("tmp$virt"),fa.obj.getType(),0);
					var.init = ~fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				ENode g;
				if !(ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2) {
					g = new CallExpr(0, mkAccess(acc), f.getMetaVirtual().get, ENode.emptyArray);
					g = new BinaryExpr(ae.pos, op, g, ~ae.value);
				} else {
					g = ~ae.value;
				}
				g = new CallExpr(ae.pos, mkAccess(acc), f.getMetaVirtual().set, new ENode[]{g});
				be.stats.add(new ExprStat(0, g));
				if (!ae.isGenVoidExpr()) {
					g = new CallExpr(0, mkAccess(acc), f.getMetaVirtual().get, ENode.emptyArray);
					be.stats.add(g);
				}
				expr = be;
			}
			ae.replaceWithNode(expr);
			expr.setGenVoidExpr(ae.isGenVoidExpr());
			expr.resolve(ae_tp);
			rewriteNode(expr);
			return false;
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
			KString set_name = new KStringBuffer(nameSet.length()+f.id.sname.length()).
				append_fast(nameSet).append_fast(f.id.sname).toKString();
			KString get_name = new KStringBuffer(nameGet.length()+f.id.sname.length()).
				append_fast(nameGet).append_fast(f.id.sname).toKString();
	
			if (ie.ctx_method != null
			&& (ie.ctx_method.id.equals(set_name) || ie.ctx_method.id.equals(get_name))
			&& ie.ctx_clazz.instanceOf(f.ctx_clazz) )
			{
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
			ENode expr;
			Type ie_tp = ie.isGenVoidExpr() ? Type.tpVoid : ie.getType();
			if (ie.isGenVoidExpr()) {
				if (ie.op == PrefixOperator.PreIncr || ie.op == PostfixOperator.PostIncr) {
					expr = new AssignExpr(ie.pos, AssignOperator.AssignAdd, ~ie.lval, new ConstIntExpr(1));
				} else {
					expr = new AssignExpr(ie.pos, AssignOperator.AssignAdd, ~ie.lval, new ConstIntExpr(-1));
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
					Var var = new Var(0,KString.from("tmp$virt"),fa.obj.getType(),0);
					var.init = ~fa.obj;
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
				ENode g;
				g = new CallExpr(0, mkAccess(acc), f.getMetaVirtual().get, ENode.emptyArray);
				if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr)
					g = new AssignExpr(ie.pos, AssignOperator.Assign, mkAccess(res), g);
				g = new BinaryExpr(ie.pos, BinaryOperator.Add, ce, g);
				g = new CallExpr(ie.pos, mkAccess(acc), f.getMetaVirtual().set, new ENode[]{g});
				be.stats.add(new ExprStat(0, g));
				if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr)
					be.stats.add(mkAccess(res));
				else
					be.stats.add(new CallExpr(0, mkAccess(acc), f.getMetaVirtual().get, ENode.emptyArray));
				expr = be;
			}
			ie.replaceWithNode(expr);
			expr.setGenVoidExpr(ie.isGenVoidExpr());
			expr.resolve(ie_tp);
			rewriteNode(expr);
			return false;
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
