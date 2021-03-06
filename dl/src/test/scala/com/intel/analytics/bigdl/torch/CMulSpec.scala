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

package com.intel.analytics.bigdl.torch

import com.intel.analytics.bigdl.nn.CMul
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.RandomGenerator._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class CMulSpec extends FlatSpec with BeforeAndAfter with Matchers {
  before {
    if (!TH.hasTorch()) {
      cancel("Torch is not installed")
    }
  }

  "A CMul(5, 1)" should "generate correct output and grad" in {
    val seed = 100
    RNG.setSeed(seed)

    val layer = new CMul[Double](Array(5, 1))
    val input = Tensor[Double](5, 4)
    var i = 0
    input.apply1(_ => {i += 1; i})
    val gradOutput = Tensor[Double](5, 4)
    i = 0
    gradOutput.apply1(_ => {i += 1; i*0.1})

    val start = System.nanoTime()
    val output = layer.forward(input)
    val gradInput = layer.backward(input, gradOutput)
    val end = System.nanoTime()
    val scalaTime = end - start

    val code = "torch.manualSeed(" + seed + ")\n" +
      """module = nn.CMul(5, 1)
        output = module:forward(input)
        gradInput = module:backward(input,gradOutput)
        gradWeight = module.gradWeight"""

    val (luaTime, torchResult) = TH.run(code, Map("input" -> input, "gradOutput" -> gradOutput),
      Array("output", "gradInput", "gradWeight"))
    val luaOutput = torchResult("output").asInstanceOf[Tensor[Double]]
    val luaGradInput = torchResult("gradInput").asInstanceOf[Tensor[Double]]
    val luaGradWeight = torchResult("gradWeight").asInstanceOf[Tensor[Double]]

    output should be (luaOutput)
    gradInput should be (luaGradInput)
    layer.gradWeight should be (luaGradWeight)

    println("Test case : CMul, Torch : " + luaTime + " s, Scala : " + scalaTime / 1e9 + " s")
  }

  "A CMul(3)" should "generate correct output and grad" in {
    val seed = 100
    RNG.setSeed(seed)

    val layer = new CMul[Double](Array(3))
    val input = Tensor[Double](2, 3)
    var i = 0
    input.apply1(_ => {i += 1; i})
    val gradOutput = Tensor[Double](2, 3)
    i = 0
    gradOutput.apply1(_ => {i += 1; i*0.1})

    val start = System.nanoTime()
    val output = layer.forward(input)
    val gradInput = layer.backward(input, gradOutput)
    val end = System.nanoTime()
    val scalaTime = end - start

    val code = "torch.manualSeed(" + seed + ")\n" +
      """module = nn.CMul(3)
        output = module:forward(input)
        gradInput = module:backward(input,gradOutput)
        gradWeight = module.gradWeight"""

    val (luaTime, torchResult) = TH.run(code, Map("input" -> input, "gradOutput" -> gradOutput),
      Array("output", "gradInput", "gradWeight"))
    val luaOutput = torchResult("output").asInstanceOf[Tensor[Double]]
    val luaGradInput = torchResult("gradInput").asInstanceOf[Tensor[Double]]
    val luaGradWeight = torchResult("gradWeight").asInstanceOf[Tensor[Double]]

    output should be (luaOutput)
    gradInput should be (luaGradInput)
    layer.gradWeight should be (luaGradWeight)

    println("Test case : CMul, Torch : " + luaTime + " s, Scala : " + scalaTime / 1e9 + " s")
  }

  "A CMul(3, 4)" should "generate correct output and grad" in {
    val seed = 100
    RNG.setSeed(seed)

    val layer = new CMul[Double](Array(3, 4))
    val input = Tensor[Double](2, 3, 4)
    var i = 0
    input.apply1(_ => {i += 1; i})
    val gradOutput = Tensor[Double](2, 3, 4)
    i = 0
    gradOutput.apply1(_ => {i += 1; i*0.1})

    val start = System.nanoTime()
    val output = layer.forward(input)
    val gradInput = layer.backward(input, gradOutput)
    val end = System.nanoTime()
    val scalaTime = end - start

    val code = "torch.manualSeed(" + seed + ")\n" +
      """module = nn.CMul(3, 4)
        output = module:forward(input)
        gradInput = module:backward(input,gradOutput)
        gradWeight = module.gradWeight"""

    val (luaTime, torchResult) = TH.run(code, Map("input" -> input, "gradOutput" -> gradOutput),
      Array("output", "gradInput", "gradWeight"))
    val luaOutput = torchResult("output").asInstanceOf[Tensor[Double]]
    val luaGradInput = torchResult("gradInput").asInstanceOf[Tensor[Double]]
    val luaGradWeight = torchResult("gradWeight").asInstanceOf[Tensor[Double]]

    output should be (luaOutput)
    gradInput should be (luaGradInput)
    layer.gradWeight should be (luaGradWeight)

    println("Test case : CMul, Torch : " + luaTime + " s, Scala : " + scalaTime / 1e9 + " s")
  }

}

