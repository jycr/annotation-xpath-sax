package com.googlecode.axs.xpath;

public final class XPathNode extends SimpleNode {

	public XPathNode(int i) {
		super(i);
	}

	public XPathNode(Parser p, int i) {
		super(p, i);
	}

	public int getNodeType() { return id; }
	
	public XPathNode jjtGetChild(int c) {
		return (XPathNode)super.jjtGetChild(c);
	}
	
	public XPathNode jjtGetParent() {
		return (XPathNode)super.jjtGetParent();
	}
}
