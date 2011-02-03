Scala HTTP wrapper for Jetty HTTP client
========================================

Usage
-----

Simple GET request:

    import http._

    ...

    Http(
      method = "GET"
      ,url = "http://google.com"
      ,onComplete = (status:Int, _, _) => println ("Google responded with "+status)
    )

onComplete takes three arguments:

* status:Int — HTTP response status
* headers:Map[String,String] — HTTP response headers
* stream:java.io.InputStream — Input stream with HTTP response body

Let's add some extra headers:

    Http(
      ...
      ,headers = Map(
        "User-Agent"->"ScalaBOT 1.0"
        ,"Content-Type"->"application/json"
      )
    )

Let's include a request body:

    Http(
      ...
      ,content = Some(myInputStream)
    )

Http.apply (...) returns a value of Future[Either[HttpError,T]] type, where T is the type returned by your
onComplete function.
