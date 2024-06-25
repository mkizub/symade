package kiev.dump.bin;

import kiev.dump.DumpException;

public class PrintDecoderFactory implements DecoderFactory {

	public Decoder makeDecoder(Signature sig, BinDumpReader reader) throws DumpException {
		if (sig == Signature.TAG_NODE_SIGN)
			return new NodeElemPrinter(reader);
		else if (sig == Signature.TAG_NODE_REF)
			return new NodeElemPrinter(reader);
		else if (sig == Signature.TAG_ATTR_SIGN)
			return new AttrElemPrinter(reader);
		else if (sig == Signature.TAG_TYPE_SIGN)
			return new TypeElemPrinter(reader);
		else if (sig == Signature.TAG_SYMB_SIGN)
			return new SymbElemPrinter(reader);
		else if (sig == Signature.TAG_SYMB_REF)
			return new SymbElemPrinter(reader);
		else if (sig == Signature.TAG_CONST_SIGN)
			return new ConstElemPrinter(reader);
		else if (sig == Signature.TAG_COMMENT_SIGN)
			return new CommentElemPrinter(reader);
		return null;
	}
}
