package uk.gov.hmrc.apprenticeshiplevy

import com.github.tomakehurst.wiremock.client.WireMock.{status =>_,_}
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

@DoNotDiscover
class RootEndpointISpec extends WiremockFunSpec with ConfiguredServer {
  describe("Root Endpoint") {
    it(s"when accessed via privileged should return 401") {
        // set up
        val response = """{"uri":"/auth/session/xxxxx","confidenceLevel":500,"credentialStrength":"none","legacyOid":"N/A","currentLoginTime":"2016-12-08T09:28:42.155Z","enrolments":"/auth/session/xxxxx/enrolments","clientId":"yyyy","ttl":4}"""
        stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withBody(response)))
        val request = FakeRequest(GET, s"/").withHeaders(standardDesHeaders: _*)

        // test
        val result = route(app, request).get

        // check
        status(result) shouldBe 401
        contentType(result) shouldBe Some("application/json")
        contentAsString(result) shouldBe """{"code":"AUTH_ERROR_WRONG_TOKEN","message":"Auth unauthorised error: OAUTH 2 User Token Required not TOTP"}"""
    }

    val contexts = Seq("/sandbox", "")
    contexts.foreach { case (context) =>
      describe (s"should when calling ${localMicroserviceUrl}$context/") {
        describe (s"and backend systems not failing") {
          it (s"should return links for each empref") {
            // set up
            val response = fileToStr("./it/resources/data/input/mapping_body")
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withBody(response)))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe OK
            contentType(result) shouldBe Some("application/hal+json")
            val json = contentAsJson(result)
            (json \ "_links" \ "self" \ "href").as[String] shouldBe "/"
            (json \ "_links" \ "123/AB12345" \ "href").as[String] shouldBe "/epaye/123%2FAB12345"
          }
        }

        describe ("when errors occur") {
          it (s"should return 401 when Auth returns 401") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(401).withStatusMessage("Not authorised.")))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 401
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_UNAUTHORIZED","message":"Auth unauthorised error: GET of 'http://localhost:8080/auth/authority' returned 401. Response body: ''"}""")
          }

          it (s"should return 403 when Auth returns 403") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(403).withStatusMessage("Forbidden.")))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 403
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_FORBIDDEN","message":"Auth forbidden error: GET of 'http://localhost:8080/auth/authority' returned 403. Response body: ''"}""")
          }

          it (s"HTTP 404") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(404).withStatusMessage("Not found.")))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 404
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_NOT_FOUND","message":"Auth endpoint not found: GET of 'http://localhost:8080/auth/authority' returned 404 (Not Found). Response body: ''"}""")
          }
        }

        describe ("when backend systems failing") {
          it (s"should return http status 503 when connection closed") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 503
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_IO","message":"Auth connection error: Remotely closed"}""")
          }

          it (s"should return 503 when returning empty response and connection closed") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 503
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_IO","message":"Auth connection error: Remotely closed"}""")
          }

          it (s"should return 408 when timed out") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(200).withFixedDelay(1000*60)))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 408
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_GATEWAY_TIMEOUT","message":"Auth not responding error: GET of 'http://localhost:8080/auth/authority' timed out with message 'Request timeout to localhost/127.0.0.1:8080 after 500 ms'"}""")
          }

          it (s"should return 503 when Auth returns 500") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(500).withStatusMessage("Internal server error")))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 503
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_BACKEND_FAILURE","message":"Auth 5xx error: GET of 'http://localhost:8080/auth/authority' returned 500. Response body: ''"}""")
          }

          it (s"should return 503 when Auth returns 503") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(503).withStatusMessage("Backend systems failing")))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 503
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_BACKEND_FAILURE","message":"Auth 5xx error: GET of 'http://localhost:8080/auth/authority' returned 503. Response body: ''"}""")
          }

          it (s"should return http status 429 when Auth HTTP 429") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(429).withBody("""{"reason" : "Drowning in requests"}""")))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 429
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_TOO_MANY_REQUESTS","message":"Auth too many requests: Drowning in requests"}""")
          }

          it (s"should return http status 503 when Auth HTTP 409") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(411).withBody("""{"reason" : "Some Auth 411 error"}""")))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 503
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_OTHER","message":"Auth 4xx error: Some Auth 411 error"}""")
          }

          it (s"should return http status 408 when Auth HTTP 408") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(408).withBody("""{"reason" : "Not responding"}""")))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 408
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_TIMEOUT","message":"Auth not responding error: Not responding"}""")
          }

          it (s"should return http status 400 when Auth HTTP 400") {
            // set up
            stubFor(get(urlEqualTo("/auth/authority")).withId(uuid).willReturn(aResponse().withStatus(400).withBody("""{"reason" : "Not responding"}""")))
            val request = FakeRequest(GET, s"$context/").withHeaders(standardDesHeaders: _*)

            // test
            val result = route(app, request).get

            // check
            status(result) shouldBe 400
            contentType(result) shouldBe Some("application/json")
            contentAsJson(result) shouldBe Json.parse("""{"code":"AUTH_ERROR_BAD_REQUEST","message":"Bad request error: Not responding"}""")
          }
        }
      }
    }
  }
}