/*
 * Zinc - The incremental compiler for Scala.
 * Copyright 2011 - 2017, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * This software is released under the terms written in LICENSE.
 */

package xsbt

import java.io.{ PrintWriter, StringWriter }

import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.{ GenericRunnerCommand, Settings }

import xsbti.Logger

import InteractiveConsoleHelper._

class InteractiveConsoleInterface(
    args: Array[String],
    bootClasspathString: String,
    classpathString: String,
    initialCommands: String,
    cleanupCommands: String,
    loader: ClassLoader,
    bindNames: Array[String],
    bindValues: Array[AnyRef],
    log: Logger
) extends xsbti.InteractiveConsoleInterface {

  private def mkSettings(options: List[String]): Settings = {
    val command = new GenericRunnerCommand(options, str => log error Message(str))
    val settings =
      if (command.ok) command.settings
      else throw new Exception(command.usageMsg) // TODO: Provide better exception

    settings.Yreplsync.value = true
    settings
  }

  private lazy val interpreterSettings: Settings = mkSettings(args.toList)

  private val compilerSettings: Settings = {
    // we need rt.jar from JDK, so java classpath is required
    val useJavaCp = "-usejavacp"

    val compilerSettings = mkSettings((args :+ useJavaCp).toList)
    if (!bootClasspathString.isEmpty)
      compilerSettings.bootclasspath.value = bootClasspathString
    compilerSettings.classpath.value = classpathString
    compilerSettings
  }

  private val outWriter: StringWriter = new StringWriter
  private val poutWriter: PrintWriter = new PrintWriter(outWriter)

  private val interpreter: IMain = new IMain(compilerSettings, new PrintWriter(outWriter))
  private def clearBuffer(): Unit = {
    // errorWriter.getBuffer.setLength(0)
    outWriter.getBuffer.setLength(0)
  }

  def interpret(line: String, synthetic: Boolean): InteractiveConsoleResponse = {
    clearBuffer()
    val r = interpreter.interpret(line, synthetic)
    InteractiveConsoleResponse(r, outWriter.toString)
  }

  def reset(): Unit = {
    clearBuffer()
    interpreter.reset()
  }
}
