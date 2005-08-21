/* Generated By:JJTree: Do not edit this line. ASTClosureType.java */

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
 * @author Maxim Kizub
 *
 */

@cfnode
@node
public class TypeClosureRef extends TypeRef {
    @att public final NArr<TypeRef>	types;

	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type[] tps = new Type[types.length-1];
        for(int i=0; i < tps.length; i++) {
			tps[i] = types[i].getType();
		}
        Type ret = types[types.length-1].getType();
        this.lnk = ClosureType.newClosureType(Type.tpClosureClazz,tps,ret);
		return this.lnk;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append('(');
		for(int i=0; i < types.length-1; i++) {
			dmp.append(types[i]);
			if( i < types.length-2) dmp.append(',').space();
		}
		dmp.append(")->").append(types[types.length-1]).space();
		return dmp;
	}
}
