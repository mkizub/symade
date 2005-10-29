/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.
 
 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/
  
package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 182 $
 *
 */

@node
public class PrescannedBody extends ASTNode {
	
	public static final int BlockMode		= 0;
	public static final int RuleBlockMode	= 1;
	public static final int CondBlockMode	= 2;

	public static PrescannedBody[] emptyArray = new PrescannedBody[0];

	public int			lineno;	
	public int			columnno;
	public int			mode;
	
	public PrescannedBody() {}
	
	public PrescannedBody(int lineno, int columnno) {
		this.lineno = lineno;	
		this.columnno = columnno;
	}
}

