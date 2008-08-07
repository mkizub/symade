package kiev.fmt.evt;

import syntax kiev.Syntax;

@ThisIsANode
public class Action extends DNode {
	@nodeAttr public String description;
	@nodeAttr public boolean isForPopupMenu;
	@nodeAttr public String actionClass;
	
	@UnVersioned
	Compiled_Action compiled;

	public boolean preResolveIn() {
		this.compiled = null;
		return super.preResolveIn();
	}
		

	public Compiled_Action getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Compiled_Action();
		compiled.actionClass = this.actionClass;
		compiled.description = this.description;
		compiled.isForPopupMenu = this.isForPopupMenu;
		return compiled;
	}
}
