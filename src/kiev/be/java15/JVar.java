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
package kiev.be.java15;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ViewOf(vcast=true, iface=true)
public view JVar of Var extends JDNode {
	
	public:ro	Type			vtype;
	public:ro	JType			jtype;
	public:ro	JENode			init;
	public:ro	int				kind;

	public final boolean isForward();
	public final boolean isInitWrapper();
	public final boolean isNeedProxy();

	@getter public final Type get$type() {
		if (((Var)this).vtype == null)
			return Type.tpVoid;
		return ((Var)this).vtype.getType();
	}
	@getter public final JType get$jtype() {
		return this.get$type().getJType();
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating Var declaration");
		code.setLinePos(this);
		try {
			if( init != null ) {
				init.generate(code,this.type);
				code.addVar(this);
				code.addInstr(Instr.op_store,this);
			} else {
				code.addVar(this);
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public void removeVar(Code code) {
		code.removeVar(this);
	}
}

