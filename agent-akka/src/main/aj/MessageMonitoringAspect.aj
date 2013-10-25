package com.eigengo.monitor.agent.akka;

import com.eigengo.monitor.output.api.CounterInterface;

public aspect MessageMonitoringAspect {

    private static final CounterInterface counterInterface = createCounterInterface();

    private static CounterInterface createCounterInterface() throws ClassCastException{
        try {
            Object o = Class.forName("com.eigengo.monitor.output.statsd.CounterInterfaceImpl").newInstance();
            return (CounterInterface) o;
        } catch (final ReflectiveOperationException e) {
            return new NullCounterInterface();
        }
    }

    pointcut receiveMessage(akka.actor.ActorCell actorCell, Object msg) : target(actorCell) &&
        call(* akka.actor.ActorCell.receiveMessage(..)) && args(msg);

    before(akka.actor.ActorCell actorCell, Object msg): receiveMessage(actorCell, msg) {
        counterInterface.incrementCounter("message."+msg.getClass().getSimpleName());
    }

}