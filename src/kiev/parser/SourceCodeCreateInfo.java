/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.tree.CreateInfo;

/**
 * @author Maxim Kizub
 *
 */

public final class SourceCodeCreateInfo extends CreateInfo {
	public KString srcfile;
	public int pos;
	
	public SourceCodeCreateInfo(KString srcfile, int pos) {
		this.srcfile = srcfile;
		this.pos = pos;
	}
	
	public SourceCodeCreateInfo(KString srcfile, int line, int column) {
		this.srcfile = srcfile;
		this.pos = (line << 11) | (column & 0x3FF);
	}
	
    public final KString getSrcFile() { return srcfile; }
    public final int getPos()         { return pos; }
    public final int getSrcLine()     { return pos >>> 11; }
    public final int getSrcColumn()   { return pos & 0x3FF; }
}

