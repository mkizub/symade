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
package kiev.dump.bin;

import java.io.*;

import kiev.dump.BinDumpFilter;
import kiev.dump.BinDumper;
import kiev.vlang.DNode;
import kiev.vlang.Env;
import kiev.vtree.INode;

/**
 * @author Maxim Kizub
 * @version $Revision: 133 $
 *
 */

public final class BinDumperImpl implements BinDumper {
	
	private static void make_output_dir(File f) throws IOException {
		File dir = f.getParentFile();
		if (dir != null) {
			dir.mkdirs();
			if( !dir.exists() || !dir.isDirectory() ) throw new IOException("Can't create output dir "+dir);
		}
	}

	public byte[] serializeToBinData(Env env, BinDumpFilter filter, INode[] nodes) throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BinDumpWriter out = new BinDumpWriter(bout);
		out.startDocument();
		new DumpMarshallingContext(nodes,env,filter,out).marshalDocument();
		out.endDocument();
		return bout.toByteArray();
	}
	
	public void dumpToBinFile(Env env, BinDumpFilter filter, INode[] nodes, File f) throws Exception
	{
		make_output_dir(f);
		String comment = 
				"\n"+
				" Copyright (c) 2005-2007 UAB \"MAKSINETA\".\n"+
				" All rights reserved. This program and the accompanying materials\n"+
				" are made available under the terms of the Common Public License Version 1.0\n"+
				" which accompanies this distribution, and is available at\n"+
				" http://www.eclipse.org/legal/cpl-v10.html\n"+
				" \n"+
				" Contributors:\n"+
				"     \"Maxim Kizub\" mkizub@symade.com - initial design and implementation\n"
				;
		BinDumpWriter writer = new BinDumpWriter(new BufferedOutputStream(new FileOutputStream(f)));
		try {
			writer.startDocument();
			writer.writeComment(comment);
			new DumpMarshallingContext(nodes,env,filter,writer).marshalDocument();
		} finally {
			writer.endDocument();
		}
	}

	public void dumpToBinStream(Env env, BinDumpFilter filter, String comment, INode[] nodes, OutputStream out) throws Exception
	{
		BinDumpWriter writer = new BinDumpWriter(new BufferedOutputStream(out));
		try {
			writer.startDocument();
			if (comment != null)
				writer.writeComment(comment);
			new DumpMarshallingContext(nodes,env,filter,writer).marshalDocument();
		} finally {
			writer.endDocument();
		}
	}

	public INode[] loadFromBinFile(Env env, String dir, File f, byte[] data) throws Exception {
		//assert (Thread.currentThread() instanceof WorkerThread);
		INode[] roots;
		if (data != null)
			roots = new BinDumpReader(env, dir, new ElemDecoderFactory(), new ByteArrayInputStream(data)).loadDocument();
		else
			roots = new BinDumpReader(env, dir, new ElemDecoderFactory(), new BufferedInputStream(new FileInputStream(f))).loadDocument();
		//if (root instanceof FileUnit) {
		//	FileUnit fu = (FileUnit) root;
		//	fu.setVal("fname", f.getName());
		//}
		return roots;
	}
	
	public INode[] loadFromBinStream(Env env, String dir, String mode, InputStream inp) throws Exception
	{
		INode[] roots = new BinDumpReader(env, dir, new ElemDecoderFactory(), new BufferedInputStream(inp)).loadDocument();
		return roots;
	}
	
	public INode loadProject(Env env, File f) throws Exception {
//		assert (Thread.currentThread() instanceof WorkerThread);
//		DumpUnMarshallingContext deserializer = new DumpUnMarshallingContext(env);
//		XMLDumpFactory.parse(new BufferedInputStream(new FileInputStream(f)), deserializer);
//		return (ANode)deserializer.result;
		return null;
	}

	public INode[] loadFromBinData(Env env, byte[] data, String tdname, DNode pkg) throws Exception {
//		assert (Thread.currentThread() instanceof WorkerThread);
//		DumpUnMarshallingContext deserializer = new DumpUnMarshallingContext(env);
//		deserializer.tdname = tdname;
//		deserializer.pkg = pkg;
//		deserializer.is_interface_only = true;
//		XMLDumpFactory.parse(new ByteArrayInputStream(data), deserializer);
//		for (DelayedTypeInfo dti : deserializer.delayed_types)
//			dti.applay();
//		return (ANode)deserializer.result;
		return null;
	}
}

