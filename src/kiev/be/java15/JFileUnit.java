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

import java.io.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final view JFileUnit of FileUnit extends JSNode {
	public:ro	String					fname;
	public		TypeNameRef				pkg;
	public:ro	JNode[]					members;
	public:ro	boolean[]				disabled_extensions;
	public		boolean					scanned_for_interface_only;

	@getter public JFileUnit get$jctx_file_unit() { return this; }
	
	public String pname();

	public void generate() {
		long curr_time = 0L, diff_time = 0L;
		String cur_file = Kiev.getCurFile();
		Kiev.setCurFile(pname());
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach (JStruct dn; members) {
				diff_time = curr_time = System.currentTimeMillis();
				dn.generate();
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose )
					Kiev.reportInfo("Generated clas "+dn,diff_time);
			}
		} finally { Kiev.setCurFile(cur_file); Kiev.setExtSet(exts); }
	}

	static void make_output_dir(String top_dir, String filename) throws IOException {
		File dir;
		dir = new File(top_dir,filename);
		dir = new File(dir.getParent());
		dir.mkdirs();
		if( !dir.exists() || !dir.isDirectory() ) throw new RuntimeException("Can't create output dir "+dir);
	}
}

