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
package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.stdlib.*;

import syntax kiev.Syntax;

@node
public final class ASTPragma extends DNode {

	@virtual typedef This  = ASTPragma;

	@att public boolean					enable;
	@att public ConstStringExpr[]		options;

	public ASTPragma() { super(new Symbol<This>()); }
	
	public void resolve(Type reqType) {}
}
