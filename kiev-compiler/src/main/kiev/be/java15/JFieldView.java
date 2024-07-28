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
 * @version $Revision: 213 $
 *
 */

public class JField extends JVar {

	@virtual typedef VT  ≤ Field;

	private final boolean is_const;
	private final Object  obj_const;

	public static JField attachJField(Field impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JField)jn;
		return new JField(impl);
	}
	
	public JField(Field impl) {
		super(impl);
		if (vn().isConstantExpr(Env.getEnv())) {
			obj_const = vn().getConstValue(Env.getEnv());
			is_const = true;
		}
	}

	public void backendCleanup() {
		jattrs = Attr.emptyArray;
	}

	public final boolean isVirtual() { vn().isVirtual() }

	public boolean	isConstantExpr(Env env) {
		return is_const;
	}
	public Object	getConstValue(Env env) {
		if (is_const)
			return obj_const;
    	throw new RuntimeException("Request for constant value of non-constant expression");
	}
}

