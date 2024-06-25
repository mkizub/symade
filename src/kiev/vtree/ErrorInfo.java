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
package kiev.vtree;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 296 $
 *
 */

@ThisIsANode(copyable=false)
public abstract class ErrorInfo extends ANode {
	@nodeData public SeverError sever;
	@nodeData public String message;
	@nodeData public String text;
	
	protected ErrorInfo(SeverError sever, String message, String text) {
		super(new AHandle(), Context.DEFAULT);
		this.sever = sever;
		this.message = message;
		this.text = text;
	}
}

@ThisIsANode(copyable=false)
public class ErrorNodeInfo extends ErrorInfo {
	@nodeData public ANode node;
	
	public ErrorNodeInfo(SeverError sever, String message, ANode node) {
		super(sever, message, null);
		this.node = node;
	}
}

@ThisIsANode(copyable=false)
public class ErrorTextInfo extends ErrorInfo {
	@nodeData public String file;
	@nodeData public int lineno;
	
	public ErrorTextInfo(SeverError sever, String message, String file, int lineno) {
		super(sever, message, null);
		this.file = file;
		this.lineno = lineno;
	}
}

