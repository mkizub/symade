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
import kiev.stdlib.*;
import kiev.vlang.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTFileUnit.java,v 1.3.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

public class ASTFileUnit extends ASTNode {

	import kiev.stdlib.Debug;

	public KString	filename;
	public FileUnit	file_unit;
	public static PrescannedBody[] emptyArray = new PrescannedBody[0];
    
    public ASTNode		pkg;
    public ASTNode[]	imports = ASTNode.emptyArray;
    public ASTNode[]	typedefs = ASTNode.emptyArray;
    public ASTNode[]	decls = ASTNode.emptyArray;
	public PrescannedBody[]		bodies = PrescannedBody.emptyArray;
	
	ASTFileUnit(int id) {
		super(0);
	}

	public void setFileName(String fn) {
		filename = KString.from(fn);
	}
	
	public void addPrescannedBody(PrescannedBody b) {
		bodies = (PrescannedBody[])Arrays.append(bodies,b);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if( n instanceof ASTPackage) {
			pkg = n;
		}
        else if( n instanceof ASTImport ) {
			imports = (ASTNode[])Arrays.append(imports,n);
		}
        else if( n instanceof ASTTypedef ) {
			typedefs = (ASTNode[])Arrays.append(typedefs,n);
		}
        else {
			decls = (ASTNode[])Arrays.append(decls,n);
		}
    }

	public ASTNode pass1() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
        try {
			int i = 0;
			if( pkg != null ) {
				pkg = (Struct)((ASTPackage)pkg).pass1();
			} else {
    	    	pkg = Env.root;
        	}
	        Struct[] members = Struct.emptyArray;
			PassInfo.push(pkg);
			try {
				for(i=0; i < decls.length; i++) {
					switch(decls[i]) {
					case ASTTypeDeclaration:
						members = (Struct[])Arrays.append(members,((ASTTypeDeclaration)decls[i]).pass1());
						break;
					case ASTEnumDeclaration:
						members = (Struct[])Arrays.append(members,((ASTEnumDeclaration)decls[i]).pass1());
						break;
					case ASTPackageDeclaration:
						members = (Struct[])Arrays.append(members,((ASTPackageDeclaration)decls[i]).pass1());
						break;
					default:
						throw new CompilerException(decls[i].pos,"Unknown type of file declaration "+decls[i].getClass());
					}
				}
			} finally { PassInfo.pop(pkg); }
			file_unit = new FileUnit(filename,(Struct)pkg,members);
			file_unit.bodies = bodies;
			return file_unit;
		} finally { Kiev.curFile = oldfn; }
	}

	public ASTNode pass2() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
		PassInfo.push(file_unit);
        try {
			// Process file imports...
			boolean java_lang_found = false;
			KString java_lang_name = KString.from("java.lang");
			boolean kiev_stdlib_found = false;
			KString kiev_stdlib_name = KString.from("kiev.stdlib");
			Struct[] imps = Struct.emptyArray;

			for(int i=0; i < imports.length; i++) {
				try {
					imps = (Struct[])Arrays.append(imps,
						((ASTImport)imports[i]).pass2());
					if( imps[imps.length-1].name.name.equals(java_lang_name))
						java_lang_found = true;
					else if( imps[imps.length-1].name.name.equals(kiev_stdlib_name))
						kiev_stdlib_found = true;
					trace(Kiev.debugResolve,"Add "+imps[imps.length-1]);
				} catch(Exception e ) {
					Kiev.reportError/*Warning*/(imports[i].getPos(),e);
				}
			}
			// Add standard imports, if they were not defined
			if( !Kiev.javaMode && !kiev_stdlib_found )
				imps = (Struct[])Arrays.append(imps,Env.newPackage(kiev_stdlib_name));
			if( !java_lang_found )
				imps = (Struct[])Arrays.append(imps,Env.newPackage(java_lang_name));
		
			file_unit.imports = imps;
			
			Typedef[] tds = Typedef.emptyArray;
			for(int i=0; i < typedefs.length; i++) {
				try {
					tds = (Typedef[])Arrays.append(tds,((ASTTypedef)typedefs[i]).pass2());
					trace(Kiev.debugResolve,"Add "+tds[tds.length-1]);
				} catch(Exception e ) {
					Kiev.reportError/*Warning*/(typedefs[i].getPos(),e);
				}
			}
		
			file_unit.typedefs = tds;
		
			// Process members - pass2()
			for(int j=0; j < decls.length; j++) {
				switch(decls[j]) {
				case ASTTypeDeclaration:
					file_unit.members[j] = (Struct)((ASTTypeDeclaration)decls[j]).pass2();
					break;
				case ASTEnumDeclaration:
					file_unit.members[j] = (Struct)((ASTEnumDeclaration)decls[j]).pass2();
					break;
				case ASTPackageDeclaration:
					file_unit.members[j] = (Struct)((ASTPackageDeclaration)decls[j]).pass2();
					break;
				default:
					throw new CompilerException(decls[j].pos,"Unknown type of file declaration "+decls[j].getClass());
				}
				file_unit.members[j].parent = file_unit;
			}
		} finally { Kiev.curFile = oldfn; PassInfo.pop(file_unit); }
		return file_unit;
	}

	public ASTNode pass2_2() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
        try {
			// Process members - pass2_2()
			PassInfo.push(file_unit);
			try {
				for(int j=0; j < decls.length; j++) {
					switch(decls[j]) {
					case ASTTypeDeclaration:
						((ASTTypeDeclaration)decls[j]).pass2_2();
						break;
					case ASTEnumDeclaration:
						((ASTEnumDeclaration)decls[j]).pass2_2();
						break;
					case ASTPackageDeclaration:
						((ASTPackageDeclaration)decls[j]).pass2_2();
						break;
					default:
						throw new CompilerException(decls[j].pos,"Unknown type of file declaration "+decls[j].getClass());
					}
				}
			} finally { PassInfo.pop(file_unit); }
		} finally { Kiev.curFile = oldfn; }
		return file_unit;
	}


	public FileUnit pass3() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
        try {
			// Process members - pass3()
			PassInfo.push(file_unit);
			try {
				for(int i=0; i < decls.length; i++) {
					switch(decls[i]) {
					case ASTTypeDeclaration:
						file_unit.members[i] = (Struct)((ASTTypeDeclaration)decls[i])
							.pass3(((ASTTypeDeclaration)decls[i]).me,((ASTTypeDeclaration)decls[i]).members);
						break;
					case ASTEnumDeclaration:
						file_unit.members[i] = (Struct)((ASTEnumDeclaration)decls[i])
							.pass3(((ASTEnumDeclaration)decls[i]).me,((ASTEnumDeclaration)decls[i]).members);
						break;
					case ASTPackageDeclaration:
						file_unit.members[i] = (Struct)((ASTPackageDeclaration)decls[i])
							.pass3(((ASTPackageDeclaration)decls[i]).me,((ASTPackageDeclaration)decls[i]).members);
						break;
					default:
						throw new CompilerException(decls[i].pos,"Unknown type of file declaration "+decls[i].getClass());
					}
				}
			} finally { PassInfo.pop(file_unit); }
		} finally { Kiev.curFile = oldfn; }
		return file_unit;
	}
    
	public FileUnit autoProxyMethods() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
        try {
			// Process members - pass3()
			PassInfo.push(file_unit);
			try {
				for(int i=0; i < decls.length; i++) {
					switch(decls[i]) {
					case ASTTypeDeclaration:
						((ASTTypeDeclaration)decls[i]).me.autoProxyMethods();
						break;
					case ASTEnumDeclaration:
						((ASTEnumDeclaration)decls[i]).me.autoProxyMethods();
						break;
					case ASTPackageDeclaration:
						((ASTPackageDeclaration)decls[i]).me.autoProxyMethods();
						break;
					default:
						throw new CompilerException(decls[i].pos,"Unknown type of file declaration "+decls[i].getClass());
					}
				}
			} finally { PassInfo.pop(file_unit); }
		} finally { Kiev.curFile = oldfn; }
		return file_unit;
	}
    
	public void resolveImports() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
        try {
			// Process members - pass3()
			PassInfo.push(file_unit);
			try {
				for(int i=0; i < decls.length; i++) {
					switch(decls[i]) {
					case ASTTypeDeclaration:
						((ASTTypeDeclaration)decls[i]).me.resolveImports();
						break;
					case ASTEnumDeclaration:
						((ASTEnumDeclaration)decls[i]).me.resolveImports();
						break;
					case ASTPackageDeclaration:
						((ASTPackageDeclaration)decls[i]).me.resolveImports();
						break;
					default:
						throw new CompilerException(decls[i].pos,"Unknown type of file declaration "+decls[i].getClass());
					}
				}
			} finally { PassInfo.pop(file_unit); }
		} finally { Kiev.curFile = oldfn; }
	}

	public void resolveFinalFields(boolean cleanup) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
        try {
			// Process members - resolveFinalFields()
			PassInfo.push(file_unit);
			try {
				for(int i=0; i < decls.length; i++) {
					switch(decls[i]) {
					case ASTTypeDeclaration:
						((ASTTypeDeclaration)decls[i]).resolveFinalFields(cleanup);
						break;
					case ASTEnumDeclaration:
						((ASTEnumDeclaration)decls[i]).resolveFinalFields(cleanup);
						break;
					case ASTPackageDeclaration:
						((ASTPackageDeclaration)decls[i]).resolveFinalFields(cleanup);
						break;
					default:
						throw new CompilerException(decls[i].pos,"Unknown type of file declaration "+decls[i].getClass());
					}
				}
			} finally { PassInfo.pop(file_unit); }
		} finally { Kiev.curFile = oldfn; }
	}
    
	public Dumper toJava(Dumper dmp) {
    	return file_unit.toJava(dmp);
	}    
}
