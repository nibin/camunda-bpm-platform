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
package org.camunda.bpm.engine.rest.impl;

import java.io.ByteArrayInputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.camunda.bpm.engine.rest.exception.InvalidRequestException;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.camunda.bpm.engine.variable.value.BytesValue;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 *
 */
public class TypedValueUtil {

  public static Response writeBinaryValueToResponse(ResponseBuilder responseBuilder, TypedValue typedValue) {

    MediaType defaultMediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;

    if(typedValue.getType() == ValueType.OBJECT) {
      ObjectValue objectValue = (ObjectValue) typedValue;

      if(objectValue.getSerializationDataFormat() != null && objectValue.getSerializationDataFormat().length() > 0) {
        // set content type if serialization dataformat available
        responseBuilder.type(objectValue.getSerializationDataFormat());
      }
      else {
        // otherwise default to octet stream
        responseBuilder.type(defaultMediaType);
      }

      responseBuilder.entity(new ByteArrayInputStream(objectValue.getValueSerialized()));

      return responseBuilder.build();

    }
    else if(typedValue.getType() == ValueType.BYTES) {
      BytesValue bytesValue = (BytesValue) typedValue;

      responseBuilder.type(defaultMediaType);

      responseBuilder.entity(new ByteArrayInputStream(bytesValue.getValue()));

      return responseBuilder.build();
    }
    else {
      // TODO: we could also return something here...
      throw new InvalidRequestException(Status.BAD_REQUEST, "Value is not a binary value.");

    }
  }

}
