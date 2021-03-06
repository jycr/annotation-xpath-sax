/* This class is autogenerated by the AXS compiler. Do not edit! */

/* **********************************************************************************/
/* Copyright (c) 2013 Benjamin K. Stuhl                                             */
/*                                                                                  */
/* Permission is hereby granted, free of charge, to any person obtaining a copy     */
/* of this software and associated documentation files (the "Software"), to deal    */
/* in the Software without restriction, including without limitation the rights     */
/* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies */
/* of the Software, and to permit persons to whom the Software is furnished to do   */
/* so, subject to the following conditions:                                         */
/*                                                                                  */
/* The above copyright notice and this permission notice shall be included in all   */
/* copies or substantial portions of the Software.                                  */
/*                                                                                  */
/* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR       */
/* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,         */
/* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE      */
/* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER           */
/* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,    */
/* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE    */
/* SOFTWARE.                                                                        */
/* **********************************************************************************/

package com.googlecode.axs.tests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Generated;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.googlecode.axs.AXSData;
import com.googlecode.axs.AbstractAnnotatedHandler;
import com.googlecode.axs.HandlerCallError;
import com.googlecode.axs.QName;
import com.googlecode.axs.XPathExpression;

import com.googlecode.axs.tests.RuntimeTest2;


@Generated(value = { "com.googlecode.axs.AnnotationProcessor", "com.googlecode.axs.tests.RuntimeTest2"})
public class RuntimeTest2_AXSData implements AXSData {
    private static Object Lock = new Object();

    @Override
    public void callXPathText(AbstractAnnotatedHandler abstractHandler, int exprIx, String callbackArg) throws SAXException {
        RuntimeTest2 handler = (RuntimeTest2) abstractHandler;

        switch (exprIx) {
        case 0: 
            // "ns1:map/ns2:key[ends-with(@value, 'c')] | ns1:map/ns2:key[starts-with(@value, 'c')]"
            handler.neverHappen(callbackArg);
            break;
        case 1: 
            // "ns1:map/ns2:key[ends-with(@value, 'c')] | ns1:map/ns2:key[starts-with(@value, 'c')]"
            handler.neverHappen(callbackArg);
            break;
        case 2: 
            // "ns2:key[captureattrs()]/value | ns1:map/ns2:key[ends-with(@value, 'bb')]/value | ns2:key[contains(@value, 'bb')]/value"
            handler.testAttributeStack(callbackArg);
            break;
        case 3: 
            // "ns2:key[captureattrs()]/value | ns1:map/ns2:key[ends-with(@value, 'bb')]/value | ns2:key[contains(@value, 'bb')]/value"
            handler.testAttributeStack(callbackArg);
            break;
        case 4: 
            // "ns2:key[captureattrs()]/value | ns1:map/ns2:key[ends-with(@value, 'bb')]/value | ns2:key[contains(@value, 'bb')]/value"
            handler.testAttributeStack(callbackArg);
            break;
        case 5: 
            // "value[@label != '']"
            handler.testEmpty(callbackArg);
            break;
        case 6: 
            // "ns2:key[matches(@value, 'ab*')]"
            handler.testRegexp(callbackArg);
            break;
        case 7: 
            // "ns2:key[starts-with('bbbbbbbbb', @value)]"
            handler.testStartsWith(callbackArg);
            break;
        default: throw new HandlerCallError("unhandled call #" + exprIx);
        }
    }

    @Override
    public void callXPathEnd(AbstractAnnotatedHandler abstractHandler, int exprIx) throws SAXException {
        RuntimeTest2 handler = (RuntimeTest2) abstractHandler;

        switch (exprIx) {
        default: throw new HandlerCallError("unhandled call #" + exprIx);
        }
    }

    @Override
    public void callXPathStart(AbstractAnnotatedHandler abstractHandler, int exprIx, Attributes callbackArg) throws SAXException {
        RuntimeTest2 handler = (RuntimeTest2) abstractHandler;

        switch (exprIx) {
        default: throw new HandlerCallError("unhandled call #" + exprIx);
        }
    }

    @Override
    public int getAXSDataVersion() {
        return 65536;
    }

    @Override
    public int getNumberOfCapturingExpressions() {
        return 8;
    }

    @Override
    public int getNumberOfEndExpressions() {
        return 0;
    }

    @Override
    public int getMaximumPredicateStackDepth() {
        return 1;
    }

    private static HashMap<String, int[]> Triggers = null;

    @Override
    public Map<String, int[]> getTriggerTags() {
        synchronized (Lock) {
            if (Triggers != null)
                return Triggers;
            Triggers = new HashMap<String, int[]>();
            Triggers.put("value", 
                    new int[] { 2, 3, 4, 5, });
            Triggers.put("key", 
                    new int[] { 0, 1, 6, 7, });
        }
        return Triggers;
    }

    private static HashSet<String> AttributeCaptureTags = null;

    @Override
    public Set<String> getAttributeCaptureTags() {
        synchronized (Lock) {
            if (AttributeCaptureTags != null)
                return AttributeCaptureTags;

            AttributeCaptureTags = new HashSet<String>();
            AttributeCaptureTags.add("value");
            AttributeCaptureTags.add("key");
        }
        return AttributeCaptureTags;
    }

    private static HashSet<String> PositionCaptureTags = null;

    @Override
    public Set<String> getPositionCaptureTags() {
        synchronized (Lock) {
            if (PositionCaptureTags != null)
                return PositionCaptureTags;

            PositionCaptureTags = new HashSet<String>();
        }
        return PositionCaptureTags;
    }

    private static String[] Literals = new String[] {
        "c",
        "bb",
        "",
        "ab*",
        "bbbbbbbbb",
    };

    private static QName[] QNames = new QName[] {
        new QName("", "value"),
        new QName("http://test.values/ns2", "key"),
        new QName("http://test.values/ns1", "map"),
        new QName("http://test.values/ns0", "value"),
        new QName("", "label"),
    };

    private static XPathExpression[] Expressions = new XPathExpression[] {
        new XPathExpression( // "ns1:map/ns2:key[ends-with(@value, 'c')] | ns1:map/ns2:key[starts-with(@value, 'c')]"
        new short[] {
            XPathExpression.INSTR_ATTRIBUTE, 0,
            XPathExpression.INSTR_LITERAL, 0,
            XPathExpression.INSTR_ENDS_WITH,
            XPathExpression.INSTR_TEST_PREDICATE,
            XPathExpression.INSTR_ELEMENT, 1,
            XPathExpression.INSTR_ELEMENT, 2,
        }, QNames, Literals),
        new XPathExpression( // "ns1:map/ns2:key[ends-with(@value, 'c')] | ns1:map/ns2:key[starts-with(@value, 'c')]"
        new short[] {
            XPathExpression.INSTR_ATTRIBUTE, 0,
            XPathExpression.INSTR_LITERAL, 0,
            XPathExpression.INSTR_STARTS_WITH,
            XPathExpression.INSTR_TEST_PREDICATE,
            XPathExpression.INSTR_ELEMENT, 1,
            XPathExpression.INSTR_ELEMENT, 2,
        }, QNames, Literals),
        new XPathExpression( // "ns2:key[captureattrs()]/value | ns1:map/ns2:key[ends-with(@value, 'bb')]/value | ns2:key[contains(@value, 'bb')]/value"
        new short[] {
            XPathExpression.INSTR_ELEMENT, 3,
            XPathExpression.INSTR_ILITERAL, 1,
            XPathExpression.INSTR_TEST_PREDICATE,
            XPathExpression.INSTR_ELEMENT, 1,
        }, QNames, Literals),
        new XPathExpression( // "ns2:key[captureattrs()]/value | ns1:map/ns2:key[ends-with(@value, 'bb')]/value | ns2:key[contains(@value, 'bb')]/value"
        new short[] {
            XPathExpression.INSTR_ELEMENT, 3,
            XPathExpression.INSTR_ATTRIBUTE, 0,
            XPathExpression.INSTR_LITERAL, 1,
            XPathExpression.INSTR_ENDS_WITH,
            XPathExpression.INSTR_TEST_PREDICATE,
            XPathExpression.INSTR_ELEMENT, 1,
            XPathExpression.INSTR_ELEMENT, 2,
        }, QNames, Literals),
        new XPathExpression( // "ns2:key[captureattrs()]/value | ns1:map/ns2:key[ends-with(@value, 'bb')]/value | ns2:key[contains(@value, 'bb')]/value"
        new short[] {
            XPathExpression.INSTR_ELEMENT, 3,
            XPathExpression.INSTR_ATTRIBUTE, 0,
            XPathExpression.INSTR_LITERAL, 1,
            XPathExpression.INSTR_CONTAINS,
            XPathExpression.INSTR_TEST_PREDICATE,
            XPathExpression.INSTR_ELEMENT, 1,
        }, QNames, Literals),
        new XPathExpression( // "value[@label != '']"
        new short[] {
            XPathExpression.INSTR_ATTRIBUTE, 4,
            XPathExpression.INSTR_LITERAL, 2,
            XPathExpression.INSTR_EQ_STR,
            XPathExpression.INSTR_NOT,
            XPathExpression.INSTR_TEST_PREDICATE,
            XPathExpression.INSTR_ELEMENT, 3,
        }, QNames, Literals),
        new XPathExpression( // "ns2:key[matches(@value, 'ab*')]"
        new short[] {
            XPathExpression.INSTR_ATTRIBUTE, 0,
            XPathExpression.INSTR_LITERAL, 3,
            XPathExpression.INSTR_MATCHES,
            XPathExpression.INSTR_TEST_PREDICATE,
            XPathExpression.INSTR_ELEMENT, 1,
        }, QNames, Literals),
        new XPathExpression( // "ns2:key[starts-with('bbbbbbbbb', @value)]"
        new short[] {
            XPathExpression.INSTR_LITERAL, 4,
            XPathExpression.INSTR_ATTRIBUTE, 0,
            XPathExpression.INSTR_STARTS_WITH,
            XPathExpression.INSTR_TEST_PREDICATE,
            XPathExpression.INSTR_ELEMENT, 1,
        }, QNames, Literals),
    };

    public XPathExpression[] getXPathExpressions() {
        return Expressions;
    }
}
