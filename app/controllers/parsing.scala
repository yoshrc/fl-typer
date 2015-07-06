package parsing

import scala.util.parsing.combinator.RegexParsers
import play.api.libs.json._

sealed abstract class Ast {
  def toJsValue: JsValue
  def toJsObject: JsObject = JsObject(Seq("ast" -> toJsValue))
}

case class Const(value: String) extends Ast {
  def toJsValue = JsString(value)
}
case class Var(name: String) extends Ast {
  def toJsValue = JsString(name)
}
case class Nil() extends Ast {
  def toJsValue = JsString("nil")
}
case class Cons(car: Ast, cdr: Ast) extends Ast {
  def toJsValue =
    JsArray(Seq(JsString("::"), car.toJsValue, cdr.toJsValue))
}
case class Abs(variable: Var, body: Ast) extends Ast {
  def toJsValue =
    JsArray(Seq(JsString("λ"), variable.toJsValue, body.toJsValue))
}
case class App(func: Ast, arg: Ast) extends Ast {
  def toJsValue =
    JsArray(Seq(JsString("app"), func.toJsValue, arg.toJsValue))
}
case class If(cond: Ast, thenExpr: Ast, elseExpr: Ast) extends Ast {
  def toJsValue =
    JsArray(Seq(JsString("if"), cond.toJsValue,
                thenExpr.toJsValue, elseExpr.toJsValue))
}
case class Let(variable: Var, binding: Ast, body: Ast) extends Ast {
  def toJsValue =
    JsArray(Seq(JsString("let"), variable.toJsValue, binding.toJsValue,
                body.toJsValue))
}
case class Case(caseExpr: Ast, nilExpr: Ast,
                carPat: Var, cdrPat: Var, consExpr: Ast) extends Ast {
  def toJsValue = {
    val cons = carPat.name + "::" + cdrPat.name
    JsArray(Seq(JsString("case"), caseExpr.toJsValue,
                JsArray(Seq(JsString("nil"), nilExpr.toJsValue)),
                JsArray(Seq(JsString(cons), consExpr.toJsValue))))
  }
}

object FLParser extends RegexParsers {
  def const: Parser[Ast] = """[0-9]+|true|false""".r ^^ { Const(_) }
  def variable: Parser[Var] =
    not(reserved)~>"""[a-zA-Z][a-zA-Z0-9_]*""".r ^^ { Var(_) }
  def nil: Parser[Nil] = "nil" ^^ { _ => Nil() }
  def abs: Parser[Abs] = "\\"~>variable~"."~expr ^^ {
    case variable~sep~body => Abs(variable, body)
  }
  def ifExpr: Parser[If] = "if"~>expr~"then"~expr~"else"~expr ^^ {
    case cond~sep1~thenExpr~sep2~elseExpr => If(cond, thenExpr, elseExpr)
      }
  def let: Parser[Let] = "let"~>variable~"="~expr~"in"~expr ^^ {
    case variable~sep1~binding~sep2~body => Let(variable, binding, body)
  }
  def caseExpr: Parser[Case] =
    "case"~>expr~"of"~
      opt("|")~"nil"~"->"~expr~
      "|"~variable~"::"~variable~"->"~expr ^^ {
        case e~_~_~_~_~nilE~_~v1~_~v2~_~consE =>
        Case(e, nilE, v1, v2, consE)
      }

  def reserved: Parser[String] = """nil|if|then|else|let|in|case|of""".r
  def nonrecExpr: Parser[Ast] =
    const | variable | nil | abs | ifExpr | let | caseExpr

  def expr: Parser[Ast] = maybeCons
  def maybeCons: Parser[Ast] = rep1sep(maybeApp, "::") ^^ { results =>
    results.reduceRight(Cons(_,_))
  }
  def maybeApp: Parser[Ast] = rep1(maybeParen) ^^ { results =>
    results.reduceLeft(App(_,_))
  }
  def maybeParen: Parser[Ast] = nonrecExpr | "("~>expr<~")"

  def parse(input: String) = parseAll(expr, input)
}
