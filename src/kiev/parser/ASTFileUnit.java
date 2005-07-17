/* Generated By:JJTree: Do not edit this line. ASTFileUnit.java */

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
import kiev.Kiev.Ext;
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

@node
public class ASTFileUnit extends ASTNode implements TopLevelDecl {
	public KString	filename;
	@ref public FileUnit	file_unit;
	@ref public Struct		file_pkg;
	public static PrescannedBody[] emptyArray = new PrescannedBody[0];

    @att public ASTPackage				pkg;
    @att public final NArr<ASTNode>	syntax;
    @att public final NArr<ASTNode>	decls;
	public PrescannedBody[]	bodies = PrescannedBody.emptyArray;
	
	public boolean[]		disabled_extensions;

	ASTFileUnit(int id) {
		super(0);
		disabled_extensions = Kiev.getCmdLineExtSet();
		syntax = new NArr<ASTNode>(this);
		decls = new NArr<ASTNode>(this);
	}

	public void setFileName(String fn) {
		filename = KString.from(fn);
	}

	public void addPrescannedBody(PrescannedBody b) {
		bodies = (PrescannedBody[])Arrays.append(bodies,b);
	}

	public void jjtAddChild(ASTNode n, int i) {
		n.parent = this;
		if( n instanceof ASTPackage) {
			pkg = (ASTPackage)n;
		}
		else if( n instanceof ASTImport || n instanceof ASTTypedef || n instanceof ASTOpdef || n instanceof ASTPragma) {
			syntax.append(n);
			// Check disabled extensions very early
			if (n instanceof ASTPragma) {
				foreach (ASTConstExpression e; ((ASTPragma)n).options)
					setExtension(e.pos,((ASTPragma)n).enable,((KString)e.val).toString());
			}
		}
		else {
			decls.append(n);
		}
    }

	private void setExtension(int pos, boolean enabled, String s) {
		Ext ext;
		try {
			ext = Ext.fromString(s);
		} catch(RuntimeException e) {
			Kiev.reportWarning(pos,"Unknown pragma '"+s+"'");
			return;
		}
		int i = ((int)ext)-1;
		if (enabled && Kiev.getCmdLineExtSet()[i])
			Kiev.reportError(pos,"Extension '"+s+"' was disabled from command line");
		disabled_extensions[i] = !enabled;
	}

	public ASTNode pass3() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
		PassInfo.push(file_unit);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			// Process members - pass3()
			for(int i=0; i < decls.length; i++) {
				if (decls[i] instanceof ASTStructDeclaration)
					file_unit.members[i] = (Struct)decls[i].pass3();
				else
					throw new CompilerException(decls[i].pos,"Unknown type of file declaration "+decls[i].getClass());
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(file_unit); Kiev.curFile = oldfn; }
		return file_unit;
	}

	public ASTNode autoProxyMethods() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
		PassInfo.push(file_unit);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			// Process members - pass3()
			for(int i=0; i < decls.length; i++) {
				decls[i].autoProxyMethods();
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(file_unit); Kiev.curFile = oldfn; }
		return file_unit;
	}

	public ASTNode resolveImports() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
		PassInfo.push(file_unit);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			// Process members - pass3()
			//for(int i=0; i < syntax.length; i++) {
			//	if (syntax[i] != null)
			//		continue; // processed at pass2
			//	try {
			//		ASTImport n = (ASTImport)syntax[i];
			//		Debug.assert(n.mode == ASTImport.IMPORT_STATIC && !n.star);
			//		file_unit.syntax[i] = n.pass2(file_unit);
			//		trace(Kiev.debugResolve,"Add "+file_unit.syntax[i]);
			//	} catch(Exception e ) {
			//		Kiev.reportError/*Warning*/(syntax[i].getPos(),e);
			//	}
			//}
			for(int i=0; i < decls.length; i++) {
				try {
					decls[i].resolveImports();
				} catch(Exception e ) {
					Kiev.reportError/*Warning*/(((ASTNode)decls[i]).getPos(),e);
				}
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(file_unit); Kiev.curFile = oldfn; }
		return file_unit;
	}

	public ASTNode resolveFinalFields(boolean cleanup) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
		PassInfo.push(file_unit);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			// Process members - resolveFinalFields()
			for(int i=0; i < decls.length; i++) {
				decls[i].resolveFinalFields(cleanup);
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(file_unit); Kiev.curFile = oldfn; }
		return file_unit;
	}

	public Dumper toJava(Dumper dmp) {
    	return file_unit.toJava(dmp);
	}
}
