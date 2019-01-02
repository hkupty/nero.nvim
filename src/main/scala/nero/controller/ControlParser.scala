package nero.controller

import scala.annotation.tailrec
import nero.flow.{Pipe, ReplMessage, ReplState, ReplResponse, Mode}

object ControlParser {
  private val openExprChars = Set('[', '(', '{', '<')
  private val closeExprChars = Set(']', ')', '}', '>')

  @tailrec
  def openExpr(str: String, stack: Int = 0): Int = str match {
    case "" => stack
    case fstr => {
      val a = fstr.head
      val rstr = fstr.tail
      val stacked = if (openExprChars(a)) {
        stack + 1
      } else if (closeExprChars(a)) {
        stack - 1
      } else {
        stack
      }
      openExpr(rstr, stacked)
    }
  }

  def isClosed(expr: Vector[String]): Boolean = {
    val result = expr.map(openExpr(_, 0)).reduce(_+_)
    result == 0
  }

}

class ControlParser(pipe: Pipe[ReplMessage, ReplResponse]) extends Pipe[ReplState, ReplResponse] {

  def send(state: ReplState): ReplResponse = {
    if (!ControlParser.isClosed(state.buffer)){
      ReplResponse.Pending
    } else {

      // TODO Unhack this
      val mode = state.mode match {
        case Mode.Pending(mode) => mode
        case mode => mode
      }
      pipe.send(ReplMessage(state.buffer.mkString("\n"), mode))
    }
  }
}
