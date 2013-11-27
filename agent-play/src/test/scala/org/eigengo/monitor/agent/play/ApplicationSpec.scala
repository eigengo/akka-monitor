package org.eigengo.monitor.agent.play

import org.specs2.mutable.Specification
import controllers.routes

import play.mvc._
import play.test.Helpers._
import play.api.http.Status._
import org.fest.assertions.Assertions._

class ApplicationSpec extends Specification {

  "Play" should {

    "perform simple check" in {
        var a = 1 + 1
        a === 2
    }

    "index template should contain the String that is passed to it" in {
      running(fakeApplication(), new Runnable() {
        def run() {
          val html: Content = views.html.index.render("Your new application is ready.")
          contentType(html) === "text/html"
          contentAsString(html) must contain("Your new application is ready.")
        }
      })
      success
    }

    "index should contain the correct string" in {
      running(fakeApplication(), new Runnable() {
        def run() {
          def result: Result = callAction(routes.ref.MainController.index())
          status(result) === OK
          contentType(result) === "text/html"
          charset(result) === "utf-8"
          contentAsString(result) must contain("Hello from Java")
        }
      })
      success
    }

  }

}
