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
package info.vizierdb.api

import scalikejdbc.DB
import play.api.libs.json._
import org.mimirdb.api.{ Request, Response }
import info.vizierdb.types.Identifier
import info.vizierdb.catalog.Branch
import javax.servlet.http.HttpServletResponse
import info.vizierdb.api.response._
import info.vizierdb.viztrails.Scheduler
import info.vizierdb.api.handler.SimpleHandler

object DeleteModuleHandler
  extends SimpleHandler
{
  def handle(pathParameters: Map[String, JsValue]): Response =
  {
    val projectId = pathParameters("projectId").as[Long]
    val branchId = pathParameters("branchId").as[Long]
    val workflowId = pathParameters.get("workflowId").map { _.as[Long] }
    val modulePosition = pathParameters("modulePosition").as[Int]
    val workflow = 
      DB.autoCommit { implicit s => 
        val branch = 
          Branch.getOption(projectId = projectId, branchId = branchId)
                 .getOrElse { 
                    return NoSuchEntityResponse()
                 }
        if(workflowId.isDefined) {
          if(branch.headId != workflowId.get){
            return VizierErrorResponse("Invalid", "Trying to modify an immutable workflow")
          }
        }
        val cellToDelete =
          branch.head.cellByPosition(modulePosition)
                .getOrElse {
                    return NoSuchEntityResponse()
                }
        branch.delete(cellToDelete.position)._2
      }

    // The workflow must be scheduled AFTER the enclosing transaction finishes
    Scheduler.schedule(workflow.id)

    DB.readOnly { implicit s => 
      return RawJsonResponse(
        Json.toJson(
          workflow.describe
        )
      )
    }
  }
}

