/*
 * Copyright 2021 4Paradigm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com._4paradigm.openmldb.batch.end2end.unsafe

import com._4paradigm.openmldb.batch.SparkTestSuite
import com._4paradigm.openmldb.batch.api.OpenmldbSession
import com._4paradigm.openmldb.batch.end2end.DataUtil
import com._4paradigm.openmldb.batch.utils.SparkUtil

class TestUnsafeJoin extends SparkTestSuite {

  override def customizedBefore(): Unit = {
    val spark = getSparkSession
    spark.conf.set("spark.openmldb.unsaferow.opt", true)
  }

  test("Test unsafe join") {
    val spark = getSparkSession
    val sess = new OpenmldbSession(spark)

    val df = DataUtil.getTestDf(spark)
    sess.registerTable("t1", df)
    sess.registerTable("t2", df)
    df.createOrReplaceTempView("t1")
    df.createOrReplaceTempView("t2")

    val sqlText = "SELECT t1.id as t1_id, t2.id as t2_id, t1.name FROM t1 LEFT JOIN t2 ON t1.id = t2.id"

    val outputDf = sess.sql(sqlText)
    val sparksqlOutputDf = sess.sparksql(sqlText)
    assert(SparkUtil.approximateDfEqual(outputDf.getSparkDf(), sparksqlOutputDf, false))
  }

  override def customizedAfter(): Unit = {
    val spark = getSparkSession
    spark.conf.set("spark.openmldb.unsaferow.opt", false)
  }

}
