package nl.clicqo.system

object Debug {
  fun getProperty(key: String): String? =
    System
      .getProperty(key)
      .takeIf { System.getProperty("app.env")?.equals("dev") == true }

  fun isDevMode(): Boolean = System.getProperty("app.env")?.equals("dev") == true
}
