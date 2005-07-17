/* Generated By:JJTree: Do not edit this line. ASTSyntaxDeclaration.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTSyntaxDeclaration.java,v 1.3.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

@node
public class ASTSyntaxDeclaration extends ASTStructDeclaration {

	public ASTSyntaxDeclaration(int id) {}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTModifiers) {
			modifiers = (ASTModifiers)n;
		}
		else if( n instanceof ASTIdentifier ) {
			name = ((ASTIdentifier)n).name;
			pos = n.getPos();
		} else {
			members.append(n);
		}
	}

	public static Struct createMembers(Struct me, NArr<ASTNode> members) {
		trace(Kiev.debugResolve,"Pass 3 for syntax "+me);
		if (!Kiev.packages_scanned.contains(me))
			Kiev.packages_scanned.append(me);
		return me;
	}

	public ASTNode autoProxyMethods() {
		me.autoProxyMethods();
		return me;
	}

	public ASTNode resolveImports() {
		me.resolveImports();
		return me;
	}

	public ASTNode resolveFinalFields(boolean cleanup) {
		me.resolveFinalFields(cleanup);
		return me;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append("syntax").space().append(name);
		dmp.space().append('{').newLine(1);
		for(int j=0; j < members.length; j++) dmp.append(members[j]);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

