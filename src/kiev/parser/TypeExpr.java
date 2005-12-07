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
public class TypeExpr extends TypeRef {
	private static KString opPVar  = KString.from("@");
	private static KString opRef   = KString.from("&");
	
	@att public TypeRef		arg;
	@att public KString		op;

	public TypeExpr() {
	}

	public TypeExpr(TypeRef arg, KString op) {
		this.pos = arg.pos;
		this.arg = arg;
		this.op = op;
	}

	public TypeExpr(TypeRef arg, Token op) {
		this.arg = arg;
		if (op.kind == ParserConstants.OPERATOR_LRBRACKETS)
			this.op = Constants.nameArrayOp;
		else
			this.op = KString.from(op.image);
		this.pos = op.getPos();
	}

	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
	    Type tp = arg.getType();
		DNode@ v;
		if (op == Constants.nameArrayOp) {
			tp = Type.newArrayType(tp);
		} else {
			Type t;
			if (!PassInfo.resolveNameR(this,v,new ResInfo(this),op)) {
				if (op == opPVar) {
					t = WrapperType.tpWrappedPrologVar;
				}
				else if (op == opRef) {
					Kiev.reportWarning(this, "Typedef for "+op+" not found, assuming wrapper of "+Type.tpRefProxy);
					t = WrapperType.tpWrappedRefProxy;
				}
				else
					throw new CompilerException(this,"Typedef for type operator "+op+" not found");
			} else {
				if (v instanceof TypeDef)
					t = ((TypeDef)v).getType();
				else
					throw new CompilerException(this,"Expected to find type for "+op+", but found "+v);
			}
			if (t.args.length != 1)
				throw new CompilerException(this,"Type '"+t+"' of type operator "+op+" must have 1 argument");
			t.checkResolved();
			if (t.isWrapper()) {
				BaseType ut = Type.newRefType(((WrapperType)t).getUnwrappedType(),new Type[]{tp});
				tp = WrapperType.newWrapperType(ut);
			} else {
				tp = Type.newRefType((BaseType)t,new Type[]{tp});
			}
		}
		this.lnk = tp;
		return tp;
	}

	public String toString() {
		if (this.lnk != null)
			return this.lnk.toString();
		return String.valueOf(arg)+op;
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

