package kiev.gui.swt;

import java.util.EventObject;

import org.eclipse.jface.viewers.TreePath;


public class TreeContentEvent extends EventObject {

	public TreeContentEvent(Object source) {
		super(source);
		// TODO Auto-generated constructor stub
	}

	public TreeContentEvent (Object source, TreePath path){
		super(source);
		
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
