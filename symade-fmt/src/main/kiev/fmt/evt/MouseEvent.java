package kiev.fmt.evt;
import syntax kiev.Syntax;

@ThisIsANode
public class MouseEvent extends UIEvent {
	@nodeAttr public int button;
	@nodeAttr public int count;
}