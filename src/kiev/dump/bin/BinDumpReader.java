package kiev.dump.bin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import kiev.dump.DumpException;
import kiev.vlang.Env;
import kiev.vtree.INode;

public final class BinDumpReader {

	final Env env;
	final String cur_dir;
	final DecoderFactory dfactory;
	ByteBuffer buf;
	int version_major;
	int version_minor;
	int tbl_addr;
	int doc_addr;
	private int[] root_ids;
	private INode[] roots;
	int indent;
	
	final HashMap<Integer,SymbElem> symbTable = new HashMap<Integer,SymbElem>();
	final HashMap<Integer,NodeElem> nodeTable = new HashMap<Integer,NodeElem>();
	final HashMap<Integer,ConstElem> constTable = new HashMap<Integer,ConstElem>();
	final HashMap<Integer,AttrElem> attrTable = new HashMap<Integer,AttrElem>();
	final HashMap<Integer,TypeElem> typeTable = new HashMap<Integer,TypeElem>();
	final HashMap<Integer,CommentElem> commentTable = new HashMap<Integer,CommentElem>();
	final HashMap<Integer,Elem> addrTable = new HashMap<Integer,Elem>();
	final ArrayList<DelayedTypeInfo> delayed_types = new ArrayList<DelayedTypeInfo>();
	
	public BinDumpReader(Env env, String dir, DecoderFactory dfactory, InputStream inp) throws DumpException, IOException {
		this.env = env;
		this.cur_dir = dir;
		this.dfactory = dfactory;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int sz;
		while ( (sz=inp.read(buf)) > 0)
			out.write(buf, 0, sz);
		buf = out.toByteArray();
		this.buf = ByteBuffer.wrap(buf);
	}
	
	private void checkHeader() throws Exception {
		int sz = buf.capacity();
		if (sz < 32)
			throw new DumpException("Corrupted dump file");
		byte[] sign = new byte[8];
		buf.get(sign);
		if (!Arrays.equals(Signature.SIGNATURE_DOC_START, sign))
			throw new DumpException("Corrupted dump file (bad start signature)");
		buf.position(sz-8);
		buf.get(sign);
		if (!Arrays.equals(Signature.SIGNATURE_DOC_END, sign))
			throw new DumpException("Corrupted dump file (bad end signature)");
		version_major = buf.getShort(8);
		version_minor = buf.getShort(10);
		if (version_major > 1 || version_minor > 0)
			throw new DumpException("Unsupported document version "+version_major+"."+version_minor);
		tbl_addr = buf.getInt(sz-8-4);
		if (tbl_addr < 16 || tbl_addr >= sz-16)
			throw new DumpException("Corrupted dump file (bad table addr)");
		doc_addr = buf.getInt(sz-8-8);
		if (doc_addr != 16)
			throw new DumpException("Corrupted dump file (bad document start addr)");

		for (TypeElem te : TypeElem.SPACIAL_TYPES) {
			te.saddr = -1;
			te.eaddr = -1;
			typeTable.put(new Integer(te.id), te);
		}

		readTables(tbl_addr);
	}
	
	public INode[] loadDocument() throws Exception {
		checkHeader();
		readDocument();
		//for (DelayedTypeInfo dti : delayed_types)
		//	dti.applay(env);
		return roots;
	}
	
	public void scanDocument() throws Exception {
		checkHeader();
		buf.position(doc_addr);
		while (buf.position() < tbl_addr) {
			readTagAndVal(true);
		}
	}
	
	private void readTables(int start_addr) throws DumpException {
		int save_pos = start_addr;
		buf.position(start_addr);
		try {
			eatSignature(Signature.TAG_START);
			Signature tag = readNextTag(false);
			if (tag != Signature.TAG_TABLE_SIGN)
				throw new DumpException("Corrupted dump file: expected to have table at "+start_addr);
			for (;;) {
				tag = readNextTag(true);
				if (tag == Signature.TAG_END) {
					eatSignature(Signature.TAG_TABLE_SIGN);
					break;
				}
				else if (tag == Signature.TAG_ATTR_SIGN) {
					int id = buf.getShort() & 0xFFFF;
					int addr = buf.getInt();
					checkElemStart(tag, addr, id);
					AttrElem el = new AttrElem(id, addr);
					if (attrTable.get(new Integer(id)) == null)
						attrTable.put(new Integer(id), el);
					if (attrTable.get(new Integer(id)).saddr != addr)
						throw new DumpException("Corrupted dump file: different addressed for attr ID 0x"+Integer.toHexString(id)+": old 0x"+Integer.toHexString(attrTable.get(new Integer(id)).saddr)+" and new 0x"+Integer.toHexString(addr)+" at "+(buf.position()-7));
					addrTable.put(new Integer(addr), el);
				}
				else if (tag == Signature.TAG_TYPE_SIGN) {
					int id = buf.getShort() & 0xFFFF;
					int addr = buf.getInt();
					checkElemStart(tag, addr, id);
					TypeElem el = new TypeElem(id, addr);
					if (typeTable.get(new Integer(id)) == null)
						typeTable.put(new Integer(id), el);
					if (typeTable.get(new Integer(id)).saddr != addr)
						throw new DumpException("Corrupted dump file: different addressed for type ID 0x"+Integer.toHexString(id)+": old 0x"+Integer.toHexString(typeTable.get(new Integer(id)).saddr)+" and new 0x"+Integer.toHexString(addr)+" at "+(buf.position()-7));
					addrTable.put(new Integer(addr), el);
				}
				else if (tag == Signature.TAG_SYMB_SIGN) {
					int id = buf.getShort() & 0xFFFF;
					int addr = buf.getInt();
					checkElemStart(tag, addr, id);
					SymbElem el = new SymbElem(id, addr);
					if (symbTable.get(new Integer(id)) == null)
						symbTable.put(new Integer(id), el);
					if (symbTable.get(new Integer(id)).saddr != addr)
						throw new DumpException("Corrupted dump file: different addressed for symbol ID 0x"+Integer.toHexString(id)+": old 0x"+Integer.toHexString(symbTable.get(new Integer(id)).saddr)+" and new 0x"+Integer.toHexString(addr)+" at "+(buf.position()-7));
					addrTable.put(new Integer(addr), el);
				}
				else if (tag == Signature.TAG_CONST_SIGN) {
					int id = buf.getShort() & 0xFFFF;
					int addr = buf.getInt();
					checkElemStart(tag, addr, id);
					ConstElem el = new ConstElem(id, addr);
					if (constTable.get(new Integer(id)) == null)
						constTable.put(new Integer(id), el);
					if (constTable.get(new Integer(id)).saddr != addr)
						throw new DumpException("Corrupted dump file: different addressed for enum ID 0x"+Integer.toHexString(id)+": old 0x"+Integer.toHexString(constTable.get(new Integer(id)).saddr)+" and new 0x"+Integer.toHexString(addr)+" at "+(buf.position()-7));
					addrTable.put(new Integer(addr), el);
				}
				else if (tag == Signature.TAG_NODE_SIGN) {
					int id = buf.getShort() & 0xFFFF;
					int addr = buf.getInt();
					checkElemStart(tag, addr, id);
					NodeElem el = new NodeElem(id, addr);
					if (nodeTable.get(new Integer(id)) == null)
						nodeTable.put(new Integer(id), el);
					if (nodeTable.get(new Integer(id)).saddr != addr)
						throw new DumpException("Corrupted dump file: different addressed for node ID 0x"+Integer.toHexString(id)+": old 0x"+Integer.toHexString(nodeTable.get(new Integer(id)).saddr)+" and new 0x"+Integer.toHexString(addr)+" at "+(buf.position()-7));
					addrTable.put(new Integer(addr), el);
				}
				else if (tag == Signature.TAG_ROOT_SIGN) {
					int id = buf.getShort() & 0xFFFF;
					int addr = buf.getInt();
					checkElemStart(Signature.TAG_NODE_SIGN, addr, id);
					if (this.root_ids == null) {
						this.root_ids = new int[]{id};
					} else {
						int[] tmp = new int[this.root_ids.length+1];
						System.arraycopy(this.root_ids, 0, tmp, 0, this.root_ids.length);
						tmp[this.root_ids.length] = id;
						this.root_ids = tmp;
					}
				}
				else {
					throw new DumpException("Corrupted dump file: unexpected signature '"+tag.sign+"' at "+(buf.position()-1));
				}
			}
			if (root_ids == null || root_ids.length == 0)
				throw new DumpException("Corrupted dump file: root node ID is not specified");
			this.roots = new INode[root_ids.length];
			for (int root_id : root_ids) {
				if (nodeTable.get(new Integer(root_id)) == null)
					throw new DumpException("Corrupted dump file: address for root node ID 0x"+Integer.toHexString(root_id)+" is not specified");
			}
			
		} finally {
			buf.position(save_pos);
		}
	}

	private void readDocument() throws DumpException {
		for (int i=0; i < root_ids.length; i++) {
			int root_id = root_ids[i];
			int addr = nodeTable.get(new Integer(root_id)).saddr;
			checkElemStart(Signature.TAG_NODE_SIGN, addr, root_id);
			NodeElem ne_root = (NodeElem)dfactory.makeDecoder(Signature.TAG_NODE_SIGN, this).readElem(root_id, addr);
			this.roots[i] = ne_root.node;
		}
	}
	
	TagAndVal readTagAndVal(boolean autodecode) throws DumpException {
		boolean far = false;
		Signature tag = readNextTag(true);
		int pos = buf.position()-1;
		if (tag == Signature.TAG_END) {
			Signature sig = readNextTag(false);
			return new TagAndVal(pos, tag, sig);
		}
		if (tag == Signature.TAG_START) {
			Signature sig = readNextTag(false);
			Decoder eldec = dfactory.makeDecoder(sig, this);
			if (eldec == null)
				throw new DumpException("Corrupted dump file: unknown tag '"+tag.sign+"' at "+pos);
			Elem el = eldec.readElem(0, pos);
			buf.position(el.eaddr);
			return new TagAndVal(pos, sig, el);
		}
		if (tag == Signature.TAG_SPACE_START || tag == Signature.TAG_SPACE_END) {
			return new TagAndVal(pos, tag, null);
		}
		if (tag == Signature.TAG_EXT_START || tag == Signature.TAG_EXT_END) {
			return new TagAndVal(pos, tag, null);
		}
		if (tag == Signature.TAG_LONG) {
			far = true;
			tag = readNextTag(false);
			pos = buf.position()-1;
		}
		if (tag == Signature.TAG_ID) {
			int id;
			if (far)		id = buf.getInt();
			else			id = buf.getShort() & 0xFFFF;
			return new TagAndVal(pos, tag, new Integer(id));
		}
		if (tag == Signature.TAG_FLAG) {
			int flags;
			if (far)		flags = buf.getInt();
			else			flags = buf.get() & 0xFF;
			return new TagAndVal(pos, tag, new Integer(flags));
		}
		if (tag == Signature.TAG_LINENO) {
			int lineno;
			if (far)		lineno = buf.getInt();
			else			lineno = buf.getShort() & 0xFFFF;
			return new TagAndVal(pos, tag, new Integer(lineno));
		}
		if (tag == Signature.TAG_TYPE_SIGN) {
			int id;
			if (far)		id = buf.getInt();
			else			id = buf.getShort() & 0xFFFF;
			TypeElem te;
			if (autodecode)
				te = (TypeElem)dfactory.makeDecoder(tag, this).readElem(id, 0);
			else
				te = typeTable.get(Integer.valueOf(id));
			return new TagAndVal(pos, tag, te);
		}
		if (tag == Signature.TAG_ATTR_SIGN) {
			int id;
			if (far)		id = buf.getInt();
			else			id = buf.getShort() & 0xFFFF;
			AttrElem ae;
			if (autodecode)
				ae = (AttrElem)dfactory.makeDecoder(tag, this).readElem(id, 0);
			else
				ae = attrTable.get(Integer.valueOf(id));
			return new TagAndVal(pos, tag, ae);
		}
		if (tag == Signature.TAG_CONST_SIGN) {
			int id;
			if (far)		id = buf.getInt();
			else			id = buf.getShort() & 0xFFFF;
			ConstElem ce;
			if (autodecode)
				ce = (ConstElem)dfactory.makeDecoder(tag, this).readElem(id, 0);
			else
				ce = constTable.get(Integer.valueOf(id));
			return new TagAndVal(pos, tag, ce);
		}
		if (tag == Signature.TAG_SYMB_SIGN || tag == Signature.TAG_SYMB_REF) {
			int id;
			if (far)		id = buf.getInt();
			else			id = buf.getShort() & 0xFFFF;
			SymbElem se;
			if (autodecode)
				se = (SymbElem)dfactory.makeDecoder(tag, this).readElem(id, 0);
			else
				se = symbTable.get(Integer.valueOf(id));
			return new TagAndVal(pos, tag, se);
		}
		if (tag == Signature.TAG_NODE_SIGN) {
			int id;
			if (far)		id = buf.getInt();
			else			id = buf.getShort() & 0xFFFF;
			NodeElem ne;
			if (autodecode)
				ne = (NodeElem)dfactory.makeDecoder(tag, this).readElem(id, 0);
			else
				ne = nodeTable.get(Integer.valueOf(id));
			return new TagAndVal(pos, tag, ne);
		}
		if (tag == Signature.TAG_NODE_REF) {
			int id;
			if (far)		id = buf.getInt();
			else			id = buf.getShort() & 0xFFFF;
			NodeElem ne = nodeTable.get(new Integer(id));
			return new TagAndVal(pos, tag, ne);
		}
		if (tag == Signature.TAG_ROOT_SIGN) {
			int id;
			if (far)		id = buf.getInt();
			else			id = buf.getShort() & 0xFFFF;
			NodeElem ne = nodeTable.get(new Integer(id));
			return new TagAndVal(pos, tag, ne);
		}
		if (tag == Signature.TAG_TABLE_SIGN) {
			return new TagAndVal(pos, tag, null);
		}
		if (tag == Signature.TAG_NULL || tag == Signature.TAG_VOID) {
			return new TagAndVal(pos, tag, null);
		}
		if (tag == Signature.TAG_INT8) {
			int val = buf.get();
			return new TagAndVal(pos, tag, new Integer(val));
		}
		if (tag == Signature.TAG_INT16) {
			int val = buf.getShort();
			return new TagAndVal(pos, tag, new Integer(val));
		}
		if (tag == Signature.TAG_INT32) {
			int val = buf.getInt();
			return new TagAndVal(pos, tag, new Integer(val));
		}
		if (tag == Signature.TAG_INT64) {
			long val = buf.getLong();
			return new TagAndVal(pos, tag, new Long(val));
		}
		if (tag == Signature.TAG_FALSE) {
			return new TagAndVal(pos, tag, new Integer(0));
		}
		if (tag == Signature.TAG_TRUE) {
			return new TagAndVal(pos, tag, new Integer(1));
		}
		if (tag == Signature.TAG_FLOAT) {
			long val = buf.getLong();
			return new TagAndVal(pos, tag, new Double(Double.longBitsToDouble(val)));
		}
		if (tag == Signature.TAG_CHAR_8) {
			char ch = readUtf8Char();
			return new TagAndVal(pos, tag, new Character(ch));
		}
		if (tag == Signature.TAG_STRZ_8 || tag == Signature.TAG_STRZ_16) {
			String str = readStr(far, tag);
			return new TagAndVal(pos, tag, str);
		}
		if (tag == Signature.TAG_OCTET) {
			int sz;
			if (far)		sz = buf.getInt();
			else			sz = buf.get() & 0xFF;
			byte[] data = new byte[sz];
			buf.get(data);
			if (buf.get() != 0)
				throw new DumpException("Corrupted dump file: expected to find zero byte at the end of octet at 0x"+Integer.toHexString(pos));
			return new TagAndVal(pos, tag, data);
		}
		throw new DumpException("Corrupted dump file: unknown tag '"+tag.sign+"' at "+pos);
	}
	
	private char readUtf8Char() {
		int b = buf.get() & 0xFF;
		if (b >= 0xE0) {
			b = (b & 0x0F) << 12;
			b = b | (buf.get() & 0x3F) << 6;
			b = b | (buf.get() & 0x3F);
		}
		else if (b >= 0xC0) {
			b = (b & 0x1F) << 6;
			b = b | (buf.get() & 0x3F);
		}
		return (char)b;
	}
	
	private String readStr(boolean far, Signature tag) throws DumpException {
		int start_pos = buf.position() - 1;
		StringBuffer sb = new StringBuffer();
		if (tag == Signature.TAG_STRZ_8) {
			if (!far) {
				for (;;) {
					char ch = readUtf8Char();
					if (ch == 0)
						break;
					sb.append(ch);
				}
			} else {
				int len = buf.getInt();
				while (buf.position() < len+start_pos+1+4) {
					char ch = readUtf8Char();
					sb.append(ch);
				}
				if (buf.get() != 0)
					throw new DumpException("Corrupted dump file: expected to find zero byte at the end of string 0x"+Integer.toHexString(start_pos));
			}
		}
		if (tag == Signature.TAG_STRZ_16) {
			if (!far) {
				for (;;) {
					char ch = buf.getChar();
					if (ch == 0)
						break;
					sb.append(ch);
				}
			} else {
				int len = buf.getInt();
				for (int i=0; i < len; i+=2) {
					char ch = buf.getChar();
					sb.append(ch);
				}
				if (buf.getChar() != 0)
					throw new DumpException("Corrupted dump file: expected to find zero byte at the end of string 0x"+Integer.toHexString(start_pos));
			}
		}
		return sb.toString();
	}
	
	Signature readNextTag(boolean ws) throws DumpException {
		char ch;
		do {
			ch = (char)buf.get();
		} while (ws && Character.isWhitespace(ch));
		Signature sign = Signature.from(ch);
		if (sign == null)
			throw new DumpException("Corrupted dump file: signature expected at "+(buf.position()-1));
		return sign;
	}
	void eatSignature(Signature sign) throws DumpException {
		if (buf.get() != sign.sign)
			throw new DumpException("Corrupted dump file: expected to have '"+sign.sign+"' at pos "+(buf.position()-1));
	}
	void checkElemStart(Signature sign, int addr, int id) throws DumpException {
		if (buf.get(addr) != Signature.TAG_START.sign)
			throw new DumpException("Corrupted dump file: expected to have '"+Signature.TAG_START.sign+"' at pos "+addr);
		if (buf.get(addr+1) != sign.sign)
			throw new DumpException("Corrupted dump file: expected to have '"+sign.sign+"' at pos "+(addr+1));
		char t = (char)buf.get(addr+2);
		if (t == Signature.TAG_ID.sign) {
			if ((buf.getShort(addr+3) & 0xFFFF) != id)
				throw new DumpException("Corrupted dump file: expected to have element ID 0x"+Integer.toHexString(id)+"' at pos "+addr);
			return;
		}
		if (t == Signature.TAG_LONG.sign) {
			t = (char)buf.get(addr+3);
			if (t != Signature.TAG_ID.sign)
				throw new DumpException("Corrupted dump file: expected to have element ID 0x"+Integer.toHexString(id)+"' at pos "+addr);
			if (buf.getInt(addr+4) != id)
				throw new DumpException("Corrupted dump file: expected to have element ID 0x"+Integer.toHexString(id)+"' at pos "+addr);
			return;
		}
		throw new DumpException("Corrupted dump file: expected to have ID tag at pos "+(addr+2));
	}
	
	int pushBufPos(int new_addr) {
		int old_addr = buf.position();
		buf.position(new_addr);
		return old_addr;
	}
	
	void popBufPos(int old_addr) {
		buf.position(old_addr);
	}

}
