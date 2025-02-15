/* -- copyright-header:v2 --
 * Copyright (C) 2017-2021 University at Buffalo,
 *                         New York University,
 *                         Illinois Institute of Technology.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -- copyright-header:end -- */
package info.vizierdb.commands

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAll
import info.vizierdb.test.SharedTestResources
import org.specs2.specification.core.Fragments
import info.vizierdb.viztrails.MutableProject
import org.apache.spark.sql.types.FloatType

class PythonSaveDatasetSpec
  extends Specification
  with BeforeAll
{
  def beforeAll = SharedTestResources.init

  Fragments.foreach(Seq(
    "Raw Data Transfer" -> "False",
    "Vizual Log Transfer" -> "True"
  )) { case (test, use_deltas) =>

    test >> {

      val project = MutableProject(s"Python $test")
      project.load("test_data/r.csv", "R")
      project.script(s"""
        |ds = vizierdb["R"]
        |
        |for row in ds.rows:
        |  if row["B"] is None:
        |    print(row.identifier)
        |    row["B"] = 42
        |ds.insert_column("D", "float")
        |ds.insert_row([11, 12, 13, 14.0])
        |ds.save(use_deltas = $use_deltas)
        """.stripMargin)
      project.waitUntilReadyAndThrowOnError
      val ds = project.artifact("R").getDataset()
      println(s"Read artifact #${project.artifact("R").id}")
      ds.data.map { _(1) } must contain(42)
      ds.data.filter { _ != null } must haveSize(ds.data.size)
      ds.schema(3).name must beEqualTo("D")
      ds.schema(3).dataType must beEqualTo(FloatType)
      ds.data.map { _(3) } must contain(14.0)
    }
  }

  "Save Raw Datasets" >> 
  {
    val project = MutableProject("Create Dataset Test")
    project.script(s"""
        |ds = vizierdb.new_dataset()
        |ds.insert_column("A")
        |ds.insert_column("B")
        |ds.insert_row(["a1","b1"])
        |ds.insert_row(["a2","b2"])
        |ds.save("Moo")
        """.stripMargin
    )
    project.waitUntilReadyAndThrowOnError
    val ds = project.artifact("Moo").getDataset()
    ds.schema(0).name must beEqualTo("A")
    ds.schema(1).name must beEqualTo("B")
    ds.data.map { _(0) } must contain("a1", "a2")
  }
}