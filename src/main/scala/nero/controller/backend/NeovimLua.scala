package nero.controller.backend

import nero.flow.{Pipe, ReplResponse}
import scala.concurrent.ExecutionContext
import com.ensarsarajcic.neovim.java.corerpc.message._

class NeovimLua(backend: NeovimBackend)(implicit ec: ExecutionContext) extends Pipe[String, ReplResponse] with NeovimErrorHandler {

  lazy val emptyArgs: Array[String] = new Array(0)

  def returnResult(cmd: String) = {
    new RequestMessage.Builder("nvim_execute_lua")
      .addArgument(s"return ${cmd}")
      .addArgument(emptyArgs)
  }

  def execute(cmd: String) = {
    new RequestMessage.Builder("nvim_execute_lua")
      .addArgument(cmd)
      .addArgument(emptyArgs)
  }

  def send(msg: String): ReplResponse = backend.sendToNeovim {
    backend
      .run(returnResult(msg))
      .recoverWith{case _ => backend.run(execute(msg))}
      .recoverWith(alwaysSuccessful)
  }
}
