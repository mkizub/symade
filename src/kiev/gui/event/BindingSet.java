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
package kiev.gui.event;

import java.io.ObjectStreamException;


public class BindingSet extends Item {
	private static final long serialVersionUID = -6693666203094437784L;

	public BindingSet				parent_set;
	public Item[]					items;
	public String					qname;	// qualified name

	
	Object readResolve() throws ObjectStreamException {
		if (this.qname != null) this.qname = this.qname.intern();
		this.init();
		return this;
	}

	public BindingSet init() {
		return this;
	}
}
