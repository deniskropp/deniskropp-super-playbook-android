const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_ocs_001";
const BOB_UID = "bob_ocs_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validRunPayload(uid, ts) {
  return {
    userId: uid,
    title: "TAS-FORECAST Evaluation",
    objective: "Evaluate TAS-FORECAST as Predictive mode alias across 5 domains",
    cmdMode: "Predictive",
    projectedRunFlagMode: "Swarm",
    cmdLang: "EN",
    currentStep: 0,
    stepsDone: 0,
    status: "IDLE",
    invokedAtomicAgents: [],
    logicTypesUsed: [],
    tasPrimitivesUsed: [],
    schemaDriftTypes: [],
    selectedWePlanOptions: [],
    structuralStatus: "pending_measurement",
    structuralMethod: "asserted",
    sonicStatus: "open",
    consentTokenPresent: false,
    errorActive: false,
    atomicSignOffCount: 0,
    signedOffAgents: [],
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write runs, decisions, or sealed siblings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("runs").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("runs")
      .doc("run_1")
      .set(validRunPayload(ALICE_UID, now))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sealedSiblings").get()
  );
});

test("Authenticated owner can create, list, update runs and child steps; terminal SEALED locks updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);
  const runRef = aliceDb.collection("users").doc(ALICE_UID).collection("runs").doc("run_alpha");

  await assertSucceeds(runRef.set(validRunPayload(ALICE_UID, created)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("runs").get());

  // Child step creation (parent run exists)
  const stepRef = runRef.collection("steps").doc("step_1");
  await assertSucceeds(
    stepRef.set({
      userId: ALICE_UID,
      runId: "run_alpha",
      stepNumber: 1,
      phase: 1,
      agentName: "GPTASe",
      primitive: "TAS-EXTRACT",
      displayExecutable: "TAS-EXTRACT",
      displayInput: "⫻data/obj",
      displayExpectedOutput: "⫻data/tas_raw",
      kickLangOutput: "⫻data/tas_raw:\n- unit_1: decompose\n- unit_2: validate\n- unit_3: synthesize",
      summary: "Extracted 3 atomic TAS units.",
      logicBranch: "none",
      ruleViolations: [],
      iterations: 1,
      status: "COMPLETED",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update on run
  await assertSucceeds(
    runRef.update({
      currentStep: 1,
      stepsDone: 1,
      status: "RUNNING",
      invokedAtomicAgents: ["GPTASe"],
      tasPrimitivesUsed: ["TAS-EXTRACT"],
      updatedAt: updated,
    })
  );

  // Reject ghost field (Shadow Update Test)
  await assertFails(
    runRef.update({
      ghostPrivilege: true,
      updatedAt: updated,
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    runRef.update({
      createdAt: updated,
      updatedAt: updated,
    })
  );

  // Seal run, then verify terminal state locks further updates
  await assertSucceeds(
    runRef.update({
      status: "SEALED",
      sealedSiblingId: "seal_001",
      updatedAt: updated,
    })
  );
  await assertFails(
    runRef.update({
      status: "RUNNING",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("runs")
      .doc("run_private")
      .set(validRunPayload(ALICE_UID, ts))
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("runs").doc("run_private").get()
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("runs").get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("runs")
      .doc("run_private")
      .update({ title: "Hijacked", updatedAt: new Date(Date.now() - 1000) })
  );
});

test("MoleculeRun, SonicRun, and DecisionRecord enforce schema bounds and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("molecules")
      .doc("mol_1")
      .set({
        userId: ALICE_UID,
        molecule: "CognitivePulse",
        formula: "Prepare + Combine -> Validate -> Seal",
        model: "KickLang v4.1 Molecule",
        space: "Kick/Space",
        scope: "Cross-domain synthesis",
        reference: "Flow-Playbook v1.1",
        sourceCmdMode: "Predictive",
        projectedRunFlagMode: "Swarm",
        currentStage: "Prepare",
        stageOutputs: [],
        sonicDelegated: false,
        status: "DRAFT",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Invalid projectedRunFlagMode (Predictive not in 4-value subset) must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("molecules")
      .doc("mol_bad")
      .set({
        userId: ALICE_UID,
        molecule: "Bad",
        formula: "X",
        model: "KickLang v4.1 Molecule",
        space: "Kick/Space",
        scope: "Test",
        reference: "Ref",
        sourceCmdMode: "Predictive",
        projectedRunFlagMode: "Predictive",
        currentStage: "Prepare",
        stageOutputs: [],
        sonicDelegated: false,
        status: "DRAFT",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sonicRuns")
      .doc("sonic_1")
      .set({
        userId: ALICE_UID,
        title: "Full Bloom Pulse",
        creativeBrief: "Warm analog arpeggio transitioning into full bloom",
        energy: 4,
        adaptiveTempo: true,
        contrastPath: "PATH_D_FULL_BLOOM",
        microTiming: "humanized +6ms swing",
        spatialMapping: "wide obsidian hall",
        consentRequired: true,
        consentTokenPresent: false,
        kickGuardDimaApproved: false,
        kickGuardFizzApproved: false,
        kickGuardMonitorApproved: false,
        promptEnvelope: "",
        sonicStatus: "open",
        status: "HALTED_CONSENT",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("decisions")
      .doc("SP_B")
      .set({
        userId: ALICE_UID,
        code: "SP-B",
        category: "CONFLICT",
        status: "CONFIRMED",
        selectedResolution: "Keep TAS-FORECAST as Predictive-mode alias",
        rationale: "Preserves 9 canonical TAS primitives until Denis seal.",
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("RULE-11: SealedSibling allows owner create/read but strictly forbids update and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 3000);
  const sealRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sealedSiblings")
    .doc("seal_v1_0_1");

  await assertSucceeds(
    sealRef.set({
      userId: ALICE_UID,
      versionLabel: "OCS Super Playbook v1.0.1-sibling",
      parentVersionRef: "OCS KickLang Super Playbook v1.0.0",
      protocolVersion: "2.2",
      languageModel: "KickLang v4.1 Molecule",
      sourceKind: "META_PLAYBOOK",
      sourceId: "run_alpha",
      structuralCoherence: 0.96,
      sonicStatus: "open",
      transcript: "⫻display/header\nExecutable: Step 20 Seal\n⫻end/flow",
      createdAt: ts,
    })
  );

  await assertSucceeds(sealRef.get());
  await assertFails(sealRef.update({ versionLabel: "Mutated Parent" }));
  await assertFails(sealRef.delete());

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sealedSiblings")
      .doc("seal_future")
      .set({
        userId: ALICE_UID,
        versionLabel: "Future Seal",
        parentVersionRef: "v1.0.0",
        protocolVersion: "2.2",
        languageModel: "KickLang v4.1 Molecule",
        sourceKind: "META_PLAYBOOK",
        sourceId: "run_alpha",
        structuralCoherence: 0.9,
        sonicStatus: "open",
        transcript: "⫻end/flow",
        createdAt: new Date(Date.now() + 60000),
      })
  );
});
