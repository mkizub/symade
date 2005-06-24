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

public class ASTSyntaxDeclaration extends ASTStructDeclaration {

	public ASTSyntaxDeclaration(int id) {}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTIdentifier ) {
			name = ((ASTIdentifier)n).name;
			pos = n.getPos();
		} else {
			members = (ASTNode[])Arrays.append(members,n);
		}
	}

//	public ASTNode pass1(ASTNode pn) {
//		trace(Kiev.debugResolve,"Pass 1 for synax "+name);
//		boolean isTop = (parent != null && parent instanceof ASTFileUnit);
//		ClazzName clname = ClazzName.fromOuterAndName(PassInfo.clazz, name, false, !isTop);
//		me = Env.newStruct(clname,PassInfo.clazz,ACC_PRIVATE|ACC_ABSTRACT|ACC_SYNTAX,true);
//		me.setResolved(true);
//		me.setMembersGenerated(true);
//		me.setStatementsGenerated(true);
//
//		if( parent instanceof ASTFileUnit || parent instanceof ASTTypeDeclaration ) {
//			Env.setProjectInfo(me.name,((ASTFileUnit)Kiev.k.getJJTree().rootNode()).filename);
//		}
//		SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
//		me.addAttr(sfa);
//
//		PassInfo.push(me);
//		try {
//			/* Generate type for this structure */
//			me.type = Type.newJavaRefType(me);
//		} finally { PassInfo.pop(me); }
//
//		return me;
//	}

//	public ASTNode pass1_1(ASTNode pn) {
//		trace(Kiev.debugResolve,"Pass 1_1 for syntax "+me);
//     	me.imported = new ASTNode[members.length];
//		for(int i=0; i < members.length; i++) {
//			ASTNode n = members[i];
//			try {
//				if (n instanceof ASTTypedef) {
//					n = n.pass1_1(pn);
//					n.parent = me;
//					me.imported[i] = n;
//					trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
//				}
//				else if (n instanceof ASTOpdef) {
//					n = n.pass1_1(pn);
//					me.imported[i] = n;
//					trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
//				}
//			} catch(Exception e ) {
//				Kiev.reportError/*Warning*/(n.getPos(),e);
//			}
//		}
//		return me;
//	}

//	public ASTNode pass2_2(ASTNode pn) {
//		trace(Kiev.debugResolve,"Pass 2_2 for syntax "+me);
//		Kiev.packages_scanned.append(me);
//		return me;
//	}

	public static Struct pass3(Struct me, ASTNode[] members) {
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

