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

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.tree.*;

import java.io.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Struct.java,v 1.6.2.1.2.5 1999/02/17 22:17:40 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.5 $
 *
 */

public class Grammar {

	public String				lexer;

	public Vector<TokenDecl>	tokens = new Vector<TokenDecl>();

}

public class TokenDecl extends Node {

	public KString		name;
	public KString		value;

	public TokenDecl(int pos, KString name, KString value) {
		super(pos);
		this.name = name;
		this.value = value;
	}
	
	public TokenDecl(int pos, KString name) {
		this(pos,name,null);
	}
}
