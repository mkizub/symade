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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public static final view RField of Field extends RVar {
	public		ConstExpr		const_value;
	
	// packer field (auto-generated for packed fields)
	public final boolean isPackerField();
	public final void setPackerField(boolean on);
	// packed field
	public final boolean isPackedField();
	public final void setPackedField(boolean on);
	// field's initializer was already added to class initializer
	public final boolean isAddedToInit();
	public final void setAddedToInit(boolean on);

	public void resolveDecl() {
		meta.resolve();
		Type tp = this.type;
		if (init instanceof TypeRef)
			((TypeRef)init).toExpr(type);
		if (tp instanceof CTimeType) {
			this.open();
			init = tp.makeInitExpr(this,init);
			try {
				Kiev.runProcessorsOn(init);
				init.resolve(tp.getEnclosedType());
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		else if( init != null ) {
			try {
				init.resolve(tp);
				Type it = init.getType();
				if( !it.isInstanceOf(tp) ) {
					this.open();
					init = new CastExpr(init.pos,tp,~init);
					init.resolve(tp);
				}
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		setResolved(true);
	}
}

