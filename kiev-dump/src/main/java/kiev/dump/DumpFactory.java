package kiev.dump;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import kiev.stdlib.Arrays;

public class DumpFactory {

	public static XMLDumper getXMLDumper() {
		try {
			return (XMLDumper) Class.forName("kiev.dump.xml.XMLDumperImpl").newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Cannot find XMLDumper", e);
		}
	}

	public static BinDumper getBinDumper() {
		try {
			return (BinDumper) Class.forName("kiev.dump.bin.BinDumperImpl")	.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Cannot find BinDumper", e);
		}
	}

	public static String getRelativePath(File f) throws IOException {
		return getRelativePath(f, new File("."));
	}

	private static Vector<String> getPathList(File f) throws IOException {
		Vector<String> l = new Vector<String>();
		File[] roots = File.listRoots();
		File r;
		r = f.getCanonicalFile();
		while (r != null && !Arrays.contains(roots, r)) {
			l.add(r.getName());
			r = r.getParentFile();
		}
		return l;
	}

	private static String matchPathLists(Vector<String> r, Vector<String> f) {
		// start at the beginning of the lists
		// iterate while both lists are equal
		String s = "";
		int i = r.size() - 1;
		int j = f.size() - 1;

		// first eliminate common root
		while (i >= 0 && j >= 0 && r.get(i).equals(f.get(j))) {
			i--;
			j--;
		}
		// for each remaining level in the home path, add a ..
		for (; i >= 0; i--)
			s += ".." + File.separator;
		// for each level in the file path, add the path
		for (; j >= 1; j--)
			s += f.get(j) + File.separator;
		// file name
		s += f.get(j);
		return s;
	}

	private static String getRelativePath(File f, File home) throws IOException {
		Vector<String> homelist = getPathList(home);
		Vector<String> filelist = getPathList(f);
		String s = matchPathLists(homelist, filelist);
		return s;
	}

}
