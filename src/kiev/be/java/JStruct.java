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
public final view JStructView of StructImpl extends JTypeDefView {

	final Struct getStruct() { return this.$view.getStruct(); }

	public access:ro	Access				acc;
	public access:ro	ClazzName			name;
	public access:ro	BaseType			type;
	public access:ro	Struct				package_clazz;
	public access:ro	Struct				typeinfo_clazz;
	public access:ro	NArr<Struct>		sub_clazz;
	public access:ro	NArr<DNode>			imported;
	public				Attr[]				attrs;
	public access:ro	NArr<DNode>			members;

	public final Type[]		get$interfaces()	{ return this.$view.interfaces.toTypeArray(); }
	public final Type[]		get$args()			{ return this.$view.args.toTypeArray(); }
	public final BaseType	get$super_type()	{ return getStruct().super_type; }

	public final boolean isClazz()					{ return !isPackage() && !isInterface() && ! isArgument(); }
	public final boolean isPackage()				{ return this.$view.is_struct_package; }
	public final boolean isArgument()				{ return this.$view.is_struct_argument; }
	public final boolean isPizzaCase()				{ return this.$view.is_struct_pizza_case; }
	public final boolean isLocal()					{ return this.$view.is_struct_local; }
	public final boolean isAnonymouse()			{ return this.$view.is_struct_anomymouse; }
	public final boolean isHasCases()				{ return this.$view.is_struct_has_pizza_cases; }
	public final boolean isVerified()				{ return this.$view.is_struct_verified; }
	public final boolean isMembersGenerated()		{ return this.$view.is_struct_members_generated; }
	public final boolean isMembersPreGenerated()	{ return this.$view.is_struct_pre_generated; }
	public final boolean isStatementsGenerated()	{ return this.$view.is_struct_statements_generated; }
	public final boolean isGenerated()				{ return this.$view.is_struct_generated; }
	public final boolean isAnnotation()			{ return this.$view.is_struct_annotation; }
	public final boolean isEnum()					{ return this.$view.is_struct_enum; }
	public final boolean isSyntax()					{ return this.$view.is_struct_syntax; }
	public final boolean isLoadedFromBytecode()	{ return this.$view.is_struct_bytecode; }

	@getter public JStructView get$child_jctx_clazz() { return this; }

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
		return getStruct().accessTypeInfoField(from.getNode(), t).getJENodeView();
	}
	
	void generate() {
		//if( Kiev.verbose ) System.out.println("[ Generating cls "+this+"]");
		if( Kiev.safe && isBad() ) return;
		if( !isPackage() ) {
			foreach (Struct sub; sub_clazz)
				sub.getJStructView().generate();
		}

		ConstPool constPool = new ConstPool();
		constPool.addClazzCP(this.type.signature);
		constPool.addClazzCP(this.type.getJType().java_signature);
		if( super_type != null ) {
			super_type.clazz.checkResolved();
			constPool.addClazzCP(this.super_type.signature);
			constPool.addClazzCP(this.super_type.getJType().java_signature);
		}
		for(int i=0; i < interfaces.length; i++) {
			interfaces[i].checkResolved();
			constPool.addClazzCP(this.interfaces[i].signature);
			constPool.addClazzCP(this.interfaces[i].getJType().java_signature);
		}
		if( !isPackage() ) {
			for(int i=0; i < this.sub_clazz.length; i++) {
				this.sub_clazz[i].checkResolved();
				constPool.addClazzCP(this.sub_clazz[i].type.signature);
				constPool.addClazzCP(this.sub_clazz[i].type.getJType().java_signature);
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
			Struct[] inner = new Struct[sub_clazz.length];
			Struct[] outer = new Struct[sub_clazz.length];
			short[] inner_access = new short[sub_clazz.length];
			for(int j=0; j < sub_clazz.length; j++) {
				inner[j] = sub_clazz[j];
				outer[j] = this.getStruct();
				inner_access[j] = sub_clazz[j].getJavaFlags();
				constPool.addClazzCP(inner[j].type.signature);
			}
			a.inner = inner;
			a.outer = outer;
			a.acc = inner_access;
			addAttr(a);
		}

		if (meta.size() > 0) this.addAttr(new RVMetaAttr(meta));
		
		for(int i=0; attrs!=null && i < attrs.length; i++) attrs[i].generate(constPool);
		foreach (ASTNode n; members; n instanceof Field) {
			JFieldView f = ((Field)n).getJFieldView();
			constPool.addAsciiCP(f.name.name);
			constPool.addAsciiCP(f.type.signature);
			constPool.addAsciiCP(f.type.getJType().java_signature);

			if( f.isAccessedFromInner()) {
				f.getField().setPrivate(false);
			}
			if (f.meta.size() > 0) f.addAttr(new RVMetaAttr(f.meta));
			if (f.isStatic() && f.init != null && f.init.isConstantExpr()) {
				Object co = f.init.getConstValue();
				if (co != null)
					f.addAttr(new ConstantValueAttr(co));
			}

			for(int j=0; f.attrs != null && j < f.attrs.length; j++)
				f.attrs[j].generate(constPool);
		}
		foreach (ASTNode m; members; m instanceof Method)
			((Method)m).type.checkJavaSignature();
		foreach (ASTNode n; members; n instanceof Method) {
			JMethodView m = ((Method)n).getJMethodView();
			constPool.addAsciiCP(m.name.name);
			constPool.addAsciiCP(m.type.signature);
			constPool.addAsciiCP(m.type.getJType().java_signature);
			if( m.jtype != null )
				constPool.addAsciiCP(m.jtype.getJType().java_signature);

			try {
				m.generate(constPool);

				if( m.isAccessedFromInner()) {
					m.getMethod().setPrivate(false);
				}

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
			JMethodView m = ((Method)n).getJMethodView();
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
			foreach (Struct sub; sub_clazz)
				sub.getJStructView().cleanup();
		}

		foreach (ASTNode n; members) {
			if (n instanceof Field)
				((Field)n).getJFieldView().attrs = Attr.emptyArray;
			else if (n instanceof Method)
				((Method)n).getJMethodView().attrs = Attr.emptyArray;
		}
		this.attrs = Attr.emptyArray;
	}

}

