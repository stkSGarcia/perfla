package io.transwarp.aiops.perfla.analyzer

import scala.collection.mutable.ArrayBuffer

class TaskNode(id: String, pattern: String, val subTasksId: Array[String]) {
  val subTasks = new ArrayBuffer[TaskNode]
  var time = 0L
  var size = 0L
  var num = 0
  var flag = true

  def print(prefix: String): Unit = {
    println(s"$prefix$pattern: $time ms, $size Byte, $num tasks")
    val basePrefix = if (prefix == "") "" else prefix.substring(0, prefix.length - 3) + "|  "
    val taskPrefix = basePrefix + "+- "
    val lastPrefix = basePrefix + "\\- "
    var subTime = 0L
    subTasks.foreach(task => {
      subTime += task.time
      task.print(taskPrefix)
    })
    if (subTasks.nonEmpty)
      println(s"${lastPrefix}Unknown time: ${time - subTime} ms")
  }
}