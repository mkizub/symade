/*******************************************************************************
 * Copyright (c) 2005-2008 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *     Roman Chepelyev (gromanc@gmail.com) - implementation and refactoring
 *******************************************************************************/
package kiev.gui;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;

import kiev.dump.DumpFactory;
import kiev.dump.xml.ImportMarshallingContext;
import kiev.dump.xml.ImportTypeAlias;
import kiev.dump.xml.ImportUnMarshaller;
import kiev.gui.event.Action;
import kiev.gui.event.Binding;
import kiev.gui.event.BindingSet;
import kiev.gui.event.Event;
import kiev.gui.event.EventActionMap;
import kiev.gui.event.Item;
import kiev.gui.event.KeyboardEvent;
import kiev.gui.event.MouseEvent;
import kiev.gui.event.InputEvent;
import kiev.vlang.Env;

/**
 * Configuration.
 */
public abstract class Configuration {
	
	public final Env env;
	
	/**
	 * Key modifiers for keyboard and mouse events.
	 */
	public enum Modifiers { CTRL, ALT, SHIFT };
	
			
	/**
	 * Default configuration for editors.
	 */
	protected BindingSet editorBindingsDefault;
	
	/**
	 * The configuration for editors.
	 */
	protected BindingSet editorBindings;
	
	/**
	 * The editors bindings.
	 */
	protected EventActionMap editorNaviMap;

	/**
	 * Default configuration for views.
	 */
	protected BindingSet infoBindingsDefault;
	
	/**
	 * The configuration for views.
	 */
	protected BindingSet infoBindings;
	
	/**
	 *  The views bindings.
	 */
	protected EventActionMap infoNaviMap;

	/**
	 * Default configuration for project view.
	 */
	protected BindingSet projectBindingsDefault;
	
	/**
	 * The configuration for project view.
	 */
	protected BindingSet projectBindings;
	
	/**
	 * The project view bindings.
	 */
	protected EventActionMap projectNaviMap;
	
	/**
	 * Default configuration for project view.
	 */
	protected BindingSet errorsBindingsDefault;
	
	/**
	 * The configuration for project view.
	 */
	protected BindingSet errorsBindings;
	
	/**
	 * The project view bindings.
	 */
	protected EventActionMap errorsNaviMap;
	
	protected Configuration(Env env) {
		this.env = env;
	}
	
	/**
	 * Returns associated modifier mask.
	 * @param mods the modifiers
	 * @return int
	 */
	public abstract int getModifierMask(Modifiers mods);

	/**
	 * Creates <code>InputEvent</code>.
	 * @return InputEvent
	 */
	public abstract InputEvent makeInputEvent();
	
	/**
	 * Creates <code>InputEvent</code> from parameters.
	 * @param mask the modifiers mask
	 * @param code the key code
	 * @return InputEvent
	 */
	public abstract InputEvent makeKeyboardInputEvent(int mask, int code);
	
	/**
	 * Creates <code>InputEvent</code> from parameters.
	 * @param mask the modifiers mask
	 * @param count the mouse click count
	 * @param button the mouse button code
	 * @return InputEvent
	 */
	public abstract InputEvent makeMouseInputEvent(int mask, int count, int button);
	

	/**
	 * Iterate through collection of elements to create an <code>InputEvent</code> and
	 * instantiates defined by configuration associated action factory. These events 
	 * and associated action factories are mapped to the bindings.
	 * @param naviMap the bindings
	 * @param bindings the collection of elements
	 */
	private void addBindings(EventActionMap naviMap, BindingSet bindings){
		if (bindings == null) return;
		for (Item item: bindings.items){
			try {
				if (item instanceof Binding){
					Binding bnd = (Binding)item;
					for (Event event: bnd.events){
						InputEvent ei = null;
						if (event instanceof KeyboardEvent){
							KeyboardEvent kbe = (KeyboardEvent)event;
							int mask = 0, code;
							code = kbe.keyCode;
							if (kbe.withAlt) mask |= getModifierMask(Modifiers.ALT);
							else if (kbe.withCtrl) mask |= getModifierMask(Modifiers.CTRL);
							else if (kbe.withShift) mask |= getModifierMask(Modifiers.SHIFT);  
							ei = makeKeyboardInputEvent(mask, code); 
						} 
						else if (event instanceof MouseEvent){
							MouseEvent me = (MouseEvent)event;
							int mask = 0, button, count;
							count = me.count;
							button = me.button;
							if (me.withAlt) mask |= getModifierMask(Modifiers.ALT);
							else if (me.withCtrl) mask |= getModifierMask(Modifiers.CTRL);
							else if (me.withShift) mask |= getModifierMask(Modifiers.SHIFT);  
							ei = makeMouseInputEvent(mask, count, button); 
						}		
						
						//create instance of action factory class
						naviMap.add(ei,	makeActionFactory(bnd.action));
					}
				} else 
				if (item instanceof Action){					
					naviMap.add(makeInputEvent(),	makeActionFactory((Action)item));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}		
	}

	/**
	 * Creates action factory from <code>Action</code>.
	 * @param action the action
	 * @return UIActionFactory
	 */
	private UIActionFactory makeActionFactory(Action action){
		UIActionFactory af = null;
		try {
			Class<?> clazz = Class.forName(action.actionClass, false, getClass().getClassLoader());
			for (Constructor<?> cr: clazz.getConstructors()){
				Class<?>[] pt = cr.getParameterTypes();
				if (pt.length == 2){
					Object[] initargs = new Object[2];
					initargs[0] = action.description;
					initargs[1] = action.isForPopupMenu;
					af = (UIActionFactory)cr.newInstance(initargs);
					break;
				}
			}
			if (af == null) af = (UIActionFactory)clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e){
			e.printStackTrace();
		}
		return af;		
	}
	
	/**
	 * Restore configuration to their defaults.
	 */
	public void resetBindings() {
		editorBindings = editorBindingsDefault;
		infoBindings = infoBindingsDefault;
		projectBindings = projectBindingsDefault;
		errorsBindings = errorsBindingsDefault;
	}

	
	/**
	 * The configuration is not bound until mapped. 
	 * @param eam the event action map
	 * @param bs the binding set
	 * @return <code>EventActionMap</code>
	 */
	private  EventActionMap getActionMap(EventActionMap eam, BindingSet bs){
		if (eam != null) return eam;		
		EventActionMap naviMap = new EventActionMap();
		eam = naviMap;
		addBindings(naviMap, bs);	
		return naviMap;
		
	}
	/**
	 * Returns the editors bindings. 
	 * @return <code>EventActionMap</code>
	 */
	public EventActionMap getEditorActionMap() {
		return getActionMap(editorNaviMap, editorBindings);
	}

	/**
	 * Returns the project view bindings. 
	 * @return <code>EventActionMap</code>
	 */
	public EventActionMap getProjectViewActionMap() {
		return getActionMap(projectNaviMap, projectBindings);
	}

	/**
	 * Returns the views bindings. 
	 * @return <code>EventActionMap</code>
	 */
	public EventActionMap getInfoViewActionMap() {
		return getActionMap(infoNaviMap, infoBindings);
	}
	
	/**
	 * Returns the errors bindings. 
	 * @return <code>EventActionMap</code>
	 */
	public EventActionMap getErrorsViewActionMap() {
		return getActionMap(errorsNaviMap, errorsBindings);
	}
	
	/**
	 * Import configuration bindings from dumped XML.
	 * @param src_bs the node
	 */
	public void attachBindings(kiev.fmt.evt.BindingSet src_bs) {
		try {
			byte[] data = DumpFactory.getXMLDumper().exportToXmlData(env, src_bs);
			BindingSet bs = (BindingSet)DumpFactory.getXMLDumper().importFromXmlStream(new ByteArrayInputStream(data), getBindingsImportContext());
			bs.init();
			attachBindings(bs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initializes corresponding members.
	 * @param bs the binding set
	 */
	private void attachBindings(BindingSet bs) {
		String name = bs.qname;
		if (name != null && name.indexOf('.') >= 0)
			name = name.substring(name.lastIndexOf('.')+1);
		if ("bindings-editor".equals(name)) {
			editorBindings = bs;
			editorNaviMap = null;
		} else
		if ("bindings-info".equals(name)) {
			infoBindings = bs;
			infoNaviMap = null;
		} else
		if ("bindings-project".equals(name)) {
			projectBindings = bs;
			projectNaviMap = null;
		}
		if ("bindings-errors".equals(name)) {
			errorsBindings = bs;
			errorsNaviMap = null;
		}
	}
	
	/**
	 * Returns Bindings Import Context.
	 * @return <code>ImportMarshallingContext</code>
	 */
	private ImportMarshallingContext getBindingsImportContext() {
		ImportUnMarshaller um = new ImportUnMarshaller();
		um.addTypeAlias(new ImportTypeAlias("bindings", BindingSet.class).
				addImplicitFieldAlias("action", "items").
				addImplicitFieldAlias("bind", "items").
				addFieldAlias("name", "qname"));
		um.addTypeAlias(new ImportTypeAlias("action", Action.class).
				addFieldAlias("menu", "isForPopupMenu").
				addFieldAlias("factory", "actionClass"));
		um.addTypeAlias(new ImportTypeAlias("bind", Binding.class).
				addImplicitFieldAlias("action", "action").
				addImplicitFieldAlias("key", "events").
				addImplicitFieldAlias("mouse", "events"));
		um.addTypeAlias(new ImportTypeAlias("key", KeyboardEvent.class).
				addFieldAlias("key", "keyCode").
				addFieldAlias("ctrl", "withCtrl").
				addFieldAlias("alt", "withAlt").
				addFieldAlias("shift", "withShift"));
		um.addTypeAlias(new ImportTypeAlias("mouse", MouseEvent.class).
				addFieldAlias("ctrl", "withCtrl").
				addFieldAlias("alt", "withAlt").
				addFieldAlias("shift", "withShift"));
		ImportMarshallingContext context = new ImportMarshallingContext(um);
		return context;
	}
	
	/**
	 * Loads configuration from resource.
	 * @param name the resource name
	 * @return <code>BindingSet</code>
	 */
	public BindingSet loadBindings(String name) {
		InputStream inp = null;
		try {
			inp = ClassLoader.getSystemResourceAsStream(name);
			System.out.println("Loading event bindings from "+name);
			BindingSet bs;
			if (name.endsWith(".ser")) {
				ObjectInput oi = new ObjectInputStream(inp);
				bs = (BindingSet)oi.readObject();
			} else {
				bs = (BindingSet)DumpFactory.getXMLDumper().importFromXmlStream(new BufferedInputStream(inp), getBindingsImportContext());
			}
			bs.init();
			attachBindings(bs);
			return bs;
		} catch (Exception e) {
			System.out.println("Read error while bindings deserialization: "+e);
		} finally {
			try { inp.close(); } catch (Exception e) {}
		}
		return null;
	}
	
}
