package kiev.fmt.common;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;

import kiev.vlang.Env;
import kiev.vlang.TextProcessor;
import kiev.vtree.INode;

public interface TextPrinter extends TextProcessor {
	public void print(INode[] nodes, Writer writer, Env env);
	public void print(INode[] nodes, OutputStream outp, Env env);
	public void print(INode[] nodes, File file, Env env);
}

