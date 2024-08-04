package kiev.gui;

import kiev.dump.ExportFactory;
import kiev.dump.MarshallingContext;
import kiev.dump.xml.*;
import kiev.vtree.ExportXMLDump;

import kiev.fmt.evt.*;

public class GUIBindingsExportFactory implements ExportFactory {

	public void setupXMLExport(ExportXMLDump node, MarshallingContext context) {
		context.add(new DataMarshaller());
		context.add(new TypeMarshaller());
		context.add(new ANodeExportMarshaller().
				add(new ExportTypeAlias("bindings", BindingSet.class).
						add(new ExportAttrAlias("sname", "name")).
						add(new ExportInlineAlias("members"))).
				add(new ExportTypeAlias("action", Action.class).
						add(new ExportAttrAlias("sname", "name")).
						add(new ExportAttrAlias("isForPopupMenu", "menu")).
						add(new ExportElemAlias("description", "description")).
						add(new ExportElemAlias("actionClass", "factory"))).
				add(new ExportTypeAlias("bind", Binding.class).
						add(new ExportInlineAlias("events")).
						add(new ExportInlineAlias("action", new SymRefExpandExportMarshaller()))).
				add(new ExportTypeAlias("key", KeyboardEvent.class).
						add(new ExportAttrAlias("keyCode", "key")).
						add(new ExportAttrAlias("withCtrl", "ctrl")).
						add(new ExportAttrAlias("withAlt", "alt")).
						add(new ExportAttrAlias("withShift", "shift"))).
				add(new ExportTypeAlias("mouse", MouseEvent.class).
						add(new ExportAttrAlias("button", "button")).
						add(new ExportAttrAlias("count", "count")).
						add(new ExportAttrAlias("withCtrl", "ctrl")).
						add(new ExportAttrAlias("withAlt", "alt")).
						add(new ExportAttrAlias("withShift", "shift")))
		);
		context.add(new DataConvertor());
	}
}
