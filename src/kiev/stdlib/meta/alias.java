package kiev.stdlib.meta;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface alias {
	public String[] value();
}

