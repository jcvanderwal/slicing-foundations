package de.eventmodelers

import org.springframework.boot.SpringApplication

object ApplicationStarter {
  @JvmStatic
  fun main(args: Array<String>) {
    SpringApplication.from(SpringApp::main).run(*args)
  }
}
