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

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeview
public class JStructView extends JTypeDefView {

	final Struct.StructImpl impl;
	public JStructView(Struct.StructImpl impl) {
		super(impl);
		this.impl = impl;
	}
	
	final Struct getVStruct() { return this.impl.getStruct(); }

	@getter public final Access					get$acc()					{ return this.impl.acc; }
	@getter public final ClazzName				get$name()					{ return this.impl.name; }
	@getter public final BaseType				get$type()					{ return this.impl.type; }
	@getter public final TypeRef				get$super_bound()			{ return this.impl.super_bound; }
	@getter public final NArr<TypeRef>			get$interfaces()			{ return this.impl.interfaces; }
	@getter public final NArr<TypeArgDef>		get$args()					{ return this.impl.args; }
	@getter public final Struct					get$package_clazz()			{ return this.impl.package_clazz; }
	@getter public final Struct					get$typeinfo_clazz()		{ return this.impl.typeinfo_clazz; }
	@getter public final NArr<Struct>			get$sub_clazz()				{ return this.impl.sub_clazz; }
	@getter public final NArr<DNode>			get$imported()				{ return this.impl.imported; }
	@getter public final Attr[]					get$attrs()					{ return this.impl.attrs; }
	@getter public final NArr<DNode>			get$members()				{ return this.impl.members; }

	@getter public final BaseType				get$super_type()			{ return (BaseType)super_bound.lnk; }

	@setter public final void set$attrs(Attr[] val)						{ this.impl.attrs = val; }

	public final boolean isClazz() { return !isPackage() && !isInterface() && ! isArgument(); }
	public final boolean isPackage()  { return this.impl.is_struct_package; }
	public final boolean isArgument() { return this.impl.is_struct_argument; }
	public final boolean isPizzaCase() { return this.impl.is_struct_pizza_case; }
	public final boolean isLocal() { return this.impl.is_struct_local; }
	public final boolean isAnonymouse() { return this.impl.is_struct_anomymouse; }
	public final boolean isHasCases() { return this.impl.is_struct_has_pizza_cases; }
	public final boolean isVerified() { return this.impl.is_struct_verified; }
	public final boolean isMembersGenerated() { return this.impl.is_struct_members_generated; }
	public final boolean isMembersPreGenerated() { return this.impl.is_struct_pre_generated; }
	public final boolean isStatementsGenerated() { return this.impl.is_struct_statements_generated; }
	public final boolean isGenerated() { return this.impl.is_struct_generated; }
	public final boolean isAnnotation() { return this.impl.is_struct_annotation; }
	public final boolean isEnum() { return this.impl.is_struct_enum; }
	public final boolean isSyntax() { return this.impl.is_struct_syntax; }
	public final boolean isLoadedFromBytecode() { return this.impl.is_struct_bytecode; }

	/** Add information about new attribute that belongs to this class */
	Attr addAttr(Attr a) {
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
		for(int i=0; interfaces!=null && i < interfaces.length; i++) {
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
		
		if( !isPackage() && sub_clazz.length > 0 ) {
			InnerClassesAttr a = new InnerClassesAttr();
			Struct[] inner = new Struct[sub_clazz.length];
			Struct[] outer = new Struct[sub_clazz.length];
			short[] inner_access = new short[sub_clazz.length];
			for(int j=0; j < sub_clazz.length; j++) {
				inner[j] = sub_clazz[j];
				outer[j] = this.getVStruct();
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
			Field f = (Field)n;
			constPool.addAsciiCP(f.name.name);
			constPool.addAsciiCP(f.type.signature);
			constPool.addAsciiCP(f.type.getJType().java_signature);

			if( f.isAccessedFromInner()) {
				f.setPrivate(false);
			}
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
				m.getJMethodView().generate(constPool);

				if( m.isAccessedFromInner()) {
					m.setPrivate(false);
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
				m.getJMethodView().generate(constPool);
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
			byte[] dump = new Bytecoder(this.getVStruct(),null,constPool).writeClazz();
			out.write(dump);
			out.close();
//			if( Kiev.verbose ) System.out.println("[Wrote bytecode for class "+this.name+"]");
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-JavaBytecode exporting: "+e);
		}
	}


}

