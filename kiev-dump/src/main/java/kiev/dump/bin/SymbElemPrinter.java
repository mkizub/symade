package kiev.dump.bin;

import java.util.Map;
import java.util.UUID;

import kiev.dump.DumpException;
import kiev.vtree.SymUUID;

public class SymbElemPrinter extends ElemPrinter<SymbElem> {

	SymbElemPrinter(BinDumpReader reader) {
		super(reader);
	}

	String elName() { return "symbol"; }
	Signature elSignature() { return Signature.TAG_SYMB_SIGN; }

	SymbElem makeInstance(int id, int addr) { return new SymbElem(id, addr); }
	Map<Integer,SymbElem> getTable() { return reader.symbTable; }

	boolean readValue(TagAndVal tav) throws DumpException
	{
		SymbElem se = (SymbElem)el;
		if (tav.tag == Signature.TAG_SYMB_SIGN) {
			se.namesp = (SymbElem)tav.val;
			out.printf("%sNAMESP: %s\n", ind(), se.namesp.name);
			return true;
		}
		if (tav.tag == Signature.TAG_OCTET) {
	        long msb = 0;
	        long lsb = 0;
	        byte[] data = (byte[])tav.val;
	        if (data.length != 16)
	        	throw new DumpException("Corrupted dump file: assuming to find UUID octet with length 16 bytes at "+tav.pos);
	        for (int i=0; i<8; i++)
	            msb = (msb << 8) | (data[i] & 0xff);
	        for (int i=8; i<16; i++)
	            lsb = (lsb << 8) | (data[i] & 0xff);
	        se.uuid = new SymUUID(msb, lsb, null);
			out.printf("%sUUID  : %s\n", ind(), se.uuid);
			return true;
		}
		if (tav.tag == Signature.TAG_STRZ_8 || tav.tag == Signature.TAG_STRZ_16) {
			String name = ((String)tav.val).intern();
			se.name = name;
			out.printf("%sNAME  : %s\n", ind(), se.name);
			return true;
		}
		if (tav.tag == Signature.TAG_SYMB_REF) {
			se.target = (SymbElem)tav.val;
			out.printf("%sTARGET: %4x\n", ind(), se.target.id);
			return true;
		}
		if (tav.tag == Signature.TAG_NODE_REF) {
			se.target = (NodeElem)tav.val;
			out.printf("%sOWNER : %4x\n", ind(), se.target.id);
			return true;
		}
		return false;
	}
}
