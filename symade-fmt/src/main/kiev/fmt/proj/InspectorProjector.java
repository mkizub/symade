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
import java.math.BigInteger;

/**
 * @author Maxim Kizub
 *
 */

public final class InspectorProjectionContext extends ProjectionContext {
	public InspectorProjectorFactory getProjectonFactory() {
		return (InspectorProjectorFactory)npfactory;
	}
}
 
public final class InspectorProjectorFactory extends AbstractNodeProjectorFactory {
	
	public InspectorProjectorFactory() {
		super(new InspectorProjectionContext());
	}

	public InspectorProjectionContext getProjectionContext() {
		return (InspectorProjectionContext)projection_context;
	}

	public NodeProjector getProjector(INode src, INode dst0) {
		InspectorNode dst = (InspectorNode)dst0;
		InspectorNodeProjector np = null;
		if (src != null) {
			np = (InspectorNodeProjector)allProjectors.get(src);
			if (np != null)
				return np;
			Language lang = src.asANode().getCompilerLang();
			String type_id;
			if (lang == null)
				type_id = "void:" + src.asANode().getCompilerNodeName();
			else
				type_id = lang.getName() + ":" + src.asANode().getCompilerNodeName();
			TypeInfo typeinfo;
			if (src instanceof TypeInfoInterface)
				typeinfo = ((TypeInfoInterface)src).getTypeInfoField();
			else
				typeinfo = TypeInfo.makeTypeInfo(src.getClass(), null);
			if (src instanceof SymbolRef)
				dst = new InspectorSymRefNode(getProjectionContext(), type_id, typeinfo);
			else
				dst = new InspectorNode(getProjectionContext(), type_id, typeinfo);
		} else {
			np = (InspectorNodeProjector)allProjectors.get(dst);
			if (np != null)
				return np;
			src = (INode)dst.typeinfo.newInstance();
		}
		InspectorNodeProjector inp = new InspectorNodeProjector(this, src, dst);
		allProjectors.put(src, inp);
		allProjectors.put(dst, inp);
		return inp;
	}
}

public class InspectorNodeProjector extends AbstractNodeProjector {

	public InspectorNodeProjector(InspectorProjectorFactory npfactory, INode src_node, InspectorNode dst_node) {
		super(npfactory, src_node, dst_node);
	}

	public InspectorProjectionContext getProjectionContext() {
		return (InspectorProjectionContext)npfactory.getProjectionContext();
	}

	public String getPutbackProjId(AttrSlot slot, Object value) {
		if (slot == InspectorSymRefNode.nodeattr$ident_or_symbol_or_type)
			return slot.name;
		InspectorAttribute a = (InspectorAttribute)value;
		return a.attr_slot.name;
	}
	
	private void initPorts() {
		if (this.ports != null)
			return;
		SpaceDstPort dst_port_attrs = (SpaceDstPort)new SpaceDstPort(InspectorNode.nodeattr$attributes);
		JoinProjection join_attrs = new JoinProjection();
		ScalarDstPort dst_port_sref = null;
		Vector<Projection> ports = new Vector<Projection>();
		ports.append(dst_port_attrs);
		if (src_node instanceof SymbolRef) {
			dst_port_sref = (ScalarDstPort)new ScalarDstPort(InspectorSymRefNode.nodeattr$ident_or_symbol_or_type);
			ports.append(dst_port_sref);
		}
		
		foreach (AttrSlot src_slot; src_node.values(); src_slot.isAttr()) {
			if (src_slot == SymbolRef.nodeattr$ident_or_symbol_or_type) {
				Projection src_port = new ScalarSrcPort((ScalarAttrSlot)src_slot);
				ports.append(src_port);
				link(src_port, dst_port_sref);
			}
			else if (src_slot instanceof ScalarAttrSlot) {
				ScalarAttrSlot attr_slot = (ScalarAttrSlot)src_slot;
				Projection src_port = new ScalarSrcPort(attr_slot);
				ports.append(src_port);
				Projection proj;
				Class clazz = attr_slot.typeinfo.clazz;
				if (attr_slot.isChild())
					proj = new WrapProjection(attr_slot.name, InspectorNodeAttr.nodeattr$node, new InspectorNodeAttr(getProjectionContext(), attr_slot));
				else if (clazz == Boolean.TYPE || clazz == Boolean.class || Enum.class.isAssignableFrom(clazz))
					proj = new WrapProjection(attr_slot.name, InspectorScalarAttribute.nodeattr$data, new InspectorEnumAttr(getProjectionContext(), attr_slot));
				else
					proj = new WrapProjection(attr_slot.name, InspectorScalarAttribute.nodeattr$data, new InspectorTextAttr(getProjectionContext(), attr_slot));
				link(src_port, proj, join_attrs, dst_port_attrs);
			}
			else if (src_slot instanceof SpaceAttrSlot) {
				SpaceAttrSlot attr_slot = (SpaceAttrSlot)src_slot;
				Projection src_port = new SpaceSrcPort(attr_slot);
				ports.append(src_port);
				Projection proj = new WrapProjection(attr_slot.name, InspectorSpaceAttr.nodeattr$nodes, new InspectorSpaceAttr(getProjectionContext(), attr_slot));
				link(src_port, proj, join_attrs, dst_port_attrs);
			}
			else if (src_slot instanceof ExtSpaceAttrSlot) {
				ExtSpaceAttrSlot attr_slot = (ExtSpaceAttrSlot)src_slot;
				Projection src_port = new ExtSpaceSrcPort(attr_slot);
				ports.append(src_port);
				Projection proj = new WrapProjection(attr_slot.name, InspectorExtSpaceAttr.nodeattr$nodes, new InspectorExtSpaceAttr(getProjectionContext(), attr_slot));
				link(src_port, proj, join_attrs, dst_port_attrs);
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

@ThisIsANode
public class InspectorNode extends ANode {
	@nodeAttr
	public InspectorAttribute∅		attributes;
	
	@nodeAttr final
	public String					type_id;

	final
	public TypeInfo					typeinfo;
	
	public InspectorNode(InspectorProjectionContext context, String type_id, TypeInfo typeinfo) {
		super(new AHandle(), context);
		this.type_id = type_id;
		this.typeinfo = typeinfo;
	}

	public Object copy(CopyContext cc) {
		InspectorNode node = new InspectorNode((InspectorProjectionContext)this.getDataContext(), this.type_id, this.typeinfo);
		return this.copyTo(node,cc);
	}
}

@ThisIsANode
public class InspectorSymRefNode extends InspectorNode {

	@nodeSRef
	@nodeAttr
	public	Object		ident_or_symbol_or_type;

	public InspectorSymRefNode(InspectorProjectionContext context, String type_id, TypeInfo typeinfo) {
		super(context, type_id, typeinfo);
	}

	public Object copy(CopyContext cc) {
		InspectorSymRefNode node = new InspectorSymRefNode((InspectorProjectionContext)this.getDataContext(), this.type_id, this.typeinfo);
		return this.copyTo(node,cc);
	}
}

@ThisIsANode
public abstract class InspectorAttribute extends ANode {
	@nodeAttr abstract
	public:ro String name;

	final
	public AttrSlot attr_slot;
	
	public InspectorAttribute(InspectorProjectionContext context, AttrSlot attr_slot) {
		super(new AHandle(), context);
		this.attr_slot = attr_slot;
	}
	
	@getter public final String get$name() { return attr_slot.name; }
}

@ThisIsANode
public abstract class InspectorScalarAttribute extends InspectorAttribute {
	@nodeAttr
	public Object data;
	
	public InspectorScalarAttribute(InspectorProjectionContext context, ScalarAttrSlot attr_slot) {
		super(context, attr_slot);
	}
	
	@getter public Object get$data() { return this.data; }
	@setter public void set$data(Object data) { this.data = data; }
}

@ThisIsANode
public class InspectorTextAttr extends InspectorScalarAttribute {
	@nodeAttr abstract
	public String value;
	
	public InspectorTextAttr(InspectorProjectionContext context, ScalarAttrSlot attr_slot) {
		super(context, attr_slot);
	}

	public Object copy(CopyContext cc) {
		InspectorTextAttr node = new InspectorTextAttr((InspectorProjectionContext)this.getDataContext(), (ScalarAttrSlot)this.attr_slot);
		return this.copyTo(node,cc);
	}

	@getter public String get$value() {
		Object data = this.data;
		if (data == null)
			return null;
		if (data instanceof String)
			return (String)data;
		return String.valueOf(data);
	}
	@setter public void set$value(String text) {
		Class clazz = attr_slot.typeinfo.clazz;
		if      (clazz == Boolean.class || clazz == Boolean.TYPE)
			setBoolean(text);
		else if (clazz == Character.class || clazz == Character.TYPE)
			setCharacter(text);
		else if (clazz == Float.class || clazz == Float.TYPE)
			this.data = Float.valueOf(text);
		else if (clazz == Double.class || clazz == Double.TYPE)
			this.data = Double.valueOf(text);
		else if (clazz == Byte.class || clazz == Byte.TYPE)
			setInteger(text);
		else if (clazz == Short.class || clazz == Short.TYPE)
			setInteger(text);
		else if (clazz == Integer.class || clazz == Integer.TYPE)
			setInteger(text);
		else if (clazz == Long.class || clazz == Long.TYPE)
			setInteger(text);
		else if (clazz == String.class)
			this.data = text;
		else
			throw new ClassCastException("Value "+text+" cannot be casted to "+clazz);
	}
	private void setBoolean(String text) {
		if (text.equalsIgnoreCase("true"))
			this.data = Boolean.TRUE;
		else if (text.equalsIgnoreCase("false"))
			this.data = Boolean.FALSE;
		else
			throw new RuntimeException("Parse error for boolean: "+text);
	}

	private void setCharacter(String text) {
		if (text.length() > 1)
			text = ConstExpr.source2ascii(text);
		if (text.length() == 1)
			this.data = Character.valueOf(text.charAt(0));
		else
			throw new RuntimeException("Parse error for character: "+text);
	}

	private void setInteger(String text) {
		int radix = 10;
		boolean neg = false;
		if( text.startsWith("-") ) { text = text.substring(1); neg = true; }
		if( text.startsWith("0x") || text.startsWith("0X") ) { text = text.substring(2); radix = 16; }
		else if( text.startsWith("0") && text.length() > 1 ) { text = text.substring(1); radix = 8; }
		long l = ConstExpr.parseLong(text,radix);
		if (neg)
			l = -l;
		Class clazz = attr_slot.typeinfo.clazz;
		if (clazz == Byte.class || clazz == Byte.TYPE)
			this.data = Byte.valueOf((byte)l);
		else if (clazz == Short.class || clazz == Short.TYPE)
			this.data = Short.valueOf((short)l);
		else if (clazz == Integer.class || clazz == Integer.TYPE)
			this.data = Integer.valueOf((int)l);
		else if (clazz == Long.class || clazz == Long.TYPE)
			this.data = Long.valueOf(l);
		else
			this.data = BigInteger.valueOf(l);
	}

}

@ThisIsANode
public class InspectorEnumAttr extends InspectorScalarAttribute {
	@nodeAttr abstract
	public Object value;
	
	public InspectorEnumAttr(InspectorProjectionContext context, ScalarAttrSlot attr_slot) {
		super(context, attr_slot);
	}
	public Object copy(CopyContext cc) {
		InspectorEnumAttr node = new InspectorEnumAttr((InspectorProjectionContext)this.getDataContext(), (ScalarAttrSlot)this.attr_slot);
		return this.copyTo(node,cc);
	}
	@getter public Object get$value() {
		return this.data;
	}
	@setter public void set$value(Object data) {
		this.data = data;
	}
	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot == InspectorEnumAttr.nodeattr$value) {
			if (attr_slot.typeinfo.clazz == Boolean.TYPE) {
				AutoCompleteResult result = new AutoCompleteResult(true);
				result.append("false", "boolean", null, Boolean.FALSE); 
				result.append("true", "boolean", null, Boolean.TRUE);
				return result;
			}
			if (attr_slot.typeinfo.clazz == Boolean.class) {
				AutoCompleteResult result = new AutoCompleteResult(true);
				result.append("false", "Boolean", null, Boolean.FALSE); 
				result.append("true", "Boolean", null, Boolean.TRUE);
				result.append("null", "null", null, null);
				return result;
			}
			if (Enum.class.isAssignableFrom(attr_slot.typeinfo.clazz) && Enum.class != attr_slot.typeinfo.clazz) {
				AutoCompleteResult result = new AutoCompleteResult(true);
				foreach (Enum e; (Enum[])attr_slot.typeinfo.clazz.getDeclaredMethod(Constants.nameEnumValues).invoke(null)) {
					result.append(e.toString(), e.getClass().getName(), null, e); 
				}
				result.append("null", "null", null, null);
				return result;
			}
		}
		return null;
	}
}

@ThisIsANode
public class InspectorNodeAttr extends InspectorAttribute {
	@nodeAttr
	public InspectorNode node;
	
	@nodeAttr abstract
	public:ro String type_id;

	public InspectorNodeAttr(InspectorProjectionContext context, ScalarAttrSlot attr_slot) {
		super(context, attr_slot);
	}
	public Object copy(CopyContext cc) {
		InspectorNodeAttr node = new InspectorNodeAttr((InspectorProjectionContext)this.getDataContext(), (ScalarAttrSlot)this.attr_slot);
		return this.copyTo(node,cc);
	}
	@getter public String get$type_id() {
		InspectorNode node = this.node;
		if (node == null)
			return "null";
		Language lang = node.getCompilerLang();
		if (lang == null)
			return "void:" + node.getCompilerNodeName();
		return lang.getName() + ":" + node.getCompilerNodeName();
	}
}

@ThisIsANode
public class InspectorSpaceAttr extends InspectorAttribute {
	@nodeAttr
	public InspectorNode∅ nodes;
	
	@nodeAttr abstract
	public:ro int length;

	public InspectorSpaceAttr(InspectorProjectionContext context, SpaceAttrSlot attr_slot) {
		super(context, attr_slot);
	}
	public Object copy(CopyContext cc) {
		InspectorSpaceAttr node = new InspectorSpaceAttr((InspectorProjectionContext)this.getDataContext(), (SpaceAttrSlot)this.attr_slot);
		return this.copyTo(node,cc);
	}
	@getter public int get$length() {
		return nodes.length;
	}
}

@ThisIsANode
public class InspectorExtSpaceAttr extends InspectorAttribute {
	@nodeAttr
	public InspectorNode∅ nodes;
	
	public InspectorExtSpaceAttr(InspectorProjectionContext context, ExtSpaceAttrSlot attr_slot) {
		super(context, attr_slot);
	}
	public Object copy(CopyContext cc) {
		InspectorExtSpaceAttr node = new InspectorExtSpaceAttr((InspectorProjectionContext)this.getDataContext(), (ExtSpaceAttrSlot)this.attr_slot);
		return this.copyTo(node,cc);
	}
}
