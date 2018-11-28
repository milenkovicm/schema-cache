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

import java.util.concurrent.atomic.AtomicInteger

import com.github.fge.jsonschema.main.JsonSchema
import com.github.milenkovicm.SimpleSpec
import scalacache.Cache
import scalacache.caffeine.CaffeineCache

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }

class StoreSpec extends SimpleSpec {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  val schema =
    """
      {
        "definitions": {},
        "$schema": "http://json-schema.org/draft-07/schema#",
        "$id": "http://example.com/root.json",
        "type": "object",
        "title": "The Root Schema",
        "required": [
          "key"
        ],
        "properties": {
          "key": {
            "$id": "#/properties/key",
            "type": "string",
            "title": "The Key Schema",
            "default": "",
            "examples": [
              "value"
            ],
            "pattern": "^(.*)$"
          }
        }
      }
    """

  "LocalStore" should {
    "memoize result" in {
      implicit val caffeineCache: Cache[String] = CaffeineCache[String]

      val atomic = new AtomicInteger()
      val store  = Store(s ⇒ Future { atomic.incrementAndGet(); s.toLowerCase() })

      val a = for {
        a1 ← store.get("A")
        a2 ← store.get("A")
        a3 ← store.get("A")
      } yield (a1, a2, a3)

      Await.ready(a, 3.seconds)
      atomic.get() shouldBe 1
    }

    "validate " in {
      implicit val caffeineCache: Cache[JsonSchema] = CaffeineCache[JsonSchema]

      val good = Json.objectMapper.readTree("""{"key": "value"}""")
      val bad  = Json.objectMapper.readTree("""{"key": 123}""")

      val validatorStore = Store(
        _ ⇒ // wont use schema name as we're returning single schema
          Future { schema }
            .map(s ⇒ Json.objectMapper.readTree(s)) // parse failure should be handled
            .map(j ⇒ Json.validationFactory.getJsonSchema(j)) // invalid schema should be handled
            .recover { // we should handle all possible errors,
              case e ⇒
                sys.error("recovering from error: " + e.getMessage)
                Json.defaultSchemaValidator // use default schema validator, in case there is issue with
          }
      )

      val resultGood = validatorStore
        .get("")
        .map(_.validate(good).isSuccess)

      val resultBad = validatorStore
        .get("")
        .map(_.validate(bad).isSuccess)

      Await.result(resultGood, 3.seconds) shouldBe true
      Await.result(resultBad, 3.seconds) shouldBe false

    }
  }
}
