package kiev.dump.bin;

import java.util.Map;

import kiev.dump.DumpException;

public class CommentElemPrinter extends ElemPrinter<CommentElem> {

	CommentElemPrinter(BinDumpReader reader) {
		super(reader);
	}

	String elName() { return "comment"; }
	Signature elSignature() { return Signature.TAG_COMMENT_SIGN; }

	CommentElem makeInstance(int id, int addr) { return new CommentElem(id, addr); }
	Map<Integer,CommentElem> getTable() { return reader.commentTable; }

	boolean readValue(TagAndVal tav) throws DumpException
	{
		CommentElem ce = (CommentElem)el;
		if (tav.tag == Signature.TAG_STRZ_8 || tav.tag == Signature.TAG_STRZ_16) {
			String text = (String)tav.val;
			out.print(text);
			ce.text = text;
			return true;
		}
		return false;
	}
}
