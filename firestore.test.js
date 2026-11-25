const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_moneyflow_uid";
const BOB_UID = "bob_moneyflow_uid";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validProfilePayload(uid, createdAt = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    email: "alice@example.com",
    displayName: "Alice",
    trackingStartCycle: "2026-11-24",
    timezone: "Asia/Kolkata",
    schemaVersion: 1,
    createdAt,
    updatedAt: createdAt,
  };
}

function validDayEntryPayload(uid, dateKey, createdAt = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    date: dateKey,
    dayType: "OFFICE_DAY",
    workStatus: "FULL",
    exitTime: "18:42",
    overtimeEnabled: true,
    note: "Release night",
    createdAt,
    updatedAt: createdAt,
  };
}

test("Unauthenticated users cannot read or write profiles or dayEntries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("dayEntries").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("dayEntries")
      .doc("2026-11-25")
      .set(validDayEntryPayload(ALICE_UID, "2026-11-25"))
  );
});

test("Owner can create, read, and update private profile; cross-user and list denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Listing all users is forbidden
  await assertFails(aliceDb.collection("users").get());

  // Valid trackingStartCycle update (24th)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      trackingStartCycle: "2026-10-24",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid trackingStartCycle (not the 24th) must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      trackingStartCycle: "2026-10-01",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow ghost field injection must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      trackingStartCycle: "2026-09-24",
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read & update denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      trackingStartCycle: "2026-08-24",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can list empty dayEntries subcollection and CRUD valid per-date overrides", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const entriesRef = aliceDb.collection("users").doc(ALICE_UID).collection("dayEntries");

  // Empty subcollection list succeeds for owner, fails for Bob
  await assertSucceeds(entriesRef.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("dayEntries").get()
  );

  // Create office day with exit time and OT
  await assertSucceeds(
    entriesRef.doc("2026-11-25").set(validDayEntryPayload(ALICE_UID, "2026-11-25"))
  );

  // Partial update to Sunday full worked with OT at 19:00
  await assertSucceeds(
    entriesRef.doc("2026-11-25").update({
      dayType: "SUNDAY",
      workStatus: "FULL",
      exitTime: "19:00",
      overtimeEnabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Create holiday without optional exitTime/note (omitted optional keys)
  await assertSucceeds(
    entriesRef.doc("2026-11-26").set({
      userId: ALICE_UID,
      date: "2026-11-26",
      dayType: "HOLIDAY",
      workStatus: "PAID_OFF",
      overtimeEnabled: false,
      createdAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Owner can delete (reset to default)
  await assertSucceeds(entriesRef.doc("2026-11-26").delete());

  // Cross-user read/write/delete denied
  const bobTarget = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("dayEntries")
    .doc("2026-11-25");
  await assertFails(bobTarget.get());
  await assertFails(
    bobTarget.update({
      workStatus: "LEAVE",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobTarget.delete());
});

test("Security rules reject malformed payloads, invalid workStatus/dayType combos, future timestamps, and @DocumentId leaks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const entriesRef = aliceDb.collection("users").doc(ALICE_UID).collection("dayEntries");

  // Date field mismatch with document ID
  await assertFails(
    entriesRef.doc("2026-11-25").set(validDayEntryPayload(ALICE_UID, "2026-11-26"))
  );

  // Invalid workStatus for OFFICE_DAY (PAID_OFF is only for SUNDAY/HOLIDAY)
  await assertFails(
    entriesRef.doc("2026-11-25").set({
      ...validDayEntryPayload(ALICE_UID, "2026-11-25"),
      dayType: "OFFICE_DAY",
      workStatus: "PAID_OFF",
    })
  );

  // Invalid workStatus for SUNDAY (LEAVE is only for OFFICE_DAY)
  await assertFails(
    entriesRef.doc("2026-11-29").set({
      ...validDayEntryPayload(ALICE_UID, "2026-11-29"),
      dayType: "SUNDAY",
      workStatus: "LEAVE",
    })
  );

  // Invalid exitTime format (25:00)
  await assertFails(
    entriesRef.doc("2026-11-25").set({
      ...validDayEntryPayload(ALICE_UID, "2026-11-25"),
      exitTime: "25:00",
    })
  );

  // Oversized note (> 200 chars)
  await assertFails(
    entriesRef.doc("2026-11-25").set({
      ...validDayEntryPayload(ALICE_UID, "2026-11-25"),
      note: "x".repeat(201),
    })
  );

  // Tampered future timestamp rejected
  await assertFails(
    entriesRef.doc("2026-11-25").set(
      validDayEntryPayload(ALICE_UID, "2026-11-25", new Date(Date.now() + 60000))
    )
  );

  // Including @DocumentId key (`dateKey` or `id`) rejected by hasOnly
  await assertFails(
    entriesRef.doc("2026-11-25").set({
      ...validDayEntryPayload(ALICE_UID, "2026-11-25"),
      id: "2026-11-25",
    })
  );

  // Identity spoofing (Alice trying to write under Bob's path or with Bob's userId)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("dayEntries")
      .doc("2026-11-25")
      .set(validDayEntryPayload(ALICE_UID, "2026-11-25"))
  );
  await assertFails(
    entriesRef.doc("2026-11-25").set(validDayEntryPayload(BOB_UID, "2026-11-25"))
  );
});
