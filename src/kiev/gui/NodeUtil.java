package kiev.gui;

import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

/**
 * Helper class to support some absent operations on trees and multiple trees.
 *
 */
public class NodeUtil {

	/**
	 * Check if the child has this parent.
	 * @param child the child of some parent
	 * @param parent the parent of some child
	 * @return the case 
	 */
	public static final boolean isA(INode child, INode parent){
		if (child == null) return false;
		else
			if (child == parent) return true;
			else return isA(child.parent(), parent);
	}



	/**
	 * Finds and returns first <code>DrawTerm</code> for the given node.
	 * @param root the root drawable
	 * @param node the node
	 * @return the drawable term
	 */
	public static final DrawTerm getFirstLeaf(Drawable root, INode node) {		
		if (root == null || root.isUnvisible())
			return null;
		if (root instanceof DrawTerm && root.drnode == node)
			return (DrawTerm)root;
		Drawable[] children = root.getChildren();
		for (int i=0; i < children.length; i++) {
			Drawable ch = children[i];
			if (ch == null || ch.isUnvisible())
				continue;
			DrawTerm d = getFirstLeaf(ch, node);
			if (d != null && !d.isUnvisible())
				return d;
		}
		return null;
	}
}
