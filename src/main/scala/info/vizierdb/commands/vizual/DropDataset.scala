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
package info.vizierdb.commands.vizual

import info.vizierdb.commands._

object DropDataset extends Command
{
  def name: String = "Drop Dataset"
  def parameters: Seq[Parameter] = Seq(
    DatasetParameter(id = "dataset", name = "Dataset"),
  )
  def format(arguments: Arguments): String = 
    s"DROP DATASET ${arguments.get[String]("dataset")}"
  def title(arguments: Arguments): String = 
    s"Drop  ${arguments.get[String]("dataset")}"
  def process(arguments: Arguments, context: ExecutionContext): Unit = 
  {
    val datasetName = arguments.get[String]("dataset")
    context.delete(datasetName)
  }
  def predictProvenance(arguments: Arguments) = 
    Some( (
      Seq.empty,
      Seq(arguments.get[String]("dataset")) 
    ) )
}

