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

@ViewOf(vcast=true, iface=true)
public static view RVar of Var extends RDNode {
	public	TypeRef		vtype;
	public	ENode		init;

	@getter public final Type get$type();
	
	// init wrapper
	public final boolean isInitWrapper();
	public final void setInitWrapper(boolean on);
	// need a proxy access 
	public final boolean isNeedProxy();
	public final void setNeedProxy(boolean on);

	public void resolveDecl() {
		if( isResolved() ) return;
		Type tp = this.type;
		if (init instanceof TypeRef)
			((TypeRef)init).toExpr(tp);
		if (tp instanceof CTimeType) {
			init = tp.makeInitExpr((Var)this,init);
			try {
				Kiev.runProcessorsOn(init);
				init.resolve(tp.getEnclosedType());
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		else if (init != null) {
			try {
				init.resolve(tp);
				Type it = init.getType();
				if (!it.isInstanceOf(tp)) {
					if (it.getAutoCastTo(tp) == null)
						Kiev.reportWarning(this, "Cannot auto-cast initializer to the var "+this);
					init = new CastExpr(init.pos,tp,~init);
					init.resolve(tp);
				}
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		DataFlowInfo.getDFlow((Var)this).out();
		setResolved(true);
	}
}
