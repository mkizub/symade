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
package kiev.vtree;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode(lang=CoreLang)
public final class NodeDecl extends DNode implements GlobalDNodeContainer {
	@SymbolRefAutoComplete @SymbolRefAutoResolve(sever=SeverError.Warning)
	@nodeAttr
	public NodeDecl⇑∅       super_decls;
	@nodeAttr
	public NodeAttribute∅   attrs;
	
	private NodeTypeInfo    node_type_info;
	
	private static int depth;

	public ASTNode[] getContainerMembers() { attrs }

	public String qname() {
		ANode p = parent();
		if (p instanceof GlobalDNode) {
			if (sname == null)
				return ((GlobalDNode)p).qname().intern();
			return (((GlobalDNode)p).qname()+"·"+sname).intern();
		} else {
			return sname;
		}
	}

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		this.node_type_info = null;
		return super.preResolveIn(env, parent, slot);
	}

	public rule resolveNameR(ResInfo path)
	{
		path ?= this
	;	path @= attrs
	}
	
	public NodeTypeInfo makeNodeTypeInfo() {
		if (node_type_info != null)
			return node_type_info;
		depth ++;
		if (depth > 100) {
			System.out.println("Infinit recursion in makeNodeTypeInfo()");
			return null;
		}
		try {
		String id = qname().intern();
		Language lang = null;
		Vector<NodeTypeInfo> direct_super_types = new Vector<NodeTypeInfo>();
		foreach (NodeDecl⇑ snd; super_decls; snd.dnode != null) {
			NodeTypeInfo nti = snd.dnode.makeNodeTypeInfo();
			if (nti != null)
				direct_super_types.append(nti);
		}
		Vector<AttrSlot> defined_slots = new Vector<AttrSlot>();
		foreach (NodeAttribute na; attrs)
			defined_slots.append(na.makeAttrSlot());
		node_type_info = new NodeTypeInfo(id, lang, sname, direct_super_types.toArray(), defined_slots.toArray());
		} finally { depth --; }
		return node_type_info;
	}
}

public enum NodeAttrKind {
	SCALAR_BOOL, SCALAR_CHAR, SCALAR_BYTE, SCALAR_SHORT, SCALAR_INT, SCALAR_LONG, SCALAR_FLOAT, SCALAR_DOUBLE, 
	SCALAR_STRING, SCALAR_ENUM, SCALAR_OBJ, 
	SCALAR_NODE, SPACE_OF_NODES, EXT_SPACE_OF_NODES
}

@ThisIsANode(lang=CoreLang)
public final class NodeAttribute extends DNode {
	private AttrSlot        attr_slot;
	
	@AttrXMLDumpInfo(attr=true, name="kind")
	@nodeAttr
	public NodeAttrKind		attr_kind;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr
	public boolean			is_data;
	
	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		this.attr_slot = null;
		return super.preResolveIn(env, parent, slot);
	}

	//public boolean includeInDump(String dump, AttrSlot attr, Object val) {
	//	if (attr.name == "is_data" && is_data == false)
	//		return false;
	//	return super.includeInDump(dump, attr, val);
	//}

	public AttrSlot makeAttrSlot() {
		if (attr_slot != null)
			return attr_slot;
		if (attr_kind == null)
			attr_kind = NodeAttrKind.SCALAR_NODE;
		switch (attr_kind) {
		case NodeAttrKind.SCALAR_BOOL:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Boolean.TYPE,null));
			break;
		case NodeAttrKind.SCALAR_CHAR:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Character.TYPE,null));
			break;
		case NodeAttrKind.SCALAR_BYTE:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Byte.TYPE,null));
			break;
		case NodeAttrKind.SCALAR_SHORT:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Short.TYPE,null));
			break;
		case NodeAttrKind.SCALAR_INT:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Integer.TYPE,null));
			break;
		case NodeAttrKind.SCALAR_LONG:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Long.TYPE,null));
			break;
		case NodeAttrKind.SCALAR_FLOAT:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Float.TYPE,null));
			break;
		case NodeAttrKind.SCALAR_DOUBLE:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Double.TYPE,null));
			break;
		case NodeAttrKind.SCALAR_ENUM:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Enum.class,null));
			break;
		case NodeAttrKind.SCALAR_STRING:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(String.class,null));
			break;
		case NodeAttrKind.SCALAR_OBJ:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(Object.class,null));
			break;
		case NodeAttrKind.SCALAR_NODE:
			attr_slot = new PScalarAttrSlot(sname, is_data ? null : ANode.nodeattr$parent, TypeInfo.newTypeInfo(ASTNode.class,null));
			break;
		case NodeAttrKind.SPACE_OF_NODES:
			attr_slot = new PSpaceAttrSlot(sname, is_data ? null : ANode.nodeattr$parent);
			break;
		case NodeAttrKind.EXT_SPACE_OF_NODES:
			attr_slot = new PExtSpaceAttrSlot(sname, is_data ? null : ANode.nodeattr$parent);
			break;
		default:
			Debug.assert("Unknown attribute kind "+attr_kind);
		}
		return attr_slot;
	}
}

public final class NodeTypeInfo {

	private final Class             clazz;
	private final String            id;
	private final Language          lang;
	private final String            name_in_lang;
	private final NodeTypeInfo[]    direct_super_types;
	private final AttrSlot[]        defined_slots;
	private final AttrSlot[]        all_slots;
	
	private final NodeTypeInfo		proxy_nti;
	
	private static NodeTypeInfo getNodeTypeInfo(Class clazz) {
		if (!INode.class.isAssignableFrom(clazz))
			return null;
		try {
			java.lang.reflect.Field f = clazz.getDeclaredField("$node_type_info");
			f.setAccessible(true);
			return (NodeTypeInfo)f.get(null);
		} catch (Exception e) {
			return null;
		}
	}
	
	private static AttrSlot[] getAllAttrSlots(Class clazz) {
		try {
			java.lang.reflect.Field f = clazz.getDeclaredField("$values");
			f.setAccessible(true);
			return (AttrSlot[])f.get(null);
		} catch (Exception e) {
			return new AttrSlot[0];
		}
	}
	
	public NodeTypeInfo(NodeTypeInfo nti) {
		this.clazz = PNode.class;
		this.proxy_nti = this;
		this.id = nti.id;
		this.lang = null;
		this.name_in_lang = nti.name_in_lang;
		this.direct_super_types = new NodeTypeInfo[nti.direct_super_types.length];
		for (int i=0; i < nti.direct_super_types.length; i++)
			this.direct_super_types[i] = nti.direct_super_types[i].getProxy();
		this.defined_slots = new AttrSlot[nti.defined_slots.length];
		for (int i=0; i < nti.defined_slots.length; i++) {
			AttrSlot slot = nti.defined_slots[i];
			if (slot instanceof ScalarAttrSlot)
				this.defined_slots[i] = new PScalarAttrSlot((ScalarAttrSlot)slot);
			else if (slot instanceof SpaceAttrSlot)
				this.defined_slots[i] = new PSpaceAttrSlot((SpaceAttrSlot)slot);
			else if (slot instanceof ExtSpaceAttrSlot)
				this.defined_slots[i] = new PExtSpaceAttrSlot((ExtSpaceAttrSlot)slot);
		}
		// calculate all slots
		Vector<AttrSlot> slots = new Vector<AttrSlot>();
		foreach (AttrSlot slot; defined_slots)
			slots.append(slot);
		foreach (NodeTypeInfo sup; direct_super_types) {
			foreach (AttrSlot slot; sup.getAllAttributes(); !slots.contains(slot))
				slots.append(slot);
		}
		this.all_slots = slots.toArray();
	}
	
	public NodeTypeInfo(Class clazz, AttrSlot[] defined_slots) {
		this.clazz = clazz;
		this.id = clazz.getName().replace('.','·').intern();
		ThisIsANode mn = clazz.getAnnotation(ThisIsANode.class);
		if (Language.class.isAssignableFrom(mn.lang()))
			this.lang = (Language)mn.lang().getMethod("getInstance").invoke(null);
		if (this.lang == null)
			this.name_in_lang = clazz.getName().intern();
		else if (mn.name().length() == 0)
			this.name_in_lang = clazz.getSimpleName().intern();
		else
			this.name_in_lang = mn.name().intern();
		Vector<NodeTypeInfo> super_types = new Vector<NodeTypeInfo>();
		NodeTypeInfo sup = getNodeTypeInfo(clazz.getSuperclass());
		if (sup != null)
			super_types.append(sup);
		foreach (Class supi; clazz.getInterfaces(); (sup = getNodeTypeInfo(supi)) != null)
			super_types.append(sup);
		this.direct_super_types = super_types.toArray();
		this.defined_slots = defined_slots;
		this.all_slots = getAllAttrSlots(clazz);
		this.proxy_nti = new NodeTypeInfo(this);
	}

	public NodeTypeInfo(String id, Language lang, String name_in_lang, NodeTypeInfo[] direct_super_types, AttrSlot[] defined_slots) {
		this.clazz = PNode.class;
		this.proxy_nti = this;
		this.id = id;
		this.lang = lang;
		this.name_in_lang = name_in_lang;
		this.direct_super_types = direct_super_types;
		this.defined_slots = defined_slots;
		// calculate all slots
		Vector<AttrSlot> slots = new Vector<AttrSlot>();
		foreach (AttrSlot slot; defined_slots)
			slots.append(slot);
		foreach (NodeTypeInfo sup; direct_super_types) {
			foreach (AttrSlot slot; sup.getAllAttributes(); !slots.contains(slot))
				slots.append(slot);
		}
		this.all_slots = slots.toArray();
	}

	public NodeTypeInfo getProxy() { proxy_nti }
	public String getId() { id }
	public NodeTypeInfo[] getDirectSuperTypes() { direct_super_types }
	public AttrSlot[] getDefinedAttributes() { defined_slots }
	public AttrSlot[] getAllAttributes() { all_slots }
	public Language getCompilerLang() { this.lang }
	public String getCompilerNodeName() { name_in_lang }
	
	public ANode newInstance() {
		if (clazz == PNode.class)
			return new PNode(this);
		if (TypeInfoInterface.class.isAssignableFrom(clazz))
			return (ANode)TypeInfo.makeTypeInfo(clazz,null).newInstance();
		else
			return (ANode)clazz.newInstance();
	}
	
}

