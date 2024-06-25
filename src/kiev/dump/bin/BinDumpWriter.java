package kiev.dump.bin;

import java.io.DataOutputStream;
import java.io.OutputStream;

import kiev.dump.DumpWriter;

public final class BinDumpWriter implements DumpWriter {
	
	class CountDataOutputStream extends DataOutputStream {
		public CountDataOutputStream(OutputStream out) {
			super(out);
		}
		int getPos() { return this.written; }
	}
	
	private final CountDataOutputStream		out;
	private int													pos;
	private int													doc_start;
	private int													tbl_start;

	public BinDumpWriter(OutputStream out) {
		this.out = new CountDataOutputStream(out);
		this.pos = 0;
		this.doc_start = 0;
		this.tbl_start = 0;
	}
	
	public int getStreamPos() {
		return this.pos;
	}

	public void startDocument() throws Exception {
		// write signature
		out.write(Signature.SIGNATURE_DOC_START);			pos += 8;
		// magor version
		out.writeShort(1);														pos += 2;
		// minor version
		out.writeShort(0);														pos += 2;
		// write flags
		out.writeInt(0);															pos += 4;
		this.doc_start = getStreamPos();
	}
	
	public void endDocument() throws Exception {
		// write document start position
		out.writeInt(doc_start);											pos += 4;
		// write table start position
		out.writeInt(tbl_start);												pos += 4;
		// write end signature
		out.write(Signature.SIGNATURE_DOC_END);			pos += 8;
		this.out.close();
	}
	
	public void startBlock(Signature sign) throws Exception {
		out.writeByte(Signature.TAG_START.sign);				pos += 1;
		out.writeByte(sign.sign);											pos += 1;
	}
	public void endBlock(Signature sign) throws Exception {
		out.writeByte(Signature.TAG_END.sign);					pos += 1;
		out.writeByte(sign.sign);											pos += 1;
	}
	public void startTable() throws Exception {
		int tbl_prev = tbl_start;
		tbl_start = getStreamPos();
		out.flush();
		out.writeByte(Signature.TAG_START.sign);				pos += 1;
		out.writeByte(Signature.TAG_TABLE_SIGN.sign);		pos += 1;
		if (tbl_prev != 0) {
			out.writeByte(Signature.TAG_TABLE_SIGN.sign);	pos += 1;
			out.writeInt(tbl_prev);											pos += 4;
		}
	}
	public void endTable() throws Exception {
		out.writeByte(Signature.TAG_END.sign);					pos += 1;
		out.writeByte(Signature.TAG_TABLE_SIGN.sign);		pos += 1;
	}
	
	public void writeVoid() throws Exception {
		out.writeByte(Signature.TAG_VOID.sign);					pos += 1;
	}
	public void writeNull() throws Exception {
		out.writeByte(Signature.TAG_NULL.sign);					pos += 1;
	}
	public void writeSpaceStart() throws Exception {
		out.writeByte(Signature.TAG_SPACE_START.sign);	pos += 1;
	}
	public void writeSpaceEnd() throws Exception {
		out.writeByte(Signature.TAG_SPACE_END.sign);		pos += 1;
	}
	public void writeExtDataStart() throws Exception {
		out.writeByte(Signature.TAG_EXT_START.sign);		pos += 1;
	}
	public void writeExtDataEnd() throws Exception {
		out.writeByte(Signature.TAG_EXT_END.sign);			pos += 1;
	}
	public void writeElemID(Elem el) throws Exception {
		if (el.id == 0)
			return;
		if ((el.id & 0xFFFF0000) == 0) {
			out.writeByte(Signature.TAG_ID.sign);					pos += 1;
			out.writeShort(el.id);											pos += 2;
		} else {
			out.writeByte(Signature.TAG_LONG.sign);			pos += 1;
			out.writeByte(Signature.TAG_ID.sign);					pos += 1;
			out.writeInt(el.id);												pos += 4;
		}
	}
	public void writeElemFlags(Elem el) throws Exception {
		if (el.flags == 0)
			return;
		if ((el.flags & 0xFFFFFF00) == 0) {
			out.writeByte(Signature.TAG_FLAG.sign);				pos += 1;
			out.writeByte(el.flags);											pos += 1;
		} else {
			out.writeByte(Signature.TAG_LONG.sign);			pos += 1;
			out.writeByte(Signature.TAG_FLAG.sign);				pos += 1;
			out.writeInt(el.flags);											pos += 4;
		}
	}
	public void writeAttrRef(AttrElem ae) throws Exception {
		out.writeByte(Signature.TAG_ATTR_SIGN.sign);		pos += 1;
		out.writeShort(ae.id);												pos += 2;
	}
	public void writeConstRef(ConstElem ce) throws Exception {
		out.writeByte(Signature.TAG_CONST_SIGN.sign);		pos += 1;
		out.writeShort(ce.id);												pos += 2;
	}
	public void writeTypeRef(TypeElem te) throws Exception {
		out.writeByte(Signature.TAG_TYPE_SIGN.sign);		pos += 1;
		out.writeShort(te.id);												pos += 2;
	}
	public void writeNodeRef(NodeElem ne, boolean ref) throws Exception {
		if (ref) {
			out.writeByte(Signature.TAG_NODE_REF.sign);		pos += 1;
		} else {
			out.writeByte(Signature.TAG_NODE_SIGN.sign);		pos += 1;
		}
		out.writeShort(ne.id);												pos += 2;
	}
	public void writeSymbolRef(SymbElem se, boolean ref) throws Exception {
		if (ref) {
			out.writeByte(Signature.TAG_SYMB_REF.sign);		pos += 1;
		} else {
			out.writeByte(Signature.TAG_SYMB_SIGN.sign);		pos += 1;
		}
		out.writeShort(se.id);												pos += 2;
	}
	public void writeRootRef(NodeElem ne) throws Exception {
		out.writeByte(Signature.TAG_ROOT_SIGN.sign);		pos += 1;
		out.writeShort(ne.id);												pos += 2;
	}
	public void writeTableEntry(Elem el, Signature sign) throws Exception {
		//assert (el.id != 0 && el.saddr != 0);
		out.writeByte(sign.sign);							pos += 1;
		out.writeShort(el.id);								pos += 2;
		out.writeInt(el.saddr);								pos += 4;
	}
	
	public void writeInt8(int val) throws Exception {
		out.writeByte(val);							pos += 1;
	}
	public void writeInt16(int val) throws Exception {
		out.writeShort(val);							pos += 2;
	}
	public void writeInt32(int val) throws Exception {
		out.writeInt(val);								pos += 4;
	}
	public void writeInt64(long val) throws Exception {
		out.writeLong(val);							pos += 8;
	}
	public void writeValueTag(Signature tag) throws Exception {
		out.writeByte(tag.sign);					pos += 1;
	}
	public void writeString(String val) throws Exception {
		if (val == null) {
			out.writeByte(Signature.TAG_NULL.sign);				pos += 1;
			return;
		}
		boolean ascii = true;
		for (char ch : val.toCharArray()) {
			if (ch == '\t' || ch == '\n' || ch == '\r')
				continue;
			if (ch < 32 || ch == 127) {
				ascii = false;
				break;
			}
		}
		byte[] bytes = val.getBytes("UTF-8");
		if (ascii) {
			out.writeByte(Signature.TAG_STRZ_8.sign);			pos += 1;
		} else {
			out.writeByte(Signature.TAG_LONG.sign);			pos += 1;
			out.writeByte(Signature.TAG_STRZ_8.sign);			pos += 1;
			out.writeInt(bytes.length);									pos += 4;
		}
		out.write(bytes);														pos += bytes.length;
		out.writeByte(0);														pos += 1;
	}
	public void writeOctet(byte[] bytes) throws Exception {
		if (bytes == null) {
			out.writeByte(Signature.TAG_NULL.sign);				pos += 1;
			return;
		}
		if ((bytes.length & 0xFFFFFF00) == 0) {
			out.writeByte(Signature.TAG_OCTET.sign);			pos += 1;
			out.writeByte(bytes.length);									pos += 1;
		} else {
			out.writeByte(Signature.TAG_LONG.sign);			pos += 1;
			out.writeByte(Signature.TAG_OCTET.sign);			pos += 1;
			out.writeInt(bytes.length);									pos += 4;
		}
		out.write(bytes);														pos += bytes.length;
		out.writeByte(0);														pos += 1;
	}
	public void writeComment(String val) throws Exception {
		if (val == null && val.length() > 0)
			return;
		startBlock(Signature.TAG_COMMENT_SIGN);
		writeString(val);
		endBlock(Signature.TAG_COMMENT_SIGN);
	}
}
