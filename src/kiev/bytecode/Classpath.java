/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.bytecode;

import kiev.stdlib.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class Classpath implements BytecodeFileConstants {
	public Vector<ClasspathEntry>	entries;

	public Classpath() {
		this(System.getProperty("java.class.path"));
	}

	public Classpath(String classpath) {
		String vers = (String)System.getProperties().get("java.version");
//		System.out.println("java.version: "+vers);
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
				try {
					if( f.isDirectory() )
						entries.append(new DirClasspathEntry(f));
					else
						entries.append(new ZipClasspathEntry(f));
				} catch (RuntimeException e) {
					System.out.println("Error adding to CLASSPATH: "+f+"\n\t"+e);
				} catch (IOException e) {
					System.out.println("Error adding to CLASSPATH: "+f+"\n\t"+e);
				}
			}
		}
	}

	public boolean exists(String clazz_name) {
		foreach(ClasspathEntry cpe; entries; cpe != null) {
			if ( cpe.exists(clazz_name) )
				return true;
		}
		return false;
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

	public static byte[] createPlainPackage(KString _clazz_name) {
		String clazz_name = _clazz_name.toString();
		String name, pkg;
		int p = clazz_name.lastIndexOf('/');
		if (p > 0) {
			name = clazz_name.substring(p+1);
			pkg = clazz_name.substring(0,p);
		} else {
			name = clazz_name;
			pkg = "";
		}
		String data =
		"<?xml version='1.1' encoding='UTF-8' standalone='yes'?>\n"+
		"<a-node class='kiev.vlang.KievPackage'>\n"+
		" <sname>"+name+"</sname>\n"+
		"</a-node>";
		return data.getBytes("UTF-8");
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
	public boolean	exists(String clazz_name);
	public byte[]	read(KString clazz_name);
}

public class DirClasspathEntry implements ClasspathEntry {
	public File			dir;

	public DirClasspathEntry(File dir) {
		this.dir = dir;
	}

	public boolean exists(String clazz_name) {
		String name = clazz_name;
		if( File.separatorChar != '/' )
			name = name.replace('/',File.separatorChar);
		File f;
		if ((f=new File(dir,name)).exists() && f.canRead() && f.isDirectory() && f.getCanonicalPath().endsWith(name))
			return true;
		else if ((f=new File(dir,name+".xml")).exists() && f.canRead() && f.isFile() && f.getCanonicalPath().endsWith(name+".xml"))
			return true;
		else if ((f=new File(dir,name+".class")).exists() && f.canRead() && f.isFile() && f.getCanonicalPath().endsWith(name+".class"))
			return true;
		return false;
	}

	public byte[] read(KString clazz_name) {
		String name = clazz_name.toString();
		if( File.separatorChar != '/' )
			name = name.replace('/',File.separatorChar);
		File f;
		if ((f=new File(dir,name)).exists() && f.canRead() && f.isDirectory() && f.getCanonicalPath().endsWith(name)) {
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
		else if ((f=new File(dir,name+".xml")).exists() && f.canRead() && f.isFile() && f.getCanonicalPath().endsWith(name+".xml")) {
			byte[] data = new byte[(int)f.length()];
			FileInputStream fis = new FileInputStream(f);
			Classpath.readFully(fis,data);
			fis.close();
			return data;
		}
		else if ((f=new File(dir,name+".class")).exists() && f.canRead() && f.isFile() && f.getCanonicalPath().endsWith(name+".class")) {
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
	public java.util.Hashtable		direntries;
	public ZipFile					zipfile;

	public ZipClasspathEntry(File name) {
		zipfile = new ZipFile(name);
		direntries = new java.util.Hashtable();
		java.util.Enumeration e = zipfile.entries();
		while (e.hasMoreElements()) {
			ZipEntry ze = (ZipEntry)e.nextElement();
			String nm = ze.getName();
			if (nm.length() == 0)
				continue;
			if (nm.startsWith("META-INF"))
				continue;
			addDirEntries(nm);
		}
	}
	private void addDirEntries(String nm) {
		int p = nm.lastIndexOf('/');
		if (p <= 0)
			return;
		nm = nm.substring(0,p+1);
		nm = nm.intern();
		if (direntries.contains(nm))
			return;
		//System.out.println("DirEntry "+nm);
		direntries.put(nm,nm);
		addDirEntries(nm.substring(0,p))
	}

	public boolean exists(String clazz_name) {
		String name = clazz_name;
		ZipEntry f;
		if( (f=zipfile.getEntry(name+"/")) != null ) {
			return true;
		}
		else if( (f=zipfile.getEntry(name+".class")) != null ) {
			return true;
		}
		return false;
	}

	public byte[] read(KString clazz_name) {
		String name = clazz_name.toString();
		ZipEntry f;
		if( direntries.contains(name+"/") ) { //if (f=zipfile.getEntry(name+"/")) != null ) {
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
		//System.out.println(zipfile.getName()+" has no "+name);
		return null;
	}
}


