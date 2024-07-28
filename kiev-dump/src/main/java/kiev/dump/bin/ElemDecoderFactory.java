package kiev.dump.bin;

import kiev.dump.DumpException;

public class ElemDecoderFactory implements DecoderFactory {

	public Decoder makeDecoder(Signature sig, BinDumpReader reader) throws DumpException {
		if (sig == Signature.TAG_NODE_SIGN)
			return new NodeElemDecoder(reader);
		else if (sig == Signature.TAG_NODE_REF)
			return new NodeElemDecoder(reader);
		else if (sig == Signature.TAG_ATTR_SIGN)
			return new AttrElemDecoder(reader);
		else if (sig == Signature.TAG_TYPE_SIGN)
			return new TypeElemDecoder(reader);
		else if (sig == Signature.TAG_SYMB_SIGN)
			return new SymbElemDecoder(reader);
		else if (sig == Signature.TAG_SYMB_REF)
			return new SymbElemDecoder(reader);
		else if (sig == Signature.TAG_CONST_SIGN)
			return new ConstElemDecoder(reader);
		return null;
	}
}
