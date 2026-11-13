const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_1";
const BOB_UID = "bob_creator_2";
const ADMIN_EMAIL = "priyathirumalaivasan@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

function validUserProfile(uid, overrides = {}) {
  return {
    userId: uid,
    displayName: "Alice Creator",
    email: "alice@example.com",
    credits: 100,
    plan: "starter",
    role: "user",
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
    ...overrides,
  };
}

function validProject(uid, overrides = {}) {
  return {
    userId: uid,
    title: "Neon Chennai Short",
    description: "Cyberpunk animated short film",
    characterSeed: "Young Tamil filmmaker wearing gold-rimmed visor",
    defaultStyle: "Cinematic",
    defaultAspectRatio: "16:9",
    videoCount: 0,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
    ...overrides,
  };
}

function validVideoJob(uid, overrides = {}) {
  return {
    userId: uid,
    modelProvider: "Google Veo",
    generationMode: "Text to Video",
    prompt: "Cinematic aerial drone shot over Marina Beach at golden hour",
    negativePrompt: "blurry, watermark, distorted",
    language: "English",
    durationSeconds: 5,
    aspectRatio: "16:9",
    resolution: "1080p",
    fps: 24,
    cameraMovement: "Aerial Orbit",
    motionIntensity: 6,
    visualStyle: "Cinematic",
    audioTrack: "Cinematic Score",
    status: "queued",
    progress: 0,
    creditsCost: 15,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
    ...overrides,
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

test("Unauthenticated user cannot read or write users or projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("videoJobs").get());
});

test("User can create own starter profile with 100 credits and role=user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID))
  );
});

test("User cannot self-assign admin role or inflate credits on create", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, { role: "admin" }))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, { credits: 9999 }))
  );
});

test("User cannot read or write another user's profile, projects, or videoJobs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProject(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("videoJobs").doc("job_1").set(validVideoJob(ALICE_UID)));
});

test("User cannot grant themselves positive credits via transactions or profile update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ credits: 500, updatedAt: pastTimestamp() })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("creditTransactions").doc("tx_1").set({
      userId: ALICE_UID,
      amount: 500,
      reason: "Fake topup",
      createdAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("creditTransactions").doc("tx_2").set({
      userId: ALICE_UID,
      amount: -15,
      reason: "Veo video generation",
      createdAt: pastTimestamp(),
    })
  );
});

test("VideoJob terminal state locking prevents modifying completed job", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const jobRef = aliceDb.collection("users").doc(ALICE_UID).collection("videoJobs").doc("job_1");
  await assertSucceeds(jobRef.set(validVideoJob(ALICE_UID)));
  await assertSucceeds(
    jobRef.update({
      status: "completed",
      progress: 100,
      videoUrl: "https://example.com/video.mp4",
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    jobRef.update({
      status: "queued",
      progress: 0,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Spoofed unverified admin email is rejected; verified admin is allowed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));

  const spoofAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofAdminDb.collection("users").get());

  const realAdminDb = testEnv.authenticatedContext("admin_1", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(realAdminDb.collection("users").get());
  await assertSucceeds(
    realAdminDb.collection("users").doc(ALICE_UID).update({
      credits: 250,
      plan: "pro",
      updatedAt: pastTimestamp(),
    })
  );
});
