package nl.clicqo.system

object Debug {
  fun getProperty(key: String): String? = System.getProperty(key).takeIf { System.getProperty("app.env") == "dev" }
}
