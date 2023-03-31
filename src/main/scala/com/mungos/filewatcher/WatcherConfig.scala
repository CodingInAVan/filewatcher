package com.mungos.filewatcher

import scala.concurrent.duration.FiniteDuration

case class WatcherConfig (
  fileConfigs: List[FileConfig]
)
case class FileConfig (
  file: String,
  interval: FiniteDuration
)
