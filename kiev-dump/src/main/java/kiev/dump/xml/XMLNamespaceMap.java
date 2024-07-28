package kiev.dump.xml;

public interface XMLNamespaceMap {
	public void add(String prefix, String uri);
	public void remove(String prefix, String uri);
	public String uri2prefix(String uri);
	public String prefix2uri(String prefix);
	public String[] getAllPrefixes();
}
