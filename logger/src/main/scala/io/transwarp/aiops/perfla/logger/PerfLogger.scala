package io.transwarp.aiops.perfla.logger

import io.transwarp.aiops.perfla.loader.{Config, Task, TaskIdentifier}
import io.transwarp.aiops.perfla.logger.PerfLogMod.PerfLogMod
import org.slf4j.LoggerFactory

class PerfLogger(clazz: Class[_] = classOf[PerfLogger]) {
  private val logger = LoggerFactory.getLogger(clazz)
  private var mod = PerfLogMod.DEFAULT

  def checkpoint: Checkpoint = {
    val caller = (new Throwable).getStackTrace()(1)
    checkpoint(caller.getClassName, caller.getMethodName, null)
  }

  def checkpoint(uuid: String): Checkpoint = {
    val caller = (new Throwable).getStackTrace()(1)
    checkpoint(caller.getClassName, caller.getMethodName, uuid)
  }

  def checkpoint(className: String, methodName: String, uuid: String): Checkpoint = {
    val checkpoint = new Checkpoint
    if (Config.isValid) {
      val identifier = new TaskIdentifier(className, methodName)
      Config.identifierMap.get(identifier).foreach(task => {
        checkpoint.task = task
        checkpoint.uuid = uuid
      })
    }
    checkpoint.startTime = System.currentTimeMillis
    checkpoint
  }

  def log(checkpoint: Checkpoint): Unit = log(checkpoint, mod)

  def log(checkpoint: Checkpoint, logMod: PerfLogMod): Unit = if (Config.isValid) {
    checkpoint.endTime = System.currentTimeMillis
    logMod match {
      case PerfLogMod.DEFAULT =>
        if (checkpoint.task != null) {
          val diff = checkpoint.endTime - checkpoint.startTime
          diff match {
            case d if d > checkpoint.task.threshold.error * checkpoint.dataSize =>
              logger.error(s"${Config.setting.prefix}" +
                s" ${checkpoint.task.pattern}" +
                s" [ERROR]" +
                s" [${checkpoint.uuid}]" +
                s" [${checkpoint.startTime}~${checkpoint.endTime}:$diff]" +
                s" [${checkpoint.dataSize}]")
            case d if d > checkpoint.task.threshold.warn * checkpoint.dataSize =>
              logger.warn(s"${Config.setting.prefix}" +
                s" ${checkpoint.task.pattern}" +
                s" [WARN]" +
                s" [${checkpoint.uuid}]" +
                s" [${checkpoint.startTime}~${checkpoint.endTime}:$diff]" +
                s" [${checkpoint.dataSize}]")
            case _ =>
          }
        }
      case PerfLogMod.FORCE =>
        val diff = checkpoint.endTime - checkpoint.startTime
        logger.error(s"${Config.setting.prefix}" +
          s" ${if (checkpoint.task == null) "Unknown task" else checkpoint.task.pattern}" +
          s" [ERROR]" +
          s" [${checkpoint.uuid}]" +
          s" [${checkpoint.startTime}~${checkpoint.endTime}:$diff]" +
          s" [${checkpoint.dataSize}]")
      case PerfLogMod.MUTE =>
      case _ => logger.warn("Unknown perf log mod!")
    }
  }

  def setMod(value: PerfLogMod): Unit = mod = value
}

class Checkpoint {
  var uuid: String = _
  var startTime: Long = _
  var endTime: Long = _
  var task: Task = _
  var dataSize: Long = 1

  def setUUID(id: String): Unit = uuid = id

  def setDataSize(size: Long): Unit = dataSize = size
}

object PerfLogger {
  def getLogger: PerfLogger = new PerfLogger

  def getLogger(clazz: Class[_]): PerfLogger = new PerfLogger(clazz)
}

object PerfLogMod extends Enumeration {
  type PerfLogMod = Value
  val DEFAULT = Value
  val FORCE = Value
  val MUTE = Value
}