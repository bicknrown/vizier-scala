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
package info.vizierdb.commands.sample

import info.vizierdb.commands._
import play.api.libs.json._
import com.typesafe.scalalogging.LazyLogging
import org.mimirdb.api.request.CreateSampleRequest
import org.mimirdb.api.request.Sample.StratifiedOn
import org.mimirdb.api.MimirAPI
import org.mimirdb.spark.SparkPrimitive
import org.mimirdb.api.request.MaterializeRequest

object ManualStratifiedSample extends Command
  with LazyLogging
{
  val PAR_INPUT_DATASET = "input_dataset"
  val PAR_STRATIFICATION_COL = "stratification_column"
  val PAR_STRATA = "strata"
  val PAR_STRATUM_VALUE = "stratum_value"
  val PAR_SAMPLE_RATE = "sample_rate"
  val PAR_OUTPUT_DATASET = "output_dataset"
  val PAR_SEED = "seed"
  val PAR_MATERIALIZE = "materialize"

  def name: String = "Manually Stratified Sample"
  def parameters: Seq[Parameter] = Seq(
    DatasetParameter(id = PAR_INPUT_DATASET, name = "Input Dataset"),
    ColIdParameter(id = PAR_STRATIFICATION_COL, name = "Column"),
    ListParameter(id = PAR_STRATA, name = "Strata", components = Seq(
      StringParameter(id = PAR_STRATUM_VALUE, name = "Column Value"),
      DecimalParameter(id = PAR_SAMPLE_RATE, name = "Sampling Rate (0.0-1.0)"),
    )),
    StringParameter(id = PAR_OUTPUT_DATASET, required = false, name = "Output Dataset"),
    StringParameter(id = PAR_SEED, hidden = true, required = false, default = None, name = "Sample Seed"),
    BooleanParameter(id = PAR_MATERIALIZE, name = "Materialize", required = false, default = Some(true))
  )
  def format(arguments: Arguments): String = 
    s"CREATE ${arguments.pretty(PAR_SAMPLE_RATE)} SAMPLE OF ${arguments.get[String](PAR_INPUT_DATASET)}"+
    s"STRATIFIED ON ${arguments.get[String](PAR_STRATIFICATION_COL)}"+
    (if(arguments.contains(PAR_OUTPUT_DATASET)) {
      s" AS ${arguments.pretty(PAR_OUTPUT_DATASET)}"
    } else { "" })
  def title(arguments: Arguments): String = 
    arguments.getOpt[String](PAR_OUTPUT_DATASET)
             .map { output => s"Sample from ${arguments.pretty(PAR_INPUT_DATASET)} into $output" }
             .getOrElse { s"Sample from ${arguments.pretty(PAR_INPUT_DATASET)}" }
  def process(arguments: Arguments, context: ExecutionContext): Unit = 
  {
    val inputName = arguments.get[String](PAR_INPUT_DATASET)
    val outputName = arguments.getOpt[String](PAR_OUTPUT_DATASET)
                              .getOrElse { inputName }
    val seed = arguments.getOpt[String](PAR_SEED).map { _.toLong }
    val stratifyOn = arguments.get[String](PAR_STRATIFICATION_COL)
    val strata = arguments.getList(PAR_STRATA)
                          .map { row => 
                            JsString(row.get[String](PAR_STRATUM_VALUE)) -> 
                              row.get[Double](PAR_SAMPLE_RATE)
                          }
    val probability = arguments.get[Float](PAR_SAMPLE_RATE)

    val input = context.dataset(inputName)
                       .getOrElse { throw new IllegalArgumentException(s"No such dataset $inputName")}
    val (output, _) = context.outputDataset(outputName)

    context.message("Registering sample...")
    val response = CreateSampleRequest(
      source = input,
      samplingMode = StratifiedOn(stratifyOn, strata),
      seed = seed,
      resultName = Some(output),
      properties = None
    ).handle

    context.updateArguments(PAR_SEED -> response.seed.toString)

    if(arguments.get[Boolean](PAR_MATERIALIZE)){
      context.message("Materializing sample...")
      val (materialized, _) = context.outputDataset(outputName)
      val response = MaterializeRequest(
        table = output,
        resultName = Some(materialized)
      ).handle
    }

    context.message("Sample created")
  }

  def predictProvenance(arguments: Arguments) = 
    Some( (Seq(arguments.get[String](PAR_INPUT_DATASET)), 
           Seq(arguments.getOpt[String](PAR_OUTPUT_DATASET)
                        .getOrElse { arguments.get[String](PAR_INPUT_DATASET) })) )
}

