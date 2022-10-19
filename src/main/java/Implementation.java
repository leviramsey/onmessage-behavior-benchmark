import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.ReceiveBuilder;

public class Implementation extends AbstractBehavior<Protocol.Set> {
  public static Behavior<Protocol.Set> create() {
    return Behaviors.setup(context -> new Implementation(context));
  }

  @Override
  public Receive<Protocol.Set> createReceive() {
    ReceiveBuilder<Protocol.Set> builder = newReceiveBuilder();

    builder.onMessage(Protocol.Set.class, this::onSet);

    return builder.build();
  }

  private Behavior<Protocol.Set> onSet(Protocol.Set setCommand) {
    ActorRef<Protocol.Response> replyTo = setCommand.replyTo;
    Integer oldValue = value;
    value = setCommand.value;

    if (oldValue == null) {
      replyTo.tell(Protocol.INITIAL_SET);
    } else if (oldValue.equals(value)) {
      replyTo.tell(new Protocol.ValueUnchanged(value));
    } else {
      replyTo.tell(new Protocol.ValueChanged(oldValue, value));
    }

    return this;
  }

  private Implementation(ActorContext<Protocol.Set> context) {
    super(context);
  }

  private Integer value;    // nullable
}
