/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev library.

 The Kiev library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public License as
 published by the Free Software Foundation.

 The Kiev library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU Library General Public
 License along with the Kiev compiler; see the file License.  If not,
 write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.bytecode;

import kiev.stdlib.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import static kiev.stdlib.Debug.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/bytecode/Classpath.java,v 1.3.4.1 1999/05/29 21:03:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

public class Classpath implements BytecodeFileConstants {
	public Vector<ClasspathEntry>	entries;

	public Classpath() {
		this(System.getProperty("java.class.path"));
	}

	public Classpath(String classpath) {
		String vers = (String)System.getProperties().get("java.version");
		if( vers!=null && vers.charAt(1)=='.' && ((vers.charAt(0)=='1' && vers.charAt(2)>=2)||(vers.charAt(0)>='2')) ) {
			String bootpath = (String)System.getProperties().get("sun.boot.class.path");
			String homepath = (String)System.getProperties().get("java.home");
			String extpath = "";
			File extdir = new File(homepath+File.separator+"lib"+File.separator+"ext");
			String[] extfiles = extdir.list();
			foreach(String f; extfiles)
				bootpath=bootpath+File.pathSeparator+extdir+File.separator+f;
			classpath = bootpath+File.pathSeparator+classpath;
//			System.out.println("actual CLASSPATH="+classpath);
		}
		StringTokenizer st = new StringTokenizer(classpath,File.pathSeparator);
		entries = new Vector<ClasspathEntry>();
		foreach(Object o; st) {
			File f = new File((String)o);
			if( f.exists() && f.canRead() ) {
				if( f.isDirectory() )
					entries.append(new DirClasspathEntry(f));
				else
					entries.append(new ZipClasspathEntry(f));
			}
		}
	}

	public File findSourceFile(String name) {
		foreach(ClasspathEntry cpe; entries; cpe != null) {
			File f = cpe.findSourceFile(name);
			if( f != null ) return f;
		}
		return null;
	}

	public byte[]	read(String clazz_name) {
		return read(KString.from( clazz_name.replace('.','/') ));
	}

	public byte[]	read(KString clazz_name) {
		foreach(ClasspathEntry cpe; entries; cpe != null) {
			byte[] data = cpe.read(clazz_name);
			if( data != null ) return data;
		}
		return null;
	}

	public static byte[] createPlainPackage(KString clazz_name) {
		Clazz cl = new Clazz();
		cl.flags = ACC_PUBLIC | ACC_PACKAGE;
		PoolConstant[] pool = new PoolConstant[3];
		pool[0] = new VoidPoolConstant();
		pool[1] = new ClazzPoolConstant();
		((ClazzPoolConstant)pool[1]).ref = 2;
		pool[2] = new Utf8PoolConstant();
		((Utf8PoolConstant)pool[2]).value = clazz_name;
		cl.pool = pool;
		cl.cp_clazz = 1;
		cl.cp_super_clazz = 0;
		cl.cp_interfaces = new int[0];
		cl.fields = new Field[0];
		cl.methods = new Method[0];
		cl.attrs = new Attribute[0];
		byte[] data = cl.writeClazz();
		return data;
	}

	public static void readFully(InputStream in, byte[] data) {
		int size = 0, from = 0;
		for(;;) {
			size = in.read(data, from, data.length-from);
			if( size == -1 ) break;
			from += size;
			if( from == data.length ) break;
		}
	}
}

public interface ClasspathEntry {
	public byte[]	read(KString clazz_name);
	public File		findSourceFile(String name);
}

public class DirClasspathEntry implements ClasspathEntry {
	public File			dir;

	public DirClasspathEntry(File dir) {
		this.dir = dir;
	}

	public File findSourceFile(String name) {
		if( File.separatorChar != '/' )
			name = name.replace('/',File.separatorChar);
		File f;
		if( (f=new File(dir,name)).exists() && f.canRead() && f.isDirectory()
		 && ( (File.separatorChar!='/'||true) && f.getCanonicalPath().endsWith(name) )
		) {
			if( (f=new File(dir,name+File.separatorChar+"package.kiev")).exists() && f.canRead()
			 || (f=new File(dir,name+File.separatorChar+"package.java")).exists() && f.canRead() )
				return f;
		}
		else if( (f=new File(dir,name+".kiev")).exists() && f.canRead() && f.isFile()
		 && ( (File.separatorChar!='/'||true) && f.getCanonicalPath().endsWith(name+".kiev") )
		)
			return f;
		else if( (f=new File(dir,name+".java")).exists() && f.canRead() && f.isFile()
		 && ( (File.separatorChar!='/'||true) && f.getCanonicalPath().endsWith(name+".java") )
		)
			return f;
		return null;
	}

	public byte[] read(KString clazz_name) {
		String name = clazz_name.toString();
		if( File.separatorChar != '/' )
			name = name.replace('/',File.separatorChar);
		File f;
		if( (f=new File(dir,name)).exists() && f.canRead() && f.isDirectory()
		 && ( (File.separatorChar!='/'||true) && f.getCanonicalPath().endsWith(name) )
		) {
			if( (f=new File(dir,name+File.separatorChar+"package.class")).exists() && f.canRead() ) {
				byte[] data = new byte[(int)f.length()];
				FileInputStream fis = new FileInputStream(f);
				Classpath.readFully(fis,data);
				fis.close();
				return data;
			} else {
				return Classpath.createPlainPackage(clazz_name);
			}
		}
		else if( (f=new File(dir,name+".class")).exists() && f.canRead() && f.isFile()
		 && ( (File.separatorChar!='/'||true) && f.getCanonicalPath().endsWith(name+".class") )
		) {
			byte[] data = new byte[(int)f.length()];
			FileInputStream fis = new FileInputStream(f);
			Classpath.readFully(fis,data);
			fis.close();
			return data;
		}
		return null;
	}
}

public class ZipClasspathEntry implements ClasspathEntry {
	public ZipFile							zipfile;

	public ZipClasspathEntry(File name) {
		zipfile = new ZipFile(name);
	}

	public File findSourceFile(String name) {
		return null;
	}

	public byte[] read(KString clazz_name) {
		String name = clazz_name.toString();
		ZipEntry f;
		if( (f=zipfile.getEntry(name+"/")) != null ) {
			if( (f=zipfile.getEntry(name+"/package.class")) != null ) {
				byte[] data = new byte[(int)f.getSize()];
				InputStream zis = zipfile.getInputStream(f);
				Classpath.readFully(zis,data);
				zis.close();
				return data;
			} else {
				return Classpath.createPlainPackage(clazz_name);
			}
		}
		else if( (f=zipfile.getEntry(name+".class")) != null ) {
			byte[] data = new byte[(int)f.getSize()];
			InputStream zis = zipfile.getInputStream(f);
			Classpath.readFully(zis,data);
			zis.close();
			return data;
		}
		return null;
	}
}


