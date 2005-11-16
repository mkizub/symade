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
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
@dflow(in="this:in()", out="stats")
public class CaseLabel extends ENode implements ScopeOfNames {

	public static final CaseLabel[] emptyArray = new CaseLabel[0];

	@att
	public ENode				val;
	
	@ref
	public Type					type;
	
	@att
	@dflow(in="", seq="true")
	public final NArr<Var>		pattern;
	
	@att
	@dflow(in="pattern", seq="true")
	public final NArr<ENode>	stats;
	
	public CodeLabel	case_label;

	public CaseLabel() {
	}

	public CaseLabel(int pos, ENode val, ENode[] stats) {
		super(pos);
		this.val = val;
		this.stats.addAll(stats);
	}

	static class CaseLabelDFFuncIn extends DFFunc {
		final int res_idx;
		CaseLabelDFFuncIn(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			CaseLabel cl = (CaseLabel)dfi.node;
			if (cl.parent instanceof SwitchStat) {
				ENode sel = ((SwitchStat)cl.parent).sel;
				if (sel != null)
					res = sel.getDFlow().out();
			}
			if (cl.pprev != null) {
				DFState prev = cl.pprev.getDFlow().out();
				if (res != null)
					res = DFState.join(res,prev);
				else
					res = prev;
			}
			if (res != null)
				dfi.setResult(res_idx, res);
			else
				res = DFState.makeNewState();
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new CaseLabelDFFuncIn(dfi);
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

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
		Var@ var;
	{
		var @= pattern,
		var.name.equals(name),
		node ?= var
	}
	
	public void resolve(Type tpVoid) {
		boolean pizza_case = false;
		SwitchStat sw = (SwitchStat)parent;
		try {
			if( val != null ) {
				val.resolve(null);
				if( val instanceof TypeRef)
					;
				else if !( val instanceof Expr )
					throw new CompilerException(this,"Unknown node of class "+val.getClass());
				if( val instanceof Expr )	{
					if( sw.mode == SwitchStat.ENUM_SWITCH ) {
						if( !(val instanceof StaticFieldAccessExpr) )
							throw new CompilerException(this,"Wrong case in enum switch");
						StaticFieldAccessExpr f = (StaticFieldAccessExpr)val;
						Type et = sw.sel.getType();
						if( f.var.type != et )
							throw new CompilerException(this,"Case of type "+f.var.type+" do not match switch expression of type "+et);
						if (et.isEnum())
							val = new ConstIntExpr(et.getStruct().getValueForEnumField(f.var));
						else
							val = (Expr)f.var.init.copy();
					}
					else if( sw.mode != SwitchStat.NORMAL_SWITCH )
						throw new CompilerException(this,"Wrong case in normal switch");
				}
				else if( val instanceof TypeRef ) {
					this.type = Type.getRealType(sw.tmpvar.getType(),val.getType());
					pizza_case = true;
					Struct cas = this.type.getStruct();
					if( cas.isPizzaCase() ) {
						if( sw.mode != SwitchStat.PIZZA_SWITCH )
							throw new CompilerException(this,"Pizza case type in non-pizza switch");
						PizzaCaseAttr case_attr = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
						val = new ConstIntExpr(case_attr.caseno);
						if( pattern.length > 0 ) {
							if( pattern.length != case_attr.casefields.length )
								throw new RuntimeException("Pattern containce "+pattern.length+" items, but case class "+cas+" has "+case_attr.casefields.length+" fields");
							for(int i=0, j=0; i < pattern.length; i++) {
								Var p = pattern[i];
								if( p.vtype == null || p.name.name.len == 1 && p.name.name.byteAt(0) == '_')
									continue;
								Type tp = Type.getRealType(sw.tmpvar.getType(),case_attr.casefields[i].type);
								if( !p.type.equals(tp) )
									throw new RuntimeException("Pattern variable "+p.name+" has type "+p.type+" but type "+tp+" is expected");
								p.init = new AccessExpr(p.pos,
										new CastExpr(p.pos,Type.getRealType(sw.tmpvar.getType(),cas.type),
											(Expr)new VarExpr(p.pos,sw.tmpvar.getVar())),
										case_attr.casefields[i]
									);
//									addSymbol(j++,p);
								p.resolveDecl();
							}
						}
					} else {
						if( sw.mode != SwitchStat.TYPE_SWITCH )
							throw new CompilerException(this,"Type case in non-type switch");
						if( val.getType() == Type.tpObject ) {
							val = null;
							sw.defCase = this;
						} else {
							val = new ConstIntExpr(0);
						}
					}
				}
				else
					throw new CompilerException(this,"Unknown node of class "+val.getClass());
			} else {
				sw.defCase = this;
				if( sw.mode == SwitchStat.TYPE_SWITCH )
					this.type = Type.tpObject;
			}
		} catch(Exception e ) { Kiev.reportError(this,e); }

		BlockStat.resolveBlockStats(this, stats);

		if( val != null ) {
			if( !((Expr)val).isConstantExpr() )
				throw new RuntimeException("Case label "+val+" must be a constant expression but "+val.getClass()+" found");
			if( !((Expr)val).getType().isIntegerInCode() )
				throw new RuntimeException("Case label "+val+" must be of integer type");
		}
	}

	public CodeLabel getLabel() {
		if( case_label == null ) case_label = Code.newLabel();
		return case_label;
	}

	public void generate(Type reqType) {
		case_label = getLabel();
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
		} catch(Exception e ) { Kiev.reportError(this,e); }
		Vector<Var> vars = null;
		if (pattern.length > 0) {
			vars = new Vector<Var>();
			foreach (Var p; pattern; p.vtype != null && !(p.name.name.len == 1 && p.name.name.byteAt(0) == '_')) {
				vars.append(p);
				p.generate(Type.tpVoid);
			}
		}
		for(int i=0; i < stats.length; i++) {
			try {
				stats[i].generate(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
		if (vars != null)
			Code.removeVars(vars.toArray());
	}

	public Dumper toJava(Dumper dmp) {
		if( val == null )
			dmp.newLine(-1).append("default:").newLine();
		else
			dmp.newLine(-1).append("case ").append(val).append(':').newLine();
		dmp.newLine(1);
		foreach (ENode s; stats)
			s.toJava(dmp);
		dmp.newLine(-1);

		return dmp;
	}
}

@node
@dflow(out="lblbrk")
public class SwitchStat extends Statement implements BreakTarget {

	@dflow
	@att public ENode					sel;

	@dflow(in="sel", seq="false")
	@att public final NArr<CaseLabel>	cases;

	@att public VarExpr					tmpvar;
	@ref public ASTNode					defCase;
	@ref private Field					typehash; // needed for re-resolving

	@att
	@dflow(in="cases")
	public Label						lblcnt;

	@att
	@dflow(in="cases")
	public Label						lblbrk;

	public CodeSwitch	cosw;
	

	public static final int NORMAL_SWITCH = 0;
	public static final int PIZZA_SWITCH = 1;
	public static final int TYPE_SWITCH = 2;
	public static final int ENUM_SWITCH = 3;

	public int mode = NORMAL_SWITCH;

	public SwitchStat() {
		this.lblcnt = new Label();
		this.lblbrk = new Label();
	}

	public SwitchStat(int pos, ENode sel, CaseLabel[] cases) {
		super(pos);
		this.lblcnt = new Label();
		this.lblbrk = new Label();
		this.sel = sel;
		this.cases.addAll(cases);
		defCase = null;
		setBreakTarget(true);
	}

	public String toString() { return "switch("+sel+")"; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if( cases.length == 0 ) {
			ExprStat st = new ExprStat(pos,(ENode)~sel);
			this.replaceWithNodeResolve(Type.tpVoid, st);
		}
		else if( cases.length == 1 && cases[0].pattern.length == 0) {
			cases[0].resolve(Type.tpVoid);
			CaseLabel cas = (CaseLabel)cases[0];
			BlockStat bl = new BlockStat(cas.pos, cas.stats.delToArray());
			bl.setBreakTarget(true);
			if( ((CaseLabel)cas).val == null ) {
				bl.stats.insert(new ExprStat(sel.pos,(ENode)~sel),0);
				this.replaceWithNodeResolve(Type.tpVoid, bl);
				return;
			} else {
				IfElseStat st = new IfElseStat(pos,
						new BinaryBoolExpr(sel.pos,BinaryOperator.Equals,(ENode)~sel,(ENode)~cas.val),
						bl,
						null
					);
				this.replaceWithNodeResolve(Type.tpVoid, st);
				return;
			}
		}
		if( tmpvar == null ) {
			BlockStat me = null;
			try {
				sel.resolve(Type.tpInt);
				Type tp = sel.getType();
				if( tp.isEnum() ) {
					mode = ENUM_SWITCH;
				}
				else if( tp.isReference() ) {
					tmpvar = new VarExpr(sel.pos, new Var(sel.pos,KString.from(
						"tmp$sel$"+Integer.toHexString(sel.hashCode())),tp,0));
					me = new BlockStat(pos);
					this.replaceWithNode(me);
					ENode old_sel = (ENode)~this.sel;
					tmpvar.getVar().init = old_sel;
					me.addSymbol(tmpvar.getVar());
					me.addStatement(this);
					if( tp.isHasCases() ) {
						mode = PIZZA_SWITCH;
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						sel = cae;
						cae.pos = pos;
						cae.obj = new VarExpr(tmpvar.pos,tmpvar.getVar());
						cae.obj.resolve(null);
						cae.func = new NameRef(pos, nameGetCaseTag);
					} else {
						mode = TYPE_SWITCH;
						typehash = new Field(KString.from("fld$sel$"+Integer.toHexString(old_sel.hashCode())),
							Type.tpTypeSwitchHash,ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
						pctx.clazz.addField(typehash);
						CallExpr cae = new CallExpr(pos,
							new StaticFieldAccessExpr(pos,typehash),
							Type.tpTypeSwitchHash.resolveMethod(KString.from("index"),KString.from("(Ljava/lang/Object;)I")),
							new Expr[]{new VarExpr(pos,tmpvar.getVar())}
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
					KString name = c.type.getClazzName().name;
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
		if( mode == TYPE_SWITCH ) {
			ConstExpr[] signs = new ConstExpr[typenames.length];
			for(int j=0; j < signs.length; j++)
				signs[j] = new ConstStringExpr(typenames[j]);
			if( defindex < 0 ) defindex = signs.length;
			typehash.init = new NewExpr(pctx.clazz.pos,Type.tpTypeSwitchHash,
				new Expr[]{ new NewInitializedArrayExpr(pctx.clazz.pos,new TypeRef(Type.tpString),1,signs),
					new ConstIntExpr(defindex)
				});
			Constructor clinit = pctx.clazz.getClazzInitMethod();
			clinit.body.addStatement(
				new ExprStat(typehash.init.getPos(),
					new AssignExpr(typehash.init.getPos(),AssignOperator.Assign
						,new StaticFieldAccessExpr(typehash.pos,typehash),new ShadowExpr(typehash.init))
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
			Statement thrErr = new ThrowStat(pos,new NewExpr(pos,Type.tpError,Expr.emptyArray));
			CaseLabel dc = new CaseLabel(pos,null,new ENode[]{thrErr});
			cases.insert(dc,0);
			dc.resolve(Type.tpVoid);
		}
		if( mode == ENUM_SWITCH ) {
			Type tp = sel.getType();
			sel = new CastExpr(pos,Type.tpInt,(ENode)~sel);
			sel.resolve(Type.tpInt);
		}
		setResolved(true);
	}

	public Label getCntLabel() {
		return lblcnt;
	}
	public Label getBrkLabel() {
		return lblbrk;
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

		try {
			if( mode == TYPE_SWITCH ) {
				lblcnt.generate(null);
				sel.generate(null);
			} else {
				sel.generate(null);
				lblcnt.generate(null);
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

			lblbrk.generate(null);
			Code.addInstr(Instr.switch_close,cosw);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
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
@dflow(out="body")
public class CatchInfo extends Statement implements ScopeOfNames {

	static CatchInfo[] emptyArray = new CatchInfo[0];

	@att
	@dflow(in="this:in")
	public Var				arg;
	@att
	@dflow(in="arg")
	public Statement		body;

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

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
	{
		node ?= arg, ((Var)node).name.equals(name)
	}

	public void resolve(Type reqType) {
		try {
			body.resolve(Type.tpVoid);
			if( body.isMethodAbrupted() ) setMethodAbrupted(true);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
	}

	public void generate(Type reqType) {
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
					ReturnStat.generateReturn(this);
				else
					Code.addInstr(Instr.op_goto,((TryStat)parent).end_label);
			}
			Code.addInstr(Instr.exit_catch_handler,code_catcher);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		} finally {
			Code.removeVar(arg);
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("catch").space().append('(').space();
		arg.toJavaDecl(dmp).space().append(')').space().append(body);
		return dmp;
	}
}

@node
@dflow(out="body")
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
		} catch(Exception e ) { Kiev.reportError(this,e);
		} finally { Code.removeVar(arg); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("finally").space().append(body).newLine();
		return dmp;
	}

}

@node
@dflow(out="body")
public class TryStat extends Statement/*defaults*/ {

	@att
	@dflow(in="")
	public Statement				body;
	
	@att
	@dflow(in="", seq="false")
	public final NArr<CatchInfo>	catchers;
	
	@att
	@dflow(in="")
	public FinallyInfo				finally_catcher;

	public CodeLabel	end_label;

	public TryStat() {
	}

	public void resolve(Type reqType) {
		for(int i=0; i < catchers.length; i++) {
			try {
				catchers[i].resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(catchers[i],e);
			}
		}
		if(finally_catcher != null) {
			try {
				finally_catcher.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(finally_catcher,e);
			}
		}
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
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
	}

	public void generate(Type reqType) {
		// Generate labels for handlers
		if(finally_catcher != null) {
			Code.addVar(finally_catcher.ret_arg);
			finally_catcher.handler = Code.newLabel();
			finally_catcher.subr_label = Code.newLabel();
			finally_catcher.subr_label.check = false;
			finally_catcher.code_catcher = Code.newCatcher(finally_catcher.handler,null);
			Code.addInstr(Instr.start_catcher,finally_catcher.code_catcher);
		}
		for(int i= catchers.length-1; i >= 0 ; i--) {
			catchers[i].handler = Code.newLabel();
			catchers[i].code_catcher = Code.newCatcher(catchers[i].handler,catchers[i].arg.type);
			Code.addInstr(Instr.start_catcher,catchers[i].code_catcher);
		}
		end_label = Code.newLabel();

		try {
			try {
				if( isAutoReturnable() )
					body.setAutoReturnable(true);
				body.generate(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() ) {
					ReturnStat.generateReturn(this);
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
					Kiev.reportError(catchers[i],e);
				}
			}
			if(finally_catcher != null) {
				try {
					Code.addInstr(Instr.stop_catcher,finally_catcher.code_catcher);
					finally_catcher.generate(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(finally_catcher,e);
				}
			}
			Code.addInstr(Instr.set_label,end_label);
		} finally {
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
@dflow(out="body")
public class SynchronizedStat extends Statement {

	@att
	@dflow(in="this:in")
	public ENode		expr;
	@att
	public Var			expr_var;
	
	@att
	@dflow(in="expr")
	public Statement	body;
	
	public CodeLabel		handler;
	public CodeCatchInfo	code_catcher;
	public CodeLabel	end_label;

	public SynchronizedStat() {
	}

	public void resolve(Type reqType) {
		try {
			expr.resolve(null);
			expr_var = new Var(pos,KString.Empty,Type.tpObject,0);
		} catch(Exception e ) { Kiev.reportError(this,e); }
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) { Kiev.reportError(this,e); }
		setAbrupted(body.isAbrupted());
		setMethodAbrupted(body.isMethodAbrupted());
	}

	public void generate(Type reqType) {
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
				Kiev.reportError(this,e);
			}
			Code.addInstr(Instr.stop_catcher,code_catcher);
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() )
					ReturnStat.generateReturn(this);
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
@dflow(out="body")
public class WithStat extends Statement {

	@att
	@dflow(in="this:in")
	public ENode		expr;
	
	@att
	@dflow(in="expr")
	public Statement	body;
	
	@ref
	public ASTNode		var_or_field;
	public CodeLabel	end_label;

	public WithStat() {
	}

	public void resolve(Type reqType) {
		try {
			expr.resolve(null);
			ENode e = expr;
			switch (e) {
			case VarExpr:					var_or_field = ((VarExpr)e).getVar();				break;
			case AccessExpr:				var_or_field = ((AccessExpr)e).var;				break;
			case StaticFieldAccessExpr:		var_or_field = ((StaticFieldAccessExpr)e).var;		break;
			case AssignExpr:				e = ((AssignExpr)e).lval;							goto case e;
			}
			if (var_or_field == null) {
				Kiev.reportError(this,"With statement needs variable or field argument");
				this.replaceWithNode(body);
				body.resolve(Type.tpVoid);
				return;
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
			return;
		}

		boolean is_forward = var_or_field.isForward();
		if (!is_forward) var_or_field.setForward(true);
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		} finally {
			if (!is_forward) var_or_field.setForward(false);
		}

		setAbrupted(body.isAbrupted());
		setMethodAbrupted(body.isMethodAbrupted());
	}

	public void generate(Type reqType) {
		end_label = Code.newLabel();
		try {
			if (expr instanceof AssignExpr)
				expr.generate(Type.tpVoid);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
		if( !body.isMethodAbrupted() ) {
			if( isAutoReturnable() )
				ReturnStat.generateReturn(this);
		}

		Code.addInstr(Instr.set_label,end_label);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("/*with ").space().append('(').space().append(expr)
			.space().append(")*/").forsed_space().append(body).newLine();
		return dmp;
	}
}

