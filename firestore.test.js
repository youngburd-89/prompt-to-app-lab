const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPricingPayload(userId, now) {
  return {
    userId,
    hourlyLaborRate: 55.0,
    packPackageFee: 180.0,
    movePackageFee: 220.0,
    cleanPackageFee: 160.0,
    fullBundleFee: 460.0,
    mileageRate: 2.25,
    cargoVanFee: 49.0,
    truck15FtFee: 89.0,
    truck20FtFee: 129.0,
    truck26FtFee: 169.0,
    suppliesBaseFee: 45.0,
    perBoxFee: 4.5,
    stairsFlightFee: 35.0,
    heavyItemFee: 65.0,
    highValueItemFee: 45.0,
    depositPercent: 30,
    fullValueRatePercent: 1.5,
    fullValueMinFee: 49.0,
    createdAt: now,
    updatedAt: now,
  };
}

function validBookingPayload(userId, now) {
  return {
    userId,
    customerName: "Alice Rivera",
    pickupAddress: "142 Oak St, Apt 3B",
    dropoffAddress: "880 Pine Ave",
    servicePackage: "FULL_BUNDLE",
    homeSize: "2BR",
    squareFootage: 1050,
    roomCount: 5,
    estimatedHours: 4.5,
    crewSize: 2,
    mileage: 18,
    truckSize: "TRUCK_15FT",
    stairsFlights: 2,
    heavyItemCount: 1,
    highValueItemCount: 2,
    declaredItemValue: 12000.0,
    suppliesIncluded: true,
    boxCount: 25,
    valuationTier: "FULL_VALUE",
    overallRiskLevel: "MODERATE",
    totalQuote: 1450.0,
    depositPercent: 30,
    depositAmount: 435.0,
    finalBalanceAmount: 1015.0,
    depositPaid: true,
    finalBalancePaid: false,
    contractAccepted: true,
    contractSignerName: "Alice Rivera",
    status: "CONFIRMED",
    truckReserved: true,
    suppliesPacked: false,
    cleanKitReady: false,
    preMoveWalkthroughDone: true,
    crewNotes: "Elevator requires padding",
    aiSummary: "Detected 2 high-value items and 1 heavy sofa; 15ft truck recommended.",
    completionSignOffName: "",
    createdAt: now,
    updatedAt: now,
  };
}

function validConditionItemPayload(userId, bookingId, now) {
  return {
    userId,
    bookingId,
    itemName: "Mid-Century Glass Dining Table",
    roomName: "Dining Room",
    category: "FRAGILE",
    estimatedValueUsd: 1800,
    riskLevel: "HIGH",
    handlingNote: "Detach legs and wrap glass top in double-wall cardboard",
    source: "AI_WALKTHROUGH",
    crewInspected: false,
    damageNoted: false,
    damageDescription: "",
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user cannot read or write bookings, settings, or condition items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookings").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bookings").doc("b1").set(validBookingPayload(ALICE_UID, now))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("settings").doc("pricing_config").set(validPricingPayload(ALICE_UID, now))
  );
});

test("Owner can create pricing config, booking, condition item, update crew checklist, and collect final 70% payment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("pricing_config").set(validPricingPayload(ALICE_UID, created))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("pricing_config").update({
      hourlyLaborRate: 62.5,
      depositPercent: 30,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  const bookingRef = aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_1");
  await assertSucceeds(bookingRef.set(validBookingPayload(ALICE_UID, created)));

  const condRef = bookingRef.collection("condition_items").doc("item_1");
  await assertSucceeds(condRef.set(validConditionItemPayload(ALICE_UID, "booking_1", created)));

  await assertSucceeds(
    condRef.update({
      crewInspected: true,
      damageNoted: true,
      damageDescription: "Minor scuff on left corner leg prior to load",
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  await assertSucceeds(
    bookingRef.update({
      status: "IN_PROGRESS",
      suppliesPacked: true,
      cleanKitReady: true,
      crewNotes: "Loaded living room and dining room",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    bookingRef.update({
      finalBalancePaid: true,
      status: "COMPLETED",
      completionSignOffName: "Alice Rivera",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: cannot update after COMPLETED
  await assertFails(
    bookingRef.update({
      status: "IN_PROGRESS",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Rejects @DocumentId injection, ghost fields, future timestamps, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Extra id field rejected
  const withId = { ...validBookingPayload(ALICE_UID, now), id: "booking_extra" };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_extra").set(withId));

  // Future timestamp rejected
  const future = new Date(Date.now() + 60000);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_future").set(validBookingPayload(ALICE_UID, future))
  );

  // Create valid booking for Alice, verify Bob cannot read or write it
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_alice").set(validBookingPayload(ALICE_UID, now))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_alice").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookings").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bookings").doc("booking_alice").update({
      status: "EN_ROUTE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
