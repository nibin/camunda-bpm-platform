/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.variable;

import java.util.HashMap;
import java.util.Map;

import static org.camunda.bpm.engine.variable.type.ValueType.*;

import org.camunda.bpm.engine.variable.type.ValueType;

/**
 * Resolves ValueType by name.
 *
 * @author Daniel Meyer
 *
 */
public class ValueTypeResolver {

  protected Map<String, ValueType> knownTypes = new HashMap<String, ValueType>();

  public ValueTypeResolver() {
    addType(BOOLEAN);
    addType(BYTES);
    addType(DATE);
    addType(DOUBLE);
    addType(INTEGER);
    addType(LONG);
    addType(NULL);
    addType(SHORT);
    addType(STRING);
    addType(OBJECT);
  }

  protected void addType(ValueType type) {
    knownTypes.put(type.getName(), type);
  }

  public ValueType typeForName(String typeName) {
    return knownTypes.get(typeName);
  }

}
