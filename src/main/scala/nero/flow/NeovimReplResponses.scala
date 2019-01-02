package nero.flow

import scala.util.control.NoStackTrace

case object NeovimResponseOwner extends ResponseOwner

trait NeovimReplResponse extends ReplResponse {
  def owner: ResponseOwner = NeovimResponseOwner
}

case class NeovimError(message: String) extends NoStackTrace with NeovimReplResponse
case class NeovimMessage(message: String) extends NeovimReplResponse
case object NeovimNop extends NeovimReplResponse
