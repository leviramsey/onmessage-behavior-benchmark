import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.ReceiveBuilder;

public class Implementation extends AbstractBehavior<Protocol.Command> {
  public static Behavior<Protocol.Command> create() {
    return Behaviors.setup(context -> new Implementation(context));
  }

  @Override
  public Receive<Protocol.Command> createReceive() {
    ReceiveBuilder<Protocol.Command> builder = newReceiveBuilder();

    builder
      .onMessage(Protocol.Set.class, this::onSet)
      .onMessage(Protocol.Get.class, this::onGet);

    return builder.build();
  }

  private Behavior<Protocol.Command> onSet(Protocol.Set setCommand) {
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

  private Behavior<Protocol.Command> onGet(Protocol.Get getCommand) {
    ActorRef<Protocol.Response> replyTo = getCommand.replyTo;

    if (value == null) {
      replyTo.tell(Protocol.NOT_YET_SET);
    } else {
      replyTo.tell(new Protocol.ValueUnchanged(value));
    }

    return this;
  }

  private Implementation(ActorContext<Protocol.Command> context) {
    super(context);
  }

  private Integer value;    // nullable
}
