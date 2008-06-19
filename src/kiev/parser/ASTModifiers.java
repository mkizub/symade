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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode(lang=void)
public final class ASTModifiers extends ASTNode {
	
	@nodeData public ANode∅				annotations;

	public ASTModifiers() {}
	
	public UserMeta add(UserMeta m)
		alias lfy operator +=
	{
		this.annotations += m;
		return m;
	}
	public MetaFlag add(MetaFlag m)
		alias lfy operator +=
	{
		this.annotations += m;
		return m;
	}
	
	public void moveToNode(DNode dn) {
		ANode[] annotations = this.annotations.delToArray();
		foreach (MNode m; annotations)
			dn.setMeta(m);
	}
	public void copyToNode(DNode dn) {
		ANode[] annotations = this.annotations;
		foreach (MNode m; annotations)
			dn.setMeta(m.ncopy());
	}
	
	public String getUUID() {
		foreach (MetaUUID m; annotations)
			return m.value;
		return null;
	}
}

