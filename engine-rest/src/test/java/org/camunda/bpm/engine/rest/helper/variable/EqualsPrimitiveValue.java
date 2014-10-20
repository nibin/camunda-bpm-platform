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

import org.camunda.bpm.engine.variable.value.PrimitiveValue;


/**
 * @author Thorben Lindhauer
 *
 */
public class EqualsPrimitiveValue extends EqualsTypedValue<PrimitiveValue<?>, EqualsPrimitiveValue> {

  protected Object value;

  public EqualsPrimitiveValue value(Object value) {
    this.value = value;
    return this;
  }

  public boolean matches(Object argument) {
    if (!super.matches(argument)) {
      return false;
    }

    if (!PrimitiveValue.class.isAssignableFrom(argument.getClass())) {
      return false;
    }

    PrimitiveValue<?> primitveValue = (PrimitiveValue<?>) argument;

    if (value == null) {
      if (primitveValue.getValue() != null) {
        return false;
      }
    } else {
      if (!value.equals(primitveValue.getValue())) {
        return false;
      }
    }

    return true;
  }

  public static EqualsPrimitiveValue primitiveValueMatcher() {
    return new EqualsPrimitiveValue();
  }

}
