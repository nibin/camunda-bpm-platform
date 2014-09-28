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
package org.camunda.bpm.engine.rest.sub.runtime.impl;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.rest.dto.runtime.VariableInstanceDto;
import org.camunda.bpm.engine.rest.exception.InvalidRequestException;
import org.camunda.bpm.engine.rest.impl.TypedValueUtil;
import org.camunda.bpm.engine.rest.sub.runtime.VariableInstanceResource;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.runtime.VariableInstanceQuery;

/**
 * @author Daniel Meyer
 *
 */
public class VariableInstanceResourceImpl implements VariableInstanceResource {

  protected String variableId;
  protected ProcessEngine engine;

  public VariableInstanceResourceImpl(String variableId, ProcessEngine engine) {
    this.variableId = variableId;
    this.engine = engine;
  }

  public VariableInstanceDto getVariable(boolean deserializeObjectValue) {
    VariableInstanceQuery baseQuery = baseQuery();

    // do not fetch byte arrays
    baseQuery.disableBinaryFetching();

    if(!deserializeObjectValue) {
      baseQuery.disableObjectValueDeserialization();
    }

    VariableInstance variableInstance = baseQuery.singleResult();

    if(variableInstance != null) {
      return VariableInstanceDto.fromVariableInstance(variableInstance);

    } else {
      throw new InvalidRequestException(Status.NOT_FOUND, "Variable instance with Id '"+variableId + "' does not exist.");

    }
  }

  public Response getBinaryVariable() {

    ResponseBuilder responseBuilder = Response.ok();

    VariableInstance variableInstance = baseQuery()
      .disableObjectValueDeserialization()
      .singleResult();
    if(variableInstance != null) {

      return TypedValueUtil.writeBinaryValueToResponse(responseBuilder, variableInstance.getTypedValue());

    } else {
      throw new InvalidRequestException(Status.NOT_FOUND, "Variable instance with Id '"+variableId + "' does not exist.");
    }
  }

  protected VariableInstanceQuery baseQuery() {
    return engine.getRuntimeService()
        .createVariableInstanceQuery()
        .variableId(variableId);
  }

}
