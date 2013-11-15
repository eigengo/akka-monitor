/*
 * Copyright (c) 2013 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eigengo.monitor.agent.akka

import org.eigengo.monitor.{TestCounter, TestCounterInterface}
import akka.actor.Props

class JavaApiFiltersSpec  extends ActorCellMonitoringAspectSpec(Some("javaapifilters.conf")) {
  sequential
  import Aspects._


  "Counting the number of actors using the java api" should {

    // records the count of actors, grouped by simple class name
    "Record the actor count" in {
      TestCounterInterface.clear()
      val akkaSystem = ExampleAkkaSystemWithJavaApiForTests.run(Array.empty)
      val parentPath = "parent.akka://helloakka/user"
      val unnamedActorPath = "path.akka://helloakka/user/$a"
      val propsClazz = "props.class org.eigengo.monitor.agent.akka.ExampleAkkaSystemWithJavaApiForTests$GreetPrinter"
      val className = "className.org.eigengo.monitor.agent.akka.ExampleAkkaSystemWithJavaApiForTests.GreetPrinter"

      val namedGreetPrinterTags = List(parentPath, "path.akka://helloakka/user/greetPrinter", propsClazz, className)
      val greeterTags = List(parentPath, "path.akka://helloakka/user/greeter",
        "props.class org.eigengo.monitor.agent.akka.ExampleAkkaSystemWithJavaApiForTests$Greeter",
        "className.org.eigengo.monitor.agent.akka.ExampleAkkaSystemWithJavaApiForTests.Greeter")

      akkaSystem.actorOf(Props[ExampleAkkaSystemWithJavaApiForTests.GreetPrinter])


      Thread.sleep(100L)
      TestCounterInterface.foldlByAspect(actorCount)(TestCounter.takeLHS) must containAllOf(Seq(
        TestCounter(actorCount, 1, namedGreetPrinterTags),
        TestCounter(actorCount, 1, greeterTags)))


      akkaSystem.shutdown()
      Thread.sleep(1000L)

      val countersAfterShutdown = TestCounterInterface.foldlByAspect(actorCount)(TestCounter.takeLHS)
      countersAfterShutdown must containAnyOf(Seq(
        TestCounter(actorCount, 0, namedGreetPrinterTags),
        TestCounter(actorCount, 0, greeterTags)))
      
      countersAfterShutdown.forall(p => p.tags.toList.foldRight(true)((string, b) => b && string != unnamedActorPath))
    }

  }

}
