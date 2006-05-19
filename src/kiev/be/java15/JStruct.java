package kiev.be.java15;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import java.io.*;

import kiev.vlang.NArr.JArr;
import kiev.ir.java15.RStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeview
public final view JStruct of Struct extends JTypeDecl {

	public:ro	Access				acc;
	public		KString				b_name;
	public:ro	JStruct				package_clazz;
	public:ro	JStruct				iface_impl;
	public:ro	JArr<JDNode>		sub_decls;
	public		Attr[]				attrs;

	public final boolean isPizzaCase();
	public final boolean isHasCases();
	public final boolean isMembersGenerated();
	public final boolean isMembersPreGenerated();
	public final boolean isStatementsGenerated();
	public final boolean isGenerated();

	public final String qname();

	public final KString bname() {
		if (b_name != null)
			return b_name;
		JStruct pkg = package_clazz;
		if (pkg == null || ((Struct)pkg) == Env.root)
			b_name = KString.from(id.uname);
		else if (pkg.isPackage())
			b_name = KString.from(pkg.bname()+"/"+id.uname);
		else
			b_name = KString.from(pkg.bname()+"$"+id.uname);
		return b_name;
	}

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

	public JENode accessTypeInfoField(JNode from, Type t, boolean from_gen) {
		return (JENode)((RStruct)((Struct)this)).accessTypeInfoField((ASTNode)from, t, from_gen);
	}
	
	public JField resolveField(String name) {
		return resolveField(name,true);
	}

	public JField resolveField(String name, boolean fatal) {
		checkResolved();
		foreach (JField f; this.members; f.id.equals(name))
			return f;
		foreach (JType jt; this.super_types) {
			JField f = jt.getJStruct().resolveField(name, false);
			if (f != null)
				return f;
		}
		if (fatal)
			throw new RuntimeException("Unresolved field "+name+" in class "+this);
		return null;
	}


	public JMethod resolveMethod(String name, KString sign) {
		return resolveMethod(this,name,sign,this,true);
	}

	public JMethod resolveMethod(String name, KString sign, boolean fatal) {
		return resolveMethod(this,name,sign,this,fatal);
	}

	private static JMethod resolveMethod(@forward JStruct self, String name, KString sign, JStruct where, boolean fatal) {
		self.checkResolved();
		foreach (JMethod m; members) {
			if( m.id.equals(name) && m.type.getJType().java_signature.equals(sign))
				return m;
		}
		if( isInterface() ) {
			JStruct defaults = self.iface_impl;
			if( defaults != null ) {
				foreach (JMethod m; defaults.members) {
					if( m.id.equals(name) && m.type.getJType().java_signature.equals(sign))
						return m;
				}
			}
		}
		trace(Kiev.debugResolve,"Method "+name+" with signature "+sign+" unresolved in class "+self);
		JMethod m = null;
		foreach (JType jst; super_types) {
			m = resolveMethod(jst.getJStruct(),name,sign,where,fatal);
			if( m != null ) return m;
		}
		if (fatal)
			throw new RuntimeException("Unresolved method "+name+sign+" in class "+where);
		return null;
	}

	public void generate() {
		//if( Kiev.verbose ) System.out.println("[ Generating cls "+this+"]");
		if( Kiev.safe && isBad() ) return;
		
		JDNode[] sub_decls = this.sub_decls.toArray();
		JNode[] members = this.members.toArray();
		
		if( !isPackage() ) {
			foreach (JStruct sub; sub_decls)
				sub.generate();
		}

		ConstPool constPool = new ConstPool();
		constPool.addClazzCP(this.xtype.getJType().java_signature);
		foreach (JType jst; super_types)
			constPool.addClazzCP(jst.java_signature);
		if( !isPackage() ) {
			foreach (JStruct sub; sub_decls) {
				sub.checkResolved();
				constPool.addClazzCP(sub.xtype.getJType().java_signature);
			}
		}
		
		if( !isPackage() ) {
			String fu = jctx_file_unit.id.sname;
			int p = fu.lastIndexOf('/');
			if (p >= 0) fu = fu.substring(p+1);
			p = fu.lastIndexOf('\\');
			if (p >= 0) fu = fu.substring(p+1);
			SourceFileAttr sfa = new SourceFileAttr(KString.from(fu));
			this.addAttr(sfa);
		}
		
		if( !isPackage() && sub_decls.length > 0 ) {
			InnerClassesAttr a = new InnerClassesAttr();
			JStruct[] inner = new JStruct[sub_decls.length];
			JStruct[] outer = new JStruct[sub_decls.length];
			short[] inner_access = new short[sub_decls.length];
			for(int j=0; j < sub_decls.length; j++) {
				inner[j] = sub_decls[j];
				outer[j] = this;
				inner_access[j] = sub_decls[j].getJavaFlags();
				constPool.addClazzCP(inner[j].xtype.getJType().java_signature);
			}
			a.inner = inner;
			a.outer = outer;
			a.acc = inner_access;
			addAttr(a);
		}

		if (meta.size() > 0) this.addAttr(new RVMetaAttr(meta));
		
		for(int i=0; attrs!=null && i < attrs.length; i++) attrs[i].generate(constPool);
		foreach (JField f; members) {
			constPool.addAsciiCP(f.id.uname);
			constPool.addAsciiCP(f.type.getJType().java_signature);

			if( f.isAccessedFromInner())
				((Field)f).setPkgPrivate();
			if (f.meta.size() > 0) f.addAttr(new RVMetaAttr(f.meta));
			if (f.isStatic() && f.init != null && f.init.isConstantExpr()) {
				Object co = f.init.getConstValue();
				if (co != null)
					f.addAttr(new ConstantValueAttr(co));
			}

			foreach (Attr a; f.attrs)
				a.generate(constPool);
		}
		foreach (JMethod m; members) {
			constPool.addAsciiCP(m.id.uname);
			constPool.addAsciiCP(m.type.getJType().java_signature);
			if( m.etype != null )
				constPool.addAsciiCP(m.etype.getJType().java_signature);

			try {
				m.generate(constPool);

				if( m.isAccessedFromInner())
					((Method)m).setPkgPrivate();

				JWBCCondition[] conditions = m.conditions.toArray();
				for(int j=0; j < conditions.length; j++) {
					if( conditions[j].definer.equals(m) ) {
						m.addAttr(conditions[j].code_attr);
					}
				}

				if (m.meta.size() > 0) m.addAttr(new RVMetaAttr(m.meta));
				boolean has_pmeta = false;
				JVar[] params = m.params.toArray();
				foreach (JVar p; params; p.meta != null && m.meta.size() > 0)
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
		foreach (JMethod m; members) {
			CodeAttr ca = (CodeAttr)m.getAttr(attrCode);
			if( ca != null ) {
				trace(Kiev.debugInstrGen," generating refs for CP for method "+this+"."+m);
				Code.patchCodeConstants(ca);
			}
		}
		if( Kiev.safe && isBad() )
			return;
		if !(isPackage() || isSyntax() || isMacro())
			this.toBytecode(constPool);
		Env.setProjectInfo((Struct)this, true);
	}

	public void toBytecode(ConstPool constPool) {
		String output_dir = Kiev.output_dir;
		if( output_dir == null ) output_dir = Kiev.javaMode ? "." : "classes";
		String out_file;
		if( Kiev.javaMode && output_dir == null )
			out_file = this.id.toString();
		else if( this.isPackage() )
			out_file = (this.bname()+"/package").replace('/',File.separatorChar);
		else
			out_file = this.bname().replace('/',File.separatorChar).toString();
		try {
			DataOutputStream out;
			JFileUnit.make_output_dir(output_dir,out_file);
			try {
				out = new DataOutputStream(new FileOutputStream(new File(output_dir,out_file+".class")));
			} catch( java.io.FileNotFoundException e ) {
				System.gc();
				System.runFinalization();
				System.gc();
				System.runFinalization();
				out = new DataOutputStream(new FileOutputStream(new File(output_dir,out_file+".class")));
			}
			byte[] dump = new Bytecoder((Struct)this,null,constPool).writeClazz();
			out.write(dump);
			out.close();
//			if( Kiev.verbose ) System.out.println("[Wrote bytecode for class "+this.name+"]");
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-JavaBytecode exporting: "+e);
		}
	}

	public void cleanup() {
		if( !isPackage() ) {
			foreach (JStruct sub; this.sub_decls)
				sub.cleanup();
		}

		foreach (JDNode n; this.members) {
			if (n instanceof JField)
				n.attrs = Attr.emptyArray;
			else if (n instanceof JMethod)
				n.attrs = Attr.emptyArray;
		}
		this.attrs = Attr.emptyArray;
	}

}

