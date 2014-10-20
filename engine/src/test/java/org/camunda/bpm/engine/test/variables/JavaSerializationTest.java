/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.engine.test.variables;

import static org.camunda.bpm.engine.variable.Variables.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.camunda.bpm.engine.impl.digest._apacheCommonsCodec.Base64;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.impl.util.StringUtil;
import org.camunda.bpm.engine.impl.variable.serializer.JavaObjectSerializer;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.camunda.bpm.engine.variable.value.ObjectValue;

public class JavaSerializationTest extends PluggableProcessEngineTestCase {

  protected static final String ONE_TASK_PROCESS = "org/camunda/bpm/engine/test/variables/oneTaskProcess.bpmn20.xml";

  protected static final String JAVA_DATA_FORMAT = JavaObjectSerializer.SERIALIZATION_DATA_FORMAT;

  protected String originalSerializationFormat;

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSerializationAsJava() throws Exception {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JavaSerializable javaSerializable = new JavaSerializable("foo");
    // request object to be serialized as JSON
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(javaSerializable).serializationDataFormat(JAVA_DATA_FORMAT).create());

    // validate untyped value
    JavaSerializable value = (JavaSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertEquals(javaSerializable, value);

    // validate typed value
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertEquals(ValueType.OBJECT, typedValue.getType());

    assertTrue(typedValue.isDeserialized());

    assertEquals(javaSerializable, typedValue.getValue());
    assertEquals(javaSerializable, typedValue.getValue(JavaSerializable.class));
    assertEquals(JavaSerializable.class, typedValue.getObjectType());

    assertEquals(JAVA_DATA_FORMAT, typedValue.getSerializationDataFormat());
    assertEquals(JavaSerializable.class.getName(), typedValue.getObjectTypeName());
    String valueSerialized = typedValue.getValueSerialized();

    // validate this is the base 64 encoded string representation of the serialized value of the java object
    byte[] decodedObject = Base64.decodeBase64(StringUtil.toByteArray(valueSerialized, processEngine));
    ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(decodedObject));
    assertEquals(value, objectInputStream.readObject());

  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetSerializedJavaOject() throws Exception {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JavaSerializable javaSerializable = new JavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.encodeBase64(baos.toByteArray()), processEngine);

    // request object to be serialized as JSON
    runtimeService.setVariable(instance.getId(), "simpleBean",
        serializedObjectValue(serializedObject)
        .serializationDataFormat(JAVA_DATA_FORMAT)
        .objectTypeName(JavaSerializable.class.getName())
        .create());

    // validate untyped value
    JavaSerializable value = (JavaSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertEquals(javaSerializable, value);

    // validate typed value
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertEquals(ValueType.OBJECT, typedValue.getType());

    assertTrue(typedValue.isDeserialized());

    assertEquals(javaSerializable, typedValue.getValue());
    assertEquals(javaSerializable, typedValue.getValue(JavaSerializable.class));
    assertEquals(JavaSerializable.class, typedValue.getObjectType());

    assertEquals(JAVA_DATA_FORMAT, typedValue.getSerializationDataFormat());
    assertEquals(JavaSerializable.class.getName(), typedValue.getObjectTypeName());
    String valueSerialized = typedValue.getValueSerialized();

    // validate this is the base 64 encoded string representation of the serialized value of the java object
    byte[] decodedObject = Base64.decodeBase64(StringUtil.toByteArray(valueSerialized, processEngine));
    ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(decodedObject));
    assertEquals(value, objectInputStream.readObject());

  }

}
