/*
 * Copyright 2020-2021 HL7.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.hl7.tinkar.dto;

import java.io.IOException;
import java.io.Writer;
import java.util.UUID;

import org.eclipse.collections.api.list.ImmutableList;
import org.hl7.tinkar.binary.*;
import org.hl7.tinkar.component.DefinitionForSemantic;
import org.hl7.tinkar.component.IdentifiedThing;
import org.hl7.tinkar.component.Semantic;
import org.hl7.tinkar.json.JSONObject;
import org.hl7.tinkar.json.JsonMarshalable;
import org.hl7.tinkar.json.JsonSemanticVersionUnmarshaler;
import org.hl7.tinkar.json.JsonVersionUnmarshaler;

/**
 *
 * @author kec
 */
public record SemanticVersionDTO(ImmutableList<UUID> componentUuids,
                                 ImmutableList<UUID> definitionForSemanticUuids,
                                 ImmutableList<UUID> referencedComponentUuids,
                                 StampDTO stamp, ImmutableList<Object> fields)
        implements Semantic, ChangeSetThing, JsonMarshalable, Marshalable {

    private static final int marshalVersion = 1;

    @Override
    public ImmutableList<UUID> getComponentUuids() {
        return componentUuids;
    }

    @Override
    public IdentifiedThing getReferencedComponent() {
        return new IdentifiedThingDTO(referencedComponentUuids);
    }

    @Override
    public DefinitionForSemantic getDefinitionForSemantic() {
        return new DefinitionForSemanticDTO(definitionForSemanticUuids);
    }

    @Override
    public void jsonMarshal(Writer writer) {
        final JSONObject json = new JSONObject();
        json.put(STAMP, stamp);
        json.put("fields", fields);
        json.writeJSONString(writer);
    }

    @JsonSemanticVersionUnmarshaler
    public static SemanticVersionDTO make(JSONObject jsonObject,
                                          ImmutableList<UUID> componentUuids,
                                          ImmutableList<UUID> definitionForSemanticUuids,
                                          ImmutableList<UUID> referencedComponentUuids) {
        JSONObject jsonStampObject = (JSONObject) jsonObject.get(STAMP);
        return new SemanticVersionDTO(componentUuids,
                definitionForSemanticUuids,
                referencedComponentUuids,
                StampDTO.make(jsonStampObject),
                jsonObject.asImmutableObjectList("fields"));
    }

    @SemanticVersionUnmarshaler
    public static SemanticVersionDTO make(TinkarInput in,
                                          ImmutableList<UUID> componentUuids,
                                          ImmutableList<UUID> definitionForSemanticUuids,
                                          ImmutableList<UUID> referencedComponentUuids) {
        try {
            int objectMarshalVersion = in.readInt();
            switch (objectMarshalVersion) {
                case marshalVersion -> {
                    return new SemanticVersionDTO(componentUuids,
                            definitionForSemanticUuids,
                            referencedComponentUuids,
                            StampDTO.make(in),
                            in.readImmutableObjectList());
                }
                default ->
                    throw new UnsupportedOperationException("Unsupported version: " + objectMarshalVersion);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    @Marshaler
    public void marshal(TinkarOutput out) {
        try {
            out.writeInt(marshalVersion);
            stamp.marshal(out);
            out.writeObjectList(fields);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
