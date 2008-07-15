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
package kiev.fmt;
import syntax kiev.Syntax;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

@ThisIsANode(lang=SyntaxLang)
public class TreeSyntax extends ATextSyntax {
	public TreeSyntax() {}

	public Draw_ATextSyntax getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Draw_TreeSyntax();
		fillCompiled(compiled);
		return compiled;
	}
}

