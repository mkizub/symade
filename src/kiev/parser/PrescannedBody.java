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

@node
public final class PrescannedBody extends ENode {
	
	public static final int BlockMode			= 0;
	public static final int RuleBlockMode		= 1;
	public static final int CondBlockMode		= 2;
	public static final int RewriteMatchMode	= 3;

	public static final PrescannedBody[] emptyArray = new PrescannedBody[0];

	@virtual typedef This  = PrescannedBody;

	public int		lineno;
	public int		columnno;
	public int		mode;
	@ref
	public ASTNode	expected_parent;

	public PrescannedBody() {}
	
	public PrescannedBody(ASTNode expected_parent, int lineno, int columnno) {
		this.expected_parent = expected_parent;
		this.lineno = lineno;	
		this.columnno = columnno;
	}
}

