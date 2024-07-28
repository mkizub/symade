package kiev.dump.bin;

public enum Signature {
	TAG_NULL					('@', true),			// NULL value
	TAG_VOID					('v', true),				// VOID - unspecified value, omit the field
	TAG_INT8						('b', true),				// Integer (byte)
	TAG_INT16					('h', true),				// Integer (short, half)
	TAG_INT32					('i', true),				//	 Integer (int)
	TAG_INT64					('l', true),				// Integer (long)
	TAG_FALSE					('f', true),				// Integer value 0, FALSE
	TAG_TRUE					('t', true),				// Integer value 1, TRUE
	TAG_FLOAT					('e', true),				// Exponential (float)
	TAG_SYMB_REF				('y', false),			// reference to symbol (as opposite to symbol of this node)
	TAG_NODE_REF			('n', false),			// reference to node (as opposite to child node)
	// char/string/octets
	TAG_CHAR_8				('c', true),				// Character, UTF-8
	TAG_STRZ_8					('s', true),				// String, UTF-8, ASCIIZ (no control chars), "!s" (TAG_LONG+TAG_STRZ_8) uses length prefix and control chars
	TAG_STRZ_16				('u', true),				// String, UTF-16 (no control chars), "!u" (TAG_LONG+TAG_STRZ_16) uses length prefix and control chars
	TAG_OCTET					('o', true),				// Octet stream, 4 bytes for length (in bytes)
	// tags & refs
	TAG_START					('<', false),			// start tag, followed by signature
	TAG_END						('>', false),			// end tag, followed by signature
	TAG_SPACE_START		('(', false),			// space values start tag
	TAG_SPACE_END			(')', false),			// space values end tag
	TAG_EXT_START			('[', false),			// extended attributes start tag
	TAG_EXT_END				(']', false),			// extended attributes end tag
	TAG_FLAG					('#', false),			// Flag signature
	TAG_ID							(':', false),			// ID tag
	TAG_LONG					('!', false),			// long prefix (for ID and refs)
	TAG_LINENO					(';', false),			// long prefix (for ID and refs)
	// signatures
	TAG_TABLE_SIGN			('^', false),			// table entry (id <-> offset) start/end tag
	TAG_ATTR_SIGN			('A', false),			// Attribute block start/end & ref signature
	TAG_TYPE_SIGN			('T', false),			// Type block start/end & ref signature
	TAG_SYMB_SIGN			('Y', false),			// sYmbol block start/end & ref signature
	TAG_NODE_SIGN			('N', false),			// Node block start/end & ref signature
	TAG_ROOT_SIGN			('R', false),			// Root node ref signature
	TAG_CONST_SIGN		('C', false),			// Constant block start/end & ref signature
	TAG_COMMENT_SIGN	('*', false)				// Comment block start/end signature
	;

	public static byte[] SIGNATURE_DOC_START = {'<','b','t','r','e','e','>',0};
	public static byte[] SIGNATURE_DOC_END = {'<','/','b','t','r','e','e','>'};
	
	public final char sign;
	public final boolean is_value;
	
	private Signature(char ch, boolean is_value) {
		this.sign = ch;
		this.is_value = is_value;
	}
	
	public static Signature from(char ch) {
		switch (ch) {
		case '@':		return TAG_NULL;
		case 'v':		return TAG_VOID;
		case 'b':		return TAG_INT8;
		case 'h':		return TAG_INT16;
		case 'i':		return TAG_INT32;
		case 'l':		return TAG_INT64;
		case 'f':		return TAG_FALSE;
		case 't':		return TAG_TRUE;
		case 'e':		return TAG_FLOAT;
		case 'n':		return TAG_NODE_REF;
		case 'y':		return TAG_SYMB_REF;
		case 'c':		return TAG_CHAR_8;
		case 's':		return TAG_STRZ_8;
		case 'u':		return TAG_STRZ_16;
		case 'o':		return TAG_OCTET;
		case '<':		return TAG_START;
		case '>':		return TAG_END;
		case '(':		return TAG_SPACE_START;
		case ')':		return TAG_SPACE_END;
		case '[':		return TAG_EXT_START;
		case ']':		return TAG_EXT_END;
		case '#':		return TAG_FLAG;
		case ':':		return TAG_ID;
		case '!':		return TAG_LONG;
		case ';':		return TAG_LINENO;
		case '^':		return TAG_TABLE_SIGN;
		case 'A':		return TAG_ATTR_SIGN;
		case 'T':		return TAG_TYPE_SIGN;
		case 'Y':		return TAG_SYMB_SIGN;
		case 'N':		return TAG_NODE_SIGN;
		case 'R':		return TAG_ROOT_SIGN;
		case 'C':		return TAG_CONST_SIGN;
		case '*':		return TAG_COMMENT_SIGN;
		}
		return null;
	}
}
