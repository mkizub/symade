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
public class Struct extends TypeDecl implements PreScanneable {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="false")	DNode[]		members;
	}

	@virtual typedef This  = Struct;
	@virtual typedef JView = JStruct;
	@virtual typedef RView = RStruct;

		 public KString						b_name;	// bytecode name
		 public WrapperMetaType				wmeta_type;
		 public ASTNodeMetaType				ameta_type;
	@ref public TypeAssign					ometa_tdef;
	@att public TypeRef						view_of;
	@ref public Struct						typeinfo_clazz;
	@ref public Struct						iface_impl;
	@ref public DNode[]						sub_decls;
	public kiev.be.java15.Attr[]			attrs = kiev.be.java15.Attr.emptyArray;

	public void callbackChildChanged(AttrSlot attr) {
		if (attr.name == "package_clazz")
			this.callbackSuperTypeChanged(this);
		else if (attr.name == "id")
			resetNames();
		else
			super.callbackChildChanged(attr);
	}
	
	private void resetNames() {
		if (id.uname != null) { // initialized!
			q_name = null;
			b_name = null;
			foreach (Struct s; sub_decls)
				s.resetNames();
		}
	}
	
	public boolean isClazz() {
		return !isPackage() && !isInterface() && !isSyntax();
	}
	
	// a pizza case	
	public final boolean isPizzaCase() {
		return this.is_struct_pizza_case;
	}
	public final void setPizzaCase(boolean on) {
		if (this.is_struct_pizza_case != on) {
			assert(!locked);
			this.is_struct_pizza_case = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// has pizza cases
	public final boolean isHasCases() {
		return this.is_struct_has_pizza_cases;
	}
	public final void setHasCases(boolean on) {
		if (this.is_struct_has_pizza_cases != on) {
			assert(!locked);
			this.is_struct_has_pizza_cases = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that structure members were generated
	public final boolean isMembersGenerated() {
		return this.is_struct_fe_passed || this.is_struct_members_generated;
	}
	public final void setMembersGenerated(boolean on) {
		assert (!this.is_struct_fe_passed);
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
	// a pizza case	
	public final boolean isCompilerNode() {
		return this.is_struct_compiler_node;
	}
	public final void setCompilerNode(boolean on) {
		if (this.is_struct_compiler_node != on) {
			assert(!locked);
			this.is_struct_compiler_node = on;
			this.callbackChildChanged(nodeattr$flags);
		}
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
		if (m instanceof Constructor) {
			foreach (Constructor mm; members; mm != m) {
				if (mm.type.equals(m.type))
					Kiev.reportError(m,"Constructor "+m+" already exists in class "+this);
			}
		} else {
			foreach (Method mm; members; mm != m) {
				if (mm.id.equals(m.id) && mm.type.equals(m.type))
					Kiev.reportError(m,"Method "+m+" already exists in class "+this);
			}
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
			if (meta != null && meta.tag > caseno)
				caseno = meta.tag;
		}
		MetaPizzaCase meta = cas.getMetaPizzaCase();
		if (meta == null)
			cas.meta.setU(meta = new MetaPizzaCase());
		meta.tag = caseno + 1;
		trace(Kiev.debugMembers,"Class's case "+cas+" added to class "	+this+" as case # "+meta.tag);
		return cas;
	}
		
	public Constructor getClazzInitMethod() {
		foreach(Constructor n; members; n.id.equals(nameClassInit) )
			return n;
		Constructor class_init = new Constructor(ACC_STATIC);
		class_init.pos = pos;
		class_init.setAutoGenerated(true);
		addMethod(class_init);
		class_init.body = new Block(pos);
		return class_init;
	}

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
		this.id = id;
		this.xmeta_type = new CompaundMetaType(this);
		this.xtype = new CompaundType((CompaundMetaType)this.xmeta_type, TVarBld.emptySet);
		this.package_clazz = outer;
		if (flags != 0) {
			if ((flags & ACC_PUBLIC) == ACC_PUBLIC) meta.setF(new MetaAccess("public"));
			if ((flags & ACC_PROTECTED) == ACC_PROTECTED) meta.setF(new MetaAccess("protected"));
			if ((flags & ACC_PRIVATE) == ACC_PRIVATE) meta.setF(new MetaAccess("private"));
			if ((flags & ACC_STATIC) == ACC_STATIC) meta.setF(new MetaStatic());
			if ((flags & ACC_FINAL) == ACC_FINAL) meta.setF(new MetaFinal());
			if ((flags & ACC_ABSTRACT) == ACC_ABSTRACT) meta.setF(new MetaAbstract());
			if ((flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) meta.setF(new MetaSynthetic());
			if ((flags & ACC_MACRO) == ACC_MACRO) meta.setF(new MetaMacro());
			if ((flags & ACC_TYPE_UNERASABLE) == ACC_TYPE_UNERASABLE) meta.setF(new MetaUnerasable());
			if ((flags & ACC_SINGLETON) == ACC_SINGLETON) meta.setF(new MetaSingleton());
			this.flags = flags;
		}
		trace(Kiev.debugCreation,"New clazz created: "+qname() +" as "+id.uname+", member of "+outer);
	}

	public Struct getStruct() { return this; }

	public Type getType() { return this.xtype; }

	public String toString() { return qname().toString(); }

	public MetaPizzaCase getMetaPizzaCase() {
		return (MetaPizzaCase)this.meta.getU("kiev.stdlib.meta.pcase");
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
		if( !isTypeDeclLoaded() ) {
			if (!Env.loadStruct(this).isTypeDeclLoaded()) {
				if (isPackage())
					setTypeDeclLoaded(true);
				else
					throw new RuntimeException("Class "+this+" not found");
			}
			if (!isTypeDeclLoaded())
				throw new RuntimeException("Class "+this+" unresolved");
		}
		return true;
	}
	
	public boolean preResolveIn() {
		if (this.isLoadedFromBytecode())
			return false;
		if (parent() instanceof Struct || parent() instanceof FileUnit)
			return true;
		if (ctx_method==null || ctx_method.isStatic())
			this.setStatic(true);
		this.setTypeDeclLoaded(true);
		this.setLocal(true);
		this.setLoadedFromBytecode(true);
		try {
			Kiev.runProcessorsOn(this);
		} finally { this.setLoadedFromBytecode(false); }
		return true;
	}

	public final boolean mainResolveIn() {
		return true; //!isLocal();
	}

	public void mainResolveOut() {
		((Struct)this).cleanDFlow();
	}

	// verify resolved tree
	public boolean preVerify() {
		setFrontEndPassed();
		foreach (TypeRef i; super_types) {
			if (i.getStruct().isFinal())
				Kiev.reportError(this, "Struct "+this+" extends final struct "+i);
		}
		return true;
	}

	public final rule resolveNameR(ASTNode@ node, ResInfo info)
	{
		info.isStaticAllowed(),
		{
			super.resolveNameR(node, info), $cut
		;
			isPackage(),
			node @= sub_decls,
			info.checkNodeName(node)
		;
			isPackage(),
			info.isCmpByEquals(),
			tryLoad(node,info.getName()), $cut
		}
	}

	public boolean tryLoad(ASTNode@ node, String name) {
		if( isPackage() ) {
			trace(Kiev.debugResolve,"Struct: trying to load in package "+this);
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
		return false;
	}

	public Field getWrappedField(boolean required) {
		foreach (TypeRef st; super_types; st.getStruct() != null) {
			Field wf = st.getStruct().getWrappedField(false);
			if (wf != null)
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

		foreach (TypeRef tr; super_types; !tr.getStruct().isMembersGenerated())
			tr.getStruct().autoGenerateMembers();

		if( Kiev.debug ) System.out.println("AutoGenerating members for "+this);

		String oldfn = Kiev.curFile;
		boolean[] old_exts = Kiev.getExtSet();
		{
			ANode fu = parent();
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
						m.params.insert(0,new FormPar(m.pos,nameThisDollar,package_clazz.xtype,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
						retype = true;
					}
					if (!isInterface() && isTypeUnerasable()) {
						m.params.insert((retype?1:0),new FormPar(m.pos,nameTypeInfo,typeinfo_clazz.xtype,FormPar.PARAM_TYPEINFO,ACC_FINAL|ACC_SYNTHETIC));
						retype = true;
					}
				}
				if( !init_found ) {
					trace(Kiev.debugResolve,nameInit+" not found in class "+this);
					Constructor init = new Constructor(ACC_PUBLIC);
					init.setAutoGenerated(true);
					if (this != Type.tpClosureClazz && this.instanceOf(Type.tpClosureClazz)) {
						if( !isStatic() ) {
							init.params.append(new FormPar(pos,nameThisDollar,package_clazz.xtype,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
							init.params.append(new FormPar(pos,"max$args",Type.tpInt,FormPar.PARAM_NORMAL,ACC_SYNTHETIC));
						} else {
							init.params.append(new FormPar(pos,"max$args",Type.tpInt,FormPar.PARAM_NORMAL,ACC_SYNTHETIC));
						}
					} else {
						if( package_clazz.isClazz() && !isStatic() ) {
							init.params.append(new FormPar(pos,nameThisDollar,package_clazz.xtype,FormPar.PARAM_OUTER_THIS,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
						}
						if (!isInterface() && isTypeUnerasable()) {
							init.params.append(new FormPar(pos,nameTypeInfo,typeinfo_clazz.xtype,FormPar.PARAM_TYPEINFO,ACC_FINAL|ACC_SYNTHETIC));
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
		if (!isInterface()) {
			foreach (TypeRef st; super_types; st.getStruct() != null) {
				mm = st.getStruct().getOverwrittenMethod(base,m);
				if (mm != null)
					break;
			}
		}
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
		foreach (UserMeta m; meta)
			m.resolve(null);
		foreach(DNode dn; members) {
			if (dn.meta != null) {
				foreach (UserMeta m; dn.meta)
					m.resolve(null);
			}
			if (dn instanceof Method) {
				Method meth = (Method)dn;
				foreach (Var p; meth.params) {
					if (p.meta != null) {
						foreach (UserMeta m; p.meta)
							m.resolve(null);
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
	
	public boolean setBody(ENode body) {
		if( !isPizzaCase() ) return false;
		Method init = (Method)members[0];
		if (init.body instanceof Block)
			init.block.stats.add(body);
		else
			init.setBody(body);
		return true;
	}
}


