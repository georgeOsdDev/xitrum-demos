package demos.action

import akka.actor.Actor
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}

import xitrum.{SockJsActor, SockJsText, WebSocketActor, WebSocketText, WebSocketBinary, WebSocketPing, WebSocketPong}
import xitrum.annotation.{GET, SOCKJS, WEBSOCKET}

@GET("sockJsChatDemo")
class SockJsChat extends AppAction {
  def execute() {
    respondView()
  }
}

@GET("websocketChatDemo")
class WebSocketChat extends AppAction {
  def execute() {
    respondView()
  }
}

//------------------------------------------------------------------------------

trait PubSub {
  this: Actor =>

  private val TOPIC = "chat"

  protected val mediator = DistributedPubSubExtension(context.system).mediator

  override def postStop() {
    // Actors are automatically removed from pubsub when they are terminated
  }

  protected def pub(msg: String) {
    mediator ! DistributedPubSubMediator.Publish(TOPIC, msg)
  }

  protected def sub() {
    mediator ! DistributedPubSubMediator.Subscribe(TOPIC, self)
  }
}

@SOCKJS("sockJsChat")
class SockJsChatActor extends SockJsActor with PubSub {
  def execute() {
    sub()
    context.become {
      case msgFromPubSub: String =>
        respondSockJsText(msgFromPubSub)

      case SockJsText(msgFromBrowser) =>
        pub(msgFromBrowser)
    }
  }
}

@WEBSOCKET("websocketChat")
class WebSocketChatActor extends WebSocketActor with PubSub {
  def execute() {
    sub()
    context.become {
      case msgFromPubSub: String =>
        respondWebSocketText(msgFromPubSub)

      case WebSocketText(msgFromBrowser) =>
        pub(msgFromBrowser)

      case WebSocketBinary(bytes) =>
      case WebSocketPing =>
      case WebSocketPong =>
    }
  }
}
