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
package org.eigengo.monitor.agent.akka;

import akka.actor.*;
import org.eigengo.monitor.agent.AgentConfiguration;
import org.eigengo.monitor.output.CounterInterface;
import scala.Option;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains advices for monitoring behaviour of an actor; typically imprisoned in an {@code ActorCell}.
 */
public aspect ActorCellMonitoringAspect extends AbstractMonitoringAspect issingleton() {
    private AkkaAgentConfiguration agentConfiguration;
    private ActorPathTagger tagger;
    private final CounterInterface counterInterface;
    private final ConcurrentHashMap<PathAndClass, AtomicLong> samplingCounters  = new ConcurrentHashMap<PathAndClass, AtomicLong>();
    private final ConcurrentHashMap<PathAndClass, AtomicInteger> numberOfActors = new ConcurrentHashMap<PathAndClass, AtomicInteger>();

    /**
     * Constructs this aspect
     */
    public ActorCellMonitoringAspect() {
        AgentConfiguration<AkkaAgentConfiguration> configuration = getAgentConfiguration("akka", AkkaAgentConfigurationJapi.apply());
        this.agentConfiguration = configuration.agent();
        this.counterInterface = createCounterInterface(configuration.common());
        this.tagger = new ActorPathTagger(this.agentConfiguration.includeRoutees());
    }

    /**
     * Injects the new {@code AkkaAgentConfiguration} instance.
     *
     * @param agentConfiguration the new configuration
     */
    synchronized final void setAgentConfiguration(AkkaAgentConfiguration agentConfiguration) {
        this.agentConfiguration = agentConfiguration;
        this.tagger = new ActorPathTagger(this.agentConfiguration.includeRoutees());
    }

    /**
     * Advises the {@code ActorCell.receiveMessage(message: Object): Unit}
     * We proceed with the pointcut if the actor is to be included in the monitoring *and* this is
     * the 'multiple-of-n'th time we've seen a message for an actor with a sample rate of n.
     *
     * Currently, we sample queue size, the fact that the message is delivered, the simple name of the class of the
     * message, and the time taken to complete the actor's reactive action.
     *
     * @param actorCell the ActorCell where the actor that receives the message "lives"
     * @param msg the incoming message
     */
    Object around(ActorCell actorCell, Object msg) : Pointcuts.actorCellReceiveMessage(actorCell, msg) {
        final ActorPath actorPath = actorCell.self().path();
        final PathAndClass pathAndClass = new PathAndClass(actorPath, getActorClassName(actorCell));
        if (!includeActorPath(pathAndClass) || !sampleMessage(pathAndClass)) return proceed(actorCell, msg);

        int samplingRate = getSampleRate(pathAndClass);

        // we tag by actor name
        final String[] tags = this.tagger.getTags(actorPath, getActorClassName(actorCell));

        // record the queue size
        this.counterInterface.recordGaugeValue(Aspects.queueSize(), actorCell.numberOfMessages(), tags);
        // record the message, general and specific
        this.counterInterface.incrementCounter(Aspects.delivered(), samplingRate, tags);
        this.counterInterface.incrementCounter(Aspects.delivered(msg), samplingRate, tags);

        // measure the time. we're using the ``nanoTime`` call to access the high-precision timer.
        // since we're not really interested in wall time, but just some increasing measure of
        // elapsed time
        Object result = null;
        final long start = System.nanoTime();
        // result will always be ``null``, because target returns ``Unit``
        result = proceed(actorCell, msg);
        // ns or µs is too fine. ms will do for now.
        final long duration = (System.nanoTime() - start) / 1000000;

        // record the actor duration
        this.counterInterface.recordExecutionTime(Aspects.actorDuration(), (int)duration, tags);

        // return null would do the trick, but we want to be _proper_.
        return result;
    }

    /**
     * Advises the {@code ActorCell.handleInvokeFailure(_, failure: Throwable): Unit}
     *
     * @param actorCell the ActorCell where the error spreads from
     * @param failure the exception that escaped from the {@code receive} method
     */
    before(ActorCell actorCell, Throwable failure) : Pointcuts.actorCellHandleInvokeFailure(actorCell, failure) {
        // record the error, general and specific
        String[] tags = this.tagger.getTags(actorCell.self().path(), getActorClassName(actorCell));
        this.counterInterface.incrementCounter(Aspects.actorError(), tags);
        this.counterInterface.incrementCounter(Aspects.actorError(failure), tags);
    }

    /**
     * Advises the {@code EventStream.publish(event: Any): Unit}
     *
     * @param event the received event
     */
    before(Object event) : Pointcuts.eventStreamPublish(event) {
        if (event instanceof UnhandledMessage) {
            UnhandledMessage unhandledMessage = (UnhandledMessage)event;
            String[] tags = this.tagger.getTags(unhandledMessage.recipient().path(), ActorPathTagger.ANONYMOUS_ACTOR_CLASS_NAME);
            this.counterInterface.incrementCounter(Aspects.undelivered(), tags);
            this.counterInterface.incrementCounter(Aspects.undelivered(unhandledMessage.getMessage()), tags);
        }
    }

    /**
     * Advises the {@code actorOf} method of {@code ActorRefFactory} implementations
     *
     * @param props the {@code Props} instance used in the call
     * @param actor the {@code ActorRef} returned from the call
     */
    after(Props props) returning (ActorRef actor) : Pointcuts.anyActorOf(props) {
        recordActorCount(actor.path(), props, CountType.Increment);
    }

    /**
     * Advises the {@code LocalActorRef.stop} method
     *
     * @param actorCell the {@code ActorCell} of the actor being stopped
     */
    after(ActorCell actorCell) : Pointcuts.actorCellInternalStop(actorCell) {
        recordActorCount(actorCell.self().path(), actorCell.props(), CountType.Decrement);
    }

    /**
     * Records the actor count increment or decrement
     *
     * @param path the ActorPath being created or destroyed
     * @param props the Props of the actor at the {@code path}
     * @param countType the increment or decrement
     */
     private void recordActorCount(ActorPath path, Props props, CountType countType) {
         final PathAndClass pac = new PathAndClass(path, getActorClassName(props));
         if (!includeActorPath(pac)) return;

         final String[] tags = this.tagger.getTags(path, pac.actorClassName());
         this.numberOfActors.putIfAbsent(pac, new AtomicInteger(0));
         // increment and get the current number of actors of this type (if the value was 0, then this returns 1 -- which is correct)
         final int currentNumberOfActors;
         switch (countType) {
             case Increment:
                 currentNumberOfActors = this.numberOfActors.get(pac).incrementAndGet();
                 break;
             case Decrement:
                 currentNumberOfActors = this.numberOfActors.get(pac).decrementAndGet();
                 break;
             default:
                 currentNumberOfActors = 0;
                 break;
         }

         this.counterInterface.recordGaugeValue(Aspects.actorCount(), currentNumberOfActors, tags);
     }

    /**
     * The count type for exhaustive matching
     */
    private static enum CountType {
        Increment, Decrement
    }

    /**
     * Decide whether to include this ActorCell in our measurements
     *
     * @param pathAndClass the PAC container
     * @return whether to include the given actor in the metrics
     */
    private boolean includeActorPath(final PathAndClass pathAndClass) {
        // do not monitor our own output code
        if (pathAndClass.actorClassName().isDefined()) {
            if (pathAndClass.actorClassName().get().startsWith("org.eigengo.monitor.output")) return false;
        }

        // include actor if it's in 'included' list
        if (this.agentConfiguration.included().accept(pathAndClass)) return true;
        // exclude actor if excluded (i.e. is in 'excluded' list, or excludeAllNotIncluded = true, and actor is not included)
        if (this.agentConfiguration.excluded().accept(pathAndClass)) return false;

        // skip system actor checks if we wish to monitor them
        if (this.agentConfiguration.includeSystemAgents()) return true;

        String userOrSystem = pathAndClass.actorPath().getElements().iterator().next();
        return "user".equals(userOrSystem);
    }

    /**
     * Lookup the sample rate for an actor from the configuration
     *
     * @param pathAndClass the PAC container
     * @return the sampling rate as non-negative integer
     */
    private int getSampleRate(final PathAndClass pathAndClass) {
        return this.agentConfiguration.sampling().getRate(pathAndClass);
    }

    /**
     * Decide whether to sample an actor on a particular occasion
     *
     * @param pathAndClass the PAC container
     * @return {@code true} if we should sample this actor
     */
    private boolean sampleMessage(final PathAndClass pathAndClass) {
        int sampleRate = getSampleRate(pathAndClass);
        if (sampleRate == 1) return true;

        this.samplingCounters.putIfAbsent(pathAndClass, new AtomicLong(0));
        long timesSeenSoFar = this.samplingCounters.get(pathAndClass).incrementAndGet();
        return (timesSeenSoFar % sampleRate == 1); // == 1 to log first value (incrementAndGet returns updated value)
    }

    /**
     * returns the canonical name of the actor type associated with an ActorCell
     *
     * @param actorCell the actor cell to get the name for
     * @return the Option of actor class, never {@code null}.
     */
    private Option<String> getActorClassName(final ActorCell actorCell) {
        return getActorClassName(actorCell.props());
    }

    /**
     * Returns the canonical name of the actor type associated with a Props instance
     *
     * Returns null if props.actorClass does not have a canonical name (i.e., if
     * it is a local or anonymous class)
     *
     * Notably, when the actor class is anonymous, the {@code getCanonicalName} returns {@code null},
     * which we deal with here.
     *
     * @param props the ActorRef {@code Props} instance
     * @return the Option of actor class, never {@code null}.
     */
    private Option<String> getActorClassName(final Props props) {
        final String canonicalName = props.actorClass().getCanonicalName();
        if (canonicalName == null) return ActorPathTagger.ANONYMOUS_ACTOR_CLASS_NAME;
        return Option.apply(canonicalName);
    }

}
