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
package kiev.gui.swt;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

/**
 * The transfer used by the clipboard to transfer objects in the current JVM.
 */
public class LocalObjectTransfer extends ByteArrayTransfer {

	/**
	 * The transfer type name. This could check up expected types.
	 */
	private static final String TYPE_NAME = "local-object-transfer-format" + System.currentTimeMillis();
	
	/**
	 * Type ID. Gets it via registering type name. 
	 */
	private static final int TYPEID = registerType(TYPE_NAME);
	
	/**
	 * Singleton instance.
	 */
	private static final LocalObjectTransfer INSTANCE = new LocalObjectTransfer();
	
	/**
	 * The object holder.
	 */
	private Object object;
	
	/**
	 * The time when object is set to clipboard.
	 */
	private long objectSetTime;

	/**
	 * Singleton pattern.
	 */
	protected LocalObjectTransfer() {}

	/**
	 * Returns the single instance of this class.
	 * @return the transfer
	 */
	public static LocalObjectTransfer getTransfer() {
		return INSTANCE;
	}

	/**
	 * Returns the object set for transfer.
	 * @return the object
	 */
	public Object getObject() {
		return object;
	}

	/**
	 * Checks that is valid type to transfer.
	 * @param result <code>true</code> if invalid transfer type, <code>false</code> otherwise
	 * @return boolean
	 */
	private boolean isInvalidNativeType(Object result) {
		return !(result instanceof byte[])
		|| !TYPE_NAME.equals(new String((byte[]) result));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.Transfer#getTypeIds()
	 */
	protected int[] getTypeIds() {
		return new int[] { TYPEID };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.Transfer#getTypeNames()
	 */
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.ByteArrayTransfer#javaToNative(java.lang.Object, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public void javaToNative(Object object, TransferData transferData) {
		byte[] check = TYPE_NAME.getBytes();
		super.javaToNative(check, transferData);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.ByteArrayTransfer#nativeToJava(org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public Object nativeToJava(TransferData transferData) {
		Object result = super.nativeToJava(transferData);
		if (isInvalidNativeType(result)) {
			System.out.println("LocalObjectTransfer error: Invalid Native Type");	        	 
		}
		return object;
	}

	/**
	 * Sets the object for transfer.
	 * @param o the object
	 */
	public void setObject(Object o) {
		object = o;
	}

	/**
	 * Returns the object set time in milliseconds.
	 * @return long
	 */
	public long getObjectSetTime() {
		return objectSetTime;
	}

	/**
	 * Sets time in milliseconds. 
	 * @param time the time
	 */
	public void setObjectSetTime(long time) {
		objectSetTime = time;
	}
}


