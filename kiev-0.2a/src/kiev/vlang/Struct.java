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

import java.io.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Struct.java,v 1.6.2.1.2.6 1999/05/29 21:03:12 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.6 $
 *
 */

public class Struct extends ASTNode implements Named, ScopeOfNames, ScopeOfMethods, SetBody, Accessable {

	import kiev.stdlib.Debug;

	public static Struct[]	emptyArray = new Struct[0];

	/** Class' access */
	public virtual Access	acc;

	/** Variouse names of the class */
	public ClazzName		name;

	/** Type associated with this class */
	public Type				type;

	/** SuperClass structure, (Type because we must inherit with real types
		specified for parametriezed super-classes)
	*/
	public Type				super_clazz;

	/** Package structure this structure belongs to */
	public Struct			package_clazz;

	/** Array of SuperInterface structure (interfaces)
		(Type because we must inherit with real types
		specified for parametriezed interfaces)
	*/
	public Type[]			interfaces = Type.emptyArray;

	/** Array of types that are generated for primitive
		paremeter types of type arguments
	*/
	public Type[]			gens = null;

	/** Reference of template class for generated one */
	public Struct			generated_from = null;

	/** Array used for unresolved TypeInfo (accessed via
	  $typeinfo.related[N]) used at resolving time */
	//public KString[]		typeinfo_related = null;

	/** The auto-generated class for parametriezed
	  classes, that containce type info
	 */
	public Struct			typeinfo_clazz;

	/** Array of substructures of the structure */
	public Struct[]			sub_clazz = Struct.emptyArray;

	/** Array of fields defined in this structure */
	public Field[]			fields = Field.emptyArray;

	/** Array of fields defined in this structure */
	public Field[]			virtual_fields = Field.emptyArray;

	/** The field this structure is wrapper of */
	public Field			wrapped_field = null;

	/** Array of methods defined in this structure */
	public Method[]			methods = Method.emptyArray;

	/** Array of imported classes,fields and methods */
	public ASTNode[]		imported = ASTNode.emptyArray;

	/** Grammar assiciated with this structure */
	public Grammar			gram;

	/** Array of attributes of this structure */
	public Attr[]			attrs = Attr.emptyArray;

	virtual abstract int	anonymouse_inner_counter;
	virtual abstract int	packer_field_counter;
	virtual abstract int	packed_field_counter;

	protected Struct(ClazzName name) {
		super(0,0);
		this.name = name;
		this.acc = new Access(0);
	}

	public Struct(ClazzName name, Struct outer, int acc) {
		super(0,acc);
		this.name = name;
		package_clazz = outer;
		this.acc = new Access(0);
		trace(Kiev.debugCreation,"New clazz created: "+name.short_name
			+" as "+name.name+", member of "+outer/*+", child of "+sup*/);
	}

	public Access get$acc() {
		return acc;
	}

	public void set$acc(Access a) {
		acc = a;
		acc.verifyAccessDecl(this);
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public int get$anonymouse_inner_counter() {
		int i=0;
		foreach(Struct s; sub_clazz; s.isAnonymouse() || s.isLocal()) i++;
		return i;
	}

	public void set$anonymouse_inner_counter(int i) {
		throw new RuntimeException();
	}

	public int get$packer_field_counter() {
		int i=0;
		foreach(Field f; fields; f.isPackerField()) i++;
		return i;
	}

	public void set$packer_field_counter(int i) {
		throw new RuntimeException();
	}

	public int get$packed_field_counter() {
		int i=0;
		foreach(Field f; fields; f.isPackedField()) i++;
		return i;
	}

	public void set$packed_field_counter(int i) {
		throw new RuntimeException();
	}

	public String toString() { return name.name.toString(); }

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

	public NodeName getName() { return new NodeName(name.short_name); }

	public int getValueForEnumField(Field f) {
		if( !isEnum() )
			throw new RuntimeException("Request for enum fields in non-enum structure "+this);
		EnumAttr ea = (EnumAttr)getAttr(attrEnum);
		if( ea == null )
			throw new RuntimeException("enum structure "+this+" without "+attrEnum+" attribute");
		for(int i=0; i < ea.fields.length; i++) {
			if( !ea.fields[i].name.equals(f.name) ) continue;
			return ea.values[i];
		}
		throw new RuntimeException("Enum value for field "+f+" not found in "+this);
	}

	public Type getPrimitiveEnumType() {
		if( !isEnum() || !isPrimitiveEnum() )
			throw new RuntimeException("Request for primitive enum super-type in non-primitive-enum structure "+this);
		if (!super_clazz.isReference())
			return super_clazz;
		PrimitiveEnumAttr ea = (PrimitiveEnumAttr)getAttr(attrPrimitiveEnum);
		if( ea == null )
			throw new RuntimeException("enum structure "+this+" without "+attrPrimitiveEnum+" attribute");
		if (ea.fields.length == 0)
			throw new RuntimeException("empty enum "+this);
		return ea.type;
	}

	public Dumper toJava(Dumper dmp) {
		if( isArgument() ) {
//			dmp.append("/*").append(name.short_name).append("*/");
			if( interfaces.length > 0 )
				return dmp.append(interfaces[0]);
			else
				return dmp.append(super_clazz);
		} else {
			return dmp.append(name);
		}
	}

	public boolean instanceOf(Struct cl) {
		if( cl == null ) return false;
		if( this.equals(cl) ) return true;
		if( super_clazz!= null
		 && super_clazz.clazz.instanceOf(cl) )
		 	return true;
		if( cl.isInterface() ) {
			for(int i=0; i < interfaces.length; i++) {
				if( interfaces[i].clazz.instanceOf(cl) ) return true;
			}
		}
		return false;
	}

	public boolean checkResolved() {
		if( generated_from != null )
			return generated_from.checkResolved();
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

	public Field resolveField(KString name) {
		return resolveField(name,this,true);
	}

	public Field resolveField(KString name, boolean fatal) {
		return resolveField(name,this,fatal);
	}

	private Field resolveField(KString name, Struct where, boolean fatal) {
		checkResolved();
		foreach(Field f; fields; f.name.equals(name) ) return f;
		foreach(Field f; virtual_fields; f.name.equals(name) ) return f;
		if( super_clazz != null ) return super_clazz.clazz.resolveField(name,where,fatal);
		if (fatal)
			throw new RuntimeException("Unresolved field "+name+" in class "+where);
		return null;
	}

	public ASTNode resolveName(KString name) {
		checkResolved();
		foreach(Field f; fields; f.name.equals(name) ) return f;
		foreach(Field f; virtual_fields; f.name.equals(name) ) return f;
		foreach(Type t; type.args; t.clazz.name.short_name.equals(name) ) return t.clazz;
		foreach(Struct s; sub_clazz; !s.isLocal() && s.name.short_name.equals(name) ) return s;
		if( this.name.short_name.equals(nameIdefault) ) {
			ASTNode n = package_clazz.resolveName(name);
			if( n != null ) return n;
		}
        if( isPackage() ) {
			Struct cl;
			ClazzName clname = ClazzName.Empty;
			try {
				if( this.equals(Env.root) ) {
					clname = ClazzName.fromToplevelName(name);
					cl = Env.getStruct(clname);
				} else {
					KStringBuffer ksb = new KStringBuffer(this.name.name.len+name.len+1);
					ksb.append(this.name.name).append('.').append(name);
					clname = ClazzName.fromToplevelName(ksb.toKString());
					cl = Env.getStruct(clname);
				}
				if( cl != null ) return cl;
				trace(Kiev.debugResolve,"Class "+clname.name
					+" with bytecode name "+clname.bytecode_name+" not found in "
					+this);
			} catch(Exception e ) {
				Kiev.reportError(0,e);
			}
		}
		return null;
	}

	rule public resolveNameR(pvar ASTNode node, pvar List<ASTNode> path, KString name, Type tp, int resfl)
		pvar Type sup;
		pvar Field forw;
		pvar List<ASTNode> p;
		pvar Method vf;
	{
		trace(Kiev.debugResolve,"Resolving name "+name+" in "+this+" for type "+tp+" and flags "+resfl),
		checkResolved(),
		{
			node ?= this, ((Struct)node.$var).name.short_name.equals(name)
		;	node @= fields, ((Field)node.$var).name.equals(name)
		;	node @= virtual_fields, ((Field)node.$var).name.equals(name)
		;	(resfl & ResolveFlags.NoImports) == 0,
			node @= imported,
			node.$var instanceof Field,
			((Field)node.$var).name.equals(name)
		;	node @= type.args, ((Type)node.$var).clazz.name.short_name.equals(name), node.$var = ((Type)node.$var).clazz
		;	node @= sub_clazz, ((Struct)node.$var).name.short_name.equals(name)
		;	this.name.short_name.equals(nameIdefault),
			package_clazz.resolveNameR(node,path,name,tp,resfl)
		;	(resfl & ResolveFlags.NoSuper) == 0,
			{	sup ?= super_clazz, sup.$var.clazz.resolveNameR(node,path,name,tp,resfl | ResolveFlags.NoImports)
			;	sup @= interfaces, sup.$var.clazz.resolveNameR(node,path,name,tp,resfl | ResolveFlags.NoImports)
			}
		;	(resfl & ResolveFlags.NoForwards) == 0,
			forw @= fields,
			forw.$var.isForward(),
			p ?= path.$var.concat(forw.$var),
			Type.getRealType(tp,forw.$var.type).clazz.resolveNameR(node,p,name,tp,resfl | ResolveFlags.NoImports),
			path.$var = p.$var
		;	isPackage(), tryLoad(node,path,name,resfl)
		;	!isPackage(), tryAbstractField(node,path,name,resfl)
		}
	}

	public boolean tryLoad(pvar ASTNode node, pvar List<ASTNode> path, KString name, int resfl) {
        if( isPackage() ) {
			Struct cl;
			ClazzName clname = ClazzName.Empty;
			try {
				if( this.equals(Env.root) ) {
					clname = ClazzName.fromToplevelName(name);
					cl = Env.getStruct(clname);
				} else {
					KStringBuffer ksb = new KStringBuffer(this.name.name.len+name.len+1);
					ksb.append(this.name.name).append('.').append(name);
					clname = ClazzName.fromToplevelName(ksb.toKString());
					cl = Env.getStruct(clname);
				}
				if( cl != null ) {
					trace(Kiev.debugResolve,"Struct "+cl+" found in "+this);
					node.$var = cl;
					return true;
				} else {
					trace(Kiev.debugResolve,"Class "+clname.name
						+" with bytecode name "+clname.bytecode_name+" not found in "
						+this);
				}
			} catch(Exception e ) {
				Kiev.reportError(0,e);
			}
		}
		node.$var = null;
		return false;
	}

	public boolean tryAbstractField(pvar ASTNode node, pvar List<ASTNode> path, KString name, int resfl) {
		if( !isPackage() ) {
			KString set_name = new KStringBuffer(nameSet.length()+name.length()).
				append_fast(nameSet).append_fast(name).toKString();
			KString get_name = new KStringBuffer(nameGet.length()+name.length()).
				append_fast(nameGet).append_fast(name).toKString();

			for(int i=0; i < methods.length; i++) {
//				if( methods[i].isStatic() ) continue;
				if( methods[i].name.equals(set_name) ) {
				 	Field f = new Field(this,name,methods[i].type.args[0],
				 		methods[i].getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT);
				 	node.$var = f;
				 	return true;
				 }
				if( methods[i].name.equals(get_name) ) {
				 	Field f = new Field(this,name,methods[i].type.ret,
				 		methods[i].getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT);
				 	node.$var = f;
				 	return true;
				 }
			}
		}
	 	node.$var = null;
	 	return false;
	}

	rule public resolveMethodR(pvar ASTNode node, pvar List<ASTNode> path, KString name, Expr[] args, Type ret, Type tp, int resfl)
		pvar Type sup;
		pvar Struct defaults;
		pvar Field forw;
		pvar List<ASTNode> p;
	{
		checkResolved(),
		trace(Kiev.debugResolve, "Resolving "+name+" in "+this+" for type "+tp+(path.$var==List.Nil?"":" in forward path "+path)),
		{
			node @= methods,
			((Method)node.$var).equalsByCast(name,args,ret,tp,resfl),
			{
				(resfl & ResolveFlags.Static) == 0
			;	(resfl & ResolveFlags.Static) != 0, node.isStatic()
			}
		;	(resfl & ResolveFlags.NoImports) == 0 || isPackage(),
			node @= imported, node.$var instanceof Method,
			((Method)node.$var).equalsByCast(name,args,ret,tp,resfl)
		;	(resfl & ResolveFlags.NoSuper) == 0,
			sup ?= super_clazz,
			sup.$var.clazz.resolveMethodR(node,path,name,args,ret,tp,resfl | ResolveFlags.NoImports)
		;	isInterface(),
			defaults @= sub_clazz,
			defaults.$var.isClazz() && defaults.$var.name.short_name.equals(nameIdefault),
			defaults.$var.resolveMethodR(node,path,name,args,ret,tp,resfl | ResolveFlags.NoSuper )
		;	(resfl & ResolveFlags.NoSuper) == 0,
			isInterface(),
			sup @= interfaces,
			sup.$var.clazz.resolveMethodR(node,path,name,args,ret,tp,resfl | ResolveFlags.NoImports)
		;	(resfl & ResolveFlags.NoForwards) == 0,
			forw @= fields,
			forw.$var.isForward(),
			p ?= path.$var.concat(forw.$var),
			Type.getRealType(tp,forw.$var.type).clazz.resolveMethodR(node,p,name,args,Type.getRealType(tp,ret),Type.getRealType(tp,forw.$var.type),resfl | ResolveFlags.NoImports),
			path.$var = p.$var
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
		for(int i=0; i < methods.length; i++) {
			if( methods[i].name.equals(name) && methods[i].type.signature.equals(sign))
				return methods[i];
		}
		if( isInterface() ) {
			Struct defaults = null;
			foreach(Struct s; sub_clazz; s.isClazz() && s.name.short_name.equals(nameIdefault) ) {
				defaults = s; break;
			}
			if( defaults != null ) {
				for(int i=0; i < defaults.methods.length; i++) {
					if( defaults.methods[i].name.equals(name) && defaults.methods[i].type.signature.equals(sign))
						return defaults.methods[i];
				}
			}
		}
		trace(Kiev.debugResolve,"Method "+name+" with signature "
			+sign+" unresolved in class "+this);
		Method m = null;
		if( super_clazz!= null )
			m = super_clazz.clazz.resolveMethod(name,sign,where,fatal);
		if( m != null ) return m;
		foreach(Type interf; interfaces) {
			m = interf.clazz.resolveMethod(name,sign,where,fatal);
			if( m != null ) return m;
		}
		if (fatal)
			throw new RuntimeException("Unresolved method "+name+sign+" in class "+where);
		return null;
	}

	/** Add information about new attribute that belongs to this class */
	public Attr addAttr(Attr a) {
		// Check we already have this attribute
		if( !(a.name==attrOperator || a.name==attrImport
			|| a.name==attrRequire || a.name==attrEnsure) ) {
			for(int i=0; i < attrs.length; i++) {
				if(attrs[i].name == a.name) {
					attrs[i] = a;
					return a;
				}
			}
		}
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
		if( sub_clazz == null ) sub_clazz = new Struct[0];
		// Check we already have this sub-class
		for(int i=0; i < sub_clazz.length; i++) {
			if( sub_clazz[i].equals(sub) ) {
//				throw new RuntimeException("Subclass "+sub+" already exists in class "+this);
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

		sub_clazz = (Struct[])Arrays.append(sub_clazz,sub);
/*
		if( gens != null ) {
			// Clone substructures for generated classes
			for(int i=0; i < gens.length; i++) {
				Struct s = gens[i].clazz;
				KStringBuffer ksb = new KStringBuffer();
				ksb.append(s.name.bytecode_name)
					.append((byte)'$').append(sub.name.short_name)
					.append((byte)'_').append((byte)'_');
				Type st = Type.getRealType(s.type,sub.type);
				for(int l=0; l < st.args.length; l++) {
					if( st.args[l].isReference() )
						ksb.append((byte)'A');
					else
						ksb.append(st.args[l].signature.byteAt(0));
				}
				ksb.append((byte)'_');
				ClazzName cn = ClazzName.fromBytecodeName(ksb.toKString());
				Struct ss = Env.newStruct(cn,true);
				st.java_signature = cn.signature();
				st.clazz = ss;
				ss.type = st;
				ss.generated_from = sub;
			}
		}
*/
		trace(Kiev.debugMembers,"Sub-class "+sub+" added to class "+this);
		if (sub.name.short_name == nameClTypeInfo) {
			typeinfo_clazz = sub;
			if( gens != null ) {
				for(int i=0; i < gens.length; i++) {
					gens[i].clazz.typeinfo_clazz = sub;
					trace(Kiev.debugMembers,"Sub-class "+sub+" is the typeinfo class of "+gens[i].clazz);
				}
			}
			trace(Kiev.debugMembers,"Sub-class "+sub+" is the typeinfo class of "+this);
		}
		return sub;
	}

	/** Add information about new method that belongs to this class */
	public Method addMethod(Method m) {
		if( methods == null ) methods = new Method[0];
		// Check we already have this method
		for(int i=0; i < methods.length; i++) {
			if( methods[i].equals(m) )
				throw new RuntimeException("Method "+m+" already exists in class "+this);
			if (methods[i].name.equals(m.name) && methods[i].type.equals(m.type))
				throw new RuntimeException("Method "+m+" already exists in class "+this);
		}
		methods = (Method[])Arrays.append(methods,m);
		m.parent = this;
		if( gens != null ) {
			for(int i=0; i < gens.length; i++) {
				Struct s = gens[i].clazz;
				assert(methods.length-1==s.methods.length);
				Method sm = new Method(s, m.name.name,
					(MethodType)Type.getRealType(s.type,m.type),
					m.getFlags()
					);
				s.methods = (Method[])Arrays.append(s.methods,sm);
			}
		}
		trace(Kiev.debugMembers,"Method "+m+" added to class "+this);
		return m;
	}

	/** Remove information about new method that belongs to this class */
	public void removeMethod(Method m) {
		// Check we already have this method
		int i = 0;
		for(i=0; i < methods.length; i++) {
			if( methods[i].equals(m) ) {
				break;
			}
		}
		if (i >= methods.length)
			throw new RuntimeException("Method "+m+" do not exists in class "+this);
		Method[] newmethods = new Method[methods.length-1];
		for (int m=0, n=0; m < methods.length; m++) {
			if (m!=i)
				newmethods[n++] = methods[m];
		}
		methods = newmethods;
		trace(Kiev.debugMembers,"Method "+m+" removed from class "+this);
	}

	/** Add information about new field that belongs to this class */
	public Field addField(Field f) {
		if( fields == null ) fields = new Field[0];
		// Check we already have this field
		for(int i=0; i < fields.length; i++) {
			if( fields[i].equals(f) ) {
				throw new RuntimeException("Field "+f+" already exists in class "+this);
			}
		}
		fields = (Field[])Arrays.append(fields,f);
		f.parent = this;
		if( gens != null ) {
			for(int i=0; i < gens.length; i++) {
				Struct s = gens[i].clazz;
				Field sf = new Field(s, f.name.name,
					Type.getRealType(s.type,f.type),
					f.getFlags()
					);
				s.fields = (Field[])Arrays.append(s.fields,sf);
			}
		}
		trace(Kiev.debugMembers,"Field "+f+" added to class "+this);
		return f;
	}

	/** Add information about new pizza case of this class */
	public Struct addCase(Struct cas) {
		setHasCases(true);
		int caseno = 0;
		PizzaCaseAttr case_attr = null;
		for(int i=0; i < sub_clazz.length; i++) {
			if( sub_clazz[i].isPizzaCase() ) {
				case_attr = (PizzaCaseAttr)sub_clazz[i].getAttr(attrPizzaCase);
				if( case_attr!=null && case_attr.caseno > caseno )
					caseno = case_attr.caseno;
			}
		}
		case_attr = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
		if( case_attr == null ) {
			case_attr = new PizzaCaseAttr();
			cas.addAttr(case_attr);
		}
		case_attr.caseno = caseno + 1;
		trace(Kiev.debugMembers,"Class's case "+cas+" added to class "
			+this+" as case # "+case_attr.caseno);
		// Add get$case$tag() method to itself
//		if( case_attr.caseno == 1 ) {
//			Method gettag = new Method(this,Constants.nameGetCaseTag,
//				MethodType.newMethodType(MethodType.tpMethodClazz,Type.emptyArray,Type.tpInt),ACC_PUBLIC);
//			gettag.body = new BlockStat(gettag.pos,gettag);
//			((BlockStat)gettag.body).addStatement(
//				new ReturnStat(gettag.pos,new ConstExpr(gettag.pos,Kiev.newInteger(0)))
//			);
//			addMethod(gettag);
//		}
		return cas;
	}

	public Type getType() { return Type.tpVoid; }

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
		if( t instanceof MethodType ) {
			return "kiev.stdlib.closure";
		}
		if( t.args.length > 0 ) {
			StringBuffer sb = new StringBuffer(128);
			if (t.clazz.generated_from != null)
				sb.append(t.clazz.generated_from.name.bytecode_name.toString().replace('/','.'));
			else
				sb.append(t.clazz.name.bytecode_name.toString().replace('/','.'));
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
			return t.clazz.name.bytecode_name.toString().replace('/','.');
		}
	}

	public Expr accessTypeInfoField(int pos, ASTNode parent, Type t) {
		assert(!Kiev.kaffe,"create type info in kaffe mode");
		if( t.isArgumented() ) {
			Expr ti_access;
			if( PassInfo.method == null || PassInfo.method.isStatic()) {
				// check we have $typeinfo as first argument
				if( PassInfo.method==null
				 || PassInfo.method.params.length < 1
				 || PassInfo.method.params[0].name.name != nameTypeInfo
				 || !PassInfo.method.params[0].type.isInstanceOf(Type.tpTypeInfo)
				 )
				 	throw new CompilerException(pos,"$typeinfo cannot be accessed from "+PassInfo.method);
				ti_access = new VarAccessExpr(pos,PassInfo.method.params[0]);
			}
			else {
				Field ti = resolveField(nameTypeInfo);
				ti_access = new FieldAccessExpr(pos,ti);
			}
			// Small optimization for the $typeinfo
			if( this.type.isInstanceOf(t.clazz.type) )
				return ti_access;

			if (t.isArgument()) {
				// Get corresponded type argument
				KString fnm = new KStringBuffer(nameTypeInfo.length()+1+t.clazz.name.short_name.length())
						.append(nameTypeInfo).append('$').append(t.clazz.name.short_name).toKString();
				Field ti_arg = typeinfo_clazz.resolveField(fnm);
				if (ti_arg == null)
					throw new RuntimeException("Field "+fnm+" not found in "+typeinfo_clazz+" from method "+PassInfo.method);
				ti_access = new AccessExpr(pos,ti_access,ti_arg);
				return ti_access;
			}
		}

		// Lookup and create if need as $typeinfo$N
		int i = 0;
		KString ts = KString.from(makeTypeInfoString(t));
	next_field:
		foreach(Field f; fields;
						f.isStatic() &&
						f.init != null &&
						f.name.name.startsWith(nameTypeInfo) &&
						!f.name.name.equals(nameTypeInfo)) {
			i++;
			KString ti_str = (KString)((ConstExpr)((CallExpr)((CastExpr)f.init).expr).args[0]).value;
			if( !ts.equals(ti_str) ) continue;
			Expr e = new StaticFieldAccessExpr(pos,this,f);
			e.parent = parent;
			return e;
		}
		Type ftype = Type.tpTypeInfo;
		if (t.args.length > 0)
			ftype = t.clazz.typeinfo_clazz.type;
		Field f = new Field(this,KString.from(nameTypeInfo+"$"+i),ftype,ACC_PRIVATE|ACC_STATIC|ACC_FINAL);
		Expr[] ti_args = new Expr[]{new ConstExpr(pos,ts)};
		f.init = new CastExpr(pos,ftype,new CallExpr(pos,
				Type.tpTypeInfo.clazz.resolveMethod(
					KString.from("newTypeInfo"),
					KString.from("(Ljava/lang/String;)Lkiev/stdlib/TypeInfo;")
				), ti_args));
		// Add initialization in <clinit>
		Method class_init = null;
		foreach(Method m; methods; m.name.equals(nameClassInit) ) {
			class_init = m;
			break;
		}
		if( class_init == null ) {
			class_init = new Method(this,nameClassInit,
				MethodType.newMethodType(null,null,Type.tpVoid),ACC_STATIC);
			class_init.pos = pos;
			addMethod(class_init);
			class_init.body = new BlockStat(pos,class_init);
		}
		if( PassInfo.method != null && PassInfo.method.name.equals(nameClassInit) ) {
			((BlockStat)class_init.body).addstats = (Statement[])Arrays.insert(
				((BlockStat)class_init.body).addstats,
				new ExprStat(f.init.getPos(),class_init.body,
					new AssignExpr(f.init.getPos(),AssignOperator.Assign
						,new StaticFieldAccessExpr(f.pos,this,f),f.init)
				),0
			);
		} else {
			((BlockStat)class_init.body).stats = (ASTNode[])Arrays.insert(
				((BlockStat)class_init.body).stats,
				new ExprStat(f.init.getPos(),class_init.body,
					new AssignExpr(f.init.getPos(),AssignOperator.Assign
						,new StaticFieldAccessExpr(f.pos,this,f),f.init)
				),0
			);
		}
		addField(f);
		Expr e = new StaticFieldAccessExpr(pos,this,f);
		e.parent = parent;
		return e;
//		System.out.println("Field "+f+" of type "+f.init+" added");
	}


	public void addMethodsForVirtualField(Field f) {
		if( f.isStatic() && f.isVirtual() ) {
			Kiev.reportError(f.pos,"Static fields can't be virtual");
			f.setVirtual(false);
		}
		if( isInterface() && f.isVirtual() ) f.setAbstract(true);

		if( !f.isVirtual() && !f.isExportCpp() ) return;
		// Check set$/get$ methods
		boolean set_found = false;
		boolean get_found = false;

		KString native_set_name = new KStringBuffer(nameSetNative.length()+f.name.name.length()).
			append_fast(nameSetNative).append_fast(f.name.name).toKString();
		KString set_name = new KStringBuffer(nameSet.length()+f.name.name.length()).
			append_fast(nameSet).append_fast(f.name.name).toKString();
		KString get_name = new KStringBuffer(nameGet.length()+f.name.name.length()).
			append_fast(nameGet).append_fast(f.name.name).toKString();

		foreach(Method m; methods; m.name.name.byteAt(3) == '$') {
			if( m.name.equals(set_name) ) {
				set_found = true;
				if( get_found ) break;
			}
			else if( m.name.equals(get_name) ) {
				get_found = true;
				if( set_found ) break;
			}
		}
		if( !set_found && f.acc.writeable() ) {
			Method set_var = new Method(this,set_name,
				MethodType.newMethodType(null,new Type[]{f.type},Type.tpVoid),
				f.getJavaFlags()
			);
			Var self;
			Var value;
			if (f.isStatic()) {
				self = null;
				value = new Var(f.pos,set_var,KString.from("value"),f.type,0);
				set_var.params = new Var[]{value};
			} else {
				self = new Var(f.pos,set_var,nameThis,this.type,0);
				value = new Var(f.pos,set_var,KString.from("value"),f.type,0);
				set_var.params = new Var[]{self,value};
			}
			if( !f.isAbstract() ) {
				BlockStat body = new BlockStat(f.pos,set_var,ASTNode.emptyArray);
				Statement ass_st = new ExprStat(f.pos,body,
					new AssignExpr(f.pos,AssignOperator.Assign,
						f.isStatic()? new StaticFieldAccessExpr(f.pos,this,f,ACC_AS_FIELD)
									: new FieldAccessExpr(f.pos,f,ACC_AS_FIELD),
						new VarAccessExpr(f.pos,value)
					)
				);
				if (f.isExportCpp()) {
					Method native_set_var = new Method(this,native_set_name,
						f.isStatic()? MethodType.newMethodType(null,new Type[]{f.type},Type.tpVoid)
									: MethodType.newMethodType(null,new Type[]{Type.tpInt,f.type},Type.tpVoid)
						,
						f.getJavaFlags()|ACC_FINAL);
					native_set_var.setPrivate(true);
					native_set_var.setNative(true);
					this.addMethod(native_set_var);
					Statement cpp_st;
					if (f.isStatic()) {
						cpp_st = new ExprStat(f.pos,body,
							new ASTCallExpression(f.pos,native_set_name,
								new Expr[]{new StaticFieldAccessExpr(f.pos,this,f,ACC_AS_FIELD)}
							)
						);
					} else {
						ASTAccessExpression this_native = new ASTAccessExpression(f.pos,
							new ASTIdentifier(f.pos,nameThis),nameThisNative);
						cpp_st = new ExprStat(f.pos,body,
							new ASTCallExpression(f.pos,native_set_name,
								new Expr[]{ this_native,new FieldAccessExpr(f.pos,f,ACC_AS_FIELD)}
							)
						);
					}
					body.stats = new ASTNode[]{ass_st,cpp_st,new ReturnStat(f.pos,body,null)};
				} else {
					body.stats = new ASTNode[]{ass_st,new ReturnStat(f.pos,body,null)};
				}
				set_var.body = body;
			}
			this.addMethod(set_var);
			f.setter = set_var;
		}
		else if( set_found && !f.acc.writeable() ) {
			Kiev.reportError(f.pos,"Virtual set$ method for non-writeable field");
		}
		else if( set_found && f.isExportCpp() ) {
			Kiev.reportError(f.pos,"Virtual set$ method for cpp-export field");
		}

		if (!isVirtual())
			return;		// no need to generate getter for $export_cpp
		if( !get_found && f.acc.readable()) {
			Method get_var = new Method(this,get_name,
				MethodType.newMethodType(null,Type.emptyArray,f.type),
				f.getJavaFlags()
			);
			Var self = new Var(f.pos,get_var,nameThis,this.type,0);
			get_var.params = new Var[]{self};
			if( !f.isAbstract() ) {
				BlockStat body = new BlockStat(f.pos,get_var,ASTNode.emptyArray);
				Statement st = new ReturnStat(f.pos,body,new FieldAccessExpr(f.pos,f,ACC_AS_FIELD));
				body.stats = new ASTNode[]{st};
				get_var.body = body;
			}
			this.addMethod(get_var);
			f.getter = get_var;
		}
		else if( get_found && !f.acc.readable() ) {
			Kiev.reportError(f.pos,"Virtual get$ method for non-readable field");
		}
	}

	public void setupWrappedField() {
		if (!isWrapper()) {
			wrapped_field = null;
			return;
		}
		if (wrapped_field != null)
			return;
		if (super_clazz != null) {
			super_clazz.clazz.setupWrappedField();
			if(super_clazz.clazz.wrapped_field != null) {
				wrapped_field = super_clazz.clazz.wrapped_field;
				return;
			}
		}
		Field wf = null;
		foreach(Field f; fields; f.isForward()) {
			if (wf == null)
				wf = f;
			else
				throw new CompilerException(f.pos,"Wrapper class with multiple forward fields");
		}
		foreach(Field f; virtual_fields; f.isForward()) {
			if (wf == null)
				wf = f;
			else
				throw new CompilerException(f.pos,"Wrapper class with multiple forward fields");
		}
		if ( wf == null )
			throw new CompilerException(this.pos,"Wrapper class "+this+" has no forward field");
		if( Kiev.verbose ) System.out.println("Class "+this+" is a wrapper for field "+wf);
		wrapped_field = wf;
	}

	public void addAbstractFields() {
		foreach(Method m; methods; m.name.name.startsWith(nameSet) || m.name.name.startsWith(nameGet) ) {
			Field f = resolveField( m.name.name.substr(nameSet.length()), false );
			if( f != null ) {
				trace(Kiev.debugCreation,"method "+m+" has field "+f);
				if( m.name.name.startsWith(nameSet) ) {
					if( m.isPublic() ) {
						f.acc.w_public = true;
						f.acc.w_protected = true;
						f.acc.w_default = true;
						f.acc.w_private = true;
					}
					else if( m.isPrivate() ) {
						f.acc.w_public = false;
						f.acc.w_protected = false;
						f.acc.w_default = false;
						f.acc.w_private = true;
					}
					else if( m.isProtected() ) {
						f.acc.w_public = false;
						f.acc.w_private = true;
					}
					else {
						f.acc.w_public = false;
						f.acc.w_default = true;
						f.acc.w_private = true;
					}
				}
				else if( m.name.name.startsWith(nameGet) ) {
					if( m.isPublic() ) {
						f.acc.r_public = true;
						f.acc.r_protected = true;
						f.acc.r_default = true;
						f.acc.r_private = true;
					}
					else if( m.isPrivate() ) {
						f.acc.r_public = false;
						f.acc.r_protected = false;
						f.acc.r_default = false;
						f.acc.r_private = true;
					}
					else if( m.isProtected() ) {
						f.acc.r_public = false;
						f.acc.r_private = true;
					}
					else {
						f.acc.r_public = false;
						f.acc.r_default = true;
						f.acc.r_private = true;
					}
				}
				f.acc.verifyAccessDecl(f);
				continue;
			}
			trace(Kiev.debugCreation,"create abstract field "+f+" for methos "+m);
			if( m.name.name.startsWith(nameSet) ) {
				virtual_fields = (Field[])Arrays.append(virtual_fields,
					f=new Field(this,m.name.name.substr(nameSet.length()),m.type.args[0],
				 		m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT));
				if( m.isPublic() ) {
					f.acc.w_public = true;
					f.acc.w_protected = true;
					f.acc.w_default = true;
					f.acc.w_private = true;
				}
				else if( m.isPrivate() ) {
					f.acc.w_public = false;
					f.acc.w_protected = false;
					f.acc.w_default = false;
					f.acc.w_private = true;
				}
				else if( m.isProtected() ) {
					f.acc.w_public = false;
					f.acc.w_private = true;
				}
				else {
					f.acc.w_public = false;
					f.acc.w_default = true;
					f.acc.w_private = true;
				}
			}
			else if( m.name.name.startsWith(nameGet) ) {
				virtual_fields = (Field[])Arrays.append(virtual_fields,
					f=new Field(this,m.name.name.substr(nameGet.length()),m.type.ret,
						m.getJavaFlags() | ACC_VIRTUAL | ACC_ABSTRACT));
				if( m.isPublic() ) {
					f.acc.r_public = true;
					f.acc.r_protected = true;
					f.acc.r_default = true;
					f.acc.r_private = true;
				}
				else if( m.isPrivate() ) {
					f.acc.r_public = false;
					f.acc.r_protected = false;
					f.acc.r_default = false;
					f.acc.r_private = true;
				}
				else if( m.isProtected() ) {
					f.acc.r_public = false;
					f.acc.r_private = true;
				}
				else {
					f.acc.r_public = false;
					f.acc.r_default = true;
					f.acc.r_private = true;
				}
			}
		}
	}

	rule locatePackerField(pvar Field f, int size)
	{
		super_clazz != null,
		super_clazz.clazz.locatePackerField(f,size)
	;	f @= fields,
		f.$var.isPackerField(),
		(32-f.pack.size) >= size
	}

	/** Auto-generates <init>()V method if no <init> declared,
		also, used to generate this$N fields and arguments for
		inner classes, case$tag field for case classes and so on
	*/
	public void autoGenerateMembers() {
		checkResolved();
		if( Kiev.debug ) System.out.println("AutoGenerating members for "+this);

		// Setup packed/packer fields
		foreach(Field f; fields; f.isPackedField() ) {
			pvar Field packer;
			// Locate or create nearest packer field that can hold this one
			if( f.pack.packer == null ) {
				if( f.pack.packer_name != null ) {
					Field p = this.resolveField(f.pack.packer_name);
					if( p == null ) {
						Kiev.reportError(f.pos,"Packer field "+f.pack.packer_name+" not found");
						f.pack = null;
						f.setPackedField(false);
						continue;
					}
					if( p.type != Type.tpInt ) {
						Kiev.reportError(f.pos,"Packer field "+p+" is not of 'int' type");
						f.pack = null;
						f.setPackedField(false);
						continue;
					}
					f.pack.packer = p;
					assert( f.pack.offset >= 0 && f.pack.offset+f.pack.size <= 32 );
				}
				else if( locatePackerField(packer,f.pack.size) ) {
					// Found
					f.pack.packer = packer;
					f.pack.packer_name = packer.name.name;
					f.pack.offset = packer.pack.size;
					packer.pack.size += f.pack.size;
				} else {
					// Create
					Field p = new Field(this,
						KString.from("$pack$"+packer_field_counter),Type.tpInt,ACC_PUBLIC);
					p.pos = this.pos;
					p.setPackerField(true);
					p.pack = new Field.PackInfo(f.pack.size,0,null);
					addField(p);
					f.pack.packer = p;
					f.pack.packer_name = p.name.name;
					f.pack.offset = 0;
				}
			}
		}

		if( !Kiev.kaffe && !isInterface() && type.args.length > 0 && !(type instanceof MethodType) ) {
			// create typeinfo class
			int flags = this.flags & JAVA_ACC_MASK;
			flags &= ~(ACC_PRIVATE | ACC_PROTECTED);
			flags |= ACC_PUBLIC | ACC_STATIC | ACC_RESOLVED;
			typeinfo_clazz = Env.newStruct(
				ClazzName.fromOuterAndName(this,nameClTypeInfo),this,flags,true
				);
			typeinfo_clazz.setPublic(true);
			if (super_clazz != null && super_clazz.clazz.typeinfo_clazz != null)
				typeinfo_clazz.super_clazz = super_clazz.clazz.typeinfo_clazz.type;
			else
				typeinfo_clazz.super_clazz = Type.tpTypeInfo;
			typeinfo_clazz.type = Type.newJavaRefType(typeinfo_clazz);
			addSubStruct(typeinfo_clazz);
			typeinfo_clazz.pos = pos;

			// add in it arguments fields, and prepare for constructor
			MethodType ti_init;
			Type[] ti_init_targs = new Type[this.type.args.length];
			Var[] ti_init_params = new Var[]{new Var(pos,null,nameThis,typeinfo_clazz.type,0)};
			ASTNode[] stats = new ASTNode[type.args.length];
			for (int arg=0; arg < type.args.length; arg++) {
				Type t = type.args[arg];
				KString fname = new KStringBuffer(nameTypeInfo.length()+1+t.clazz.name.short_name.length())
					.append(nameTypeInfo).append('$').append(t.clazz.name.short_name).toKString();
				Field f = new Field(typeinfo_clazz,fname,Type.tpTypeInfo,ACC_PUBLIC|ACC_FINAL);
				typeinfo_clazz.addField(f);
				ti_init_targs[arg] = Type.tpTypeInfo;
				Var v = new Var(pos,null,t.clazz.name.short_name,Type.tpTypeInfo,0);
				ti_init_params = (Var[])Arrays.append(ti_init_params,v);
				stats[arg] = new ExprStat(pos,null,
					new AssignExpr(pos,AssignOperator.Assign,
						new FieldAccessExpr(pos,f),
						new VarAccessExpr(pos,v)
					)
				);
			}
			BlockStat ti_init_body = new BlockStat(pos,null,stats);

			// create typeinfo field
			Field tif = addField(new Field(this,nameTypeInfo,typeinfo_clazz.type,ACC_PUBLIC|ACC_FINAL));

			// create constructor method
			ti_init = MethodType.newMethodType(null,ti_init_targs,Type.tpVoid);
			Method init = new Method(typeinfo_clazz,nameInit,ti_init,ACC_PUBLIC);
			init.params = ti_init_params;
			typeinfo_clazz.addMethod(init);
			init.body = ti_init_body;
			ti_init_body.parent = init;
			// and add super-constructor call
			if (typeinfo_clazz.super_clazz == Type.tpTypeInfo) {
				//do nothing, default constructor may be added later
			} else {
				init.setNeedFieldInits(true);
				ASTCallExpression call_super = new ASTCallExpression(0);
				call_super.pos = pos;
				call_super.func = nameSuper;
				Expr[] exprs = new Expr[super_clazz.args.length];
				for (int arg=0; arg < super_clazz.args.length; arg++) {
					Type t = super_clazz.args[arg];
					t = Type.getRealType(this.type,t);
					if (t.isArgumented()) {
						exprs[arg] = new ASTIdentifier(pos,t.clazz.name.short_name);
					} else {
						CallExpr ce = new CallExpr(pos,
							Type.tpTypeInfo.clazz.resolveMethod(
								KString.from("newTypeInfo"),
								KString.from("(Ljava/lang/String;)Lkiev/stdlib/TypeInfo;")
							),
							new Expr[]{new ConstExpr(pos,KString.from(makeTypeInfoString(t)))}
						);
						ce.type_of_static = this.type;
						exprs[arg] = ce;
					}
				}
				call_super.args = exprs;
				ti_init_body.stats = (ASTNode[])Arrays.insert(ti_init_body.stats,
						new ASTStatementExpression(call_super),0);
			}

			// create method to get typeinfo field
			MethodType tim_type = MethodType.newMethodType(null,Type.emptyArray,Type.tpTypeInfo);
			Method tim = addMethod(new Method(this,nameGetTypeInfo,tim_type,ACC_PUBLIC));
			tim.body = new BlockStat(pos,tim,new ASTNode[]{
				new ReturnStat(pos,null,new FieldAccessExpr(pos,tif))
			});
			tim.params = new Var[]{new Var(pos,tim,nameThis,this.type,0)};
		}

		if( isEnum() ) {
			int enum_index = Integer.MIN_VALUE;
			if( !isPrimitiveEnum() && super_clazz != Type.tpEnum ) {
				if( !super_clazz.clazz.isEnum() )
					throw new CompilerException(pos,"enum type extends not enum type "+super_clazz);
				int ins_pos = 0;
				foreach(Field f; super_clazz.clazz.fields) {
					Field nf = new Field(this,f.name.name,this.type,ACC_PUBLIC | ACC_STATIC | ACC_FINAL );
					int val = super_clazz.clazz.getValueForEnumField(f);
					if( enum_index < val ) enum_index = val;
					nf.init = new NewExpr(pos, this.type, new Expr[]{new ConstExpr(pos,Kiev.newInteger(val))});
					fields = (Field[])Arrays.insert(fields,nf,ins_pos++);
				}
			}
			for(int i=0; i < fields.length; i++) {
				Field f = fields[i];
				if( f.init != null ) continue;
				int enum_index;
				if (isPrimitiveEnum()) {
					if( i==0 )
						enum_index = 0;
					else
						enum_index = 1 + ((Number)((ConstExpr)fields[i-1].init).getConstValue()).intValue();
					f.init = new ConstExpr(f.pos,Kiev.newInteger(enum_index));
				} else {
					if( i==0 )
						enum_index = 0;
					else
						enum_index = 1 + ((Number)((ConstExpr)
							((NewExpr)fields[i-1].init).args[0]).getConstValue()).intValue();
					f.init = new NewExpr(f.pos,this.type,
						new Expr[]{new ConstExpr(f.pos,Kiev.newInteger(enum_index))});
				}
			}
			int len = fields.length;
			int[] values = new int[len];
			for(int i=0; i < len; i++) {
				if (isPrimitiveEnum()) {
					values[i] = ((Number)((ConstExpr)fields[i].init).getConstValue()).intValue();
				} else {
					values[i] = ((Number)((ConstExpr)
						((NewExpr)fields[i].init).args[0]).getConstValue()).intValue();
				}
			}
			if (isPrimitiveEnum()) {
				PrimitiveEnumAttr ea = new PrimitiveEnumAttr(this.super_clazz,fields,values);
				addAttr(ea);
			} else {
				EnumAttr ea = new EnumAttr(fields,values);
				addAttr(ea);
			}
			this.super_clazz = Type.tpEnum;
		}

		if( isPizzaCase() ) {
			PizzaCaseAttr case_attr = (PizzaCaseAttr)getAttr(attrPizzaCase);
			Field ftag = addField(new Field(
				this,nameCaseTag,Type.tpInt,ACC_PUBLIC|ACC_FINAL|ACC_STATIC) );
			ConstExpr ce = new ConstExpr(ftag.pos,Kiev.newInteger(case_attr.caseno));
			ftag.init = ce;

/*			if( case_attr.casefields.length == 0 ) {
				Field selftag = addField(new Field(
					this,nameTagSelf,this.type,ACC_PUBLIC|ACC_FINAL|ACC_STATIC) );
				selftag.init = new NewExpr(selftag.pos,this.type,Expr.emptyArray);
			}
*/
			Method gettag = new Method(this,nameGetCaseTag,
				MethodType.newMethodType(this,Type.emptyArray,Type.tpInt),ACC_PUBLIC);
			gettag.body = new BlockStat(gettag.pos,gettag);
			((BlockStat)gettag.body).addStatement(
				new ReturnStat(gettag.pos,new StaticFieldAccessExpr(ftag.pos,this,ftag))
			);
			addMethod(gettag);
		}
		else if( isHasCases() ) {
			// Add get$case$tag() method to itself
			Method gettag = new Method(this,Constants.nameGetCaseTag,
				MethodType.newMethodType(MethodType.tpMethodClazz,Type.emptyArray,Type.tpInt),ACC_PUBLIC);
			gettag.body = new BlockStat(gettag.pos,gettag);
			((BlockStat)gettag.body).addStatement(
				new ReturnStat(gettag.pos,new ConstExpr(gettag.pos,Kiev.newInteger(0)))
			);
			addMethod(gettag);
		}

		// Check if it's an inner class
		if( isClazz() && package_clazz.isClazz() && !isStatic() ) {
			int n = 0;
			for(Struct pkg=package_clazz;
					pkg.isClazz() && !pkg.isStatic();
						pkg=pkg.package_clazz) n++;
			Field f = addField(new Field(this,
				KString.from(nameThisDollar.toString()+(n-1)),package_clazz.type,ACC_FORWARD));
			f.pos = pos;
		}

		if( !isInterface() && !isPackage() ) {
			// Default <init> method, if no one is declared
			boolean init_found = false;
			// Add outer hidden parameter to constructors for inner and non-static classes
			for(int i=0; i < methods.length; i++) {
				if( !(methods[i].name.equals(nameInit) || methods[i].name.equals(nameNewOp)) ) continue;
				if( methods[i].name.equals(nameInit) )
					init_found = true;
				boolean retype = false;
				Method m = methods[i];
				Type[] targs = m.type.args;
				package_clazz.checkResolved();
				if( package_clazz.isClazz() && !isStatic() ) {
					// Insert outer class type as second argument, but first type
					// in signature
					targs = (Type[])Arrays.insert(targs,package_clazz.type,0);
					// Also add formal parameter
					if( m.isStatic() )
						m.params = (Var[])Arrays.insert(m.params,new Var(m.pos,m,nameThisDollar,targs[0],0),0);
					else
						m.params = (Var[])Arrays.insert(m.params,new Var(m.pos,m,nameThisDollar,targs[0],0),1);
					retype = true;
				}
				if( !Kiev.kaffe && !isInterface() && type.args.length > 0 && !(this.type instanceof MethodType) ) {
					targs = (Type[])Arrays.insert(targs,typeinfo_clazz.type,(retype?1:0));
					if( m.isStatic() )
						m.params = (Var[])Arrays.insert(m.params,new Var(m.pos,m,nameTypeInfo,typeinfo_clazz.type,0),(retype?1:0));
					else
						m.params = (Var[])Arrays.insert(m.params,new Var(m.pos,m,nameTypeInfo,typeinfo_clazz.type,0),(retype?2:1));
					retype = true;
				}
				if( retype ) {
					// Make new MethodType for the constructor
					m.type = MethodType.newMethodType(m.type.clazz,targs,m.type.ret);
					m.jtype = (MethodType)m.type.getJavaType();
					if( gens != null ) {
						for(int g=0; g < gens.length; g++) {
							gens[g].clazz.methods[i].type = (MethodType)Type.getRealType(gens[g],m.type);
							gens[g].clazz.methods[i].jtype = (MethodType)Type.getRealType(gens[g],m.type).getJavaType();
						}
					}
				}
			}
			if( !init_found ) {
				trace(Kiev.debugResolve,nameInit+" not found in class "+this);
				Method init = null;
				if( super_clazz != null && super_clazz.clazz == Type.tpClosureClazz ) {
					MethodType mt;
					Var thisOuter, maxArgs;
					if( !isStatic() ) {
						mt = MethodType.newMethodType(MethodType.tpMethodClazz,
							new Type[]{package_clazz.type,Type.tpInt},Type.tpVoid);
						init = new Method(this,nameInit,mt,ACC_PUBLIC);
						init.params = new Var[]{
								new Var(pos,init,nameThis,this.type,0),
								thisOuter=new Var(pos,init,nameThisDollar,package_clazz.type,0),
								maxArgs=new Var(pos,init,KString.from("max$args"),Type.tpInt,0),
							};
					} else {
						mt = MethodType.newMethodType(MethodType.tpMethodClazz,
							new Type[]{Type.tpInt},Type.tpVoid);
						init = new Method(this,nameInit,mt,ACC_PUBLIC);
						init.params = new Var[]{
								new Var(pos,init,nameThis,this.type,0),
								maxArgs=new Var(pos,init,KString.from("max$args"),Type.tpInt,0),
							};
					}
				} else {
					MethodType mt;
					Type[] targs = Type.emptyArray;
					Var[] params = new Var[]{new Var(pos,init,nameThis,this.type,0)};
					if( package_clazz.isClazz() && !isStatic() ) {
						targs = (Type[])Arrays.append(targs,package_clazz.type);
						params = (Var[])Arrays.append(params,new Var(pos,init,nameThisDollar,package_clazz.type,0));
					}
					if( !Kiev.kaffe && !isInterface() && type.args.length > 0 && !(this.type instanceof MethodType) ) {
						targs = (Type[])Arrays.append(targs,typeinfo_clazz.type);
						params = (Var[])Arrays.append(params,new Var(pos,init,nameTypeInfo,typeinfo_clazz.type,0));
					}
					if( isEnum() ) {
						targs = (Type[])Arrays.append(targs,Type.tpInt);
						params = (Var[])Arrays.append(params,new Var(pos,init,nameEnumVal,Type.tpInt,0));
						if (isPrimitiveEnum())
							params[0].type = Type.tpEnum;
					}
					mt = MethodType.newMethodType(MethodType.tpMethodClazz,targs,Type.tpVoid);
					init = new Method(this,nameInit,mt,ACC_PUBLIC);
					init.params = params;
					foreach(Var v; params) v.parent = init;
				}
				init.pos = pos;
				init.body = new BlockStat(pos,init);
				if( isEnum() )
					init.setPrivate(true);
				else
					init.setPublic(true);
				addMethod(init);
			}
		}
		else if( isInterface() ) {
			Struct defaults = null;
			for(int i=0; i < methods.length; i++) {
				methods[i].setPublic(true);
				if( !methods[i].isAbstract() ) {
					Method m = methods[i];
					if( m.isStatic() ) continue;
					// Now, non-static methods (templates)
					// Make it static and add abstract method
					Method abstr = new Method(m.parent,m.name.name,m.type,m.getFlags()|ACC_PUBLIC );
					abstr.pos = m.pos;
					abstr.setStatic(false);
					abstr.setAbstract(true);
					abstr.params = m.params;
					abstr.parent = this;
					methods[i] = abstr;

					// Make inner class name$default
					if( defaults == null ) {
						defaults = Env.newStruct(
							ClazzName.fromOuterAndName(this,nameIdefault),
							this,ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT, true
						);
						defaults.parent = this;
						defaults.setResolved(true);
						SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
						defaults.addAttr(sfa);
						defaults.type = Type.newRefType(defaults);
						defaults.super_clazz = Type.tpObject;
// jtype?						defaults.interfaces = (Type[])Arrays.append(defaults.interfaces,this.type);
					}
					m.setStatic(true);
					m.setVirtualStatic(true);
					Type[] types = (Type[])Arrays.insert(m.type.args,this.type,0);
					m.type = MethodType.newMethodType(null,types,m.type.ret);
					m.jtype = (MethodType)m.type.getJavaType();
					m.parent = defaults;
					defaults.addMethod(m);
				}
				if( isInterface() && !methods[i].isStatic()/*!methods[i].name.equals(nameClassInit)*/ ) {
					methods[i].setAbstract(true);
				}
			}
		}

		// Generate enum's methods
		if( isEnum() ) {
			// Cast from int
			MethodType tomet;
			//if (isPrimitiveEnum())
			// 	tomet = MethodType.newMethodType(null,new Type[]{Type.tpInt},getPrimitiveEnumType());
			//else
			 	tomet = MethodType.newMethodType(null,new Type[]{Type.tpInt},this.type);
			Method tome = new Method(this,nameCastOp,tomet,ACC_PUBLIC | ACC_STATIC);
			tome.pos = pos;
			tome.params = new Var[]{new Var(pos,tome,nameEnumVal,Type.tpInt,0)};
			tome.body = new BlockStat(pos,tome);
			SwitchStat sw = new SwitchStat(pos,tome.body,new VarAccessExpr(pos,tome.params[0]),ASTNode.emptyArray);
			EnumAttr ea;
			if (isPrimitiveEnum())
				ea = (PrimitiveEnumAttr)getAttr(attrPrimitiveEnum);
			else
				ea = (EnumAttr)getAttr(attrEnum);
			if( ea == null )
				throw new RuntimeException("enum structure "+this+" without "+attrEnum+" attribute");
			ASTNode[] cases = new ASTNode[ea.fields.length+1];
			for(int i=0; i < ea.fields.length; i++) {
				cases[i] = new CaseLabel(pos,sw,
					new ConstExpr(pos,Kiev.newInteger(ea.values[i])),
					new ASTNode[]{
						new ReturnStat(pos,null,new StaticFieldAccessExpr(pos,this,ea.fields[i]))
					});
			}
			cases[cases.length-1] = new CaseLabel(pos,sw,null,
					new ASTNode[]{
						new ThrowStat(pos,null,new NewExpr(pos,Type.tpCastException,Expr.emptyArray))
					});
			sw.cases = cases;
			((BlockStat)tome.body).addStatement(sw);
			addMethod(tome);

			// toString
			{
			MethodType tostrt, jtostrt;
			int acc_flags;
			if (isPrimitiveEnum()) {
				tostrt = MethodType.newMethodType(null,new Type[]{this.type},Type.tpString);
				jtostrt= MethodType.newMethodType(null,new Type[]{Type.tpInt},Type.tpString);
				acc_flags = ACC_PUBLIC | ACC_STATIC;
			} else {
				tostrt = MethodType.newMethodType(null,Type.emptyArray,Type.tpString);
				jtostrt=tostrt;
				acc_flags = ACC_PUBLIC;
			}
			Method tostr = new Method(this,KString.from("toString"),tostrt,acc_flags);
			tostr.name.addAlias(nameCastOp);
			tostr.pos = pos;
			tostr.jtype = jtostrt;
			if (isPrimitiveEnum()) {
				tostr.params = new Var[]{new Var(pos,tostr,nameEnumVal,this.type,0)};
			} else {
				tostr.params = new Var[]{new Var(pos,tostr,nameThis,this.type,0)};
			}
			tostr.body = new BlockStat(pos,tostr);
			if (isPrimitiveEnum()) {
				sw = new SwitchStat(pos,tostr.body,new VarAccessExpr(pos,tostr.params[0]),ASTNode.emptyArray);
			} else {
				sw = new SwitchStat(pos,tostr.body,new FieldAccessExpr(pos,(Field)Type.tpEnum.clazz.resolveName(nameEnumVal)),ASTNode.emptyArray);
			}
			cases = new ASTNode[ea.fields.length+1];
			for(int i=0; i < ea.fields.length; i++) {
				Field f = ea.fields[i];
				KString str = f.name.name;
				if (f.name.aliases != List.Nil) {
					str = f.name.aliases.head();
					str = str.substr(1,str.length()-1);
				}
				cases[i] = new CaseLabel(pos,sw,
					!isPrimitiveEnum() ? new ConstExpr(pos,Kiev.newInteger(ea.values[i]))
										:new StaticFieldAccessExpr(pos,this,f),
					new ASTNode[]{
						new ReturnStat(pos,null,new ConstExpr(pos,str))
					});
			}
			cases[cases.length-1] = new CaseLabel(pos,sw,null,
					new ASTNode[]{
						new ThrowStat(pos,null,new NewExpr(pos,Type.tpRuntimeException,Expr.emptyArray))
					});
			sw.cases = cases;
			((BlockStat)tostr.body).addStatement(sw);
			addMethod(tostr);
			}

			// fromString
			{
			MethodType fromstrt, jfromstrt;
			int acc_flags;
			if (isPrimitiveEnum()) {
				fromstrt = MethodType.newMethodType(null,new Type[]{Type.tpString},this.type);
				jfromstrt= MethodType.newMethodType(null,new Type[]{Type.tpString},Type.tpInt);
				acc_flags = ACC_PUBLIC | ACC_STATIC;
			} else {
				fromstrt = MethodType.newMethodType(null,new Type[]{Type.tpString},this.type);
				jfromstrt= fromstrt;
				acc_flags = ACC_PUBLIC | ACC_STATIC;
			}
			Method fromstr = new Method(this,KString.from("fromString"),fromstrt,acc_flags);
			fromstr.name.addAlias(nameCastOp);
			fromstr.pos = pos;
			fromstr.jtype = jfromstrt;
			fromstr.params = new Var[]{new Var(pos,fromstr,KString.from("val"),Type.tpString,0)};
			fromstr.body = new BlockStat(pos,fromstr);
			AssignExpr ae = new AssignExpr(pos,AssignOperator.Assign,
				new VarAccessExpr(pos,fromstr.params[0]),
				new CallAccessExpr(pos,
					new VarAccessExpr(pos,fromstr.params[0]),
					Type.tpString.clazz.resolveMethod(
						KString.from("intern"),KString.from("()Ljava/lang/String;"),true
					),
					Expr.emptyArray
				));
			((BlockStat)fromstr.body).addStatement(new ExprStat(pos,null,ae));
			for(int i=0; i < ea.fields.length; i++) {
				Field f = ea.fields[i];
				KString str = f.name.name;
				if (f.name.aliases != List.Nil) {
					str = f.name.aliases.head();
					str = str.substr(1,str.length()-1);
				}
				IfElseStat ifst = new IfElseStat(pos,null,
					new BinaryBooleanExpr(pos,BinaryOperator.Equals,
						new VarAccessExpr(pos,fromstr.params[0]),
						new ConstExpr(pos,str)),
					new ReturnStat(pos,null,new StaticFieldAccessExpr(pos,this,f)),
					null
					);
				((BlockStat)fromstr.body).addStatement(ifst);
			}
			((BlockStat)fromstr.body).addStatement(
				new ThrowStat(pos,null,new NewExpr(pos,Type.tpRuntimeException,Expr.emptyArray))
				);
			addMethod(fromstr);
			}
		}

		// generate native methods for creation/deletion native instance
		getCppNativeInstMethods();

	}

	private void getCppNativeInstMethods() {
		if (isExportCpp() && isClazz() && !isEnum() && !isPackage() && !isPizzaCase()) {
			int flags = ACC_PROTECTED;
			if (isAbstract()) flags |= ACC_ABSTRACT;
			else flags |= ACC_NATIVE;
			MethodType create_type = MethodType.newMethodType(null,Type.emptyArray,Type.tpInt);
			Method create = new Method(this,nameCppCreateInstance,create_type,create_type,flags);
			create.params = new Var[]{new Var(pos,create,nameThis,this.type,0)};
			this.addMethod(create);
			MethodType delete_type = MethodType.newMethodType(null,new Type[]{Type.tpInt},Type.tpVoid);
			Method delete = new Method(this,nameCppDeleteInstance,delete_type,delete_type,flags);
			delete.params = new Var[]{new Var(pos,delete,nameThis,this.type,0),new Var(pos,delete,KString.from("nself"),this.type,0)};
			this.addMethod(delete);
		}
	}

	public Method getOverwrittenMethod(Type base, Method m) {
		Method mm = null, mmret = null;
		if( super_clazz != null && !isInterface() )
			mm = super_clazz.clazz.getOverwrittenMethod(base,m);
		if( mmret == null && mm != null ) mmret = mm;
		trace(Kiev.debugMultiMethod,"lookup overwritten methods for "+base+" in "+this+" with "+methods.length+" methods");
		for(int i=0; i < methods.length; i++) {
			if( methods[i].isStatic() || methods[i].isPrivate() || methods[i].name.equals(nameInit) ) continue;
			if( !methods[i].name.equals(m.name) || methods[i].type.args.length != m.type.args.length ) {
//				trace(Kiev.debugMultiMethod,"Method "+m+" not matched by "+methods[i]+" in class "+this);
				continue;
			}
			MethodType mit = (MethodType)Type.getRealType(base,methods[i].jtype);
			if( m.jtype.equals(mit) ) {
				trace(Kiev.debugMultiMethod,"Method "+m+" overrides "+methods[i]+" of type "+mit+" in class "+this);
				mm = methods[i];
				// Append constraints to m from mm
				foreach(WorkByContractCondition cond; mm.conditions)
					m.conditions = (WorkByContractCondition[])Arrays.appendUniq(m.conditions,cond);
				if( mmret == null && mm != null ) mmret = mm;
				break;
			} else {
				trace(Kiev.debugMultiMethod,"Method "+m+" does not overrides "+methods[i]+" of type "+mit+" in class "+this);
			}
		}
		return mmret;
	}

/*	private void makeDispatchMethod(Method m) {
		Type[] dtypes = new Type[m.type.args.length];
		for(int i=0; i < dtypes.length; i++) {
			//if( m.type.args[i].isPizzaCase() )
			//	dtypes[i] = m.type.args[i].clazz.super_clazz;
			//else
			//	dtypes[i] = m.type.args[i].getNonArgsType();
			if( !m.type.args[i].isReference() )
				dtypes[i] = m.type.args[i];
			else
				dtypes[i] = Type.tpObject;
		}
		Method nm;
		if( m instanceof RuleMethod )
			nm = new RuleMethod(this,m.name.name,
				MethodType.newMethodType(null,dtypes,m.type.ret),
				m.getFlags() | ACC_PUBLIC | ACC_MULTIMETHOD | ACC_RULEMETHOD);
		else
			nm = new Method(this,m.name.name,
				MethodType.newMethodType(null,dtypes,m.type.ret),
				m.getFlags() | ACC_PUBLIC | ACC_MULTIMETHOD);
		nm.setStatic(m.isStatic());
		BlockStat nmbody = new BlockStat(0,nm);
		nmbody.addStatement(new ThrowStat(pos,nmbody,new NewExpr(pos,Type.tpMessageException,Expr.emptyArray)));
		Var[] nmvars = new Var[nm.type.args.length+1];
		if( nm.isStatic() ) {
			nmvars = new Var[nm.type.args.length];
			if( nm instanceof RuleMethod ) {
				nmvars[0] = new Var(0,nm,namePEnv,Type.tpRule,0);
				for(int k=1; k < nmvars.length; k++)
					nmvars[k] = new Var(0,nm,KString.from("arg"+k),nm.type.args[k],0);
			} else {
				for(int k=0; k < nmvars.length; k++)
					nmvars[k] = new Var(0,nm,KString.from("arg"+k),nm.type.args[k],0);
			}
		} else {
			nmvars = new Var[nm.type.args.length+1];
			if( nm instanceof RuleMethod ) {
				nmvars[0] = new Var(0,nm,nameThis,this.type,0);
				nmvars[1] = new Var(0,nm,namePEnv,Type.tpRule,0);
				for(int k=2; k < nmvars.length; k++)
					nmvars[k] = new Var(0,nm,KString.from("arg"+k),nm.type.args[k-1],0);
			} else {
				nmvars[0] = new Var(0,nm,nameThis,this.type,0);
				for(int k=1; k < nmvars.length; k++)
					nmvars[k] = new Var(0,nm,KString.from("arg"+k),nm.type.args[k-1],0);
			}
		}
		nm.params = nmvars;
		nm.body = nmbody;

		addMethod(nm);

		trace(Kiev.debugMultiMethod,"dispatch "+nm.parent+"."+nm+" for "+m+" added");
		m.setMultiMethod(true);
		nm.setMultiMethod(true);
	}

	private String getDispatchableNameSuffix(KString sign) {
		String s = Integer.toHexString(sign.calcFullIndex());
		while( s.length() < 8 ) s = "0"+s;
		return s;
	}

	public KString getDispatchableName(Method m) {
		int index;
		KString dn;
		if( (index=m.name.name.indexOf('$')) < 0 ) {
			dn = m.name.name;
		} else {
			dn = m.name.name.substr(0,index);
		}
		dn = new KStringBuffer(dn.length()+9).append_fast(dn).append_fast((byte)'$')
			.append(getDispatchableNameSuffix(m.type.signature)).toKString();
		return dn;
	}

	public void setDispatchableName(Method m) {
		KString newname = getDispatchableName(m);
		m.setPrivate(true);
		if( m.name.aliases != List.Nil ) {
			// check it already was renamed
			foreach(KString n; m.name.aliases; n.equals(newname))
				return;
		}
		m.name.addAlias(m.name.name);
		m.name.name = newname;
	}

	void debugMsg(boolean cond, String msg) {
		if( cond ) System.out.println(msg);
	}

	rule processDispatchMethods()
		pvar Method m;	// method we proceed
	{
		m @= methods,					// For all methods
		!m.$var.name.equals(nameInit),
		debugMsg(Kiev.debugMultiMethod, "Checking if "+m.$var+" needs to be dispatched"),
		needToBeDispatched(m.$var),		// Check it needs to be dispatched
		debugMsg(Kiev.debugMultiMethod, "Method "+m.$var+" needs to be dispatched"),
		{
			checkIsNotDispatched(m.$var),	// Check it's not already dispatched
			debugMsg(Kiev.debugMultiMethod, "Method "+m.$var+" has no a dispatch method already"),
			makeDispatchMethod(m.$var)		// and make a dispatch method
		;
			!Kiev.kaffe,
			setDispatchableName(m.$var)
		},
		false							// process next method
	}

	rule needToBeDispatched(Method m)
		pvar Type	at;		// argument type
		pvar Type	ata;	// argument type argument
		pvar Type	ata1;	// argument type argument of declared parametriezed class
	{
		m.isMultiMethod(),
		!m.type.getMMType().argsClassesEquals(m.type),
		debugMsg(Kiev.debugMultiMethod, "Needs to be dispatched because it's a multimethod and not the dispatcher"),
		$cut,true
	;	at @= m.type.args,
		{
			at.$var.clazz.isPizzaCase()
		;	!at.isArray(), at.$var.args.length > 0, at.$var.checkResolved(),
			ata @= at.$var.args & ata1 @= at.$var.clazz.type.args,
			!ata.$var.clazz.isArgument(),
			!ata.$var.equals(ata1.$var),
			debugMsg(Kiev.debugMultiMethod, "Needs to be dispatched because of argument "+ata+" != "+ata1)
		},
		$cut,true
	}

	rule checkIsNotDispatched(Method m)
		pvar Method dm;		// didpatch method
	{
		dm @= methods,					// For all methods in this class
		dm.$var.name.equals(m.name),	// with the same name, args and static flag
		m.type.args.length==dm.$var.type.args.length && m.isStatic()==dm.$var.isStatic(),
		debugMsg(Kiev.debugMultiMethod, "Checking if method "+m+" is dispatched by "+dm+" ?.."),
		m.type.getMMType().argsClassesEquals(dm.$var.type),
		debugMsg(Kiev.debugMultiMethod, "Method "+m+" is probably dispatched by "+dm+" ?.."),
		notNeedToBeDispatched(dm.$var),	// and not need to be dispatched
		debugMsg(Kiev.debugMultiMethod, "Method "+m+" is already dispatched by "+dm),
		$cut, false
	;
		true
	}

	rule notNeedToBeDispatched(Method m)
	{
		needToBeDispatched(m),$cut,false;
		true
	}

	rule processMultiMethods()
		pvar List<Method> all_mm;
	{
		true
	}

	List<Method> collectAllPublicMethods(List<Method> mml) {
		if( super_clazz != null && !super_clazz.equals(Type.tpObject) && !super_clazz.isInstanceOf(Type.tpDynamic) )
			mml = super_clazz.clazz.collectAllPublicMethods(mml);
	l:	for(int i=0; i < methods.length; i++) {
			Method m = methods[i];
			if( m.isPublic() && !m.isStatic() && !m.name.equals(nameInit) ) {
				for(List<Method> ml = mml; ml != List.Nil; ml = ml.tail()) {
					if( m.name.equals(ml.head().name) && m.type.equals(ml.head().type) ) {
						((List.Cons<Method>)ml).head = m;
						continue l;
					}
				}
				mml = mml.concat(m);
			}
		}
		return mml;
	}

	ListBuffer<Method> collectAllMMmethods(ListBuffer<Method> mml) {
		if( super_clazz != null )
			mml = super_clazz.clazz.collectAllMMmethods(mml);
	l:	for(int i=0; i < methods.length; i++) {
			Method m = methods[i];
			if( !m.isMultiMethod() ) {
				foreach(Type t; m.type.args) {
					if( t.isPizzaCase() || t.isHasCases() || (!t.isArray() && t.args.length > 0) ) {
						Kiev.reportWarning(0,"Method "+m+" has to be declared as multimethod");
						m.setMultiMethod(true);
					}
				}
			}
			if( m.isMultiMethod() ) {
				for(int j=0; j < mml.length(); j++) {
					if( m.name.equals(mml.getAt(j).name) && m.type.equals(mml.getAt(j).type) ) {
						mml.setAt(j,m);
						continue l;
					}
				}
				mml = mml.append(m);
			}
		}
		return mml;
	}
*/

/*	ListBuffer<Method> collectAllMMmethods(Method mm, ListBuffer<Method> mml) {
		if( super_clazz != null )
			mml = super_clazz.clazz.collectAllMMmethods(mm,mml);
		trace(Kiev.debugMultiMethod,"Collecting MM methods above "+mm+" in class "+this);
	l:	for(int i=0; i < methods.length; i++) {
			Method m = methods[i];
			if( m == mm ) continue;
			if( mm.isStatic() != m.isStatic() ) continue;
			if( !m.name.equals(mm.name) ) continue;
			if( !m.type.greater(mm.type) ) {
				trace(Kiev.debugMultiMethod,"Method "+m+" is not greater then "+mm);
				// Check that m is a dispatched by mm method
				if( mm.type.equals(m.type) && m.name.name.length() > mm.name.name.length() ) {
					KString newname = getDispatchableName(m);
					if( m.name.equals(newname) ) // yes, it is
						trace(Kiev.debugMultiMethod,"Method "+m+" is dispatched by "+mm);
					else continue;
				}
				else continue;
			}
			trace(Kiev.debugMultiMethod,"Method "+m+" is greater then "+mm);
			for(int j=0; j < mml.length(); j++) {
				if( m.name.equals(mml.getAt(j).name) && m.type.equals(mml.getAt(j).type) ) {
					mml.setAt(j,m);
					trace(Kiev.debugMultiMethod,"Method "+m+" aready in the list");
					continue l;
				}
			}
			mml = mml.append(m);
//			for(List<Method> ml = mml; ml != List.Nil; ml = ml.tail()) {
//				if( m.name.name.equals(ml.head().name.name) && m.type.equals(ml.head().type) ) {
//					((List.Cons<Method>)ml).head = m;
//					trace(Kiev.debugMultiMethod,"Method "+m+" aready in the list");
//					continue l;
//				}
//			}
//			trace(Kiev.debugMultiMethod,"Method "+m+" is added to list");
//			mml = mml.concat(m);
//		}
		return mml;
	}

	List<Method> collectAllMethods(KString nm, int argslen, List<Method> mml) {
		if( super_clazz != null )
			mml = super_clazz.clazz.collectAllMethods(nm,argslen,mml);
		trace(Kiev.debugMultiMethod,"Collecting MM methods above "+nm+" in class "+this);
	l:	for(int i=0; i < methods.length; i++) {
			Method m = methods[i];
			if( !m.name.equals(nm) || m.type.args.length != argslen ) continue;
			for(List<Method> ml = mml; ml != List.Nil; ml = ml.tail()) {
				if( m.name.equals(ml.head().name) && m.type.equals(ml.head().type) ) {
					((List.Cons<Method>)ml).head = m;
					trace(Kiev.debugMultiMethod,"Method "+m+" aready in the list");
					continue l;
				}
			}
			trace(Kiev.debugMultiMethod,"Method "+m+" is added to list");
			mml = mml.concat(m);
		}
		return mml;
	}
*/

	public void autoGenerateStatements() {

		if( Kiev.debug ) System.out.println("AutoGenerating statements for "+this);
		assert( PassInfo.clazz == this );
		// <clinit> & common$init, if need
		Method class_init = null;
		BlockStat instance_init = null;

		for(int i=0; i < fields.length; i++) {
			Field f = fields[i];
			if( isInterface() ) {
				f.setStatic(true);
				f.setFinal(true);
			}
			if( f.init == null ) continue;
			if( f.init.isConstantExpr() && f.init.getConstValue() == null ) continue;
			else if( f.isStatic() && f.init.isConstantExpr() && !f.name.equals(KString.Empty) ) {
				// Attribute may already be assigned (see resolveFinalFields)
				if( f.getAttr(attrConstantValue) == null ) {
					ConstantValueAttr a;
					if( f.init instanceof ConstExpr )
						a = new ConstantValueAttr((ConstExpr)f.init);
					else
						a = new ConstantValueAttr(new ConstExpr(f.init.pos,f.init.getConstValue()));
					f.addAttr(a);
				}
			}
			else if( f.isStatic() ) {
				if( class_init == null ) {
					foreach(Method m; methods; m.name.equals(nameClassInit) ) {
						class_init = m;
						break;
					}
				}
				if( class_init == null ) {
					class_init = new Method(this,nameClassInit,
						MethodType.newMethodType(null,null,Type.tpVoid),ACC_STATIC);
					class_init.pos = pos;
					addMethod(class_init);
					class_init.body = new BlockStat(pos,class_init);
				}
				if( f.name.equals(KString.Empty) )
					((BlockStat)class_init.body).addStatement(((StatExpr)f.init).stat);
				else
					((BlockStat)class_init.body).addStatement(
						new ExprStat(f.init.getPos(),class_init.body,
							new InitializeExpr(f.init.getPos(),AssignOperator.Assign
								,new StaticFieldAccessExpr(f.pos,this,f),f.init,f.isInitWrapper())
						)
					);
			} else {
				if( instance_init == null ) {
					instance_init = new BlockStat(pos,instance_init);
				}
				Statement init_stat;
				if( f.name.equals(KString.Empty) ) {
					init_stat = ((StatExpr)f.init).stat;
				} else {
					init_stat = new ExprStat(f.init.getPos(),instance_init,
						new InitializeExpr(f.init.getPos(),AssignOperator.Assign,new FieldAccessExpr(f.pos,f),f.init,f.isInitWrapper())
					);
				}
				instance_init.addStatement(init_stat);
				init_stat.setHidden(true);
			}
			if( f.name.equals(KString.Empty) ) {
				Field[] nf = new Field[fields.length-1];
				for(int n=0,m=0; n < fields.length; n++)
					if( n!=i ) nf[m++] = fields[n];
				fields = nf;
				i--;
			}
		}

		// Generate super(...) constructor calls, if they are not
		// specified as first statements of a constructor
		if( !name.name.equals(Type.tpObject.clazz.name.name) ) {
			for(int i=0; i < methods.length; i++) {
				if( isInterface() && !methods[i].isAbstract() ) {
					Method m = methods[i];
					if( m.isStatic() ) continue;
					// Now, non-static methods (templates)
					// Make it static and add abstract method
					Method abstr = new Method(m.parent,m.name.name,m.type,m.getFlags() | ACC_PUBLIC );
					abstr.pos = m.pos;
					abstr.setStatic(false);
					abstr.setAbstract(true);
					abstr.params = m.params;

					m.setStatic(true);
					m.setVirtualStatic(true);
					this.addMethod(abstr);
				}
				if( isInterface() && !methods[i].isStatic()/*!methods[i].name.equals(nameClassInit)*/ ) {
					methods[i].setAbstract(true);
				}
				if( !methods[i].name.equals(nameInit) ) continue;

				Method m = methods[i];

				ASTNode initbody = m.body;

				if( m.isAbstract() ) continue;

				boolean gen_def_constr = false;
	            ASTNode[] stats;
	            if( initbody instanceof ASTBlock )
					stats = ((ASTBlock)initbody).stats;
				else
					stats = ((BlockStat)initbody).stats;
				if( stats.length==0 ) gen_def_constr = true;
	//			else if( !(stats[0] instanceof ExprStat) ) gen_def_constr = true;
				else {
					if( stats[0] instanceof ASTStatementExpression ) {
						ASTStatementExpression es = (ASTStatementExpression)stats[0];
						ASTNode ce = es.expr;
						if( es.expr instanceof ASTExpression )
							ce = ((ASTExpression)es.expr).nodes.head();
						else
							ce = es.expr;
						if( ce instanceof ASTCallExpression ) {
							KString nm = ((ASTCallExpression)ce).func;
							if( !(nm.equals(nameThis) || nm.equals(nameSuper) ) )
								gen_def_constr = true;
							else if( nm.equals(nameSuper) )
								m.setNeedFieldInits(true);
						}
						else
							gen_def_constr = true;
					}
					else if( stats[0] instanceof ExprStat ) {
						ExprStat es = (ExprStat)stats[0];
						if( es.expr instanceof CallExpr ) {
							KString nm = ((CallExpr)es.expr).func.name.name;
							if( !(nm.equals(nameThis) || nm.equals(nameSuper) || nm.equals(nameInit)) )
								gen_def_constr = true;
							else {
								if( nm.equals(nameSuper) || (nm.equals(nameInit) && ((CallExpr)es.expr).super_flag) )
									m.setNeedFieldInits(true);
								// autoinsert typeinfo if super class needs
								if( !Kiev.kaffe && super_clazz.args.length > 0 ) {
									CallExpr cae = (CallExpr)es.expr;
									// Insert our-generated typeinfo, or from childs class?
									if( m.type.args.length > 0 && m.type.args[0].isInstanceOf(typeinfo_clazz.type) )
										cae.args = (Expr[])Arrays.insert(cae.args,
											new VarAccessExpr(cae.pos,m.params[1])/*.resolveExpr(typeinfo_clazz.type)*/,0);
									else
										throw new RuntimeException("Don't know where to get "+typeinfo_clazz.type+" $typeinfo");
										/*cae.args = (Expr[])Arrays.insert(cae.args,
											accessTypeInfoField(cae.pos,cae,super_clazz)
												.resolveExpr(Type.tpTypeInfo)
											,0);*/
								}
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
					ASTCallExpression call_super = new ASTCallExpression(0);
					call_super.pos = pos;
					call_super.func = nameSuper;
					if( super_clazz.clazz == Type.tpClosureClazz ) {
						ASTIdentifier max_args = new ASTIdentifier(0);
						max_args.name = nameClosureMaxArgs;
						call_super.args = new Expr[]{max_args};
					}
					else if( package_clazz.isClazz() && isAnonymouse() ) {
						int skip_args = 0;
						if( !isStatic() ) skip_args++;
						if( this.type.args.length > 0 && super_clazz.args.length == 0 ) skip_args++;
						if( m.params.length > skip_args+1 ) {
							for(int i=skip_args+1; i < m.params.length; i++) {
								call_super.args = (Expr[])Arrays.append(
									call_super.args,
									new VarAccessExpr(m.pos,call_super,m.params[i])
									);
							}
						}
					}
					else if( isEnum() ) {
						ASTIdentifier index = new ASTIdentifier(0);
						index.name = nameEnumVal;
						call_super.args = new Expr[]{index};
					}
					// Insert our-generated typeinfo, or from childs class?
					/*else if( !Kiev.kaffe && super_clazz.args.length > 0 ) {
						call_super.args = new Expr[]{
							new VarAccessExpr(m.pos,m.params[1])
							};
					}
					*/
					stats = (ASTNode[])Arrays.insert(stats,
						new ASTStatementExpression(call_super),0);
				}
				int p = 1;
//				MethodParamsAttr pa = (MethodParamsAttr)methods[i].getAttr(attrMethodParams);
				if( package_clazz.isClazz() && !isStatic() ) {
					stats = (ASTNode[])Arrays.insert(stats,
						new ExprStat(pos,null,
							new AssignExpr(pos,AssignOperator.Assign,
								new FieldAccessExpr(pos,OuterThisAccessExpr.outerOf(this)),
								new VarAccessExpr(pos,methods[i].params[1])
							)
						),p++
					);
				}
				if (isExportCpp() && !super_clazz.clazz.isExportCpp() && !isEnum() && !isPackage() && !isPizzaCase()) {
					stats = (ASTNode[])Arrays.insert(stats,
						new ExprStat(pos,null,
							new AssignExpr(pos,AssignOperator.Assign,
								new FieldAccessExpr(pos,resolveField(nameThisNative)),
								new CallExpr(pos,resolveMethod(nameCppCreateInstance,nameCppCreateInstanceSign),Expr.emptyArray)
							)
						),p++
					);
				}
				if( isPizzaCase() ) {
					for(int j= package_clazz.isClazz() && !isStatic() ? 2 : 1;
												j < methods[i].params.length; j++ ) {
						if( methods[i].params[j].name.name == nameTypeInfo )
							continue;
						Field f = resolveField(methods[i].params[j].name.name);
						if( f == null )
							throw new RuntimeException("Can't find field "+methods[i].params[j].name.name);
						stats = (ASTNode[])Arrays.insert(stats,
							new ExprStat(pos,null,
								new AssignExpr(pos,AssignOperator.Assign,
									new FieldAccessExpr(pos,f),
									new VarAccessExpr(pos,methods[i].params[j])
								)
							),p++
						);
					}
				}
				if( !Kiev.kaffe && type.args.length > 0
				 && !(this.type instanceof MethodType)
				 && m.isNeedFieldInits()
				) {
					Field tif = resolveField(nameTypeInfo);
					Var v = null;
					foreach(Var vv; m.params; vv.name.equals(nameTypeInfo) ) {
						v = vv;
						break;
					}
					assert(v != null);
					stats = (ASTNode[])Arrays.insert(stats,
						new ExprStat(pos,null,
							new AssignExpr(m.pos,AssignOperator.Assign,
								new FieldAccessExpr(m.pos,tif),
								new VarAccessExpr(m.pos,v)
							)),
						p++);
				}
				if( instance_init != null && m.isNeedFieldInits() ) {
					stats = (ASTNode[])Arrays.insert(stats,instance_init,p++);
				}
	            if( initbody instanceof ASTBlock )
					((ASTBlock)initbody).stats = stats;
				else
					((BlockStat)initbody).stats = stats;
			}
		}

		// add destruction of native instance
		if (isExportCpp() && isClazz() && !isEnum() && !isPackage() && !isPizzaCase()) {
			Method fin = resolveMethod(nameFinalize,nameFinalizeSig,false);
			if (fin == null || fin.parent != this) {
				fin = new Method(this,nameFinalize,MethodType.newMethodType(null,Type.emptyArray,Type.tpVoid),ACC_PUBLIC);
				fin.params = new Var[]{new Var(pos,fin,nameThis,this.type,0)};
				this.addMethod(fin);
				fin.body = new BlockStat(pos,fin);
			}
			BlockStat body = (BlockStat)fin.body;
			body.addStatement(
				new ExprStat(pos,body,
					new CallExpr(pos,
						resolveMethod(nameCppDeleteInstance,nameCppDeleteInstanceSign),
						new Expr[]{new FieldAccessExpr(pos,resolveField(nameThisNative))}
					)
				)
			);
		}

//		for(int i=0; i < methods.length; i++) {
//			Method m = methods[i];
//			if( !m.isProduction() ) continue;
//			((Production)m).preresolve();
//		}

//		for(int i=0; i < methods.length; i++) {
//			Method m = methods[i];
//			if( !m.isMultiMethod() ) continue;
//			if( m instanceof RuleMethod && m.body instanceof ASTRuleBlock ) continue;
//			makeDispatch(m);
//		}

	}

	protected void combineMethods() {
		for(int i=0; i < methods.length; i++) {
			Method m = methods[i];
			if (m.name.equals(nameClassInit) || m.name.equals(nameInit))
				continue;
			if( m.isMultiMethod() ) {
				trace(Kiev.debugMultiMethod,"Multimethod "+m+" already processed...");
				continue; // do not process method twice...
			}
			MethodType type1 = m.dtype;
			if (type1 == null)
				type1 = m.type;
			trace(Kiev.debugMultiMethod,"Generating dispatch method for "+m+" with dispatch type "+type1);
			Method mm = null;
			// find all methods with the same java type
			ListBuffer<Method> mlistb = new ListBuffer<Method>();
			for(int j=i; j < methods.length; j++) {
				if (m.isRuleMethod() != methods[j].isRuleMethod())
					continue;
				if( !methods[j].name.equals(m.name) || methods[j].type.args.length != m.type.args.length )
					continue;
				MethodType type2 = methods[j].dtype;
				if (type2==null) type2 = methods[j].type;
				if (type1.isMultimethodSuper(type2)) {
					trace(Kiev.debugMultiMethod,"added dispatchable method "+methods[j]);
					if (mm == null && type1.equals(methods[j].type)) {
						trace(Kiev.debugMultiMethod,"will attach dispatching to this method "+methods[j]);
						mm = methods[j];
					}
					mlistb.append(methods[j]);
				} else {
					trace(Kiev.debugMultiMethod,"methods "+methods[j]+" with dispatch type "+type2+" doesn't match...");
				}
			}
			Method overwr = null;

			if (super_clazz != null )
				overwr = super_clazz.clazz.getOverwrittenMethod(this.type,m);

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
			if (mm != null)
				overwr = null;


			List<Method> mlist = mlistb.toList();
			int offs = 0;

			// create a new dispatcher method...
			if (mm == null) {
				// create dispatch method
				if (m.isRuleMethod())
					mm = new RuleMethod(this, m.name.name, type1, m.flags);
				else
					mm = new Method(this, m.name.name, type1, m.flags);
				mm.setStatic(m.isStatic());
				mm.params = new Var[m.params.length];
				if (!m.isStatic()) {
					mm.params[0] = new Var(pos,mm,Constants.nameThis,this.type,0);
					offs = 1;
				}
				for (int j=offs; j < mm.params.length; j++) {
					mm.params[j] = new Var(m.params[j].pos,mm,m.params[j].name.name,mm.type.args[j-offs],m.params[j].flags);
				}
			}

			// create mmtree
			MMTree mmt = new MMTree(mm);
			for(List<Method> ul = mlist; ul != List.Nil; ul = ul.tail())
				mmt.add(ul.head());

			trace(Kiev.debugMultiMethod,"Dispatch tree "+mm+" is:\n"+mmt);

			IfElseStat st = null;
			PassInfo.push(mm);
			try {
				st = makeDispatchStatInline(mm,mmt);
			} finally {
				PassInfo.pop(mm);
			}

			if (overwr != null) {
				IfElseStat last_st = st;
				Statement br;
				while (last_st.elseSt != null)
					last_st = (IfElseStat)last_st.elseSt;
				Expr[] vae = new Expr[mm.params.length-offs];
				for(int k=0; k < vae.length; k++) {
					vae[k] = new CastExpr(0,mm.type.args[k],
						new VarAccessExpr(0,mm.params[k+offs]), Kiev.verify);
				}
				if( m.type.ret != Type.tpVoid ) {
					if( overwr.type.ret == Type.tpVoid )
						br = new BlockStat(0,last_st,new ASTNode[]{
							new ExprStat(0,null,new CallExpr(0,overwr,vae,true)),
							new ReturnStat(0,null,new ConstExpr(mm.pos,null))
						});
					else {
						if( !overwr.type.ret.isReference() && mm.type.ret.isReference() )
							br = new ReturnStat(0,last_st,CastExpr.autoCastToReference(
								new CallExpr(0,overwr,vae,true)));
						else
							br = new ReturnStat(0,last_st,new CallExpr(0,overwr,vae,true));
					}
				} else {
					br = new BlockStat(0,last_st,new ASTNode[]{
						new ExprStat(0,null,new CallExpr(0,overwr,vae,true)),
						new ReturnStat(0,null,null)
					});
				}
				last_st.elseSt = br;
				br.parent = last_st;
			}
			if (st != null) {
				BlockStat body = new BlockStat(0,mm);
				body.addStatement(st);
				mm.body = body;
			}
			mm.setMultiMethod(true);
			boolean add_mm = true;
			foreach (Method rm; mlist) {
				// remove method, if need...
				if (mm != rm) {
					removeMethod(rm);
					offs = m.isStatic() ? 0 : 1;
					if (!rm.type.ret.equals(mm.type.ret)) {
						// insert new method
						Method nm = new Method(this,rm.name.name,rm.type,rm.flags);
						nm.pos = rm.pos;
						nm.name = rm.name;
						nm.params = rm.params;
						Expr[] vae = new Expr[mm.params.length-offs];
						for(int k=0; k < vae.length; k++) {
							vae[k] = new VarAccessExpr(0,mm.params[k+offs]);
						}
						nm.body = new BlockStat(0,nm,new ASTNode[]{
							new ReturnStat(0,null,
								new CastExpr(0,rm.type.ret,
									new CallExpr(0,mm,vae)))
							});
						addMethod(nm);
					}
					// also, check if we just removed current method,
					// and correct iterator index
					if (m == rm)
						i--;
				} else {
					add_mm = false;	// do not add it
				}
			}
			if (add_mm)
				addMethod(mm);
		}

		// Setup java types for methods
		for(int i=0; i < methods.length; i++) {
			if( methods[i].isStatic() || methods[i].isPrivate() || methods[i].name.equals(nameInit) ) continue;
			Method m = null;
			if( super_clazz != null )
				m = super_clazz.clazz.getOverwrittenMethod(this.type,methods[i]);
			foreach(Type si; interfaces ) {
				if( m == null ) m = si.clazz.getOverwrittenMethod(this.type,methods[i]);
				else si.clazz.getOverwrittenMethod(this.type,methods[i]);
			}
			if( m == null ) {
				methods[i].jtype = (MethodType)methods[i].type.getJavaType();
			} else {
				methods[i].jtype = m.jtype;
			}
		}

	}

/*	public void makeDispatch(Method m) {
			trace(Kiev.debugMultiMethod,"Generating dispatch entry for "+m);
			List<Method> upper = collectAllMMmethods(m,new ListBuffer<Method>()).toList();
			if( upper == List.Nil ) {
				trace(Kiev.debugMultiMethod,"No methods to dispatch for "+m);
				return;
			}
			trace(Kiev.debugMultiMethod,"Methods to dispatch for "+m+" are:\n"+upper);
			// Now build dispatch tree of methods
			MMTree mmt = new MMTree(m);
			for(List<Method> ul = upper; ul != List.Nil; ul = ul.tail())
				mmt.add(ul.head());
			trace(Kiev.debugMultiMethod,"Dispatch tree "+m+" is:\n"+mmt);
			Statement st = makeDispatchStat(m,mmt);
			if( m.body instanceof ASTBlock )
				((ASTBlock)m.body).stats = (ASTNode[])Arrays.insert(((ASTBlock)m.body).stats,st,0);
			else
				((BlockStat)m.body).stats = (ASTNode[])Arrays.insert(((BlockStat)m.body).stats,st,0);
	}
*/
/*	IfElseStat makeDispatchStat(Method mm, MMTree mmt) {
		Type.tpNull.checkResolved();
		int moffs = mm.name.name.startsWith(KString.from("$message$")) ? 2 : 0;
		int voffs = mm.isStatic()? 0 : 1;
		int roffs = 0;//(mm instanceof RuleMethod) ? 1 : 0;
		IfElseStat dsp = null;
		BooleanExpr cond = null;
		for(int i=0; i < mmt.uppers.length; i++) {
			if( mmt.uppers[i] == null ) continue;
			Method m = mmt.uppers[i].m;
			for(int j=0; j < m.type.args.length; j++) {
				Type t = m.type.args[j];
				if( mmt.m != null && t.equals(mmt.m.type.args[j]) ) continue;
				BooleanExpr be = null;
				if( mmt.m != null && !t.clazz.equals(mmt.m.type.args[j].clazz) )
					be = new InstanceofExpr(pos,
						new VarAccessExpr(pos,mm.params[j+voffs+moffs]),
						Type.getRefTypeForPrimitive(t));
				if( !Kiev.kaffe && t.args.length > 0 && !t.isArray() && !(t instanceof MethodType) ) {
					BooleanExpr tibe = new BooleanWrapperExpr(pos, new CallAccessExpr(pos,
						accessTypeInfoField(pos,this,t),
						Type.tpTypeInfo.clazz.resolveMethod(
							KString.from("$instanceof"),KString.from("(Ljava/lang/Object;Lkiev/stdlib/TypeInfo;)Z")),
						new Expr[]{
							new VarAccessExpr(pos,mm.params[j+voffs+moffs]),
							new AccessExpr(pos,
								new CastExpr(pos,t,new VarAccessExpr(pos,mm.params[j+voffs+moffs])),
								t.clazz.resolveField(nameTypeInfo))
						}));
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
				cond = new ConstBooleanExpr(mm.pos,true);
			IfElseStat br;
			if( mmt.uppers[i].uppers.length==0 ) {
				Expr[] vae = new Expr[mm.params.length-voffs-moffs-roffs];
				for(int k=0; k < vae.length; k++) {
					if( mm.params[k+voffs+moffs+roffs].type.isReference() && !m.type.args[k+roffs].isReference() )
						vae[k] = CastExpr.autoCastToPrimitive(new CastExpr(
							0,Type.getRefTypeForPrimitive(m.type.args[k+roffs]),
							new VarAccessExpr(0,mm.params[k+voffs+moffs+roffs]), Kiev.verify)
							);
					else
						vae[k] = new CastExpr(0,m.type.args[k+roffs],
							new VarAccessExpr(0,mm.params[k+voffs+moffs+roffs]), Kiev.verify);
				}
				Statement st;
				if( mm.type.ret != Type.tpVoid ) {
					if( mmt.uppers[i].m.type.ret == Type.tpVoid )
						st = new BlockStat(0,dsp,new ASTNode[]{
							new ExprStat(0,null,new CallExpr(0,mmt.uppers[i].m,vae)),
							new ReturnStat(0,null,new ConstExpr(mm.pos,null))
						});
					else {
						if( !mmt.uppers[i].m.type.ret.isReference() && mm.type.ret.isReference() )
							st = new ReturnStat(0,null,CastExpr.autoCastToReference(
								new CallExpr(0,mmt.uppers[i].m,vae)));
						else
							st = new ReturnStat(0,null,new CallExpr(0,mmt.uppers[i].m,vae));
					}
				} else {
					st = new BlockStat(0,dsp,new ASTNode[]{
						new ExprStat(0,null,new CallExpr(0,mmt.uppers[i].m,vae)),
						new ReturnStat(0,null,null)
					});
				}
				br = new IfElseStat(0,null,cond,st,null);
			} else {
				br = new IfElseStat(0,null,cond,makeDispatchStat(mm,mmt.uppers[i]),null);
			}
			cond = null;
			if( dsp == null ) dsp = br;
			else {
				IfElseStat st = dsp;
				while( st.elseSt != null ) st = (IfElseStat)st.elseSt;
				st.elseSt = br;
				br.parent = st;
			}
		}
		if( mmt.m != mm ) {
			Statement br;
			if( mmt.m == null )
				br = new BreakStat(mm.pos,null,null);
			else {
				Expr[] vae = new Expr[mm.params.length-voffs-moffs-roffs];
				for(int k=0; k < vae.length; k++) {
					vae[k] = new CastExpr(0,mmt.m.type.args[k+roffs],
						new VarAccessExpr(0,mm.params[k+voffs+moffs+roffs]), Kiev.verify);
				}
				if( mm.type.ret != Type.tpVoid ) {
					br = new ReturnStat(0,null,new CallExpr(0,mmt.m,vae));
				} else {
					br = new BlockStat(0,dsp,new ASTNode[]{
						new ReturnStat(0,null,null)
					});
				}
			}
			IfElseStat st = dsp;
			while( st.elseSt != null ) st = (IfElseStat)st.elseSt;
			st.elseSt = br;
			br.parent = st;
		}
		return dsp;
	}
*/
	IfElseStat makeDispatchStatInline(Method mm, MMTree mmt) {
		Type.tpNull.checkResolved();
		int voffs = mm.isStatic()? 0 : 1;
		IfElseStat dsp = null;
		BooleanExpr cond = null;
		for(int i=0; i < mmt.uppers.length; i++) {
			if( mmt.uppers[i] == null ) continue;
			Method m = mmt.uppers[i].m;
			for(int j=0; j < m.type.args.length; j++) {
				Type t = m.type.args[j];
				if( mmt.m != null && t.equals(mmt.m.type.args[j]) ) continue;
				BooleanExpr be = null;
				if( mmt.m != null && !t.clazz.equals(mmt.m.type.args[j].clazz) )
					be = new InstanceofExpr(pos,
						new VarAccessExpr(pos,mm.params[j+voffs]),
						Type.getRefTypeForPrimitive(t));
				if( !Kiev.kaffe && t.args.length > 0 && !t.isArray() && !(t instanceof MethodType) ) {
					BooleanExpr tibe = new BooleanWrapperExpr(pos, new CallAccessExpr(pos,
						accessTypeInfoField(pos,this,t),
						Type.tpTypeInfo.clazz.resolveMethod(
							KString.from("$instanceof"),KString.from("(Ljava/lang/Object;Lkiev/stdlib/TypeInfo;)Z")),
						new Expr[]{
							new VarAccessExpr(pos,mm.params[j+voffs]),
							new AccessExpr(pos,
								new CastExpr(pos,t,new VarAccessExpr(pos,mm.params[j+voffs])),
								t.clazz.resolveField(nameTypeInfo))
						}));
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
				cond = new ConstBooleanExpr(mm.pos,true);
			IfElseStat br;
			if( mmt.uppers[i].uppers.length==0 ) {
				/*
				Expr[] decls = new Expr[mm.params.length-voffs];
				for(int k=0; k < decls.length; k++) {
					if( mm.params[k+voffs].type.isReference() && !m.type.args[k].isReference() )
						decls[k] = new AssignExpr(m.pos,AssignOperator.Assign,
							new VarAccessExpr(0,mm.params[k+voffs]),
							CastExpr.autoCastToPrimitive(new CastExpr(
								0,Type.getRefTypeForPrimitive(m.type.args[k]),
								new VarAccessExpr(0,mm.params[k+voffs]), Kiev.verify)
							));
					else
						decls[k] = new AssignExpr(m.pos,AssignOperator.Assign,
							new VarAccessExpr(0,mm.params[k+voffs]),
							new CastExpr(0,m.type.args[k],new VarAccessExpr(0,mm.params[k+voffs]), Kiev.verify));
				}
				*/
				Statement st = new InlineMethodStat(mmt.uppers[i].m.pos,mm,mmt.uppers[i].m,mm);
				br = new IfElseStat(0,null,cond,st,null);
			} else {
				br = new IfElseStat(0,null,cond,makeDispatchStatInline(mm,mmt.uppers[i]),null);
			}
			cond = null;
			if( dsp == null ) dsp = br;
			else {
				IfElseStat st = dsp;
				while( st.elseSt != null ) st = (IfElseStat)st.elseSt;
				st.elseSt = br;
				br.parent = st;
			}
		}
		if( mmt.m != mm && mmt.m != null) {
			Statement br;
			/*
			Expr[] decls = new Expr[mm.params.length-voffs];
			for(int k=0; k < decls.length; k++) {
				decls[k] = new AssignExpr(mmt.m.pos,AssignOperator.Assign,
					new VarAccessExpr(0,mm.params[k+voffs]),
					new CastExpr(0,mmt.m.type.args[k],
						new VarAccessExpr(0,mm.params[k+voffs]), Kiev.verify));
			}
			*/
			br = new InlineMethodStat(mmt.m.pos,mm,mmt.m,mm);
			IfElseStat st = dsp;
			while( st.elseSt != null ) st = (IfElseStat)st.elseSt;
			st.elseSt = br;
			br.parent = st;
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

	public void autoProxyMethods() {
		checkResolved();
		if( isMembersGenerated() ) return;
		if( isPackage() ) return;
		if( super_clazz != null && !super_clazz.clazz.isMembersGenerated() ) {
			if ( super_clazz.clazz.generated_from != null )
				super_clazz.clazz.generated_from.autoProxyMethods();
			else
				super_clazz.clazz.autoProxyMethods();
		}
		for(int i=0; i < interfaces.length; i++)
			if( !interfaces[i].clazz.isMembersGenerated() ) {
				if ( interfaces[i].clazz.generated_from != null )
					interfaces[i].clazz.generated_from.autoProxyMethods();
				else
					interfaces[i].clazz.autoProxyMethods();
			}
		ASTNode fu = parent;
		while( fu != null && !(fu instanceof FileUnit))
			fu = fu.parent;
		if( fu != null )
			Kiev.curFile = ((FileUnit)fu).filename;

		for(int i=0; i < interfaces.length; i++) {
			if ( interfaces[i].clazz.generated_from != null )
				interfaces[i].clazz.generated_from.autoProxyMethods(this);
			else
				interfaces[i].clazz.autoProxyMethods(this);
		}

		autoGenerateMembers();

		if( isClazz() ) {
			boolean make_abstract = false;
			foreach(Method m; methods; m.isAbstract()) {
				if( m.isStatic() ) {
					m.setBad(true);
					this.setBad(true);
					Kiev.reportError(m.pos,"Static method cannot be declared abstract");
				}
//				if( !isAbstract() ) {
//					m.setBad(true);
//					this.setBad(true);
//					make_abstract = true;
//					Kiev.reportError(m.pos,"Class "+this+" must be declared abstract because of method "+m);
//				}
			}
//			if( make_abstract ) this.setAbstract(true);
		}

		//if( !(isInterface() || isPackage()) ) {
		//	processDispatchMethods();
		//	processMultiMethods();
		//}

		// Check all methods
		if( !isAbstract() && isClazz() ) {
			List<Method> ms = List.Nil;
			ms = collectVTmethods(ms);
//			foreach(Method am; ms; am.isAbstract() ) {
//				Kiev.reportError(pos,"Class "+this+" must be declared abstract because of method "+am);
//				setAbstract(true);
//			}
		}

		setMembersGenerated(true);
		foreach(Struct s; sub_clazz)	s.autoProxyMethods();

		combineMethods();
	}

	// Check that Struct me implements all methods and
	// if not and method is VirtualStatic - add proxy method to 'me'
	public void autoProxyMethods(Struct me) {
		Struct defaults = null;
		foreach(Struct s; sub_clazz; s.isClazz() && s.name.short_name.equals(nameIdefault) ) {
			defaults = s;
			break;
		}
		for(int i=0; i < methods.length; i++) {
			if( methods[i].isStatic() ) continue;
			Struct s = me;
			boolean found = false;
		scan_class:
			for(;;) {
				for(int j=0; j < s.methods.length; j++) {
					if( !s.methods[j].isStatic()
					 && s.methods[j].name.equals(methods[i].name)
					) {
						if( Type.getRealType(me.type,s.methods[j].type).equals(
										Type.getRealType(me.type,methods[i].type)) ) {
							trace(Kiev.debugResolve,"chk: methods "+methods[i].name+
									Type.getRealType(me.type,s.methods[j].type)+
									" and "+methods[i].name+
									Type.getRealType(me.type,methods[i].type)+" equals");
							if( !s.methods[j].isPublic() ) {
								Kiev.reportWarning(0,"Method "+s+"."+s.methods[j]+" must be declared public");
								s.methods[j].setPublic(true);
							}
						 	found = true;
						 	break scan_class;
						 } else {
							trace(Kiev.debugResolve,"chk: methods "+methods[i].name+
									Type.getRealType(me.type,s.methods[j].type)+
									" and "+methods[i].name+
									Type.getRealType(me.type,methods[i].type)+" not equals");
						 }
					 }
				}
				if( s.super_clazz != null )
					s = s.super_clazz.clazz;
				else break;
			}
			if( found ) continue;
			// Not found, check for VirtualStatic()
			Method m = methods[i];
			// Check this methods was produced from non-abstract method
			for(int n=0; defaults != null && n < defaults.methods.length; n++) {
				if( defaults.methods[n].isStatic()
				 && m.name.equals(methods[n].name)
				 && m.type.args.length == (defaults.methods[n].type.args.length-1)
				) {
					boolean match = true;
					for(int p=0; p < m.type.args.length; p++) {
						if( !m.type.args[p].equals(defaults.methods[n].type.args[p+1]) ) {
							match = false;
							break;
						}
					}
					if( match ) {
//						System.out.println(""+m+" "+m.isStatic()+" == "+defaults.methods[n]+" "+defaults.methods[n].isStatic());
						m = defaults.methods[n];
						break;
					}
				}
//				System.out.println(""+m+" "+m.isStatic()+" != "+defaults.methods[n]+" "+defaults.methods[n].isStatic());
			}
			if( m.isStatic() && m.isVirtualStatic() ) {
				Type[] types = new Type[m.type.args.length-1];
				Var[] params = new Var[m.type.args.length];
				for(int l=0; l < types.length; l++)
					types[l] = m.type.args[l+1];
				Method proxy = new Method(me,m.name.name,
					MethodType.newMethodType(null,types,m.type.ret),
					m.getFlags() | ACC_PUBLIC );
				proxy.setPublic(true);
				for(int l=0; l < params.length; l++)
					params[l] = new Var(0,proxy,l==0?nameThis:KString.from("arg"+l),m.type.args[l],0);
				proxy.params = params;
				proxy.setStatic(false);
				proxy.setVirtualStatic(false);
				BlockStat bs = new BlockStat(0,proxy,ASTNode.emptyArray);
				Expr[] args = new Expr[m.type.args.length];
				for(int k=0; k < args.length; k++)
					args[k] = new VarAccessExpr(0,params[k]);
				CallExpr ce = new CallExpr(0,m,args);
				if( proxy.type.ret == Type.tpVoid ) {
					bs.addStatement(new ExprStat(0,bs,ce));
					bs.addStatement(new ReturnStat(0,bs,null));
				} else {
					bs.addStatement(new ReturnStat(0,bs,ce));
				}
				proxy.body = bs;
				me.addMethod(proxy);
			}
			else if (m.name.equals(nameGetTypeInfo))
				; // will be auto-generated later
			else {
				if( me.isInterface() )
					; // do not add methods to interfaces
				else if( me.isAbstract() ) {
					// Add abstract method
					Method proxy = new Method(me,m.name.name,m.type,m.getFlags() | ACC_PUBLIC | ACC_ABSTRACT);
					//proxy.jtype = m.jtype;
					me.addMethod(proxy);
				}
				else if( !m.name.equals(nameGetTypeInfo) )
					Kiev.reportWarning(me.pos,"Method "+m+" of interface "+this+" not implemented in "+me);
			}
		}
		// Check all sub-interfaces
		for(int i=0; i < interfaces.length; i++) {
			interfaces[i].clazz.autoProxyMethods(me);
		}
	}


	/** This routine validates declaration of class, auto-generates
		<init>()V method if no <init> declared,
		also, used to generate this$N fields and arguments for
		inner classes, case$tag field for case classes and so on
	*/
	public void checkIntegrity() {
	}

	public void resolveFinalFields(boolean cleanup) {
		trace(Kiev.debugResolve,"Resolving final fields for class "+name);
		PassInfo.push(this);
		NodeInfoPass.init();
		ScopeNodeInfoVector state = NodeInfoPass.pushState();
		state.guarded = true;
		try {
			// Resolve final values of class's fields
			for(int i=0; fields!=null && i < fields.length; i++) {
				Field f = fields[i];
				if( f == null || f.init == null || f.name.equals(KString.Empty) ) continue;
				if( /*f.isStatic() &&*/ f.init != null ) {
					try {
						if (isPrimitiveEnum())
							f.init = f.init.resolveExpr(f.type.clazz.getPrimitiveEnumType());
						else
							f.init = f.init.resolveExpr(f.type);
					} catch( Exception e ) {
						Kiev.reportError(f.init.pos,e);
					}
					trace(Kiev.debugResolve && f.init!= null && f.init.isConstantExpr(),
							(f.isStatic()?"Static":"Instance")+" fields: "+name+"::"+f.name+" = "+f.init);
				}
				if( cleanup && !f.isFinal() && f.init!=null && !f.init.isConstantExpr() ) {
					f.init = null;
				}
				if( f.isStatic() && f.init!=null && f.init.isConstantExpr() && f.init.getConstValue()!=null ) {
					if( f.getAttr(attrConstantValue) != null )
						f.addAttr(new ConstantValueAttr(new ConstExpr(f.init.pos,f.init.getConstValue()) ));
				}
			}
	   	    // Process inner classes and cases
	   	    if( !isPackage() ) {
				for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
					sub_clazz[i].resolveFinalFields(cleanup);
				}
			}
		} finally {
			NodeInfoPass.close();
			PassInfo.pop(this);
		}
	}

	public void resolveImports() {
		PassInfo.push(this);
		try {
			ASTNode[] old_imported = imported;
			imported = ASTNode.emptyArray;
			for(int i=0; i < old_imported.length; i++) {
				ASTNode imp = old_imported[i];
				if( imp instanceof ASTImport )
					imp = ((ASTImport)imp).pass2();
				if( imp instanceof Import) imp = ((Import)imp).node;
				if( imp == null )
					Kiev.reportWarning(pos,"Imported member "+imported[i]+" not found");
				else if( imp instanceof Field ) {
					if( !imp.isStatic() ) {
						Kiev.reportError(imp.pos,"Imported field "+imp+" must be static");
					} else {
						imported = (ASTNode[])Arrays.append(imported,imp);
					}
				}
				else if( imp instanceof Method ) {
					if( !imp.isStatic() ) {
						Kiev.reportError(imp.pos,"Imported method "+imp+" must be static");
					} else {
						imported = (ASTNode[])Arrays.append(imported,imp);
					}
				}
				else if( imp instanceof Struct ) {
					Struct is = (Struct)imp;
					is.checkResolved();
					for(int j=0; j < is.fields.length; j++) {
						if( is.fields[j].isStatic() && !is.fields[j].name.equals(KString.Empty) )
							imported = (ASTNode[])Arrays.append(imported,is.fields[j]);
					}
					for(int j=0; j < is.methods.length; j++) {
						if( is.methods[j].isStatic() )
							imported = (ASTNode[])Arrays.append(imported,is.methods[j]);
					}
				}
				else
					throw new CompilerException(imp.pos,"Unknown type if imported member: "+imp);
			}
			try {
				if( !isPackage() ) {
					for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
						if( !sub_clazz[i].isAnonymouse() /*&& !sub_clazz[i].isPassed_2()*/)
							sub_clazz[i].resolveImports();
					}
				}
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
			}
		} finally { PassInfo.pop(this); }
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isGenerated() ) return this;
		long curr_time;
		PassInfo.push(this);
		try {
			autoGenerateStatements();
		} finally { PassInfo.pop(this); }
		try {
			if( !isPackage() ) {
				for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
					if( !sub_clazz[i].isAnonymouse() /*&& !sub_clazz[i].isPassed_2()*/)
						sub_clazz[i].resolve(null);
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		}

		long diff_time = curr_time = System.currentTimeMillis();
		List<Struct> pstruct = new List.Cons<Struct>(this,List.Nil);
		if( !(isLocal() || isAnonymouse()) ) {
			ASTNode p = parent;
			while( p != null ) {
				if( p instanceof Struct ) pstruct = new List.Cons<Struct>((Struct)p,pstruct);
				p = p.parent;
			}
		}
		foreach(Struct ps; pstruct)
			PassInfo.push(ps);
		try {
			// Verify access
			foreach(Field f; fields) {
				try {
					f.type.clazz.checkResolved();
					f.type.clazz.acc.verifyReadWriteAccess(f.type.clazz);
				} catch(Exception e ) { Kiev.reportError(pos,e); }
			}
			foreach(Method m; methods) {
				try {
					m.type.ret.clazz.checkResolved();
					m.type.ret.clazz.acc.verifyReadWriteAccess(m.type.ret.clazz);
					foreach(Type t; m.type.args) {
						t.clazz.checkResolved();
						t.clazz.acc.verifyReadWriteAccess(t.clazz);
					}
				} catch(Exception e ) { Kiev.reportError(pos,e); }
			}

			for(int i=0; methods!=null && i < methods.length; i++) {
				methods[i] = (Method)methods[i].resolve(null);
			}
			if( type.args != null && type.args.length > 0 && !(type instanceof MethodType) ) {
				ClassArgumentsAttr a = new ClassArgumentsAttr();
				short[] argno = new short[type.args.length];
				for(int j=0; j < type.args.length; j++) {
					argno[j] = (short)j; // ((Argument)type.args[j].clazz).argno;
				}
				a.args = type.args;
				a.argno = argno;
				addAttr(a);
			}
			// Autogenerate hidden args for initializers of local class
			if( isLocal() ) {
				Field[] proxy_fields = Field.emptyArray;
				for(int i=0; i < fields.length; i++) {
					if( fields[i].isNeedProxy() )
						proxy_fields = (Field[])Arrays.append(proxy_fields,fields[i]);
				}
				if( proxy_fields.length > 0 ) {
					for(int i=0; methods!=null && i < methods.length; i++) {
						if( !methods[i].name.equals(nameInit) ) continue;
						Method m = methods[i];
						Type[] tps = m.type.args;
						for(int j=0; j < proxy_fields.length; j++) {
							int par = m.params.length;
							KString nm = new KStringBuffer().append(nameVarProxy)
								.append(proxy_fields[j].name).toKString();
							m.params = (Var[])Arrays.append(m.params,
								new Var(m.pos,m,nm,proxy_fields[j].type,0)
							);
							((BlockStat)m.body).stats = (ASTNode[])Arrays.insert(((BlockStat)m.body).stats,
								new ExprStat(m.pos,m.body,
									new AssignExpr(m.pos,AssignOperator.Assign,
										new FieldAccessExpr(m.pos,proxy_fields[j]),
										new VarAccessExpr(m.pos,m.params[par])
									)
								),1
							);
							tps = (Type[])Arrays.append(tps,proxy_fields[j].type);
						}
						m.type = MethodType.newMethodType(null,tps,m.type.ret);
						m.jtype = (MethodType)m.type.getJavaType();
					}
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally {
			pstruct = pstruct.reverse();
			foreach(Struct ps; pstruct)
				PassInfo.pop(ps);
		}
		setGenerated(true);
		diff_time = System.currentTimeMillis() - curr_time;
		if( Kiev.verbose ) Kiev.reportInfo("Resolved class "+this,diff_time);
		return this;
	}

	List<Method> addMethodsToVT(Type tp, List<Method> ms, boolean by_name_name) {
	next_method:
		foreach(Method m; methods; !m.isStatic() && !m.name.equals(nameInit) ) {
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
		if( super_clazz != null ) {
			ms = super_clazz.clazz.collectVTinterfaceMethods(
				Type.getRealType(tp,super_clazz),ms);
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
		if( super_clazz != null ) {
			ms = super_clazz.clazz.collectVTvirtualMethods(tp,ms);
		}
//		System.out.println("collecting in "+this);
		ms = addMethodsToVT(tp,ms,true);
		return ms;
	}

	List<Method> collectVTmethods(List<Method> ms) {
		ms = collectVTinterfaceMethods(this.type,ms);
		ms = collectVTvirtualMethods(this.type,ms);
		return ms;
	}

	public void generate() {
		Struct jthis = Kiev.argtype == null? this : Kiev.argtype.clazz;
//		if( Kiev.verbose ) System.out.println("[ Generating class  "+jthis+"]");
		if( Kiev.safe && isBad() ) return;
		PassInfo.push(this);
		try {
			if( !isPackage() ) {
				for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
					Struct s = sub_clazz[i];
					Type rt = Type.getRealType(Kiev.argtype,s.type);
					Type oldargtype = Kiev.argtype;
					Kiev.argtype = rt;
					try {
						s.generate();
					} finally { Kiev.argtype = oldargtype; }
				}
			}

			// Check all methods
			if( !isAbstract() && isClazz() ) {
				List<Method> ms = List.Nil;
				ms = collectVTmethods(ms);
//				System.out.println("VT: "+ms);
//				foreach(Method m; ms) {
//					if( m.isAbstract() ) throw new RuntimeException("Abstract method "+m+" in non-abstract class "+jthis);
//				}
			}

	        ConstPool.reInit();
			ConstPool.addClazzCP(jthis.type.signature);
			ConstPool.addClazzCP(jthis.type.java_signature);
			if( !Kiev.kaffe || type instanceof MethodType)
				ConstPool.addClazzCP(jthis.type.java_signature);
			if( super_clazz != null ) {
				super_clazz.clazz.checkResolved();
				ConstPool.addClazzCP(jthis.super_clazz.signature);
				if( !Kiev.kaffe || super_clazz instanceof MethodType)
					ConstPool.addClazzCP(jthis.super_clazz.java_signature);
			}
			for(int i=0; interfaces!=null && i < interfaces.length; i++) {
				interfaces[i].clazz.checkResolved();
				ConstPool.addClazzCP(jthis.interfaces[i].signature);
				if( !Kiev.kaffe )
					ConstPool.addClazzCP(jthis.interfaces[i].java_signature);
			}
			if( !isPackage() ) {
				for(int i=0; jthis.sub_clazz!=null && i < jthis.sub_clazz.length; i++) {
					jthis.sub_clazz[i].checkResolved();
					ConstPool.addClazzCP(jthis.sub_clazz[i].type.signature);
					if( !Kiev.kaffe )
						ConstPool.addClazzCP(jthis.sub_clazz[i].type.java_signature);
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
					ConstPool.addClazzCP(inner[j].type.signature);
				}
				a.inner = inner;
				a.outer = outer;
				a.acc = inner_access;
				addAttr(a);
			}

			if( packed_field_counter > 0 ) {
				addAttr(new PackedFieldsAttr(this));
			}

			if( isPackage() ) {
				for(int i=0; i < imported.length; i++) {
					addAttr(new ImportAttr(imported[i]));
				}
			}

			if( jthis.gens != null ) {
				jthis.addAttr(new GenerationsAttr(jthis.gens));
			} else {
				for(int i=0; i < attrs.length; i++) {
					if( attrs[i].name == attrGenerations ) {
						Attr[] a = new Attr[attrs.length-1];
						for(int k=0; k < i; k++) a[k] = attrs[k];
						for(int k=i+1; k < attrs.length; k++) a[k-1] = attrs[k];
						this.attrs = a;
						i--;
					}
				}
			}

			{
				int flags = 0;
				if( jthis.isWrapper() ) flags |= 1;

				if( flags != 0 ) jthis.addAttr(new FlagsAttr(flags) );
			}

			for(int i=0; attrs!=null && i < attrs.length; i++) attrs[i].generate();
			for(int i=0; fields!=null && i < fields.length; i++) {
				Field f = fields[i];
				ConstPool.addAsciiCP(f.name.name);
				ConstPool.addAsciiCP(Type.getRealType(Kiev.argtype,f.type).signature);
				if( !Kiev.kaffe )
					ConstPool.addAsciiCP(Type.getRealType(Kiev.argtype,f.type).java_signature);

				int flags = 0;
				if( f.isVirtual() ) flags |= 2;
				if( f.isForward() ) flags |= 8;
				if( f.isExportCpp() ) flags |= 64;
				if( f.isAccessedFromInner()) {
					flags |= (f.acc.flags << 24);
					f.setPrivate(false);
				}
				else if( f.isPublic() && f.acc.flags != 0xFF) flags |= (f.acc.flags << 24);
				else if( f.isProtected() && f.acc.flags != 0x3F) flags |= (f.acc.flags << 24);
				else if( f.isPrivate() && f.acc.flags != 0x03) flags |= (f.acc.flags << 24);
				else if( !f.isPublic() && !f.isProtected() && !f.isPrivate() && f.acc.flags != 0x0F) flags |= (f.acc.flags << 24);

				if( flags != 0 ) f.addAttr(new FlagsAttr(flags) );
				if( f.name.aliases != List.Nil ) f.addAttr(new AliasAttr(f.name));
				if( f.isPackerField() ) f.addAttr(new PackerFieldAttr(f));

				for(int j=0; f.attrs != null && j < f.attrs.length; j++)
					f.attrs[j].generate();
			}
			for(int i=0; methods!=null && i < methods.length; i++) {
				Method m = methods[i];
				m.type.checkJavaSignature();
				if( this != jthis )
					jthis.methods[i].type.checkJavaSignature();
				ConstPool.addAsciiCP(m.name.name);
				ConstPool.addAsciiCP(Type.getRealType(Kiev.argtype,m.type).signature);
				if( !Kiev.kaffe )
					ConstPool.addAsciiCP(Type.getRealType(Kiev.argtype,m.type).java_signature);
				if( !Kiev.kaffe && m.jtype != null )
					ConstPool.addAsciiCP(Type.getRealType(Kiev.argtype,m.jtype).java_signature);

				try {
					m.generate();

					int flags = 0;
					if( m.isMultiMethod() ) flags |= 1;
					if( m.isVarArgs() ) flags |= 4;
					if( m.isRuleMethod() ) flags |= 16;
					if( m.isInvariantMethod() ) flags |= 32;
					if( m.isExportCpp() ) flags |= 64;
					if( m.isAccessedFromInner()) {
						flags |= (m.acc.flags << 24);
						m.setPrivate(false);
					}

					if( flags != 0 ) m.addAttr(new FlagsAttr(flags) );
					if( m.name.aliases != List.Nil ) m.addAttr(new AliasAttr(m.name));

					if( m.isInvariantMethod() && m.violated_fields.length > 0 ) {
						m.addAttr(new CheckFieldsAttr(m.violated_fields));
					}

					for(int j=0; j < m.conditions.length; j++) {
						if( m.conditions[j].definer == m ) {
							m.addAttr(m.conditions[j].code);
						}
					}
					for(int j=0; m.attrs!=null && j < m.attrs.length; j++) {
						m.attrs[j].generate();
					}
				} catch(Exception e ) {
					Kiev.reportError(m.pos,"Compilation error: "+e);
					m.generate();
					for(int j=0; m.attrs!=null && j < m.attrs.length; j++) {
						m.attrs[j].generate();
					}
				}
				if( Kiev.safe && isBad() ) return;
			}
			ConstPool.generate();
			foreach(Method m; methods) {
				CodeAttr ca = (CodeAttr)m.getAttr(attrCode);
				if( ca != null ) {
					trace(Kiev.debugInstrGen," generating refs for CP for method "+this+"."+m);
					Code.generateCode2(ca);
				}
			}
			if( Kiev.safe && isBad() ) return;
			Bytecoder bc = new Bytecoder(this,null);
			bc.kievmode = true;
			byte[] dump = bc.writeClazz();
			Attr ka = addAttr(new KievAttr(dump));
			ka.generate();
			if( Kiev.safe && isBad() ) return;
			FileUnit.toBytecode(this);
			Env.setProjectInfo(name, true);
	        ConstPool.reInit();
			kiev.Main.runGC();
		} finally { PassInfo.pop(this); }
//		setPassed_3(true);
	}

	public Dumper toJavaDecl(Dumper dmp) {
		PassInfo.push(this);
		try {
		Struct jthis = Kiev.argtype == null? this : Kiev.argtype.clazz;
		if( Kiev.verbose ) System.out.println("[ Dumping class "+jthis+"]");
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( isInterface() || isArgument() ) {
			dmp.append("interface").forsed_space();
			if( isArgument() ) dmp.append("/*argument*/").space();
			dmp.append(jthis.name.short_name.toString()).space();
			if( type.args!=null && type.args.length > 0 ) {
				dmp.append("/* <");
				for(int i=0; i < type.args.length; i++) {
					dmp.append(Type.getRealType(Kiev.argtype,type).args[i]);
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
			if( super_clazz != null && !super_clazz.equals(Type.tpObject) && super_clazz.isReference())
				dmp.append("extends").forsed_space().append(jthis.super_clazz.clazz).forsed_space();
			if( interfaces!=null && interfaces.length > 0 ) {
				dmp.space().append("implements").forsed_space();
				for(int i=0; i < interfaces.length; i++) {
					dmp.append(Type.getRealType(Kiev.argtype,this.interfaces[i]).clazz);
					if( i < (interfaces.length-1) ) dmp.append(',').space();
				}
			}
		}
		dmp.forsed_space().append('{').newLine(1);
		if( !isPackage() ) {
			for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
				if( sub_clazz[i].isLocal() ) continue;
				if( sub_clazz[i].isArgument() ) continue;
//				if( !(sub_clazz[i] instanceof Struct) ) continue;
				sub_clazz[i].toJavaDecl(dmp).newLine();
			}
		}
		for(int i=0; fields!=null && i < fields.length; i++) {
			fields[i].toJavaDecl(dmp).newLine();
		}
		for(int i=0; methods!=null && i < methods.length; i++) {
			if( methods[i].name.equals(nameClassInit) ) continue;
			methods[i].toJavaDecl(dmp).newLine();
		}
		dmp.newLine(-1).append('}').newLine();
		} finally { PassInfo.pop(this); }
		return dmp;
	}

	public Dumper toCppDecl(Dumper dmp) {
		PassInfo.push(this);
		try {
		Struct jthis = Kiev.argtype == null? this : Kiev.argtype.clazz;
		if( Kiev.verbose ) System.out.println("[ Exporting to C++ .h   "+jthis+"]");

		String suffix = "";
		if (jthis == this && this.type.args.length > 0)
			suffix = "__A_";

		if (isEnum()) {
			dmp.append("enum").forsed_space();
		} else {
			dmp.append("class").forsed_space();
		}
		dmp.append(jthis.name.short_name.toString()).append(suffix);
		dmp.forsed_space();
		char parent_delimiter = ':';
		if( super_clazz != null && !super_clazz.equals(Type.tpObject) && !isEnum()) {
			super_clazz.clazz.name.make_cpp_name();
			dmp.append(parent_delimiter).append(" public ").append(super_clazz.clazz.name.cpp_name);
			parent_delimiter = ',';
		}
		if( interfaces!=null && interfaces.length > 0 ) {
			for(int i=0; i < interfaces.length; i++) {
				if (interfaces[i].isInstanceOf(Type.tpTypeInfoInterface)) continue;
				this.interfaces[i].clazz.name.make_cpp_name();
				dmp.append(parent_delimiter).append(" public ").append(this.interfaces[i].clazz.name.cpp_name);
				parent_delimiter = ',';
			}
		}

		dmp.forsed_space().append('{').newLine(1);
		for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
			if( sub_clazz[i].isLocal() ) continue;
			if( sub_clazz[i].isArgument() ) continue;
			if (sub_clazz[i].instanceOf(Type.tpTypeInfo.clazz)) continue;
			sub_clazz[i].toCppDecl(dmp).newLine();
		}

		if (!isEnum()) {
			if (!super_clazz.clazz.isExportCpp() && !isInterface())
				dmp.append("protected: jobject jself;").newLine();
			for(int i=0; fields!=null && i < fields.length; i++) {
				fields[i].toCppDecl(dmp).newLine();
			}
			dmp.newLines(2);
			dmp.append("//{ add native-only fields declarations here").newLine();
			dmp.append("//} end of your code").newLine();
			dmp.newLines(2);

			if (!isInterface())
				dmp.append("protected: ").append(jthis.name.short_name.toString()).append(suffix).append("();").newLines(2);

			for(int i=0; methods!=null && i < methods.length; i++) {
				methods[i].toCppDecl(dmp).newLine();
			}

			for(int i=0; methods!=null && i < methods.length; i++) {
				Method m = methods[i];
				if (m.isNative()) {
					dmp.append("private: static JNIEXPORT ");
					m.type.ret.toCppJNI(dmp);
					dmp.append(" JNICALL ");
					dmp.append(m.name.name).append("(JNIEnv*,");
					if (m.isStatic())	dmp.append("jclass");
					else				dmp.append("jobject");
					if (m.name.name.startsWith(nameSetNative)) {
						if (!m.isStatic()) {
							dmp.append(",jint nself,");
							m.type.args[1].toCppJNI(dmp);
						} else {
							dmp.append(",");
							m.type.args[0].toCppJNI(dmp);
						}
						dmp.append(" value");
					} else {
						for (int i=0, j=m.isStatic()?0:1; i < m.type.args.length; i++, j++) {
							dmp.append(',');
							Type argtp = Type.getRealType(Kiev.argtype,m.type.args[i]);
							argtp.toCppJNI(dmp);
							dmp.space().append(m.params[j].name);
						}
					}
					dmp.append(");").newLine();
				}
			}

			dmp.newLines(2);
			dmp.append("//{ add native-only methods declarations here").newLine();
			dmp.append("//} end of your code").newLine();
			dmp.newLines(2);

			dmp.append("private: static JNINativeMethod ").append("NativeMethodTable[];").newLine();
			dmp.append("public: static bool RegisterNativeMethods(JNIEnv *jenv);").newLine();
		} else {
			for(int i=0; fields!=null && i < fields.length; i++) {
				dmp.append(fields[i].name).append(" = ").append(fields[i].init);
				if ( i < fields.length-1)
					dmp.append(',');
				dmp.newLine();
			}
		}

		dmp.newLine(-1).append("};").newLines(2);
		} finally { PassInfo.pop(this); }
		return dmp;
	}

	public Dumper toCpp(Dumper dmp) {
		if (isEnum()) return dmp;

		dmp.append("//{ add native-only class initialization code here").newLine();
		dmp.append("//} end of your code").newLine();
		dmp.newLines(2);

		PassInfo.push(this);
		try {
			Struct jthis = Kiev.argtype == null? this : Kiev.argtype.clazz;
			if( Kiev.verbose ) System.out.println("[ Exporting to C++ .cpp "+jthis+"]");

			String suffix = "";
			if (jthis == this && this.type.args.length > 0)
				suffix = "__A_";

			// constructor
			dmp.append(jthis.name.short_name).append(suffix).append("::")
			   .append(jthis.name.short_name).append(suffix).append("()");
			if( super_clazz != null && !super_clazz.equals(Type.tpObject) && !isEnum()) {
				dmp.append(" : ").append(super_clazz.clazz.name.cpp_name).append("()");
			}
			dmp.newLine().append("{").newLine(1);
			for(int i=0; fields!=null && i < fields.length; i++) {
				Field f = fields[i];
				if (!f.isExportCpp() || f.isStatic()) continue;
				if (f.init == null) {
					if (f.type.clazz.isPrimitiveEnum()) {
						f.type.clazz.name.make_cpp_name();
						dmp.append(f.name).append(" = (").append(f.type.clazz.name.cpp_name).append(")0;").newLine();
					} else {
						dmp.append(f.name).append(" = ").append("0;").newLine();
					}
				} else {
					if (f.init.isConstantExpr()) {
						dmp.append(f.name).append(" = ");
						Object val = f.init.getConstValue();
						if (val == null || val instanceof KString) dmp.append('0');
						else if (val instanceof Character) {
							if (val.charValue() < 128)
								dmp.append("(jchar)").append(val);
							else
								dmp.append("(jchar)").append(Integer.toHexString((int)val.charValue()));
						}
						else dmp.append(val);
						dmp.append(";").newLine();
					}
				}
			}
			dmp.append("//{ add native instance initialization code here").newLine();
			dmp.append("//} end of your code").newLine();
			dmp.newLine(-1).append("}").newLines(2);

			for(int i=0; fields!=null && i < fields.length; i++) {
				Field f = fields[i];
				if (f.isExportCpp()) {
					KString nm_name = new KStringBuffer(nameSetNative.len+f.name.name.len)
						.append(nameSetNative).append(f.name.name).toKString();
					Method m;
					if (f.isStatic())
						m = resolveMethod(nm_name,KString.from("("+f.type.signature+")V"));
					else
						m = resolveMethod(nm_name,KString.from("(I"+f.type.signature+")V"));
					dmp.append("JNIEXPORT ");
					m.type.ret.toCppJNI(dmp).append(" JNICALL ");
					dmp.append(jthis.name.short_name).append(suffix).append("::").append(m.name);
					dmp.append("(JNIEnv* jenv, ");
					if (m.isStatic())	dmp.append("jclass jcls, ");
					else				dmp.append("jobject jself, ");
					if (!f.isStatic()) {
						dmp.append("jint nself, ");
						m.type.args[1].toCppJNI(dmp).append(" value");
					} else {
						m.type.args[0].toCppJNI(dmp).append(" value");
					}
					dmp.append(") {").newLine(1);
					if (!f.isStatic()) {
						dmp.append("register ").append(jthis.name.short_name).append(suffix).append("& self = ");
						dmp.append("*(").append(jthis.name.short_name).append(suffix).append("*)").append("nself;").newLine();
					}

					if (f.type.clazz.isEnum() && f.type.clazz.isPrimitiveEnum()) {
						f.type.clazz.name.make_cpp_name();
						if (!f.isStatic()) { dmp.append("self."); }
						dmp.append(f.name).append(" = ").append('(').append(f.type.clazz.name.cpp_name).append(')').append("value;").newLine();
					}
					else if (f.type.signature == nameKStringSignature) {
						dmp.append("if (self.").append(f.name).append(") free(self.").append(f.name).append(");").newLine();
						dmp.append("jbyteArray jbytes = (jbyteArray)jenv->CallObjectMethod(value,jenv->GetMethodID(jenv->GetObjectClass(value),\"getBytes\",\"()[B\"));").newLine();
						dmp.append("jint length = jenv->GetArrayLength(jbytes);").newLine();
						dmp.append("self.").append(f.name).append(" = (char*)malloc(length+1);").newLine();
						dmp.append("jenv->GetByteArrayRegion(jbytes,0,length,(jbyte*)self.").append(f.name).append(");").newLine();
						dmp.append("self.").append(f.name).append("[length] = 0;").newLine();
					}
					else if (f.type.clazz.isExportCpp()) {
						if (!f.isStatic()) { dmp.append("self."); }
						dmp.append(f.name).append(" = ").append('(')
						.append(Type.getRealType(Kiev.argtype,f.type)).append(')')
						.append("jenv->GetIntField(value,jenv->GetFieldID(jenv->GetObjectClass(value),\"this$native\",\"I\"));").newLine();
					}
					else if (f.type.isArray()) {
						Type t = Type.getRealType(Kiev.argtype,f.type).args[0];
						dmp.append("if (self.").append(f.name).append(") free(self.").append(f.name).append(");").newLine();
						dmp.append("jint length = jenv->GetArrayLength(value);").newLine();
						dmp.append("self.").append(f.name).append(" = (").append(t).append("*)");
						dmp.append("malloc(length*sizeof(").append(t).append("));").newLine();
						boolean is_primitive = true;
						if		(t == Type.tpByte)dmp.append("jenv->GetByteArrayRegion(value,0,length,(jbyte*)self.");
						else if (t == Type.tpBoolean)dmp.append("jenv->GetBooleanArrayRegion(value,0,length,(jboolean*)self.");
						else if (t == Type.tpChar)	dmp.append("jenv->GetCharArrayRegion(value,0,length,(jchar*)self.");
						else if (t == Type.tpShort)	dmp.append("jenv->GetShortArrayRegion(value,0,length,(jshort*)self.");
						else if (t == Type.tpInt)	dmp.append("jenv->GetIntArrayRegion(value,0,length,(jint*)self.");
						else if (t == Type.tpLong)	dmp.append("jenv->GetLongArrayRegion(value,0,length,(jlong*)self.");
						else if (t == Type.tpFloat)	dmp.append("jenv->GetFloatArrayRegion(value,0,length,(jfloat*)self.");
						else if (t == Type.tpDouble)dmp.append("jenv->GetDoubleArrayRegion(value,0,length,(jdouble*)self.");
						else {
							dmp.append("for (int i=0; i < length; i++) {").newLine(1);
							dmp.append("self.").append(f.name).append("[i] = (void*)jenv->GetObjectArrayElement(value,i);");
							dmp.newLine(-1).append("}").newLine();
							is_primitive = false;
						}
						if (is_primitive) dmp.append(f.name).append(");").newLine();
					}
					else {
						if (!f.isStatic()) { dmp.append("self."); }
						dmp.append(f.name).append(" = value;").newLine();
					}
					dmp.append("//{ add code for field '"+f.name.name+"' here ").newLine();
					dmp.append("//} end of your code").newLine();
					dmp.newLine(-1).append("}").newLine();
				}
			}
			if (!isAbstract()) {
				dmp.append("JNIEXPORT jint JNICALL ").append(jthis.name.short_name).append(suffix)
					.append("::native_CreateInstance(JNIEnv*jenv, jobject jself) {").newLine(1)
					.append(jthis.name.short_name).append(suffix).append("& self = *new ")
					.append(jthis.name.short_name).append(suffix).append("();").newLine()
					.append("self.jself = jself;").newLine()
					.append("//{ add ").append(jthis.name.short_name).append("::native_CreateInstance code here").newLine()
					.append("//} end of your code").newLine()
					.append("return (jint)&self;").newLine()
					.newLine(-1).append("}").newLine();
				dmp.append("JNIEXPORT void JNICALL ").append(jthis.name.short_name).append(suffix)
					.append("::native_DeleteInstance(JNIEnv* jenv, jobject jself, jint nself) {").newLine(1)
					.append("register ").append(jthis.name.short_name).append(suffix).append("& self = ")
					.append("*(").append(jthis.name.short_name).append(suffix).append("*)").append("nself;").newLine()
					.append("//{ add ").append(jthis.name.short_name).append("::native_DeleteInstance code here").newLine()
					.append("//} end of your code").newLine()
					.append("delete (").append(jthis.name.short_name).append(suffix).append("*)nself;").newLine()
					.newLine(-1).append("}").newLine();
			}

			dmp.newLines(2);
			dmp.append("//{ add rest of native code here").newLine();
			dmp.append("//} end of your code").newLine();
			dmp.newLines(2);

			int n_native = 0;
			dmp.append("static bool NativeMethodAlreadyRegistered;").newLine();
			dmp.append("JNINativeMethod ").append(jthis.name.short_name).append(suffix).append("::NativeMethodTable[] = {").newLine(1);
			for(int i=0; methods!=null && i < methods.length; i++) {
				Method m = methods[i];
				if (m.isNative()) {
					dmp.append("{ ");
					dmp.append("\"").append(m.name.name).append("\",\t");
					Type mt = Type.getRealType(Kiev.argtype,m.type);
					dmp.append("\"").append(mt.java_signature).append("\",\t");
					dmp.append(jthis.name.short_name).append(suffix).append("::").append(m.name.name);
					dmp.append(" },").newLine();
					n_native ++;
				}
			}
			dmp.newLine(-1).append("};").newLine();

			dmp.append("bool ").append(jthis.name.short_name).append(suffix).append("::RegisterNativeMethods(JNIEnv *jenv) {").newLine(1);
			dmp.append("if (NativeMethodAlreadyRegistered) return true;").newLine();
			dmp.append("jclass cls = jenv->FindClass(\"").append(jthis.name.bytecode_name).append("\");").newLine();
			dmp.append("if (!cls) return false;").newLine();
			dmp.append("bool res_cls = 0 == jenv->RegisterNatives(cls,NativeMethodTable,"+n_native+");").newLine();
			dmp.append("if (!res_cls) return false;").newLine();
			dmp.append("jenv->NewGlobalRef(cls);").newLine();
			dmp.append("NativeMethodAlreadyRegistered = true;").newLine();
			dmp.append("return true;").newLine();
			dmp.newLine(-1).append("};").newLine();


		} finally { PassInfo.pop(this); }
		return dmp;
	}

	public void cleanup() {
   	    if( !isPackage() ) {
			for(int i=0; sub_clazz!=null && i < sub_clazz.length; i++) {
					sub_clazz[i].cleanup();
			}
		}

		for(int i=0; fields!=null && i < fields.length; i++) {
			Field f = fields[i];
			if( f.init != null && !f.isFinal() && !f.init.isConstantExpr() )
				f.init = null;
			else if( f.init != null )
				f.init.parent = null;
			f.attrs = Attr.emptyArray;
		}
		for(int i=0; methods!=null && i < methods.length; i++) {
			methods[i].params = Var.emptyArray;
			methods[i].body = null;
			Attr[] ats = methods[i].attrs;
			methods[i].attrs = Attr.emptyArray;
			for(int j=0; j < ats.length; j++) {
				if( ats[j].name.equals(attrExceptions) )
					methods[i].addAttr(ats[j]);
			}
			methods[i].cleanup();
		}
		Attr[] ats = this.attrs;
		this.attrs = Attr.emptyArray;
		for(int j=0; j < ats.length; j++) {
			if( ats[j].name.equals(attrSourceFile)
			||  ats[j].name.equals(attrPizzaCase)
			||  ats[j].name.equals(attrEnum)
			||  ats[j].name.equals(attrPrimitiveEnum)
			||  ats[j].name.equals(attrRequire)
			||  ats[j].name.equals(attrEnsure)
			)
				addAttr(ats[j]);
		}
		//typeinfo_related = null;
	}

	public boolean setBody(Statement body) {
		if( !isPizzaCase() ) return false;
		Method init = methods[0];
		if (init.body != null)
			((BlockStat)init.body).addStatement(body);
		else
			init.setBody(body);
		return true;
	}
}


