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

import example.domain.ToDo;
import example.repository.ToDoRepository;
import graphql.kickstart.tools.GraphQLQueryResolver;
import jakarta.inject.Singleton;

/**
 * @author Marcel Overdijk
 */
@Singleton
public class ToDoQueryResolver implements GraphQLQueryResolver {

    private final ToDoRepository toDoRepository;

    public ToDoQueryResolver(ToDoRepository toDoRepository) {
        this.toDoRepository = toDoRepository;
    }

    public Iterable<ToDo> toDos() {
        return toDoRepository.findAll();
    }
}
