package kiev.fmt.evt;

import syntax kiev.Syntax;

@ThisIsANode
public class Binding extends ENode {
	@nodeAttr public UIEvent∅ events;
	@nodeAttr public SymbolRef<Action> action;	

	public void preResolveOut() {
		super.preResolveOut();
		SymbolRef.resolveSymbol(SeverError.Error, action);
	}
	
	public DNode[] resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "action")
			return SymbolRef.autoCompleteSymbol(action,str);
		return super.resolveAutoComplete(str,slot);
	}

}
