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

import java.io.File
import scalikejdbc.DB
import play.api.libs.json._
import java.io.InputStream
import info.vizierdb.VizierAPI
import info.vizierdb.catalog.{ Project, Artifact }
import org.mimirdb.api.{ Request, Response }
import info.vizierdb.types._
import info.vizierdb.artifacts.{ DatasetColumn, DatasetRow, DatasetAnnotation }
import org.mimirdb.api.request.LoadInlineRequest
import info.vizierdb.filestore.Filestore
import java.io.FileOutputStream
import info.vizierdb.util.Streams
import info.vizierdb.api.response._
import info.vizierdb.api.handler.{ Handler, ClientConnection }

object CreateFileHandler
  extends Handler
{
  override def filePart = Some("file")
  def handle(
    pathParameters: Map[String, JsValue], 
    connection: ClientConnection 
  ): Response =
  {
    val projectId = pathParameters("projectId").as[Long]
    val (content, filename) = connection.getPart("file")
    handle(projectId, content, filename)
  }
  def handle(
    projectId: Identifier,
    content: InputStream,
    filename: String
  ): Response = {
    DB.autoCommit { implicit s => 
      val project = 
        Project.getOption(projectId)
               .getOrElse { 
                  return NoSuchEntityResponse()
               }

      val artifact = Artifact.make(
        project.id, 
        ArtifactType.FILE,
        "application/octet-stream",
        Json.obj(
          "filename" -> filename
        ).toString.getBytes
      )

      val file: File = Filestore.getAbsolute(project.id, artifact.id)

      Streams.cat(content, new FileOutputStream(file))

      RawJsonResponse(
        artifact.summarize()
      )
    }
  } 

}

