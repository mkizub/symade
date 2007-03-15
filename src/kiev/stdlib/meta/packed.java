/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.stdlib.meta;

import java.lang.annotation.*;

import syntax kiev.stdlib.Syntax;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface packed {
	public int    size();
	public String in() default "";
	public int    offset() default 0;
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
private @interface packer {
	public int    size() default 0;
}
