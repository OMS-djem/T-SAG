const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_tech";
const BOB_UID = "bob_tech";
const ADMIN_UID = "admin_super";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read users, mail_config, or reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("mail_config").get());
  await assertFails(unauthDb.collection("site_reports").get());
});

test("Authenticated user: can create and read own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@mobiserve.com" }).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice Technician",
      email: "alice@mobiserve.com",
      phone: "+213550123456",
      role: "TECHNICIAN",
      isActive: true,
    })
  );

  const profile = await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot elevate own role to ADMIN", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      name: "Bob Technician",
      email: "bob@mobiserve.com",
      phone: "+213550654321",
      role: "TECHNICIAN",
      isActive: true,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@mobiserve.com" }).firestore();
  // Attempt to elevate to ADMIN should fail
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).update({
      role: "ADMIN"
    })
  );
});

test("Admin: can manage mail_config and update user roles", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      name: "Super Admin",
      email: "admin@mobiserve.com",
      phone: "+213550000000",
      role: "ADMIN",
      isActive: true,
    });
    await context.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      name: "Bob Technician",
      email: "bob@mobiserve.com",
      phone: "+213550654321",
      role: "TECHNICIAN",
      isActive: true,
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "admin@mobiserve.com" }).firestore();

  // Admin can update bob's role to OM
  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      role: "OM"
    })
  );

  // Admin can update mail_config
  await assertSucceeds(
    adminDb.collection("mail_config").doc("settings").set({
      senderEmail: "admin@mobiserve.com",
      senderName: "Mobiserve NOC",
      toGroup: "noc@mobiserve.com,om@mobiserve.com",
      ccGroup: "supervision@mobiserve.com",
    })
  );
});

test("Authenticated user: can create site report with own UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@mobiserve.com" }).firestore();
  await assertSucceeds(
    aliceDb.collection("site_reports").doc("report_123").set({
      siteId: "ALG001",
      siteName: "Algiers Port Tower",
      status: "ACCORDED",
      technicianName: "Alice",
      technicianPhone: "+213550123456",
      technicianUid: ALICE_UID,
      latitude: 36.752887,
      longitude: 3.042048,
    })
  );
});
