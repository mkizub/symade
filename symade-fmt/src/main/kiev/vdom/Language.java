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
package kiev.vdom;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsALanguage(name="vdom")
@singleton
public final class XMLLang extends LangBase {
	{
		this.defaultEditorSyntaxName = "stx-fmt·syntax-for-xml";
		this.defaultInfoSyntaxName = "stx-fmt·syntax-for-xml";
	}
	public String getName() { return "vdom"; }
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }

	private static Class[] superLanguages = {};
	private static Class[] nodeClasses = {
		XMLQName.class,
		XMLNode.class,
			XMLElement.class,
			XMLAttribute.class,
			XMLText.class
	};
}

