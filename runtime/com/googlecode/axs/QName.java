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

import javax.xml.XMLConstants;

/**
 * This is a clone of javax.xml.namespace.QName,
 * which we embed for compatibility with older versions of Android.
 */

public class QName {
    private final String mNamespaceURI;
    private final String mLocalPart;

    public QName(String namespaceURI, String localPart) {
        if (namespaceURI == null) {
            mNamespaceURI = XMLConstants.NULL_NS_URI;
        } else {
            mNamespaceURI = namespaceURI;
        }

        mLocalPart = localPart;
    }

    public QName(String localPart) {
        this(
            XMLConstants.NULL_NS_URI,
            localPart);
    }

    public String getNamespaceURI() {
        return mNamespaceURI;
    }

    public String getLocalPart() {
        return mLocalPart;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other instanceof QName) {
            QName qName = (QName) other;
            return mLocalPart.equals(qName.mLocalPart) && mNamespaceURI.equals(qName.mNamespaceURI);
        }
        return false;
    }

    public final int hashCode() {
        return mNamespaceURI.hashCode() ^ mLocalPart.hashCode();
    }

    public String toString() {
        if (mNamespaceURI.length() == 0) {
            return mLocalPart;
        }
        
        return "{" + mNamespaceURI +"}" + mLocalPart;
    }
}
