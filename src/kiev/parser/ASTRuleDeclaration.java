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

	@att public ASTModifiers			modifiers;
    @att public ASTIdentifier			ident;
    @att public final NArr<ASTNode>	params;
    @att public final NArr<ASTAlias>	aliases;
    @att public final NArr<ASTNode>	localvars;
    @att public Statement	body;
	@virtual
	public virtual PrescannedBody pbody;
	@att public final NArr<ASTRequareDeclaration>	req;
	@att public final NArr<ASTEnsureDeclaration>	ens;

	@ref public RuleMethod	me;

	public ASTRuleDeclaration() {
		modifiers = new ASTModifiers();
	}

	public ASTRuleDeclaration(int id) {
		this();
	}

	@getter public PrescannedBody get$pbody() { return pbody; }
	@setter public void set$pbody(PrescannedBody p) { pbody = p; }

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTModifiers) {
			modifiers = (ASTModifiers)n;
		}
		else if( n instanceof ASTIdentifier ) {
			ident = (ASTIdentifier)n;
			pos = n.getPos();
		}
		else if( n instanceof ASTFormalParameter ) {
			params.append(n);
		}
		else if( n instanceof ASTVarDecls ) {
			localvars.append(n);
		}
		else if( n instanceof ASTAlias ) {
			aliases.append((ASTAlias)n);
        }
        else if( n instanceof ASTRequareDeclaration ) {
			req.append((ASTRequareDeclaration)n);
        }
        else if( n instanceof ASTEnsureDeclaration ) {
			ens.append((ASTEnsureDeclaration)n);
        }
        else if( n instanceof ASTRuleBlock ) {
			body = (Statement)n;
        }
        else {
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

    public ASTNode pass3() {
		Struct clazz;
		if( parent instanceof ASTTypeDeclaration )
			clazz = ((ASTTypeDeclaration)parent).me;
		else if( parent instanceof Struct )
			clazz = (Struct)parent;
		else
			throw new CompilerException(pos,"Method must be declared on class level only");
		// TODO: check flags for fields
		int flags = modifiers.getFlags();
		Struct ps;
		if( parent instanceof ASTTypeDeclaration)
			ps = ((ASTTypeDeclaration)parent).me;
		else
			ps = (Struct)parent;
		if( ps.isPackage() ) flags |= ACC_STATIC;
		if( (flags & ACC_PRIVATE) != 0 ) flags &= ~ACC_FINAL;
		else if( ps.isClazz() && ps.isFinal() ) flags |= ACC_FINAL;
		else if( ps.isInterface() ) {
			flags |= ACC_PUBLIC;
			if( pbody == null ) flags |= ACC_ABSTRACT;
		}
		if( isVarArgs() ) flags |= ACC_VARARGS;
		Type type = Type.tpRule;
		NArr<Var> vars = new NArr<Var>(null, false);
		vars.append(new Var(pos,this,namePEnv,Type.tpRule,0));
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
			Type type = ((ASTType)vdecls.type).getType();
			int dim = 0;
			while( type.isArray() ) { dim++; type = type.args[0]; }
			Var[] vars = new Var[vdecls.vars.length];
			Expr[] inits = new Expr[vdecls.vars.length];
			for(int j=0; j < vdecls.vars.length; j++) {
				ASTVarDecl vdecl = (ASTVarDecl)vdecls.vars[j];
				KString vname = vdecl.name;
				Type tp = type;
				for(int k=0; k < vdecl.dim; k++) tp = Type.newArrayType(tp);
				for(int k=0; k < dim; k++) tp = Type.newArrayType(tp);
				vars[j] = new Var(vdecl.pos,this,vname,tp,flags);
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
//		if( isVarArgs() ) {
//			vars[vars.length-1] = new Var(pos,null,nameVarArgs,Type.newArrayType(Type.tpObject),0);
//			margs = (Type[])Arrays.append(margs,vars[vars.length-1].type);
//		}
		MethodType mtype = MethodType.newMethodType(null,mfargs,margs,type);
		me = new RuleMethod(clazz,ident.name,mtype,flags | ACC_MULTIMETHOD);
		trace(Kiev.debugMultiMethod,"Rule "+me+" has java type "+me.jtype);
		me.setPos(getPos());
        me.body = body;
        if( me.body != null )
	        me.body.parent = me;
		if( !me.isStatic() )
			vars.insert(new Var(pos,me,Constants.nameThis,clazz.type,0),0);
		for(int i=0; i < vars.length; i++) {
			vars[i].parent = me;
		}
		for(int i=0; i < lvars.length; i++) {
			lvars[i].parent = me;
		}
		foreach(ASTAlias al; aliases) al.attach(me);
//		MethodParamsAttr pa = new MethodParamsAttr(clazz,vars);
//		me.addAttr(pa);
		me.params.addAll(vars);
		if( lvars.length > 0 )
			me.localvars = lvars;
        clazz.addMethod(me);
//        if( throwns != null ) {
//        	Type[] thrs = ((ASTThrows)throwns).pass3();
//        	ExceptionsAttr athr = new ExceptionsAttr();
//        	athr.exceptions = thrs;
//			me.addAttr(athr);
//        }
		if( pbody != null ) pbody.setParent(me);

		if( modifiers.acc != null ) me.acc = new Access(modifiers.acc.accflags);

		for(int i=0; req!=null && i < req.length; i++) {
			WorkByContractCondition cond = (WorkByContractCondition)req[i].pass3();
			cond.parent = me;
			cond.definer = me;
			me.conditions.append(cond);
		}
		for(int i=0; ens!=null && i < ens.length; i++) {
			WorkByContractCondition cond = (WorkByContractCondition)ens[i].pass3();
			cond.parent = me;
			cond.definer = me;
			me.conditions.append(cond);
		}

        return me;
    }

}

