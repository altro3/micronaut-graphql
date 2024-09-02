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
package example.graphql;

import graphql.ExecutionInput;
import io.micronaut.configuration.graphql.GraphQLExecutionInputCustomizer;
import io.micronaut.context.annotation.Primary;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

@Singleton
@Primary
public class RequestResponseCustomizer implements GraphQLExecutionInputCustomizer {

    @Override
    public Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest,
                                               @Nullable MutableHttpResponse<String> httpResponse) {
        executionInput.getGraphQLContext()
            .put("httpRequest", httpRequest)
            .put("httpResponse", httpResponse);

        return Publishers.just(executionInput);
    }

}
