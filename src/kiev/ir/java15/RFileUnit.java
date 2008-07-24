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
package kiev.ir.java15;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ViewOf(vcast=true, iface=true)
public static final view RFileUnit of FileUnit extends RNameSpace {
	public:ro	String					fname;
	public:ro	boolean[]				disabled_extensions;
	public		boolean					scanned_for_interface_only;

	public String pname();

	public void resolveDecl() {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving file "+fname);
		String curr_file = Kiev.getCurFile();
		Kiev.setCurFile(pname());
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach (ASTNode n; members) {
				try {
					if (n instanceof DNode && !n.isMacro())
						n.resolveDecl();
					else if (n instanceof SNode)
						n.resolveDecl();
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.setCurFile(curr_file); Kiev.setExtSet(exts); }
	}
}

@ViewOf(vcast=true, iface=true)
public static view RNameSpace of NameSpace extends RSNode {
	public		SymbolRef<KievPackage>	srpkg;
	public:ro	ASTNode[]				members;

	public void resolveDecl() {
		foreach (ASTNode n; members) {
			try {
				if (n instanceof DNode && !n.isMacro())
					n.resolveDecl();
				else if (n instanceof SNode)
					n.resolveDecl();
			} catch(Exception e) {
				Kiev.reportError(n,e);
			}
		}
	}
}

