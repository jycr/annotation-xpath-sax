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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;

/**
 * This class provides the compiler processing needed to support the
 * annotations, specifically the generation of the _AXSData class
 * containing the compiled XPath expressions needed for a given 
 * AbstractAnnotatedHandler subclass
 * @author Ben
 *
 */
@SupportedAnnotationTypes({
	"com.googlecode.axs.XPath",
	"com.googlecode.axs.XPathStart",
	"com.googlecode.axs.XPathEnd",
	"com.googlecode.axs.XPathNamespaces"
})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions("axs.nogenerated")
public class AnnotationProcessor extends AbstractProcessor {
	// accumulate all the classes we need to generate _AXSData classes for
	private HashMap<TypeElement, AnnotatedClass> mClasses = new HashMap<TypeElement, AnnotatedClass>();
	private int mAnnotatedMethods = 0;
	
	public AnnotationProcessor() {
		super();
	}
	
	private TypeElement enclosingClassOf(Element elem) {
		while (true) {
			if (elem.getKind() == ElementKind.CLASS)
				return (TypeElement) elem;
			elem = elem.getEnclosingElement();
		}
	}

	@Override
	public boolean process(Set<? extends TypeElement> elements,
			RoundEnvironment env) {
		
		final Filer filer = processingEnv.getFiler();
		final Messager messager = processingEnv.getMessager();

		// collect all the annotations for each user class into
		// a coherent set
		for (TypeElement annotationElement : elements) {
			for (Element e : env.getElementsAnnotatedWith(annotationElement)) {
				TypeElement classElement = enclosingClassOf(e);
				
				AnnotatedClass ac = mClasses.get(classElement);
				if (ac == null) {
					ac = new AnnotatedClass(messager, classElement);
					mClasses.put(classElement, ac);
				}
				if (e.getKind() == ElementKind.METHOD) {
					ac.addMethodAnnotation(e, annotationElement);
					mAnnotatedMethods += 1;
				} else if (e.getKind() == ElementKind.CLASS) {
					ac.addClassAnnotation(annotationElement);
				} else {
					messager.printMessage(Kind.ERROR, "Invalid location for annotation " + annotationElement.getSimpleName(), e);
				}
			}
		}
		
		// only write out the _AXSData classes on the final annotation processing round
		if (!env.processingOver())
			return true;
		
		messager.printMessage(Kind.NOTE, "Found " + mClasses.size() +
				(mClasses.size() > 1 ? " classes" : " class") + 
				" with XPath annotations on " + mAnnotatedMethods +
				(mAnnotatedMethods == 1 ? " method" : " methods"));
		
		// compile the XPath expressions for each class and write
		// out the _AXSData for it
		for (AnnotatedClass ac : mClasses.values()) {
			CompiledAXSData axsData = new CompiledAXSData(ac, messager);
			axsData.compile();
			
			String filename = ac.className() + "_AXSData";
			try {
				FileObject axsDataFile = filer.createSourceFile(filename,
												ac.classElement());
				BufferedWriter writer = new BufferedWriter(axsDataFile.openWriter());
				
				if (processingEnv.getOptions().containsKey("axs.nogenerated"))
					AXSDataWriter.setUseGeneratedAnnotation(false);
				
				AXSDataWriter.writeAXSData(writer, axsData);
				writer.close();
			} catch (IOException e) {
				messager.printMessage(Kind.ERROR, "Error writing " + filename + 
								": " + e.toString());
			}
		}
		return true;
	}

}
