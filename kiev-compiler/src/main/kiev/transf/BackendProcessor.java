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

public abstract class BackendProcessor extends AbstractProcessor implements Constants {
	
	private KievBackend backend;
	
	BackendProcessor(Env env, int id, KievBackend backend) {
		super(env, id);
		this.backend = backend;
	}
	public boolean isEnabled() {
		return this.backend == KievBackend.Generic || this.backend == Kiev.useBackend;
	}
	public boolean isDisabled() {
		return !isEnabled();
	}

}

