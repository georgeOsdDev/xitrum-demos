package demos.action

import xitrum.annotation.GET
import xitrum.view.Scalate

@GET("scalate/notFile")
class ScalateJadeString extends AppAction {
  def execute() {
    val template = "p This Jade template is from a string, not from a file."
    val string   = Scalate.renderJadeString(template)
    respondInlineView(string)
  }
}

@GET("scalate/mustache")
class ScalateMustache extends AppAction {
  def execute() {
    at("name")        = "Chris"
    at("value")       = 10000
    at("taxed_value") = 10000 - (10000 * 0.4)
    at("in_ca")       = true
    respondView(Map("type" -> "mustache"))
  }
}
