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
 * @version $Revision$
 *
 */

@ThisIsANode(lang=CoreLang)
public class OpdefSymbol extends Symbol {
	OpdefSymbol() {}
	OpdefSymbol(int pos, String name) {
		super(pos, name);
	}
}

public enum OpdefMode {
	// Assign orders
	LFY,
	// Binary orders
	XFX, XFY, YFX, YFY,
	// Prefix orders
	XF, YF,
	// Postfix orders
	FX, FY,
	// Multi operators
	XFXFY, FXFY
}

@ThisIsANode(lang=CoreLang)
public class Opdef extends DNode implements ScopeOfNames {
	

	@nodeAttr public int				prior;
	@nodeAttr public OpdefMode			opmode;
	@nodeAttr public String				image;
	@nodeAttr public boolean			type_operator;
	@AttrXMLDumpInfo(ignore=true)
	@nodeAttr public Operator			resolved;
	@AttrXMLDumpInfo(ignore=true)
	@nodeAttr public Symbol∅			symbols;

	@setter
	public final void set$image(String value) {
		this.image = (value != null) ? value.intern() : null;
	}
	
	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "resolved") {
			symbols.delAll();
			if (this.resolved != null) {
				foreach (OpArg.OPER arg; this.resolved.args) {
					//System.out.println("OpdefSymbol: "+arg.text);
					symbols += new OpdefSymbol(pos, arg.text);
				}
			}
		}
		super.callbackChildChanged(ct, attr, data);
	}

	public Opdef() {}
	
	public String toString() {
		return image;
	}
	
	public rule resolveNameR(ResInfo info)
	{
		info ?= this
	;	info @= symbols
	}
}

