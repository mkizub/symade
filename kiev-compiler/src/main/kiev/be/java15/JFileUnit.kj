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
 * @version $Revision: 236 $
 *
 */


public class JFileUnit extends JSNode {

	@virtual typedef VT  ≤ FileUnit;

	public static JFileUnit attachJFileUnit(FileUnit impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JFileUnit)jn;
		if (impl.scanned_for_interface_only)
			return new JFileUnit(impl);
		return new JFileUnitToCompile(impl);
	}
	
	public JFileUnit(FileUnit impl) {
		super(impl);
	}

	public void backendCleanup() {}

	public void generate(JEnv jenv) {}
}

public class JFileUnitToCompile extends JFileUnit {
	public final String		pname;
	public final String		fname;
	public final long		timestamp;

	public JStruct[]		structs;

	public JFileUnitToCompile(FileUnit impl) {
		super(impl);
		timestamp = Long.MAX_VALUE;
		if (Kiev.fast_gen)
			timestamp = impl.source_timestamp;
		pname = impl.pname();
		fname = impl.fname;
		Vector<JStruct> structs = new Vector<JStruct>();
		collectStructs(impl, structs);
		this.structs = structs.toArray();
	}
	
	private static void collectStructs(SyntaxScope ss, Vector<JStruct> structs) {
		foreach (ASTNode n; ss.members) {
			if (n instanceof Struct)
				structs.append((JStruct)(Struct)n);
			else if (n instanceof SyntaxScope)
				collectStructs((SyntaxScope)n, structs);
		}
	}
	
	public void generate(JEnv jenv) {
		String cur_file = Kiev.getCurFile();
		Kiev.setCurFile(pname);
        try {
			long curr_time = 0L, diff_time = 0L;
			foreach (JStruct n; structs) {
				diff_time = curr_time = System.currentTimeMillis();
				boolean gen = n.generate(jenv, timestamp);
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose && gen )
					Kiev.reportInfo("Generated clas "+n,diff_time);
			}
		} finally { Kiev.setCurFile(cur_file); }
	}
}

