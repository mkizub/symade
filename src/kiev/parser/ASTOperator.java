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

/**
 * @author Maxim Kizub
 * @version $Revision: 703 $
 *
 */

@node(name="Op")
public final class ASTOperator extends ENode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = ASTOperator;

	ASTOperator() {}
	ASTOperator(Token t) {
		this.pos = t.getPos();
		this.ident = t.image;
	}
	
	public void resolve(Type reqType) {
		throw new RuntimeException();
	}

	public String toString() {
		return this.ident;
	}
}

