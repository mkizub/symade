package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import java.io.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JTypeDefView;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */


@node(copyable=false)
public class Struct extends TypeDef implements Named, ScopeOfNames, ScopeOfMethods, ScopeOfOperators, SetBody, Accessable {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="false")	DNode[]		members;
	}

	@node
	public static final class StructImpl extends TypeDefImpl {
		public StructImpl() {}
		public StructImpl(int pos) { super(pos); }
		public StructImpl(int pos, int fl) { super(pos, fl); }

		public final Struct getStruct() { return (Struct)this._self; }
		
		     public Access						acc;
		     public ClazzName					name;
		     BaseTypeProvider					imeta_type;
		     WrapperTypeProvider				wmeta_type;
		@ref public BaseType					type;
		@att public TypeRef						view_of;
		@att public TypeRef						super_bound;
		@att public NArr<TypeRef>				interfaces;
		@att public NArr<TypeArgDef>			args;
		@ref public Struct						package_clazz;
		@ref public Struct						typeinfo_clazz;
		@ref public NArr<Struct>				sub_clazz;
		@ref public NArr<DNode>					imported;
		public kiev.be.java.Attr[]				attrs = kiev.be.java.Attr.emptyArray;
		@att public NArr<DNode>					members;

		public void callbackChildChanged(AttrSlot attr) {
			if (attr.name == "members") {
				if (type != null)
					type.invalidate();
			}
			else if (attr.name == "meta") {
				if (type != null)
					type.invalidate();
			}
			else if (attr.name == "args") {
				imeta_type.version++;
			}
			if (attr.name == "super_bound") {
				imeta_type.version++;
			}
			else if (attr.name == "interfaces") {
				imeta_type.version++;
			}
			else if (attr.name == "package_clazz") {
				imeta_type.version++;
			}
		}	
	}
	@nodeview
	public static final view StructView of StructImpl extends TypeDefView {
		public				Access				acc;
		public				ClazzName			name;
		public				BaseTypeProvider	imeta_type;
		public				WrapperTypeProvider	wmeta_type;
		public				BaseType			type;
		public				TypeRef				view_of;
		public				TypeRef				super_bound;
		public access:ro	NArr<TypeRef>		interfaces;
		public access:ro	NArr<TypeArgDef>	args;
		public				Struct				package_clazz;
		public				Struct				typeinfo_clazz;
		public access:ro	NArr<Struct>		sub_clazz;
		public access:ro	NArr<DNode>			imported;
		public access:ro	NArr<DNode>			members;

		@setter public final void set$acc(Access val) { this.$view.acc = val; this.$view.acc.verifyAccessDecl(getDNode()); }
		@getter public final BaseType	get$super_type()	{ return (BaseType)super_bound.lnk; }
		@setter public final void set$super_type(BaseType tp) { super_bound = new TypeRef(super_bound.pos, tp); }
		
		public boolean isClazz() {
			return !isPackage() && !isInterface() && ! isArgument();
		}
		
		// package	
		public final boolean isPackage()  {
			return this.$view.is_struct_package;
		}
		public final void setPackage(boolean on) {
			assert(!on || (!isInterface() && ! isEnum() && !isSyntax()));
			if (this.$view.is_struct_package != on) {
				this.$view.is_struct_package = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// a class's argument	
		public final boolean isArgument() {
			return this.$view.is_struct_argument;
		}
		public final void setArgument(boolean on) {
			if (this.$view.is_struct_argument != on) {
				this.$view.is_struct_argument = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// a pizza case	
		public final boolean isPizzaCase() {
			return this.$view.is_struct_pizza_case;
		}
		public final void setPizzaCase(boolean on) {
			if (this.$view.is_struct_pizza_case != on) {
				this.$view.is_struct_pizza_case = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// a structure with the only one instance (singleton)	
		public final boolean isSingleton() {
			return this.$view.is_struct_singleton;
		}
		public final void setSingleton(boolean on) {
			if (this.$view.is_struct_singleton != on) {
				this.$view.is_struct_singleton = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// a local (in method) class	
		public final boolean isLocal() {
			return this.$view.is_struct_local;
		}
		public final void setLocal(boolean on) {
			if (this.$view.is_struct_local != on) {
				this.$view.is_struct_local = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// an anonymouse (unnamed) class	
		public final boolean isAnonymouse() {
			return this.$view.is_struct_anomymouse;
		}
		public final void setAnonymouse(boolean on) {
			if (this.$view.is_struct_anomymouse != on) {
				this.$view.is_struct_anomymouse = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// has pizza cases
		public final boolean isHasCases() {
			return this.$view.is_struct_has_pizza_cases;
		}
		public final void setHasCases(boolean on) {
			if (this.$view.is_struct_has_pizza_cases != on) {
				this.$view.is_struct_has_pizza_cases = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// verified
		public final boolean isVerified() {
			return this.$view.is_struct_verified;
		}
		public final void setVerified(boolean on) {
			if (this.$view.is_struct_verified != on) {
				this.$view.is_struct_verified = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// indicates that structure members were generated
		public final boolean isMembersGenerated() {
			return this.$view.is_struct_members_generated;
		}
		public final void setMembersGenerated(boolean on) {
			if (this.$view.is_struct_members_generated != on) {
				this.$view.is_struct_members_generated = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// indicates that structure members were pre-generated
		public final boolean isMembersPreGenerated() {
			return this.$view.is_struct_pre_generated;
		}
		public final void setMembersPreGenerated(boolean on) {
			if (this.$view.is_struct_pre_generated != on) {
				this.$view.is_struct_pre_generated = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		
		// indicates that statements in code were generated
		public final boolean isStatementsGenerated() {
			return this.$view.is_struct_statements_generated;
		}
		public final void setStatementsGenerated(boolean on) {
			if (this.$view.is_struct_statements_generated != on) {
				this.$view.is_struct_statements_generated = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// indicates that the structrue was generared (from template)
		public final boolean isGenerated() {
			return this.$view.is_struct_generated;
		}
		public final void setGenerated(boolean on) {
			if (this.$view.is_struct_generated != on) {
				this.$view.is_struct_generated = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// indicates that type of the structure was attached
		public final boolean isTypeResolved() {
			return this.$view.is_struct_type_resolved;
		}
		public final void setTypeResolved(boolean on) {
			if (this.$view.is_struct_type_resolved != on) {
				this.$view.is_struct_type_resolved = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// indicates that type arguments of the structure were resolved
		public final boolean isArgsResolved() {
			return this.$view.is_struct_args_resolved;
		}
		public final void setArgsResolved(boolean on) {
			if (this.$view.is_struct_args_resolved != on) {
				this.$view.is_struct_args_resolved = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// indicates that the structrue has runtime-visible type arguments
		public final boolean isRuntimeArgTyped() {
			return this.$view.is_struct_rt_arg_typed;
		}
		public final void setRuntimeArgTyped(boolean on) {
			if (this.$view.is_struct_rt_arg_typed != on) {
				this.$view.is_struct_rt_arg_typed = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// kiev annotation
		public final boolean isAnnotation() {
			return this.$view.is_struct_annotation;
		}
		public final void setAnnotation(boolean on) {
			assert(!on || (!isPackage() && !isSyntax()));
			if (this.$view.is_struct_annotation != on) {
				this.$view.is_struct_annotation = on;
				if (on) this.setInterface(true);
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// java enum
		public final boolean isEnum() {
			return this.$view.is_struct_enum;
		}
		public final void setEnum(boolean on) {
			if (this.$view.is_struct_enum != on) {
				this.$view.is_struct_enum = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// kiev syntax
		public final boolean isSyntax() {
			return this.$view.is_struct_syntax;
		}
		public final void setSyntax(boolean on) {
			assert(!on || (!isPackage() && ! isEnum()));
			if (this.$view.is_struct_syntax != on) {
				this.$view.is_struct_syntax = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// structure was loaded from bytecode
		public final boolean isLoadedFromBytecode() {
			return this.$view.is_struct_bytecode;
		}
		public final void setLoadedFromBytecode(boolean on) {
			this.$view.is_struct_bytecode = on;
		}
	}
	public NodeView			getNodeView()		alias operator(210,fy,$cast) { return new StructView((StructImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		alias operator(210,fy,$cast) { return new StructView((StructImpl)this.$v_impl); }
	public TypeDefView		getTypeDefView()	alias operator(210,fy,$cast) { return new StructView((StructImpl)this.$v_impl); }
	public StructView		getStructView()		alias operator(210,fy,$cast) { return new StructView((StructImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		alias operator(210,fy,$cast) { return new JStructView((StructImpl)this.$v_impl); }
	public JDNodeView		getJDNodeView()		alias operator(210,fy,$cast) { return new JStructView((StructImpl)this.$v_impl); }
	public JTypeDefView		getJTypeDefView()	alias operator(210,fy,$cast) { return new JStructView((StructImpl)this.$v_impl); }
	public JStructView		getJStructView()	alias operator(210,fy,$cast) { return new JStructView((StructImpl)this.$v_impl); }

	/** Variouse names of the class */
	     public abstract virtual			ClazzName					name;

	/** Type associated with this class */
	     public abstract virtual			BaseTypeProvider			imeta_type;
	     public abstract virtual			WrapperTypeProvider			wmeta_type;
	@ref public abstract virtual			BaseType					type;
	@att public abstract virtual			TypeRef						view_of;

	/** Bound super-class for class arguments */
	@att public abstract virtual			TypeRef						super_bound;

	/** Bound super-class for class arguments */
	@ref public abstract virtual			BaseType					super_type;

	/** SuperInterface types */
	@att public abstract virtual access:ro	NArr<TypeRef>				interfaces;

	/** Class' type arguments */
	@att public abstract virtual access:ro	NArr<TypeArgDef>			args;
	
	/** Class' access */
	     public abstract virtual			Access						acc;

	/** Package structure this structure belongs to */
	@ref public abstract virtual			Struct						package_clazz;

	/** The auto-generated class for parametriezed
	  classes, that containce type info
	 */
	@ref public abstract virtual			Struct						typeinfo_clazz;
	
	/** Array of substructures of the structure */
	@ref public abstract virtual access:ro	NArr<Struct>				sub_clazz;

	/** Array of imported classes,fields and methods */
	@ref public abstract virtual access:ro	NArr<DNode>					imported;

	/** Array of methods defined in this structure */
	@att public abstract virtual access:ro	NArr<DNode>					members;
	
	@getter public Access				get$acc()					{ return this.getStructView().acc; }
	@getter public ClazzName			get$name()					{ return this.getStructView().name; }
	@getter public BaseTypeProvider		get$imeta_type()			{ return this.getStructView().imeta_type; }
	@getter public WrapperTypeProvider	get$wmeta_type()			{ return this.getStructView().wmeta_type; }
	@getter public BaseType				get$type()					{ return this.getStructView().type; }
	@getter public TypeRef				get$view_of()				{ return this.getStructView().view_of; }
	@getter public TypeRef				get$super_bound()			{ return this.getStructView().super_bound; }
	@getter public NArr<TypeRef>		get$interfaces()			{ return this.getStructView().interfaces; }
	@getter public NArr<TypeArgDef>		get$args()					{ return this.getStructView().args; }
	@getter public Struct				get$package_clazz()			{ return this.getStructView().package_clazz; }
	@getter public Struct				get$typeinfo_clazz()		{ return this.getStructView().typeinfo_clazz; }
	@getter public NArr<Struct>			get$sub_clazz()				{ return this.getStructView().sub_clazz; }
	@getter public NArr<DNode>			get$imported()				{ return this.getStructView().imported; }
	@getter public NArr<DNode>			get$members()				{ return this.getStructView().members; }
	@getter public BaseType				get$super_type()			{ return this.getStructView().super_type; }

	@setter public void set$acc(Access val)							{ this.getStructView().acc = val; }
	@setter public void set$name(ClazzName val)						{ this.getStructView().name = val; }
	@setter public void set$imeta_type(BaseTypeProvider val)			{ this.getStructView().imeta_type = val; }
	@setter public void set$wmeta_type(WrapperTypeProvider val)		{ this.getStructView().wmeta_type = val; }
	@setter public void set$type(BaseType val)							{ this.getStructView().type = val; }
	@setter public void set$view_of(TypeRef val)						{ this.getStructView().view_of = val; }
	@setter public void set$super_bound(TypeRef val)					{ this.getStructView().super_bound = val; }
	@setter public void set$package_clazz(Struct val)					{ this.getStructView().package_clazz = val; }
	@setter public void set$typeinfo_clazz(Struct val)					{ this.getStructView().typeinfo_clazz = val; }
	@setter public void set$super_type(BaseType val) 					{ this.getStructView().super_type = val; }

	
	Struct() {
		super(new StructImpl(0,0));
		this.name = ClazzName.Empty;
	}
	
	public Struct(ClazzName name, Struct outer, int acc) {
		super(new StructImpl(0,acc));
		this.name = name;
		this.imeta_type = new BaseTypeProvider(this);
		this.type = BaseType.createRefType(this, TVarSet.emptySet);
		this.super_bound = new TypeRef();
		this.meta = new MetaSet();
		package_clazz = outer;
		this.acc = new Access(0);
		trace(Kiev.debugCreation,"New clazz created: "+name.short_name	+" as "+name.name+", member of "+outer);
	}

	@getter public Struct get$child_ctx_clazz()	{ return this; }

	public Object copy() {
		throw new CompilerException(this,"Struct node cannot be copied");
	};

	public String toString() { return name.name.toString(); }

	// normal class
	public boolean isClazz() { return getStructView().isClazz(); }
	// package	
	public boolean isPackage() { return getStructView().isPackage(); }
	public void setPackage(boolean on) { getStructView().setPackage(on); }
	// a class's argument	
	public boolean isArgument() { return getStructView().isArgument(); }
	public void setArgument(boolean on) { getStructView().setArgument(on); }
	// a class's argument	
	public boolean isPizzaCase() { return getStructView().isPizzaCase(); }
	public void setPizzaCase(boolean on) { getStructView().setPizzaCase(on); }
	// a structure with the only one instance (singleton)	
	public boolean isSingleton() { return getStructView().isSingleton(); }
	public void setSingleton(boolean on) { getStructView().setSingleton(on); }
	// a local (in method) class	
	public boolean isLocal() { return getStructView().isLocal(); }
	public void setLocal(boolean on) { getStructView().setLocal(on); }
	// an anonymouse (unnamed) class	
	public boolean isAnonymouse() { return getStructView().isAnonymouse(); }
	public void setAnonymouse(boolean on) { getStructView().setAnonymouse(on); }
	// has pizza cases
	public boolean isHasCases() { return getStructView().isHasCases(); }
	public void setHasCases(boolean on) { getStructView().setHasCases(on); }
	// verified
	public boolean isVerified() { return getStructView().isVerified(); }
	public void setVerified(boolean on) { getStructView().setVerified(on); }
	// indicates that structure members were generated
	public boolean isMembersGenerated() { return getStructView().isMembersGenerated(); }
	public void setMembersGenerated(boolean on) { getStructView().setMembersGenerated(on); }
	// indicates that structure members were pre-generated
	public boolean isMembersPreGenerated() { return getStructView().isMembersPreGenerated(); }
	public void setMembersPreGenerated(boolean on) { getStructView().setMembersPreGenerated(on); }
	// indicates that statements in code were generated
	public boolean isStatementsGenerated() { return getStructView().isStatementsGenerated(); }
	public void setStatementsGenerated(boolean on) { getStructView().setStatementsGenerated(on); }
	// indicates that the structrue was generared (from template)
	public boolean isGenerated() { return getStructView().isGenerated(); }
	public void setGenerated(boolean on) { getStructView().setGenerated(on); }
	// indicates that type arguments of the structure were resolved
	public final boolean isTypeResolved() { return getStructView().isTypeResolved(); }
	public final void setTypeResolved(boolean on) { getStructView().setTypeResolved(on); }
	// indicates that type arguments of the structure were resolved
	public final boolean isArgsResolved() { return getStructView().isArgsResolved(); }
	public final void setArgsResolved(boolean on) { getStructView().setArgsResolved(on); }
	// indicates that the structrue has runtime-visible type arguments
	public final boolean isRuntimeArgTyped() { return getStructView().isRuntimeArgTyped(); }
	public final void setRuntimeArgTyped(boolean on) { getStructView().setRuntimeArgTyped(on); }
	// java annotation
	public boolean isAnnotation() { return getStructView().isAnnotation(); }
	public void setAnnotation(boolean on) { getStructView().setAnnotation(on); }
	// java enum
	public boolean isEnum() { return getStructView().isEnum(); }
	public void setEnum(boolean on) { getStructView().setEnum(on); }
	// kiev syntax
	public boolean isSyntax() { return getStructView().isSyntax(); }
	public void setSyntax(boolean on) { getStructView().setSyntax(on); }
	// structure was loaded from bytecode
	public boolean isLoadedFromBytecode() { return getStructView().isLoadedFromBytecode(); }
	public void setLoadedFromBytecode(boolean on) { getStructView().setLoadedFromBytecode(on); }

	public NodeName getName() { return name; }
	
	/** hashCode of structure is a hash code
		of it's name, which must be unique for each structure
	 */
	public int hashCode() { return name.hashCode(); }

	/** Checks if this structure is equals to another
		Structures are equals if their fully-qualified
		names equals
	 */
	public boolean equals(Struct cl) {
		if( cl == null ) return false;
		return name.name.equals(cl.name.name);
	}

	public MetaPizzaCase getMetaPizzaCase() {
//		return (MetaPizzaCase)this.meta.get(MetaPizzaCase.NAME);
		foreach (Meta m; meta.metas; m instanceof MetaPizzaCase)
			return (MetaPizzaCase)m;
		return null;
	}

	public MetaErasable getMetaErasable() {
		foreach (Meta m; meta.metas; m instanceof MetaErasable)
			return (MetaErasable)m;
		return null;
	}

	public Field[] getEnumFields() {
		if( !isEnum() )
			throw new RuntimeException("Request for enum fields in non-enum structure "+this);
		int idx = 0;
		foreach (ASTNode n; this.members; n instanceof Field && ((Field)n).isEnumField())
			idx++;
		Field[] eflds = new Field[idx];
		idx = 0;
		foreach (ASTNode n; this.members; n instanceof Field && ((Field)n).isEnumField()) {
			eflds[idx] = (Field)n;
			idx ++;
		}
		return eflds;
	}

	public int getIndexOfEnumField(Field f) {
		if( !isEnum() )
			throw new RuntimeException("Request for enum fields in non-enum structure "+this);
		int idx = 0;
		foreach (ASTNode n; this.members; n instanceof Field && ((Field)n).isEnumField()) {
			if (f == n)
				return idx;
			idx++;
		}
		throw new RuntimeException("Enum value for field "+f+" not found in "+this);
	}

	public Dumper toJava(Dumper dmp) {
		if( isArgument() ) {
//			dmp.append("/*").append(name.short_name).append("*/");
			if( interfaces.length > 0 )
				return dmp.append(interfaces[0]);
			else
				return dmp.append(super_type);
		} else {
			if (isLocal())
				return dmp.append(name.short_name);
			else
				return dmp.append(name);
		}
	}
	
	public int countAnonymouseInnerStructs() {
		int i=0;
		foreach(Struct s; sub_clazz; s.isAnonymouse() || s.isLocal()) i++;
		return i;
	}

	public int countPackedFields() {
		int i = 0;
		foreach (DNode n; members; n instanceof Field && ((Field)n).isPackedField()) i++;
		return i;
	}

	public int countAbstractFields() {
		int i = 0;
		foreach (DNode n; members; n instanceof Field && n.isAbstract()) i++;
		return i;
	}

	public boolean checkResolved() {
		if( !isResolved() ) {
			if( Env.getStruct(this.name) == null ) {
				if (isPackage())
					setResolved(true);
				else
					throw new RuntimeException("Class "+this+" not found");
			}
			if( !isResolved() )
				throw new RuntimeException("Class "+this+" unresolved");
		}
		return true;
	}

	public boolean instanceOf(Struct cl) {
		if( cl == null ) return false;
		if( this.equals(cl) ) return true;
		if( super_bound.lnk != null && super_type.clazz.instanceOf(cl) )
			return true;
		if( cl.isInterface() ) {
			for(int i=0; i < interfaces.length; i++) {
				if( interfaces[i].getStruct().instanceOf(cl) ) return true;
			}
		}
		return false;
	}

	public Field resolveField(KString name) {
		return resolveField(name,this,true);
	}

	public Field resolveField(KString name, boolean fatal) {
		return resolveField(name,this,fatal);
	}

	private Field resolveField(KString name, Struct where, boolean fatal) {
		checkResolved();
		foreach(DNode f; members; f instanceof Field && ((Field)f).name.equals(name) ) return (Field)f;
		if( super_type != null ) return super_type.getStruct().resolveField(name,where,fatal);
		if (fatal)
			throw new RuntimeException("Unresolved field "+name+" in class "+where);
		return null;
	}

	public rule resolveOperatorR(Operator@ op)
		ASTNode@ imp;
	{
		trace( Kiev.debugResolve, "Resolving operator: "+op+" in syntax "+this),
		{
			imp @= imported,
			imp instanceof Opdef && ((Opdef)imp).resolved != null,
			op ?= ((Opdef)imp).resolved,
			trace( Kiev.debugResolve, "Resolved operator: "+op+" in syntax "+this)
		;	imp @= imported,
			imp instanceof Import && ((Import)imp).mode == Import.ImportMode.IMPORT_SYNTAX,
			((Struct)((Import)imp).resolved).resolveOperatorR(op)
		}
	}

	public rule resolveNameR(DNode@ node, ResInfo info, KString name)
	{
		info.isStaticAllowed(),
		trace(Kiev.debugResolve,"Struct: Resolving name "+name+" in "+this),
		checkResolved(),
		{
			trace(Kiev.debugResolve,"Struct: resolving in "+this),
			resolveNameR_1(node,info,name), // resolve in this class
			$cut
		;	info.isImportsAllowed(),
			trace(Kiev.debugResolve,"Struct: resolving in imports of "+this),
			resolveNameR_2(node,info,name), // resolve in imports
			$cut
		;	this.name.short_name.equals(nameIdefault),
			trace(Kiev.debugResolve,"Struct: resolving in default interface implementation of "+this),
			package_clazz.resolveNameR(node,info,name),
			$cut
		;	info.isSuperAllowed(),
			trace(Kiev.debugResolve,"Struct: resolving in super-class of "+this),
			resolveNameR_3(node,info,name), // resolve in super-classes
			$cut
		;	this.isPackage(),
			trace(Kiev.debugResolve,"Struct: trying to load in package "+this),
			tryLoad(node,name),
			$cut
		}
	}
	protected rule resolveNameR_1(DNode@ node, ResInfo info, KString name)
		TypeArgDef@ arg;
	{
			this.name.short_name.equals(name), node ?= this
		;	arg @= args,
			arg.name.name.equals(name),
			node ?= arg
		;	node @= members,
			node instanceof Field && ((Field)node).name.equals(name) && info.check(node)
		;	node @= members,
			node instanceof Struct && ((Struct)node).name.short_name.equals(name)
		;	isPackage(),
			node @= sub_clazz,
			((Struct)node).name.short_name.equals(name)
	}
	protected rule resolveNameR_2(DNode@ node, ResInfo info, KString name)
	{
			node @= imported,
			{	node instanceof Field && ((Field)node).isStatic() && ((Field)node).name.equals(name)
			;	node instanceof TypeDefOp && ((TypeDefOp)node).name.equals(name)
			}
	}
	protected rule resolveNameR_3(DNode@ node, ResInfo info, KString name)
		Type@ sup;
	{
			{	sup ?= super_type,
				info.enterSuper() : info.leaveSuper(),
				sup.getStruct().resolveNameR(node,info,name)
			;	sup @= TypeRef.linked_elements(interfaces),
				info.enterSuper() : info.leaveSuper(),
				sup.getStruct().resolveNameR(node,info,name)
			}
	}

	public boolean tryLoad(ASTNode@ node, KString name) {
		if( isPackage() ) {
			Struct cl;
			ClazzName clname = ClazzName.Empty;
			if( this.equals(Env.root) ) {
				clname = ClazzName.fromToplevelName(name,false);
				cl = Env.getStruct(clname);
			} else {
				KStringBuffer ksb = new KStringBuffer(this.name.name.len+name.len+1);
				ksb.append(this.name.name).append('.').append(name);
				clname = ClazzName.fromToplevelName(ksb.toKString(),false);
				cl = Env.getStruct(clname);
			}
			if( cl != null ) {
				trace(Kiev.debugResolve,"Struct "+cl+" found in "+this);
				node = cl;
				return true;
			} else {
				trace(Kiev.debugResolve,"Class "+clname.name
					+" with bytecode name "+clname.bytecode_name+" not found in "
					+this);
			}
		}
		node = null;
		return false;
	}

	final public rule resolveMethodR(DNode@ node, ResInfo info, KString name, MethodType mt)
	{
		resolveStructMethodR(node, info, name, mt, this.type)
	}

	protected rule resolveStructMethodR(DNode@ node, ResInfo info, KString name, MethodType mt, Type tp)
		ASTNode@ member;
		Type@ sup;
	{
		info.isStaticAllowed(),
		checkResolved(),
		trace(Kiev.debugResolve, "Resolving "+name+" in "+this),
		{
			node @= members,
			node instanceof Method,
			((Method)node).name.equals(name),
			info.check(node),
			((Method)node).equalsByCast(name,mt,tp,info)
		;	info.isImportsAllowed() && isPackage(),
			node @= imported, node instanceof Method,
			((Method)node).equalsByCast(name,mt,tp,info)
		;	info.isSuperAllowed(),
			sup ?= super_type,
			info.enterSuper() : info.leaveSuper(),
			sup.getStruct().resolveStructMethodR(node,info,name,mt,Type.getRealType(tp,sup))
		;	isInterface(),
			member @= members,
			member instanceof Struct && ((Struct)member).isClazz() && ((Struct)member).name.short_name.equals(nameIdefault),
			info.enterMode(ResInfo.noSuper) : info.leaveMode(),
			((Struct)member).resolveStructMethodR(node,info,name,mt,Type.getRealType(tp,sup))
		;	info.isSuperAllowed(),
			isInterface(),
			sup @= TypeRef.linked_elements(interfaces),
			info.enterSuper() : info.leaveSuper(),
			sup.getStruct().resolveStructMethodR(node,info,name,mt,Type.getRealType(tp,sup))
		}
	}

	public Method resolveMethod(KString name, Type ret, ...) {
		Type[] args = new Type[va_args.length];
		for (int i=0; i < va_args.length; i++)
			args[i] = (Type)va_args[i];
		MethodType mt = new MethodType(args,ret);
		DNode@ m;
		if (!this.type.resolveCallAccessR(m, new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic), name, mt) &&
			!this.type.resolveCallStaticR(m, new ResInfo(this,ResInfo.noForwards|ResInfo.noImports), name, mt))
			throw new CompilerException(this,"Unresolved method "+name+mt+" in class "+this);
		return (Method)m;
	}

	/** Add information about new sub structure, this class (package) containes */
	public Struct addSubStruct(Struct sub) {
		// Check we already have this sub-class
		for(int i=0; i < sub_clazz.length; i++) {
			if( sub_clazz[i].equals(sub) ) {
				// just ok
				return sub;
			}
		}
		// Check package class is null or equals to this
		if( sub.package_clazz == null ) sub.package_clazz = this;
		else if( sub.package_clazz != this ) {
			throw new RuntimeException("Sub-structure "+sub+" already has package class "
				+sub.package_clazz+" that differs from "+this);
		}

		sub_clazz.append(sub);

		trace(Kiev.debugMembers,"Sub-class "+sub+" added to class "+this);
		if (sub.name.short_name == nameClTypeInfo) {
			typeinfo_clazz = sub;
			trace(Kiev.debugMembers,"Sub-class "+sub+" is the typeinfo class of "+this);
		}
		return sub;
	}

	/** Add information about new method that belongs to this class */
	public Method addMethod(Method m) {
		// Check we already have this method
		foreach (ASTNode n; members; n instanceof Method) {
			Method mm = (Method)n;
			if( mm.equals(m) )
				throw new RuntimeException("Method "+m+" already exists in class "+this);
			if (mm.name.equals(m.name) && mm.type.equals(m.type))
				throw new RuntimeException("Method "+m+" already exists in class "+this);
		}
		members.append(m);
		trace(Kiev.debugMembers,"Method "+m+" added to class "+this);
		return m;
	}

	/** Remove information about new method that belongs to this class */
	public void removeMethod(Method m) {
		// Check we already have this method
		int i = 0;
		for(i=0; i < members.length; i++) {
			if( members[i].equals(m) ) {
				members.del(i);
				trace(Kiev.debugMembers,"Method "+m+" removed from class "+this);
				return;
			}
		}
		throw new RuntimeException("Method "+m+" do not exists in class "+this);
	}

	/** Add information about new field that belongs to this class */
	public Field addField(Field f) {
		// Check we already have this field
		foreach (ASTNode n; members; n instanceof Field) {
			Field ff = (Field)n;
			if( ff.equals(f) ) {
				throw new RuntimeException("Field "+f+" already exists in class "+this);
			}
		}
		members.append(f);
		trace(Kiev.debugMembers,"Field "+f+" added to class "+this);
		return f;
	}

	/** Remove information about a field that belongs to this class */
	public void removeField(Field f) {
		// Check we already have this method
		for(int i=0; i < members.length; i++) {
			if( members[i].equals(f) ) {
				members.del(i);
				trace(Kiev.debugMembers,"Field "+f+" removed from class "+this);
				return;
			}
		}
		throw new RuntimeException("Field "+f+" do not exists in class "+this);
	}

	/** Add information about new pizza case of this class */
	public Struct addCase(Struct cas) {
		setHasCases(true);
		int caseno = 0;
		foreach (DNode n; members; n instanceof Struct && ((Struct)n).isPizzaCase()) {
			Struct s = (Struct)n;
			MetaPizzaCase meta = s.getMetaPizzaCase();
			if (meta != null && meta.getTag() > caseno)
				caseno = meta.getTag();
		}
		MetaPizzaCase meta = cas.getMetaPizzaCase();
		if (meta == null) {
			meta = new MetaPizzaCase();
			cas.meta.set(meta);
		}
		meta.setTag(caseno + 1);
		trace(Kiev.debugMembers,"Class's case "+cas+" added to class "	+this+" as case # "+meta.getTag());
		return cas;
	}

	public Type getType() { return this.type; }

/*
 TypeInfo has three modes - first, it's an unparametriezed
 or parametriezed type with all resolved arguments like
 "Ljava/lang/String" or "Lkiev/stdlib/Hashtable<I,Ljava/lang/String;>"

 Second - TypeInfo for current type arguments like
 "Akiev/stdlib/List$A;". They are accessed via
 $typeinfo.typeargs[N]

 Third - TypeInfo for types, that not fully specified
 at compiler time (withing parametriezed types) like
 "Lkiev/stdlib/List<Lkiev/stdlib/List$A>

 The TypeInfo of the object itself is stored as
 public final TypeInfo $typeinfo;

 Resolved TypeInfo fields are stored and accessed as
 public static final TypeInfo $typeinfo$N;

 Unresolved TypeInfo fields are stored in "related"
 array of TypeInfo object. When new instance is
 created, it's passed with it's own $typeinfo,
 then, if $typeinfo.related==null, it fills
 this array, replacing arguments with $typeinfo.typeargs
 actual values for type arguments. Then, this
 TypeInfo are accessed via $typeinfo.related[N];

*/

	public static String makeTypeInfoString(Type t) {
		StringBuffer sb = new StringBuffer(128);
		sb.append(t.getJType().toClassForNameString());
		if( t instanceof BaseType && ((BaseType)t).clazz.isRuntimeArgTyped() ) {
			BaseType bt = (BaseType)t;
			sb.append('<');
			boolean comma = false;
			foreach (TVar tv; bt.clazz.type.bindings().tvars; !tv.isBound() && !tv.isAlias()) {
				Type ta = bt.resolve(tv.var);
				if (comma) sb.append(',');
				sb.append(makeTypeInfoString(ta));
				comma = true;
			}
			sb.append('>');
			return sb.toString();
		} else {
		}
		return sb.toString();
	}

	public ENode accessTypeInfoField(ASTNode from, Type t) {
		if( t.isRtArgumented() ) {
			ENode ti_access;
			Method ctx_method = from.ctx_method;
			if (ctx_method != null && ctx_method.isStatic()) {
				// check we have $typeinfo as first argument
				if (ctx_method.getTypeInfoParam() == null)
					throw new CompilerException(from,"$typeinfo cannot be accessed from "+ctx_method);
				else
					ti_access = new LVarExpr(from.pos,ctx_method.getTypeInfoParam());
			}
			else {
				Field ti = resolveField(nameTypeInfo);
				ti_access = new IFldExpr(from.pos,new ThisExpr(pos),ti);
			}
			// Small optimization for the $typeinfo
			if( this.type.isInstanceOf(t.getInitialType()) )
				return ti_access;

			if (t.isArgument()) {
				// Get corresponded type argument
				ArgumentType at = (ArgumentType)t;
				KString fnm = new KStringBuffer(nameTypeInfo.length()+1+at.name.short_name.length())
						.append(nameTypeInfo).append('$').append(at.name.short_name).toKString();
				Field ti_arg = typeinfo_clazz.resolveField(fnm);
				if (ti_arg == null)
					throw new RuntimeException("Field "+fnm+" not found in "+typeinfo_clazz+" from method "+from.ctx_method);
				ti_access = new IFldExpr(from.pos,ti_access,ti_arg);
				return ti_access;
			}
		}

		KString ts = KString.from(makeTypeInfoString(t));

		// Special case for interfaces, that cannot have private fields,
		// but need typeinfo in <clinit>
		if ((from.ctx_method == null || from.ctx_method.name.name == nameClassInit) && from.ctx_clazz.isInterface()) {
			BaseType ftype = Type.tpTypeInfo;
			if (t.getStruct().isRuntimeArgTyped()) {
				if (t.getStruct().typeinfo_clazz == null)
					t.getStruct().autoGenerateTypeinfoClazz();
				ftype = t.getStruct().typeinfo_clazz.type;
			}
			ENode[] ti_args = new ENode[]{new ConstStringExpr(ts)};
			ENode e = new CastExpr(from.pos,ftype,new CallExpr(from.pos,null,
					Type.tpTypeInfo.clazz.resolveMethod(KString.from("newTypeInfo"),Type.tpTypeInfo,Type.tpString),
					ti_args));
			return e;
		}
		
		// Lookup and create if need as $typeinfo$N
		int i = 0;
	next_field:
		foreach(DNode n; members; n instanceof Field && n.isStatic()) {
			Field f = (Field)n;
			if (f.init == null || !f.name.name.startsWith(nameTypeInfo) || f.name.name.equals(nameTypeInfo))
				continue;
			i++;
			KString ti_str = ((ConstStringExpr)((CallExpr)((CastExpr)f.init).expr).args[0]).value;
			if( !ts.equals(ti_str) ) continue;
			ENode e = new SFldExpr(from.pos,f);
			return e;
		}
		BaseType ftype = Type.tpTypeInfo;
		if (t.getStruct().isRuntimeArgTyped()) {
			if (t.getStruct().typeinfo_clazz == null)
				t.getStruct().autoGenerateTypeinfoClazz();
			ftype = t.getStruct().typeinfo_clazz.type;
		}
		Field f = new Field(KString.from(nameTypeInfo+"$"+i),ftype,ACC_STATIC|ACC_FINAL); // package-private for inner classes
		ENode[] ti_args = new ENode[]{new ConstStringExpr(ts)};
		f.init = new CastExpr(from.pos,ftype,new CallExpr(from.pos,null,
				Type.tpTypeInfo.clazz.resolveMethod(KString.from("newTypeInfo"),Type.tpTypeInfo,Type.tpString),
				ti_args));
		addField(f);
		// Add initialization in <clinit>
		Constructor class_init = getClazzInitMethod();
		if( from.ctx_method != null && from.ctx_method.name.equals(nameClassInit) ) {
			class_init.addstats.insert(
				new ExprStat(f.init.getPos(),
					new AssignExpr(f.init.getPos(),AssignOperator.Assign
						,new SFldExpr(f.pos,f),new Shadow(f.init))
				),0
			);
		} else {
			class_init.addstats.insert(
				new ExprStat(f.init.getPos(),
					new AssignExpr(f.init.getPos(),AssignOperator.Assign
						,new SFldExpr(f.pos,f),new Shadow(f.init))
				),0
			);
		}
		ENode e = new SFldExpr(from.pos,f);
		return e;
//		System.out.println("Field "+f+" of type "+f.init+" added");
	}

	public Field getWrappedField(boolean required) {
		if (super_type != null && super_type.clazz instanceof Struct) {
			Struct ss = (Struct)super_type.clazz;
			Field wf = ss.getWrappedField(false);
			if(wf != null)
				return wf;
		}
		Field wf = null;
		foreach(ASTNode n; members; n instanceof Field && ((Field)n).isForward()) {
			if (wf == null)
				wf = (Field)n;
			else
				throw new CompilerException(n,"Wrapper class with multiple forward fields");
		}
		if ( wf == null ) {
			if (required)
				throw new CompilerException(this,"Wrapper class "+this+" has no forward field");
			return null;
		}
		if( Kiev.verbose ) System.out.println("Class "+this+" is a wrapper for field "+wf);
		return wf;
	}

	private void autoGenerateTypeinfoClazz() {
		if (typeinfo_clazz != null)
			return;
		if (!isInterface() && isRuntimeArgTyped()) {
			// create typeinfo class
			int flags = this.flags & JAVA_ACC_MASK;
			flags &= ~(ACC_PRIVATE | ACC_PROTECTED);
			flags |= ACC_PUBLIC | ACC_STATIC;
			typeinfo_clazz = Env.newStruct(
				ClazzName.fromOuterAndName(this,nameClTypeInfo,false,true),this,flags,true
				);
			members.add(typeinfo_clazz);
			typeinfo_clazz.setPublic(true);
			typeinfo_clazz.setResolved(true);
			if (super_type != null && ((Struct)super_type.clazz).typeinfo_clazz != null)
				typeinfo_clazz.super_type = ((Struct)super_type.clazz).typeinfo_clazz.type;
			else
				typeinfo_clazz.super_type = Type.tpTypeInfo;
			typeinfo_clazz.type = BaseType.createRefType(typeinfo_clazz, Type.emptyArray);
			addSubStruct(typeinfo_clazz);
			typeinfo_clazz.pos = pos;

			// create constructor method
			Constructor init = new Constructor(new MethodType(Type.emptyArray,Type.tpVoid),ACC_PUBLIC);
			init.body = new BlockStat(pos);
			// add in it arguments fields, and prepare for constructor
			foreach (TVar tv; this.type.bindings().tvars; !tv.isBound() && !tv.isAlias()) {
				ArgumentType t = tv.var;
				KString fname = KString.from(nameTypeInfo+"$"+t.name.short_name);
				Field f = new Field(fname,Type.tpTypeInfo,ACC_PUBLIC|ACC_FINAL);
				typeinfo_clazz.addField(f);
				FormPar v = new FormPar(pos,t.name.short_name,Type.tpTypeInfo,FormPar.PARAM_NORMAL,ACC_FINAL);
				init.params.append(v);
				init.body.stats.append(new ExprStat(pos,
					new AssignExpr(pos,AssignOperator.Assign,
						new IFldExpr(pos,new ThisExpr(pos),f),
						new LVarExpr(pos,v)
					)
				));
			}

			// create typeinfo field
			Field tif = addField(new Field(nameTypeInfo,typeinfo_clazz.type,ACC_PUBLIC|ACC_FINAL));
			// add constructor to the class
			typeinfo_clazz.addMethod(init);
			
			// and add super-constructor call
			if (typeinfo_clazz.super_type ≈ Type.tpTypeInfo) {
				//do nothing, default constructor may be added later
			} else {
				init.setNeedFieldInits(true);
				ASTCallExpression call_super = new ASTCallExpression(pos, nameSuper, ENode.emptyArray);
				foreach (TVar tv; super_type.clazz.type.bindings().tvars; !tv.isBound() && !tv.isAlias()) {
					Type t = tv.var.rebind(this.type.bindings());
					ENode expr;
					if (t instanceof ArgumentType) {
						expr = new ASTIdentifier(pos,t.name.short_name);
					} else {
						expr = new CallExpr(pos,null,
							Type.tpTypeInfo.clazz.resolveMethod(
								KString.from("newTypeInfo"),
								Type.tpTypeInfo,Type.tpString
							),
							new ENode[]{new ConstStringExpr(KString.from(makeTypeInfoString(t)))}
						);
					}
					call_super.args.append(expr);
				}
				init.body.stats.insert(new ExprStat(call_super),0);
			}

			// create method to get typeinfo field
			MethodType tim_type = new MethodType(Type.emptyArray,Type.tpTypeInfo);
			Method tim = addMethod(new Method(nameGetTypeInfo,tim_type,ACC_PUBLIC));
			tim.body = new BlockStat(pos,new ENode[]{
				new ReturnStat(pos,new IFldExpr(pos,new ThisExpr(pos),tif))
			});
		}

	}
	

	public void autoGenerateMembers() {
		checkResolved();
		if( isMembersGenerated() ) return;
		if( isPackage() ) return;

		if( super_type != null && !super_type.clazz.isMembersGenerated() ) {
			super_type.clazz.autoGenerateMembers();
		}
		for(int i=0; i < interfaces.length; i++) {
			if( !interfaces[i].getStruct().isMembersGenerated() )
				interfaces[i].getStruct().autoGenerateMembers();
		}

		if( Kiev.debug ) System.out.println("AutoGenerating members for "+this);

		KString oldfn = Kiev.curFile;
		boolean[] old_exts = Kiev.getExtSet();
		{
			ASTNode fu = parent;
			while( fu != null && !(fu instanceof FileUnit))
				fu = fu.parent;
			if( fu != null ) {
				Kiev.curFile = ((FileUnit)fu).filename;
				Kiev.setExtSet(((FileUnit)fu).disabled_extensions);
			}
		}

		try {
			autoGenerateTypeinfoClazz();
	
			// Check if it's an inner class
			if( isClazz() && package_clazz.isClazz() && !isStatic() ) {
				int n = 0;
				for(Struct pkg=package_clazz; pkg.isClazz() && !pkg.isStatic(); pkg=pkg.package_clazz)
					n++;
				Field f = addField(new Field(KString.from(nameThisDollar.toString()+n),type.getOuterArg(),ACC_FORWARD|ACC_FINAL));
				f.pos = pos;
			}
	
			if( !isInterface() && !isPackage() ) {
				// Default <init> method, if no one is declared
				boolean init_found = false;
				// Add outer hidden parameter to constructors for inner and non-static classes
				int i = -1;
				foreach (DNode n; members; ) {
					i++;
					if !(n instanceof Method)
						continue;
					Method m = (Method)n;
					if( !(m.name.equals(nameInit) || m.name.equals(nameNewOp)) ) continue;
					if( m.name.equals(nameInit) )
						init_found = true;
					boolean retype = false;
					Type[] targs = m.type.args;
					package_clazz.checkResolved();
					if( package_clazz.isClazz() && !isStatic() ) {
						// Insert outer class type as second argument, but first type
						// in signature
						targs = (Type[])Arrays.insert(targs,package_clazz.type,0);
						// Also add formal parameter
						m.params.insert(new FormPar(m.pos,nameThisDollar,targs[0],FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL),0);
						retype = true;
					}
					if (!isInterface() && isRuntimeArgTyped()) {
						targs = (Type[])Arrays.insert(targs,typeinfo_clazz.type,(retype?1:0));
						m.params.insert(new FormPar(m.pos,nameTypeInfo,typeinfo_clazz.type,FormPar.PARAM_TYPEINFO,ACC_FINAL),(retype?1:0));
						retype = true;
					}
				}
				if( !init_found ) {
					trace(Kiev.debugResolve,nameInit+" not found in class "+this);
					Constructor init = null;
					if( super_type != null && super_type.clazz == Type.tpClosureClazz ) {
						MethodType mt;
						if( !isStatic() ) {
							mt = new MethodType(new Type[]{Type.tpInt},Type.tpVoid);
							init = new Constructor(mt,ACC_PUBLIC);
							init.params.append(new FormPar(pos,nameThisDollar,package_clazz.type,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL));
							init.params.append(new FormPar(pos,KString.from("max$args"),Type.tpInt,FormPar.PARAM_NORMAL,0));
						} else {
							mt = new MethodType(new Type[]{Type.tpInt},Type.tpVoid);
							init = new Constructor(mt,ACC_PUBLIC);
							init.params.append(new FormPar(pos,KString.from("max$args"),Type.tpInt,FormPar.PARAM_NORMAL,0));
						}
					} else {
						MethodType mt;
						Type[] targs = Type.emptyArray;
						FormPar[] params = new FormPar[0];
						if( package_clazz.isClazz() && !isStatic() ) {
							targs = (Type[])Arrays.append(targs,package_clazz.type);
							params = (FormPar[])Arrays.append(params,new FormPar(pos,nameThisDollar,package_clazz.type,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL));
						}
						if (!isInterface() && isRuntimeArgTyped()) {
							targs = (Type[])Arrays.append(targs,typeinfo_clazz.type);
							params = (FormPar[])Arrays.append(params,new FormPar(pos,nameTypeInfo,typeinfo_clazz.type,FormPar.PARAM_TYPEINFO,ACC_FINAL));
						}
						if( isEnum() ) {
							targs = (Type[])Arrays.append(targs,Type.tpString);
							targs = (Type[])Arrays.append(targs,Type.tpInt);
							//targs = (Type[])Arrays.append(targs,Type.tpString);
							params = (FormPar[])Arrays.append(params,new FormPar(pos,KString.from("name"),Type.tpString,FormPar.PARAM_NORMAL,0));
							params = (FormPar[])Arrays.append(params,new FormPar(pos,nameEnumOrdinal,Type.tpInt,FormPar.PARAM_NORMAL,0));
							//params = (FormPar[])Arrays.append(params,new FormPar(pos,KString.from("text"),Type.tpString,FormPar.PARAM_NORMAL,0));
						}
						if (isView()) {
							targs = (Type[])Arrays.append(targs,view_of.getType());
							params = (FormPar[])Arrays.append(params,new FormPar(pos,nameView,view_of.getType(),FormPar.PARAM_NORMAL,ACC_FINAL));
						}
						mt = new MethodType(targs,Type.tpVoid);
						init = new Constructor(mt,ACC_PUBLIC);
						init.params.addAll(params);
					}
					init.pos = pos;
					init.body = new BlockStat(pos);
					if (isEnum() || isSingleton())
						init.setPrivate(true);
					else
						init.setPublic(true);
					addMethod(init);
				}
			}
			else if( isInterface() ) {
				Struct defaults = null;
				foreach (ASTNode n; members; n instanceof Method) {
					Method m = (Method)n;
					m.setPublic(true);
					if( !m.isAbstract() ) {
						if( m.isStatic() ) continue;
						// Now, non-static methods (templates)
						// Make it static and add abstract method
						Method abstr = new Method(m.name.name,m.type,m.getFlags()|ACC_PUBLIC );
						abstr.pos = m.pos;
						abstr.setStatic(false);
						abstr.setAbstract(true);
						abstr.params.copyFrom(m.params);
						m.replaceWithNode(abstr);
	
						// Make inner class name$default
						if( defaults == null ) {
							defaults = Env.newStruct(
								ClazzName.fromOuterAndName(this,nameIdefault,false,true),
								this,ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT, true
							);
							members.add(defaults);
							defaults.setResolved(true);
							//Type[] tarr = type.args;
							//defaults.type = Type.createRefType(defaults, tarr);
							//defaults.super_type = Type.tpObject;
							//defaults.interfaces.add(new TypeRef(this.type));
							Kiev.runProcessorsOn(defaults);
						}
						m.setStatic(true);
						m.setVirtualStatic(true);
						m.params.insert(0,new FormPar(pos,Constants.nameThis,this.type,FormPar.PARAM_NORMAL,ACC_FINAL|ACC_FORWARD));
						defaults.addMethod(m);
					}
					if( isInterface() && !m.isStatic() ) {
						m.setAbstract(true);
					}
				}
			}
		} finally { Kiev.setExtSet(old_exts); Kiev.curFile = oldfn; }

		setMembersGenerated(true);
		foreach(DNode s; members; s instanceof Struct)
			((Struct)s).autoGenerateMembers();

	}

	public Method getOverwrittenMethod(Type base, Method m) {
		Method mm = null, mmret = null;
		if( super_type != null && !isInterface() )
			mm = super_type.clazz.getOverwrittenMethod(base,m);
		if( mmret == null && mm != null ) mmret = mm;
		trace(Kiev.debugMultiMethod,"lookup overwritten methods for "+base+" in "+this);
		foreach (ASTNode n; members; n instanceof Method) {
			Method mi = (Method)n;
			if( mi.isStatic() || mi.isPrivate() || mi.name.equals(nameInit) ) continue;
			if( mi.name.name != m.name.name || mi.type.args.length != m.type.args.length ) {
//				trace(Kiev.debugMultiMethod,"Method "+m+" not matched by "+methods[i]+" in class "+this);
				continue;
			}
			MethodType mit = (MethodType)Type.getRealType(base,mi.jtype);
			if( m.jtype.equals(mit) ) {
				trace(Kiev.debugMultiMethod,"Method "+m+" overrides "+mi+" of type "+mit+" in class "+this);
				mm = mi;
				// Append constraints to m from mm
				foreach(WBCCondition cond; mm.conditions)
					m.conditions.appendUniq(cond);
				if( mmret == null && mm != null ) mmret = mm;
				break;
			} else {
				trace(Kiev.debugMultiMethod,"Method "+m+" does not overrides "+mi+" of type "+mit+" in class "+this);
			}
		}
		return mmret;
	}
	
	public Constructor getClazzInitMethod() {
		foreach(ASTNode n; members; n instanceof Method && ((Method)n).name.equals(nameClassInit) )
			return (Constructor)n;
		Constructor class_init = new Constructor(new MethodType(Type.emptyArray,Type.tpVoid),ACC_STATIC);
		class_init.pos = pos;
		addMethod(class_init);
		class_init.body = new BlockStat(pos);
		return class_init;
	}

	public void autoGenerateStatements() {

		if( Kiev.debug ) System.out.println("AutoGenerating statements for "+this);
		// <clinit> & common$init, if need
		Constructor class_init = null;
		Initializer instance_init = null;

		foreach (DNode n; members; n instanceof Field || n instanceof Initializer) {
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
				if( f.isStatic() ) {
					if( class_init == null )
						class_init = getClazzInitMethod();
					class_init.body.addStatement(
						new ExprStat(f.init.getPos(),
							new AssignExpr(f.init.getPos(),
								f.isInitWrapper() ? AssignOperator.Assign2 : AssignOperator.Assign,
								new SFldExpr(f.pos,f),new Shadow(f.init)
							)
						)
					);
				} else {
					if( instance_init == null ) {
						instance_init = new Initializer();
						instance_init.pos = f.init.pos;
						instance_init.body = new BlockStat();
					}
					ENode init_stat;
					init_stat = new ExprStat(f.init.getPos(),
							new AssignExpr(f.init.getPos(),
								f.isInitWrapper() ? AssignOperator.Assign2 : AssignOperator.Assign,
								new IFldExpr(f.pos,new ThisExpr(0),f),
								new Shadow(f.init)
							)
						);
					instance_init.body.addStatement(init_stat);
					init_stat.setHidden(true);
				}
			} else {
				Initializer init = (Initializer)n;
				ENode init_stat = new Shadow(init);
				init_stat.setHidden(true);
				if (init.isStatic()) {
					if( class_init == null )
						class_init = getClazzInitMethod();
					class_init.body.addStatement(init_stat);
				} else {
					if( instance_init == null ) {
						instance_init = new Initializer();
						instance_init.pos = init.pos;
						instance_init.body = new BlockStat();
					}
					instance_init.body.addStatement(init_stat);
				}
			}
		}

		// template methods of interfaces
		if( isInterface() ) {
			foreach (ASTNode n; members; n instanceof Method) {
				Method m = (Method)n;
				if( !m.isAbstract() ) {
					if( m.isStatic() ) continue;
					// Now, non-static methods (templates)
					// Make it static and add abstract method
					Method abstr = new Method(m.name.name,m.type,m.getFlags() | ACC_PUBLIC );
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
		if( !name.name.equals(Type.tpObject.clazz.name.name) ) {
			foreach (ASTNode n; members; n instanceof Constructor) {
				Constructor m = (Constructor)n;
				if( m.isStatic() ) continue;

				ASTNode initbody = m.body;

				boolean gen_def_constr = false;
				NArr<ASTNode> stats = ((BlockStat)initbody).stats;
				if( stats.length==0 ) {
					gen_def_constr = true;
				} else {
					if( stats[0] instanceof ExprStat ) {
						ExprStat es = (ExprStat)stats[0];
						ENode ce = es.expr;
						if( es.expr instanceof ASTExpression )
							ce = ((ASTExpression)es.expr).nodes[0];
						else
							ce = es.expr;
						if( ce instanceof ASTCallExpression ) {
							NameRef nm = ((ASTCallExpression)ce).func;
							if( !(nm.name.equals(nameThis) || nm.name.equals(nameSuper) ) )
								gen_def_constr = true;
							else if( nm.name.equals(nameSuper) )
								m.setNeedFieldInits(true);
						}
						else if( ce instanceof CallExpr ) {
							KString nm = ((CallExpr)ce).func.name.name;
							if( !(nm.equals(nameThis) || nm.equals(nameSuper) || nm.equals(nameInit)) )
								gen_def_constr = true;
							else {
								if( nm.equals(nameSuper) || (nm.equals(nameInit) && es.expr.isSuperExpr()) )
									m.setNeedFieldInits(true);
							}
						}
						else
							gen_def_constr = true;
					}
					else
						gen_def_constr = true;
				}
				if( gen_def_constr ) {
					m.setNeedFieldInits(true);
					ASTCallExpression call_super = new ASTCallExpression();
					call_super.pos = pos;
					call_super.func = new NameRef(pos, nameSuper);
					if( super_type.clazz == Type.tpClosureClazz ) {
						ASTIdentifier max_args = new ASTIdentifier();
						max_args.name = nameClosureMaxArgs;
						call_super.args.add(max_args);
					}
					else if( package_clazz.isClazz() && isAnonymouse() ) {
						int skip_args = 0;
						if( !isStatic() ) skip_args++;
						if( this.isRuntimeArgTyped() && super_type.clazz.isRuntimeArgTyped() ) skip_args++;
						if( m.params.length > skip_args+1 ) {
							for(int i=skip_args+1; i < m.params.length; i++) {
								call_super.args.append( new LVarExpr(m.pos,m.params[i]));
							}
						}
					}
					else if( isEnum() ) {
						call_super.args.add(new ASTIdentifier(pos, KString.from("name")));
						call_super.args.add(new ASTIdentifier(pos, nameEnumOrdinal));
						//call_super.args.add(new ASTIdentifier(pos, KString.from("text")));
					}
					else if( isView() && super_type.getStruct().isView() ) {
						call_super.args.add(new ASTIdentifier(pos, nameView));
					}
					stats.insert(new ExprStat(call_super),0);
				}
				int p = 1;
				if( package_clazz.isClazz() && !isStatic() ) {
					stats.insert(
						new ExprStat(pos,
							new AssignExpr(pos,AssignOperator.Assign,
								new IFldExpr(pos,new ThisExpr(pos),OuterThisAccessExpr.outerOf(this)),
								new LVarExpr(pos,m.params[0])
							)
						),p++
					);
				}
				if (isView()) {
					foreach (FormPar fp; m.params; fp.name.equals(nameView)) {
						stats.insert(
							new ExprStat(pos,
								new AssignExpr(pos,AssignOperator.Assign,
									new IFldExpr(pos,new ThisExpr(pos),resolveField(nameView)),
									new LVarExpr(pos,fp)
								)
							),p++
						);
						break;
					}
				}
				if (isRuntimeArgTyped() && m.isNeedFieldInits()) {
					Field tif = resolveField(nameTypeInfo);
					Var v = m.getTypeInfoParam();
					assert(v != null);
					stats.insert(
						new ExprStat(pos,
							new AssignExpr(m.pos,AssignOperator.Assign,
								new IFldExpr(m.pos,new ThisExpr(0),tif),
								new LVarExpr(m.pos,v)
							)),
						p++);
				}
				if( instance_init != null && m.isNeedFieldInits() ) {
					stats.insert((ENode)instance_init.body.copy(),p++);
				}
			}
		}
	}

	protected void combineMethods() {
		for (int cur_m=0; cur_m < members.length; cur_m++) {
			if !(members[cur_m] instanceof Method)
				continue;
			Method m = (Method)members[cur_m];
			if (m.name.equals(nameClassInit) || m.name.equals(nameInit))
				continue;
			if( m.isMultiMethod() ) {
				trace(Kiev.debugMultiMethod,"Multimethod "+m+" already processed...");
				continue; // do not process method twice...
			}
			MethodType type1 = m.type;
			MethodType dtype1 = m.dtype;
			trace(Kiev.debugMultiMethod,"Generating dispatch method for "+m+" with dispatch type "+type1);
			Method mm = null;
			// find all methods with the same java type
			ListBuffer<Method> mlistb = new ListBuffer<Method>();
			foreach (ASTNode nj; members; nj instanceof Method) {
				Method mj = (Method)nj;
//				if (m.isRuleMethod() != mj.isRuleMethod())
//					continue;
				MethodType type2 = mj.type;
				MethodType dtype2 = mj.dtype;
				if( mj.name.name != m.name.name || dtype2.args.length != dtype1.args.length )
					continue;
				if (dtype1.isMultimethodSuper(dtype2)) {
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
					trace(Kiev.debugMultiMethod,"methods "+mj+" with dispatch type "+type2+" doesn't match...");
				}
			}
			Method overwr = null;

			if (super_type != null )
				overwr = super_type.clazz.getOverwrittenMethod(this.type,m);

			// nothing to do, if no methods to combine
			if (mlistb.length() == 1 && mm != null) {
				// mm will have the base type - so, no super. call will be done
				if (overwr != null && overwr.isMultiMethod()) {
					mm.setMultiMethod(true);
					trace(Kiev.debugMultiMethod,"no need to dispatch "+m+" - just mark it as multimethod");
				} else {
					trace(Kiev.debugMultiMethod,"no need to dispatch "+m+" - not a multimethod");
				}
				continue;
			}
			// if multimethod already assigned, thus, no super. call will be done - forget it
			if (mm != null) {
				trace(Kiev.debugMultiMethod,"will attach dispatching to this method "+mm);
				overwr = null;
			}


			List<Method> mlist = mlistb.toList();

			// create a new dispatcher method...
			if (mm == null) {
				// create dispatch method
				if (m.isRuleMethod())
					mm = new RuleMethod(m.name.name, type1, m.flags);
				else
					mm = new Method(m.name.name, type1, m.flags);
				mm.name.aliases = m.name.aliases;
				mm.setStatic(m.isStatic());
				for (int j=0; j < m.params.length; j++) {
					mm.params.add(new FormPar(m.params[j].pos,m.params[j].name.name,type1.args[j],m.params[j].kind,m.params[j].flags));
				}
			}

			// create mmtree
			MMTree mmt = new MMTree(mm);
			for(List<Method> ul = mlist; ul != List.Nil; ul = ul.tail()) {
				Method rm = ul.head();
				if (rm != mm)
					removeMethod(rm);
				mmt.add(rm);
			}

			trace(Kiev.debugMultiMethod,"Dispatch tree "+mm+" is:\n"+mmt);

			IfElseStat st = null;
			st = makeDispatchStatInline(mm,mmt);

			if (overwr != null) {
				IfElseStat last_st = st;
				ENode br;
				while (last_st.elseSt != null)
					last_st = (IfElseStat)last_st.elseSt;
				ENode[] vae = new ENode[mm.params.length];
				for(int k=0; k < vae.length; k++) {
					vae[k] = new CastExpr(0,mm.type.args[k],
						new LVarExpr(0,mm.params[k]), Kiev.verify);
				}
				if( m.type.ret ≢ Type.tpVoid ) {
					if( overwr.type.ret ≡ Type.tpVoid )
						br = new BlockStat(0,new ENode[]{
							new ExprStat(0,new CallExpr(0,new ThisExpr(true),overwr,vae,true)),
							new ReturnStat(0,new ConstNullExpr())
						});
					else {
						if( !overwr.type.ret.isReference() && mm.type.ret.isReference() ) {
							CallExpr ce = new CallExpr(0,new ThisExpr(true),overwr,vae,true);
							br = new ReturnStat(0,ce);
							CastExpr.autoCastToReference(ce);
						}
						else
							br = new ReturnStat(0,new CallExpr(0,new ThisExpr(true),overwr,vae,true));
					}
				} else {
					br = new BlockStat(0,new ENode[]{
						new ExprStat(0,new CallExpr(0,new ThisExpr(true),overwr,vae,true)),
						new ReturnStat(0,null)
					});
				}
				last_st.elseSt = br;
			}
			if (st != null) {
				BlockStat body = new BlockStat(0);
				body.addStatement(st);
				if (mm.body != null)
					mm.body.stats.insert(0, body);
				else
					mm.body = body;
			}
			mm.setMultiMethod(true);
			boolean add_mm = true;
			foreach (Method rm; mlist) {
				if (mm != rm) {
					// already removed
					if (!rm.type.ret.equals(mm.type.ret)) {
						// insert new method
						Method nm = new Method(rm.name.name,rm.type,rm.flags);
						nm.pos = rm.pos;
						nm.name = rm.name;
						nm.params.addAll(rm.params);
						ENode[] vae = new ENode[mm.params.length];
						for(int k=0; k < vae.length; k++) {
							vae[k] = new LVarExpr(0,mm.params[k]);
						}
						nm.body = new BlockStat(0,new ENode[]{
							new ReturnStat(0,
								new CastExpr(0,rm.type.ret,
									new CallExpr(0,new ThisExpr(true),mm,vae)))
							});
						addMethod(nm);
					}
					// also, check if we just removed current method,
					// and correct iterator index
					if (m == rm)
						cur_m--;
				} else {
					add_mm = false; // do not add it
				}
			}
			if (add_mm)
				addMethod(mm);
		}

		// Setup java types for methods
		foreach (ASTNode n; members; n instanceof Method) {
			Method mi = (Method)n;
			if( mi.isStatic() || mi.isPrivate() || mi.name.equals(nameInit) ) continue;
			Method m = null;
			if( super_type != null )
				m = super_type.clazz.getOverwrittenMethod(this.type,mi);
			foreach(TypeRef si; interfaces ) {
				if( m == null )
					m = si.getStruct().getOverwrittenMethod(this.type,mi);
				else
					si.getStruct().getOverwrittenMethod(this.type,mi);
			}
			if( m != null ) {
				for (int i=0; i < m.params.length; i++) {
					assert(m.params[i].stype != null);
					mi.params[i].stype = (TypeRef)m.params[i].stype.copy();
				}
				mi.dtype_ref.ret = new TypeRef(m.jtype.ret);
			}
		}

	}

	IfElseStat makeDispatchStatInline(Method mm, MMTree mmt) {
		Type.tpNull.checkResolved();
		IfElseStat dsp = null;
		ENode cond = null;
		for(int i=0; i < mmt.uppers.length; i++) {
			if( mmt.uppers[i] == null ) continue;
			Method m = mmt.uppers[i].m;
			for(int j=0; j < m.type.args.length; j++) {
				Type t = m.type.args[j];
				if( mmt.m != null && t.equals(mmt.m.type.args[j]) ) continue;
				ENode be = null;
				if( mmt.m != null && !t.equals(mmt.m.type.args[j]) )
					be = new InstanceofExpr(pos,
						new LVarExpr(pos,mm.params[j]),
						Type.getRefTypeForPrimitive(t));
				if (t instanceof WrapperType)
					t = t.getUnwrappedType();
				if (t instanceof BaseType && ((BaseType)t).clazz.isRuntimeArgTyped()) {
					if (t.getStruct().typeinfo_clazz == null)
						t.getStruct().autoGenerateTypeinfoClazz();
					ENode tibe = new CallExpr(pos,
						accessTypeInfoField(mmt.m,t),
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("$instanceof"),Type.tpBoolean,Type.tpObject,Type.tpTypeInfo),
						new ENode[]{
							new LVarExpr(pos,mm.params[j]),
							new IFldExpr(pos,
								new CastExpr(pos,t,new LVarExpr(pos,mm.params[j])),
								t.getStruct().resolveField(nameTypeInfo))
						});
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
				ENode st = new InlineMethodStat(mmt.uppers[i].m.pos,mmt.uppers[i].m,mm);
				br = new IfElseStat(0,cond,st,null);
			} else {
				br = new IfElseStat(0,cond,makeDispatchStatInline(mm,mmt.uppers[i]),null);
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
			br = new InlineMethodStat(mmt.m.pos,mmt.m,mm);
			IfElseStat st = dsp;
			while( st.elseSt != null ) st = (IfElseStat)st.elseSt;
			st.elseSt = br;
		}
		return dsp;
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

	public boolean preGenerate() {
		autoProxyMethods();
//		new kiev.backend.java15.TreeMapper().mapStruct(this);
		return true;
	}
	
	public void autoProxyMethods() {
		checkResolved();
		if( isMembersPreGenerated() ) return;
		if( isPackage() ) return;
		if( super_type != null && !super_type.clazz.isMembersPreGenerated() ) {
			super_type.clazz.autoProxyMethods();
		}
		for(int i=0; i < interfaces.length; i++)
			if( !interfaces[i].getStruct().isMembersPreGenerated() ) {
				interfaces[i].getStruct().autoProxyMethods();
			}
		ASTNode fu = parent;
		while( fu != null && !(fu instanceof FileUnit))
			fu = fu.parent;
		if( fu != null )
			Kiev.curFile = ((FileUnit)fu).filename;

		if( isClazz() && super_type != null && super_type.getStruct().isFinal())
			Kiev.reportError(this, "Class "+this+" extends final class "+super_type);
		for(int i=0; i < interfaces.length; i++) {
			if( isInterface() && interfaces[i].getStruct().isFinal())
				Kiev.reportError(this, "Iterface "+this+" extends final interface "+interfaces[i]);
			interfaces[i].getStruct().autoProxyMethods(this);
		}

		if( isClazz() ) {
			boolean make_abstract = false;
			foreach(DNode n; members; n instanceof Method && n.isAbstract() && n.isStatic()) {
				Method m = (Method)n;
				m.setBad(true);
				this.setBad(true);
				Kiev.reportError(m,"Static method cannot be declared abstract");
			}
		}

		// Check all methods
//		if( !isAbstract() && isClazz() ) {
//			List<Method> ms = List.Nil;
//			ms = collectVTmethods(ms);
//		}

		setMembersPreGenerated(true);
		
		foreach(DNode s; members; s instanceof Struct)
			((Struct)s).autoProxyMethods();

		combineMethods();
	}

	// Check that Struct me implements all methods and
	// if not and method is VirtualStatic - add proxy method to 'me'
	public void autoProxyMethods(Struct me) {
		Struct defaults = null;
		foreach(ASTNode n; members; n instanceof Struct && ((Struct)n).isClazz()) {
			Struct s = (Struct)n;
			if (s.name.short_name.equals(nameIdefault) ) {
				defaults = s;
				break;
			}
		}
		foreach (DNode n; members; n instanceof Method && !n.isStatic()) {
			Method mi = (Method)n;
			Struct s = me;
			boolean found = false;
		scan_class:
			for(;;) {
				foreach (ASTNode sn; s.members; sn instanceof Method) {
					Method mj = (Method)sn;
					if( !mj.isStatic() && mj.name.equals(mi.name) ) {
						if( Type.getRealType(me.type,mj.type).equals(
										Type.getRealType(me.type,mi.type)) ) {
							trace(Kiev.debugResolve,"chk: methods "+mi.name+
									Type.getRealType(me.type,mj.type)+
									" and "+mi.name+Type.getRealType(me.type,mi.type)+" equals");
							if( !mj.isPublic() ) {
								Kiev.reportWarning(me,"Method "+s+"."+mj+" must be declared public");
								mj.setPublic(true);
							}
							found = true;
							break scan_class;
						 } else {
							trace(Kiev.debugResolve,"chk: methods "+mi.name+
									Type.getRealType(me.type,mj.type)+
									" and "+mi.name+Type.getRealType(me.type,mi.type)+" not equals");
						 }
					 }
				}
				if( s.super_type != null )
					s = (Struct)s.super_type.clazz;
				else break;
			}
			if( found ) continue;
			// Not found, check for VirtualStatic()
			Method m = mi;
			// Check this methods was produced from non-abstract method
			if (defaults != null) {
				foreach (ASTNode nn; defaults.members; nn instanceof Method) {
					Method mn = (Method)nn;
					if( mn.isStatic() && m.name.equals(mn.name) && m.type.args.length+1 == mn.type.args.length) {
						boolean match = true;
						for(int p=0; p < m.type.args.length; p++) {
							if( !m.type.args[p].equals(mn.type.args[p+1]) ) {
								match = false;
								break;
							}
						}
						if( match ) {
//							System.out.println(""+m+" "+m.isStatic()+" == "+defaults.methods[n]+" "+defaults.methods[n].isStatic());
							m = mn;
							break;
						}
					}
//					System.out.println(""+m+" "+m.isStatic()+" != "+defaults.methods[n]+" "+defaults.methods[n].isStatic());
				}
			}
			if( m.isStatic() && m.isVirtualStatic() ) {
				TypeCallRef tcr = new TypeCallRef();
				tcr.ret = new TypeRef(mi.type.ret);
				int flags = m.getFlags() | ACC_PUBLIC;
				flags &= ~ACC_STATIC;
				Method proxy = new Method(m.name.name,tcr,(TypeCallRef)tcr.copy(),flags);
				if (proxy.isVirtualStatic())
					proxy.setVirtualStatic(false);
				me.addMethod(proxy);
				for(int p=1; p < m.params.length; p++)
					proxy.params.add(new FormPar(0,m.params[p].name.name,m.type.args[p],m.params[p].kind,0));
				BlockStat bs = new BlockStat(0,ENode.emptyArray);
				ENode[] args = new ENode[m.type.args.length];
				args[0] = new ThisExpr();
				for(int k=1; k < args.length; k++)
					args[k] = new LVarExpr(0,proxy.params[k-1]);
				CallExpr ce = new CallExpr(0,null,m,args);
				if( proxy.type.ret ≡ Type.tpVoid ) {
					bs.addStatement(new ExprStat(0,ce));
					bs.addStatement(new ReturnStat(0,null));
				} else {
					bs.addStatement(new ReturnStat(0,ce));
				}
				proxy.body = bs;
			}
			else if (m.name.equals(nameGetTypeInfo))
				; // will be auto-generated later
			else {
				if( me.isInterface() )
					; // do not add methods to interfaces
				else if( me.isAbstract() ) {
					// Add abstract method
					Method proxy = new Method(m.name.name,m.type,m.getFlags() | ACC_PUBLIC | ACC_ABSTRACT);
					//proxy.jtype = m.jtype;
					me.addMethod(proxy);
				}
				else if( !m.name.equals(nameGetTypeInfo) )
					Kiev.reportWarning(me,"Method "+m+" of interface "+this+" not implemented in "+me);
			}
		}
		// Check all sub-interfaces
		for(int i=0; i < interfaces.length; i++) {
			interfaces[i].getStruct().autoProxyMethods(me);
		}
	}

	static class StructDFFunc extends DFFunc {
		final int res_idx;
		StructDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			res = DFState.makeNewState();
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new StructDFFunc(dfi);
	}

	

	/** This routine validates declaration of class, auto-generates
		<init>()V method if no <init> declared,
		also, used to generate this$N fields and arguments for
		inner classes, case$tag field for case classes and so on
	*/
	public void checkIntegrity() {
	}

	public ASTNode resolveFinalFields() {
		trace(Kiev.debugResolve,"Resolving final fields for class "+name);
		// Resolve final values of class's fields
		foreach (ASTNode n; members; n instanceof Field) {
			Field f = (Field)n;
			if( f == null || f.init == null ) continue;
			if( f.init != null ) {
				try {
					f.init.resolve(f.type);
					if (f.init instanceof TypeRef)
						((TypeRef)f.init).toExpr(f.type);
					if (f.init.getType() ≉ f.type) {
						ENode finit = (ENode)~f.init;
						f.init = new CastExpr(finit.pos, f.type, finit);
						f.init.resolve(f.type);
					}
				} catch( Exception e ) {
					Kiev.reportError(f.init,e);
				}
				trace(Kiev.debugResolve && f.init!= null && f.init.isConstantExpr(),
						(f.isStatic()?"Static":"Instance")+" fields: "+name+"::"+f.name+" = "+f.init);
			}
		}
		// Process inner classes and cases
		if( !isPackage() ) {
			for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
				sub_clazz[i].resolveFinalFields();
			}
		}
		return this;
	}

	public ASTNode resolveImports() {
		return this;
	}
	
	public void resolveMetaDefaults() {
		if (isAnnotation()) {
			foreach(ASTNode m; members; m instanceof Method) {
				try {
					((Method)m).resolveMetaDefaults();
				} catch(Exception e) {
					Kiev.reportError(m,e);
				}
			}
		}
		if( !isPackage() ) {
			for(int i=0; i < sub_clazz.length; i++) {
				if( !sub_clazz[i].isAnonymouse() )
					sub_clazz[i].resolveMetaDefaults();
			}
		}
	}

	public void resolveMetaValues() {
		foreach (Meta m; meta)
			m.resolve();
		foreach(DNode dn; members) {
			if (dn.meta != null) {
				foreach (Meta m; dn.meta)
					m.resolve();
			}
			if (dn instanceof Method) {
				Method meth = (Method)dn;
				foreach (Var p; meth.params) {
					if (p.meta != null) {
						foreach (Meta m; p.meta)
							m.resolve();
					}
				}
			}
		}
		
		if( !isPackage() ) {
			for(int i=0; i < sub_clazz.length; i++) {
				sub_clazz[i].resolveMetaValues();
			}
		}
	}

	public final void preResolve() {
		this.resolveImports();
	}
	
	public final boolean mainResolveIn(TransfProcessor proc) {
		this.resolveFinalFields();
		return !isLocal();
	}
	
	public void mainResolveOut() {
		cleanDFlow();
	}
	
	public void resolveDecl() {
		if( isGenerated() ) return;
		long curr_time;
		autoGenerateStatements();
		if( !isPackage() ) {
			foreach (ASTNode n; members; n instanceof Struct) {
				try {
					Struct ss = (Struct)n;
					ss.resolveDecl();
				} catch(Exception e ) {
					Kiev.reportError(n,e);
				}
			}
		}

		long diff_time = curr_time = System.currentTimeMillis();
		try {
			// Verify access
			foreach(ASTNode n; members; n instanceof Field) {
				Field f = (Field)n;
				try {
					f.type.checkResolved();
					if (f.type.getStruct()!=null)
						f.type.getStruct().acc.verifyReadWriteAccess(this,f.type.getStruct());
				} catch(Exception e ) { Kiev.reportError(n,e); }
			}
			foreach(ASTNode n; members; n instanceof Method) {
				Method m = (Method)n;
				try {
					m.type.ret.checkResolved();
					if (m.type.ret.getStruct()!=null)
						m.type.ret.getStruct().acc.verifyReadWriteAccess(this,m.type.ret.getStruct());
					foreach(Type t; m.type.args) {
						t.checkResolved();
						if (t.getStruct()!=null)
							t.getStruct().acc.verifyReadWriteAccess(this,t.getStruct());
					}
				} catch(Exception e ) { Kiev.reportError(m,e); }
			}

			foreach(DNode n; members; n instanceof Method || n instanceof Initializer) {
				n.resolveDecl();
			}
			
			// Autogenerate hidden args for initializers of local class
			if( isLocal() ) {
				Field[] proxy_fields = Field.emptyArray;
				foreach(ASTNode n; members; n instanceof Field) {
					Field f = (Field)n;
					if( f.isNeedProxy() )
						proxy_fields = (Field[])Arrays.append(proxy_fields,f);
				}
				if( proxy_fields.length > 0 ) {
					foreach(ASTNode n; members; n instanceof Method) {
						Method m = (Method)n;
						if( !m.name.equals(nameInit) ) continue;
						for(int j=0; j < proxy_fields.length; j++) {
							int par = m.params.length;
							KString nm = new KStringBuffer().append(nameVarProxy)
								.append(proxy_fields[j].name).toKString();
							m.params.append(new FormPar(m.pos,nm,proxy_fields[j].type,FormPar.PARAM_LVAR_PROXY,ACC_FINAL));
							((BlockStat)m.body).stats.insert(
								new ExprStat(m.pos,
									new AssignExpr(m.pos,AssignOperator.Assign,
										new IFldExpr(m.pos,new ThisExpr(0),proxy_fields[j]),
										new LVarExpr(m.pos,m.params[par])
									)
								),1
							);
						}
					}
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
		setGenerated(true);
		diff_time = System.currentTimeMillis() - curr_time;
		if( Kiev.verbose ) Kiev.reportInfo("Resolved class "+this,diff_time);
	}
/*
	List<Method> addMethodsToVT(Type tp, List<Method> ms, boolean by_name_name) {
	next_method:
		foreach(ASTNode n; members; n instanceof Method) {
			Method m = (Method)n;
			if (!m.isStatic() && !m.name.equals(nameInit))
				continue;
			for(List<Method> msi = ms; msi != List.Nil; msi = msi.tail()) {
				if( (  (by_name_name && m.name.name.equals(msi.head().name.name))
					|| (!by_name_name && m.name.equals(msi.head().name)) )
//				 && Type.getRealType(tp,m.jtype).equals(Type.getRealType(tp,msi.head().jtype)) ) {
				 && Type.getRealType(tp,m.type).equals(Type.getRealType(tp,msi.head().type)) ) {
					((List.Cons<Method>)msi).head = m;
//					System.out.println("replace from "+this+" method "+m.name+m.type.signature);
					continue next_method;
				}
			}
//			System.out.println("add from "+this+" method "+m.name+m.jtype.signature);
			ms = new List.Cons<Method>(m,ms);
		}
		return ms;
	}

	List<Method> collectVTinterfaceMethods(Type tp, List<Method> ms) {
		if( super_type != null ) {
			ms = super_type.clazz.collectVTinterfaceMethods(
				Type.getRealType(tp,super_type),ms);
		}
		foreach(Type i; interfaces) {
			ms = i.clazz.collectVTinterfaceMethods(
				Type.getRealType(tp,i),ms);
		}
		if( isInterface() ) {
//			System.out.println("collecting in "+this);
			ms = addMethodsToVT(tp,ms,false);
		}
		return ms;
	}

	List<Method> collectVTvirtualMethods(Type tp, List<Method> ms)
	{
		if( super_type != null )
			ms = super_type.clazz.collectVTvirtualMethods(tp,ms);
//		System.out.println("collecting in "+this);
		ms = addMethodsToVT(tp,ms,true);
		return ms;
	}

	List<Method> collectVTmethods(List<Method> ms) {
		ms = collectVTinterfaceMethods(this.type,ms);
		ms = collectVTvirtualMethods(this.type,ms);
		return ms;
	}
*/

	public Dumper toJavaDecl(Dumper dmp) {
		Struct jthis = this;
		if( Kiev.verbose ) System.out.println("[ Dumping class "+jthis+"]");
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( isInterface() || isArgument() ) {
			dmp.append("interface").forsed_space();
			if( isArgument() ) dmp.append("/*argument*/").space();
			dmp.append(jthis.name.short_name.toString()).space();
			if( this.args.length > 0 ) {
				dmp.append("/* <");
				for(int i=0; i < this.args.length; i++) {
					dmp.append(this.args[i]);
					if( i < this.args.length-1 ) dmp.append(',');
				}
				dmp.append("> */");
			}
			if( interfaces!=null && interfaces.length > 0 ) {
				dmp.space().append("extends").forsed_space();
				for(int i=0; i < interfaces.length; i++) {
					dmp.append(interfaces[i]);
					if( i < (interfaces.length-1) ) dmp.append(',').space();
				}
			}
		} else {
			dmp.append("class").forsed_space();
			dmp.append(jthis.name.short_name.toString());
			if( this.args.length > 0 ) {
				dmp.append("/* <");
				for(int i=0; i < this.args.length; i++) {
					dmp.append(this.args[i]);
					if( i < this.args.length-1 ) dmp.append(',');
				}
				dmp.append("> */");
			}
			dmp.forsed_space();
			if( super_type != null && !super_type.equals(Type.tpObject) && super_type.isReference())
				dmp.append("extends").forsed_space().append(jthis.super_type.clazz).forsed_space();
			if( interfaces!=null && interfaces.length > 0 ) {
				dmp.space().append("implements").forsed_space();
				for(int i=0; i < interfaces.length; i++) {
					dmp.append(this.interfaces[i]);
					if( i < (interfaces.length-1) ) dmp.append(',').space();
				}
			}
		}
		dmp.forsed_space().append('{').newLine(1);
		if( !isPackage() ) {
			foreach (ASTNode n; members; n instanceof Struct) {
				Struct s = (Struct)n;
				if( s.isArgument() ) continue;
				s.toJavaDecl(dmp).newLine();
			}
		}
		foreach (ASTNode n; members; n instanceof Field) {
			Field f = (Field)n;
			f.toJavaDecl(dmp).newLine();
		}
		foreach (ASTNode n; members; n instanceof Method) {
			Method m = (Method)n;
			if( m.name.equals(nameClassInit) ) continue;
			m.toJavaDecl(dmp).newLine();
		}
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

	public boolean setBody(ENode body) {
		if( !isPizzaCase() ) return false;
		Method init = (Method)members[0];
		if (init.body != null)
			((BlockStat)init.body).addStatement(body);
		else
			init.setBody(body);
		return true;
	}
}


