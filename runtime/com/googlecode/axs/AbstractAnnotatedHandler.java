package com.googlecode.axs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.QName;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The AnnotatedHandlerBase provides the engine that powers any AXS handler class:
 * all handlers must derive from it.
 * @author Ben
 *
 */
public class AbstractAnnotatedHandler extends DefaultHandler {
	
	// a Stack of the current tag path
	private Stack<QName> mTagStack = new Stack<QName>();
	
	// a Stack of the attributes for the current tag path
	// note: we only store attributes that may be needed to execute one of the
	// XPath expressions in the handler; all tags whose attributes are never referenced
	// have null stored here.
	private Stack<HashMap<QName, String>> mAttributesStack = new Stack<HashMap<QName, String>>();
	
	// a Stack of the active text captures
	private Stack<StringBuilder> mTextCaptureStack = new Stack<StringBuilder>();
	
	// how many text captures are active
	private int mNrActiveTextCaptures = 0;
	
	// according to some Googling, it is faster to store a mutable object in a Map
	// and update it in-place than it is to do a fetch, update, store sequence
	private final class Position {
		  private int mValue = 1;
		  public void increment() {
			  ++mValue;
		  }
		  public int get() {
			  return mValue;
		  }
		  
		  public String toString() {
			  return "Position(" + mValue + ")";
		  }
	};
	
	// a Stack of HashMap<QName, Position>s which store position capture information
	// each Map stores the number of times that each QName has been seen under this parent
	private Stack<HashMap<QName, Position>> mPositionCaptureStack = new Stack<HashMap<QName, Position>>();
	
	// the evaluation stack for the predicate evaluator
	private int[] mPredicateStack = null;
	
	// the string stack for the predicate evaluator
	private String[] mPredicateStringStack = new String[2];

	// the compiled XPath expression data provider for our subclass
	private AXSData mAXSData = null;
	
	// cached values pulled from the AXSData
	private XPathExpression[] mExpressions = null;
	private int mNrCaptureExpressions = 0;
	private int mNrEndExpressions = 0;
	private Map<String, int[]> mTriggerTags = null;
	private Set<String> mAttributeCaptureTags = null;
	private Set<String> mPositionCaptureTags = null;
	private boolean mCaptureAttributes = false;
	private boolean mCapturePositions = false;

	// cache a few objects to reduce GC churn for the common case that only one expression is active
	// at a time
	private StringBuilder mCachedStringBuilder = null;
	private HashMap<QName, String> mCachedAttributesMap = null;
	private HashMap<QName, Position> mCachedPositionMap = null;

	private static final boolean TRACE_EXECUTION = false;
	
	public AbstractAnnotatedHandler() {
		super();
		
		// runtime-load the _AXSData class that corresponds to our subclass
		try {
			ClassLoader loader = this.getClass().getClassLoader();
			@SuppressWarnings("rawtypes")
			Class axsDataClass = loader.loadClass(this.getClass().getName() + "_AXSData");
			mAXSData = (AXSData) axsDataClass.newInstance();
		} catch (ClassNotFoundException e) {
			throw new XPathExecutionError("Unable to load _AXSData for class "+ this.getClass().getName());
		} catch (IllegalAccessException e) {
			throw new XPathExecutionError("Bug: got an IllegalAccessException while instantiating _AXSData" +
						" for class "+ this.getClass().getName());
		} catch (InstantiationException e) {
			throw new XPathExecutionError("Bug: got an InstantiationException while instantiating _AXSData" +
					" for class "+ this.getClass().getName());			
		}
	}

	// load and cache all the relevant data from the _AXSData
	private void setup() {
		mExpressions = mAXSData.getXPathExpressions();
		mNrCaptureExpressions = mAXSData.getNumberOfCapturingExpressions();
		mNrEndExpressions = mAXSData.getNumberOfEndExpressions();
		mTriggerTags = mAXSData.getTriggerTags();
		mAttributeCaptureTags = mAXSData.getAttributeCaptureTags();
		mPositionCaptureTags = mAXSData.getPositionCaptureTags();
		
		mCaptureAttributes = !mAttributeCaptureTags.isEmpty();
		mCapturePositions = !mPositionCaptureTags.isEmpty();
		
		mPredicateStack = new int[mAXSData.getMaximumPredicateStackDepth()];
	}
	
	// clear everything for a new document
	private void reset() {
		mTagStack.clear();
		mAttributesStack.clear();
		mTextCaptureStack.clear();
		mNrActiveTextCaptures = 0;
	}

	@Override
	public void startDocument() throws SAXException {
		if (mExpressions == null)
			setup();
		reset();
	}
	
	private void instrOk() {
		System.out.println("ok");
	}
	
	private void instrFail() {
		System.out.println("fail");
	}

	/**
	 * Test an XPathExpression against the current tag.
	 * @param xpr the expression to test
	 * @return true if the expression matches, false otherwise
	 */
	private boolean testExpression(XPathExpression xpr)
	{
		final short[] instructions = xpr.instructions();
		final String[] literals = xpr.literals();
		final QName[] qNames = xpr.qNames();
		final int[] evaluationStack = mPredicateStack;
		int esp = 0;
		int ip = 0, maxIp = instructions.length;
		int tagp = mTagStack.size() - 1; // the current tag
		String[] stringStack = mPredicateStringStack;
		int ssp = 0;
		
		if (TRACE_EXECUTION)
			System.out.println("START TEST:");
		
		// We implement predicates as a simple stack machine, while nodes are tested
		// directly against the tag stack. The expression is compiled to a reverse-Polish
		// form, where we test from the top of the tag stack back up towards the root.
		
		while (ip < maxIp) {
			final int instr = instructions[ip];
			
			switch (instr) {
			case XPathExpression.INSTR_ROOT:
				// test if we've hit the document root
				if (TRACE_EXECUTION)
					System.out.print("  ROOT(tagp=" + tagp + "): ");
				if (tagp != -1) {
					if (TRACE_EXECUTION)
						instrFail();
					return false;
				}
				if (TRACE_EXECUTION)
					instrOk();
				break;
			case XPathExpression.INSTR_ELEMENT:
			{
				final QName wantedTag = qNames[instructions[++ip]];
				
				if (TRACE_EXECUTION)
					System.out.print("  ELEMENT(" + wantedTag + "): ");

				// test if the current node is a specific tag
				if (tagp < 0) {
					if (TRACE_EXECUTION)
						instrFail();
					return false;
				}
				final QName tag = mTagStack.get(tagp);

				if (!wantedTag.equals(tag)) {
					if (TRACE_EXECUTION)
						instrFail();
					return false;
				}
				
				if (TRACE_EXECUTION)
					instrOk();

				// consume this level in the tag stack
				tagp--;
				break;
			}
			case XPathExpression.INSTR_CONTAINS:
			{
				if (stringStack[0].contains(stringStack[1])) {
					evaluationStack[esp++] = 1;
				} else {
					evaluationStack[esp++] = 0;
				}
				
				if (TRACE_EXECUTION)
					System.out.println("  CONTAINS(\"" + stringStack[0] + "\", \"" +
										stringStack[1] + "\"): " + evaluationStack[esp-1]);
				ssp = 0;
				break;
			}
			case XPathExpression.INSTR_ATTRIBUTE:
			{
				final QName attrName = qNames[instructions[++ip]];

				if (TRACE_EXECUTION)
					System.out.print("  ATTRIBUTE(" + attrName + "): ");

				// push the value of an attribute onto the predicate string stack
				if (tagp < 0) {
					if (TRACE_EXECUTION)
						instrFail();
					return false;
				}
				final HashMap<QName, String> attrs = mAttributesStack.get(tagp);
				if (attrs == null) {
					// predicates are executed before the tag value is tested, so
					// there is no guarantee that capture data is available if the tag
					// test would eventually fail
					if (TRACE_EXECUTION)
						instrFail();
					return false;
				}
				final String value = attrs.get(attrName);
				stringStack[ssp++] = (value != null ? value : "");
				
				if (TRACE_EXECUTION)
					System.out.println(stringStack[ssp-1]);

				break;
			}
			case XPathExpression.INSTR_LITERAL:
				// push a literal onto the predicate string stack
				stringStack[ssp++] = literals[instructions[++ip]];
				
				if (TRACE_EXECUTION)
					System.out.println("  LITERAL(\"" + stringStack[ssp-1] + "\")");
				break;
			case XPathExpression.INSTR_EQ_STR:
				// compare the two strings on the string stack and push the result
				// on the evaluation stack
				if (stringStack[0].equals(stringStack[1])) {
					evaluationStack[esp++] = 1;
				} else {
					evaluationStack[esp++] = 0;
				}
				ssp = 0;
				
				if (TRACE_EXECUTION)
					System.out.println("  EQ_STR: " + evaluationStack[esp-1]);
				break;
			case XPathExpression.INSTR_NOT:
				// invert the sense of the top of the evaluation stack
				if (evaluationStack[esp-1] == 0) {
					evaluationStack[esp-1] = 1;
				} else {
					evaluationStack[esp-1] = 0;
				}
				
				if (TRACE_EXECUTION)
					System.out.println("  NOT: " + evaluationStack[esp-1]);
				break;
			case XPathExpression.INSTR_ILITERAL:
				// push a numeric value onto the evaluation stack
				evaluationStack[esp++] = instructions[++ip];
				
				if (TRACE_EXECUTION)
					System.out.println("  ILITERAL(" + evaluationStack[esp-1] + ")");
				break;
			case XPathExpression.INSTR_AND:
			{
				// AND together the top two values on the evaluation stack
				final int i1 = evaluationStack[--esp];
				final int i2 = evaluationStack[esp-1];
				evaluationStack[esp-1] = ((i1 != 0) && (i2 != 0) ? 1 : 0);
				
				if (TRACE_EXECUTION)
					System.out.println("  AND: " + evaluationStack[esp-1]);
				break;
			}
			case XPathExpression.INSTR_OR:
			{
				// OR together the top two values on the evaluation stack
				final int i1 = evaluationStack[--esp];
				final int i2 = evaluationStack[esp-1];
				evaluationStack[esp-1] = ((i1 != 0) || (i2 != 0) ? 1 : 0);
				
				if (TRACE_EXECUTION)
					System.out.println("  OR: " + evaluationStack[esp-1]);
				break;
			}
			case XPathExpression.INSTR_TEST_PREDICATE:
				// test whether the predicate matched
				if (TRACE_EXECUTION)
					System.out.print("  TEST_PREDICATE: ");

				if (esp != 1 || evaluationStack[0] == 0) {
					if (TRACE_EXECUTION)
						instrFail();
					return false;
				}

				esp = 0;
				if (TRACE_EXECUTION)
					instrOk();
				break;
			case XPathExpression.INSTR_ENDS_WITH:
			{
				if (stringStack[0].endsWith(stringStack[1])) {
					evaluationStack[esp++] = 1;
				} else {
					evaluationStack[esp++] = 0;
				}
				ssp = 0;
				
				if (TRACE_EXECUTION)
					System.out.println("  ENDS_WITH(\"" + stringStack[0] + "\", \"" +
										stringStack[1] + "\"): " + evaluationStack[esp-1]);
				break;
			}
			case XPathExpression.INSTR_STARTS_WITH:
			{
				if (stringStack[0].startsWith(stringStack[1])) {
					evaluationStack[esp++] = 1;
				} else {
					evaluationStack[esp++] = 0;
				}
				ssp = 0;
				
				if (TRACE_EXECUTION)
					System.out.println("  STARTS_WITH(\"" + stringStack[0] + "\", \"" +
										stringStack[1] + "\"): " + evaluationStack[esp-1]);
				break;
			}
			case XPathExpression.INSTR_NONCONSECUTIVE_ELEMENT:
				// consume any number of tags until the QName before the double slash is found
			{
				final QName targetTag = qNames[instructions[++ip]];
				
				if (TRACE_EXECUTION)
					System.out.print("  NONCONSECUTIVE_ELEMENT(" + targetTag + "): ");
				while (tagp >= 0) {
					if (mTagStack.get(tagp).equals(targetTag))
						break;
					tagp--;
				}
				if (tagp < 0) {
					if (TRACE_EXECUTION)
						instrFail();
					return false;
				}
				if (TRACE_EXECUTION)
					instrOk();
				break;
			}
			case XPathExpression.INSTR_POSITION:
				// push the position() of the current tag onto the evaluation stack
				if (TRACE_EXECUTION)
					System.out.print("  POSITION: ");

				if (tagp <= 0) {
					if (TRACE_EXECUTION)
						instrFail();
					return false;
				}
				// look up the current tag under its parent
				final HashMap<QName, Position> posMap = mPositionCaptureStack.get(tagp-1);
				if (posMap == null) {
					// predicates are executed before the tag value is tested, so
					// there is no guarantee that capture data is available if the tag
					// test would eventually fail
					if (TRACE_EXECUTION)
						instrFail();
					return false;
				}
				final int pos = posMap.get(mTagStack.get(tagp)).get();
				evaluationStack[esp++] = pos;
				
				if (TRACE_EXECUTION)
					System.out.println(String.valueOf(evaluationStack[esp-1]));
				break;
			case XPathExpression.INSTR_LT:
			{
				// less-than operation
				final int i1 = evaluationStack[--esp];
				final int i2 = evaluationStack[esp-1];
				evaluationStack[esp-1] = (i2 < i1 ? 1 : 0);
				
				if (TRACE_EXECUTION)
					System.out.println("  LT: " + evaluationStack[esp-1]);
				break;
			}
			case XPathExpression.INSTR_GT:
			{
				// greater-than operation
				final int i1 = evaluationStack[--esp];
				final int i2 = evaluationStack[esp-1];
				evaluationStack[esp-1] = (i2 > i1 ? 1 : 0);
				
				if (TRACE_EXECUTION)
					System.out.println("  GT: " + evaluationStack[esp-1]);
				break;
			}
			case XPathExpression.INSTR_EQ:
			{
				// numeric equality operation
				final int i1 = evaluationStack[--esp];
				final int i2 = evaluationStack[esp-1];
				evaluationStack[esp-1] = (i2 == i1 ? 1 : 0);
				
				if (TRACE_EXECUTION)
					System.out.println("  EQ: " + evaluationStack[esp-1]);
				break;
			}
			case XPathExpression.INSTR_NE:
			{
				// numeric inequality operation
				final int i1 = evaluationStack[--esp];
				final int i2 = evaluationStack[esp-1];
				evaluationStack[esp-1] = (i2 != i1 ? 1 : 0);
				
				if (TRACE_EXECUTION)
					System.out.println("  NE: " + evaluationStack[esp-1]);
				break;
			}
			case XPathExpression.INSTR_LE:
			{
				// less-than-or-equal operation
				final int i1 = evaluationStack[--esp];
				final int i2 = evaluationStack[esp-1];
				evaluationStack[esp-1] = (i2 <= i1 ? 1 : 0);
				
				if (TRACE_EXECUTION)
					System.out.println("  LE: " + evaluationStack[esp-1]);
				break;
			}
			case XPathExpression.INSTR_GE:
			{
				// greater-than-or-equal operation
				final int i1 = evaluationStack[--esp];
				final int i2 = evaluationStack[esp-1];
				evaluationStack[esp-1] = (i2 >= i1 ? 1 : 0);
				
				if (TRACE_EXECUTION)
					System.out.println("  GE: " + evaluationStack[esp-1]);
				break;
			}
			default:
				throw new XPathExecutionError("unexpected instruction value " + instr);
			}
			
			ip++;
		}

		return true;
	}
	
	/**
	 * Build a QName from the startElement()/endElement() parameters.
	 * @param uri
	 * @param localName
	 * @param qName
	 * @return a new QName
	 */
	private QName makeQName(String uri, String localName, String qName) {
		if (! "".equals(uri))
			return new QName(uri, localName);
		return new QName(qName);
	}
	
	/**
	 * Capture all the attributes in @p attrs.
	 * @param attrs the attributes to capture to the attribute stack
	 */
	private void captureAttributes(Attributes attrs) {
		HashMap<QName, String> map = null;
		
		// try to hit the map cache
		if (mCachedAttributesMap != null) {
			map = mCachedAttributesMap;
			mCachedAttributesMap = null;
			map.clear();
		} else {
			map = new HashMap<QName, String>();
		}
		
		// store all the attributes into the map
		for (int i = 0, len = attrs.getLength(); i < len; i++) {
			final String uri = attrs.getURI(i);
			final String localName = attrs.getLocalName(i);
			final String value = attrs.getValue(i);
			
			map.put(makeQName(uri, localName, localName), value);
		}
		
		// push the map onto the attributes stack
		mAttributesStack.push(map);
	}
	
	/**
	 * Update the position record for a new tag. Assumes that the capture stack is still
	 * at the position for the tag's parent.
	 * @param qn the QName of the new tag
	 */
	private void capturePosition(QName qn) {
		// there is only one root element, ever
		if (mPositionCaptureStack.empty())
			return;
		
		HashMap<QName, Position> map = mPositionCaptureStack.pop();
		
		if (map == null) {
			if (mCachedPositionMap != null) {
				map = mCachedPositionMap;
				mCachedPositionMap = null;
				map.clear();
			} else {
				map = new HashMap<QName, Position>();
			}
		}
		
		Position p = map.get(qn);
		if (p != null) {
			p.increment();
		} else {
			map.put(qn, new Position());
		}
		
		mPositionCaptureStack.push(map);
	}
	
	/**
	 * Set up to start capturing all the text within this element.
	 */
	private void startTextCapture() {
		StringBuilder sb = mTextCaptureStack.pop();
		
		if (sb != null) {
			mTextCaptureStack.push(sb);
			return;
		}

		mNrActiveTextCaptures++;
		
		if (mCachedStringBuilder != null) {
			mCachedStringBuilder.setLength(0);
			mTextCaptureStack.push(mCachedStringBuilder);
			mCachedStringBuilder = null;
		} else {
			mTextCaptureStack.push(new StringBuilder());
		}
	}
	
	/**
	 * The implementation of startElement() provides support for starting text captures for
	 * {@literal @}XPath() handlers and firing {@literal @}XPathStart() handlers. Make sure
	 * you call it via super if you override it in your handler subclass.
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
		// push this tag onto the stack
		final QName qn = makeQName(uri, localName, qName);
		mTagStack.push(qn);
		
		if (TRACE_EXECUTION)
			System.out.println("startElement: <" + qn + ">");

		String normalizedLocalName = qn.getLocalPart();
		
		// test whether to perform attribute capture
		if (mCaptureAttributes) {
			if (mAttributeCaptureTags.contains(normalizedLocalName)) {
				captureAttributes(attrs);
				if (TRACE_EXECUTION)
					System.out.println("captureAttributes => " + mAttributesStack.peek());
			} else {
				mAttributesStack.push(null);
			}
		}

		// test whether to perform position capture
		if (mCapturePositions) {
			if (mPositionCaptureTags.contains(normalizedLocalName)) {
				if (TRACE_EXECUTION)
					System.out.println("capturePosition => " + mPositionCaptureStack.peek());
				// update the position marker in the parent tag's level in the stack
				capturePosition(qn);
			}
			// push the position capture stack
			mPositionCaptureStack.push(null);
		}
		
		// push the text capture stack
		mTextCaptureStack.push(null);

		// test whether we should start text capture for one or more expressions
		// and test whether we have any expressions to fire
		int[] triggeredExpressions = mTriggerTags.get(normalizedLocalName);
		
		if (triggeredExpressions == null)
			return;
		
		for (final int exprIndex : triggeredExpressions) {
			// skip @XPathEnd() expressions
			if (exprIndex >= mNrCaptureExpressions && exprIndex < mNrCaptureExpressions+mNrEndExpressions)
				continue;
			
			if (!testExpression(mExpressions[exprIndex]))
				continue;
			
			// the expression matched: execute it
			if (exprIndex < mNrCaptureExpressions) {
				if (TRACE_EXECUTION)
					System.out.println("CAPTURE for @XPath(" + exprIndex + ")");
				
				// we've found the start of an @XPath() expression: start capturing TEXT elements
				startTextCapture();
			} else {
				if (TRACE_EXECUTION)
					System.out.println("CALL @XPathStart(" + exprIndex + ")");
				
				// must be an @XPathStart() expression: fire it
				mAXSData.callXPathStart(this, exprIndex, attrs);
			}
		}
	}
	
	/**
	 * The implementation of characters() provides text capture support for {@literal @}XPath()
	 * handlers. Make sure you call if via super if you override it in your handler subclass.
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (mNrActiveTextCaptures == 0)
			return;
		
		// append the characters to all the active text captures
		int n = mNrActiveTextCaptures, ix = mTextCaptureStack.size() - 1;
		while (n > 0) {
			final StringBuilder sb = mTextCaptureStack.get(ix);
			
			if (sb != null) {
				sb.append(ch, start, length);
				n--;
			}
			ix--;
		}
	}
	
	/**
	 * The implementation of endElement() is responsible for firing {@literal @}XPath() and 
	 * {@literal @}XPathEnd() handlers. Make sure to call it via super if you override it in
	 * your handler subclass.
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		// if we've completed a capture, close it out
		String text = null;
		final StringBuilder sb = mTextCaptureStack.pop();
		
		if (sb != null) {
			text = sb.toString();
			mNrActiveTextCaptures--;
			mCachedStringBuilder = sb;
		}

		if (TRACE_EXECUTION) {
			QName qn = makeQName(uri, localName, qName);
			System.out.println("endElement: <" + qn + ">");
		}

		// test whether we have any expressions to fire
		int[] triggeredExpressions = mTriggerTags.get(uri.equals("") ? qName : localName);
		
		if (triggeredExpressions != null) {
			for (final int exprIndex : triggeredExpressions) {
				// skip @XPathStart() expressions
				if (exprIndex >= mNrCaptureExpressions+mNrEndExpressions)
					continue;
				
				if (!testExpression(mExpressions[exprIndex]))
					continue;
				
				// the expression matched: execute it
				if (exprIndex < mNrCaptureExpressions) {
					if (TRACE_EXECUTION)
						System.out.println("CALL @XPath(" + exprIndex + ",\"" + text + "\")");
					
					// we've found the end of an @XPath() expression: fire it
					mAXSData.callXPathText(this, exprIndex, text);
				} else {
					if (TRACE_EXECUTION)
						System.out.println("CALL @XPathEnd(" + exprIndex + ")");
					
					// must be an @XPathEnd() expression: fire it
					mAXSData.callXPathEnd(this, exprIndex);
				}
			}
		}
		
		// pop the remaining stacks
		if (mCapturePositions) {
			final HashMap<QName, Position> posMap = mPositionCaptureStack.pop();
			
			if (posMap != null)
				mCachedPositionMap = posMap;
		}
		
		if (mCaptureAttributes) {
			final HashMap<QName, String> attrMap = mAttributesStack.pop();
			
			if (attrMap != null)
				mCachedAttributesMap = attrMap;
		}
		
		mTagStack.pop();
	}
	
	/**
	 * Query what depth the current tag is at. Call this from inside an expression handler.
	 * @return the number of tag to the root from the current tag
	 */
	public int tagDepth() {
		return mTagStack.size();
	}
	
	/**
	 * Query the tag at some other depth in the document. Depth 0 refers to the root element; the current tag
	 * is at {@link tagDepth}()-1.
	 * @param depth
	 * @return the QName of the element at the given @p depth
	 */
	public QName tagAtDepth(int depth) {
		return mTagStack.get(depth);
	}
	
	/**
	 * Look for a tag in the path to the current tag.
	 * @param qName the fully-qualified name of the tag to search for
	 * @return the depth of the found tag, or -1 if it was not found
	 */
	public int findTag(QName tag) {
		return mTagStack.indexOf(tag);
	}
	
	/**
	 * Look for a tag in the path to the current tag.
	 * @param qName the fully-qualified name of the tag to search for
	 * @param start the position to start the search at
	 * @return the depth of the found tag, or -1 if it was not found
	 */
	public int findTag(QName tag, int start) {
		return mTagStack.indexOf(tag, start);
	}
	
	/**
	 * Query the attributes map a tag at some other depth in the document. Depth 0 refers to the root element;
	 * the current tag is {@link tagDepth}()-1. Use the special captureattrs() function as a predicate in
	 * your XPath expression to mark that attributes should be captured for a given element even if they are
	 * not used in any other predicate.
	 * @param depth
	 * @return either a map of attributes or null if the attributes were not captured
	 */
	public Map<QName, String> attributesAtDepth(int depth) {
		return mAttributesStack.get(depth);
	}
}
