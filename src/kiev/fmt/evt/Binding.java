package kiev.fmt.evt;

import syntax kiev.Syntax;
import kiev.fmt.Draw_TextSyntax;
import kiev.fmt.Draw_ATextSyntax;

import java.util.logging.*;

@ThisIsANode
public class Binding extends ENode {
	@nodeAttr public UIEvent∅ events;
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	public final Action⇑ action;

	public Compiled_Binding getCompiled() {
		Compiled_Binding bnd = new Compiled_Binding();
		// add code here
		return bnd;
	}
}
