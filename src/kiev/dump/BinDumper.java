package kiev.dump;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import kiev.vlang.Env;
import kiev.vtree.INode;

public interface BinDumper {

	public byte[] serializeToBinData(Env env, BinDumpFilter filter, INode[] nodes) throws Exception;
	
	public void dumpToBinFile(Env env, BinDumpFilter filter, INode[] nodes, File f) throws Exception;
	
	public void dumpToBinStream(Env env, BinDumpFilter filter, String comment, INode[] nodes, OutputStream out) throws Exception;
	
	public INode[] loadFromBinFile(Env env, String dir, File f, byte[] data) throws Exception;
	
	public INode[] loadFromBinStream(Env env, String dir, String mode, InputStream inp) throws Exception;
	
	public INode loadProject(Env env, File f) throws Exception;
}
