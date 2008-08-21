package kiev.gui.swt;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class LocalObjectTransfer extends ByteArrayTransfer {

	private static final String TYPE_NAME = "local-object-transfer-format" + (new Long(System.currentTimeMillis())).toString(); 
	private static final int TYPEID = registerType(TYPE_NAME);
	private static final LocalObjectTransfer INSTANCE = new LocalObjectTransfer();
	private Object object;
	private long objectSetTime;

	protected LocalObjectTransfer() {
	}

	public static LocalObjectTransfer getTransfer() {
		return INSTANCE;
	}

	public Object getObject() {
		return object;
	}

	private boolean isInvalidNativeType(Object result) {
		return !(result instanceof byte[])
		|| !TYPE_NAME.equals(new String((byte[]) result));
	}

	protected int[] getTypeIds() {
		return new int[] { TYPEID };
	}

	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

	public void javaToNative(Object object, TransferData transferData) {
		byte[] check = TYPE_NAME.getBytes();
		super.javaToNative(check, transferData);
	}

	public Object nativeToJava(TransferData transferData) {
		Object result = super.nativeToJava(transferData);
		if (isInvalidNativeType(result)) {
			System.out.println("LocalObjectTransfer error: Invalid Native Type");	        	 
		}
		return object;
	}

	public void setObject(Object o) {
		object = o;
	}

	public long getObjectSetTime() {
		return objectSetTime;
	}

	public void setObjectSetTime(long time) {
		objectSetTime = time;
	}
}


