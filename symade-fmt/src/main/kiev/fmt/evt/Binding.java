package kiev.fmt.evt;
import syntax kiev.Syntax;

@ThisIsANode
public class Binding extends ENode {
	@nodeAttr public UIEvent∅ events;
	
	@SymbolRefAutoComplete @SymbolRefAutoResolve
	@nodeAttr
	public final Action⇑ action;

	public String toString() { return "bind action: "+action+" to {"+Arrays.toString(events)+")"; }

}
