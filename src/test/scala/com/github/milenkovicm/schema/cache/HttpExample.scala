/*
 * Copyright 2018 Marko Milenkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.milenkovicm.schema.cache
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.{ DeserializationFeature, ObjectMapper }
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.fge.jsonschema.main.{ JsonSchema, JsonSchemaFactory }
import scalacache._
import scalacache.caffeine._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }

object HttpExample extends App {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  val good = Json.objectMapper.readTree("""{"key": "value"}""")
  val bad  = Json.objectMapper.readTree("""{"key": 123}""")

  val validatorStore = HttpStore()

  validatorStore
    .get("test-schema")
    .map(_.validate(good).isSuccess)
    .onComplete {
      case Success(res) => println(s"good is valid: $res")
      case Failure(_)   => sys.error("should not get here")
    }

  validatorStore
    .get("test-schema")
    .map(_.validate(bad).isSuccess)
    .onComplete {
      case Success(res) => println(s"bad is valid: $res")
      case Failure(_)   => sys.error("should not get here")
    }
}

// This example uses Jackson as Json object representation and JsonSchema as validator.
object Json {
  val objectMapper = (new ObjectMapper() with ScalaObjectMapper)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(DefaultScalaModule)
    .asInstanceOf[ObjectMapper with ScalaObjectMapper]

  val validationFactory = JsonSchemaFactory.byDefault()

  val defaultSchemaValidator = Json.validationFactory.getJsonSchema(Json.objectMapper.readTree("{}"))
}

// Simple example which will fetch data from a predefined http location if not in local cache
object HttpStore {

  // provide standard implicits for akka http
  implicit val system: ActorSystem                        = ActorSystem()
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // provide (scala) cache implementation
  // in this case caffeine cache.
  // implicit cache could be pre set with desired caching strategy
  implicit val caffeineCache: Cache[JsonSchema] = CaffeineCache[JsonSchema]

  def translateUrl(name: String) =
    s"https://gist.githubusercontent.com/milenkovicm/3b44b9012515ab008cfb845066d0ebc0/raw/865f6dce9a63e6120ca86fee937d78cda66bd686/$name.json"

  // function which will be executed to provide schemas definition if
  // not found in local cache
  def lookup(name: String): Future[JsonSchema] =
    Http()
      .singleRequest(HttpRequest(uri = translateUrl(name)))
      .map(_.entity)
      .flatMap(_.toStrict(1000.seconds))
      .map(_.data.utf8String)
      .map(s ⇒ Json.objectMapper.readTree(s)) // parse failure should be handled
      .map(j ⇒ Json.validationFactory.getJsonSchema(j)) // invalid schema should be handled
      .recover { // we should handle all possible errors,
        case e ⇒
          sys.error("recovering from error: " + e.getMessage)
          Json.defaultSchemaValidator // use default schema validator, in case there is issue with
      }

  def apply(): Store[JsonSchema] = Store(lookup)
}
