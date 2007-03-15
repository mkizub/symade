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
package kiev.stdlib;

import syntax kiev.stdlib.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 703 $
 *
 */

/** The super class of all enumerations */
public abstract class Enum {
	
	private final String $name;
	private final int $ordinal;
	private final String $text;
	
	protected Enum(String name, int ordinal) {
		this.$name = name.intern();
		this.$ordinal = ordinal;
		this.$text = name;
	}
	
	protected Enum(String name, int ordinal, String text) {
		this.$name = name.intern();
		this.$ordinal = ordinal;
		this.$text = text.intern();
	}
	
	public final String name() {
		return $name;
	}
	
	public final int ordinal() {
		return $ordinal;
	}
	
	public String toString() {
		return $name;
	}

}
