package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.Struct.StructImpl;
import kiev.vlang.Struct.StructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RStruct of StructImpl extends StructView {
	
	public ENode accessTypeInfoField(ASTNode from, Type t, boolean from_gen) {
		while (t instanceof WrapperType)
			t = ((WrapperType)t).getUnwrappedType();
		Method ctx_method = from.ctx_method;
		if (t.isUnerasable()) {
			if (ctx_method != null && ctx_method.isTypeUnerasable() && t instanceof ArgType) {
				NArr<TypeDef> targs = ctx_method.targs;
				for (int i=0; i < targs.length; i++) {
					TypeDef td = targs[i];
					if (td.getAType() == t) {
						return new LVarExpr(from.pos, ctx_method.getTypeInfoParam(FormPar.PARAM_TYPEINFO_N+i));
					}
				}
			}
			if (this.instanceOf(Type.tpTypeInfo.clazz) && ctx_method != null && ctx_method.name.name == nameInit) {
				if (t instanceof ArgType)
					return new ASTIdentifier(from.pos,t.name);
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
				if (this.ctype ≈ t)
					return ti_access;
	
				if (t.isArgument()) {
					// Get corresponded type argument
					ArgType at = (ArgType)t;
					KString fnm = new KStringBuffer(nameTypeInfo.len+1+at.name.len)
							.append(nameTypeInfo).append('$').append(at.name).toKString();
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
		if ((from.ctx_method == null || from.ctx_method.name.name == nameClassInit) && from.ctx_clazz.isInterface()) {
			return new TypeInfoExpr(from.pos, new TypeRef(t));
		}
		
		// Lookup and create if need as $typeinfo$N
		foreach(DNode n; members; n instanceof Field && n.isStatic()) {
			Field f = (Field)n;
			if (f.init == null || !f.name.name.startsWith(nameTypeInfo) || f.name.name.equals(nameTypeInfo))
				continue;
			if (((TypeInfoExpr)f.init).type.getType() ≈ t)
				return new SFldExpr(from.pos,f);
		}
		TypeInfoExpr ti_expr = new TypeInfoExpr(pos, new TypeRef(t));
		// check we can use a static field
		NopExpr nop = new NopExpr(ti_expr);
		from.addNodeData(nop);
		nop.resolve(null);
		ti_expr.detach();
		from.delNodeData(NopExpr.ID);
		foreach (ENode ti_arg; ti_expr.cl_args; !(ti_arg instanceof SFldExpr)) {
			// oops, cannot make it a static field
			return ti_expr;
		}
		if (from_gen)
			throw new RuntimeException("Ungenerated typeinfo for type "+t+" ("+t.getClass()+")");
		int i = 0;
		foreach(DNode n; members; n instanceof Field && n.isStatic()) {
			Field f = (Field)n;
			if (f.init == null || !f.name.name.startsWith(nameTypeInfo) || f.name.name.equals(nameTypeInfo))
				continue;
			i++;
		}
		Field f = new Field(KString.from(nameTypeInfo+"$"+i),ti_expr.getType(),ACC_STATIC|ACC_FINAL); // package-private for inner classes
		f.init = ti_expr;
		getStruct().addField(f);
		f.resolveDecl();
		// Add initialization in <clinit>
		Constructor class_init = getStruct().getClazzInitMethod();
		if( ctx_method != null && ctx_method.name.equals(nameClassInit) ) {
			class_init.addstats.append(
				new ExprStat(f.init.pos,
					new AssignExpr(f.init.pos,AssignOperator.Assign
						,new SFldExpr(f.pos,f),new Shadow(f.init))
				)
			);
		} else {
			class_init.addstats.append(
				new ExprStat(f.init.pos,
					new AssignExpr(f.init.pos,AssignOperator.Assign
						,new SFldExpr(f.pos,f),new Shadow(f.init))
				)
			);
		}
		f.setAddedToInit(true);
		ENode e = new SFldExpr(from.pos,f);
		return e;
//		System.out.println("Field "+f+" of type "+f.init+" added");
	}

	public List<ArgType> getTypeInfoArgs() {
		ListBuffer<ArgType> lb = new ListBuffer<ArgType>();
		TVar[] templ = this.imeta_type.getTemplBindings().tvars;
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
		flags |= ACC_PUBLIC | ACC_STATIC;
		typeinfo_clazz = Env.newStruct(
			ClazzName.fromOuterAndName(this.getStruct(),nameClTypeInfo,false,true),this.getStruct(),flags,true
			);
		members.add(typeinfo_clazz);
		typeinfo_clazz.setPublic();
		typeinfo_clazz.setResolved(true);
		if (super_type != null && ((Struct)super_type.clazz).typeinfo_clazz != null)
			typeinfo_clazz.super_type = ((Struct)super_type.clazz).typeinfo_clazz.ctype;
		else
			typeinfo_clazz.super_type = Type.tpTypeInfo;
		getStruct().addSubStruct(typeinfo_clazz);
		typeinfo_clazz.pos = pos;

		// create constructor method
		{
			Constructor init = new Constructor(ACC_PROTECTED);
			init.body = new Block(pos);
			init.params.add(new FormPar(pos,KString.from("hash"),Type.tpInt,FormPar.PARAM_NORMAL,ACC_FINAL));
			init.params.add(new FormPar(pos,KString.from("clazz"),Type.tpClass,FormPar.PARAM_NORMAL,ACC_FINAL));
			// add in it arguments fields, and prepare for constructor
			foreach (ArgType at; this.getTypeInfoArgs()) {
				KString fname = KString.from(nameTypeInfo+"$"+at.name);
				Field f = new Field(fname,Type.tpTypeInfo,ACC_PUBLIC|ACC_FINAL);
				typeinfo_clazz.addField(f);
				FormPar v = new FormPar(pos,at.name,Type.tpTypeInfo,FormPar.PARAM_NORMAL,ACC_FINAL);
				init.params.append(v);
				init.body.stats.append(new ExprStat(pos,
					new AssignExpr(pos,AssignOperator.Assign,
						new IFldExpr(pos,new ThisExpr(pos),f),
						new LVarExpr(pos,v)
					)
				));
			}
	
			// create typeinfo field
			Field tif = getStruct().addField(new Field(nameTypeInfo,typeinfo_clazz.ctype,ACC_PUBLIC|ACC_FINAL));
			// add constructor to the class
			typeinfo_clazz.addMethod(init);
			
			// and add super-constructor call
			init.setNeedFieldInits(true);
			ASTCallExpression call_super = new ASTCallExpression(pos, nameSuper, ENode.emptyArray);
			call_super.args.add(new LVarExpr(pos,init.params[0]));
			call_super.args.add(new LVarExpr(pos,init.params[1]));
			init.body.stats.insert(new ExprStat(call_super),0);
			foreach (ArgType at; super_type.clazz.getRView().getTypeInfoArgs()) {
				Type t = at.applay(this.ctype);
				ENode expr;
				if (t instanceof ArgType)
					expr = new ASTIdentifier(pos,t.name);
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
			Method init = new Method(KString.from("newTypeInfo"), typeinfo_clazz.ctype, ACC_STATIC|ACC_PUBLIC);
			init.params.add(new FormPar(pos,KString.from("clazz"),Type.tpClass,FormPar.PARAM_NORMAL,ACC_FINAL));
			init.params.add(new FormPar(pos,KString.from("args"),new ArrayType(Type.tpTypeInfo),FormPar.PARAM_NORMAL,ACC_FINAL));
			init.body = new Block(pos);
			Var h = new Var(pos,KString.from("hash"),Type.tpInt,ACC_FINAL);
			Var v = new Var(pos,KString.from("ti"),typeinfo_clazz.ctype,0);
			Method mhash = Type.tpTypeInfo.clazz.resolveMethod(KString.from("hashCode"),Type.tpInt,Type.tpClass,new ArrayType(Type.tpTypeInfo));
			h.init = new CallExpr(pos,null,mhash,new ENode[]{
				new LVarExpr(pos,init.params[0]),
				new LVarExpr(pos,init.params[1])
			});
			init.body.addSymbol(h);
			Method mget = Type.tpTypeInfo.clazz.resolveMethod(KString.from("get"),Type.tpTypeInfo,Type.tpInt,Type.tpClass,new ArrayType(Type.tpTypeInfo));
			v.init = new CallExpr(pos,null,mget,new ENode[]{
				new LVarExpr(pos,h),
				new LVarExpr(pos,init.params[0]),
				new LVarExpr(pos,init.params[1])
			});
			init.body.addSymbol(v);
			NewExpr ne = new NewExpr(pos,typeinfo_clazz.ctype,
				new ENode[]{
					new LVarExpr(pos,h),
					new LVarExpr(pos,init.params[0])
				});
			int i = 0;
			foreach (ArgType at; this.getTypeInfoArgs())
				ne.args.add(new ContainerAccessExpr(pos, new LVarExpr(pos,init.params[1]), new ConstIntExpr(i++)));
			init.body.stats.add(new IfElseStat(pos,
				new BinaryBoolExpr(pos,BinaryOperator.Equals,new LVarExpr(pos,v),new ConstNullExpr()),
				new ExprStat(pos,new AssignExpr(pos, AssignOperator.Assign,new LVarExpr(pos,v),ne)),
				null
			));
			init.body.stats.add(new ReturnStat(pos,new LVarExpr(pos,v)));
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
			Method meq = new Method(KString.from("eq"), Type.tpBoolean, ACC_PUBLIC);
			meq.params.add(new FormPar(pos,KString.from("clazz"),Type.tpClass,FormPar.PARAM_NORMAL,ACC_FINAL));
			meq.params.add(new FormPar(pos,KString.from("args"),new ArrayType(Type.tpTypeInfo),FormPar.PARAM_VARARGS,ACC_FINAL));
			typeinfo_clazz.addMethod(meq);
			meq.body = new Block(pos);
			meq.body.stats.add(new IfElseStat(pos,
				new BinaryBoolExpr(pos,BinaryOperator.NotEquals,
					new IFldExpr(pos,new ThisExpr(pos), typeinfo_clazz.resolveField(KString.from("clazz"))),
					new LVarExpr(pos,meq.params[0])
					),
				new ReturnStat(pos,new ConstBoolExpr(false)),
				null
			));
			int idx = 0;
			foreach (ArgType at; this.getTypeInfoArgs()) {
				Field f = typeinfo_clazz.resolveField(KString.from(nameTypeInfo+"$"+at.name));
				meq.body.stats.add(new IfElseStat(pos,
					new BinaryBoolExpr(pos,BinaryOperator.NotEquals,
						new IFldExpr(pos,new ThisExpr(pos), f),
						new ContainerAccessExpr(pos, new LVarExpr(pos,meq.params[1]), new ConstIntExpr(idx))
						),
					new ReturnStat(pos,new ConstBoolExpr(false)),
					null
				));
				idx++;
			}
			meq.body.stats.add(new ReturnStat(pos,new ConstBoolExpr(true)));
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
			Method misa = new Method(KString.from("$assignableFrom"), Type.tpBoolean, ACC_PUBLIC);
			misa.params.add(new FormPar(pos,KString.from("ti"),Type.tpTypeInfo,FormPar.PARAM_NORMAL,ACC_FINAL));
			typeinfo_clazz.addMethod(misa);
			misa.body = new Block(pos);
			misa.body.stats.add(new IfElseStat(pos,
				new BooleanNotExpr(pos,
					new CallExpr(pos,
						new IFldExpr(pos,new ThisExpr(), typeinfo_clazz.resolveField(KString.from("clazz"))),
						Type.tpClass.clazz.resolveMethod(KString.from("isAssignableFrom"),Type.tpBoolean,Type.tpClass),
						new ENode[]{
							new IFldExpr(pos,new LVarExpr(pos,misa.params[0]), typeinfo_clazz.resolveField(KString.from("clazz")))
						}
					)
				),
				new ReturnStat(pos,new ConstBoolExpr(false)),
				null
			));
			misa.body.stats.add(new ExprStat(pos,
				new AssignExpr(pos,AssignOperator.Assign,
					new LVarExpr(pos,misa.params[0]),
					new CastExpr(pos,typeinfo_clazz.ctype,new LVarExpr(pos,misa.params[0]))
				)
			));
			foreach (ArgType at; this.getTypeInfoArgs()) {
				Field f = typeinfo_clazz.resolveField(KString.from(nameTypeInfo+"$"+at.name));
				misa.body.stats.add(new IfElseStat(pos,
					new BooleanNotExpr(pos,
						new CallExpr(pos,
							new IFldExpr(pos,new ThisExpr(), f),
							Type.tpTypeInfo.clazz.resolveMethod(KString.from("$assignableFrom"),Type.tpBoolean,Type.tpTypeInfo),
							new ENode[]{
								new IFldExpr(pos,new LVarExpr(pos,misa.params[0]), f)
							}
						)
					),
					new ReturnStat(pos,new ConstBoolExpr(false)),
					null
				));
			}
			misa.body.stats.add(new ReturnStat(pos,new ConstBoolExpr(true)));
		}
		// create $instanceof function
		// public boolean $instanceof(Object obj) {
		// 	if (obj == null ) return false;
		// 	if!(this.clazz.isInstance(obj)) return false;
		// 	return this.$assignableFrom(((Outer)obj).$typeinfo));
		// }
		{
			Method misa = new Method(KString.from("$instanceof"), Type.tpBoolean, ACC_PUBLIC);
			misa.params.add(new FormPar(pos,KString.from("obj"),Type.tpObject,FormPar.PARAM_NORMAL,ACC_FINAL));
			typeinfo_clazz.addMethod(misa);
			misa.body = new Block(pos);
			misa.body.stats.add(new IfElseStat(pos,
				new BinaryBoolExpr(pos,BinaryOperator.Equals,
					new LVarExpr(pos,misa.params[0]),
					new ConstNullExpr()
					),
				new ReturnStat(pos,new ConstBoolExpr(false)),
				null
			));
			misa.body.stats.add(new IfElseStat(pos,
				new BooleanNotExpr(pos,
					new CallExpr(pos,
						new IFldExpr(pos,new ThisExpr(pos), typeinfo_clazz.resolveField(KString.from("clazz"))),
						Type.tpClass.clazz.resolveMethod(KString.from("isInstance"),Type.tpBoolean,Type.tpObject),
						new ENode[]{new LVarExpr(pos,misa.params[0])}
						)
					),
				new ReturnStat(pos,new ConstBoolExpr(false)),
				null
			));
			misa.body.stats.add(new ReturnStat(pos,
				new CallExpr(pos,
					new ThisExpr(),
					typeinfo_clazz.resolveMethod(KString.from("$assignableFrom"),Type.tpBoolean,Type.tpTypeInfo),
					new ENode[]{
						new IFldExpr(pos,
							new CastExpr(pos,this.ctype,new LVarExpr(pos,misa.params[0])),
							this.resolveField(nameTypeInfo)
						)
					}
				)
			));
		}
	}
	
	public boolean preGenerate() {
		getStruct().checkResolved();
		if( isMembersPreGenerated() ) return true;
		if( isPackage() ) return false;
		
		// first, pre-generate super-types
		foreach (TypeProvider sup; this.getAllSuperTypes(); sup instanceof CompaundTypeProvider)
			sup.clazz.preGenerate();

		// generate typeinfo class, if needed
		autoGenerateTypeinfoClazz();
		// generate a class for interface non-abstract members
		autoGenerateIdefault();
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
					trace("        "+m.ctx_clazz+"."+m.name.name+m.type);
			}
		}
		
		if (isClazz()) {
			// forward default implementation to interfaces
			foreach (VTableEntry vte; vtable; vte.overloader == null)
				autoProxyMixinMethods(vte);
			// generate bridge methods
			foreach (VTableEntry vte; vtable)
				autoBridgeMethods(vte);
//			// generate method dispatchers for multimethods
//			foreach (VTableEntry vte; vtable; vte.overloader == null)
//				createMethodDispatchers(vte);
		}
		
		
		setMembersPreGenerated(true);

		combineMethods();

		return true;
	}

	static final class VTableEntry {
		KString      name;
		CallType     etype;
		protected:no,ro,ro,rw
		List<Method> methods = List.Nil;
		VTableEntry  overloader;
		VTableEntry(KString name, CallType etype) {
			this.name = name;
			this.etype = etype;
		}
		public void add(Method m) {
			assert (!methods.contains(m));
			methods = new List.Cons<Method>(m, methods);
		}
	}
	
	private void buildVTable(Vector<VTableEntry> vtable, List<Struct> processed) {
		if (processed.contains(this.getStruct()))
			return;
		processed = new List.Cons<Struct>(this.getStruct(), processed);
		// take vtable from super-types
		if (super_bound.getType() != null) {
			super_bound.getType().getStruct().getRView().buildVTable(vtable, processed);
			foreach (TypeRef sup; interfaces)
				sup.getType().getStruct().getRView().buildVTable(vtable, processed);
		}
		
		// process override
		foreach (DNode n; members; n instanceof Method && !(n instanceof Constructor)) {
			Method m = (Method)n;
			if (m.isStatic() && !m.isVirtualStatic())
				continue;
			if (m.isMethodBridge())
				continue;
			CallType etype = m.etype;
			KString name = m.name.name;
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
			foreach (DNode n; members; n instanceof Method && !(n instanceof Constructor)) {
				Method m = (Method)n;
				if (m.isStatic() && !m.isVirtualStatic())
					continue;
				if (m.isMethodBridge())
					continue;
				if (m.name.name != vte.name || vte.methods.contains(m))
					continue;
				CallType mt = m.etype.toCallTypeRetAny();
				if (mt ≈ et)
					vte.add(m);
			}
			if (!this.isInterface()) {
				foreach (VTableEntry vte2; vtable; vte2 != vte && vte2.name == vte.name) {
					foreach (Method m; vte2.methods; !vte.methods.contains(m)) {
						CallType mt = m.dtype.toCallTypeRetAny().applay(this.ctype);
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
				if (m2.ctx_clazz.instanceOf(m1.ctx_clazz))
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
			if (m.ctx_clazz == this.getStruct()) {
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
			if (m.ctx_clazz == this && m.type.applay(this.ctype) ≈ root.type.applay(this.ctype)) {
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
			if (m.ctx_clazz == this && !m.isVirtualStatic()) {
				m.setVirtualStatic(true);
				if (m.name.name == vte.name) {
					KString name = m.name.name;
					m.name.name = KString.from(name+"$mm$"+tmp);
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
			root.body.stats.insert(0, st);
	}
*/
	private void autoProxyMixinMethods(VTableEntry vte) {
		// check we have a virtual method for this entry
		foreach (Method m; vte.methods) {
			if (m.ctx_clazz.isInterface())
				continue;
			// found a virtual method, nothing to proxy here
			return;
		}
		// all methods are from interfaces, check if we have a default implementation
		Method def = null;
		foreach (Method m; vte.methods) {
			// find default implementation class
			Struct i = null;
			foreach (DNode n; m.ctx_clazz.members; n instanceof Struct && n.name.short_name == nameIdefault) {
				i = n;
				break;
			}
			if (i == null)
				continue;
			Method fnd = null;
			Type[] params = m.type.params();
			params = (Type[])Arrays.insert(params,m.ctx_clazz.ctype,0);
			CallType mt = new CallType(params, m.type.ret());
			foreach (Method dm; i.members; dm instanceof Method && dm.name.name == m.name.name && dm.type ≈ mt) {
				fnd = dm;
				break;
			}
			if (def == null)
				def = fnd; // first method found
			else if (fnd.ctx_clazz.instanceOf(def.ctx_clazz))
				def = fnd; // overriden default implementation
			else if (def.ctx_clazz.instanceOf(fnd.ctx_clazz))
				; // just ignore
			else
				Kiev.reportWarning(this,"Umbigous default implementation for methods:\n"+
					"    "+def.ctx_clazz+"."+def+"\n"+
					"    "+fnd.ctx_clazz+"."+fnd
				);
		}
		Method m = null;
		if (def == null) {
			// create an abstract method
			Method def = vte.methods.head();
			if (!this.isAbstract())
				Kiev.reportWarning(this,"Method "+vte.name+vte.etype+" is not implemented in "+this);
			m = new Method(vte.name, vte.etype.ret(), ACC_ABSTRACT | ACC_PUBLIC | ACC_SYNTHETIC);
			for (int i=0; i < vte.etype.arity; i++)
				m.params.append(new FormPar(0,def.params[i].name.name,vte.etype.arg(i),FormPar.PARAM_NORMAL,ACC_FINAL));
			members.append(m);
		} else {
			// create a proxy call
			m = new Method(vte.name, vte.etype.ret(), ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC);
			for (int i=0; i < vte.etype.arity; i++)
				m.params.append(new FormPar(0,KString.from("arg$"+i),vte.etype.arg(i),FormPar.PARAM_NORMAL,ACC_FINAL));
			members.append(m);
			m.body = new Block();
			if( m.type.ret() ≡ Type.tpVoid )
				m.body.stats.add(new ExprStat(0,makeDispatchCall(0, m, def)));
			else
				m.body.stats.add(new ReturnStat(0,makeDispatchCall(0, m, def)));
		}
		vte.add(m);
	}

	private void autoBridgeMethods(VTableEntry vte) {
		// get overloader vtable entry
		VTableEntry ovr = vte;
		while (ovr.overloader != null)
			ovr = ovr.overloader;
		// find overloader method
		Method mo = null;
		foreach (Method m; vte.methods) {
			if (m.ctx_clazz == this.getStruct() && m.etype ≈ ovr.etype) {
				mo = m;
				break;
			}
		}
		if (mo == null)
			return; // not overloaded in this class
	next_m:
		foreach (Method m; vte.methods; m.ctx_clazz != this.getStruct()) {
			// check this class have no such a method
			foreach (DNode x; this.members; x instanceof Method && x.name.name == m.name.name) {
				if (x.etype ≈ vte.etype)
					continue next_m;
			}
			Method bridge = new Method(m.name.name, vte.etype.ret(), ACC_BRIDGE | ACC_SYNTHETIC | mo.flags);
			for (int i=0; i < vte.etype.arity; i++)
				bridge.params.append(new FormPar(mo.pos,m.params[i].name.name,vte.etype.arg(i),FormPar.PARAM_NORMAL,ACC_FINAL));
			bridge.pos = mo.pos;
			members.append(bridge);
			trace(Kiev.debugMultiMethod,"Created a bridge method "+this+"."+bridge+" for vtable entry "+vte.name+vte.etype);
			bridge.body = new Block();
			if (bridge.type.ret() ≢ Type.tpVoid)
				bridge.body.stats.append(new ReturnStat(mo.pos,makeDispatchCall(mo.pos, bridge, mo)));
			else
				bridge.body.stats.append(new ExprStat(mo.pos,makeDispatchCall(mo.pos, bridge, mo)));
			vte.add(bridge);
		}
	}

	private void autoGenerateIdefault() {
		if (!isInterface())
			return;
		Struct defaults = null;
		foreach (DNode n; members; n instanceof Method) {
			Method m = (Method)n;
			if (!m.isAbstract()) {
				if (m instanceof Constructor) continue; // ignore <clinit>

				// Make inner class name$default
				if( defaults == null ) {
					defaults = Env.newStruct(
						ClazzName.fromOuterAndName(this.getStruct(),nameIdefault,false,true),
						this.getStruct(),ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT, true
					);
					members.add(defaults);
					defaults.setResolved(true);
					Kiev.runProcessorsOn(defaults);
				}
				
				if (m.isStatic()) {
					defaults.members.add(~m);
					continue;
				}

				// Now, non-static methods (templates)
				// Make it static and add abstract method
				Method def = new Method(m.name.name,m.type.ret(),m.getFlags()|ACC_STATIC);
				def.pos = m.pos;
				def.params.moveFrom(m.params); // move, because the vars are resolved
				m.params.copyFrom(def.params);
				def.params.insert(0,new FormPar(pos,Constants.nameThis,this.ctype,FormPar.PARAM_NORMAL,ACC_FINAL|ACC_FORWARD));
				defaults.members.add(def);
				def.body = ~m.body;
				def.setVirtualStatic(true);

				m.setAbstract(true);
			}
		}
	}

	private void combineMethods() {
		List<Method> multimethods = List.Nil;
		for (int cur_m=0; cur_m < members.length; cur_m++) {
			if !(members[cur_m] instanceof Method)
				continue;
			Method m = (Method)members[cur_m];
			if (m.name.equals(nameClassInit) || m.name.equals(nameInit))
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
					mmm = new RuleMethod(m.name.name, m.flags | ACC_SYNTHETIC);
				else
					mmm = new Method(m.name.name, m.type.ret(), m.flags | ACC_SYNTHETIC);
				mmm.setStatic(m.isStatic());
				mmm.name.aliases = m.name.aliases;
				mmm.targs.copyFrom(m.targs);
				foreach (FormPar fp; m.params)
					mmm.params.add(new FormPar(fp.pos,fp.name.name,fp.stype.getType(),fp.kind,fp.flags));
				this.members.add(mmm);
			}
			CallType type1 = mmm.type;
			CallType dtype1 = mmm.dtype;
			CallType etype1 = mmm.etype;
			this.members.detach(mmm);
			Method mm = null;
			trace(Kiev.debugMultiMethod,"Generating dispatch method for "+m+" with dispatch type "+etype1);
			// find all methods with the same java type
			ListBuffer<Method> mlistb = new ListBuffer<Method>();
			foreach (DNode nj; members; nj instanceof Method && !nj.isMethodBridge() && nj.isStatic() == m.isStatic()) {
				Method mj = (Method)nj;
				CallType type2 = mj.type;
				CallType dtype2 = mj.dtype;
				CallType etype2 = mj.etype;
				if( mj.name.name != m.name.name || etype2.arity != etype1.arity )
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

			if (super_type != null )
				overwr = super_type.clazz.getOverwrittenMethod(this.ctype,m);

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
				this.getStruct().addMethod(mm);
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
			st = makeDispatchStat(mm,mmt);

			if (overwr != null) {
				IfElseStat last_st = st;
				ENode br;
				while (last_st.elseSt != null)
					last_st = (IfElseStat)last_st.elseSt;
				ENode[] vae = new ENode[mm.params.length];
				for(int k=0; k < vae.length; k++) {
					vae[k] = new CastExpr(0,mm.type.arg(k),
						new LVarExpr(0,mm.params[k]), Kiev.verify);
				}
				if( m.type.ret() ≢ Type.tpVoid ) {
					if( overwr.type.ret() ≡ Type.tpVoid )
						br = new Block(0,new ENode[]{
							new ExprStat(0,new CallExpr(0,new ThisExpr(true),overwr,null,vae,true)),
							new ReturnStat(0,new ConstNullExpr())
						});
					else {
						if( !overwr.type.ret().isReference() && mm.type.ret().isReference() ) {
							CallExpr ce = new CallExpr(0,new ThisExpr(true),overwr,null,vae,true);
							br = new ReturnStat(0,ce);
							CastExpr.autoCastToReference(ce);
						}
						else
							br = new ReturnStat(0,new CallExpr(0,new ThisExpr(true),overwr,null,vae,true));
					}
				} else {
					br = new Block(0,new ENode[]{
						new ExprStat(0,new CallExpr(0,new ThisExpr(true),overwr,null,vae,true)),
						new ReturnStat(0,null)
					});
				}
				last_st.elseSt = br;
			}
			assert (mm.parent == this.getStruct());
			if (st != null) {
				Block body = new Block(0);
				body.stats.add(st);
				if (mm.body != null)
					mm.body.stats.insert(0, body);
				else
					mm.body = body;
			}
			multimethods = new List.Cons<Method>(mm, multimethods);
		}
	}

	private IfElseStat makeDispatchStat(Method mm, MMTree mmt) {
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
				if (t instanceof WrapperType)
					t = t.getUnwrappedType();
				if (t instanceof CompaundType && ((CompaundType)t).clazz.isTypeUnerasable()) {
					if (t.getStruct().typeinfo_clazz == null)
						t.getStruct().getRView().autoGenerateTypeinfoClazz();
					ENode tibe = new CallExpr(pos,
						accessTypeInfoField(mmt.m,t,false),
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("$instanceof"),Type.tpBoolean,Type.tpObject),
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
				ENode st = makeMMDispatchCall(mmt.uppers[i].m.pos,mm,mmt.uppers[i].m);
				br = new IfElseStat(0,cond,st,null);
			} else {
				br = new IfElseStat(0,cond,makeDispatchStat(mm,mmt.uppers[i]),null);
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
			br = makeMMDispatchCall(mmt.m.pos,mm,mmt.m);
			IfElseStat st = dsp;
			while( st.elseSt != null ) st = (IfElseStat)st.elseSt;
			st.elseSt = br;
		}
		return dsp;
	}
	
	private ENode makeMMDispatchCall(int pos, Method dispatcher, Method dispatched) {
		assert (dispatched != dispatcher);
		assert (dispatched.isAttached());
		if (dispatched.ctx_clazz == this.getStruct()) {
			assert (dispatched.parent == this.getStruct());
			return new InlineMethodStat(pos,~dispatched,dispatcher);
		} else {
			return makeDispatchCall(pos,dispatched,dispatcher);
		}
	}

	private ENode makeDispatchCall(int pos, Method dispatcher, Method dispatched) {
		//return new InlineMethodStat(pos,dispatched,dispatcher)
		ENode obj = null;
		if (!dispatched.isStatic() && !dispatcher.isStatic())
			obj = new ThisExpr(pos);
		CallExpr ce = new CallExpr(pos, obj, dispatched, null, ENode.emptyArray, this.getStruct() != dispatched.ctx_clazz);
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
				sb.append(m.parent).append('.').append(m).append('\n');
			else
				sb.append("root:\n");
			for(int j=0; j < uppers.length; j++) {
				if( uppers[j] == null ) continue;
				uppers[j].dump(i+1,sb);
			}
			return sb;
		}

	}
	
}

