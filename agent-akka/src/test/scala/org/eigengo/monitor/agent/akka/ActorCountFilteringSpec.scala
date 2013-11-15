package org.eigengo.monitor.agent.akka

import org.eigengo.monitor.{TestCounter, TestCounterInterface}
import akka.actor.Props

class ActorCountFilteringSpec extends ActorCellMonitoringAspectSpec(Some("count-filtering.conf")) {
  sequential
  import Aspects._

  "Actor count monitoring" should {

    // records the count of actors, grouped by simple class name
    "Not record the count of exluded actors" in {
      val parent = "parent.akka://default/user"
      val path = "path.akka://default/user/simple"
      val props = "props.class org.eigengo.monitor.agent.akka.SimpleActor"
      val className = "className.org.eigengo.monitor.agent.akka.SimpleActor"
      val tags = List(parent, path, props, className)
      TestCounterInterface.clear()

      val monitoredActor = system.actorOf(Props[SimpleActor], "simple")

      // this actor is excluded -- we shouldn't be monitoring it
      val unmonitoredActor = system.actorOf(Props[KillableActor], "killable")

      Thread.sleep(1000)

      val counterBeforeKill = TestCounterInterface.foldlByAspect(actorCount)((a,_) =>a)
      counterBeforeKill.size === 1
      counterBeforeKill must contain(TestCounter(actorCount, 1, tags))

      monitoredActor ! 'stop
      unmonitoredActor ! 'stop

      Thread.sleep(500)

      val counterAfterKill = TestCounterInterface.foldlByAspect(actorCount)((a,_) =>a)
      counterAfterKill.size === 2
      counterAfterKill must contain(TestCounter(actorCount, 0, tags))
    }
  }
}