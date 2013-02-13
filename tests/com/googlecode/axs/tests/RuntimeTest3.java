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
	
	@XPath("entry[@key = 'keyb']//value[@type='type2']")
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
