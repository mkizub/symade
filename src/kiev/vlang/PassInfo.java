/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.vlang;

import kiev.be.java15.JLabel;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface BreakTarget {
	public JLabel getBrkLabel();
}

public interface ContinueTarget {
	public JLabel getCntLabel();
}

public final class ParentEnumerator implements Enumeration<ASTNode> {
	ASTNode n;
	ASTNode r;
	public ParentEnumerator(ASTNode n) {
		this.n = n;
	}
	public boolean hasMoreElements() {
		return n.isAttached();
	}
	public ASTNode nextElement() {
		if (r != null)
			n = r;
		r = (ASTNode)n.parent();
		return r;
	}
}

public class SymbolIterator implements Enumeration<ASTNode> {
	ASTNode[] stats;
	ASTNode last_stat;
	public SymbolIterator(ASTNode[] stats, ASTNode element) {
		this.stats = stats;
		if (element != null && Arrays.indexOf(stats,element) >= 0) {
			last_stat = (ASTNode)ANode.getPrevNode(element);
		} else {
			if (stats.length > 0)
				last_stat = stats[stats.length-1];
		}
	}
	public boolean hasMoreElements() {
		return last_stat != null;
	}
	public ASTNode nextElement() {
		if ( last_stat != null ) {
			ASTNode r = last_stat;
			last_stat = (ASTNode)ANode.getPrevNode(last_stat);
			return r;
		}
		throw new NoSuchElementException();
	}
}

public class PassInfo {

	// No instances
	private PassInfo() {}

	public static rule resolveNameR(ASTNode from, ASTNode@ node, ResInfo path)
		ASTNode@ p;
		ParentEnumerator pe;
	{
		{
			path.isSuperAllowed(),
			path.enterMode(ResInfo.noSuper) : path.leaveMode(),
			trace( Kiev.debug && Kiev.debugResolve, "PassInfo: resolving name "+path.getName()+" (no super)")
		;
			trace( Kiev.debug && Kiev.debugResolve, "PassInfo: resolving name "+path.getName())
		},
		pe = new ParentEnumerator(from),
		p @= pe,
		p instanceof ScopeOfNames,
		trace( Kiev.debug && Kiev.debugResolve, "PassInfo: resolving name '"+path.getName()+"' in scope '"+p+"'"),
		path.space_prev = pe.n,
		((ScopeOfNames)p).resolveNameR(node,path)
	}

	private static void addResolvedMethod(
		Method m, ResInfo info,
		Vector<Method>  methods, Vector<ResInfo> paths, Vector<CallType> types)
	{
		trace(Kiev.debug && Kiev.debugResolve,"Candidate method "+m+" with path "+info+" found...");
		if !(info.check(m))
			return;
		for (int i=0; i < methods.length; i++) {
			if (methods[i] == m) {
				trace(Kiev.debug && Kiev.debugResolve,"Duplicate methods "+m+" with paths "+info+" and "+paths[i]+" found...");
				if (info.getTransforms() < paths[i].getTransforms()) {
					trace(Kiev.debug && Kiev.debugResolve,"Will use "+m+" with paths "+info);
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
		Method@ node,
		ResInfo info,
		CallType mt)
	{
		trace(Kiev.debug && Kiev.debugResolve,"Resolving best method "+Method.toString(info.getName(),mt)+" in "+sc);
		Vector<Method>  methods  = new Vector<Method>();
		Vector<ResInfo> paths    = new Vector<ResInfo>();
		Vector<CallType> types   = new Vector<CallType>();
		if (sc instanceof ScopeOfMethods) {
			ScopeOfMethods scm = (ScopeOfMethods)sc;
			foreach( scm.resolveMethodR(node,info,mt) )
				addResolvedMethod((Method)node,info,methods,paths,types);
		}
		else if (sc instanceof Type && info.isStaticAllowed()) {
			Type tp = (Type)sc;
			foreach( tp.meta_type.tdecl.resolveMethodR(node,info,mt) )
				addResolvedMethod((Method)node,info,methods,paths,types);
		}
		else if (sc instanceof Type) {
			Type tp = (Type)sc;
			foreach( tp.resolveCallAccessR(node,info,mt) )
				addResolvedMethod((Method)node,info,methods,paths,types);
		}
		else if (sc instanceof Operator) {
			Operator op = (Operator)sc;
			foreach( op.resolveOperatorMethodR(node,info,mt) )
				addResolvedMethod((Method)node,info,methods,paths,types);
		}
		else
			throw new RuntimeException("Unknown scope "+sc);
		if( methods.size() == 0 ) {
			trace(Kiev.debug && Kiev.debugResolve,"Nothing found...");
			return false;
		}

		if (Kiev.debug && Kiev.debugResolve) {
			StringBuffer msg = new StringBuffer("Found "+methods.length+" candidate methods:\n");
			for(int i=0; i < methods.length; i++) {
				msg.append("\t").append(methods[i].parent()).append('.').append(paths[i]).append(methods[i]).append('\n');
			}
			msg.append("while resolving ").append(Method.toString(info.getName(),mt));
			trace(Kiev.debug && Kiev.debugResolve,msg.toString());
		}

		if( methods.size() == 1 ) {
			node = methods[0];
			info.set(paths[0]);
			return true;
		}
		
		for (int i=0; i < methods.length; i++) {
			Method m1 = methods[i];
			ResInfo p1 = paths[i];
			CallType mt1 = types[i];
		next_method:
			for (int j=0; j < methods.length; j++) {
				Method m2 = methods[j];
				ResInfo p2 = paths[j];
				CallType mt2 = types[j];
				
				if (m1 == m2)
					continue;
				
				trace(Kiev.debug && Kiev.debugResolve,"Compare "+m1+" and "+m2+" to be more specific for "+Method.toString(info.getName(),mt));

				Type b;
				boolean m1_is_va = m1.isVarArgs();
				boolean m2_is_va = m2.isVarArgs();
				
				if (p1.getTransforms() > p2.getTransforms()) {
					trace(Kiev.debug && Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because of path");
					continue next_method;
				}
				if (m1_is_va) {
					if (m1.type.arity < m2.type.arity) {
						trace(Kiev.debug && Kiev.debugResolve,"Method "+m1+" is less specific because of arity then "+m2);
						continue next_method;
					}
				}
				if (m2_is_va) {
					if (m2.type.arity < m1.type.arity) {
						trace(Kiev.debug && Kiev.debugResolve,"Method "+m1+" is more specific because of arity then "+m2);
						goto is_more_specific;
					}
				}
				if (mt1 ≉ mt2) {
					Type t1;
					Type t2;
					for (int k=0; k < mt.arity; k++) {
						if (m1_is_va && k >= mt1.arity && !(mt1.arity == mt.arity && k == mt.arity && mt1.arg(k) instanceof ArrayType))
							t1 = ((ArrayType)m1.getVarArgParam().getType()).arg;
						else
							t1 = mt1.arg(k);
						if (m2_is_va && k >= mt2.arity && !(mt2.arity == mt.arity && k == mt.arity && mt2.arg(k) instanceof ArrayType))
							t2 = ((ArrayType)m2.getVarArgParam().getType()).arg;
						else
							t2 = mt2.arg(k);
						if (t1 ≉ t2) {
							b = mt.arg(k).betterCast(t1,t2);
							if (b ≡ t2) {
								trace(Kiev.debug && Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because arg "+k);
								continue next_method;
							}
							if (b ≡ null && t1 ≥ t2) {
								trace(Kiev.debug && Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because arg "+k);
								continue next_method;
							}
						}
					}
					t1 = mt1.ret();
					t2 = mt2.ret();
					if (t1 ≉ t2 && !(t1 ≥ t2)) {
						b = mt.ret().betterCast(t1,t2);
						if (b ≡ t2) {
							trace(Kiev.debug && Kiev.debugResolve,"Method "+m1+" and "+m2+" is not more specific because ret");
							continue next_method;
						}
						if (b ≡ null && t2 ≥ t1) {
							trace(Kiev.debug && Kiev.debugResolve,"Method "+m1+" has less specific return value, then "+m2);
							continue next_method;
						}
					}
				}
			is_more_specific:;
				trace(Kiev.debug && Kiev.debugResolve,"Methods "+m1+" is more specific then "+m2+" resolving for "+Method.toString(info.getName(),mt));
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
			msg.append("\t").append(methods[i].parent()).append('.');
			msg.append(paths[i]).append(methods[i]).append('\n');
		}
		msg.append("while resolving ").append(Method.toString(info.getName(),mt));
		throw new CompilerException(info.getFrom(), msg.toString());
	}

	public static rule resolveMethodR(ASTNode from, Method@ node, ResInfo path, CallType mt)
		ASTNode@ p;
		ParentEnumerator pe;
	{
		pe = new ParentEnumerator(from),
		p @= pe,
		p instanceof ScopeOfMethods,
		path.space_prev = pe.n,
		resolveBestMethodR((ScopeOfMethods)p,node,path,mt),
		trace(Kiev.debug && Kiev.debugResolve,"Best method is "+node+" with path/transform "+path+" found...")
	}

	public static boolean checkException(ASTNode from, Type exc) throws RuntimeException {
		if( !exc.isInstanceOf(Type.tpThrowable) )
			throw new CompilerException(from,"A class of object for throw statement must be a subclass of "+Type.tpThrowable+" but type "+exc+" found");
		if( exc.isInstanceOf(Type.tpError) || exc.isInstanceOf(Type.tpRuntimeException) ) return true;
		for (; from != null; from = (ASTNode)from.parent()) {
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
