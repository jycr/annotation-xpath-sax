/*
 * This file is part of AXS, Annotation-XPath for SAX.
 * 
 * Copyright (c) 2013 Benjamin K. Stuhl
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.googlecode.axs;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import com.googlecode.axs.xpath.AndExpression;
import com.googlecode.axs.xpath.AttributeExpression;
import com.googlecode.axs.xpath.CaptureAttrsFunction;
import com.googlecode.axs.xpath.Expression;
import com.googlecode.axs.xpath.IntegerValue;
import com.googlecode.axs.xpath.NameValue;
import com.googlecode.axs.xpath.Node;
import com.googlecode.axs.xpath.NotExpression;
import com.googlecode.axs.xpath.NumericComparisonExpression;
import com.googlecode.axs.xpath.OrExpression;
import com.googlecode.axs.xpath.ParseException;
import com.googlecode.axs.xpath.Parser;
import com.googlecode.axs.xpath.ParserVisitor;
import com.googlecode.axs.xpath.PositionFunction;
import com.googlecode.axs.xpath.Predicate;
import com.googlecode.axs.xpath.SimpleNode;
import com.googlecode.axs.xpath.Slash;
import com.googlecode.axs.xpath.SlashSlash;
import com.googlecode.axs.xpath.Start;
import com.googlecode.axs.xpath.StepExpression;
import com.googlecode.axs.xpath.StringComparisonExpression;
import com.googlecode.axs.xpath.StringSearchFunction;
import com.googlecode.axs.xpath.StringValue;

/**
 * This class performs the actual compilation of XPath expressions to generate
 * the _AXSData needed at run time.
 * @author Ben
 *
 */
public class CompiledAXSData implements ParserVisitor {
	private AnnotatedClass mClass;
	private Messager mMessager;

	public CompiledAXSData(AnnotatedClass ac, Messager messager) {
		mClass = ac;
		mMessager = messager;
		
		mInstructions = new Vector<ShortVector>();
		mLiterals = new Vector<String>();
		mQNames = new Vector<QName>();
		mLiteralIndices = new HashMap<String, Integer>();
		mQNameIndices = new HashMap<QName, Integer>();
	}

	private int mMaxPredicateStackDepth = 0, mCurrentPredicateStackDepth = 0;
	private Vector<ShortVector> mInstructions = null;
	private Vector<String> mLiterals = null;
	private Vector<QName> mQNames = null;
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
	private Element mCurrentMethodElement = null;

	private void errorMessage(String message) {
		mMessager.printMessage(Kind.ERROR, message, mCurrentMethodElement);
	}
	
	/**
	 * Add @p literal to the literals table and return its index
	 * @param literal
	 * @return the index of the literal
	 */
	private short addLiteral(String literal) {
		Integer index = mLiteralIndices.get(literal);
		
		if (index != null)
			return index.shortValue();
		
		mLiterals.add(literal);
		mLiteralIndices.put(literal, mLiterals.size() - 1);
		return (short)(mLiterals.size() - 1);
	}
	
	private QName parseQName(String name, boolean isAttribute) {
		int colonPosition = name.indexOf(':');
		
		if (colonPosition > 0) {
			String prefix = name.substring(0, colonPosition);
			String localName = name.substring(colonPosition + 1);
			
			String uri = mClass.prefixMap().get(prefix);
			
			if (uri == null) {
				errorMessage("No Namespace URI mapping specified for prefix \"" + prefix + "\"");
			}
			return new QName(uri, localName);
		}
		
		return new QName(isAttribute ? null : mClass.prefixMap().get(""), name);
	}
	
	/**
	 * Add @p qName to the QNames table and return its index
	 * @param qName
	 * @return the index of the QName
	 */
	private short addQName(QName qName) {
		Integer index = mQNameIndices.get(qName);
		
		if (index != null)
			return index.shortValue();
		
		mQNames.add(qName);
		mQNameIndices.put(qName, mQNames.size() - 1);
		return (short)(mQNames.size() - 1);
	}
	
	/**
	 * Add @p n to the stack size required for this expression.
	 * @param n
	 */
	private void requireStackDepth(int n) {
		mCurrentPredicateStackDepth += n;
	}
	
	/**
	 * Start the process of tracking stack sizes needed for this predicate.
	 */
	private void startTrackingStackDepth() {
		mCurrentPredicateStackDepth = 0;
	}
	
	private void finishTrackingStackDepth() {
		if (mCurrentPredicateStackDepth > mMaxPredicateStackDepth)
			mMaxPredicateStackDepth = mCurrentPredicateStackDepth;
	}
	
	@Override
	public Object visit(SimpleNode node, ShortVector data) {
		errorMessage("Got an unhandled #" + node.getClass().getSimpleName() + " node in the parse tree!");
		return null;
	}

	@Override
	public Object visit(Start node, ShortVector data) {
		errorMessage("Got an unhandled #" + node.getClass().getSimpleName() + " node in the parse tree!");
		return null;
	}

	@Override
	public Object visit(Slash node, ShortVector data) {
		errorMessage("Got an unhandled #" + node.getClass().getSimpleName() + " node in the parse tree!");
		return null;
	}

	@Override
	public Object visit(SlashSlash node, ShortVector data) {
		errorMessage("Got an unhandled #" + node.getClass().getSimpleName() + " node in the parse tree!");
		return null;
	}

	// Predicate nodes and all the expression types contained under them
	// return a mask of capture type flags as an Integer; StepExpression
	// nodes return the union of all the captures their Predicates require
	
	private static final int CAPTURE_NONE = 0;
	private static final int CAPTURE_ATTRIBUTES = 0x0001;
	private static final int CAPTURE_POSITIONS = 0x0002;

	/**
	 * Compile all the Predicates required by this node. Does _not_
	 * compile the axis step itself.
	 */
	@Override
	public Object visit(StepExpression node, ShortVector instrs) {
		errorMessage("Got an unhandled #" + node.getClass().getSimpleName() + " node in the parse tree!");
		return null;
	}
	
	@Override
	public Object visit(Predicate node, ShortVector instrs) {
		int captures = CAPTURE_NONE;
		
		startTrackingStackDepth();
		
		if (node.jjtGetNumChildren() == 1 && node.jjtGetChild(0) instanceof IntegerValue) {
			// it's an implicit position() predicate, e.g. bar/foo[3]
			requireStackDepth(1);
			instrs.push(XPathExpression.INSTR_POSITION);
			
			node.jjtGetChild(0).jjtAccept(this, instrs);
			
			instrs.push(XPathExpression.INSTR_EQ);
			captures |= CAPTURE_POSITIONS;
		} else {
			for (int i = 0, children = node.jjtGetNumChildren(); i < children; i++) {
				captures |= (Integer) node.jjtGetChild(i).jjtAccept(this, instrs);
			}
		}
		
		finishTrackingStackDepth();

		instrs.push(XPathExpression.INSTR_TEST_PREDICATE);
		return captures;
	}

	@Override
	public Object visit(OrExpression node, ShortVector instrs) {
		int children = node.jjtGetNumChildren();
		int captures = CAPTURE_NONE;
		
		for (int i = 0; i < children; i++) {
			captures |= (Integer) node.jjtGetChild(i).jjtAccept(this, instrs);
		}
		
		for (int i = 1; i < children; i++) {
			instrs.push(XPathExpression.INSTR_OR);
		}
		return captures;
	}

	@Override
	public Object visit(AndExpression node, ShortVector instrs) {
		int children = node.jjtGetNumChildren();
		int captures = CAPTURE_NONE;
		
		for (int i = 0; i < children; i++) {
			captures |= (Integer) node.jjtGetChild(i).jjtAccept(this, instrs);
		}
		
		for (int i = 1; i < children; i++) {
			instrs.push(XPathExpression.INSTR_AND);
		}
		return captures;
	}

	@Override
	public Object visit(NotExpression node, ShortVector instrs) {
		if (node.jjtGetNumChildren() != 1)
			errorMessage("Unexpectedly found " + node.jjtGetNumChildren() + " descendants of a not() expression");
		int captures = (Integer) node.jjtGetChild(0).jjtAccept(this, instrs);
		instrs.push(XPathExpression.INSTR_NOT);
		return captures;
	}

	@Override
	public Object visit(NumericComparisonExpression node, ShortVector instrs) {
		if (node.jjtGetNumChildren() != 2)
			errorMessage("Unexpectedly found " + node.jjtGetNumChildren() + " descendants of a numeric comparison expression");
		int captures = CAPTURE_NONE;
		
		for (int i = 0; i < 2; i++) {
			captures |= (Integer) node.jjtGetChild(i).jjtAccept(this, instrs);
		}
		
		String op = (String) node.jjtGetValue();
		if ("=".equals(op)) {
			instrs.push(XPathExpression.INSTR_EQ);
		} else if ("!=".equals(op)) {
			instrs.push(XPathExpression.INSTR_NE);
		} else if (">".equals(op)) {
			instrs.push(XPathExpression.INSTR_GT);
		} else if ("<".equals(op)) {
			instrs.push(XPathExpression.INSTR_LT);
		} else if (">=".equals(op)) {
			instrs.push(XPathExpression.INSTR_GE);
		} else if ("<=".equals(op)) {
			instrs.push(XPathExpression.INSTR_LE);
		} else {
			errorMessage("Unknown binary operator \"" + op + "\"");
		}
		return captures;
	}

	@Override
	public Object visit(StringComparisonExpression node, ShortVector instrs) {
		if (node.jjtGetNumChildren() != 2)
			errorMessage("Unexpectedly found " + node.jjtGetNumChildren() + " descendants of a string comparison expression");
		int captures = CAPTURE_NONE;
		
		for (int i = 0; i < 2; i++) {
			captures |= (Integer) node.jjtGetChild(i).jjtAccept(this, instrs);
		}
		
		requireStackDepth(1);
		String op = (String) node.jjtGetValue();
		if ("=".equals(op)) {
			instrs.push(XPathExpression.INSTR_EQ_STR);
		} else if ("!=".equals(op)) {
			instrs.push(XPathExpression.INSTR_EQ_STR);
			instrs.push(XPathExpression.INSTR_NOT);
		} else {
			errorMessage("Unknown binary operator \"" + op + "\"");
		}
		return captures;
	}

	@Override
	public Object visit(StringSearchFunction node, ShortVector instrs) {
		if (node.jjtGetNumChildren() != 2)
			errorMessage("Unexpectedly found " + node.jjtGetNumChildren() + " descendants of a string search function expression");
		int captures = CAPTURE_NONE;
		
		for (int i = 0; i < 2; i++) {
			captures |= (Integer) node.jjtGetChild(i).jjtAccept(this, instrs);
		}
		
		String fnName = (String) node.jjtGetValue();
		
		requireStackDepth(1);
		if ("contains".equals(fnName)) {
			instrs.push(XPathExpression.INSTR_CONTAINS);
		} else if ("starts-with".equals(fnName)) {
			instrs.push(XPathExpression.INSTR_STARTS_WITH);
		} else if ("ends-with".equals(fnName)) {
			instrs.push(XPathExpression.INSTR_ENDS_WITH);
		} else if ("matches".equals(fnName)) {
			String pattern = mLiterals.get(instrs.get(instrs.size() - 1));
			
			// test-compile the expression so that malformed expressions are
			// a compile-time error, not a runtime error
			try {
				Pattern.compile(pattern);
			} catch (PatternSyntaxException e) {
				errorMessage("Malformed regular expression: " + e);
			}
			instrs.push(XPathExpression.INSTR_MATCHES);
		} else {
			errorMessage("Unknown string comparison function \"" + fnName + "\"");
		}

		return captures;
	}

	@Override
	public Object visit(IntegerValue node, ShortVector instrs) {
		String ival = (String) node.jjtGetValue();
		
		requireStackDepth(1);
		instrs.push(XPathExpression.INSTR_ILITERAL);
		instrs.push(Short.parseShort(ival));
		return CAPTURE_NONE;
	}

	@Override
	public Object visit(StringValue node, ShortVector instrs) {
		String str = (String) node.jjtGetValue();
		
		// the stored value includes the start and end quotes: strip them
		// and unescape any quotation marks in the literal
		char quote = str.charAt(0);
		str = str.substring(1, str.length()-1);
		str = str.replace(new String(new char[] { quote,  quote }), new String(new char[] { quote }));

		instrs.push(XPathExpression.INSTR_LITERAL);
		instrs.push(addLiteral(str));
		return CAPTURE_NONE;
	}

	@Override
	public Object visit(AttributeExpression node, ShortVector instrs) {
		String name = (String) node.jjtGetChild(0).jjtAccept(this, instrs);
		
		if (name.startsWith("child::") || name.startsWith("descendant::")) {
			errorMessage("Cannot use Element as a value in a predicate");
		} else if (name.startsWith("attribute::")) {
			name = name.substring(11);
		}
		QName qName = parseQName(name, true);
		
		instrs.push(XPathExpression.INSTR_ATTRIBUTE);
		instrs.push(addQName(qName));
		return CAPTURE_ATTRIBUTES;
	}

	@Override
	public Object visit(CaptureAttrsFunction node, ShortVector instrs) {
		requireStackDepth(1);
		instrs.push(XPathExpression.INSTR_ILITERAL);
		instrs.push((short) 1);
		return CAPTURE_ATTRIBUTES;
	}

	@Override
	public Object visit(PositionFunction node, ShortVector instrs) {
		requireStackDepth(1);
		instrs.push(XPathExpression.INSTR_POSITION);
		return CAPTURE_POSITIONS;
	}

	/**
	 * Returns the raw Name value
	 */
	@Override
	public Object visit(NameValue node, ShortVector instrs) {
		return node.jjtGetValue();
	}

	/**
	 * Do the work of compiling an Expression.
	 * 
	 * @return the Local Name of the last tag in the expression
	 */
	@Override
	public Object visit(Expression expressionNode, ShortVector instrs) {
		// we compile steps from last to first as matches the tag stack
		int totalSteps = expressionNode.jjtGetNumChildren();
		
		if (totalSteps == 0) {
			errorMessage("An XPath expression must have at least one element");
			return "";
		}
		
		// we need to keep track of whether the step _after_ the current step
		// was a descendant:: step, e.g. when compiling "a/b/descendant::c", we need
		// to know that "c" was a descendant:: step when we compile "b"
		boolean lastWasDescendant = false;
		
		for (int step = totalSteps - 1; step >= 0; step -= 2) {
			// determine the tag name for this step
			Node axisStepNode = expressionNode.jjtGetChild(step);
			String name = (String) axisStepNode.jjtGetChild(0).jjtAccept(this, instrs);
			boolean isDescendant = lastWasDescendant;

			lastWasDescendant = false;
			if (name.startsWith("child::")) {
				name = name.substring(7);
			} else if (name.startsWith("descendant::")) {
				name = name.substring(11);
				lastWasDescendant = true;
			} else if (name.startsWith("attribute::") || name.startsWith("@")) {
				errorMessage("Cannot use an attribute name as an Axis Step");
			}
			
			// this is the real name for this step
			QName qName = parseQName(name, false);
			
			// add an instruction scroll up the stack to this node, if the following
			// step was a '//'
			if (step != totalSteps - 1) {
				// we're not on the "b" of ...a/b
				Node separatorNode = expressionNode.jjtGetChild(step + 1);
				
				if (separatorNode instanceof SlashSlash) {
					isDescendant = true;
				}
			}

			int nonconsecutiveElementLabel = instrs.size();
			if (isDescendant) {
				// this tag is the "b" in "a/b//c" or "a/b/descendant::c":
				// look up the stack until we find it
				instrs.push(XPathExpression.INSTR_NONCONSECUTIVE_ELEMENT);
				instrs.push(addQName(qName));
			}
			
			// now that we're at the correct tag, compile any Predicates for this step
			int captureFlags = CAPTURE_NONE;
			for (int i = 1, children = axisStepNode.jjtGetNumChildren(); i < children; i++) {
				captureFlags |= (Integer) axisStepNode.jjtGetChild(i).jjtAccept(this, instrs);
				
				if (isDescendant) {
					// each Predicate ends with an INSTR_TEST_PREDICATE, patch it into an
					// INSTR_SOFT_TEST_PREDICATE so that if the predicate fails the VM will 
					// branch back to retry the INSTR_NONCONSECUTIVE_ELEMENT up the tag stack
					if (instrs.top() != XPathExpression.INSTR_TEST_PREDICATE)
						errorMessage("Internal error: found a predicate that ended with instruction " + instrs.top());
					int softTestLabel = instrs.size() - 1;
					instrs.put(instrs.size() - 1, XPathExpression.INSTR_SOFT_TEST_PREDICATE);
					instrs.push((short)(nonconsecutiveElementLabel - softTestLabel));
				}
			}

			// then compile the step itself
			instrs.push(XPathExpression.INSTR_ELEMENT);
			instrs.push(addQName(qName));
			
			if ((captureFlags & CAPTURE_ATTRIBUTES) != 0) {
				mAttributeCaptureTags.add(qName.getLocalPart());
			}
			
			if ((captureFlags & CAPTURE_POSITIONS) != 0) {
				if (step < 2) {
					// this is the topmost node in the pattern: we can't capture positions for it
					errorMessage("Cannot use position() predicates for the topmost node in a path");
				}
				mPositionCaptureTags.add(qName.getLocalPart());
			}
		}
		
		// if the pattern starts with a Slash, mark it absolute
		if (expressionNode.jjtGetChild(0) instanceof Slash) {
			instrs.push(XPathExpression.INSTR_ROOT);
		}
		
		// return the local part of the innermost node in the pattern
		String lastNodeName = (String) expressionNode.jjtGetChild(totalSteps - 1)
														.jjtGetChild(0)
														.jjtAccept(this, instrs);
		return parseQName(lastNodeName, false).getLocalPart();
	}
	
	private String compileExpression(Node expressionNode, ShortVector instrVector) {
		return (String) expressionNode.jjtAccept(this, instrVector);
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
		mCurrentMethodElement = mClass.methodElements().get(methodName);
		
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
				
				// System.out.println("parsed \"" + xpathExpression + "\" to " + instructions);
				// store the compiled method
				mInstructions.add(instructions);
				mMethods.add(new Method(methodName, xpathExpression, mInstructions.size() - 1));
				addTrigger(trigger, mInstructions.size() - 1);
			}
		} catch (ParseException e) {
			errorMessage("Syntax error in XPath expression:\n" + e.getMessage());
		} catch (Exception e) {
			errorMessage("Internal error parsing XPath expression \"" + xpathExpression + "\": " + e.toString());
			e.printStackTrace();
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
	
	public Vector<ShortVector> instructions() {
		return mInstructions;
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
