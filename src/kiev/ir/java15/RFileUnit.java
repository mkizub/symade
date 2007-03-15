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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public static final view RFileUnit of FileUnit extends RSNode {
	public		String					name;
	public		TypeNameRef				pkg;
	public:ro	ASTNode[]				members;
	public:ro	boolean[]				disabled_extensions;
	public		boolean					scanned_for_interface_only;

	public void resolveDecl() {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving file "+name);
		String curr_file = Kiev.getCurFile();
		Kiev.setCurFile(name);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach (DNode n; members; !n.isMacro()) {
				try {
					n.resolveDecl();
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.setCurFile(curr_file); Kiev.setExtSet(exts); }
	}
}

