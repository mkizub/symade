package kiev.fmt.evt;

import syntax kiev.Syntax;
import kiev.fmt.Draw_TextSyntax;
import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.evt.Compiled_Action;
import kiev.fmt.evt.Compiled_Binding;
import kiev.fmt.evt.Compiled_Event;
import kiev.fmt.evt.Compiled_KeyboardEvent;
import java.lang.Exception;

import java.util.logging.*;

@ThisIsANode
public class Binding extends ENode {
	@nodeAttr public UIEvent∅ events;
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	public final Action⇑ action;

	public Compiled_Binding getCompiled() throws Exception {
		Compiled_Binding bnd = new Compiled_Binding();
		Compiled_Event[] evt = new Compiled_Event[events.length];
		Compiled_Action act = new Compiled_Action();
		for (int i=0; i<events.length; i++){
			if (events[i] instanceof KeyboardEvent){
				Compiled_KeyboardEvent ev = new Compiled_KeyboardEvent();
				KeyboardEvent kbe = (KeyboardEvent)events[i];
				ev.keyCode = kbe.keyCode;
				ev.withAlt = kbe.withAlt;
				ev.withCtrl = kbe.withCtrl;
				ev.withShift = kbe.withShift;				
				evt[i] = ev;
			} else {
				throw new Exception("Unsupported event binding"); 
			}
		}
		act = action.dnode.getCompiled();
		bnd.events = evt;
		bnd.action = act;
		return bnd;
	}
}
