package org.eigengo.monitor.example.play.app.controllers

import play.api.mvc.{Action, Controller}


object TestingApp extends Controller {
  def index = Action {
    Ok(views.html.index("Hello Play Framework"))
  }
}

