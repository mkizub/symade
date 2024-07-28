package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

import kiev.dump.DumpFactory;
import kiev.vlang.Env;
import kiev.vtree.INode;

public class Draw_SyntaxNodeTemplate implements Serializable {
	private static final long serialVersionUID = -2306890714699138747L;
	public String name;
	public byte[] dump;
	public transient INode template_node;

	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
	
	public INode getTemplateNode() {
		if (template_node != null)
			return template_node;
		if (dump == null)
			return null;
		try {
			INode[] nodes = DumpFactory.getXMLDumper().deserializeFromXmlData(Env.getEnv(),dump);
			if (nodes != null && nodes.length > 0)
				template_node = nodes[0];
		} catch (Exception e) {
			e.printStackTrace();
		}
		return template_node;
	}
}

