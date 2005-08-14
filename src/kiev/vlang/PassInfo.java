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

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/PassInfo.java,v 1.3.2.1.2.2 1999/05/29 21:03:12 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1.2.2 $
 *
 */

public interface Typed {
	public Type		getType();
}

public interface BreakTarget {
	public CodeLabel getBreakLabel();
}

public interface ContinueTarget {
	public CodeLabel getContinueLabel();
}

public interface Named {
	public NodeName	getName();
}

public class PathEnumerator implements Enumeration<ASTNode> {
	public int i;
	public PathEnumerator() { i = PassInfo.pathTop; }
	public boolean hasMoreElements() { return i > 0; }
	public ASTNode nextElement() { return PassInfo.path[--i]; }
}

public class PassInfo {

	// No instances
	private PassInfo() {}

	// Pass info and global resolving section
	public static FileUnit			file_unit;
	public static Struct			clazz;
	public static Method			method;
	public static ASTNode[]			path	= new ASTNode[1024];
	public static int				pathTop = 0;


	public static void push(ASTNode node) {
		trace(Kiev.debugAST,"AST "+pathTop+" push '"+node+"'"+debugAt());
        path[pathTop++] = node;
		if( node instanceof FileUnit ) {
			trace(Kiev.debugAST,"AST set file unit  '"+node+"'"+debugAt());
			file_unit = (FileUnit)node;
		}
		else if( node instanceof Struct ) {
			trace(Kiev.debugAST,"AST set clazz  '"+node+"'"+debugAt());
			clazz = (Struct)node;
		}
		else if( node instanceof Method ) {
			trace(Kiev.debugAST,"AST set method '"+node+"'"+debugAt());
			method = (Method)node;
		}
		Code.setLinePos(node.getPosLine());
//		{
//			if( node instanceof Statement )
//				Code.pushStackPos();
//		}
		if( node instanceof Scope ) {
			trace(Kiev.debugAST,"AST enetred scope '"+node+"'"+debugAt());
			enterScope((Scope)node);
		}
	}

	public static void pop(ASTNode n) {
		trace(Kiev.debugAST,"AST "+pathTop+" pop  '"+n+"'"+debugAt());
    	if( n != path[pathTop-1] ) {
    		if( n == path[pathTop-2] )
    			pop( path[pathTop-1] );
    		else
	    		throw new RuntimeException("PassInfo push/pop node "+n+" and node "+path[pathTop-1]+" missmatch");
    	}
    	ASTNode node = path[--pathTop];
    	if( n!=node )
    		throw new RuntimeException("PassInfo push/pop node "+n+" and node "+node+" missmatch");
        path[pathTop] = null;
		if( node instanceof FileUnit ) {
			file_unit = null;
		}
		else if( node instanceof Struct ) {
			clazz = null;
			for(int i=pathTop-1; i >= 0; i-- ) {
				if( path[i] instanceof Struct ) {
					clazz = (Struct)path[i];
					trace(Kiev.debugAST,"AST set clazz  '"+clazz+"'"+debugAt());
					break;
				}
			}
		}
		else if( node instanceof Method ) {
			method = null;
			for(int i=pathTop-1; i >= 0; i-- ) {
				if( path[i] instanceof Method ) {
					method = (Method)path[i];
					trace(Kiev.debugAST,"AST set method '"+method+"'"+debugAt());
					break;
				}
			}
		}
//		if( genPass && code != null ) {
//			if( node instanceof Statement )
//				code.popStackPos();
//		}
		if( node instanceof Scope ) {
			trace(Kiev.debugAST,"AST leaved scope '"+node+"'"+debugAt());
			leaveScope((Scope)node);
		}
	}

	private static String debugAt() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
		new Exception().printStackTrace(new PrintStream(bos));
		byte[] msg = bos.toByteArray();
		int from;
		int to;
		int cnt = 0;
		for(from=0; from < msg.length; from++) {
			if( msg[from] == '\n' ) {
				from++; cnt++;
				if( ++cnt >= 5 ) break;
			}
		}
		for(to=from; to < msg.length; to++)
			if( msg[to] == '\n' ) break;
		to--;
		return new String(msg,0,from,to-from);
	}

	public static void enterScope(Scope sc) {
		trace(Kiev.debugResolve,"Entered into scope "+sc);
	}

	public static void leaveScope(Scope scope) {
		trace(Kiev.debugResolve,"Leave scope "+scope);
	}

	public static boolean checkClassName(KString qname) {
		ASTNode@ node;
		if (!resolveNameR(node,new ResInfo(),qname))
			return false;
		if (node instanceof BaseStruct && !node.isPackage())
			return true;
		if (node instanceof TypeRef)
			return true;
		return false;
	}

	public static rule resolveOperatorR(Operator@ op)
		ASTNode@ p;
	{
		p @= new PathEnumerator(),
		p instanceof ScopeOfOperators,
		((ScopeOfOperators)p).resolveOperatorR(op)
	}

	public static rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
		KString@ qname_head;
		KString@ qname_tail;
		ASTNode@ p;
	{
		trace( Kiev.debugResolve, "PassInfo: resolving name "+name),
		name.indexOf('.') > 0, $cut,
		trace( Kiev.debugResolve, "PassInfo: name '"+name+"' is qualified"),
		qname_head ?= name.substr(0,name.lastIndexOf('.')),
		qname_tail ?= name.substr(name.lastIndexOf('.')+1),
		resolveNameR(p,path,qname_head),
		p instanceof Struct,
		((Struct)p).resolveNameR(node,path,qname_tail)
	;
		p @= new PathEnumerator(),
		p instanceof ScopeOfNames,
		trace( Kiev.debugResolve, "PassInfo: resolving name '"+name+"' in scope '"+p+"'"),
		((ScopeOfNames)p).resolveNameR(node,path,name)
	}

	public static boolean resolveBestMethodR(
		Object sc,
		ASTNode@ node,
		ResInfo info,
		KString name,
		MethodType mt)
	{
		trace(Kiev.debugResolve,"Resolving best method "+Method.toString(name,mt)+" in "+sc);
		Vector<Method>  methods  = new Vector<Method>();
		Vector<ResInfo> paths    = new Vector<ResInfo>();
		Vector<MethodType> types = new Vector<MethodType>();
		if (sc instanceof ScopeOfMethods) {
			ScopeOfMethods scm = (ScopeOfMethods)sc;
		search_next_in_scope:
			foreach( scm.resolveMethodR(node,info,name,mt) ) {
				trace(Kiev.debugResolve,"Candidate method "+node+" with path "+info+" found...");
				if (node.isPrivate() && clazz != (Struct)node.parent)
					continue;
				Method m = (Method)node;
				for (int i=0; i < methods.length; i++) {
					if (methods[i] == m) {
						trace(Kiev.debugResolve,"Duplicate methods "+m+" with paths "+info+" and "+paths[i]+" found...");
						if (info.getTransforms() < paths[i].getTransforms()) {
							trace(Kiev.debugResolve,"Will use "+m+" with paths "+info);
							methods[i] = m;
							paths[i] = info.copy();
						}
						continue search_next_in_scope;
					}
				}
				methods.append(m);
				paths.append(info.copy());
				if (!m.isRuleMethod()) {
					types.append(info.mt);
				} else {
					Type[] ta = new Type[info.mt.args.length-1];
					for (int i=0; i < ta.length; i++)
						ta[i] = info.mt.args[i+1];
					MethodType mt1 = MethodType.newMethodType(null,ta,mt.ret);
					types.append(mt1);
				}
			}
		}
		else if (sc instanceof Type) {
			Type tp = (Type)sc;
		search_next_in_type:
			foreach( tp.resolveCallAccessR(node,info,name,mt) ) {
				trace(Kiev.debugResolve,"Candidate method "+node+" with path "+info+" found...");
				if (node.isPrivate() && clazz != (Struct)node.parent)
					continue;
				Method m = (Method)node;
				for (int i=0; i < methods.length; i++) {
					if (methods[i] == m) {
						trace(Kiev.debugResolve,"Duplicate methods "+m+" with paths "+info+" and "+paths[i]+" found...");
						if (info.getTransforms() < paths[i].getTransforms()) {
							trace(Kiev.debugResolve,"Will use "+m+" with paths "+info);
							methods[i] = m;
							paths[i] = info.copy();
						}
						continue search_next_in_type;
					}
				}
				methods.append(m);
				paths.append(info.copy());
				if (!m.isRuleMethod()) {
					types.append(info.mt);
				} else {
					Type[] ta = new Type[info.mt.args.length-1];
					for (int i=0; i < ta.length; i++)
						ta[i] = info.mt.args[i+1];
					MethodType mt1 = MethodType.newMethodType(null,ta,mt.ret);
					types.append(mt1);
				}
			}
		}
		else
			throw new RuntimeException("Unknown scope "+sc);
		if( methods.size() == 0 ) {
			trace(Kiev.debugResolve,"Nothing found...");
			return false;
		}

		if (Kiev.debugResolve) {
			StringBuffer msg = new StringBuffer("Found "+methods.length+" candidate methods:\n");
			for(int i=0; i < methods.length; i++) {
				msg.append("\t").append(methods[i].parent).append('.').append(paths[i]).append(methods[i]).append('\n');
			}
			msg.append("while resolving ").append(Method.toString(name,mt));
			trace(Kiev.debugResolve,msg.toString());
		}

		if( methods.size() == 1 ) {
			node = methods[0];
			info.set(paths[0]);
			return true;
		}
		
		for (int i=0; i < methods.length; i++) {
			Method m1 = methods[i];
			ResInfo p1 = paths[i];
			MethodType mt1 = types[i];
		next_method:
			for (int j=0; j < methods.length; j++) {
				Method m2 = methods[j];
				ResInfo p2 = paths[j];
				MethodType mt2 = types[j];
				
				if (m1 == m2)
					continue;
				
				trace(Kiev.debugResolve,"Compare "+m1+" and "+m2+" to be more specific for "+Method.toString(name,mt));

				Type b;
				int m1_arg_offs = m1.isRuleMethod() ? 1 : 0;
				int m2_arg_offs = m2.isRuleMethod() ? 1 : 0;
				
				if (p1.getTransforms() > p2.getTransforms()) {
					trace(Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because of path");
					continue next_method;
				}
				for (int k=0; k < mt.args.length; k++) {
					if (mt1.args[k] != mt2.args[k]) {
						b = mt.args[k].betterCast(mt1.args[k],mt2.args[k]);
						if (b == mt2.args[k]) {
							trace(Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because arg "+k);
							continue next_method;
						}
						if (b == null && !mt1.args[k].isInstanceOf(mt2.args[k])) {
							trace(Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because arg "+k);
							continue next_method;
						}
					}
				}
				if (mt1.ret != mt2.ret) {
					b = mt.ret.betterCast(mt1.ret,mt2.ret);
					if (b == mt2.ret) {
						trace(Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because ret");
						continue next_method;
					}
					if (b == null && mt2.ret.isInstanceOf(mt1.ret)) {
						trace(Kiev.debugResolve,"Method "+m1+" has less specific return value, then "+m2);
						continue next_method;
					}
				}
				
				trace(Kiev.debugResolve,"Methods "+m1+" is more specific then "+m2+" resolving for "+Method.toString(name,mt));
				methods.remove(j);
				paths.remove(j);
				types.remove(j);
				j--;
				if (i >= j)
					i--;
			}
		}
		if( methods.size() == 1 ) {
			node = methods[0];
			info.set(paths[0]);
			return true;
		}
		
//		// Check that all methods in list are multimethods
//		// and all path-es are the same
//		boolean all_multi = true;
//		Method m_multi = lm.head();
//		ResInfo p_multi = lp.head();
//		for(; lm != List.Nil; lm = lm.tail(), lp = lp.tail()) {
//			if( !lm.head().isMultiMethod() ) { all_multi=false; break; }
//			if( !lp.head().equals(p_multi) ) { all_multi=false; break; }
//			if (!lm.head().isPrivate()) {
//				m_multi = lm.head();
//				p_multi = lp.head();
//			}
//		}
//		if( all_multi ) {
//			node = m_multi;
//			info.set(p_multi);
//			return true;
//		}
		StringBuffer msg = new StringBuffer("Umbigous methods:\n");
		for(int i=0; i < methods.length; i++) {
			msg.append("\t").append(methods[i].parent).append('.');
			msg.append(paths[i]).append(methods[i]).append('\n');
		}
		msg.append("while resolving ").append(Method.toString(name,mt));
		throw new RuntimeException(msg.toString());
	}

	public static rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		KString@ qname_head;
		KString@ qname_tail;
		ASTNode@ p;
	{
		name.indexOf('.') > 0, $cut,
		qname_head ?= name.substr(0,name.lastIndexOf('.')),
		qname_tail ?= name.substr(name.lastIndexOf('.')+1),
		resolveNameR(p,new ResInfo(),qname_head),
		p instanceof Struct,
		((Struct)p).resolveMethodR(node, info, qname_tail, mt)
	;
		p @= new PathEnumerator(), p instanceof ScopeOfMethods,
		resolveBestMethodR((ScopeOfMethods)p,node,info,name,mt),
		trace(Kiev.debugResolve,"Best method is "+node+" with path/transform "+info+" found...")
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	public static Object[] resolveContinueLabel(KString name) {
		Object[] cl = new Object[0];
		if( name == null || name.equals(KString.Empty) ) {
			// Search for loop statements
			for(int i=pathTop-1; i >= 0; i-- ) {
				if( path[i] instanceof TryStat ) {
					TryStat ts = (TryStat)path[i];
					if( ts.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,((FinallyInfo)ts.finally_catcher).subr_label);
				}
				else if( path[i] instanceof SynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,((SynchronizedStat)path[i]).expr_var);
				}
				if( path[i] instanceof Method ) break;
				if( ! (path[i] instanceof ContinueTarget) ) continue;
				ContinueTarget t = (ContinueTarget)path[i];
				return (Object[])Arrays.append(cl,t.getContinueLabel());
			}
			throw new RuntimeException("Continue not within loop statement");
		} else {
			// Search for labels with loop statement
			for(int i=pathTop-1; i >= 0; i-- ) {
				if( path[i] instanceof TryStat ) {
					TryStat ts = (TryStat)path[i];
					if( ts.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,((FinallyInfo)ts.finally_catcher).subr_label);
				}
				else if( path[i] instanceof SynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,((SynchronizedStat)path[i]).expr_var);
				}
				if( path[i] instanceof Method ) break;
				if( path[i] instanceof LabeledStat && ((LabeledStat)path[i]).getName().equals(name) ) {
					Statement st = ((LabeledStat)path[i]).stat;
					if( ! (st instanceof ContinueTarget) )
						throw new RuntimeException("Label "+name+" does not refer to continue target");
					ContinueTarget t = (ContinueTarget)st;
					return (Object[])Arrays.append(cl,t.getContinueLabel());
				}
			}
		}
		throw new RuntimeException("Label "+name+" unresolved or isn't a continue target");
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	public static Object[] resolveBreakLabel(KString name) {
		Object[] cl = new Object[0];
		if( name == null || name.equals(KString.Empty) ) {
			// Search for loop/switch statements
			for(int i=pathTop-1; i >= 0; i-- ) {
				if( path[i] instanceof TryStat ) {
					TryStat ts = (TryStat)path[i];
					if( ts.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,((FinallyInfo)ts.finally_catcher).subr_label);
				}
				else if( path[i] instanceof SynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,((SynchronizedStat)path[i]).expr_var);
				}
				if( path[i] instanceof Method ) break;
				if( path[i] instanceof BreakTarget || path[i] instanceof BlockStat );
				else continue;
				if( path[i] instanceof BreakTarget ) {
					BreakTarget t = (BreakTarget)path[i];
					return (Object[])Arrays.append(cl,t.getBreakLabel());
				}
				else if( path[i] instanceof BlockStat && path[i].isBreakTarget() ){
					BlockStat t = (BlockStat)path[i];
					return (Object[])Arrays.append(cl,t.getBreakLabel());
				}
			}
			for(int i=pathTop-1; i >= 0; i-- ) {
				if( path[i] instanceof BreakTarget || path[i] instanceof BreakStat );
				else continue;
				System.out.println(path[i]+" is break target "+path[i].isBreakTarget());
			}
			throw new RuntimeException("Break not within loop statement");
		} else {
			// Search for labels with loop/switch statement
			for(int i=pathTop-1; i >= 0; i-- ) {
				if( path[i] instanceof TryStat ) {
					TryStat ts = (TryStat)path[i];
					if( ts.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,((FinallyInfo)ts.finally_catcher).subr_label);
				}
				else if( path[i] instanceof SynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,((SynchronizedStat)path[i]).expr_var);
				}
				if( path[i] instanceof Method ) break;
				if( path[i] instanceof LabeledStat && ((LabeledStat)path[i]).getName().equals(name) ) {
					Statement st = ((LabeledStat)path[i]).stat;
					if( ! (st instanceof BreakTarget || st instanceof BlockStat) )
						throw new RuntimeException("Label "+name+" does not refer to break target");
					if( st instanceof BreakTarget )
						return (Object[])Arrays.append(cl,((BreakTarget)st).getBreakLabel());
					else
						return (Object[])Arrays.append(cl,((BlockStat)st).getBreakLabel());
				}
			}
		}
		throw new RuntimeException("Label "+name+" unresolved or isn't a break target");
	}

	public static boolean checkException(Type exc) throws RuntimeException {
		if( !exc.isInstanceOf(Type.tpThrowable) )
			throw new RuntimeException("A class of object for throw statement must be a subclass of "+Type.tpThrowable+" but type "+exc+" found");
		if( exc.isInstanceOf(Type.tpError) || exc.isInstanceOf(Type.tpRuntimeException) ) return true;
		for(int i=pathTop-1; i >= 0; i-- ) {
			if( path[i] instanceof TryStat ) {
				TryStat trySt = (TryStat)path[i];
				for(int j=0; j < trySt.catchers.length; j++)
					if( exc.isInstanceOf(((CatchInfo)trySt.catchers[j]).arg.type) ) return true;
			}
			else if( path[i] instanceof Method ) {
				Method m = (Method)path[i];
                ExceptionsAttr a = (ExceptionsAttr)m.getAttr(Constants.attrExceptions);
				for(int j=0; a!=null && j < a.exceptions.length; j++)
					if( exc.isInstanceOf(a.exceptions[j]) ) return true;
				break;
			}
		}
		return false;
	}
}
