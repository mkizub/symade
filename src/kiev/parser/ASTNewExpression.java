/* Generated By:JJTree: Do not edit this line. ASTNewExpression.java */

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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTNewExpression.java,v 1.4.2.1.2.2 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.2.1.2.2 $
 *
 */

@node
public class ASTNewExpression extends Expr {
	public ASTNode	type;
    public Expr[]	args = Expr.emptyArray;
    public ASTNode	members[] = ASTNode.emptyArray;
    public boolean	anonymouse;

	public ASTNewExpression(int id) {
		super(0);
	}

	public void jjtAddChild(ASTNode n, int i) {
    	if(i==0) {
			type=n;
			pos = n.getPos();
		}
		else if( n instanceof Expr ) {
			args = (Expr[])Arrays.append(args,n);
        }
        else {
			members = (ASTNode[])Arrays.append(members,n);
        }
    }

	public ASTNode resolve(Type reqType) {
		// Find out possible constructors
		Type tp = ((ASTNonArrayType)type).getType();
		Struct s = tp.clazz;
		s.checkResolved();
		Type[] targs = Type.emptyArray;
		if( args.length > 0 ) {
			targs = new Type[args.length];
			boolean found = false;
			foreach(Method m; s.methods; m.name.equals(nameInit) || m.name.equals(nameNewOp)) {
				Type[] mtargs = m.type.args;
				int i = 0;
				if( !s.package_clazz.isPackage() && !s.isStatic() ) i++;
				if( mtargs.length > i && mtargs[i].isInstanceOf(Type.tpTypeInfo) ) i++;
				if( mtargs.length-i != args.length )
					continue;
				found = true;
				for(int j=0; i < mtargs.length; i++,j++) {
					if( targs[j] == null )
						targs[j] = Type.getRealType(tp,mtargs[i]);
					else if( targs[j] == Type.tpVoid )
						;
					else if( targs[j] != Type.getRealType(tp,mtargs[i]) )
						targs[j] = Type.tpVoid;
				}
			}
			if( !found )
				throw new CompilerException(pos,"Class "+s+" do not have constructors with "+args.length+" arguments");
		}
		for(int i=0; i < args.length; i++) {
			try {
				if( targs[i] != Type.tpVoid )
					args[i] = args[i].resolveExpr(targs[i]);
				else
					args[i] = (Expr)args[i].resolve(null);
			} catch(Exception e) {
				Kiev.reportError(pos,e);
			}
		}
		if( members.length == 0 && !anonymouse )
			return new NewExpr(pos,tp,args).resolve(reqType);
		// Local anonymouse class
		Type sup;
		if( type instanceof Type ) sup = (Type)type;
		else sup = ((ASTNonArrayType)type).getType();
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
		me.setStatic(PassInfo.method==null || PassInfo.method.isStatic());
		SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
		me.addAttr(sfa);
		if( sup.clazz.isInterface() ) {
			me.super_clazz = Type.tpObject;
			me.interfaces = new Type[]{sup};
		} else {
			me.super_clazz = sup;
		}
		me.parent = this;

		for(int i=0; i < members.length; i++) {
			members[i].parent = me;
		}

		if( sup.clazz.instanceOf(Type.tpClosureClazz) ) {
			ASTMethodDeclaration md = (ASTMethodDeclaration)members[0];
			members = ASTNode.emptyArray;
			me.type = Type.newRefType(me,Type.emptyArray);
			Method m = (Method)md.pass3();
			me.type = MethodType.newMethodType(me,null,m.type.args,m.type.ret);
		} else {
			me.type = Type.newRefType(me,Type.emptyArray);
			// Create default initializer, if number of arguments > 0
			if( args.length > 0 ) {
				MethodType mt;
				Type[] targs = Type.emptyArray;
				Var[] params = new Var[]{new Var(pos,null,nameThis,me.type,0)};
				for(int i=0; i < args.length; i++) {
					args[i] = (Expr)args[i].resolve(null);
					Type at = args[i].getType();
					targs = (Type[])Arrays.append(targs,at);
					params = (Var[])Arrays.append(params,new Var(pos,null,KString.from("arg$"+i),at,0));
				}
				mt = MethodType.newMethodType(MethodType.tpMethodClazz,null,targs,Type.tpVoid);
				Method init = new Method(me,nameInit,mt,ACC_PUBLIC);
				init.params = params;
				foreach(Var v; params) v.parent = init;
				init.pos = pos;
				init.body = new BlockStat(pos,init);
				init.setPublic(true);
				me.addMethod(init);
			}
		}

        // Process inner classes and cases
        PassInfo.push(me);
        try {
			ExportJavaTop exporter = new ExportJavaTop();
			for(int i=0; i < members.length; i++) {
				exporter.pass1(members[i], me);
				exporter.pass1_1(members[i], me);
				exporter.pass2(members[i], me);
				exporter.pass2_2(members[i], me);
			}
		} finally { PassInfo.pop(me); }
		me = ASTTypeDeclaration.createMembers(me,members);
		me.autoProxyMethods();
		me.resolveFinalFields(false);
		Expr ne;
		if( sup.clazz.instanceOf(Type.tpClosureClazz) ) {
			ne = new NewClosure(pos,me.type);
		} else {
			ne = new NewExpr(pos,me.type,args);
		}
		me.parent = ne;
		ne.parent = parent;
		return ne.resolve(reqType);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
    	sb.append(" new ").append(type).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		if( members.length > 0 ) {
			sb.append("{ ... }");
		}
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append("new").space().append(type).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		dmp.append(')');
		if( members.length > 0 ) {
			dmp.space().append('{').newLine(1);
			for(int j=0; j < members.length; j++) dmp.append(members[j]);
			dmp.newLine(-1).append('}').newLine();
		}
		return dmp;
	}
}
