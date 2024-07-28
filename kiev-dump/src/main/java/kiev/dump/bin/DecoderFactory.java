package kiev.dump.bin;

import kiev.dump.DumpException;

public interface DecoderFactory {

	public Decoder makeDecoder(Signature sig, BinDumpReader reader) throws DumpException;
}
