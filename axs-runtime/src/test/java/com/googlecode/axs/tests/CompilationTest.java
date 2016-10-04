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

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
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
	
	@BeforeClass
	public static void main(String[] args) {
		CompilationTest t = new CompilationTest();
		assertNotNull("Instantiated a CompilationTest object", t);
		System.out.println("[OK] Instantiated a CompilationTest object\n");
	}
}
