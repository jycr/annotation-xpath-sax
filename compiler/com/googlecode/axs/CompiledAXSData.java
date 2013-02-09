package com.googlecode.axs;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;
import javax.xml.namespace.QName;

import com.googlecode.axs.xpath.Node;
import com.googlecode.axs.xpath.Parser;
import com.googlecode.axs.xpath.ParserTreeConstants;
import com.googlecode.axs.xpath.XPathNode;

/**
 * This class performs the actual compilation of XPath expressions to generate
 * the _AXSData needed at run time.
 * @author Ben
 *
 */
public class CompiledAXSData {
	private AnnotatedClass mClass;

	public CompiledAXSData(AnnotatedClass ac) {
		mClass = ac;
		
		mTokens = new Vector<ShortVector>();
		mLiterals = new Vector<String>();
		mQNames = new Vector<QName>();
		mLiteralIndices = new HashMap<String, Integer>();
		mQNameIndices = new HashMap<QName, Integer>();
	}

	private int mMaxPredicateStackDepth = 0;
	private Vector<ShortVector> mTokens = null;
	private Vector<String> mLiterals = null;
	private Vector<QName> mQNames = null;
	private short mCurrentTag = -1, mPriorTag = -1;
	private HashMap<String, Integer> mLiteralIndices = null;
	private HashMap<QName, Integer> mQNameIndices = null;
	private HashMap<String, Vector<Integer>> mTriggerTags = new HashMap<String, Vector<Integer>>();
	private HashSet<String> mAttributeCaptureTags = new HashSet<String>();
	private HashSet<String> mPositionCaptureTags = new HashSet<String>();

	public class Method {
		private String mName;
		private String mXPathExpression;
		private int mIndex;
		
		public Method(String name, String expression, int index) {
			mName = name;
			mXPathExpression = expression;
			mIndex = index;
		}
		
		public String name() {
			return mName;
		}
		
		public String expression() {
			return mXPathExpression;
		}
		
		public int index() {
			return mIndex;
		}
	}
	
	private Vector<Method> mMethods = new Vector<Method>();
	private int mNrXPathMethods = 0;
	private int mNrXPathEndMethods = 0;
	private int mNrXPathStartMethods = 0;

	/**
	 * Add @p literal to the literals table and return its index
	 * @param literal
	 * @return the index of the literal
	 */
	private int addLiteral(String literal) {
		Integer index = mLiteralIndices.get(literal);
		
		if (index != null)
			return index;
		
		mLiterals.add(literal);
		mLiteralIndices.put(literal, mLiterals.size() - 1);
		return mLiterals.size() - 1;
	}
	
	/**
	 * Add @p qName to the QNames table and return its index
	 * @param qName
	 * @return the index of the QName
	 */
	private int addQName(QName qName) {
		Integer index = mQNameIndices.get(qName);
		
		if (index != null)
			return index;
		
		mQNames.add(qName);
		mQNameIndices.put(qName, mQNames.size() - 1);
		return mQNames.size() - 1;
	}
	
	/**
	 * Compile a Name node down to a QName index
	 * @param node the node to compile
	 * @return the QName index of the Name
	 */
	private short compileName(XPathNode node) {
		String prefixedName = (String) node.jjtGetValue();
		
		QName qName = null;
		return (short)addQName(qName);
	}
	
	/**
	 * Compile a String node down to a Literal table index
	 * @param node the node to compile
	 * @return the Literal index of the string
	 */
	private short compileStringLiteral(XPathNode node) {
		String text = (String) node.jjtGetValue();

		String literal = null; 
		
		return (short)addLiteral(literal);
	}
	
	/**
	 * Compile a ComparisonExpression node.
	 * @param node
	 * @param tokens
	 */
	private void compileComparison(XPathNode node, ShortVector tokens) {
		
	}
	
	/**
	 * Compile a FunctionExpression node.
	 * @param node
	 * @param tokens
	 */
	private void compileFunction(XPathNode node, ShortVector tokens) {
		String fnName = (String) node.jjtGetValue();
		
		if (fnName == "position") {
			tokens.push(XPathExpression.INSTR_POSITION);
			// FIXME: capture position info
		} else if (fnName == "captureattrs") {
			// FIXME: capture attributes
		} else if (fnName == "not") {
			tokens.push(XPathExpression.INSTR_NOT);
		} else {
			throw new XPathExecutionError("unknown function \"" + fnName + "\"");
		}
	}

	/**
	 * Compile a single XPathNode.
	 * @param node the node to compile
	 * @param tokens the token array to compile into
	 */
	private void compileNode(XPathNode node, ShortVector tokens) {
		switch (node.getNodeType()) {
		case ParserTreeConstants.JJTABSOLUTEPATHEXPRESSION:
			tokens.push(XPathExpression.INSTR_ROOT);
			break;
		case ParserTreeConstants.JJTDECENDENTEXPRESSION:
			tokens.push(XPathExpression.INSTR_DOUBLE_SLASH);
			tokens.push(mPriorTag);
			break;
		case ParserTreeConstants.JJTEXPRESSION:
			// Expressions get broken into alternations but have no compiled form
			break;
		case ParserTreeConstants.JJTCHILDEXPRESSION:
		case ParserTreeConstants.JJTATTRIBUTEEXPRESSION:
			// Name elements look _up_ the tree to see how they should be compiled
			break;
		case ParserTreeConstants.JJTNAME:
			int parentType = node.jjtGetParent().getNodeType();
			switch (parentType) {
			case ParserTreeConstants.JJTABSOLUTEPATHEXPRESSION:
			case ParserTreeConstants.JJTCHILDEXPRESSION:
			case ParserTreeConstants.JJTEXPRESSION:
			case ParserTreeConstants.JJTDECENDENTEXPRESSION:
				tokens.push(XPathExpression.INSTR_ELEMENT);
				mPriorTag = mCurrentTag;
				mCurrentTag = compileName(node); // remember the tag we're processing
				tokens.push(mCurrentTag);
				break;
			case ParserTreeConstants.JJTATTRIBUTEEXPRESSION:
				tokens.push(XPathExpression.INSTR_ATTRIBUTE);
				tokens.push(compileName(node));
				break;
			default:
				throw new XPathExecutionError("unexpected parent for Name node");
			}
		case ParserTreeConstants.JJTANDEXPRESSION:
			tokens.push(XPathExpression.INSTR_AND);
			break;
		case ParserTreeConstants.JJTCOMPARISONEXPRESSION:
			compileComparison(node, tokens);
			break;
		case ParserTreeConstants.JJTFUNCTIONEXPRESSION:
			compileFunction(node, tokens);
			break;
		case ParserTreeConstants.JJTINTEGER:
			tokens.push(XPathExpression.INSTR_ILITERAL);
			tokens.push((short) Integer.parseInt((String) node.jjtGetValue()));
			break;
		case ParserTreeConstants.JJTOREXPRESSION:
			tokens.push(XPathExpression.INSTR_OR);
			break;
		case ParserTreeConstants.JJTPREDICATE:
			tokens.push(XPathExpression.INSTR_TEST_PREDICATE);
			break;
		case ParserTreeConstants.JJTSTRING:
			tokens.push(XPathExpression.INSTR_LITERAL);
			tokens.push(compileStringLiteral(node));
			break;
		case ParserTreeConstants.JJTWILDCARD:
			if (node.jjtGetParent().getNodeType() == ParserTreeConstants.JJTCHILDEXPRESSION) {
				// wildcard nodes only are meaningful in the context of a ChildExpression "a/*/b"
				tokens.push(XPathExpression.INSTR_WILDCARD_ELEMENT);
			}
			break;
		}
	}
	
	/**
	 * Compile one Expression from the parser AST
	 * @param node the SimpleNode of the Expression
	 * @param tokens the token array to compile into
	 */
	private String compileExpression(Messager messager, XPathNode node, ShortVector tokens) {
		Stack<Node> nodes = new Stack<Node>();
		
		while (true) {
			
		}
	}
	
	/**
	 * Compile a single XPath expression.
	 * The token list is directly add()ed to mTokens and the literals and QNames
	 * to the global pools in mLiterals and mQNames. The resulting one or more
	 * Methods are added to mMethods and the appropriate triggers are addTrigger()ed.
	 * @param messager
	 * @param methodName the name of the annotated method
	 * @param xpathExpression the XPath annotation of the annotated method
	 */
	private void compileOneExpression(Messager messager, String methodName, String xpathExpression) {
		final Map<String, String> prefixMap = mClass.prefixMap();
		
		// invoke the JJTree/JavaCC parser in com.googlecode.axs.xpath.Parser
		Parser parser = new Parser(new StringReader(xpathExpression));
		XPathNode rootNode;
		
		try {
			// attempt the parse
			rootNode = parser.Start();
			
			// the Start element has as direct children all the alternative expressions
			for (int child = 0, nrChildren = rootNode.jjtGetNumChildren(); child < nrChildren; child++) {
				ShortVector tokens = new ShortVector();
				String trigger = compileExpression(messager, rootNode.jjtGetChild(child), tokens);
				
				// store the compiled method
				mTokens.add(tokens);
				mMethods.add(new Method(methodName, xpathExpression, mTokens.size() - 1));
				addTrigger(trigger, mTokens.size() - 1);
			}
		} catch (Exception e) {
			messager.printMessage(Kind.ERROR, "Error parsing XPath expression \"" + xpathExpression + "\": " + e.toString());
		}
	}
	
	/**
	 * Add @p xprIx to the list of triggers for @p tag
	 * @param tag
	 * @param xprIx
	 */
	private void addTrigger(String tag, int xprIx) {
		Vector<Integer> triggers = mTriggerTags.get(tag);
		
		if (triggers == null) {
			triggers = new Vector<Integer>();
			mTriggerTags.put(tag, triggers);
		}
		triggers.add(xprIx);
	}
	
	/**
	 * Compile the methods in one class of annotation and return the number of Methods generated.
	 * @param messager
	 * @param methodSet
	 * @return
	 */
	private int compileOneSet(Messager messager, Map<String, String> methodSet) {
		int firstMethodIndex = mMethods.size() - 1;
		String[] sortedMethods = methodSet.keySet().toArray(new String[0]);
		Arrays.sort(sortedMethods);
		
		for (String method : sortedMethods) {
			String xpathExpression = methodSet.get(method);
			compileOneExpression(messager, method, xpathExpression);
		}
		
		return (mMethods.size() - 1) - firstMethodIndex;
	}
	
	/**
	 * Compile all the XPath expressions in the AnnotatedClass
	 * @param messager a Message for logging
	 */
	public void compile(Messager messager) {
		mNrXPathMethods = compileOneSet(messager, mClass.xPathMethods());
		mNrXPathEndMethods = compileOneSet(messager, mClass.xPathEndMethods());
		mNrXPathStartMethods = compileOneSet(messager, mClass.xPathStartMethods());
	}
	
	public Vector<ShortVector> tokens() {
		return mTokens;
	}
	
	public Vector<String> literals() {
		return mLiterals;
	}
	
	public Vector<QName> qNames() {
		return mQNames;
	}
	
	public Vector<Method> methods() {
		return mMethods;
	}
	
	public int numberOfXPathMethods() {
		return mNrXPathMethods;
	}
	
	public int numberOfXPathEndMethods() {
		return mNrXPathEndMethods;
	}
	
	public int numberOfXPathStartMethods() {
		return mNrXPathStartMethods;
	}
	
	public int maximumPredicateStackDepth() {
		return mMaxPredicateStackDepth;
	}
	
	public Set<String> attributeCaptureTags() {
		return mAttributeCaptureTags;
	}
	
	public Set<String> positionCaptureTags() {
		return mPositionCaptureTags;
	}
	
	public Map<String, Vector<Integer>> triggers() {
		return mTriggerTags;
	}
	
	public String packageName() {
		String className = mClass.className().toString();
		int lastDot = className.lastIndexOf('.');
		String packageName = className.substring(0, lastDot);
		
		return packageName;
	}
	
	public String className() {
		return mClass.classElement().getSimpleName().toString();
	}
}
