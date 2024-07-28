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
package kiev.fmt;
import syntax kiev.Syntax;

import kiev.fmt.common.*;

import java.io.*;

public final class SyntaxManager {
	private static Hashtable<String,Draw_ATextSyntax>	languageSyntaxMap			= new Hashtable<String,Draw_ATextSyntax>();
	private static Hashtable<Language,Draw_ATextSyntax>	languageEditorSyntaxMap		= new Hashtable<Language,Draw_ATextSyntax>();
	private static Hashtable<Language,Draw_ATextSyntax>	languageInfoSyntaxMap		= new Hashtable<Language,Draw_ATextSyntax>();
	private static Hashtable<String,Draw_StyleSheet>	styleSheetMap				= new Hashtable<String,Draw_StyleSheet>();
	
	private SyntaxManager() {}

	public static Draw_ATextSyntax getDefaultEditorSyntax(Language lng) {
		Draw_ATextSyntax stx = languageEditorSyntaxMap.get(lng);
		if (stx != null)
			return stx;
		stx = getLanguageSyntax(lng.getDefaultEditorSyntax(),null);
		languageEditorSyntaxMap.put(lng, stx);
		return stx;
	}
	
	public static Draw_ATextSyntax getDefaultInfoSyntax(Language lng) {
		Draw_ATextSyntax stx = languageInfoSyntaxMap.get(lng);
		if (stx != null)
			return stx;
		stx = getLanguageSyntax(lng.getDefaultInfoSyntax(),null);
		languageInfoSyntaxMap.put(lng, stx);
		return stx;
	}
	
	public static Draw_ATextSyntax getLanguageSyntax(String name, Env env) {
		if (env != null) {
			DNode ts = env.resolveGlobalDNode(name);
			if (ts instanceof ATextSyntax){
				trace(Kiev.verbose, "getLanguageSyntax ts="+ ts);
				return ts.getCompiled().init();
			}
		}
		Draw_ATextSyntax stx = languageSyntaxMap.get(name);
		if (stx != null)
			return stx;
		return loadLanguageSyntax(name);
	}
	
	public static Draw_ATextSyntax loadLanguageSyntax(String name) {
		Draw_ATextSyntax dts = null;
		InputStream inp = null;
		try {
			inp = SyntaxManager.class.getClassLoader().getSystemResourceAsStream(name.replace('·','/')+".ser");
			ObjectInput oi = new ObjectInputStream(inp);
			dts = (Draw_ATextSyntax)oi.readObject();
			dts.init();
		} catch (Exception e) {
			System.out.println("Read error while syntax deserialization: "+e);
		} finally {
			if (inp != null)
				inp.close();
		}
		if (dts != null)
			languageSyntaxMap.put(name, dts);
		return dts;
	}

	public static Draw_StyleSheet getStyleSheet(String name, Env env) {
		if (env != null) {
			DNode ss = env.resolveGlobalDNode(name);
			if (ss instanceof StyleSheet){
				trace(Kiev.verbose, "getStyleSheet ts="+ ss);
				return ss.getCompiled().init();
			}
		}
		Draw_StyleSheet dss = styleSheetMap.get(name);
		if (dss != null)
			return dss;
		return loadStyleSheet(name);
	}
	
	public static Draw_StyleSheet loadStyleSheet(String name) {
		Draw_StyleSheet dss = null;
		InputStream inp = null;
		try {
			inp = SyntaxManager.class.getClassLoader().getSystemResourceAsStream(name.replace('·','/')+".ser");
			ObjectInput oi = new ObjectInputStream(inp);
			dss = (Draw_StyleSheet)oi.readObject();
			dss.init();
		} catch (IOException e) {
			System.out.println("Read error while style deserialization: "+e);
		} finally {
			if (inp != null)
				inp.close();
		}
		if (dss != null)
			styleSheetMap.put(name, dss);
		return dss;
	}

	public static void dumpTextFile(ASTNode node, File f, String stx)
		throws IOException
	{
		make_output_dir(f);
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
		DefaultTextProcessor tp = new DefaultTextProcessor();
		tp.setProperty("class",stx);
		tp.print(new INode[]{node}, writer, null);
	}

	private static void make_output_dir(File f) throws IOException {
		File dir = f.getParentFile();
		if (dir != null) {
			dir.mkdirs();
			if( !dir.exists() || !dir.isDirectory() ) throw new IOException("Can't create output dir "+dir);
		}
	}
	
}
