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
public class PizzaFE_Pass3 extends TransfProcessor {
	private PizzaFE_Pass3() { super(KievExt.PizzaCase); }
	public String getDescr() { "Pizza case members" }

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
	}

	public void doProcess(FileUnit:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode clazz) {
		if !(clazz.isPizzaCase()) {
			foreach (Struct dn; clazz.members)
				this.doProcess(dn);
			return;
		}
		if (clazz.isSingleton())
			return;
		PizzaCase pcase = (PizzaCase)clazz;
		// Create constructor for pizza case
		Constructor init = new Constructor(ACC_PUBLIC|ACC_SYNTHETIC);
		foreach (Field f; pcase.group.decls)
			init.params.add(new LVar(f.pos,f.sname,f.type,Var.PARAM_NORMAL,0));
		init.body = new Block(clazz.pos);
		int p = 0;
		foreach (Field f; pcase.group.decls) {
			Var v = null;
			foreach (Var fp; init.params; fp.sname == f.sname) {
				init.block.stats.insert(
					p++,
					new ExprStat(
						new AssignExpr(f.pos,Operator.Assign,
							new IFldExpr(f.pos,new ThisExpr(f.pos),f),
							new LVarExpr(f.pos,fp)
						)
					)
				);
				break;
			}
		}
		init.pos = clazz.pos;
		foreach (Constructor ctor; clazz.members; ctor.isSynthetic()) {
			ctor.replaceWithNode(init);
			return;
		}
		clazz.addMethod(init);
	}
}

@singleton
public class PizzaME_PreGenerate extends BackendProcessor {
	private PizzaME_PreGenerate() { super(KievBackend.Java15); }
	public String getDescr() { "Pizza case pre-generation" }

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"PizzaME_PreGenerate");
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
	
	public void doProcess(Struct:ASTNode clazz) {
		foreach (Struct dn; clazz.members)
			this.doProcess(dn);
		if( clazz.isPizzaCase() ) {
			PizzaCase pcase = (PizzaCase)clazz;
			Field ftag = clazz.addField(new Field(
				nameCaseTag,Type.tpInt,ACC_PUBLIC|ACC_FINAL|ACC_STATIC) );
			ftag.init = new ConstIntExpr(pcase.tag);

			Method gettag = new MethodImpl(nameGetCaseTag,Type.tpInt,ACC_PUBLIC | ACC_SYNTHETIC);
			gettag.body = new Block(gettag.pos);
			gettag.block.stats.add(
				new ReturnStat(gettag.pos,new SFldExpr(ftag.pos,ftag))
			);
			clazz.addMethod(gettag);
		}
		else if( clazz.isHasCases() ) {
			// Add get$case$tag() method to itself
			Method gettag = new MethodImpl(Constants.nameGetCaseTag,Type.tpInt,ACC_PUBLIC | ACC_SYNTHETIC);
			gettag.body = new Block(gettag.pos);
			gettag.block.stats.add(
				new ReturnStat(gettag.pos,new ConstIntExpr(0))
			);
			clazz.addMethod(gettag);
		}

	}
}

