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
package kiev.dump.xml;

import kiev.Kiev;
import kiev.dump.AcceptInfo;
import kiev.dump.DumpFactory;
import kiev.dump.UnMarshaller;
import kiev.dump.UnMarshallingContext;
import kiev.dump.AttributeSet;
import kiev.stdlib.TypeInfo;
import kiev.vtree.*;
import kiev.vlang.*;
import kiev.vlang.types.Type;

import java.io.File;
import java.util.StringTokenizer;
import javax.xml.namespace.QName;

import static kiev.stdlib.Asserts.*;

public class ANodeUnMarshaller implements UnMarshaller {

	public static final String SOP_URI = "sop://sop/";

	INode root;

	private String getTypeInfoSign(AttributeSet attributes) {
		int n = attributes.getCount();
		for (int i=0; i < n; i++) {
			String nm = attributes.getName(i);
			if (nm.equals("ti"))
				return attributes.getValue(i);
		}
		return null;
	}

	private String getUUID(AttributeSet attributes) {
		int n = attributes.getCount();
		for (int i=0; i < n; i++) {
			String nm = attributes.getName(i);
			if (nm.equals("uuid"))
				return attributes.getValue(i);
		}
		return null;
	}

	private AttrSlot getAttrFromQName(INode node, QName qname) {
		String uri = qname.getNamespaceURI();
		if (uri != null && uri.length() > 0)
			return null;
		String name = qname.getLocalPart();
		for (AttrSlot a : node.values()) {
			if (!a.isXmlIgnore() && a.getXmlLocalName().equals(name))
				return a;
		}
		for (AttrSlot a : node.values()) {
			if (!a.isXmlIgnore() && a.name.equals(name))
				return a;
		}
		return null;
	}

    public boolean canUnMarshal(QName qname, AttributeSet attrs, UnMarshallingContext context) {
		String uri = qname.getNamespaceURI();
		if (uri.length() == 0)
			return false;
		//String name = qname.getLocalPart();
		if (uri.equals(SOP_URI) || ((DumpUnMarshallingContext)context).getLanguage(uri) != null)
			return true;
		return false;
	}

	public AcceptInfo canAccept(Object self, QName qname, AttributeSet attrs, UnMarshallingContext context) {
		AttrSlot attr = getAttrFromQName((INode)self, qname);
		if (attr != null)
			return new AcceptInfo(true, qname);
		return null;
	}

	public Object exit(Object self, UnMarshallingContext context) {
		return self;
	}

    public Object create(QName qname, AttributeSet attributes, UnMarshallingContext ucontext) {
    	try {
		DumpUnMarshallingContext context = (DumpUnMarshallingContext)ucontext;
		String uri = qname.getNamespaceURI();
		String name = qname.getLocalPart();
		Language lng = null;
		String cl_name = null;
		String ti_sign = getTypeInfoSign(attributes);
		String dn_uuid = context.is_import ? null : getUUID(attributes);
		INode result = null;
		if (uri.equals(SOP_URI)) {
			cl_name = name;
		} else {
			lng = context.getLanguage(uri);
			cl_name = lng.getClassByNodeName(name);
		}
		if (root == null) {
			if (cl_name.equals("kiev.vlang.FileUnit")) {
				File file = context.file;
				if (file == null) {
					if (context.tdname != null)
						file = new File(context.tdname.replace('·','/')+".xml");
					else if (context.pkg != null)
						file = new File(((GlobalDNode)context.pkg).qname().replace('·','/')+".xml");
				}
				FileUnit fu;
				if (file == null)
					fu = new FileUnit();
				else
					fu = FileUnit.makeFile(DumpFactory.getRelativePath(file), Env.getProject(), false);
				root = fu;
				if (fu.getVal(fu.getAttrSlot("ftype")) == null)
					fu.setVal(fu.getAttrSlot("ftype"), "text/xml/tree-dump");
				fu.setVal(fu.getAttrSlot("current_syntax"), new ProjectSyntaxFactoryXmlDump());
				addAttributes(context, fu, attributes);
				if (context.pkg instanceof KievPackage) {
					INode srpkg = (INode)fu.getVal(fu.getAttrSlot("srpkg"));
					srpkg.setVal(srpkg.getAttrSlot("symbol"), context.pkg.getVal(context.pkg.getAttrSlot("symbol")));
				}
				result = root;
			}
			else if (context.pkg != null) {
				String q_name;
				if (context.pkg instanceof KievRoot)
					q_name = context.tdname;
				else
					q_name = ((GlobalDNode)context.pkg).qname() + '·' + context.tdname;
				DNode dn = context.getEnv().resolveGlobalDNode(q_name);
				Symbol sym = null;
				if (dn != null)
					sym = (Symbol)dn.getVal(dn.getAttrSlot("symbol"));
				else
					sym = ((Symbol)context.pkg.getVal(context.pkg.getAttrSlot("symbol"))).makeGlobalSubSymbol(context.tdname);
				if (cl_name.equals("kiev.vlang.KievPackage"))
					dn = context.getEnv().newPackage(context.tdname, (KievPackage)context.pkg);
				else if (lng != null)
					dn = (DNode)lng.makeNode(context.getEnv(), name, ti_sign, dn_uuid);
				else if (ti_sign != null)
					dn = (DNode)TypeInfo.newTypeInfo(ti_sign).newInstance();
				else
					dn = (DNode)Class.forName(cl_name).newInstance();
				if (dn_uuid != null) {
					if (sym.suuid() != null && !sym.suuid().toString().equals(dn_uuid))
						System.out.println("Warning: Different UUIDs of global symbol "+sym.qname());
					else
						sym.setUUID(context.getEnv(),dn_uuid);
				}
				if (dn.getVal(dn.getAttrSlot("symbol")) != sym) {
					if (sym.parent() != null)
						sym.parent().detach();
					dn.setVal(dn.getAttrSlot("symbol"), sym.detach());
				}
				addAttributes(context, dn, attributes);
				root = dn;
				if (dn instanceof KievPackage) {
					dn.setAutoGenerated(true);
					if (dn.parent() == null)
						context.pkg.addVal(context.pkg.getAttrSlot("pkg_members"), dn);
				}
				else if (dn instanceof DNode) {
					if (dn.parent() == null) {
						if (context.pkg instanceof KievPackage)
							context.pkg.addVal(context.pkg.getAttrSlot("pkg_members"), dn);
						else
							context.pkg.addVal(context.pkg.getAttrSlot("members"), dn);
					}
				}
				if (context.is_interface_only)
					dn.setInterfaceOnly();
				result = dn;
			}
			else {
				if (lng != null)
					root = lng.makeNode(context.getEnv(), name, ti_sign, dn_uuid);
				else if (ti_sign != null)
					root = (INode)TypeInfo.newTypeInfo(ti_sign).newInstance();
				else
					root = (INode)Class.forName(cl_name).newInstance();
				addAttributes(context, root, attributes);
				if (root instanceof DNode && context.is_interface_only)
					((DNode)root).setInterfaceOnly();
				result = root;
			}
		} else {
			INode n;
			AttrSlot attr = getAttrFromQName((INode)context.peekNode(), context.peekAttr());
			if (!attr.isWrittable()) {
				if (attr instanceof ASpaceAttrSlot) {
					if (ti_sign != null)
						n = (INode)TypeInfo.newTypeInfo(ti_sign).newInstance();
					else
						n = (INode)attr.typeinfo.newInstance();
				} else {
					n = (INode)((ScalarAttrSlot)attr).get((INode)context.peekNode());
					if (n == null) {
						if (ti_sign != null)
							n = (INode)TypeInfo.newTypeInfo(ti_sign).newInstance();
						else
							n = (INode)attr.typeinfo.newInstance();
					}
				}
				addAttributes(context, n, attributes);
			}
			else if (lng != null) {
				n = lng.makeNode(context.getEnv(), name, ti_sign, dn_uuid);
				addAttributes(context, n, attributes);
			}
			else {
				try {
					if (ti_sign != null)
						n = (INode)TypeInfo.newTypeInfo(ti_sign).newInstance();
					else
						n = (INode)Class.forName(cl_name).newInstance();
					addAttributes(context, n, attributes);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					n = null;
				}
			}
			if (n instanceof DNode) {
				if (context.is_interface_only)
					((DNode)n).setInterfaceOnly();
				if (!n.isAttached() && context.peekNode() instanceof SyntaxScope) {
					KievPackage pkg = ((SyntaxScope)context.peekNode()).getPackage();
					pkg.addVal(pkg.getAttrSlot("pkg_members"), n);
				}
			}
			result = n;
		}

		return result;
    	} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
    		throw new RuntimeException(e);
    	}
	}

	public void accept(Object self, QName qname, Object target, UnMarshallingContext ucontext) {
		DumpUnMarshallingContext context = (DumpUnMarshallingContext)ucontext;
		String uri = qname.getNamespaceURI();
		String name = qname.getLocalPart();
		Language lng = null;
		AttrSlot attr = null;
		if (uri.length() > 0) {
			lng = context.getLanguage(uri);
			if (lng != null)
				attr = lng.getExtAttrByName(name);
			if (attr == null) {
				System.out.println("Attribute '"+qname+"' not found in language "+(lng!=null?lng.getName():"?"));
				return;
			}
		} else {
			attr = getAttrFromQName((INode)self, qname);
			if (attr == null) {
				System.out.println("Attribute '"+name+"' not found in "+self.getClass());
				return;
			}
		}

		if (target instanceof TypeDecl) {
			((TypeDecl)target).setTypeDeclNotLoaded(false);
		}
		if (attr instanceof ASpaceAttrSlot) {
			if (target instanceof INode)
				((ASpaceAttrSlot)attr).add((INode)self, (INode)target);
		}
		else if (attr instanceof ScalarAttrSlot){
			if (target instanceof INode) {
				if (!((INode)target).isAttached())
					((ScalarAttrSlot)attr).set((INode)self, (INode)target);
			} else {
				if (target instanceof String) {
					String text = (String)target;
					if (text.length() > 0) {
						try {
							writeAttribute(context, (INode)self, (ScalarAttrSlot)attr, text);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
	}

	private void addAttributes(DumpUnMarshallingContext context, INode node, AttributeSet attributes) throws Exception {
		int n = attributes.getCount();
	next_attr:
		for (int i=0; i < n; i++) {
			String nm = attributes.getName(i);
			String prefix = attributes.getPrefix(i);
			if (prefix != null && prefix.length() > 0)
				continue;
			if (nm.equals("class") || nm.equals("ti"))
				continue;
			if (nm.equals("uuid")) {
				if (context.is_import)
					continue;
				if (node instanceof Symbol) {
					Symbol sym = (Symbol)node;
					String dn_uuid = attributes.getValue(i);
					if (sym.suuid() != null && !sym.suuid().toString().equals(dn_uuid))
						System.out.println("Warning: Different UUIDs of global symbol "+sym.qname());
					else
						sym.setUUID(context.getEnv(),dn_uuid);
				}
				else if (node instanceof DNode) {
					Symbol sym = (Symbol)node.getVal(node.getAttrSlot("symbol"));
					String dn_uuid = attributes.getValue(i);
					if (sym.suuid() != null && !sym.suuid().toString().equals(dn_uuid))
						System.out.println("Warning: Different UUIDs of global symbol "+sym.qname());
					else
						sym.setUUID(context.getEnv(),dn_uuid);
				}
				else if (node instanceof MetaUUID)
					node.setVal(node.getAttrSlot("value"), attributes.getValue(i));
				continue;
			}
			if (nm.equals("p")) {
				StringTokenizer st = new StringTokenizer(attributes.getValue(i), ":", true);
				int lineNo = 0;
				int linePos = 0;
				long filePos = 0;
				String fileName = null;
				do {
					if (st.hasMoreTokens()) {
						String lnStr = st.nextToken();
						if (!lnStr.equals(":"))
							lineNo = Integer.parseInt(lnStr);
						if (!st.hasMoreTokens() || !st.nextToken().equals(":"))
							break;
					}
					if (st.hasMoreTokens()) {
						String lnStr = st.nextToken();
						if (!lnStr.equals(":"))
							linePos = Integer.parseInt(lnStr);
						if (!st.hasMoreTokens() || !st.nextToken().equals(":"))
							break;
					}
					if (st.hasMoreTokens()) {
						String lnStr = st.nextToken();
						if (!lnStr.equals(":"))
							filePos = Long.parseLong(lnStr);
						if (!st.hasMoreTokens() || !st.nextToken().equals(":"))
							break;
					}
					if (st.hasMoreTokens()) {
						fileName = st.nextToken();
						// just in case of file names with ':' character
						while (st.hasMoreTokens())
							fileName += st.nextToken();
					}
				} while (false);
				long pos = (filePos << 32) | (lineNo << 12) | (linePos & 0xFFF);
				if (pos != 0 && node instanceof ANode)
					((ANode)node).pos = pos;
				if (fileName != null && !fileName.isEmpty())
					((ANode)node).setFileName(fileName);
				continue;
			}
			for (AttrSlot attr : node.values()) {
				if (attr instanceof ScalarAttrSlot && !attr.isXmlIgnore() && attr.getXmlLocalName().equals(nm)) {
					writeAttribute(context, node, (ScalarAttrSlot)attr, attributes.getValue(i));
					continue next_attr;
				}
			}
			for (AttrSlot attr : node.values()) {
				if (attr instanceof ScalarAttrSlot && !attr.isXmlIgnore() && attr.name.equals(nm)) {
					writeAttribute(context, node, (ScalarAttrSlot)attr, attributes.getValue(i));
					continue next_attr;
				}
			}
			System.out.println("Attribute '"+nm+"' not found in "+node.getClass());
		}
	}
	//@SuppressWarnings("unchecked")
	private void writeAttribute(DumpUnMarshallingContext context, INode node, ScalarAttrSlot attr, String value) throws Exception {
		Class clazz = attr.typeinfo.clazz;
		if (clazz == String.class)
			attr.set(node,value);
		else if (clazz == Boolean.TYPE)
			attr.set(node,Boolean.valueOf(value.trim()));
		else if (clazz == Integer.TYPE)
			attr.set(node,Integer.valueOf((int)parseLong(value)));
		else if (clazz == Byte.TYPE)
			attr.set(node,Byte.valueOf((byte)parseLong(value)));
		else if (clazz == Short.TYPE)
			attr.set(node,Short.valueOf((short)parseLong(value)));
		else if (clazz == Long.TYPE)
			attr.set(node,Long.valueOf(parseLong(value)));
		else if (clazz == Float.TYPE)
			attr.set(node,Float.valueOf(value.trim()));
		else if (clazz == Double.TYPE)
			attr.set(node,Double.valueOf(value.trim()));
		else if (clazz == Character.TYPE)
			attr.set(node,Character.valueOf(value.trim().charAt(0)));
		else if (Enum.class.isAssignableFrom(clazz)) {
			//attr.set(node,Enum.valueOf(clazz,value.trim()));
			if (Enum.class != clazz && Enum.class.isAssignableFrom(clazz))
				attr.set(node,clazz.getMethod("valueOf",String.class).invoke(null,value.trim()));
			else if (node instanceof ConstEnumExpr && Enum.class.isAssignableFrom(((ConstEnumExpr)node).getTypeInfoField().getTopArgs()[0].clazz) && Enum.class != ((ConstEnumExpr)node).getTypeInfoField().getTopArgs()[0].clazz)
				attr.set(node,((ConstEnumExpr)node).getTypeInfoField().getTopArgs()[0].clazz.getMethod("valueOf",String.class).invoke(null,value.trim()));
			else
				Kiev.reportWarning("Attribute '"+attr.name+"' of "+node.getClass()+" uses unsupported "+clazz);
		}
		else if (clazz == Operator.class)
			attr.set(node,Operator.getOperatorByName(value.trim()));
		else if (Type.class.isAssignableFrom(clazz)) {
			//attr.set(node,AType.fromSignature(value.trim()));
			if (node instanceof ENode && attr.name == "type_lnk")
				((ENode)node).setTypeSignature(value.trim());
			else
				context.delayed_types.add(new DelayedTypeInfo(node, attr, value.trim()));
		}
		else if (Symbol.class == clazz) {
			value = value.trim();
			Symbol symbol;
			if (attr.isWrittable())
				symbol = (Symbol)attr.typeinfo.newInstance();
			else
				symbol = (Symbol)attr.get(node);
			int p = value.indexOf('‣');
			if (p >= 0) {
				String sname = value.substring(0,p);
				String uuid = value.substring(p+1);
				symbol.setVal(symbol.getAttrSlot("sname"), sname);
				if (!context.is_import)
					symbol.setUUID(context.getEnv(),uuid);
			} else {
				symbol.setVal(symbol.getAttrSlot("sname"), value);
			}
			if (attr.isWrittable())
				attr.set(node,symbol);
		}
		else if (SymbolRef.class == clazz) {
			SymbolRef symref;
			if (attr.isWrittable())
				symref = (SymbolRef)attr.typeinfo.newInstance();
			else
				symref = (SymbolRef)attr.get(node);
			value = value.trim();
			int p = value.indexOf('‣');
			if (p >= 0) {
				String name = value.substring(0,p);
				String uuid = value.substring(p+1);
				if (!context.is_import)
					symref.setNameAndUUID(new NameAndUUID(name, uuid, context.getEnv()));
				else
					symref.setVal(symref.getAttrSlot("name"), name);
			} else {
				symref.setVal(symref.getAttrSlot("name"), value);
			}
			if (attr.isWrittable())
				attr.set(node,symref);
		}
		else if (attr.isSymRef()) {
			value = value.trim();
			int p = value.indexOf('‣');
			if (p >= 0) {
				String name = value.substring(0,p);
				String uuid = value.substring(p+1);
				if (!context.is_import)
					attr.set(node, new NameAndUUID(name, uuid, context.getEnv()));
				else
					attr.set(node,name);
			} else {
				attr.set(node,value);
			}
		}
		else
			//throw new SAXException("Attribute '"+attr.name+"' of "+node.getClass()+" uses unsupported "+clazz);
			Kiev.reportWarning("Attribute '"+attr.name+"' of "+node.getClass()+" uses unsupported "+clazz);
	}
	private static long parseLong(String text) {
		text = text.trim();
		int radix;
		if( text.startsWith("0x") || text.startsWith("0X") ) { text = text.substring(2); radix = 16; }
		else if( text.startsWith("0") && text.length() > 1 ) { text = text.substring(1); radix = 8; }
		else { radix = 10; }
		if (text.charAt(text.length()-1) == 'L' || text.charAt(text.length()-1) == 'l') {
			text = text.substring(0,text.length()-1);
			if (text.length() == 0)
				return 0L; // 0L
		}
		long l = ConstExpr.parseLong(text,radix);
		return l;
	}
}

