package kiev.fmt.common;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

import kiev.vlang.Env;
import kiev.vlang.TextProcessor;
import kiev.vtree.INode;

public interface TextParser extends TextProcessor {
	public INode[] parse(Reader reader, Env env);
	public INode[] parse(InputStream inp, Env env);
	public INode[] parse(File file, Env env);
}

