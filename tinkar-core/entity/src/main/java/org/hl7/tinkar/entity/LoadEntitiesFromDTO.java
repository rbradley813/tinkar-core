package org.hl7.tinkar.entity;

import org.hl7.tinkar.dto.StampDTO;
import org.hl7.tinkar.entity.internal.Get;
import org.hl7.tinkar.dto.ConceptChronologyDTO;
import org.hl7.tinkar.component.FieldDataType;
import org.hl7.tinkar.dto.TypePatternChronologyDTO;
import org.hl7.tinkar.dto.binary.TinkarInput;
import org.hl7.tinkar.common.util.time.Stopwatch;
import org.hl7.tinkar.dto.SemanticChronologyDTO;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LoadEntitiesFromDTO {
    protected static final Logger LOG = Logger.getLogger(LoadEntitiesFromDTO.class.getName());
    final File importFile;
    final AtomicInteger importCount = new AtomicInteger();
    final Stopwatch stopwatch = new Stopwatch();


    public LoadEntitiesFromDTO(File importFile) {
        this.importFile = importFile;
        LOG.info("Loading entities from: " + importFile.getAbsolutePath());
    }

    public Integer call() throws IOException {
        try {

            try (ZipFile zipFile = new ZipFile(importFile, Charset.forName("UTF-8"))) {
                ZipEntry tinkZipEntry = zipFile.getEntry("export.tink");
                TinkarInput tinkIn = new TinkarInput(zipFile.getInputStream(tinkZipEntry));
                LOG.info(":LoadEntitiesFromDTO: begin processing");

                while (true) {
                    FieldDataType fieldDataType = FieldDataType.fromToken(tinkIn.readByte());
                    switch (fieldDataType) {
                        case CONCEPT_CHRONOLOGY: {
                            ConceptChronologyDTO ccDTO = ConceptChronologyDTO.make(tinkIn);
                            Get.entityService().putChronology(ccDTO);
                            importCount.incrementAndGet();
                        }
                        break;
                        case SEMANTIC_CHRONOLOGY: {
                            SemanticChronologyDTO scDTO = SemanticChronologyDTO.make(tinkIn);
                            Get.entityService().putChronology(scDTO);
                            importCount.incrementAndGet();
                        }
                        break;
                        case TYPE_PATTERN_CHRONOLOGY: {
                            TypePatternChronologyDTO dsDTO = TypePatternChronologyDTO.make(tinkIn);
                            Get.entityService().putChronology(dsDTO);
                            LOG.info("TypePattern: " + dsDTO);
                            importCount.incrementAndGet();
                        }
                        break;

                        default:
                            throw new UnsupportedOperationException("Can't handle fieldDataType: " + fieldDataType);

                    }
                }

            } catch (EOFException eof) {
                // continue, will autoclose.
            }
            stopwatch.end();
            LOG.info(report());
            ConceptProxy PATH_ORIGINS_ASSEMBLAGE =
                    new ConceptProxy("Path origins assemblage (SOLOR)", UUID.fromString("1239b874-41b4-32a1-981f-88b448829b4b"));
            ConceptProxy ENGLISH_LANGUAGE = new ConceptProxy("English language", UUID.fromString("06d905ea-c647-3af9-bfe5-2514e135b558"));


            ConceptProxy DESCRIPTION_PATTERN =
                    new ConceptProxy("Description pattern", UUID.fromString("a4de0039-2625-5842-8a4c-d1ce6aebf021"));
            ConceptProxy PATH_ORIGINS_PATTERN =
                    new ConceptProxy("Path Origin pattern", UUID.fromString("70f89dd5-2cdb-59bb-bbaa-98527513547c"));

            int[] originNids = Get.entityService().entityNidsOfType(PATH_ORIGINS_PATTERN.nid());
            LOG.info("Origin nids: " + Arrays.toString(originNids));
            for (int originNid: originNids) {
                LOG.info("Origin semantic: " + Get.entityService().getEntityFast(originNid));
            }
            int[] referencingSemanticNids = Get.entityService().semanticNidsForComponent(PATH_ORIGINS_ASSEMBLAGE.nid());
            for (int referencingSemanticNid: referencingSemanticNids) {
                LOG.info("Referencing semantic: " + Get.entityService().getEntityFast(referencingSemanticNid));
            }

            LOG.info("Trying type: DESCRIPTION_PATTERN");
            int[] referencingSemanticNidsOfType2 = Get.entityService().semanticNidsForComponentOfType(PATH_ORIGINS_ASSEMBLAGE.nid(), DESCRIPTION_PATTERN.nid());
            for (int referencingSemanticNid: referencingSemanticNidsOfType2) {
                LOG.info("Referencing semantic of type: " + Get.entityService().getEntityFast(referencingSemanticNid));
            }

            return importCount.get();
        } finally {
            //Get.activeTasks().remove(this);
        }
    }

    public String report() {
        return "Imported: " + importCount + " items in: " + stopwatch.elapsedTime();
    }
}
