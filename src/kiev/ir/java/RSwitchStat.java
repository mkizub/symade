package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.CaseLabel.CaseLabelImpl;
import kiev.vlang.CaseLabel.CaseLabelView;
import kiev.vlang.SwitchStat.SwitchStatImpl;
import kiev.vlang.SwitchStat.SwitchStatView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public static final view RCaseLabel of CaseLabelImpl extends CaseLabelView {
	
	public void resolve(Type reqType) {
		boolean pizza_case = false;
		SwitchStat sw = (SwitchStat)parent_node;
		try {
			if( val != null ) {
				val.resolve(null);
				if( val instanceof TypeRef ) {
					this.type = Type.getRealType(sw.tmpvar.getType(),val.getType());
					pizza_case = true;
					Struct cas = this.type.getStruct();
					if( cas.isPizzaCase() ) {
						if( sw.mode != SwitchStat.PIZZA_SWITCH )
							throw new CompilerException(this,"Pizza case type in non-pizza switch");
						//PizzaCaseAttr case_attr = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
						MetaPizzaCase meta = cas.getMetaPizzaCase();
						//val = new ConstIntExpr(case_attr.caseno);
						val = new ConstIntExpr(meta.getTag());
						if( pattern.length > 0 ) {
							Field[] fields = meta.getFields();
							if( pattern.length != fields.length )
								throw new RuntimeException("Pattern containce "+pattern.length+" items, but case class "+cas+" has "+fields.length+" fields");
							for(int i=0, j=0; i < pattern.length; i++) {
								Var p = pattern[i];
								if( p.type == Type.tpVoid || p.name.name.len == 1 && p.name.name.byteAt(0) == '_')
									continue;
								Field f = fields[i];
								Type tp = Type.getRealType(sw.tmpvar.getType(),f.type);
								if( !p.type.isInstanceOf(tp) ) // error, because of Cons<A,List<List.A>> != Cons<A,List<Cons.A>>
									throw new RuntimeException("Pattern variable "+p.name+" has type "+p.type+" but type "+tp+" is expected");
								p.init = new IFldExpr(p.pos,
										new CastExpr(p.pos,
											Type.getRealType(sw.tmpvar.getType(),cas.ctype),
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
						if( val.getType() ≈ Type.tpObject ) {
							val = null;
							sw.defCase = ((CaseLabelImpl)this)._self;
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
						if (et.getStruct() != null && et.getStruct().isEnum())
							val = new ConstIntExpr(et.getStruct().getIndexOfEnumField(f.var));
						else
							val = f.var.init.ncopy();
					}
					else if( sw.mode != SwitchStat.NORMAL_SWITCH )
						throw new CompilerException(this,"Wrong case in normal switch");
				}
			} else {
				sw.defCase = ((CaseLabelImpl)this)._self;
				if( sw.mode == SwitchStat.TYPE_SWITCH )
					this.type = Type.tpObject;
			}
		} catch(Exception e ) { Kiev.reportError(this,e); }

		Block.resolveStats(Type.tpVoid, this.getENode(), stats);

		if( val != null ) {
			if( !val.isConstantExpr() )
				throw new RuntimeException("Case label "+val+" must be a constant expression but "+val.getClass()+" found");
			if( !val.getType().isIntegerInCode() )
				throw new RuntimeException("Case label "+val+" must be of integer type");
		}
	}
}

@nodeview
public static final view RSwitchStat of SwitchStatImpl extends SwitchStatView {

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
				bl.stats.insert(new ExprStat(sel.pos,~sel),0);
				this.replaceWithNodeResolve(Type.tpVoid, bl);
				return;
			} else {
				IfElseStat st = new IfElseStat(pos,
						new BinaryBoolExpr(sel.pos,BinaryOperator.Equals,~sel,~cas.val),
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
					tmpvar = new LVarExpr(sel.pos, new Var(sel.pos,KString.from(
						"tmp$sel$"+Integer.toHexString(sel.hashCode())),tp,0));
					me = new Block(pos);
					this.replaceWithNode(me);
					ENode old_sel = ~this.sel;
					tmpvar.getVar().init = old_sel;
					me.addSymbol(tmpvar.getVar());
					me.stats.add(this.getENode());
					if( tp.getStruct() != null && tp.getStruct().isHasCases() ) {
						mode = SwitchStat.PIZZA_SWITCH;
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						sel = cae;
						cae.pos = pos;
						cae.obj = new LVarExpr(tmpvar.pos,tmpvar.getVar());
						cae.obj.resolve(null);
						cae.func = new NameRef(pos, nameGetCaseTag);
					} else {
						mode = SwitchStat.TYPE_SWITCH;
						typehash = new Field(KString.from("fld$sel$"+Integer.toHexString(old_sel.hashCode())),
							Type.tpTypeSwitchHash,ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
						ctx_clazz.addField(typehash);
						CallExpr cae = new CallExpr(pos,
							new SFldExpr(pos,typehash),
							Type.tpTypeSwitchHash.clazz.resolveMethod(KString.from("index"),Type.tpInt, Type.tpObject),
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
		KString[] typenames = new KString[0];
		int defindex = -1;
		for(int i=0; i < cases.length; i++) {
			try {
				cases[i].resolve(Type.tpVoid);
				if( typehash != null ) {
					CaseLabel c = (CaseLabel)cases[i];
					if( c.type == null || !c.type.isReference() )
						throw new CompilerException(c,"Mixed switch and typeswitch cases");
					KString name = c.type.getStruct().name.name;
					typenames = (KString[])Arrays.append(typenames,name);
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
			ConstExpr[] signs = new ConstExpr[typenames.length];
			for(int j=0; j < signs.length; j++)
				signs[j] = new ConstStringExpr(typenames[j]);
			if( defindex < 0 ) defindex = signs.length;
			typehash.init = new NewExpr(ctx_clazz.pos,Type.tpTypeSwitchHash,
				new ENode[]{ new NewInitializedArrayExpr(ctx_clazz.pos,new TypeRef(Type.tpString),1,signs),
					new ConstIntExpr(defindex)
				});
			Constructor clinit = ctx_clazz.getClazzInitMethod();
			clinit.body.stats.add(
				new ExprStat(typehash.init.pos,
					new AssignExpr(typehash.init.pos,AssignOperator.Assign
						,new SFldExpr(typehash.pos,typehash),new Shadow(typehash.init))
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
							for(int i=0; i < tpclz.sub_clazz.length; i++) {
								if( tpclz.sub_clazz[i].isPizzaCase() ) {
//									case_attr = (PizzaCaseAttr)tpclz.sub_clazz[i].getAttr(attrPizzaCase);
//									if( case_attr!=null && case_attr.caseno > caseno )
//										caseno = case_attr.caseno;
									MetaPizzaCase meta = tpclz.sub_clazz[i].getMetaPizzaCase();
									if( meta!=null && meta.getTag() > caseno )
										caseno = meta.getTag();
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
			cases.insert(dc,0);
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

