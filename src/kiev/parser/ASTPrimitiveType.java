/* Generated By:JJTree: Do not edit this line. ASTPrimitiveType.java */

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
import kiev.stdlib.*;
import kiev.vlang.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTPrimitiveType.java,v 1.3.4.2 1999/02/17 21:38:28 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.2 $
 *
 */

@node
public class ASTPrimitiveType extends ASTType implements kiev020Constants {

	public void set(Token t) {
    	pos = t.getPos();
  		switch(t.kind) {
  		case BOOLEAN: this.lnk = Type.tpBoolean; return;
  		case BYTE:    this.lnk = Type.tpByte; return;
  		case CHAR:    this.lnk = Type.tpChar; return;
  		case SHORT:   this.lnk = Type.tpShort; return;
  		case INT:     this.lnk = Type.tpInt; return;
  		case LONG:    this.lnk = Type.tpLong; return;
  		case FLOAT:   this.lnk = Type.tpFloat; return;
  		case DOUBLE:  this.lnk = Type.tpDouble; return;
  		case VOID:    this.lnk = Type.tpVoid; return;
  		case RULE:    this.lnk = Type.tpRule; return;
  		default: throw new CompilerException(pos,"Unknown primitive type "+t.image);
		}
	}

	public boolean isBound() {
		return true;
	}

	public Type getType() {
		return this.lnk;
	}
	
	public String toString() {
		return this.lnk.toString();
	}
	
    public Dumper toJava(Dumper dmp) {
    	return dmp.append(this.lnk);
    }
}

