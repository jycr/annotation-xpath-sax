package com.googlecode.axs;

/**
 * This is thrown if the XPath expression evaluator encounters an error. It should
 * never happen.
 * @author Ben
 *
 */
public class XPathExecutionError extends Error {
	private static final long serialVersionUID = 8766724750974708974L;

	public XPathExecutionError() {
	}

	public XPathExecutionError(String message) {
		super(message);
	}

	public XPathExecutionError(Throwable cause) {
		super(cause);
	}

	public XPathExecutionError(String message, Throwable cause) {
		super(message, cause);
	}

}
