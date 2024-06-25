package kiev.fmt.common;

import java.io.Serializable;

import kiev.fmt.SyntaxSizeUnit;

public class Draw_Size implements Serializable {
	private static final long serialVersionUID = -3873408858389668155L;

	public SyntaxSizeUnit unit;
	public int gfx_size;
	public int txt_size;
}

