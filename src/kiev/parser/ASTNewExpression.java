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

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTNewExpression.java,v 1.4.2.1.2.2 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.2.1.2.2 $
 *
 */

@node
@cfnode
public class ASTNewExpression extends Expr {
	@att
	public ASTType					type;
	
	@att
	public final NArr<Expr>			args;
	
    @att
	public final Struct				clazz;
	
	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof Expr ) {
			args.append((Expr)n);
        }
    }

	public ASTNode resolve(Type reqType) {
		// Find out possible constructors
		Type tp = type.getType();
		BaseStruct s = tp.clazz;
		s.checkResolved();
		Type[] targs = Type.emptyArray;
		if( args.length > 0 ) {
			targs = new Type[args.length];
			boolean found = false;
			if (s instanceof Struct) {
				Struct ss = (Struct)s;
				foreach(ASTNode n; ss.members; n instanceof Method) {
					Method m = (Method)n;
					if (!(m.name.equals(nameInit) || m.name.equals(nameNewOp)))
						continue;
					Type[] mtargs = m.type.args;
					int i = 0;
					if( !ss.package_clazz.isPackage() && !ss.isStatic() ) i++;
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
		if( clazz == null )
			return new NewExpr(pos,tp,args.toArray()).resolve(reqType);
		// Local anonymouse class
		Type sup  = tp;
		Struct me = clazz;
		clazz.setResolved(true);
		clazz.setLocal(true);
		clazz.setAnonymouse(true);
		clazz.setStatic(PassInfo.method==null || PassInfo.method.isStatic());
		SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
		clazz.addAttr(sfa);
		if( sup.clazz.isInterface() ) {
			clazz.super_type = Type.tpObject;
			clazz.interfaces.add(new TypeRef(sup));
		} else {
			clazz.super_type = sup;
		}

		if( sup.clazz.instanceOf(Type.tpClosureClazz) ) {
			assert (false);
//			ASTMethodDeclaration md = (ASTMethodDeclaration)members[0];
//			members.delAll();
//			me.type = Type.newRefType(me,Type.emptyArray);
//			Method m = (Method)md.pass3();
//			me.type = MethodType.newMethodType(me,null,m.type.args,m.type.ret);
		} else {
			me.type = Type.newRefType(me,Type.emptyArray);
			// Create default initializer, if number of arguments > 0
			if( args.length > 0 ) {
				MethodType mt;
				Type[] targs = Type.emptyArray;
				NArr<Var> params = new NArr<Var>(null, null);
				params.append(new Var(pos,null,nameThis,me.type,0));
				for(int i=0; i < args.length; i++) {
					args[i] = (Expr)args[i].resolve(null);
					Type at = args[i].getType();
					targs = (Type[])Arrays.append(targs,at);
					params.append(new Var(pos,null,KString.from("arg$"+i),at,0));
				}
				mt = MethodType.newMethodType(MethodType.tpMethodClazz,null,targs,Type.tpVoid);
				Method init = new Method(me,nameInit,mt,ACC_PUBLIC);
				init.params.addAll(params);
				init.pos = pos;
				init.body = new BlockStat(pos,init);
				init.setPublic(true);
				me.addMethod(init);
			}
		}

        // Process inner classes and cases
		ExportJavaTop exporter = new ExportJavaTop();
		exporter.pass1(me);
		exporter.pass1_1(me);
		exporter.pass2(me);
		exporter.pass2_2(me);
		exporter.pass3(me);
		me.autoProxyMethods();
		me.resolveFinalFields(false);
		Expr ne;
		if( sup.clazz.instanceOf(Type.tpClosureClazz) ) {
			ne = new NewClosure(pos,me.type);
			ne.clazz = me;
		} else {
			ne = new NewExpr(pos,me.type,args.toArray());
			ne.clazz = me;
		}
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
		if( clazz != null ) {
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
		if( clazz != null ) {
			dmp.space().append('{').newLine(1);
			for(int j=0; j < clazz.members.length; j++) dmp.append(clazz.members[j]);
			dmp.newLine(-1).append('}').newLine();
		}
		return dmp;
	}
}
