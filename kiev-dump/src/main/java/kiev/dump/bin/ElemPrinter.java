package kiev.dump.bin;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import kiev.dump.DumpException;

public abstract class ElemPrinter<E extends Elem> implements Decoder<E>{

	private static final String indentBuffer;
	public static final PrintWriter out;
	static {
		StringBuilder sb = new StringBuilder(1024);
		for (int i=0; i < 1024; i++) sb.append(' ');
		indentBuffer = sb.toString();
		PrintWriter pw;
		try {
			pw = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true);
		} catch (UnsupportedEncodingException e) {
			pw = new PrintWriter(System.out, true);
		}
		out = pw;
	}
	public final BinDumpReader reader;
	public E el;
	
	ElemPrinter(BinDumpReader reader) {
		this.reader = reader;
	}
	
	public final String ind() {
		return indentBuffer.substring(0, reader.indent); 
	}
	
	abstract String elName();
	abstract Signature elSignature();
	abstract E makeInstance(int id, int addr) throws DumpException;
	abstract Map<Integer,E> getTable();
	abstract boolean readValue(TagAndVal tav) throws DumpException;
	
	public E readElem(int id, int addr) throws DumpException {
		if (id == 0 && addr == 0)
			throw new DumpException("Corrupted dump file: read "+elName()+" ID 0x0 from address 0x0");
		// get/create the element
		if (id != 0)
			el = getTable().get(Integer.valueOf(id));
		if (el == null && addr != 0)
			el = (E)reader.addrTable.get(Integer.valueOf(addr));
		if (el == null)
			el = makeInstance(id, addr);
		// verify id and addr
		if (id != 0) {
			if (el.id == 0) {
				el.id = id;
				getTable().put(Integer.valueOf(id), el);
			} else {
				if (el.id != id)
					throw new DumpException("Corrupted dump file: found "+elName()+" at addr 0x"+Integer.toHexString(addr)+" ID 0x"+Integer.toHexString(id)+" while expected to find ID 0x"+Integer.toHexString(el.id));
			}
		}
		if (addr != 0) {
			if (el.saddr == 0) {
				el.saddr = addr;
				reader.addrTable.put(Integer.valueOf(addr), el);
			} else {
				if (el.saddr != addr)
					throw new DumpException("Corrupted dump file: found "+elName()+" ID 0x"+Integer.toHexString(id)+" at addr 0x"+Integer.toHexString(addr)+" while expected to get it at addr 0x"+Integer.toHexString(el.saddr));
			}
		}
		// verify/update tables
		if (el.id != 0) {
			Elem tmp = getTable().get(Integer.valueOf(el.id));
			if (tmp == null)
				getTable().put(Integer.valueOf(id), el);
			else if (tmp != el)
				throw new DumpException("Corrupted dump file: duplicated "+elName()+" ID 0x"+Integer.toHexString(id)+" at addr 0x"+Integer.toHexString(addr));
		}
		if (el.saddr != 0) {
			Elem tmp = reader.addrTable.get(Integer.valueOf(el.saddr));
			if (tmp == null)
				getTable().put(Integer.valueOf(el.saddr), el);
			else if (tmp != el)
				throw new DumpException("Corrupted dump file: duplicated "+elName()+" ID 0x"+Integer.toHexString(id)+" at addr 0x"+Integer.toHexString(addr));
		}
		
		if (el.eaddr != 0)
			return el;
		
		el.eaddr = el.saddr;
		int old_addr = reader.pushBufPos(el.saddr);
		try {
			reader.eatSignature(Signature.TAG_START);
			reader.eatSignature(elSignature());
			out.printf("%s<%s\n", ind(), elName());
			++reader.indent;
			for (;;) {
				TagAndVal tav = reader.readTagAndVal(false);
				if (tav.tag == Signature.TAG_END) {
					if (tav.val != elSignature())
						throw new DumpException("Corrupted dump file: expected to find end-of-type tag at "+tav.pos);
					el.eaddr = reader.buf.position();
					--reader.indent;
					out.printf("%s>%s\n", ind(), elName());
					//el.build(env);
					return el;
				}
				if (tav.tag == Signature.TAG_ID) {
					int el_id = tav.intVal();
					out.printf("%sID    : %4x\n", ind(), el_id);
					if (el.id == 0)
						el.id = el_id;
					if (el.id != el_id)
						throw new DumpException("Corrupted dump file: assuming to find "+elName()+" ID 0x"+Integer.toHexString(el.id)+" but found ID 0x"+Integer.toHexString(el_id)+" at "+tav.pos);
					continue;
				}
				if (tav.tag == Signature.TAG_FLAG) {
					el.flags = tav.intVal();
					out.printf("%sFLAGS : %4x\n", ind(), el.flags);
					continue;
				}
				if (!readValue(tav))
					throw new DumpException("Corrupted dump file: unexpected "+elName()+" signature '"+tav.tag.sign+"' at "+tav.pos);
			}
		} finally {
			reader.popBufPos(old_addr);
		}
	}
}
