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
package com.intel.analytics.bigdl.models.googlenet

import java.nio.file.Path

import com.intel.analytics.bigdl.dataset._
import com.intel.analytics.bigdl.dataset.image._
import com.intel.analytics.bigdl.tensor.Tensor
import org.apache.spark.SparkContext

object ImageNet2012 {
  def localFolder(path : Path, imageSize : Int, batchSize : Int, parallel: Int)
  : LocalDataSet[Batch[Float]] = {
    val ds = DataSet.SequenceFolder.paths(path, 1281167)
    val fileTransformer = LocalSeqFileToBytes()
    val arrayToImage = SampleToRGBImg()
    val cropper = RGBImgCropper(cropWidth = imageSize, cropHeight = imageSize)
    val normalizer = RGBImgNormalizer(0.485, 0.456, 0.406, 0.229, 0.224, 0.225)
    val flipper = HFlip(0.5)
    val multiThreadToTensor = MTLabeledRGBImgToBatch[SeqFileLocalPath](
      width = imageSize,
      height = imageSize,
      batchSize = batchSize,
      transformer = fileTransformer -> arrayToImage -> cropper -> flipper -> normalizer
    )
    ds -> multiThreadToTensor
  }

  def hdfs(
    path : String,
    sc : SparkContext,
    imageSize : Int,
    batchSize : Int,
    nodeNumber: Int,
    coresPerNode : Int)
  : DistributedDataSet[Batch[Float]] = {
    require(batchSize % nodeNumber == 0, "batch size can't be divided by node number")
    val ds = DataSet.SequenceFolder.files(path, sc, 1000, nodeNumber)
    val arrayToImage = SampleToRGBImg()
    val cropper = RGBImgCropper(cropWidth = imageSize, cropHeight = imageSize)
    val normalizer = RGBImgNormalizer(0.485, 0.456, 0.406, 0.229, 0.224, 0.225)
    val flipper = HFlip(0.5)
    val multiThreadToTensor = MTLabeledRGBImgToBatch[Sample](
      width = imageSize,
      height = imageSize,
      batchSize = batchSize / nodeNumber,
      transformer = arrayToImage -> cropper -> flipper -> normalizer
    )
    ds -> multiThreadToTensor
  }
}
