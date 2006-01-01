/* Generated By:JJTree: Do not edit this line. SimpleNode.java */

package kiev.parser;

import kiev.*;
import kiev.stdlib.*;
import kiev.vlang.*;

@node
public abstract class SimpleNode extends ASTNode {
  
  @att public final NArr<ASTNode> children;

  public SimpleNode() {
	children = new NArr<ASTNode>(this, new AttrSlot("children", true, true));
  }

  public SimpleNode(int i) {
	super(kiev.Kiev.k.getToken(0)==null?0:kiev.Kiev.k.getToken(0).getPos());
	children = new NArr<ASTNode>(this, new AttrSlot("children", true, true));
  }

  public void jjtAddChild(ASTNode n, int i)
  {
	  children.append(n);
  }

  public ASTNode jjtGetChild(int i) {
  	throw new CompilerException(pos,"Unused method jjtGetChild is called");
//    return children[i];
  }

  public int jjtGetNumChildren() {
  	throw new CompilerException(pos,"Unused method jjtGetNumChildren is called");
//    return (children == null) ? 0 : children.length;
  }

  /* You can override these two methods in subclasses of SimpleNode to
     customize the way the node appears when the tree is dumped.  If
     your output uses more than one line you should override
     toString(String), otherwise overriding toString() is probably all
     you need to do. */

//  public String toString() { return kiev003TreeConstants.jjtNodeName[id]; }
//  public String toString(String prefix) { return prefix + toString(); }

  /* Override this method if you want to customize how the node dumps
     out its children. */

//  public void dump(String prefix) {
//    System.out.println(toString(prefix));
//    if (children != null) {
//      for (int i = 0; i < children.length; ++i) {
//		SimpleNode n = (SimpleNode)children[i];
//		if (n != null) {
//		  n.dump(prefix + " ");
//		}
//      }
//    }
//  }

	public Dumper toJava(Dumper dmp) {
		throw new CompilerException(pos,"toJava call for unresolved node");
    }

}
