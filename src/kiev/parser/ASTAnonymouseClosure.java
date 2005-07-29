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
import kiev.stdlib.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTAnonymouseClosure.java,v 1.5 1999/01/29 01:22:21 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5 $
 *
 */

@node
public class ASTAnonymouseClosure extends Expr {
    @att public final NArr<ASTNode>		params;
    @att public ASTType						rettype;
    @att public Statement					body;
	@att public Expr						new_closure;

	public ASTAnonymouseClosure() {
	}

	public ASTAnonymouseClosure(int id) {
	}

  	public void set(Token t) {
    	pos = t.getPos();
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTFormalParameter ) {
			params.append((ASTFormalParameter)n);
		}
		else if( n instanceof ASTType ) {
			rettype = (ASTType)n;
		}
		else if( n instanceof Statement ) {
			body = (Statement)n;
		}
		else {
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
		}
    }

	public ASTNode resolve(Type reqType) {
		if( isResolved() ) return new_closure;
		ClazzName clname = ClazzName.fromBytecodeName(
			new KStringBuffer(PassInfo.clazz.name.bytecode_name.len+8)
				.append_fast(PassInfo.clazz.name.bytecode_name)
				.append_fast((byte)'$')
				.append(PassInfo.clazz.anonymouse_inner_counter)
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
		me.super_clazz = Type.tpClosureClazz.type;

		Type[] types = new Type[params.length];
		Var[] vars = new Var[params.length];
		for(int i=0; i < types.length; i++) {
			if( params[i] instanceof Var )
				vars[i] = (Var)params[i];
			else
				vars[i] = ((ASTFormalParameter)params[i]).pass3();
			types[i] = vars[i].type;
		}
		Type ret = rettype.getType();
		me.type = MethodType.newMethodType(me,null,types,ret);

		NArr<ASTNode> members = new NArr<ASTNode>(null, false);
		if( ret != Type.tpRule ) {
			ASTMethodDeclaration md = new ASTMethodDeclaration();
			KString call_name;
			if( ret.isReference() ) md.ident = new ASTIdentifier(pos, KString.from("call_Object"));
			else md.ident = new ASTIdentifier(pos, KString.from("call_"+ret));
			md.modifiers.modifier.add(ASTModifier.modPUBLIC);
			if( ret.isReference() )
				md.rettype = new ASTType(pos, Type.tpObject);
			else
				md.rettype = new ASTType(pos, ret);
			md.body = body;
			md.parent = me;
			members.add(md);
		} else {
			ASTRuleDeclaration md = new ASTRuleDeclaration();
			md.ident = new ASTIdentifier(pos, KString.from("call_rule"));
			md.body = body;
			md.parent = me;
			members.add(md);
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
				new ConstExpr(v.getPos(),new Integer(i)));
			DeclStat dc = new DeclStat(v.getPos(),body,v);
			if( !v.type.isReference() ) {
				Type celltp = Type.getProxyType(v.type);
				val = new AccessExpr(v.getPos(),dc,
						new CastExpr(v.getPos(),celltp,val,true),
						(Field)celltp.clazz.resolveName(nameCellVal)
					);
			} else {
				val = new CastExpr(v.getPos(),v.type,val,true);
			}
			dc.init = val;
			stats.insert(dc,i);
		}

		me = ASTTypeDeclaration.createMembers(me,members);
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
