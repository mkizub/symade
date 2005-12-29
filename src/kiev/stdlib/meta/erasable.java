package kiev.stdlib.meta;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface erasable {
	public boolean value() default true;
}

