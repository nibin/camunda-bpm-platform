package org.camunda.bpm.engine.test.variables;

public class FailingSerializationBean extends JsonSerializable {

  public FailingSerializationBean() {
  }

  public FailingSerializationBean(String stringProperty, int intProperty, boolean booleanProperty) {
    super(stringProperty, intProperty, booleanProperty);
  }

  public String getStringProperty() {
    throw new RuntimeException("I am failing");
  }
}
