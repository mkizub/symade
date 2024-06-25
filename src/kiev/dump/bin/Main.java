package kiev.dump.bin;

import java.io.FileInputStream;

public class Main {

	public static void main(String[] args) throws Exception {
		BinDumpReader reader = new BinDumpReader(null, ".", new PrintDecoderFactory(), new FileInputStream(args[0]));
		reader.scanDocument();
	}
}
