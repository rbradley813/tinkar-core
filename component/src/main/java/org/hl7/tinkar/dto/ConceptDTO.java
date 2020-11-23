package org.hl7.tinkar.dto;

import org.eclipse.collections.api.list.ImmutableList;
import org.hl7.tinkar.component.Concept;

import java.util.UUID;

public record ConceptDTO(ImmutableList<UUID> uuids) implements Concept {

    @Override
    public ImmutableList<UUID> getComponentUuids() {
        return uuids;
    }
}
