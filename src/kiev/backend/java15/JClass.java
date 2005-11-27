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
public /*abstract*/ class JStruct extends JDNode {
	@att
	@dflow(in="", seq="false")
	final NArr<JStruct>				sub_clazz;
	@att
	final KString	qname; // fully qualified name
	@att
	final KString	sname; // short name
	/** Array of members of this class, except inner classes */
	@att
	@dflow(in="", seq="false")
	public final NArr<JDNode>		members;

	
	public static JStruct newJStruct(Struct vs)
		alias operator(240,lfy,new)
	{
		if (vs.isPackage())
			return JPackage.newJPackage(vs);
		else if (vs.isAnnotation())
			return JAnnotation.newJAnnotation(vs);
		else if (vs.isInterface())
			return JInterface.newJInterface(vs);
		else if (vs.isEnum())
			return JEnum.newJEnum(vs);
		return JClazz.newJClazz(vs);
	}
	
	JStruct(Struct vpkg) {
		super(vpkg);
		this.qname = vpkg.name.name;
		this.sname = vpkg.name.short_name;
	}
	Struct getVStruct() {
		return (Struct)dnode;
	}
	public String toString() {
		return qname.toString();
	}
	public int hashCode() {
		return getVStruct().hashCode();
	}
	public void addMember(JDNode jd) {
		if (jd instanceof JPackage)
			Kiev.reportError(jd, "Java classes may not have sub-packages");
		else if (jd instanceof JStruct)
			sub_clazz.addUniq((JStruct)jd);
		else
			members.addUniq(jd);
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
		assert(vpkg.isPackage());
		JPackage jp = (JPackage)findJDNode(vpkg);
		if (jp == null)
			jp = new JPackage(vpkg);
		return jp;
	}
	
	private JPackage(Struct vpkg) {
		super(vpkg);
		assert(vpkg.isPackage());
		if (getVStruct() != Env.root)
			newJPackage(getVStruct().package_clazz).addMember(this);
	}
	
	public void addMember(JDNode jd) {
		if (jd instanceof JPackage)
			sub_package.addUniq((JPackage)jd);
		else if (jd instanceof JStruct)
			sub_clazz.addUniq((JStruct)jd);
		else
			Kiev.reportError(jd, "Java package may only have sub-packages and classes");
	}

	public void toJavaDecl(String output_dir) {
		foreach (JPackage jp; sub_package) {
			try {
				jp.toJavaDecl(output_dir);
			} catch(Exception e) {
				Kiev.reportError(jp,e);
			}
		}
		foreach (JStruct jc; sub_clazz) {
			try {
				this.toJavaDecl(output_dir, jc);
			} catch(Exception e) {
				Kiev.reportError(jc,e);
			}
		}
	}

	public void toJavaDecl(String output_dir, JStruct cl) {
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

	public static JClazz newJClazz(Struct vcls)
		alias operator(240,lfy,new)
	{
		JClazz jc = (JClazz)findJDNode(vcls);
		if (jc == null)
			jc = new JClazz(vcls);
		return jc;
	}
	
	private JClazz(Struct vclazz) {
		super(vclazz);
		JStruct.newJStruct(vclazz.package_clazz).addMember(this);
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
		dmp.append("class").forsed_space().append(sname).forsed_space().append('{').newLine(1);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

@node(copyable=false)
@dflow(in="root()")
public final class JInterface extends JStruct {

	public static JInterface newJInterface(Struct vcls)
		alias operator(240,lfy,new)
	{
		JInterface jc = (JInterface)findJDNode(vcls);
		if (jc == null)
			jc = new JInterface(vcls);
		return jc;
	}
	
	private JInterface(Struct vclazz) {
		super(vclazz);
		JStruct.newJStruct(vclazz.package_clazz).addMember(this);
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
		if( Kiev.verbose ) System.out.println("[ Dumping iface "+this+"]");
		Env.toJavaModifiers(dmp,jthis.getJavaFlags());
		dmp.append("interface").forsed_space().append(sname).forsed_space().append('{').newLine(1);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

}

@node(copyable=false)
@dflow(in="root()")
public final class JAnnotation extends JStruct {

	public static JAnnotation newJAnnotation(Struct vcls)
		alias operator(240,lfy,new)
	{
		JAnnotation jc = (JAnnotation)findJDNode(vcls);
		if (jc == null)
			jc = new JAnnotation(vcls);
		return jc;
	}
	
	private JAnnotation(Struct vclazz) {
		super(vclazz);
		JStruct.newJStruct(vclazz.package_clazz).addMember(this);
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
		if( Kiev.verbose ) System.out.println("[ Dumping meta  "+this+"]");
		Env.toJavaModifiers(dmp,jthis.getJavaFlags());
		dmp.append("@interface").forsed_space().append(sname).forsed_space().append('{').newLine(1);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

}

@node(copyable=false)
@dflow(in="root()")
public final class JEnum extends JStruct {

	public static JEnum newJEnum(Struct vcls)
		alias operator(240,lfy,new)
	{
		JEnum jc = (JEnum)findJDNode(vcls);
		if (jc == null)
			jc = new JEnum(vcls);
		return jc;
	}
	
	private JEnum(Struct vclazz) {
		super(vclazz);
		JStruct.newJStruct(vclazz.package_clazz).addMember(this);
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
		if( Kiev.verbose ) System.out.println("[ Dumping enum  "+this+"]");
		Env.toJavaModifiers(dmp,jthis.getJavaFlags());
		dmp.append("enum").forsed_space().append(sname).forsed_space().append('{').newLine(1);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

}

