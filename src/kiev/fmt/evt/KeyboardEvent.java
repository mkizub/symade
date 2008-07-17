package kiev.fmt.evt;

import syntax kiev.Syntax;

@ThisIsANode
public class KeyboardEvent extends UIEvent {
  @nodeAttr public int keyCode;
  @nodeAttr public boolean withCtrl;
  @nodeAttr public boolean withAlt;
  @nodeAttr public boolean withShift;
}