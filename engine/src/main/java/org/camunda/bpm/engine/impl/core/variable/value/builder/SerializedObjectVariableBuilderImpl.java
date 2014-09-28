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
package org.camunda.bpm.engine.impl.core.variable.value.builder;

import java.nio.charset.Charset;

import org.camunda.bpm.engine.impl.core.variable.value.ObjectValueImpl;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.engine.variable.value.builder.SerializedObjectVariableBuilder;

/**
 * @author Daniel Meyer
 *
 */
public class SerializedObjectVariableBuilderImpl implements SerializedObjectVariableBuilder {

  protected ObjectValueImpl variableValue;

  public SerializedObjectVariableBuilderImpl() {
    variableValue = new ObjectValueImpl(null, null, null, null, false);
  }

  public SerializedObjectVariableBuilderImpl(ObjectValue value) {
    variableValue = (ObjectValueImpl) value;
  }

  public SerializedObjectVariableBuilderImpl serializationDataFormat(String dataFormatName) {
    variableValue.setSerializationDataFormat(dataFormatName);
    return this;
  }

  public ObjectValue create() {
    return variableValue;
  }

  public SerializedObjectVariableBuilder objectTypeName(String typeName) {
    variableValue.setObjectTypeName(typeName);
    return this;
  }

  public SerializedObjectVariableBuilder serializedValue(byte[] value) {
    variableValue.setSerializedValue(value);
    return this;
  }

  public SerializedObjectVariableBuilder serializedValue(String value) {
    return serializedValue(value.getBytes(Charset.forName("utf-8")));
  }

}
