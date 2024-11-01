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
    private final EntityProxy.Concept moduleCore = EntityProxy.Concept.make(PublicIds.newRandom());
    private final EntityProxy.Concept moduleNIH = EntityProxy.Concept.make(PublicIds.newRandom());
    private StampCalculator stampCalc;
    private PatternEntityVersion latestStatedAxiomPatternVersion;

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

        EntityProxy.Semantic axiomSemanticCore = EntityProxy.Semantic.make(PublicIds.newRandom());
        EntityProxy.Semantic axiomSemanticNIH = EntityProxy.Semantic.make(PublicIds.newRandom());

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

    @Test
    public void acetazolamideOwlTransformation() {
        String owlAxiom1 = "EquivalentClasses(:[04190e13-bde3-306a-9b7c-30498c158902] ObjectIntersectionOf(:[ae364969-80c6-32b9-9c1d-eaebed617d9b] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectSomeValuesFrom(:[65bf3b7f-c854-36b5-81c3-4915461020a8] :[608be0ea-10fb-3815-a486-d163f1553760]))))";
        String owlAxiom2 = "SubClassOf(:[04190e13-bde3-306a-9b7c-30498c158902] ObjectIntersectionOf(:[ae364969-80c6-32b9-9c1d-eaebed617d9b] ObjectSomeValuesFrom(:[41bc8b8b-9232-39da-8189-67f8be7bfac0] :[a15b7cce-a8d3-3760-a747-6bdf45bd168b])))";
        String owlAxiom3 = "SubClassOf(:[04190e13-bde3-306a-9b7c-30498c158902] ObjectIntersectionOf(:[ae364969-80c6-32b9-9c1d-eaebed617d9b] ObjectSomeValuesFrom(:[41bc8b8b-9232-39da-8189-67f8be7bfac0] :[3da933a8-8fcf-3687-960a-5e9c74fa96f0])))";
        String owlAxiom4 = "SubClassOf(:[04190e13-bde3-306a-9b7c-30498c158902] ObjectIntersectionOf(:[ae364969-80c6-32b9-9c1d-eaebed617d9b] ObjectSomeValuesFrom(:[41bc8b8b-9232-39da-8189-67f8be7bfac0] :[5af2b0ac-66e4-3294-a89a-21b2e4599c0b])))";
        long timestamp1 = dateStringToEpochMillis("20190731");
        long timestamp2 = dateStringToEpochMillis("20210131");
        long timestamp3 = dateStringToEpochMillis("20211130");
        long timestamp4 = dateStringToEpochMillis("20220131");

        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("04190e13-bde3-306a-9b7c-30498c158902"));
        EntityProxy.Semantic axiomSemantic = EntityProxy.Semantic.make(PublicIds.singleSemanticId(OWL_AXIOM_SYNTAX_PATTERN, testConcept.publicId()));
        DiTreeEntity expectedStatedDiTree = expectedOwlTransformationProvider.expectedAcetazolamideStatedDiTree();

        Composer composer = new Composer("acetazolamideOwlTransformation");
        Session session1 = composer.open(State.ACTIVE, timestamp1, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session1.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                .concept(testConcept)
                .attach((AxiomSyntax axiomSyntax) -> axiomSyntax
                        .semantic(axiomSemantic)
                        .text(owlAxiom1)));
        composer.commitSession(session1);

        Session session2 = composer.open(State.ACTIVE, timestamp2, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session2.compose(new AxiomSyntax().semantic(axiomSemantic).text(owlAxiom2), testConcept);
        composer.commitSession(session2);

        Session session3 = composer.open(State.ACTIVE, timestamp3, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session3.compose(new AxiomSyntax().semantic(axiomSemantic).text(owlAxiom3), testConcept);
        composer.commitSession(session3);

        Session session4 = composer.open(State.ACTIVE, timestamp4, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session4.compose(new AxiomSyntax().semantic(axiomSemantic).text(owlAxiom4), testConcept);
        composer.commitSession(session4);

        int[] actualOwlNids = EntityService.get().semanticNidsForComponentOfPattern(testConcept.nid(), OWL_AXIOM_SYNTAX_PATTERN.nid());
        assertEquals(1, actualOwlNids.length, "Did not find expected number of Owl Axioms");

        runOwltoLogicalTransformation();

        int[] actualStatedDefNids = EntityService.get().semanticNidsForComponentOfPattern(testConcept.nid(), EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid());
        assertEquals(1, actualStatedDefNids.length, "Did not find expected number of actual Stated Axioms");
        SemanticEntityVersion actualLatestStatedDef = (SemanticEntityVersion) stampCalc.latest(actualStatedDefNids[0]).get();

        DiTreeEntity actualStatedDiTree = latestStatedAxiomPatternVersion.getFieldWithMeaning(EL_PLUS_PLUS_STATED_TERMINOLOGICAL_AXIOMS, actualLatestStatedDef);

        assertEquals(State.ACTIVE, actualLatestStatedDef.state());
        assertEquals(timestamp4, actualLatestStatedDef.time());
        assertEquals(USER, actualLatestStatedDef.author());
        assertEquals(SOLOR_OVERLAY_MODULE, actualLatestStatedDef.module());
        assertEquals(DEVELOPMENT_PATH, actualLatestStatedDef.path());
        assertEquals(expectedStatedDiTree, actualStatedDiTree,
                "Owl to Logical Definition Transformation did not produce the expected Stated Definition DiTreeEntity");
    }

    @Test
    public void aminoAcidModifiedDietOwlTransformation() {
        String owlAxiom1 = "SubClassOf(:[390b3a9c-8adb-3d12-99a2-5da70925bc1d] ObjectIntersectionOf(:[327e59bf-1639-3413-b013-2a971fc9ff30] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[45c2765a-583c-3f75-93ac-598ef630aefe])) ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectSomeValuesFrom(:[996261c3-3c12-3f09-8f14-e30e85e9e70d] :[f70fd917-7876-3c35-b3bc-7ff1b6add153]))))";
        String owlAxiom2 = "SubClassOf(:[390b3a9c-8adb-3d12-99a2-5da70925bc1d] ObjectIntersectionOf(:[eee97196-505d-352a-a307-9aa41c4ea5cd] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[d0f9e3b1-29e4-399f-b129-36693ba4acbc] :[87cc5116-8014-32a7-b324-9e28e8658a5e]) ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[45c2765a-583c-3f75-93ac-598ef630aefe])))))";
        String owlAxiom3 = "SubClassOf(:[390b3a9c-8adb-3d12-99a2-5da70925bc1d] ObjectIntersectionOf(:[eee97196-505d-352a-a307-9aa41c4ea5cd] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[d0f9e3b1-29e4-399f-b129-36693ba4acbc] :[87cc5116-8014-32a7-b324-9e28e8658a5e]) ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[45c2765a-583c-3f75-93ac-598ef630aefe]) ObjectSomeValuesFrom(:[b610d820-4486-3b5e-a2c1-9b66bc718c6d] :[ab748274-9007-3c3e-804d-ab4ebec55a83])))))";
        String owlAxiom4 = "SubClassOf(:[390b3a9c-8adb-3d12-99a2-5da70925bc1d] ObjectIntersectionOf(:[eee97196-505d-352a-a307-9aa41c4ea5cd] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[d0f9e3b1-29e4-399f-b129-36693ba4acbc] :[87cc5116-8014-32a7-b324-9e28e8658a5e]) ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[45c2765a-583c-3f75-93ac-598ef630aefe]))) ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectSomeValuesFrom(:[b610d820-4486-3b5e-a2c1-9b66bc718c6d] :[ab748274-9007-3c3e-804d-ab4ebec55a83]))))";
        long timestamp1 = dateStringToEpochMillis("20190731");
        long timestamp2 = dateStringToEpochMillis("20210131");
        long timestamp3 = dateStringToEpochMillis("20210731");
        long timestamp4 = dateStringToEpochMillis("20220228");

        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("390b3a9c-8adb-3d12-99a2-5da70925bc1d"));
        EntityProxy.Semantic axiomSemantic = EntityProxy.Semantic.make(PublicIds.singleSemanticId(OWL_AXIOM_SYNTAX_PATTERN, testConcept.publicId()));
        DiTreeEntity expectedStatedDiTree = expectedOwlTransformationProvider.expectedAminoAcidModifiedDietStatedDiTree();

        Composer composer = new Composer("aminoAcidModifiedDietOwlTransformation");
        Session session1 = composer.open(State.ACTIVE, timestamp1, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session1.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                .concept(testConcept)
                .attach((AxiomSyntax axiomSyntax) -> axiomSyntax
                        .semantic(axiomSemantic)
                        .text(owlAxiom1)));
        composer.commitSession(session1);

        Session session2 = composer.open(State.ACTIVE, timestamp2, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session2.compose(new AxiomSyntax().semantic(axiomSemantic).text(owlAxiom2), testConcept);
        composer.commitSession(session2);

        Session session3 = composer.open(State.ACTIVE, timestamp3, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session3.compose(new AxiomSyntax().semantic(axiomSemantic).text(owlAxiom3), testConcept);
        composer.commitSession(session3);

        Session session4 = composer.open(State.ACTIVE, timestamp4, AUTHOR_FOR_VERSION, moduleCore, DEVELOPMENT_PATH);
        session4.compose(new AxiomSyntax().semantic(axiomSemantic).text(owlAxiom4), testConcept);
        composer.commitSession(session4);

        int[] actualOwlNids = EntityService.get().semanticNidsForComponentOfPattern(testConcept.nid(), OWL_AXIOM_SYNTAX_PATTERN.nid());
        assertEquals(1, actualOwlNids.length, "Did not find expected number of Owl Axioms");

        runOwltoLogicalTransformation();

        int[] actualStatedDefNids = EntityService.get().semanticNidsForComponentOfPattern(testConcept.nid(), EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid());
        assertEquals(1, actualStatedDefNids.length, "Did not find expected number of actual Stated Axioms");
        SemanticEntityVersion actualLatestStatedDef = (SemanticEntityVersion) stampCalc.latest(actualStatedDefNids[0]).get();

        DiTreeEntity actualStatedDiTree = latestStatedAxiomPatternVersion.getFieldWithMeaning(EL_PLUS_PLUS_STATED_TERMINOLOGICAL_AXIOMS, actualLatestStatedDef);

        assertEquals(State.ACTIVE, actualLatestStatedDef.state());
        assertEquals(timestamp4, actualLatestStatedDef.time());
        assertEquals(USER, actualLatestStatedDef.author());
        assertEquals(SOLOR_OVERLAY_MODULE, actualLatestStatedDef.module());
        assertEquals(DEVELOPMENT_PATH, actualLatestStatedDef.path());
        assertEquals(expectedStatedDiTree, actualStatedDiTree,
                "Owl to Logical Definition Transformation did not produce the expected Stated Definition DiTreeEntity");
    }

    @Test
    public void copperSupplementOwlTransformation() {
        String owlAxiom1 = "SubClassOf(:[2c900d28-d7ce-35d3-bb94-9d60b2c351c1] :[bb4c9834-2519-38d9-9403-5a9a18b5da58])";
        String owlAxiom2 = "SubClassOf(:[2c900d28-d7ce-35d3-bb94-9d60b2c351c1] :[f315815b-e2b4-3a95-98b2-8be901b0b012])";
        String owlAxiom3 = "SubClassOf(:[2c900d28-d7ce-35d3-bb94-9d60b2c351c1] :[870cd418-7682-30d7-9ce6-b54325057162])";
        String owlAxiom4 = "SubClassOf(:[2c900d28-d7ce-35d3-bb94-9d60b2c351c1] :[f315815b-e2b4-3a95-98b2-8be901b0b012])";
        long timestamp1 = dateStringToEpochMillis("20190731");
        long timestamp2 = dateStringToEpochMillis("20190901");
        long timestamp3 = dateStringToEpochMillis("20200131");
        long timestamp4 = dateStringToEpochMillis("20210901");

        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("2c900d28-d7ce-35d3-bb94-9d60b2c351c1"));
        DiTreeEntity expectedStatedDiTree = expectedOwlTransformationProvider.expectedCopperSupplementStatedDiTree();

        EntityProxy.Semantic axiomSemanticCore = EntityProxy.Semantic.make(PublicIds.newRandom());
        EntityProxy.Semantic axiomSemanticNIH = EntityProxy.Semantic.make(PublicIds.newRandom());

        Composer composer = new Composer("copperSupplementOwlTransformation");
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

    @Test
    public void non_compliantWithPeritonealDialysisPrescriptionOwlTransformation() {
        String owlAxiom1 = "SubClassOf(:[0d788a52-fbe1-3454-92eb-cd55be2b2798] :[0079b56b-9a9b-3cc5-b74c-9c1d537851c7])";
        String owlAxiom2 = "SubClassOf(:[0d788a52-fbe1-3454-92eb-cd55be2b2798] :[163d6f94-d16f-3215-8560-661ef3108947])";
        String owlAxiom3 = "SubClassOf(:[0d788a52-fbe1-3454-92eb-cd55be2b2798] ObjectIntersectionOf(:[de9911aa-c71c-3252-be88-6ae964acafaa] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectSomeValuesFrom(:[75e0da0c-21ea-301f-a176-bf056788afe5] :[74acace1-8fc9-3b72-8ea4-3b06c97a2bed]))))";
        String owlAxiom4 = "SubClassOf(:[0d788a52-fbe1-3454-92eb-cd55be2b2798] :[163d6f94-d16f-3215-8560-661ef3108947])";
        long timestamp1 = dateStringToEpochMillis("20190731");
        long timestamp2 = dateStringToEpochMillis("20190901");
        long timestamp3 = dateStringToEpochMillis("20210731");
        long timestamp4 = dateStringToEpochMillis("20210901");

        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("0d788a52-fbe1-3454-92eb-cd55be2b2798"));
        DiTreeEntity expectedStatedDiTree = expectedOwlTransformationProvider.expectedNon_compliantWithPeritonealDialysisPrescriptionOwlTransformationStatedDiTree();

        EntityProxy.Semantic axiomSemanticCore = EntityProxy.Semantic.make(PublicIds.newRandom());
        EntityProxy.Semantic axiomSemanticNIH = EntityProxy.Semantic.make(PublicIds.newRandom());

        Composer composer = new Composer("non_compliantWithPeritonealDialysisPrescriptionOwlTransformation");
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

    @Test
    public void mineralModifiedDietOwlTransformation() {
        String owlAxiom1 = "SubClassOf(:[d589ab7f-79c7-3bf3-b0c4-fe816e8e03b2] ObjectIntersectionOf(:[327e59bf-1639-3413-b013-2a971fc9ff30] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[842e7340-dbdf-3245-9950-c944a34625f5]))))";
        String owlAxiom2 = "SubClassOf(:[d589ab7f-79c7-3bf3-b0c4-fe816e8e03b2] ObjectIntersectionOf(:[327e59bf-1639-3413-b013-2a971fc9ff30] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[00c3c2a5-c6b8-33c9-b692-a74728620680]) ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[842e7340-dbdf-3245-9950-c944a34625f5])))))";
        String owlAxiom3 = "SubClassOf(:[d589ab7f-79c7-3bf3-b0c4-fe816e8e03b2] ObjectIntersectionOf(:[327e59bf-1639-3413-b013-2a971fc9ff30] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[d0f9e3b1-29e4-399f-b129-36693ba4acbc] :[87cc5116-8014-32a7-b324-9e28e8658a5e]) ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[842e7340-dbdf-3245-9950-c944a34625f5])))))";
        String owlAxiom4 = "SubClassOf(:[d589ab7f-79c7-3bf3-b0c4-fe816e8e03b2] ObjectIntersectionOf(:[327e59bf-1639-3413-b013-2a971fc9ff30] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[00c3c2a5-c6b8-33c9-b692-a74728620680]) ObjectSomeValuesFrom(:[49ee3912-abb7-325c-88ba-a98824b4c47d] :[842e7340-dbdf-3245-9950-c944a34625f5])))))";
        long timestamp1 = dateStringToEpochMillis("20190731");
        long timestamp2 = dateStringToEpochMillis("20190901");
        long timestamp3 = dateStringToEpochMillis("20210131");
        long timestamp4 = dateStringToEpochMillis("20210301");

        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("d589ab7f-79c7-3bf3-b0c4-fe816e8e03b2"));
        DiTreeEntity expectedStatedDiTree = expectedOwlTransformationProvider.expectedMineralModifiedDietStatedDiTree();

        EntityProxy.Semantic axiomSemanticCore = EntityProxy.Semantic.make(PublicIds.newRandom());
        EntityProxy.Semantic axiomSemanticNIH = EntityProxy.Semantic.make(PublicIds.newRandom());

        Composer composer = new Composer("mineralModifiedDietOwlTransformation");
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

    @Test
    public void osteoarthritisOfFingerJointOfRightHandOwlTransformation() {
        String owlAxiom1 = "EquivalentClasses(:[5c967ffc-ec55-3ab1-9cb0-7a5fb4b6b1f2] ObjectIntersectionOf(:[ab4e618b-b954-3d56-a44b-f0f29d6f59d3] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[3161e31b-7d00-33d9-8cbd-9c33dc153aae] :[8bace47f-d473-3cba-80f9-c95e9dcc7a67]) ObjectSomeValuesFrom(:[3a6d919d-6c25-3aae-9bc3-983ead83a928] :[c73764b8-0299-396a-a978-219cb315b33d])))))";
        String owlAxiom2 = "EquivalentClasses(:[5c967ffc-ec55-3ab1-9cb0-7a5fb4b6b1f2] ObjectIntersectionOf(:[ab4e618b-b954-3d56-a44b-f0f29d6f59d3] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[3161e31b-7d00-33d9-8cbd-9c33dc153aae] :[8bace47f-d473-3cba-80f9-c95e9dcc7a67]) ObjectSomeValuesFrom(:[3a6d919d-6c25-3aae-9bc3-983ead83a928] :[86aa4f82-cb05-3ed1-b6a8-1f6ab0eaf7ee]) ObjectSomeValuesFrom(:[3a6d919d-6c25-3aae-9bc3-983ead83a928] :[c73764b8-0299-396a-a978-219cb315b33d]))) ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectSomeValuesFrom(:[3a6d919d-6c25-3aae-9bc3-983ead83a928] :[2af9e114-6a06-352d-9181-093fbfc57a5a]))))";
        String owlAxiom3 = "EquivalentClasses(:[5c967ffc-ec55-3ab1-9cb0-7a5fb4b6b1f2] ObjectIntersectionOf(:[ab4e618b-b954-3d56-a44b-f0f29d6f59d3] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[3161e31b-7d00-33d9-8cbd-9c33dc153aae] :[b430ad90-7aa5-349e-9435-1b64f3eecd53]) ObjectSomeValuesFrom(:[3a6d919d-6c25-3aae-9bc3-983ead83a928] :[c73764b8-0299-396a-a978-219cb315b33d])))))";
        String owlAxiom4 = "EquivalentClasses(:[5c967ffc-ec55-3ab1-9cb0-7a5fb4b6b1f2] ObjectIntersectionOf(:[ab4e618b-b954-3d56-a44b-f0f29d6f59d3] ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectIntersectionOf(ObjectSomeValuesFrom(:[3161e31b-7d00-33d9-8cbd-9c33dc153aae] :[b430ad90-7aa5-349e-9435-1b64f3eecd53]) ObjectSomeValuesFrom(:[3a6d919d-6c25-3aae-9bc3-983ead83a928] :[86aa4f82-cb05-3ed1-b6a8-1f6ab0eaf7ee]) ObjectSomeValuesFrom(:[3a6d919d-6c25-3aae-9bc3-983ead83a928] :[c73764b8-0299-396a-a978-219cb315b33d]))) ObjectSomeValuesFrom(:[051fbfed-3c40-3130-8c09-889cb7b7b5b6] ObjectSomeValuesFrom(:[3a6d919d-6c25-3aae-9bc3-983ead83a928] :[2af9e114-6a06-352d-9181-093fbfc57a5a]))))";
        long timestamp1 = dateStringToEpochMillis("20190731");
        long timestamp2 = dateStringToEpochMillis("20190901");
        long timestamp3 = dateStringToEpochMillis("20200131");
        long timestamp4 = dateStringToEpochMillis("20200301");

        EntityProxy.Concept testConcept = EntityProxy.Concept.make(PublicIds.of("5c967ffc-ec55-3ab1-9cb0-7a5fb4b6b1f2"));
        DiTreeEntity expectedStatedDiTree = expectedOwlTransformationProvider.expectedOsteoarthritisOfFingerJointOfRightHandStatedDiTree();

        EntityProxy.Semantic axiomSemanticCore = EntityProxy.Semantic.make(PublicIds.newRandom());
        EntityProxy.Semantic axiomSemanticNIH = EntityProxy.Semantic.make(PublicIds.newRandom());

        Composer composer = new Composer("mineralModifiedDietOwlTransformation");
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

}
