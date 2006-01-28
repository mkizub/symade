package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.be.java.JCaseLabel;
import kiev.be.java.JSwitchStat;
import kiev.be.java.JCatchInfo;
import kiev.be.java.JFinallyInfo;
import kiev.be.java.JTryStat;
import kiev.be.java.JSynchronizedStat;
import kiev.be.java.JWithStat;

import kiev.be.java.CodeLabel;
import kiev.be.java.CodeSwitch;
import kiev.be.java.CodeCatchInfo;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public class CaseLabel extends ENode implements ScopeOfNames {
	
	@dflow(in="this:in()", out="stats") private static class DFI {
	@dflow(in="this:in", seq="true") Var[]		pattern;
	@dflow(in="pattern", seq="true") ENode[]	stats;
	}
	
	public static final CaseLabel[] emptyArray = new CaseLabel[0];

	@virtual typedef NImpl = CaseLabelImpl;
	@virtual typedef VView = CaseLabelView;
	@virtual typedef JView = JCaseLabel;

	@nodeimpl
	public static final class CaseLabelImpl extends ENodeImpl {
		@virtual typedef ImplOf = CaseLabel;
		@att public ENode			val;
		@ref public Type			type;
		@att public NArr<Var>		pattern;
		@att public NArr<ENode>		stats;
		@ref public CodeLabel		case_label;
	}
	@nodeview
	public static final view CaseLabelView of CaseLabelImpl extends ENodeView {
		public				ENode			val;
		public				Type			type;
		public access:ro	NArr<Var>		pattern;
		public access:ro	NArr<ENode>		stats;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public CaseLabel() {
		super(new CaseLabelImpl());
	}

	public CaseLabel(int pos, ENode val, ENode[] stats) {
		this();
		this.pos = pos;
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
			CaseLabel cl = (CaseLabel)dfi.node_impl.getNode();
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
				sb.append(pattern[i].vtype).append(' ').append(pattern[i].name);
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

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
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
							sw.defCase = this;
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
						if (et.isEnum())
							val = new ConstIntExpr(et.getStruct().getIndexOfEnumField(f.var));
						else
							val = (ENode)f.var.init.copy();
					}
					else if( sw.mode != SwitchStat.NORMAL_SWITCH )
						throw new CompilerException(this,"Wrong case in normal switch");
				}
			} else {
				sw.defCase = this;
				if( sw.mode == SwitchStat.TYPE_SWITCH )
					this.type = Type.tpObject;
			}
		} catch(Exception e ) { Kiev.reportError(this,e); }

		BlockStat.resolveBlockStats(this, stats);

		if( val != null ) {
			if( !val.isConstantExpr() )
				throw new RuntimeException("Case label "+val+" must be a constant expression but "+val.getClass()+" found");
			if( !val.getType().isIntegerInCode() )
				throw new RuntimeException("Case label "+val+" must be of integer type");
		}
	}

	public Dumper toJava(Dumper dmp) {
		if( val == null )
			dmp.newLine(-1).append("default:");
		else
			dmp.newLine(-1).append("case ").append(val).append(':');
		dmp.newLine(1);
		foreach (ENode s; stats)
			s.toJava(dmp);
		return dmp;
	}
}

@nodeset
public class SwitchStat extends ENode {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")			ENode			sel;
	@dflow(in="sel", seq="false")	CaseLabel[]		cases;
	@dflow(in="cases")				Label			lblcnt;
	@dflow(in="cases")				Label			lblbrk;
	}
	
	public static final int NORMAL_SWITCH = 0;
	public static final int PIZZA_SWITCH = 1;
	public static final int TYPE_SWITCH = 2;
	public static final int ENUM_SWITCH = 3;

	@virtual typedef NImpl = SwitchStatImpl;
	@virtual typedef VView = SwitchStatView;
	@virtual typedef JView = JSwitchStat;

	@nodeimpl
	public static class SwitchStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = SwitchStat;
		@att                 public int mode; /* = NORMAL_SWITCH; */
		@att                 public ENode					sel;
		@att                 public NArr<CaseLabel>		cases;
		@att                 public LVarExpr				tmpvar;
		@ref                 public CaseLabel				defCase;
		@ref                 public Field					typehash; // needed for re-resolving
		@att(copyable=false) public Label					lblcnt;
		@att(copyable=false) public Label					lblbrk;
		@att                 public CodeSwitch				cosw;
	}
	@nodeview
	public static view SwitchStatView of SwitchStatImpl extends ENodeView {
		public				int						mode;
		public				ENode					sel;
		public access:ro	NArr<CaseLabel>			cases;
		public				LVarExpr				tmpvar;
		public				CaseLabel				defCase;
		public				Field					typehash; // needed for re-resolving
		public access:ro	Label					lblcnt;
		public access:ro	Label					lblbrk;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public SwitchStat() {
		super(new SwitchStatImpl());
		setBreakTarget(true);
		((SwitchStatImpl)this.$v_impl).lblcnt = new Label();
		((SwitchStatImpl)this.$v_impl).lblbrk = new Label();
	}

	public SwitchStat(int pos, ENode sel, CaseLabel[] cases) {
		this();
		this.pos = pos;
		this.sel = sel;
		this.cases.addAll(cases);
		defCase = null;
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
					tmpvar = new LVarExpr(sel.pos, new Var(sel.pos,KString.from(
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
						cae.obj = new LVarExpr(tmpvar.pos,tmpvar.getVar());
						cae.obj.resolve(null);
						cae.func = new NameRef(pos, nameGetCaseTag);
					} else {
						mode = TYPE_SWITCH;
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
		if( mode == TYPE_SWITCH ) {
			ConstExpr[] signs = new ConstExpr[typenames.length];
			for(int j=0; j < signs.length; j++)
				signs[j] = new ConstStringExpr(typenames[j]);
			if( defindex < 0 ) defindex = signs.length;
			typehash.init = new NewExpr(ctx_clazz.pos,Type.tpTypeSwitchHash,
				new ENode[]{ new NewInitializedArrayExpr(ctx_clazz.pos,new TypeRef(Type.tpString),1,signs),
					new ConstIntExpr(defindex)
				});
			Constructor clinit = ctx_clazz.getClazzInitMethod();
			clinit.body.addStatement(
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
						Field[] eflds = tp.getStruct().getEnumFields();
						if (eflds.length == cases.length)
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
		if( mode == ENUM_SWITCH ) {
			Type tp = sel.getType();
			sel = new CastExpr(pos,Type.tpInt,(ENode)~sel);
			sel.resolve(Type.tpInt);
		}
		setResolved(true);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("switch").space().append('(')
			.append(sel).space().append(')').space().append('{').newLine(1);
		for(int i=0; i < cases.length; i++) dmp.append(cases[i]);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

@nodeset
public class CatchInfo extends ENode implements ScopeOfNames {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	Var				arg;
	@dflow(in="arg")		ENode			body;
	}
	
	static CatchInfo[] emptyArray = new CatchInfo[0];

	@virtual typedef NImpl = CatchInfoImpl;
	@virtual typedef VView = CatchInfoView;
	@virtual typedef JView = JCatchInfo;

	@nodeimpl
	public static class CatchInfoImpl extends ENodeImpl {
		@virtual typedef ImplOf = CatchInfo;
		@att public Var				arg;
		@att public ENode			body;
		@att public CodeLabel		handler;
		@att public CodeCatchInfo	code_catcher;
	}
	@nodeview
	public static view CatchInfoView of CatchInfoImpl extends ENodeView {
		public Var				arg;
		public ENode			body;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public CatchInfo() {
		super(new CatchInfoImpl());
	}
	public CatchInfo(CatchInfoImpl impl) {
		super(impl);
	}

	public String toString() {
		return "catch( "+arg+" )";
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
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

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("catch").space().append('(').space();
		arg.toJavaDecl(dmp).space().append(')').space().append(body);
		return dmp;
	}
}

@nodeset
public class FinallyInfo extends CatchInfo {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	Var				arg;
	@dflow(in="arg")		ENode			body;
	}
	
	@virtual typedef NImpl = FinallyInfoImpl;
	@virtual typedef VView = FinallyInfoView;
	@virtual typedef JView = JFinallyInfo;

	@nodeimpl
	public static class FinallyInfoImpl extends CatchInfoImpl {
		@virtual typedef ImplOf = FinallyInfo;
		@att public Var			ret_arg;
		@att public CodeLabel	subr_label;
	}
	@nodeview
	public static view FinallyInfoView of FinallyInfoImpl extends CatchInfoView {
		public Var			ret_arg;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public FinallyInfo() {
		super(new FinallyInfoImpl());
	}

	public String toString() { return "finally"; }

	public void resolve(Type reqType) {
		if (arg == null)
			arg = new Var(pos,KString.Empty,Type.tpThrowable,0);
		if (ret_arg == null)
			ret_arg = new Var(pos,KString.Empty,Type.tpObject,0);
		super.resolve(reqType);
	}
	
	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("finally").space().append(body).newLine();
		return dmp;
	}

}

@nodeset
public class TryStat extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")				ENode			body;
	@dflow(in="this:in", seq="false")	CatchInfo[]		catchers;
	@dflow(in="this:in")				FinallyInfo		finally_catcher;
	}
	
	@virtual typedef NImpl = TryStatImpl;
	@virtual typedef VView = TryStatView;
	@virtual typedef JView = JTryStat;

	@nodeimpl
	public static final class TryStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = TryStat;
		@att public ENode				body;
		@att public NArr<CatchInfo>		catchers;
		@att public FinallyInfo			finally_catcher;
		@att public CodeLabel			end_label;
	}
	@nodeview
	public static final view TryStatView of TryStatImpl extends ENodeView {
		public				ENode				body;
		public access:ro	NArr<CatchInfo>		catchers;
		public				FinallyInfo			finally_catcher;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public TryStat() {
		super(new TryStatImpl());
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

	public Dumper toJava(Dumper dmp) {
		dmp.append("try").space().append(body).newLine();
		for(int i=0; i < catchers.length; i++)
			dmp.append(catchers[i]).newLine();
		if(finally_catcher != null)
			dmp.append(finally_catcher).newLine();
		return dmp;
	}

}

@nodeset
public class SynchronizedStat extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="expr")		ENode		body;
	}
	
	@virtual typedef NImpl = SynchronizedStatImpl;
	@virtual typedef VView = SynchronizedStatView;
	@virtual typedef JView = JSynchronizedStat;

	@nodeimpl
	public static final class SynchronizedStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = SynchronizedStat;
		@att public ENode			expr;
		@att public Var				expr_var;
		@att public ENode			body;
		@att public CodeLabel		handler;
		@att public CodeCatchInfo	code_catcher;
		@att public CodeLabel		end_label;
	}
	@nodeview
	public static final view SynchronizedStatView of SynchronizedStatImpl extends ENodeView {
		public ENode			expr;
		public Var				expr_var;
		public ENode			body;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public SynchronizedStat() {
		super(new SynchronizedStatImpl());
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

	public Dumper toJava(Dumper dmp) {
		dmp.append("synchronized").space().append('(').space().append(expr)
			.space().append(')').forsed_space().append(body).newLine();
		return dmp;
	}

}

@nodeset
public class WithStat extends ENode {

	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="expr")		ENode		body;
	}
	
	@virtual typedef NImpl = WithStatImpl;
	@virtual typedef VView = WithStatView;
	@virtual typedef JView = JWithStat;

	@nodeimpl
	public static final class WithStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = WithStat;
		@att public ENode		expr;
		@att public ENode		body;
		@ref public LvalDNode	var_or_field;
		@att public CodeLabel	end_label;
	}
	@nodeview
	public static final view WithStatView of WithStatImpl extends ENodeView {
		public ENode		expr;
		public ENode		body;
		public LvalDNode	var_or_field;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public WithStat() {
		super(new WithStatImpl());
	}

	public void resolve(Type reqType) {
		try {
			expr.resolve(null);
			ENode e = expr;
			switch (e) {
			case LVarExpr:		var_or_field = ((LVarExpr)e).getVar();	break;
			case IFldExpr:		var_or_field = ((IFldExpr)e).var;		break;
			case SFldExpr:		var_or_field = ((SFldExpr)e).var;		break;
			case AssignExpr:	e = ((AssignExpr)e).lval;				goto case e;
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

	public Dumper toJava(Dumper dmp) {
		dmp.append("/*with ").space().append('(').space().append(expr)
			.space().append(")*/").forsed_space().append(body).newLine();
		return dmp;
	}
}

