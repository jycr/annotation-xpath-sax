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

package com.googlecode.axs.tests;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.googlecode.axs.AbstractAnnotatedHandler;
import com.googlecode.axs.QName;
import com.googlecode.axs.XPath;
import com.googlecode.axs.XPathNamespaces;

public class RuntimeTest3 extends AbstractAnnotatedHandler {
	private void expect(String expectation, String value) {
		if (expectation.equals(value)) {
			System.out.println("[OK] expect \"" + expectation + "\"");
		} else {
			System.out.println("[FAIL] expect \"" + expectation + "\", got \"" + value + "\"");
		}
	}
	
	@XPath("entry[@mod = 'moda'][@key = 'keyb']//value[@type='type2']")
	public void testPredicateRetrial(String text) {
		expect("Retrial1", text);
	}

	public static void main(String[] args) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		
		if (! (new File(args[0] + "/testData3.xml")).exists()) {
			System.out.println("Usage: RuntimeTest3 path/to/testData");
			System.exit(1);
		}
		
		try {
			SAXParser parser = factory.newSAXParser();
			
			String testDataRoot = args[0];
			RuntimeTest3 test1 = new RuntimeTest3();
	
			parser.parse(testDataRoot + "/testData3.xml", test1);
		} catch (SAXException e) {
			System.out.println("[FAIL] Got a SAXException: "+ e);
		} catch (IOException e) {
			System.out.println("Usage: RuntimeTest3 path/to/testData");
		} catch (ParserConfigurationException e) {
			System.out.println("[FAIL] Got a ParserConfigurationException: " + e);
		}
	}
}
