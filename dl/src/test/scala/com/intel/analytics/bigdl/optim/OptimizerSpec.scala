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

import java.nio.file.{Files, Paths}

import com.intel.analytics.bigdl.nn.Sequential
import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.models.alexnet.AlexNet
import com.intel.analytics.bigdl.utils.{File, T, Table}
import org.scalatest.{FlatSpec, Matchers}

class OptimizerSpec extends FlatSpec with Matchers {
  val model = new Sequential[Float]()

  "Optimizer" should "end with maxEpoch" in {
    val dummyOptimizer = new Optimizer[Float, Float, Float](model, null, null) {
      override def optimize(): Module[Float] = {
        val state = T("epoch" -> 9)
        endWhen(state) should be(false)
        state("epoch") = 10
        endWhen(state) should be(false)
        state("epoch") = 11
        endWhen(state) should be(true)
        model
      }
    }
    dummyOptimizer.setEndWhen(Trigger.maxEpoch(10)).optimize()
  }

  it should "end with iteration" in {
    val dummyOptimizer = new Optimizer[Float, Float, Float](model, null, null) {
      override def optimize(): Module[Float] = {
        val state = T("neval" -> 999)
        endWhen(state) should be(false)
        state("neval") = 1000
        endWhen(state) should be(false)
        state("neval") = 1001
        endWhen(state) should be(true)
        model
      }
    }
    dummyOptimizer.setEndWhen(Trigger.maxIteration(1000)).optimize()
  }

  it should "be triggered every epoch" in {
    val dummyOptimizer = new Optimizer[Float, Float, Float](model, null, null) {
      override def optimize(): Module[Float] = {
        val state = T("epoch" -> 9)
        validationTrigger.get(state) should be(false)
        cacheTrigger.get(state) should be(false)
        state("epoch") = 10
        validationTrigger.get(state) should be(true)
        cacheTrigger.get(state) should be(true)
        validationTrigger.get(state) should be(false)
        cacheTrigger.get(state) should be(false)
        state("epoch") = 11
        validationTrigger.get(state) should be(true)
        cacheTrigger.get(state) should be(true)
        cachePath.isDefined should be(true)
        model
      }
    }
    dummyOptimizer.setValidation(Trigger.everyEpoch, null, null)
    dummyOptimizer.setCache("", Trigger.everyEpoch)
    dummyOptimizer.optimize()
  }

  it should "be triggered every 5 iterations" in {
    val dummyOptimizer = new Optimizer[Float, Float, Float](model, null, null) {
      override def optimize(): Module[Float] = {
        val state = T("neval" -> 1)
        validationTrigger.get(state) should be(false)
        cacheTrigger.get(state) should be(false)
        state("neval") = 4
        validationTrigger.get(state) should be(false)
        cacheTrigger.get(state) should be(false)
        state("neval") = 5
        validationTrigger.get(state) should be(true)
        cacheTrigger.get(state) should be(true)
        model
      }
    }
    dummyOptimizer.setValidation(Trigger.severalIteration(5), null, null)
    dummyOptimizer.setCache("", Trigger.severalIteration(5))
    dummyOptimizer.optimize()
  }

  it should "save model to given path" in {
    val filePath = java.io.File.createTempFile("OptimizerSpec", "model").getAbsolutePath
    Files.delete(Paths.get(filePath))
    Files.createDirectory(Paths.get(filePath))
    val model = AlexNet(1000)
    val dummyOptimizer = new Optimizer[Float, Float, Float](model, null, null) {
      override def optimize(): Module[Float] = {
        Optimizer.saveModel(model, this.cachePath, this.isOverWrite)
        model
      }
    }
    dummyOptimizer.setCache(filePath, Trigger.everyEpoch)
    dummyOptimizer.optimize()

    model.clearState()
    val loadedModel = File.load[Module[Double]] (filePath + "/model")
    loadedModel should be(model)
  }

  it should "save model and state to given path with postfix" in {
    val filePath = java.io.File.createTempFile("OptimizerSpec", "model").getAbsolutePath
    Files.delete(Paths.get(filePath))
    Files.createDirectory(Paths.get(filePath))
    val model = AlexNet(1000)
    val dummyOptimizer = new Optimizer[Float, Float, Float](model, null, null) {
      override def optimize(): Module[Float] = {
        Optimizer.saveModel(model, this.cachePath, this.isOverWrite, ".test")
        model
      }
    }
    dummyOptimizer.setCache(filePath, Trigger.everyEpoch)
    dummyOptimizer.optimize()

    model.clearState()
    val loadedModel =
      File.load[Module[Double]](filePath + "/model.test")
    loadedModel should be(model)
  }

  it should "save state to given path" in {
    val filePath = java.io.File.createTempFile("OptimizerSpec", "state").getAbsolutePath
    Files.delete(Paths.get(filePath))
    Files.createDirectory(Paths.get(filePath))
    val state = T("test" -> 123)
    val dummyOptimizer = new Optimizer[Float, Float, Float](model, null, null) {
      override def optimize(): Module[Float] = {
        Optimizer.saveState(state, this.cachePath, this.isOverWrite)
        model
      }
    }.setState(state)
    dummyOptimizer.setCache(filePath, Trigger.everyEpoch)
    dummyOptimizer.optimize()

    val loadedState = File.load[Table](filePath + "/state")
    loadedState should be(state)
  }

  it should "save state to given path with post fix" in {
    val filePath = java.io.File.createTempFile("OptimizerSpec", "state").getAbsolutePath
    Files.delete(Paths.get(filePath))
    Files.createDirectory(Paths.get(filePath))
    val state = T("test" -> 123)
    val dummyOptimizer = new Optimizer[Float, Float, Float](model, null, null) {
      override def optimize(): Module[Float] = {
        Optimizer.saveState(state, this.cachePath, this.isOverWrite, ".post")
        model
      }
    }.setState(state)
    dummyOptimizer.setCache(filePath, Trigger.everyEpoch)
    dummyOptimizer.optimize()

    val loadedState = File.load[Table](filePath + "/state.post")
    loadedState should be(state)
  }
}
