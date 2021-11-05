package com.loganalyzer

import java.util.logging.Logger
import com.loganalyzer.LogAnalyzer.{AnalyzeReply, AnalyzeRequest, AnalyzerGrpc}
import io.grpc.{Server, ServerBuilder}

import scala.concurrent.{Await, ExecutionContext, Future}
import java.nio.charset.StandardCharsets
import java.util.Base64
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.google.gson.Gson
import com.helperutils.Parameters
import com.loganalyzer.gRPCServer.logger

import java.util
import java.util.concurrent.TimeUnit
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.*

//Object for gRPC server which runs locally
object gRPCServer {
  private val logger = Logger.getLogger(classOf[gRPCServer].getName)

  def main(args: Array[String]): Unit = {
    // Creating the server object
    val server = new gRPCServer(ExecutionContext.global)
    //Creating the server and making it blocked to not shut it down
    server.start()
    server.blockUntilShutdown()
  }

  private val port = Parameters.GRPC_PORT_NUMBER
}

class gRPCServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    //Creating a server to receive the message
    server = ServerBuilder.forPort(gRPCServer.port).addService(AnalyzerGrpc.bindService(new LogAnalyzerImpl, executionContext)).build.start
    gRPCServer.logger.info(Parameters.GRPC_SERVER_STARTING_LOG_MESSAGE + gRPCServer.port)
    sys.addShutdownHook {
      gRPCServer.logger.info(Parameters.GRPC_SHUTDOWN_MESSAGE)
      self.stop()
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }


  //Implementation of the gRPC stub
  private class LogAnalyzerImpl extends AnalyzerGrpc.Analyzer {
    //Overriding the function to analyze log
    override def analyzeLog(req: AnalyzeRequest) = {
      implicit val system = ActorSystem(Behaviors.empty, Parameters.SINGLE_REQUEST)
      // needed for the future flatMap/onComplete in the end
      implicit val executionContext = system.executionContext
      //Creating a HTTP request to the Lambda function
      val query = Map(Parameters.TIME_KEY -> req.time, Parameters.DT_KEY -> req.dt)
      val response = Http().singleRequest(HttpRequest(
        method = HttpMethods.GET,
        uri = Uri(Parameters.ADDRESS).withQuery(Query(query))
      ))
      //Extract the output as a JSON from the response
      val gson = new Gson()
      val futureString = response
        .flatMap(_.entity.toStrict(Parameters.MIN_DURATION.seconds))
        .map(_.data.utf8String)
      val maxTime = Duration(Parameters.MIN_DURATION, TimeUnit.SECONDS)
      //Blocking call - If string is obtained, convert that to JSON and get the required value for which key is mentioned
      val finalResult: String = Try(Await.result(futureString, maxTime)) match{
        case Success(str) => {
          println(str)
          val json = gson.fromJson(str, classOf[util.Map[String, Object]])
          json.getOrDefault(Parameters.RETURN_VALUE_KEY,Parameters.DEFAULT_RETURN).toString
        }
        case Failure(str) => Parameters.FAILED_MESSAGE
      }
      //Send the Output from the stub in the specified format that the client understands
      Future.successful(AnalyzeReply(finalResult))
    }
  }

}

