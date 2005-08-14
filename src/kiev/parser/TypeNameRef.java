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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeNameRef extends TypeRef {
	@att public ASTIdentifier			name;

	public TypeNameRef() {
	}

	public TypeNameRef(KString nm) {
		name = new ASTIdentifier(nm);
	}

	public TypeNameRef(ASTIdentifier nm) {
		this.pos = nm.getPos();
		this.name = nm;
	}

	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		KString nm = name.name;
		ASTNode@ v;
		if( !PassInfo.resolveNameR(v,new ResInfo(),nm) )
			throw new CompilerException(pos,"Unresolved identifier "+nm);
		if( v instanceof TypeRef ) {
			this.lnk = ((TypeRef)v).getType();
		} else {
			if( !(v instanceof BaseStruct) )
				throw new CompilerException(pos,"Type name "+nm+" is not a structure, but "+v);
			BaseStruct bs = (BaseStruct)v;
			bs.checkResolved();
			this.lnk = bs.type;
		}
		return this.lnk;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

