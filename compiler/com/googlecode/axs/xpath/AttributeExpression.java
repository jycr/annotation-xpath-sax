/* Generated By:JJTree: Do not edit this line. AttributeExpression.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.googlecode.axs.xpath;

public
class AttributeExpression extends SimpleNode {
  public AttributeExpression(int id) {
    super(id);
  }

  public AttributeExpression(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(ParserVisitor visitor, com.googlecode.axs.ShortVector data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=11845895838f441ba135a5fc4f06f950 (do not edit this line) */
