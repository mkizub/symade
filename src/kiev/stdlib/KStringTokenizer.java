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
  
package kiev.stdlib;

import kiev.stdlib.KString.KStringScanner;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/stdlib/KStringTokenizer.java,v 1.2 1998/10/21 19:44:43 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.2 $
 *
 */

public class KStringTokenizer {
    private KString str;
    private KStringScanner scan;
    private char delimiter;
    private boolean retTokens;

    /**
     * Constructs a string tokenizer for the specified string. The 
     * characters in the <code>delim</code> argument are the delimiters 
     * for separating tokens. 
     * <p>
     * If the <code>returnTokens</code> flag is <code>true</code>, then 
     * the delimiter characters are also returned as tokens. Each 
     * delimiter is returned as a string of length one. If the flag is 
     * <code>false</code>, the delimiter characters are skipped and only 
     * serve as separators between tokens. 
     *
     * @param   str            a string to be parsed.
     * @param   delim          the delimiters.
     * @param   returnTokens   flag indicating whether to return the delimiters
     *                         as tokens.
     */
    public KStringTokenizer(KString str, char delim, boolean returnTokens) {
		this.str = str;
		delimiter = delim;
		retTokens = returnTokens;
		scan = new KStringScanner(str);
    }

    /**
     * Constructs a string tokenizer for the specified string. The 
     * characters in the <code>delim</code> argument are the delimiters 
     * for separating tokens. 
     *
     * @param   str     a string to be parsed.
     * @param   delim   the delimiters.
     * @since   JDK1.0
     */
    public KStringTokenizer(KString str, char delim) {
		this(str, delim, false);
    }

    /**
     * Constructs a string tokenizer for the specified string. The 
     * tokenizer uses the default delimiter set, which is 
     * <code>"&#92;t&#92;n&#92;r"</code>: the space character, the tab
     * character, the newline character, and the carriage-return character. 
     *
     * @param   str   a string to be parsed.
     * @since   JDK1.0
     */
    public KStringTokenizer(KString str) {
		this(str, ' ', false);
    }

    /**
     * Skips delimiters.
     */
    private void skipDelimiters() {
    	while (!retTokens && scan.hasMoreChars() && scan.peekChar() == delimiter )
    		scan.nextChar();
    }

    /**
     * Tests if there are more tokens available from this tokenizer's string.
     *
     * @return  <code>true</code> if there are more tokens available from this
     *          tokenizer's string; <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public boolean hasMoreTokens() {
		skipDelimiters();
		return scan.hasMoreChars();
    }

    /**
     * Returns the next token from this string tokenizer.
     *
     * @return     the next token from this string tokenizer.
     * @exception  NoSuchElementException  if there are no more tokens in this
     *               tokenizer's string.
     * @since      JDK1.0
     */
    public KString nextToken() {
		skipDelimiters();
	
		if (!scan.hasMoreChars()) {
		    throw new java.util.NoSuchElementException();
		}
	
		int start = str.offset + scan.pos;
		while (scan.hasMoreChars() && scan.peekChar() != delimiter) {
			scan.nextChar();
		}
		if (retTokens && (start == str.offset + scan.pos) && scan.peekChar() == delimiter ) {
			scan.nextChar();
		}
		return KString.from(KString.buffer, start, scan.pos + str.offset);
    }

    /**
     * Returns the next token in this string tokenizer's string. The new 
     * delimiter set remains the default after this call. 
     *
     * @param      delim   the new delimiters.
     * @return     the next token, after switching to the new delimiter set.
     * @exception  NoSuchElementException  if there are no more tokens in this
     *               tokenizer's string.
     * @since   JDK1.0
     */
    public KString nextToken(char delim) {
		delimiter = delim;
		return nextToken();
    }

    /**
     * Returns the same value as the <code>hasMoreTokens</code>
     * method. It exists so that this class can implement the
     * <code>Enumeration</code> interface. 
     *
     * @return  <code>true</code> if there are more tokens;
     *          <code>false</code> otherwise.
     * @see     java.util.Enumeration
     * @see     java.util.StringTokenizer#hasMoreTokens()
     * @since   JDK1.0
     */
    public boolean hasMoreElements() {
		return hasMoreTokens();
    }

    /**
     * Calculates the number of times that this tokenizer's 
     * <code>nextToken</code> method can be called before it generates an 
     * exception. 
     *
     * @return  the number of tokens remaining in the string using the current
     *          delimiter set.
     * @see     java.util.StringTokenizer#nextToken()
     * @since   JDK1.0
     */
    public int countTokens() {
		int count = 0;
		int currpos = scan.pos;
		while( hasMoreTokens() ) { nextToken(); count++; }
		scan.pos = currpos;
		return count;
    }
}
