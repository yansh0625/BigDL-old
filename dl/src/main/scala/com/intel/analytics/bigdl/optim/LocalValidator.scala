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

package com.intel.analytics.bigdl.optim

import com.intel.analytics.bigdl.dataset.{DataSet => DataSource, Batch}
import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.optim.DistriOptimizer._
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.{Engine, MklBlas, MklDnn}
import org.apache.log4j.Logger

object LocalValidator {
  val logger = Logger.getLogger(getClass)
}

class LocalValidator[T](model: Module[T])
  extends Validator[T, Iterator[Batch[T]]](model) {

  private val coreNumber = Engine.coreNumber()

  private val subModelNumber = Engine.getEngineType match {
    case MklBlas => coreNumber
    case MklDnn => 1
  }

  private val workingModels = (1 to subModelNumber).map(_ => model.cloneModule().evaluate()).toArray

  override def test(
    dataSet: DataSource[Iterator[Batch[T]]],
    vMethods: Array[ValidationMethod[T]]
  ): Array[(ValidationResult, ValidationMethod[T])] = {
    val dataIter = dataSet.data(looped = false)
    var count = 0
    dataIter.map(batch => {
      require(batch.data.size(1) == batch.labels.size(1))
      val stackSize = batch.data.size(1) / subModelNumber
      val extraSize = batch.data.size(1) % subModelNumber
      val parallelism = if (stackSize == 0) extraSize else subModelNumber
      val start = System.nanoTime()
      val result = Engine.default.invokeAndWait(
        (0 until parallelism).map(b =>
          () => {
            val offset = b * stackSize + math.min(b, extraSize)
            val length = stackSize + (if (b < extraSize) 1 else 0)
            val input = batch.data.narrow(1, offset + 1, length)
            val target = batch.labels.narrow(1, offset + 1, length)
            val output = workingModels(b).forward(input)
            vMethods.map(validation => {
              validation(output.asInstanceOf[Tensor[T]], target)
            })
          }
        )
      ).reduce((left, right) => {
        left.zip(right).map { case (l, r) =>
          l + r
        }
      })
      count += batch.data.size(1)
      logger.info(s"[Validation] $count/${dataSet.size()} Throughput is ${
        batch.data.size(1) / ((System.nanoTime() - start) / 1e9)
      } record / sec")
      result
    }).reduce((left, right) => {
      left.zip(right).map { case (l, r) =>
        l + r
      }
    }).zip(vMethods)
  }
}
