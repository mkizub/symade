package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.types.*;
import java.io.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JTypeDeclView;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */


@node(copyable=false)
public class Struct extends TypeDecl implements Named, ScopeOfNames, ScopeOfMethods, ScopeOfOperators, SetBody, Accessable {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="false")	DNode[]		members;
	}

	@node
	public static final class StructImpl extends TypeDeclImpl {
		public StructImpl() {}
		public StructImpl(int pos) { super(pos); }
		public StructImpl(int pos, int fl) { super(pos, fl); }

		public final Struct getStruct() { return (Struct)this._self; }
		
		     public Access						acc;
		     public ClazzName					name;
		     public CompaundTypeProvider		imeta_type;
		     public WrapperTypeProvider			wmeta_type;
		     public OuterTypeProvider			ometa_type;
		     public CompaundType				ctype;
		@att public TypeRef						view_of;
		@att public TypeRef						super_bound;
		@att public NArr<TypeRef>				interfaces;
		@att public NArr<TypeDef>				args;
		@ref public Struct						package_clazz;
		@ref public Struct						typeinfo_clazz;
		@ref public NArr<Struct>				sub_clazz;
		@ref public NArr<DNode>					imported;
		@ref public NArr<TypeDecl>				direct_extenders;
		public kiev.be.java.Attr[]				attrs = kiev.be.java.Attr.emptyArray;
		@att public NArr<DNode>					members;
		     private TypeProvider[]				super_types;

		public void callbackChildChanged(AttrSlot attr) {
			if (attr.name == "args" ||
				attr.name == "super_bound" ||
				attr.name == "interfaces" ||
				attr.name == "package_clazz"
			) {
				this.callbackSuperTypeChanged(this);
			}
		}
		
		public void callbackSuperTypeChanged(TypeDeclImpl chg) {
			super_types = null;
			imeta_type.version++;
			foreach (TypeDecl td; direct_extenders)
				td.callbackSuperTypeChanged(chg);
		}
		
		public TypeProvider[] getAllSuperTypes() {
			if (super_types != null)
				return super_types;
			Vector<TypeProvider> types = new Vector<TypeProvider>();
			addSuperTypes(super_bound, types);
			foreach (TypeRef it; interfaces)
				addSuperTypes(it, types);
			if (types.length == 0)
				super_types = TypeProvider.emptyArray;
			else
				super_types = types.toArray();
			return super_types;
		}
		
		private void addSuperTypes(TypeRef suptr, Vector<TypeProvider> types) {
			Type sup = suptr.getType();
			if (sup == null)
				return;
			TypeProvider tt = sup.getStruct().imeta_type;
			if (!types.contains(tt))
				types.append(tt);
			TypeProvider[] sup_types = sup.getStruct().getAllSuperTypes();
			foreach (TypeProvider t; sup_types) {
				if (!types.contains(t))
					types.append(t);
			}
		}
	}
	@nodeview
	public static final view StructView of StructImpl extends TypeDeclView {
		public				Access					acc;
		public				ClazzName				name;
		public access:ro	CompaundTypeProvider	imeta_type;
		public				WrapperTypeProvider		wmeta_type;
		public				OuterTypeProvider		ometa_type;
		public access:ro	CompaundType			ctype;
		public				TypeRef					view_of;
		public				TypeRef					super_bound;
		public access:ro	NArr<TypeRef>			interfaces;
		public access:ro	NArr<TypeDef>			args;
		public				Struct					package_clazz;
		public				Struct					typeinfo_clazz;
		public access:ro	NArr<Struct>			sub_clazz;
		public access:ro	NArr<DNode>				imported;
		public access:ro	NArr<TypeDecl>			direct_extenders;
		public access:ro	NArr<DNode>				members;

		@setter public final void set$acc(Access val) { this.$view.acc = val; Access.verifyDecl((Struct)getDNode()); }
		@getter public final CompaundType	get$super_type()	{ return (CompaundType)super_bound.lnk; }
		@setter public final void set$super_type(CompaundType tp) { super_bound = new TypeRef(super_bound.pos, tp); }

		public TypeProvider[] getAllSuperTypes() {
			return this.$view.getAllSuperTypes();
		}
		
		public boolean isClazz() {
			return !isPackage() && !isInterface();
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
	public TypeDeclView		getTypeDeclView()	alias operator(210,fy,$cast) { return new StructView((StructImpl)this.$v_impl); }
	public StructView		getStructView()		alias operator(210,fy,$cast) { return new StructView((StructImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		alias operator(210,fy,$cast) { return new JStructView((StructImpl)this.$v_impl); }
	public JDNodeView		getJDNodeView()		alias operator(210,fy,$cast) { return new JStructView((StructImpl)this.$v_impl); }
	public JTypeDeclView	getJTypeDeclView()	alias operator(210,fy,$cast) { return new JStructView((StructImpl)this.$v_impl); }
	public JStructView		getJStructView()	alias operator(210,fy,$cast) { return new JStructView((StructImpl)this.$v_impl); }

	/** Variouse names of the class */
	     public abstract virtual			ClazzName					name;

	/** Type associated with this class */
	     public abstract virtual access:ro	CompaundTypeProvider		imeta_type;
	     public abstract virtual			WrapperTypeProvider			wmeta_type;
	     public abstract virtual			OuterTypeProvider			ometa_type;
	@ref public abstract virtual access:ro	CompaundType				ctype;
	@att public abstract virtual			TypeRef						view_of;

	/** Bound super-class for class arguments */
	@att public abstract virtual			TypeRef						super_bound;

	/** Bound super-class for class arguments */
	@ref public abstract virtual			CompaundType				super_type;

	/** SuperInterface types */
	@att public abstract virtual access:ro	NArr<TypeRef>				interfaces;

	/** Class' type arguments */
	@att public abstract virtual access:ro	NArr<TypeDef>				args;
	
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
	
	@getter public Access					get$acc()					{ return this.getStructView().acc; }
	@getter public ClazzName				get$name()					{ return this.getStructView().name; }
	@getter public CompaundTypeProvider	get$imeta_type()			{ return this.getStructView().imeta_type; }
	@getter public WrapperTypeProvider		get$wmeta_type()			{ return this.getStructView().wmeta_type; }
	@getter public OuterTypeProvider		get$ometa_type()			{ return this.getStructView().ometa_type; }
	@getter public CompaundType				get$ctype()					{ return this.getStructView().ctype; }
	@getter public TypeRef					get$view_of()				{ return this.getStructView().view_of; }
	@getter public TypeRef					get$super_bound()			{ return this.getStructView().super_bound; }
	@getter public NArr<TypeRef>			get$interfaces()			{ return this.getStructView().interfaces; }
	@getter public NArr<TypeDef>			get$args()					{ return this.getStructView().args; }
	@getter public Struct					get$package_clazz()			{ return this.getStructView().package_clazz; }
	@getter public Struct					get$typeinfo_clazz()		{ return this.getStructView().typeinfo_clazz; }
	@getter public NArr<Struct>				get$sub_clazz()				{ return this.getStructView().sub_clazz; }
	@getter public NArr<DNode>				get$imported()				{ return this.getStructView().imported; }
	@getter public NArr<DNode>				get$members()				{ return this.getStructView().members; }
	@getter public CompaundType				get$super_type()			{ return this.getStructView().super_type; }

	@setter public void set$acc(Access val)							{ this.getStructView().acc = val; }
	@setter public void set$name(ClazzName val)						{ this.getStructView().name = val; }
	@setter public void set$wmeta_type(WrapperTypeProvider val)		{ this.getStructView().wmeta_type = val; }
	@setter public void set$ometa_type(OuterTypeProvider val)			{ this.getStructView().ometa_type = val; }
	@setter public void set$view_of(TypeRef val)						{ this.getStructView().view_of = val; }
	@setter public void set$super_bound(TypeRef val)					{ this.getStructView().super_bound = val; }
	@setter public void set$package_clazz(Struct val)					{ this.getStructView().package_clazz = val; }
	@setter public void set$typeinfo_clazz(Struct val)					{ this.getStructView().typeinfo_clazz = val; }
	@setter public void set$super_type(CompaundType val) 				{ this.getStructView().super_type = val; }

	Struct() {
		super(new StructImpl(0,0));
		this.name = ClazzName.Empty;
	}
	
	public Struct(ClazzName name, Struct outer, int acc) {
		super(new StructImpl(0,acc));
		this.name = name;
		((StructImpl)this.$v_impl).imeta_type = new CompaundTypeProvider(this);
		((StructImpl)this.$v_impl).ctype = new CompaundType(this.imeta_type, TVarBld.emptySet);
		this.super_bound = new TypeRef();
		this.meta = new MetaSet();
		package_clazz = outer;
		trace(Kiev.debugCreation,"New clazz created: "+name.short_name	+" as "+name.name+", member of "+outer);
	}

	@getter public Struct get$child_ctx_clazz()	{ return this; }

	public Struct getStruct() { return this; }

	public Object copy() {
		throw new CompilerException(this,"Struct node cannot be copied");
	};

	public String toString() { return name.name.toString(); }

	public TypeProvider[] getAllSuperTypes() {
		return getStructView().getAllSuperTypes();
	}
		
	// normal class
	public boolean isClazz() { return getStructView().isClazz(); }
	// package	
	public boolean isPackage() { return getStructView().isPackage(); }
	public void setPackage() { getStructView().setPackage(); }
	// a pizza class case	
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
	// java annotation
	public boolean isAnnotation() { return getStructView().isAnnotation(); }
	public void setAnnotation(boolean on) { getStructView().setAnnotation(on); }
	// java enum
	public boolean isEnum() { return getStructView().isEnum(); }
	public void setEnum(boolean on) { getStructView().setEnum(on); }
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
		return (MetaPizzaCase)this.getNodeData(MetaPizzaCase.ID);
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
		if (isLocal())
			return dmp.append(name.short_name);
		else
			return dmp.append(name);
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
		if( super_bound.getStruct() != null && super_bound.getStruct().instanceOf(cl) )
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
			info.space_prev == null || (info.space_prev.pslot.name != "super_bound" && info.space_prev.pslot.name != "interfaces"),
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
		TypeDef@ arg;
	{
			this.name.short_name.equals(name), node ?= this
		;	arg @= args,
			arg.name.name.equals(name),
			node ?= arg
		;	node @= members,
			node instanceof TypeDef && ((TypeDef)node).name.name.equals(name)
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
			;	node instanceof TypeDef && ((TypeDef)node).name.name.equals(name)
			}
	}
	protected rule resolveNameR_3(DNode@ node, ResInfo info, KString name)
		TypeRef@ sup_ref;
		Struct@ sup;
	{
			{	sup_ref ?= super_bound,
				sup ?= sup_ref.getStruct(),
				info.enterSuper() : info.leaveSuper(),
				sup.resolveNameR(node,info,name)
			;	sup_ref @= interfaces,
				sup ?= sup_ref.getStruct(),
				info.enterSuper() : info.leaveSuper(),
				sup.resolveNameR(node,info,name)
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

	final public rule resolveMethodR(DNode@ node, ResInfo info, KString name, CallType mt)
	{
		resolveStructMethodR(node, info, name, mt, this.ctype)
	}

	public rule resolveStructMethodR(DNode@ node, ResInfo info, KString name, CallType mt, Type tp)
		ASTNode@ member;
		Type@ sup;
	{
		info.isStaticAllowed(),
		checkResolved(),
		trace(Kiev.debugResolve, "Resolving "+name+" in "+this),
		{
			node @= members,
			node instanceof Method,
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
		CallType mt = new CallType(args,ret);
		DNode@ m;
		if (!this.ctype.resolveCallAccessR(m, new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic), name, mt) &&
			!this.ctype.resolveCallStaticR(m, new ResInfo(this,ResInfo.noForwards|ResInfo.noImports), name, mt))
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
		members.append(m);
		trace(Kiev.debugMembers,"Method "+m+" added to class "+this);
		foreach (ASTNode n; members; n instanceof Method && n != m) {
			Method mm = (Method)n;
			if( mm.equals(m) )
				Kiev.reportError(m,"Method "+m+" already exists in class "+this);
			if (mm.name.equals(m.name) && mm.type.equals(m.type))
				Kiev.reportError(m,"Method "+m+" already exists in class "+this);
		}
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
		if (meta == null)
			cas.addNodeData(meta = new MetaPizzaCase());
		meta.setTag(caseno + 1);
		trace(Kiev.debugMembers,"Class's case "+cas+" added to class "	+this+" as case # "+meta.getTag());
		return cas;
	}

	public Type getType() { return this.ctype; }

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
		addField(f);
		f.resolveDecl();
		// Add initialization in <clinit>
		Constructor class_init = getClazzInitMethod();
		if( ctx_method != null && ctx_method.name.equals(nameClassInit) ) {
			class_init.addstats.append(
				new ExprStat(f.init.getPos(),
					new AssignExpr(f.init.getPos(),AssignOperator.Assign
						,new SFldExpr(f.pos,f),new Shadow(f.init))
				)
			);
		} else {
			class_init.addstats.append(
				new ExprStat(f.init.getPos(),
					new AssignExpr(f.init.getPos(),AssignOperator.Assign
						,new SFldExpr(f.pos,f),new Shadow(f.init))
				)
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
	
	public List<ArgType> getTypeInfoArgs() {
		ListBuffer<ArgType> lb = new ListBuffer<ArgType>();
		TVar[] templ = this.imeta_type.getTemplBindings().tvars;
		foreach (TVar tv; templ; tv.isFree() && tv.var.isUnerasable())
			lb.append(tv.var);
		return lb.toList();
	}

	void autoGenerateTypeinfoClazz() {
		if (typeinfo_clazz != null)
			return;
		if (isInterface() || !isTypeUnerasable())
			return;
		// create typeinfo class
		int flags = this.flags & JAVA_ACC_MASK;
		flags &= ~(ACC_PRIVATE | ACC_PROTECTED);
		flags |= ACC_PUBLIC | ACC_STATIC;
		typeinfo_clazz = Env.newStruct(
			ClazzName.fromOuterAndName(this,nameClTypeInfo,false,true),this,flags,true
			);
		members.add(typeinfo_clazz);
		typeinfo_clazz.setPublic();
		typeinfo_clazz.setResolved(true);
		if (super_type != null && ((Struct)super_type.clazz).typeinfo_clazz != null)
			typeinfo_clazz.super_type = ((Struct)super_type.clazz).typeinfo_clazz.ctype;
		else
			typeinfo_clazz.super_type = Type.tpTypeInfo;
		addSubStruct(typeinfo_clazz);
		typeinfo_clazz.pos = pos;

		// create constructor method
		{
			Constructor init = new Constructor(ACC_PROTECTED);
			init.body = new BlockStat(pos);
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
			Field tif = addField(new Field(nameTypeInfo,typeinfo_clazz.ctype,ACC_PUBLIC|ACC_FINAL));
			// add constructor to the class
			typeinfo_clazz.addMethod(init);
			
			// and add super-constructor call
			init.setNeedFieldInits(true);
			ASTCallExpression call_super = new ASTCallExpression(pos, nameSuper, ENode.emptyArray);
			call_super.args.add(new LVarExpr(pos,init.params[0]));
			call_super.args.add(new LVarExpr(pos,init.params[1]));
			init.body.stats.insert(new ExprStat(call_super),0);
			foreach (ArgType at; super_type.clazz.getTypeInfoArgs()) {
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
			Method tim = addMethod(new Method(nameGetTypeInfo,Type.tpTypeInfo,ACC_PUBLIC | ACC_SYNTHETIC));
			tim.body = new BlockStat(pos,new ENode[]{
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
			init.body = new BlockStat(pos);
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
			init.body.addStatement(new IfElseStat(pos,
				new BinaryBoolExpr(pos,BinaryOperator.Equals,new LVarExpr(pos,v),new ConstNullExpr()),
				new ExprStat(pos,new AssignExpr(pos, AssignOperator.Assign,new LVarExpr(pos,v),ne)),
				null
			));
			init.body.addStatement(new ReturnStat(pos,new LVarExpr(pos,v)));
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
			meq.body = new BlockStat(pos);
			meq.body.addStatement(new IfElseStat(pos,
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
				meq.body.addStatement(new IfElseStat(pos,
					new BinaryBoolExpr(pos,BinaryOperator.NotEquals,
						new IFldExpr(pos,new ThisExpr(pos), f),
						new ContainerAccessExpr(pos, new LVarExpr(pos,meq.params[1]), new ConstIntExpr(idx))
						),
					new ReturnStat(pos,new ConstBoolExpr(false)),
					null
				));
				idx++;
			}
			meq.body.addStatement(new ReturnStat(pos,new ConstBoolExpr(true)));
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
			misa.body = new BlockStat(pos);
			misa.body.addStatement(new IfElseStat(pos,
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
			misa.body.addStatement(new ExprStat(pos,
				new AssignExpr(pos,AssignOperator.Assign,
					new LVarExpr(pos,misa.params[0]),
					new CastExpr(pos,typeinfo_clazz.ctype,new LVarExpr(pos,misa.params[0]))
				)
			));
			foreach (ArgType at; this.getTypeInfoArgs()) {
				Field f = typeinfo_clazz.resolveField(KString.from(nameTypeInfo+"$"+at.name));
				misa.body.addStatement(new IfElseStat(pos,
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
			misa.body.addStatement(new ReturnStat(pos,new ConstBoolExpr(true)));
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
			misa.body = new BlockStat(pos);
			misa.body.addStatement(new IfElseStat(pos,
				new BinaryBoolExpr(pos,BinaryOperator.Equals,
					new LVarExpr(pos,misa.params[0]),
					new ConstNullExpr()
					),
				new ReturnStat(pos,new ConstBoolExpr(false)),
				null
			));
			misa.body.addStatement(new IfElseStat(pos,
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
			misa.body.addStatement(new ReturnStat(pos,
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
	
	public void autoGenerateIdefault() {
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
						ClazzName.fromOuterAndName(this,nameIdefault,false,true),
						this,ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT, true
					);
					members.add(defaults);
					defaults.setResolved(true);
					Kiev.runProcessorsOn(defaults);
				}
				
				if (m.isStatic()) {
					defaults.members.add((Method)~m);
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
				def.body = (BlockStat)~m.body;
				def.setVirtualStatic(true);

				m.setAbstract(true);
			}
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
					package_clazz.checkResolved();
					if( package_clazz.isClazz() && !isStatic() ) {
						// Add formal parameter
						m.params.insert(new FormPar(m.pos,nameThisDollar,package_clazz.ctype,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL),0);
						retype = true;
					}
					if (!isInterface() && isTypeUnerasable()) {
						m.params.insert(new FormPar(m.pos,nameTypeInfo,typeinfo_clazz.ctype,FormPar.PARAM_TYPEINFO,ACC_FINAL),(retype?1:0));
						retype = true;
					}
				}
				if( !init_found ) {
					trace(Kiev.debugResolve,nameInit+" not found in class "+this);
					Constructor init = new Constructor(ACC_PUBLIC);
					if( super_type != null && super_type.clazz == Type.tpClosureClazz ) {
						if( !isStatic() ) {
							init.params.append(new FormPar(pos,nameThisDollar,package_clazz.ctype,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL));
							init.params.append(new FormPar(pos,KString.from("max$args"),Type.tpInt,FormPar.PARAM_NORMAL,0));
						} else {
							init.params.append(new FormPar(pos,KString.from("max$args"),Type.tpInt,FormPar.PARAM_NORMAL,0));
						}
					} else {
						if( package_clazz.isClazz() && !isStatic() ) {
							init.params.append(new FormPar(pos,nameThisDollar,package_clazz.ctype,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL));
						}
						if (!isInterface() && isTypeUnerasable()) {
							init.params.append(new FormPar(pos,nameTypeInfo,typeinfo_clazz.ctype,FormPar.PARAM_TYPEINFO,ACC_FINAL));
						}
						if( isEnum() ) {
							init.params.append(new FormPar(pos,KString.from("name"),Type.tpString,FormPar.PARAM_NORMAL,0));
							init.params.append(new FormPar(pos,nameEnumOrdinal,Type.tpInt,FormPar.PARAM_NORMAL,0));
							//init.params.append(new FormPar(pos,KString.from("text"),Type.tpString,FormPar.PARAM_NORMAL,0));
						}
						if (isStructView()) {
							init.params.append(new FormPar(pos,nameView,view_of.getType(),FormPar.PARAM_NORMAL,ACC_FINAL));
						}
					}
					init.pos = pos;
					init.body = new BlockStat(pos);
					if (isEnum() || isSingleton())
						init.setPrivate();
					else
						init.setPublic();
					addMethod(init);
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
		trace(Kiev.debugMultiMethod,"lookup overwritten methods for "+base+"."+m+" in "+this);
		foreach (ASTNode n; members; n instanceof Method) {
			Method mi = (Method)n;
			if( mi.isStatic() || mi.isPrivate() || mi.name.equals(nameInit) ) continue;
			if( mi.name.name != m.name.name || mi.type.arity != m.type.arity ) {
//				trace(Kiev.debugMultiMethod,"Method "+m+" not matched by "+methods[i]+" in class "+this);
				continue;
			}
			CallType mit = (CallType)Type.getRealType(base,mi.etype);
			if( m.etype.equals(mit) ) {
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
		Constructor class_init = new Constructor(ACC_STATIC);
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
					Method abstr = new Method(m.name.name,m.type.ret(),m.getFlags() | ACC_PUBLIC );
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
					if( super_type != null && super_type.clazz == Type.tpClosureClazz ) {
						ASTIdentifier max_args = new ASTIdentifier();
						max_args.name = nameClosureMaxArgs;
						call_super.args.add(max_args);
					}
					else if( package_clazz.isClazz() && isAnonymouse() ) {
						int skip_args = 0;
						if( !isStatic() ) skip_args++;
						if( this.isTypeUnerasable() && super_type.clazz.isTypeUnerasable() ) skip_args++;
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
					else if( isStructView() && super_type.getStruct().isStructView() ) {
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
				if (isStructView()) {
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
				if (isTypeUnerasable() && m.isNeedFieldInits()) {
					Field tif = resolveField(nameTypeInfo);
					Var v = m.getTypeInfoParam(FormPar.PARAM_TYPEINFO);
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
				this.addMethod(mm);
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
						br = new BlockStat(0,new ENode[]{
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
					br = new BlockStat(0,new ENode[]{
						new ExprStat(0,new CallExpr(0,new ThisExpr(true),overwr,null,vae,true)),
						new ReturnStat(0,null)
					});
				}
				last_st.elseSt = br;
			}
			assert (mm.parent == this);
			if (st != null) {
				BlockStat body = new BlockStat(0);
				body.addStatement(st);
				if (mm.body != null)
					mm.body.stats.insert(0, body);
				else
					mm.body = body;
			}
			multimethods = new List.Cons<Method>(mm, multimethods);
		}

		// Setup java types for methods
//		foreach (ASTNode n; members; n instanceof Method) {
//			Method mi = (Method)n;
//			if( mi.isStatic() || mi.isPrivate() || mi.name.equals(nameInit) ) continue;
//			Method m = null;
//			if( super_type != null )
//				m = super_type.clazz.getOverwrittenMethod(this.ctype,mi);
//			foreach(TypeRef si; interfaces ) {
//				if( m == null )
//					m = si.getStruct().getOverwrittenMethod(this.ctype,mi);
//				else
//					si.getStruct().getOverwrittenMethod(this.ctype,mi);
//			}
//			if( m != null ) {
//				for (int i=0; i < m.params.length; i++) {
//					assert(m.params[i].stype != null);
//					mi.params[i].stype = (TypeRef)m.params[i].stype.copy();
//				}
//				mi.dtype_ret = new TypeRef(m.dtype.ret());
//			}
//		}

	}

	IfElseStat makeDispatchStat(Method mm, MMTree mmt) {
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
						t.getStruct().autoGenerateTypeinfoClazz();
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
		if (dispatched.ctx_clazz == this) {
			assert (dispatched.parent == this);
			return new InlineMethodStat(pos,(Method)~dispatched,dispatcher);
		} else {
			return makeDispatchCall(pos,dispatched,dispatcher);
		}
	}

	private ENode makeDispatchCall(int pos, Method dispatcher, Method dispatched) {
		//return new InlineMethodStat(pos,dispatched,dispatcher)
		ENode obj = null;
		if (!dispatched.isStatic() && !dispatcher.isStatic())
			obj = new ThisExpr(pos);
		CallExpr ce = new CallExpr(pos, obj, dispatched, null, ENode.emptyArray, this != dispatched.ctx_clazz);
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

	// verify resolved tree
	public boolean preVerify() {
		if (isClazz() && super_type != null && super_type.getStruct().isFinal()) {
			Kiev.reportError(this, "Class "+this+" extends final class "+super_type);

		}
		else if (isInterface()) {
			foreach (TypeRef i; interfaces) {
				if(i.getStruct().isFinal())
					Kiev.reportError(this, "Iterface "+this+" extends final interface "+i);
			}
		}
		return true;
	}
	
	public boolean preGenerate() {
		checkResolved();
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
		access:no,ro,ro,rw
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
		if (processed.contains(this))
			return;
		// take vtable from super-types
		if (super_bound.getType() != null) {
			super_bound.getType().getStruct().buildVTable(vtable, processed);
			foreach (TypeRef sup; interfaces)
				sup.getType().getStruct().buildVTable(vtable, processed);
		}
		
		// process override
		foreach (DNode n; members; n instanceof Method && !(n instanceof Constructor)) {
			Method m = (Method)n;
			if (m.isStatic() && !m.isVirtualStatic())
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
				else
					Kiev.reportWarning(this,"Bad method overloading for:\n"+
						"    "+vte1.name+vte1.etype+"\n"+
						"    "+vte2.name+vte2.etype
					);
			}
		}
		// find highest overloader
		foreach (VTableEntry vte; vtable; vte.overloader != null) {
			while (vte.overloader.overloader != null)
				vte.overloader = vte.overloader.overloader;
		}
	}
	
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
			if (m.ctx_clazz == this) {
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
			fp.stype = new TypeRef(fp.stype.getErasedType());
			fp.vtype = new TypeRef(fp.stype.getErasedType());
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
			root.body = new BlockStat(root.pos);
		IfElseStat st = makeDispatchStat(root,mmt);
		if (st != null)
			root.body.stats.insert(0, st);
	}
	
	public void autoProxyMixinMethods(VTableEntry vte) {
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
			m.body = new BlockStat();
			if( m.type.ret() ≡ Type.tpVoid )
				m.body.addStatement(new ExprStat(0,makeDispatchCall(0, m, def)));
			else
				m.body.addStatement(new ReturnStat(0,makeDispatchCall(0, m, def)));
		}
		vte.add(m);
	}

	public void autoBridgeMethods(VTableEntry vte) {
		// get overloader vtable entry
		VTableEntry ovr = vte;
		while (ovr.overloader != null)
			ovr = ovr.overloader;
		// find overloader method
		Method mo = null;
		foreach (Method m; vte.methods) {
			if (m.ctx_clazz == this && m.etype ≈ ovr.etype) {
				mo = m;
				break;
			}
		}
		if (mo == null)
			return; // not overloaded in this class
	next_m:
		foreach (Method m; vte.methods; m != mo) {
			// check this class have no such a method
			foreach (DNode x; this.members; x instanceof Method && x.name.name == m.name.name) {
				if (x.etype ≈ m.etype)
					continue next_m;
			}
			Method bridge = new Method(m.name.name, vte.etype.ret(), ACC_BRIDGE | ACC_SYNTHETIC | mo.flags);
			for (int i=0; i < vte.etype.arity; i++)
				bridge.params.append(new FormPar(mo.pos,m.params[i].name.name,vte.etype.arg(i),FormPar.PARAM_NORMAL,ACC_FINAL));
			bridge.pos = mo.pos;
			members.append(bridge);
			bridge.body = new BlockStat();
			if (bridge.type.ret() ≢ Type.tpVoid)
				bridge.body.stats.append(new ReturnStat(mo.pos,makeDispatchCall(mo.pos, bridge, mo)));
			else
				bridge.body.stats.append(new ExprStat(mo.pos,makeDispatchCall(mo.pos, bridge, mo)));
			vte.add(bridge);
		}
	}

/*	
	public void autoProxyMethods() {
		for(int i=0; i < interfaces.length; i++) {
			interfaces[i].getStruct().autoProxyMethods(this);
		}
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
								mj.setPublic();
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
				int flags = m.getFlags() | ACC_PUBLIC;
				flags &= ~ACC_STATIC;
				Method proxy = new Method(m.name.name,(TypeRef)mi.type_ret.copy(),flags);
				if (proxy.isVirtualStatic())
					proxy.setVirtualStatic(false);
				me.addMethod(proxy);
				proxy.params.copyFrom(m.params);
				BlockStat bs = new BlockStat(0,ENode.emptyArray);
				ENode[] args = new ENode[m.type.args.length];
				args[0] = new ThisExpr();
				for(int k=1; k < args.length; k++)
					args[k] = new LVarExpr(0,proxy.params[k-1]);
				CallExpr ce = new CallExpr(0,null,m,args);
				if( proxy.type.ret() ≡ Type.tpVoid ) {
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
					Method proxy = new Method(m.name.name,m.type.ret(),m.getFlags() | ACC_PUBLIC | ACC_ABSTRACT);
					proxy.params.copyFrom(m.params);
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
*/

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
						Access.verifyReadWrite(this,f.type.getStruct());
				} catch(Exception e ) { Kiev.reportError(n,e); }
			}
			foreach(ASTNode n; members; n instanceof Method) {
				Method m = (Method)n;
				try {
					m.type.ret().checkResolved();
					if (m.type.ret().getStruct()!=null)
						Access.verifyReadWrite(this,m.type.ret().getStruct());
					foreach(Type t; m.type.params()) {
						t.checkResolved();
						if (t.getStruct()!=null)
							Access.verifyReadWrite(this,t.getStruct());
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

	public Dumper toJavaDecl(Dumper dmp) {
		Struct jthis = this;
		if( Kiev.verbose ) System.out.println("[ Dumping class "+jthis+"]");
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( isInterface() ) {
			dmp.append("interface").forsed_space();
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


