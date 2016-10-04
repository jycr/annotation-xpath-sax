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

package com.googlecode.axs;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

/**
 * This class collects all the annotated methods found within a single subclass
 * of AbstractAnnotatedHandler, so that they can be combined into a single _AXSData
 * file.
 * @author Ben
 *
 */
class AnnotatedClass {
	// the class that this object is annotating
	private TypeElement mClassElement;
	private Messager mMessager;

	// maps of method names to XPath expressions
	private HashMap<String, String> mXPathMethods = new HashMap<String, String>();
	private HashMap<String, String> mXPathEndMethods = new HashMap<String, String>();
	private HashMap<String, String> mXPathStartMethods = new HashMap<String, String>();
	
	// maps of method names to Elements
	private HashMap<String, Element> mMethodElements = new HashMap<String, Element>();
	
	// map of prefixes to Namespace URIs
	private HashMap<String, String> mPrefixMap = new HashMap<String, String>();
	
	public AnnotatedClass(Messager messager, TypeElement clazz) {
		mMessager = messager;
		mClassElement = clazz;
		
		mPrefixMap.put("", null);
	}

	public void addMethodAnnotation(Element methodElement, TypeElement annotationElement) {
		final String aType = annotationElement.getSimpleName().toString();
		final String method = methodElement.getSimpleName().toString();
		
		mMethodElements.put(method, methodElement);
		
		if ("XPath".equals(aType)) {
			XPath xp = methodElement.getAnnotation(XPath.class);
			mXPathMethods.put(method, xp.value());
		} else if ("XPathStart".equals(aType)) {
			XPathStart xp = methodElement.getAnnotation(XPathStart.class);
			mXPathStartMethods.put(method, xp.value());
		} else if ("XPathEnd".equals(aType)) {
			XPathEnd xp = methodElement.getAnnotation(XPathEnd.class);
			mXPathEndMethods.put(method, xp.value());
		} else {
			mMessager.printMessage(Kind.ERROR, 
					"Cannot apply annotation " + aType + " to element of type " + methodElement.getKind(), methodElement);
		}
	}
	
	public void addClassAnnotation(TypeElement annotationElement) {
		final String aType = annotationElement.getSimpleName().toString();
		
		if (!"XPathNamespaces".equals(aType)) {
			mMessager.printMessage(Kind.ERROR, "Cannot apply annotation " + aType, mClassElement);
			return;
		}
		XPathNamespaces ns = mClassElement.getAnnotation(XPathNamespaces.class);
		String[] prefixes = ns.value();
		
		for (String p : prefixes) {
			int eqPos = p.indexOf('=');
			
			if (eqPos < 0) {
				mMessager.printMessage(Kind.ERROR, "Namespaces must be of the form \"prefix=URI\"", mClassElement);
				continue;
			}
			String prefix = p.substring(0, eqPos);
			String uri = p.substring(eqPos+1);
			mMessager.printMessage(Kind.NOTE, "Mapping namespace \"" + prefix + "\" to \"" + uri + "\" for class " + className());
			mPrefixMap.put(prefix, uri);
		}
	}

	public Name className() {
		return mClassElement.getQualifiedName();
	}
	
	public TypeElement classElement() {
		return mClassElement;
	}
	
	public Map<String, Element> methodElements() {
		return mMethodElements;
	}
	
	public Map<String, String> xPathMethods() {
		return mXPathMethods;
	}
	
	public Map<String, String> xPathEndMethods() {
		return mXPathEndMethods;
	}
	
	public Map<String, String> xPathStartMethods() {
		return mXPathStartMethods;
	}
	
	public Map<String, String> prefixMap() {
		return mPrefixMap;
	}
}
