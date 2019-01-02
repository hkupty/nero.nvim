package nero.controller

import nero.flow._


class State(pipe: Pipe[ReplState, ReplResponse]) extends Pipe[ReplState, ReplState] {

  def updateState(state: ReplState, resp: ReplResponse): ReplState = resp match {
      case ReplResponse.Close => state.withMode(Mode.Closed)
      case ReplResponse.Pending => state.withMode(Mode.Pending(state.mode))
      case _ => {
        // TODO Deal better with normal responses
        val newState = state.mode match {
          case Mode.Pending(mode) => state.withMode(mode)
          case _ => state
        }

        newState.cleanBuffer()
      }
    }

  def send(state: ReplState): ReplState = {
    updateState(state, pipe.send(state))
  }
}
