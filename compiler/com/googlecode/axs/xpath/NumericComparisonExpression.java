/* Generated By:JJTree: Do not edit this line. NumericComparisonExpression.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.googlecode.axs.xpath;

public
class NumericComparisonExpression extends SimpleNode {
  public NumericComparisonExpression(int id) {
    super(id);
  }

  public NumericComparisonExpression(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(ParserVisitor visitor, com.googlecode.axs.ShortVector data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=d9cb7aea3f698aae67865cd1940ebd30 (do not edit this line) */