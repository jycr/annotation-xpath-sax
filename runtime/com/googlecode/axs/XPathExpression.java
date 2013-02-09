package com.googlecode.axs;

import javax.xml.namespace.QName;

/**
 * An XPathExpression is a precompiled form of an XPath expression. Its format
 * is subject to change in future versions of this library.
 * @author Ben
 *
 */
public final class XPathExpression {
	// The list of valid Token[] values
	public static final short INSTR_ROOT = 1;
	public static final short INSTR_ELEMENT = 2; // the following value in Tokens[] is the index into QNames[]
	public static final short INSTR_CONTAINS = 3;
	public static final short INSTR_ATTRIBUTE = 4; // the following value in Tokens[] is the index into QNames[]
	public static final short INSTR_LITERAL = 5; // the following value in Tokens[] is the index into Literals[]
	public static final short INSTR_EQ_STR = 6;
	public static final short INSTR_NOT = 7;
	public static final short INSTR_ILITERAL = 8; // the following value in Tokens[] is the literal value 
	public static final short INSTR_AND = 9;
	public static final short INSTR_OR = 10;
	public static final short INSTR_TEST_PREDICATE = 11;
	public static final short INSTR_WILDCARD_ELEMENT = 12;
	public static final short INSTR_STARTS_WITH = 13;
	public static final short INSTR_NONCONSECUTIVE_ELEMENT = 14; // the following value in Tokens[] is the index into QNames
	public static final short INSTR_POSITION = 15; // the following value in Tokens[] is the index into QNames
	public static final short INSTR_LT = 16;
	public static final short INSTR_GT = 17;
	public static final short INSTR_EQ = 18;
	public static final short INSTR_NE = 19;
	public static final short INSTR_LE = 20;
	public static final short INSTR_GE = 21;
	public static final short INSTR_ENDS_WITH = 22;

	private short[] mTokens = null;
	private QName[] mQNames = null;
	private String[] mLiterals = null;
	
	public XPathExpression(short[] tokens, QName[] qNames, String[] literals) {
		mTokens = tokens;
		mQNames = qNames;
		mLiterals = literals;
	}
	
	public short[] tokens() {
		return mTokens;
	}
	
	public QName[] qNames() {
		return mQNames;
	}
	
	public String[] literals() {
		return mLiterals;
	}
}
