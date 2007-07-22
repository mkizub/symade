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

@node(lang=CoreLang)
public class Opdef extends SNode {
	@virtual typedef This  = Opdef;

	// Assign orders
	public static final int LFY			= 0;

	// Binary orders
	public static final int XFX			= 1;
	public static final int XFY			= 2;
	public static final int YFX			= 3;
	public static final int YFY			= 4;

	// Prefix orders
	public static final int XF			= 5;
	public static final int YF			= 6;

	// Postfix orders
	public static final int FX			= 7;
	public static final int FY			= 8;

	// Multi operators
	public static final int XFXFY		= 9;
	public static final int FXFY		= 10;

	// Order/arity strings
	public static final String[]	orderAndArityNames = new String[] {
		"lfy",		// LFY
		"xfx",		// XFX
		"xfy",		// XFY
		"yfx",		// YFX
		"yfy",		// YFY
		"xf",		// XF
		"yf",		// YF
		"fx",		// FX
		"fy",		// FY
		"xfxfy",	// XFXFY
		"fxfy"		// FXFY
	};
	
	@att public int					prior;
	@att public int					opmode;
	@att public String				image;
	@att public String				decl;
	@ref public Operator			resolved;

	@setter
	public void set$image(String value) {
		this.image = (value != null) ? value.intern() : null;
	}
	
	public Opdef() {}
	
	public void setImage(ASTNode n) {
		this.pos = n.pos;
		if( n instanceof EToken ) {
			image = ((EToken)n).ident;
			return;
		}
		else if( n instanceof SymbolRef ) {
			image = ((SymbolRef)n).name;
			return;
		}
		throw new CompilerException(n,"Bad operator definition");
	}
	
	public void setMode(SymbolRef n) {
		opmode = -1;
		String optype = ((SymbolRef)n).name;
		for(int i=0; i < Opdef.orderAndArityNames.length; i++) {
			if( Opdef.orderAndArityNames[i].equals(optype) ) {
				opmode = i;
				break;
			}
		}
		if( opmode < 0 )
			throw new CompilerException(n,"Operator mode must be one of "+Arrays.toString(Opdef.orderAndArityNames));
		return;
	}
	
	public void setPriority(ConstIntExpr n) {
		prior = n.value;
		if( prior < 0 || prior > 255 )
			throw new CompilerException(n,"Operator priority must have value from 0 to 255");
		pos = n.pos;
		return;
	}
	
	public String toString() {
		return image;
	}
	
	public boolean hasName(String name, boolean by_equals) {
		if (resolved == null) return false;
		foreach (OpArg.OPER arg; resolved.args; arg.text == name)
			return true;
		return false;
	}
}

