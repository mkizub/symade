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
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/ComplexStatement.java,v 1.5.2.1.2.2 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1.2.2 $
 *
 */

@node
public class CaseLabel extends ENode implements ScopeOfNames {

	static CaseLabel[] emptyArray = new CaseLabel[0];

	@att public ENode				val;
	@ref public Type				type;
	@att public final NArr<Var>		pattern;
	@att public final NArr<ENode>	stats;

	public CodeLabel	case_label;

	public CaseLabel() {
	}

	public CaseLabel(int pos, ASTNode parent, ENode val, ENode[] stats) {
		super(pos,parent);
		this.val = val;
		this.stats.addAll(stats);
	}

	public String toString() {
		if( val == null )
			return "default:";
		else if(pattern.length > 0) {
			StringBuffer sb = new StringBuffer();
			sb.append("case ").append(val).append('(');
			for(int i=0; i < pattern.length; i++) {
				sb.append(pattern[i].type).append(' ').append(pattern[i].name);
				if( i < pattern.length-1 ) sb.append(',');
			}
			sb.append("):");
			return sb.toString();
		}
		return "case "+val+':';
	}

	public ENode addStatement(int i, ENode st) {
		if( st == null ) return null;
		stats.insert(st,i);
		return st;
	}

	public void addSymbol(int idx, Named sym) {
		ENode decl;
		if (sym instanceof Var)
			decl = new VarDecl((Var)sym);
		else if (sym instanceof Struct)
			decl = new LocalStructDecl((Struct)sym);
		else
			throw new RuntimeException("Expected e-node declaration, but got "+sym+" ("+sym.getClass()+")");
		foreach(ASTNode n; stats) {
			if (n instanceof Named && ((Named)n).getName().equals(sym.getName()) ) {
				Kiev.reportError(decl.pos,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.insert(decl,idx);
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
		Var@ var;
	{
		var @= pattern,
		var.name.equals(name),
		node ?= var
	}
	
	public void cleanup() {
		parent=null;
		val.cleanup();
		val = null;
		type = null;
		pattern.cleanup();
		stats.cleanup();
	}

	public void resolve(Type tpVoid) {
		boolean pizza_case = false;
		PassInfo.push(this);
		try {
			SwitchStat sw = (SwitchStat)parent;
			try {
				if( val != null ) {
					val.resolve(null);
					if( val instanceof WrapedExpr) {
						WrapedExpr w = (WrapedExpr)val;
						if (w.expr instanceof TypeRef )
							val = (TypeRef)w.expr;
						else if (w.expr instanceof Struct) {
							Struct s = (Struct)w.expr;
							s.checkResolved();
							val = new TypeRef(s.type);
						}
						else
							throw new CompilerException(pos,"Unknown node of class "+w.expr.getClass());
					}
					else if( val instanceof TypeRef)
						;
					else if !( val instanceof Expr )
						throw new CompilerException(pos,"Unknown node of class "+val.getClass());
					if( val instanceof Expr )	{
						if( sw.mode == SwitchStat.ENUM_SWITCH ) {
							if( !(val instanceof StaticFieldAccessExpr) )
								throw new CompilerException(pos,"Wrong case in enum switch");
							StaticFieldAccessExpr f = (StaticFieldAccessExpr)val;
							Type et = sw.sel.getType();
							if( f.var.type != et )
								throw new CompilerException(pos,"Case of type "+f.var.type+" do not match switch expression of type "+et);
							if (et.isEnum())
								val = new ConstIntExpr(et.getStruct().getValueForEnumField(f.var));
							else
								val = (Expr)f.var.init.copy();
						}
						else if( sw.mode != SwitchStat.NORMAL_SWITCH )
							throw new CompilerException(pos,"Wrong case in normal switch");
					}
					else if( val instanceof TypeRef ) {
						this.type = Type.getRealType(sw.tmpvar.type,val.getType());
						pizza_case = true;
						Struct cas = this.type.getStruct();
						if( cas.isPizzaCase() ) {
							if( sw.mode != SwitchStat.PIZZA_SWITCH )
								throw new CompilerException(pos,"Pizza case type in non-pizza switch");
							PizzaCaseAttr case_attr = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
							val = new ConstIntExpr(case_attr.caseno);
							if( pattern.length > 0 ) {
								if( pattern.length != case_attr.casefields.length )
									throw new RuntimeException("Pattern containce "+pattern.length+" items, but case class "+cas+" has "+case_attr.casefields.length+" fields");
								for(int i=0, j=0; i < pattern.length; i++) {
									Var p = pattern[i];
									if( p.vtype == null || p.name.name.len == 1 && p.name.name.byteAt(0) == '_')
										continue;
									Type tp = Type.getRealType(sw.tmpvar.type,case_attr.casefields[i].type);
									if( !p.type.equals(tp) )
										throw new RuntimeException("Pattern variable "+p.name+" has type "+p.type+" but type "+tp+" is expected");
									p.init = new AccessExpr(p.pos,
											new CastExpr(p.pos,Type.getRealType(sw.tmpvar.type,cas.type),
												(Expr)new VarAccessExpr(p.pos,sw.tmpvar)),
											case_attr.casefields[i]
										);
									p.resolveDecl();
									addSymbol(j++,p);
								}
							}
						} else {
							if( sw.mode != SwitchStat.TYPE_SWITCH )
								throw new CompilerException(pos,"Type case in non-type switch");
							if( val.getType() == Type.tpObject ) {
								val = null;
								sw.defCase = this;
							} else {
								val = new ConstIntExpr(0);
							}
						}
					}
					else
						throw new CompilerException(pos,"Unknown node of class "+val.getClass());
				} else {
					sw.defCase = this;
					if( sw.mode == SwitchStat.TYPE_SWITCH )
						this.type = Type.tpObject;
				}
			} catch(Exception e ) { Kiev.reportError(pos,e); }

			BlockStat.resolveBlockStats(this, stats);

			if( val != null ) {
				if( !((Expr)val).isConstantExpr() )
					throw new RuntimeException("Case label "+val+" must be a constant expression but "+val.getClass()+" found");
				if( !((Expr)val).getType().isIntegerInCode() )
					throw new RuntimeException("Case label "+val+" must be of integer type");
			}
		} finally { PassInfo.pop(this); }
	}

	public CodeLabel getLabel() {
		if( case_label == null ) case_label = Code.newLabel();
		return case_label;
	}

	public void generate(Type reqType) {
		case_label = getLabel();
		PassInfo.push(this);
		try {
			try {
				Code.addInstr(Instr.set_label,case_label);
				if( val == null ) ((SwitchStat)parent).cosw.addDefault(case_label);
				else {
					Object v = ((Expr)val).getConstValue();
					if( v instanceof Number )
						((SwitchStat)parent).cosw.addCase( ((Number)v).intValue(), case_label);
					else if( v instanceof java.lang.Character )
						((SwitchStat)parent).cosw.addCase( (int)((java.lang.Character)v).charValue(), case_label);
					else
						throw new RuntimeException("Case label "+v+" must be of integer type");
				}
			} catch(Exception e ) { Kiev.reportError(pos,e); }
			for(int i=0; i < stats.length; i++) {
				try {
					stats[i].generate(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(stats[i].getPos(),e);
				}
			}
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( val == null )
			dmp.newLine(-1).append("default:").newLine();
		else
			dmp.newLine(-1).append("case ").append(val).append(':').newLine();
		dmp.append(stats).newLine(1);
		return dmp;
	}
}

@node
@cfnode
public class SwitchStat extends BlockStat implements BreakTarget {

	@att public ENode					sel;
	@ref public Var						tmpvar;
	@att public final NArr<CaseLabel>	cases;
	@ref public ASTNode					defCase;
	@ref private Field					typehash; // needed for re-resolving

	public CodeSwitch	cosw;
	protected CodeLabel	break_label = null;
	protected CodeLabel	continue_label = null;

	public static final int NORMAL_SWITCH = 0;
	public static final int PIZZA_SWITCH = 1;
	public static final int TYPE_SWITCH = 2;
	public static final int ENUM_SWITCH = 3;

	public int mode = NORMAL_SWITCH;

	public SwitchStat() {
	}

	public SwitchStat(int pos, ASTNode parent, ENode sel, CaseLabel[] cases) {
		super(pos, parent);
		this.sel = sel;
		this.cases.addAll(cases);
		defCase = null;
		setBreakTarget(true);
	}

	public String toString() { return "switch("+sel+")"; }

	public void cleanup() {
		parent=null;
		sel.cleanup();
		sel = null;
		tmpvar = null;
		foreach(ASTNode n; cases; n!=null) n.cleanup();
		cases = null;
		if( defCase != null ) {
			defCase.cleanup();
			defCase = null;
		}
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if( cases.length == 0 ) {
			ExprStat st = new ExprStat(pos,parent,(Expr)sel);
			this.replaceWithNodeResolve(Type.tpVoid, st);
		}
		else if( cases.length == 1 && cases[0].pattern.length == 0) {
			cases[0].resolve(Type.tpVoid);
			CaseLabel cas = (CaseLabel)cases[0];
			BlockStat bl = new BlockStat(cas.pos, null, cas.stats);
			bl.setBreakTarget(true);
			if( ((CaseLabel)cas).val == null ) {
				bl.stats.insert(new ExprStat(sel.pos,bl,sel),0);
				this.replaceWithNodeResolve(Type.tpVoid, bl);
				return;
			} else {
				IfElseStat st = new IfElseStat(pos,parent,
						new BinaryBoolExpr(sel.pos,BinaryOperator.Equals,sel,cas.val),
						bl,
						null
					);
				this.replaceWithNodeResolve(Type.tpVoid, st);
				return;
			}
		}
		if( tmpvar == null ) {
			BlockStat me = null;
			PassInfo.push(this);
			try {
				sel.resolve(Type.tpInt);
				Type tp = sel.getType();
				if( tp.isEnum() ) {
					mode = ENUM_SWITCH;
				}
				else if( tp.isReference() ) {
					tmpvar = new Var(sel.getPos(),KString.from(
						"tmp$sel$"+Integer.toHexString(sel.hashCode())),tp,0);
					me = new BlockStat(pos,parent);
					this.replaceWithNode(me);
					tmpvar.init = sel;
					me.addSymbol(tmpvar);
					me.addStatement(this);
					if( tp.isHasCases() ) {
						mode = PIZZA_SWITCH;
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						sel = cae;
						cae.pos = pos;
						cae.obj = new VarAccessExpr(tmpvar.pos,tmpvar);
						cae.obj.resolve(null);
						cae.func = new ASTIdentifier(pos, nameGetCaseTag);
					} else {
						mode = TYPE_SWITCH;
						typehash = new Field(KString.from("fld$sel$"+Integer.toHexString(sel.hashCode())),
							Type.tpTypeSwitchHash,ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
						PassInfo.clazz.addField(typehash);
						CallAccessExpr cae = new CallAccessExpr(pos,
							new StaticFieldAccessExpr(pos,PassInfo.clazz,typehash),
							Type.tpTypeSwitchHash.resolveMethod(KString.from("index"),KString.from("(Ljava/lang/Object;)I")),
							new Expr[]{new VarAccessExpr(pos,tmpvar)}
							);
						sel = cae;
					}
				}
			} catch(Exception e ) { Kiev.reportError(sel.getPos(),e);
			} finally { PassInfo.pop(this); }
			if( me != null ) {
				me.resolve(reqType);
				return;
			}
		}
		PassInfo.push(this);
		NodeInfoPass.pushState();
		ScopeNodeInfoVector result_state = null;
		ScopeNodeInfoVector case_states[] = new ScopeNodeInfoVector[cases.length];
		try {
			sel.resolve(Type.tpInt);
			KString[] typenames = new KString[0];
			int defindex = -1;
			for(int i=0; i < cases.length; i++) {
				boolean pushed_sni = false;
				try {
					case_states[i] = NodeInfoPass.pushState();
					pushed_sni = true;
					cases[i].resolve(Type.tpVoid);
					case_states[i] = NodeInfoPass.popState();
					pushed_sni = false;
					if( typehash != null ) {
						CaseLabel c = (CaseLabel)cases[i];
						if( c.type == null || !c.type.isReference() )
							throw new CompilerException(c.pos,"Mixed switch and typeswitch cases");
						KString name = c.type.getClazzName().name;
						typenames = (KString[])Arrays.append(typenames,name);
						if( c.val != null )
							c.val = new ConstIntExpr(i);
						else
							defindex = i;
					}
				}
				catch(Exception e ) { Kiev.reportError(cases[i].getPos(),e); }
				finally { if (pushed_sni) NodeInfoPass.popState(); }
				if( tmpvar!=null && i < cases.length-1 && !cases[i].isAbrupted() ) {
					Kiev.reportWarning(cases[i+1].pos, "Fall through to switch case");
				}
			}
			if( mode == TYPE_SWITCH ) {
				ConstExpr[] signs = new ConstExpr[typenames.length];
				for(int j=0; j < signs.length; j++)
					signs[j] = new ConstStringExpr(typenames[j]);
				if( defindex < 0 ) defindex = signs.length;
				typehash.init = new NewExpr(PassInfo.clazz.pos,Type.tpTypeSwitchHash,
					new Expr[]{ new NewInitializedArrayExpr(PassInfo.clazz.pos,new TypeRef(Type.tpString),1,signs),
						new ConstIntExpr(defindex)
					});
				Method clinit = PassInfo.clazz.getClazzInitMethod();
				((Initializer)clinit.body).addStatement(
					new ExprStat(typehash.init.getPos(),clinit.body,
						new AssignExpr(typehash.init.getPos(),AssignOperator.Assign
							,new StaticFieldAccessExpr(typehash.pos,PassInfo.clazz,typehash),new ShadowExpr(typehash.init))
					)
				);
			}
			for(int i=0; i < cases.length; i++) {
				for(int j=0; j < i; j++) {
					Expr vi = (Expr)((CaseLabel)cases[i]).val;
					Expr vj = (Expr)((CaseLabel)cases[j]).val;
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
					if( mode == ENUM_SWITCH ) {
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
							EnumAttr ea = null;
							ea = (EnumAttr)tp.getStruct().getAttr(attrEnum);
							if( ea.fields.length == cases.length )
								setMethodAbrupted(true);
						}
					}
					// Check if it's a pizza-type switch and all cases are
					// abrupted and all class's cases present
					// Check if all cases are abrupted
					else if( mode == PIZZA_SWITCH ) {
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
								PizzaCaseAttr case_attr;
								int caseno = 0;
								Struct tpclz = tp.getStruct();
								for(int i=0; i < tpclz.sub_clazz.length; i++) {
									if( tpclz.sub_clazz[i].isPizzaCase() ) {
										case_attr = (PizzaCaseAttr)tpclz.sub_clazz[i].getAttr(attrPizzaCase);
										if( case_attr!=null && case_attr.caseno > caseno )
											caseno = case_attr.caseno;
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
				Statement thrErr = new ThrowStat(pos,this,new NewExpr(pos,Type.tpError,Expr.emptyArray));
				CaseLabel dc = new CaseLabel(pos,this,null,new ENode[]{thrErr});
				cases.insert(dc,0);
				dc.resolve(Type.tpVoid);
			}
			if( mode == ENUM_SWITCH ) {
				Type tp = sel.getType();
				Expr cae = new CastExpr(pos,Type.tpInt,sel);
				cae.parent = sel.parent;
				sel = cae;
				sel.resolve(Type.tpInt);
			}
		} finally {
			result_state = NodeInfoPass.popState();
			PassInfo.pop(this);
			if( !isMethodAbrupted() ) {
				for(int i=0; i < case_states.length; i++) {
					if( !cases[i].isMethodAbrupted() && case_states[i] != null )
						result_state = NodeInfoPass.joinInfo(result_state, case_states[i]);
				}
				NodeInfoPass.addInfo(result_state);
			}
		}
		setResolved(true);
	}

	public CodeLabel getBreakLabel() {
		if( break_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'break' label");
		return break_label;
	}

	public CodeLabel getContinueLabel() {
		if( continue_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'continue' label");
		return continue_label;
	}

	public void generate(Type reqType) {
		int lo = Integer.MAX_VALUE;
		int hi = Integer.MIN_VALUE;

		int ntags = defCase==null? cases.length : cases.length-1;
		int[] tags = new int[ntags];

		for (int i=0, j=0; i < cases.length; i++) {
			if (((CaseLabel)cases[i]).val != null) {
				int val;
				Object v = ((Expr)((CaseLabel)cases[i]).val).getConstValue();
				if( v instanceof Number )
					val = ((Number)v).intValue();
				else if( v instanceof java.lang.Character )
					val = (int)((java.lang.Character)v).charValue();
				else
					throw new RuntimeException("Case label "+v+" must be of integer type");
				tags[j++] = val;
				if (val < lo) lo = val;
				if (hi < val) hi = val;
			}
		}

		long table_space_cost = (long)4 + (hi - lo + 1); // words
		long table_time_cost = 3; // comparisons
		long lookup_space_cost = (long)3 + 2 * ntags;
		long lookup_time_cost = ntags;
		boolean tabswitch =
			table_space_cost + 3 * table_time_cost <=
			lookup_space_cost + 3 * lookup_time_cost;

		PassInfo.push(this);
		continue_label = Code.newLabel();
		break_label = Code.newLabel();
		try {
			if( mode == TYPE_SWITCH ) {
				Code.addInstr(Instr.set_label,continue_label);
				sel.generate(null);
			} else {
				sel.generate(null);
				Code.addInstr(Instr.set_label,continue_label);
			}
			if( tabswitch ) {
				cosw = Code.newTableSwitch(lo,hi);
				Code.addInstr(Instr.op_tableswitch,cosw);
			} else {
				qsort(tags,0,tags.length-1);
				cosw = Code.newLookupSwitch(tags);
				Code.addInstr(Instr.op_lookupswitch,cosw);
			}
			
			for(int i=0; i < cases.length; i++) {
				if( isAutoReturnable() )
					cases[i].setAutoReturnable(true);
				((CaseLabel)cases[i]).generate(Type.tpVoid);
			}
			Vector<Var> vars = new Vector<Var>();
			for(int i=0; i < cases.length; i++) {
				foreach (ENode n; cases[i].stats; n instanceof VarDecl)
					vars.append(((VarDecl)n).var);
			}
			Code.removeVars(vars.toArray());

			Code.addInstr(Instr.set_label,break_label);
			Code.addInstr(Instr.switch_close,cosw);
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("switch").space().append('(')
			.append(sel).space().append(')').space().append('{').newLine(1);
		for(int i=0; i < cases.length; i++) dmp.append(cases[i]);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

	/** sort (int) arrays of keys and values
	 */
	static void qsort(int[] keys, int lo, int hi) {
		int i = lo;
		int j = hi;
		int pivot = keys[(i+j)/2];
		do {
			while (keys[i] < pivot) i++;
			while (pivot < keys[j]) j--;
			if (i <= j) {
				int temp = keys[i];
				keys[i] = keys[j];
				keys[j] = temp;
				i++;
				j--;
			}
		} while (i <= j);
		if (lo < j) qsort(keys, lo, j);
		if (i < hi) qsort(keys, i, hi);
	}
}

@node
@cfnode
public class CatchInfo extends Statement implements ScopeOfNames {

	static CatchInfo[] emptyArray = new CatchInfo[0];

	@att public Var			arg;
	@att public Statement	body;

	public CodeLabel		handler;
	public CodeCatchInfo	code_catcher;

	public CatchInfo() {
	}

//	public CatchInfo(int pos, ASTNode parent, Var arg, Statement body) {
//		super(pos, parent);
//		this.arg = arg;
//		this.body = body;
//	}

	public String toString() {
		return "catch( "+arg+" )";
	}

	public void cleanup() {
		parent=null;
		arg = null;
		body.cleanup();
		body = null;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
	{
		node ?= arg, ((Var)node).name.equals(name)
	}

	public void resolve(Type reqType) {
//		arg = (Var)arg.resolve();
		PassInfo.push(this);
		try {
			body.resolve(Type.tpVoid);
			if( body.isMethodAbrupted() ) setMethodAbrupted(true);
		} catch(Exception e ) {
			Kiev.reportError(body.pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void generate(Type reqType) {
		PassInfo.push(this);
		Code.addVar(arg);
		try {
			// This label must be created by TryStat's generate routine;
			Code.addInstr(Instr.enter_catch_handler,code_catcher);
			Code.addInstr(Instr.op_store,arg);
			body.generate(Type.tpVoid);
			if( !body.isMethodAbrupted() ) {
				if( ((TryStat)parent).finally_catcher != null ) {
					Code.addInstr(Instr.op_jsr,
						((FinallyInfo)((TryStat)parent).finally_catcher).subr_label);
				}
				if( isAutoReturnable() )
					ReturnStat.generateReturn();
				else
					Code.addInstr(Instr.op_goto,((TryStat)parent).end_label);
			}
			Code.addInstr(Instr.exit_catch_handler,code_catcher);
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally {
			Code.removeVar(arg);
			PassInfo.pop(this);
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("catch").space().append('(').space();
		arg.toJavaDecl(dmp).space().append(')').space().append(body);
		return dmp;
	}
}

@node
@cfnode
public class FinallyInfo extends CatchInfo {

	@att public Var		ret_arg;
	public CodeLabel	subr_label;

	public FinallyInfo() {
	}

//	public FinallyInfo(int pos, ASTNode parent, Statement body) {
//		super(pos,parent,new Var(pos,KString.Empty,Type.tpThrowable,0),body);
//		ret_arg = new Var(pos,KString.Empty,Type.tpObject,0);
//	}

	public String toString() { return "finally"; }

	public void resolve(Type reqType) {
		if (arg == null)
			arg = new Var(pos,KString.Empty,Type.tpThrowable,0);
		if (ret_arg == null)
			ret_arg = new Var(pos,KString.Empty,Type.tpObject,0);
		super.resolve(reqType);
	}
	
	public void generate(Type reqType) {
		PassInfo.push(this);
		try {
			CodeCatchInfo null_ci = null;
			// This label must be created by TryStat's generate routine;
			Code.addInstr(Instr.set_label,handler);
			Code.addInstr(Instr.enter_catch_handler,null_ci);
			Code.addVar(arg);
			Code.addInstr(Instr.op_store,arg);
			Code.addInstr(Instr.op_jsr,subr_label);
			Code.addInstr(Instr.op_load,arg);
			Code.addInstr(Instr.op_throw);
			Code.addInstr(Instr.exit_catch_handler,null_ci);

			// This label must be created by TryStat's generate routine;
			Code.addInstr(Instr.set_label,subr_label);
			Code.addInstr(Instr.enter_catch_handler,null_ci);
			Code.addInstr(Instr.op_store,ret_arg);

			body.generate(Type.tpVoid);
			Code.addInstr(Instr.op_ret,ret_arg);
		} catch(Exception e ) { Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); Code.removeVar(arg); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("finally").space().append(body).newLine();
		return dmp;
	}

}

@node
@cfnode
public class TryStat extends Statement/*defaults*/ {

	@att public Statement				body;
	@att public final NArr<CatchInfo>	catchers;
	@att public FinallyInfo				finally_catcher;

	public CodeLabel	end_label;

	public TryStat() {
	}

//	public TryStat(int pos, ASTNode parent, Statement body, ASTNode[] catchers, ASTNode finally_catcher) {
//		super(pos, parent);
//		this.body = body;
//		this.catchers.addAll(catchers);
//		this.finally_catcher = finally_catcher;
//	}

	public void cleanup() {
		parent=null;
		body.cleanup();
		body = null;
		foreach(ASTNode n; catchers; n!=null) n.cleanup();
		catchers = null;
		if( finally_catcher != null ) {
			finally_catcher.cleanup();
			finally_catcher = null;
		}
	}

	public void resolve(Type reqType) {
		ScopeNodeInfoVector finally_state = null;
		for(int i=0; i < catchers.length; i++) {
			try {
				NodeInfoPass.pushState();
				catchers[i].resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(catchers[i].pos,e);
			} finally {
				NodeInfoPass.popState();
			}
		}
		if(finally_catcher != null) {
			try {
				NodeInfoPass.pushState();
				finally_catcher.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(finally_catcher.pos,e);
			} finally {
				finally_state = NodeInfoPass.popState();
			}
		}
		PassInfo.push(this);
		NodeInfoPass.pushState();
		try {
			try {
				body.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
			}
			// Check if abrupted
			if( finally_catcher!= null && finally_catcher.isMethodAbrupted())
				setMethodAbrupted(true);
			else if( finally_catcher!= null && finally_catcher.isAbrupted())
				setMethodAbrupted(false);
			else {
				// Check that the body and all cases are abrupted
				boolean has_unabrupted_catcher = false;
				if( !body.isMethodAbrupted() ) has_unabrupted_catcher = true;
				else {
					for(int i=0; i < catchers.length; i++) {
						if( !catchers[i].isMethodAbrupted() ) {
							has_unabrupted_catcher = true;
							break;
						}
					}
				}
				if( !has_unabrupted_catcher ) setMethodAbrupted(true);
			}
		} finally {
			NodeInfoPass.popState();
			if( finally_state != null && !finally_catcher.isMethodAbrupted())
				NodeInfoPass.addInfo(finally_state);
			PassInfo.pop(this);
		}
	}

	public void generate(Type reqType) {
		// Generate labels for handlers
		if(finally_catcher != null) {
			Code.addVar(((FinallyInfo)finally_catcher).ret_arg);
			((FinallyInfo)finally_catcher).handler = Code.newLabel();
			((FinallyInfo)finally_catcher).subr_label = Code.newLabel();
			((FinallyInfo)finally_catcher).subr_label.check = false;
			((FinallyInfo)finally_catcher).code_catcher = Code.newCatcher(((FinallyInfo)finally_catcher).handler,null);
			Code.addInstr(Instr.start_catcher,((FinallyInfo)finally_catcher).code_catcher);
		}
		for(int i= catchers.length-1; i >= 0 ; i--) {
			((CatchInfo)catchers[i]).handler = Code.newLabel();
			((CatchInfo)catchers[i]).code_catcher = Code.newCatcher(((CatchInfo)catchers[i]).handler,((CatchInfo)catchers[i]).arg.type);
			Code.addInstr(Instr.start_catcher,((CatchInfo)catchers[i]).code_catcher);
		}
		end_label = Code.newLabel();

		PassInfo.push(this);
		try {
			try {
				if( isAutoReturnable() )
					body.setAutoReturnable(true);
				body.generate(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
			}
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() ) {
					ReturnStat.generateReturn();
				} else {
					if( finally_catcher != null )
						Code.addInstr(Instr.op_jsr,finally_catcher.subr_label);
					Code.addInstr(Instr.op_goto,end_label);
				}
			}
			for(int i=0; i < catchers.length; i++) {
				Code.addInstr(Instr.stop_catcher,catchers[i].code_catcher);
			}

			for(int i=0; i < catchers.length; i++) {
				if( isAutoReturnable() )
					catchers[i].setAutoReturnable(true);
				try {
					catchers[i].generate(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(catchers[i].pos,e);
				}
			}
			if(finally_catcher != null) {
				try {
					Code.addInstr(Instr.stop_catcher,finally_catcher.code_catcher);
					finally_catcher.generate(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(finally_catcher.pos,e);
				}
			}
			Code.addInstr(Instr.set_label,end_label);
		} finally {
			PassInfo.pop(this);
			if(finally_catcher != null)
				Code.removeVar(finally_catcher.ret_arg);
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("try").space().append(body).newLine();
		for(int i=0; i < catchers.length; i++)
			dmp.append(catchers[i]).newLine();
		if(finally_catcher != null)
			dmp.append(finally_catcher).newLine();
		return dmp;
	}

}

@node
@cfnode
public class SynchronizedStat extends Statement {

	@att public Statement	body;
	@att public ENode		expr;
	@att public Var			expr_var;
	public CodeLabel		handler;
	public CodeCatchInfo	code_catcher;
	public CodeLabel	end_label;

	public SynchronizedStat() {
	}

//	public SynchronizedStat(int pos, ASTNode parent, Expr expr, Statement body) {
//		super(pos, parent);
//		this.expr = expr;
//		this.body = body;
//	}

	public void cleanup() {
		parent=null;
		body.cleanup();
		body = null;
		expr.cleanup();
		expr = null;
		expr_var = null;
	}

	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			try {
				expr.resolve(null);
				expr_var = new Var(pos,KString.Empty,Type.tpObject,0);
			} catch(Exception e ) { Kiev.reportError(pos,e); }
			try {
				body.resolve(Type.tpVoid);
			} catch(Exception e ) { Kiev.reportError(pos,e); }
			setAbrupted(body.isAbrupted());
			setMethodAbrupted(body.isMethodAbrupted());
		} finally { PassInfo.pop(this); }
	}

	public void generate(Type reqType) {
		PassInfo.push(this);

		expr.generate(null);
		try {
			Code.addVar(expr_var);
			Code.addInstr(Instr.op_dup);
			Code.addInstr(Instr.op_store,expr_var);
			Code.addInstr(Instr.op_monitorenter);
			handler = Code.newLabel();
			end_label = Code.newLabel();
			code_catcher = Code.newCatcher(handler,null);
			Code.addInstr(Instr.start_catcher,code_catcher);
			try {
				if( isAutoReturnable() )
					body.setAutoReturnable(true);
				body.generate(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
			}
			Code.addInstr(Instr.stop_catcher,code_catcher);
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() )
					ReturnStat.generateReturn();
				else {
					Code.addInstr(Instr.op_load,expr_var);
					Code.addInstr(Instr.op_monitorexit);
					Code.addInstr(Instr.op_goto,end_label);
				}
			}

			Code.addInstr(Instr.set_label,handler);
			Code.stack_push(Type.tpThrowable);
			Code.addInstr(Instr.op_load,expr_var);
			Code.addInstr(Instr.op_monitorexit);
			Code.addInstr(Instr.op_throw);

			Code.addInstr(Instr.set_label,end_label);
		} finally {
			PassInfo.pop(this);
			Code.removeVar(expr_var);
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("synchronized").space().append('(').space().append(expr)
			.space().append(')').forsed_space().append(body).newLine();
		return dmp;
	}

}

@node
@cfnode
public class WithStat extends Statement {

	@att public Statement	body;
	@att public ENode		expr;
	@ref public ASTNode		var_or_field;
	public CodeLabel	end_label;

	public WithStat() {
	}

//	public WithStat(int pos, ASTNode parent, Expr expr, Statement body) {
//		super(pos, parent);
//		this.expr = expr;
//		this.body = body;
//	}

	public void cleanup() {
		parent=null;
		body.cleanup();
		body = null;
		expr.cleanup();
		expr = null;
		var_or_field = null;
	}

	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			try {
				expr.resolve(null);
				ENode e = expr;
				switch (e) {
				case VarAccessExpr:				var_or_field = ((VarAccessExpr)e).var;				break;
				case LocalPrologVarAccessExpr:	var_or_field = ((LocalPrologVarAccessExpr)e).var;	break;
				case AccessExpr:				var_or_field = ((AccessExpr)e).var;				break;
				case StaticFieldAccessExpr:		var_or_field = ((StaticFieldAccessExpr)e).var;		break;
				case AssignExpr:				e = ((AssignExpr)e).lval;							goto case e;
				}
				if (var_or_field == null) {
					Kiev.reportError(pos,"With statement needs variable or field argument");
					this.replaceWithNode(body);
					body.resolve(Type.tpVoid);
					return;
				}
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
				return;
			}

			boolean is_forward = var_or_field.isForward();
			if (!is_forward) var_or_field.setForward(true);
			try {
				body.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
			} finally {
				if (!is_forward) var_or_field.setForward(false);
			}

			setAbrupted(body.isAbrupted());
			setMethodAbrupted(body.isMethodAbrupted());
		} finally { PassInfo.pop(this); }
	}

	public void generate(Type reqType) {
		PassInfo.push(this);

		try {
			end_label = Code.newLabel();
			try {
				if (expr instanceof AssignExpr)
					expr.generate(Type.tpVoid);
				if( isAutoReturnable() )
					body.setAutoReturnable(true);
				body.generate(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
			}
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() )
					ReturnStat.generateReturn();
			}

			Code.addInstr(Instr.set_label,end_label);
		} finally {
			PassInfo.pop(this);
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("/*with ").space().append('(').space().append(expr)
			.space().append(")*/").forsed_space().append(body).newLine();
		return dmp;
	}
}

