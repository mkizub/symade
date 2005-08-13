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
public class ASTMethodDeclaration extends ASTNode implements PreScanneable, ScopeOfNames, ScopeOfMethods {
	@att public ASTModifiers					modifiers;
    @att public ASTIdentifier					ident;
    @att public final NArr<FormPar>			params;
    @att public TypeRef							rettype;
    @att public final NArr<BaseStruct>			argtypes;
    @att public final NArr<ASTAlias>			aliases;
    @att public ASTNode							throwns;
    @att public Statement						body;
	@att public PrescannedBody 					pbody;
	@att public final NArr<WBCCondition>		conditions;
    @att public MetaValue						annotation_default;

	@ref public Method							me;
	@ref public Type[]							ftypes;

	ASTMethodDeclaration() {
		modifiers = new ASTModifiers();
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name, Type tp)
		Type@ t;
	{
		ftypes != null,
		t @= ftypes,
		t.clazz.name.short_name.equals(name),
		node ?= new TypeRef(t)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo path, KString name, Expr[] args, Type ret, Type type)
	{
		false
	}

    public ASTNode pass3() {
		if !( parent instanceof Struct )
			throw new CompilerException(pos,"Method must be declared on class level only");
		Struct clazz = (Struct)parent;
		// TODO: check flags for methods
		int flags = modifiers.getFlags();
		if( clazz.isPackage() ) flags |= ACC_STATIC;
		if( (flags & ACC_PRIVATE) != 0 ) flags &= ~ACC_FINAL;
		else if( clazz.isClazz() && clazz.isFinal() ) flags |= ACC_FINAL;
		else if( clazz.isInterface() ) {
			flags |= ACC_PUBLIC;
			if( pbody == null ) flags |= ACC_ABSTRACT;
		}
		if( isVarArgs() ) flags |= ACC_VARARGS;

		if (argtypes.length > 0) {
			ftypes = new Type[argtypes.length];
			for (int i=0; i < argtypes.length; i++)
				ftypes[i] = argtypes[i].type;
		}

		if (clazz.isAnnotation() && params.length > 0) {
			Kiev.reportError(pos, "Annotation methods may not have arguments");
			params.delAll();
			setVarArgs(false);
		}

		if (clazz.isAnnotation() && (body != null || pbody != null)) {
			Kiev.reportError(pos, "Annotation methods may not have bodies");
			body = null;
			pbody = null;
		}

		Type[] margs  = new Type[params.length];
		Type[] mjargs = new Type[params.length];
		boolean has_dispatcher = false;
		Type type;
		
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
			for (int i=0; i < params.length; i++) {
				FormPar fp = params[i];
				if (fp.meta != null)
					fp.meta.verify();
				margs[i] = fp.type;
				if (fp.stype != null) {
					mjargs[i] = fp.stype.getType();
					has_dispatcher = true;
				}
				else if (fp.type.isPizzaCase()) {
					mjargs[i] = Type.getRealType(PassInfo.clazz.type, fp.type.clazz.super_type);
					has_dispatcher = true;
				}
				else {
					mjargs[i] = fp.type;
				}
			}
		} finally {
			PassInfo.pop(this);
		}
		if( isVarArgs() ) {
			FormPar va = new FormPar(pos,nameVarArgs,Type.newArrayType(Type.tpObject),0);
			params.append(va);
			margs = (Type[])Arrays.append(margs,va.type);
			mjargs = (Type[])Arrays.append(margs,va.type);
		}
		MethodType mtype = MethodType.newMethodType(null,ftypes,margs,type);
		MethodType mjtype = has_dispatcher ? MethodType.newMethodType(null,null,mjargs,type) : null;
		me = new Method(clazz,ident.name,mtype,mjtype,flags);
		trace(Kiev.debugMultiMethod,"Method "+me+" has dispatcher type "+me.dtype);
		me.setPos(getPos());
		modifiers.getMetas(me.meta);
		if (me.parent.isAnnotation() && annotation_default != null) {
			//me.annotation_default = ASTAnnotation.makeValue(annotation_default);
			me.annotation_default = annotation_default.verify();
		}
        me.body = body;
		me.pbody = pbody;
		foreach(ASTAlias al; aliases) al.attach(me);
		me.params.addAll(params);
        this.replaceWith(me);
        if( throwns != null ) {
        	Type[] thrs = ((ASTThrows)throwns).pass3();
        	ExceptionsAttr athr = new ExceptionsAttr();
        	athr.exceptions = thrs;
			me.addAttr(athr);
        }

		if( modifiers.acc != null ) me.acc = modifiers.acc;

		foreach(WBCCondition cond; conditions) {
			cond.definer = me;
			me.conditions.append(cond);
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

