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
package org.apache.tools.ant.lang;

import syntax kiev.Syntax;

@ThisIsANode
public abstract class AntDataType extends DNode {
}

@ThisIsANode
public final class AntDescription extends AntDataType {
	@nodeAttr public String text;
}

@ThisIsANode
public class AntPatternEntry extends AntDataType {
	@nodeAttr public String pattern;
	@nodeAttr public AntProperty⇑ onlyif;
	@nodeAttr public AntProperty⇑ unless;
}

@ThisIsANode
public class AntPatternSet extends AntDataType {
	@nodeAttr public AntPatternEntry∅ include;
	@nodeAttr public AntPatternEntry∅ exclude;
	@nodeAttr public AntPatternEntry∅ includesFile;
	@nodeAttr public AntPatternEntry∅ excludesFile;
}

@ThisIsANode
public class AntDirSet extends AntDataType {
	@nodeAttr public String dir;
	@nodeAttr public AntPatternSet pset;
	@nodeAttr public AntPatternEntry∅ psets;
	@nodeAttr public boolean caseSensitive;
	@nodeAttr public boolean followSymlinks;
}
@ThisIsANode
public class AntFileSet extends AntDataType {
	@nodeAttr public String dir;
	@nodeAttr public AntPatternSet pset;
	@nodeAttr public AntPatternEntry∅ psets;
	@nodeAttr public boolean caseSensitive;
	@nodeAttr public boolean followSymlinks;
}

