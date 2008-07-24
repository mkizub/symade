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
package kiev.be.java15;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ViewOf(vcast=true, iface=true)
public final view JFileUnit of FileUnit extends JNameSpace {
	public:ro	String					fname;
	public:ro	boolean[]				disabled_extensions;
	public		boolean					scanned_for_interface_only;

	public String pname();

	public void generate() {
		long curr_time = 0L, diff_time = 0L;
		String cur_file = Kiev.getCurFile();
		Kiev.setCurFile(pname());
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach (JNode n; members) {
				if (n instanceof JNameSpace) {
					n.generate();
				}
				else if (n instanceof JStruct) {
					diff_time = curr_time = System.currentTimeMillis();
					n.generate();
					diff_time = System.currentTimeMillis() - curr_time;
					if( Kiev.verbose )
						Kiev.reportInfo("Generated clas "+n,diff_time);
				}
			}
		} finally { Kiev.setCurFile(cur_file); Kiev.setExtSet(exts); }
	}
}

@ViewOf(vcast=true, iface=true)
public view JNameSpace of NameSpace extends JSNode {
	public		SymbolRef<KievPackage>	srpkg;
	public:ro	JNode[]					members;

	public void generate() {
		long curr_time = 0L, diff_time = 0L;
		foreach (JNode n; members) {
			if (n instanceof JNameSpace) {
				n.generate();
			}
			else if (n instanceof JStruct) {
				diff_time = curr_time = System.currentTimeMillis();
				n.generate();
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose )
					Kiev.reportInfo("Generated clas "+n,diff_time);
			}
		}
	}
}

