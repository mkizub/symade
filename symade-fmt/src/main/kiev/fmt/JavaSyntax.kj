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

import kiev.fmt.common.*;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;


@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaAccess extends SyntaxElem {
	public SyntaxJavaAccess() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxJavaAccess dr_elem = new Draw_SyntaxJavaAccess(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaPackedField extends SyntaxElem {
	public SyntaxJavaPackedField() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxJavaPackedField dr_elem = new Draw_SyntaxJavaPackedField(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

