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

import java.util.concurrent.ConcurrentHashMap

import scalacache._
import scalacache.memoization.memoizeF
import scalacache.modes.scalaFuture

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

object Store {

  /**
    * Store constructor.
    *
    * @param provider function to provide resource if not available
    * @param cache cache implementation to use as the storage
    * @param flags cache storage flags
    * @param ttl time to leave for memoized values
    * @tparam T type of cache entry
    * @return an instance of Store
    */
  def apply[T](
      provider: String ⇒ Future[T]
  )(implicit cache: Cache[T], flags: Flags = Flags.defaultFlags, ttl: Option[Duration] = None): Store[T] =
    new Store[T](provider, cache, flags, ttl)
}

/**
  * A store implementation which will return stored value or if value is not available it will execute provider function.
  *
  * @param provider function to provide resource if not available
  * @param cache cache implementation to use as the storage
  * @param flags cache storage flags
  * @param ttl time to leave for memoized values
  * @tparam T type of cache entry
  */
class Store[T](provider: String ⇒ Future[T], cache: Cache[T], flags: Flags, ttl: Option[Duration]) {

  // Holds references to in flight calls to prevent concurrent providers for a same resource
  private val inflight = new ConcurrentHashMap[String, Future[T]]()

  /**
    * Returns stored value or if value is not available will execute provider function.
    *
    * If multiple request for the same key are issued only one will trigger provider function,
    * rest of them will piggyback on created request.
    *
    * @param key cached key
    * @param executionContext execution context
    * @return Future[T] with a value which is stored locally or retrieved using provider
    */
  def get(key: String)(implicit executionContext: ExecutionContext): Future[T] =
    memoizeF(ttl) { populate(key) }(cache, scalaFuture.mode, flags)

  private def populate(name: String)(implicit executionContext: ExecutionContext): Future[T] =
    inflight.computeIfAbsent(name, name ⇒ {
      val f = provider(name)
      f.onComplete(_ ⇒ inflight.remove(name))
      f
    })
}
