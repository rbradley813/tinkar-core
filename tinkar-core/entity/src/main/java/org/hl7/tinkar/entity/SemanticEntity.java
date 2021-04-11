package org.hl7.tinkar.entity;

import io.activej.bytebuf.ByteBuf;
import org.eclipse.collections.api.list.ImmutableList;
import org.hl7.tinkar.common.service.PrimitiveData;
import org.hl7.tinkar.entity.internal.Get;
import org.hl7.tinkar.component.FieldDataType;
import org.hl7.tinkar.component.*;
import org.hl7.tinkar.terms.SemanticFacade;

import java.util.Arrays;

public class SemanticEntity
        extends Entity<SemanticEntityVersion>
        implements SemanticFacade, SemanticChronology<SemanticEntityVersion> {

    protected int referencedComponentNid;

    protected int patternNid;

    @Override
    protected int subclassFieldBytesSize() {
        return 4; // referenced component
    }

    @Override
    public FieldDataType dataType() {
        return FieldDataType.SEMANTIC_CHRONOLOGY;
    }

    @Override
    public ImmutableList<SemanticEntityVersion> versions() {
        return super.versions();
    }

    @Override
    protected void finishEntityWrite(ByteBuf byteBuf) {
        byteBuf.writeInt(referencedComponentNid);
        byteBuf.writeInt(patternNid);
    }

    @Override
    protected void finishEntityRead(ByteBuf readBuf, byte formatVersion) {
        this.referencedComponentNid = readBuf.readInt();
        this.patternNid = readBuf.readInt();
    }

    @Override
    protected void finishEntityRead(Chronology chronology) {
        if (chronology instanceof SemanticChronology semanticChronology) {
            referencedComponentNid = Get.entityService().nidForComponent(semanticChronology.referencedComponent());
            patternNid = Get.entityService().nidForComponent(semanticChronology.pattern());
        }
    }

    @Override
    protected SemanticEntityVersion makeVersion(ByteBuf readBuf, byte formatVersion) {
        return SemanticEntityVersion.make(this, readBuf, formatVersion);
    }

    @Override
    protected SemanticEntityVersion makeVersion(Version version) {
        return SemanticEntityVersion.make(this, (SemanticVersion) version);
    }

    @Override
    public Entity referencedComponent() {
        return Get.entityService().getEntityFast(this.referencedComponentNid);
    }

    public int referencedComponentNid() {
        return this.referencedComponentNid;
    }

    public int patternNid() {
        return this.patternNid;
    }

    @Override
    public Pattern pattern() {
        return Get.entityService().getEntityFast(patternNid);
    }


    public static SemanticEntity make(ByteBuf readBuf, byte entityFormatVersion) {
        SemanticEntity semanticEntity = new SemanticEntity();
        semanticEntity.fill(readBuf, entityFormatVersion);
        return semanticEntity;
    }

    public static SemanticEntity make(SemanticChronology other) {
        SemanticEntity semanticEntity = new SemanticEntity();
        semanticEntity.fill(other);
        return semanticEntity;
    }

    @Override
    public String toString() {
        return "SemanticEntity{" +
                "type: " + PrimitiveData.text(patternNid) +
                " <" +
                nid +
                "> " + Arrays.toString(publicId().asUuidArray()) +
                ", rc: " + PrimitiveData.text(referencedComponentNid) +
                ", v: " + versions +
                '}';
    }
}