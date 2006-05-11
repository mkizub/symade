package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.types.*;
import java.io.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.be.java15.JTypeDecl;
import kiev.be.java15.JStruct;
import kiev.ir.java15.RStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */


@node
public class Struct extends TypeDecl implements ScopeOfNames, ScopeOfMethods, ScopeOfOperators, SetBody, Accessable {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="false")	DNode[]		members;
	}

	@virtual typedef This  = Struct;
	@virtual typedef VView = VStruct;
	@virtual typedef JView = JStruct;
	@virtual typedef RView = RStruct;

		 public Access						acc;
		 public String						q_name;	// qualified name
		 public KString						b_name;	// bytecode name
		 public CompaundMetaType			imeta_type;
		 public WrapperMetaType				wmeta_type;
		 public OuterMetaType				ometa_type;
		 public ASTNodeMetaType				ameta_type;
		 public CompaundType				ctype;
	@att public TypeRef						view_of;
	@att public TypeRef						super_bound;
	@att public NArr<TypeRef>				interfaces;
	@att public NArr<TypeConstr>			args;
	@ref public Struct						package_clazz;
	@ref public Struct						typeinfo_clazz;
	@ref public Struct						iface_impl;
	@ref public NArr<DNode>					sub_decls;
	@ref public NArr<TypeDecl>				direct_extenders;
	public kiev.be.java15.Attr[]			attrs = kiev.be.java15.Attr.emptyArray;
	@att public NArr<ASTNode>				members;
		 private MetaType[]					super_types;

	@getter public final CompaundType	get$super_type()	{ return (CompaundType)super_bound.lnk; }
	@setter public final void set$super_type(CompaundType tp) { super_bound = new TypeRef(super_bound.pos, tp); }

	public void callbackChildChanged(AttrSlot attr) {
		if (attr.name == "args" ||
			attr.name == "super_bound" ||
			attr.name == "interfaces" ||
			attr.name == "package_clazz"
		) {
			this.callbackSuperTypeChanged(this);
		}
		if (attr.name == "id") {
			resetNames();
		}
	}
	
	private void resetNames() {
		q_name = null;
		b_name = null;
		foreach (Struct s; sub_decls)
			s.resetNames(); 
	}
	
	public void callbackSuperTypeChanged(TypeDecl chg) {
		super_types = null;
		imeta_type.version++;
		foreach (TypeDecl td; direct_extenders)
			td.callbackSuperTypeChanged(chg);
	}
	
	public MetaType[] getAllSuperTypes() {
		if (super_types != null)
			return super_types;
		Vector<MetaType> types = new Vector<MetaType>();
		addSuperTypes(super_bound, types);
		foreach (TypeRef it; interfaces)
			addSuperTypes(it, types);
		if (types.length == 0)
			super_types = MetaType.emptyArray;
		else
			super_types = types.toArray();
		return super_types;
	}
	
	public boolean isClazz() {
		return !isPackage() && !isInterface();
	}
	
	// a pizza case	
	public final boolean isPizzaCase() {
		return this.is_struct_pizza_case;
	}
	public final void setPizzaCase(boolean on) {
		if (this.is_struct_pizza_case != on) {
			this.is_struct_pizza_case = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a structure with the only one instance (singleton)	
	public final boolean isSingleton() {
		return this.is_struct_singleton;
	}
	public final void setSingleton(boolean on) {
		if (this.is_struct_singleton != on) {
			this.is_struct_singleton = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a local (in method) class	
	public final boolean isLocal() {
		return this.is_struct_local;
	}
	public final void setLocal(boolean on) {
		if (this.is_struct_local != on) {
			this.is_struct_local = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// an anonymouse (unnamed) class	
	public final boolean isAnonymouse() {
		return this.is_struct_anomymouse;
	}
	public final void setAnonymouse(boolean on) {
		if (this.is_struct_anomymouse != on) {
			this.is_struct_anomymouse = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// has pizza cases
	public final boolean isHasCases() {
		return this.is_struct_has_pizza_cases;
	}
	public final void setHasCases(boolean on) {
		if (this.is_struct_has_pizza_cases != on) {
			this.is_struct_has_pizza_cases = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that structure members were generated
	public final boolean isMembersGenerated() {
		return this.is_struct_members_generated;
	}
	public final void setMembersGenerated(boolean on) {
		if (this.is_struct_members_generated != on) {
			this.is_struct_members_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that structure members were pre-generated
	public final boolean isMembersPreGenerated() {
		return this.is_struct_pre_generated;
	}
	public final void setMembersPreGenerated(boolean on) {
		if (this.is_struct_pre_generated != on) {
			this.is_struct_pre_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	
	// indicates that statements in code were generated
	public final boolean isStatementsGenerated() {
		return this.is_struct_statements_generated;
	}
	public final void setStatementsGenerated(boolean on) {
		if (this.is_struct_statements_generated != on) {
			this.is_struct_statements_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that the structrue was generared (from template)
	public final boolean isGenerated() {
		return this.is_struct_generated;
	}
	public final void setGenerated(boolean on) {
		if (this.is_struct_generated != on) {
			this.is_struct_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that type of the structure was attached
	public final boolean isTypeResolved() {
		return this.is_struct_type_resolved;
	}
	public final void setTypeResolved(boolean on) {
		if (this.is_struct_type_resolved != on) {
			this.is_struct_type_resolved = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that type arguments of the structure were resolved
	public final boolean isArgsResolved() {
		return this.is_struct_args_resolved;
	}
	public final void setArgsResolved(boolean on) {
		if (this.is_struct_args_resolved != on) {
			this.is_struct_args_resolved = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// kiev annotation
	public final boolean isAnnotation() {
		return this.is_struct_annotation;
	}
	public final void setAnnotation(boolean on) {
		assert(!on || (!isPackage() && !isSyntax()));
		if (this.is_struct_annotation != on) {
			this.is_struct_annotation = on;
			if (on) this.setInterface(true);
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// java enum
	public final boolean isEnum() {
		return this.is_struct_enum;
	}
	public final void setEnum(boolean on) {
		if (this.is_struct_enum != on) {
			this.is_struct_enum = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// structure was loaded from bytecode
	public final boolean isLoadedFromBytecode() {
		return this.is_struct_bytecode;
	}
	public final void setLoadedFromBytecode(boolean on) {
		this.is_struct_bytecode = on;
	}

	/** Add information about new sub structure, this class (package) containes */
	public Struct addSubStruct(Struct sub) {
		// Check we already have this sub-class
		for(int i=0; i < sub_decls.length; i++) {
			if( sub_decls[i].equals(sub) ) {
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

		sub_decls.append(sub);

		trace(Kiev.debugMembers,"Sub-class "+sub+" added to class "+this);
		if (sub.id.sname == nameClTypeInfo) {
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
		foreach (Method mm; members; mm != m) {
			if( mm.equals(m) )
				Kiev.reportError(m,"Method "+m+" already exists in class "+this);
			if (mm.id.equals(m.id) && mm.type.equals(m.type))
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
		foreach (Field ff; members) {
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
		foreach (Struct s; members; s.isPizzaCase()) {
			MetaPizzaCase meta = s.getMetaPizzaCase();
			if (meta != null && meta.getTag() > caseno)
				caseno = meta.getTag();
		}
		MetaPizzaCase meta = cas.getMetaPizzaCase();
		if (meta == null)
			cas.addNodeData(meta = new MetaPizzaCase(), MetaPizzaCase.ATTR);
		meta.setTag(caseno + 1);
		trace(Kiev.debugMembers,"Class's case "+cas+" added to class "	+this+" as case # "+meta.getTag());
		return cas;
	}
		
	@getter public Struct get$child_ctx_clazz()	{ return (Struct)this; }

	public boolean instanceOf(Struct cl) {
		if( cl == null ) return false;
		if( this.getStruct().equals(cl) ) return true;
		if( super_bound.getStruct() != null && super_bound.getStruct().instanceOf(cl) )
			return true;
		if( cl.isInterface() ) {
			for(int i=0; i < interfaces.length; i++) {
				if( interfaces[i].getStruct().instanceOf(cl) ) return true;
			}
		}
		return false;
	}

	public Field resolveField(String name) {
		return resolveField(this,name,this,true);
	}

	public Field resolveField(String name, boolean fatal) {
		return resolveField(this,name,this,fatal);
	}

	private static Field resolveField(Struct self, String name, Struct where, boolean fatal) {
		self.getStruct().checkResolved();
		foreach(Field f; self.members; f.id.equals(name) ) return f;
		if( self.super_type != null ) return resolveField(self.super_type.getStruct(),name,where,fatal);
		if (fatal)
			throw new RuntimeException("Unresolved field "+name+" in class "+where);
		return null;
	}

	public Method resolveMethod(String name, Type ret, ...) {
		Type[] args = new Type[va_args.length];
		for (int i=0; i < va_args.length; i++)
			args[i] = (Type)va_args[i];
		CallType mt = new CallType(args,ret);
		Method@ m;
		if (!this.ctype.resolveCallAccessR(m, new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic), name, mt) &&
			!this.ctype.resolveCallStaticR(m, new ResInfo(this,ResInfo.noForwards|ResInfo.noImports), name, mt))
			throw new CompilerException(this,"Unresolved method "+name+mt+" in class "+this);
		return (Method)m;
	}

	public Constructor getClazzInitMethod() {
		foreach(Constructor n; members; n.id.equals(nameClassInit) )
			return n;
		Constructor class_init = new Constructor(ACC_STATIC);
		class_init.pos = pos;
		class_init.setHidden(true);
		addMethod(class_init);
		class_init.body = new Block(pos);
		return class_init;
	}

	@nodeview
	public static final view VStruct of Struct extends VTypeDecl {
		public				Access					acc;
		public:ro			CompaundMetaType		imeta_type;
		public				WrapperMetaType			wmeta_type;
		public				OuterMetaType			ometa_type;
		public:ro			CompaundType			ctype;
		public				TypeRef					view_of;
		public				TypeRef					super_bound;
		public:ro			NArr<TypeRef>			interfaces;
		public:ro			NArr<TypeConstr>		args;
		public				Struct					package_clazz;
		public				Struct					typeinfo_clazz;
		public				Struct					iface_impl;
		public:ro			NArr<DNode>				sub_decls;
		public:ro			NArr<TypeDecl>			direct_extenders;
		public:ro			NArr<ASTNode>			members;

		@getter public final CompaundType	get$super_type();
		@setter public final void set$super_type(CompaundType tp);

		public MetaType[] getAllSuperTypes();

		public final Struct getStruct() { return (Struct)this; }

		public final String qname();
		public boolean isClazz();
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

		public boolean preResolveIn() {
			if (this.isLoadedFromBytecode())
				return false;
			if (parent() instanceof Struct || parent() instanceof FileUnit)
				return true;
			if (ctx_method==null || ctx_method.isStatic())
				this.setStatic(true);
			this.setResolved(true);
			this.setLocal(true);
			this.setLoadedFromBytecode(true);
			try {
				Kiev.runProcessorsOn(this);
			} finally { this.setLoadedFromBytecode(false); }
			return true;
		}

		public final boolean mainResolveIn() {
			resolveFinalFields(this);
			return true; //!isLocal();
		}

		private static void resolveFinalFields(@forward VStruct self) {
			trace(Kiev.debugResolve,"Resolving final fields for class "+qname());
			// Resolve final values of class's fields
			foreach (Field f; members; !f.isMacro()) {
				try {
					f.resolveDecl();
				} catch( Exception e ) {
					Kiev.reportError(f.init,e);
				}
				trace(Kiev.debugResolve && f.init!= null && f.init.isConstantExpr(),
						(f.isStatic()?"Static":"Instance")+" fields: "+qname()+"::"+f.id+" = "+f.init);
			}
			// Process inner classes and cases
			if( !isPackage() ) {
				foreach (Struct sub; sub_decls)
					resolveFinalFields((VStruct)sub);
			}
		}

		public void mainResolveOut() {
			((Struct)this).cleanDFlow();
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

	}

	@getter public Access			get$acc()			{ return this.acc; }
	@setter public void set$acc(Access val)			{ this.acc = val; Access.verifyDecl(this); }

	public final String qname() {
		if (q_name != null)
			return q_name;
		Struct pkg = package_clazz;
		if (pkg == null || pkg == Env.root)
			q_name = id.uname;
		else
			q_name = (pkg.qname()+"."+id.uname).intern();
		return q_name;
	}

	public Struct() {
		this.id = new Symbol(null,"");
		this.q_name = "";
		this.b_name = KString.Empty;
	}
	
	public Struct(Symbol id, Struct outer, int flags) {
		this.flags = flags;
		this.id = id;
		this.imeta_type = new CompaundMetaType(this);
		this.ctype = new CompaundType(this.imeta_type, TVarBld.emptySet);
		this.super_bound = new TypeRef();
		this.meta = new MetaSet();
		package_clazz = outer;
		trace(Kiev.debugCreation,"New clazz created: "+qname() +" as "+id.uname+", member of "+outer);
	}

	public Struct getStruct() { return this; }

	public Object copy() {
		throw new CompilerException(this,"Struct node cannot be copied");
	};

	public Type getType() { return this.ctype; }

	public String toString() { return qname().toString(); }

	public MetaPizzaCase getMetaPizzaCase() {
		return (MetaPizzaCase)this.getNodeData(MetaPizzaCase.ATTR);
	}

	public Field[] getEnumFields() {
		if( !isEnum() )
			throw new RuntimeException("Request for enum fields in non-enum structure "+this);
		int idx = 0;
		foreach (Field n; this.members; n.isEnumField())
			idx++;
		Field[] eflds = new Field[idx];
		idx = 0;
		foreach (Field n; this.members; n.isEnumField()) {
			eflds[idx] = n;
			idx ++;
		}
		return eflds;
	}

	public int getIndexOfEnumField(Field f) {
		if( !isEnum() )
			throw new RuntimeException("Request for enum fields in non-enum structure "+this);
		int idx = 0;
		foreach (Field n; this.members; n.isEnumField()) {
			if (f == n)
				return idx;
			idx++;
		}
		throw new RuntimeException("Enum value for field "+f+" not found in "+this);
	}

	public Dumper toJava(Dumper dmp) {
		if (isLocal())
			return dmp.append(id.uname);
		else
			return dmp.append(qname());
	}
	
	public int countAnonymouseInnerStructs() {
		int i=0;
		foreach(Struct s; sub_decls; s.isAnonymouse() || s.isLocal()) i++;
		return i;
	}

	public int countPackedFields() {
		int i = 0;
		foreach (Field n; members; n.isPackedField()) i++;
		return i;
	}

	public int countAbstractFields() {
		int i = 0;
		foreach (Field n; members; n.isAbstract()) i++;
		return i;
	}

	public final boolean checkResolved() {
		if( !isResolved() ) {
			if (!Env.loadStruct(this).isResolved()) {
				if (isPackage())
					setResolved(true);
				else
					throw new RuntimeException("Class "+this+" not found");
			}
			if (!isResolved())
				throw new RuntimeException("Class "+this+" unresolved");
		}
		return true;
	}
	
	public rule resolveOperatorR(Operator@ op)
		ASTNode@ imp;
	{
		trace( Kiev.debugResolve, "Resolving operator: "+op+" in syntax "+this),
		{
			imp @= members,
			imp instanceof Opdef && ((Opdef)imp).resolved != null,
			op ?= ((Opdef)imp).resolved,
			trace( Kiev.debugResolve, "Resolved operator: "+op+" in syntax "+this)
		;	imp @= members,
			imp instanceof Import && ((Import)imp).mode == Import.ImportMode.IMPORT_SYNTAX,
			((Struct)((Import)imp).resolved).resolveOperatorR(op)
		}
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info, String name)
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
		;	info.isSuperAllowed(),
			info.space_prev == null || (info.space_prev.pslot().name != "super_bound" && info.space_prev.pslot().name != "interfaces"),
			trace(Kiev.debugResolve,"Struct: resolving in super-class of "+this),
			resolveNameR_3(node,info,name), // resolve in super-classes
			$cut
		;	this.isPackage(),
			trace(Kiev.debugResolve,"Struct: trying to load in package "+this),
			tryLoad(node,name),
			$cut
		}
	}
	protected rule resolveNameR_1(ASTNode@ node, ResInfo info, String name)
		TypeDef@ arg;
	{
			this.id.equals(name), node ?= this
		;	arg @= args,
			arg.id.equals(name),
			node ?= arg
		;	node @= members,
			node instanceof TypeDef && ((TypeDef)node).id.equals(name)
		;	node @= members,
			node instanceof Field && ((Field)node).id.equals(name) && info.check(node)
		;	node @= members,
			node instanceof Struct && ((Struct)node).id.equals(name)
		;	isPackage(),
			node @= sub_decls,
			((DNode)node).id.equals(name)
	}
	protected rule resolveNameR_2(ASTNode@ node, ResInfo info, String name)
	{
		node @= members,
		{	node instanceof Field && ((Field)node).isStatic() && ((Field)node).id.equals(name)
		;	node instanceof TypeDef && ((TypeDef)node).id.equals(name)
		}
	}
	protected rule resolveNameR_3(ASTNode@ node, ResInfo info, String name)
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

	public boolean tryLoad(ASTNode@ node, String name) {
		if( isPackage() ) {
			Struct cl;
			String qn = name;
			if (this.equals(Env.root))
				cl = Env.loadStruct(qn);
			else
				cl = Env.loadStruct(qn=(this.qname()+"."+name).intern());
			if( cl != null ) {
				trace(Kiev.debugResolve,"Struct "+cl+" found in "+this);
				node = cl;
				return true;
			} else {
				trace(Kiev.debugResolve,"Class "+qn+" not found in "+this);
			}
		}
		node = null;
		return false;
	}

	final public rule resolveMethodR(Method@ node, ResInfo info, String name, CallType mt)
	{
		resolveStructMethodR(node, info, name, mt, this.ctype)
	}

	public rule resolveStructMethodR(Method@ node, ResInfo info, String name, CallType mt, Type tp)
		ASTNode@ member;
		Type@ sup;
		TypeRef@ supref;
	{
		info.isStaticAllowed(),
		checkResolved(),
		trace(Kiev.debugResolve, "Resolving "+name+" in "+this),
		{
			member @= members,
			member instanceof Method,
			info.check(member),
			node ?= ((Method)member),
			((Method)node).equalsByCast(name,mt,tp,info)
		;	info.isImportsAllowed() && isPackage(),
			member @= members, member instanceof Method,
			node ?= ((Method)member),
			((Method)node).equalsByCast(name,mt,tp,info)
		;	info.isSuperAllowed(),
			sup ?= super_type,
			info.enterSuper() : info.leaveSuper(),
			sup.getStruct().resolveStructMethodR(node,info,name,mt,Type.getRealType(tp,sup))
		;	isInterface(),
			member ?= iface_impl,
			info.enterMode(ResInfo.noSuper) : info.leaveMode(),
			((Struct)member).resolveStructMethodR(node,info,name,mt,Type.getRealType(tp,((Struct)member).ctype))
		;	info.isSuperAllowed(),
			isInterface(),
			supref @= interfaces,
			info.enterSuper() : info.leaveSuper(),
			supref.getStruct().resolveStructMethodR(node,info,name,mt,Type.getRealType(tp,supref.getType()))
		}
	}

	public Field getWrappedField(boolean required) {
		if (super_type != null && super_type.clazz instanceof Struct) {
			Struct ss = (Struct)super_type.clazz;
			Field wf = ss.getWrappedField(false);
			if(wf != null)
				return wf;
		}
		Field wf = null;
		foreach(Field n; members; n.isForward()) {
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

		String oldfn = Kiev.curFile;
		boolean[] old_exts = Kiev.getExtSet();
		{
			ASTNode fu = parent();
			while( fu != null && !(fu instanceof FileUnit))
				fu = fu.parent();
			if( fu != null ) {
				Kiev.curFile = ((FileUnit)fu).id.sname;
				Kiev.setExtSet(((FileUnit)fu).disabled_extensions);
			}
		}

		try {
			((RStruct)this).autoGenerateTypeinfoClazz();
	
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
					if( !(m.id.equals(nameInit) || m.id.equals(nameNewOp)) ) continue;
					if( m.id.equals(nameInit) )
						init_found = true;
					boolean retype = false;
					package_clazz.checkResolved();
					if( package_clazz.isClazz() && !isStatic() ) {
						// Add formal parameter
						m.params.insert(0,new FormPar(m.pos,nameThisDollar,package_clazz.ctype,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
						retype = true;
					}
					if (!isInterface() && isTypeUnerasable()) {
						m.params.insert((retype?1:0),new FormPar(m.pos,nameTypeInfo,typeinfo_clazz.ctype,FormPar.PARAM_TYPEINFO,ACC_FINAL|ACC_SYNTHETIC));
						retype = true;
					}
				}
				if( !init_found ) {
					trace(Kiev.debugResolve,nameInit+" not found in class "+this);
					Constructor init = new Constructor(ACC_PUBLIC);
					init.setHidden(true);
					if( super_type != null && super_type.clazz == Type.tpClosureClazz ) {
						if( !isStatic() ) {
							init.params.append(new FormPar(pos,nameThisDollar,package_clazz.ctype,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
							init.params.append(new FormPar(pos,"max$args",Type.tpInt,FormPar.PARAM_NORMAL,ACC_SYNTHETIC));
						} else {
							init.params.append(new FormPar(pos,"max$args",Type.tpInt,FormPar.PARAM_NORMAL,ACC_SYNTHETIC));
						}
					} else {
						if( package_clazz.isClazz() && !isStatic() ) {
							init.params.append(new FormPar(pos,nameThisDollar,package_clazz.ctype,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
						}
						if (!isInterface() && isTypeUnerasable()) {
							init.params.append(new FormPar(pos,nameTypeInfo,typeinfo_clazz.ctype,FormPar.PARAM_TYPEINFO,ACC_FINAL|ACC_SYNTHETIC));
						}
						if( isEnum() ) {
							init.params.append(new FormPar(pos,"name",Type.tpString,FormPar.PARAM_NORMAL,ACC_SYNTHETIC));
							init.params.append(new FormPar(pos,nameEnumOrdinal,Type.tpInt,FormPar.PARAM_NORMAL,ACC_SYNTHETIC));
							//init.params.append(new FormPar(pos,"text",Type.tpString,FormPar.PARAM_NORMAL,ACC_SYNTHETIC));
						}
						if (isStructView()) {
							init.params.append(new FormPar(pos,nameImpl,view_of.getType(),FormPar.PARAM_NORMAL,ACC_FINAL|ACC_SYNTHETIC));
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
		} finally { Kiev.setExtSet(old_exts); Kiev.curFile = oldfn; }

		setMembersGenerated(true);
		foreach(Struct s; members)
			s.autoGenerateMembers();
	}

	public Method getOverwrittenMethod(Type base, Method m) {
		Method mm = null, mmret = null;
		if( super_type != null && !isInterface() )
			mm = super_type.clazz.getOverwrittenMethod(base,m);
		if( mmret == null && mm != null ) mmret = mm;
		trace(Kiev.debugMultiMethod,"lookup overwritten methods for "+base+"."+m+" in "+this);
		foreach (Method mi; members) {
			if( mi.isStatic() || mi.isPrivate() || mi.id.equals(nameInit) ) continue;
			if( mi.id.uname != m.id.uname || mi.type.arity != m.type.arity ) {
//				trace(Kiev.debugMultiMethod,"Method "+m+" not matched by "+methods[i]+" in class "+this);
				continue;
			}
			CallType mit = (CallType)Type.getRealType(base,mi.etype);
			if( m.etype.equals(mit) ) {
				trace(Kiev.debugMultiMethod,"Method "+m+" overrides "+mi+" of type "+mit+" in class "+this);
				mm = mi;
				// Append constraints to m from mm
				foreach(WBCCondition cond; mm.conditions; m.conditions.indexOf(cond) < 0)
					m.conditions.add(cond);
				if( mmret == null && mm != null ) mmret = mm;
				break;
			} else {
				trace(Kiev.debugMultiMethod,"Method "+m+" does not overrides "+mi+" of type "+mit+" in class "+this);
			}
		}
		return mmret;
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

	public void resolveMetaDefaults() {
		if (isAnnotation()) {
			foreach(Method m; members) {
				try {
					m.resolveMetaDefaults();
				} catch(Exception e) {
					Kiev.reportError(m,e);
				}
			}
		}
		if( !isPackage() ) {
			foreach (Struct sub; sub_decls) {
				if (!sub.isAnonymouse())
					sub.resolveMetaDefaults();
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
			foreach (Struct sub; sub_decls) {
				sub.resolveMetaValues();
			}
		}
	}
	
	public Dumper toJavaDecl(Dumper dmp) {
		Struct jthis = this;
		if( Kiev.verbose ) System.out.println("[ Dumping class "+jthis+"]");
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( isInterface() ) {
			dmp.append("interface").forsed_space();
			dmp.append(jthis.id.toString()).space();
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
			dmp.append(jthis.id.toString());
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
			foreach (Struct s; members) {
				s.toJavaDecl(dmp).newLine();
			}
		}
		foreach (Field f; members) {
			f.toJavaDecl(dmp).newLine();
		}
		foreach (Method m; members) {
			if( m.id.equals(nameClassInit) ) continue;
			m.toJavaDecl(dmp).newLine();
		}
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

	public boolean setBody(ENode body) {
		if( !isPizzaCase() ) return false;
		Method init = (Method)members[0];
		if (init.body != null)
			init.body.stats.add(body);
		else
			init.setBody(body);
		return true;
	}
}


