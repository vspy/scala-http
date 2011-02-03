package http

import scala.annotation._
import scala.actors._
import java.io.{InputStream, ByteArrayInputStream}
import org.eclipse.jetty.client.{
  HttpClient=>HC
  ,HttpExchange=>HE
  ,ContentExchange=>CE
}
import org.eclipse.jetty.http.HttpFields

class HttpError(message:String, reason:Option[Throwable] = None ) {
  override def toString = {
    val b = new StringBuilder
    b.append("HttpError[")
    b.append(message)
    if (reason.isDefined) {
      val t = reason.get
      b.append(", reason: ")
      b.append(t.getClass)
      b.append(" (")
      b.append(t.getMessage)
      b.append(")")      
    }
    b.append("]")
    b.toString
  }
}
object InvalidRedirect extends HttpError("Invalid redirect")
object TooManyRedirects extends HttpError("Too many redirects")
case class ConnectionFailed(t:Throwable) extends HttpError("Connection failed",Some(t))
object ConnectionTimedOut extends HttpError("Connection timed out")

object Http {
  val client = new HC()
  client.setConnectorType(HC.CONNECTOR_SELECT_CHANNEL)
  client.start()
  
  private object Eval

  def apply[A](
    method : String
    ,url : String
    ,onComplete : (Int,Map[String,Any],InputStream) => A = ((_:Int,_:Map[String,Any],_:InputStream)=>{})
    ,headers : Seq[Tuple2[String, String]] = Nil
    ,content : Option[InputStream] = None
    ,followRedirects : Boolean = true
    ,maxRedirects : Int = 5
  ):Future[Either[HttpError,A]] = {

    val future = new Future[Either[HttpError,A]] with DaemonActor {
      var result : Option[Either[HttpError,A]] = None

      private def exchange = new CE(true) {

        override def onExpire =
          result = Some(Left(ConnectionTimedOut))

        override def onException(t:Throwable) =
          result = Some(Left(ConnectionFailed(t)))

        override def onConnectionFailed(t:Throwable) =
          result = Some(Left(ConnectionFailed(t)))

        override def onResponseComplete() = 
          result = Some({
            val fields = getResponseFields
            @tailrec
            def allFields(i:Int=0,l:List[Tuple2[String, String]]=List()):List[Tuple2[String, String]] =
              if (i < fields.size) {
                val f = fields.getField(i)
                allFields(i+1, (f.getName->f.getValue)::l)
              } else l.reverse

            val map = Map( allFields():_* )
            val status = getResponseStatus
  
            if (followRedirects && ( status == 301 || status == 302 ) )
              if (maxRedirects>0) 
                (for(l<-map.get("Location")) yield 
                  Http(method,l,onComplete,headers,content,followRedirects,maxRedirects-1)()
                ) getOrElse Left(InvalidRedirect)
              else Left(TooManyRedirects) 
            else {
              Right(onComplete(
                status
                ,map
                ,new ByteArrayInputStream(getResponseContentBytes())
              ))
            }
          })
        }

      def apply(): Either[HttpError,A] = {
        if (result.isEmpty) {
          this !? Eval
        }
        result.get
      }
    
      def isSet = !result.isEmpty
    
      def respond(k: Either[HttpError,A] => Unit) =
        if (isSet) k(result.get)
        else {
          val ft = this !! Eval
          ft.inputChannel.react {
            case _ => k(result.get)
          }
        }

      def act() =
        {
          val e = exchange
          e.setMethod(method)
          e.setURL(url)

          for ( (k,v) <- headers ) { 
            e.setRequestHeader(k,v) 
          }
          if (content.isDefined ) { 
            e.setRequestContentSource(content.get) 
          }

          client.send(e)
          e.waitForDone
        } andThen {
          loop {
            react {
              case Eval => reply()
            }
          }   
        }

      def inputChannel = null
    }

    future.start
    future 
  }

}



