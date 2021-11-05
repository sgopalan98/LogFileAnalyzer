package com.loganalyzer


import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.loganalyzer.LogAnalyzer.AnalyzeRequest
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}

import java.util
import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import java.util.Base64
import java.nio.charset.StandardCharsets
import com.google.gson.Gson
import com.helperutils.Parameters

import scala.collection.immutable.HashMap
import scala.concurrent.duration.Duration
import scala.concurrent.duration.*



object HttpClient {

  def main(args: Array[String]): Unit = {
    //Creating a HttpClient with AWS Lambda endpoint
    val client = new HttpClient(Parameters.ADDRESS)
    //Get the input to query and call the Http request
    try {
      //Getting the input from commandline args
      if(args.nonEmpty && args.length == 2){
          val time = args(0)
          val dt = args(1)
          client.analyze(time, dt)
      }
        //Using the default args
      else{
        val time = Parameters.DEFAULT_INPUT_TIME
        val dt = Parameters.DEFAULT_INPUT_DT
        client.analyze(time, dt)
      }
    }
    catch{
      case e: Exception => client.getLogger().info(Parameters.EXCEPTION_MESSAGE+e)
    }
  }
}

class HttpClient (private val address: String){
  //Create a logger to set INFO
  private[this] val logger = Logger.getLogger(classOf[HttpClient].getName)


  def getLogger() = {
    logger
  }


  def analyze(time: String, dt: String): String = {

    //Setting the system and context for the Akka Http call

    implicit val system = ActorSystem(Behaviors.empty, Parameters.SINGLE_REQUEST)
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    //Creating an HttpRequest and send it.
    val query = Map(Parameters.TIME_KEY -> time, Parameters.DT_KEY -> dt)
    val response = Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      uri = Uri(s"${address}").withQuery(Query(query))
    ))
    //When result is complete, get the result and log the message
    val gson = new Gson()
    val futureString = response
      .flatMap(_.entity.toStrict(Parameters.MIN_DURATION.seconds))
      .map(_.data.utf8String)
    val maxTime = Duration(Parameters.MIN_DURATION, TimeUnit.SECONDS)
    //Doing a blocking call to get the result
    val finalResult: String = Try(Await.result(futureString, maxTime)) match{
      case Success(str) => {
        val json = gson.fromJson(str, classOf[util.Map[String, Object]])
        json.getOrDefault(Parameters.RETURN_VALUE_KEY,Parameters.DEFAULT_RETURN).toString
      }
      case Failure(str) => Parameters.FAILED_MESSAGE
    }
    //Logging the result
    logger.info(Parameters.PRINT_RESULT_MESSAGE + finalResult)
    return finalResult
  }
}
