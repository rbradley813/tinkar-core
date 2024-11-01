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

import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.ConceptAssembler;
import dev.ikm.tinkar.composer.template.FullyQualifiedName;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.graph.DiTreeEntity;
import dev.ikm.tinkar.entity.graph.EntityVertex;
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

import java.util.UUID;

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
        EntityProxy.Concept presenceOfOrganism = EntityProxy.Concept.make("Finding of presence of organism (finding)", UUID.fromString("7e808a14-74a2-33e0-bf55-9f893ba8c5d0"));
        EntityProxy.Concept interprets = EntityProxy.Concept.make("Interprets (attribute)", UUID.fromString("75e0da0c-21ea-301f-a176-bf056788afe5"));
        EntityProxy.Concept sputum = EntityProxy.Concept.make("Sputum observable (observable entity)", UUID.fromString("afc2bb42-ebc7-32cc-ba9e-d78ef99fa57f"));
        EntityProxy.Concept hasInterpretation = EntityProxy.Concept.make("Has interpretation (attribute)", UUID.fromString("993a598d-a95a-3235-813e-59252c975070"));
        EntityProxy.Concept present = EntityProxy.Concept.make("Present (qualifier value)", UUID.fromString("850e9dff-7c45-3db4-8cf9-34c8fb7359fd"));
        EntityProxy.Concept detectionOfBacteria = EntityProxy.Concept.make("Detection of bacteria (procedure)", UUID.fromString("f5aadb4e-e760-3d6f-996d-b969b3db98e2"));

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

    DiTreeEntity expectedAcetazolamideStatedDiTree() {
        EntityProxy.Concept medicinalProduct = EntityProxy.Concept.make("Medicinal product (product)", UUID.fromString("ae364969-80c6-32b9-9c1d-eaebed617d9b"));
        EntityProxy.Concept playsRole = EntityProxy.Concept.make("Plays role (attribute)", UUID.fromString("41bc8b8b-9232-39da-8189-67f8be7bfac0"));
        EntityProxy.Concept antiglaucomaTherapeuticRole = EntityProxy.Concept.make("Antiglaucoma therapeutic role (role)", UUID.fromString("5af2b0ac-66e4-3294-a89a-21b2e4599c0b"));

        preLoadConcepts(medicinalProduct, playsRole, antiglaucomaTherapeuticRole);

        MutableList<EntityVertex> vertexMap = Lists.mutable.empty();
        MutableIntObjectMap<ImmutableIntList> successorMap = IntObjectMaps.mutable.empty();
        MutableIntIntMap predecessorMap = IntIntMaps.mutable.empty();
        int vertexIdx = 0;

        //Construct Vertex Map
        EntityVertex definitionRootVertex = createEntityVertex(vertexIdx++, TinkarTerm.DEFINITION_ROOT);
        vertexMap.add(definitionRootVertex);

        EntityVertex originVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(medicinalProduct)));
        vertexMap.add(originVertex.vertexIndex(), originVertex);

        EntityVertex roleValueVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(antiglaucomaTherapeuticRole)));
        vertexMap.add(roleValueVertex.vertexIndex(), roleValueVertex);

        EntityVertex roleVertex = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), playsRole,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex.vertexIndex(), roleVertex);

        EntityVertex andNecessaryVertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andNecessaryVertex.vertexIndex(), andNecessaryVertex);

        EntityVertex necessarySetVertex = createEntityVertex(vertexIdx++, TinkarTerm.NECESSARY_SET);
        vertexMap.add(necessarySetVertex.vertexIndex(), necessarySetVertex);

        ImmutableIntList andNecessaryChildIdxs = IntLists.immutable.of(originVertex.vertexIndex(), roleVertex.vertexIndex());
        //Construct Successor Map
        successorMap.put(definitionRootVertex.vertexIndex(), IntLists.immutable.of(necessarySetVertex.vertexIndex()));
        successorMap.put(necessarySetVertex.vertexIndex(), IntLists.immutable.of(andNecessaryVertex.vertexIndex()));
        successorMap.put(andNecessaryVertex.vertexIndex(), andNecessaryChildIdxs);
        successorMap.put(roleVertex.vertexIndex(), IntLists.immutable.of(roleValueVertex.vertexIndex()));

        //Construct Predecessor Map
        predecessorMap.put(necessarySetVertex.vertexIndex(), definitionRootVertex.vertexIndex());
        predecessorMap.put(andNecessaryVertex.vertexIndex(), necessarySetVertex.vertexIndex());
        andNecessaryChildIdxs.forEach((andNecessaryChildIdx) -> predecessorMap.put(andNecessaryChildIdx, andNecessaryVertex.vertexIndex()));
        predecessorMap.put(roleValueVertex.vertexIndex(), roleVertex.vertexIndex());

        return new DiTreeEntity(definitionRootVertex, vertexMap.toImmutable(), successorMap.toImmutable(), predecessorMap.toImmutable());
    }

    DiTreeEntity expectedAminoAcidModifiedDietStatedDiTree() {
        EntityProxy.Concept proteinModifiedDiet = EntityProxy.Concept.make("Protein and/or protein derivative modified diet (regime/therapy)", UUID.fromString("eee97196-505d-352a-a307-9aa41c4ea5cd"));
        EntityProxy.Concept hasFocus = EntityProxy.Concept.make("Has focus (attribute)", UUID.fromString("b610d820-4486-3b5e-a2c1-9b66bc718c6d"));
        EntityProxy.Concept nutritionalIntakeRequirement = EntityProxy.Concept.make("Composition of nutritional intake of amino acid inconsistent with requirement (finding)", UUID.fromString("ab748274-9007-3c3e-804d-ab4ebec55a83"));
        EntityProxy.Concept directSubstance = EntityProxy.Concept.make("Direct substance (attribute)", UUID.fromString("49ee3912-abb7-325c-88ba-a98824b4c47d"));
        EntityProxy.Concept aminoAcid = EntityProxy.Concept.make("Amino acid (substance)", UUID.fromString("45c2765a-583c-3f75-93ac-598ef630aefe"));
        EntityProxy.Concept method = EntityProxy.Concept.make("Method (attribute)", UUID.fromString("d0f9e3b1-29e4-399f-b129-36693ba4acbc"));
        EntityProxy.Concept actionAdministration = EntityProxy.Concept.make("Administration - action (qualifier value)", UUID.fromString("87cc5116-8014-32a7-b324-9e28e8658a5e"));

        EntityProxy.Concept proteinFood = EntityProxy.Concept.make("Protein food (substance)", UUID.fromString("f70fd917-7876-3c35-b3bc-7ff1b6add153"));
        EntityProxy.Concept usingSubstance = EntityProxy.Concept.make("Using substance (attribute)", UUID.fromString("996261c3-3c12-3f09-8f14-e30e85e9e70d"));

        preLoadConcepts(proteinModifiedDiet, hasFocus, nutritionalIntakeRequirement, directSubstance, aminoAcid, method, actionAdministration, proteinFood, usingSubstance);

        MutableList<EntityVertex> vertexMap = Lists.mutable.empty();
        MutableIntObjectMap<ImmutableIntList> successorMap = IntObjectMaps.mutable.empty();
        MutableIntIntMap predecessorMap = IntIntMaps.mutable.empty();
        int vertexIdx = 0;

        //Construct Vertex Map
        EntityVertex definitionRootVertex = createEntityVertex(vertexIdx++, TinkarTerm.DEFINITION_ROOT);
        vertexMap.add(definitionRootVertex);

        EntityVertex originVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(proteinModifiedDiet)));
        vertexMap.add(originVertex.vertexIndex(), originVertex);

        EntityVertex roleValueVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(actionAdministration)));
        vertexMap.add(roleValueVertex1.vertexIndex(), roleValueVertex1);

        EntityVertex roleVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), method,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex1.vertexIndex(), roleVertex1);

        EntityVertex roleValueVertex2 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(aminoAcid)));
        vertexMap.add(roleValueVertex2.vertexIndex(), roleValueVertex2);

        EntityVertex roleVertex2 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), directSubstance,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex2.vertexIndex(), roleVertex2);

        EntityVertex andRG1Vertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andRG1Vertex.vertexIndex(), andRG1Vertex);

        EntityVertex roleGroupVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), TinkarTerm.ROLE_GROUP,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleGroupVertex1.vertexIndex(), roleGroupVertex1);

        EntityVertex roleValueVertex3 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(nutritionalIntakeRequirement)));
        vertexMap.add(roleValueVertex3.vertexIndex(), roleValueVertex3);

        EntityVertex roleVertex3 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), hasFocus,
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

    DiTreeEntity expectedCopperSupplementStatedDiTree() {
        EntityProxy.Concept originConcept = EntityProxy.Concept.make("Mineral supplement (substance)", UUID.fromString("870cd418-7682-30d7-9ce6-b54325057162"));

        preLoadConcepts(originConcept);

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

    DiTreeEntity expectedNon_compliantWithPeritonealDialysisPrescriptionOwlTransformationStatedDiTree() {
        EntityProxy.Concept nonComplianceWithTreatment = EntityProxy.Concept.make("Noncompliance with treatment (finding)", UUID.fromString("de9911aa-c71c-3252-be88-6ae964acafaa"));
        EntityProxy.Concept interprets = EntityProxy.Concept.make("Interprets (attribute)", UUID.fromString("75e0da0c-21ea-301f-a176-bf056788afe5"));
        EntityProxy.Concept complianceBehaviorToTreatment = EntityProxy.Concept.make("Compliance behavior to treatment (observable entity)", UUID.fromString("74acace1-8fc9-3b72-8ea4-3b06c97a2bed"));

        preLoadConcepts(nonComplianceWithTreatment, interprets, complianceBehaviorToTreatment);

        MutableList<EntityVertex> vertexMap = Lists.mutable.empty();
        MutableIntObjectMap<ImmutableIntList> successorMap = IntObjectMaps.mutable.empty();
        MutableIntIntMap predecessorMap = IntIntMaps.mutable.empty();
        int vertexIdx = 0;

        //Construct Vertex Map
        EntityVertex definitionRootVertex = createEntityVertex(vertexIdx++, TinkarTerm.DEFINITION_ROOT);
        vertexMap.add(definitionRootVertex);

        EntityVertex originVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(nonComplianceWithTreatment)));
        vertexMap.add(originVertex.vertexIndex(), originVertex);

        EntityVertex roleValueVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(complianceBehaviorToTreatment)));
        vertexMap.add(roleValueVertex.vertexIndex(), roleValueVertex);

        EntityVertex roleVertex = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), interprets,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex.vertexIndex(), roleVertex);

        EntityVertex andRGVertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andRGVertex.vertexIndex(), andRGVertex);

        EntityVertex roleGroupVertex = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), TinkarTerm.ROLE_GROUP,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleGroupVertex.vertexIndex(), roleGroupVertex);

        EntityVertex andNecessaryVertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andNecessaryVertex.vertexIndex(), andNecessaryVertex);

        EntityVertex necessarySetVertex = createEntityVertex(vertexIdx++, TinkarTerm.NECESSARY_SET);
        vertexMap.add(necessarySetVertex.vertexIndex(), necessarySetVertex);

        ImmutableIntList andNecessaryChildIdxs = IntLists.immutable.of(originVertex.vertexIndex(), roleGroupVertex.vertexIndex());
        ImmutableIntList andRGChildIdxs = IntLists.immutable.of(roleVertex.vertexIndex());
        //Construct Successor Map
        successorMap.put(definitionRootVertex.vertexIndex(), IntLists.immutable.of(necessarySetVertex.vertexIndex()));
        successorMap.put(necessarySetVertex.vertexIndex(), IntLists.immutable.of(andNecessaryVertex.vertexIndex()));
        successorMap.put(andNecessaryVertex.vertexIndex(), andNecessaryChildIdxs);
        successorMap.put(roleGroupVertex.vertexIndex(), IntLists.immutable.of(andRGVertex.vertexIndex()));
        successorMap.put(andRGVertex.vertexIndex(), andRGChildIdxs);
        successorMap.put(roleVertex.vertexIndex(), IntLists.immutable.of(roleValueVertex.vertexIndex()));

        //Construct Predecessor Map
        predecessorMap.put(necessarySetVertex.vertexIndex(), definitionRootVertex.vertexIndex());
        predecessorMap.put(andNecessaryVertex.vertexIndex(), necessarySetVertex.vertexIndex());
        andNecessaryChildIdxs.forEach((andNecessaryChildIdx) -> predecessorMap.put(andNecessaryChildIdx, andNecessaryVertex.vertexIndex()));
        predecessorMap.put(andRGVertex.vertexIndex(), roleGroupVertex.vertexIndex());
        andRGChildIdxs.forEach((andRGChildIdx) -> predecessorMap.put(andRGChildIdx, andRGVertex.vertexIndex()));
        predecessorMap.put(roleValueVertex.vertexIndex(), roleVertex.vertexIndex());

        return new DiTreeEntity(definitionRootVertex, vertexMap.toImmutable(), successorMap.toImmutable(), predecessorMap.toImmutable());
    }

    DiTreeEntity expectedMineralModifiedDietStatedDiTree() {
        EntityProxy.Concept dietaryRegime = EntityProxy.Concept.make("Dietary regime (regime/therapy)", UUID.fromString("327e59bf-1639-3413-b013-2a971fc9ff30"));
        EntityProxy.Concept directSubstance = EntityProxy.Concept.make("Direct substance (attribute)", UUID.fromString("49ee3912-abb7-325c-88ba-a98824b4c47d"));
        EntityProxy.Concept mineral = EntityProxy.Concept.make("Mineral (substance)", UUID.fromString("842e7340-dbdf-3245-9950-c944a34625f5"));
        EntityProxy.Concept method = EntityProxy.Concept.make("Method (attribute)", UUID.fromString("d0f9e3b1-29e4-399f-b129-36693ba4acbc"));
        EntityProxy.Concept actionAdministration = EntityProxy.Concept.make("Administration - action (qualifier value)", UUID.fromString("87cc5116-8014-32a7-b324-9e28e8658a5e"));

        preLoadConcepts(dietaryRegime, directSubstance, mineral, method, actionAdministration);

        MutableList<EntityVertex> vertexMap = Lists.mutable.empty();
        MutableIntObjectMap<ImmutableIntList> successorMap = IntObjectMaps.mutable.empty();
        MutableIntIntMap predecessorMap = IntIntMaps.mutable.empty();
        int vertexIdx = 0;

        //Construct Vertex Map
        EntityVertex definitionRootVertex = createEntityVertex(vertexIdx++, TinkarTerm.DEFINITION_ROOT);
        vertexMap.add(definitionRootVertex);

        EntityVertex originVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(dietaryRegime)));
        vertexMap.add(originVertex.vertexIndex(), originVertex);

        EntityVertex roleValueVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(actionAdministration)));
        vertexMap.add(roleValueVertex1.vertexIndex(), roleValueVertex1);

        EntityVertex roleVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), method,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex1.vertexIndex(), roleVertex1);

        EntityVertex roleValueVertex2 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(mineral)));
        vertexMap.add(roleValueVertex2.vertexIndex(), roleValueVertex2);

        EntityVertex roleVertex2 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), directSubstance,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex2.vertexIndex(), roleVertex2);

        EntityVertex andRG1Vertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andRG1Vertex.vertexIndex(), andRG1Vertex);

        EntityVertex roleGroupVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), TinkarTerm.ROLE_GROUP,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleGroupVertex1.vertexIndex(), roleGroupVertex1);

        EntityVertex andNecessaryVertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andNecessaryVertex.vertexIndex(), andNecessaryVertex);

        EntityVertex necessarySetVertex = createEntityVertex(vertexIdx++, TinkarTerm.NECESSARY_SET);
        vertexMap.add(necessarySetVertex.vertexIndex(), necessarySetVertex);

        ImmutableIntList andNecessaryChildIdxs = IntLists.immutable.of(originVertex.vertexIndex(), roleGroupVertex1.vertexIndex());
        ImmutableIntList andRG1ChildIdxs = IntLists.immutable.of(roleVertex1.vertexIndex(), roleVertex2.vertexIndex());
        //Construct Successor Map
        successorMap.put(definitionRootVertex.vertexIndex(), IntLists.immutable.of(necessarySetVertex.vertexIndex()));
        successorMap.put(necessarySetVertex.vertexIndex(), IntLists.immutable.of(andNecessaryVertex.vertexIndex()));
        successorMap.put(andNecessaryVertex.vertexIndex(), andNecessaryChildIdxs);
        successorMap.put(roleGroupVertex1.vertexIndex(), IntLists.immutable.of(andRG1Vertex.vertexIndex()));
        successorMap.put(andRG1Vertex.vertexIndex(), andRG1ChildIdxs);
        successorMap.put(roleVertex1.vertexIndex(), IntLists.immutable.of(roleValueVertex1.vertexIndex()));
        successorMap.put(roleVertex2.vertexIndex(), IntLists.immutable.of(roleValueVertex2.vertexIndex()));

        //Construct Predecessor Map
        predecessorMap.put(necessarySetVertex.vertexIndex(), definitionRootVertex.vertexIndex());
        predecessorMap.put(andNecessaryVertex.vertexIndex(), necessarySetVertex.vertexIndex());
        andNecessaryChildIdxs.forEach((andNecessaryChildIdx) -> predecessorMap.put(andNecessaryChildIdx, andNecessaryVertex.vertexIndex()));
        predecessorMap.put(andRG1Vertex.vertexIndex(), roleGroupVertex1.vertexIndex());
        andRG1ChildIdxs.forEach((andRG1ChildIdx) -> predecessorMap.put(andRG1ChildIdx, andRG1Vertex.vertexIndex()));
        predecessorMap.put(roleValueVertex1.vertexIndex(), roleVertex1.vertexIndex());
        predecessorMap.put(roleValueVertex2.vertexIndex(), roleVertex2.vertexIndex());

        return new DiTreeEntity(definitionRootVertex, vertexMap.toImmutable(), successorMap.toImmutable(), predecessorMap.toImmutable());
    }

    DiTreeEntity expectedOsteoarthritisOfFingerJointOfRightHandStatedDiTree() {
        EntityProxy.Concept disease = EntityProxy.Concept.make("Disease (disorder)", UUID.fromString("ab4e618b-b954-3d56-a44b-f0f29d6f59d3"));
        EntityProxy.Concept associatedMorphology = EntityProxy.Concept.make("Associated morphology (attribute)", UUID.fromString("3161e31b-7d00-33d9-8cbd-9c33dc153aae"));
        EntityProxy.Concept degenerativeAbnormality = EntityProxy.Concept.make("Degenerative abnormality (morphologic abnormality)", UUID.fromString("b430ad90-7aa5-349e-9435-1b64f3eecd53"));
        EntityProxy.Concept findingSite = EntityProxy.Concept.make("Finding site (attribute)", UUID.fromString("3a6d919d-6c25-3aae-9bc3-983ead83a928"));
        EntityProxy.Concept rightFingerJointStructure = EntityProxy.Concept.make("Structure of joint of finger of right hand (body structure)", UUID.fromString("c73764b8-0299-396a-a978-219cb315b33d"));

        preLoadConcepts(disease, associatedMorphology, degenerativeAbnormality, findingSite, rightFingerJointStructure);

        MutableList<EntityVertex> vertexMap = Lists.mutable.empty();
        MutableIntObjectMap<ImmutableIntList> successorMap = IntObjectMaps.mutable.empty();
        MutableIntIntMap predecessorMap = IntIntMaps.mutable.empty();
        int vertexIdx = 0;

        //Construct Vertex Map
        EntityVertex definitionRootVertex = createEntityVertex(vertexIdx++, TinkarTerm.DEFINITION_ROOT);
        vertexMap.add(definitionRootVertex);

        EntityVertex originVertex = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(disease)));
        vertexMap.add(originVertex.vertexIndex(), originVertex);

        EntityVertex roleValueVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(degenerativeAbnormality)));
        vertexMap.add(roleValueVertex1.vertexIndex(), roleValueVertex1);

        EntityVertex roleVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), associatedMorphology,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex1.vertexIndex(), roleVertex1);

        EntityVertex roleValueVertex2 = createEntityVertex(vertexIdx++, TinkarTerm.CONCEPT_REFERENCE,
                IntObjectMaps.mutable.of(TinkarTerm.CONCEPT_REFERENCE.nid(), abstractObject(rightFingerJointStructure)));
        vertexMap.add(roleValueVertex2.vertexIndex(), roleValueVertex2);

        EntityVertex roleVertex2 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), findingSite,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleVertex2.vertexIndex(), roleVertex2);

        EntityVertex andRG1Vertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andRG1Vertex.vertexIndex(), andRG1Vertex);

        EntityVertex roleGroupVertex1 = createEntityVertex(vertexIdx++, TinkarTerm.ROLE,
                IntObjectMaps.mutable.of(TinkarTerm.ROLE_TYPE.nid(), TinkarTerm.ROLE_GROUP,
                        TinkarTerm.ROLE_OPERATOR.nid(), EXISTENTIAL_RESTRICTION));
        vertexMap.add(roleGroupVertex1.vertexIndex(), roleGroupVertex1);

        EntityVertex andSufficientVertex = createEntityVertex(vertexIdx++, TinkarTerm.AND);
        vertexMap.add(andSufficientVertex.vertexIndex(), andSufficientVertex);

        EntityVertex sufficientSetVertex = createEntityVertex(vertexIdx++, TinkarTerm.SUFFICIENT_SET);
        vertexMap.add(sufficientSetVertex.vertexIndex(), sufficientSetVertex);

        ImmutableIntList andSufficientChildIdxs = IntLists.immutable.of(originVertex.vertexIndex(), roleGroupVertex1.vertexIndex());
        ImmutableIntList andRG1ChildIdxs = IntLists.immutable.of(roleVertex1.vertexIndex(), roleVertex2.vertexIndex());
        //Construct Successor Map
        successorMap.put(definitionRootVertex.vertexIndex(), IntLists.immutable.of(sufficientSetVertex.vertexIndex()));
        successorMap.put(sufficientSetVertex.vertexIndex(), IntLists.immutable.of(andSufficientVertex.vertexIndex()));
        successorMap.put(andSufficientVertex.vertexIndex(), andSufficientChildIdxs);
        successorMap.put(roleGroupVertex1.vertexIndex(), IntLists.immutable.of(andRG1Vertex.vertexIndex()));
        successorMap.put(andRG1Vertex.vertexIndex(), andRG1ChildIdxs);
        successorMap.put(roleVertex1.vertexIndex(), IntLists.immutable.of(roleValueVertex1.vertexIndex()));
        successorMap.put(roleVertex2.vertexIndex(), IntLists.immutable.of(roleValueVertex2.vertexIndex()));

        //Construct Predecessor Map
        predecessorMap.put(sufficientSetVertex.vertexIndex(), definitionRootVertex.vertexIndex());
        predecessorMap.put(andSufficientVertex.vertexIndex(), sufficientSetVertex.vertexIndex());
        andSufficientChildIdxs.forEach((andSufficientChildIdx) -> predecessorMap.put(andSufficientChildIdx, andSufficientVertex.vertexIndex()));
        predecessorMap.put(andRG1Vertex.vertexIndex(), roleGroupVertex1.vertexIndex());
        andRG1ChildIdxs.forEach((andRG1ChildIdx) -> predecessorMap.put(andRG1ChildIdx, andRG1Vertex.vertexIndex()));
        predecessorMap.put(roleValueVertex1.vertexIndex(), roleVertex1.vertexIndex());
        predecessorMap.put(roleValueVertex2.vertexIndex(), roleVertex2.vertexIndex());

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
