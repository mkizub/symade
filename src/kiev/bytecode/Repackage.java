/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev library.

 The Kiev library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public License as
 published by the Free Software Foundation.

 The Kiev library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU Library General Public
 License along with the Kiev compiler; see the file License.  If not,
 write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.bytecode;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/bytecode/Repackage.java,v 1.2 1998/10/21 19:44:18 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.2 $
 *
 */

public class Repackage implements BytecodeFileConstants,BytecodeHandler,BytecodeAttributeNames {
	import kiev.stdlib.Debug;

	class RepackageEntry {
		String		from_package;
		String		to_package;
		RepackageEntry(String from, String to) {
			from_package = from;
			to_package = to;
		}
	}

	public Vector<RepackageEntry>	entries = new Vector<RepackageEntry>();

	public void addRepackageRule(String from, String to) {
		from = from.replace('.','/');
		if( !from.endsWith("/") ) from = from+"/";
		from = from.intern();
		to = to.replace('.','/');
		if( !to.endsWith("/") ) to = to+"/";
		to = to.intern();
		if( from.startsWith("java/") )
			throw new RuntimeException("Request to repackage core package: "+from+"->"+to);
		// Ensure 'from' and 'to' are not duplicated
		foreach(RepackageEntry rpe; entries) {
			if( rpe.from_package == from || rpe.from_package == to
			 || rpe.to_package == from	 || rpe.to_package == to
			) {
				if( rpe.from_package == from && rpe.to_package == to ) return;
				throw new RuntimeException("Requested repackage "+from+"->"+to+" conflicts with "
					+rpe.from_package+"->"+rpe.to_package);
			}
		}
		trace(Clazz.traceRules,"Add repackage rule: "+from+"->"+to);
		entries.append(new RepackageEntry(from,to));
	}

	public int		getPriority() { return PreprocessStage-1; }

	public void processClazz(Clazz clazz) {
		PoolConstant[] pool = clazz.pool;
		int len = pool.length;
		for(int i=0; i < len; i++) {
			if( pool[i].constant_type == CONSTANT_CLASS ) {
				fixClazzPoolConstant(clazz,i);
			}
			else if( pool[i].constant_type == CONSTANT_NAMEANDTYPE ) {
				assert( pool[i] instanceof NameAndTypePoolConstant );
				int pc = ((NameAndTypePoolConstant)pool[i]).ref_type;
				fixSignaturePoolConstant(clazz,pc);
			}
		}
		foreach(Field f; clazz.fields)
			fixSignaturePoolConstant(clazz,f.cp_type);
		foreach(Method m; clazz.methods)
			fixSignaturePoolConstant(clazz,m.cp_type);
		foreach(Attribute a; clazz.attrs) {
			if( a.getName(clazz) == attrCode ) {
				foreach(int cp_sig; ((CodeAttribute)a).catchers_cp_signature; cp_sig > 0)
					fixSignaturePoolConstant(clazz,cp_sig);
				foreach(Attribute ca; ((CodeAttribute)a).attrs; ca.getName(clazz) == attrLocalVarTable ) {
					foreach(int cp_sig; ((LocalVariableTableAttribute)ca).cp_signature; cp_sig > 0)
						fixSignaturePoolConstant(clazz,cp_sig);
				}
			}
			else if( a.getName(clazz) == attrExceptions ) {
				foreach(int cp_sig; ((ExceptionsAttribute)a).cp_exceptions; cp_sig > 0)
					fixSignaturePoolConstant(clazz,cp_sig);
			}
//			else if( a.getName() == attrInnerClasses ) {
//				// cp_inners[] & cp_outers[] are CONSTANT_CLASS, processed before
//			}
		}
	}

	public void fixClazzPoolConstant(Clazz clazz, int i) {
		PoolConstant[] pool = clazz.pool;
		assert( pool[i] instanceof ClazzPoolConstant );
		Utf8PoolConstant pc = (Utf8PoolConstant)pool[((ClazzPoolConstant)pool[i]).ref];
		String val1 = pc.value.toString();
		int elen = entries.length();
		for(int j=0; j < elen; j++) {
			String val2 = entries[j].from_package;
			if( val1.length() >= val2.length() && val1.startsWith(val2) ) {
				KString newvalue = KString.from( entries[j].to_package.concat(val1.substring(val2.length())));
				trace(Clazz.traceRules,"Repackage: CONSTANT_CLASS "+i+": "+pc.value+"->"+newvalue+" by rule "+val2+"->"+entries[j].to_package);
				pc.value = newvalue;
				break;
//			} else {
//				trace(Clazz.traceRules,"Repackage: CONSTANT_CLASS "+i+": rule "+val2+"->"+entries[j].to_package+" does not match to "+val1);
			}
		}
	}

	public void fixSignaturePoolConstant(Clazz clazz, int i) {
		PoolConstant[] pool = clazz.pool;
		assert( pool[i] instanceof Utf8PoolConstant );
		Utf8PoolConstant pc = (Utf8PoolConstant)pool[i];
		String val1 = pc.value.toString();
		int elen = entries.length();
		for(int j=0; j < elen; j++) {
			String val2 = entries[j].from_package;
			int index;
			while( (index=val1.indexOf(val2))>0 && (val1.charAt(index-1)=='L' || val1.charAt(index-1)=='A') ) {
				val1 = val1.substring(0,index)+entries[j].to_package+val1.substring(index+val2.length());
			}
		}
		trace(Clazz.traceRules && !val1.equals(pc.value.toString()),"Repackage: CONSTANT_NAMEANDTYPE "+i+": "+pc.value+"->"+val1);
		pc.value = KString.from(val1);
	}
}


