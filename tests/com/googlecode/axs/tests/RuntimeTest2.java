package com.googlecode.axs.tests;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.googlecode.axs.AbstractAnnotatedHandler;
import com.googlecode.axs.XPath;
import com.googlecode.axs.XPathNamespaces;

@XPathNamespaces({ "ns1=http://test.values/ns1", "ns2=http://test.values/ns2", "=http://test.values/ns0" })
public class RuntimeTest2 extends AbstractAnnotatedHandler {
	private void expect(String expectation, String value) {
		if (expectation.equals(value)) {
			System.out.println("[OK] expect \"" + expectation + "\"");
		} else {
			System.out.println("[FAIL] expect \"" + expectation + "\", got \"" + value + "\"");
		}
	}
	
	@XPath("ns2:key[captureattrs()]/value " +
			"| ns1:map/ns2:key[ends-with(@value, 'bb')]/value " +
			"| ns2:key[contains(@value, 'bb')]/value")
	void testAttributeStack(String text) {
		Map<QName, String> attrs = attributesAtDepth(tagDepth() - 2);
		String key = attrs.get(new QName("value"));
		
		if (key.equals("abbb"))
			expect("Value 10", text);
		else if (key.equals("bbbb"))
			expect("Value 11", text);
		else
			System.out.println("[FAIL] unexpected key \"" + key + "\"");
	}
	
	@XPath("ns1:map/ns2:key[ends-with(@value, 'c')] | ns1:map/ns2:key[starts-with(@value, 'c')]")
	void neverHappen(String text) {
		System.out.println("[FAIL] pattern matched that shouldn't");
	}
	
	@XPath("ns2:key[starts-with('bbbbbbbbb', @value)]")
	void testStartsWith(String text) {
		expect("Value 11", text);
	}
	
	@XPath("value[@label != '']")
	void testEmpty(String text) {
		expect("", text);
	}
	
	@XPath("ns2:key[matches(@value, 'ab*')]")
	void testRegexp(String text) {
		expect("Value 10", text);
	}

	public static void main(String[] args) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		
		if (! (new File(args[0] + "/testData2.xml")).exists()) {
			System.out.println("Usage: RuntimeTest1 path/to/testData");
			System.exit(1);
		}
		
		try {
			SAXParser parser = factory.newSAXParser();
			
			String testDataRoot = args[0];
			RuntimeTest2 test1 = new RuntimeTest2();
	
			parser.parse(testDataRoot + "/testData2.xml", test1);
		} catch (SAXException e) {
			System.out.println("[FAIL] Got a SAXException: "+ e);
		} catch (IOException e) {
			System.out.println("Usage: RuntimeTest1 path/to/testData");
		} catch (ParserConfigurationException e) {
			System.out.println("[FAIL] Got a ParserConfigurationException: " + e);
		}
	}
}
