/* Generated By:JJTree: Do not edit this line. ASTAnonymouseClosure.java */

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
import kiev.vlang.*;
import kiev.transf.*;
import kiev.stdlib.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTAnonymouseClosure.java,v 1.5 1999/01/29 01:22:21 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5 $
 *
 */

@node
@cfnode
public class ASTAnonymouseClosure extends Expr {
    @att public final NArr<FormPar>		params;
    @att public TypeRef						rettype;
    @att public Statement					body;
	@att public Expr						new_closure;

  	public void set(Token t) {
    	pos = t.getPos();
	}

	public ASTNode resolve(Type reqType) {
		if( isResolved() ) return new_closure;
		ClazzName clname = ClazzName.fromBytecodeName(
			new KStringBuffer(PassInfo.clazz.name.bytecode_name.len+8)
				.append_fast(PassInfo.clazz.name.bytecode_name)
				.append_fast((byte)'$')
				.append(PassInfo.clazz.countAnonymouseInnerStructs())
				.toKString(),
			false
		);
		Struct me = Env.newStruct(clname,PassInfo.clazz,flags,true);
		me.setResolved(true);
		me.setLocal(true);
		me.setAnonymouse(true);
		if( PassInfo.method==null || PassInfo.method.isStatic() ) me.setStatic(true);
		me.parent = parent;
		SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
		me.addAttr(sfa);
		if( Env.getStruct(Type.tpClosureClazz.name) == null )
			throw new RuntimeException("Core class "+Type.tpClosureClazz.name+" not found");
		me.super_type = Type.tpClosureClazz.type;

		Type[] types = new Type[params.length];
		Var[] vars = new Var[params.length];
		for(int i=0; i < types.length; i++) {
			vars[i] = (Var)params[i];
			types[i] = vars[i].type;
		}
		Type ret = rettype.getType();
		me.type = ClosureType.newClosureType(me,types,ret);

		if( ret != Type.tpRule ) {
			ASTMethodDeclaration md = new ASTMethodDeclaration();
			KString call_name;
			if( ret.isReference() ) md.ident = new ASTIdentifier(pos, KString.from("call_Object"));
			else md.ident = new ASTIdentifier(pos, KString.from("call_"+ret));
			md.modifiers.modifier.add(ASTModifier.modPUBLIC);
			if( ret.isReference() )
				md.rettype = new TypeRef(pos, Type.tpObject);
			else
				md.rettype = new TypeRef(pos, ret);
			md.body = body;
			me.members.add(md);
		} else {
			ASTRuleDeclaration md = new ASTRuleDeclaration();
			md.ident = new ASTIdentifier(pos, KString.from("call_rule"));
			md.body = body;
			me.members.add(md);
		}

		NArr<ASTNode> stats;
		if( body instanceof ASTBlock )
			stats = ((ASTBlock)body).stats;
		else
			stats = ((BlockStat)body).stats;
		for(int i=0; i < vars.length; i++) {
			Var v = vars[i];
			Expr val = new ContainerAccessExpr(pos,
				new AccessExpr(pos,new ThisExpr(pos),(Field)Type.tpClosureClazz.resolveName(nameClosureArgs)),
				new ConstIntExpr(i));
			DeclStat dc = new DeclStat(v.getPos(),body,v);
			if( !v.type.isReference() ) {
				Type celltp = Type.getProxyType(v.type);
				val = new AccessExpr(v.getPos(),dc,
						new CastExpr(v.getPos(),celltp,val,true),
						(Field)celltp.resolveName(nameCellVal)
					);
			} else {
				val = new CastExpr(v.getPos(),v.type,val,true);
			}
			dc.init = val;
			stats.insert(dc,i);
		}

		ExportJavaTop exporter = new ExportJavaTop();
		//exporter.pass1(me);
		//exporter.pass1_1(me);
		//exporter.pass2(me);
		//exporter.pass2_2(me);
		exporter.pass3(me);
		//me.autoProxyMethods();
		//me.resolveFinalFields(false);
		new_closure = new NewClosure(pos,me.type);
		new_closure.parent = parent;
		new_closure = (Expr)new_closure.resolve(reqType);
		setResolved(true);
		return new_closure;
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
    	sb.append("fun (");
		for(int i=0; i < params.length; i++) {
			sb.append(params[i]);
			if( i < params.length-1 ) sb.append(',');
		}
		sb.append(") {...}");
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append("fun").space().append('(');
		for(int i=0; i < params.length; i++) {
			params[i].toJava(dmp);
			if( i < params.length-1 ) dmp.append(',');
		}
		dmp.append(')').space();
		body.toJava(dmp);
		return dmp;
	}
}
