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

import com.intel.analytics.bigdl.nn.abstractnn.TensorModule
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.RandomGenerator._

import scala.reflect.ClassTag

class CMul[@specialized(Float, Double) T: ClassTag](
  val size: Array[Int])(
  implicit ev: TensorNumeric[T]) extends TensorModule[T] {

  val weight: Tensor[T] = Tensor[T](size)
  val gradWeight : Tensor[T] = Tensor[T](size)

  reset()

  override def reset(): Unit = {
    val stdv = 1.0/math.sqrt(weight.nElement())
    weight.apply1(_ => ev.fromType[Double](RNG.uniform(-stdv, stdv)))
    zeroGradParameters()
  }

  override def updateOutput(input: Tensor[T]): Tensor[T] = {
    output.resizeAs(input).copy(input)
    if (input.nElement() == weight.nElement()) {
      output.cmul(weight)
    } else {
      val expand = if (weight.dim() == input.dim()) {
        weight.view(weight.size())
      } else {
        weight.view(Array(1) ++ weight.size())
      }

      expand.expandAs(output)
      output.cmul(expand)
    }
    output
  }

  override def updateGradInput(input: Tensor[T], gradOutput: Tensor[T]): Tensor[T] = {
    gradInput.resizeAs(input).zero()
    if (weight.nElement() == gradOutput.nElement()) {
      gradInput.addcmul(ev.fromType[Int](1), weight, gradOutput)
    } else {
      val expand = if (weight.dim() == gradOutput.dim()) {
        weight.view(weight.size())
      } else {
        weight.view(Array(1) ++ weight.size())
      }

      expand.expandAs(gradOutput)
      gradInput.cmul(expand, gradOutput)
    }

    gradInput
  }

  override def accGradParameters(input: Tensor[T], gradOutput: Tensor[T],
    scale: Double = 1.0): Unit = {

    if (weight.nElement() == gradOutput.nElement()) {
      gradWeight.addcmul(ev.fromType[Double](scale), input, gradOutput)
    } else {
      if (weight.dim() == input.dim()) {
        val sumFrom = Tensor[T](input.size()).copy(input)
        sumFrom.cmul(gradOutput)

        val sumInto = Tensor[T](input.size())
        var i = 1
        while (i <= weight.dim()) {
          if (weight.size(i) != input.size(i)) {
            sumInto.sum(sumFrom, i)
          }
          i += 1
        }
        gradWeight.add(ev.fromType[Double](scale), sumInto)
      } else {
        val repeat = Tensor[T](input.size()).copy(input)
        repeat.cmul(gradOutput)
        val sum = Tensor[T](input.size())
        sum.sum(repeat, 1)
        gradWeight.view(Array(1) ++ gradWeight.size()).add(ev.fromType[Double](scale), sum)
      }

    }
  }

  override def updateParameters(learningRate: T): Unit = {
    weight.map(gradWeight, (a, b) => ev.minus(a, ev.times(learningRate, b)))
  }

  override def zeroGradParameters(): Unit = {
    gradWeight.zero()
  }

  override def parameters(): (Array[Tensor[T]], Array[Tensor[T]]) = {
    (Array(this.weight), Array(this.gradWeight))
  }

  override def equals(obj: Any): Boolean = {
    if (!super.equals(obj)) {
      return false
    }

    if (!obj.isInstanceOf[CMul[T]]) {
      return false
    }
    val other = obj.asInstanceOf[CMul[T]]
    if (this.eq(other)) {
      return true
    }

    size == other.size &&
      gradWeight == other.gradWeight &&
      weight == other.weight
  }

  override def hashCode() : Int = {
    val seed = 37
    var hash = super.hashCode()
    hash = hash * seed + size.hashCode()
    hash = hash * seed + gradWeight.hashCode()
    hash = hash * seed + weight.hashCode()

    hash
  }

  override def toString(): String = {
    s"nn.CMul(${java.util.Arrays.toString(size)})"
  }
}

object CMul {
  def apply[@specialized(Float, Double) T: ClassTag](
      size: Array[Int])(implicit ev: TensorNumeric[T]) : CMul[T] = {
    new CMul[T](size)
  }
}
