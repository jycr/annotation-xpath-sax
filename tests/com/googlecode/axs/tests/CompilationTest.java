package com.googlecode.axs.tests;

import org.xml.sax.Attributes;

import com.googlecode.axs.AbstractAnnotatedHandler;
import com.googlecode.axs.XPath;
import com.googlecode.axs.XPathEnd;
import com.googlecode.axs.XPathNamespaces;
import com.googlecode.axs.XPathStart;

@XPathNamespaces({
	"=http://foo.bar/NS1",
	"baz=http://baz.blah/NS2",
	"grep=mailto:grep.is.not.awk"
})
public class CompilationTest extends AbstractAnnotatedHandler {
	public CompilationTest() {
		super();
	}

	@XPath("foo/ (: not baz :) bar")
	public void barElement(String text) {
		
	}
	
	@XPath("/grep[captureattrs()]//foo[2]")
	public void fooElement2(String text) {
		
	}
	
	@XPathStart("foo/baz/blah/blah")
	public void blahElement2(Attributes attrs) {
		
	}
	
	@XPathStart("/baz//blah")
	public void blahElement(Attributes attrs) {
		
	}
	
	@XPathEnd("grep[@is = 'awk']/not")
	public void notElementEnd() {
		
	}
	
	@XPathEnd("/grep[@is != 'awk']//awk[2]")
	public void awkElementEnd() {
		
	}
	
	public static void main(String[] args) {
		CompilationTest t = new CompilationTest();
		
		System.out.println("[OK] Instantiated a CompilationTest object\n");
	}
}
