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

import com.intel.analytics.bigdl.tensor.Tensor
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}


class TensorSpec extends FlatSpec with BeforeAndAfter with Matchers {
  before {
    if (!TH.hasTorch()) {
      cancel("Torch is not installed")
    }
  }

  "Read empty LongTensor" should "generate correct output" in {
    val empty = Tensor[Double]()

    val code = "output = torch.LongTensor()\n"

    val (luaTime, torchResult) = TH.run(code, Map("empty" -> empty),
      Array("output"))
    val luaOutput1 = torchResult("output").asInstanceOf[Tensor[Double]]
    luaOutput1 should be (empty)
  }

  "Read LongTensor" should "generate correct output" in {
    val tensor = Tensor[Double](1, 2, 3)

    val code = "output = torch.LongTensor(1, 2, 3)\n" +
      "output:zero()"

    val (luaTime, torchResult) = TH.run(code, Map("tensor" -> tensor),
      Array("output"))
    val luaOutput1 = torchResult("output").asInstanceOf[Tensor[Double]]
    luaOutput1 should be (tensor)
  }
}
