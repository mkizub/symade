package kiev.vdom;
import syntax kiev.Syntax;

@ThisIsANode(lang=XMLLang)
public class XMLQName extends ASTNode {

	@nodeAttr public String prefix;
	@nodeAttr public String local;
	@nodeAttr public String uri;

	public XMLQName() {}
	public XMLQName(String local) {
		this.local = local;
	}
	public XMLQName(String uri, String local) {
		this.uri = uri;
		this.local = local;
	}
	public XMLQName(String uri, String local, String prefix) {
		this.uri = uri;
		this.local = local;
		if (prefix != null && prefix.length() > 0)
			this.prefix = prefix;
	}
	
	public boolean eq(String str) {
		if (local != null && local.equals(str))
			return true;
		return false;
	}
	
	public String toString() {
		if (prefix==null || prefix.length() == 0)
			return local;
		return prefix + ":" + local;
	}
}
