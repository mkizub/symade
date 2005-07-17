/* Generated By:JJTree: Do not edit this line. ASTImport.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTImport.java,v 1.4.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.4.1 $
 *
 */

@node
public class ASTImport extends ASTNode implements TopLevelDecl {
	public static final int	IMPORT_CLASS   = 0;
	public static final int	IMPORT_STATIC  = 1;
	public static final int	IMPORT_PACKAGE = 2;
	public static final int	IMPORT_SYNTAX  = 3;
	public KString		name;
	public int			mode = IMPORT_CLASS;
    public boolean		star = false;
    public ASTNode[]	args = null;

	public ASTImport(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTQName ) {
	    	name = ((ASTQName)n).toKString();
    	    pos = n.getPos();
		}
		else if( n instanceof ASTType ) {
			if( args == null ) args = new ASTType[0];
			args = (ASTNode[])Arrays.append(args,n);
		}
    }

//	public ASTNode pass1_1(ASTNode pn) {
//		if (args != null || (mode==IMPORT_STATIC && !star)) return null;
//		PVar<ASTNode> v = new PVar<ASTNode>();
//		if( !PassInfo.resolveNameR(v,new ResInfo(),name,null,0) )
//			throw new CompilerException(pos,"Unresolved identifier "+name);
//		ASTNode n = v;
//		if (mode == IMPORT_CLASS && !(n instanceof Struct))
//			throw new CompilerException(pos,"Identifier "+name+" is not a class or package");
//		else if (mode == IMPORT_PACKAGE && !(n instanceof Struct && ((Struct)n).isPackage()))
//			throw new CompilerException(pos,"Identifier "+name+" is not a package");
//		else if (mode == IMPORT_STATIC && !(star || (n instanceof Field)))
//			throw new CompilerException(pos,"Identifier "+name+" is not a field");
//		else if (mode == IMPORT_SYNTAX && !(n instanceof Struct && n.isSyntax()))
//			throw new CompilerException(pos,"Identifier "+name+" is not a syntax");
//		return new Import(pos,PassInfo.file_unit,n,mode,star);
//	}

	public ASTNode resolveImports() {
		if (args == null || (mode==IMPORT_STATIC && star)) return null;
		PVar<ASTNode> v = new PVar<ASTNode>();
		int i = 0;
		Expr[] exprs;
		if( args.length > 0 && args[0]==Type.tpRule) {
			exprs = new Expr[args.length-1];
			i++;
		} else {
			exprs = new Expr[args.length];
		}
		for(int j=0; j < exprs.length; j++,i++)
			exprs[j] = new VarAccessExpr(0,new Var(0,null,KString.Empty,
				(args[i] instanceof ASTType ? ((ASTType)args[i]).getType() : (kiev.vlang.Type)args[i]),0));
		if( !PassInfo.resolveMethodR(v,null,name,exprs,null,null,0) )
			throw new CompilerException(pos,"Unresolved method "+Method.toString(name,exprs));
		ASTNode n = v;
		if (mode != IMPORT_STATIC || !(n instanceof Method))
			throw new CompilerException(pos,"Identifier "+name+" is not a method");
		return new Import(pos,PassInfo.file_unit,n,IMPORT_STATIC,false);
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append("import").space().append(name);
        if(star)
        	dmp.append(".*");
        return dmp.append(';').newLine();
    }
}
