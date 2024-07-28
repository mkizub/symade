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
package kiev.fmt.common;

import kiev.dump.*;
import kiev.fmt.DrawTerm;
import kiev.fmt.SyntaxManager;
import kiev.vlang.Env;
import kiev.vlang.SyntaxScope;
import kiev.vtree.ANode;
import kiev.vtree.ExportXMLDump;
import kiev.vtree.INode;

import java.io.*;

public class DefaultTextProcessor implements TextDrawSyntax, TextPrinter, TextParser {
	private String syntax_qname;
	private boolean current;
	private String mode;
	private String comment;

	private int pos_x;
	private int pos_y;
	
	public void setProperty(String name, String value) {
		if (name.equals("current"))
			current = Boolean.parseBoolean(value);
		else if (name.equals("class"))
			syntax_qname = value;
		else if (name.equals("mode"))
			mode = value;
		else if (name.equals("comment"))
			comment = value;
		else
			throw new IllegalArgumentException(name);
	}
	
	public Draw_ATextSyntax lookup(Env env) {
		if (current)
			return SyntaxManager.getLanguageSyntax(syntax_qname, env);
		return SyntaxManager.getLanguageSyntax(syntax_qname, null);
	}

	public void print(INode[] nodes, File file, Env env) {
		try {
			make_output_dir(file);
			if (syntax_qname == null || syntax_qname.equals("<bin-dump>")) {
				dumpBin(nodes, new BufferedOutputStream(new FileOutputStream(file)), env);
				return;
			}
			print(nodes, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")), env);
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}

	public void print(INode[] nodes, OutputStream outp, Env env) {
		try {
			if (syntax_qname == null || syntax_qname.equals("<bin-dump>")) {
				dumpBin(nodes, outp, env);
				return;
			}
			print(nodes, new BufferedWriter(new OutputStreamWriter(outp, "UTF-8")), env);
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}

	public void print(INode[] nodes, Writer writer, Env env) {
		try {
			if (syntax_qname == null || syntax_qname.equals("<xml-dump>")) {
				dumpXml(nodes, writer, env);
				writer.close();
				return;
			}
			if (syntax_qname.equals("<xml-export>")) {
				for (INode node : nodes) {
					if (node instanceof SyntaxScope) {
						for (INode n : (INode[])node.getVal(node.getAttrSlot("members"))) {
							if (n instanceof ExportXMLDump)
								exportXml((ExportXMLDump)n, writer, env);
						}
					} else {
						exportXml((ExportXMLDump)node, writer, env);
					}
				}
				writer.close();
				return;
			}
			for (INode node : nodes) {
				Draw_ATextSyntax stx = lookup(env);
				TextFormatter tf = new TextFormatter(env);
				tf.format(node, null, stx, null);
				this.pos_x = 0;
				this.pos_y = 0;
				draw(tf.getRootDrawLayoutBlock(), writer);
			}
			writer.close();
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}
	
	private void dumpXml(INode[] nodes, Writer writer, Env env) throws Exception {
		if (mode == null)
			mode = "full";
		if (comment == null)
			comment =	"\n"+
						" Copyright (c) 2005-2008 UAB \"MAKSINETA\".\n"+
						" All rights reserved. This program and the accompanying materials\n"+
						" are made available under the terms of the Common Public License Version 1.0\n"+
						" which accompanies this distribution, and is available at\n"+
						" http://www.eclipse.org/legal/cpl-v10.html\n"+
						" \n"+
						" Contributors:\n"+
						"     \"Maxim Kizub\" mkizub@symade.com - initial design and implementation\n"
						;
		DumpFactory.getXMLDumper().dumpToXMLStream(env, new XMLDumpFilter(mode), comment, nodes, writer);
	}
	
	private void dumpBin(INode[] nodes, OutputStream out, Env env) throws Exception {
		if (comment == null)
			comment =	"\n"+
						" Copyright (c) 2005-2008 UAB \"MAKSINETA\".\n"+
						" All rights reserved. This program and the accompanying materials\n"+
						" are made available under the terms of the Common Public License Version 1.0\n"+
						" which accompanies this distribution, and is available at\n"+
						" http://www.eclipse.org/legal/cpl-v10.html\n"+
						" \n"+
						" Contributors:\n"+
						"     \"Maxim Kizub\" mkizub@symade.com - initial design and implementation\n"
						;
		DumpFactory.getBinDumper().dumpToBinStream(env, new BinDumpFilter(), comment, nodes, out);
	}
	
	private void exportXml(ExportXMLDump node, Writer writer, Env env) throws Exception {
		if (comment == null)
			comment =	"\n"+
						" Copyright (c) 2005-2008 UAB \"MAKSINETA\".\n"+
						" All rights reserved. This program and the accompanying materials\n"+
						" are made available under the terms of the Common Public License Version 1.0\n"+
						" which accompanies this distribution, and is available at\n"+
						" http://www.eclipse.org/legal/cpl-v10.html\n"+
						" \n"+
						" Contributors:\n"+
						"     \"Maxim Kizub\" mkizub@symade.com - initial design and implementation\n"
						;
		DumpFactory.getXMLDumper().exportToXMLStream(env, comment, node, writer);
	}
	
	private void draw(DrawLayoutInfo root, Writer writer) throws Exception {
		if (!(root.getDrawable() instanceof DrawTerm)) {
			for (DrawLayoutInfo dli : root.getBlocks())
				draw(dli, writer);
			return;
		}
		DrawLayoutInfo leaf = root;
		int x = leaf.getX();
		int y = leaf.getY();

		while (pos_y < y) {
			writer.write('\n');
			pos_y++;
			pos_x = 0;
		}
		while (pos_x < x) {
			writer.write(' ');
			pos_x++;
		}
		
		Object term_obj = ((DrawTerm)leaf.getDrawable()).getTermObj();
		if (term_obj != null && term_obj != DrawTerm.NULL_NODE && term_obj != DrawTerm.NULL_VALUE) {
			String text = String.valueOf(term_obj);
			if (text != null) {
				writer.write(text);
				pos_x += text.length();
				x += text.length();
			}
		}
		while (pos_x < x) {
			writer.write(' ');
			pos_x++;
		}
	}

	public INode[] parse(File file, Env env) {
		try {
			if (syntax_qname.equals("<bin-dump>")) {
				return DumpFactory.getBinDumper().loadFromBinFile(env, file.getParent(), file, null);
			}
			if (syntax_qname.equals("<xml-dump>")) {
				return DumpFactory.getXMLDumper().loadFromXmlFile(env, file, null);
			}
			return parse(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")), env);
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}

	public INode[] parse(InputStream inp, Env env) {
		try {
			if (syntax_qname == null || syntax_qname.equals("<bin-dump>")) {
				return DumpFactory.getBinDumper().loadFromBinStream(env, ".", "full", inp);
			}
			return parse(new BufferedReader(new InputStreamReader(inp, "UTF-8")), env);
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}

	public INode[] parse(Reader reader, Env env) {
		try {
			if (syntax_qname == null || syntax_qname.equals("<xml-dump>")) {
				return DumpFactory.getXMLDumper().loadFromXmlStream(env, mode, reader);
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
		throw new IllegalStateException("Can parse only <xml-dump>");
	}

	private static void make_output_dir(File f) throws IOException {
		File dir = f.getParentFile();
		if (dir != null) {
			dir.mkdirs();
			if( !dir.exists() || !dir.isDirectory() ) throw new IOException("Can't create output dir "+dir);
		}
	}

}

