package kiev.dump.xml;

import kiev.dump.XMLDumpFilter;
import kiev.vlang.Env;

public class DumpMarshallingContext extends AMarshallingContext {
	
	public final XMLDumpFilter filter;
	
	public DumpMarshallingContext(Env env, XMLDumpWriter out, XMLDumpFilter filter) {
		super(env, out);
		this.filter = filter;
		
		add(new DataMarshaller());
		add(new TypeMarshaller());
		add(new ANodeDumpMarshaller());
		
		add(new DataConvertor());
		add(new TypeConvertor());
		add(new SymbolConvertor());
		add(new SymRefConvertor());
	}
	
	public String getDumpMode() { return filter.dumpModeStack.peek(); }
	public void popDumpMode() { filter.dumpModeStack.pop(); }
	public void pushDumpMode(String dump) { filter.dumpModeStack.push(dump); }
}

