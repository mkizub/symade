package kiev.vdom;
import syntax kiev.Syntax;

@ThisIsANode(lang=XMLLang)
public class XMLElement extends XMLNode {

	@nodeAttr public XMLQName name;
	@nodeAttr public XMLAttribute∅ attributes;
	@nodeAttr public XMLNode∅ elements;
	
	public XMLElement() {}
	public XMLElement(String name) {
		this.name = new XMLQName(name);
	}
	public XMLElement(XMLQName name) {
		this.name = name;
	}

	public String toString() {
		return "<" + name + "... />";
	}
}
