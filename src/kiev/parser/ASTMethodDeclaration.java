/* Generated By:JJTree: Do not edit this line. ASTMethodDeclaration.java */

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
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTMethodDeclaration.java,v 1.3.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

public class ASTMethodDeclaration extends ASTNode implements PreScanneable, ScopeOfNames {
	public int			dim;
    public ASTNode[]	modifier = ASTNode.emptyArray;
	public ASTAccess	acc;
    public KString		name;
    public ASTNode[]	params = ASTNode.emptyArray;
    public ASTNode		type;
    public ASTNode[]	ftypes = ASTNode.emptyArray;
    public ASTAlias[]	aliases = ASTAlias.emptyArray;
    public ASTNode		throwns;
    public Statement	body;
	public virtual PrescannedBody pbody;
	public ASTRequareDeclaration[]	req;
	public ASTEnsureDeclaration[]	ens;

	public Method		me;

	public PrescannedBody get$pbody() { return pbody; }
	public void set$pbody(PrescannedBody p) { pbody = p; }

	ASTMethodDeclaration(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if( n instanceof ASTModifier ) {
        	modifier = (ASTNode[])Arrays.append(modifier,n);
        }
		else if( n instanceof ASTAccess ) {
			if( acc != null )
				throw new CompilerException(n.getPos(),"Duplicate 'access' specified");
			acc = (ASTAccess)n;
		}
        else if( n instanceof ASTArgumentDeclaration ) {
			ftypes = (ASTNode[])Arrays.append(ftypes,n);
		}
        else if( n instanceof ASTType ) {
        	type = n;
        }
    	else if( n instanceof ASTIdentifier ) {
        	name = ((ASTIdentifier)n).name;
        	pos = n.getPos();
		}
    	else if( n instanceof ASTFormalParameter ) {
        	params = (ASTNode[])Arrays.append(params,n);
        }
    	else if( n instanceof ASTAlias ) {
        	aliases = (ASTAlias[])Arrays.append(aliases,n);
        }
        else if( n instanceof ASTThrows ) {
        	throwns = n;
        }
        else if( n instanceof ASTRequareDeclaration ) {
			if( req == null )
				req = new ASTRequareDeclaration[]{(ASTRequareDeclaration)n};
			else
				req = (ASTRequareDeclaration[])Arrays.append(req,(ASTRequareDeclaration)n);
        }
        else if( n instanceof ASTEnsureDeclaration ) {
			if( ens == null )
				ens = new ASTEnsureDeclaration[]{(ASTEnsureDeclaration)n};
			else
				ens = (ASTEnsureDeclaration[])Arrays.append(ens,(ASTEnsureDeclaration)n);
        }
        else if( n instanceof Statement ) {
			body = (Statement)n;
        }
        else {
        	throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	rule public resolveNameR(ASTNode@ node, ResPath path, KString name, Type tp, int resfl)
	{
		ftypes instanceof Type[] && ftypes.length > 0,
		node @= ((Type[])ftypes),
		((Type)node).clazz.name.short_name.equals(name)
	}

    public Method pass3() {
		Struct clazz;
		if( parent instanceof ASTTypeDeclaration )
			clazz = ((ASTTypeDeclaration)parent).me;
		else if( parent instanceof Struct )
			clazz = (Struct)parent;
		else
			throw new CompilerException(pos,"Method must be declared on class level only");
		int flags = 0;
		// TODO: check flags for fields
		for(int i=0; i < modifier.length; i++)
			flags |= ((ASTModifier)modifier[i]).flag();
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

		Type[] mfargs = new Type[ftypes.length];
		for(int i=0; i < ftypes.length; i++)
			mfargs[i] = Env.newMethodArgument(((ASTArgumentDeclaration)ftypes[i]).name,clazz).type;
		ftypes = mfargs;	// become scope of names

		Type[] margs = Type.emptyArray;
		Type[] mjargs = Type.emptyArray;
		Var[] vars = new Var[params.length + (isVarArgs()?1:0)];
		boolean has_dispatcher = false;
		Type type;

		// push the method, because formal parameters may refer method's type args
		PassInfo.push(this);
		try {
			if( this.type != null ) {
				if( this.type instanceof ASTType )
					type = ((ASTType)this.type).pass2();
				else
					type = (Type)this.type;
			} else {
				type = Type.tpVoid;
				if( !name.equals(clazz.name.short_name) )
					throw new CompilerException(pos,"Return type missed or bad constructor name "+name);
				name = Constants.nameInit;
			}
			for(int i=0; i < dim; i++) type = Type.newArrayType(type);
			for(int i=0; i < params.length; i++) {
				ASTFormalParameter fdecl = (ASTFormalParameter)params[i];
				vars[i] = fdecl.pass3();
				margs = (Type[])Arrays.append(margs,fdecl.resolved_type);
				if (fdecl.resolved_jtype != null) {
					mjargs = (Type[])Arrays.append(mjargs,fdecl.resolved_jtype);
					has_dispatcher = true;
				}
				else if (fdecl.resolved_type.clazz.isPizzaCase()) {
					mjargs = (Type[])Arrays.append(mjargs,
						Type.getRealType(PassInfo.clazz.type,
							fdecl.resolved_type.clazz.super_clazz));
					has_dispatcher = true;
				}
				else if (has_dispatcher) {
					mjargs = (Type[])Arrays.append(mjargs,margs[i]);
				}
			}
		} finally {
			PassInfo.pop(this);
		}
		if( isVarArgs() ) {
			vars[vars.length-1] = new Var(pos,null,nameVarArgs,Type.newArrayType(Type.tpObject),0);
			margs = (Type[])Arrays.append(margs,vars[vars.length-1].type);
			mjargs = (Type[])Arrays.append(margs,vars[vars.length-1].type);
		}
		MethodType mtype = MethodType.newMethodType(null,mfargs,margs,type);
		MethodType mjtype = has_dispatcher ? MethodType.newMethodType(null,null,mjargs,type) : null;
		me = new Method(clazz,name,mtype,mjtype,flags);
		trace(Kiev.debugMultiMethod,"Method "+me+" has dispatcher type "+me.dtype);
		me.setPos(getPos());
        me.body = body;
        if( me.body != null )
	        me.body.parent = me;
		if( !me.isStatic() )
			vars = (Var[])Arrays.insert(vars,new Var(pos,me,Constants.nameThis,clazz.type,0),0);
		for(int i=0; i < vars.length; i++) {
			vars[i].parent = me;
		}
		foreach(ASTAlias al; aliases) al.attach(me);
//		MethodParamsAttr pa = new MethodParamsAttr(clazz,vars);
//		me.addAttr(pa);
		me.params = vars;
        clazz.addMethod(me);
        if( throwns != null ) {
        	Type[] thrs = ((ASTThrows)throwns).pass3();
        	ExceptionsAttr athr = new ExceptionsAttr();
        	athr.exceptions = thrs;
			me.addAttr(athr);
        }
		if( pbody != null ) pbody.setParent(me);

		if( acc != null ) me.acc = new Access(acc.accflags);

		for(int i=0; req!=null && i < req.length; i++) {
			WorkByContractCondition cond = (WorkByContractCondition)req[i].pass3();
			cond.parent = me;
			cond.definer = me;
			me.conditions = (WorkByContractCondition[])	Arrays.append(me.conditions,cond);
		}
		for(int i=0; ens!=null && i < ens.length; i++) {
			WorkByContractCondition cond = (WorkByContractCondition)ens[i].pass3();
			cond.parent = me;
			cond.definer = me;
			me.conditions = (WorkByContractCondition[])	Arrays.append(me.conditions,cond);
		}

        return me;
    }

	public Dumper toJava(Dumper dmp) {
		dmp.space().append(name);
        dmp.append('(');
        for(int i=0; i < params.length; i++) {
        	dmp.append(params[i]);
            if( i < params.length - 1 ) dmp.append(',').space();
        }
        dmp.append(')').append(';');
        return dmp;
	}
}
