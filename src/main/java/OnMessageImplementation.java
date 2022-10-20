import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractOnMessageBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

public class OnMessageImplementation extends AbstractOnMessageBehavior<Protocol.Command> {
  public static Behavior<Protocol.Command> create() {
    return Behaviors.setup(context -> new OnMessageImplementation(context));
  }

  @Override
  public Behavior<Protocol.Command> onMessage(Protocol.Command message) {
    // pre-Java 17 style: instanceof and cast
    if (message instanceof Protocol.Set) {
      Protocol.Set setMessage = (Protocol.Set)message;
      ActorRef<Protocol.Response> replyTo = setMessage.replyTo;
      Integer oldValue = value;
      value = setMessage.value;

      if (oldValue == null) {
        replyTo.tell(Protocol.INITIAL_SET);
      } else if (oldValue.equals(value)) {
        replyTo.tell(new Protocol.ValueUnchanged(value));
      } else {
        replyTo.tell(new Protocol.ValueChanged(oldValue, value));
      }
    } else if (message instanceof Protocol.Get) {
      ActorRef<Protocol.Response> replyTo = ((Protocol.Get)message).replyTo;

      if (value == null) {
        replyTo.tell(Protocol.NOT_YET_SET);
      } else {
        replyTo.tell(new Protocol.ValueUnchanged(value));
      }
    }

    return this;
  }

  private OnMessageImplementation(ActorContext<Protocol.Command> context) {
    super(context);
  }

  private Integer value;    // nullable
}
