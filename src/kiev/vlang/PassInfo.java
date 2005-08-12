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
		if (!resolveNameR(node,new ResInfo(),qname,null))
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

	public static rule resolveNameR(ASTNode@ node, ResInfo path, KString name, Type tp)
		KString@ qname_head;
		KString@ qname_tail;
		ASTNode@ p;
	{
		trace( Kiev.debugResolve, "PassInfo: resolving name "+name),
		name.indexOf('.') > 0, $cut,
		trace( Kiev.debugResolve, "PassInfo: name '"+name+"' is qualified"),
		qname_head ?= name.substr(0,name.lastIndexOf('.')),
		qname_tail ?= name.substr(name.lastIndexOf('.')+1),
		resolveNameR(p,path,qname_head,tp),
		p instanceof Struct,
		((Struct)p).resolveNameR(node,path,qname_tail,tp)
	;
		p @= new PathEnumerator(),
		p instanceof ScopeOfNames,
		trace( Kiev.debugResolve, "PassInfo: resolving name '"+name+"' in scope '"+p+"'"),
		((ScopeOfNames)p).resolveNameR(node,path,name,tp)
	}

//	static boolean checkResolvedPathForName(ASTNode node, ResInfo info) {
//		assert(node != null);
//		// Vars will be auto-wrapped in Ref<...> if needed
//		if( node instanceof Var ) return true;
//		// Structures/types/typedefs do not need path
//		if( node instanceof Struct || node instanceof Type || node instanceof Typedef) {
//			assert (info.isEmpty());
//			return true;
//		}
//		// Check field
//		if( node instanceof Field || node instanceof Method ) {
//			trace( Kiev.debugResolve, "check path for "+node+" to access from "+PassInfo.clazz+" to "+node.parent);
//			assert( node.parent instanceof Struct );
//			if( node.isStatic() ) {
//				assert (info.isEmpty());
//				trace( Kiev.debugResolve, "path for static "+node+" trunkated");
//				return true;
//			}
//			if( PassInfo.clazz.instanceOf((Struct)node.parent) ) {
//				assert (info.isEmpty());
//				trace( Kiev.debugResolve, "node's parent "+node.parent+" is the current class "+PassInfo.clazz);
//				return true;
//			}
//			BaseStruct s = PassInfo.clazz;
//			// Check that path != List.Nil
//			if( info.isEmpty() ) {
//				trace( Kiev.debugResolve, "empty path - need to fill with this$N");
//				// If inner clazz is static - fail
//				if( PassInfo.clazz.isStatic() ) {
//					trace( Kiev.debugResolve, "filling of path is failed - class "+PassInfo.clazz+" is static");
////					throw new RuntimeException("Access to non-static "+node+" from staic inner class "+PassInfo.clazz);
//					return false;
//				}
//				s = PassInfo.clazz;
//			} else {
//				s = info.path.getAt(0).getType().clazz;
//				if( s.instanceOf((Struct)node.parent) ) {
//					trace( Kiev.debugResolve, "valid path - "+path);
//					return true;
//				}
//			}
//			assert( s instanceof Struct );
//			// Fill path as this$0 if possible
//		fill_path:
//			for(;;) {
//				foreach(ASTNode n; ((Struct)s).members; n instanceof Field && ((Field)n).name.name.startsWith(Constants.nameThisDollar) ) {
//					Field f = (Field)n;
//					trace( Kiev.debugResolve, "Add "+f+" to path for node "+node);
//					info.path.prepend(f);
//					// Check we've finished
//					if( f.type.clazz.instanceOf((Struct)node.parent)) return true;
//					s = f.type.clazz;
//					continue fill_path;
//				}
//				trace( Kiev.debugResolve, "Can't find this$N in class "+s+" to fill path");
//				return false;
//			}
//		}
//		throw new RuntimeException("Unknown node of type "+node.getClass());
//	}

	public static boolean resolveBestMethodR(
		ScopeOfMethods sc,
		ASTNode@ node,
		ResInfo info,
		KString name,
		Expr[] args,
		Type ret,
		Type type)
	{
		trace(Kiev.debugResolve,"Resolving best method "+Method.toString(name,args)+" in "+sc+" for base type "+type);
		List<Method> lm = List.Nil;
		List<ResInfo> lp = List.Nil;
		foreach( sc.resolveMethodR(node,info,name,args,ret,type) ) {
			trace(Kiev.debugResolve,"Candidate method "+node+" with path "+info+" found...");
			if (node.isPrivate() && clazz != (Struct)node.parent)
				continue;
			lm = lm.concat((Method)node);
			lp = lp.concat(info.copy());
		}
		if( lm == List.Nil ) {
			trace(Kiev.debugResolve,"Nothing found...");
			return false;
		}
		if( lm.tail() == List.Nil ) {
			node = lm.head();
			info.set(lp.head());
			return true;
		}
		List<Method> lm1 = lm;
		List<ResInfo> lp1 = lp;
	next_method:
		for(; lm1 != List.Nil; lm1 = lm1.tail(), lp1 = lp1.tail()) {
			Method m1 = lm1.head();
			ResInfo p1 = lp1.head();

			List<Method> lm2 = lm;
			List<ResInfo> lp2 = lp;

			for(; lm2 != List.Nil; lm2 = lm2.tail(), lp2 = lp2.tail()) {
				Method m2 = lm2.head();
				if( m1 == m2 ) continue;
				ResInfo p2 = lp2.head();

				boolean m1_is_better = true;
				boolean m2_is_better = true;
				int m1_offs = (m1 instanceof RuleMethod )? 1:0;
				int m2_offs = (m2 instanceof RuleMethod )? 1:0;
				// Select better method
				for(int i=0; i < args.length; i++) {
					Type t = Type.getRealType(type,args[i].getType());
					Type t1 = Type.getRealType(type,m1.type.args[i+m1_offs]);
					Type t2 = Type.getRealType(type,m2.type.args[i+m2_offs]);
					if( t1 == t2 ) continue;
					Type t_better = t.betterCast(t1,t2);
					trace(Kiev.debugResolve,"better cast for arg"+i+" "+t+" between "+t1+" and "+t2+" is "+t_better);
					if( t_better != t1 )	m1_is_better = false;
					if( t_better != t2 )	m2_is_better = false;
				}
				//{
				//	Type r = ret;
				//	if (r == null) r = Type.tpVoid;
				//	Type t = Type.getRealType(type,r);
				//	Type t1 = Type.getRealType(type,m1.type.ret);
				//	Type t2 = Type.getRealType(type,m2.type.ret);
				//	if( t1 != t2 ) {
				//		Type t_better = t.betterCast(t1,t2);
				//		trace(Kiev.debugResolve,"better cast for ret "+t+" between "+t1+" and "+t2+" is "+t_better);
				//		if( r == Type.tpVoid && t_better == null ) {	// Equals
				//			if( t2.isInstanceOf(t1) ) m2_is_better = false;
				//			if( t1.isInstanceOf(t1) ) m1_is_better = false;
				//		} else {
				//			if( t_better != t1 )	m1_is_better = false;
				//			if( t_better != t2 )	m2_is_better = false;
				//		}
				//	}
				//}
				if( m1_is_better && m2_is_better ) {	// Equals
					Type t1 = Type.getRealType(type,m1.type.ret);
					Type t2 = Type.getRealType(type,m2.type.ret);
					if ( !t1.equals(t2) ) {
						if (t1.isInstanceOf(t2)) m2_is_better = false;
						if (t2.isInstanceOf(t1)) m1_is_better = false;
					}
				}
				if( m1_is_better && m2_is_better ) {	// Equals
					if( m1.parent != m2.parent ) {
						if( m1.parent == sc ) continue;
						if( m2.parent == sc ) continue next_method;
						if( ((Struct)m1.parent).instanceOf((Struct)m2.parent) ) continue;
					}
					if( p1.getTransforms() < p2.getTransforms() ) continue;
					if( p1.getTransforms() > p2.getTransforms() ) continue next_method;
					continue next_method; // Totally equals
				}
				else if( m1_is_better && !m2_is_better ) continue;
				continue next_method;
			}
			// Is better than all in list
			node = m1;
			info.set(p1);
			return true;
		}
		// Check that all methods in list are multimethods
		// and all path-es are the same
		boolean all_multi = true;
		Method m_multi = lm.head();
		ResInfo p_multi = lp.head();
		for(; lm != List.Nil; lm = lm.tail(), lp = lp.tail()) {
			if( !lm.head().isMultiMethod() ) { all_multi=false; break; }
			if( !lp.head().equals(p_multi) ) { all_multi=false; break; }
			if (!lm.head().isPrivate()) {
				m_multi = lm.head();
				p_multi = lp.head();
			}
		}
		if( all_multi ) {
			node = m_multi;
			info.set(p_multi);
			return true;
		}
		StringBuffer msg = new StringBuffer("Umbigous methods:\n");
		for(; lm != List.Nil; lm = lm.tail(), lp = lp.tail()) {
			msg.append("\t").append(lm.head().parent).append('.');
			msg.append(lp.head()).append(lm.head()).append('\n');
		}
		msg.append("while resolving ").append(Method.toString(name,args,ret));
		throw new RuntimeException(msg.toString());
	}

	public static rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, Expr[] args, Type ret, Type type)
		KString@ qname_head;
		KString@ qname_tail;
		ASTNode@ p;
	{
		name.indexOf('.') > 0, $cut,
		qname_head ?= name.substr(0,name.lastIndexOf('.')),
		qname_tail ?= name.substr(name.lastIndexOf('.')+1),
		resolveNameR(p,null,qname_head,type),
		p instanceof Struct,
		((Struct)p).resolveMethodR(node, info, qname_tail, args, ret, type)
	;
		p @= new PathEnumerator(), p instanceof ScopeOfMethods,
		resolveBestMethodR((ScopeOfMethods)p,node,info,name,args,ret,type),
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
