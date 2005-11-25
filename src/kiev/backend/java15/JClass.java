package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import java.io.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@node(copyable=false)
@dflow(in="root()")
abstract class JStruct extends JDNode {
	@att
	@dflow(in="", seq="false")
	final NArr<JClazz>		sub_clazz;
	@att
	final KString	qname; // fully qualified name
	@att
	final KString	sname; // short name
	
	JStruct(Struct vpkg) {
		super(vpkg);
		this.qname = vpkg.name.name;
		this.sname = vpkg.name.short_name;
	}
	Struct getVStruct() {
		return (Struct)dnode;
	}
	public int hashCode() {
		return getVStruct().hashCode();
	}

}

@node(copyable=false)
@dflow(in="root()")
public final class JPackage extends JStruct {
	@att
	@dflow(in="", seq="false")
	final NArr<JPackage>	sub_package;
	
	public static JPackage newJPackage(Struct vpkg)
		alias operator(240,lfy,new)
	{
		JPackage jp;
		JDNodeInfo jdi = (JDNodeInfo)vpkg.getNodeData(JDNodeInfo.ID);
		if (jdi == null) {
			jp = new JPackage(vpkg);
			if (jp.getVStruct() != Env.root)
				newJPackage(jp.getVStruct().package_clazz).sub_package.appendUniq(jp);
		} else {
			jp = (JPackage)jdi.jdnode;
		}
		return jp;
	}
	
	private JPackage(Struct vpkg) {
		super(vpkg);
		assert(vpkg.isPackage());
	}
	
	public void importSubTree() {
		foreach (DNode d; getVStruct().sub_clazz; d instanceof Struct) {
			if (d.isPackage()) {
				JPackage jp = new JPackage((Struct)d);
				jp.importSubTree();
			} else {
				JClazz jc = new JClazz((Struct)d);
				jc.importSubTree();
			}
		}
	}

	public void toJavaDecl(String output_dir) {
		foreach (JPackage jp; sub_package) {
			try {
				jp.toJavaDecl(output_dir);
			} catch(Exception e) {
				Kiev.reportError(jp,e);
			}
		}
		foreach (JClazz jc; sub_clazz) {
			try {
				this.toJavaDecl(output_dir, jc);
			} catch(Exception e) {
				Kiev.reportError(jc,e);
			}
		}
	}

	public void toJavaDecl(String output_dir, JClazz cl) {
		if( output_dir == null ) output_dir = "jsrc";
		Dumper dmp = new Dumper();
		if( cl.parent != Env.root ) {
			dmp.append("package ").append(((JPackage)cl.parent).qname).append(';').newLine();
		}

		cl.toJavaDecl(dmp);

		try {
			File f;
			String out_file = cl.qname.replace('.',File.separatorChar).toString();
			make_output_dir(output_dir,out_file);
			f = new File(output_dir,out_file+".java");
			Writer out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
			out.write(dmp.toString());
			out.close();
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-Java exporting: "+e);
		}
	}

	private static void make_output_dir(String top_dir, String filename) throws IOException {
		File dir;
		dir = new File(top_dir,filename);
		dir = new File(dir.getParent());
		dir.mkdirs();
		if( !dir.exists() || !dir.isDirectory() ) throw new RuntimeException("Can't create output dir "+dir);
	}
}

@node(copyable=false)
@dflow(in="root()")
public final class JClazz extends JStruct {
	/** Array of fields of this class */
	@att
	@dflow(in="", seq="false")
	public final NArr<JField>				fields;

	/** Array of methods of this class */
	@att
	@dflow(in="", seq="false")
	public final NArr<JMethod>				methods;

	public static JClazz newJClazz(Struct vcls)
		alias operator(240,lfy,new)
	{
		JClazz jc;
		JDNodeInfo jdi = (JDNodeInfo)vcls.getNodeData(JDNodeInfo.ID);
		if (jdi == null) {
			jc = new JClazz(vcls);
			if (vcls.package_clazz.isPackage())
				JPackage.newJPackage(vcls.package_clazz).sub_clazz.appendUniq(jc);
			else
				newJClazz(vcls.package_clazz).sub_clazz.appendUniq(jc);
		} else {
			jc = (JClazz)jdi.jdnode;
		}
		return jc;
	}
	
	private JClazz(Struct vclazz) {
		super(vclazz);
	}

	public Dumper toJava(Dumper dmp) {
		if (isArgument() || isLocal())
			dmp.append(sname);
		else
			dmp.append(qname);
		return dmp;
	}
	
	public Dumper toJavaDecl(Dumper dmp) {
		Struct jthis = getVStruct();
		if( Kiev.verbose ) System.out.println("[ Dumping class "+this+"]");
		Env.toJavaModifiers(dmp,jthis.getJavaFlags());
		dmp.append("class").forsed_space().append(qname).append(';').newLine();
		return dmp;
	}

	public void importSubTree() {
		foreach (DNode d; getVStruct().members; d instanceof Struct) {
			JClazz jc = new JClazz((Struct)d);
			jc.importSubTree();
		}
	}
}

