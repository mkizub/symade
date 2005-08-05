/* Generated By:JJTree: Do not edit this line. ASTAccess.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/Attic/ASTAccess.java,v 1.1.2.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.1.2.1 $
 *
 */

@node
public class ASTAccess extends SimpleNode {

	public static final int ACC_PRIVATE_R	=  0x1;
	public static final int ACC_PRIVATE_W	=  0x2;
	public static final int ACC_DEFAULT_R	=  0x4;
	public static final int ACC_DEFAULT_W	=  0x8;
	public static final int ACC_PROTECTED_R	= 0x10;
	public static final int ACC_PROTECTED_W	= 0x20;
	public static final int ACC_PUBLIC_R	= 0x40;
	public static final int ACC_PUBLIC_W	= 0x80;

	public int		accflags;
	private int		offset = 6;

	public void jjtAddChild(ASTNode n, int i) {
       	throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
    }

	public void set(int acc)
		throws ParseException
	{
		if( offset < 0 ) throw new ParseException("More then 4 access values");
		for(int i=offset; i >= 0; i-=2) {
			accflags &= ~(3 << i);
			accflags |= acc << i;
		}
		offset -= 2;
	}
}
