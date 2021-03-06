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
package com.intel.analytics.bigdl.nn

import com.intel.analytics.bigdl.nn.abstractnn.AbstractModule
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.RandomGenerator._
import com.intel.analytics.bigdl.utils.{T, Table}

import scala.reflect.ClassTag

/**
 * a bilinear transformation with sparse inputs,
  * The input tensor given in forward(input) is a table containing both inputs x_1 and x_2,
  * which are tensors of size N x inputDimension1 and N x inputDimension2, respectively.
 *
 * @param inputSize1
 * @param inputSize2
 * @param outputSize
 * @param biasRes  The layer can be trained without biases by setting bias = false. otherwise true
 */
class Bilinear[T: ClassTag](inputSize1: Int,
  inputSize2: Int,
  outputSize: Int,
  biasRes: Boolean = true
 )(implicit ev: TensorNumeric[T]) extends AbstractModule[Table, Tensor[T], T] {

  require((inputSize1 > 0) && (inputSize2 > 0) && (outputSize > 0),
    "inputSize1 and inputSize2 and outputSize should be positive integer numbers")

  val weight = Tensor[T](outputSize, inputSize1, inputSize2)
  val bias: Tensor[T] = if (biasRes)Tensor[T](outputSize) else null

  var buff1: Tensor[T] = Tensor[T]()
  var buff2: Tensor[T] = Tensor[T]()

  val gradWeight: Tensor[T] = Tensor[T](outputSize, inputSize1, inputSize2)
  val gradBias: Tensor[T] = Tensor[T](outputSize)

  reset()

  override def reset(): Unit = {
    val stdv = 1.0 / math.sqrt(weight.size(2))
    weight.apply1(_ => ev.fromType[Double](RNG.uniform(-stdv, stdv)))
    if (null != bias ) bias.apply1(_ => ev.fromType[Double](RNG.uniform(-stdv, stdv)))
    zeroGradParameters()
  }

  override def updateOutput(input: Table): Tensor[T] = {
    require(input.length() == 2,
      "input should be a table containing two data Tensors")
    val res1 = input[Tensor[T]](1)
    val res2 = input[Tensor[T]](2)

    require(res1.nDimension() == 2 && res2.nDimension() == 2 && res1.size(1) == res2.size(1),
      "input Tensors should be two-dimensional and have the same number of rows")
    require(res1.size(2) == weight.size(2) && res2.size(2) == weight.size(3),
      "dimensionality of first input and second input is erroneous")

    // set up buffer
    buff2.resizeAs(res2)

    // compute output scores
    output.resize(res1.size(1), weight.size(1))
    var k = 1
    while(k < (weight.size(1) + 1)) {
      buff2.zero()
      buff2.addmm(res1, weight(k))
      buff2.cmul(res2)
      output.narrow(2, k, 1).sum(buff2, 2)
      k += 1
    }
    if (bias != null) {
      output.add(bias.reshape(Array(1, bias.nElement())).expand(output.size()))
    }
    output
  }

  override def updateGradInput(input: Table, gradOutput: Tensor[T]): Table = {
    val res1 = input[Tensor[T]](1)
    val res2 = input[Tensor[T]](2)

    require(res1.size(1) == gradOutput.size(1),
      "number of rows in gradOutput does not match input")
    require(gradOutput.size(2) == weight.size(1),
      "number of columns in gradOutput does not output size of layer")

    if (!gradInput.contains(1)) gradInput.insert(1, Tensor[T]())
    if (!gradInput.contains(2)) gradInput.insert(2, Tensor[T]())

    val gradInput1 = gradInput[Tensor[T]](1)
    val gradInput2 = gradInput[Tensor[T]](2)

    // compute d output / d input:
    gradInput1.resizeAs(res1).zero()
    gradInput2.resizeAs(res2).zero()

    // do first slice of weight tensor (k = 1)
    gradInput1.addmm(res2, weight.select(1, 1).t())
    gradInput1.cmul(gradOutput.narrow(2, 1, 1).expand(
      Array(gradInput1.size(1), gradInput1.size(2))))

    gradInput2.addmm(ev.fromType(1), res1, weight.select(1, 1))
    gradInput2.cmul(gradOutput.narrow(2, 1, 1).expand(
      Array(gradInput2.size(1), gradInput2.size(2))))

    // do remaing slices of weight tensor
    if(weight.size(1) > 1) {
      buff1.resizeAs(res1)

      var k = 2
      while(k < (weight.size(1) + 1)) {
        buff1.zero()
        buff2.zero()

        buff1.addmm(res2, weight.select(1, k).t())
        buff1.cmul(gradOutput.narrow(2, k, 1).expand(
          Array(gradInput1.size(1), gradInput1.size(2))))
        gradInput1.add(buff1)

        buff2.addmm(input(1), weight.select(1, k))
        buff2.cmul(gradOutput.narrow(2, k, 1).expand(
          Array(gradInput2.size(1), gradInput2.size(2))))
        gradInput2.add(buff2)
        k += 1
      }
    }
    gradInput
  }

  override def accGradParameters(input: Table, gradOutput: Tensor[T], scale: Double = 1.0): Unit = {
    val res1 = input[Tensor[T]](1)
    val res2 = input[Tensor[T]](2)

    // make sure we have buffer
    if(null == buff1) buff1 = Tensor[T]()
    buff1.resizeAs(res1)

    // accumulate parameter gradients:
    var k = 1
    while(k < (weight.size(1) + 1)) {
      buff1.zero()
      buff1.cmul(res1, gradOutput.narrow(2, k, 1).expandAs(res1))
      gradWeight.select(1, k).addmm(buff1.t(), input(2))
      k += 1
    }
    if(null != bias) gradBias.add(ev.fromType(scale), gradOutput.sum(1))
  }

  override def zeroGradParameters(): Unit = {
    gradWeight.zero()
    gradBias.zero()
  }

  override def clearState() : this.type = {
    super.clearState()
    buff1.set()
    buff2.set()
    this
  }

  override def parameters(): (Array[Tensor[T]], Array[Tensor[T]]) = {
    (Array(this.weight, this.bias), Array(this.gradWeight, this.gradBias))
  }

  override def toString(): String = {
    s"nn.Bilinear($inputSize1, $inputSize2, $outputSize, $biasRes)"
  }
}

object Bilinear {
  def apply[@specialized(Float, Double) T: ClassTag](
    inputSize1: Int,
    inputSize2: Int,
    outputSize: Int,
    biasRes: Boolean = true)(implicit ev: TensorNumeric[T]) : Bilinear[T] = {
    new Bilinear[T](inputSize1, inputSize2, outputSize, biasRes)
  }
}
