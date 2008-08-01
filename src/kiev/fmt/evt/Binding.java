package kiev.fmt.evt;

import syntax kiev.Syntax;

@ThisIsANode
public class Binding extends ENode {
	@nodeAttr
	public UIEvent∅ events;

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	public final Action⇑ action;

}
