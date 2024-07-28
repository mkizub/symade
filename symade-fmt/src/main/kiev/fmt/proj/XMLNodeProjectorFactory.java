/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.fmt.proj;

import syntax kiev.Syntax;

import java.util.IdentityHashMap;

/**
 * @author Maxim Kizub
 *
 */

public final class XMLProjectionContext extends ProjectionContext {
	public XMLNodeProjectorFactory getProjectonFactory() {
		return (XMLNodeProjectorFactory)npfactory;
	}
}
 
public final class XMLNodeProjectorFactory extends AbstractNodeProjectorFactory {

	public XMLNodeProjectorFactory() {
		super(new XMLProjectionContext());
	}
	
	public XMLProjectionContext getProjectionContext() {
		return (XMLProjectionContext)projection_context;
	}

	public NodeProjector getProjector(INode src, INode dst) {
		XMLNodeProjector np = null;
		if (src != null) {
			np = (XMLNodeProjector)allProjectors.get(src);
			if (np != null)
				return np;
			TypeInfo ti;
			if (src instanceof TypeInfoInterface)
				ti = src.getTypeInfoField();
			else
				ti = TypeInfo.makeTypeInfo(src.getClass(), null);
			dst = new XMLANodeElement(getProjectionContext(), src.asANode().getCompilerNodeName(), ti);
			np = new XMLNodeProjector(this, src, (XMLANodeElement)dst);
			allProjectors.put(src, np);
			allProjectors.put(dst, np);
			return np;
		}
		if (dst != null) {
			np = (XMLNodeProjector)allProjectors.get(dst);
			if (np != null)
				return np;
			XMLANodeElement xne = (XMLANodeElement)dst;
			src = (INode)xne.typeinfo.newInstance();
			np = new XMLNodeProjector(this, src, xne);
			allProjectors.put(src, np);
			allProjectors.put(dst, np);
			return np;
		}
		throw new RuntimeException("Cannot get projector for null");
	}

}

public class XMLTranslProjection implements Projection {
	private final String   proj_id;
	private final TypeInfo typeinfo;
	private Projection     src_port;
	private Projection     dst_port;
	XMLTranslProjection(String proj_id, TypeInfo typeinfo) {
		this.proj_id = proj_id;
		this.typeinfo = typeinfo;
	}
	public String getProjId() { return proj_id; }
	public void setSrcProj(Projection proj) { this.src_port = proj; }
	public void setDstProj(Projection proj) { this.dst_port = proj; }
	public void project(ProjectAction act, ProjectionValue value, int idx) {
		Object val = value.value;
		if (val != null)
			val = String.valueOf(val);
		switch (act) {
		case ProjectAction.SET_SCALAR:
			dst_port.project(ProjectAction.SET_SCALAR, new ProjectionValue(proj_id,val), -1);
			return;
		case ProjectAction.UPDATED:
			dst_port.project(ProjectAction.UPDATED, new ProjectionValue(proj_id,val), -1);
			return;
		}
		throw new RuntimeException("Unknown projection action "+act);
	}
	public void putback(ProjectAction act, ProjectionValue value, int idx) {
		String str = (String)value.value;
		Object val = str;
		if (str != null) {
			Class clazz = typeinfo.clazz;
			if (clazz == Boolean.class   || clazz == Boolean.TYPE)			val = Boolean.valueOf(str);
			if (clazz == Character.class || clazz == Character.TYPE)		val = new Character(str.charAt(0));
			if (clazz == Byte.class      || clazz == Byte.TYPE)				val = Byte.valueOf(str);
			if (clazz == Short.class     || clazz == Short.TYPE)			val = Short.valueOf(str);
			if (clazz == Integer.class   || clazz == Integer.TYPE)			val = Integer.valueOf(str);
			if (clazz == Long.class      || clazz == Long.TYPE)				val = Long.valueOf(str);
			if (clazz == Float.class     || clazz == Float.TYPE)			val = Float.valueOf(str);
			if (clazz == Double.class    || clazz == Double.TYPE)			val = Double.valueOf(str);
			if (Enum.class.isAssignableFrom(clazz))							val = clazz.getMethod("valueOf",String.class).invoke(null,str.trim());
			if (clazz == String.class)										val = str;
		}
		switch (act) {
		case ProjectAction.SET_SCALAR:
			src_port.putback(ProjectAction.SET_SCALAR, new ProjectionValue(proj_id,val), -1);
			return;
		}
		throw new RuntimeException("Unknown projection action "+act);
	}
	public void dispose() {
		if (this.src_port != null) {
			Projection src_port = this.src_port;
			this.src_port = null;
			src_port.dispose();
		}
		if (this.dst_port != null) {
			Projection dst_port = this.dst_port;
			this.dst_port = null;
			dst_port.dispose();
		}
	}
}

public class XMLNodeProjector extends AbstractNodeProjector {

	public XMLNodeProjector(XMLNodeProjectorFactory npfactory, INode src_node, XMLANodeElement dst_node) {
		super(npfactory, src_node, dst_node);
	}

	public XMLProjectionContext getProjectionContext() {
		return (XMLProjectionContext)npfactory.getProjectionContext();
	}

	public String getPutbackProjId(AttrSlot slot, Object value) {
		if (value instanceof XMLAttribute)
			return value.qname;
		if (value instanceof XMLElement)
			return value.qname;
		return null;
	}
	
	private void initPorts() {
		if (this.ports == null)
			return;
		SpaceDstPort dst_port_attrs = (SpaceDstPort)new SpaceDstPort(XMLANodeElement.nodeattr$attributes);
		JoinProjection join_attrs = new JoinProjection();
		SpaceDstPort dst_port_elems = (SpaceDstPort)new SpaceDstPort(XMLANodeElement.nodeattr$elements);
		JoinProjection join_elems = new JoinProjection();
		
		Vector<Projection> ports = new Vector<Projection>();
		ports.append(dst_port_attrs);
		ports.append(dst_port_elems);
		foreach (AttrSlot src_slot; src_node.getNodeTypeInfo().getAllAttributes()) {
			if (src_slot instanceof ScalarAttrSlot) {
				Projection src_port = new ScalarSrcPort((ScalarAttrSlot)src_slot);
				if (src_slot.isChild()) {
					Projection proj = new WrapProjection(src_slot.name, XMLScalarAttrElement.nodeattr$node, new XMLScalarAttrElement(getProjectionContext(), src_slot.name));
					link(src_port, proj, join_elems, dst_port_elems);
				}
				else if (src_slot.isXmlAttr()) {
					Class c = src_slot.typeinfo.clazz;
					if !(c.isPrimitive() || c == String.class || Enum.class.isAssignableFrom(c))
						continue;
					XMLTranslProjection transl = new XMLTranslProjection(src_slot.name, src_slot.typeinfo);
					Projection ptxt = new WrapProjection(src_slot.name, XMLText.nodeattr$text, new XMLText(getProjectionContext(), src_slot.typeinfo));
					Projection proj = new WrapProjection(src_slot.name, XMLAttribute.nodeattr$text, new XMLAttribute(getProjectionContext(), src_slot.name));
					link(src_port, transl, ptxt, proj, join_attrs, dst_port_attrs);
				}
				else {
					XMLTranslProjection transl = new XMLTranslProjection(src_slot.name, src_slot.typeinfo);
					Projection ptxt = new WrapProjection(src_slot.name, XMLText.nodeattr$text, new XMLText(getProjectionContext(), src_slot.typeinfo));
					Projection proj = new WrapProjection(src_slot.name, XMLTextElement.nodeattr$text, new XMLTextElement(getProjectionContext(), src_slot.name));
					link(src_port, transl, ptxt, proj, join_elems, dst_port_elems);
				}
				ports.append(src_port);
			}
			else if (src_slot instanceof SpaceAttrSlot) {
				Class c = src_slot.typeinfo.clazz;
				if !(c.isPrimitive() || c == String.class || Enum.class.isAssignableFrom(c))
					continue;
				Projection src_port = new SpaceSrcPort((SpaceAttrSlot)src_slot);
				Projection proj = new WrapProjection(src_slot.name, XMLSpaceAttrElement.nodeattr$nodes, new XMLSpaceAttrElement(getProjectionContext(), src_slot.name));
				link(src_port, proj, join_elems, dst_port_elems);
			}
		}
		this.ports = ports.toArray();
	}
	
	public void project() {
		initPorts();
		super.project();
	}

	public void putback() {
		initPorts();
		super.putback();
	}

}

// super-class of all XML nodes
@ThisIsANode
public abstract class XMLNode extends ANode {
	@nodeAttr
	final
	public String				qname;
	
	public XMLNode(XMLProjectionContext context, String qname) {
		super(new AHandle(), context);
		this.qname = qname;
	}
}

// XML text data
@ThisIsANode
public class XMLText extends XMLNode {
	@nodeAttr
	public String				text;
	
	public final TypeInfo		typeinfo;
	
	public XMLText(XMLProjectionContext context, TypeInfo typeinfo) {
		super(context, "#text");
		this.typeinfo = typeinfo;
	}

	public Object copy(CopyContext cc) {
		XMLText node = new XMLText((XMLProjectionContext)this.getDataContext(), this.typeinfo);
		return this.copyTo(node,cc);
	}

	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot == XMLText.nodeattr$text) {
			if (this.typeinfo.clazz == Boolean.TYPE) {
				AutoCompleteResult result = new AutoCompleteResult(true);
				result.append("false", "boolean", null, Boolean.FALSE); 
				result.append("true", "boolean", null, Boolean.TRUE);
				return result;
			}
			if (this.typeinfo.clazz == Boolean.class) {
				AutoCompleteResult result = new AutoCompleteResult(true);
				result.append("false", "Boolean", null, Boolean.FALSE); 
				result.append("true", "Boolean", null, Boolean.TRUE);
				result.append("null", "null", null, null);
				return result;
			}
			if (Enum.class.isAssignableFrom(this.typeinfo.clazz) && Enum.class != this.typeinfo.clazz) {
				AutoCompleteResult result = new AutoCompleteResult(true);
				foreach (Enum e; (Enum[])this.typeinfo.clazz.getDeclaredMethod(Constants.nameEnumValues).invoke(null)) {
					result.append(e.toString(), e.getClass().getName(), null, e); 
				}
				result.append("null", "null", null, null);
				return result;
			}
		}
		return null;
	}
}

// XML attribute of elements
@ThisIsANode
public class XMLAttribute extends XMLNode {
	@nodeAttr
	public XMLText				text;
	
	public XMLAttribute(XMLProjectionContext context, String qname) {
		super(context, qname);
	}

	public Object copy(CopyContext cc) {
		XMLAttribute node = new XMLAttribute((XMLProjectionContext)this.getDataContext(), this.qname);
		return this.copyTo(node,cc);
	}
}

// XML element
@ThisIsANode
public abstract class XMLElement extends XMLNode {
	public XMLElement(XMLProjectionContext context, String qname) {
		super(context, qname);
	}
}

// XML element with text inside
@ThisIsANode
public class XMLTextElement extends XMLElement {
	@nodeAttr
	public XMLText				text;
	
	public XMLTextElement(XMLProjectionContext context, String qname) {
		super(context, qname);
	}

	public Object copy(CopyContext cc) {
		XMLTextElement node = new XMLTextElement((XMLProjectionContext)this.getDataContext(), this.qname);
		return this.copyTo(node,cc);
	}
}

// XML element that reflects a scalar attribute of INode
@ThisIsANode
public class XMLScalarAttrElement extends XMLElement {
	@nodeAttr
	public XMLANodeElement		node;
	
	public XMLScalarAttrElement(XMLProjectionContext context, String qname) {
		super(context, qname);
	}

	public Object copy(CopyContext cc) {
		XMLScalarAttrElement node = new XMLScalarAttrElement((XMLProjectionContext)this.getDataContext(), this.qname);
		return this.copyTo(node,cc);
	}
}

// XML element that reflects a space attribute of INode
@ThisIsANode
public class XMLSpaceAttrElement extends XMLElement {
	@nodeAttr
	public XMLANodeElement∅		nodes;
	
	public XMLSpaceAttrElement(XMLProjectionContext context, String qname) {
		super(context, qname);
	}

	public Object copy(CopyContext cc) {
		XMLSpaceAttrElement node = new XMLSpaceAttrElement((XMLProjectionContext)this.getDataContext(), this.qname);
		return this.copyTo(node,cc);
	}
}

// XML element that reflects an INode
@ThisIsANode
public class XMLANodeElement extends XMLElement {
	public final TypeInfo		typeinfo;
	@nodeAttr
	public XMLAttribute∅		attributes;
	@nodeAttr
	public XMLElement∅			elements;
	
	public XMLANodeElement(XMLProjectionContext context, String qname, TypeInfo typeinfo) {
		super(context, qname);
		this.typeinfo = typeinfo;
	}

	public Object copy(CopyContext cc) {
		XMLANodeElement node = new XMLANodeElement((XMLProjectionContext)this.getDataContext(), this.qname, this.typeinfo);
		return this.copyTo(node,cc);
	}
}
