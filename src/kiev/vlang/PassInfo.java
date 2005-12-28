package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import java.io.*;

import kiev.be.java.JLabelView;

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
	public JLabelView getBrkLabel();
}

public interface ContinueTarget {
	public JLabelView getCntLabel();
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

	public static boolean checkClassName(ASTNode from, KString qname) {
		DNode@ node;
		if (!resolveNameR(from, node,new ResInfo(from),qname))
			return false;
		if (node instanceof TypeDef)
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

	public static rule resolveQualifiedNameR(ASTNode from, DNode@ node, ResInfo path, KString name)
		KString@ qname_head;
		KString@ qname_tail;
		ASTNode@ p;
		DNode@ sp;
		ParentEnumerator pe;
	{
		trace( Kiev.debugResolve, "PassInfo: resolving name "+name),
		name.indexOf('.') > 0, $cut,
		trace( Kiev.debugResolve, "PassInfo: name '"+name+"' is qualified"),
		qname_head ?= name.substr(0,name.lastIndexOf('.')),
		qname_tail ?= name.substr(name.lastIndexOf('.')+1),
		resolveQualifiedNameR(from,sp,path,qname_head),
		sp instanceof Struct,
		((Struct)sp).resolveNameR(node,path,qname_tail)
	;
		pe = new ParentEnumerator(from),
		p @= pe,
//		trace( Kiev.debugResolve, "PassInfo: next parent is '"+p+"' "+p.getClass()),
		p instanceof ScopeOfNames,
		trace( Kiev.debugResolve, "PassInfo: resolving name '"+name+"' in scope '"+p+"'"),
		path.space_prev = pe.n,
		((ScopeOfNames)p).resolveNameR(node,path,name)
	}

	public static rule resolveNameR(ASTNode from, DNode@ node, ResInfo path, KString name)
		KString@ qname_head;
		KString@ qname_tail;
		ASTNode@ p;
		ParentEnumerator pe;
	{
		trace( Kiev.debugResolve, "PassInfo: resolving name "+name),
		assert(name.indexOf('.') < 0),
		pe = new ParentEnumerator(from),
		p @= pe,
//		trace( Kiev.debugResolve, "PassInfo: next parent is '"+p+"' "+p.getClass()),
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
		types.append(info.mt);
	}
	
	public static boolean resolveBestMethodR(
		Object sc,
		DNode@ node,
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
				
				if (p1.getTransforms() > p2.getTransforms()) {
					trace(Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because of path");
					continue next_method;
				}
				if (m1.isVarArgs()) {
					if (m1.type.args.length < m2.type.args.length) {
						trace(Kiev.debugResolve,"Method "+m1+" is less specific because of arity then "+m2);
						continue next_method;
					}
				}
				if (m2.isVarArgs()) {
					if (m2.type.args.length < m1.type.args.length) {
						trace(Kiev.debugResolve,"Method "+m1+" is more specific because of arity then "+m2);
						goto is_more_specific;
					}
				}
				for (int k=0; k < mt.args.length; k++) {
					if (mt1.args[k] ≉ mt2.args[k]) {
						b = mt.args[k].betterCast(mt1.args[k],mt2.args[k]);
						if (b ≡ mt2.args[k]) {
							trace(Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because arg "+k);
							continue next_method;
						}
						if (b ≡ null && mt1.args[k] ≥ mt2.args[k]) {
							trace(Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because arg "+k);
							continue next_method;
						}
					}
				}
				if (mt1.ret ≉ mt2.ret) {
					b = mt.ret.betterCast(mt1.ret,mt2.ret);
					if (b ≡ mt2.ret) {
						trace(Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because ret");
						continue next_method;
					}
					if (b ≡ null && mt2.ret ≥ mt1.ret) {
						trace(Kiev.debugResolve,"Method "+m1+" has less specific return value, then "+m2);
						continue next_method;
					}
				}
			is_more_specific:;
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
		
		StringBuffer msg = new StringBuffer("Umbigous methods:\n");
		for(int i=0; i < methods.length; i++) {
			msg.append("\t").append(methods[i].parent).append('.');
			msg.append(paths[i]).append(methods[i]).append('\n');
		}
		msg.append("while resolving ").append(Method.toString(name,mt));
		throw new RuntimeException(msg.toString());
	}

	public static rule resolveMethodR(ASTNode from, DNode@ node, ResInfo path, KString name, MethodType mt)
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

	public static boolean checkException(ASTNode from, Type exc) throws RuntimeException {
		if( !exc.isInstanceOf(Type.tpThrowable) )
			throw new CompilerException(from,"A class of object for throw statement must be a subclass of "+Type.tpThrowable+" but type "+exc+" found");
		if( exc.isInstanceOf(Type.tpError) || exc.isInstanceOf(Type.tpRuntimeException) ) return true;
		for (; from != null; from = from.parent) {
			if( from instanceof TryStat ) {
				TryStat trySt = (TryStat)from;
				for(int j=0; j < trySt.catchers.length; j++)
					if( exc.isInstanceOf(trySt.catchers[j].arg.type) ) return true;
			}
			else if( from instanceof Method ) {
				MetaThrows throwns = from.getMetaThrows();
				if( throwns == null )
					return false;
				ASTNode[] mthrs = throwns.getThrowns();
				for (int i=0; i < mthrs.length; i++)
					if (exc.isInstanceOf(mthrs[i].getType())) return true;
				return false;
			}
		}
		return false;
	}
}
