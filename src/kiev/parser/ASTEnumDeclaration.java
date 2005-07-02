/* Generated By:JJTree: Do not edit this line. ASTEnumDeclaration.java */

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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTEnumDeclaration.java,v 1.3.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

@node
public class ASTEnumDeclaration extends ASTTypeDeclaration {
	
	@att public final NArr<ASTEnumFieldDeclaration> enum_fields;
	
	public ASTEnumDeclaration(int id) {
		super(0);
		enum_fields = new NArr<ASTEnumFieldDeclaration>(this);
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTModifiers) {
			modifiers = (ASTModifiers)n;
		}
        else if( n instanceof ASTIdentifier ) {
			name = ((ASTIdentifier)n).name;
			pos = n.getPos();
		}
        else if( n instanceof ASTExtends ) {
			ext = n;
		}
        else if( n instanceof ASTEnumFieldDeclaration ) {
			enum_fields.append((ASTEnumFieldDeclaration)n);
		}
        else {
			members = (ASTNode[])Arrays.append(members,n);
        }
    }

	public static Struct createMembers(Struct me, NArr<ASTEnumFieldDeclaration> enum_fields, ASTNode[] members) {
		trace(Kiev.debugResolve,"Pass 3 for enum "+me);
        PassInfo.push(me);
        try {
			// Process members
			int next_val = 0;
			foreach (ASTEnumFieldDeclaration efd; enum_fields) {
				efd.parent = me;
				Type me_type = me.type;
				Field f = new Field(me,efd.name.name,me_type,ACC_PUBLIC | ACC_STATIC | ACC_FINAL );
				f.pos = efd.pos;
				f.setEnumField(true);
				f = me.addField(f);
				f.parent = me;
				if (me.isPrimitiveEnum()) {
					if (efd.val != null) {
						if (efd.val.val instanceof Character)
							next_val = ((Character)efd.val.val).charValue();
						else
							next_val = ((Number)efd.val.val).intValue();
					}
					f.init = new ConstExpr(efd.pos,new Integer(next_val));
				} else {
					if (efd.val != null)
						Kiev.reportError(me.pos,"Enum "+me+" is not a primitive enum");
					if (efd.text == null)
						f.init = new NewExpr(f.pos,me.type,new Expr[]{
									new ConstExpr(efd.name.pos,efd.name.name),
									new ConstExpr(efd.pos, new Integer(next_val)),
									new ConstExpr(efd.name.pos,efd.name.name)
						});
					else
						f.init = new NewExpr(f.pos,me.type,new Expr[]{
									new ConstExpr(efd.name.pos,efd.name.name),
									new ConstExpr(efd.pos, new Integer(next_val)),
									new ConstExpr(efd.text.pos, efd.text.val)
						});
				}
				next_val++;
				if (efd.text != null)
					f.name.addAlias(KString.from("\""+efd.text.val+"\""));
				f.init.parent = f;
			}
		} finally { PassInfo.pop(me); }

		ASTTypeDeclaration.createMembers(me, members);
		
		return me;
	}

	public void resolveFinalFields(boolean cleanup) {
		// Resolve final values of class's fields
		me.resolveFinalFields(cleanup);
	}

}
