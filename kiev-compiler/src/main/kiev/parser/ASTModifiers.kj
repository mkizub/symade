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

@unerasable
public final class ASTModifiers extends Vector<MNode> {
	
	public ASTModifiers() {}
	
	public MNode add(MNode m)
		operator "V += V"
	{
		super.append(m);
		return m;
	}
	
	public void moveToNode(DNode dn) {
		foreach (MNode m; this)
			dn.setMeta(m);
	}
	public void copyToNode(DNode dn) {
		foreach (MNode m; this)
			dn.setMeta(new Copier().copyFull(m));
	}
	
	public String getUUID() {
		foreach (MetaUUID m; this)
			return m.value;
		return null;
	}
	
	public boolean isGetter() {
		foreach (MetaGetter m; this)
			return true;
		return false;
	}
	public boolean isSetter() {
		foreach (MetaSetter m; this)
			return true;
		return false;
	}
	public boolean isSymadeNode() {
		foreach (UserMeta m; this; m.decl.name == "ThisIsANode" || m.decl.name == "kiev·vtree·ThisIsANode")
			return true;
		return false;
	}
}

