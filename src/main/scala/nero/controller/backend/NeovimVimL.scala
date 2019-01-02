package nero.controller.backend

import nero.flow.{Pipe, ReplResponse}
import scala.concurrent.ExecutionContext
import com.ensarsarajcic.neovim.java.corerpc.message._

class NeovimVimL(backend: NeovimBackend)(implicit ec: ExecutionContext) extends Pipe[String, ReplResponse] with NeovimErrorHandler {

  lazy val emptyArgs: Array[String] = new Array(0)

  def execute(cmd: String) = {
    new RequestMessage.Builder("nvim_command_output")
      .addArgument(s"execute '${cmd}'")
      .addArgument(emptyArgs)
  }

  def send(msg: String): ReplResponse = backend.sendToNeovim {
    backend
      .run(execute(msg))
      .recoverWith(alwaysSuccessful)
  }
}
