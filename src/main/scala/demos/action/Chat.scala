package demos.action

import akka.actor.{Actor, ActorRef, Props}
import xitrum.{Config, Logger, SockJsActor, SockJsText, WebSocketActor, WebSocketText, WebSocketBinary, WebSocketPing, WebSocketPong}
import xitrum.annotation.{GET, SOCKJS, WEBSOCKET}
import xitrum.mq.{MessageQueue, QueueMessage}
import demos.util.LeoFS

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

@GET("websocketImageChatDemo")
class WebSocketImageChat extends AppAction {
  def execute() {
    respondView()
  }
}

case class MsgsFromQueue(msgs: Seq[String])

trait MessageQueueListener {
  this: Actor =>

  protected val TOPIC = "chat"

  protected val listener = (messages: Seq[QueueMessage]) => {
    val msgs = messages.map(_.body.toString)
    self ! MsgsFromQueue(msgs)

    // Tell MessageQueue not to unsubscribe this listener
    false
  }

  override def postStop() {
    MessageQueue.unsubscribe(TOPIC, listener)
  }
}

@SOCKJS("sockJsChat")
class SockJsChatActor extends SockJsActor with MessageQueueListener {
  def execute() {
    MessageQueue.subscribe(TOPIC, listener, 0)
    context.become {
      case MsgsFromQueue(msgs) =>
        msgs.foreach { msg => respondSockJsText(msg) }

      case SockJsText(text) =>
        MessageQueue.publish(TOPIC, text)
    }
  }
}

@WEBSOCKET("websocketChat")
class WebSocketChatActor extends WebSocketActor with MessageQueueListener {
  def execute() {
    MessageQueue.subscribe(TOPIC, listener, 0)
    context.become {
      case MsgsFromQueue(msgs) =>
        msgs.foreach { msg => respondWebSocketText(msg) }

      case WebSocketText(text) =>
        MessageQueue.publish(TOPIC, text)

      case WebSocketBinary(bytes) =>
      case WebSocketPing =>
      case WebSocketPong =>
    }
  }
}

case class Publish(msg:String);
case class Subscribe(num:Int);

@WEBSOCKET("websocketImageChat")
class WebSocketImageChatActor extends WebSocketActor{
  val manager = MessageQueManager.getManager
  def execute() {

    manager ! Subscribe(10) // read latest 10
    context.become {
      case MsgsFromQueue(msgs) =>
        msgs.foreach { msg => respondWebSocketText(msg) }
      case WebSocketText(text) =>
        manager ! Publish(text)
      case WebSocketBinary(bytes) =>
      case WebSocketPing =>
      case WebSocketPong =>
    }
  }
}

object MessageQueManager{
  val manager = Config.actorSystem.actorOf(Props[MessageQueManager], "manager")
  def getManager:ActorRef ={
    manager
  }
}

class MessageQueManager extends Actor with Logger{
  var clients = Seq[ActorRef]()
  def receive = {
    case pub: Publish =>
      LeoFS.save((System.currentTimeMillis()).toString,pub.msg)
      clients.foreach(_ ! MsgsFromQueue(Seq(pub.msg)))
    case sub: Subscribe =>
        clients = clients :+ sender
        val messages = LeoFS.readHead(sub.num)
        sender ! MsgsFromQueue(messages)
    case _ =>
      logger.error("unexpected message")
  }
}
