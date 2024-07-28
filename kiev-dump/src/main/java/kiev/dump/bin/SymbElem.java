package kiev.dump.bin;

import kiev.vlang.Env;
import kiev.vlang.KievPackage;
import kiev.vtree.SymUUID;
import kiev.vtree.Symbol;

public class SymbElem extends Elem {

	public static final int IS_NAMESPACE = 1 << 0; // the symbol is a namespace (package)
	
	// UUID
	public SymUUID				uuid;
	// simple symbol name
	public String				name;
	// namespace symbol
	public SymbElem				namesp;
	// target node
	public Elem					target;
	// the Symbol/String node instance
	public Symbol				symbol;

	public SymbElem(int id, SymUUID uuid, String name, SymbElem namesp, Symbol symbol) {
		super(id);
		this.uuid = uuid;
		this.name = name;
		this.namesp = namesp;
		this.symbol = symbol;
	}
	public SymbElem(int id, int addr) {
		super(id, addr);
	}
	
	public Symbol makeSymbol(Env env) {
		if (symbol != null)
			return symbol;
		if (uuid != null) {
			Symbol sym = env.getSymbolByUUID(uuid);
			if (sym != null) {
				this.symbol = sym;
				return sym;
			}
		}
		if (namesp != null) {
			Symbol ns = namesp.makeSymbol(env);
			if (isNameSpace())
				env.newPackage(name, (KievPackage)ns.parent());
			Symbol sym = ns.makeSubSymbol(name);
			if (uuid != null) {
				if (sym.suuid() == null)
					sym.setUUID(env, uuid.high, uuid.low);
				else if (!sym.suuid().equals(uuid))
					System.out.println("Warning: Different UUIDs of global symbol "+sym.qname());
			}
			this.symbol = sym;
			return sym;
		}
		this.symbol = new Symbol(name);
		if (uuid != null)
			this.symbol.setUUID(env, uuid.high, uuid.low);
		return symbol;
	}
	
	public boolean isNameSpace() { return (flags & IS_NAMESPACE) != 0; }
}
