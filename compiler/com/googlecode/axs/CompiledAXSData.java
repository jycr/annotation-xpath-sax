package com.googlecode.axs;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;
import javax.xml.namespace.QName;

import com.googlecode.axs.xpath.Node;
import com.googlecode.axs.xpath.Parser;

/**
 * This class performs the actual compilation of XPath expressions to generate
 * the _AXSData needed at run time.
 * @author Ben
 *
 */
public class CompiledAXSData {
	private AnnotatedClass mClass;
	private Messager mMessager;

	public CompiledAXSData(AnnotatedClass ac, Messager messager) {
		mClass = ac;
		mMessager = messager;
		
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
	
	private String compileExpression(Node expressionNode, ShortVector instrVector) {
		
		return "";
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
	private void compileOneExpression(String methodName, String xpathExpression) {
		final Map<String, String> prefixMap = mClass.prefixMap();
		
		// invoke the JJTree/JavaCC parser in com.googlecode.axs.xpath.Parser
		Parser parser = new Parser(new StringReader(xpathExpression));
		Node rootNode;
		
		try {
			// attempt the parse
			rootNode = parser.Start();
			
			// the Start element has as direct children all the alternative expressions
			for (int child = 0, nrChildren = rootNode.jjtGetNumChildren(); child < nrChildren; child++) {
				ShortVector instructions = new ShortVector();
				String trigger = compileExpression(rootNode.jjtGetChild(child), instructions);
				
				// store the compiled method
				mTokens.add(instructions);
				mMethods.add(new Method(methodName, xpathExpression, mTokens.size() - 1));
				addTrigger(trigger, mTokens.size() - 1);
			}
		} catch (Exception e) {
			mMessager.printMessage(Kind.ERROR, "Error parsing XPath expression \"" + xpathExpression + "\": " + e.toString());
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
	private int compileOneSet(Map<String, String> methodSet) {
		int firstMethodIndex = mMethods.size() - 1;
		String[] sortedMethods = methodSet.keySet().toArray(new String[0]);
		Arrays.sort(sortedMethods);
		
		for (String method : sortedMethods) {
			String xpathExpression = methodSet.get(method);
			compileOneExpression(method, xpathExpression);
		}
		
		return (mMethods.size() - 1) - firstMethodIndex;
	}
	
	/**
	 * Compile all the XPath expressions in the AnnotatedClass
	 * @param messager a Message for logging
	 */
	public void compile() {
		mNrXPathMethods = compileOneSet(mClass.xPathMethods());
		mNrXPathEndMethods = compileOneSet(mClass.xPathEndMethods());
		mNrXPathStartMethods = compileOneSet(mClass.xPathStartMethods());
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
