/* Generated By:JJTree: Do not edit this line. ASTInvariantDeclaration.java */

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

import static kiev.stdlib.Debug.*;
import static kiev.vlang.WorkByContractCondition.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTInvariantDeclaration.java,v 1.3.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

@node
public class ASTInvariantDeclaration extends ASTCondDeclaration {
	@att public ASTModifiers	modifiers;
	public KString				name;

	public ASTInvariantDeclaration(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTModifiers) {
			modifiers = (ASTModifiers)n;
		}
    		else if( n instanceof ASTIdentifier ) {
			name = ((ASTIdentifier)n).name;
			pos = n.getPos();
		}
		else if( n instanceof ASTBlock ) {
			body = (Statement)n;
			if( pos == 0 ) pos = n.pos;
			if( name == null ) name = KString.from("inv$"+Integer.toHexString(this.hashCode()));
        }
        else {
        	throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

    public ASTNode pass3() {
		// TODO: check flags for fields
		int flags = modifiers.getFlags();
		MethodType mt = MethodType.newMethodType(null,null,Type.emptyArray,Type.tpVoid);
		Method m = new Method(PassInfo.clazz,name,mt,flags);
		m.setInvariantMethod(true);
		if( !m.isStatic() ) {
			m.params = new Var[]{new Var(pos,m,nameThis,PassInfo.clazz.type,0)};
		} else {
			m.params = Var.emptyArray;
		}
		WorkByContractCondition cond = new WorkByContractCondition(pos,CondInvariant,name,body);
		if( pbody != null ) pbody.setParent(cond);
		m.body = cond;
		cond.parent = m;
		PassInfo.clazz.addMethod(m);
	  	return m;
    }
}

