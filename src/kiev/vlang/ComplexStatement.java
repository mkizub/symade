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

public class CaseLabel extends ASTNode {

	static CaseLabel[] emptyArray = new CaseLabel[0];

	public ASTNode		val;
	public Type			type;
	public Var[]		pattern;
	public BlockStat	stats;

	public CodeLabel	case_label;

	public CaseLabel(int pos, ASTNode parent, ASTNode val, ASTNode[] stats) {
		super(pos,parent);
		this.val = val;
		if( val != null && val instanceof Expr )
			this.val.parent = this;
		this.stats = new BlockStat(pos,this,stats);
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public String toString() {
		if( val == null ) return "default:";
		else if(pattern != null) {
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

	public Statement addStatement(int i, Statement st) {
		if( st == null ) return null;
		stats.stats = (ASTNode[])Arrays.insert(stats.stats,st,i);
		return st;
	}

	public void cleanup() {
		parent=null;
		val.cleanup();
		val = null;
		type = null;
		pattern = null;
		stats.cleanup();
		stats = null;
	}

	public ASTNode resolve(Type tpVoid) throws RuntimeException {
		boolean pizza_case = false;
		PassInfo.push(this);
		try {
			SwitchStat sw = (SwitchStat)parent;
			try {
				if( val != null ) {
					ASTNode v;
					if( val instanceof Expr ) v = ((Expr)val).resolve(null);
					else v = val;
					if( v instanceof WrapedExpr && ((WrapedExpr)v).expr instanceof Struct )
						v = ((WrapedExpr)v).expr;
					if( v instanceof Expr )	{
						if( sw.mode == SwitchStat.ENUM_SWITCH ) {
							if( !(v instanceof StaticFieldAccessExpr) )
								throw new CompilerException(pos,"Wrong case in enum switch");
							StaticFieldAccessExpr f = (StaticFieldAccessExpr)v;
							Type et = sw.sel.getType();
							if( f.var.type != et )
								throw new CompilerException(pos,"Case of type "+f.var.type+" do not match switch expression of type "+et);
							if (et.clazz.isPrimitiveEnum())
								et = et.clazz.getPrimitiveEnumType();
							if (et.clazz.isEnum() && !et.clazz.isPrimitiveEnum())
								val = new ConstExpr(pos,Kiev.newInteger(et.clazz.getValueForEnumField(f.var)));
							else
								val = new ConstExpr(pos,((ConstExpr)f.var.init).value);
						}
						else if( sw.mode != SwitchStat.NORMAL_SWITCH )
							throw new CompilerException(pos,"Wrong case in normal switch");
						else
							val = (Expr)v;
					}
					else if( v instanceof Struct ) {
						((Struct)v).checkResolved();
						v = Type.getRealType(sw.tmpvar.type,v.type).clazz;
						this.type = ((Struct)v).type;
						pizza_case = true;
						Struct cas = (Struct)v;
						if( cas.isPizzaCase() ) {
							if( sw.mode != SwitchStat.PIZZA_SWITCH )
								throw new CompilerException(pos,"Pizza case type in non-pizza switch");
							PizzaCaseAttr case_attr = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
							val = new ConstExpr(val.getPos(),Kiev.newInteger(case_attr.caseno));
							if( pattern != null && pattern.length > 0 ) {
								if( pattern.length != case_attr.casefields.length )
									throw new RuntimeException("Pattern containce "+pattern.length+" items, but case class "+cas+" has "+case_attr.casefields.length+" fields");
								for(int i=0, j=0; i < pattern.length; i++) {
									if( pattern[i]==null ) continue;
									pattern[i] = (Var)pattern[i].resolve(null);
									Type tp = Type.getRealType(sw.tmpvar.type,case_attr.casefields[i].type);
									if( !pattern[i].type.equals(tp) )
										throw new RuntimeException("Pattern variable "+pattern[i].name+" has type "+pattern[i].type+" but type "+tp+" is expected");
									Expr init =
										new AccessExpr(pattern[i].pos,
											new CastExpr(pattern[i].pos,Type.getRealType(sw.tmpvar.type,cas.type),
												(Expr)new VarAccessExpr(pattern[i].pos,sw.tmpvar).resolve(null)),
											case_attr.casefields[i]
										);
									addStatement(j++,new DeclStat(pattern[i].pos,stats,pattern[i],init));
								}
							}
						} else {
							if( sw.mode != SwitchStat.TYPE_SWITCH )
								throw new CompilerException(pos,"Type case in non-type switch");
							if( v.equals(Type.tpObject.clazz) ) {
								val = null;
								sw.defCase = this;
							} else {
								val = new ConstExpr(val.getPos(),Kiev.newInteger(0));
							}
						}
					}
					else
						throw new CompilerException(pos,"Unknown node of class "+v.getClass());
				} else {
					sw.defCase = this;
					if( sw.mode == SwitchStat.TYPE_SWITCH )
						this.type = Type.tpObject;
				}
			} catch(Exception e ) { Kiev.reportError(pos,e); }

			//this.stats.resolveBlockStats();
			this.stats.resolve(Type.tpVoid);
			this.setAbrupted(stats.isAbrupted());
			this.setMethodAbrupted(stats.isMethodAbrupted());

			if( val != null ) {
				if( !((Expr)val).isConstantExpr() )
					throw new RuntimeException("Case label "+val+" must be a constant expression but "+val.getClass()+" found");
				if( !((Expr)val).getType().isIntegerInCode() )
					throw new RuntimeException("Case label "+val+" must be of integer type");
			}
		} finally { PassInfo.pop(this); }
		return this;
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
			if (isAutoReturnable()) stats.setAutoReturnable(true);
			stats.generate(Type.tpVoid);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( val == null )
			dmp.newLine(-1).append("default:").newLine();
		else
			dmp.newLine(-1).append("case ").append(val).append(':').newLine();
		dmp.append(stats).newLine(1);
//		for(int i=0; i < stats.length; i++) {
//			if( stats[i] == null ) dmp.append(';');
//			else dmp.append(stats[i]).newLine();
//		}
		return dmp;
	}
}

public class SwitchStat extends BlockStat implements BreakTarget {

	public Expr			sel;
	public Var			tmpvar;
	public Field		typehash;
	public ASTNode[]	cases = ASTNode.emptyArray;
	public ASTNode		defCase;

	public CodeSwitch	cosw;
	protected CodeLabel	break_label = null;
	protected CodeLabel	continue_label = null;

	public static final int NORMAL_SWITCH = 0;
	public static final int PIZZA_SWITCH = 1;
	public static final int TYPE_SWITCH = 2;
	public static final int ENUM_SWITCH = 3;

	public int mode = NORMAL_SWITCH;

	public SwitchStat(int pos, ASTNode parent, Expr sel, ASTNode[] cases) {
		super(pos, parent);
		this.sel = sel;
		this.sel.parent = this;
		this.cases = cases;
		for(int i=0; i < cases.length; i++) cases[i].parent = this;
		defCase = null;
		setBreakTarget(true);
	}

	public String toString() { return "switch("+sel+")"; }

//	public void addCase(CaseLabel c) throws RuntimeException {
//		if( c.val == null && defCase != null )
//			throw new RuntimeException("More than one default case in switch statement");
//		if( c.val == null ) defCase = c;
//		cases = (CaseLabel[])Arrays.append(cases,c);
//	}

	public void cleanup() {
		parent=null;
		sel.cleanup();
		sel = null;
		tmpvar = null;
		typehash = null;
		foreach(ASTNode n; cases; n!=null) n.cleanup();
		cases = null;
		if( defCase != null ) {
			defCase.cleanup();
			defCase = null;
		}
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		if( cases.length == 0 ) return new ExprStat(pos,parent,sel).resolve(Type.tpVoid);
		else if( cases.length == 1 && cases[0] instanceof ASTNormalCase) {
			CaseLabel cas = (CaseLabel)((ASTNormalCase)cases[0]).resolve(Type.tpVoid);
			BlockStat st = cas.stats;
			st.setBreakTarget(true);
			st = (BlockStat)st;
			if( ((CaseLabel)cas).val == null ) {
				st.stats = (ASTNode[])Arrays.insert(st.stats,new ExprStat(sel.pos,st,sel),0);
				return st.resolve(reqType);
			} else {
				return new IfElseStat(pos,parent,
						new BinaryBooleanExpr(sel.pos,BinaryOperator.Equals,sel,
						(Expr)((Expr)((CaseLabel)cas).val).resolve(Type.tpInt)),st,null
					).resolve(Type.tpVoid);
			}
		}
		BlockStat me = null;
		if( tmpvar == null ) {
			PassInfo.push(this);
			try {
				sel = (Expr)sel.resolve(Type.tpInt);
				Type tp = sel.getType();
				if( tp.clazz.isEnum() ) {
					mode = ENUM_SWITCH;
				}
				else if( tp.isReference() ) {
//					if( sel instanceof VarAccessExpr ) {
//						tmpvar = ((VarAccessExpr)sel).var;
//					} else {
						tmpvar = new Var(sel.getPos(),KString.from(
							"tmp$sel$"+Integer.toHexString(sel.hashCode())),tp,0);
						Expr init = sel;
						me = new BlockStat(pos,parent);
						Statement ds = new DeclStat(tmpvar.pos,me,tmpvar,init);
						me.addStatement(ds);
						me.addStatement(this);
//					}
					if( tp.clazz.isHasCases() ) {
						mode = PIZZA_SWITCH;
						ASTCallAccessExpression cae = new ASTCallAccessExpression(0);
						cae.pos = pos;
						cae.obj = (Expr)new VarAccessExpr(tmpvar.pos,tmpvar).resolve(null);
						cae.func = nameGetCaseTag;
						cae.parent = sel.parent;
						sel = cae;
					} else {
						mode = TYPE_SWITCH;
						typehash = new Field(PassInfo.clazz,
							KString.from("fld$sel$"+Integer.toHexString(sel.hashCode())),
							Type.tpTypeSwitchHash,ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
						PassInfo.clazz.addField(typehash);
						CallAccessExpr cae = new CallAccessExpr(pos,this,
							new StaticFieldAccessExpr(pos,PassInfo.clazz,typehash),
							Type.tpTypeSwitchHash.clazz.resolveMethod(KString.from("index"),KString.from("(Ljava/lang/Object;)I")),
							new Expr[]{new VarAccessExpr(pos,tmpvar)}
							);
						sel = cae;
					}
				}
			} catch(Exception e ) { Kiev.reportError(sel.getPos(),e);
			} finally { PassInfo.pop(this); }
		}
		if( me != null ) return me.resolve(reqType);
		PassInfo.push(this);
		NodeInfoPass.pushState();
		ScopeNodeInfoVector result_state = null;
		ScopeNodeInfoVector case_states[] = new ScopeNodeInfoVector[cases.length];
		try {
			sel = (Expr)sel.resolve(Type.tpInt);
			KString[] typenames = new KString[0];
			int defindex = -1;
			for(int i=0; i < cases.length; i++) {
				try {
					case_states[i] = NodeInfoPass.pushState();
					if( cases[i] instanceof ASTNormalCase ) {
						cases[i] = (CaseLabel)((ASTNormalCase)cases[i]).resolve(Type.tpVoid);
					}
					else if( cases[i] instanceof ASTPizzaCase ) {
						cases[i] = (CaseLabel)((ASTPizzaCase)cases[i]).resolve(Type.tpVoid);
					}
					else if( cases[i] instanceof CaseLabel ) {
						cases[i] = (CaseLabel)((CaseLabel)cases[i]).resolve(Type.tpVoid);
					}
					else
						throw new CompilerException(cases[i].pos,"Unknown type of case");
					case_states[i] = NodeInfoPass.popState();
					if( typehash != null ) {
						CaseLabel c = (CaseLabel)cases[i];
						if( c.type == null || !c.type.isReference() )
							throw new CompilerException(c.pos,"Mixed switch and typeswitch cases");
						KString name = c.type.clazz.name.name;
						typenames = (KString[])Arrays.append(typenames,name);
						if( c.val != null )
							c.val = new ConstExpr(c.val.getPos(),Kiev.newInteger(i));
						else
							defindex = i;
					}
				} catch(Exception e ) { Kiev.reportError(cases[i].getPos(),e); }
				if( tmpvar!=null && i < cases.length-1 && !cases[i].isAbrupted() ) {
					Kiev.reportWarning(cases[i+1].pos, "Fall through to switch case");
				}
			}
			if( mode == TYPE_SWITCH ) {
				ConstExpr[] signs = new ConstExpr[typenames.length];
				for(int j=0; j < signs.length; j++)
					signs[j] = new ConstExpr(PassInfo.clazz.pos,typenames[j]);
				if( defindex < 0 ) defindex = signs.length;
				typehash.init = new NewExpr(PassInfo.clazz.pos,Type.tpTypeSwitchHash,
					new Expr[]{ new NewInitializedArrayExpr(PassInfo.clazz.pos,Type.tpString,1,signs),
						new ConstExpr(PassInfo.clazz.pos,Kiev.newInteger(defindex))
					});
				Method clinit = null;
				foreach(Method m; PassInfo.clazz.methods; m.name.equals(nameClassInit)) {
					clinit = m; break;
				}
				if( clinit == null ) {
					clinit = new Method(PassInfo.clazz,nameClassInit,
						MethodType.newMethodType(null,null,null,Type.tpVoid),ACC_STATIC);
					clinit.pos = PassInfo.clazz.pos;
					PassInfo.clazz.addMethod(clinit);
					clinit.body = new BlockStat(PassInfo.clazz.pos,clinit);
				}
				((BlockStat)clinit.body).addStatement(
					new ExprStat(typehash.init.getPos(),clinit.body,
						new AssignExpr(typehash.init.getPos(),AssignOperator.Assign
							,new StaticFieldAccessExpr(typehash.pos,PassInfo.clazz,typehash),typehash.init)
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
							if (tp.clazz.isPrimitiveEnum())
								ea = (EnumAttr)tp.clazz.getAttr(attrPrimitiveEnum);
							else
								ea = (EnumAttr)tp.clazz.getAttr(attrEnum);
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
								for(int i=0; i < tp.clazz.sub_clazz.length; i++) {
									if( tp.clazz.sub_clazz[i].isPizzaCase() ) {
										case_attr = (PizzaCaseAttr)tp.clazz.sub_clazz[i].getAttr(attrPizzaCase);
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
				CaseLabel dc = new CaseLabel(pos,this,null,new ASTNode[]{thrErr});
				cases = (ASTNode[])Arrays.insert(cases,dc,0);
				dc.resolve(Type.tpVoid);
			}
			if( mode == ENUM_SWITCH ) {
				Type tp = sel.getType();
				Expr cae = new CastExpr(pos,Type.tpInt,sel);
				cae.parent = sel.parent;
				sel = (Expr)cae.resolve(Type.tpInt);
			}
		} finally {
			PassInfo.pop(this);
			result_state = NodeInfoPass.popState();
			if( !isMethodAbrupted() ) {
				for(int i=0; i < case_states.length; i++) {
					if( !cases[i].isMethodAbrupted() && case_states[i] != null )
						result_state = NodeInfoPass.joinInfo(result_state, case_states[i]);
				}
				NodeInfoPass.addInfo(result_state);
			}
		}
		setResolved(true);
		if( me != null ) return me.resolve(reqType);
		return this;
	}

	public CodeLabel getBreakLabel() throws RuntimeException {
		if( break_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'break' label");
		return break_label;
	}

	public CodeLabel getContinueLabel() throws RuntimeException {
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
			//Code.addVars(vars);
			for(int i=0; i < cases.length; i++) {
				if( isAutoReturnable() )
					cases[i].setAutoReturnable(true);
				((CaseLabel)cases[i]).generate(Type.tpVoid);
			}
			//Code.removeVars(vars);
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

public class CatchInfo extends Statement implements Scope {

	static CatchInfo[] emptyArray = new CatchInfo[0];

	public Var	arg;
	public Statement	body;

	public CodeLabel		handler;
	public CodeCatchInfo	code_catcher;

	public CatchInfo(int pos, ASTNode parent, Var arg, Statement body) {
		super(pos, parent);
		this.arg = arg;
		this.arg.parent = arg;
		this.body = body;
		this.body.parent = this;
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public String toString() {
		return "catch( "+arg+" )";
	}

	public void cleanup() {
		parent=null;
		arg = null;
		body.cleanup();
		body = null;
	}

	rule public resolveNameR(ASTNode@ node, ResInfo path, KString name, Type tp, int resfl)
	{
		node ?= arg, ((Var)node).name.equals(name)
	}

	rule public resolveMethodR(ASTNode@ node, ResInfo path, KString name, Expr[] args, Type ret, Type type, int resfl)
	{
		false
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
//		arg = (Var)arg.resolve();
		PassInfo.push(this);
		try {
			body = (Statement)body.resolve(Type.tpVoid);
			if( body.isMethodAbrupted() ) setMethodAbrupted(true);
		} catch(Exception e ) {
			Kiev.reportError(body.pos,e);
		} finally { PassInfo.pop(this); }
		return this;
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

public class FinallyInfo extends CatchInfo {

	public Var	ret_arg;
	public CodeLabel	subr_label;

	public FinallyInfo(int pos, ASTNode parent, Statement body) {
		super(pos,parent,new Var(pos,KString.Empty,Type.tpThrowable,0),body);
        ret_arg = new Var(pos,KString.Empty,Type.tpObject,0);
        ret_arg.parent = this;
	}

	public String toString() { return "finally"; }

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

public class TryStat extends Statement/*defaults*/ {

	public Statement	body;
	public ASTNode[]	catchers = ASTNode.emptyArray;
	public ASTNode		finally_catcher;

	public CodeLabel	end_label;

	public TryStat(int pos, ASTNode parent, Statement body, ASTNode[] catchers, ASTNode finally_catcher) {
		super(pos, parent);
		this.body = body;
		this.body.parent = this;
		this.catchers = catchers;
		for(int i=0; i < catchers.length; i++) catchers[i].parent = this;
		if( finally_catcher != null ) {
			this.finally_catcher = finally_catcher;
			this.finally_catcher.parent = this;
		}
	}

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

	public ASTNode resolve(Type reqType) throws RuntimeException {
		ScopeNodeInfoVector finally_state = null;
		for(int i=0; i < catchers.length; i++) {
			try {
				NodeInfoPass.pushState();
				catchers[i] = (CatchInfo)((ASTCatchInfo)catchers[i]).resolve(Type.tpVoid);
				catchers[i].parent = this;
			} catch(Exception e ) {
				Kiev.reportError(catchers[i].pos,e);
			} finally {
				NodeInfoPass.popState();
			}
		}
		if(finally_catcher != null) {
			try {
				NodeInfoPass.pushState();
				finally_catcher = (FinallyInfo)((ASTFinallyInfo)finally_catcher).resolve(Type.tpVoid);
				finally_catcher.parent = this;
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
				body = (Statement)body.resolve(Type.tpVoid);
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
			PassInfo.pop(this);
			NodeInfoPass.popState();
			if( finally_state != null && !finally_catcher.isMethodAbrupted())
				NodeInfoPass.addInfo(finally_state);
		}
		return this;
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
				if( finally_catcher != null ) {
					Code.addInstr(Instr.op_jsr,((FinallyInfo)finally_catcher).subr_label);
				}
				if( isAutoReturnable() )
					ReturnStat.generateReturn();
				else
					Code.addInstr(Instr.op_goto,end_label);
			}
			for(int i=0; i < catchers.length; i++) {
				Code.addInstr(Instr.stop_catcher,((CatchInfo)catchers[i]).code_catcher);
			}

			for(int i=0; i < catchers.length; i++) {
				if( isAutoReturnable() )
					catchers[i].setAutoReturnable(true);
				try {
					((CatchInfo)catchers[i]).generate(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(catchers[i].pos,e);
				}
			}
			if(finally_catcher != null) {
				try {
					Code.addInstr(Instr.stop_catcher,((FinallyInfo)finally_catcher).code_catcher);
					((FinallyInfo)finally_catcher).generate(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(finally_catcher.pos,e);
				}
			}
			Code.addInstr(Instr.set_label,end_label);
		} finally {
			PassInfo.pop(this);
			if(finally_catcher != null)
				Code.removeVar(((FinallyInfo)finally_catcher).ret_arg);
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

public class SynchronizedStat extends Statement {

	public Statement	body;
	public Expr			expr;
	public Var			expr_var;
	public CodeLabel		handler;
	public CodeCatchInfo	code_catcher;
	public CodeLabel	end_label;

	public SynchronizedStat(int pos, ASTNode parent, Expr expr, Statement body) {
		super(pos, parent);
		this.expr = expr;
		this.expr.parent = this;
		this.body = body;
		this.body.parent = this;
	}

	public void cleanup() {
		parent=null;
		body.cleanup();
		body = null;
		expr.cleanup();
		expr = null;
		expr_var = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		try {
			try {
				expr = (Expr)expr.resolve(null);
				expr_var = new Var(pos,this,KString.Empty,Type.tpObject,0);
			} catch(Exception e ) { Kiev.reportError(pos,e); }
			try {
				body = (Statement)body.resolve(Type.tpVoid);
			} catch(Exception e ) { Kiev.reportError(pos,e); }
			setAbrupted(body.isAbrupted());
			setMethodAbrupted(body.isMethodAbrupted());
		} finally { PassInfo.pop(this); }
		return this;
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

public class WithStat extends Statement {

	public Statement	body;
	public Expr			expr;
	public ASTNode		var_or_field;
	public CodeLabel	end_label;

	public WithStat(int pos, ASTNode parent, Expr expr, Statement body) {
		super(pos, parent);
		this.expr = expr;
		this.expr.parent = this;
		this.body = body;
		this.body.parent = this;
	}

	public void cleanup() {
		parent=null;
		body.cleanup();
		body = null;
		expr.cleanup();
		expr = null;
		var_or_field = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		try {
			try {
				expr = (Expr)expr.resolve(null);
				Expr e = expr;
				switch (e) {
				case VarAccessExpr:				var_or_field = ((VarAccessExpr)e).var;				break;
				case LocalPrologVarAccessExpr:	var_or_field = ((LocalPrologVarAccessExpr)e).var;	break;
				case AccessExpr:				var_or_field = ((AccessExpr)e).var;					break;
				case FieldAccessExpr:			var_or_field = ((FieldAccessExpr)e).var;			break;
				case StaticFieldAccessExpr:		var_or_field = ((StaticFieldAccessExpr)e).var;		break;
				case AssignExpr:                e = ((AssignExpr)e).lval;                           goto case e;
				}
				if (var_or_field == null) {
					Kiev.reportError(pos,"With statement needs variable or field argument");
					return body.resolve(Type.tpVoid);
				}
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
				return body.resolve(Type.tpVoid);
			}

			boolean is_forward = var_or_field.isForward();
			if (!is_forward) var_or_field.setForward(true);
			try {
				body = (Statement)body.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
			} finally {
				if (!is_forward) var_or_field.setForward(false);
			}

			setAbrupted(body.isAbrupted());
			setMethodAbrupted(body.isMethodAbrupted());
		} finally { PassInfo.pop(this); }
		return this;
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

