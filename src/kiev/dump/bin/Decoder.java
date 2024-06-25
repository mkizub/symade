package kiev.dump.bin;

import kiev.dump.DumpException;

public interface Decoder<E extends Elem> {

	E readElem(int id, int addr) throws DumpException;

}
