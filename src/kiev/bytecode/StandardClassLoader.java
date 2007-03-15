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

import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 703 $
 *
 */

public class StandardClassLoader extends ClassLoader {
	protected Hashtable<String,Class>	cache = new Hashtable<String,Class>();
	protected Classpath					classpath;

	public StandardClassLoader() {
		classpath = new Classpath();
	}

	public StandardClassLoader(String path) {
		classpath = new Classpath(path);
	}

	public synchronized boolean existsClazz(String name) {
		return classpath.exists(name);
	}

	protected synchronized byte[] loadClazzData(String name, boolean force) {
		byte data[] = classpath.read(name);
		if( force && (data == null || data.length == 0) ) {
			data = classpath.createPlainPackage(KString.from(name.replace('.','/')));
		}
		trace(Clazz.traceRules && data != null ,"Bytecode for clazz "+name+" loaded");
		return data;
	}

	public synchronized Clazz loadClazz(String name) {
		return loadClazz(name,false);
	}

	public synchronized Clazz loadClazz(String name, boolean force) {
		trace(Clazz.traceRules,"Loading bytecode for clazz "+name);
		byte data[] = loadClazzData(name,force);
		if( data == null || data.length == 0 ) {
			trace(Clazz.traceRules,"Bytecode for clazz "+name+" not found");
			return null;
		}
		trace(Clazz.traceRules,"Reading bytecode for clazz "+name);
		Clazz clazz = new Clazz();
		clazz.readClazz(data);
//		trace(Clazz.traceRules,"Processing bytecode for clazz "+name);
//		handleClazz(clazz);
		return clazz;
	}

	public synchronized Class loadClass(String name, boolean resolve)
		throws ClassNotFoundException
	{
		Class c = cache.get(name);
		if (c == null) {
			if( name.charAt(0)=='[' || name.startsWith("java.") )
				c = findSystemClass(name);
			else {
				Clazz clazz = loadClazz(name);
				if( clazz == null ) throw new ClassNotFoundException(name);
				byte[] data = clazz.writeClazz();
				c = defineClass(data, 0, data.length);
//				System.out.println("Loaded "+c);
			}
			cache.put(name, c);
		}
		if (resolve) {
			resolveClass(c);
		}
		return c;
	}

	public synchronized File findSourceFile(String name) {
		return classpath.findSourceFile(name);
	}

	public static void main(String[] args)
		throws Throwable
	{
		if( args.length == 0 ) {
			System.err.println("Usage:\njava kiev.bytecode.StandardClassLoader class.name [args...]");
			return;
		}
//		Clazz.traceRead = true;
//		Clazz.traceWrite = true;
//		Clazz.traceRules = true;
		StandardClassLoader cl = new StandardClassLoader();
		Class c = cl.loadClass(args[0],true);
//		System.out.println("Class "+c+" loaded");
		String[] args1 = new String[args.length-1];
		System.arraycopy(args,1,args1,0,args1.length);
		java.lang.reflect.Method m = null;
		try {
			m =	c.getDeclaredMethod("main",new Class[]{Class.forName("[Ljava.lang.String;")});
		} catch(NoSuchMethodException e) {
			e.printStackTrace();
			return;
		}
		try {
			m.invoke(null,new Object[]{args1});
		} catch( java.lang.reflect.InvocationTargetException e ) {
			throw e.getTargetException();
		}
	}
}

