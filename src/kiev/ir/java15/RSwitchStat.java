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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public static final view RCaseLabel of CaseLabel extends RENode {
	public		ENode			val;
	public		Type			type;
	public:ro	Var[]			pattern;
	public:ro	ASTNode[]		stats;

	public void resolve(Type reqType) {
		boolean pizza_case = false;
		SwitchStat sw = (SwitchStat)parent();
		try {
			if( val != null ) {
				val.resolve(null);
				if( val instanceof TypeRef ) {
					this.open();
					this.type = Type.getRealType(sw.tmpvar.getType(),val.getType());
					pizza_case = true;
					Struct cas = this.type.getStruct();
					if( cas.isPizzaCase() ) {
						if( sw.mode != SwitchStat.PIZZA_SWITCH )
							throw new CompilerException(this,"Pizza case type in non-pizza switch");
						PizzaCase pcase = (PizzaCase)cas.variant;
						this.open();
						val = new ConstIntExpr(pcase.tag);
						if( pattern.length > 0 ) {
							PizzaCase pcase = (PizzaCase)cas.variant;
							if( pattern.length != pcase.group.decls.length )
								throw new RuntimeException("Pattern containce "+pattern.length+" items, but case class "+cas+" has "+pcase.group.decls.length+" fields");
							for(int i=0, j=0; i < pattern.length; i++) {
								Var p = pattern[i];
								if( p.type == Type.tpVoid || p.id.sname == nameUnderscore)
									continue;
								Field f = (Field)pcase.group.decls[i];
								Type tp = Type.getRealType(sw.tmpvar.getType(),f.type);
								if( !p.type.isInstanceOf(tp) ) // error, because of Cons<A,List<List.A>> != Cons<A,List<Cons.A>>
									throw new RuntimeException("Pattern variable "+p.id+" has type "+p.type+" but type "+tp+" is expected");
								p.open();
								p.init = new IFldExpr(p.pos,
										new CastExpr(p.pos,
											Type.getRealType(sw.tmpvar.getType(),cas.xtype),
											new LVarExpr(p.pos,sw.tmpvar.getVar())
										),
										f
									);
//									addSymbol(j++,p);
								p.resolveDecl();
							}
						}
					} else {
						if( sw.mode != SwitchStat.TYPE_SWITCH )
							throw new CompilerException(this,"Type case in non-type switch");
						this.open();
						if( val.getType() ≈ Type.tpObject ) {
							val = null;
							sw.defCase = (CaseLabel)this;
						} else {
							val = new ConstIntExpr(0);
						}
					}
				} else {
					if( sw.mode == SwitchStat.ENUM_SWITCH ) {
						if( !(val instanceof SFldExpr) )
							throw new CompilerException(this,"Wrong case in enum switch");
						SFldExpr f = (SFldExpr)val;
						Type et = sw.sel.getType();
						if( f.var.type ≢ et )
							throw new CompilerException(this,"Case of type "+f.var.type+" do not match switch expression of type "+et);
						this.open();
						if (et.getStruct() != null && et.getStruct().isEnum())
							val = new ConstIntExpr(et.getStruct().getIndexOfEnumField((Field)f.var));
						else
							val = f.var.init.ncopy();
					}
					else if( sw.mode != SwitchStat.NORMAL_SWITCH )
						throw new CompilerException(this,"Wrong case in normal switch");
				}
			} else {
				if (sw.defCase != this) {
					sw.open();
					sw.defCase = (CaseLabel)this;
				}
				if( sw.mode == SwitchStat.TYPE_SWITCH && this.type ≉ Type.tpObject) {
					this.open();
					this.type = Type.tpObject;
				}
			}
		} catch(Exception e ) { Kiev.reportError(this,e); }

		RBlock.resolveStats(Type.tpVoid, getSpacePtr("stats"));

		if( val != null ) {
			if( !val.isConstantExpr() )
				throw new RuntimeException("Case label "+val+" must be a constant expression but "+val.getClass()+" found");
			if( !val.getType().isIntegerInCode() )
				throw new RuntimeException("Case label "+val+" must be of integer type");
		}
	}
}

public static final view RSwitchStat of SwitchStat extends RENode {
	public		int						mode;
	public		ENode					sel;
	public:ro	CaseLabel[]				cases;
	public		LVarExpr				tmpvar;
	public		CaseLabel				defCase;
	public		Field					typehash; // needed for re-resolving
	public:ro	Label					lblcnt;
	public:ro	Label					lblbrk;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if( cases.length == 0 ) {
			ExprStat st = new ExprStat(pos,~sel);
			this.replaceWithNodeResolve(Type.tpVoid, st);
		}
		else if( cases.length == 1 && cases[0].pattern.length == 0) {
			cases[0].resolve(Type.tpVoid);
			CaseLabel cas = (CaseLabel)cases[0];
			Block bl = new Block(cas.pos, cas.stats.delToArray());
			bl.setBreakTarget(true);
			if( ((CaseLabel)cas).val == null ) {
				bl.stats.insert(0,new ExprStat(sel.pos,~sel));
				this.replaceWithNodeResolve(Type.tpVoid, bl);
				return;
			} else {
				cas.open();
				IfElseStat st = new IfElseStat(pos,
						new BinaryBoolExpr(sel.pos,Operator.Equals,~sel,~cas.val),
						bl,
						null
					);
				this.replaceWithNodeResolve(Type.tpVoid, st);
				return;
			}
		}
		if( tmpvar == null ) {
			Block me = null;
			try {
				sel.resolve(Type.tpInt);
				Type tp = sel.getType();
				if( tp.getStruct() != null && tp.getStruct().isEnum() ) {
					mode = SwitchStat.ENUM_SWITCH;
				}
				else if( tp.isReference() ) {
					this.open();
					tmpvar = new LVarExpr(sel.pos, new LVar(sel.pos,"tmp$sel$"+Integer.toHexString(sel.hashCode()),tp,Var.VAR_LOCAL,0));
					me = new Block(pos);
					this.replaceWithNode(me);
					ENode old_sel = ~this.sel;
					tmpvar.getVar().init = old_sel;
					me.addSymbol(tmpvar.getVar());
					me.stats.add((ENode)this);
					if( tp.getStruct() != null && tp.getStruct().isHasCases() ) {
						mode = SwitchStat.PIZZA_SWITCH;
						sel = new CallExpr(pos,new LVarExpr(tmpvar.pos,tmpvar.getVar()),
							new SymbolRef<Method>(pos, nameGetCaseTag),null,ENode.emptyArray);
						Kiev.runProcessorsOn(sel);
					} else {
						mode = SwitchStat.TYPE_SWITCH;
						typehash = new Field("fld$sel$"+Integer.toHexString(old_sel.hashCode()),
							Type.tpTypeSwitchHash,ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
						ctx_tdecl.members.add(typehash);
						CallExpr cae = new CallExpr(pos,
							new SFldExpr(pos,typehash),
							Type.tpTypeSwitchHash.clazz.resolveMethod("index",Type.tpInt, Type.tpObject),
							new ENode[]{new LVarExpr(pos,tmpvar.getVar())}
							);
						sel = cae;
					}
				}
			} catch(Exception e ) { Kiev.reportError(sel,e); }
			if( me != null ) {
				me.resolve(reqType);
				return;
			}
		}
		sel.resolve(Type.tpInt);
		TypeRef[] typenames = new TypeRef[0];
		int defindex = -1;
		for(int i=0; i < cases.length; i++) {
			try {
				cases[i].resolve(Type.tpVoid);
				if( typehash != null ) {
					CaseLabel c = (CaseLabel)cases[i];
					if( c.type == null || !c.type.isReference() )
						throw new CompilerException(c,"Mixed switch and typeswitch cases");
					typenames = (TypeRef[])Arrays.append(typenames,new TypeRef(c.type));
					if( c.val != null )
						c.val = new ConstIntExpr(i);
					else
						defindex = i;
				}
			}
			catch(Exception e ) { Kiev.reportError(cases[i],e); }
			if( tmpvar!=null && i < cases.length-1 && !cases[i].isAbrupted() ) {
				Kiev.reportWarning(cases[i+1], "Fall through to switch case");
			}
		}
		if( mode == SwitchStat.TYPE_SWITCH ) {
			TypeClassExpr[] types = new TypeClassExpr[typenames.length];
			for(int j=0; j < types.length; j++)
				types[j] = new TypeClassExpr(typenames[j].pos,typenames[j]);
			if( defindex < 0 ) defindex = types.length;
			typehash.init = new NewExpr(ctx_tdecl.pos,Type.tpTypeSwitchHash,
				new ENode[]{ new NewInitializedArrayExpr(ctx_tdecl.pos,new TypeExpr(Type.tpClass,Operator.PostTypeArray),1,types),
					new ConstIntExpr(defindex)
				});
			Constructor clinit = ((Struct)ctx_tdecl).getClazzInitMethod();
			clinit.block.stats.add(
				new ExprStat(typehash.init.pos,
					new AssignExpr(typehash.init.pos,Operator.Assign
						,new SFldExpr(typehash.pos,typehash),typehash.init.ncopy())
				)
			);
			//typehash.resolveDecl();
		}
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
			boolean has_default_case = false;
			for(int i=0; i < cases.length; i++) {
				if( ((CaseLabel)cases[i]).val == null ) {
					has_default_case = true;
					break;
				}
			}
			boolean has_unabrupted_case = false;
			if( !has_default_case ) {
				// Check if it's an enum-type switch and all cases are
				// abrupted and all enum values cases present
				// Check if all cases are abrupted
				if( mode == SwitchStat.ENUM_SWITCH ) {
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
						Field[] eflds = tp.getStruct().getEnumFields();
						if (eflds.length == cases.length)
							setMethodAbrupted(true);
					}
				}
				// Check if it's a pizza-type switch and all cases are
				// abrupted and all class's cases present
				// Check if all cases are abrupted
				else if( mode == SwitchStat.PIZZA_SWITCH ) {
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
					if( tmpvar != null ) {
						if( !has_unabrupted_case ) {
							Type tp = tmpvar.getType();
							//PizzaCaseAttr case_attr;
							int caseno = 0;
							Struct tpclz = tp.getStruct();
							foreach (Struct sub; tpclz.sub_decls) {
								if( sub.isPizzaCase() ) {
									PizzaCase pcase = (PizzaCase)sub.variant;
									if (pcase.tag > caseno)
										caseno = pcase.tag;
								}
							}
							if( caseno == cases.length ) setMethodAbrupted(true);
						}
					}
				}
			} else {
				if (!cases[cases.length-1].isAbrupted()) {
					setAbrupted(false);
					has_unabrupted_case = true;
				}
				if( !has_unabrupted_case ) setMethodAbrupted(true);
			}
		}
		if( isMethodAbrupted() && defCase==null ) {
			ENode thrErr = new ThrowStat(pos,new NewExpr(pos,Type.tpError,ENode.emptyArray));
			CaseLabel dc = new CaseLabel(pos,null,new ENode[]{thrErr});
			((SwitchStat)this).cases.insert(0,dc);
			dc.resolve(Type.tpVoid);
		}
		if( mode == SwitchStat.ENUM_SWITCH ) {
			Type tp = sel.getType();
			sel = new CastExpr(pos,Type.tpInt,~sel);
			sel.resolve(Type.tpInt);
		}
		setResolved(true);
	}
}

