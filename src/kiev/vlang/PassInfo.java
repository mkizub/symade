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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 296 $
 *
 */

public final class ParentEnumerator implements Enumeration<ANode> {
	ANode n;
	ANode r;
	final boolean in_syntax;
	public ParentEnumerator(ANode n, boolean in_syntax) {
		this.n = n;
		this.in_syntax = in_syntax;
	}
	public boolean hasMoreElements() {
		if (in_syntax && ANode.nodeattr$syntax_parent.get(n) != null)
			return true;
		return n.isAttached();
	}
	public ANode nextElement() {
		if (r != null)
			n = r;
		if (in_syntax) {
			r = (ANode)ANode.nodeattr$syntax_parent.get(n);
			if (r == null)
				r = n.parent();
		} else {
			r = n.parent();
		}
		return r;
	}
}

public class SymbolIterator implements Enumeration<ANode> {
	INode[] stats;
	INode last_stat;
	int   last_pos;
	public SymbolIterator(INode[] stats, INode element) {
		this.stats = stats;
		if (element == null) {
			if (stats.length > 0) {
				last_pos = stats.length-1;
				last_stat = stats[last_pos];
			}
			return;
		}
		last_pos = Arrays.indexOf(stats,element) - 1;
		if (last_pos >= 0)
			last_stat = stats[last_pos];
	}
	public boolean hasMoreElements() {
		return last_stat != null;
	}
	public ANode nextElement() {
		if ( last_stat != null ) {
			INode r = last_stat;
			last_pos -= 1;
			if (last_pos >= 0)
				last_stat = stats[last_pos];
			else
				last_stat = null;
			return r.asANode();
		}
		throw new NoSuchElementException();
	}
}

public class PassInfo {

	// No instances
	private PassInfo() {}

	public static rule resolveNameR(ANode from, ResInfo path)
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
		pe = new ParentEnumerator(from, path.inSyntaxContext()),
		p @= pe,
		p instanceof ScopeOfNames,
		trace( Kiev.debug && Kiev.debugResolve, "PassInfo: resolving name '"+path.getName()+"' in scope '"+p+"'"),
		path.setPrev(pe.n),
		((ScopeOfNames)p).resolveNameR(path)
	}

	private static void addResolvedMethod(ResInfo info, Vector<ResInfo> paths)
	{
		DNode m = info.resolvedDNode();
		trace(Kiev.debug && Kiev.debugResolve,"Candidate method "+m+" with path "+info+" found...");
		for (int i=0; i < paths.length; i++) {
			if (paths[i].resolvedDNode() == m.dnode) {
				trace(Kiev.debug && Kiev.debugResolve,"Duplicate methods "+m+" with paths "+info+" and "+paths[i]+" found...");
				if (info.getTransforms() < paths[i].getTransforms()) {
					trace(Kiev.debug && Kiev.debugResolve,"Will use "+m+" with paths "+info);
					paths[i] = info.copy();
				}
				return;
			}
		}
		paths.append(info.copy());
	}
	
	public static boolean resolveBestMethodR(
		Object sc,
		ResInfo info,
		CallType mt)
	{
		trace(Kiev.debug && Kiev.debugResolve,"Resolving best method "+Method.toString(info.getName(),mt)+" in "+sc);
		Vector<ResInfo>  paths   = new Vector<ResInfo>();
		if (sc instanceof ScopeOfMethods) {
			ScopeOfMethods scm = (ScopeOfMethods)sc;
			foreach( scm.resolveMethodR(info,mt) )
				addResolvedMethod(info,paths);
		}
		else if (sc instanceof Type && info.isStaticAllowed()) {
			Type tp = (Type)sc;
			foreach( tp.meta_type.tdecl.resolveMethodR(info,mt) )
				addResolvedMethod(info,paths);
		}
		else if (sc instanceof Type) {
			Type tp = (Type)sc;
			foreach( tp.resolveCallAccessR(info,mt) )
				addResolvedMethod(info,paths);
		}
		else if (sc instanceof Opdef) {
			Opdef opd = (Opdef)sc;
			foreach( opd.resolveOperatorMethodR(info,mt) )
				addResolvedMethod(info,paths);
		}
		else
			throw new RuntimeException("Unknown scope "+sc);
		if( paths.size() == 0 ) {
			trace(Kiev.debug && Kiev.debugResolve,"Nothing found...");
			return false;
		}

		if (Kiev.debug && Kiev.debugResolve) {
			StringBuffer msg = new StringBuffer("Found "+paths.length+" candidate methods:\n");
			for(int i=0; i < paths.length; i++) {
				Method m = (Method)paths[i].resolvedDNode();
				msg.append("\t").append(m.parent()).append('.').append(paths[i]).append(m).append('\n');
			}
			msg.append("while resolving ").append(Method.toString(info.getName(),mt));
			trace(Kiev.debug && Kiev.debugResolve,msg.toString());
		}

		if( paths.size() == 1 ) {
			info.set(paths[0]);
			return true;
		}
		
		for (int i=0; i < paths.length; i++) {
			Method m1 = (Method)paths[i].resolvedDNode();
			ResInfo p1 = paths[i];
			CallType mt1 = paths[i].resolved_type;
		next_method:
			for (int j=0; j < paths.length; j++) {
				Method m2 = (Method)paths[j].resolvedDNode();
				ResInfo p2 = paths[j];
				CallType mt2 = paths[j].resolved_type;
				
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
					if (m1.mtype.arity < m2.mtype.arity) {
						trace(Kiev.debug && Kiev.debugResolve,"Method "+m1+" is less specific because of arity then "+m2);
						continue next_method;
					}
				}
				if (m2_is_va) {
					if (m2.mtype.arity < m1.mtype.arity) {
						trace(Kiev.debug && Kiev.debugResolve,"Method "+m1+" is more specific because of arity then "+m2);
						goto is_more_specific;
					}
				}
				if (mt1 ≉ mt2) {
					Type t1;
					Type t2;
					for (int k=0; k < mt.arity; k++) {
						if (m1_is_va && k >= mt1.arity && !(mt1.arity == mt.arity && k == mt.arity && mt1.arg(k) instanceof ArrayType))
							t1 = ((ArrayType)m1.getVarArgParam().getType(info.env)).arg;
						else
							t1 = mt1.arg(k);
						if (m2_is_va && k >= mt2.arity && !(mt2.arity == mt.arity && k == mt.arity && mt2.arg(k) instanceof ArrayType))
							t2 = ((ArrayType)m2.getVarArgParam().getType(info.env)).arg;
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
				paths.remove(j);
				j--;
				if (i >= j)
					i--;
			}
		}
		if( paths.size() == 1 ) {
			info.set(paths[0]);
			return true;
		}
		
		StringBuffer msg = new StringBuffer("Umbigous methods:\n");
		for(int i=0; i < paths.length; i++) {
			Method m = (Method)paths[i].resolvedDNode();
			msg.append("\t").append(m.parent()).append('.');
			msg.append(paths[i]).append(m).append('\n');
		}
		msg.append("while resolving ").append(Method.toString(info.getName(),mt));
		throw new CompilerException(info.getFrom(), msg.toString());
	}

	public static rule resolveMethodR(ANode from, ResInfo path, CallType mt)
		ASTNode@ p;
		ParentEnumerator pe;
	{
		pe = new ParentEnumerator(from, path.inSyntaxContext()),
		p @= pe,
		p instanceof ScopeOfMethods,
		path.setPrev(pe.n),
		resolveBestMethodR((ScopeOfMethods)p,path,mt),
		trace(Kiev.debug && Kiev.debugResolve,"Best method is "+path.resolvedSymbol()+" with path/transform "+path+" found...")
	}

	public static boolean checkException(ASTNode from, Type exc, Env env) throws RuntimeException {
		if( !exc.isInstanceOf(env.tenv.tpThrowable) )
			throw new CompilerException(from,"A class of object for throw statement must be a subclass of "+env.tenv.tpThrowable+" but type "+exc+" found");
		if( exc.isInstanceOf(env.tenv.tpError) || exc.isInstanceOf(env.tenv.tpRuntimeException) ) return true;
		for (; from != null; from = (ASTNode)from.parent()) {
			if( from instanceof TryStat ) {
				TryStat trySt = (TryStat)from;
				for(int j=0; j < trySt.catchers.length; j++)
					if( exc.isInstanceOf(trySt.catchers[j].arg.getType(env)) ) return true;
			}
			else if( from instanceof Method ) {
				MetaThrows throwns = from.getMetaThrows();
				if( throwns == null )
					return false;
				ASTNode[] mthrs = throwns.getThrowns();
				for (int i=0; i < mthrs.length; i++)
					if (exc.isInstanceOf(mthrs[i].getType(env))) return true;
				return false;
			}
		}
		return false;
	}
}
