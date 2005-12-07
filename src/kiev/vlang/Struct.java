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
import kiev.transf.*;

import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */


@node(copyable=false)
@dflow(in="root()")
public class Struct extends TypeDef implements Named, ScopeOfNames, ScopeOfMethods, ScopeOfOperators, SetBody, Accessable {

	/** Variouse names of the class */
	public ClazzName								name;

	/** Type associated with this class */
	@ref public BaseType							type;

	/** Bound super-class for class arguments */
	@att public TypeRef								super_bound;

	/** Bound super-class for class arguments */
	@virtual
	@ref public virtual abstract BaseType			super_type;

	/** SuperInterface types */
	@att public final NArr<TypeRef>					interfaces;

	/** Class' type arguments */
	@att public final NArr<TypeArgDef>				args;
	
	/** Class' access */
	@virtual
	public virtual Access							acc;

	/** Package structure this structure belongs to */
	@ref public Struct								package_clazz;

	/** The auto-generated class for parametriezed
	  classes, that containce type info
	 */
	@ref public Struct								typeinfo_clazz;
	
	/** Array of substructures of the structure */
	@ref public final NArr<Struct>					sub_clazz;

	/** Array of imported classes,fields and methods */
	@ref public final NArr<DNode>					imported;

	/** Array of attributes of this structure */
	public Attr[]									attrs = Attr.emptyArray;
	
	/** Array of methods defined in this structure */
	@att
	@dflow(in="", seq="false")
	public final NArr<DNode>						members;
	
	/** JClass for java backend */
	//@ref public kiev.backend.java15.JClass			jclass;

	protected Struct(ClazzName name) {
		super(0,0);
		this.name = name;
		this.super_bound = new TypeRef();
		this.meta = new MetaSet();
		this.acc = new Access(0);
	}

	public Struct(ClazzName name, Struct outer, int acc) {
		super(0,acc);
		this.name = name;
		this.super_bound = new TypeRef();
		this.meta = new MetaSet();
		package_clazz = outer;
		this.acc = new Access(0);
		trace(Kiev.debugCreation,"New clazz created: "+name.short_name
			+" as "+name.name+", member of "+outer);
	}

	public void setupContext() {
		if (this.parent == null)
			this.pctx = new NodeContext(this).enter(this);
		else
			this.pctx = this.parent.pctx.enter(this);
	}

	public Object copy() {
		throw new CompilerException(this,"Struct node cannot be copied");
	};

	public String toString() { return name.name.toString(); }

	@getter public Access get$acc() {
		return acc;
	}

	@setter public void set$acc(Access a) {
		acc = a;
		acc.verifyAccessDecl(this);
	}
	
	@getter public BaseType get$super_type() {
		return (BaseType)super_bound.lnk;
	}

	@setter public void set$super_type(BaseType tp) {
		super_bound = new TypeRef(super_bound.pos, tp);
	}

	//
	// Struct specific
	//
	public boolean isClazz()		{
		return !isPackage() && !isInterface() && ! isArgument();
	}
	
	// package	
	@getter public final boolean get$is_struct_package()  alias isPackage  {
		return this.is_struct_package;
	}
	@setter public final void set$is_struct_package(boolean on) alias setPackage {
		assert(!on || (!isInterface() && ! isEnum() && !isSyntax()));
		if (this.is_struct_package != on) {
			this.is_struct_package = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a class's argument	
	@getter public final boolean get$is_struct_argument()  alias isArgument  {
		return this.is_struct_argument;
	}
	@setter public final void set$is_struct_argument(boolean on) alias setArgument {
		if (this.is_struct_argument != on) {
			this.is_struct_argument = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a class's argument	
	@getter public final boolean get$is_struct_pizza_case()  alias isPizzaCase  {
		return this.is_struct_pizza_case;
	}
	@setter public final void set$is_struct_pizza_case(boolean on) alias setPizzaCase {
		if (this.is_struct_pizza_case != on) {
			this.is_struct_pizza_case = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a local (in method) class	
	@getter public final boolean get$is_struct_local()  alias isLocal  {
		return this.is_struct_local;
	}
	@setter public final void set$is_struct_local(boolean on) alias setLocal {
		if (this.is_struct_local != on) {
			this.is_struct_local = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// an anonymouse (unnamed) class	
	@getter public final boolean get$is_struct_anomymouse()  alias isAnonymouse  {
		return this.is_struct_anomymouse;
	}
	@setter public final void set$is_struct_anomymouse(boolean on) alias setAnonymouse {
		if (this.is_struct_anomymouse != on) {
			this.is_struct_anomymouse = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// has pizza cases
	@getter public final boolean get$is_struct_has_pizza_cases()  alias isHasCases  {
		return this.is_struct_has_pizza_cases;
	}
	@setter public final void set$is_struct_has_pizza_cases(boolean on) alias setHasCases {
		if (this.is_struct_has_pizza_cases != on) {
			this.is_struct_has_pizza_cases = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// verified
	@getter public final boolean get$is_struct_verified()  alias isVerified  {
		return this.is_struct_verified;
	}
	@setter public final void set$is_struct_verified(boolean on) alias setVerified {
		if (this.is_struct_verified != on) {
			this.is_struct_verified = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that structure members were generated
	@getter public final boolean get$is_struct_members_generated()  alias isMembersGenerated  {
		return this.is_struct_members_generated;
	}
	@setter public final void set$is_struct_members_generated(boolean on) alias setMembersGenerated {
		if (this.is_struct_members_generated != on) {
			this.is_struct_members_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that structure members were pre-generated
	@getter public final boolean get$is_struct_pre_generated()  alias isMembersPreGenerated  {
		return this.is_struct_pre_generated;
	}
	@setter public final void set$is_struct_pre_generated(boolean on) alias setMembersPreGenerated {
		if (this.is_struct_pre_generated != on) {
			this.is_struct_pre_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	
	// indicates that statements in code were generated
	@getter public final boolean get$is_struct_statements_generated()  alias isStatementsGenerated  {
		return this.is_struct_statements_generated;
	}
	@setter public final void set$is_struct_statements_generated(boolean on) alias setStatementsGenerated {
		if (this.is_struct_statements_generated != on) {
			this.is_struct_statements_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that the structrue was generared (from template)
	@getter public final boolean get$is_struct_generated()  alias isGenerated  {
		return this.is_struct_generated;
	}
	@setter public final void set$is_struct_generated(boolean on) alias setGenerated {
		if (this.is_struct_generated != on) {
			this.is_struct_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// kiev annotation
	@getter public final boolean get$is_struct_annotation()  alias isAnnotation  {
		return this.is_struct_annotation;
	}
	@setter public final void set$is_struct_annotation(boolean on) alias setAnnotation {
		assert(!on || (!isPackage() && !isSyntax()));
		if (this.is_struct_annotation != on) {
			this.is_struct_annotation = on;
			if (on) this.setInterface(true);
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// java enum
	@getter public final boolean get$is_struct_enum()  alias isEnum {
		return this.is_struct_enum;
	}
	@setter public final void set$is_struct_enum(boolean on) alias setEnum {
		if (this.is_struct_enum != on) {
			this.is_struct_enum = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// kiev syntax
	@getter public final boolean get$is_struct_syntax()  alias isSyntax  {
		return this.is_struct_syntax;
	}
	@setter public final void set$is_struct_syntax(boolean on) alias setSyntax {
		assert(!on || (!isPackage() && ! isEnum()));
		if (this.is_struct_syntax != on) {
			this.is_struct_syntax = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// structure was loaded from bytecode
	@getter public final boolean get$is_struct_bytecode()  alias isLoadedFromBytecode  {
		return this.is_struct_bytecode;
	}
	@setter public final void set$is_struct_bytecode(boolean on) alias setLoadedFromBytecode {
		this.is_struct_bytecode = on;
	}

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

	public void callbackChildChanged(AttrSlot attr) {
		if (attr.name == "members") {
			if (type != null)
				type.invalidate();
		}
		else if (attr.name == "args") {
			if (type != null)
				type.invalidate();
		}
		if (attr.name == "super_bound") {
			if (type != null)
				type.invalidate();
		}
		else if (attr.name == "interfaces") {
			if (type != null)
				type.invalidate();
		}
		else if (attr.name == "meta") {
			if (type != null)
				type.invalidate();
		}
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
		foreach(ASTNode f; members; f instanceof Field && ((Field)f).name.equals(name) ) return (Field)f;
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

	public Method resolveMethod(KString name, KString sign) {
		return resolveMethod(name,sign,this,true);
	}

	public Method resolveMethod(KString name, KString sign, boolean fatal) {
		return resolveMethod(name,sign,this,fatal);
	}

	private Method resolveMethod(KString name, KString sign, Struct where, boolean fatal) {
		checkResolved();
		foreach (ASTNode n; members; n instanceof Method) {
			Method m = (Method)n;
			if( m.name.equals(name) && m.type.signature.equals(sign))
				return m;
		}
		if( isInterface() ) {
			Struct defaults = null;
			foreach(ASTNode n; members; n instanceof Struct && ((Struct)n).isClazz() && ((Struct)n).name.short_name.equals(nameIdefault) ) {
				defaults = (Struct)n;
				break;
			}
			if( defaults != null ) {
				foreach (ASTNode n; defaults.members; n instanceof Method) {
					Method m = (Method)n;
					if( m.name.equals(name) && m.type.signature.equals(sign))
						return m;
				}
			}
		}
		trace(Kiev.debugResolve,"Method "+name+" with signature "
			+sign+" unresolved in class "+this);
		Method m = null;
		if( super_type != null )
			m = super_type.clazz.resolveMethod(name,sign,where,fatal);
		if( m != null ) return m;
		foreach(TypeRef interf; interfaces) {
			m = interf.getStruct().resolveMethod(name,sign,where,fatal);
			if( m != null ) return m;
		}
		if (fatal)
			throw new RuntimeException("Unresolved method "+name+sign+" in class "+where);
		return null;
	}

	/** Add information about new attribute that belongs to this class */
	public Attr addAttr(Attr a) {
		// Check we already have this attribute
//		if( !(a.name==attrOperator || a.name==attrImport
//			|| a.name==attrRequire || a.name==attrEnsure) ) {
			for(int i=0; i < attrs.length; i++) {
				if(attrs[i].name == a.name) {
					attrs[i] = a;
					return a;
				}
			}
//		}
		attrs = (Attr[])Arrays.append(attrs,a);
		return a;
	}

	public Attr getAttr(KString name) {
		if( attrs != null )
			for(int i=0; i < attrs.length; i++)
				if( attrs[i].name.equals(name) )
					return attrs[i];
		return null;
	}

	/** Add information about new sub structure, this class (package)
		containes
	 */
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
//		PizzaCaseAttr case_attr = null;
		foreach (DNode n; members; n instanceof Struct && ((Struct)n).isPizzaCase()) {
			Struct s = (Struct)n;
//			case_attr = (PizzaCaseAttr)s.getAttr(attrPizzaCase);
//			if( case_attr!=null && case_attr.caseno > caseno )
//				caseno = case_attr.caseno;
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
//		case_attr = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
//		if( case_attr == null ) {
//			case_attr = new PizzaCaseAttr();
//			cas.addAttr(case_attr);
//		}
//		case_attr.caseno = caseno + 1;
//		trace(Kiev.debugMembers,"Class's case "+cas+" added to class "
//			+this+" as case # "+case_attr.caseno);
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
		if( t.isArray() ) {
			t = t.getJavaType();
			return "["+t.args[0];
		}
		if( t instanceof ClosureType ) {
			return "kiev.stdlib.closure";
		}
		if( t.isArgument() ) {
			return makeTypeInfoString(t.getSuperType());
		}
		if( t.args.length > 0 ) {
			StringBuffer sb = new StringBuffer(128);
			sb.append(t.getClazzName().bytecode_name.toString().replace('/','.'));
			sb.append('<');
			for(int i=0; i < t.args.length; i++) {
				Type ta = t.args[i];
				sb.append(makeTypeInfoString(ta));
				if( i < t.args.length-1 )
					sb.append(',');
			}
			sb.append('>');
			return sb.toString();
		} else {
			return t.getClazzName().bytecode_name.toString().replace('/','.');
		}
	}

	public ENode accessTypeInfoField(ASTNode from, Type t) {
		if( t.isArgumented() ) {
			ENode ti_access;
			if( from.pctx.method == null || from.pctx.method.isStatic()) {
				// check we have $typeinfo as first argument
				if( from.pctx.method==null
				 || from.pctx.method.params.length < 1
				 || from.pctx.method.params[0].name.name != nameTypeInfo
				 || !from.pctx.method.params[0].type.isInstanceOf(Type.tpTypeInfo)
				 )
					throw new CompilerException(from,"$typeinfo cannot be accessed from "+from.pctx.method);
				ti_access = new LVarExpr(from.pos,from.pctx.method.params[0]);
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
				KString fnm = new KStringBuffer(nameTypeInfo.length()+1+t.getClazzName().short_name.length())
						.append(nameTypeInfo).append('$').append(t.getClazzName().short_name).toKString();
				Field ti_arg = typeinfo_clazz.resolveField(fnm);
				if (ti_arg == null)
					throw new RuntimeException("Field "+fnm+" not found in "+typeinfo_clazz+" from method "+from.pctx.method);
				ti_access = new IFldExpr(from.pos,ti_access,ti_arg);
				return ti_access;
			}
		}

		KString ts = KString.from(makeTypeInfoString(t));

		// Special case for interfaces, that cannot have private fields,
		// but need typeinfo in <clinit>
		if ((from.pctx.method == null || from.pctx.method.name.name == nameClassInit) && from.pctx.clazz.isInterface()) {
			BaseType ftype = Type.tpTypeInfo;
			if (t.args.length > 0) {
				if (t.getStruct().typeinfo_clazz == null)
					t.getStruct().autoGenerateTypeinfoClazz();
				ftype = t.getStruct().typeinfo_clazz.type;
			}
			ENode[] ti_args = new ENode[]{new ConstStringExpr(ts)};
			ENode e = new CastExpr(from.pos,ftype,new CallExpr(from.pos,null,
					Type.tpTypeInfo.clazz.resolveMethod(
						KString.from("newTypeInfo"),
						KString.from("(Ljava/lang/String;)Lkiev/stdlib/TypeInfo;")
					), ti_args));
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
		if (t.args.length > 0) {
			if (t.getStruct().typeinfo_clazz == null)
				t.getStruct().autoGenerateTypeinfoClazz();
			ftype = t.getStruct().typeinfo_clazz.type;
		}
		Field f = new Field(KString.from(nameTypeInfo+"$"+i),ftype,ACC_STATIC|ACC_FINAL); // package-private for inner classes
		ENode[] ti_args = new ENode[]{new ConstStringExpr(ts)};
		f.init = new CastExpr(from.pos,ftype,new CallExpr(from.pos,null,
				Type.tpTypeInfo.clazz.resolveMethod(
					KString.from("newTypeInfo"),
					KString.from("(Ljava/lang/String;)Lkiev/stdlib/TypeInfo;")
				), ti_args));
		addField(f);
		// Add initialization in <clinit>
		Constructor class_init = getClazzInitMethod();
		if( from.pctx.method != null && from.pctx.method.name.equals(nameClassInit) ) {
			class_init.addstats.insert(
				new ExprStat(f.init.getPos(),
					new AssignExpr(f.init.getPos(),AssignOperator.Assign
						,new SFldExpr(f.pos,f),new ShadowExpr(f.init))
				),0
			);
		} else {
			class_init.addstats.insert(
				new ExprStat(f.init.getPos(),
					new AssignExpr(f.init.getPos(),AssignOperator.Assign
						,new SFldExpr(f.pos,f),new ShadowExpr(f.init))
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
		if( !isInterface() && type.args.length > 0 && !(type instanceof ClosureType) ) {
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
			typeinfo_clazz.type = Type.newJavaRefType(typeinfo_clazz);
			addSubStruct(typeinfo_clazz);
			typeinfo_clazz.pos = pos;

			// add in it arguments fields, and prepare for constructor
			MethodType ti_init;
			Type[] ti_init_targs = new Type[this.type.args.length];
			FormPar[] ti_init_params = new FormPar[]{};
			ENode[] stats = new ENode[type.args.length];
			for (int arg=0; arg < type.args.length; arg++) {
				Type t = type.args[arg];
				KString fname = new KStringBuffer(nameTypeInfo.length()+1+t.getClazzName().short_name.length())
					.append(nameTypeInfo).append('$').append(t.getClazzName().short_name).toKString();
				Field f = new Field(fname,Type.tpTypeInfo,ACC_PUBLIC|ACC_FINAL);
				typeinfo_clazz.addField(f);
				ti_init_targs[arg] = Type.tpTypeInfo;
				FormPar v = new FormPar(pos,t.getClazzName().short_name,Type.tpTypeInfo,FormPar.PARAM_NORMAL,ACC_FINAL);
				ti_init_params = (FormPar[])Arrays.append(ti_init_params,v);
				stats[arg] = new ExprStat(pos,
					new AssignExpr(pos,AssignOperator.Assign,
						new IFldExpr(pos,new ThisExpr(pos),f),
						new LVarExpr(pos,v)
					)
				);
			}
			BlockStat ti_init_body = new BlockStat(pos,stats);

			// create typeinfo field
			Field tif = addField(new Field(nameTypeInfo,typeinfo_clazz.type,ACC_PUBLIC|ACC_FINAL));

			// create constructor method
			ti_init = MethodType.newMethodType(null,ti_init_targs,Type.tpVoid);
			Constructor init = new Constructor(ti_init,ACC_PUBLIC);
			init.params.addAll(ti_init_params);
			typeinfo_clazz.addMethod(init);
			init.body = ti_init_body;
			// and add super-constructor call
			if (typeinfo_clazz.super_type == Type.tpTypeInfo) {
				//do nothing, default constructor may be added later
			} else {
				init.setNeedFieldInits(true);
				ASTCallExpression call_super = new ASTCallExpression();
				call_super.pos = pos;
				call_super.func = new NameRef(pos, nameSuper);
				ENode[] exprs = new ENode[super_type.args.length];
				for (int arg=0; arg < super_type.args.length; arg++) {
					Type t = super_type.args[arg];
					t = Type.getRealType(this.type,t);
					if (t.isArgumented()) {
						exprs[arg] = new ASTIdentifier(pos,t.getClazzName().short_name);
					} else {
						CallExpr ce = new CallExpr(pos,null,
							Type.tpTypeInfo.clazz.resolveMethod(
								KString.from("newTypeInfo"),
								KString.from("(Ljava/lang/String;)Lkiev/stdlib/TypeInfo;")
							),
							new ENode[]{new ConstStringExpr(KString.from(makeTypeInfoString(t)))}
						);
						//ce.type_of_static = this.type;
						exprs[arg] = ce;
					}
				}
				foreach (ENode e; exprs)
					call_super.args.add(e);
				ti_init_body.stats.insert(new ExprStat(call_super),0);
			}

			// create method to get typeinfo field
			MethodType tim_type = MethodType.newMethodType(null,Type.emptyArray,Type.tpTypeInfo);
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
				for(Struct pkg=package_clazz;
						pkg.isClazz() && !pkg.isStatic();
							pkg=pkg.package_clazz) n++;
				Field f = addField(new Field(
					KString.from(nameThisDollar.toString()+(n-1)),package_clazz.type,ACC_FORWARD|ACC_FINAL));
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
					if( !isInterface() && type.args.length > 0 && !(type instanceof ClosureType) ) {
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
							mt = MethodType.newMethodType(new Type[]{Type.tpInt},Type.tpVoid);
							init = new Constructor(mt,ACC_PUBLIC);
							init.params.append(new FormPar(pos,nameThisDollar,package_clazz.type,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL));
							init.params.append(new FormPar(pos,KString.from("max$args"),Type.tpInt,FormPar.PARAM_NORMAL,0));
						} else {
							mt = MethodType.newMethodType(new Type[]{Type.tpInt},Type.tpVoid);
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
						if( !isInterface() && type.args.length > 0 && !(type instanceof ClosureType) ) {
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
						mt = MethodType.newMethodType(targs,Type.tpVoid);
						init = new Constructor(mt,ACC_PUBLIC);
						init.params.addAll(params);
					}
					init.pos = pos;
					init.body = new BlockStat(pos);
					if( isEnum() )
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
							SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
							defaults.addAttr(sfa);
							Type[] tarr = type.args;
							defaults.type = Type.newRefType(defaults, tarr);
							defaults.super_type = Type.tpObject;
							//defaults.interfaces.add(new TypeRef(this.type));
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
			if( !mi.name.equals(m.name) || mi.type.args.length != m.type.args.length ) {
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
		Constructor class_init = new Constructor(MethodType.newMethodType(null,null,Type.tpVoid),ACC_STATIC);
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
				if( f.init.isConstantExpr() && f.init.getConstValue() == null )
					continue;
				if( f.isStatic() && f.init.isConstantExpr() ) {
					// Attribute may already be assigned (see resolveFinalFields)
					if( f.getAttr(attrConstantValue) == null ) {
						ConstantValueAttr a;
						if( f.init instanceof ConstExpr )
							a = new ConstantValueAttr((ConstExpr)f.init);
						else
							a = new ConstantValueAttr(ConstExpr.fromConst(f.init.getConstValue()));
						f.addAttr(a);
					}
					continue;
				}
				if( f.isStatic() ) {
					if( class_init == null )
						class_init = getClazzInitMethod();
					class_init.body.addStatement(
						new ExprStat(f.init.getPos(),
							new AssignExpr(f.init.getPos(),
								f.isInitWrapper() ? AssignOperator.Assign2 : AssignOperator.Assign,
								new SFldExpr(f.pos,f),new ShadowExpr(f.init)
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
								new ShadowExpr(f.init)
							)
						);
					instance_init.body.addStatement(init_stat);
					init_stat.setHidden(true);
				}
			} else {
				Initializer init = (Initializer)n;
				ENode init_stat = new InitializerShadow(init);
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
					abstr.params = m.params;

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
						ASTNode ce = es.expr;
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
								if( nm.equals(nameSuper) || (nm.equals(nameInit) && ((CallExpr)es.expr).super_flag) )
									m.setNeedFieldInits(true);
//								// autoinsert typeinfo if super class needs
//								if( super_type.args.length > 0 ) {
//									CallExpr cae = (CallExpr)es.expr;
//									// Insert our-generated typeinfo, or from childs class?
//									if( m.type.args.length > 0 && m.type.args[0].isInstanceOf(typeinfo_clazz.type) ) {
//										if (!(cae.args[0] instanceof LVarExpr) || ((LVarExpr)cae.args[0]).getVar() != m.params[0])
//										cae.args.insert(0,new LVarExpr(cae.pos,m.params[0]));
//									} else {
//										throw new RuntimeException("Don't know where to get "+typeinfo_clazz.type+" $typeinfo");
//									}
//								}
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
						if( this.type.args.length > 0 && super_type.args.length == 0 ) skip_args++;
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
				if( type.args.length > 0
				 && !(type instanceof ClosureType)
				 && m.isNeedFieldInits()
				) {
					Field tif = resolveField(nameTypeInfo);
					Var v = null;
					foreach(Var vv; m.params; vv.name.equals(nameTypeInfo) ) {
						v = vv;
						break;
					}
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
				if( !mj.name.equals(m.name) || dtype2.args.length != dtype1.args.length )
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
				if( m.type.ret != Type.tpVoid ) {
					if( overwr.type.ret == Type.tpVoid )
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
				if( t.args.length > 0 && !t.isArray() && !(t instanceof ClosureType) ) {
					if (t.getStruct().typeinfo_clazz == null)
						t.getStruct().autoGenerateTypeinfoClazz();
					ENode tibe = new CallExpr(pos,
						accessTypeInfoField(mmt.m,t),
						Type.tpTypeInfo.clazz.resolveMethod(
							KString.from("$instanceof"),KString.from("(Ljava/lang/Object;Lkiev/stdlib/TypeInfo;)Z")),
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

		for(int i=0; i < interfaces.length; i++) {
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
				if( proxy.type.ret == Type.tpVoid ) {
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

	public ASTNode resolveFinalFields(boolean cleanup) {
		trace(Kiev.debugResolve,"Resolving final fields for class "+name);
		// Resolve final values of class's fields
		foreach (ASTNode n; members; n instanceof Field) {
			Field f = (Field)n;
			if( f == null || f.init == null ) continue;
			if( /*f.isStatic() &&*/ f.init != null ) {
				try {
					f.init.resolve(f.type);
					if (f.init instanceof TypeRef)
						((TypeRef)f.init).toExpr(f.type);
					if (f.init.getType() != f.type) {
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
			if( cleanup && !f.isFinal() && f.init!=null && !f.init.isConstantExpr() ) {
				f.init = null;
			}
			if( f.isStatic() && f.init!=null && f.init.isConstantExpr() && f.init.getConstValue()!=null ) {
				if( f.getAttr(attrConstantValue) != null )
					f.addAttr(new ConstantValueAttr(ConstExpr.fromConst(f.init.getConstValue()) ));
			}
		}
		// Process inner classes and cases
		if( !isPackage() ) {
			for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
				sub_clazz[i].resolveFinalFields(cleanup);
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
		this.resolveFinalFields(false);
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
					if (f.type.isStruct())
						f.type.getStruct().acc.verifyReadWriteAccess(this,f.type.getStruct());
				} catch(Exception e ) { Kiev.reportError(n,e); }
			}
			foreach(ASTNode n; members; n instanceof Method) {
				Method m = (Method)n;
				try {
					m.type.ret.checkResolved();
					if (m.type.ret.isStruct())
						m.type.ret.getStruct().acc.verifyReadWriteAccess(this,m.type.ret.getStruct());
					foreach(Type t; m.type.args) {
						t.checkResolved();
						if (t.isStruct())
							t.getStruct().acc.verifyReadWriteAccess(this,t.getStruct());
					}
				} catch(Exception e ) { Kiev.reportError(m,e); }
			}

			foreach(DNode n; members; n instanceof Method || n instanceof Initializer) {
				n.resolveDecl();
			}
			
//			if( type.args != null && type.args.length > 0 && !(type instanceof ClosureType) ) {
//				ClassArgumentsAttr a = new ClassArgumentsAttr();
//				short[] argno = new short[type.args.length];
//				for(int j=0; j < type.args.length; j++) {
//					argno[j] = (short)j; // ((Argument)type.args[j].clazz).argno;
//				}
//				a.args = type.args;
//				a.argno = argno;
//				addAttr(a);
//			}
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

	public void generate() {
		Struct jthis = this;
		//if( Kiev.verbose ) System.out.println("[ Generating cls "+jthis+"]");
		if( Kiev.safe && isBad() ) return;
		if( !isPackage() ) {
			for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
				Struct s = sub_clazz[i];
				s.generate();
			}
		}

		// Check all methods
//			if( !isAbstract() && isClazz() ) {
//				List<Method> ms = List.Nil;
//				ms = collectVTmethods(ms);
////				System.out.println("VT: "+ms);
////				foreach(Method m; ms) {
////					if( m.isAbstract() ) throw new RuntimeException("Abstract method "+m+" in non-abstract class "+jthis);
////				}
//			}

		ConstPool constPool = new ConstPool();
		constPool.addClazzCP(jthis.type.signature);
		constPool.addClazzCP(jthis.type.getJType().java_signature);
		if( super_type != null ) {
			super_type.clazz.checkResolved();
			constPool.addClazzCP(jthis.super_type.signature);
			constPool.addClazzCP(jthis.super_type.getJType().java_signature);
		}
		for(int i=0; interfaces!=null && i < interfaces.length; i++) {
			interfaces[i].checkResolved();
			constPool.addClazzCP(jthis.interfaces[i].signature);
			constPool.addClazzCP(jthis.interfaces[i].getJType().java_signature);
		}
		if( !isPackage() ) {
			for(int i=0; jthis.sub_clazz!=null && i < jthis.sub_clazz.length; i++) {
				jthis.sub_clazz[i].checkResolved();
				constPool.addClazzCP(jthis.sub_clazz[i].type.signature);
				constPool.addClazzCP(jthis.sub_clazz[i].type.getJType().java_signature);
			}
		}
		
		if( !isPackage() && sub_clazz!=null && sub_clazz.length > 0 ) {
			InnerClassesAttr a = new InnerClassesAttr();
			Struct[] inner = new Struct[sub_clazz.length];
			Struct[] outer = new Struct[sub_clazz.length];
			short[] inner_access = new short[sub_clazz.length];
			for(int j=0; j < sub_clazz.length; j++) {
				inner[j] = sub_clazz[j];
				outer[j] = this;
				inner_access[j] = sub_clazz[j].getJavaFlags();
				constPool.addClazzCP(inner[j].type.signature);
			}
			a.inner = inner;
			a.outer = outer;
			a.acc = inner_access;
			addAttr(a);
		}

//		if( countPackedFields() > 0 ) {
//			addAttr(new PackedFieldsAttr(this));
//		}
//
//		if( isSyntax() ) { // || isPackage()
//			for(int i=0; i < imported.length; i++) {
//				ASTNode node = imported[i];
//				if (node instanceof Typedef)
//					addAttr(new TypedefAttr((Typedef)node));
//				else if (node instanceof Opdef)
//					addAttr(new OperatorAttr(((Opdef)node).resolved));
////					else if (node instanceof Import)
////						addAttr(new ImportAlias(node));
////					else
////						addAttr(new ImportAttr(imported[i]));
//			}
//		}
//
//		{
//			int flags = 0;
////				if( jthis.isWrapper() ) flags |= 1;
//			if( jthis.isSyntax()  ) flags |= 2;
//
//			if( flags != 0 ) jthis.addAttr(new FlagsAttr(flags) );
//		}

		if (meta.size() > 0) jthis.addAttr(new RVMetaAttr(meta));
		
		for(int i=0; attrs!=null && i < attrs.length; i++) attrs[i].generate(constPool);
		foreach (ASTNode n; members; n instanceof Field) {
			Field f = (Field)n;
			constPool.addAsciiCP(f.name.name);
			constPool.addAsciiCP(f.type.signature);
			constPool.addAsciiCP(f.type.getJType().java_signature);

//			int flags = 0;
//			if( f.isVirtual() ) flags |= 2;
//			if( f.isForward() ) flags |= 8;
			if( f.isAccessedFromInner()) {
//				flags |= (f.acc.flags << 24);
				f.setPrivate(false);
			}
//			else if( f.isPublic() && f.acc.flags != 0xFF) flags |= (f.acc.flags << 24);
//			else if( f.isProtected() && f.acc.flags != 0x3F) flags |= (f.acc.flags << 24);
//			else if( f.isPrivate() && f.acc.flags != 0x03) flags |= (f.acc.flags << 24);
//			else if( !f.isPublic() && !f.isProtected() && !f.isPrivate() && f.acc.flags != 0x0F) flags |= (f.acc.flags << 24);

//			if( flags != 0 ) f.addAttr(new FlagsAttr(flags) );
//			if( f.name.aliases != List.Nil ) f.addAttr(new AliasAttr(f.name));
//			if( f.isPackerField() ) f.addAttr(new PackerFieldAttr(f));

			if (f.meta.size() > 0) f.addAttr(new RVMetaAttr(f.meta));

			for(int j=0; f.attrs != null && j < f.attrs.length; j++)
				f.attrs[j].generate(constPool);
		}
		foreach (ASTNode m; members; m instanceof Method)
			((Method)m).type.checkJavaSignature();
		foreach (ASTNode n; members; n instanceof Method) {
			Method m = (Method)n;
			constPool.addAsciiCP(m.name.name);
			constPool.addAsciiCP(m.type.signature);
			constPool.addAsciiCP(m.type.getJType().java_signature);
			if( m.jtype != null )
				constPool.addAsciiCP(m.jtype.getJType().java_signature);

			try {
				m.generate(constPool);

//				int flags = 0;
//				if( m.isMultiMethod() ) flags |= 1;
//				if( m.isVarArgs() ) flags |= 4;
//				if( m.isRuleMethod() ) flags |= 16;
//				if( m.isInvariantMethod() ) flags |= 32;
				if( m.isAccessedFromInner()) {
//					flags |= (m.acc.flags << 24);
					m.setPrivate(false);
				}

//				if( flags != 0 ) m.addAttr(new FlagsAttr(flags) );
//				if( m.name.aliases != List.Nil ) m.addAttr(new AliasAttr(m.name));
//
//				if( m.isInvariantMethod() && m.violated_fields.length > 0 ) {
//					m.addAttr(new CheckFieldsAttr(m.violated_fields.toArray()));
//				}

				for(int j=0; j < m.conditions.length; j++) {
					if( m.conditions[j].definer == m ) {
						m.addAttr(m.conditions[j].code_attr);
					}
				}

				if (m.meta.size() > 0) m.addAttr(new RVMetaAttr(m.meta));
				boolean has_pmeta = false; 
				foreach (Var p; m.params; p.meta != null && m.meta.size() > 0) {
					has_pmeta = true;
				}
				if (has_pmeta) {
					MetaSet[] mss;
					mss = new MetaSet[m.params.length];
					for (int i=0; i < mss.length; i++)
						mss[i] = m.params[i].meta;
					m.addAttr(new RVParMetaAttr(mss));
				}
				if (m.annotation_default != null)
					m.addAttr(new DefaultMetaAttr(m.annotation_default));

				for(int j=0; m.attrs!=null && j < m.attrs.length; j++) {
					m.attrs[j].generate(constPool);
				}
			} catch(Exception e ) {
				Kiev.reportError(m,"Compilation error: "+e);
				m.generate(constPool);
				for(int j=0; m.attrs!=null && j < m.attrs.length; j++) {
					m.attrs[j].generate(constPool);
				}
			}
			if( Kiev.safe && isBad() ) return;
		}
		constPool.generate();
		foreach (ASTNode n; members; n instanceof Method) {
			Method m = (Method)n;
			CodeAttr ca = (CodeAttr)m.getAttr(attrCode);
			if( ca != null ) {
				trace(Kiev.debugInstrGen," generating refs for CP for method "+this+"."+m);
				Code.patchCodeConstants(ca);
			}
		}
		if( Kiev.safe && isBad() ) return;
//		Bytecoder bc = new Bytecoder(this,null,constPool);
//		bc.kievmode = true;
//		byte[] dump = bc.writeClazz();
//		Attr ka = addAttr(new KievAttr(dump));
//		ka.generate(constPool);
//		if( Kiev.safe && isBad() ) return;
		FileUnit.toBytecode(this,constPool);
		Env.setProjectInfo(name, true);
		kiev.Main.runGC();
//		setPassed_3(true);
	}

	public Dumper toJavaDecl(Dumper dmp) {
		Struct jthis = this;
		if( Kiev.verbose ) System.out.println("[ Dumping class "+jthis+"]");
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( isInterface() || isArgument() ) {
			dmp.append("interface").forsed_space();
			if( isArgument() ) dmp.append("/*argument*/").space();
			dmp.append(jthis.name.short_name.toString()).space();
			if( type.args!=null && type.args.length > 0 ) {
				dmp.append("/* <");
				for(int i=0; i < type.args.length; i++) {
					dmp.append(type.args[i]);
					if( i < type.args.length-1 ) dmp.append(',');
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
			if( type.args!=null && type.args.length > 0 ) {
				dmp.append("/* <");
				for(int i=0; i < type.args.length; i++) {
					dmp.append(jthis.type.args[i]);
					if( i < type.args.length-1 ) dmp.append(',');
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

	public void cleanup() {
		if( !isPackage() ) {
			foreach (Struct sub; sub_clazz)
					sub.cleanup();
		}

		foreach (ASTNode n; members; n instanceof Field) {
			Field f = (Field)n;
			f.attrs = Attr.emptyArray;
		}
		foreach (ASTNode n; members; n instanceof Method) {
			Method m = (Method)n;
			Attr[] ats = m.attrs;
			m.attrs = Attr.emptyArray;
			for(int j=0; j < ats.length; j++) {
				if( ats[j].name.equals(attrExceptions) )
					m.addAttr(ats[j]);
			}
		}
		Attr[] ats = this.attrs;
		this.attrs = Attr.emptyArray;
		for(int j=0; j < ats.length; j++) {
			if( ats[j].name.equals(attrSourceFile)
//			||	ats[j].name.equals(attrPizzaCase)
//			||	ats[j].name.equals(attrEnum)
//			||	ats[j].name.equals(attrRequire)
//			||	ats[j].name.equals(attrEnsure)
			)
				addAttr(ats[j]);
		}
		//typeinfo_related = null;
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


