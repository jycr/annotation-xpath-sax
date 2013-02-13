package com.googlecode.axs;

import javax.xml.XMLConstants;

/**
 * This is a clone javax.xml.namespace.QName,
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
