/* Generated By:JJTree: Do not edit this line. ASTRuleDeclaration.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTRuleDeclaration.java,v 1.3.2.1.2.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1.2.1 $
 *
 */

@node
public class ASTRuleDeclaration extends ASTNode implements PreScanneable {

	@att public ASTModifiers				modifiers;
    @att public ASTIdentifier				ident;
    @att public final NArr<ASTNode>		params;
    @att public final NArr<ASTAlias>		aliases;
    @att public final NArr<ASTNode>		localvars;
    @att public Statement					body;
	@att public PrescannedBody				pbody;
	@att public final NArr<WBCCondition>	conditions;

	@ref public RuleMethod	me;

	public ASTRuleDeclaration() {
		modifiers = new ASTModifiers();
	}

	@getter public PrescannedBody get$pbody() { return pbody; }
	@setter public void set$pbody(PrescannedBody p) { pbody = p; }

    public ASTNode pass3() {
		if !( parent instanceof Struct )
			throw new CompilerException(pos,"Method must be declared on class level only");
		Struct clazz = (Struct)parent;
		// TODO: check flags for fields
		int flags = modifiers.getFlags();
		if( clazz.isPackage() ) flags |= ACC_STATIC;
		if( (flags & ACC_PRIVATE) != 0 ) flags &= ~ACC_FINAL;
		else if( clazz.isClazz() && clazz.isFinal() ) flags |= ACC_FINAL;
		else if( clazz.isInterface() ) {
			flags |= ACC_PUBLIC;
			if( pbody == null ) flags |= ACC_ABSTRACT;
		}
		if( isVarArgs() ) flags |= ACC_VARARGS;
		Type type = Type.tpRule;
		NArr<FormPar> vars = new NArr<FormPar>();
		vars.append(new FormPar(pos,namePEnv,Type.tpRule,0));
		vars[0].setForward(true);
		Type[] margs = new Type[] {Type.tpRule};
		Type[] mfargs = Type.emptyArray;
		for(int i=0; i < params.length; i++) {
			ASTFormalParameter fdecl = (ASTFormalParameter)params[i];
			vars.append(fdecl.pass3());
			margs = (Type[])Arrays.append(margs,vars[i+1].type);
		}
		Var[] lvars = Var.emptyArray;
		for(int i=0; i < localvars.length; i++) {
			ASTVarDecls vdecls = (ASTVarDecls)localvars[i];
			int flags = 0;
			Type type = ((TypeRef)vdecls.type).getType();
			int dim = 0;
			while( type.isArray() ) { dim++; type = type.args[0]; }
			Var[] vars = new Var[vdecls.vars.length];
			Expr[] inits = new Expr[vdecls.vars.length];
			for(int j=0; j < vdecls.vars.length; j++) {
				ASTVarDecl vdecl = (ASTVarDecl)vdecls.vars[j];
				KString vname = vdecl.name.name;
				Type tp = type;
				for(int k=0; k < vdecl.dim; k++) tp = Type.newArrayType(tp);
				for(int k=0; k < dim; k++) tp = Type.newArrayType(tp);
				vars[j] = new Var(vdecl.pos,vname,tp,flags);
				if (vdecls.hasFinal()) vars[j].setFinal(true);
				if (vdecls.hasForward()) vars[j].setForward(true);
				vars[j].setLocalRuleVar(true);
				if( vdecl.init != null )
					inits[j] = vdecl.init.resolveExpr(vars[j].type);
				else if (vars[j].isFinal())
					Kiev.reportError(vars[j].pos,"Final variable "+vars[j]+" must have initializer");
				lvars = (Var[])Arrays.append(lvars,vars[j]);
			}
		}
		MethodType mtype = MethodType.newMethodType(null,mfargs,margs,type);
		me = new RuleMethod(clazz,ident.name,mtype,flags | ACC_MULTIMETHOD);
		trace(Kiev.debugMultiMethod,"Rule "+me+" has java type "+me.jtype);
		me.setPos(getPos());
        me.body = body;
		me.pbody = pbody;
		for(int i=0; i < lvars.length; i++) {
			lvars[i].parent = me;
		}
		foreach(ASTAlias al; aliases) al.attach(me);
		me.params.addAll(vars);
		me.localvars.addAll(lvars);
        this.replaceWith(me);

		if( modifiers.acc != null ) me.acc = modifiers.acc;

		foreach(WBCCondition cond; conditions) {
			cond.definer = me;
			me.conditions.append(cond);
		}


        return me;
    }

}

