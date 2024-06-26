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
 * @version $Revision: 213 $
 *
 */

public final class Ref<A>
{

	@forward public A			$val;

	public Ref() {}

	public Ref(A value) {
		this.$val = value;
	}

	public String toString() {
		return String.valueOf($val);
	}

	public boolean equals(A value) {
		A r = $val;
		return (r==null && value==null) || r.equals(value);
	}
}
