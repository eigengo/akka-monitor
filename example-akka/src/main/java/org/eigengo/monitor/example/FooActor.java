package org.eigengo.monitor.example;


import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import scala.collection.immutable.List;

public class FooActor extends UntypedActor {

    int longSleep = 10;
    final ActorRef bar;

    FooActor(ActorRef baz) {
        this.bar = baz;
    }


    public void onReceive(Object o) {
        try {
            receiveInt(Integer.valueOf(o.toString()));
        } catch (Exception e) {
            // and ignore it
        }
    }

    public void receiveInt(int i) throws InterruptedException {
        if (i > 0) {
            System.out.println("Counting down... Now "+ i);
            Thread.sleep(longSleep);
            if (i % 10 == 0) {bar.tell(i, self());}
            this.self().tell(i - 1, self());
        } else if (i == 0) {
            System.out.println("Foo done.");
        }
    }
}
