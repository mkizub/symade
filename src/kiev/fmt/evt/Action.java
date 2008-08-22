package kiev.fmt.evt;

import syntax kiev.Syntax;

@ThisIsANode
public class Action extends DNode {
	@nodeAttr public String description;
	@nodeAttr public boolean isForPopupMenu;
	@nodeAttr public String actionClass;
	
	@UnVersioned
	kiev.gui.event.Action compiled;

	public String toString() { return "action: "+sname+" call "+actionClass; }

	public boolean preResolveIn() {
		this.compiled = null;
		return super.preResolveIn();
	}
		
	public kiev.gui.event.Action getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new kiev.gui.event.Action();
		compiled.actionClass = this.actionClass;
		compiled.description = this.description;
		compiled.isForPopupMenu = this.isForPopupMenu;
		return compiled;
	}
}
