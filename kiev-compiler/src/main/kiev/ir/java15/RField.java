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
public static final view RField of Field extends RVar {
	public		ConstExpr		const_value;
	
	// field's initializer was already added to class initializer
	public final boolean isAddedToInit();
	public final void setAddedToInit(boolean on);

	public void resolveDecl(Env env) {
		Type tp = this.getType(env);
		if (init instanceof TypeRef)
			((RTypeRef)(TypeRef)init).toExpr(getType(env),env);
		if (tp instanceof CTimeType) {
			init = tp.makeInitExpr(this,init);
			try {
				Kiev.runProcessorsOn(init);
				resolveENode(init,tp.getEnclosedType(),env);
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		else if( init != null ) {
			try {
				resolveENode(init,tp,env);
				Type it = init.getType(env);
				if( !it.isInstanceOf(tp) ) {
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

