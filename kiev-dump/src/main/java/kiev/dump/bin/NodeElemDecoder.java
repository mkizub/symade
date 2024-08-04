package kiev.dump.bin;

import java.util.Map;

import kiev.dump.DumpException;
import kiev.stdlib.TypeInfo;
import kiev.vlang.ConstEnumExpr;
import kiev.vlang.DNode;
import kiev.vlang.ENode;
import kiev.vlang.FileUnit;
import kiev.vlang.KievPackage;
import kiev.vlang.MetaFlag;
import kiev.vlang.Operator;
import kiev.vlang.Project;
import kiev.vlang.types.Type;
import kiev.vtree.AttrSlot;
import kiev.vtree.ASpaceAttrSlot;
import kiev.vtree.INode;
import kiev.vtree.ScalarAttrSlot;
import kiev.vtree.Symbol;
import kiev.vtree.SymbolRef;
import kiev.vtree.Copier;

public class NodeElemDecoder extends ElemDecoder<NodeElem> {

	private TagAndVal[] leading_tav;
	private int attr_pos;

	NodeElemDecoder(BinDumpReader reader) {
		super(reader);
	}

	String elName() { return "node"; }
	Signature elSignature() { return Signature.TAG_NODE_SIGN; }

	NodeElem makeInstance(int id, int addr) { return new NodeElem(id, addr); }
	Map<Integer,NodeElem> getTable() { return reader.nodeTable; }

	boolean readValue(TagAndVal tav) throws DumpException
	{
		NodeElem ne = (NodeElem)el;
		TypeElem te = ne.tp;
		if (tav.tag == Signature.TAG_TYPE_SIGN) {
			if (te != null)
				throw new DumpException("Corrupted dump file: duplicated node type at "+tav.pos);
			te = (TypeElem)tav.val;
			ne.tp = te;
			if (te.leading_attrs > 0)
				leading_tav = new TagAndVal[te.leading_attrs];
			else
				checkMakeNode();
			return true;
		}
		if (ne.node == null) {
			if (!checkMakeNode()) {
				leading_tav[attr_pos] = tav;
				attr_pos++;
				checkMakeNode();
				return true;
			}
		}
		if (tav.tag == Signature.TAG_LINENO) {
			ne.node.asANode().setLineNo(tav.intVal());
			return true;
		}
		if (tav.tag == Signature.TAG_VOID) {
			checkValTag(tav.tag, ne.node, te);
			attr_pos++;
			return true;
		}
		if (tav.tag == Signature.TAG_SPACE_START) {
			checkValTag(tav.tag, ne.node, te);
			while ( (tav=reader.readTagAndVal(true)).tag != Signature.TAG_SPACE_END) {
				if (tav.tag == Signature.TAG_NODE_SIGN) {
					INode n = ((NodeElem)tav.val).node;
					if (n instanceof MetaFlag) {
						n = new Copier().copyFull(n);
						AttrSlot slot = getAttrSlot();
						((ASpaceAttrSlot)slot).add(ne.node, n);
					} else {
						ne.node.addVal(getAttrSlot(), n);
					}
				}
				else if (tav.tag == Signature.TAG_NODE_REF) {
					NodeElem nr = (NodeElem)tav.val;
					if (!nr.isRead())
						nr = (NodeElem)reader.dfactory.makeDecoder(tav.tag, reader).readElem(nr.id, nr.saddr);
					INode n = nr.node;
					if (n instanceof MetaFlag) {
						n = new Copier().copyFull(n);
						AttrSlot slot = getAttrSlot();
						((ASpaceAttrSlot)slot).add(ne.node, n);
					} else {
						ne.node.addVal(getAttrSlot(), n);
					}
				}
				else if (tav.tag == Signature.TAG_SYMB_SIGN) {
					SymbElem se = (SymbElem)tav.val;
					Symbol sym = se.makeSymbol(reader.env);
					ne.node.addVal(getAttrSlot(), sym);
				}
				else if (tav.tag == Signature.TAG_SYMB_REF) {
					SymbElem se = (SymbElem)tav.val;
					Symbol sym = se.makeSymbol(reader.env);
					AttrSlot slot = getAttrSlot();
					SymbolRef sref;
					if (slot.typeinfo.clazz == SymbolRef.class)
						sref = (SymbolRef)slot.typeinfo.newInstance();
					else
						sref = (SymbolRef)TypeInfo.newTypeInfo("kiev.vtree.SymbolRef").newInstance();
					sref.setVal(sref.getAttrSlot("ident_or_symbol_or_type"), sym);
					ne.node.addVal(getAttrSlot(), sref);
				}
				else if (tav.tag == Signature.TAG_VOID) {
					continue;
				}
				else
					throw new DumpException("Corrupted dump file: unexpected value tag '"+tav.tag.sign+"' in node space at "+tav.pos);
			}
			attr_pos++;
			return true;
		}
		if (tav.tag == Signature.TAG_EXT_START) {
			for (;;) {
				TagAndVal a = reader.readTagAndVal(true);
				if (a.tag == Signature.TAG_EXT_END)
					break;
				if (a.tag != Signature.TAG_ATTR_SIGN)
					throw new DumpException("Corrupted dump file: unexpected tag '"+tav.tag.sign+"' in value map at "+tav.pos);
				AttrElem ae = (AttrElem)tav.val;
				TagAndVal v = reader.readTagAndVal(true);
				setValue(ne, ne.node, ae, v);
			}
			return true;
		}
		// otherwice set it as an attribute value
		checkValTag(tav.tag, ne.node, te);
		setValue(ne, ne.node, te.attrs[attr_pos], tav);
		attr_pos += 1;
		return true;
	}

	private AttrSlot getAttrSlot() {
		return el.tp.attrs[attr_pos].getAttrSlot();
	}

	private void checkValTag(Signature tag, INode node, TypeElem te) throws DumpException {
		if (te == null)
			throw new DumpException("Corrupted dump file: value tag '"+tag.sign+"' before node type specification at "+(reader.buf.position()-1));
		if (attr_pos >= te.attrs.length)
			throw new DumpException("Corrupted dump file: unexpected value tag '"+tag.sign+"' for attr index "+attr_pos+" in type"+te.name+" at "+(reader.buf.position()-1));
	}

	private boolean checkMakeNode() throws DumpException {
		TypeElem te = el.tp;
		if (attr_pos >= te.leading_attrs) {
			NodeElem ne = el;
			if (te.typeinfo.clazz == FileUnit.class) {
				String fname = leading_tav[0].strVal();
				FileUnit fu = FileUnit.makeFile(reader.cur_dir+"/"+fname, reader.env.proj, false);
				fu.loded_from_binary_dump = true;
				ne.node = fu;
			} else {
				ne.node = (INode)te.typeinfo.newInstance();
				for (int a=0; a < te.leading_attrs; a++) {
					setValue(ne, ne.node, te.attrs[a], leading_tav[a]);
				}
				if (!ne.node.isAttached() && ne.node instanceof DNode) {
					DNode dn = (DNode)ne.node;
					Symbol sym = (Symbol)dn.getVal(dn.getAttrSlot("symbol"));
					if (sym != null && sym.getNameSpaceSymbol() != null) {
						Symbol ns = sym.getNameSpaceSymbol();
						INode p = ns.parent();
						if (p instanceof KievPackage)
							p.addVal(p.getAttrSlot("pkg_members"), dn);
					}
				}
			}
			return true;
		}
		return false;
	}

	private void setValue(NodeElem ne, INode node, AttrElem ae, TagAndVal tav) throws DumpException {
		if (tav.tag == Signature.TAG_NULL) {
			if (ae.vtype == TypeElem.teSYMREF) {
				//AttrSlot slot = ae.getAttrSlot();
				//SymbolRef sref;
				//if (ae.getAttrSlot().isWrittable()) {
				//	if (slot.typeinfo.clazz == SymbolRef.class)
				//		sref = (SymbolRef)slot.typeinfo.newInstance();
				//	else
				//		sref = (SymbolRef)TypeInfo.newTypeInfo("kiev.vtree.SymbolRef").newInstance();
				//	sref.setVal("ident_or_symbol_or_type", null);
				//	node.setVal(slot, sref);
				//} else {
				//	sref = (SymbolRef)node.getVal(slot);
				//	sref.setVal("ident_or_symbol_or_type", null);
				//}
				return;
			}
			node.setVal(ae.getAttrSlot(), null);
			return;
		}
		if (tav.tag == Signature.TAG_NODE_SIGN) {
			if (ae.getAttrSlot().isWrittable())
				node.setVal(ae.getAttrSlot(), ((NodeElem)tav.val).node);
			return;
		}
		if (tav.tag == Signature.TAG_NODE_REF) {
			NodeElem nr = (NodeElem)tav.val;
			if (!nr.isRead())
				nr = (NodeElem)reader.dfactory.makeDecoder(tav.tag, reader).readElem(nr.id, nr.saddr);
			INode n = nr.node;
			node.setVal(ae.getAttrSlot(), n);
			return;
		}
		if (tav.tag == Signature.TAG_SYMB_SIGN) {
			SymbElem se = (SymbElem)tav.val;
			Symbol sym = se.makeSymbol(reader.env);
			AttrSlot slot = ae.getAttrSlot();
			if (node.getVal(slot) != sym) {
				if (sym.isAttached() && slot.isChild()) {
					if (node instanceof DNode && slot.name == "symbol") {
						INode p = sym.parent();
						INode pp = p.parent();
						p.detach();
						if (pp instanceof KievPackage && !node.isAttached()) {
							sym.detach();
							node.setVal(ae.getAttrSlot(), sym);
							pp.addVal(pp.getAttrSlot("pkg_members"), node);
							return;
						}
					}
					sym.detach();
				}
				node.setVal(ae.getAttrSlot(), sym);
			}
			return;
		}
		if (tav.tag == Signature.TAG_SYMB_REF) {
			SymbElem se = (SymbElem)tav.val;
			Symbol sym = se.makeSymbol(reader.env);
			AttrSlot slot = ae.getAttrSlot();
			if (slot.typeinfo.clazz == Symbol.class) {
				node.setVal(slot, sym);
				return;
			}
			SymbolRef sref = (SymbolRef)node.getVal(slot);
			if (sref != null) {
				sref.setVal(sref.getAttrSlot("ident_or_symbol_or_type"), sym);
			}
			else if (slot.typeinfo.clazz == SymbolRef.class) {
				sref = (SymbolRef)slot.typeinfo.newInstance();
				sref.setVal(sref.getAttrSlot("ident_or_symbol_or_type"), sym);
				node.setVal(ae.getAttrSlot(), sref);
			}
			else {
				sref = (SymbolRef)TypeInfo.newTypeInfo("kiev.vtree.SymbolRef").newInstance();
				sref.setVal(sref.getAttrSlot("ident_or_symbol_or_type"), sym);
				node.setVal(ae.getAttrSlot(), sref);
			}
			return;
		}
		if (tav.tag.is_value) {
			setNodeValue(ne, node, ae, tav);
			return;
		}
		if (tav.tag == Signature.TAG_CONST_SIGN) {
			TagAndVal ctav = new TagAndVal(tav.pos, tav.tag, ((ConstElem)tav.val).value);
			setNodeValue(ne, node, ae, ctav);
			return;
		}
		throw new DumpException("Corrupted dump file: unexpected signature '"+tav.tag.sign+"' at "+tav.pos);
	}

	@SuppressWarnings({"unchecked"})
	private void setNodeValue(NodeElem ne, INode node, AttrElem ae, TagAndVal tav) {
		ScalarAttrSlot attr = (ScalarAttrSlot)ae.getAttrSlot();
		Class clazz = attr.typeinfo.clazz;
		if (clazz == String.class)
			attr.set(node,tav.strVal());
		else if (clazz == Boolean.TYPE)
			attr.set(node,Boolean.valueOf(tav.boolVal()));
		else if (clazz == Integer.TYPE)
			attr.set(node,Integer.valueOf(tav.intVal()));
		else if (clazz == Byte.TYPE)
			attr.set(node,Byte.valueOf((byte)tav.intVal()));
		else if (clazz == Short.TYPE)
			attr.set(node,Short.valueOf((short)tav.intVal()));
		else if (clazz == Long.TYPE)
			attr.set(node,Long.valueOf(tav.longVal()));
		else if (clazz == Float.TYPE)
			attr.set(node,Float.valueOf(tav.floatVal()));
		else if (clazz == Double.TYPE)
			attr.set(node,Double.valueOf(tav.doubleVal()));
		else if (clazz == Character.TYPE)
			attr.set(node,Character.valueOf(tav.charVal()));
		else if (Enum.class.isAssignableFrom(clazz)) {
			try {
				if (tav.val instanceof Enum) {
					attr.set(node,tav.val);
				}
				else if (tav.val instanceof String) {
					if (Enum.class != clazz && Enum.class.isAssignableFrom(clazz))
						attr.set(node,clazz.getMethod("valueOf",String.class).invoke(null,tav.strVal()));
					else if (node instanceof ConstEnumExpr && Enum.class.isAssignableFrom(((ConstEnumExpr)node).getTypeInfoField().getTopArgs()[0].clazz) && Enum.class != ((ConstEnumExpr)node).getTypeInfoField().getTopArgs()[0].clazz)
						attr.set(node,((ConstEnumExpr)node).getTypeInfoField().getTopArgs()[0].clazz.getMethod("valueOf",String.class).invoke(null,tav.strVal()));
					//else
					//	Kiev.reportWarning("Attribute '"+attr.name+"' of "+node.getClass()+" uses unsupported "+clazz);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (clazz == Operator.class) {
			String op = tav.strVal();
			if (op != null && op.length() > 0)
				attr.set(node,Operator.getOperatorByName(op));
		}
		else if (Type.class.isAssignableFrom(clazz)) {
			String sign = tav.strVal();
			if (sign != null && sign.length() > 0) {
				if (node instanceof ENode && attr.name == "type_lnk")
					((ENode)node).setTypeSignature(sign);
				else
					reader.delayed_types.add(new DelayedTypeInfo(ne, node, attr, sign));
			}
		}
		else if (clazz == SymbolRef.class) {
			SymbolRef sref = (SymbolRef)node.getVal(attr);
			if (sref != null) {
				sref.setVal(sref.getAttrSlot("ident_or_symbol_or_type"), tav.strVal());
			}
			else if (attr.typeinfo.clazz == SymbolRef.class) {
				sref = (SymbolRef)attr.typeinfo.newInstance();
				sref.setVal(sref.getAttrSlot("ident_or_symbol_or_type"), tav.strVal());
				node.setVal(ae.getAttrSlot(), sref);
			}
			else {
				sref = (SymbolRef)TypeInfo.newTypeInfo("kiev.vtree.SymbolRef").newInstance();
				sref.setVal(sref.getAttrSlot("ident_or_symbol_or_type"), tav.strVal());
				node.setVal(ae.getAttrSlot(), sref);
			}
		}
		else
			System.out.println("Attribute '"+attr.name+"' of "+node.getClass()+" uses unsupported "+clazz);
	}

}
