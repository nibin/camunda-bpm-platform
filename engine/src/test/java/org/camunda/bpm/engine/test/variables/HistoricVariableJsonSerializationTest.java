package org.camunda.bpm.engine.test.variables;

import static org.camunda.bpm.engine.variable.Variables.objectValue;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.history.HistoricVariableUpdate;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.spin.DataFormats;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

public class HistoricVariableJsonSerializationTest extends PluggableProcessEngineTestCase {

  protected static final String ONE_TASK_PROCESS = "org/camunda/bpm/engine/test/variables/oneTaskProcess.bpmn20.xml";

  protected static final String JSON_FORMAT_NAME = DataFormats.jsonTree().getName();

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSelectHistoricVariableInstances() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JsonSerializable bean = new JsonSerializable("a String", 42, false);
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME).create());

    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertNotNull(historicVariable.getValue());
    assertNull(historicVariable.getErrorMessage());

    assertEquals(ValueType.OBJECT.getName(), historicVariable.getTypeName());
    assertEquals(ValueType.OBJECT.getName(), historicVariable.getVariableTypeName());

    JsonSerializable historyValue = (JsonSerializable) historicVariable.getValue();
    assertEquals(bean.getStringProperty(), historyValue.getStringProperty());
    assertEquals(bean.getIntProperty(), historyValue.getIntProperty());
    assertEquals(bean.getBooleanProperty(), historyValue.getBooleanProperty());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSelectHistoricSerializedValues() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JsonSerializable bean = new JsonSerializable("a String", 42, false);
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME));

    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertNotNull(historicVariable.getValue());
    assertNull(historicVariable.getErrorMessage());

    ObjectValue typedValue = (ObjectValue) historicVariable.getTypedValue();
    assertEquals(JSON_FORMAT_NAME, typedValue.getSerializationDataFormat());
    JSONAssert.assertEquals(bean.toExpectedJsonString(),new String(typedValue.getValueSerialized()), true);
    assertEquals(JsonSerializable.class.getName(), typedValue.getObjectTypeName());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSelectHistoricSerializedValuesUpdate() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JsonSerializable bean = new JsonSerializable("a String", 42, false);
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME));

    if (ProcessEngineConfiguration.HISTORY_FULL.equals(processEngineConfiguration.getHistory())) {

      HistoricVariableUpdate historicUpdate = (HistoricVariableUpdate)
          historyService.createHistoricDetailQuery().variableUpdates().singleResult();

      assertNotNull(historicUpdate.getValue());
      assertNull(historicUpdate.getErrorMessage());

      assertEquals(ValueType.OBJECT.getName(), historicUpdate.getTypeName());
      assertEquals(ValueType.OBJECT.getName(), historicUpdate.getVariableTypeName());

      JsonSerializable historyValue = (JsonSerializable) historicUpdate.getValue();
      assertEquals(bean.getStringProperty(), historyValue.getStringProperty());
      assertEquals(bean.getIntProperty(), historyValue.getIntProperty());
      assertEquals(bean.getBooleanProperty(), historyValue.getBooleanProperty());

      ObjectValue typedValue = (ObjectValue) historicUpdate.getTypedValue();
      assertEquals(JSON_FORMAT_NAME, typedValue.getSerializationDataFormat());
      JSONAssert.assertEquals(bean.toExpectedJsonString(),new String(typedValue.getValueSerialized()), true);
      assertEquals(JsonSerializable.class.getName(), typedValue.getObjectTypeName());

    }
  }

}
