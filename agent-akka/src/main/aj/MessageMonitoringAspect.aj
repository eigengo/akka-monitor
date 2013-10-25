package com.eigengo.monitor.agent.akka;

import com.eigengo.monitor.output.api.CounterInterface;

public aspect MessageMonitoringAspect {

    pointcut receiveMessage(akka.actor.ActorCell actorCell, Object msg) : target(actorCell) &&
        call(* akka.actor.ActorCell.receiveMessage(..)) && args(msg);

    before(akka.actor.ActorCell actorCell, Object msg): receiveMessage(actorCell, msg) {
        CounterInterface.incrementCounter("actor.message.received");
    }

}