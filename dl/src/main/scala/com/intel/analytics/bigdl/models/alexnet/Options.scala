/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.bigdl.models.alexnet

import scopt.OptionParser

object Options {

  case class TrainParams(
    folder: String = "./",
    cache: Option[String] = None,
    modelSnapshot: Option[String] = None,
    stateSnapshot: Option[String] = None,
    coreNumber: Int = (Runtime.getRuntime().availableProcessors() / 2)
  )

  val trainParser = new OptionParser[TrainParams]("BigDL AlexNet Example") {
    head("Train AlexNet model on single node")
    opt[String]('f', "folder")
      .text("where you put your local hadoop sequence files")
      .action((x, c) => c.copy(folder = x))
    opt[String]("model")
      .text("model snapshot location")
      .action((x, c) => c.copy(modelSnapshot = Some(x)))
    opt[String]("state")
      .text("state snapshot location")
      .action((x, c) => c.copy(stateSnapshot = Some(x)))
    opt[Int]('c', "core")
      .text("cores number to train the model")
      .action((x, c) => c.copy(coreNumber = x))
    opt[String]("cache")
      .text("where to cache the model")
      .action((x, c) => c.copy(cache = Some(x)))
  }

  case class TestParams(
    folder: String = "./",
    model: String = "",
    coreNumber: Int = (Runtime.getRuntime().availableProcessors() / 2)
  )

  val testParser = new OptionParser[TestParams]("BigDL AlexNet Test Example") {
    opt[String]('f', "folder")
      .text("where you put your local hadoop sequence files")
      .action((x, c) => c.copy(folder = x))

    opt[String]("model")
      .text("model snapshot location")
      .action((x, c) => c.copy(model = x))
      .required()
  }
}
