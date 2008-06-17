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
package kiev.ir.java15;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ViewOf(vcast=true, iface=true)
public static final view RCaseLabel of CaseLabel extends RENode {
	public		ENode			val;
	public		Type			ctype;
	public:ro	Var[]			pattern;

	public void resolve(Type reqType) {
		boolean pizza_case = false;
		SwitchStat sw = (SwitchStat)parent();
		try {
			if( val != null ) {
				val.resolve(null);
				if( val instanceof TypeRef ) {
					this.ctype = Type.getRealType(sw.sel.getType(),val.getType());
					pizza_case = true;
					TypeDecl cas = this.ctype.meta_type.tdecl;
					if (cas.isPizzaCase()) {
						if!(sw instanceof MatchStat)
							throw new CompilerException(this,"Pizza case type in non-pizza switch");
						PizzaCase pcase = (PizzaCase)cas;
						val = new ConstIntExpr(pcase.tag);
						if( pattern.length > 0 ) {
							PizzaCase pcase = (PizzaCase)cas;
							if( pattern.length != pcase.case_fields.length )
								throw new RuntimeException("Pattern containce "+pattern.length+" items, but case class "+cas+" has "+pcase.case_fields.length+" fields");
							for(int i=0, j=0; i < pattern.length; i++) {
								Var p = pattern[i];
								if( p.getType() == Type.tpVoid || p.sname == nameUnderscore)
									continue;
								Field f = pcase.case_fields[i];
								Type tp = Type.getRealType(sw.sel.getType(),f.getType());
								if( !p.getType().isInstanceOf(tp) ) // error, because of Cons<A,List<List.A>> != Cons<A,List<Cons.A>>
									throw new RuntimeException("Pattern variable "+p.sname+" has type "+p.getType()+" but type "+tp+" is expected");
								p.init = new IFldExpr(p.pos,
										new CastExpr(p.pos,
											Type.getRealType(sw.sel.getType(),cas.xtype),
											new LVarExpr(p.pos,((MatchStat)sw).tmp_var)
										),
										f
									);
								p.resolveDecl();
							}
						}
					} else {
						if!(sw instanceof SwitchTypeStat)
							throw new CompilerException(this,"Type case in non-type switch");
						if( val.getType() ≈ Type.tpObject ) {
							val = null;
							assert (sw.cases.indexOf(this) >= 0);
							sw.defCase = (CaseLabel)this;
						} else {
							val = new ConstIntExpr(0);
						}
					}
				} else {
					if (sw instanceof SwitchEnumStat) {
						if( !(val instanceof SFldExpr) )
							throw new CompilerException(this,"Wrong case in enum switch");
						SFldExpr f = (SFldExpr)val;
						Type et = sw.sel.getType();
						if( f.var.getType() ≉ et )
							throw new CompilerException(this,"Case of type "+f.var.getType()+" do not match switch expression of type "+et);
						if (et.getStruct() != null && et.getStruct().isEnum())
							val = new ConstIntExpr(((JavaEnum)et.getStruct()).getIndexOfEnumField((Field)f.var));
						else
							val = f.var.init.ncopy();
					}
					else if (sw.getClass() != SwitchStat.class)
						throw new CompilerException(this,"Wrong case in normal switch");
				}
			} else {
				if (sw.defCase != this) {
					assert (sw.cases.indexOf(this) >= 0);
					sw.defCase = (CaseLabel)this;
				}
				if ((sw instanceof SwitchTypeStat) && this.ctype ≉ Type.tpObject) {
					this.ctype = Type.tpObject;
				}
			}
		} catch(Exception e ) { Kiev.reportError(this,e); }

		if( val != null ) {
			if( !val.isConstantExpr() )
				throw new RuntimeException("Case label "+val+" must be a constant expression but "+val.getClass()+" found");
			if( !val.getType().isIntegerInCode() )
				throw new RuntimeException("Case label "+val+" must be of integer type");
		}
	}
}

@ViewOf(vcast=true, iface=true)
public static view RSwitchStat of SwitchStat extends RBlock {
	public		ENode					sel;
	public:ro	CaseLabel[]				cases;
	public		CaseLabel				defCase;
	public		ENode					sel_to_int;
	public:ro	Label					lblcnt;
	public:ro	Label					lblbrk;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if( cases.length == 0 ) {
			ExprStat st = new ExprStat(pos,~sel);
			this.replaceWithNodeResolve(Type.tpVoid, st);
		}
		else if( cases.length == 1 && cases[0].pattern.length == 0 && cases[0].val == null) {
			cases[0].resolve(Type.tpVoid);
			CaseLabel cas = (CaseLabel)cases[0];
			Block bl = new Block(cas.pos);
			foreach (ASTNode n; ((SwitchStat)this).stats.delToArray(); !(n instanceof CaseLabel))
				bl.stats += n;
			bl.setBreakTarget(true);
			if( cas.val == null ) {
				bl.stats.insert(0,new ExprStat(sel.pos,~sel));
				this.replaceWithNodeResolve(Type.tpVoid, bl);
				return;
			} else {
				IfElseStat st = new IfElseStat(pos,
						new BinaryBoolExpr(sel.pos,Operator.Equals,~sel,~cas.val),
						bl,
						null
					);
				this.replaceWithNodeResolve(Type.tpVoid, st);
				return;
			}
		}
		if (defCase == null)
			setBreaked(true);
		sel.resolve(Type.tpInt);
		TypeRef[] typenames = new TypeRef[0];
		RBlock.resolveStats(Type.tpVoid, getSpacePtr("stats"));
		RSwitchStat.getThrowForMethodAbrupted((SwitchStat)this);
		setResolved(true);
	}

	static void getThrowForMethodAbrupted(SwitchStat sw) {	
		if (sw.isMethodAbrupted() && sw.defCase==null) {
			CaseLabel dflt = new CaseLabel(sw.pos,null);
			sw.stats.insert(0,dflt);
			sw.cases.insert(0,dflt);
			sw.defCase = dflt;
			ThrowStat thrw = new ThrowStat(sw.pos,new NewExpr(sw.pos,Type.tpError,ENode.emptyArray));
			sw.stats.insert(1,thrw);
			dflt.resolve(Type.tpVoid);
			thrw.resolve(Type.tpVoid);
		}
	}
	
}

@ViewOf(vcast=true, iface=true)
public static final view RSwitchEnumStat of SwitchEnumStat extends RSwitchStat {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		sel.resolve(Type.tpEnum);
		Type tp = sel.getType();
		if (!tp.isReference() || !tp.getStruct().isEnum())
			throw new CompilerException(this, "Enum switch selector must be the enum type");
		this.sel_to_int = new CallExpr(pos,
				new NopExpr(),
				Type.tpEnum.tdecl.resolveMethod("ordinal", Type.tpInt),
				ENode.emptyArray
				);
		JavaEnum jen = (JavaEnum)tp.getStruct();
		if (defCase == null && jen.getEnumFields().length > cases.length)
			setBreaked(true);
		RBlock.resolveStats(Type.tpVoid, getSpacePtr("stats"));
		for(int i=0; i < cases.length; i++) {
			for(int j=0; j < i; j++) {
				ENode vi = cases[i].val;
				ENode vj = cases[j].val;
				if( i != j &&  vi != null && vj != null
				 && vi.getConstValue().equals(vj.getConstValue()) )
					throw new RuntimeException("Duplicate value "+vi+" and "+vj+" in switch statement");
			}
		}
		// Check if abrupted
		if (!isBreaked() && defCase == null)
			setMethodAbrupted(true);
		RSwitchStat.getThrowForMethodAbrupted((SwitchStat)this);
		setResolved(true);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RSwitchTypeStat of SwitchTypeStat extends RSwitchStat {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		sel.resolve(Type.tpObject);
		Type tp = sel.getType();
		if (!tp.isReference())
			throw new CompilerException(this, "Type switch selector must be the reference type");
		Field typehash = new Field("fld$sel$"+Integer.toHexString(sel.hashCode()),
			Type.tpTypeSwitchHash,ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
		ctx_tdecl.members.add(typehash);
		this.sel_to_int = new CallExpr(pos,
				new TypeRef(Type.tpTypeSwitchHash),
				Type.tpTypeSwitchHash.tdecl.resolveMethod("index",Type.tpInt, Type.tpObject, Type.tpTypeSwitchHash),
				new ENode[]{new NopExpr(), new SFldExpr(pos,typehash)}
				);
		if (defCase == null)
			setBreaked(true);
		sel.resolve(Type.tpInt);
		TypeRef[] typenames = new TypeRef[0];
		RBlock.resolveStats(Type.tpVoid, getSpacePtr("stats"));
		foreach (CaseLabel cl; cases; cl.isDirectFlowReachable())
			Kiev.reportWarning(cl, "Fall through to switch case");
		int defindex = -1;
		for(int i=0; i < cases.length; i++) {
			CaseLabel c = cases[i];
			if( c.ctype == null || !c.ctype.isReference() )
				throw new CompilerException(c,"Mixed switch and switch-type cases");
			typenames = (TypeRef[])Arrays.append(typenames,new TypeRef(c.ctype));
			if( c.val != null )
				c.val = new ConstIntExpr(i);
			else
				defindex = i;
		}
		TypeClassExpr[] types = new TypeClassExpr[typenames.length];
		for(int j=0; j < types.length; j++)
			types[j] = new TypeClassExpr(typenames[j].pos,typenames[j]);
		if( defindex < 0 ) defindex = types.length;
		typehash.init = new NewExpr(ctx_tdecl.pos,Type.tpTypeSwitchHash,
			new ENode[]{ new NewInitializedArrayExpr(ctx_tdecl.pos,new TypeExpr(Type.tpClass,Operator.PostTypeArray),types),
				new ConstIntExpr(defindex)
			});
		Constructor clinit = ((Struct)ctx_tdecl).getClazzInitMethod();
		clinit.block.stats.add(
			new ExprStat(typehash.init.pos,
				new AssignExpr(typehash.init.pos,Operator.Assign
					,new SFldExpr(typehash.pos,typehash),typehash.init.ncopy())
			)
		);
		for(int i=0; i < cases.length; i++) {
			for(int j=0; j < i; j++) {
				ENode vi = cases[i].val;
				ENode vj = cases[j].val;
				if( i != j &&  vi != null && vj != null
				 && vi.getConstValue().equals(vj.getConstValue()) )
					throw new RuntimeException("Duplicate value "+vi+" and "+vj+" in switch statement");
			}
		}
		RSwitchStat.getThrowForMethodAbrupted((SwitchStat)this);
		setResolved(true);
	}
}

@ViewOf(vcast=true, iface=true)
public static final view RMatchStat of MatchStat extends RSwitchStat {

	public Var					tmp_var;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		sel.resolve(Type.tpObject);
		Type tp = sel.getType();
		if (!tp.isReference() || !tp.meta_type.tdecl.isHasCases())
			throw new CompilerException(this, "Pattern-match switch selector must be the type with cases");
		this.sel_to_int = new CallExpr(pos,
				new NopExpr(),
				tp.meta_type.tdecl.resolveMethod(nameGetCaseTag, Type.tpInt),
				ENode.emptyArray
				);
		tmp_var = new LVar(pos,"$tmp",sel.getType(),Var.VAR_LOCAL,0);
		int defindex = -1;
		RBlock.resolveStats(Type.tpVoid, getSpacePtr("stats"));
		for(int i=0; i < cases.length; i++) {
			for(int j=0; j < i; j++) {
				ENode vi = cases[i].val;
				ENode vj = cases[j].val;
				if( i != j &&  vi != null && vj != null
				 && vi.getConstValue().equals(vj.getConstValue()) )
					throw new RuntimeException("Duplicate value "+vi+" and "+vj+" in switch statement");
			}
		}
		// Check if abrupted
		if( !isBreaked() ) {
			boolean has_unabrupted_case = false;
			if (defCase == null) {
				// Check if it's a pizza-type switch and all cases are
				// abrupted and all class's cases present
				// Check if all cases are abrupted
				for(int i=0; i < cases.length; i++) {
					if( !cases[i].isMethodAbrupted() && cases[i].isAbrupted() ) {
						has_unabrupted_case = true;
						break;
					}
					else if( !cases[i].isAbrupted() ) {
						for(int j = i+1; j < cases.length; j++) {
							if( cases[j].isAbrupted()  ) {
								if( !cases[j].isMethodAbrupted() ) {
									has_unabrupted_case = true;
									break;
								}
							}
						}
					}
				}
				if( !has_unabrupted_case ) {
					Type tp = sel.getType();
					int caseno = 0;
					ComplexTypeDecl tpclz = (ComplexTypeDecl)tp.meta_type.tdecl;
					foreach (Struct sub; tpclz.sub_decls) {
						if (sub.isPizzaCase()) {
							PizzaCase pcase = (PizzaCase)sub;
							if (pcase.tag > caseno)
								caseno = pcase.tag;
						}
					}
					if( caseno == cases.length ) setMethodAbrupted(true);
				}
			} else {
				if (!cases[cases.length-1].isAbrupted()) {
					setAbrupted(false);
					has_unabrupted_case = true;
				}
				if( !has_unabrupted_case ) setMethodAbrupted(true);
			}
		}
		RSwitchStat.getThrowForMethodAbrupted((SwitchStat)this);
		setResolved(true);
	}
}

