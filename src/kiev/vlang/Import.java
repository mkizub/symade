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

package kiev.vlang;

import kiev.Kiev;
import kiev.parser.PrescannedBody;
import kiev.stdlib.*;
import java.io.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Import.java,v 1.5.2.1.2.1 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1.2.1 $
 *
 */

public class Import extends ASTNode implements Constants, ScopeOfNames, ScopeOfMethods {

	import kiev.stdlib.Debug;

	public static final Import[] emptyArray = new Import[0];

	public static final int	IMPORT_CLASS   = 0;
	public static final int	IMPORT_STATIC  = 1;
	public static final int	IMPORT_PACKAGE = 2;
	public static final int	IMPORT_SYNTAX  = 3;

	public int			mode = IMPORT_CLASS;
    public boolean		star = false;
    public ASTNode		node;

	public Import(int pos, FileUnit fu, ASTNode node, int mode, boolean star) {
		super(pos, fu);
		this.node = node;
		this.mode = mode;
		this.star = star;
	}

	public String toString() {
		StringBuffer str = new StringBuffer("import ");
		if (mode == IMPORT_STATIC)  str.append("static ");
		if (mode == IMPORT_PACKAGE) str.append("package ");
		if (mode == IMPORT_SYNTAX)  str.append("syntax ");
		str.append(node);
		if (star) str.append(".*");
		return str.toString();
	}

	public ASTNode resolve() throws RuntimeException {
		return this;
	}

	public void generate() {}

	rule public resolveNameR(pvar ASTNode node, pvar List<ASTNode> path, KString name, Type tp, int resfl)
		pvar Struct s;
		pvar Struct sub;
	{
		this.node instanceof Method, $cut, false
	;
		mode == IMPORT_CLASS && this.node instanceof Struct && !star,
		((Struct)this.node).checkResolved(),
		s ?= ((Struct)this.node),
		!s.$var.isPackage(),
		{
			s.$var.name.name.equals(name), node ?= s.$var
		;	s.$var.name.short_name.equals(name), node ?= s.$var
		}
	;
		mode == IMPORT_CLASS && this.node instanceof Struct && star,
		((Struct)this.node).checkResolved(),
		s ?= ((Struct)this.node),
		{
			!s.$var.isPackage(),
			sub @= s.$var.sub_clazz, !sub.$var.isArgument(),
			{
				sub.$var.name.name.equals(name), node ?= sub.$var
			;	sub.$var.name.short_name.equals(name), node ?= sub.$var
			}
		;	s.$var.isPackage(), s.$var.resolveNameR(node,path,name,tp,resfl)
		}
	}

	rule public resolveMethodR(pvar ASTNode node, pvar List<ASTNode> path, KString name, Expr[] args, Type ret, Type type, int resfl)
	{
		mode == IMPORT_STATIC && this.node instanceof Method,
		((Method)this.node).equals(name,args,ret,type,resfl),
		node ?= ((Method)this.node)
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append(this.toString()).append(";").newLine();
		return dmp;
	}

	public void cleanup() {
		parent=null;
		node = null;
	}

}

