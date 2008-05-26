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
public final view RStruct of Struct extends RTypeDecl {

	static final AttrSlot TI_ATTR = new ExtAttrSlot("rstruct ti field temp expr",ANode.nodeattr$parent,false,true,TypeInfo.newTypeInfo(TypeInfoExpr.class,null));	

	public:ro			WrapperMetaType			wmeta_type;
	public:ro			SymbolRef<TypeDecl>		package_clazz;
	public				Struct					typeinfo_clazz;
	public				Struct					iface_impl;
	public:ro			DNode[]					sub_decls;

	public final Struct getStruct() { return (Struct)this; }

	// a pizza case	
	public final boolean isPizzaCase();
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
	// is a mixin
	public final boolean isMixin();
	// indicates that structure members were generated
	public final boolean isMembersGenerated();
	public final void setMembersGenerated(boolean on);
	// indicates that structure members were pre-generated
	public final boolean isMembersPreGenerated();
	public final void setMembersPreGenerated(boolean on);
	// indicates that type of the structure was attached
	public final boolean isTypeResolved();
	public final void setTypeResolved(boolean on);
	// indicates that type arguments of the structure were resolved
	public final boolean isArgsResolved();
	public final void setArgsResolved(boolean on);
	// kiev annotation
	public final boolean isAnnotation();
	// java enum
	public final boolean isEnum();
	// structure was loaded from bytecode
	public final boolean isLoadedFromBytecode();
	public final void setLoadedFromBytecode(boolean on);

	public Struct addSubStruct(Struct sub);
	public Method addMethod(Method m);
	public Field addField(Field f);
	public PizzaCase addCase(PizzaCase cas);

	public boolean instanceOf(TypeDecl cl);
	public Field resolveField(String name);
	public Field resolveField(String name, boolean fatal);
	public Method resolveMethod(String name, Type ret, Type... va_args);
	public Constructor getClazzInitMethod();

	public ENode accessTypeInfoField(ASTNode from, Type t, boolean from_gen) {
		while (t instanceof CTimeType)
			t = t.getEnclosedType();
		if (t.getStruct() != null && ((Struct)this) == t.getStruct().typeinfo_clazz && t.getStruct().xtype ≈ t)
			return new ThisExpr(from.pos);
		Method ctx_method = from.ctx_method;
		if (t.isUnerasable()) {
			if (ctx_method != null && ctx_method.isTypeUnerasable() && t instanceof ArgType) {
				int i=0;
				foreach (TypeDef td; ctx_method.targs) {
					if (td.getAType() == t)
						return new LVarExpr(from.pos, ctx_method.getTypeInfoParam(Var.PARAM_TYPEINFO_N+i));
					i++;
				}
			}
			if (this.instanceOf(Type.tpTypeInfo.tdecl) && ctx_method != null && ctx_method instanceof Constructor && !ctx_method.isStatic()) {
				if (t instanceof ArgType)
					return new EToken(from.pos,t.name.toString(),ETokenKind.IDENTIFIER,true);
			}
			if (this.isTypeUnerasable()) {
				ENode ti_access;
				if (ctx_method != null && ctx_method.isStatic()) {
					// check we have $typeinfo as first argument
					if (ctx_method.getTypeInfoParam(Var.PARAM_TYPEINFO) == null)
						throw new CompilerException(from,"$typeinfo cannot be accessed from "+ctx_method);
					else
						ti_access = new LVarExpr(from.pos,ctx_method.getTypeInfoParam(Var.PARAM_TYPEINFO));
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
		if ((from.ctx_method == null || from.ctx_method instanceof Constructor && from.ctx_method.isStatic()) && from.ctx_tdecl.isInterface()) {
			return new TypeInfoExpr(from.pos, new TypeRef(t));
		}
		
		// Lookup and create if need as $typeinfo$N
		foreach(Field f; this.members; f.isStatic()) {
			if (f.init == null || !f.sname.startsWith(nameTypeInfo) || f.sname.equals(nameTypeInfo))
				continue;
			if (((TypeInfoExpr)f.init).type.getType() ≈ t)
				return new SFldExpr(from.pos,f);
		}
		TypeInfoExpr ti_expr = new TypeInfoExpr(pos, new TypeRef(t));
		// check we can use a static field
		TI_ATTR.set(from, ti_expr);
		try {
			ti_expr.resolve(null);
		} finally { ~ti_expr; }
		foreach (ENode ti_arg; ti_expr.cl_args; !(ti_arg instanceof SFldExpr)) {
			// oops, cannot make it a static field
			return ti_expr;
		}
		if (from_gen)
			throw new RuntimeException("Ungenerated typeinfo for type "+t+" ("+t.getClass()+")");
		int i = 0;
		foreach(Field f; this.members; f.isStatic()) {
			if (f.init == null || !f.sname.startsWith(nameTypeInfo) || f.sname.equals(nameTypeInfo))
				continue;
			i++;
		}
		Field f = new Field(nameTypeInfo+"$"+i,ti_expr.getType(),ACC_SYNTHETIC|ACC_STATIC|ACC_FINAL); // package-private for inner classes
		f.init = ti_expr;
		getStruct().addField(f);
		f.resolveDecl();
		// Add initialization in <clinit>
		Constructor class_init = getStruct().getClazzInitMethod();
		if( ctx_method != null && ctx_method instanceof Constructor && ctx_method.isStatic() ) {
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
		int flags = this.meta.mflags & JAVA_ACC_MASK;
		flags &= ~(ACC_PRIVATE | ACC_PROTECTED);
		flags |= ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC;
		typeinfo_clazz = Env.getRoot().newStruct(nameClTypeInfo,true,this.getStruct(),flags,new JavaClass(),true,null);
		((Struct)this).members.add(typeinfo_clazz);
		typeinfo_clazz.setPublic();
		if (super_types.length > 0 && super_types[0].getStruct().typeinfo_clazz != null)
			typeinfo_clazz.super_types.insert(0, new TypeRef(super_types[0].getStruct().typeinfo_clazz.xtype));
		else
			typeinfo_clazz.super_types.insert(0, new TypeRef(Type.tpTypeInfo));
		if (this.isInterfaceOnly())
			typeinfo_clazz.meta.is_interface_only = true;
		getStruct().addSubStruct(typeinfo_clazz);
		typeinfo_clazz.pos = pos;

		// create constructor method
		{
			Constructor init = new Constructor(ACC_PROTECTED);
			init.body = new Block(pos);
			init.params.add(new LVar(pos,"hash",Type.tpInt,Var.PARAM_NORMAL,ACC_FINAL));
			init.params.add(new LVar(pos,"clazz",Type.tpClass,Var.PARAM_NORMAL,ACC_FINAL));
			// add in it arguments fields, and prepare for constructor
			foreach (ArgType at; this.getTypeInfoArgs()) {
				String fname = nameTypeInfo+"$"+at.name;
				Field f = new Field(fname,Type.tpTypeInfo,ACC_PUBLIC|ACC_FINAL);
				typeinfo_clazz.addField(f);
				LVar v = new LVar(pos,at.name.toString(),Type.tpTypeInfo,Var.PARAM_NORMAL,ACC_FINAL);
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
			CtorCallExpr call_super = new CtorCallExpr(pos, new SuperExpr(), ENode.emptyArray);
			call_super.args.add(new LVarExpr(pos,init.params[0]));
			call_super.args.add(new LVarExpr(pos,init.params[1]));
			init.block.stats.insert(0,new ExprStat(call_super));
			foreach (ArgType at; ((RStruct)super_types[0].getStruct()).getTypeInfoArgs()) {
				Type t = at.applay(this.xtype);
				ENode expr;
				if (t instanceof ArgType)
					expr = new EToken(pos,t.name.toString(),ETokenKind.IDENTIFIER,true);
				else if (t.isUnerasable())
					expr = new TypeInfoExpr(pos,new TypeRef(t));
				else
					expr = accessTypeInfoField(call_super, t, false);
				call_super.args.append(expr);
			}

			// create method to get typeinfo field
			Method tim = getStruct().addMethod(new MethodImpl(nameGetTypeInfo,Type.tpTypeInfo,ACC_PUBLIC | ACC_SYNTHETIC));
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
		Method mNewTypeInfo = null;
		{
			Method init = new MethodImpl("newTypeInfo", typeinfo_clazz.xtype, ACC_STATIC|ACC_PUBLIC);
			init.params.add(new LVar(pos,"clazz",Type.tpClass,Var.PARAM_NORMAL,ACC_FINAL));
			init.params.add(new LVar(pos,"args",new ArrayType(Type.tpTypeInfo),Var.PARAM_NORMAL,ACC_FINAL));
			init.body = new Block(pos);
			Var h = new LVar(pos,"hash",Type.tpInt,Var.VAR_LOCAL,ACC_FINAL);
			Var v = new LVar(pos,"ti",typeinfo_clazz.xtype,Var.VAR_LOCAL,0);
			Method mhash = Type.tpTypeInfo.tdecl.resolveMethod("hashCode",Type.tpInt,Type.tpClass,new ArrayType(Type.tpTypeInfo));
			h.init = new CallExpr(pos,null,mhash,new ENode[]{
				new LVarExpr(pos,init.params[0]),
				new LVarExpr(pos,init.params[1])
			});
			init.block.addSymbol(h);
			Method mget = Type.tpTypeInfo.tdecl.resolveMethod("get",Type.tpTypeInfo,Type.tpInt,Type.tpClass,new ArrayType(Type.tpTypeInfo));
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
			mNewTypeInfo = init;
		}

		// create getTopArgs()
		// public TypeInfo[] getTopArgs() {
		//  return new TypeInfo[]{typeinfo$A,typeinfo$B,...);
		// }
		{
			Method mrr = new MethodImpl("getTopArgs", new ArrayType(Type.tpTypeInfo), ACC_PUBLIC);
			typeinfo_clazz.addMethod(mrr);
			mrr.body = new Block(pos);
			Vector<ENode> targs = new Vector<ENode>();
			foreach (ArgType at; this.getTypeInfoArgs()) {
				Field f = typeinfo_clazz.resolveField((nameTypeInfo+"$"+at.name).intern());
				targs.append(new IFldExpr(pos,new ThisExpr(pos), f));
			}
			mrr.block.stats.add(new ReturnStat(pos,
				new NewInitializedArrayExpr(pos,new TypeExpr(Type.tpTypeInfo,Operator.PostTypeArray),1,targs.toArray())
				));
		}
		
		// create equals function:
		// public boolean eq(Clazz clazz, TypeInfo... args) {
		// 	if (this.clazz != clazz) return false;
		// 	if (typeinfo$0 != args[0]) return false;
		// 	...
		// 	return true;
		// }
		{
			Method meq = new MethodImpl("eq", Type.tpBoolean, ACC_PUBLIC);
			meq.params.add(new LVar(pos,"clazz",Type.tpClass,Var.PARAM_NORMAL,ACC_FINAL));
			meq.params.add(new LVar(pos,"args",new ArrayType(Type.tpTypeInfo),Var.PARAM_VARARGS,ACC_FINAL));
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
			Method misa = new MethodImpl("$assignableFrom", Type.tpBoolean, ACC_PUBLIC);
			misa.params.add(new LVar(pos,"ti",Type.tpTypeInfo,Var.PARAM_NORMAL,ACC_FINAL));
			typeinfo_clazz.addMethod(misa);
			misa.body = new Block(pos);
			misa.block.stats.add(new IfElseStat(pos,
				new BooleanNotExpr(pos,
					new CallExpr(pos,
						new IFldExpr(pos,new ThisExpr(), typeinfo_clazz.resolveField("clazz")),
						Type.tpClass.tdecl.resolveMethod("isAssignableFrom",Type.tpBoolean,Type.tpClass),
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
							Type.tpTypeInfo.tdecl.resolveMethod("$assignableFrom",Type.tpBoolean,Type.tpTypeInfo),
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
			Method misa = new MethodImpl("$instanceof", Type.tpBoolean, ACC_PUBLIC);
			misa.params.add(new LVar(pos,"obj",Type.tpObject,Var.PARAM_NORMAL,ACC_FINAL));
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
						Type.tpClass.tdecl.resolveMethod("isInstance",Type.tpBoolean,Type.tpObject),
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
		// create newInstance function
		// public Object newInstance() {
		// 	return new Xxx();
		// }
		{
			boolean ctor_exists = false;
			foreach(Constructor c; this.members; !c.isStatic() && c.isPublic() && c.type.arity==0) {
				ctor_exists = true;
				break;
			}
			if (ctor_exists) {
				Method mni = new MethodImpl("newInstance", Type.tpObject, ACC_PUBLIC);
				typeinfo_clazz.addMethod(mni);
				mni.body = new Block(pos);
				mni.block.stats.add(new ReturnStat(pos,
					new NewExpr(pos,new TypeRef(this.xtype),ENode.emptyArray)
				));
			}
		}
		Kiev.runProcessorsOn(typeinfo_clazz)
	}
	
	public void autoGenerateConstructor() {
		if (!isInterface() && !isPackage() && !isSyntax()) {
			//updatePackageClazz();
			// Default <init> method, if no one is declared
			boolean init_found = false;
			// Add outer hidden parameter to constructors for inner and non-static classes
			foreach (Constructor m; members; !m.isStatic()) {
				init_found = true;
				package_clazz.dnode.checkResolved();
				if (!isInterface() && isTypeUnerasable() && m.getTypeInfoParam(Var.PARAM_TYPEINFO) == null)
					m.params.insert(0,new LVar(m.pos,nameTypeInfo,typeinfo_clazz.xtype,Var.PARAM_TYPEINFO,ACC_FINAL|ACC_SYNTHETIC));
				if (isStructInner() && !isStatic() && m.getOuterThisParam() == null)
					m.params.insert(0,new LVar(m.pos,nameThisDollar,package_clazz.dnode.xtype,Var.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
				if (isEnum()) {
					if (m.params.length < 1 || m.params[0].kind != Var.PARAM_ENUM_NAME)
						m.params.insert(0,new LVar(pos,"enum$name",Type.tpString,Var.PARAM_ENUM_NAME,ACC_SYNTHETIC));
					if (m.params.length < 2 || m.params[1].kind != Var.PARAM_ENUM_ORD)
						m.params.insert(1,new LVar(pos,"enum$ordinal",Type.tpInt,Var.PARAM_ENUM_ORD,ACC_SYNTHETIC));
				}
			}
			if( !init_found ) {
				trace(Kiev.debug && Kiev.debugResolve,"Constructor not found in class "+this);
				Constructor init = new Constructor(ACC_PUBLIC);
				init.setAutoGenerated(true);
				if (this != Type.tpClosureClazz && this.instanceOf(Type.tpClosureClazz)) {
					if (isStructInner() && !isStatic()) {
						init.params.append(new LVar(pos,nameThisDollar,package_clazz.dnode.xtype,Var.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
						init.params.append(new LVar(pos,"max$args",Type.tpInt,Var.PARAM_NORMAL,ACC_SYNTHETIC));
					} else {
						init.params.append(new LVar(pos,"max$args",Type.tpInt,Var.PARAM_NORMAL,ACC_SYNTHETIC));
					}
				} else {
					if (isStructInner() && !isStatic()) {
						init.params.append(new LVar(pos,nameThisDollar,package_clazz.dnode.xtype,Var.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
					}
					if (!isInterface() && isTypeUnerasable()) {
						init.params.append(new LVar(pos,nameTypeInfo,typeinfo_clazz.xtype,Var.PARAM_TYPEINFO,ACC_FINAL|ACC_SYNTHETIC));
					}
					if (isEnum()) {
						init.params.insert(0,new LVar(pos,"enum$name",Type.tpString,Var.PARAM_ENUM_NAME,ACC_SYNTHETIC));
						init.params.insert(1,new LVar(pos,"enum$ordinal",Type.tpInt,Var.PARAM_ENUM_ORD,ACC_SYNTHETIC));
					}
				}
				init.pos = pos;
				init.body = new Block(pos);
				if (isEnum() || isSingleton())
					init.setPrivate();
				else
					init.setPublic();
				addMethod(init);
			}
		}
	}

	public boolean preGenerate() {
		getStruct().checkResolved();
		if (isMembersPreGenerated() /*|| isLoadedFromBytecode()*/) return true;
		if (isPackage()) return false;
		setMembersPreGenerated(true);
		
		// first, pre-generate super-types
		foreach (CompaundMetaType sup; this.getAllSuperTypes())
			((Struct)sup.tdecl).preGenerate();

		if (isMixin())
			((Struct)this).meta.is_struct_interface = true;
		// generate typeinfo class, if needed
		autoGenerateTypeinfoClazz();
		// generate a class for interface non-abstract members
		autoGenerateIdefault(this);
		// fix super-types for java
		autoFixSuperTypes(this);
		// generate default constrctor if needed
		autoGenerateConstructor();
		
		if (this.isInterfaceOnly()) {
			foreach (Struct s; sub_decls)
				s.preGenerate();
			return false;
		}
		
		// build vtable
		List<Struct> processed = List.Nil;
		Vector<VTableEntry> vtable = new Vector<VTableEntry>();
		buildVTable(vtable, processed);
		if (Kiev.debug && Kiev.debugMultiMethod) {
			trace("vtable for "+this+":");
			foreach (VTableEntry vte; vtable) {
				trace("    "+vte.name+vte.etype);
				if (vte.overloader != null)
				trace("            overloaded by "+vte.overloader.name+vte.overloader.etype);
				foreach (Method m; vte.methods)
					trace("        "+m.ctx_tdecl+"."+m.sname+m.type);
			}
		}
		
		if (isClazz()) {
			// forward default implementation to interfaces
			autoCopyMixinMembers(this,vtable);
			// generate bridge methods
			foreach (VTableEntry vte; vtable)
				autoBridgeMethods(this,vte);
//			// generate method dispatchers for multimethods
//			foreach (VTableEntry vte; vtable; vte.overloader == null)
//				createMethodDispatchers(vte);
			// check unimplemented methods
			foreach (VTableEntry vte; vtable)
				checkUnimplementedMethod(this,vte);
		}
		
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
	
	public final void buildVTable(Vector<VTableEntry> vtable, List<Struct> processed) {
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
			//if (m.isMethodBridge())
			//	continue;
			CallType etype = m.etype;
			String name = m.sname;
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
				//if (m.isMethodBridge())
				//	continue;
				if (m.sname != vte.name || vte.methods.contains(m))
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

	private static void autoCopyMixinMembers(@forward RStruct self, Vector<VTableEntry> vtable) {
		// make master-copy context
		ASTNode.CopyContext cc = new ASTNode.CopyContext();
		Struct me = (Struct)self;
		Vector<DNode> to_copy = new Vector<DNode>();
		// copy non-abstract fields
		foreach (TypeRef tr; self.super_types) {
			Struct ss = tr.getStruct();
			if (ss == null || !ss.isInterface() || !ss.isMixin() || ss.iface_impl == me)
				continue;
			foreach (Field f; ss.members) {
				if !(f.isFinal() && f.isStatic())
					to_copy.append(f.ncopy(cc));
			}
		}
	next_entry:
	foreach (VTableEntry vte; vtable; vte.overloader == null) {
			// check we have a virtual method for this entry
			foreach (Method m; vte.methods) {
				if (m.ctx_tdecl.isInterface())
					continue;
				// found a virtual method, nothing to proxy here
				continue next_entry;
			}
			// all methods are from interfaces, check if we have a default implementation
			Method def = null;
			Struct def_iface = null;
			foreach (Method m; vte.methods; m.body != null) {
				// find default implementation class
				if !(m.ctx_tdecl instanceof Struct)
					continue;
				Struct mtd = (Struct)m.ctx_tdecl;
				if (!mtd.isInterface() || mtd.isAnnotation())
					continue;
				if (def == null) {
					def = m; // first method found
					def_iface = mtd;
				}
				else if (mtd.instanceOf(def_iface)) {
					def = m; // overriden default implementation
					def_iface = mtd;
				}
				else if (def_iface.instanceOf(mtd))
					; // just ignore
				else
					Kiev.reportWarning(me,"Umbigous default implementation for methods:\n"+
						"    "+def_iface+"."+def+"\n"+
						"    "+mtd+"."+m
					);
			}
			Method m = null;
			if (def == null) {
				// create an abstract method
				Method def = vte.methods.head();
				if (!me.isAbstract())
					Kiev.reportWarning(me,"Method "+vte.name+vte.etype+" is not implemented in "+me);
				//m = new MethodImpl(vte.name, vte.etype.ret(), ACC_ABSTRACT | ACC_PUBLIC | ACC_SYNTHETIC);
				//for (int i=0; i < vte.etype.arity; i++)
				//	m.params.append(new LVar(0,def.params[i].sname,vte.etype.arg(i),Var.PARAM_NORMAL,ACC_FINAL));
			} else {
				// create a proxy call
				m = def.ncopy(cc);
			}
			if (m != null) {
				to_copy.append(m);
				vte.add(m);
			}
		}
		cc.updateLinks();
		foreach (DNode dn; to_copy)
			me.members += dn;
		foreach (DNode dn; to_copy)
			Kiev.runProcessorsOn(dn);
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
			foreach (Method x; self.members; x.sname == m.sname) {
				if (x.etype ≈ vte.etype)
					continue next_m;
			}
			Method bridge = new MethodImpl(m.sname, vte.etype.ret(), ACC_BRIDGE | ACC_SYNTHETIC | mo.meta.mflags);
			for (int i=0; i < vte.etype.arity; i++)
				bridge.params.append(new LVar(mo.pos,m.params[i].sname,vte.etype.arg(i),Var.PARAM_NORMAL,ACC_FINAL));
			bridge.pos = mo.pos;
			((Struct)self).members.append(bridge);
			trace(Kiev.debug && Kiev.debugMultiMethod,"Created a bridge method "+self+"."+bridge+" for vtable entry "+vte.name+vte.etype);
			bridge.body = new Block();
			if (bridge.type.ret() ≢ Type.tpVoid)
				bridge.block.stats.append(new ReturnStat(mo.pos,makeDispatchCall(self,mo.pos, bridge, mo)));
			else
				bridge.block.stats.append(new ExprStat(mo.pos,makeDispatchCall(self,mo.pos, bridge, mo)));
			vte.add(bridge);
		}
	}
	
	private static void checkUnimplementedMethod(@forward RStruct self, VTableEntry vte) {
		if (self.isAbstract())
			return;
		foreach (Method m; vte.methods; !m.ctx_tdecl.isInterface()) {
			if (!m.isAbstract())
				return;
			break;
		}
		Kiev.reportWarning(self,"Method "+vte.name+vte.etype+" is not implemented in "+self);
	}

	private static Struct makeImpl(@forward RStruct self) {
		Struct defaults = Env.getRoot().newStruct(nameIFaceImpl,true,
			self.getStruct(),ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC | ACC_ABSTRACT | ACC_FORWARD,
			new JavaClass(), true, null
		);
		((Struct)self).members.add(defaults);
		if (self.isInterfaceOnly())
			defaults.meta.is_interface_only = true;
		iface_impl = defaults;
		// add the super-type
		defaults.super_types += new TypeRef(self.xtype);
		TypeRef tro = new TypeRef(StdTypes.tpObject);
		tro.setAutoGenerated(true);
		// add super-type for mixin
		if (self.super_types.length == 0) {
			defaults.super_types.insert(0, tro);
		} else {
			TypeRef sup_tr = self.super_types[0];
			for (int i=1; sup_tr.isAutoGenerated() && self.super_types.length > i; i++) {
				if (self.super_types[i].isAutoGenerated())
					continue;
				sup_tr = self.super_types[i];
				break;
			}
			Type st = sup_tr.getType();
			if (st.getStruct() == null) {
				defaults.super_types.insert(0, tro);
			}
			else if (st.getStruct().isClazz()) {
				defaults.super_types.insert(0, new TypeRef(st.getStruct().xtype));
			}
			else if (st.getStruct().isInterface() && st.getStruct().iface_impl != null) {
				defaults.super_types.insert(0, new TypeRef(st.getStruct().iface_impl.xtype));
			}
		}
		Kiev.runProcessorsOn(defaults);
		return defaults;
	}

	private static void autoFixSuperTypes(RStruct rself) {
		@forward Struct self = (Struct)rself;
		if (self.xtype ≈ StdTypes.tpObject) {
			if (self.super_types.length > 1 || self.super_types.length == 1 && self.super_types[0].getType() ≢ StdTypes.tpAny)
				Kiev.reportError(self,"Object must have no super-types");
			return;
		}
		if (self.isPackage() || self.isSyntax() || self.isStructView())
			return;
		// first super-type must be java class
		if (self.super_types.length == 0) {
			if (self.isAnnotation()) {
				TypeRef tr = new TypeRef(Type.tpObject);
				tr.setAutoGenerated(true);
				self.super_types.add(tr);
				tr = new TypeRef(Type.tpAnnotation);
				tr.setAutoGenerated(true);
				self.super_types.add(tr);
			}
			else if (self.isEnum()) {
				TypeRef tr = new TypeRef(Type.tpEnum);
				tr.setAutoGenerated(true);
				self.super_types.insert(0, tr);
			}
			else {
				TypeRef tr = new TypeRef(Type.tpObject);
				tr.setAutoGenerated(true);
				self.super_types.insert(0, tr);
			}
		}
		if (self.super_types.length >= 1) {
			TypeRef st = self.super_types[0];

			if (self.isAnnotation()) {
				assert (st.getType() ≈ StdTypes.tpObject);
				if (self.super_types.length == 1) {
					TypeRef tr = new TypeRef(Type.tpAnnotation);
					tr.setAutoGenerated(true);
					self.super_types.add(tr);
				}
				else if (self.super_types[1].getType() ≉ StdTypes.tpAnnotation) {
					Kiev.reportWarning(self,"Annotation must extend "+StdTypes.tpAnnotation);
					TypeRef tr = new TypeRef(Type.tpAnnotation);
					tr.setAutoGenerated(true);
					self.super_types.insert(1, tr);
				}
			}
			else if (self.isEnum()) {
				assert (st.getType() ≈ StdTypes.tpEnum);
			}

			Struct s = st.getStruct();
			if (s != null && self.isClazz()) {
				if (s.isInterface() && s.iface_impl != null) {
					TypeRef tr = new TypeRef(s.iface_impl.xtype);
					tr.setAutoGenerated(true);
					self.super_types.insert(0, tr);
				}
				else if (!s.isClazz()) {
					TypeRef tr = new TypeRef(Type.tpObject);
					tr.setAutoGenerated(true);
					self.super_types.insert(0, tr);
				}
			} else {
				if (s == null || !s.isClazz()) {
					TypeRef tr = new TypeRef(Type.tpObject);
					tr.setAutoGenerated(true);
					self.super_types.insert(0, tr);
				}
			}
		}
		if (self.super_types.length == 0) {
			Kiev.reportError(self, "The class must extend another class");
			return;
		}
		if (self.super_types[0].getStruct() == null || !self.super_types[0].getStruct().isClazz()) {
			Kiev.reportError(self, "The first super-class of "+self+" must be a java class, but found "+self.super_types[0]);
			return;
		}
		for (int i=1; i < self.super_types.length; i++) {
			if (self.super_types[i].getStruct() == null || !self.super_types[i].getStruct().isInterface()) {
				Kiev.reportError(self, "The non-first super-class of "+self+" must be a java interface, but found "+self.super_types[i]);
				return;
			}
		}
	}

	private static void autoGenerateIdefault(@forward RStruct self) {
		if (!isInterface() || isStructView() || isAnnotation())
			return;
		Struct defaults = iface_impl;
		if (defaults != null)
			return;
		Vector<DNode> to_copy = new Vector<DNode>();
		foreach (DNode dn; members) {
			if (dn instanceof Method) {
				Method m = (Method)dn;
				if (!m.isAbstract() || m.body != null) {
					if (m instanceof Constructor) {
						if (!m.isStatic())
							to_copy.append(m);
						continue;
					}
					to_copy.append(m);
				}
			}
			else if (self.isMixin() && dn instanceof Field) {
				if !(dn.isFinal() && dn.isStatic())
					to_copy.append(dn);
			}
		}
		// Make inner class name$_Impl_
		if (defaults == null && (isInterface() && isMixin() || to_copy.length > 0))
			defaults = makeImpl(self);
		if (defaults == null)
			return;
		ASTNode.CopyContext cc = new ASTNode.CopyContext();
		foreach (DNode dn; to_copy) {
			if (dn instanceof Constructor) {
				if (dn.isStatic())
					continue;
				defaults.members += dn.ncopy(cc);
			}
			else if (dn instanceof Method) {
				defaults.members += dn.ncopy(cc);
			}
			else if (dn instanceof Field) {
				defaults.members += dn.ncopy(cc);
			}
		}
		cc.updateLinks();
		Kiev.runProcessorsOn(defaults);
		defaults.preGenerate();
		boolean has_abstract_methods = false;
		foreach (Method m; defaults.members; m.isAbstract()) {
			has_abstract_methods = true;
			break;
		}
		if !(has_abstract_methods)
			defaults.setAbstract(false);
	}

	private static void combineMethods(@forward RStruct self) {
		List<Method> multimethods = List.Nil;
		for (int cur_m=0; cur_m < members.length; cur_m++) {
			if !(members[cur_m] instanceof Method)
				continue;
			Method m = (Method)members[cur_m];
			if (m instanceof Constructor)
				continue;
			if (m.isMethodBridge())
				continue;
			if( multimethods.contains(m) ) {
				trace(Kiev.debug && Kiev.debugMultiMethod,"Multimethod "+m+" already processed...");
				continue; // do not process method twice...
			}
			Method mmm;
			{
				// create dispatch method
				if (m.isRuleMethod())
					mmm = new RuleMethod(m.sname, m.meta.mflags | ACC_SYNTHETIC);
				else
					mmm = new MethodImpl(m.sname, m.type.ret(), m.meta.mflags | ACC_SYNTHETIC);
				mmm.setStatic(m.isStatic());
				mmm.targs.copyFrom(m.targs);
				foreach (Var fp; m.params) {
					TypeRef stype = fp.stype;
					if (stype != null)
						mmm.params.add(new LVar(fp.pos,fp.sname,stype.getType(),fp.kind,fp.meta.mflags));
					else
						mmm.params.add(new LVar(fp.pos,fp.sname,fp.type,fp.kind,fp.meta.mflags));
				}
				((Struct)self).members.add(mmm);
			}
			CallType type1 = mmm.type.getErasedType(); // erase type, like X... -> X[]
			CallType dtype1 = mmm.dtype;
			CallType etype1 = mmm.etype;
			mmm.detach();
			Method mm = null;
			trace(Kiev.debug && Kiev.debugMultiMethod,"Generating dispatch method for "+m+" with dispatch type "+etype1);
			// find all methods with the same java type
			ListBuffer<Method> mlistb = new ListBuffer<Method>();
			foreach (Method mj; members; !mj.isMethodBridge() && mj.isStatic() == m.isStatic()) {
				CallType type2 = mj.type.getErasedType(); // erase type, like X... -> X[]
				CallType dtype2 = mj.dtype;
				CallType etype2 = mj.etype;
				if( mj.sname != m.sname || etype2.arity != etype1.arity )
					continue;
				if (etype1.isMultimethodSuper(etype2)) {
					trace(Kiev.debug && Kiev.debugMultiMethod,"added dispatchable method "+mj);
					if (mm == null) {
						if (type1.equals(type2))
							mm = mj;
					} else {
						if (((CallType)mm.type.getErasedType()).greater(type2))
							mm = mj;
					}
					mlistb.append(mj);
				} else {
					trace(Kiev.debug && Kiev.debugMultiMethod,"methods "+mj+" with dispatch type "+etype2+" doesn't match...");
				}
			}
			Method overwr = null;

			if (super_types.length > 0)
				overwr = getOverwrittenMethod(super_types[0].getStruct(),self.xtype,m);

			// nothing to do, if no methods to combine
			if (mlistb.length() == 1 && mm != null) {
				// mm will have the base type - so, no super. call will be done
				trace(Kiev.debug && Kiev.debugMultiMethod,"no need to dispatch "+m);
				continue;
			}

			List<Method> mlist = mlistb.toList();

			if (mm == null) {
				// create a new dispatcher method...
				mm = mmm;
				self.getStruct().addMethod(mm);
				trace(Kiev.debug && Kiev.debugMultiMethod,"will add new dispatching method "+mm);
			} else {
				// if multimethod already assigned, thus, no super. call will be done - forget it
				trace(Kiev.debug && Kiev.debugMultiMethod,"will attach dispatching to this method "+mm);
				overwr = null;
			}

			// create mmtree
			MMTree mmt = new MMTree(mm);

			foreach (Method m; mlist; m != mm)
				mmt.add(m);

			trace(Kiev.debug && Kiev.debugMultiMethod,"Dispatch tree "+mm+" is:\n"+mmt);

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
				CallExpr ce = new CallExpr(0,new SuperExpr(),overwr,null,vae);
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

	private static Method getOverwrittenMethod(Struct clazz, Type base, Method m) {
		Method mm = null, mmret = null;
		if (!clazz.isInterface()) {
			foreach (TypeRef st; clazz.super_types; st.getStruct() != null) {
				mm = getOverwrittenMethod(st.getStruct(),base,m);
				if (mm != null)
					break;
			}
		}
		if( mmret == null && mm != null ) mmret = mm;
		trace(Kiev.debug && Kiev.debugMultiMethod,"lookup overwritten methods for "+base+"."+m+" in "+clazz);
		foreach (Method mi; clazz.members) {
			if( mi.isStatic() || mi.isPrivate() || mi instanceof Constructor ) continue;
			if( mi.sname != m.sname || mi.type.arity != m.type.arity ) {
//				trace(Kiev.debug && Kiev.debugMultiMethod,"Method "+m+" not matched by "+methods[i]+" in class "+this);
				continue;
			}
			CallType mit = (CallType)Type.getRealType(base,mi.etype);
			if( m.etype.equals(mit) ) {
				trace(Kiev.debug && Kiev.debugMultiMethod,"Method "+m+" overrides "+mi+" of type "+mit+" in class "+clazz);
				mm = mi;
				// Append constraints to m from mm
				foreach(WBCCondition cond; mm.conditions; m.conditions.indexOf(cond) < 0)
					m.conditions.add(cond);
				if( mmret == null && mm != null ) mmret = mm;
				break;
			} else {
				trace(Kiev.debug && Kiev.debugMultiMethod,"Method "+m+" of type "+m.etype+" does not overrides "+mi+" of type "+mit+" in class "+clazz);
			}
		}
		return mmret;
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
				if (t instanceof CompaundType && ((CompaundType)t).tdecl.isTypeUnerasable()) {
					if (t.getStruct().typeinfo_clazz == null)
						((RStruct)t.getStruct()).autoGenerateTypeinfoClazz();
					ENode tibe = new CallExpr(pos,
						accessTypeInfoField(mmt.m,t,false),
						Type.tpTypeInfo.tdecl.resolveMethod("$instanceof",Type.tpBoolean,Type.tpObject),
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
		if (!dispatched.isStatic() && !dispatcher.isStatic()) {
			if (self.getStruct() != dispatched.ctx_tdecl)
				obj = new SuperExpr(pos);
			else
				obj = new ThisExpr(pos);
		}
		CallExpr ce = new CallExpr(pos, obj, dispatched, null, ENode.emptyArray);
		if (self.getStruct() != dispatched.ctx_tdecl)
			ce.setSuperExpr(true);
		if (dispatched.isVirtualStatic() && !dispatcher.isStatic())
			ce.args.append(new ThisExpr(pos));
		foreach (Var fp; dispatcher.params)
			ce.args.append(new LVarExpr(0,fp));
		if!(dispatcher.etype.ret() ≥ dispatched.etype.ret())
			return new CastExpr(pos, dispatcher.etype.ret(), ce);
		return ce;
	}

	static class MMTree {
		static final MMTree[] emptyArray = new MMTree[0];
		Method m;
		MMTree[] uppers = MMTree.emptyArray;
		MMTree(Method m) { this.m = m; }
		void add(Method mm) {
			if( m!=null && !mm.type.greater(m.type) ) {
				trace(Kiev.debug && Kiev.debugMultiMethod,"method "+mm+" type <= "+m);
				if( m.type.isMultimethodSuper(mm.type) ) {
					trace(Kiev.debug && Kiev.debugMultiMethod,"method "+mm+" type == "+m);
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
					trace(Kiev.debug && Kiev.debugMultiMethod,"method "+mm+" type > "+m);
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
			trace(Kiev.debug && Kiev.debugMultiMethod,"method "+mm+" linked to "+link_to);
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

	static class AutoGenInfo {	
		Constructor class_init;
		Initializer instance_init;
		int static_pos = -1;
	}
	public void autoGenerateStatementsForDecl(DNode n, AutoGenInfo agi) {
		Initializer instance_init = null;
		if( n instanceof Field ) {
			Field f = (Field)n;
			if (f.init == null)
				return;
			if (f.isConstantExpr()) {
				ConstExpr ce = ConstExpr.fromConst(f.getConstValue());
				if (!ce.valueEquals(f.const_value))
					f.const_value = ce;
			}
			if (f.init.isConstantExpr() && f.isStatic())
				return;
			if (f.isAddedToInit())
				return;
			if( f.isStatic() ) {
				if( agi.class_init == null ) {
					agi.class_init = getClazzInitMethod();
					agi.static_pos = agi.class_init.block.stats.length-1;
					if (agi.static_pos < 0) {
						agi.static_pos = 0;
					} else {
						assert (agi.class_init.block.stats[agi.static_pos] instanceof ReturnStat);
					}
				}
				ENode init_stat = new ExprStat(f.init.pos,
						new AssignExpr(f.init.pos,
							f.isInitWrapper() ? Operator.Assign2 : Operator.Assign,
							new SFldExpr(f.pos,f),f.init.ncopy()
						)
					);
				agi.class_init.block.stats.insert(agi.static_pos++,init_stat);
				Kiev.runProcessorsOn(init_stat);
				RStruct.runResolveOn(init_stat);
			} else {
				if( agi.instance_init == null ) {
					agi.instance_init = new Initializer();
					agi.instance_init.pos = f.init.pos;
					agi.instance_init.body = new Block();
					((Struct)this).members.add(agi.instance_init);
				}
				ENode init_stat;
				init_stat = new ExprStat(f.init.pos,
						new AssignExpr(f.init.pos,
							f.isInitWrapper() ? Operator.Assign2 : Operator.Assign,
							new IFldExpr(f.pos,new ThisExpr(0),f),
							f.init.ncopy()
						)
					);
				agi.instance_init.block.stats.add(init_stat);
				init_stat.setAutoGenerated(true);
				Kiev.runProcessorsOn(init_stat);
				RStruct.runResolveOn(init_stat);
			}
			f.setAddedToInit(true);
		} else {
			Initializer init = (Initializer)n;
			ENode init_stat = new Shadow(init);
			init_stat.setAutoGenerated(true);
			if (init.isStatic()) {
				if( agi.class_init == null ) {
					agi.class_init = getClazzInitMethod();
					agi.static_pos = agi.class_init.block.stats.length-1;
					if (agi.static_pos < 0) {
						agi.static_pos = 0;
					} else {
						assert (agi.class_init.block.stats[agi.static_pos] instanceof ReturnStat);
					}
				}
				agi.class_init.block.stats.insert(agi.static_pos++,init_stat);
			} else {
				if( agi.instance_init == null ) {
					agi.instance_init = new Initializer();
					agi.instance_init.pos = init.pos;
					agi.instance_init.body = new Block();
					((Struct)this).members.add(agi.instance_init);
				}
				agi.instance_init.block.stats.add(init_stat);
				Kiev.runProcessorsOn(init_stat);
				RStruct.runResolveOn(init_stat);
			}
		}
	}

	public void autoGenerateStatements() {

		if( Kiev.debug ) System.out.println("AutoGenerating statements for "+this);
		// <clinit> & common$init, if need
		AutoGenInfo agi = new AutoGenInfo();

		foreach (ASTNode n; members) {
			if (n == agi.instance_init)
				continue;
			if (n instanceof Field) {
				autoGenerateStatementsForDecl((Field)n, agi);
			}
			else if (n instanceof Initializer) {
				autoGenerateStatementsForDecl((Initializer)n, agi);
			}
		}

		// Generate super(...) constructor calls, if they are not
		// specified as first statements of a constructor
		if (((Struct)this) != Type.tpObject.tdecl) {
			foreach (Constructor m; members) {
				if( m.isStatic() ) continue;

				Block initbody = m.body;
				
				if (initbody == null)
					continue; // API class loaded?

				CtorCallExpr ctor_call = null;
				if (initbody.stats.length != 0) {
					if (initbody.stats[0] instanceof ExprStat) {
						ExprStat es = (ExprStat)initbody.stats[0];
						if (es.expr instanceof CtorCallExpr)
							ctor_call = (CtorCallExpr)es.expr;
					}
					else if (initbody.stats[0] instanceof CtorCallExpr) {
						ctor_call = (CtorCallExpr)initbody.stats[0];
					}
					if (ctor_call != null) {
						if (ctor_call.obj instanceof ThisExpr) {
							//ctor_call = (CtorCallExpr)ce; // this(args)
						}
						else if (ctor_call.obj instanceof SuperExpr) {
							//ctor_call = (CtorCallExpr)ce; // super(args)
							m.setNeedFieldInits(true);
						}
						else
							throw new CompilerException(ctor_call,"Expected one of this() or super() constructor call");
					}
				}
				if (ctor_call == null) {
					m.setNeedFieldInits(true);
					ctor_call = new CtorCallExpr(pos, new SuperExpr(), ENode.emptyArray);
					if( super_types.length > 0 && super_types[0].getStruct() == Type.tpClosureClazz ) {
						EToken max_args = new EToken(pos,nameClosureMaxArgs,ETokenKind.IDENTIFIER,true);
						ctor_call.args.add(max_args);
					}
					else if (isAnonymouse()) {
						int skip_args = 0;
						if( isStructInner() && !isStatic() ) skip_args++;
						if( this.isTypeUnerasable() && super_types[0].getStruct().isTypeUnerasable() ) skip_args++;
						for(int i=skip_args; i < m.params.length; i++) {
							ctor_call.args.append( new LVarExpr(m.pos,m.params[i]));
						}
					}
					else if (isEnum()) {
						if (ctor_call.obj instanceof ThisExpr) {
							ctor_call.eargs.insert(0,new EToken(pos, "enum$name", ETokenKind.IDENTIFIER,true));
							ctor_call.eargs.insert(1,new EToken(pos, "enum$ordinal", ETokenKind.IDENTIFIER,true));
						} else {
							ctor_call.args.insert(0,new EToken(pos, "enum$name", ETokenKind.IDENTIFIER,true));
							ctor_call.args.insert(1,new EToken(pos, "enum$ordinal", ETokenKind.IDENTIFIER,true));
						}
					}
					initbody.stats.insert(0,new ExprStat(ctor_call));
					Kiev.runProcessorsOn(initbody.stats[0]);
					RStruct.runResolveOn(initbody.stats[0]);
				} else {
					if (isEnum()) {
						if (ctor_call.obj instanceof ThisExpr) {
							ctor_call.eargs.insert(0,new EToken(pos, "enum$name", ETokenKind.IDENTIFIER,true));
							ctor_call.eargs.insert(1,new EToken(pos, "enum$ordinal", ETokenKind.IDENTIFIER,true));
						} else {
							ctor_call.args.insert(0,new EToken(pos, "enum$name", ETokenKind.IDENTIFIER,true));
							ctor_call.args.insert(1,new EToken(pos, "enum$ordinal", ETokenKind.IDENTIFIER,true));
						}
						Kiev.runProcessorsOn(ctor_call);
						RStruct.runResolveOn(ctor_call);
					}
				}
				int p = 1;
				if (isStructInner() && !isStatic()) {
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
				if (isTypeUnerasable() && m.isNeedFieldInits()) {
					Field tif = resolveField(nameTypeInfo);
					Var v = m.getTypeInfoParam(Var.PARAM_TYPEINFO);
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
				while (p < initbody.stats.length) {
					if (initbody.stats[p] instanceof ExprStat) {
						ExprStat es = (ExprStat)initbody.stats[p];
						if (es.expr instanceof AssignExpr) {
							AssignExpr ae = (AssignExpr)es.expr;
							if (ae.lval instanceof IFldExpr) {
								IFldExpr fe = (IFldExpr)ae.lval;
								if (fe.obj instanceof ThisExpr && fe.var.isFinal()) {
									p++;
									continue;
								}
							}
						}
					}
					break;
				}
				if( agi.instance_init != null && m.isNeedFieldInits() ) {
					initbody.stats.insert(p,agi.instance_init.body.ncopy());
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
		else if (node instanceof SNode)
			node.resolveDecl();
	}
	
	public void resolveDecl() {
		if( isResolved() ) return;
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
			foreach(Field f; this.members) {
				try {
					f.type.checkResolved();
					if (f.type.getStruct()!=null)
						MetaAccess.verifyReadWrite((Struct)this,f.type.getStruct());
				} catch(Exception e ) { Kiev.reportError(f,e); }
			}
			foreach(Method m; members) {
				try {
					m.type.ret().checkResolved();
					if (m.type.ret().getStruct()!=null)
						MetaAccess.verifyReadWrite((Struct)this,m.type.ret().getStruct());
					foreach(Type t; m.type.params()) {
						t.checkResolved();
						if (t.getStruct()!=null)
							MetaAccess.verifyReadWrite((Struct)this,t.getStruct());
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
				if (n instanceof Method)
					n.resolveDecl();
				else if (n instanceof Initializer)
					n.resolveDecl();
			}
			autoGenerateStatements();
			foreach (Constructor c; members; !c.isResolved())
				c.resolveDecl();
			
			// Autogenerate hidden args for initializers of local class
			if( isLocal() ) {
				Field[] proxy_fields = Field.emptyArray;
				foreach(Field f; this.members) {
					if( f.isNeedProxy() )
						proxy_fields = (Field[])Arrays.append(proxy_fields,f);
				}
				if( proxy_fields.length > 0 ) {
					foreach(Constructor m; members; !m.isStatic()) {
						for(int j=0; j < proxy_fields.length; j++) {
							int par = m.params.length;
							String nm = proxy_fields[j].sname;
							m.params.append(new LVar(m.pos,nm,proxy_fields[j].type,Var.PARAM_LVAR_PROXY,ACC_FINAL|ACC_SYNTHETIC));
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
		setResolved(true);
		//diff_time = System.currentTimeMillis() - curr_time;
		//if( Kiev.verbose ) Kiev.reportInfo("Resolved class "+this,diff_time);
	}
	
}

