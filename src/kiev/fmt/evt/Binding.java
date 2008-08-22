package kiev.fmt.evt;

import syntax kiev.Syntax;

import java.lang.Exception;

import kiev.gui.event.Event;

@ThisIsANode
public class Binding extends ENode {
	@nodeAttr public UIEvent∅ events;
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	public final Action⇑ action;

	public String toString() { return "bind action: "+action+" to {"+Arrays.toString(events)+")"; }

	public kiev.gui.event.Binding getCompiled() throws Exception {
		kiev.gui.event.Binding bnd = new kiev.gui.event.Binding();
		Event[] evt = new Event[events.length];
		for (int i=0; i<events.length; i++){
			if (events[i] instanceof KeyboardEvent){
				kiev.gui.event.KeyboardEvent ev = new kiev.gui.event.KeyboardEvent();
				KeyboardEvent kbe = (KeyboardEvent)events[i];
				ev.keyCode = kbe.keyCode;
				ev.withAlt = kbe.withAlt;
				ev.withCtrl = kbe.withCtrl;
				ev.withShift = kbe.withShift;				
				evt[i] = ev;
			} 
			else if (events[i] instanceof MouseEvent){
				kiev.gui.event.MouseEvent ev = new kiev.gui.event.MouseEvent();
				MouseEvent me = (MouseEvent)events[i];
				ev.button = me.button;
				ev.count = me.count;
				ev.withAlt = me.withAlt;
				ev.withCtrl = me.withCtrl;
				ev.withShift = me.withShift;				
				evt[i] = ev;
			} 
			else {
				throw new Exception("Unsupported event binding"); 
			}
		}
		bnd.events = evt;
		bnd.action = action.dnode.getCompiled();
		return bnd;
	}
}
