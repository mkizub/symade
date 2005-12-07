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
import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

@node
@dflow(out="this:in")
public class TypeNameRef extends TypeRef {
	@att public NameRef			name;

	public TypeNameRef() {
	}

	public TypeNameRef(KString nm) {
		name = new NameRef(nm);
	}

	public TypeNameRef(NameRef nm) {
		this.pos = nm.getPos();
		this.name = nm;
	}

	public TypeNameRef(NameRef nm, Type tp) {
		this.pos = nm.getPos();
		this.name = nm;
		this.lnk = tp;
	}


	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		KString nm = name.name;
		DNode@ v;
		if( !PassInfo.resolveQualifiedNameR(this,v,new ResInfo(this,ResInfo.noForwards),nm) )
			throw new CompilerException(this,"Unresolved identifier "+nm);
		if( v instanceof TypeDef ) {
			TypeDef td = (TypeDef)v;
			td.checkResolved();
			this.lnk = td.getType();
		}
		if (this.lnk == null)
			throw new CompilerException(this,"Type "+this+" is not found");
		return this.lnk;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

