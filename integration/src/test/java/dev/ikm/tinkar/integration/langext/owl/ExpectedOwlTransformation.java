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
package dev.ikm.tinkar.integration.langext.owl;

import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.component.Concept;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.ConceptAssembler;
import dev.ikm.tinkar.composer.template.FullyQualifiedName;
import dev.ikm.tinkar.entity.ConceptRecord;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.graph.DiTreeEntity;
import dev.ikm.tinkar.entity.graph.EntityVertex;
import dev.ikm.tinkar.entity.transaction.Transaction;
import dev.ikm.tinkar.terms.ConceptFacade;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.IntIntMaps;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;

import java.util.List;

import static dev.ikm.tinkar.entity.graph.EntityVertex.abstractObject;
import static dev.ikm.tinkar.terms.TinkarTerm.EXISTENTIAL_RESTRICTION;
import static dev.ikm.tinkar.terms.TinkarTerm.HEALTH_CONCEPT;
import static dev.ikm.tinkar.terms.TinkarTerm.TRANSITIVE_PROPERTY;

public class ExpectedOwlTransformation {

    DiTreeEntity expectedClinicalFindingStatedDiTree() {
        EntityProxy.Concept originConcept = HEALTH_CONCEPT;

        MutableList<EntityVertex> vertexMap = Lists.mutable.empty();
        MutableIntObjectMap<ImmutableIntList> successorMap = IntObjectMaps.mutable.empty();
        MutableIntIntMap predecessorMap = IntIntMaps.mutable.empty();
        int vertexIdx = 0;

        //Construct Vertex Map
        EntityVertex definitionRootVertex = createEntityVertex(vertexIdx++, TinkarTerm.DEFINITION_ROOT);
        vertexMap.add(definitionRootVertex);

        EntityVertex originVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(originConcept)));
        vertexMap.add(originVertex.vertexIndex(), originVertex);

        EntityVertex andVertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andVertex.vertexIndex(), andVertex);

        EntityVertex necessarySetVertex = createEntityVertex(vertexIdx++, TinkarTerm.NECESSARY_SET);
        vertexMap.add(necessarySetVertex.vertexIndex(), necessarySetVertex);

        //Construct Successor Map
        successorMap.put(definitionRootVertex.vertexIndex(), IntLists.immutable.of(necessarySetVertex.vertexIndex()));
        successorMap.put(andVertex.vertexIndex(), IntLists.immutable.of(originVertex.vertexIndex()));
        successorMap.put(necessarySetVertex.vertexIndex(), IntLists.immutable.of(andVertex.vertexIndex()));

        //Construct Predecessor Map
        predecessorMap.put(originVertex.vertexIndex(), andVertex.vertexIndex());
        predecessorMap.put(andVertex.vertexIndex(), necessarySetVertex.vertexIndex());
        predecessorMap.put(necessarySetVertex.vertexIndex(), definitionRootVertex.vertexIndex());

        return new DiTreeEntity(definitionRootVertex, vertexMap.toImmutable(), successorMap.toImmutable(), predecessorMap.toImmutable());
    }

    DiTreeEntity expectedPartOfStatedDiTree() {
        EntityProxy.Concept originConcept = TRANSITIVE_PROPERTY;
        EntityProxy.Concept propertyConcept = TRANSITIVE_PROPERTY;

        MutableList<EntityVertex> vertexMap = Lists.mutable.empty();
        MutableIntObjectMap<ImmutableIntList> successorMap = IntObjectMaps.mutable.empty();
        MutableIntIntMap predecessorMap = IntIntMaps.mutable.empty();
        int vertexIdx = 0;

        //Construct Vertex Map
        EntityVertex definitionRootVertex = createEntityVertex(vertexIdx++, TinkarTerm.DEFINITION_ROOT);
        vertexMap.add(definitionRootVertex);

        EntityVertex originVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(originConcept)));
        vertexMap.add(originVertex.vertexIndex(), originVertex);

        EntityVertex andNecessaryVertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andNecessaryVertex.vertexIndex(), andNecessaryVertex);

        EntityVertex necessarySetVertex = createEntityVertex(vertexIdx++, TinkarTerm.NECESSARY_SET);
        vertexMap.add(necessarySetVertex.vertexIndex(), necessarySetVertex);

        EntityVertex propertyValueVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(propertyConcept)));
        vertexMap.add(propertyValueVertex.vertexIndex(), propertyValueVertex);

        EntityVertex andPropertyVertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andPropertyVertex.vertexIndex(), andPropertyVertex);

        EntityVertex propertySetVertex = createEntityVertex(vertexIdx++, TinkarTerm.PROPERTY_SET);
        vertexMap.add(propertySetVertex.vertexIndex(), propertySetVertex);

        //Construct Successor Map
        successorMap.put(definitionRootVertex.vertexIndex(), IntLists.immutable.of(necessarySetVertex.vertexIndex(), propertySetVertex.vertexIndex()));
        successorMap.put(andNecessaryVertex.vertexIndex(), IntLists.immutable.of(originVertex.vertexIndex()));
        successorMap.put(necessarySetVertex.vertexIndex(), IntLists.immutable.of(andNecessaryVertex.vertexIndex()));
        successorMap.put(andPropertyVertex.vertexIndex(), IntLists.immutable.of(propertyValueVertex.vertexIndex()));
        successorMap.put(propertySetVertex.vertexIndex(), IntLists.immutable.of(andPropertyVertex.vertexIndex()));

        //Construct Predecessor Map
        predecessorMap.put(originVertex.vertexIndex(), andNecessaryVertex.vertexIndex());
        predecessorMap.put(andNecessaryVertex.vertexIndex(), necessarySetVertex.vertexIndex());
        predecessorMap.put(necessarySetVertex.vertexIndex(), definitionRootVertex.vertexIndex());
        predecessorMap.put(propertyValueVertex.vertexIndex(), andPropertyVertex.vertexIndex());
        predecessorMap.put(andPropertyVertex.vertexIndex(), propertySetVertex.vertexIndex());
        predecessorMap.put(propertySetVertex.vertexIndex(), definitionRootVertex.vertexIndex());

        return new DiTreeEntity(definitionRootVertex, vertexMap.toImmutable(), successorMap.toImmutable(), predecessorMap.toImmutable());
    }

    DiTreeEntity expectedAcid_fastBacilliInSputumStatedDiTree() {
        EntityProxy.Concept presenceOfOrganism = EntityProxy.Concept.make("Finding of presence of organism (finding)", UuidUtil.fromSNOMED("365690003"));
        EntityProxy.Concept interprets = EntityProxy.Concept.make("Interprets (attribute)", UuidUtil.fromSNOMED("363714003"));
        EntityProxy.Concept sputum = EntityProxy.Concept.make("Sputum observable (observable entity)", UuidUtil.fromSNOMED("364695003"));
        EntityProxy.Concept hasInterpretation = EntityProxy.Concept.make("Has interpretation (attribute)", UuidUtil.fromSNOMED("363713009"));
        EntityProxy.Concept present = EntityProxy.Concept.make("Present (qualifier value)", UuidUtil.fromSNOMED("52101004"));
        EntityProxy.Concept detectionOfBacteria = EntityProxy.Concept.make("Detection of bacteria (procedure)", UuidUtil.fromSNOMED("117725006"));

        preLoadConcepts(presenceOfOrganism, interprets, sputum, hasInterpretation, present, detectionOfBacteria);

        MutableList<EntityVertex> vertexMap = Lists.mutable.empty();
        MutableIntObjectMap<ImmutableIntList> successorMap = IntObjectMaps.mutable.empty();
        MutableIntIntMap predecessorMap = IntIntMaps.mutable.empty();
        int vertexIdx = 0;

        //Construct Vertex Map
        EntityVertex definitionRootVertex = createEntityVertex(vertexIdx++, TinkarTerm.DEFINITION_ROOT);
        vertexMap.add(definitionRootVertex);

        EntityVertex originVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(presenceOfOrganism)));
        vertexMap.add(originVertex.vertexIndex(), originVertex);

        EntityVertex roleValueVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(present)));
        vertexMap.add(roleValueVertex1.vertexIndex(), roleValueVertex1);

        EntityVertex roleVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), hasInterpretation,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex1.vertexIndex(), roleVertex1);

        EntityVertex roleValueVertex2 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(detectionOfBacteria)));
        vertexMap.add(roleValueVertex2.vertexIndex(), roleValueVertex2);

        EntityVertex roleVertex2 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), interprets,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex2.vertexIndex(), roleVertex2);

        EntityVertex andRG1Vertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andRG1Vertex.vertexIndex(), andRG1Vertex);

        EntityVertex roleGroupVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), TinkarTerm.ROLE_GROUP,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleGroupVertex1.vertexIndex(), roleGroupVertex1);

        EntityVertex roleValueVertex3 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(sputum)));
        vertexMap.add(roleValueVertex3.vertexIndex(), roleValueVertex3);

        EntityVertex roleVertex3 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), interprets,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex3.vertexIndex(), roleVertex3);

        EntityVertex andRG2Vertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andRG2Vertex.vertexIndex(), andRG2Vertex);

        EntityVertex roleGroupVertex2 = createEntityVertex(vertexIdx++,  TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), TinkarTerm.ROLE_GROUP,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleGroupVertex2.vertexIndex(), roleGroupVertex2);

        EntityVertex andNecessaryVertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andNecessaryVertex.vertexIndex(), andNecessaryVertex);

        EntityVertex necessarySetVertex = createEntityVertex(vertexIdx++, TinkarTerm.NECESSARY_SET);
        vertexMap.add(necessarySetVertex.vertexIndex(), necessarySetVertex);

        ImmutableIntList andNecessaryChildIdxs = IntLists.immutable.of(originVertex.vertexIndex(), roleGroupVertex1.vertexIndex(), roleGroupVertex2.vertexIndex());
        ImmutableIntList andRG1ChildIdxs = IntLists.immutable.of(roleVertex1.vertexIndex(), roleVertex2.vertexIndex());
        ImmutableIntList andRG2ChildIdxs = IntLists.immutable.of(roleVertex3.vertexIndex());
        //Construct Successor Map
        successorMap.put(definitionRootVertex.vertexIndex(), IntLists.immutable.of(necessarySetVertex.vertexIndex()));
        successorMap.put(necessarySetVertex.vertexIndex(), IntLists.immutable.of(andNecessaryVertex.vertexIndex()));
        successorMap.put(andNecessaryVertex.vertexIndex(), andNecessaryChildIdxs);
        successorMap.put(roleGroupVertex1.vertexIndex(), IntLists.immutable.of(andRG1Vertex.vertexIndex()));
        successorMap.put(andRG1Vertex.vertexIndex(), andRG1ChildIdxs);
        successorMap.put(roleVertex1.vertexIndex(), IntLists.immutable.of(roleValueVertex1.vertexIndex()));
        successorMap.put(roleGroupVertex2.vertexIndex(), IntLists.immutable.of(andRG2Vertex.vertexIndex()));
        successorMap.put(andRG2Vertex.vertexIndex(), andRG2ChildIdxs);
        successorMap.put(roleVertex2.vertexIndex(), IntLists.immutable.of(roleValueVertex2.vertexIndex()));
        successorMap.put(roleVertex3.vertexIndex(), IntLists.immutable.of(roleValueVertex3.vertexIndex()));

        //Construct Predecessor Map
        predecessorMap.put(necessarySetVertex.vertexIndex(), definitionRootVertex.vertexIndex());
        predecessorMap.put(andNecessaryVertex.vertexIndex(), necessarySetVertex.vertexIndex());
        andNecessaryChildIdxs.forEach((andNecessaryChildIdx) -> predecessorMap.put(andNecessaryChildIdx, andNecessaryVertex.vertexIndex()));
        predecessorMap.put(andRG1Vertex.vertexIndex(), roleGroupVertex1.vertexIndex());
        andRG1ChildIdxs.forEach((andRG1ChildIdx) -> predecessorMap.put(andRG1ChildIdx, andRG1Vertex.vertexIndex()));
        predecessorMap.put(roleValueVertex1.vertexIndex(), roleVertex1.vertexIndex());
        andRG2ChildIdxs.forEach((andRG2ChildIdx) -> predecessorMap.put(andRG2ChildIdx, andRG2Vertex.vertexIndex()));
        predecessorMap.put(roleValueVertex2.vertexIndex(), roleVertex2.vertexIndex());
        predecessorMap.put(roleValueVertex3.vertexIndex(), roleVertex3.vertexIndex());

        return new DiTreeEntity(definitionRootVertex, vertexMap.toImmutable(), successorMap.toImmutable(), predecessorMap.toImmutable());
    }

    private EntityVertex createEntityVertex(int vertexIndex, ConceptFacade conceptFacade){
        return createEntityVertex(vertexIndex, conceptFacade, IntObjectMaps.mutable.empty());
    }

    private EntityVertex createEntityVertex(int vertexIndex, ConceptFacade conceptFacade, MutableIntObjectMap<Object> properties){
        EntityVertex entityVertex = EntityVertex.make(conceptFacade);
        entityVertex.setVertexIndex(vertexIndex);
        entityVertex.setProperties(properties);
        return entityVertex;
    }

    private void preLoadConcepts(EntityProxy.Concept... supportingConcepts) {
        Composer composer = new Composer("preLoadConcepts");
        Session session = composer.open(State.ACTIVE, TinkarTerm.USER, TinkarTerm.MODULE, TinkarTerm.DEVELOPMENT_PATH);

        for (EntityProxy.Concept concept : supportingConcepts) {
            EntityService.get().getEntity(concept.publicId()).ifPresentOrElse((ignored) -> {},
                    () -> {
                        session.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                                .concept(concept)
                                .attach((FullyQualifiedName fqn) -> fqn
                                        .language(TinkarTerm.ENGLISH_LANGUAGE)
                                        .text(concept.description())
                                        .caseSignificance(TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)));
                    });
        }
        composer.commitSession(session);
    }

}
