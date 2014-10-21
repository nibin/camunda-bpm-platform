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
package org.camunda.bpm.integrationtest.functional.classloading.beans;

import org.camunda.bpm.engine.delegate.CaseVariableListener;
import org.camunda.bpm.engine.delegate.DelegateCaseVariableInstance;

/**
 * @author Thorben Lindhauer
 *
 */
public class ExampleCaseVariableListener implements CaseVariableListener {

  public void notify(DelegateCaseVariableInstance variableInstance) throws Exception {
    if ("variable".equals(variableInstance.getName())) {
      if ("initial".equals(variableInstance.getValue())) {
        variableInstance.getSourceExecution().setVariable("variable", "listener-notified-1");
      } else if ("manual-start".equals(variableInstance.getValue())) {
        variableInstance.getSourceExecution().setVariable("variable", "listener-notified-2");
      }// else ignore
    } else {
      throw new RuntimeException("Unexpected invocation");
    }

  }

}
