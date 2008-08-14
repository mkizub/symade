package kiev.gui.swing;

import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import kiev.fmt.DrawNodeTerm;
import kiev.fmt.DrawPlaceHolder;
import kiev.fmt.Draw_SyntaxPlaceHolder;
import kiev.fmt.Drawable;
import kiev.gui.ActionPoint;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vtree.ANode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ExtSpaceAttrSlot;
import kiev.vtree.ScalarAttrSlot;
import kiev.vtree.ScalarPtr;
import kiev.vtree.SpaceAttrSlot;
import kiev.vtree.ExtChildrenIterator;
import kiev.vtree.Transaction;

public class Clipboard {

	/** The object in clipboard */
	public static final java.awt.datatransfer.Clipboard clipboard
			= java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	
	public static UIActionFactory newPasteHereFactory(){
		return new PasteHereFactory();
	}
	public static UIActionFactory newPasteNextFactory(){
		return new PasteNextFactory();
	}

	public static void setClipboardContent(Object obj) {
		Transferable tr = null;
		if (obj instanceof ANode)
			tr = new TransferableANode((ANode)obj);
		else
			tr = new StringSelection(String.valueOf(obj));
		Clipboard.clipboard.setContents(tr, (ClipboardOwner)tr);
	}

	final static class PasteHereFactory implements UIActionFactory {
		public String getDescr() { return "Paste an element at this position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			Transferable content = Clipboard.clipboard.getContents(null);
			if (!content.isDataFlavorSupported(TransferableANode.getTransferableANodeFlavor()))
				return null;
			ANode node = null;
			try {
				node = (ANode)content.getTransferData(TransferableANode.getTransferableANodeFlavor());
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			// try paste as a node into null
			if (dr instanceof DrawNodeTerm) {
				DrawNodeTerm dt = (DrawNodeTerm)dr;
				ScalarPtr pattr = dt.getScalarPtr();
				if (pattr.get() == null && pattr.slot.typeinfo.$instanceof(node))
					return new PasteElemHere(node, editor, pattr.node, pattr.slot);
			}
			// try paste as a node into placeholder
			if (dr instanceof DrawPlaceHolder && dr.syntax.elem_decl != null && ((Draw_SyntaxPlaceHolder)dr.syntax).attr_name != null) {
				Draw_SyntaxPlaceHolder dsph = (Draw_SyntaxPlaceHolder)dr.syntax;
				ANode drnode = dr.drnode;
				for (AttrSlot attr: drnode.values()) {
					if (attr.name != dsph.attr_name)
						continue;
					if (attr instanceof ScalarAttrSlot && ((ScalarAttrSlot)attr).get(drnode) == null && attr.typeinfo.$instanceof(node))
						return new PasteElemHere(node, editor, drnode, attr);
					else if (attr instanceof SpaceAttrSlot && ((SpaceAttrSlot)attr).getArray(drnode).length == 0 && attr.typeinfo.$instanceof(node))
						return new PasteElemHere(node, editor, drnode, attr);
				}
			}
			// try paste as an element of list
			ActionPoint ap = editor.getActionPoint(false);
			if (ap != null && ap.length >= 0 && ap.slot.typeinfo.$instanceof(node)) {
				return new PasteElemHere(node, editor, ap);
			}
			return null;
		}
	}

	final static class PasteNextFactory implements UIActionFactory {
		public String getDescr() { return "Paste an element at next position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Transferable content = Clipboard.clipboard.getContents(null);
			if (!content.isDataFlavorSupported(TransferableANode.getTransferableANodeFlavor()))
				return null;
			ANode node = null;
			try {
				node = (ANode)content.getTransferData(TransferableANode.getTransferableANodeFlavor());
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			ActionPoint ap = editor.getActionPoint(true);
			if (ap == null || ap.length < 0)
				return null;
			if (!ap.slot.typeinfo.$instanceof(node))
				return null;
			return new PasteElemNext(editor);
		}
	}
}

final class PasteElemHere implements Runnable {
	final ANode       paste_node;
	final Editor      editor;
	final ANode       into_node;
	final AttrSlot    attr_slot;
	final ActionPoint ap;
	PasteElemHere(ANode paste_node, Editor editor, ANode into_node, AttrSlot attr_slot) {
		this.paste_node = paste_node;
		this.editor = editor;
		this.into_node = into_node;
		this.attr_slot = attr_slot;
		this.ap = null;
	}
	PasteElemHere(ANode paste_node, Editor editor, ActionPoint ap) {
		this.paste_node = paste_node;
		this.editor = editor;
		this.into_node = null;
		this.attr_slot = null;
		this.ap = ap;
	}
	public void run() {
		ANode paste_node = this.paste_node;
		editor.changes.push(Transaction.open("Editor.java:PasteElemHere"));
		try {
			if (paste_node.isAttached())
				paste_node = paste_node.ncopy();
			if (attr_slot != null) {
				if (attr_slot instanceof SpaceAttrSlot) {
					if (((SpaceAttrSlot)attr_slot).indexOf(into_node, paste_node) >= 0)
						paste_node = paste_node.ncopy();
					((SpaceAttrSlot)attr_slot).insert(into_node, 0, paste_node);
				}
				else if (attr_slot instanceof ExtSpaceAttrSlot) {
					for (ExtChildrenIterator iter = ((ExtSpaceAttrSlot)attr_slot).iterate(into_node); iter.hasMoreElements();) {
						if (iter.nextElement() == paste_node) {
							paste_node = paste_node.ncopy();
							break;
						}
					}
					((ExtSpaceAttrSlot)attr_slot).add(into_node, paste_node);
				}
				else if (attr_slot instanceof ScalarAttrSlot) {
					if (((ScalarAttrSlot)attr_slot).get(into_node) == paste_node)
						paste_node = paste_node.ncopy();
					((ScalarAttrSlot)attr_slot).set(into_node, paste_node);
				}
			}
			else if (ap != null) {
				if (((SpaceAttrSlot)ap.slot).indexOf(into_node, paste_node) >= 0)
					paste_node = paste_node.ncopy();
				((SpaceAttrSlot)ap.slot).insert(ap.node,ap.index,paste_node);
			}
		} finally {
			editor.changes.peek().close();
		}
		editor.formatAndPaint(true);
	}
}

final class PasteElemNext implements Runnable {
	final Editor editor;
	PasteElemNext(Editor editor) {
		this.editor = editor;
	}
	public void run() {
		Transferable content = Clipboard.clipboard.getContents(null);
		ANode paste_node = null;
		try {
			paste_node = (ANode)content.getTransferData(TransferableANode.getTransferableANodeFlavor());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		ActionPoint ap = editor.getActionPoint(true);
		editor.changes.push(Transaction.open("Editor.java:PasteElemNext"));
		try {
			if (paste_node.isAttached())
				paste_node = paste_node.ncopy();
			AttrSlot attr_slot = ap.slot;
			if (attr_slot instanceof SpaceAttrSlot) {
				if (((SpaceAttrSlot)attr_slot).indexOf(ap.node, paste_node) >= 0)
					paste_node = paste_node.ncopy();
				((SpaceAttrSlot)attr_slot).insert(ap.node,ap.index,paste_node);
			}
			else if (attr_slot instanceof ExtSpaceAttrSlot) {
				for (ExtChildrenIterator iter = ((ExtSpaceAttrSlot)attr_slot).iterate(ap.node); iter.hasMoreElements();) {
					if (iter.nextElement() == paste_node) {
						paste_node = paste_node.ncopy();
						break;
					}
				}
				((ExtSpaceAttrSlot)attr_slot).add(ap.node, paste_node);
			}
		} finally {
			editor.changes.peek().close();
		}
		editor.formatAndPaint(true);
	}
}


