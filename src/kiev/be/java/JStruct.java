package kiev.be.java;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.*;
import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.Struct.StructImpl;


/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeview
public final view JStructView of StructImpl extends JTypeDeclView {

	public final Struct getStruct() { return this.$view.getStruct(); }

	public access:ro	Access				acc;
	public access:ro	ClazzName			name;
	public access:ro	ConcreteType		concr_type;
	public access:ro	JBaseType			jtype;
	public access:ro	JStructView[]		sub_clazz;
	public				Attr[]				attrs;
	public access:ro	JDNodeView[]		members;

	public final JBaseType		get$jtype()			{ return (JBaseType)this.concr_type.getJType(); }
	public final Type[]			get$interfaces()	{ return this.$view.interfaces.toTypeArray(); }
	public final Type[]			get$args()			{ return this.$view.args.toTypeArray(); }
	public final CompaundType	get$super_type()	{ return getStruct().super_type; }
	public final JStructView[]	get$sub_clazz()		{ return (JStructView[])this.$view.sub_clazz.toJViewArray(JStructView.class); }
	public final JDNodeView[]	get$members()		{ return (JDNodeView[])this.$view.members.toJViewArray(JDNodeView.class); }

	public final boolean isClazz()					{ return this.getStruct().isClazz(); }
	public final boolean isPackage()				{ return this.getStruct().isPackage(); }
	public final boolean isPizzaCase()				{ return this.$view.is_struct_pizza_case; }
	public final boolean isLocal()					{ return this.$view.is_struct_local; }
	public final boolean isAnonymouse()			{ return this.$view.is_struct_anomymouse; }
	public final boolean isHasCases()				{ return this.$view.is_struct_has_pizza_cases; }
	public final boolean isMembersGenerated()		{ return this.$view.is_struct_members_generated; }
	public final boolean isMembersPreGenerated()	{ return this.$view.is_struct_pre_generated; }
	public final boolean isStatementsGenerated()	{ return this.$view.is_struct_statements_generated; }
	public final boolean isGenerated()				{ return this.$view.is_struct_generated; }
	public final boolean isAnnotation()			{ return this.$view.is_struct_annotation; }
	public final boolean isEnum()					{ return this.$view.is_struct_enum; }
	public final boolean isSyntax()					{ return this.getStruct().isSyntax(); }
	public final boolean isLoadedFromBytecode()	{ return this.$view.is_struct_bytecode; }

	@getter public JStructView get$child_jctx_clazz() { return this; }

	public boolean checkResolved() { return getStruct().checkResolved(); }
	
	/** Add information about new attribute that belongs to this class */
	public Attr addAttr(Attr a) {
		// Check we already have this attribute
		for(int i=0; i < attrs.length; i++) {
			if(attrs[i].name == a.name) {
				attrs[i] = a;
				return a;
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

	public JENodeView accessTypeInfoField(JNodeView from, Type t) {
		return getStruct().accessTypeInfoField(from.getNode(), t).getJView();
	}
	
	public boolean instanceOf(JStructView cl) {
		if( cl == null ) return false;
		if( this.equals(cl) ) return true;
		if( super_type != null && super_type.clazz.getJView().instanceOf(cl) )
			return true;
		if( cl.isInterface() ) {
			for(int i=0; i < interfaces.length; i++) {
				if( interfaces[i].getStruct().getJView().instanceOf(cl) ) return true;
			}
		}
		return false;
	}

	public JFieldView resolveField(KString name) {
		return resolveField(name,this,true);
	}

	public JFieldView resolveField(KString name, boolean fatal) {
		return resolveField(name,this,fatal);
	}

	private JFieldView resolveField(KString name, JStructView where, boolean fatal) {
		this.checkResolved();
		foreach(JDNodeView n; members; n instanceof JFieldView) {
			JFieldView f = (JFieldView)n;
			if (f.name == name)
				return f;
		}
		if( super_type != null )
			return super_type.getStruct().getJView().resolveField(name,where,fatal);
		if (fatal)
			throw new RuntimeException("Unresolved field "+name+" in class "+where);
		return null;
	}

	public JMethodView resolveMethod(KString name, KString sign) {
		return resolveMethod(name,sign,this,true);
	}

	public JMethodView resolveMethod(KString name, KString sign, boolean fatal) {
		return resolveMethod(name,sign,this,fatal);
	}

	private JMethodView resolveMethod(KString name, KString sign, JStructView where, boolean fatal) {
		this.checkResolved();
		foreach (JDNodeView n; members; n instanceof JMethodView) {
			JMethodView m = (JMethodView)n;
			if( m.name.equals(name) && m.type.getJType().java_signature.equals(sign))
				return m;
		}
		if( isInterface() ) {
			JStructView defaults = null;
			foreach(JDNodeView n; members; n instanceof JStructView && ((JStructView)n).isClazz() && ((JStructView)n).name.short_name.equals(nameIdefault) ) {
				defaults = (JStructView)n;
				break;
			}
			if( defaults != null ) {
				foreach (JDNodeView n; defaults.members; n instanceof JMethodView) {
					JMethodView m = (JMethodView)n;
					if( m.name.equals(name) && m.type.getJType().java_signature.equals(sign))
						return m;
				}
			}
		}
		trace(Kiev.debugResolve,"Method "+name+" with signature "+sign+" unresolved in class "+this);
		JMethodView m = null;
		if( super_type != null )
			m = super_type.getJStruct().resolveMethod(name,sign,where,fatal);
		if( m != null ) return m;
		foreach(Type interf; interfaces) {
			m = interf.getJStruct().resolveMethod(name,sign,where,fatal);
			if( m != null ) return m;
		}
		if (fatal)
			throw new RuntimeException("Unresolved method "+name+sign+" in class "+where);
		return null;
	}

	void generate() {
		//if( Kiev.verbose ) System.out.println("[ Generating cls "+this+"]");
		if( Kiev.safe && isBad() ) return;
		
		JStructView[] sub_clazz = this.sub_clazz;
		JDNodeView[] members = this.members;
		
		if( !isPackage() ) {
			foreach (JStructView sub; sub_clazz)
				sub.generate();
		}

		ConstPool constPool = new ConstPool();
		constPool.addClazzCP(this.concr_type.getJType().java_signature);
		if( super_type != null ) {
			super_type.clazz.checkResolved();
			constPool.addClazzCP(this.super_type.getJType().java_signature);
		}
		for(int i=0; i < interfaces.length; i++) {
			interfaces[i].checkResolved();
			constPool.addClazzCP(this.interfaces[i].getJType().java_signature);
		}
		if( !isPackage() ) {
			foreach (JStructView sub; sub_clazz) {
				sub.checkResolved();
				constPool.addClazzCP(sub.concr_type.getJType().java_signature);
			}
		}
		
		if( !isPackage() ) {
			KString fu = jctx_file_unit.filename;
			int p = fu.lastIndexOf((byte)'/');
			if (p >= 0) fu = fu.substr(p+1);
			p = fu.lastIndexOf((byte)'\\');
			if (p >= 0) fu = fu.substr(p+1);
			SourceFileAttr sfa = new SourceFileAttr(fu);
			this.addAttr(sfa);
		}
		
		if( !isPackage() && sub_clazz.length > 0 ) {
			InnerClassesAttr a = new InnerClassesAttr();
			JStructView[] inner = new JStructView[sub_clazz.length];
			JStructView[] outer = new JStructView[sub_clazz.length];
			short[] inner_access = new short[sub_clazz.length];
			for(int j=0; j < sub_clazz.length; j++) {
				inner[j] = sub_clazz[j];
				outer[j] = this;
				inner_access[j] = sub_clazz[j].getJavaFlags();
				constPool.addClazzCP(inner[j].concr_type.getJType().java_signature);
			}
			a.inner = inner;
			a.outer = outer;
			a.acc = inner_access;
			addAttr(a);
		}

		if (meta.size() > 0) this.addAttr(new RVMetaAttr(meta));
		
		for(int i=0; attrs!=null && i < attrs.length; i++) attrs[i].generate(constPool);
		foreach (JDNodeView n; members; n instanceof JFieldView) {
			JFieldView f = (JFieldView)n;
			constPool.addAsciiCP(f.name);
			constPool.addAsciiCP(f.type.getJType().java_signature);

			if( f.isAccessedFromInner())
				f.getDNode().setPkgPrivate();
			if (f.meta.size() > 0) f.addAttr(new RVMetaAttr(f.meta));
			if (f.isStatic() && f.init != null && f.init.isConstantExpr()) {
				Object co = f.init.getConstValue();
				if (co != null)
					f.addAttr(new ConstantValueAttr(co));
			}

			foreach (Attr a; f.attrs)
				a.generate(constPool);
		}
		foreach (JDNodeView n; members; n instanceof JMethodView) {
			JMethodView m = (JMethodView)n;
			constPool.addAsciiCP(m.name);
			constPool.addAsciiCP(m.type.getJType().java_signature);
			if( m.etype != null )
				constPool.addAsciiCP(m.etype.getJType().java_signature);

			try {
				m.generate(constPool);

				if( m.isAccessedFromInner())
					m.getDNode().setPkgPrivate();

				JWBCConditionView[] conditions = m.conditions;
				for(int j=0; j < conditions.length; j++) {
					if( conditions[j].definer.equals(m) ) {
						m.addAttr(conditions[j].code_attr);
					}
				}

				if (m.meta.size() > 0) m.addAttr(new RVMetaAttr(m.meta));
				boolean has_pmeta = false;
				JVarView[] params = m.params;
				foreach (JVarView p; params; p.meta != null && m.meta.size() > 0)
					has_pmeta = true;
				if (has_pmeta) {
					MetaSet[] mss;
					mss = new MetaSet[params.length];
					for (int i=0; i < mss.length; i++)
						mss[i] = params[i].meta;
					m.addAttr(new RVParMetaAttr(mss));
				}
				if (m.annotation_default != null)
					m.addAttr(new DefaultMetaAttr(m.annotation_default));

				foreach (Attr a; m.attrs)
					a.generate(constPool);
			} catch(Exception e ) {
				Kiev.reportError(m,"Compilation error: "+e);
				m.generate(constPool);
				foreach (Attr a; m.attrs)
					a.generate(constPool);
			}
			if( Kiev.safe && isBad() ) return;
		}
		constPool.generate();
		foreach (JDNodeView n; members; n instanceof JMethodView) {
			JMethodView m = (JMethodView)n;
			CodeAttr ca = (CodeAttr)m.getAttr(attrCode);
			if( ca != null ) {
				trace(Kiev.debugInstrGen," generating refs for CP for method "+this+"."+m);
				Code.patchCodeConstants(ca);
			}
		}
		if( Kiev.safe && isBad() ) return;
		this.toBytecode(constPool);
		Env.setProjectInfo(name, true);
	}

	void toBytecode(ConstPool constPool) {
		String output_dir = Kiev.output_dir;
		if( output_dir == null ) output_dir = Kiev.javaMode ? "." : "classes";
		String out_file;
		if( Kiev.javaMode && output_dir == null )
			out_file = this.name.short_name.toString();
		else if( this.isPackage() )
			out_file = (this.name.bytecode_name+"/package").replace('/',File.separatorChar);
		else
			out_file = this.name.bytecode_name.replace('/',File.separatorChar).toString();
		try {
			DataOutputStream out;
			JFileUnitView.make_output_dir(output_dir,out_file);
			try {
				out = new DataOutputStream(new FileOutputStream(new File(output_dir,out_file+".class")));
			} catch( java.io.FileNotFoundException e ) {
				System.gc();
				System.runFinalization();
				System.gc();
				System.runFinalization();
				out = new DataOutputStream(new FileOutputStream(new File(output_dir,out_file+".class")));
			}
			byte[] dump = new Bytecoder(this.getStruct(),null,constPool).writeClazz();
			out.write(dump);
			out.close();
//			if( Kiev.verbose ) System.out.println("[Wrote bytecode for class "+this.name+"]");
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-JavaBytecode exporting: "+e);
		}
	}

	public void cleanup() {
		if( !isPackage() ) {
			foreach (JStructView sub; this.sub_clazz)
				sub.cleanup();
		}

		foreach (JDNodeView n; this.members) {
			if (n instanceof JFieldView)
				((JFieldView)n).attrs = Attr.emptyArray;
			else if (n instanceof JMethodView)
				((JMethodView)n).attrs = Attr.emptyArray;
		}
		this.attrs = Attr.emptyArray;
	}

}

