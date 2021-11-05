package com.logfileanalyzer.test


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.helperutils._
import com.loganalyzer._

class TestSuite1 extends AnyFlatSpec with Matchers {

  behavior of "Configuration getters"

  "The Http Server" should "return failure message" in {
    val client  = new HttpClient(Parameters.ADDRESS)
    val invalidInputTime = "00:00:00"
    val invalidDifferential = "00:00:00"
    val message = client.analyze(invalidInputTime, invalidDifferential)
    message shouldBe Parameters.DEFAULT_RETURN
  }

  "The Http Server" should "not return failure messgae" in {
    val client  = new HttpClient(Parameters.ADDRESS)
    val invalidInputTime = Parameters.DEFAULT_INPUT_TIME
    val invalidDifferential = Parameters.DEFAULT_INPUT_DT
    val message = client.analyze(invalidInputTime, invalidDifferential)
    message should not be Parameters.DEFAULT_RETURN
  }

  it should "get the correct URL for Lambda" in {
    val addressFromParams = Parameters.ADDRESS
    val config = ObtainConfigReference("LogAnalyzer") match {
      case Some(value) => value
      case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
    }
    val addressFromConfig = config.getString("LogAnalyzer.ADDRESS")
    addressFromParams shouldBe addressFromConfig
  }

  it should "get the keys for GET request" in {
    val param1FromParams = Parameters.TIME_KEY
    val param2FromParams = Parameters.DT_KEY
    val config = ObtainConfigReference("LogAnalyzer") match {
      case Some(value) => value
      case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
    }
    val param1FromConfig = config.getString("LogAnalyzer.TIME_KEY")
    val param2FromConfig = config.getString("LogAnalyzer.DT_KEY")
    param1FromParams shouldBe param1FromConfig
    param2FromParams shouldBe param2FromConfig
  }

  it should "get the correct port for gRPC" in {
    val grpcPortFromParams = Parameters.GRPC_PORT_NUMBER
    val config = ObtainConfigReference("LogAnalyzer") match {
      case Some(value) => value
      case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
    }
    val grpcPortFromConfig = config.getInt("LogAnalyzer.GRPC_PORT_NUMBER")
    grpcPortFromParams shouldBe grpcPortFromConfig
  }


}
