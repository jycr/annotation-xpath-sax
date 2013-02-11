package com.googlecode.axs;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;
import javax.xml.namespace.QName;

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
import com.googlecode.axs.xpath.Parser;
import com.googlecode.axs.xpath.ParserTreeConstants;
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
		
		mTokens = new Vector<ShortVector>();
		mLiterals = new Vector<String>();
		mQNames = new Vector<QName>();
		mLiteralIndices = new HashMap<String, Integer>();
		mQNameIndices = new HashMap<QName, Integer>();
	}

	private int mMaxPredicateStackDepth = 0, mCurrentPredicateStackDepth = 0;
	private Vector<ShortVector> mTokens = null;
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
	
	private QName parseQName(String name) {
		int colonPosition = name.indexOf(':');
		
		if (colonPosition > 0) {
			String prefix = name.substring(0, colonPosition);
			String localName = name.substring(colonPosition + 1);
			
			String uri = mClass.prefixMap().get(prefix);
			
			if (uri == null) {
				errorMessage("No Namespace URI mapping specified for prefix \"" + prefix + "\"");
			}
			return new QName(uri, localName, prefix);
		}
		return new QName(name);
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
		int captures = CAPTURE_NONE;
		
		for (int i = 1, children = node.jjtGetNumChildren(); i < children; i++) {
			captures |= (Integer) node.jjtGetChild(i).jjtAccept(this, instrs);
		}
		return captures;
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
			errorMessage("Unexpectedly found " + node.jjtGetNumChildren() + " decendents of a not() expression");
		int captures = (Integer) node.jjtGetChild(0).jjtAccept(this, instrs);
		instrs.push(XPathExpression.INSTR_NOT);
		return captures;
	}

	@Override
	public Object visit(NumericComparisonExpression node, ShortVector instrs) {
		if (node.jjtGetNumChildren() != 2)
			errorMessage("Unexpectedly found " + node.jjtGetNumChildren() + " decendents of a numeric comparison expression");
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
			errorMessage("Unexpectedly found " + node.jjtGetNumChildren() + " decendents of a string comparison expression");
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
			errorMessage("Unexpectedly found " + node.jjtGetNumChildren() + " decendents of a string comparison function expression");
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
		
		if (name.startsWith("child::") || name.startsWith("decendent::")) {
			errorMessage("Cannot use Element as a value in a predicate");
		} else if (name.startsWith("attribute::")) {
			name = name.substring(11);
		}
		QName qName = parseQName(name);
		
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
		
		for (int child = totalSteps - 1; child >= 0; child -= 2) {
			// compile any Predicates for this step
			Node axisStepNode = expressionNode.jjtGetChild(child);
			int captureFlags = (Integer) axisStepNode.jjtAccept(this, instrs);

			// then compile the step itself
			// the last step is always INSTR_ELEMENT
			short tagInstr = XPathExpression.INSTR_ELEMENT;
			
			if (child != totalSteps - 1) {
				// we're not on the "b" of ...a/b
				Node separatorNode = expressionNode.jjtGetChild(child + 1);
				
				if (separatorNode instanceof SlashSlash) {
					// this tag is the "b" in a/b//c...
					tagInstr = XPathExpression.INSTR_NONCONSECUTIVE_ELEMENT;
				}
			}
			
			// remove axis prefixes from the Name for the step
			String name = (String) axisStepNode.jjtGetChild(0).jjtAccept(this, instrs);
			if (name.startsWith("child::")) {
				name = name.substring(7);
			} else if (name.startsWith("decendent::")) {
				name = name.substring(11);
				
				// replace the separator _before_ this step with a SlashSlash
				if (child > 0) {
					expressionNode.jjtAddChild(new SlashSlash(ParserTreeConstants.JJTSLASHSLASH), child - 1);
				}
			} else if (name.startsWith("attribute::") || name.startsWith("@")) {
				errorMessage("Cannot use an attribute name as an Axis Step");
			}
			
			// add the step to the instructions vector
			QName qName = parseQName(name);
			
			instrs.push(tagInstr);
			instrs.push(addQName(qName));
			
			if ((captureFlags & CAPTURE_ATTRIBUTES) != 0) {
				mAttributeCaptureTags.add(qName.getLocalPart());
			}
			
			if ((captureFlags & CAPTURE_POSITIONS) != 0) {
				if (child < 2) {
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
		return parseQName(lastNodeName).getLocalPart();
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
				mTokens.add(instructions);
				mMethods.add(new Method(methodName, xpathExpression, mTokens.size() - 1));
				addTrigger(trigger, mTokens.size() - 1);
			}
		} catch (Exception e) {
			mMessager.printMessage(Kind.ERROR, "Error parsing XPath expression \"" + xpathExpression + "\": " + e.toString());
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
