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
package kiev.ir.java15;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ViewOf(vcast=true)
public static view RVar of Var extends RDNode {
	public	TypeRef		vtype;
	public	ENode		init;

	// init wrapper
	public final boolean isInitWrapper();
	public final void setInitWrapper(boolean on);
	// need a proxy access 
	public final boolean isNeedProxy();
	public final void setNeedProxy(boolean on);

	public boolean preGenerate(Env env) { return true; }

	public void resolveDecl(Env env) {
		if( isResolved() ) return;
		Type tp = this.getType(env);
		if (init instanceof TypeRef)
			((RTypeRef)(TypeRef)init).toExpr(tp,env);
		if (tp instanceof CTimeType) {
			init = tp.makeInitExpr((Var)this,init);
			try {
				Kiev.runProcessorsOn(init);
				resolveENode(init,tp.getEnclosedType(),env);
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		else if (init != null) {
			try {
				resolveENode(init,tp,env);
				Type it = init.getType(env);
				if (!it.isInstanceOf(tp)) {
					if (it.getAutoCastTo(tp) == null)
						Kiev.reportWarning(this, "Cannot auto-cast initializer to the var "+this);
					init = new CastExpr(init.pos,tp,~init);
					resolveENode(init,tp,env);
				}
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		setResolved(true);
	}
}
