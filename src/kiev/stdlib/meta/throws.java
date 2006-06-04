package kiev.stdlib.meta;

import java.lang.annotation.*;

import syntax kiev.stdlib.Syntax;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface throws {
	public Class[] value();
}

