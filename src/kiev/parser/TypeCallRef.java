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
@dflow(out="this:in")
public class TypeCallRef extends TypeRef {

	@att public final NArr<TypeRef>		args;
	@att public TypeRef					ret;

	public TypeCallRef() {
	}

	public TypeCallRef(MethodType mt) {
		this.ret = new TypeRef(mt.ret);
		foreach (Type a; mt.args)
			this.args += new TypeRef(a);
		this.lnk = mt;
	}

	public boolean isBound() {
		return true;
	}

	public void callbackChildChanged(AttrSlot attr) {
		this.lnk = null;
		if (parent != null && pslot != null) {
			parent.callbackChildChanged(pslot);
		}
	}

	public MethodType getMType() {
		if (this.lnk != null)
			return (MethodType)this.lnk;
		Type rt = ret.getType();
		Type[] atypes = new Type[args.length];
		for(int i=0; i < atypes.length; i++) {
			atypes[i] = args[i].getType();
		}
		this.lnk = MethodType.newMethodType(null,atypes,rt);
		return (MethodType)this.lnk;
	}
	public Type getType() {
		return getMType();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1)
				sb.append(',');
		}
		sb.append(")->").append(ret);
		return sb.toString();
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}
