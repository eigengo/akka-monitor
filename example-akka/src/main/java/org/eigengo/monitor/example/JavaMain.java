package org.eigengo.monitor.example;

import akka.actor.*;
import akka.routing.RoundRobinRouter;
import akka.routing.RouterConfig;
import org.eigengo.monitor.example.akka.ListMaker;
import scala.collection.immutable.List;
import scala.util.matching.Regex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JavaMain {

    static ActorSystem system = ActorSystem.create("example");
    static Props barProps = Props.create(BarActor.class, List.empty());
    static RouterConfig barConfig = new RoundRobinRouter(10);
    static ActorRef bar = system.actorOf(barProps.withRouter(barConfig), "bar");
    static ActorRef foo = system.actorOf(Props.create(FooActor.class, new ListMaker(bar).toSomeSeq()), "foo");
    static Regex CountPattern = new Regex("(\\d+)", new ListMaker("number").toStringSeq());
    static boolean keepGoing;

        public static void main(String[] args) {

            keepGoing = true;
            String[] withStuff = new String[1];
            withStuff[0] = "";
            while (keepGoing) {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

                try {
                    withStuff = br.readLine().split("\\s");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                doStuff(withStuff[0]);
            }
        }

    private static void doStuff(String arg) {
        if (arg.equals("quit")) {
            keepGoing = false;
            system.shutdown();
            System.exit(0);
        } else if (arg.equals("go")) {
            for (int i = 0; i <= 20; i++) {
                foo.tell(400, ActorRef.noSender());
            }
        } else if (CountPattern.findFirstIn(arg).equals(scala.Option.apply(arg))) {
            foo.tell(Integer.valueOf(arg) * 10, ActorRef.noSender());
        } else {
            System.out.println("nope");
        }

    }


}
