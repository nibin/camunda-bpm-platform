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
package org.camunda.bpm.engine.rest.helper.variable;

import java.io.UnsupportedEncodingException;

import org.camunda.bpm.engine.variable.type.ValueType;
import org.camunda.bpm.engine.variable.value.ObjectValue;


/**
 * @author Thorben Lindhauer
 *
 */
public class EqualsObjectValue extends EqualsTypedValue<ObjectValue, EqualsObjectValue> {

  protected String serializationFormat;
  protected String objectTypeName;
  protected byte[] serializedValue;

  public EqualsObjectValue() {
    this.type = ValueType.OBJECT;
  }

  public EqualsObjectValue serializationFormat(String serializationFormat) {
    this.serializationFormat = serializationFormat;
    return this;
  }

  public EqualsObjectValue objectTypeName(String objectTypeName) {
    this.objectTypeName = objectTypeName;
    return this;
  }

  public EqualsObjectValue serializedStringValue(String serializedValue) {
    try {
      this.serializedValue = serializedValue.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Cannot convert string to utf-8");
    }
    return this;
  }

  public EqualsObjectValue serializedValue(byte[] serializedValue) {
    this.serializedValue = serializedValue;
    return this;
  }

  public boolean matches(Object argument) {
    if (!super.matches(argument)) {
      return false;
    }

    if (!ObjectValue.class.isAssignableFrom(argument.getClass())) {
      return false;
    }

    ObjectValue objectValue = (ObjectValue) argument;

    if (serializationFormat == null) {
      if (objectValue.getSerializationDataFormat() != null) {
        return false;
      }
    } else {
      if (!serializationFormat.equals(objectValue.getSerializationDataFormat())) {
        return false;
      }
    }

    if (objectTypeName == null) {
      if (objectValue.getObjectTypeName() != null) {
        return false;
      }
    } else {
      if (!objectTypeName.equals(objectValue.getObjectTypeName())) {
        return false;
      }
    }

    if (!objectValue.isDeserialized()) {
      return false;
    }

    if (serializedValue == null) {
      if (objectValue.getValueSerializedString() != null) {
        return false;
      }
    } else {
      if (!serializedValue.equals(objectValue.getValueSerializedString())) {
        return false;
      }
    }

    return true;
  }

  public static EqualsObjectValue objectValueMatcher() {
    return new EqualsObjectValue();
  }

}
