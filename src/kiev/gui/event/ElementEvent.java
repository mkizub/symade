package kiev.gui.event;

import java.util.EventObject;



public class ElementEvent extends EventObject {

  protected int id;
  /**
   * The first number in the range of ids used for element events.
   */
  public static final int ELEMENT_FIRST		= 1000;

  /**
   * The last number in the range of ids used for element events.
   */
  public static final int ELEMENT_LAST		= 1000;

  /**
   * This event indicates that the element's changed.
   */
  public static final int ELEMENT_CHANGED	= ELEMENT_FIRST;
  
	public ElementEvent(Object source, int type) {
		super(source);
		this.id = id;
	}
	
  /**
   * Returns the event type.
   */
  public int getID() {
      return id;
  }

}
