/*
 * Copyright © 2015 Integrated Knowledge Management (support@ikm.dev)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ikm.tinkar.ext.lang.owl;


import dev.ikm.tinkar.common.id.IntIdSet;
import dev.ikm.tinkar.common.id.IntIds;
import dev.ikm.tinkar.common.id.impl.IntId1Set;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.TrackingCallable;
import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
import dev.ikm.tinkar.coordinate.logic.PremiseType;
import dev.ikm.tinkar.coordinate.stamp.StampCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.StampPositionRecord;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.SemanticEntity;
import dev.ikm.tinkar.entity.SemanticEntityVersion;
import dev.ikm.tinkar.entity.SemanticRecord;
import dev.ikm.tinkar.entity.SemanticRecordBuilder;
import dev.ikm.tinkar.entity.SemanticVersionRecord;
import dev.ikm.tinkar.entity.SemanticVersionRecordBuilder;
import dev.ikm.tinkar.entity.StampEntity;
import dev.ikm.tinkar.entity.graph.DiTreeEntity;
import dev.ikm.tinkar.entity.graph.adaptor.axiom.LogicalExpression;
import dev.ikm.tinkar.entity.graph.isomorphic.IsomorphicResults;
import dev.ikm.tinkar.entity.graph.isomorphic.IsomorphicResultsLeafHash;
import dev.ikm.tinkar.entity.transaction.Transaction;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.eclipse.collections.api.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class OwlToLogicAxiomTransformerAndWriter extends TrackingCallable<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(OwlToLogicAxiomTransformerAndWriter.class);

    /**
     * The never role group set.
     */
    private final IntIdSet neverRoleGroupSet = IntIds.set.of(
            TinkarTerm.PART_OF.nid(),
            TinkarTerm.LATERALITY.nid(),
            TinkarTerm.HAS_ACTIVE_INGREDIENT.nid(),
            TinkarTerm.HAS_DOSE_FORM.nid()
    );

    private final IntIdSet definingCharacteristicSet = IntIds.set.of(
            TinkarTerm.INFERRED_PREMISE_TYPE.nid(),
            TinkarTerm.STATED_PREMISE_TYPE.nid()
    );

    private final int destinationPatternNid;
    private final Semaphore writeSemaphore;
    private final List<TransformationGroup> transformationRecords;
    private Transaction transaction;
    private int authorNid = TinkarTerm.USER.nid();
    private int moduleNid = TinkarTerm.SOLOR_OVERLAY_MODULE.nid();
    private int pathNid = TinkarTerm.DEVELOPMENT_PATH.nid();

    private static final AtomicInteger foundWatchCount = new AtomicInteger(0);

    /**
     * @param transaction           - if supplied, this does NOT commit the transaction.  If not supplied, this creates (and commits) its own transaction.
     * @param transformationRecords
     * @param writeSemaphore
     */
    public OwlToLogicAxiomTransformerAndWriter(Transaction transaction, List<TransformationGroup> transformationRecords,
                                               int destinationPatternNid, Semaphore writeSemaphore) {

        this.transaction = transaction;
        this.transformationRecords = transformationRecords;
        this.destinationPatternNid = destinationPatternNid;
        this.writeSemaphore = writeSemaphore;
        this.writeSemaphore.acquireUninterruptibly();
        updateTitle("EL++ OWL transformation");
        updateMessage("");
        addToTotalWork(transformationRecords.size());
    }

    public OwlToLogicAxiomTransformerAndWriter(Transaction transaction, List<TransformationGroup> transformationRecords,
                                               int destinationPatternNid, Semaphore writeSemaphore,
                                               int authorNid, int moduleNid, int pathNid) {
        this(transaction, transformationRecords, destinationPatternNid, writeSemaphore);
        this.authorNid = authorNid;
        this.moduleNid = moduleNid;
        this.pathNid = pathNid;
    }

    @Override
    public Void compute() throws Exception {
        try {
            boolean commitTransaction = this.transaction == null;
            if (commitTransaction) {
                this.transaction = Transaction.make("OwlTransformerAndWriter");
            }
            int count = 0;

            LOG.debug("starting batch transform of {} records", transformationRecords.size());
            for (TransformationGroup transformationGroup : transformationRecords) {
                try {
                    transformOwlExpressions(transformationGroup.conceptNid, transformationGroup.semanticNids, transformationGroup.getPremiseType());
                } catch (Exception e) {
                    LOG.error("Error in Owl Transform: ", e);
                }
                if (count % 1000 == 0) {
                    updateMessage("Processing concept: " + PrimitiveData.text(transformationGroup.conceptNid));
                    LOG.trace("Processing concept: {}", PrimitiveData.text(transformationGroup.conceptNid));
                }
                count++;
                completedUnitOfWork();
            }
            if (commitTransaction) {
                transaction.commit();
            }
            LOG.debug("Finished processing batch of: {}", count);
            return null;
        } finally {
            this.writeSemaphore.release();
        }
    }

    /**
     * Transform relationships.
     *
     * @param premiseType the stated
     */
    private void transformOwlExpressions(int conceptNid, int[] owlNids, PremiseType premiseType) throws Exception {
        updateMessage("Converting " + premiseType + " Owl expressions");

        for (int owlNid : owlNids) {
            SemanticEntity<SemanticEntityVersion> owlChronology = EntityService.get().getEntityFast(owlNid);
            for (SemanticEntityVersion owlVersion : owlChronology.versions()) {
                // TODO use pattern to get field
                String owlExpression = (String) owlVersion.fieldValues().get(0);

                StringBuilder propertyBuilder = new StringBuilder();
                StringBuilder classBuilder = new StringBuilder();

                if (owlExpression.toLowerCase().contains("property")) {
                    propertyBuilder.append(" ").append(owlExpression);
                    if (!owlExpression.toLowerCase().contains("objectpropertychain")) {
                        //TODO ask Michael Lawley if this is ok...
                        String tempExpression = owlExpression.toLowerCase().replace("subobjectpropertyof", " subclassof");
                        tempExpression = tempExpression.toLowerCase().replace("subdatapropertyof", " subclassof");
                        classBuilder.append(" ").append(tempExpression);
                    }
                } else {
                    classBuilder.append(" ").append(owlExpression);
                }

                String owlClassExpressionsToProcess = classBuilder.toString();
                String owlPropertyExpressionsToProcess = propertyBuilder.toString();

                LogicalExpression logicalExpression = SctOwlUtilities.sctToLogicalExpression(
                        owlClassExpressionsToProcess,
                        owlPropertyExpressionsToProcess);

                // See if a semantic already exists in this pattern referencing this concept...
                int[] destinationSemanticNids = EntityService.get().semanticNidsForComponentOfPattern(conceptNid, destinationPatternNid);
                switch (destinationSemanticNids.length) {
                    case 0 -> newSemanticWithVersion(conceptNid, logicalExpression, owlVersion.stamp());
                    case 1 -> addSemanticVersionIfAbsent(conceptNid, logicalExpression, owlVersion.stamp(), destinationSemanticNids[0]);
                    default -> throw new IllegalStateException("To many graphs for component: " + PrimitiveData.text(conceptNid));
                }
            }
        }
    }

    private void newSemanticWithVersion(int conceptNid, LogicalExpression logicalExpression, StampEntity stamp) {
        // Create UUID from seed and assign SemanticBuilder the value
        UUID generartedSemanticUuid = UuidT5Generator.singleSemanticUuid(EntityService.get().getEntityFast(destinationPatternNid),
                EntityService.get().getEntityFast(conceptNid));

        SemanticRecordBuilder newSemanticBuilder = SemanticRecordBuilder.builder()
                .mostSignificantBits(generartedSemanticUuid.getMostSignificantBits())
                .leastSignificantBits(generartedSemanticUuid.getLeastSignificantBits())
                .patternNid(destinationPatternNid)
                .referencedComponentNid(conceptNid)
                .nid(PrimitiveData.nid(generartedSemanticUuid))
                .versions(Lists.immutable.empty());

        addNewVersion(logicalExpression, newSemanticBuilder.build(), stamp);
    }

    private void addSemanticVersionIfAbsent(int conceptNid, LogicalExpression logicalExpression,
                                            StampEntity stamp, int semanticNid) throws Exception {
        StampCoordinateRecord stampCoordinateRecord = StampCoordinateRecord.make(
                StateSet.of(stamp.state()),
                StampPositionRecord.make(stamp.time(), stamp.pathNid()),
                new IntId1Set(stamp.moduleNid()));

        SemanticRecord existingSemantic = EntityService.get().getEntityFast(semanticNid);
        Latest<SemanticVersionRecord> latest = stampCoordinateRecord.stampCalculator().latest(semanticNid);

        if (latest.isAbsent() || !latest.get().stamp().state().equals(stamp.state())) {
            // Latest is non-existent or this version changes the state, need to add new.
            addNewVersion(logicalExpression, SemanticRecordBuilder.builder(existingSemantic).build(), stamp);
        } else {
            DiTreeEntity latestExpression = (DiTreeEntity) latest.get().fieldValues().get(0);
            DiTreeEntity newExpression = (DiTreeEntity) logicalExpression.sourceGraph();

            IsomorphicResultsLeafHash isomorphicResultsComputer = new IsomorphicResultsLeafHash(latestExpression, newExpression, conceptNid);
            IsomorphicResults isomorphicResults = isomorphicResultsComputer.call();

            if (!isomorphicResults.equivalent()) {
                addNewVersion(logicalExpression, SemanticRecordBuilder.builder(existingSemantic).build(), stamp);
            }
        }
    }

    private void addNewVersion(LogicalExpression logicalExpression,
                               SemanticRecord semanticRecord, StampEntity stamp) {

        StampEntity transactionStamp = transaction.getStamp(stamp.state(), stamp.time(), authorNid, stamp.moduleNid(), stamp.pathNid());

        SemanticVersionRecordBuilder semanticVersionBuilder = SemanticVersionRecordBuilder.builder()
                .fieldValues(Lists.immutable.of(logicalExpression.sourceGraph()))
                .stampNid(transactionStamp.nid())
                .chronology(semanticRecord);

        SemanticRecord newSemanticRecord = semanticRecord.analogueBuilder().with(semanticVersionBuilder.build()).build();
        EntityService.get().putEntity(newSemanticRecord);
    }
}
