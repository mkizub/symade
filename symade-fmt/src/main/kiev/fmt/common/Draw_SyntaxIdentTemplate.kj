package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Draw_SyntaxIdentTemplate implements Serializable {
	private static final long serialVersionUID = 917619957681340035L;

	transient Pattern	pattern;

	public String 							regexp_ok;
	public String 							esc_prefix;
	public String 							esc_suffix;
	public String[]							keywords;

	Pattern getPattern() {
		if (pattern != null)
			return pattern;
		if (regexp_ok == null)
			regexp_ok = ".*";
		try {
			pattern = Pattern.compile(regexp_ok);
		} catch (PatternSyntaxException e) {
			System.out.println("Syntax error in ident template pattern: "+regexp_ok);
			try { pattern = Pattern.compile(".*"); } catch (PatternSyntaxException pe) {}
		}
		return pattern;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.esc_prefix != null) this.esc_prefix = this.esc_prefix.intern();
		if (this.esc_suffix != null) this.esc_suffix = this.esc_suffix.intern();
		if (this.keywords != null) {
			for (int i=0; i < this.keywords.length; i++)
				this.keywords[i] = this.keywords[i].intern();
		}
		return this;
	}
}

