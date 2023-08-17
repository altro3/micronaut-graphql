/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.graphql.ws.apollo;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import io.micronaut.configuration.graphql.GraphQLRequestBody;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Class to handle the message to and from the websocket.
 *
 * @author Gerard Klijs
 * @since 1.3
 * @deprecated The Apollo subscriptions-transport-ws protocol is deprecated and its usage should be replaced with the new graphql-ws implementation.
 */
@Deprecated(since = "4.0")
@Serdeable
public class GraphQLApolloWsRequest {

    private static final String TYPE_ERROR_MESSAGE = "Could not map %s to a known client type.";

    private ClientType type;
    @Nullable
    private String id;
    @Nullable
    private GraphQLRequestBody payload;

    /**
     * Get the type.
     *
     * @return the type of message as ClientType
     */
    public ClientType getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type as string
     */
    @JsonSetter
    public void setType(final String type) {
        this.type = fromString(type);
    }

    /**
     * Get the id.
     *
     * @return id as string
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id
     */
    @JsonSetter
    public void setId(@Nullable final String id) {
        this.id = id;
    }

    /**
     * Get the payload.
     *
     * @return payload as map, likely to contain a graphql query
     */
    @Nullable
    public GraphQLRequestBody getPayload() {
        return payload;
    }

    /**
     * Sets the payload.
     *
     * @param payload the payload
     */
    @JsonSetter
    public void setPayload(@Nullable final GraphQLRequestBody payload) {
        this.payload = payload;
    }

    private ClientType fromString(String type) {
        for (ClientType clientType : ClientType.values()) {
            if (clientType.getType().equals(type)) {
                return clientType;
            }
        }
        throw new IllegalArgumentException(String.format(TYPE_ERROR_MESSAGE, type));
    }

    /**
     * Types of messages received from the client.
     */
    @Serdeable
    public enum ClientType {
        GQL_CONNECTION_INIT("connection_init"),
        GQL_START("start"),
        GQL_STOP("stop"),
        GQL_CONNECTION_TERMINATE("connection_terminate");

        private String type;

        /**
         * Default constructor.
         *
         * @param type string
         */
        ClientType(String type) {
            this.type = type;
        }

        /**
         * Get the type.
         *
         * @return type as string
         */
        @JsonValue
        public String getType() {
            return type;
        }
    }
}
