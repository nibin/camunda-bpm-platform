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
package org.camunda.bpm.engine.impl.variable.serializer;

import org.camunda.bpm.engine.variable.type.PrimitiveValueType;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 *
 */
public abstract class PrimitiveValueSerializer<T extends PrimitiveValue<?>> extends AbstractTypedValueSerializer<T> {

  public PrimitiveValueSerializer(PrimitiveValueType variableType) {
    super(variableType);
  }

  public String getName() {
    // default implementation returns the name of the type. This is OK since we assume that
    // there is only a single serializer for a primitive variable type.
    // If multiple serializers exist for the same type, they must override
    // this method and return distinct values.
    return valueType.getName().toLowerCase();
  }

  public T readValue(ValueFields valueFields, boolean deserializeObjectValue) {
    // primitive values are always deserialized
    return readValue(valueFields);
  }

  public abstract T readValue(ValueFields valueFields);

  public PrimitiveValueType getType() {
    return (PrimitiveValueType) super.getType();
  }

  protected boolean canWriteValue(TypedValue typedValue) {
    Object value = typedValue.getValue();
    Class<?> javaType = getType().getJavaType();

    return value == null || javaType.isAssignableFrom(value.getClass());
  }

}
