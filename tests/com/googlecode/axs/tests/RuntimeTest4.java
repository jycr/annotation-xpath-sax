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

public class RuntimeTest4 extends AbstractAnnotatedHandler {
	@XPath("italic//bold")
	public void testPredicateRetrial(String text) {
		if (text.equals("Text 1 and text 2.")) {
			System.out.println("[OK] Matched longest copy.");
		} else if (text.equals("text 2")) {
			System.out.println("[OK] Matched medium copy.");
		} else if (text.equals("2")) {
			System.out.println("[OK] Matched shortest copy.");
		} else {
			System.out.println("[FAIL] Got unexpected text \"" + text + "\"");
		}
	}

	public static void main(String[] args) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		
		if (! (new File(args[0] + "/testData4.xml")).exists()) {
			System.out.println("Usage: RuntimeTest4 path/to/testData");
			System.exit(1);
		}
		
		System.out.println("[INFO] Should get exactly 3 [OK]s.");
		
		try {
			SAXParser parser = factory.newSAXParser();
			
			String testDataRoot = args[0];
			RuntimeTest4 test1 = new RuntimeTest4();
	
			parser.parse(testDataRoot + "/testData4.xml", test1);
		} catch (SAXException e) {
			System.out.println("[FAIL] Got a SAXException: "+ e);
		} catch (IOException e) {
			System.out.println("Usage: RuntimeTest3 path/to/testData");
		} catch (ParserConfigurationException e) {
			System.out.println("[FAIL] Got a ParserConfigurationException: " + e);
		}
	}
}
