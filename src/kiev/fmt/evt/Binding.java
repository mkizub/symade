package kiev.fmt.evt;

import syntax kiev.Syntax;

@ThisIsANode
public class Binding extends ENode {
	@nodeAttr public UIEvent∅ events;
	@nodeAttr public SymbolRef<Action> action;	
}
