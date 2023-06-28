package io.micronaut.configuration.graphql.ws.apollo

import graphql.ExecutionInput
import io.micronaut.configuration.graphql.GraphQLExecutionInputCustomizer
import io.micronaut.configuration.graphql.GraphQLRequestBody
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.websocket.WebSocketClient
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * @author Gerard Klijs
 * @since 1.3
 */
class GraphQLApolloWsControllerSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQLApolloWsClient graphQLWsClient

    def setup() {
        embeddedServer = ApplicationContext.run(EmbeddedServer, ["spec.name": GraphQLApolloWsControllerSpec.simpleName], "apollows") as EmbeddedServer
        WebSocketClient wsClient = embeddedServer.applicationContext.createBean(WebSocketClient, embeddedServer.getURI())
        graphQLWsClient = Flux.from(wsClient.connect(GraphQLApolloWsClient, "/graphql-ws")).blockFirst()
    }

    void "test init connection, keep alive off"() {
        given:
        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest()
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_CONNECTION_INIT.getType())

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLApolloWsResponse response = graphQLWsClient.nextResponse()
        response.getType() == GraphQLApolloWsResponse.ServerType.GQL_CONNECTION_ACK.getType()
        GraphQLApolloWsResponse noResponse = graphQLWsClient.nextResponse()
        noResponse == null

        and:
        response.id == null
        response.payload == null
    }

    void "test query over websocket"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "query{ foo }"
        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest()
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_START.type)
        request.setId("foo_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLApolloWsResponse response = graphQLWsClient.nextResponse()
        response.getPayload().getSpecification().get("data") == [foo: "bar"]
        GraphQLApolloWsResponse completeResponse = graphQLWsClient.nextResponse()

        and:
        response.id == "foo_id"
        response.type == "data"
        completeResponse.type == "complete"
    }

    void "handle error in query over websocket"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "query{ error }"
        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest()
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_START.type)
        request.setId("error_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLApolloWsResponse response = graphQLWsClient.nextResponse()
        response.getPayload().getSpecification().get("data") == null
        response.getPayload().getSpecification().get("errors") != null
        List<Map> errorList = (List<Map>) response.getPayload().getSpecification().get("errors")
        errorList != null
        errorList.size() == 1
        errorList.get(0).get("message") == "Exception while fetching data (/error) : No error present"
        errorList.get(0).get("locations") == [[line: 1, column: 8]]
        errorList.get(0).get("path") == ["error"]
        errorList.get(0).get("extensions") == [classification: "DataFetchingException"]
        GraphQLApolloWsResponse completeResponse = graphQLWsClient.nextResponse()

        and:
        response.id == "error_id"
        response.type == "error"
        completeResponse.type == "complete"
    }

    void "test mutation over websocket"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "mutation{ change( newValue: \"Value_B\" ){ current old }}"
        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest()
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_START.type)
        request.setId("change_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLApolloWsResponse response = graphQLWsClient.nextResponse()
        response.getPayload().getSpecification().get("data") == [change: [current: "Value_B", old: ["Value_A"]]]
        GraphQLApolloWsResponse completeResponse = graphQLWsClient.nextResponse()

        and:
        response.id == "change_id"
        response.type == "data"
        completeResponse.type == "complete"
    }

    void "test customizer using http request in mutation over websocket"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "mutation{ change( newValue: \"\$[path]\" ){ current old }}"
        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest()
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_START.type)
        request.setId("change_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLApolloWsResponse response = graphQLWsClient.nextResponse()
        response.getPayload().getSpecification().get("data") == [change: [current: "/graphql-ws", old: ["Value_A"]]]
        GraphQLApolloWsResponse completeResponse = graphQLWsClient.nextResponse()

        and:
        response.id == "change_id"
        response.type == "data"
        completeResponse.type == "complete"
    }

    void "test subscription over websocket, stop after two"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody()
        body.query = "subscription{ counter }"
        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest()
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_START.type)
        request.setId("counter_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLApolloWsResponse response1 = graphQLWsClient.nextResponse()
        response1.getPayload().getSpecification().get("data") == [counter: 0]
        GraphQLApolloWsResponse response2 = graphQLWsClient.nextResponse()
        response2.getPayload().getSpecification().get("data") == [counter: 1]
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_STOP.type)
        graphQLWsClient.send(request)
        GraphQLApolloWsResponse response3 = graphQLWsClient.nextResponse()

        and:
        response1.id == "counter_id"
        response1.type == "data"
        response2.id == "counter_id"
        response2.type == "data"
        response3.id == "counter_id"
        response3.type == "complete"
    }

    void "test subscription over websocket, let it complete"() {
        given:
        GraphQLRequestBody body = new GraphQLRequestBody();
        body.query = "subscription{ counter }"
        GraphQLApolloWsRequest request = new GraphQLApolloWsRequest()
        request.setType(GraphQLApolloWsRequest.ClientType.GQL_START.type)
        request.setId("counter_id")
        request.setPayload(body)

        when:
        graphQLWsClient.send(request)

        then:
        GraphQLApolloWsResponse response1 = graphQLWsClient.nextResponse()
        response1.getPayload().getSpecification().get("data") == [counter: 0]
        GraphQLApolloWsResponse response2 = graphQLWsClient.nextResponse()
        response2.getPayload().getSpecification().get("data") == [counter: 1]
        GraphQLApolloWsResponse response3 = graphQLWsClient.nextResponse()
        response3.getPayload().getSpecification().get("data") == [counter: 2]
        GraphQLApolloWsResponse response4 = graphQLWsClient.nextResponse()

        and:
        response1.id == "counter_id"
        response1.type == "data"
        response2.id == "counter_id"
        response2.type == "data"
        response3.id == "counter_id"
        response3.type == "data"
        response4.id == "counter_id"
        response4.type == "complete"
    }
}

@Singleton
@Primary
@Requires(property = "spec.name", value = "GraphQLApolloWsControllerSpec")
class SetValueFromRequestInputCustomizer implements GraphQLExecutionInputCustomizer {
    private final static String PATH_PLACEHOLDER = "\$[path]"

    @Override
    Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest,
                                        MutableHttpResponse<String> httpResponse) {
        if (executionInput.getQuery().contains(PATH_PLACEHOLDER)) {
            return Publishers.just(executionInput.transform({
                builder -> builder.query(executionInput.getQuery().replace(PATH_PLACEHOLDER, httpRequest.getPath()))
            }))
        } else {
            return Publishers.just(executionInput)
        }
    }
}
