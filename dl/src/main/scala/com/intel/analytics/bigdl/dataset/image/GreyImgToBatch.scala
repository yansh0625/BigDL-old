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

package com.intel.analytics.bigdl.dataset.image

import com.intel.analytics.bigdl.dataset.{Batch, Transformer}
import com.intel.analytics.bigdl.tensor.{Storage, Tensor}

import scala.collection.Iterator

object GreyImgToBatch {
  def apply(batchSize : Int) : GreyImgToBatch = {
    new GreyImgToBatch(batchSize)
  }
}

class GreyImgToBatch(batchSize: Int)
  extends Transformer[LabeledGreyImage, Batch[Float]] {

  private def copyImage(img: GreyImage, storage: Array[Float], offset: Int): Unit = {
    val content = img.content
    val frameLength = img.width() * img.height()
    var j = 0
    while (j < frameLength) {
      storage(offset + j) = content(j)
      j += 1
    }
  }

  override def apply(prev: Iterator[LabeledGreyImage]): Iterator[Batch[Float]] = {
    new Iterator[Batch[Float]] {
      private val featureTensor: Tensor[Float] = Tensor[Float]()
      private val labelTensor: Tensor[Float] = Tensor[Float]()
      private var featureData: Array[Float] = null
      private var labelData: Array[Float] = null
      private var width = 0
      private var height = 0

      override def hasNext: Boolean = prev.hasNext

      override def next(): Batch[Float] = {
        if (prev.hasNext) {
          var i = 0
          while (i < batchSize && prev.hasNext) {
            val img = prev.next()
            if (featureData == null) {
              featureData = new Array[Float](batchSize * img.height() * img.width())
              labelData = new Array[Float](batchSize)
              height = img.height()
              width = img.width()
            }
            copyImage(img, featureData, i * img.width() * img.height())
            labelData(i) = img.label()
            i += 1
          }
          if (labelTensor.nElement() != i) {
            featureTensor.set(Storage[Float](featureData),
              storageOffset = 1, sizes = Array(i, height, width))
            labelTensor.set(Storage[Float](labelData),
              storageOffset = 1, sizes = Array(i))
          }
          Batch(featureTensor, labelTensor)
        } else {
          null
        }
      }
    }
  }
}
