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

public final class ProcessVirtFld implements Constants {
	
	public void createMembers(Struct s) {
		foreach(Field f; s.fields)
			addMethodsForVirtualField(s, f);
		addAbstractFields(s);
		foreach (Field f; s.fields) {
			if (!f.isVirtual())
				continue;
			if (s.isInterface() && !f.isAbstract())
				f.setAbstract(true);
		}
	}
	
	public static void addMethodsForVirtualField(Struct s, Field f) {
		if( f.isStatic() && f.isVirtual() ) {
			Kiev.reportError(f.pos,"Static fields can't be virtual");
			f.setVirtual(false);
		}
		if( s.isInterface() && f.isVirtual() ) f.setAbstract(true);

		if( !f.isVirtual() ) return;
		// Check set$/get$ methods
		boolean set_found = false;
		boolean get_found = false;

		KString set_name = new KStringBuffer(nameSet.length()+f.name.name.length()).
			append_fast(nameSet).append_fast(f.name.name).toKString();
		KString get_name = new KStringBuffer(nameGet.length()+f.name.name.length()).
			append_fast(nameGet).append_fast(f.name.name).toKString();

		foreach(Method m; s.methods; m.name.name.byteAt(3) == '$') {
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
			Method set_var = new Method(s,set_name,
				MethodType.newMethodType(null,null,new Type[]{f.type},Type.tpVoid),
				f.getJavaFlags()
			);
			Var self;
			Var value;
			if (f.isStatic()) {
				self = null;
				value = new Var(f.pos,set_var,KString.from("value"),f.type,0);
				set_var.params.add(value);
			} else {
				self = new Var(f.pos,set_var,nameThis,s.type,0);
				value = new Var(f.pos,set_var,KString.from("value"),f.type,0);
				set_var.params.add(self);
				set_var.params.add(value);
			}
			if( !f.isAbstract() ) {
				BlockStat body = new BlockStat(f.pos,set_var,ASTNode.emptyArray);
				Statement ass_st = new ExprStat(f.pos,body,
					new AssignExpr(f.pos,AssignOperator.Assign,
						f.isStatic()? new StaticFieldAccessExpr(f.pos,s,f,true)
									: new AccessExpr(f.pos,new ThisExpr(0),f,true),
						new VarAccessExpr(f.pos,value)
					)
				);
				body.stats.append(ass_st);
				Type astT = Type.fromSignature(KString.from("Lkiev/vlang/ASTNode;"));
				if (f.meta.get(ProcessVNode.mnAtt) != null && f.type.isInstanceOf(astT)) {
					Statement p_st = new IfElseStat(0,
							new BinaryBooleanExpr(0, BinaryOperator.NotEquals,
								new VarAccessExpr(0, value),
								new ConstExpr(0, null)
							),
							new ExprStat(0,null,
								new AssignExpr(0, AssignOperator.Assign,
									new AccessExpr(0,
										new VarAccessExpr(0, value),
										astT.clazz.resolveField(KString.from("parent"))
									),
									new ThisExpr()
								)
							),
							null
						);
					body.stats.append(p_st);
				}
				body.stats.append(new ReturnStat(f.pos,body,null));
				set_var.body = body;
			}
			s.addMethod(set_var);
			f.set = set_var;
		}
		else if( set_found && !f.acc.writeable() ) {
			Kiev.reportError(f.pos,"Virtual set$ method for non-writeable field");
		}

		if (!f.isVirtual())
			return;		// no need to generate getter
		if( !get_found && f.acc.readable()) {
			Method get_var = new Method(s,get_name,
				MethodType.newMethodType(null,Type.emptyArray,f.type),
				f.getJavaFlags()
			);
			Var self = new Var(f.pos,get_var,nameThis,s.type,0);
			get_var.params.add(self);
			if( !f.isAbstract() ) {
				BlockStat body = new BlockStat(f.pos,get_var,ASTNode.emptyArray);
				body.stats.add(new ReturnStat(f.pos,body,new AccessExpr(f.pos,new ThisExpr(0),f,true)));
				get_var.body = body;
			}
			s.addMethod(get_var);
			f.get = get_var;
		}
		else if( get_found && !f.acc.readable() ) {
			Kiev.reportError(f.pos,"Virtual get$ method for non-readable field");
		}
	}

	public void autoGenerateMembers(Struct s) {
	}
	
	private void addSetterForAbstractField(Struct s, KString name, Method m) {
		Field f = s.resolveField( name, false );
		if( f != null ) {
			trace(Kiev.debugCreation,"method "+m+" has field "+f);
			if (f.parent != m.parent)
				return;
			if (f.set != null && f.set != m)
				return;
		} else {
			s.addField(f=new Field(s,name,m.type.args[0],m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT));
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		f.set = m;
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
		f.acc.verifyAccessDecl(f);
	}
	
	private void addGetterForAbstractField(Struct s, KString name, Method m) {
		Field f = s.resolveField( name, false );
		if( f != null ) {
			trace(Kiev.debugCreation,"method "+m+" has field "+f);
			if (f.parent != m.parent)
				return;
			if (f.get != null && f.get != m)
				return;
		} else {
			s.addField(f=new Field(s,name,m.type.ret,m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT));
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
		}
		f.get = m;
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
		f.acc.verifyAccessDecl(f);
	}
	
	public void addAbstractFields(Struct s) {
		foreach(Method m; s.methods) {
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
	
	
	private void rewriteNode(ASTNode node, String id) {
		foreach (String name; node.values()) {
			Object val = node.getVal(name);
			assert(!(val instanceof ASTNode) || (((ASTNode)val).parent == node), "parent of "+val+" is not "+node+" but "+((ASTNode)val).parent+":\n"+val.toJava(new Dumper()));
			rewrite(val, name);
		}
	}
	
	public void rewrite(ASTNode:Object node, String id) {
		//System.out.println("ProcessVirtFld: rewrite "+node.getClass().getName()+" in "+id);
		PassInfo.push(node);
		try {
			rewriteNode(node, id);
		} finally { PassInfo.pop(node); }
	}
	
	public void rewrite(NArr<ASTNode>:Object arr, String id) {
		//System.out.println("ProcessVirtFld: rewrite "+arr.getClass().getName()+" in "+id);
		foreach (ASTNode n; arr) {
			assert(n == null || (((ASTNode)n).parent == arr.getParent()), "parent of "+n+" is not "+arr.getParent()+" but "+n.parent+":\n"+n.toJava(new Dumper()));
			rewrite(n, id);
		}
	}

	public void rewrite(Object:Object o, String id) {
		//System.out.println("ProcessVirtFld: rewrite "+(o==null?"null":o.getClass().getName())+" in "+id);
		return;
	}

	public void rewrite(FileUnit:Object node, String id) {
		//System.out.println("ProcessPackedFld: rewrite "+node.getClass().getName()+" in "+id);
		NodeInfoPass.init();
		ScopeNodeInfoVector state = NodeInfoPass.pushState();
		state.guarded = true;
		PassInfo.push(node);
		try {
			rewriteNode(node, id);
		} finally { NodeInfoPass.close(); PassInfo.pop(node); }
	}
	
	public void rewrite(AccessExpr:Object fa, String id) {
		//System.out.println("ProcessVirtFld: rewrite "+fa.getClass().getName()+" "+fa+" in "+id);
		PassInfo.push(fa);
		try {
			Field f = fa.var;
			if( !f.isVirtual() || fa.isAsField() ) {
				rewriteNode(fa, id);
				return;
			}
			KString get_name = new KStringBuffer(nameGet.length()+f.name.name.length()).
				append_fast(nameGet).append_fast(f.name.name).toKString();
	
			if (PassInfo.method.name.equals(get_name)) {
				fa.setAsField(true);
				rewriteNode(fa, id);
				return;
			}
			// We rewrite by get$ method. set$ method is rewritten by AssignExpr
			if (f.get == null) {
				Kiev.reportError(fa.pos, "Getter method for virtual field "+f+" not found");
				fa.setAsField(true);
				rewriteNode(fa, id);
				return;
			}
			if (fa.parent == null) {
				Kiev.reportError(fa.pos, "Internal error: parent == null");
				rewriteNode(fa, id);
				return;
			}
			Expr ce = new CallAccessExpr(fa.pos, fa.parent, fa.obj, f.get, Expr.emptyArray);
			//ce = ce.resolveExpr(fa.getType());
			fa.parent.replaceVal(id, fa, ce);
			rewriteNode(ce, id);
		} finally { PassInfo.pop(fa); }
	}
	
	public void rewrite(AssignExpr:Object ae, String id) {
		//System.out.println("ProcessVirtFld: rewrite "+ae.getClass().getName()+" "+ae+" in "+id);
		PassInfo.push(ae);
		try {
			if (ae.lval instanceof AccessExpr) {
				AccessExpr fa = (AccessExpr)ae.lval;
				Field f = fa.var;
				if( !f.isVirtual() || fa.isAsField() ) {
					rewriteNode(ae, id);
					return;
				}
				KString set_name = new KStringBuffer(nameSet.length()+f.name.name.length()).
					append_fast(nameSet).append_fast(f.name.name).toKString();
		
				if (PassInfo.method.name.equals(set_name)) {
					fa.setAsField(true);
					rewriteNode(ae, id);
					return;
				}
				// Rewrite by set$ method
				if (f.set == null) {
					Kiev.reportError(fa.pos, "Setter method for virtual field "+f+" not found");
					fa.setAsField(true);
					rewriteNode(ae, id);
					return;
				}
				if (f.get == null && (!ae.isGenVoidExpr() || !(ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2))) {
					Kiev.reportError(fa.pos, "Getter method for virtual field "+f+" not found");
					fa.setAsField(true);
					rewriteNode(ae, id);
					return;
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
					expr = new CallAccessExpr(ae.pos, ae.parent, fa.obj, f.set, new Expr[]{ae.value});
					expr = expr.resolveExpr(Type.tpVoid);
				}
				else {
					BlockExpr be = new BlockExpr(ae.pos, ae.parent);
					Object acc;
					if (fa.obj instanceof ThisExpr) {
						acc = fa.obj;
					}
					else if (fa.obj instanceof VarAccessExpr) {
						acc = ((VarAccessExpr)fa.obj).var;
					}
					else {
						acc = new Var(0,null,KString.from("tmp$virt"),fa.obj.getType(),0);
						DeclStat ds = new DeclStat(fa.obj.pos, be, (Var)acc, fa.obj);
						be.addStatement(ds);
					}
					Expr g;
					if !(ae.op == AssignOperator.Assign || ae.op == AssignOperator.Assign2) {
						g = new CallAccessExpr(0, null, mkAccess(acc), f.get, Expr.emptyArray);
						g = new BinaryExpr(ae.pos, op, g, ae.value);
					} else {
						g = ae.value;
					}
					g = new CallAccessExpr(ae.pos, ae.parent, mkAccess(acc), f.set, new Expr[]{g});
					be.addStatement(new ExprStat(0, null, g));
					if (!ae.isGenVoidExpr()) {
						g = new CallAccessExpr(0, null, mkAccess(acc), f.get, Expr.emptyArray);
						be.setExpr(g);
					}
					expr = be;
					expr = expr.resolveExpr(ae.isGenVoidExpr() ? Type.tpVoid : ae.getType());
				}
				ae.parent.replaceVal(id, ae, expr);
				expr.parent = ae.parent;
				rewrite(expr, id);
			}
			else {
				rewriteNode(ae, id);
			}
		} finally { PassInfo.pop(ae); }
	}
	
	public void rewrite(IncrementExpr:Object ie, String id) {
		//System.out.println("ProcessVirtFld: rewrite "+ie.getClass().getName()+" "+ie+" in "+id);
		PassInfo.push(ie);
		try {
			if (ie.lval instanceof AccessExpr) {
				AccessExpr fa = (AccessExpr)ie.lval;
				Field f = fa.var;
				if( !f.isVirtual() || fa.isAsField() ) {
					rewriteNode(ie, id);
					return;
				}
				KString set_name = new KStringBuffer(nameSet.length()+f.name.name.length()).
					append_fast(nameSet).append_fast(f.name.name).toKString();
				KString get_name = new KStringBuffer(nameGet.length()+f.name.name.length()).
					append_fast(nameGet).append_fast(f.name.name).toKString();
		
				if (PassInfo.method.name.equals(set_name) || PassInfo.method.name.equals(get_name)) {
					fa.setAsField(true);
					rewriteNode(ie, id);
					return;
				}
				// Rewrite by set$ method
				if (f.set == null) {
					Kiev.reportError(fa.pos, "Setter method for virtual field "+f+" not found");
					fa.setAsField(true);
					rewriteNode(ie, id);
					return;
				}
				if (f.get == null) {
					Kiev.reportError(fa.pos, "Getter method for virtual field "+f+" not found");
					fa.setAsField(true);
					rewriteNode(ie, id);
					return;
				}
				Expr expr;
				if (ie.isGenVoidExpr()) {
					if (ie.op == PrefixOperator.PreIncr || ie.op == PostfixOperator.PostIncr) {
						expr = new AssignExpr(ie.pos, AssignOperator.AssignAdd, ie.lval, new ConstExpr(0,new Integer(1)));
					} else {
						expr = new AssignExpr(ie.pos, AssignOperator.AssignAdd, ie.lval, new ConstExpr(0,new Integer(-1)));
					}
					expr = expr.resolveExpr(Type.tpVoid);
					expr.setGenVoidExpr(true);
				}
				else {
					BlockExpr be = new BlockExpr(ie.pos, ie.parent);
					Object acc;
					if (fa.obj instanceof ThisExpr) {
						acc = fa.obj;
					}
					else if (fa.obj instanceof VarAccessExpr) {
						acc = ((VarAccessExpr)fa.obj).var;
					}
					else {
						acc = new Var(0,null,KString.from("tmp$virt"),fa.obj.getType(),0);
						DeclStat ds = new DeclStat(fa.obj.pos, be, (Var)acc, fa.obj);
						be.addStatement(ds);
					}
					Var res = null;
					if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr) {
						res = new Var(0,null,KString.from("tmp$res"),f.getType(),0);
						DeclStat ds = new DeclStat(fa.obj.pos, be, res);
						be.addStatement(ds);
					}
					ConstExpr ce;
					if (ie.op == PrefixOperator.PreIncr || ie.op == PostfixOperator.PostIncr)
						ce = new ConstExpr(0,new Integer(1));
					else
						ce = new ConstExpr(0,new Integer(-1));
					Expr g;
					g = new CallAccessExpr(0, null, mkAccess(acc), f.get, Expr.emptyArray);
					if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr)
						g = new AssignExpr(ie.pos, AssignOperator.Assign, mkAccess(res), g);
					g = new CallAccessExpr(ie.pos, ie.parent, mkAccess(acc), f.set, new Expr[]{g});
					be.addStatement(new ExprStat(0, null, g));
					if (ie.op == PostfixOperator.PostIncr || ie.op == PostfixOperator.PostDecr)
						be.setExpr(mkAccess(res));
					else
						be.setExpr(new CallAccessExpr(0, null, mkAccess(acc), f.get, Expr.emptyArray));
					expr = be;
					expr = expr.resolveExpr(ie.isGenVoidExpr() ? Type.tpVoid : ie.getType());
				}
				ie.parent.replaceVal(id, ie, expr);
				expr.parent = ie.parent;
				rewrite(expr, id);
			}
			else {
				rewriteNode(ie, id);
			}
		} finally { PassInfo.pop(ie); }
	}
	
	private Expr mkAccess(Object o) {
		if (o instanceof Var) return new VarAccessExpr(0,null,(Var)o);
		if (o instanceof ThisExpr) return new ThisExpr(0,null);
		throw new RuntimeException("Unknown accessor "+o);
	}
}