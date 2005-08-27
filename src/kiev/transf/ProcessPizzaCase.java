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

package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public class ProcessPizzaCase extends TransfProcessor implements Constants {

	public ProcessPizzaCase(Kiev.Ext ext) {
		super(ext);
	}

	public void pass3(ASTNode:ASTNode node) {
	}

	public void pass3(FileUnit:ASTNode fu) {
		foreach (ASTNode n; fu.members)
			pass3(n);
	}

	public void pass3(Struct:ASTNode clazz) {
		if !(clazz.isPizzaCase()) {
			foreach (DNode dn; clazz.members; dn instanceof Struct)
				this.pass3(dn);
			return;
		}
		PizzaCaseAttr case_attr = (PizzaCaseAttr)clazz.getAttr(attrPizzaCase);
		case_attr.casefields = Field.emptyArray;
		foreach (DNode dn; clazz.members; dn instanceof Field) {
			Field f = (Field)dn;
			case_attr.casefields = (Field[])Arrays.append(case_attr.casefields,f);
		}
		// Create constructor for pizza case
		Vector<Type> targs = new Vector<Type>();
		foreach (Field f; case_attr.casefields)
			targs.append(f.type);
		MethodType mt = MethodType.newMethodType(null,targs.toArray(),Type.tpVoid);
		Constructor init = new Constructor(mt,ACC_PUBLIC);
		init.pos = clazz.pos;
		foreach (Field f; case_attr.casefields)
			init.params.add(new FormPar(f.pos,f.name.name,f.type,0));
		clazz.addMethod(init);
		init.body = new BlockStat(clazz.pos,init);
	}

	public void preGenerate(ASTNode:ASTNode node) {
		return;
	}
	
	public void preGenerate(FileUnit:ASTNode fu) {
		foreach (DNode dn; fu.members; dn instanceof Struct)
			this.preGenerate(dn);
	}
	
	public void preGenerate(Struct:ASTNode clazz) {
		foreach (ASTNode dn; clazz.members; dn instanceof Struct) {
			this.preGenerate(dn);
		}
		if( clazz.isPizzaCase() ) {
			PizzaCaseAttr case_attr = (PizzaCaseAttr)clazz.getAttr(attrPizzaCase);
			Field ftag = clazz.addField(new Field(
				nameCaseTag,Type.tpInt,ACC_PUBLIC|ACC_FINAL|ACC_STATIC) );
			ConstExpr ce = new ConstIntExpr(case_attr.caseno);
			ftag.init = ce;

			Method gettag = new Method(nameGetCaseTag,
				MethodType.newMethodType(Type.emptyArray,Type.tpInt),ACC_PUBLIC);
			gettag.body = new BlockStat(gettag.pos,gettag);
			((BlockStat)gettag.body).addStatement(
				new ReturnStat(gettag.pos,new StaticFieldAccessExpr(ftag.pos,ftag))
			);
			clazz.addMethod(gettag);
		}
		else if( clazz.isHasCases() ) {
			// Add get$case$tag() method to itself
			Method gettag = new Method(Constants.nameGetCaseTag,
				MethodType.newMethodType(Type.emptyArray,Type.tpInt),ACC_PUBLIC);
			gettag.body = new BlockStat(gettag.pos,gettag);
			((BlockStat)gettag.body).addStatement(
				new ReturnStat(gettag.pos,new ConstIntExpr(0))
			);
			clazz.addMethod(gettag);
		}

	}

}

