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

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.engine.variable.value.builder.SerializedObjectVariableBuilder;
import org.camunda.spin.DataFormats;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

public class JsonSerializationTest extends PluggableProcessEngineTestCase {

  protected static final String ONE_TASK_PROCESS = "org/camunda/bpm/engine/test/variables/oneTaskProcess.bpmn20.xml";

  protected static final String JSON_FORMAT_NAME = DataFormats.jsonTree().getName();

  protected String originalSerializationFormat;

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSerializationAsJson() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    SimpleBean bean = new SimpleBean("a String", 42, true);
    // request object to be serialized as JSON
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME).create());

    // validate untyped value
    Object value = runtimeService.getVariable(instance.getId(), "simpleBean");
    assertEquals(bean, value);

    // validate typed value
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertEquals(ValueType.OBJECT, typedValue.getType());

    assertTrue(typedValue.isDeserialized());

    assertEquals(bean, typedValue.getValue());
    assertEquals(bean, typedValue.getValue(SimpleBean.class));
    assertEquals(SimpleBean.class, typedValue.getObjectType());

    assertEquals(JSON_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertEquals(SimpleBean.class.getName(), typedValue.getObjectTypeName());
    JSONAssert.assertEquals(bean.toExpectedJsonString(), new String(typedValue.getValueSerialized()), true);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testListSerializationAsJson() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<SimpleBean> beans = new ArrayList<SimpleBean>();
    for (int i = 0; i < 20; i++) {
      beans.add(new SimpleBean("a String" + i, 42 + i, true));
    }

    runtimeService.setVariable(instance.getId(), "simpleBeans", objectValue(beans).serializationDataFormat(JSON_FORMAT_NAME).create());

    // validate untyped value
    Object value = runtimeService.getVariable(instance.getId(), "simpleBeans");
    assertEquals(beans, value);

    // validate typed value
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBeans");
    assertEquals(ValueType.OBJECT, typedValue.getType());
    assertEquals(beans, typedValue.getValue());
    assertTrue(typedValue.isDeserialized());
    assertEquals(JSON_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertNotNull(typedValue.getObjectTypeName());
    JSONAssert.assertEquals(toExpectedJsonArray(beans), new String(typedValue.getValueSerialized()), true);

  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testFailingSerialization() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    FailingSerializationBean failingBean = new FailingSerializationBean("a String", 42, true);

    try {
      runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(failingBean).serializationDataFormat(JSON_FORMAT_NAME));
      fail("exception expected");
    } catch (ProcessEngineException e) {
      // happy path
    }
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testFailingDeserialization() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    FailingDeserializationBean failingBean = new FailingDeserializationBean("a String", 42, true);

    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(failingBean).serializationDataFormat(JSON_FORMAT_NAME));

    try {
      runtimeService.getVariable(instance.getId(), "simpleBean");
      fail("exception expected");
    }
    catch(ProcessEngineException e) {
      // happy path
    }

    try {
      runtimeService.getVariableTyped(instance.getId(), "simpleBean");
      fail("exception expected");
    }
    catch(ProcessEngineException e) {
      // happy path
    }

    // However, I can access the serialized value
    ObjectValue objectValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean", false);
    assertFalse(objectValue.isDeserialized());
    assertNotNull(objectValue.getObjectTypeName());
    assertNotNull(objectValue.getValueSerialized());
    // but not the deserialized properties
    try {
      objectValue.getValue();
      fail("exception expected");
    }
    catch(IllegalStateException e) {
      assertTextPresent("Object is not deserialized", e.getMessage());
    }

    try {
      objectValue.getValue(SimpleBean.class);
      fail("exception expected");
    }
    catch(IllegalStateException e) {
      assertTextPresent("Object is not deserialized", e.getMessage());
    }

    try {
      objectValue.getObjectType();
      fail("exception expected");
    }
    catch(IllegalStateException e) {
      assertTextPresent("Object is not deserialized", e.getMessage());
    }

  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testFailForNonExistingSerializationFormat() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    SimpleBean simpleBean = new SimpleBean();

    try {
      runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(simpleBean).serializationDataFormat("non existing data format"));
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      assertTextPresent("Cannot find serializer for value", e.getMessage());
      // happy path
    }
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testVariableValueCaching() {
    final ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    processEngineConfiguration.getCommandExecutorTxRequired().execute(new Command<Void>() {

      @Override
      public Void execute(CommandContext commandContext) {
        SimpleBean bean = new SimpleBean("a String", 42, true);
        runtimeService.setVariable(instance.getId(), "simpleBean", bean);

        Object returnedBean = runtimeService.getVariable(instance.getId(), "simpleBean");
        assertSame(bean, returnedBean);

        return null;
      }
    });

    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().singleResult();

    Object returnedBean = variableInstance.getValue();
    Object theSameReturnedBean = variableInstance.getValue();
    assertSame(returnedBean, theSameReturnedBean);
  }

//  public void testApplicationOfGlobalConfiguration() throws JSONException {
//    DataFormats.jsonTreeGlobal().mapper().config("aKey", "aValue");
//
//    SpinVariableTypeResolver resolver = new SpinVariableTypeResolver();
//    SpinSerializationType variableType = (SpinSerializationType) resolver.getTypeForSerializationFormat(JSON_FORMAT_NAME);
//
//    DataFormats.jsonTreeGlobal().mapper().config("aKey", null);
//
//    JsonJacksonTreeDataFormat dataFormat = (JsonJacksonTreeDataFormat) variableType.getDefaultDataFormat();
//    assertNotSame("The variable type should not use the global data format instance",
//        DataFormats.jsonTreeGlobal(), dataFormat);
//
//    assertEquals("The global configuration should have been applied to the variable type's format",
//        "aValue", dataFormat.mapper().getConfiguration().get("aKey"));
//  }
//

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testGetSerializedVariableValue() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    SimpleBean bean = new SimpleBean("a String", 42, true);
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME).create());

    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean", false);

    byte[] serializedValue = typedValue.getValueSerialized();
    String variableAsJson = new String(serializedValue);
    JSONAssert.assertEquals(bean.toExpectedJsonString(), variableAsJson, true);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetSerializedVariableValue() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    SimpleBean bean = new SimpleBean("a String", 42, true);
    String beanAsJson = bean.toExpectedJsonString();

    SerializedObjectVariableBuilder serializedValue = serializedObjectValue(beanAsJson)
      .serializationDataFormat(JSON_FORMAT_NAME)
      .objectTypeName(bean.getClass().getCanonicalName());

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // java object can be retrieved
    SimpleBean returnedBean = (SimpleBean) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertEquals(bean, returnedBean);

    // validate typed value metadata
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertEquals(bean, typedValue.getValue());
    assertEquals(JSON_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertEquals(bean.getClass().getCanonicalName(), typedValue.getObjectTypeName());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetSerializedVariableValueNoTypeName() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    SimpleBean bean = new SimpleBean("a String", 42, true);
    String beanAsJson = bean.toExpectedJsonString();

    SerializedObjectVariableBuilder serializedValue = serializedObjectValue(beanAsJson)
      .serializationDataFormat(JSON_FORMAT_NAME);
      // no type name

    try {
      runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);
      fail("Exception expected.");
    }
    catch(Exception e) {
      assertTextPresent("no 'objectTypeName' provided for non-null value", e.getMessage());
    }
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetSerializedVariableValueMismatchingTypeName() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    SimpleBean bean = new SimpleBean("a String", 42, true);
    String beanAsJson = bean.toExpectedJsonString();

    SerializedObjectVariableBuilder serializedValue = serializedObjectValue(beanAsJson)
      .serializationDataFormat(JSON_FORMAT_NAME)
      .objectTypeName("Insensible type name."); // < not a valid type name

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    try {
      runtimeService.getVariable(instance.getId(), "simpleBean");
      fail("Exception expected.");
    }
    catch(Exception e) {
      // happy path
    }

    serializedValue = serializedObjectValue(beanAsJson)
      .serializationDataFormat(JSON_FORMAT_NAME)
      .objectTypeName(JsonSerializationTest.class.getName()); // < not the right type name

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    try {
      runtimeService.getVariable(instance.getId(), "simpleBean");
      fail("Exception expected.");
    }
    catch(Exception e) {
      // happy path
    }
  }


  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetSerializedVariableValueNull() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    SerializedObjectVariableBuilder serializedValue = serializedObjectValue()
      .serializationDataFormat(JSON_FORMAT_NAME)
      .objectTypeName(SimpleBean.class.getCanonicalName());

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // null can be retrieved
    SimpleBean returnedBean = (SimpleBean) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertNull(returnedBean);

    // validate typed value metadata
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertNull(typedValue.getValue());
    assertNull(typedValue.getValueSerialized());
    assertEquals(JSON_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertEquals(SimpleBean.class.getCanonicalName(), typedValue.getObjectTypeName());

  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetSerializedVariableValueNullNoTypeName() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    SerializedObjectVariableBuilder serializedValue = serializedObjectValue()
      .serializationDataFormat(JSON_FORMAT_NAME);
    // no objectTypeName specified

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // null can be retrieved
    SimpleBean returnedBean = (SimpleBean) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertNull(returnedBean);

    // validate typed value metadata
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertNull(typedValue.getValue());
    assertNull(typedValue.getValueSerialized());
    assertEquals(JSON_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertNull(typedValue.getObjectTypeName());
  }

  protected String toExpectedJsonArray(List<SimpleBean> beans) {
    StringBuilder jsonBuilder = new StringBuilder();

    jsonBuilder.append("[");
    for (int i = 0; i < beans.size(); i++) {
      jsonBuilder.append(beans.get(i).toExpectedJsonString());

      if (i != beans.size() - 1)  {
        jsonBuilder.append(", ");
      }
    }
    jsonBuilder.append("]");

    return jsonBuilder.toString();
  }
}
