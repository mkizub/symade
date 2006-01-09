package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JCaseLabelView;
import kiev.be.java.JSwitchStatView;
import kiev.be.java.JCatchInfoView;
import kiev.be.java.JFinallyInfoView;
import kiev.be.java.JTryStatView;
import kiev.be.java.JSynchronizedStatView;
import kiev.be.java.JWithStatView;

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

@node
public class CaseLabel extends ENode implements ScopeOfNames {
	
	@dflow(in="this:in()", out="stats") private static class DFI {
	@dflow(in="this:in", seq="true") Var[]		pattern;
	@dflow(in="pattern", seq="true") ENode[]	stats;
	}
	
	public static final CaseLabel[] emptyArray = new CaseLabel[0];

	@node
	public static final class CaseLabelImpl extends ENodeImpl {
		@att public ENode			val;
		@ref public Type			type;
		@att public NArr<Var>		pattern;
		@att public NArr<ENode>		stats;
		@ref public CodeLabel		case_label;
		public CaseLabelImpl() {}
		public CaseLabelImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view CaseLabelView of CaseLabelImpl extends ENodeView {
		public				ENode			val;
		public				Type			type;
		public access:ro	NArr<Var>		pattern;
		public access:ro	NArr<ENode>		stats;
	}

	@att public abstract virtual			ENode			val;
	@ref public abstract virtual			Type			type;
	@att public abstract virtual access:ro	NArr<Var>		pattern;
	@att public abstract virtual access:ro	NArr<ENode>		stats;
	
	public NodeView				getNodeView()			alias operator(210,fy,$cast) { return new CaseLabelView((CaseLabelImpl)this.$v_impl); }
	public ENodeView			getENodeView()			alias operator(210,fy,$cast) { return new CaseLabelView((CaseLabelImpl)this.$v_impl); }
	public CaseLabelView		getCaseLabelView()		alias operator(210,fy,$cast) { return new CaseLabelView((CaseLabelImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			alias operator(210,fy,$cast) { return new JCaseLabelView((CaseLabelImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			alias operator(210,fy,$cast) { return new JCaseLabelView((CaseLabelImpl)this.$v_impl); }
	public JCaseLabelView		getJCaseLabelView()		alias operator(210,fy,$cast) { return new JCaseLabelView((CaseLabelImpl)this.$v_impl); }

	@getter public ENode			get$val()				{ return this.getCaseLabelView().val; }
	@getter public Type				get$type()				{ return this.getCaseLabelView().type; }
	@getter public NArr<Var>		get$pattern()			{ return this.getCaseLabelView().pattern; }
	@getter public NArr<ENode>		get$stats()				{ return this.getCaseLabelView().stats; }
	@setter public void		set$val(ENode val)				{ this.getCaseLabelView().val = val; }
	@setter public void		set$type(Type val)				{ this.getCaseLabelView().type = val; }
	

	public CaseLabel() {
		super(new CaseLabelImpl());
	}

	public CaseLabel(int pos, ENode val, ENode[] stats) {
		super(new CaseLabelImpl(pos));
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
											Type.getRealType(sw.tmpvar.getType(),cas.concr_type),
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

@node
public class SwitchStat extends ENode implements BreakTarget {
	
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

	@node
	public static class SwitchStatImpl extends ENodeImpl {
		@att                 public int mode; /* = NORMAL_SWITCH; */
		@att                 public ENode					sel;
		@att                 public NArr<CaseLabel>		cases;
		@att                 public LVarExpr				tmpvar;
		@ref                 public CaseLabel				defCase;
		@ref                 public Field					typehash; // needed for re-resolving
		@att(copyable=false) public Label					lblcnt;
		@att(copyable=false) public Label					lblbrk;
		@att                 public CodeSwitch				cosw;
		public SwitchStatImpl() {}
		public SwitchStatImpl(int pos) { super(pos); }
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

	@att public abstract virtual			int						mode;
	@att public abstract virtual			ENode					sel;
	@att public abstract virtual access:ro	NArr<CaseLabel>			cases;
	@att public abstract virtual			LVarExpr				tmpvar;
	@ref public abstract virtual			CaseLabel				defCase;
	@ref public abstract virtual			Field					typehash; // needed for re-resolving
	@att public abstract virtual access:ro	Label					lblcnt;
	@att public abstract virtual access:ro	Label					lblbrk;

	@getter public int				get$mode()				{ return this.getSwitchStatView().mode; }
	@getter public ENode			get$sel()				{ return this.getSwitchStatView().sel; }
	@getter public NArr<CaseLabel>	get$cases()				{ return this.getSwitchStatView().cases; }
	@getter public LVarExpr			get$tmpvar()			{ return this.getSwitchStatView().tmpvar; }
	@getter public CaseLabel		get$defCase()			{ return this.getSwitchStatView().defCase; }
	@getter public Field			get$typehash()			{ return this.getSwitchStatView().typehash; }
	@getter public Label			get$lblcnt()			{ return this.getSwitchStatView().lblcnt; }
	@getter public Label			get$lblbrk()			{ return this.getSwitchStatView().lblbrk; }
	
	@setter public void			set$mode(int val)			{ this.getSwitchStatView().mode = val; }
	@setter public void			set$sel(ENode val)			{ this.getSwitchStatView().sel = val; }
	@setter public void			set$tmpvar(LVarExpr val)	{ this.getSwitchStatView().tmpvar = val; }
	@setter public void			set$defCase(CaseLabel val)	{ this.getSwitchStatView().defCase = val; }
	@setter public void			set$typehash(Field val)		{ this.getSwitchStatView().typehash = val; }

	public NodeView					getNodeView()			alias operator(210,fy,$cast) { return new SwitchStatView((SwitchStatImpl)this.$v_impl); }
	public ENodeView				getENodeView()			alias operator(210,fy,$cast) { return new SwitchStatView((SwitchStatImpl)this.$v_impl); }
	public SwitchStatView			getSwitchStatView()		alias operator(210,fy,$cast) { return new SwitchStatView((SwitchStatImpl)this.$v_impl); }
	public JNodeView				getJNodeView()			alias operator(210,fy,$cast) { return new JSwitchStatView((SwitchStatImpl)this.$v_impl); }
	public JENodeView				getJENodeView()			alias operator(210,fy,$cast) { return new JSwitchStatView((SwitchStatImpl)this.$v_impl); }
	public JSwitchStatView			getJSwitchStatView()	alias operator(210,fy,$cast) { return new JSwitchStatView((SwitchStatImpl)this.$v_impl); }
	
	public SwitchStat() {
		super(new SwitchStatImpl());
		((SwitchStatImpl)this.$v_impl).lblcnt = new Label();
		((SwitchStatImpl)this.$v_impl).lblbrk = new Label();
	}

	public SwitchStat(int pos, ENode sel, CaseLabel[] cases) {
		super(new SwitchStatImpl(pos));
		((SwitchStatImpl)this.$v_impl).lblcnt = new Label();
		((SwitchStatImpl)this.$v_impl).lblbrk = new Label();
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
				new ExprStat(typehash.init.getPos(),
					new AssignExpr(typehash.init.getPos(),AssignOperator.Assign
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

@node
public class CatchInfo extends ENode implements ScopeOfNames {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	Var				arg;
	@dflow(in="arg")		ENode			body;
	}
	
	static CatchInfo[] emptyArray = new CatchInfo[0];

	@node
	public static class CatchInfoImpl extends ENodeImpl {
		@att public Var				arg;
		@att public ENode			body;
		@att public CodeLabel		handler;
		@att public CodeCatchInfo	code_catcher;
		public CatchInfoImpl() {}
		public CatchInfoImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view CatchInfoView of CatchInfoImpl extends ENodeView {
		public Var				arg;
		public ENode			body;
	}
	
	@att public abstract virtual Var			arg;
	@att public abstract virtual ENode			body;
	
	@getter public Var			get$arg()				{ return this.getCatchInfoView().arg; }
	@getter public ENode		get$body()				{ return this.getCatchInfoView().body; }
	@setter public void			set$arg(Var val)		{ this.getCatchInfoView().arg = val; }
	@setter public void			set$body(ENode val)		{ this.getCatchInfoView().body = val; }

	public NodeView				getNodeView()			alias operator(210,fy,$cast) { return new CatchInfoView((CatchInfoImpl)this.$v_impl); }
	public ENodeView			getENodeView()			alias operator(210,fy,$cast) { return new CatchInfoView((CatchInfoImpl)this.$v_impl); }
	public CatchInfoView		getCatchInfoView()		alias operator(210,fy,$cast) { return new CatchInfoView((CatchInfoImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			alias operator(210,fy,$cast) { return new JCatchInfoView((CatchInfoImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			alias operator(210,fy,$cast) { return new JCatchInfoView((CatchInfoImpl)this.$v_impl); }
	public JCatchInfoView		getJCatchInfoView()		alias operator(210,fy,$cast) { return new JCatchInfoView((CatchInfoImpl)this.$v_impl); }
	
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

@node
public class FinallyInfo extends CatchInfo {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	Var				arg;
	@dflow(in="arg")		ENode			body;
	}
	
	@node
	public static class FinallyInfoImpl extends CatchInfoImpl {
		@att public Var			ret_arg;
		@att public CodeLabel	subr_label;
		public FinallyInfoImpl() {}
		public FinallyInfoImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view FinallyInfoView of FinallyInfoImpl extends CatchInfoView {
		public Var			ret_arg;
	}
	
	@att public abstract virtual Var			ret_arg;
	
	@getter public Var			get$ret_arg()				{ return this.getFinallyInfoView().ret_arg; }
	@setter public void			set$ret_arg(Var val)		{ this.getFinallyInfoView().ret_arg = val; }

	public NodeView				getNodeView()			alias operator(210,fy,$cast) { return new FinallyInfoView((FinallyInfoImpl)this.$v_impl); }
	public ENodeView			getENodeView()			alias operator(210,fy,$cast) { return new FinallyInfoView((FinallyInfoImpl)this.$v_impl); }
	public CatchInfoView		getCatchInfoView()		alias operator(210,fy,$cast) { return new FinallyInfoView((FinallyInfoImpl)this.$v_impl); }
	public FinallyInfoView		getFinallyInfoView()	alias operator(210,fy,$cast) { return new FinallyInfoView((FinallyInfoImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			alias operator(210,fy,$cast) { return new JFinallyInfoView((FinallyInfoImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			alias operator(210,fy,$cast) { return new JFinallyInfoView((FinallyInfoImpl)this.$v_impl); }
	public JCatchInfoView		getJCatchInfoView()		alias operator(210,fy,$cast) { return new JFinallyInfoView((FinallyInfoImpl)this.$v_impl); }
	public JFinallyInfoView		getJFinallyInfoView()	alias operator(210,fy,$cast) { return new JFinallyInfoView((FinallyInfoImpl)this.$v_impl); }
	
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

@node
public class TryStat extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")				ENode			body;
	@dflow(in="this:in", seq="false")	CatchInfo[]		catchers;
	@dflow(in="this:in")				FinallyInfo		finally_catcher;
	}
	
	@node
	public static final class TryStatImpl extends ENodeImpl {
		@att public ENode				body;
		@att public NArr<CatchInfo>		catchers;
		@att public FinallyInfo			finally_catcher;
		@att public CodeLabel			end_label;
		public TryStatImpl() {}
		public TryStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view TryStatView of TryStatImpl extends ENodeView {
		public				ENode				body;
		public access:ro	NArr<CatchInfo>		catchers;
		public				FinallyInfo			finally_catcher;
	}

	@att public abstract virtual			ENode				body;
	@att public abstract virtual access:ro	NArr<CatchInfo>		catchers;
	@att public abstract virtual			FinallyInfo			finally_catcher;
	
	@getter public ENode			get$body()				{ return this.getTryStatView().body; }
	@getter public NArr<CatchInfo>	get$catchers()			{ return this.getTryStatView().catchers; }
	@getter public FinallyInfo		get$finally_catcher()	{ return this.getTryStatView().finally_catcher; }
	
	@setter public void		set$body(ENode val)						{ this.getTryStatView().body = val; }
	@setter public void		set$finally_catcher(FinallyInfo val)	{ this.getTryStatView().finally_catcher = val; }
	
	public NodeView				getNodeView()			alias operator(210,fy,$cast) { return new TryStatView((TryStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			alias operator(210,fy,$cast) { return new TryStatView((TryStatImpl)this.$v_impl); }
	public TryStatView			getTryStatView()		alias operator(210,fy,$cast) { return new TryStatView((TryStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			alias operator(210,fy,$cast) { return new JTryStatView((TryStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			alias operator(210,fy,$cast) { return new JTryStatView((TryStatImpl)this.$v_impl); }
	public JTryStatView			getJTryStatView()		alias operator(210,fy,$cast) { return new JTryStatView((TryStatImpl)this.$v_impl); }


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

@node
@dflow(out="body")
public class SynchronizedStat extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="expr")		ENode		body;
	}
	
	@node
	public static final class SynchronizedStatImpl extends ENodeImpl {
		@att public ENode			expr;
		@att public Var				expr_var;
		@att public ENode			body;
		@att public CodeLabel		handler;
		@att public CodeCatchInfo	code_catcher;
		@att public CodeLabel		end_label;
		public SynchronizedStatImpl() {}
		public SynchronizedStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view SynchronizedStatView of SynchronizedStatImpl extends ENodeView {
		public ENode			expr;
		public Var				expr_var;
		public ENode			body;
	}

	@att public abstract virtual ENode			expr;
	@att public abstract virtual Var			expr_var;
	@att public abstract virtual ENode			body;
	
	@getter public ENode			get$expr()		{ return this.getSynchronizedStatView().expr; }
	@getter public Var				get$expr_var()	{ return this.getSynchronizedStatView().expr_var; }
	@getter public ENode			get$body()		{ return this.getSynchronizedStatView().body; }
	
	@setter public void		set$expr(ENode val)		{ this.getSynchronizedStatView().expr = val; }
	@setter public void		set$expr_var(Var val)	{ this.getSynchronizedStatView().expr_var = val; }
	@setter public void		set$body(ENode val)		{ this.getSynchronizedStatView().body = val; }
	
	public NodeView					getNodeView()				{ return new SynchronizedStatView((SynchronizedStatImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new SynchronizedStatView((SynchronizedStatImpl)this.$v_impl); }
	public SynchronizedStatView		getSynchronizedStatView()	{ return new SynchronizedStatView((SynchronizedStatImpl)this.$v_impl); }
	public JNodeView				getJNodeView()				{ return new JSynchronizedStatView((SynchronizedStatImpl)this.$v_impl); }
	public JENodeView				getJENodeView()				{ return new JSynchronizedStatView((SynchronizedStatImpl)this.$v_impl); }
	public JSynchronizedStatView	getJSynchronizedStatView()	{ return new JSynchronizedStatView((SynchronizedStatImpl)this.$v_impl); }


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

@node
public class WithStat extends ENode {

	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="expr")		ENode		body;
	}
	
	@node
	public static final class WithStatImpl extends ENodeImpl {
		@att public ENode		expr;
		@att public ENode		body;
		@ref public LvalDNode	var_or_field;
		@att public CodeLabel	end_label;
		public WithStatImpl() {}
		public WithStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view WithStatView of WithStatImpl extends ENodeView {
		public ENode		expr;
		public ENode		body;
		public LvalDNode	var_or_field;
	}

	@att public abstract virtual ENode			expr;
	@att public abstract virtual ENode			body;
	@att public abstract virtual LvalDNode		var_or_field;
	
	@getter public ENode			get$expr()			{ return this.getWithStatView().expr; }
	@getter public ENode			get$body()			{ return this.getWithStatView().body; }
	@getter public LvalDNode		get$var_or_field()	{ return this.getWithStatView().var_or_field; }
	
	@setter public void		set$expr(ENode val)				{ this.getWithStatView().expr = val; }
	@setter public void		set$body(ENode val)				{ this.getWithStatView().body = val; }
	@setter public void		set$var_or_field(LvalDNode val)	{ this.getWithStatView().var_or_field = val; }
	
	public NodeView			getNodeView()		{ return new WithStatView((WithStatImpl)this.$v_impl); }
	public ENodeView		getENodeView()		{ return new WithStatView((WithStatImpl)this.$v_impl); }
	public WithStatView		getWithStatView()	{ return new WithStatView((WithStatImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		{ return new JWithStatView((WithStatImpl)this.$v_impl); }
	public JENodeView		getJENodeView()		{ return new JWithStatView((WithStatImpl)this.$v_impl); }
	public JWithStatView	getJWithStatView()	{ return new JWithStatView((WithStatImpl)this.$v_impl); }

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

