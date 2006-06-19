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

@nodeview
public final view RStruct of Struct extends RTypeDecl {

	static final AttrSlot TI_ATTR = new DataAttrSlot("rstruct ti field temp expr",true,false,TypeInfoExpr.class);	

	public:ro			Access					acc;
	public:ro			WrapperMetaType			wmeta_type;
	public:ro			OuterMetaType			ometa_type;
	public:ro			TypeRef					view_of;
	public:ro			Struct					package_clazz;
	public				Struct					typeinfo_clazz;
	public				Struct					iface_impl;
	public:ro			DNode[]					sub_decls;

	public final Struct getStruct() { return (Struct)this; }

	// a pizza case	
	public final boolean isPizzaCase();
	public final void setPizzaCase(boolean on);
	// a structure with the only one instance (singleton)	
	public final boolean isSingleton();
	public final void setSingleton(boolean on);
	// a local (in method) class	
	public final boolean isLocal();
	public final void setLocal(boolean on);
	// an anonymouse (unnamed) class	
	public final boolean isAnonymouse();
	public final void setAnonymouse(boolean on);
	// has pizza cases
	public final boolean isHasCases();
	public final void setHasCases(boolean on);
	// indicates that structure members were generated
	public final boolean isMembersGenerated();
	public final void setMembersGenerated(boolean on);
	// indicates that structure members were pre-generated
	public final boolean isMembersPreGenerated();
	public final void setMembersPreGenerated(boolean on);
	// indicates that statements in code were generated
	public final boolean isStatementsGenerated();
	public final void setStatementsGenerated(boolean on);
	// indicates that the structrue was generared (from template)
	public final boolean isGenerated();
	public final void setGenerated(boolean on);
	// indicates that type of the structure was attached
	public final boolean isTypeResolved();
	public final void setTypeResolved(boolean on);
	// indicates that type arguments of the structure were resolved
	public final boolean isArgsResolved();
	public final void setArgsResolved(boolean on);
	// kiev annotation
	public final boolean isAnnotation();
	public final void setAnnotation(boolean on);
	// java enum
	public final boolean isEnum();
	// structure was loaded from bytecode
	public final boolean isLoadedFromBytecode();
	public final void setLoadedFromBytecode(boolean on);

	public Struct addSubStruct(Struct sub);
	public Method addMethod(Method m);
	public void removeMethod(Method m);
	public Field addField(Field f);
	public void removeField(Field f);
	public Struct addCase(Struct cas);

	public boolean instanceOf(Struct cl);
	public Field resolveField(String name);
	public Field resolveField(String name, boolean fatal);
	public Method resolveMethod(String name, Type ret, ...);
	public Constructor getClazzInitMethod();

	public ENode accessTypeInfoField(ASTNode from, Type t, boolean from_gen) {
		while (t instanceof CTimeType)
			t = t.getEnclosedType();
		Method ctx_method = from.ctx_method;
		if (t.isUnerasable()) {
			if (ctx_method != null && ctx_method.isTypeUnerasable() && t instanceof ArgType) {
				int i=0;
				foreach (TypeDef td; ctx_method.targs) {
					if (td.getAType() == t)
						return new LVarExpr(from.pos, ctx_method.getTypeInfoParam(FormPar.PARAM_TYPEINFO_N+i));
					i++;
				}
			}
			if (this.instanceOf(Type.tpTypeInfo.clazz) && ctx_method != null && ctx_method.id.uname == nameInit) {
				if (t instanceof ArgType)
					return new ASTIdentifier(from.pos,t.name.toString());
			}
			if (this.isTypeUnerasable()) {
				ENode ti_access;
				if (ctx_method != null && ctx_method.isStatic()) {
					// check we have $typeinfo as first argument
					if (ctx_method.getTypeInfoParam(FormPar.PARAM_TYPEINFO) == null)
						throw new CompilerException(from,"$typeinfo cannot be accessed from "+ctx_method);
					else
						ti_access = new LVarExpr(from.pos,ctx_method.getTypeInfoParam(FormPar.PARAM_TYPEINFO));
				}
				else {
					Field ti = resolveField(nameTypeInfo);
					ti_access = new IFldExpr(from.pos,new ThisExpr(pos),ti);
				}
				// Check that we need our $typeinfo
				if (this.xtype ≈ t)
					return ti_access;
	
				if (t instanceof ArgType) {
					// Get corresponded type argument
					ArgType at = (ArgType)t;
					String fnm = (nameTypeInfo+'$'+at.name).intern();
					Field ti_arg = typeinfo_clazz.resolveField(fnm);
					if (ti_arg == null)
						throw new RuntimeException("Field "+fnm+" not found in "+typeinfo_clazz+" from method "+from.ctx_method);
					ti_access = new IFldExpr(from.pos,ti_access,ti_arg);
					return ti_access;
				}
			}
		}

		// Special case for interfaces, that cannot have private fields,
		// but need typeinfo in <clinit>
		if ((from.ctx_method == null || from.ctx_method.id.uname == nameClassInit) && from.ctx_tdecl.isInterface()) {
			return new TypeInfoExpr(from.pos, new TypeRef(t));
		}
		
		// Lookup and create if need as $typeinfo$N
		foreach(Field f; members; f.isStatic()) {
			if (f.init == null || !f.id.uname.startsWith(nameTypeInfo) || f.id.uname.equals(nameTypeInfo))
				continue;
			if (((TypeInfoExpr)f.init).type.getType() ≈ t)
				return new SFldExpr(from.pos,f);
		}
		TypeInfoExpr ti_expr = new TypeInfoExpr(pos, new TypeRef(t));
		// check we can use a static field
		from.addNodeData(ti_expr, TI_ATTR);
		ti_expr.resolve(null);
		~ti_expr;
		foreach (ENode ti_arg; ti_expr.cl_args; !(ti_arg instanceof SFldExpr)) {
			// oops, cannot make it a static field
			return ti_expr;
		}
		if (from_gen)
			throw new RuntimeException("Ungenerated typeinfo for type "+t+" ("+t.getClass()+")");
		int i = 0;
		foreach(Field f; members; f.isStatic()) {
			if (f.init == null || !f.id.uname.startsWith(nameTypeInfo) || f.id.uname.equals(nameTypeInfo))
				continue;
			i++;
		}
		Field f = new Field(nameTypeInfo+"$"+i,ti_expr.getType(),ACC_SYNTHETIC|ACC_STATIC|ACC_FINAL); // package-private for inner classes
		f.init = ti_expr;
		getStruct().addField(f);
		f.resolveDecl();
		// Add initialization in <clinit>
		Constructor class_init = getStruct().getClazzInitMethod();
		if( ctx_method != null && ctx_method.id.equals(nameClassInit) ) {
			class_init.addstats.append(
				new ExprStat(f.init.pos,
					new AssignExpr(f.init.pos,Operator.Assign
						,new SFldExpr(f.pos,f),f.init.ncopy())
				)
			);
			Kiev.runProcessorsOn(class_init.addstats[class_init.addstats.length-1]);
		} else {
			class_init.addstats.append(
				new ExprStat(f.init.pos,
					new AssignExpr(f.init.pos,Operator.Assign
						,new SFldExpr(f.pos,f),f.init.ncopy())
				)
			);
			Kiev.runProcessorsOn(class_init.addstats[class_init.addstats.length-1]);
		}
		f.setAddedToInit(true);
		ENode e = new SFldExpr(from.pos,f);
		return e;
//		System.out.println("Field "+f+" of type "+f.init+" added");
	}

	public List<ArgType> getTypeInfoArgs() {
		ListBuffer<ArgType> lb = new ListBuffer<ArgType>();
		TVar[] templ = this.xmeta_type.getTemplBindings().tvars;
		foreach (TVar tv; templ; tv.isFree() && tv.var.isUnerasable())
			lb.append(tv.var);
		return lb.toList();
	}

	public void autoGenerateTypeinfoClazz() {
		if (typeinfo_clazz != null)
			return;
		if (isInterface() || !isTypeUnerasable())
			return;
		// create typeinfo class
		int flags = this.flags & JAVA_ACC_MASK;
		flags &= ~(ACC_PRIVATE | ACC_PROTECTED);
		flags |= ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC;
		typeinfo_clazz = Env.newStruct(nameClTypeInfo,true,this.getStruct(),flags,true);
		((Struct)this).members.add(typeinfo_clazz);
		typeinfo_clazz.setPublic();
		typeinfo_clazz.setResolved(true);
		if (super_types.length > 0 && super_types[0].getStruct().typeinfo_clazz != null)
			typeinfo_clazz.super_types.insert(0, new TypeRef(super_types[0].getStruct().typeinfo_clazz.xtype));
		else
			typeinfo_clazz.super_types.insert(0, new TypeRef(Type.tpTypeInfo));
		getStruct().addSubStruct(typeinfo_clazz);
		typeinfo_clazz.pos = pos;

		// create constructor method
		{
			Constructor init = new Constructor(ACC_PROTECTED);
			init.body = new Block(pos);
			init.params.add(new FormPar(pos,"hash",Type.tpInt,FormPar.PARAM_NORMAL,ACC_FINAL));
			init.params.add(new FormPar(pos,"clazz",Type.tpClass,FormPar.PARAM_NORMAL,ACC_FINAL));
			// add in it arguments fields, and prepare for constructor
			foreach (ArgType at; this.getTypeInfoArgs()) {
				String fname = nameTypeInfo+"$"+at.name;
				Field f = new Field(fname,Type.tpTypeInfo,ACC_PUBLIC|ACC_FINAL);
				typeinfo_clazz.addField(f);
				FormPar v = new FormPar(pos,at.name.toString(),Type.tpTypeInfo,FormPar.PARAM_NORMAL,ACC_FINAL);
				init.params.append(v);
				init.block.stats.append(new ExprStat(pos,
					new AssignExpr(pos,Operator.Assign,
						new IFldExpr(pos,new ThisExpr(pos),f),
						new LVarExpr(pos,v)
					)
				));
			}
	
			// create typeinfo field
			Field tif = getStruct().addField(new Field(nameTypeInfo,typeinfo_clazz.xtype,ACC_PUBLIC|ACC_FINAL|ACC_SYNTHETIC));
			// add constructor to the class
			typeinfo_clazz.addMethod(init);
			
			// and add super-constructor call
			init.setNeedFieldInits(true);
			CallExpr call_super = new CallExpr(pos, null, new SymbolRef(nameSuper), null, ENode.emptyArray);
			call_super.args.add(new LVarExpr(pos,init.params[0]));
			call_super.args.add(new LVarExpr(pos,init.params[1]));
			init.block.stats.insert(0,new ExprStat(call_super));
			foreach (ArgType at; ((RStruct)super_types[0].getStruct()).getTypeInfoArgs()) {
				Type t = at.applay(this.xtype);
				ENode expr;
				if (t instanceof ArgType)
					expr = new ASTIdentifier(pos,t.name.toString());
				else if (t.isUnerasable())
					expr = new TypeInfoExpr(pos,new TypeRef(t));
				else
					expr = accessTypeInfoField(call_super, t, false);
				call_super.args.append(expr);
			}

			// create method to get typeinfo field
			Method tim = getStruct().addMethod(new Method(nameGetTypeInfo,Type.tpTypeInfo,ACC_PUBLIC | ACC_SYNTHETIC));
			tim.body = new Block(pos,new ENode[]{
				new ReturnStat(pos,new IFldExpr(pos,new ThisExpr(pos),tif))
			});
		}

		// create public constructor
		// public static TypeInfo newTypeInfo(Class clazz, TypeInfo[] args) {
		// 	int hash = hashCode(clazz, args);
		// 	TypeInfo ti = get(hash, clazz, args);
		// 	if (ti == null)
		// 		ti = new TypeInfo(hash, clazz, args[0], args[1], ...);
		// 	return ti;
		// }
		{
			Method init = new Method("newTypeInfo", typeinfo_clazz.xtype, ACC_STATIC|ACC_PUBLIC);
			init.params.add(new FormPar(pos,"clazz",Type.tpClass,FormPar.PARAM_NORMAL,ACC_FINAL));
			init.params.add(new FormPar(pos,"args",new ArrayType(Type.tpTypeInfo),FormPar.PARAM_NORMAL,ACC_FINAL));
			init.body = new Block(pos);
			Var h = new Var(pos,"hash",Type.tpInt,ACC_FINAL);
			Var v = new Var(pos,"ti",typeinfo_clazz.xtype,0);
			Method mhash = Type.tpTypeInfo.clazz.resolveMethod("hashCode",Type.tpInt,Type.tpClass,new ArrayType(Type.tpTypeInfo));
			h.init = new CallExpr(pos,null,mhash,new ENode[]{
				new LVarExpr(pos,init.params[0]),
				new LVarExpr(pos,init.params[1])
			});
			init.block.addSymbol(h);
			Method mget = Type.tpTypeInfo.clazz.resolveMethod("get",Type.tpTypeInfo,Type.tpInt,Type.tpClass,new ArrayType(Type.tpTypeInfo));
			v.init = new CallExpr(pos,null,mget,new ENode[]{
				new LVarExpr(pos,h),
				new LVarExpr(pos,init.params[0]),
				new LVarExpr(pos,init.params[1])
			});
			init.block.addSymbol(v);
			NewExpr ne = new NewExpr(pos,typeinfo_clazz.xtype,
				new ENode[]{
					new LVarExpr(pos,h),
					new LVarExpr(pos,init.params[0])
				});
			int i = 0;
			foreach (ArgType at; this.getTypeInfoArgs())
				ne.args.add(new ContainerAccessExpr(pos, new LVarExpr(pos,init.params[1]), new ConstIntExpr(i++)));
			init.block.stats.add(new IfElseStat(pos,
				new BinaryBoolExpr(pos,Operator.Equals,new LVarExpr(pos,v),new ConstNullExpr()),
				new ExprStat(pos,new AssignExpr(pos, Operator.Assign,new LVarExpr(pos,v),ne)),
				null
			));
			init.block.stats.add(new ReturnStat(pos,new LVarExpr(pos,v)));
			typeinfo_clazz.addMethod(init);
		}
		
		// create equals function:
		// public boolean eq(Clazz clazz, TypeInfo... args) {
		// 	if (this.clazz != clazz) return false;
		// 	if (typeinfo$0 != args[0]) return false;
		// 	...
		// 	return true;
		// }
		{
			Method meq = new Method("eq", Type.tpBoolean, ACC_PUBLIC);
			meq.params.add(new FormPar(pos,"clazz",Type.tpClass,FormPar.PARAM_NORMAL,ACC_FINAL));
			meq.params.add(new FormPar(pos,"args",new ArrayType(Type.tpTypeInfo),FormPar.PARAM_VARARGS,ACC_FINAL));
			typeinfo_clazz.addMethod(meq);
			meq.body = new Block(pos);
			meq.block.stats.add(new IfElseStat(pos,
				new BinaryBoolExpr(pos,Operator.NotEquals,
					new IFldExpr(pos,new ThisExpr(pos), typeinfo_clazz.resolveField("clazz")),
					new LVarExpr(pos,meq.params[0])
					),
				new ReturnStat(pos,new ConstBoolExpr(false)),
				null
			));
			int idx = 0;
			foreach (ArgType at; this.getTypeInfoArgs()) {
				Field f = typeinfo_clazz.resolveField((nameTypeInfo+"$"+at.name).intern());
				meq.block.stats.add(new IfElseStat(pos,
					new BinaryBoolExpr(pos,Operator.NotEquals,
						new IFldExpr(pos,new ThisExpr(pos), f),
						new ContainerAccessExpr(pos, new LVarExpr(pos,meq.params[1]), new ConstIntExpr(idx))
						),
					new ReturnStat(pos,new ConstBoolExpr(false)),
					null
				));
				idx++;
			}
			meq.block.stats.add(new ReturnStat(pos,new ConstBoolExpr(true)));
		}
		
		// create assignableFrom function
		// public boolean $assignableFrom(TypeInfo ti) {
		// 	if!(this.clazz.isAssignableFrom(ti.clazz)) return false;
		// 	ti = (__ti__)ti;
		// 	if!(this.$typeinfo$A.$assignableFrom(ti.$typeinfo$A)) return false;
		// 	...
		// 	return true;
		// }
		{
			Method misa = new Method("$assignableFrom", Type.tpBoolean, ACC_PUBLIC);
			misa.params.add(new FormPar(pos,"ti",Type.tpTypeInfo,FormPar.PARAM_NORMAL,ACC_FINAL));
			typeinfo_clazz.addMethod(misa);
			misa.body = new Block(pos);
			misa.block.stats.add(new IfElseStat(pos,
				new BooleanNotExpr(pos,
					new CallExpr(pos,
						new IFldExpr(pos,new ThisExpr(), typeinfo_clazz.resolveField("clazz")),
						Type.tpClass.clazz.resolveMethod("isAssignableFrom",Type.tpBoolean,Type.tpClass),
						new ENode[]{
							new IFldExpr(pos,new LVarExpr(pos,misa.params[0]), typeinfo_clazz.resolveField("clazz"))
						}
					)
				),
				new ReturnStat(pos,new ConstBoolExpr(false)),
				null
			));
			misa.block.stats.add(new ExprStat(pos,
				new AssignExpr(pos,Operator.Assign,
					new LVarExpr(pos,misa.params[0]),
					new CastExpr(pos,typeinfo_clazz.xtype,new LVarExpr(pos,misa.params[0]))
				)
			));
			foreach (ArgType at; this.getTypeInfoArgs()) {
				Field f = typeinfo_clazz.resolveField((nameTypeInfo+"$"+at.name).intern());
				misa.block.stats.add(new IfElseStat(pos,
					new BooleanNotExpr(pos,
						new CallExpr(pos,
							new IFldExpr(pos,new ThisExpr(), f),
							Type.tpTypeInfo.clazz.resolveMethod("$assignableFrom",Type.tpBoolean,Type.tpTypeInfo),
							new ENode[]{
								new IFldExpr(pos,new LVarExpr(pos,misa.params[0]), f)
							}
						)
					),
					new ReturnStat(pos,new ConstBoolExpr(false)),
					null
				));
			}
			misa.block.stats.add(new ReturnStat(pos,new ConstBoolExpr(true)));
		}
		// create $instanceof function
		// public boolean $instanceof(Object obj) {
		// 	if (obj == null ) return false;
		// 	if!(this.clazz.isInstance(obj)) return false;
		// 	return this.$assignableFrom(((Outer)obj).$typeinfo));
		// }
		{
			Method misa = new Method("$instanceof", Type.tpBoolean, ACC_PUBLIC);
			misa.params.add(new FormPar(pos,"obj",Type.tpObject,FormPar.PARAM_NORMAL,ACC_FINAL));
			typeinfo_clazz.addMethod(misa);
			misa.body = new Block(pos);
			misa.block.stats.add(new IfElseStat(pos,
				new BinaryBoolExpr(pos,Operator.Equals,
					new LVarExpr(pos,misa.params[0]),
					new ConstNullExpr()
					),
				new ReturnStat(pos,new ConstBoolExpr(false)),
				null
			));
			misa.block.stats.add(new IfElseStat(pos,
				new BooleanNotExpr(pos,
					new CallExpr(pos,
						new IFldExpr(pos,new ThisExpr(pos), typeinfo_clazz.resolveField("clazz")),
						Type.tpClass.clazz.resolveMethod("isInstance",Type.tpBoolean,Type.tpObject),
						new ENode[]{new LVarExpr(pos,misa.params[0])}
						)
					),
				new ReturnStat(pos,new ConstBoolExpr(false)),
				null
			));
			misa.block.stats.add(new ReturnStat(pos,
				new CallExpr(pos,
					new ThisExpr(),
					typeinfo_clazz.resolveMethod("$assignableFrom",Type.tpBoolean,Type.tpTypeInfo),
					new ENode[]{
						new IFldExpr(pos,
							new CastExpr(pos,this.xtype,new LVarExpr(pos,misa.params[0])),
							this.resolveField(nameTypeInfo)
						)
					}
				)
			));
		}
		Kiev.runProcessorsOn(typeinfo_clazz)
	}
	
	public boolean preGenerate() {
		getStruct().checkResolved();
		if( isMembersPreGenerated() ) return true;
		if( isPackage() ) return false;
		
		// first, pre-generate super-types
		foreach (CompaundMetaType sup; this.getAllSuperTypes())
			((Struct)sup.tdecl).preGenerate();

		// generate typeinfo class, if needed
		autoGenerateTypeinfoClazz();
		// generate a class for interface non-abstract members
		autoGenerateIdefault(this);
		// build vtable
		List<Struct> processed = List.Nil;
		Vector<VTableEntry> vtable = new Vector<VTableEntry>();
		buildVTable(vtable, processed);
		if (Kiev.debugMultiMethod) {
			trace("vtable for "+this+":");
			foreach (VTableEntry vte; vtable) {
				trace("    "+vte.name+vte.etype);
				if (vte.overloader != null)
				trace("            overloaded by "+vte.overloader.name+vte.overloader.etype);
				foreach (Method m; vte.methods)
					trace("        "+m.ctx_tdecl+"."+m.id.uname+m.type);
			}
		}
		
		if (isClazz()) {
			// forward default implementation to interfaces
			foreach (VTableEntry vte; vtable; vte.overloader == null)
				autoProxyMixinMethods(this,vte);
			// generate bridge methods
			foreach (VTableEntry vte; vtable)
				autoBridgeMethods(this,vte);
//			// generate method dispatchers for multimethods
//			foreach (VTableEntry vte; vtable; vte.overloader == null)
//				createMethodDispatchers(vte);
		}
		
		
		setMembersPreGenerated(true);

		combineMethods(this);

		return true;
	}

	static final class VTableEntry {
		String			name;
		CallType		etype;
		protected:no,ro,ro,rw
		List<Method>	methods = List.Nil;
		VTableEntry		overloader;
		VTableEntry(String name, CallType etype) {
			this.name = name;
			this.etype = etype;
		}
		public void add(Method m) {
			assert (!methods.contains(m));
			methods = new List.Cons<Method>(m, methods);
		}
	}
	
	public:no,no,no,rw final void buildVTable(Vector<VTableEntry> vtable, List<Struct> processed) {
		if (processed.contains(this.getStruct()))
			return;
		processed = new List.Cons<Struct>(this.getStruct(), processed);
		// take vtable from super-types
		foreach (TypeRef sup; super_types)
			((RStruct)sup.getType().getStruct()).buildVTable(vtable, processed);
		
		// process override
		foreach (Method m; members; !(m instanceof Constructor)) {
			if (m.isStatic() && !m.isVirtualStatic())
				continue;
			if (m.isMethodBridge())
				continue;
			CallType etype = m.etype;
			String name = m.id.uname;
			boolean is_new = true;
			foreach (VTableEntry vte; vtable) {
				if (name == vte.name && etype ≈ vte.etype) {
					is_new = false;
					if (!vte.methods.contains(m))
						vte.add(m);
				}
			}
			if (is_new)
				vtable.append(new VTableEntry(name, etype));
		}
		
		// process overload
		foreach (VTableEntry vte; vtable) {
			CallType et = vte.etype.toCallTypeRetAny();
			foreach (Method m; members; !(m instanceof Constructor)) {
				if (m.isStatic() && !m.isVirtualStatic())
					continue;
				if (m.isMethodBridge())
					continue;
				if (m.id.uname != vte.name || vte.methods.contains(m))
					continue;
				CallType mt = m.etype.toCallTypeRetAny();
				if (mt ≈ et)
					vte.add(m);
			}
			if (!this.isInterface()) {
				foreach (VTableEntry vte2; vtable; vte2 != vte && vte2.name == vte.name) {
					foreach (Method m; vte2.methods; !vte.methods.contains(m)) {
						CallType mt = m.dtype.toCallTypeRetAny().applay(this.xtype);
						if (mt ≈ et)
							vte.add(m);
					}
				}
			}
		}
		
		// mark overloaded entries in vtable
		foreach (VTableEntry vte1; vtable; vte1.overloader == null) {
			foreach (VTableEntry vte2; vtable; vte1 != vte2 && vte1.name == vte2.name && vte2.overloader == null) {
				CallType t1 = vte1.etype.toCallTypeRetAny();
				CallType t2 = vte2.etype.toCallTypeRetAny();
				if (t1 ≉ t2)
					continue;
				Type r1 = vte1.etype.ret();
				Type r2 = vte2.etype.ret();
				if (r1 ≥ r2)
					vte2.overloader = vte1;
				else if (r2 ≥ r1)
					vte1.overloader = vte2;
				//else
				//	Kiev.reportWarning(this,"Bad method overloading for:\n"+
				//		"    "+vte1.name+vte1.etype+"\n"+
				//		"    "+vte2.name+vte2.etype
				//	);
			}
		}
		// find highest overloader
		foreach (VTableEntry vte; vtable; vte.overloader != null) {
			while (vte.overloader.overloader != null)
				vte.overloader = vte.overloader.overloader;
		}
	}

/*
	private void createMethodDispatchers(VTableEntry vte) {
		// get a set of overloaded methods that are not overriden
		Vector<Method> mmset = new Vector<Method>();
	next_m1:
		foreach (Method m1; vte.methods) {
			for (int i=0; i < mmset.length; i++) {
				Method m2 = mmset[i];
				if (m2.type ≉ m1.type)
					continue; // different overloading
				if (m2.ctx_tdecl.instanceOf(m1.ctx_tdecl))
					continue next_m1; // m2 overrides m1
				mmset[i] = m1; // m1 overrides m2
				continue next_m1;
			}
			// new overloading
			mmset.append(m1);
		}
		// check we have any new method in this class
		Method found = null;
		foreach (Method m; mmset) {
			if (m.ctx_tdecl == this.getStruct()) {
				found = m;
				break;
			}
		}
		if (found == null)
			return; // no new methods in this class
		// make the root dispatch method type
		Method root = new Method(vte.name, vte.etype.ret(), ACC_PUBLIC | ACC_SYNTHETIC);
		root.params.copyFrom(found.params);
		root.pos = found.pos;
		foreach (FormPar fp; root.params) {
			fp.stype = new TypeRef(fp.stype.getType().getErasedType());
			fp.vtype = new TypeRef(fp.stype.getType().getErasedType());
		}
		members.append(root);
		// check if we already have this method in this class
		foreach (Method m; mmset) {
			if (m.ctx_tdecl == this && m.type.applay(this.xtype) ≈ root.type.applay(this.xtype)) {
				members.detach(root);
				root = found = m;
				break;
			}
		}
		if (found != root) {
			vte.add(root);
			mmset.append(root);
		}
		// check it's a multimethod entry
		if (mmset.length <= 1)
			return; // not a multimethod entry
		// make multimethods to be static
		int tmp = 1;
		foreach (Method m; mmset; m != root) {
			if (m.ctx_tdecl == this && !m.isVirtualStatic()) {
				m.setVirtualStatic(true);
				if (m.name.name == vte.name) {
					String name = m.name.name;
					m.name.name = (name+"$mm$"+tmp).intern();
					m.name.addAlias(name);
				}
			}
		}
		
		// create mmtree
		MMTree mmt = new MMTree(root);
		foreach (Method m; mmset; m != root)
			mmt.add(m);

		trace(Kiev.debugMultiMethod,"Dispatch tree "+this+"."+vte.name+vte.etype+" is:\n"+mmt);

		if (root.body==null)
			root.body = new Block(root.pos);
		IfElseStat st = makeDispatchStat(root,mmt);
		if (st != null)
			root.block.stats.insert(0, st);
	}
*/
	private static void autoProxyMixinMethods(@forward RStruct self, VTableEntry vte) {
		// check we have a virtual method for this entry
		foreach (Method m; vte.methods) {
			if (m.ctx_tdecl.isInterface())
				continue;
			// found a virtual method, nothing to proxy here
			return;
		}
		// all methods are from interfaces, check if we have a default implementation
		Method def = null;
		foreach (Method m; vte.methods) {
			// find default implementation class
			if (iface_impl == null)
				continue;
			Method fnd = null;
			Type[] params = m.type.params();
			params = (Type[])Arrays.insert(params,m.ctx_tdecl.xtype,0);
			CallType mt = new CallType(self.xtype,null,params,m.type.ret(),false);
			foreach (Method dm; iface_impl.members; dm.id.uname == m.id.uname && dm.type ≈ mt) {
				fnd = dm;
				break;
			}
			if (def == null)
				def = fnd; // first method found
			else if (fnd.ctx_tdecl.instanceOf(def.ctx_tdecl))
				def = fnd; // overriden default implementation
			else if (def.ctx_tdecl.instanceOf(fnd.ctx_tdecl))
				; // just ignore
			else
				Kiev.reportWarning(self,"Umbigous default implementation for methods:\n"+
					"    "+def.ctx_tdecl+"."+def+"\n"+
					"    "+fnd.ctx_tdecl+"."+fnd
				);
		}
		Method m = null;
		if (def == null) {
			// create an abstract method
			Method def = vte.methods.head();
			if (!self.isAbstract())
				Kiev.reportWarning(self,"Method "+vte.name+vte.etype+" is not implemented in "+self);
			m = new Method(vte.name, vte.etype.ret(), ACC_ABSTRACT | ACC_PUBLIC | ACC_SYNTHETIC);
			for (int i=0; i < vte.etype.arity; i++)
				m.params.append(new FormPar(0,def.params[i].id.uname,vte.etype.arg(i),FormPar.PARAM_NORMAL,ACC_FINAL));
			((Struct)self).members.append(m);
		} else {
			// create a proxy call
			m = new Method(vte.name, vte.etype.ret(), ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC);
			for (int i=0; i < vte.etype.arity; i++)
				m.params.append(new FormPar(0,"arg$"+i,vte.etype.arg(i),FormPar.PARAM_NORMAL,ACC_FINAL));
			((Struct)self).members.append(m);
			m.body = new Block();
			if( m.type.ret() ≡ Type.tpVoid )
				m.block.stats.add(new ExprStat(0,makeDispatchCall(self,0, m, def)));
			else
				m.block.stats.add(new ReturnStat(0,makeDispatchCall(self,0, m, def)));
		}
		vte.add(m);
	}

	private static void autoBridgeMethods(@forward RStruct self, VTableEntry vte) {
		// get overloader vtable entry
		VTableEntry ovr = vte;
		while (ovr.overloader != null)
			ovr = ovr.overloader;
		// find overloader method
		Method mo = null;
		foreach (Method m; vte.methods) {
			if (m.ctx_tdecl == self.getStruct() && m.etype ≈ ovr.etype) {
				mo = m;
				break;
			}
		}
		if (mo == null)
			return; // not overloaded in this class
	next_m:
		foreach (Method m; vte.methods; m.ctx_tdecl != self.getStruct()) {
			// check this class have no such a method
			foreach (Method x; self.members; x.id.uname == m.id.uname) {
				if (x.etype ≈ vte.etype)
					continue next_m;
			}
			Method bridge = new Method(m.id.uname, vte.etype.ret(), ACC_BRIDGE | ACC_SYNTHETIC | mo.flags);
			for (int i=0; i < vte.etype.arity; i++)
				bridge.params.append(new FormPar(mo.pos,m.params[i].id.uname,vte.etype.arg(i),FormPar.PARAM_NORMAL,ACC_FINAL));
			bridge.pos = mo.pos;
			((Struct)self).members.append(bridge);
			trace(Kiev.debugMultiMethod,"Created a bridge method "+self+"."+bridge+" for vtable entry "+vte.name+vte.etype);
			bridge.body = new Block();
			if (bridge.type.ret() ≢ Type.tpVoid)
				bridge.block.stats.append(new ReturnStat(mo.pos,makeDispatchCall(self,mo.pos, bridge, mo)));
			else
				bridge.block.stats.append(new ExprStat(mo.pos,makeDispatchCall(self,mo.pos, bridge, mo)));
			vte.add(bridge);
		}
	}

	private static void autoGenerateIdefault(@forward RStruct self) {
		if (!isInterface() || isStructView())
			return;
		Struct defaults = iface_impl;
		if (defaults != null)
			return;
		foreach (Method m; members) {
			if (!m.isAbstract()) {
				if (m instanceof Constructor) continue; // ignore <clinit>

				// Make inner class name$default
				if( defaults == null ) {
					defaults = Env.newStruct(nameIFaceImpl,true,
						self.getStruct(),ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_FORWARD, true
					);
					((Struct)self).members.add(defaults);
					defaults.setResolved(true);
					iface_impl = defaults;
					Kiev.runProcessorsOn(defaults);
				}
				
				if (m.isStatic()) {
					defaults.members.add(~m);
					continue;
				}

				// Now, non-static methods (templates)
				// Make it static and add abstract method
				Method def = new Method(m.id.uname,m.type.ret(),m.getFlags()|ACC_STATIC);
				def.pos = m.pos;
				def.params.addAll(m.params.delToArray()); // move, because the vars are resolved
				m.params.copyFrom(def.params);
				def.params.insert(0,new FormPar(pos,Constants.nameThis,self.xtype,FormPar.PARAM_NORMAL,ACC_FINAL|ACC_FORWARD));
				defaults.members.add(def);
				def.body = ~m.body;
				def.setVirtualStatic(true);

				m.setAbstract(true);
			}
		}
	}

	private static void combineMethods(@forward RStruct self) {
		List<Method> multimethods = List.Nil;
		for (int cur_m=0; cur_m < members.length; cur_m++) {
			if !(members[cur_m] instanceof Method)
				continue;
			Method m = (Method)members[cur_m];
			if (m.id.equals(nameClassInit) || m.id.equals(nameInit))
				continue;
			if (m.isMethodBridge())
				continue;
			if( multimethods.contains(m) ) {
				trace(Kiev.debugMultiMethod,"Multimethod "+m+" already processed...");
				continue; // do not process method twice...
			}
			Method mmm;
			{
				// create dispatch method
				if (m.isRuleMethod())
					mmm = new RuleMethod(m.id.uname, m.flags | ACC_SYNTHETIC);
				else
					mmm = new Method(m.id.uname, m.type.ret(), m.flags | ACC_SYNTHETIC);
				mmm.setStatic(m.isStatic());
				mmm.id.aliases = m.id.aliases;
				mmm.targs.copyFrom(m.targs);
				foreach (FormPar fp; m.params)
					mmm.params.add(new FormPar(fp.pos,fp.id.uname,fp.stype.getType(),fp.kind,fp.flags));
				((Struct)self).members.add(mmm);
			}
			CallType type1 = mmm.type;
			CallType dtype1 = mmm.dtype;
			CallType etype1 = mmm.etype;
			((Struct)self).members.detach(mmm);
			Method mm = null;
			trace(Kiev.debugMultiMethod,"Generating dispatch method for "+m+" with dispatch type "+etype1);
			// find all methods with the same java type
			ListBuffer<Method> mlistb = new ListBuffer<Method>();
			foreach (Method mj; members; !mj.isMethodBridge() && mj.isStatic() == m.isStatic()) {
				CallType type2 = mj.type;
				CallType dtype2 = mj.dtype;
				CallType etype2 = mj.etype;
				if( mj.id.uname != m.id.uname || etype2.arity != etype1.arity )
					continue;
				if (etype1.isMultimethodSuper(etype2)) {
					trace(Kiev.debugMultiMethod,"added dispatchable method "+mj);
					if (mm == null) {
						if (type1.equals(type2))
							mm = mj;
					} else {
						if (mm.type.greater(type2))
							mm = mj;
					}
					mlistb.append(mj);
				} else {
					trace(Kiev.debugMultiMethod,"methods "+mj+" with dispatch type "+etype2+" doesn't match...");
				}
			}
			Method overwr = null;

			if (super_types.length > 0)
				overwr = super_types[0].getStruct().getOverwrittenMethod(self.xtype,m);

			// nothing to do, if no methods to combine
			if (mlistb.length() == 1 && mm != null) {
				// mm will have the base type - so, no super. call will be done
				trace(Kiev.debugMultiMethod,"no need to dispatch "+m);
				continue;
			}

			List<Method> mlist = mlistb.toList();

			if (mm == null) {
				// create a new dispatcher method...
				mm = mmm;
				self.getStruct().addMethod(mm);
				trace(Kiev.debugMultiMethod,"will add new dispatching method "+mm);
			} else {
				// if multimethod already assigned, thus, no super. call will be done - forget it
				trace(Kiev.debugMultiMethod,"will attach dispatching to this method "+mm);
				overwr = null;
			}

			// create mmtree
			MMTree mmt = new MMTree(mm);

			foreach (Method m; mlist; m != mm)
				mmt.add(m);

			trace(Kiev.debugMultiMethod,"Dispatch tree "+mm+" is:\n"+mmt);

			IfElseStat st = null;
			st = makeDispatchStat(self,mm,mmt);

			if (overwr != null) {
				IfElseStat last_st = st;
				ENode br;
				while (last_st.elseSt != null)
					last_st = (IfElseStat)last_st.elseSt;
				ENode[] vae = new ENode[mm.params.length];
				for(int k=0; k < vae.length; k++) {
					vae[k] = new CastExpr(0,mm.type.arg(k), new LVarExpr(0,mm.params[k]));
				}
				CallExpr ce = new CallExpr(0,new ThisExpr(true),overwr,null,vae);
				ce.setSuperExpr(true);
				if( m.type.ret() ≢ Type.tpVoid ) {
					if( overwr.type.ret() ≡ Type.tpVoid )
						br = new Block(0,new ENode[]{
							new ExprStat(0,ce),
							new ReturnStat(0,new ConstNullExpr())
						});
					else if( !overwr.type.ret().isReference() && mm.type.ret().isReference() ) {
						br = new ReturnStat(0,ce);
						CastExpr.autoCastToReference(ce);
					}
					else {
						br = new ReturnStat(0,ce);
					}
				} else {
					br = new Block(0,new ENode[]{
						new ExprStat(0,ce),
						new ReturnStat(0,null)
					});
				}
				last_st.elseSt = br;
			}
			assert (mm.parent() == self.getStruct());
			if (st != null) {
				Block body = new Block(0);
				body.stats.add(st);
				if (mm.body != null)
					mm.block.stats.insert(0, body);
				else
					mm.body = body;
			}
			multimethods = new List.Cons<Method>(mm, multimethods);
		}
	}

	private static IfElseStat makeDispatchStat(@forward RStruct self, Method mm, MMTree mmt) {
		IfElseStat dsp = null;
		ENode cond = null;
		for(int i=0; i < mmt.uppers.length; i++) {
			if( mmt.uppers[i] == null ) continue;
			Method m = mmt.uppers[i].m;
			for(int j=0; j < m.type.arity; j++) {
				Type t = m.type.arg(j);
				if( mmt.m != null && t.equals(mmt.m.type.arg(j)) ) continue;
				ENode be = null;
				if( mmt.m != null && !t.equals(mmt.m.type.arg(j)) ) {
					if (!t.isReference())
						be = new InstanceofExpr(pos, new LVarExpr(pos,mm.params[j]), ((CoreType)t).getRefTypeForPrimitive());
					else
						be = new InstanceofExpr(pos, new LVarExpr(pos,mm.params[j]), t);
				}
				while (t instanceof CTimeType)
					t = t.getEnclosedType();
				if (t instanceof CompaundType && ((CompaundType)t).clazz.isTypeUnerasable()) {
					if (t.getStruct().typeinfo_clazz == null)
						((RStruct)t.getStruct()).autoGenerateTypeinfoClazz();
					ENode tibe = new CallExpr(pos,
						accessTypeInfoField(mmt.m,t,false),
						Type.tpTypeInfo.clazz.resolveMethod("$instanceof",Type.tpBoolean,Type.tpObject),
						new ENode[]{ new LVarExpr(pos,mm.params[j]) }
						);
					if( be == null )
						be = tibe;
					else
						be = new BinaryBooleanAndExpr(0,be,tibe);
				}
				if( cond == null ) cond = be;
				else cond = new BinaryBooleanAndExpr(0,cond,be);
			}
			if( cond == null )
//				throw new RuntimeException("Null condition in "+mmt.m+" -> "+m+" dispatching");
				cond = new ConstBoolExpr(true);
			IfElseStat br;
			if( mmt.uppers[i].uppers.length==0 ) {
				ENode st = makeMMDispatchCall(self,mmt.uppers[i].m.pos,mm,mmt.uppers[i].m);
				br = new IfElseStat(0,cond,st,null);
			} else {
				br = new IfElseStat(0,cond,makeDispatchStat(self,mm,mmt.uppers[i]),null);
			}
			cond = null;
			if( dsp == null ) dsp = br;
			else {
				IfElseStat st = dsp;
				while( st.elseSt != null ) st = (IfElseStat)st.elseSt;
				st.elseSt = br;
			}
		}
		if( mmt.m != mm && mmt.m != null) {
			ENode br;
			br = makeMMDispatchCall(self,mmt.m.pos,mm,mmt.m);
			IfElseStat st = dsp;
			while( st.elseSt != null ) st = (IfElseStat)st.elseSt;
			st.elseSt = br;
		}
		return dsp;
	}
	
	private static ENode makeMMDispatchCall(@forward RStruct self, int pos, Method dispatcher, Method dispatched) {
		assert (dispatched != dispatcher);
		assert (dispatched.isAttached());
		if (dispatched.ctx_tdecl == self.getStruct()) {
			assert (dispatched.parent() == self.getStruct());
			return new InlineMethodStat(pos,~dispatched,dispatcher);
		} else {
			return makeDispatchCall(self,pos,dispatched,dispatcher);
		}
	}

	private static ENode makeDispatchCall(@forward RStruct self, int pos, Method dispatcher, Method dispatched) {
		//return new InlineMethodStat(pos,dispatched,dispatcher)
		ENode obj = null;
		if (!dispatched.isStatic() && !dispatcher.isStatic())
			obj = new ThisExpr(pos);
		CallExpr ce = new CallExpr(pos, obj, dispatched, null, ENode.emptyArray);
		if (self.getStruct() != dispatched.ctx_tdecl)
			ce.setSuperExpr(true);
		if (dispatched.isVirtualStatic() && !dispatcher.isStatic())
			ce.args.append(new ThisExpr(pos));
		foreach (FormPar fp; dispatcher.params)
			ce.args.append(new LVarExpr(0,fp));
		if!(dispatcher.etype.ret() ≥ dispatched.etype.ret())
			return new CastExpr(pos, dispatcher.etype.ret(), ce);
		return ce;
	}

	static class MMTree {
		static MMTree[] emptyArray = new MMTree[0];
		Method m;
		MMTree[] uppers = MMTree.emptyArray;
		MMTree(Method m) { this.m = m; }
		void add(Method mm) {
			if( m!=null && !mm.type.greater(m.type) ) {
				trace(Kiev.debugMultiMethod,"method "+mm+" type <= "+m);
				if( m.type.isMultimethodSuper(mm.type) ) {
					trace(Kiev.debugMultiMethod,"method "+mm+" type == "+m);
					// dispatched method of equal type
					MMTree mt = new MMTree(mm);
					mt.uppers = uppers;
					uppers = new MMTree[]{mt};
					return;
				}
				throw new RuntimeException("Method "+mm+" not added to mm tree!!!");
			}
			for(int i=0; i < uppers.length; i++) {
				if( uppers[i] == null ) continue;
				if( mm.type.greater(uppers[i].m.type) ) {
					trace(Kiev.debugMultiMethod,"method "+mm+" type > "+m);
					uppers[i].add(mm);
					return;
				}
			}
			int link_to = -1;
			for(int i=0; i < uppers.length; i++) {
				if( uppers[i].m.type.greater(mm.type) ) {
					if( uppers[i] == null ) continue;
					if( link_to < 0 ) {
						MMTree mt = new MMTree(mm);
						mt.uppers = new MMTree[]{uppers[i]};
						uppers[i] = mt;
						link_to = i;
					} else {
						uppers[link_to].uppers = (MMTree[])Arrays.append(uppers[link_to].uppers, uppers[i]);
						uppers[i] = null;
					}
				}
			}
			trace(Kiev.debugMultiMethod,"method "+mm+" linked to "+link_to);
			if( link_to < 0 ) {
				uppers = (MMTree[])Arrays.append(uppers, new MMTree(mm));
			}
		}
		public String toString() {
			StringBuffer sb = new StringBuffer("\n");
			return dump(0,sb).toString();
		}
		public StringBuffer dump(int i, StringBuffer sb) {
			for(int j=0; j < i; j++) sb.append('\t');
			if (m != null)
				sb.append(m.parent()).append('.').append(m).append('\n');
			else
				sb.append("root:\n");
			for(int j=0; j < uppers.length; j++) {
				if( uppers[j] == null ) continue;
				uppers[j].dump(i+1,sb);
			}
			return sb;
		}

	}
	

	public void autoGenerateStatements() {

		if( Kiev.debug ) System.out.println("AutoGenerating statements for "+this);
		// <clinit> & common$init, if need
		Constructor class_init = null;
		Initializer instance_init = null;
		int static_pos = -1;

		foreach (DNode n; members; n instanceof Field || n instanceof Initializer) {
			if (n == instance_init)
				continue;
			if( isInterface() && !n.isAbstract() ) {
				n.setStatic(true);
				n.setFinal(true);
			}
			if( n instanceof Field ) {
				Field f = (Field)n;
				if (f.init == null)
					continue;
				if (f.isConstantExpr())
					f.const_value = ConstExpr.fromConst(f.getConstValue());
				if (f.init.isConstantExpr() && f.isStatic())
					continue;
				if (f.isAddedToInit())
					continue;
				if( f.isStatic() ) {
					if( class_init == null ) {
						class_init = getClazzInitMethod();
						static_pos = class_init.block.stats.length-1;
						if (static_pos < 0) {
							static_pos = 0;
						} else {
							assert (class_init.block.stats[static_pos] instanceof ReturnStat);
						}
					}
					ENode init_stat = new ExprStat(f.init.pos,
							new AssignExpr(f.init.pos,
								f.isInitWrapper() ? Operator.Assign2 : Operator.Assign,
								new SFldExpr(f.pos,f),f.init.ncopy()
							)
						);
					class_init.block.stats.insert(static_pos++,init_stat);
					Kiev.runProcessorsOn(init_stat);
					RStruct.runResolveOn(init_stat);
				} else {
					if( instance_init == null ) {
						instance_init = new Initializer();
						instance_init.pos = f.init.pos;
						instance_init.body = new Block();
						((Struct)this).members.add(instance_init);
					}
					ENode init_stat;
					init_stat = new ExprStat(f.init.pos,
							new AssignExpr(f.init.pos,
								f.isInitWrapper() ? Operator.Assign2 : Operator.Assign,
								new IFldExpr(f.pos,new ThisExpr(0),f),
								f.init.ncopy()
							)
						);
					instance_init.block.stats.add(init_stat);
					init_stat.setHidden(true);
					Kiev.runProcessorsOn(init_stat);
					RStruct.runResolveOn(init_stat);
				}
				f.setAddedToInit(true);
			} else {
				Initializer init = (Initializer)n;
				ENode init_stat = new Shadow(init);
				init_stat.setHidden(true);
				if (init.isStatic()) {
					if( class_init == null ) {
						class_init = getClazzInitMethod();
						static_pos = class_init.block.stats.length-1;
						if (static_pos < 0) {
							static_pos = 0;
						} else {
							assert (class_init.block.stats[static_pos] instanceof ReturnStat);
						}
					}
					class_init.block.stats.insert(static_pos++,init_stat);
				} else {
					if( instance_init == null ) {
						instance_init = new Initializer();
						instance_init.pos = init.pos;
						instance_init.body = new Block();
						((Struct)this).members.add(instance_init);
					}
					instance_init.block.stats.add(init_stat);
					Kiev.runProcessorsOn(init_stat);
					RStruct.runResolveOn(init_stat);
				}
			}
		}

		// template methods of interfaces
		if( isInterface() ) {
			foreach (Method m; members) {
				if( !m.isAbstract() ) {
					if( m.isStatic() ) continue;
					// Now, non-static methods (templates)
					// Make it static and add abstract method
					Method abstr = new Method(m.id.uname,m.type.ret(),m.getFlags() | ACC_PUBLIC );
					abstr.pos = m.pos;
					abstr.setStatic(false);
					abstr.setAbstract(true);
					abstr.params.copyFrom(m.params);

					m.setStatic(true);
					m.setVirtualStatic(true);
					this.addMethod(abstr);
				}
				if( !m.isStatic() ) {
					m.setAbstract(true);
				}
			}
		}
		
		// Generate super(...) constructor calls, if they are not
		// specified as first statements of a constructor
		if (qname() != Type.tpObject.clazz.qname()) {
			foreach (Constructor m; members) {
				if( m.isStatic() ) continue;

				Block initbody = m.body;

				boolean gen_def_constr = false;
				if (initbody.stats.length == 0) {
					gen_def_constr = true;
				} else {
					if (initbody.stats[0] instanceof ExprStat) {
						ExprStat es = (ExprStat)initbody.stats[0];
						ENode ce = es.expr;
						if (ce instanceof CallExpr && ce.func instanceof Constructor) {
							String nm = ce.ident.name;
							if (nm == nameThis)
								; // this(args)
							else if (nm == nameSuper)
								m.setNeedFieldInits(true);
							else
								throw new CompilerException(ce,"Expected one of this() or super() constructor call");
						}
						else
							gen_def_constr = true;
					}
					else
						gen_def_constr = true;
				}
				if( gen_def_constr ) {
					m.setNeedFieldInits(true);
					CallExpr call_super = new CallExpr(pos, null, new SymbolRef(pos, nameSuper), null, ENode.emptyArray);
					if( super_types.length > 0 && super_types[0].getStruct() == Type.tpClosureClazz ) {
						ASTIdentifier max_args = new ASTIdentifier();
						max_args.name = nameClosureMaxArgs;
						call_super.args.add(max_args);
					}
					else if( package_clazz.isClazz() && isAnonymouse() ) {
						int skip_args = 0;
						if( !isStatic() ) skip_args++;
						if( this.isTypeUnerasable() && super_types[0].getStruct().isTypeUnerasable() ) skip_args++;
						if( m.params.length > skip_args+1 ) {
							for(int i=skip_args+1; i < m.params.length; i++) {
								call_super.args.append( new LVarExpr(m.pos,m.params[i]));
							}
						}
					}
					else if( isEnum() ) {
						call_super.args.add(new ASTIdentifier(pos, "name"));
						call_super.args.add(new ASTIdentifier(pos, nameEnumOrdinal));
						//call_super.args.add(new ASTIdentifier(pos, "text"));
					}
					else if( isForward() && package_clazz.isStructView() && super_types[0].getStruct().package_clazz.isStructView() ) {
						call_super.args.add(new ASTIdentifier(pos, nameImpl));
					}
					initbody.stats.insert(0,new ExprStat(call_super));
					Kiev.runProcessorsOn(initbody.stats[0]);
					RStruct.runResolveOn(initbody.stats[0]);
				}
				int p = 1;
				if( package_clazz.isClazz() && !isStatic() ) {
					initbody.stats.insert(p,
						new ExprStat(pos,
							new AssignExpr(pos,Operator.Assign,
								new IFldExpr(pos,new ThisExpr(pos),OuterThisAccessExpr.outerOf((Struct)this)),
								new LVarExpr(pos,m.params[0])
							)
						)
					);
					Kiev.runProcessorsOn(initbody.stats[p]);
					RStruct.runResolveOn(initbody.stats[p]);
					p++;
				}
				if (isForward() && package_clazz.isStructView()) {
					Field fview = this.resolveField(nameImpl);
					if (fview.parent() == (Struct)this) {
						foreach (FormPar fp; m.params; fp.id.equals(nameImpl)) {
							initbody.stats.insert(p,
								new ExprStat(pos,
									new AssignExpr(pos,Operator.Assign,
										new IFldExpr(pos,new ThisExpr(pos),resolveField(nameImpl)),
										new LVarExpr(pos,fp)
									)
								)
							);
							Kiev.runProcessorsOn(initbody.stats[p]);
							RStruct.runResolveOn(initbody.stats[p]);
							p++;
							break;
						}
					}
				}
				if (isTypeUnerasable() && m.isNeedFieldInits()) {
					Field tif = resolveField(nameTypeInfo);
					Var v = m.getTypeInfoParam(FormPar.PARAM_TYPEINFO);
					assert(v != null);
					initbody.stats.insert(p,
						new ExprStat(pos,
							new AssignExpr(m.pos,Operator.Assign,
								new IFldExpr(m.pos,new ThisExpr(0),tif),
								new LVarExpr(m.pos,v)
							))
						);
					Kiev.runProcessorsOn(initbody.stats[p]);
					RStruct.runResolveOn(initbody.stats[p]);
					p++;
				}
				if( instance_init != null && m.isNeedFieldInits() ) {
					initbody.stats.insert(p,instance_init.body.ncopy());
					Kiev.runProcessorsOn(initbody.stats[p]);
					RStruct.runResolveOn(initbody.stats[p]);
					p++;
				}
			}
		}
	}

	private static void runResolveOn(ASTNode node) {
		if (node instanceof ENode)
			node.resolve(null);
		else if (node instanceof DNode)
			node.resolveDecl();
	}
	
	public void resolveDecl() {
		if( isGenerated() ) return;
		long curr_time;
		if( !isPackage() ) {
			foreach (Struct ss; members) {
				try {
					ss.resolveDecl();
				} catch(Exception e ) {
					Kiev.reportError(ss,e);
				}
			}
		}

		long diff_time = curr_time = System.currentTimeMillis();
		try {
			// Verify access
			foreach(Field f; members) {
				try {
					f.type.checkResolved();
					if (f.type.getStruct()!=null)
						Access.verifyReadWrite((Struct)this,f.type.getStruct());
				} catch(Exception e ) { Kiev.reportError(f,e); }
			}
			foreach(Method m; members) {
				try {
					m.type.ret().checkResolved();
					if (m.type.ret().getStruct()!=null)
						Access.verifyReadWrite((Struct)this,m.type.ret().getStruct());
					foreach(Type t; m.type.params()) {
						t.checkResolved();
						if (t.getStruct()!=null)
							Access.verifyReadWrite((Struct)this,t.getStruct());
					}
				} catch(Exception e ) { Kiev.reportError(m,e); }
			}

			// for(;;) beacause a constructor may be added during resolving
			for(int i=0; i < members.length; i++) {
				ASTNode n = members[i];
				if (n instanceof Field)
					n.resolveDecl();
			}
			for(int i=0; i < members.length; i++) {
				ASTNode n = members[i];
				if (n instanceof Method || n instanceof Initializer)
					((DNode)n).resolveDecl();
			}
			autoGenerateStatements();
			foreach (Constructor c; members; !c.isResolved())
				c.resolveDecl();
			
			// Autogenerate hidden args for initializers of local class
			if( isLocal() ) {
				Field[] proxy_fields = Field.emptyArray;
				foreach(Field f; members) {
					if( f.isNeedProxy() )
						proxy_fields = (Field[])Arrays.append(proxy_fields,f);
				}
				if( proxy_fields.length > 0 ) {
					foreach(Method m; members) {
						if( !m.id.equals(nameInit) ) continue;
						for(int j=0; j < proxy_fields.length; j++) {
							int par = m.params.length;
							String nm = proxy_fields[j].id.sname;
							m.params.append(new FormPar(m.pos,nm,proxy_fields[j].type,FormPar.PARAM_LVAR_PROXY,ACC_FINAL|ACC_SYNTHETIC));
							m.block.stats.insert(
								1,
								new ExprStat(m.pos,
									new AssignExpr(m.pos,Operator.Assign,
										new IFldExpr(m.pos,new ThisExpr(0),proxy_fields[j]),
										new LVarExpr(m.pos,m.params[par])
									)
								)
							);
							((ENode)m.block.stats[1]).resolve(Type.tpVoid);
						}
					}
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
		setGenerated(true);
		//diff_time = System.currentTimeMillis() - curr_time;
		//if( Kiev.verbose ) Kiev.reportInfo("Resolved class "+this,diff_time);
	}
	
}

