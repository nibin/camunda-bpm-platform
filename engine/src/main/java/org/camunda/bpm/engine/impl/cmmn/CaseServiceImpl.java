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
package org.camunda.bpm.engine.impl.cmmn;

import java.util.Collection;

import org.camunda.bpm.engine.CaseService;
import org.camunda.bpm.engine.exception.NotFoundException;
import org.camunda.bpm.engine.exception.NotValidException;
import org.camunda.bpm.engine.exception.NullValueException;
import org.camunda.bpm.engine.exception.cmmn.CaseExecutionNotFoundException;
import org.camunda.bpm.engine.impl.ServiceImpl;
import org.camunda.bpm.engine.impl.cmmn.cmd.GetCaseExecutionVariableCmd;
import org.camunda.bpm.engine.impl.cmmn.cmd.GetCaseExecutionVariableTypedCmd;
import org.camunda.bpm.engine.impl.cmmn.cmd.GetCaseExecutionVariablesCmd;
import org.camunda.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionQueryImpl;
import org.camunda.bpm.engine.impl.cmmn.entity.runtime.CaseInstanceQueryImpl;
import org.camunda.bpm.engine.impl.cmmn.entity.runtime.CaseSentryPartQueryImpl;
import org.camunda.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.camunda.bpm.engine.runtime.CaseExecutionQuery;
import org.camunda.bpm.engine.runtime.CaseInstanceBuilder;
import org.camunda.bpm.engine.runtime.CaseInstanceQuery;
import org.camunda.bpm.engine.runtime.CaseSentryPartQuery;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.value.TypedValue;

/**
 * @author Roman Smirnov
 *
 */
public class CaseServiceImpl extends ServiceImpl implements CaseService {

  public CaseInstanceBuilder withCaseDefinitionByKey(String caseDefinitionKey) {
    return new CaseInstanceBuilderImpl(commandExecutor, caseDefinitionKey, null);
  }

  public CaseInstanceBuilder withCaseDefinition(String caseDefinitionId) {
    return new CaseInstanceBuilderImpl(commandExecutor, null, caseDefinitionId);
  }

  public CaseInstanceQuery createCaseInstanceQuery() {
    return new CaseInstanceQueryImpl(commandExecutor);
  }

  public CaseExecutionQuery createCaseExecutionQuery() {
    return new CaseExecutionQueryImpl(commandExecutor);
  }

  public CaseSentryPartQuery createCaseSentryPartQuery() {
    return new CaseSentryPartQueryImpl(commandExecutor);
  }

  public CaseExecutionCommandBuilder withCaseExecution(String caseExecutionId) {
    return new CaseExecutionCommandBuilderImpl(commandExecutor, caseExecutionId);
  }

  public VariableMap getVariables(String caseExecutionId) {
    return getVariables(caseExecutionId, true);
  }

  public VariableMap getVariables(String caseExecutionId, boolean deserializeValues) {
    return getCaseExecutionVariables(caseExecutionId, null, false, deserializeValues);
  }

  public VariableMap getVariablesLocal(String caseExecutionId) {
    return getVariablesLocal(caseExecutionId, true);
  }

  public VariableMap getVariablesLocal(String caseExecutionId, boolean deserializeValues) {
    return getCaseExecutionVariables(caseExecutionId, null, true, deserializeValues);
  }

  public VariableMap getVariables(String caseExecutionId, Collection<String> variableNames) {
    return getVariables(caseExecutionId, variableNames, true);
  }

  public VariableMap getVariables(String caseExecutionId, Collection<String> variableNames, boolean deserializeValues) {
    return getCaseExecutionVariables(caseExecutionId, variableNames, false, deserializeValues);
  }

  public VariableMap getVariablesLocal(String caseExecutionId, Collection<String> variableNames) {
    return getVariables(caseExecutionId, variableNames, true);
  }

  public VariableMap getVariablesLocal(String caseExecutionId, Collection<String> variableNames, boolean deserializeValues) {
    return getCaseExecutionVariables(caseExecutionId, variableNames, true, deserializeValues);
  }

  protected VariableMap getCaseExecutionVariables(String caseExecutionId, Collection<String> variableNames, boolean isLocal, boolean deserializeValues) {
    try {
      return commandExecutor.execute(new GetCaseExecutionVariablesCmd(caseExecutionId, variableNames, isLocal, deserializeValues));
    }
    catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    }
    catch (CaseExecutionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  public Object getVariable(String caseExecutionId, String variableName) {
    return getCaseExecutionVariable(caseExecutionId, variableName, false);
  }

  public Object getVariableLocal(String caseExecutionId, String variableName) {
    return getCaseExecutionVariable(caseExecutionId, variableName, true);
  }

  protected Object getCaseExecutionVariable(String caseExecutionId, String variableName, boolean isLocal) {
    try {
      return commandExecutor.execute(new GetCaseExecutionVariableCmd(caseExecutionId, variableName, isLocal));
    }
    catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    }
    catch (CaseExecutionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  public <T extends TypedValue> T getVariableTyped(String caseExecutionId, String variableName) {
    return getVariableTyped(caseExecutionId, variableName, true);
  }

  public <T extends TypedValue> T getVariableTyped(String caseExecutionId, String variableName, boolean deserializeValue) {
    return getCaseExecutionVariableTyped(caseExecutionId, variableName, false, deserializeValue);
  }

  public <T extends TypedValue> T getVariableLocalTyped(String caseExecutionId, String variableName) {
    return getVariableLocalTyped(caseExecutionId, variableName, true);
  }

  public <T extends TypedValue> T getVariableLocalTyped(String caseExecutionId, String variableName, boolean deserializeValue) {
    return getCaseExecutionVariableTyped(caseExecutionId, variableName, true, deserializeValue);
  }

  @SuppressWarnings("unchecked")
  protected <T extends TypedValue> T getCaseExecutionVariableTyped(String caseExecutionId, String variableName, boolean isLocal, boolean deserializeValue) {
    try {
      return (T) commandExecutor.execute(new GetCaseExecutionVariableTypedCmd(caseExecutionId, variableName, isLocal, deserializeValue));
    }
    catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    }
    catch (CaseExecutionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }


}
