package kiev.dump.bin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;

import javax.xml.namespace.QName;

import kiev.dump.BinDumpFilter;
import kiev.dump.Convertor;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vlang.DNode;
import kiev.vlang.Env;
import kiev.vlang.KievPackage;
import kiev.vlang.MetaFlag;
import kiev.vlang.TypeDecl;
import kiev.vtree.INode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ASpaceAttrSlot;
import kiev.vtree.ExtSpaceIterator;
import kiev.vtree.NameAndUUID;
import kiev.vtree.NodeTypeInfo;
import kiev.vtree.ParentAttrSlot;
import kiev.vtree.Symbol;
import kiev.vtree.SymbolRef;
import kiev.vtree.SymUUID;
import kiev.vtree.ITreeWalker;

public class DumpMarshallingContext implements MarshallingContext {

	public final INode[] roots;
	public final Env env;
	public final BinDumpWriter writer;
	public final BinDumpFilter filter;
	
	private final ANodeDumpMarshaller node_marshaller = new ANodeDumpMarshaller();
	private final DataMarshaller data_marshaller = new DataMarshaller();
	
	private final IdentityHashMap<Object,SymbElem> symbTable = new IdentityHashMap<Object,SymbElem>();
	private final IdentityHashMap<INode,NodeElem> nodeTable = new IdentityHashMap<INode,NodeElem>();
	private final IdentityHashMap<Class,NodeElem> flagTable = new IdentityHashMap<Class,NodeElem>();
	private final IdentityHashMap<Object,ConstElem> constTable = new IdentityHashMap<Object,ConstElem>();
	private final IdentityHashMap<AttrSlot,AttrElem> attrTable = new IdentityHashMap<AttrSlot,AttrElem>();
	private final HashMap<String,TypeElem> typeTable = new HashMap<String,TypeElem>();
	
	private int symbol_id_counter;
	private int node_id_counter;
	private int const_id_counter;
	private int attr_id_counter;
	private int type_id_counter;
	
	public DumpMarshallingContext(INode[] roots, Env env, BinDumpFilter filter, BinDumpWriter writer) {
		this.roots = roots;
		this.env = env;
		this.writer = writer;
		this.filter = filter;
		for (TypeElem te : TypeElem.SPACIAL_TYPES)
			typeTable.put(te.name, te);
		type_id_counter = 256;
	}
	
	public Env getEnv() {
		return env;
	}

	/**
	 * Adds new marshaller to this context
	 * @param m marshaler to add
	 * @return self
	 */
	public MarshallingContext add(Marshaller m) {
		throw new UnsupportedOperationException("Marshalling error");
	}

	/**
	 * Adds new data converter to this context
	 * @param c converter to add
	 * @return self
	 */
	public MarshallingContext add(Convertor c) {
		throw new UnsupportedOperationException("Marshalling error");
	}
	
    public void marshalDocument() {
    	try {
    		createElements();
    		writeElements();
	    	for (INode root : roots)
	    		node_marshaller.marshal(root, writer, this);
    		writeElemOffsets();
    	} catch (Exception e) {
    		throw new RuntimeException("Marshalling error", e);
    	}
/*
    	System.out.println("Symbols: "+symbTable.size()+", 0x"+Integer.toHexString(symbTable.size()));
    	System.out.println("Constants: "+constTable.size()+", 0x"+Integer.toHexString(constTable.size()));
    	System.out.println("Attributes: "+attrTable.size()+", 0x"+Integer.toHexString(attrTable.size()));
    	System.out.println("Types: "+typeTable.size()+", 0x"+Integer.toHexString(typeTable.size()));
    	System.out.println("Nodes: "+nodeTable.size()+", 0x"+Integer.toHexString(nodeTable.size()));
    	System.out.println("Flags: "+flagTable.size()+", 0x"+Integer.toHexString(flagTable.size()));
    	int n = 0;
    	for (NodeElem ne : nodeTable.values()) {
    		if (ne.id > 0)
    			n += 1;
    	}
    	for (NodeElem ne : flagTable.values()) {
    		if (ne.id > 0)
    			n += 1;
    	}
    	System.out.println("NID: "+n+", 0x"+Integer.toHexString(n));
*/
    }

    /**
	 * Marshal another object searching for the default marshaller
	 * @param data the next item to convert
	 */
    public void marshalData(Object data) {
		throw new UnsupportedOperationException("Marshalling error");
    }
    
    /**
     * Marshal another object using the specified marshaller
     * @param data       the next item to convert
     * @param marshaller the marshaller to use
     */
    public void marshalData(Object data, Marshaller marshaller) {
		throw new UnsupportedOperationException("Marshalling error");
    }

	/**
	 * Convert (attribute) data into String
	 * @param data the item to convert
	 */
	public String convertData(Object data) {
		throw new UnsupportedOperationException("Marshalling error");
	}

	/**
	 * Write attribute searching for the default string convertor
	 * @param attr the attribute name
	 * @param data the item to convert
	 */
    public void attributeData(QName attr, Object data) {
		throw new UnsupportedOperationException("Marshalling error");
    }
    
    /**
     * Write attribute using the specified convertor
	 * @param attr the attribute name
     * @param data       the next item to convert
     * @param convertor  the convertor to use
     */
    public void attributeData(QName attr, Object data, Convertor convertor) {
		throw new UnsupportedOperationException("Marshalling error");
    }

    /** Get local symbol element */
    public SymbElem getSymbolElem(Symbol sym) {
    	return symbTable.get(sym);
    }
    public SymbElem getSymbolElem(NameAndUUID nid) {
    	return symbTable.get(nid);
    }
    public SymbElem getSymbolElem(String name) {
    	return symbTable.get(name);
    }
    /** Get local node element */
    public NodeElem getNodeElem(INode node) {
    	if (node instanceof MetaFlag)
    		return flagTable.get(node.getClass());
    	return nodeTable.get(node);
    }
    /** Get type element */
    public TypeElem getTypeElem(String type_id) {
    	return typeTable.get(type_id);
    }
    /** Get attribute element */
    public AttrElem getAttrElem(AttrSlot slot) {
    	return attrTable.get(slot);
    }
    public ConstElem getConstElem(Enum eval) {
    	return constTable.get(eval);
    }
    
    // Scan data from the root and create elements
    private void createElements() {
    	if (roots == null || roots.length == 0)
    		return;
    	for (INode root : roots) {
	    	root.walkTree(null, null, new ITreeWalker() {
	    		public boolean pre_exec(INode node, INode parent, AttrSlot slot) {
	    	    	if (node instanceof Symbol)
	    	    		makeSymbolElem((Symbol)node);
	    	    	else if (node instanceof SymbolRef) {
	    	    		Object name = node.getVal(node.getAttrSlot("ident_or_symbol_or_type"));
	    	    		if (name instanceof Symbol)
	        	    		makeSymbolElem((Symbol)name);
	    	    		if (name instanceof NameAndUUID)
	        	    		makeSymbolElem((NameAndUUID)name);
	    	    	}
	    	    	else if (node instanceof MetaFlag) {
	    	    		makeFlagElem(node.getClass());
	    	    	}
	    	    	else
	    	    		makeNodeElem(node);
	    			//ExtSpaceIterator en = node.asANode().getExtSpaceIterator(null);
	    			//while (en.hasMoreElements()) {
	    			//	AttrSlot attr = en.nextAttrSlot();
	    			//	INode n = en.nextElement();
	    			//	if (!attr.isAttr() || !attr.isExternal())
	    			//		continue;
	    			//	makeAttrElem(attr);
	    			//}
	    	    	if (node instanceof DNode) {
	    				Class[] flags = ((DNode)node).getMetaFlags();
	    				for (int fl = 0; fl < 32; fl++) {
	    					if (flags[fl] != null)
	    	    	    		makeFlagElem(flags[fl]);
	    				}
	    	    	}
					return true;
	    		}
	    	});
	    	NodeElem ne = makeNodeElem(root);
	    	if (ne.id == 0)
	    		ne.id = ++node_id_counter;
    	}
    	for (SymbElem se : symbTable.values()) {
    		Symbol sym = se.symbol;
    		if (sym == null || se.target != null)
    			continue;
			INode p = sym.parent();
			if (p instanceof DNode) {
				NodeElem ne = nodeTable.get(p);
				if (ne != null) {
					se.target = ne;
					if (ne.id == 0)
						ne.id = ++node_id_counter;
				}
			}
    	}
    }
    
    private SymbElem makeSymbolElem(Symbol sym) {
    	SymbElem se = getSymbolElem(sym);
		if (se != null)
			return se;
		if (sym.parent() instanceof TypeDecl)
			sym.getUUID(env);
		SymUUID suuid = sym.suuid();
		if (suuid == SymUUID.Empty)
			suuid = null;
		SymbElem nse = null;
		Symbol ns = sym.getNameSpaceSymbol();
		if (ns != null)
			nse = makeSymbolElem(ns);
		String name = (String)sym.getVal(sym.getAttrSlot("sname"));
		int symbol_id = ++symbol_id_counter;
		se = new SymbElem(symbol_id, suuid, name, nse, sym);
		if (sym.parent() instanceof KievPackage)
			se.flags |= SymbElem.IS_NAMESPACE;
		symbTable.put(sym, se);
		//Symbol tgt = (Symbol)sym.getVal("target");
		//if (tgt != null)
		//	se.target = makeSymbolElem(tgt);
		return se;
    }
    private SymbElem makeSymbolElem(NameAndUUID nid) {
    	SymbElem se = getSymbolElem(nid);
		if (se != null)
			return se;
		SymUUID suuid = new SymUUID(nid.uuid_high,nid.uuid_low, null);
		SymbElem nse = null;
		String name = nid.name;
		int symbol_id = ++symbol_id_counter;
		se = new SymbElem(symbol_id, suuid, name, nse, null);
		symbTable.put(nid, se);
		return se;
    }
    private NodeElem makeNodeElem(INode node) {
    	NodeElem ne = getNodeElem(node);
		if (ne != null)
			return ne;
		TypeElem te = makeTypeElem(node.getNodeTypeInfo());
		ne = new NodeElem(0, te);
		nodeTable.put(node, ne);
		return ne;
    }
    private NodeElem makeFlagElem(Class clazz) {
    	NodeElem ne = flagTable.get(clazz);
		if (ne != null)
			return ne;
		try {
			ne = makeNodeElem((INode)clazz.newInstance());
		} catch (Exception e) {
			throw new RuntimeException();
		}
		ne.id = ++node_id_counter;
		flagTable.put(clazz, ne);
		return ne;
    }
    private TypeElem makeTypeElem(NodeTypeInfo nti) {
    	TypeElem te = getTypeElem(nti.getId());
    	if (te != null)
    		return te;
		NodeTypeInfo[] sup_nti = nti.getDirectSuperTypes();
		TypeElem[] super_types = new TypeElem[sup_nti.length];
		for (int i=0; i < sup_nti.length; i++)
			super_types[i] = makeTypeElem(sup_nti[i]);
		int type_id = ++type_id_counter;
		te = new TypeElem(type_id, 0, nti.getId());
		te.super_types = super_types;
		ArrayList<AttrElem> attrs = new ArrayList<AttrElem>();
		AttrSlot[] all_slots = nti.getAllAttributes();
		for (AttrSlot a : all_slots) {
			if (!a.isBinLeading() || a.isBinIgnore() || a instanceof ParentAttrSlot) continue;
			attrs.add(makeAttrElem(a));
		}
		for (AttrSlot a : all_slots) {
			if (a.isBinLeading() || a.isBinIgnore() || a instanceof ParentAttrSlot) continue;
			attrs.add(makeAttrElem(a));
		}
		te.attrs = attrs.toArray(new AttrElem[attrs.size()]);
		typeTable.put(nti.getId(), te);
		return te;
    }
    private TypeElem makeEnumTypeElem(Class clazz) {
    	String name = clazz.getName().replace('.', '·').intern();
    	TypeElem te = getTypeElem(name);
    	if (te != null)
    		return te;
		int type_id = ++type_id_counter;
		te = new TypeElem(type_id, TypeElem.IS_ENUM, name);
		ArrayList<ConstElem> consts = new ArrayList<ConstElem>();
		for (Enum e : (Enum[])clazz.getEnumConstants()) {
			consts.add(makeConstElem(te, e));
		}
		te.consts = consts.toArray(new ConstElem[consts.size()]);
		typeTable.put(name, te);
		return te;
    }
    private AttrElem makeAttrElem(AttrSlot slot) {
    	AttrElem ae = getAttrElem(slot);
		if (ae != null)
			return ae;
		int attr_id = ++attr_id_counter;
		Class clazz = slot.typeinfo.clazz;
		TypeElem vtype;
		if			(clazz == Symbol.class)								vtype = TypeElem.teSYMBOL;
		else if	(clazz == SymbolRef.class)								vtype = TypeElem.teSYMREF;
		else if	(slot.isSymRef())										vtype = TypeElem.teSYMREF;
		else if	(slot instanceof ParentAttrSlot)						vtype = TypeElem.tePARENT;
		else if	(clazz == Boolean.class)								vtype = TypeElem.teBOOL;
		else if	(clazz == Boolean.TYPE)									vtype = TypeElem.teBOOL;
		else if	(clazz == Byte.class)									vtype = TypeElem.teINT;
		else if	(clazz == Byte.TYPE)									vtype = TypeElem.teINT;
		else if	(clazz == Short.class)									vtype = TypeElem.teINT;
		else if	(clazz == Short.TYPE)									vtype = TypeElem.teINT;
		else if	(clazz == Integer.class)								vtype = TypeElem.teINT;
		else if	(clazz == Integer.TYPE)									vtype = TypeElem.teINT;
		else if	(clazz == Long.class)									vtype = TypeElem.teINT;
		else if	(clazz == Long.TYPE)									vtype = TypeElem.teINT;
		else if	(clazz == Float.class)									vtype = TypeElem.teFLOAT;
		else if	(clazz == Float.TYPE)									vtype = TypeElem.teFLOAT;
		else if	(clazz == Double.class)									vtype = TypeElem.teFLOAT;
		else if	(clazz == Double.TYPE)									vtype = TypeElem.teFLOAT;
		else if	(clazz == Character.class)								vtype = TypeElem.teCHAR;
		else if	(clazz == Character.TYPE)								vtype = TypeElem.teCHAR;
		else if	(clazz == String.class)									vtype = TypeElem.teSTRING;
		else if	(Enum.class.isAssignableFrom(clazz))					vtype = makeEnumTypeElem(clazz);
		else															vtype = TypeElem.teDATA;
		int flags = 0;
		if (slot.isChild())
			flags |= AttrElem.IS_CHILD;
		if (slot.isBinLeading())
			flags |= AttrElem.IS_LEADING;
		else
			flags |= AttrElem.IS_OPTIONAL;
		if (slot instanceof ASpaceAttrSlot)
			flags |= AttrElem.IS_SPACE;
		//if (slot.isExternal())
		//	flags |= AttrElem.IS_EXTERNAL;
		if (slot.isNotCopyable())
			flags |= AttrElem.IS_NO_COPY;
		
		ae = new AttrElem(attr_id, vtype, flags, slot);
		attrTable.put(slot, ae);
		return ae;
    }
    private ConstElem makeConstElem(TypeElem vtype, Enum val) {
		int const_id = ++const_id_counter;
		ConstElem ce = new ConstElem(const_id, vtype, val);
		constTable.put(val, ce);
		return ce;
    }
    
    // Write standard elements
    private void writeElements() throws Exception {
    	{	// write symbols
			SymbElem[] elems = symbTable.values().toArray(new SymbElem[symbTable.size()]);
			Arrays.sort(elems);
			for (SymbElem el : elems)
				writeSymbolElem(el);
		}
    	{	// write attributes
			AttrElem[] elems = attrTable.values().toArray(new AttrElem[attrTable.size()]);
			Arrays.sort(elems);
			for (AttrElem el : elems)
				writeAttrElem(el);
		}
    	{	// write types
			TypeElem[] elems = typeTable.values().toArray(new TypeElem[typeTable.size()]);
			Arrays.sort(elems);
			for (TypeElem el : elems)
				writeTypeElem(el);
		}
    	{	// write const values
			ConstElem[] elems = constTable.values().toArray(new ConstElem[constTable.size()]);
			Arrays.sort(elems);
			for (ConstElem el : elems)
				writeConstElem(el);
    	}
    	{	// write flag nodes
			Class[] elems = flagTable.keySet().toArray(new Class[flagTable.size()]);
			for (Class clazz : elems)
				node_marshaller.marshal(clazz.newInstance(), writer, this);
		}
    	// write root nodes
    	for (INode root : roots)
    		writer.writeRootRef(getNodeElem(root));
    }

    // Write standard elements
    private void writeElemOffsets() throws Exception {
    	writer.startTable();
    	{	// write symbols
			SymbElem[] elems = symbTable.values().toArray(new SymbElem[symbTable.size()]);
			Arrays.sort(elems);
			for (Elem el : elems)
				writer.writeTableEntry(el, Signature.TAG_SYMB_SIGN);
		}
    	{	// write attributes
			AttrElem[] elems = attrTable.values().toArray(new AttrElem[attrTable.size()]);
			Arrays.sort(elems);
			for (Elem el : elems)
				writer.writeTableEntry(el, Signature.TAG_ATTR_SIGN);
		}
    	{	// write types
			TypeElem[] elems = typeTable.values().toArray(new TypeElem[typeTable.size()]);
			Arrays.sort(elems);
			for (TypeElem el : elems) {
				if (!el.isSpecial())
					writer.writeTableEntry(el, Signature.TAG_TYPE_SIGN);
			}
		}
    	{	// write const values
			ConstElem[] elems = constTable.values().toArray(new ConstElem[constTable.size()]);
			Arrays.sort(elems);
			for (Elem el : elems)
				writer.writeTableEntry(el, Signature.TAG_CONST_SIGN);
    	}
    	{	// write nodes
			NodeElem[] elems = nodeTable.values().toArray(new NodeElem[nodeTable.size()]);
			Arrays.sort(elems);
			for (Elem el : elems) {
				if (el.id != 0 && el.saddr != 0)
					writer.writeTableEntry(el, Signature.TAG_NODE_SIGN);
			}
		}
    	// write root node
    	for (INode root : roots)
    		writer.writeTableEntry(getNodeElem(root), Signature.TAG_ROOT_SIGN);
    	writer.endTable();
    }

    private void writeSymbolElem(SymbElem se) throws Exception {
    	if (se.eaddr != 0)
    		return;	// already written
		se.saddr = writer.getStreamPos();
		writer.startBlock(Signature.TAG_SYMB_SIGN);
		writer.writeElemID(se);
		writer.writeElemFlags(se);
		if (se.uuid != null) {
			writer.writeValueTag(Signature.TAG_OCTET);
			writer.writeInt8(16);	// UUID is 16 bytes long
			writer.writeInt64(se.uuid.high);
			writer.writeInt64(se.uuid.low);
			writer.writeInt8(0);		// zero-terminate the octet
		}
		if (se.namesp != null)
			writer.writeSymbolRef(se.namesp, false);
		if (se.name != null)
			writer.writeString(se.name);
		if (se.target != null) {
			if (se.target instanceof SymbElem)
				writer.writeSymbolRef((SymbElem)se.target, true);
			else if (se.target instanceof NodeElem)
				writer.writeNodeRef((NodeElem)se.target, true);
		}
		if (se.comment != null)
			writer.writeComment(se.comment.text);
		writer.endBlock(Signature.TAG_SYMB_SIGN);
		se.eaddr = writer.getStreamPos();
    }
    
    private void writeConstElem(ConstElem ce) throws Exception {
    	if (ce.eaddr != 0)
    		return;	// already written
		ce.saddr = writer.getStreamPos();
		writer.startBlock(Signature.TAG_CONST_SIGN);
		writer.writeElemID(ce);
		writer.writeElemFlags(ce);
		writer.writeTypeRef(ce.vtype);
		if (ce.value instanceof Enum) {
			writer.writeString(((Enum)ce.value).name());
		}
		else if (ce.value instanceof String) {
			writer.writeString((String)ce.value);
		}
		else if (ce.value instanceof Number) {
			data_marshaller.marshal(ce.value, writer, this);
		}
		if (ce.comment != null)
			writer.writeComment(ce.comment.text);
		writer.endBlock(Signature.TAG_CONST_SIGN);
		ce.eaddr = writer.getStreamPos();
    }
    
    private void writeAttrElem(AttrElem ae) throws Exception {
    	if (ae.eaddr != 0)
    		return;	// already written
		ae.saddr = writer.getStreamPos();
		writer.startBlock(Signature.TAG_ATTR_SIGN);
		writer.writeElemID(ae);
		writer.writeElemFlags(ae);
		writer.writeTypeRef(ae.vtype);
		if (ae.intype != null) {
			writer.writeValueTag(Signature.TAG_TABLE_SIGN);
			writer.writeTypeRef(ae.intype);
		}
		writer.writeString(ae.name);
		if (ae.comment != null)
			writer.writeComment(ae.comment.text);
		writer.endBlock(Signature.TAG_ATTR_SIGN);
		ae.eaddr = writer.getStreamPos();
    }
    
    private void writeTypeElem(TypeElem te) throws Exception {
    	if (te.eaddr != 0)
    		return;	// already written
		te.saddr = writer.getStreamPos();
		writer.startBlock(Signature.TAG_TYPE_SIGN);
		writer.writeElemID(te);
		writer.writeElemFlags(te);
		writer.writeString(te.name);
		if (te.super_types != null) {
			for (TypeElem sup : te.super_types)
				writer.writeTypeRef(sup);
		}
		if (te.attrs != null) {
			for (AttrElem ae : te.attrs)
				writer.writeAttrRef(ae);
		}
		if (te.consts != null) {
			for (ConstElem ce : te.consts)
				writeConstElem(ce);
		}
		if (te.comment != null)
			writer.writeComment(te.comment.text);
		writer.endBlock(Signature.TAG_TYPE_SIGN);
		te.eaddr = writer.getStreamPos();
    }
}
