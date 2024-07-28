/*******************************************************************************
 * Copyright (c) 2005-2008 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *     Roman Chepelyev (gromanc@gmail.com) - implementation and refactoring
 *******************************************************************************/
package kiev.gui.event;

import java.io.ObjectStreamException;

/**
 * The Binding Set.
 */
public class BindingSet extends Item {
	/**
	 * The serial.
	 */
	private static final long serialVersionUID = -6693666203094437784L;

	/**
	 * The parent.
	 */
	public BindingSet parent_set;
	
	/**
	 * The items;
	 */
	public Item[] items;
	
	/**
	 * The qualified name.
	 */
	public String qname; 

	
	/**
	 * Resolve.
	 * @return Object
	 * @throws ObjectStreamException
	 */
	Object readResolve() throws ObjectStreamException {
		if (this.qname != null) this.qname = this.qname.intern();
		this.init();
		return this;
	}

	/**
	 * Initialize.
	 * @return BindingSet
	 */
	public BindingSet init() {
		return this;
	}
}
