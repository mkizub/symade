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

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Import.java,v 1.5.2.1.2.1 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1.2.1 $
 *
 */

public class Import extends ASTNode implements Constants, Scope {
	public static final Import[] emptyArray = new Import[0];

	public static final int	IMPORT_CLASS   = 0;
	public static final int	IMPORT_STATIC  = 1;
	public static final int	IMPORT_PACKAGE = 2;
	public static final int	IMPORT_SYNTAX  = 3;

	public int			mode = IMPORT_CLASS;
    public boolean		star = false;
    public ASTNode		node;

	public Import(int pos, ASTNode parent, ASTNode node, int mode, boolean star) {
		super(pos, parent);
		this.node = node;
		this.mode = mode;
		this.star = star;
	}

	public String toString() {
		StringBuffer str = new StringBuffer("import ");
		if (mode == IMPORT_STATIC)  str.append("static ");
		if (mode == IMPORT_PACKAGE) str.append("package ");
		if (mode == IMPORT_SYNTAX)  str.append("syntax ");
		if (node instanceof Field)  str.append(node.getType()).append('.');
		str.append(node);
		if (star) str.append(".*");
		return str.toString();
	}

	public ASTNode resolve() throws RuntimeException {
		return this;
	}

	public void generate() {}

	rule public resolveNameR(ASTNode@ node, ResInfo path, KString name, Type tp, int resfl)
		Struct@ s;
		Struct@ sub;
		ASTNode@ tmp;
	{
		this.node instanceof Method, $cut, false
	;
		mode == IMPORT_CLASS && this.node instanceof Struct && !star,
		((Struct)this.node).checkResolved(),
		s ?= ((Struct)this.node),
		!s.isPackage(),
		{
			s.name.name.equals(name), node ?= s.$var
		;	s.name.short_name.equals(name), node ?= s.$var
		}
	;
		mode == IMPORT_CLASS && this.node instanceof Struct && star,
		((Struct)this.node).checkResolved(),
		s ?= ((Struct)this.node),
		{
			!s.isPackage(),
			sub @= s.sub_clazz, !sub.isArgument(),
			{
				sub.name.name.equals(name), node ?= sub.$var
			;	sub.name.short_name.equals(name), node ?= sub.$var
			}
		;	s.isPackage(), s.resolveNameR(node,path,name,tp,resfl)
		}
	;
		mode == IMPORT_STATIC && star && this.node instanceof Struct,
		((Struct)this.node).checkResolved(),
		((Struct)this.node).resolveNameR(node,path,name,tp,resfl|ResolveFlags.NoForwards|ResolveFlags.NoImports|ResolveFlags.Static),
		node instanceof Field && node.isStatic() && node.isPublic()
	;
		mode == IMPORT_SYNTAX,
		((Struct)this.node).checkResolved(),
		tmp @= ((Struct)this.node).imported,
		{
			tmp instanceof Field,
			trace(Kiev.debugResolve,"Syntax check field "+tmp+" == "+name),
			((Field)tmp).name.equals(name),
			node ?= tmp
		;	tmp instanceof Typedef,
			trace(Kiev.debugResolve,"Syntax check typedef "+tmp+" == "+name),
			((Typedef)tmp).name.equals(name),
			node ?= ((Typedef)tmp).type
		//;	trace(Kiev.debugResolve,"Syntax check "+tmp.getClass()+" "+tmp+" == "+name), false
		}
	}

	rule public resolveMethodR(ASTNode@ node, ResInfo path, KString name, Expr[] args, Type ret, Type type, int resfl)
	{
		mode == IMPORT_STATIC && !star && this.node instanceof Method,
		((Method)this.node).equalsByCast(name,args,ret,type,resfl),
		node ?= ((Method)this.node)
	;
		mode == IMPORT_STATIC && star && this.node instanceof Struct,
		((Struct)this.node).checkResolved(),
		((Struct)this.node).resolveMethodR(node,path,name,args,ret,type,resfl|ResolveFlags.NoForwards|ResolveFlags.NoImports|ResolveFlags.Static),
		node instanceof Method && node.isStatic() && node.isPublic()
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

