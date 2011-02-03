package http

import org.specs._
import scala.actors._
import java.io.InputStream

class HttpSpec extends Specification {

  "http client" should {

    "get google.com successfully" in {
      val httpOp = Http(
        method="GET"
        ,url = "http://google.com" 
        ,onComplete = (status:Int, _, _) => (status == 200)
      )

      httpOp() fold( 
         _=>fail
        ,(x:Boolean)=> x must be equalTo(true)
      )
    }

    "return error when connecting http://g0o0o0gle.com" in {
      val httpOp = Http(
        method="GET"
        ,url = "http://g0o0o0gle.com"
        ,onComplete = (_, _, _) => {}
      )

      httpOp() fold(
        e  => e must haveClass[ConnectionFailed]
        , _=> fail
      )
    }

  }

}

