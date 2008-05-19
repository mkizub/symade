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

public abstract class BackendProcessor implements Constants {
	private KievBackend backend;
	
	BackendProcessor(KievBackend backend) {
		this.backend = backend;
	}
	public boolean isEnabled() {
		return this.backend == KievBackend.Generic || this.backend == Kiev.useBackend;
	}

	public abstract String getDescr();
	public abstract void process(ASTNode node, Transaction tr);

	public static String getPropS(String base, String name, String dflt) {
		String fname = base+"."+name;
		String val = System.getProperty(fname,dflt);
		if (val != null)
			val = val.trim().intern();
		trace(Kiev.debug, "Loaded "+fname+" as \""+val+"\"");
		return val;
	}
}

