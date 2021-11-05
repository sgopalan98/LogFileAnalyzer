package com.loganalyzer

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.helperutils.Parameters
import com.loganalyzer.LogAnalyzer.AnalyzeRequest
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}
import scala.util.{Failure, Success}
import com.loganalyzer.LogAnalyzer.{AnalyzeReply, AnalyzeRequest, AnalyzerGrpc}
import com.loganalyzer.LogAnalyzer.AnalyzerGrpc.AnalyzerBlockingStub


//Creating a gRPC client for analyzing the log
object gRPCClient {
  def apply(host: String, port: Int): gRPCClient = {
    //Creating a channel to communicate with the server hosted in this address
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val blockingStub = AnalyzerGrpc.blockingStub(channel)
    new gRPCClient(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    //Creating a gRPC client object
    val client = gRPCClient(Parameters.GRPC_SERVER_ADDRESS, Parameters.GRPC_PORT_NUMBER)
    try {
      //Input to be sent to the gRPC server
      if(args.nonEmpty && args.length == 2) {
        val time = args(0)
        val dt = args(1)
        client.analyzeLog(time, dt)
      }
      else{
        val time = Parameters.DEFAULT_INPUT_TIME
        val dt = Parameters.DEFAULT_INPUT_DT
        client.analyzeLog(time, dt)
      }
    } finally {
      client.shutdown()
    }
  }
}


//Class that receives the server stub and the channel as the input through which the request has to be sent
class gRPCClient private(private val channel: ManagedChannel,
                         private val blockingStub: AnalyzerBlockingStub){
    private[this] val logger = Logger.getLogger(classOf[gRPCClient].getName)

    def shutdown(): Unit = {
      channel.shutdown.awaitTermination(Parameters.CHANNEL_SHUTDOWN, TimeUnit.SECONDS)
    }

    //Function that calls the server stub
    def analyzeLog(time: String, dt:String): Unit = {

      //Creating a request object that is passed to the server stub
      val request = AnalyzeRequest(time = time, dt = dt)
      try {
        //Sending the request object to the server stub
        val response = blockingStub.analyzeLog(request)
        logger.info(Parameters.GRPC_CLIENT_OUTPUT_MESSAGE + response.message)
      }
      catch {
        case e: StatusRuntimeException =>
          logger.log(Level.WARNING, Parameters.GRPC_CLIENT_FAILED, e.getStatus)
      }
    }
}