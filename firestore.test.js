const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, timestamp = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    localProjectId: 1,
    title: "DevDirector Promo Video",
    tagline: "Turn your repo into a launch video",
    repoUrl: "https://github.com/example/repo",
    brandName: "DevDirector",
    primaryColorHex: "#6366F1",
    secondaryColorHex: "#8B5CF6",
    accentColorHex: "#F59E0B",
    narrativeTemplate: "RAGE_HOOK",
    targetDurationSeconds: 30,
    voiceActor: "MATILDA",
    musicTrack: "INSPIRED_AMBIENT",
    transitionStyle: "METALLIC_SWOOSH",
    scenesJson: "[]",
    activeAspectRatio: "LANDSCAPE_16_9",
    createdAt: timestamp,
    updatedAt: timestamp,
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

// --- UNAUTHENTICATED DENIAL ---

test("Unauthenticated user: denied read/write on users and projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID))
  );
});

// --- USER PROFILE SECURITY & PARITY ---

test("User profile: owner create, partial update, and get succeed; list and cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Mirror FirebasePromoSync.upsertUserProfile create payload (no @DocumentId `id`)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@example.com",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserves immutable userId and createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Director",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // PII isolation: list users denied even for signed-in user; Bob cannot read Alice
  await assertFails(aliceDb.collection("users").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field & immutable field attacks rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

// --- CLOUD PROMO PROJECT PARITY, PARTIAL UPDATES & SUBCOLLECTION LIST ---

test("CloudPromoProject: Kotlin create, partial update, list, and delete succeed for owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const projectRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects")
    .doc("proj_1");

  // Empty subcollection list must succeed without undefined property errors
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );

  // Create mirrors FirebasePromoSync.backupProjectToCloud createPayload
  await assertSucceeds(projectRef.set(validProjectPayload(ALICE_UID, created)));

  // Get & list succeed for owner
  await assertSucceeds(projectRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );

  // Partial update mirrors FirebasePromoSync.backupProjectToCloud updatePayload
  await assertSucceeds(
    projectRef.update({
      localProjectId: 1,
      title: "Updated Launch Promo",
      tagline: "Ship faster with Gemini",
      repoUrl: "https://github.com/example/repo",
      brandName: "DevDirector",
      primaryColorHex: "#6366F1",
      secondaryColorHex: "#8B5CF6",
      accentColorHex: "#F59E0B",
      narrativeTemplate: "DEMO_FIRST",
      targetDurationSeconds: 60,
      voiceActor: "DANIEL",
      musicTrack: "INSPIRED_AMBIENT",
      transitionStyle: "METALLIC_SWOOSH",
      scenesJson: "[{\"title\":\"Hook\"}]",
      activeAspectRatio: "PORTRAIT_9_16",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner delete succeeds
  await assertSucceeds(projectRef.delete());
});

// --- CROSS-USER ISOLATION & ADVERSARIAL RED TEAM TESTS ---

test("Cross-user isolation: Bob cannot read, list, create, update, or delete Alice's projects", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceProjects = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects");

  await assertFails(aliceProjects.get());
  await assertFails(aliceProjects.doc("proj_1").get());
  await assertFails(
    aliceProjects.doc("proj_2").set(validProjectPayload(BOB_UID))
  );
  await assertFails(
    aliceProjects.doc("proj_1").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceProjects.doc("proj_1").delete());
});

test("Adversarial payloads: rejects @DocumentId 'id' field, spoofed userId, future timestamp, oversized string, and bad bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projects = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects");

  // 1. Extra @DocumentId 'id' field (shadow key) rejected
  await assertFails(
    projects.doc("proj_bad_id_field").set({
      ...validProjectPayload(ALICE_UID),
      id: "proj_bad_id_field",
    })
  );

  // 2. Spoofed userId rejected
  await assertFails(
    projects.doc("proj_spoof").set(validProjectPayload(BOB_UID))
  );

  // 3. Future timestamp (> request.time) rejected
  await assertFails(
    projects
      .doc("proj_future")
      .set(validProjectPayload(ALICE_UID, new Date(Date.now() + 60000)))
  );

  // 4. Out-of-range duration rejected
  await assertFails(
    projects.doc("proj_bad_dur").set({
      ...validProjectPayload(ALICE_UID),
      targetDurationSeconds: 9999,
    })
  );

  // 5. Oversized title (> 200 chars) rejected
  await assertFails(
    projects.doc("proj_big_title").set({
      ...validProjectPayload(ALICE_UID),
      title: "A".repeat(201),
    })
  );

  // 6. Invalid doc ID characters rejected
  await assertFails(
    projects.doc("bad id!@#").set(validProjectPayload(ALICE_UID))
  );

  // 7. Immutable createdAt mutation rejected on update
  const validRef = projects.doc("proj_immutable");
  await assertSucceeds(validRef.set(validProjectPayload(ALICE_UID)));
  await assertFails(
    validRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
