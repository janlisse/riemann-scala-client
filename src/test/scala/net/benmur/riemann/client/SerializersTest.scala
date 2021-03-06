package net.benmur.riemann.client

import scala.collection.JavaConversions.asJavaIterable

import org.scalatest.FunSuite
import org.scalatest.Matchers

import com.aphyr.riemann.Proto

import testingsupport.SerializersFixture.event1
import testingsupport.SerializersFixture.event2
import testingsupport.SerializersFixture.protobufEvent1
import testingsupport.SerializersFixture.protobufEvent2

class SerializersTest extends FunSuite with Matchers {
  object Serializers extends Serializers
  import Serializers._
  import testingsupport.SerializersFixture._

  test("out: convert a full EventPart to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(protobufEvent1).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(event1)
    }
  }

  test("out: convert an empty EventPart to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart())
    }
  }

  test("out: convert an EventPart with only host to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setHost("host")).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(host = Some("host")))
    }
  }

  test("out: convert an EventPart with only service to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setService("service")).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(service = Some("service")))
    }
  }

  test("out: convert an EventPart with only state to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setState("state")).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(state = Some("state")))
    }
  }

  test("out: convert an EventPart with only time to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setTime(1234L)).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(time = Some(1234L)))
    }
  }

  test("out: convert an EventPart with only description to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setDescription("description")).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(description = Some("description")))
    }
  }

  test("out: convert an EventPart with only tags to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.addAllTags(List("tag1"))).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(tags = List("tag1")))
    }
  }

  test("out: convert an EventPart setting only metric (long) to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setMetricSint64(1234L).setMetricF(1234f)).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(metric = Some(1234L)))
    }
  }

  test("out: convert an EventPart setting only metric (double) to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setMetricD(1234.9).setMetricF(1234.9f)).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(metric = Some(1234.9)))
    }
  }

  test("out: convert an EventPart setting only metric (float) to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setMetricF(1234.9f)).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(metric = Some(1234.9f)))
    }
  }

  test("out: convert an EventPart with only ttl to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setTtl(1234L)).build

    assertResult(expected) {
      serializeEventPartToProtoMsg(EventPart(ttl = Some(1234L)))
    }
  }

  test("out: convert an Iterable of full EventParts to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(protobufEvent1).addEvents(protobufEvent2).build

    assertResult(expected) {
      serializeEventPartsToProtoMsg(EventSeq(event1, event2))
    }
  }

  test("out: convert a Query to a protobuf Msg") {
    assertResult(Proto.Msg.newBuilder.setQuery(Proto.Query.newBuilder.setString("true")).build) {
      serializeQueryToProtoMsg(Query("true"))
    }
  }

  test("in: convert a protobuf Msg response with an ok status") {
    assertResult(Nil) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).build)
    }
  }

  test("in: convert a protobuf Msg response with a non-ok status and an error message") {
    val ex = intercept[RemoteError] {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(false).setError("meh").build)
    }
    ex.message should === ("meh")
  }

  test("in: convert a failed Query result from a protobuf Msg with events") {
    val ex = intercept[RemoteError] {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(false).setError("meh").addEvents(protobufEvent1).build)
    }
    ex.message should === ("meh")
  }

  test("in: convert a successful Query result from a protobuf Msg to multiple EventParts") {
    assertResult(List(event1)) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(protobufEvent1).build)
    }
  }

  test("in: convert a Query result with missing ok from a protobuf Msg to RemoteError") {
    val ex = intercept[RemoteError] {
      unserializeProtoMsg(Proto.Msg.newBuilder.addEvents(protobufEvent1).build)
    }
    ex.message should === ("Response has no status")
  }

  test("in: convert a protobuf Msg with empty Event to a List(EventPart)") {
    assertResult(List(EventPart())) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only host to a List(EventPart)") {
    assertResult(List(EventPart(host = Some("host")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setHost("host")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only service to a List(EventPart)") {
    assertResult(List(EventPart(service = Some("service")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setService("service")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only state to a List(EventPart)") {
    assertResult(List(EventPart(state = Some("state")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setState("state")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only time to a List(EventPart)") {
    assertResult(List(EventPart(time = Some(1234L)))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setTime(1234L)).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only description to a List(EventPart)") {
    assertResult(List(EventPart(description = Some("description")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setDescription("description")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only tags to a List(EventPart)") {
    assertResult(List(EventPart(tags = List("tag1", "tag2")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.addAllTags(List("tag1", "tag2"))).build)
    }
  }

  test("in: convert a protobuf Msg with Event with metrics (long) and (float) to a List(EventPart)") {
    val expected = List(EventPart(metric = Some(1234L)))
    val actual = unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setMetricSint64(1234L).setMetricF(1234)).build)
    assertResult(expected)(actual)
    assertResult(expected.map(_.metric.map(_.getClass))) {
      actual.map(_.metric.map(_.getClass))
    }
  }

  test("in: convert a protobuf Msg with Event with only metric (float) to a List(EventPart)") {
    val expected = List(EventPart(metric = Some(1234.0f)))
    val actual = unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setMetricF(1234.0f)).build)
    assertResult(expected)(actual)
    assertResult(expected.map(_.metric.map(_.getClass))) {
      actual.map(_.metric.map(_.getClass))
    }
  }

  test("in: convert a protobuf Msg with Event with metrics (double) and (float) to a List(EventPart)") {
    val expected = List(EventPart(metric = Some(1234.1)))
    val actual = unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setMetricD(1234.1).setMetricF(1234.1f)).build)
    assertResult(expected)(actual)
    assertResult(expected.map(_.metric.map(_.getClass))) {
      actual.map(_.metric.map(_.getClass))
    }
  }

  test("in: convert a protobuf Msg with Event with only ttl to a List(EventPart)") {
    assertResult(List(EventPart(ttl = Some(1234L)))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setTtl(1234L)).build)
    }
  }
}
