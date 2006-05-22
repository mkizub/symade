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

	public static final String nameMetaGetter = "kiev.stdlib.meta.getter"; 
	public static final String nameMetaSetter = "kiev.stdlib.meta.setter"; 
	
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
		foreach(Struct sub; s.sub_decls)
			autoGenerateMembers(sub);
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
		if( f != null ) {
			trace(Kiev.debugCreation,"method "+m+" has field "+f);
			if (f.parent() != m.parent())
				return;
			Method setter = (Method)Field.SETTER_ATTR.get(f);
			if (setter != null && setter != m)
				return;
			if (f.acc == null) f.acc = new Access(0);
		} else {
			s.addField(f=new Field(name,m.type.arg(0),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC));
			f.acc = new Access(0);
			f.acc.flags = 0;
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		f.setVirtual(true);
		Field.SETTER_ATTR.set(f, m);
		if (m.meta.get(nameMetaSetter) == null) {
			Kiev.reportWarning(m,"Method looks to be a setter, but @setter is not specified");
		}
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
	
	private void addGetterForAbstractField(Struct s, String name, Method m) {
		name = name.intern();
		Field f = s.resolveField( name, false );
		if( f != null ) {
			trace(Kiev.debugCreation,"method "+m+" has field "+f);
			if (f.parent() != m.parent())
				return;
			Method getter = (Method)Field.GETTER_ATTR.get(f);
			if (getter != null && getter != m)
				return;
			if (f.acc == null) f.acc = new Access(0);
		} else {
			s.addField(f=new Field(name,m.type.ret(),m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT | ACC_SYNTHETIC));
			f.acc = new Access(0);
			f.acc.flags = 0;
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		f.setVirtual(true);
		Field.GETTER_ATTR.set(f, m);
		if (m.meta.get(nameMetaGetter) == null) {
			Kiev.reportWarning(m,"Method looks to be a getter, but @getter is not specified");
		}
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

	public static final String nameMetaGetter = ProcessVirtFld.nameMetaGetter; 
	public static final String nameMetaSetter = ProcessVirtFld.nameMetaSetter; 
	
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
		foreach(Struct sub; s.sub_decls)
			preGenerate(sub);
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
		if( !set_found && Access.writeable(f) ) {
			Method set_var = new Method(set_name,Type.tpVoid,f.getJavaFlags() | ACC_SYNTHETIC);
			if (s.isInterface())
				set_var.setFinal(false);
			else if (f.meta.get(ProcessVNode.mnAtt) != null)
				set_var.setFinal(true);
			s.addMethod(set_var);
			set_var.meta.set(new Meta(nameMetaSetter)).resolve();
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
				set_var.body = body;
				ENode ass_st = new ExprStat(f.pos,
					new AssignExpr(f.pos,AssignOperator.Assign,
						f.isStatic()? new SFldExpr(f.pos,f,true)
									: new IFldExpr(f.pos,new ThisExpr(0),f,true),
						new LVarExpr(f.pos,value)
					)
				);
				body.stats.append(ass_st);
			}
			Field.SETTER_ATTR.set(f, set_var);
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
			get_var.meta.set(new Meta(nameMetaGetter)).resolve();
			if( !f.isAbstract() ) {
				Block body = new Block(f.pos);
				get_var.body = body;
				body.stats.add(new ReturnStat(f.pos,new IFldExpr(f.pos,new ThisExpr(0),f,true)));
			}
			Field.GETTER_ATTR.set(f, get_var);
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
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return JavaVirtFldBackend.this.rewrite((ASTNode)n); return false; }
		});
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
		ENode ce = new CallExpr(fa.pos, ~fa.obj, getter, ENode.emptyArray);
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
			if (getter == null && (!ae.isGenVoidExpr() || !(ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2))) {
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
				if !(ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2) {
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
					Var var = new Var(0,"tmp$virt",fa.obj.getType(),0);
					var.init = ~fa.obj;
					be.addSymbol(var);
					acc = var;
				}
				Var res = null;
				if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr) {
					res = new Var(0,"tmp$res",f.getType(),0);
					be.addSymbol(res);
				}
				ConstExpr ce;
				if (ie.op == PrefixOperator.PreIncr || ie.op == PostfixOperator.PostIncr)
					ce = new ConstIntExpr(1);
				else
					ce = new ConstIntExpr(-1);
				ENode g;
				g = new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray);
				if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr)
					g = new AssignExpr(ie.pos, AssignOperator.Assign, mkAccess(res), g);
				g = new BinaryExpr(ie.pos, BinaryOperator.Add, ce, g);
				g = new CallExpr(ie.pos, mkAccess(acc), setter, new ENode[]{g});
				be.stats.add(new ExprStat(0, g));
				if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr)
					be.stats.add(mkAccess(res));
				else
					be.stats.add(new CallExpr(0, mkAccess(acc), getter, ENode.emptyArray));
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
