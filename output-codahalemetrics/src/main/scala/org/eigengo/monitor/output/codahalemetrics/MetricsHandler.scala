package org.eigengo.monitor.output.codahalemetrics

import com.codahale.metrics.{Metric, MetricFilter}
import scala.collection.JavaConversions._
import java.util.concurrent.TimeUnit
import com.codahale.metrics.MetricRegistry

/**
 * Submits the counters to the local Codahale metrics interface
 */
trait MetricsHandler {

  def registry: MetricRegistry
  def marshaller: NameMarshaller

  /**
   * Increment the counter identified by {@code aspect} by one.
   *
   * @param aspect the aspect to increment
   * @param delta the amount to adjust by
   * @param tags optional tags
   */
  def updateCounter(aspect: String, delta: Int, tags: Seq[String]): Unit = {
    registry.counter(marshaller.buildName(aspect, tags)).inc(delta)
  }

  /**
   * Records gauge {@code value} for the given {@code aspect}, with optional {@code tags}
   *
   * @param aspect the aspect to record the value for
   * @param value the value
   * @param tags optional tags
   */
  def updateGaugeValue(aspect: String, value: Int, tags: Seq[String]): Unit =  {
    val name = marshaller.buildName(aspect, tags)

    // See if this is already registered
    registry.getGauges(new MetricFilter { def matches(regName:String, metric:Metric): Boolean =
      regName.equals(name)}).values.headOption match {
        case Some(m) =>
          m.asInstanceOf[UpdatableGauge[Int]].setValue(value)
        case None =>
          // Not registered so do so now
          val gauge = new UpdatableGauge[Int]()
          gauge.setValue(value)
          registry.register(name, gauge)
      }
  }

  /**
   * Records the execution time of the given {@code aspect}, with optional {@code tags}
   *
   * @param aspect the aspect to record the execution time for
   * @param duration the execution time (most likely in ms)
   * @param tags optional tags
   */
  def updateExecutionTime(aspect: String, duration: Int, tags: Seq[String]): Unit = {
    registry.timer(marshaller.buildName(aspect, tags)).update(duration, TimeUnit.MILLISECONDS)
  }
}
