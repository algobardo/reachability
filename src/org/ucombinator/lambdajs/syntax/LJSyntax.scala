package org.ucombinator.lambdajs.syntax

import util.parsing.input.Positional

/**
 * @author ilya
 */

trait LJSyntax {

  type Addr

  type Label = String

  /**
   * Variable environments
   */
  type Env = Map[Var, Addr]

  object Exp {
    private var maxSerialNumber = 0

    def allocateSerialNumber(): Long = {
      maxSerialNumber += 1
      maxSerialNumber
    }
  }

  sealed abstract class Exp extends Positional {
    def isValue: Boolean = false
  }

  sealed abstract class StampedExp extends Exp {
    val stamp: Int
  }

  /*
   *  Value-like expressions
   */

  case class Var(name: String, stamp: Int) extends StampedExp

  case class EString(s: String) extends Exp {
    override def isValue = true
  }

  case class EAddr(a: Addr) extends Exp {
    override def isValue = true
  }

  case class ENum(n: Long) extends Exp {
    override def isValue = true
  }

  case class EBool(b: Boolean) extends Exp {
    override def isValue = true
  }

  case object EUndef extends Exp {
    override def isValue = true
  }

  case object ENull extends Exp {
    override def isValue = true
  }

  case class Fun(params: List[Var], body: Exp, stamp: Int) extends StampedExp

  /*
   * Other LambdaJS expressions
   */
  case class Record(entries: List[(StringValue, Exp)]) extends Exp {
    override def isValue = entries.foldLeft(true) {
      case (result, (s, v)) => result && v.isValue
    }
  }

  case class Let(x: Var, rhs: Exp, body: Exp, stamp: Int) extends StampedExp

  case class App(fun: Exp, args: List[Exp], stamp: Int) extends StampedExp

  case class Lookup(rec: Exp, index: Exp, stamp: Int) extends StampedExp

  case class Update(rec: Exp, index: Exp, rhs: Exp, stamp: Int) extends StampedExp

  case class Del(rec: Exp, index: Exp, stamp: Int) extends StampedExp

  case class Asgn(lhs: Exp, rhs: Exp, stamp: Int) extends StampedExp

  case class Ref(e: Exp, stamp: Int) extends StampedExp

  case class Deref(e: Exp, stamp: Int) extends StampedExp

  case class If(cond: Exp, tb: Exp, eb: Exp, stamp: Int) extends StampedExp

  case class Seq(fst: Exp, snd: Exp, stamp: Int) extends StampedExp

  case class While(cond: Exp, body: Exp, stamp: Int) extends StampedExp

  case class Labelled(lab: Label, e: Exp, stamp: Int) extends StampedExp

  case class Break(lab: Label, e: Exp, stamp: Int) extends StampedExp

  case class TryCatch(e: Exp, x: Var, rest: Exp, stamp: Int) extends StampedExp

  case class TryFinally(e: Exp, rest: Exp, stamp: Int) extends StampedExp

  case class Throw(e: Exp, stamp: Int) extends StampedExp

  case class OpApp(op: String, args: List[Exp], stamp: Int) extends StampedExp

  /**
   * Values
   */

  sealed abstract class Value

  abstract class AbstractStringValue extends Value

  case class StringValue(s: String) extends AbstractStringValue

  case object StringTop extends AbstractStringValue

  case class BoolValue(b: Boolean) extends Value

  abstract class AbstractNumValue extends Value

  case class NumValue(n: Long) extends AbstractNumValue

  case object NumTopValue extends AbstractNumValue

  def mkNumValue(n: Long): AbstractNumValue = {
    if (n > 2) {
      NumTopValue
    } else if (n < -2) {
      NumTopValue
    } else {
      NumValue(n)
    }
  }

  case class AddrValue(a: Addr) extends Value

  case object UndefValue extends Value

  case object NullValue extends Value

  case class RecValue(entries: List[(StringValue, Value)]) extends Value

  case class FunValue(fun: Fun, env: Env) extends Value

  /*
  * Closures
  */

  sealed abstract class Closure

  case class GroundClo(e: Exp, env: Env) extends Closure

  case class RecordClo(entries: List[(StringValue, Closure)]) extends Closure

  case class LetClo(x: Var, rhs: Closure, body: Closure) extends Closure

  case class AppClo(fun: Closure, args: List[Closure]) extends Closure

  case class LookupClo(rec: Closure, index: Closure) extends Closure

  case class UpdateClo(rec: Closure, index: Closure, rhs: Closure) extends Closure

  case class DelClo(rec: Closure, index: Closure) extends Closure

  case class AsgnClo(lhs: Closure, rhs: Closure) extends Closure

  case class RefClo(e: Closure) extends Closure

  case class DerefClo(e: Closure) extends Closure

  case class IfClo(cond: Closure, tb: Closure, eb: Closure) extends Closure

  case class SeqClo(fst: Closure, snd: Closure) extends Closure

  case class WhileClo(cond: Closure, body: Closure) extends Closure

  case class LabelledClo(lab: Label, e: Closure) extends Closure

  case class BreakClo(lab: Label, e: Closure) extends Closure

  case class TryCatchClo(e: Closure, x: Var, rest: Closure) extends Closure

  case class TryFinallyClo(e: Closure, rest: Closure) extends Closure

  case class ThrowClo(e: Closure) extends Closure

  case class OpClo(op: String, args: List[Closure]) extends Closure

  case class ValueClo(v: Value) extends Closure

  /***************************************
   * Potential redeces
   ***************************************/

  sealed abstract class PotentialRedex

  case class PR_VAR(v: Var, env: Env) extends PotentialRedex

  case class PR_APP(v: Value, args: List[Value]) extends PotentialRedex

  case class PR_LET(x: Var, v: Value, clo: Closure) extends PotentialRedex

  case class PR_REC_REF(v: Value, s: AbstractStringValue) extends PotentialRedex

  case class PR_REC_SET(v: Value, s: AbstractStringValue, v2: Value) extends PotentialRedex

  case class PR_REC_DEL(v: Value, s: AbstractStringValue) extends PotentialRedex

  case class PR_IF(v: Value, tb: Exp, eb: Exp, e: Env) extends PotentialRedex

  case class PR_OP(op: String, vs: List[Value]) extends PotentialRedex

  case class PR_SET(v: Value, v1: Value) extends PotentialRedex

  case class PR_REF(v: Value) extends PotentialRedex

  case class PR_ASGN(a: Value, v: Value) extends PotentialRedex

  case class PR_DEREF(v: Value) extends PotentialRedex

  case class PR_THROW(v: Value) extends PotentialRedex

  case class PR_BREAK(l: Label, v: Value) extends PotentialRedex


  /***************************************
   * Utility functions
   ***************************************/

  /**
   * Convert an expression to closure
   */
  def exp2Clo(expr: Exp, r: Env): Closure = {
    def toGround(exp: Exp) = GroundClo(exp, r)

    expr match {
      case v if v.isValue => ValueClo(exp2Value(v))
      case Record(entries) => RecordClo(entries.map {
        case (s, t) => (s, toGround(t))
      })
      case f@Fun(_, _, _) => toGround(f)
      case x@Var(_, _) => toGround(x)
      case Let(x, rhs, b, _) => LetClo(x, toGround(rhs), toGround(b))
      case App(fun: Exp, args: List[Exp], _) => AppClo(toGround(fun), args.map(toGround))
      case Lookup(rec: Exp, index: Exp, _) => LookupClo(toGround(rec), toGround(index))
      case Update(rec: Exp, index: Exp, rhs: Exp, _) => UpdateClo(toGround(rec), toGround(index), toGround(rhs))
      case Del(rec: Exp, index: Exp, _) => DelClo(toGround(rec), toGround(index))
      case Asgn(lhs: Exp, rhs: Exp, _) => AsgnClo(toGround(lhs), toGround(rhs))
      case Ref(e: Exp, _) => RefClo(toGround(e))
      case Deref(e: Exp, _) => DerefClo(toGround(e))
      case If(cond: Exp, tb: Exp, eb: Exp, _) => IfClo(toGround(cond), toGround(tb), toGround(eb))
      case Seq(fst: Exp, snd: Exp, _) => SeqClo(toGround(fst), toGround(snd))
      case While(cond: Exp, body: Exp, _) => WhileClo(toGround(cond), toGround(body))
      case Labelled(lab: Label, e: Exp, _) => LabelledClo(lab, toGround(e))
      case Break(lab: Label, e: Exp, _) => BreakClo(lab, toGround(e))
      case TryCatch(e: Exp, x: Var, rest: Exp, _) => TryCatchClo(toGround(e), x, toGround(rest))
      case TryFinally(e: Exp, rest: Exp, _) => TryFinallyClo(toGround(e), toGround(rest))
      case Throw(e: Exp, _) => ThrowClo(toGround(e))
      case OpApp(op: String, args: List[Exp], _) => OpClo(op, args.map(toGround))
      case x => throw new Exception("Cannot convert the expression " + x.toString + " to a closure.")
    }
  }

  /**
   * Convert an expression to value
   */
  def exp2Value(e: Exp): Value = e match {
    case EString(s) => StringValue(s)
    case EAddr(a) => AddrValue(a)
    case ENum(n) => mkNumValue(n)
    case EBool(b) => BoolValue(b)
    case EUndef => UndefValue
    case ENull => NullValue
    case r@Record(entries) if r.isValue => RecValue(entries.map {
      case (s, ee) => (s, exp2Value(ee))
    })
    case x => throw new Exception("Not value term: " + e.toString)
  }

}

