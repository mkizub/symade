/* Generated By:JJTree: Do not edit this line. ASTFormalParameter.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTFormalParameter.java,v 1.3.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

public class ASTFormalParameter extends ASTNode {
	public int			dim;
	public ASTModifiers	modifiers;
	public ASTNode		type;
	public ASTNode		mm_type;
    public KString		name;
	public Type			resolved_type;
	public Type			resolved_jtype;

	ASTFormalParameter(int id) {
		super(0);
	}

	public void set(Token t) {
		this.name = new KToken(t).image;
        pos = t.getPos();
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTModifiers) {
			modifiers = (ASTModifiers)n;
		}
        else if( n instanceof ASTType ) {
        	if (type == null)
				type = n;
			else
				mm_type = n;
		}
        else if( n instanceof ASTIdentifier ) {
			name = ((ASTIdentifier)n).name;
            pos = n.getPos();
		}
        else {
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public Var pass3() {
		if( !Kiev.javaMode && name.len == 1 && name.charAt(0)=='_' ) return null;
		// TODO: check flags for fields
		int flags = modifiers.getFlags();
		Type type = ((ASTType)this.type).getType();
		Type mm_type = (this.mm_type == null) ? null : ((ASTType)this.mm_type).getType();
		for(int i=0; i < dim; i++) {
			type = Type.newArrayType(type);
			if (mm_type != null)
				mm_type = Type.newArrayType(mm_type);
		}
//		if( (flags & ACC_PROLOGVAR) != 0 ) {
//			Kiev.reportWarning(pos,"Modifier 'pvar' is deprecated. Replace 'pvar Type' with 'Type@', please");
//			type = Type.newRefType(Type.tpPrologVar.clazz,new Type[]{type});
//			if (mm_type != null)
//				throw new CompilerException(this.mm_type.getPos(),"You can't specify 'actual' type for prolog parameter");
//		}
		resolved_type = type;
		resolved_jtype = (mm_type!=null) ? mm_type : null;
		return new Var(pos,name,type,flags);
	}

    public Dumper toJava(Dumper dmp) {
		dmp.space().append(type);
        for(int i=0; i < dim; i++) dmp.append("[]");
        dmp.space().append(name).space();
        return dmp;
    }
}
