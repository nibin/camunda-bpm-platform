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
package org.camunda.bpm.engine.impl.core.variable.value;

import javax.lang.model.type.NullType;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.variable.type.SerializableValueType;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.camunda.bpm.engine.variable.value.ObjectValue;

/**
 * @author Daniel Meyer
 *
 */
public class ObjectValueImpl extends AbstractTypedValue<Object> implements ObjectValue {

  private static final long serialVersionUID = 1L;

  protected String objectTypeName;

  protected String serializationDataFormat;

  protected byte[] serializedValue;

  protected boolean isDeserialized;

  public ObjectValueImpl(
      Object deserializedValue,
      byte[] serializedValue,
      String serializationDataFormat,
      String objectTypeName,
      boolean isDeserialized) {

    super(deserializedValue, ValueType.OBJECT);

    this.serializedValue = serializedValue;
    this.serializationDataFormat = serializationDataFormat;
    this.objectTypeName = objectTypeName;
    this.isDeserialized = isDeserialized;
  }

  public ObjectValueImpl(Object value) {
    this(value, null, null, null, true);
  }

  public String getSerializationDataFormat() {
    return serializationDataFormat;
  }

  public void setSerializationDataFormat(String serializationDataFormat) {
    this.serializationDataFormat = serializationDataFormat;
  }

  public String getObjectTypeName() {
    return objectTypeName;
  }

  public void setObjectTypeName(String objectTypeName) {
    this.objectTypeName = objectTypeName;
  }

  public byte[] getValueSerialized() {
    return serializedValue;
  }

  public String getValueSerializedString() {
    return new String(serializedValue);
  }

  public void setSerializedValue(byte[] serializedValue) {
    this.serializedValue = serializedValue;
  }

  public boolean isDeserialized() {
    return isDeserialized;
  }

  public Object getValue() {
    if(isDeserialized) {
      return super.getValue();
    }
    else {
      throw new IllegalStateException("Object is not deserialized.");
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T getValue(Class<T> type) {
    Object value = getValue();
    if(type.isAssignableFrom(value.getClass())) {
      return (T) value;
    }
    else {
      throw new ProcessEngineException("Value '"+value+"' is not of type '"+type+"'.");
    }
  }

  public Class<?> getObjectType() {
    Object value = getValue();

    if(value == null) {
      return NullType.class;
    }
    else {
      return value.getClass();
    }
  }

  public SerializableValueType getType() {
    return (SerializableValueType) super.getType();
  }

  @Override
  public String toString() {
    return "ObjectValue ["
        + "value=" + value
        + ", isDeserialized=" + isDeserialized
        + ", serializationDataFormat=" + serializationDataFormat
        + ", objectTypeName=" + objectTypeName
        + ", serializedValue="+ (serializedValue != null ? (serializedValue.length + " bytes") : null)
        + "]";
  }


}
