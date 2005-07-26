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

@node
public class ASTMethodDeclaration extends ASTNode implements PreScanneable, Scope {
	@att public ASTModifiers							modifiers;
    @att public ASTIdentifier							ident;
    @att public final NArr<ASTFormalParameter>			params;
    @att public ASTType									rettype;
    @att public final NArr<ASTArgumentDeclaration>		argtypes;
    @att public final NArr<ASTAlias>					aliases;
    @att public ASTNode									throwns;
    @att public Statement								body;
	public virtual PrescannedBody 						pbody;
	@att public final NArr<ASTRequareDeclaration>		req;
	@att public final NArr<ASTEnsureDeclaration>		ens;
    @att public ASTAnnotationValue						annotation_default;

	@ref public Method									me;
	@ref public final NArr<Type>						ftypes;

	public PrescannedBody get$pbody() { return pbody; }
	public void set$pbody(PrescannedBody p) { pbody = p; }

	ASTMethodDeclaration() {
		super(0);
		modifiers = new ASTModifiers();
		params = new NArr<ASTFormalParameter>(this);
		argtypes = new NArr<ASTArgumentDeclaration>(this);
		aliases = new NArr<ASTAlias>(this);
		req = new NArr<ASTRequareDeclaration>(this);
		ens = new NArr<ASTEnsureDeclaration>(this);
		ftypes = new NArr<Type>(this);
	}

	ASTMethodDeclaration(int id) {
		this();
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTModifiers) {
			modifiers = (ASTModifiers)n;
		}
        else if( n instanceof ASTArgumentDeclaration ) {
			argtypes.append((ASTArgumentDeclaration)n);
		}
        else if( n instanceof ASTType ) {
        	rettype = (ASTType)n;
        }
    	else if( n instanceof ASTIdentifier ) {
        	ident = (ASTIdentifier)n;
        	pos = n.getPos();
		}
    	else if( n instanceof ASTFormalParameter ) {
        	params.append((ASTFormalParameter)n);
        }
    	else if( n instanceof ASTAlias ) {
        	aliases.append((ASTAlias)n);
        }
        else if( n instanceof ASTThrows ) {
        	throwns = n;
        }
        else if( n instanceof ASTRequareDeclaration ) {
			req.append((ASTRequareDeclaration)n);
        }
        else if( n instanceof ASTEnsureDeclaration ) {
			ens.append((ASTEnsureDeclaration)n);
        }
        else if( n instanceof Statement ) {
			body = (Statement)n;
        }
		else if (n instanceof ASTAnnotationValue) {
			annotation_default = (ASTAnnotationValue)n;
		}
        else {
        	throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
        }
    }

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name, Type tp, int resfl)
	{
		node @= ftypes,
		((Type)node).clazz.name.short_name.equals(name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo path, KString name, Expr[] args, Type ret, Type type, int resfl)
	{
		false
	}

    public ASTNode pass3() {
		Struct clazz;
		if( parent instanceof ASTTypeDeclaration )
			clazz = ((ASTTypeDeclaration)parent).me;
		else if( parent instanceof Struct )
			clazz = (Struct)parent;
		else
			throw new CompilerException(pos,"Method must be declared on class level only");
		// TODO: check flags for methods
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

		foreach (ASTArgumentDeclaration ad; argtypes)
			ftypes.append( Env.newMethodArgument(ad.ident.name,clazz).type );

		Type[] margs = Type.emptyArray;
		Type[] mjargs = Type.emptyArray;
		Var[] vars = new Var[params.length + (isVarArgs()?1:0)];
		boolean has_dispatcher = false;
		Type type;
		
		if (ps.isAnnotation() && vars.length > 0) {
			Kiev.reportError(pos, "Annotation methods may not have arguments");
			params.delAll();
			vars = new Var[0];
			setVarArgs(false);
		}

		if (ps.isAnnotation() && (body != null || pbody != null)) {
			Kiev.reportError(pos, "Annotation methods may not have bodies");
			body = null;
			pbody = null;
		}

		// push the method, because formal parameters may refer method's type args
		PassInfo.push(this);
		try {
			if( this.rettype != null ) {
				type = this.rettype.getType();
			} else {
				type = Type.tpVoid;
				if( !ident.name.equals(clazz.name.short_name) )
					throw new CompilerException(pos,"Return type missed or bad constructor name "+ident);
				ident.name = Constants.nameInit;
			}
			for(int i=0; i < params.length; i++) {
				ASTFormalParameter fdecl = (ASTFormalParameter)params[i];
				vars[i] = fdecl.pass3();
				margs = (Type[])Arrays.append(margs,fdecl.type.getType());
				if (fdecl.mm_type != null) {
					mjargs = (Type[])Arrays.append(mjargs,fdecl.mm_type.getType());
					has_dispatcher = true;
				}
				else if (fdecl.type.getType().clazz.isPizzaCase()) {
					mjargs = (Type[])Arrays.append(mjargs,
						Type.getRealType(PassInfo.clazz.type,
							fdecl.type.getType().clazz.super_clazz));
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
		MethodType mtype = MethodType.newMethodType(null,ftypes.toArray(),margs,type);
		MethodType mjtype = has_dispatcher ? MethodType.newMethodType(null,null,mjargs,type) : null;
		me = new Method(clazz,ident.name,mtype,mjtype,flags);
		trace(Kiev.debugMultiMethod,"Method "+me+" has dispatcher type "+me.dtype);
		me.setPos(getPos());
		modifiers.getMetas(me.meta);
		if (me.parent.isAnnotation() && annotation_default != null) {
			me.annotation_default = ASTAnnotation.makeValue(annotation_default);
		}
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

		if( modifiers.acc != null ) me.acc = new Access(modifiers.acc.accflags);

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
		dmp.space().append(ident.name);
        dmp.append('(');
        for(int i=0; i < params.length; i++) {
        	dmp.append(params[i]);
            if( i < params.length - 1 ) dmp.append(',').space();
        }
        dmp.append(')').append(';');
        return dmp;
	}
}

