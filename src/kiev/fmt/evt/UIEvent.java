package kiev.fmt.evt;

import syntax kiev.Syntax;

@ThisIsANode
public abstract class UIEvent extends ENode {
	public static final UIEvent[] emptyArray = new UIEvent[0];

	@nodeAttr public boolean withCtrl;
	@nodeAttr public boolean withAlt;
	@nodeAttr public boolean withShift;

	@nodeAttr public String text;

	public String toString() { return text; }

}
