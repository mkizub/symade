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
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface Typed {
	public Type		getType();
}

public interface BreakTarget {
	public Label getBrkLabel();
}

public interface ContinueTarget {
	public Label getCntLabel();
}

public interface Named {
	public NodeName	getName();
}

public final class ParentEnumerator implements Enumeration<ASTNode> {
	ASTNode n, r;
	public ParentEnumerator(ASTNode n) {
		this.n = n;
	}
	public boolean hasMoreElements() {
		return n.parent != null;
	}
	public ASTNode nextElement() {
		if (r == null) {
			r = n.parent;
		} else {
			n = r;
			r = n.parent;
		}
		return r;
	}
}

public class SymbolIterator implements Enumeration<ASTNode> {
	NArr<ASTNode> stats;
	ASTNode last_stat;
	public SymbolIterator(NArr<ASTNode> stats, ASTNode element) {
		this.stats = stats;
		if (element != null && element.pslot == stats.getPSlot()) {
			assert(stats.indexOf(element) >= 0);
			last_stat = element;
		} else {
			if (stats.size() > 0)
				last_stat = stats[stats.size()-1];
		}
	}
	public boolean hasMoreElements() {
		return last_stat != null;
	}
	public ASTNode nextElement() {
		if ( last_stat != null ) {
			ASTNode r = last_stat;
			last_stat = last_stat.pprev;
			return r;
		}
		throw new NoSuchElementException();
	}
	/// BUG BUG BUG ///
	public Object nextElement() {
		if ( last_stat != null ) {
			ASTNode r = last_stat;
			last_stat = last_stat.pprev;
			return r;
		}
		throw new NoSuchElementException();
	}
}

public class PassInfo {

	// No instances
	private PassInfo() {}

	// Pass info and global resolving section
	public static Struct			clazz;
	public static Method			method;
	private static final ASTNode[]	path	= new ASTNode[1024];
	private static int				pathTop = 0;


	public static void pushStruct(Struct node) {
		trace(Kiev.debugAST,AT()+" push '"+node+"'"+debugAt());
        path[pathTop++] = node;
		trace(Kiev.debugAST,AT()+" set clazz  '"+node+"'"+debugAt());
		clazz = node;
		Code.setLinePos(node.getPosLine());
	}
	
	public static void popStruct(Struct node) {
		trace(Kiev.debugAST,AT()+" pop  '"+node+"'"+debugAt());
    	if( node != path[pathTop-1] )
    		throw new RuntimeException("PassInfo push/pop node "+node+" and node "+path[pathTop-1]+" missmatch");
		--pathTop;
		clazz = null;
		for(int i=pathTop-1; i >= 0; i-- ) {
			if( path[i] instanceof Struct ) {
				clazz = (Struct)path[i];
				trace(Kiev.debugAST,AT()+" set clazz  '"+clazz+"'"+debugAt());
				break;
			}
		}
	}
	
	public static void pushMethod(Method node) {
		trace(Kiev.debugAST,AT()+" push '"+node+"'"+debugAt());
        path[pathTop++] = node;
		trace(Kiev.debugAST,AT()+" set method '"+node+"'"+debugAt());
		method = node;
		Code.setLinePos(node.getPosLine());
	}
	
	public static void popMethod(Method node) {
		trace(Kiev.debugAST,AT()+" pop  '"+node+"'"+debugAt());
    	if( node != path[pathTop-1] )
    		throw new RuntimeException("PassInfo push/pop node "+node+" and node "+path[pathTop-1]+" missmatch");
		--pathTop;
		method = null;
		for(int i=pathTop-1; i >= 0; i-- ) {
			if( path[i] instanceof Method ) {
				method = (Method)path[i];
				trace(Kiev.debugAST,AT()+" set method '"+method+"'"+debugAt());
				break;
			}
		}
	}
	
//	public static void push(ASTNode node) {
//		trace(Kiev.debugAST,AT()+" push '"+node+"'"+debugAt());
//		if( node instanceof FileUnit ) {
//			trace(Kiev.debugAST,AT()+" set file unit  '"+node+"'"+debugAt());
//			file_unit = (FileUnit)node;
//			path[pathTop++] = node;
//			Kiev.reportError(node.pos, "Unoptimized "+AT()+" push '"+node.getClass()+"'"+debugAt());
//		}
//		else if( node instanceof Struct ) {
//			trace(Kiev.debugAST,AT()+" set clazz  '"+node+"'"+debugAt());
//			clazz = (Struct)node;
//			path[pathTop++] = node;
//			Kiev.reportError(node.pos, "Unoptimized "+AT()+" push '"+node.getClass()+"'"+debugAt());
//		}
//		else if( node instanceof Method ) {
//			trace(Kiev.debugAST,AT()+" set method '"+node+"'"+debugAt());
//			method = (Method)node;
//			path[pathTop++] = node;
//			Kiev.reportError(node.pos, "Unoptimized "+AT()+" push '"+node.getClass()+"'"+debugAt());
//		}
//		Code.setLinePos(node.getPosLine());
//	}

//	public static void pop(ASTNode n) {
//		trace(Kiev.debugAST,AT()+" pop  '"+n+"'"+debugAt());
////    	if( n != path[pathTop-1] ) {
////    		if( n == path[pathTop-2] )
////    			pop( path[pathTop-1] );
////    		else
////	    		throw new RuntimeException("PassInfo push/pop node "+n+" and node "+path[pathTop-1]+" missmatch");
////    	}
////    	ASTNode node = path[--pathTop];
////    	if( n!=node )
////    		throw new RuntimeException("PassInfo push/pop node "+n+" and node "+node+" missmatch");
////      path[pathTop] = null;
//		if( n instanceof FileUnit ) {
//			ASTNode node = path[pathTop-1];
//			if (n != node)
//				throw new RuntimeException("PassInfo push/pop node "+n+" and node "+node+" missmatch");
//			--pathTop;
//			file_unit = null;
//			Kiev.reportError(node.pos, "Unoptimized "+AT()+" pop '"+node.getClass()+"'"+debugAt());
//		}
//		else if( node instanceof Struct ) {
//			ASTNode node = path[pathTop-1];
//			if (n != node)
//				throw new RuntimeException("PassInfo push/pop node "+n+" and node "+node+" missmatch");
//			--pathTop;
//			clazz = null;
//			for(int i=pathTop-1; i >= 0; i-- ) {
//				if( path[i] instanceof Struct ) {
//					clazz = (Struct)path[i];
//					trace(Kiev.debugAST,AT()+" set clazz  '"+clazz+"'"+debugAt());
//					break;
//				}
//			}
//			Kiev.reportError(node.pos, "Unoptimized "+AT()+" pop '"+node.getClass()+"'"+debugAt());
//		}
//		else if( node instanceof Method ) {
//			ASTNode node = path[pathTop-1];
//			if (n != node)
//				throw new RuntimeException("PassInfo push/pop node "+n+" and node "+node+" missmatch");
//			--pathTop;
//			method = null;
//			for(int i=pathTop-1; i >= 0; i-- ) {
//				if( path[i] instanceof Method ) {
//					method = (Method)path[i];
//					trace(Kiev.debugAST,AT()+" set method '"+method+"'"+debugAt());
//					break;
//				}
//			}
//			Kiev.reportError(node.pos, "Unoptimized "+AT()+" pop '"+node.getClass()+"'"+debugAt());
//		}
//	}

	private static String SP =
	"                                                                                   "+
	"                                                                                   ";
	private static String AT() { return "AST "+pathTop+SP.substring(0,pathTop); }
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

	public static boolean checkClassName(ASTNode from, KString qname) {
		ASTNode@ node;
		if (!resolveNameR(from, node,new ResInfo(from),qname))
			return false;
		if (node instanceof Struct && !node.isPackage())
			return true;
		if (node instanceof TypeRef)
			return true;
		return false;
	}

	public static rule resolveOperatorR(ASTNode from, Operator@ op)
		ASTNode@ p;
	{
		p @= new ParentEnumerator(from),
		p instanceof ScopeOfOperators,
		((ScopeOfOperators)p).resolveOperatorR(op)
	}

	public static rule resolveQualifiedNameR(ASTNode from, ASTNode@ node, ResInfo path, KString name)
		KString@ qname_head;
		KString@ qname_tail;
		ASTNode@ p;
		ParentEnumerator pe;
	{
		trace( Kiev.debugResolve, "PassInfo: resolving name "+name),
		name.indexOf('.') > 0, $cut,
		trace( Kiev.debugResolve, "PassInfo: name '"+name+"' is qualified"),
		qname_head ?= name.substr(0,name.lastIndexOf('.')),
		qname_tail ?= name.substr(name.lastIndexOf('.')+1),
		resolveQualifiedNameR(from,p,path,qname_head),
		p instanceof Struct,
		((Struct)p).resolveNameR(node,path,qname_tail)
	;
		pe = new ParentEnumerator(from),
		p @= pe,
		trace( Kiev.debugResolve, "PassInfo: next parent is '"+p+"' "+p.getClass()),
		p instanceof ScopeOfNames,
		trace( Kiev.debugResolve, "PassInfo: resolving name '"+name+"' in scope '"+p+"'"),
		path.space_prev = pe.n,
		((ScopeOfNames)p).resolveNameR(node,path,name)
	}

	public static rule resolveNameR(ASTNode from, ASTNode@ node, ResInfo path, KString name)
		KString@ qname_head;
		KString@ qname_tail;
		ASTNode@ p;
		ParentEnumerator pe;
	{
		trace( Kiev.debugResolve, "PassInfo: resolving name "+name),
		assert(name.indexOf('.') < 0),
		pe = new ParentEnumerator(from),
		p @= pe,
		trace( Kiev.debugResolve, "PassInfo: next parent is '"+p+"' "+p.getClass()),
		p instanceof ScopeOfNames,
		trace( Kiev.debugResolve, "PassInfo: resolving name '"+name+"' in scope '"+p+"'"),
		path.space_prev = pe.n,
		((ScopeOfNames)p).resolveNameR(node,path,name)
	}

	private static void addResolvedMethod(
		Method m, ResInfo info,
		Vector<Method>  methods, Vector<ResInfo> paths, Vector<MethodType> types)
	{
		trace(Kiev.debugResolve,"Candidate method "+m+" with path "+info+" found...");
		if !(info.check(m))
			return;
		for (int i=0; i < methods.length; i++) {
			if (methods[i] == m) {
				trace(Kiev.debugResolve,"Duplicate methods "+m+" with paths "+info+" and "+paths[i]+" found...");
				if (info.getTransforms() < paths[i].getTransforms()) {
					trace(Kiev.debugResolve,"Will use "+m+" with paths "+info);
					methods[i] = m;
					paths[i] = info.copy();
				}
				return;
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
			MethodType mt1 = MethodType.newMethodType(null,ta,info.mt.ret);
			types.append(mt1);
		}
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
			foreach( scm.resolveMethodR(node,info,name,mt) )
				addResolvedMethod((Method)node,info,methods,paths,types);
		}
		else if (sc instanceof Type && info.isStaticAllowed()) {
			Type tp = (Type)sc;
			foreach( tp.resolveCallStaticR(node,info,name,mt) )
				addResolvedMethod((Method)node,info,methods,paths,types);
		}
		else if (sc instanceof Type) {
			Type tp = (Type)sc;
			foreach( tp.resolveCallAccessR(node,info,name,mt) )
				addResolvedMethod((Method)node,info,methods,paths,types);
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

	public static rule resolveMethodR(ASTNode from, ASTNode@ node, ResInfo path, KString name, MethodType mt)
		KString@ qname_head;
		KString@ qname_tail;
		ASTNode@ p;
		ParentEnumerator pe;
	{
		assert(name.indexOf('.') < 0),
		pe = new ParentEnumerator(from),
		p @= pe,
		p instanceof ScopeOfMethods,
		path.space_prev = pe.n,
		resolveBestMethodR((ScopeOfMethods)p,node,path,name,mt),
		trace(Kiev.debugResolve,"Best method is "+node+" with path/transform "+path+" found...")
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
