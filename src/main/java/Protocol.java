import akka.actor.typed.ActorRef;

public class Protocol {
  /** The command from the outside */
  public static class Set {
    public Integer value;
    public ActorRef<Response> replyTo;

    public Set(Integer value, ActorRef<Response> replyTo) {
      this.value = value;
      this.replyTo = replyTo;
    }
  }

  public interface Response {}

  public static Response INITIAL_SET = new Response() {};

  public static class ValueUnchanged implements Response {
    public Integer value;

    public ValueUnchanged(Integer value) {
      this.value = value;
    }
  }

  public static class ValueChanged implements Response {
    public Integer from;
    public Integer to;

    public ValueChanged(Integer from, Integer to) {
      this.from = from;
      this.to = to;
    }
  }
}
