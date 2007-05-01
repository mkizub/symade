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
package kiev.parser;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="Id")
public class ASTIdentifier extends ENode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = ASTIdentifier;

	public ASTIdentifier() {}

	public ASTIdentifier(String name) {
		this.ident = name;
	}

	public ASTIdentifier(Token t) {
		this.pos = t.getPos();
		this.ident = t.image;
	}

	public ASTIdentifier(int pos, String name) {
		this.pos = pos;
		this.ident = name;
	}

	public int getPriority() { return 256; }

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.ident = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.ident = t.image;
	}
	
	public boolean preResolveIn() {
		String name = this.ident;
		if( name == Constants.nameThis ) {
			replaceWithNode(new ThisExpr(pos));
			return false;
		}
		else if( name == Constants.nameSuper ) {
			replaceWithNode(new SuperExpr(pos));
			return false;
		}

		// resolve in the path of scopes
		ASTNode@ v;
		ResInfo info = new ResInfo(this,name);
		if( !PassInfo.resolveNameR((ASTNode)this,v,info) )
			throw new CompilerException(this,"Unresolved identifier "+name);
		if( v instanceof Opdef ) {
			ASTOperator op = new ASTOperator();
			op.ident = name;
			replaceWithNode(op);
		}
		else if( v instanceof TypeDecl ) {
			TypeDecl td = (TypeDecl)v;
			td.checkResolved();
			replaceWithNode(new TypeNameRef(name, td.getType()));
		}
		else {
			replaceWithNode(info.buildAccess((ASTNode)this, null, v).closeBuild());
		}
		return false;
	}

	public void resolve(Type reqType) {
		String name = this.ident;
		if( name == Constants.nameFILE ) {
			replaceWithNode(new ConstStringExpr(Kiev.getCurFile()));
			return;
		}
		else if( name == Constants.nameLINENO ) {
			replaceWithNode(new ConstIntExpr(pos>>>11));
			return;
		}
		else if( name == Constants.nameMETHOD ) {
			if( ctx_method != null )
				replaceWithNode(new ConstStringExpr(ctx_method.sname));
			else
				replaceWithNode(new ConstStringExpr(nameInit));
			return;
		}
		else if( name == Constants.nameDEBUG ) {
			replaceWithNode(new ConstBoolExpr(Kiev.debugOutputA));
			return;
		}
		else if( name == Constants.nameReturnVar ) {
			Kiev.reportWarning(this,"Keyword '$return' is deprecated. Replace with 'Result', please");
			name = Constants.nameResultVar;
		}
		DNode@ v;
		ResInfo info = new ResInfo(this,name);
		if( !PassInfo.resolveNameR(this,v,info) ) {
			if( name.startsWith(Constants.nameDEF) ) {
				String prop = name.toString().substring(2);
				String val = Env.getProperty(prop);
				if( val == null ) val = Env.getProperty(prop.replace('_','.'));
				if( val != null ) {
					if( reqType ≡ null || reqType ≈ Type.tpString) {
						replaceWithNode(new ConstStringExpr(val));
						return;
					}
					if( reqType.isBoolean() ) {
						if( val == "" ) 
							replaceWithNode(new ConstBoolExpr(true));
						else
							replaceWithNode(new ConstBoolExpr(Boolean.valueOf(val).booleanValue()));
						return;
					}
					if( reqType.isInteger() ) {
						replaceWithNode(new ConstIntExpr(Integer.valueOf(val).intValue()));
						return;
					}
					if( reqType.isNumber() ) {
						replaceWithNode(new ConstDoubleExpr(Double.valueOf(val).doubleValue()));
					}
					replaceWithNode(new ConstStringExpr(val));
					return;
				}
				if( reqType.isBoolean() )
					replaceWithNode(new ConstBoolExpr(false));
				else
					replaceWithNode(new ConstNullExpr());
				return;
			}
			throw new CompilerException(this,"Unresolved identifier "+name);
		}
		if( v instanceof Struct ) {
			Struct s = (Struct)v;
			s.checkResolved();
			if( reqType != null && reqType.equals(Type.tpInt) ) {
				if( s.isPizzaCase() ) {
					PizzaCase pcase = (PizzaCase)s;
					replaceWithNodeResolve(reqType, new ConstIntExpr(pcase.tag));
					return;
				}
			}
			TypeNameRef tnr = new TypeNameRef(name,s.xtype);
			tnr.pos = this.pos;
			replaceWithNode(tnr);
			return;
		}
		else if( v instanceof TypeDecl ) {
			replaceWithNode(new TypeRef(((TypeDecl)v).getType()));
			return;
		}
		replaceWithNodeResolve(reqType, info.buildAccess(this, null, v).closeBuild());
	}

	public String toString() {
		return ident;
	}

	public ANode doRewrite(RewriteContext ctx) {
		return (ANode)ctx.root.getVal(this.ident);
	}
}

