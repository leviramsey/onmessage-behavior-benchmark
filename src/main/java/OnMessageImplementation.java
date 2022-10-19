import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractOnMessageBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

public class OnMessageImplementation extends AbstractOnMessageBehavior<Protocol.Set> {
  public static Behavior<Protocol.Set> create() {
    return Behaviors.setup(context -> new OnMessageImplementation(context));
  }

  @Override
  public Behavior<Protocol.Set> onMessage(Protocol.Set message) {
    ActorRef<Protocol.Response> replyTo = message.replyTo;
    Integer oldValue = value;
    value = message.value;

    if (oldValue == null) {
      replyTo.tell(Protocol.INITIAL_SET);
    } else if (oldValue.equals(value)) {
      replyTo.tell(new Protocol.ValueUnchanged(value));
    } else {
      replyTo.tell(new Protocol.ValueChanged(oldValue, value));
    }

    return this;
  }

  private OnMessageImplementation(ActorContext<Protocol.Set> context) {
    super(context);
  }

  private Integer value;    // nullable
}
