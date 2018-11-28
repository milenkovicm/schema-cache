# Schema Cache [![Build Status](https://api.travis-ci.org/milenkovicm/schema-cache.svg?branch=master)](https://travis-ci.org/milenkovicm/schema-cache)

An simple example how to to load a (json) data definitions (schemas) from remote repository and save them in a local store.
Any subsequent calls should be served from the local store, unless data retention policy have not removed local copy.

It is based on [scala cache](https://cb372.github.io/scalacache/). 

Examples use [caffeine cache](https://github.com/ben-manes/caffeine)
as local cache and [akka http](https://doc.akka.io/docs/akka-http/current/) to fetch data from a remote
schema repository.

For the purpose of demonstration I've used [json schema validator](https://github.com/java-json-tools/json-schema-validator)
to demonstrate how to validate [jackson (json) objects](https://github.com/FasterXML/jackson) using remote 
schema definition.  

List of supported cache implementation could be found at http://cb372.github.io/scalacache/docs/cache-implementations.html


```scala
implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
implicit val caffeineCache: Cache[JsonSchema] = CaffeineCache[JsonSchema]

val validatorStore = Store(
_ ⇒ // probably you'd use schema name here
  Future { schema }
    .map(s ⇒ Json.objectMapper.readTree(s)) // parse failure should be handled
    .map(j ⇒ Json.validationFactory.getJsonSchema(j)) // invalid schema should be handled
    .recover { // we should handle all possible errors,
      case e ⇒
        sys.error("recovering from error: " + e.getMessage)
        Json.defaultSchemaValidator // use default schema validator, in case there is issue with
  }
)
val goodJson = Json.objectMapper.readTree("""{"key": "value"}""")
val result = validatorStore
.get("")
.map(_.validate(goodJson).isSuccess)

Await.result(result, 3.seconds) shouldBe true
```

Simple HTTP provider is implemented in `com.github.milenkovicm.schema.cache.HttpExample`

## Contribution policy

Contributions via GitHub pull requests are gladly accepted from their original author. Along with
any pull requests, please state that the contribution is your original work and that you license
the work to the project under the project's open source license. Whether or not you state this
explicitly, by submitting any copyrighted material via pull request, email, or other means you
agree to license the material under the project's open source license and warrant that you have the
legal authority to do so.

## License

This code is open source software licensed under the
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) license.


