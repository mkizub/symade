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

@ViewOf(vcast=true)
public static view RSyntaxScope of SyntaxScope extends RSNode {
	public:ro	SymbolRef<KievPackage>	srpkg;
	public:ro	ASTNode[]				members;
}

@ViewOf(vcast=true)
public static final view RFileUnit of FileUnit extends RSyntaxScope {
	public:ro	String					fname;
	public		boolean					scanned_for_interface_only;

	public String pname();

	public void resolveDecl(Env env) {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving file "+fname);
		String curr_file = Kiev.getCurFile();
		Kiev.setCurFile(pname());
        try {
			foreach (ASTNode n; members) {
				try {
					if (n instanceof DNode && !n.isMacro())
						resolveDNode(n,env);
					else if (n instanceof SNode)
						resolveSNode(n,env);
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.setCurFile(curr_file); }
	}
}

@ViewOf(vcast=true)
public static view RNameSpace of NameSpace extends RSyntaxScope {
	public void resolveDecl(Env env) {
		foreach (ASTNode n; members) {
			try {
				if (n instanceof DNode && !n.isMacro())
					resolveDNode(n,env);
				else if (n instanceof SNode)
					resolveSNode(n,env);
			} catch(Exception e) {
				Kiev.reportError(n,e);
			}
		}
	}
}

