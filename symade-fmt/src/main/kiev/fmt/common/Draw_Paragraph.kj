package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

import kiev.fmt.ParagraphKind;

public class Draw_Paragraph implements Serializable {
	private static final long serialVersionUID = 4457405626413042874L;

	public String name;
	public ParagraphKind flow;
	public Draw_ParOption[] options;
	
	Object readResolve() throws ObjectStreamException {
		if (this.name != null)
			this.name = this.name.intern();
		return this;
	}
	
	public <O extends Draw_ParOption> O getOption(Class<O> clz) {
		if (options == null)
			return null;
		for (Draw_ParOption opt : options)
			if (opt.getClass() == clz)
				return (O)opt;
		return null;
	}
	
	public Draw_ParIndent getIndent() {
		return getOption(Draw_ParIndent.class);
	}
	public Draw_ParNoIndentIfPrev getNoIndentIfPrev() {
		return getOption(Draw_ParNoIndentIfPrev.class);
	}
	public Draw_ParNoIndentIfNext getNoIndentIfNext() {
		return getOption(Draw_ParNoIndentIfNext.class);
	}
	public Draw_ParSize getSize() {
		return getOption(Draw_ParSize.class);
	}
	public Draw_ParLines getLines() {
		return getOption(Draw_ParLines.class);
	}
	public Draw_ParInset getInsets() {
		return getOption(Draw_ParInset.class);
	}
	public Draw_ParAlignBlock getAlignBlock() {
		return getOption(Draw_ParAlignBlock.class);
	}
	public Draw_ParAlignContent getAlignContent() {
		return getOption(Draw_ParAlignContent.class);
	}
}
