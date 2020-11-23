/*
 * Copyright 2020 kec.
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
import org.hl7.tinkar.binary.Marshalable;
import org.hl7.tinkar.binary.Marshaler;
import org.hl7.tinkar.binary.TinkarInput;
import org.hl7.tinkar.binary.TinkarOutput;
import org.hl7.tinkar.binary.Unmarshaler;
import org.hl7.tinkar.component.DefinitionForSemantic;
import org.hl7.tinkar.component.IdentifiedThing;
import org.hl7.tinkar.component.Semantic;
import org.hl7.tinkar.json.JSONObject;
import org.hl7.tinkar.json.JsonChronologyUnmarshaler;
import org.hl7.tinkar.json.JsonMarshalable;

/**
 *
 * @author kec
 */
public record SemanticChronologyDTO(ImmutableList<UUID> componentUuids,
                                    ImmutableList<UUID> definitionForSemanticUuids,
                                    ImmutableList<UUID> referencedComponentUuids,
                                    ImmutableList<SemanticVersionDTO> semanticVersions)
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
        json.put(CLASS, this.getClass().getCanonicalName());
        json.put("componentUuids", componentUuids);
        json.put("definitionForSemanticUuids", definitionForSemanticUuids);
        json.put("referencedComponentUuids", referencedComponentUuids);
        json.put("semanticVersions", semanticVersions);
        json.writeJSONString(writer);
    }
    
    @JsonChronologyUnmarshaler
    public static SemanticChronologyDTO make(JSONObject jsonObject) {
        ImmutableList<UUID> componentUuids = jsonObject.asImmutableUuidList("componentUuids");
        ImmutableList<UUID> definitionForSemanticUuids = jsonObject.asImmutableUuidList("definitionForSemanticUuids");
        ImmutableList<UUID> referencedComponentUuids = jsonObject.asImmutableUuidList("referencedComponentUuids");
        return new SemanticChronologyDTO(componentUuids,
                definitionForSemanticUuids,
                referencedComponentUuids,
                        jsonObject.asSemanticVersionList("semanticVersions",
                                componentUuids,
                                definitionForSemanticUuids,
                                referencedComponentUuids)
                );
    }

    @Unmarshaler
    public static SemanticChronologyDTO make(TinkarInput in) {
        try {
            int objectMarshalVersion = in.readInt();
            switch (objectMarshalVersion) {
                case marshalVersion -> {
                    ImmutableList<UUID> componentUuids = in.readImmutableUuidList();
                    ImmutableList<UUID> definitionForSemanticUuids  = in.readImmutableUuidList();
                    ImmutableList<UUID> referencedComponentUuids = in.readImmutableUuidList();
                    return new SemanticChronologyDTO(
                            componentUuids, definitionForSemanticUuids, referencedComponentUuids,
                            in.readSemanticVersionList(componentUuids, definitionForSemanticUuids, referencedComponentUuids));
                }
                default -> throw new UnsupportedOperationException("Unsupported version: " + objectMarshalVersion);
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
            out.writeUuidList(componentUuids);
            out.writeUuidList(definitionForSemanticUuids);
            out.writeUuidList(referencedComponentUuids);
            out.writeSemanticVersionList(semanticVersions);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

