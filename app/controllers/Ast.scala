package ast

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
case object Nil extends Ast {
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

object Const {
  val add = Const("+")
  val sub = Const("-")
  val mul = Const("*")
  val div = Const("/")
  val uminus = Const("-")
  val uplus = Const("+")
  val and = Const("and")
  val or = Const("or")
  val not = Const("not")
  val lt = Const("<")
  val le = Const("<=")
  val intEq = Const("=")
  val ge = Const(">=")
  val gt = Const(">")
}

object Implicits {
  import scala.language.implicitConversions
  import scala.collection.immutable

  implicit def intToConst(x: Int): Const = Const(x.toString)
  implicit def boolToConst(x: Boolean): Const = Const(x.toString)
  implicit def strToVar(x: String): Var = Var(x)
  implicit def listToAst(x: List[Ast]): Ast = x match {
    case immutable.Nil => Nil
    case hd::tl => Cons(hd, listToAst(tl))
  }
  implicit def consToCons(x: immutable.::[Ast]): Cons =
    Cons(x.head, listToAst(x.tail))
  implicit def nilToNil(x: immutable.Nil.type): Nil.type = Nil
}