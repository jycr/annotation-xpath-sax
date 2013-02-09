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

import com.googlecode.axs.xpath.AndExpression;
import com.googlecode.axs.xpath.AttributeExpression;
import com.googlecode.axs.xpath.CaptureAttrsFunction;
import com.googlecode.axs.xpath.Expression;
import com.googlecode.axs.xpath.FunctionExpression;
import com.googlecode.axs.xpath.IntegerValue;
import com.googlecode.axs.xpath.NameValue;
import com.googlecode.axs.xpath.Node;
import com.googlecode.axs.xpath.NotExpression;
import com.googlecode.axs.xpath.NumericComparisonExpression;
import com.googlecode.axs.xpath.OrExpression;
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
import com.googlecode.axs.xpath.Wildcard;

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

	private int mMaxPredicateStackDepth = 0;
	private Vector<ShortVector> mTokens = null;
	private Vector<String> mLiterals = null;
	private Vector<QName> mQNames = null;
	private HashMap<String, Integer> mLiteralIndices = null;
	private HashMap<QName, Integer> mQNameIndices = null;
	private HashMap<String, Vector<Integer>> mTriggerTags = new HashMap<String, Vector<Integer>>();
	private HashSet<String> mAttributeCaptureTags = new HashSet<String>();
	private HashSet<String> mPositionCaptureTags = new HashSet<String>();
	private Stack<Boolean> mPositionCaptureStack = new Stack<Boolean>();
	
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
	
	@Override
	public Object visit(SimpleNode node, ShortVector data) {
		mMessager.printMessage(Kind.ERROR, "Got an unhandled #" + node.getClass().getSimpleName() +
				" node in the parse tree!");
		return null;
	}

	@Override
	public Object visit(Start node, ShortVector data) {
		mMessager.printMessage(Kind.ERROR, "Got an unhandled #" + node.getClass().getSimpleName() +
				" node in the parse tree!");
		return null;
	}

	@Override
	public Object visit(Slash node, ShortVector data) {
		mMessager.printMessage(Kind.ERROR, "Got an unhandled #" + node.getClass().getSimpleName() +
				" node in the parse tree!");
		return null;
	}

	@Override
	public Object visit(SlashSlash node, ShortVector data) {
		mMessager.printMessage(Kind.ERROR, "Got an unhandled #" + node.getClass().getSimpleName() +
				" node in the parse tree!");
		return null;
	}

	@Override
	public Object visit(Wildcard node, ShortVector data) {
		mMessager.printMessage(Kind.ERROR, "Got an unhandled #" + node.getClass().getSimpleName() +
				" node in the parse tree!");
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(OrExpression node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(AndExpression node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(NotExpression node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(NumericComparisonExpression node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(StringComparisonExpression node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(StringSearchFunction node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(IntegerValue node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(StringValue node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(AttributeExpression node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(CaptureAttrsFunction node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PositionFunction node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(FunctionExpression node, ShortVector instrs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(NameValue node, ShortVector instrs) {
		mMessager.printMessage(Kind.ERROR, "Got an unhandled #" + node.getClass().getSimpleName() +
				" node in the parse tree!");
		return null;
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
		boolean capturePosition = false;
		String lastNodeName = "";
		
		for (int child = totalSteps - 1; child >= 0; child -= 2) {
			// compile any Predicates for this step
			Node axisStepNode = expressionNode.jjtGetChild(child);
			int captureFlags = (Integer) axisStepNode.jjtAccept(this, instrs);

			// then compile the step itself
			// the last steps is always INSTR_ELEMENT
			short tagInstr = XPathExpression.INSTR_ELEMENT;
			
			if (child != totalSteps - 1) {
				// we're not on the "b" of ...a/b
				Node separatorNode = expressionNode.jjtGetChild(child + 1);
				
				if (separatorNode instanceof SlashSlash) {
					// this tag is the "b" in a/b//c...
					tagInstr = XPathExpression.INSTR_NONCONSECUTIVE_ELEMENT;
				}
			}
			
			if (capturePosition) {
				// the node below us needs to know its position(), i.e.
				// we're now at the "b" of a/b/c[position() > 3]
			}
		}
		return null;
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
