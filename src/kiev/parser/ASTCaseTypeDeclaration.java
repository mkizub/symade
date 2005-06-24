/* Generated By:JJTree: Do not edit this line. ASTCaseTypeDeclaration.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTCaseTypeDeclaration.java,v 1.3.2.1.2.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1.2.1 $
 *
 */

public class ASTCaseTypeDeclaration extends ASTStructDeclaration implements PreScanneable {
	public ASTNode[]	casefields = ASTNode.emptyArray;
	public Statement	body;
	public virtual PrescannedBody pbody;

	public ASTCaseTypeDeclaration(int id) {}

	public PrescannedBody get$pbody() { return pbody; }
	public void set$pbody(PrescannedBody p) { pbody = p; }

  	public void set(Token t) {
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
			name = ((ASTIdentifier)n).name;
            pos = n.getPos();
		}
        else if( n instanceof ASTArgumentDeclaration ) {
			argument = (ASTNode[])Arrays.append(argument,n);
		}
        else if( n instanceof ASTFormalParameter ) {
			casefields = (ASTNode[])Arrays.append(casefields,n);
		}
        else if( n instanceof Statement ) {
			body = (Statement)n;
		}
        else {
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

//	public ASTNode pass1(ASTNode pn) {
//		int flags = 0;
//		Struct sup = null;
//		Struct[] impls = Struct.emptyArray;
//		// TODO: check flags for structures
//		for(int i=0; i < modifier.length; i++)
//			flags |= ((ASTModifier)modifier[i]).flag();
//		KString short_name = this.name;
//		ClazzName clname = ClazzName.fromOuterAndName(PassInfo.clazz,short_name,false,true);
//
//		flags |= AccessFlags.ACC_PIZZACASE;
//
//		Struct parnt;
//		if( parent instanceof Struct )
//			parnt = (Struct)parent;
//		else
//			parnt = ((ASTTypeDeclaration)parent).me;
//		me = Env.newStruct(clname,parnt,flags,true);
//		me.setResolved(true);
//		me.setStatic(true);
//		me.setPizzaCase(true);
//		Env.setProjectInfo(me.name,((ASTFileUnit)Kiev.k.getJJTree().rootNode()).filename);
//		SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
//		me.addAttr(sfa);
//		parnt.addCase(me);
//
//		/* Then may be class arguments - they are proceed here, but their
//		   inheritance - at pass2()
//		*/
//		// Case argumets have to be arguments of outer classes
//		for(int i=0; i < argument.length; i++) {
//			Type[] outer_args = ((ASTTypeDeclaration)parent).me.type.args;
//			if( outer_args == null || outer_args.length <= i
//			|| !outer_args[i].clazz.name.short_name.equals(((ASTArgumentDeclaration)argument[i]).name) )
//				throw new CompilerException(argument[i].getPos(),"Case argument must match outer class argument,"
//					+" but arg["+i+"] is "+((ASTArgumentDeclaration)argument[i]).name
//					+" and have to be "+outer_args[i].clazz.name.short_name);
//		}
//
//		/* Create type for class's arguments, if any */
//		Type[] targs = Type.emptyArray;
//		if( argument.length > 0 ) {
//			targs = ((ASTTypeDeclaration)parent).me.type.args;
//		}
//
//		/* Generate type for this structure */
//		me.type = Type.newRefType(me,targs);
//
//		me.super_clazz = ((ASTTypeDeclaration)parent).me.type;
//
//		return me;
//	}

	public ASTNode pass2(ASTNode pn) {

		Struct parnt;
		if( parent instanceof Struct )
			parnt = (Struct)parent;
		else
			parnt = ((ASTTypeDeclaration)parent).me;

		me.super_clazz = parnt.type;

		if( acc != null ) me.acc = new Access(acc.accflags);

		return me;
	}

	public Struct pass3() {
		Struct parnt;
		if( parent instanceof Struct )
			parnt = (Struct)parent;
		else
			parnt = ((ASTTypeDeclaration)parent).me;

		me.super_clazz = parnt.type;

		// Process members
		Type[] targs = new Type[casefields.length];
		Var[] vars = new Var[casefields.length+1];
		vars[0] = new Var(pos,Constants.nameThis,me.type,0);
		PizzaCaseAttr case_attr = (PizzaCaseAttr)me.getAttr(attrPizzaCase);
		for(int i=0; i < casefields.length; i++) {
			ASTFormalParameter fdecl = (ASTFormalParameter)casefields[i];
			vars[i+1] = fdecl.pass3();
			targs[i] = vars[i+1].type;
			Field f = me.addField(new Field(me,vars[i+1].name.name,vars[i+1].type,ACC_PUBLIC));
			case_attr.casefields = (Field[])Arrays.append(case_attr.casefields,f);
			f.setPublic(true);
		}
		MethodType mt = MethodType.newMethodType(Type.tpMethodClazz,null,targs,Type.tpVoid);
		Method init = new Method(me,Constants.nameInit,mt,ACC_PUBLIC);
		init.setPos(getPos());
		init.parent = me;
		for(int i=0; i < vars.length; i++) vars[i].parent = init;
//		init.addAttr( new MethodParamsAttr(me,vars) );
		init.params = vars;
		me.addMethod(init);
		if( body != null )
			init.body = body;
		else {
			init.body = new BlockStat(pos,init);
		}
		if( pbody != null ) pbody.setParent(me);

		return me;
	}

	public void resolveFinalFields(boolean cleanup) {
		// Resolve final values of class's fields
		me.resolveFinalFields(cleanup);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(me.name.name).space();
	}

}
