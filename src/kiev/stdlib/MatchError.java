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

/** a class for errors due to missing patterns
 * @author   Martin Odersky
 * @author Maxim Kizub
 * @version $Revision: 703 $
 *
 */
public class MatchError extends Error {

    public MatchError() {
	super();
    }

    public MatchError(String s) {
	super(s);
    }
} 

