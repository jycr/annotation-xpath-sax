/* Generated By:JJTree: Do not edit this line. NameValue.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.googlecode.axs.xpath;

public
class NameValue extends SimpleNode {
  public NameValue(int id) {
    super(id);
  }

  public NameValue(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(ParserVisitor visitor, com.googlecode.axs.ShortVector data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=3cb0300af68ed39663634d217b621085 (do not edit this line) */