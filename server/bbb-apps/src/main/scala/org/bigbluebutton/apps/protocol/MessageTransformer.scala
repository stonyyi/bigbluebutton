package org.bigbluebutton.apps.protocol

import spray.json.JsObject
import spray.json.JsValue
import spray.json.DefaultJsonProtocol
import spray.json.JsonParser
import org.parboiled.errors.ParsingException
import akka.event.LoggingAdapter
import akka.event.slf4j.SLF4JLogging
import scala.util.Try
import spray.json.DeserializationException
import org.bigbluebutton.apps.protocol.HeaderAndPayloadJsonSupport._
import org.bigbluebutton.apps.users.unmarshalling.UsersAppMessageHandler
import org.bigbluebutton.meeting.MeetingMessageHandler

object MessageTransformer extends MeetingMessageHandler 
                          with UsersAppMessageHandler
                          with SLF4JLogging {  

  /**
   * Extract the header from the message.
   * 
   * @ returns the header is successful
   */
  def extractMessageHeader(msg: JsObject):Header = {
    try {
      msg.fields.get("header") match {
        case Some(header) => header.convertTo[Header]
        case None => throw MessageProcessException("Cannot get header: [" + msg + "]")
     }
    } catch {
      case e: DeserializationException =>
        throw MessageProcessException("Failed to deserialize header: [" + msg + "]")
    }
  }
 
  /**
   * Extract the payload from the message.
   * 
   * @returns the payload if successful
   */
  def extractPayload(msg: JsObject): JsObject = {
    msg.fields.get("payload") match {
      case Some(payload) => payload.asJsObject
      case None => throw MessageProcessException("Cannot get payload information: [" + msg + "]")
    } 
  }
  
  /**
   * Converts a JSON string into JsObject.
   */
  def jsonMessageToObject(msg: String): JsObject = {
    log.debug("Converting to json : {}", msg)    
    try {
      JsonParser(msg).asJsObject
    } catch {
      case e: ParsingException => {
        log.error("Cannot parse message: {}", msg)
        throw MessageProcessException("Cannot parse JSON message: [" + msg + "]")
      }
    }
  }

  def processMessage(header: Header, payload:JsObject):HeaderAndPayload = {
    HeaderAndPayload(header, payload)
  }
    
  def transformMessage(jsonMsg: String):Try[HeaderAndPayload] = {
    for {
      jsonObj <- Try(jsonMessageToObject(jsonMsg))
      header <- Try(extractMessageHeader(jsonObj))
      payload <- Try(extractPayload(jsonObj))
      message = processMessage(header, payload)
    } yield message
  }
  

  
}