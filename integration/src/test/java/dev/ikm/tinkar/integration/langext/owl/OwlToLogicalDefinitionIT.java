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

import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.ConceptAssembler;
import dev.ikm.tinkar.composer.template.AxiomSyntax;
import dev.ikm.tinkar.coordinate.Calculators;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticEntityVersion;
import dev.ikm.tinkar.entity.graph.DiTreeEntity;
import dev.ikm.tinkar.entity.transaction.Transaction;
import dev.ikm.tinkar.ext.lang.owl.Rf2OwlToLogicAxiomTransformer;
import dev.ikm.tinkar.integration.TestConstants;
import dev.ikm.tinkar.integration.helper.DataStore;
import dev.ikm.tinkar.integration.helper.TestHelper;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static dev.ikm.tinkar.terms.TinkarTerm.AUTHOR_FOR_VERSION;
import static dev.ikm.tinkar.terms.TinkarTerm.DEVELOPMENT_MODULE;
import static dev.ikm.tinkar.terms.TinkarTerm.DEVELOPMENT_PATH;
import static dev.ikm.tinkar.terms.TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN;
import static dev.ikm.tinkar.terms.TinkarTerm.EL_PLUS_PLUS_STATED_TERMINOLOGICAL_AXIOMS;
import static dev.ikm.tinkar.terms.TinkarTerm.OWL_AXIOM_SYNTAX_PATTERN;
import static dev.ikm.tinkar.terms.TinkarTerm.SOLOR_OVERLAY_MODULE;
import static dev.ikm.tinkar.terms.TinkarTerm.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OwlToLogicalDefinitionIT {
    private static final Logger LOG = LoggerFactory.getLogger(OwlToLogicalDefinitionIT.class);
    private final ExpectedOwlTransformation expectedOwlTransformationProvider = new ExpectedOwlTransformation();
    private StampCalculator stampCalc;
    private PatternEntityVersion latestStatedAxiomPatternVersion;
    private final EntityProxy.Semantic axiomSemanticCore = EntityProxy.Semantic.make(PublicIds.newRandom());
    private final EntityProxy.Semantic axiomSemanticNIH = EntityProxy.Semantic.make(PublicIds.newRandom());
    private final EntityProxy.Concept moduleCore = EntityProxy.Concept.make(PublicIds.newRandom());
    private final EntityProxy.Concept moduleNIH = EntityProxy.Concept.make(PublicIds.newRandom());

    @BeforeAll
    public void beforeAll() {
        TestHelper.startDataBase(DataStore.EPHEMERAL_STORE);
        TestHelper.loadDataFile(TestConstants.PB_STARTER_DATA);
        stampCalc = Calculators.Stamp.DevelopmentLatest();
        latestStatedAxiomPatternVersion = stampCalc.latestPatternEntityVersion(EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid()).get();
    }

    @AfterAll
    public void afterAll(){
        TestHelper.stopDatabase();
    }

    private long dateStringToEpochMillis(String dateString) {
        long epochMillis;
        try {
            epochMillis = new SimpleDateFormat("yyyyMMdd").parse(dateString).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return epochMillis;
    }

    private void runOwltoLogicalTransformation() {
        Transaction owlTransformationTransaction = Transaction.make();
        try {
            new Rf2OwlToLogicAxiomTransformer(
                    owlTransformationTransaction,
                    OWL_AXIOM_SYNTAX_PATTERN,
                    EL_PLUS_PLUS_STATED_AXIOMS_PATTERN).call();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Test
    public void clinicalFindingOwlTransformation() {
        String owlAxiom = "SubClassOf(:[bd83b1dd-5a82-34fa-bb52-06f666420a1c] :[ee9ac5d2-a07c-3981-a57a-f7f26baf38d8])";
        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("bd83b1dd-5a82-34fa-bb52-06f666420a1c"));
        long millisTimeStamp = dateStringToEpochMillis("20190731");

        Composer composer = new Composer("clinicalFindingOwlTransformation");
        Session session = composer.open(State.ACTIVE, millisTimeStamp, AUTHOR_FOR_VERSION, DEVELOPMENT_MODULE, DEVELOPMENT_PATH);
        session.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                .concept(testConcept)
                .attach((AxiomSyntax axiomSyntax) -> axiomSyntax
                        .text(owlAxiom)));
        composer.commitSession(session);

        runOwltoLogicalTransformation();

        int[] actualStatedDefNids = EntityService.get().semanticNidsForComponentOfPattern(testConcept.nid(), EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid());
        assertEquals(1, actualStatedDefNids.length, "Did not find expected number of actual Stated Axioms");
        SemanticEntityVersion actualLatestStatedDef = (SemanticEntityVersion) stampCalc.latest(actualStatedDefNids[0]).get();

        DiTreeEntity actualStatedDiTree = latestStatedAxiomPatternVersion.getFieldWithMeaning(EL_PLUS_PLUS_STATED_TERMINOLOGICAL_AXIOMS, actualLatestStatedDef);
        DiTreeEntity expectedStatedDiTree = expectedOwlTransformationProvider.expectedClinicalFindingStatedDiTree();

        assertEquals(State.ACTIVE, actualLatestStatedDef.state());
        assertEquals(millisTimeStamp, actualLatestStatedDef.time());
        assertEquals(USER, actualLatestStatedDef.author());
        assertEquals(SOLOR_OVERLAY_MODULE, actualLatestStatedDef.module());
        assertEquals(DEVELOPMENT_PATH, actualLatestStatedDef.path());
        assertEquals(expectedStatedDiTree, actualStatedDiTree,
                "Owl to Logical Definition Transformation did not produce the expected Stated Definition DiTreeEntity");
    }

    @Test
    public void partOfOwlTransformation() {
        String owlAxiom = "TransitiveObjectProperty(:[b4c3f6f9-6937-30fd-8412-d0c77f8a7f73])";
        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("b4c3f6f9-6937-30fd-8412-d0c77f8a7f73"));
        EntityProxy.Semantic axiomSemantic = EntityProxy.Semantic.make(PublicIds.singleSemanticId(OWL_AXIOM_SYNTAX_PATTERN, testConcept.publicId()));
        long activeMillisTimeStamp = dateStringToEpochMillis("20180731");
        long inactiveMillisTimeStamp = dateStringToEpochMillis("20190131");

        Composer composer = new Composer("partOfOwlTransformation");
        Session activeSession = composer.open(State.ACTIVE, activeMillisTimeStamp, AUTHOR_FOR_VERSION, DEVELOPMENT_MODULE, DEVELOPMENT_PATH);
        activeSession.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                .concept(testConcept)
                .attach((AxiomSyntax axiomSyntax) -> axiomSyntax
                        .semantic(axiomSemantic)
                        .text(owlAxiom)));
        composer.commitSession(activeSession);

        Session inactiveSession = composer.open(State.INACTIVE, inactiveMillisTimeStamp, AUTHOR_FOR_VERSION, DEVELOPMENT_MODULE, DEVELOPMENT_PATH);
        inactiveSession.compose(new AxiomSyntax().semantic(axiomSemantic).text(owlAxiom), testConcept);
        composer.commitSession(inactiveSession);

        runOwltoLogicalTransformation();

        int[] actualStatedDefNids = EntityService.get().semanticNidsForComponentOfPattern(testConcept.nid(), EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid());
        assertEquals(1, actualStatedDefNids.length, "Did not find expected number of actual Stated Axioms");
        SemanticEntityVersion actualLatestStatedDef = (SemanticEntityVersion) stampCalc.latest(actualStatedDefNids[0]).get();

        DiTreeEntity actualStatedDiTree = latestStatedAxiomPatternVersion.getFieldWithMeaning(EL_PLUS_PLUS_STATED_TERMINOLOGICAL_AXIOMS, actualLatestStatedDef);
        DiTreeEntity expectedStatedDiTree = expectedOwlTransformationProvider.expectedPartOfStatedDiTree();

        assertEquals(State.ACTIVE, actualLatestStatedDef.state());
        assertEquals(activeMillisTimeStamp, actualLatestStatedDef.time());
        assertEquals(USER, actualLatestStatedDef.author());
        assertEquals(SOLOR_OVERLAY_MODULE, actualLatestStatedDef.module());
        assertEquals(DEVELOPMENT_PATH, actualLatestStatedDef.path());
        assertEquals(expectedStatedDiTree, actualStatedDiTree,
                "Owl to Logical Definition Transformation did not produce the expected Stated Definition DiTreeEntity");
    }


    @Test
    public void acid_fastBacilliInSputumOwlTransformation() {
        String owlAxiom1 = "SubClassOf(:[e1267848-0192-3d54-9e65-375558b46038] ObjectIntersectionOf(:[eb63277b-d57e-3061-aad8-18f8f8f840cf] :[54edaa6f-1ec2-3437-aee5-e0c2c31b7636]))";
        String owlAxiom2 = "SubClassOf(:[e1267848-0192-3d54-9e65-375558b46038] :[5fdcf819-f57f-3b9b-87d6-f48f483b70f2])";
        String owlAxiom3 = "SubClassOf(:[e1267848-0192-3d54-9e65-375558b46038] ObjectIntersectionOf(:[7e808a14-74a2-33e0-bf55-9f893ba8c5d0] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[993a598d-a95a-3235-813e-59252c975070] :[850e9dff-7c45-3db4-8cf9-34c8fb7359fd]) ObjectSomeValuesFrom(:[75e0da0c-21ea-301f-a176-bf056788afe5] :[f5aadb4e-e760-3d6f-996d-b969b3db98e2]))) ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectSomeValuesFrom(:[75e0da0c-21ea-301f-a176-bf056788afe5] :[afc2bb42-ebc7-32cc-ba9e-d78ef99fa57f]))))";
        String owlAxiom4 = "SubClassOf(:[e1267848-0192-3d54-9e65-375558b46038] :[5fdcf819-f57f-3b9b-87d6-f48f483b70f2])";
        long timestamp1 = dateStringToEpochMillis("20190731");
        long timestamp2 = dateStringToEpochMillis("20190901");
        long timestamp3 = dateStringToEpochMillis("20210731");
        long timestamp4 = dateStringToEpochMillis("20210901");

        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("e1267848-0192-3d54-9e65-375558b46038"));
        DiTreeEntity expectedStatedDiTree = expectedOwlTransformationProvider.expectedAcid_fastBacilliInSputumStatedDiTree();

        Composer composer = new Composer("acid_fastBacilliInSputumOwlTransformation");
        Session session1 = composer.open(State.ACTIVE, timestamp1, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session1.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                .concept(testConcept)
                .attach((AxiomSyntax axiomSyntax) -> axiomSyntax
                        .semantic(axiomSemanticCore)
                        .text(owlAxiom1)));
        composer.commitSession(session1);

        Session session2 = composer.open(State.ACTIVE, timestamp2, AUTHOR_FOR_VERSION, moduleNIH, DEVELOPMENT_PATH);
        session2.compose(new AxiomSyntax().semantic(axiomSemanticNIH).text(owlAxiom2), testConcept);
        composer.commitSession(session2);

        Session session3 = composer.open(State.ACTIVE, timestamp3, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session3.compose(new AxiomSyntax().semantic(axiomSemanticCore).text(owlAxiom3), testConcept);
        composer.commitSession(session3);

        Session session4 = composer.open(State.INACTIVE, timestamp4, AUTHOR_FOR_VERSION, moduleNIH, DEVELOPMENT_PATH);
        session4.compose(new AxiomSyntax().semantic(axiomSemanticNIH).text(owlAxiom4), testConcept);
        composer.commitSession(session4);

        int[] actualOwlNids = EntityService.get().semanticNidsForComponentOfPattern(testConcept.nid(), OWL_AXIOM_SYNTAX_PATTERN.nid());
        assertEquals(2, actualOwlNids.length, "Did not find expected number of Owl Axioms");

        runOwltoLogicalTransformation();

        int[] actualStatedDefNids = EntityService.get().semanticNidsForComponentOfPattern(testConcept.nid(), EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid());
        assertEquals(1, actualStatedDefNids.length, "Did not find expected number of actual Stated Axioms");
        SemanticEntityVersion actualLatestStatedDef = (SemanticEntityVersion) stampCalc.latest(actualStatedDefNids[0]).get();

        DiTreeEntity actualStatedDiTree = latestStatedAxiomPatternVersion.getFieldWithMeaning(EL_PLUS_PLUS_STATED_TERMINOLOGICAL_AXIOMS, actualLatestStatedDef);

        assertEquals(State.ACTIVE, actualLatestStatedDef.state());
        assertEquals(timestamp3, actualLatestStatedDef.time());
        assertEquals(USER, actualLatestStatedDef.author());
        assertEquals(SOLOR_OVERLAY_MODULE, actualLatestStatedDef.module());
        assertEquals(DEVELOPMENT_PATH, actualLatestStatedDef.path());
        assertEquals(expectedStatedDiTree, actualStatedDiTree,
                "Owl to Logical Definition Transformation did not produce the expected Stated Definition DiTreeEntity");
    }

//    @Test
//    public void osteoarthritisOfFingerJointOfRightHandOwlTransformation() {
//        String owlAxiom = "TransitiveObjectProperty(:[b4c3f6f9-6937-30fd-8412-d0c77f8a7f73])";
//        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("b4c3f6f9-6937-30fd-8412-d0c77f8a7f73"));
//        EntityProxy.Semantic axiomSemantic = EntityProxy.Semantic.make(PublicIds.singleSemanticId(OWL_AXIOM_SYNTAX_PATTERN, testConcept.publicId()));
//        long activeMillisTimeStamp = dateStringToEpochMillis("20180731");
//        long inactiveMillisTimeStamp = dateStringToEpochMillis("20190131");
//
//        Composer composer = new Composer("partOfOwlTransformation");
//        Session activeSession = composer.open(State.ACTIVE, activeMillisTimeStamp, AUTHOR_FOR_VERSION, DEVELOPMENT_MODULE, DEVELOPMENT_PATH);
//        activeSession.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
//                .concept(testConcept)
//                .attach((AxiomSyntax axiomSyntax) -> axiomSyntax
//                        .semantic(axiomSemantic)
//                        .text(owlAxiom)));
//        composer.commitSession(activeSession);
//
//        Session inactiveSession = composer.open(State.INACTIVE, inactiveMillisTimeStamp, AUTHOR_FOR_VERSION, DEVELOPMENT_MODULE, DEVELOPMENT_PATH);
//        inactiveSession.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
//                .concept(testConcept)
//                .attach((AxiomSyntax axiomSyntax) -> axiomSyntax
//                        .semantic(axiomSemantic)
//                        .text(owlAxiom)));
//        composer.commitSession(inactiveSession);
//
//        runOwltoLogicalTransformation();
//
//        int[] actualStatedDefNids = EntityService.get().semanticNidsForComponentOfPattern(testConcept.nid(), EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid());
//        assertEquals(1, actualStatedDefNids.length, "Did not find expected number of actual Stated Axioms");
//        SemanticEntityVersion actualLatestStatedDef = (SemanticEntityVersion) stampCalc.latest(actualStatedDefNids[0]).get();
//
//        DiTreeEntity actualStatedDiTree = latestStatedAxiomPatternVersion.getFieldWithMeaning(EL_PLUS_PLUS_STATED_TERMINOLOGICAL_AXIOMS, actualLatestStatedDef);
//        DiTreeEntity expectedStatedDiTree = expectedPartOfDiTree();
//
//        assertEquals(State.ACTIVE, actualLatestStatedDef.state());
//        assertEquals(activeMillisTimeStamp, actualLatestStatedDef.time());
//        assertEquals(USER, actualLatestStatedDef.author());
//        assertEquals(SOLOR_OVERLAY_MODULE, actualLatestStatedDef.module());
//        assertEquals(DEVELOPMENT_PATH, actualLatestStatedDef.path());
//        assertEquals(expectedStatedDiTree, actualStatedDiTree,
//                "Owl to Logical Definition Transformation did not produce the expected Stated Definition DiTreeEntity");
//    }

}
