package kiev.fmt.evt;

import syntax kiev.Syntax;

@ThisIsANode
public class Action extends DNode {
	@nodeAttr public String description;
	@nodeAttr public boolean isForPopupMenu;
	@nodeAttr public String actionClass;

}
