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

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.core.variable.value.ObjectValueImpl;
import org.camunda.bpm.engine.impl.core.variable.value.UntypedValueImpl;
import org.camunda.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

/**
 * Abstract implementation of a {@link TypedValueSerializer} for {@link ObjectValue ObjectValues}.
 *
 * @author Daniel Meyer
 *
 */
public abstract class AbstractObjectValueSerializer extends AbstractTypedValueSerializer<ObjectValue> {

  protected String serializationDataFormat;

  public AbstractObjectValueSerializer(String serializationDataFormat) {
    super(ValueType.OBJECT);
    this.serializationDataFormat = serializationDataFormat;
  }

  public String getSerializationDataformat() {
    return serializationDataFormat;
  }

  public ObjectValue convertToTypedValue(UntypedValueImpl untypedValue) {
    // untyped values are always deserialized
    return Variables.objectValue(untypedValue.getValue()).create();
  }

  public void writeValue(ObjectValue value, ValueFields valueFields) {

    ObjectValueImpl objectValue = (ObjectValueImpl) value;

    String objectTypeName = objectValue.getObjectTypeName();
    byte[] serializedValue = null;

    if(objectValue.isDeserialized()) {
      Object objectToSerialize = objectValue.getValue();
      if(objectToSerialize != null) {
        if(objectTypeName == null) {
          // detect a type name
          objectTypeName = getTypeNameForDeserialized(objectToSerialize);
        }
        // serialize to byte array
        try {
          serializedValue = serializeToByteArray(objectToSerialize);
          if(valueFields.getByteArrayValue() == null && objectToSerialize != null) {
            dirtyCheckOnFlush(objectToSerialize, serializedValue, valueFields);
          }
        } catch(Exception e) {
          throw new ProcessEngineException("Cannot serialize object in variable '"+valueFields.getName()+"': "+e.getMessage(), e);
        }
      }
    }
    else {
      serializedValue = objectValue.getValueSerialized();
      if(objectTypeName == null && serializedValue != null) {
        throw new ProcessEngineException("Cannot write serialized value for variable '"+valueFields.getName()+"': no 'objectTypeName' provided for non-null value.");
      }
    }

    // write value and type to fields.
    writeToValueFields(valueFields, objectTypeName, serializedValue);

    // update the ObjectValue to keep it consistent with value fields.
    updateObjectValue(objectValue, objectTypeName, serializedValue);
  }

  public ObjectValue readValue(ValueFields valueFields, boolean deserializeObjectValue) {

    byte[] serializedValue = readSerializedValueFromFields(valueFields);
    String objectTypeName = readObjectNameFromFields(valueFields);

    if(deserializeObjectValue) {
      Object deserializedObject = null;
      if(serializedValue != null) {
        try {
          deserializedObject = deserializeFromByteArray(serializedValue, objectTypeName);
        } catch (Exception e) {
          throw new ProcessEngineException("Cannot deserialize object in variable '"+valueFields.getName()+"': "+e.getMessage(), e);
        }
      }
      ObjectValueImpl objectValue = new ObjectValueImpl(deserializedObject, serializedValue, serializationDataFormat, objectTypeName, true);
      if(deserializedObject != null) {
        dirtyCheckOnFlush(deserializedObject, serializedValue, valueFields);
      }
      return objectValue;
    }
    else {
      return new ObjectValueImpl(null, serializedValue, serializationDataFormat, objectTypeName, false);
    }
  }

  protected void writeToValueFields(ValueFields valueFields, String objectTypeName, byte[] serializedValue) {
    ByteArrayValueSerializer.setBytes(valueFields, serializedValue);
    valueFields.setDataFormatId(serializationDataFormat);
    valueFields.setTextValue2(objectTypeName);
  }

  protected void updateObjectValue(ObjectValueImpl objectValue, String objectTypeName, byte[] serializedValue) {
    objectValue.setSerializedValue(serializedValue);
    objectValue.setSerializationDataFormat(serializationDataFormat);
    objectValue.setObjectTypeName(objectTypeName);
  }

  protected String readObjectNameFromFields(ValueFields valueFields) {
    return valueFields.getTextValue2();
  }

  protected byte[] readSerializedValueFromFields(ValueFields valueFields) {
    return ByteArrayValueSerializer.getBytes(valueFields);
  }

  protected boolean canWriteValue(TypedValue typedValue) {

    Object objectToSerialize = null;
    String requestedDataformat = null;

    if(typedValue instanceof UntypedValueImpl) {
      objectToSerialize = typedValue.getValue();
      requestedDataformat = null;
    }
    else if(typedValue instanceof ObjectValue) {
      ObjectValue objectValue = (ObjectValue) typedValue;
      String requestedDataFormat = objectValue.getSerializationDataFormat();

      if(!objectValue.isDeserialized()) {
        // serialized object => dataformat must match
        return serializationDataFormat.equals(requestedDataFormat);
      }
      else {
        objectToSerialize = typedValue.getValue();
        requestedDataformat = objectValue.getSerializationDataFormat();
      }
    } else {
      // not an object value
      return false;
    }

    boolean canSerialize = objectToSerialize == null || canSerializeObject(objectToSerialize);

    if(requestedDataformat != null) {
      if(requestedDataformat.equals(serializationDataFormat)) {
        return canSerialize;
      }
      else {
        return false;
      }
    }
    else {
      return canSerialize;
    }
  }

  protected void dirtyCheckOnFlush(Object deserializedObject, byte[] serializedValue, ValueFields valueFields) {
    // make sure changes to the object are flushed in case it is
    // further modified in the context of the same command
    if(valueFields instanceof VariableInstanceEntity) {
      Context
        .getCommandContext()
        .getSession(DeserializedObjectsSession.class)
        .addDeserializedObject(this, deserializedObject, serializedValue, (VariableInstanceEntity)valueFields);
    }
  }

  // methods to be implemented by subclasses ////////////

  /**
   * return true if this serializer is able to serialize the provided object.
   *
   * @param value the object to test (guaranteed to be a non-null value)
   * @return true if the serializer can handle the object.
   */
  protected abstract boolean canSerializeObject(Object value);

  /**
   * Returns the type name for the deserialized object.
   *
   * @param deserializedObject. Guaranteed not to be null
   * @return the type name fot the object.
   */
  protected abstract String getTypeNameForDeserialized(Object deserializedObject);

  /**
   * Implementations must return a byte[] representation of the provided object.
   * The object is guaranteed not to be null.
   *
   * @param deserializedObject the object to serialize
   * @return the byte array value of the object
   * @throws exception in case the object cannot be serialized
   */
  protected abstract byte[] serializeToByteArray(Object deserializedObject) throws Exception;

  /**
   * Deserialize the object from a byte array.
   *
   * @param object the object to deserialize
   * @param objectTypeName the type name of the object to deserialize
   * @return the deserialized object
   * @throws exception in case the object cannot be deserialized
   */
  protected abstract Object deserializeFromByteArray(byte[] object, String objectTypeName) throws Exception;

}
