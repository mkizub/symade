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
 * @version $Revision: 232 $
 *
 */

public class JVar extends JDNode {

	@virtual typedef VT  ≤ Var;

	@abstract
	public			Type			vtype;
	@abstract
	public			JENode			init;
	public final	int				kind;

	public static JVar attachJVar(Var impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JVar)jn;
		if (impl instanceof Field)
			return new JField((Field)impl);
		return new JVar(impl);
	}
	
	protected JVar(Var impl) {
		super(impl);
		this.kind = vn().kind;
	}
	
	public final boolean isNeedProxy() { vn().isNeedProxy() }

	@getter public final Type get$vtype() {
		return vn().vtype.getType(Env.getEnv());
	}

	@getter public final JENode get$init() {
		return (JENode)vn().init;
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating Var declaration");
		code.setLinePos(this);
		try {
			if( init != null ) {
				init.generate(code,this.vtype);
				code.addVar(this);
				code.addInstr(Instr.op_store,this);
			} else {
				code.addVar(this);
			}
		} catch(Exception e ) {
			Kiev.reportError(vn(),e);
		}
	}

	public void removeVar(Code code) {
		code.removeVar(this);
	}
}


