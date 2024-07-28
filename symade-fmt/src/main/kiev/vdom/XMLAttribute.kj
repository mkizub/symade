package kiev.vdom;
import syntax kiev.Syntax;

@ThisIsANode(lang=XMLLang)
public class XMLAttribute extends XMLNode {

	@nodeAttr public XMLQName name;
	@nodeAttr public XMLText text;
	
	public XMLAttribute() {}
	public XMLAttribute(String name, String text) {
		this.name = new XMLQName(name);
		this.text = new XMLText(text);
	}
	public XMLAttribute(XMLQName name, XMLText text) {
		this.name = name;
		this.text = text;
	}
	
	public String toString() {
		return name + "=\"" + text.text + "\"";
	}
}
