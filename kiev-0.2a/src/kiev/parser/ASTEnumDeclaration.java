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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTEnumDeclaration.java,v 1.3.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

public class ASTEnumDeclaration extends ASTTypeDeclaration {

	import kiev.stdlib.Debug;

	public ASTEnumDeclaration(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if( n instanceof ASTModifier) {
			modifier = (ASTNode[])Arrays.append(modifier,n);
		}
		else if( n instanceof ASTAccess ) {
			if( acc != null )
				throw new CompilerException(n.getPos(),"Duplicate 'access' specified");
			acc = (ASTAccess)n;
		}
        else if( n instanceof ASTIdentifier ) {
			if( name == null ) {
				name = ((ASTIdentifier)n).name;
				pos = n.getPos();
			} else {
				members = (ASTNode[])Arrays.append(members,n);
			}
		}
        else if( n instanceof ASTExtends ) {
			ext = n;
		}
        else if( n instanceof ASTConstExpression ) {
			members = (ASTNode[])Arrays.append(members,n);
		}
        else {
			members = (ASTNode[])Arrays.append(members,n);
        }
    }

	public ASTNode pass1() {
		trace(Kiev.debugResolve,"Pass 1 for enum "+name);
		int flags = 0;
		Struct sup = null;
		Struct[] impls = Struct.emptyArray;
		// TODO: check flags for structures
		for(int i=0; i < modifier.length; i++)
			flags |= ((ASTModifier)modifier[i]).flag();
		KString short_name = this.name;
		ClazzName clname = null;
		if( this.name != null ) {
			clname = ClazzName.fromOuterAndName(PassInfo.clazz,short_name);
		}

        flags |= ACC_ENUM;

		me = Env.newStruct(clname,PassInfo.clazz/*,sup*/,flags,true);
		me.setResolved(true);
		if( !(parent instanceof ASTFileUnit) ) me.setStatic(true);
		if( parent instanceof ASTFileUnit || parent instanceof ASTTypeDeclaration ) {
			Env.setProjectInfo(me.name,((ASTFileUnit)Kiev.k.getJJTree().rootNode()).filename);
		}
		SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
		me.addAttr(sfa);
		me.setEnum(true);

        PassInfo.push(me);
        try {
			/* Then may be class arguments - they are proceed here, but their
			   inheritance - at pass2()
			*/
			// TODO: decide if inner classes's argumets have to be arguments of outer classes
			/* Generate type for this structure */
			me.type = Type.newJavaRefType(me);

        	// No inner classes and cases for enum
		} finally { PassInfo.pop(me); }

		return me;
	}

	public ASTNode pass2() {
		trace(Kiev.debugResolve,"Pass 2 for enum "+me);
        PassInfo.push(me);
        try {
		} finally { PassInfo.pop(me); }

		return me;
	}

	public ASTNode pass2_2() {
		trace(Kiev.debugResolve,"Pass 2_2 for enum "+me);
        PassInfo.push(me);
        try {
			/* Now, process 'extends' and 'implements' clauses */
			ASTNonArrayType at;
			if( ext != null ) {
				ASTExtends exts = (ASTExtends)ext;
				at = (ASTNonArrayType)exts.children[0];
				me.super_clazz = at.pass2();
			}
			if( me.super_clazz == null ) {
				me.super_clazz = Type.tpEnum;
			}

			if( !me.super_clazz.isReference() ) {
				me.setPrimitiveEnum(true);
				me.type.setMeAsPrimitiveEnum();
			}

			if( acc != null ) me.acc = new Access(acc.accflags);

		} finally { PassInfo.pop(me); }

		return me;
	}

	public static Struct pass3(Struct me, ASTNode[] members) {
		trace(Kiev.debugResolve,"Pass 3 for enum "+me);
        PassInfo.push(me);
        try {
			// Process members
			for(int i=0; i < members.length; i++) {
				members[i].parent = me;
				if( members[i] instanceof ASTIdentifier ) {
					KString fname = ((ASTIdentifier)members[i]).name;
					Type me_type;
					//if (me.isPrimitiveEnum())
					//	me_type = me.super_clazz;
					//else
						me_type = me.type;
					Field f = new Field(me,fname,me_type,ACC_PUBLIC | ACC_STATIC | ACC_FINAL );
					members[i] = me.addField(f);
				}
				else if( members[i] instanceof ASTConstExpression ) {
					Object val = ((ASTConstExpression)members[i]).val;
					if( val instanceof KString ) {
						int n;
						if (members[i-1] instanceof Field)
							n = i-1;
						else if (members[i-2] instanceof Field)
							n = i-2;
						else
							throw new CompilerException(members[i].pos,"Cannot find enum field to attach string");
						((Field)members[n]).name.addAlias(KString.from("\""+val+"\""));
						continue;
					}
					if( !( val instanceof Number || val instanceof Character))
						throw new CompilerException(members[i].pos,"Not an integer/character value");
					if (me.isPrimitiveEnum()) {
						((Field)members[i-1]).init = new ConstExpr(members[i].pos,val);
					} else {
						((Field)members[i-1]).init =
							new NewExpr(me.pos,me.type,new Expr[]{
									new ConstExpr(members[i].pos,val)});
					}
				}
				else {
					throw new CompilerException(members[i].getPos(),"Unknown type if enum member: "+members[i]);
				}
				members[i].parent = me;
			}
		} finally { PassInfo.pop(me); }

		return me;
	}

	public void resolveFinalFields(boolean cleanup) {
   	    // Process inner classes and cases
		for(int i=0; i < members.length; i++) {
			if( !(members[i] instanceof ASTImport) ) continue;
			ASTNode imp = ((ASTImport)members[i]).pass2();
			if( imp == null )
				Kiev.reportError(members[i].getPos(),"Imported member "+imp+" not found");
			else if( imp instanceof Field ) {
				if( !imp.isStatic() ) {
					Kiev.reportError(members[i].getPos(),"Imported field "+imp+" must be static");
				} else {
					me.imported = (ASTNode[])Arrays.append(me.imported,imp);
				}
			}
			else if( imp instanceof Method ) {
				if( !imp.isStatic() ) {
					Kiev.reportError(members[i].getPos(),"Imported method "+imp+" must be static");
				} else {
					me.imported = (ASTNode[])Arrays.append(me.imported,imp);
				}
			}
			else if( imp instanceof Struct ) {
				Struct is = (Struct)imp;
				for(int j=0; j < is.fields.length; j++) {
					if( is.fields[j].isStatic() && !is.fields[j].name.equals(KString.Empty) )
						me.imported = (ASTNode[])Arrays.append(me.imported,is.fields[j]);
				}
				for(int j=0; j < is.methods.length; j++) {
					if( is.methods[j].isStatic() )
						me.imported = (ASTNode[])Arrays.append(me.imported,is.methods[j]);
				}
			}
			else
				throw new CompilerException(members[i].getPos(),"Unknown type if imported member: "+imp);
		}
		// Resolve final values of class's fields
		me.resolveFinalFields(cleanup);
	}

}
