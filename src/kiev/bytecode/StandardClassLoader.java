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

import java.io.*;

import static kiev.stdlib.Debug.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/bytecode/StandardClassLoader.java,v 1.3.4.1 1999/05/29 21:03:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.1 $
 *
 */

public class StandardClassLoader extends ClassLoader {
	protected Hashtable<String,Class>	cache = new Hashtable<String,Class>();
	protected Classpath					classpath;
	public Vector<BytecodeHandler>		handlers = new Vector<BytecodeHandler>();

	{
		handlers.append(repackage);
	}

	public StandardClassLoader() {
		classpath = new Classpath();
	}

	public StandardClassLoader(String path) {
		classpath = new Classpath(path);
	}

	public void addHandler(BytecodeHandler bh) {
		handlers.append(bh);
	}

	public void handleClazz(Clazz clazz) {
		if( handlers.length <= 0 ) return;
		Vector<BytecodeHandler> handls = new Vector<BytecodeHandler>();
		int stage = 0;
		int nextstage = 1000;
		for(;;) {
			handls.cleanup();
			foreach(BytecodeHandler bh; handlers) {
				int bhstage = bh.getPriority();
				if( bhstage == stage ) handls.append(bh);
				else if( bhstage > stage && bhstage < nextstage ) nextstage = bhstage;
			}
			foreach(BytecodeHandler bh; handls) {
				bh.processClazz(clazz);
			}
			if( nextstage <= stage ) break;
			stage = nextstage;
			nextstage = 1000;
		}
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
		trace(Clazz.traceRules,"Processing bytecode for clazz "+name);
		handleClazz(clazz);
		return clazz;
	}

	public synchronized Class loadClass(String name, boolean resolve) {
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

	public static void main(String[] args) {
		if( args.length == 0 ) {
			System.err.println("Usage:\njava kiev.bytecode.StandardClassLoader class.name [args...]");
			return;
		}
//		Clazz.traceRead = true;
//		Clazz.traceWrite = true;
//		Clazz.traceRules = true;
		StandardClassLoader cl = new StandardClassLoader();
//		cl.addHandler(new KievAttributeHandler());
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

