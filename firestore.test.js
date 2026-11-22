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
const BOB_UID = "bob_viewer_2";
const OWNER_UID = "owner_admin_uid";
const OWNER_EMAIL = "ravindralodwal034@gmail.com";

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

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("apps").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("videos").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("app_config").doc("live").get());
  await assertFails(unauthDb.collection("moderation_reports").get());
  await assertFails(
    unauthDb.collection("apps").doc("app_1").set({
      creatorId: ALICE_UID,
      creatorName: "Alice Studio",
      title: "Unauth App",
      tagline: "Should fail",
      description: "Blocked by rules",
      category: "Tools",
      downloadUrl: "https://example.com/app",
      versionName: "1.0",
      visibility: "public",
      downloadCount: 0,
      likeCount: 0,
      accentIndex: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User profiles & bookmarks: strictly isolated to owning user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      studioName: "Alice Tube Studio",
      bio: "Building creative apps and animations",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      studioName: "Alice Studio Pro",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      studioName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("app_app_1").set({
      userId: ALICE_UID,
      targetId: "app_1",
      targetType: "app",
      createdAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );
});

test("StoreApp: creator create/update/delete, public discovery query, and bounded viewer counters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("apps").doc("app_1").set({
      creatorId: ALICE_UID,
      creatorName: "Alice Studio",
      title: "Pixel Sketch",
      tagline: "Fast vector drawing on Android",
      description: "Create vibrant illustrations and share them with friends.",
      category: "Tools",
      downloadUrl: "https://example.com/pixel-sketch",
      versionName: "1.2.0",
      visibility: "public",
      downloadCount: 0,
      likeCount: 0,
      accentIndex: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("apps").doc("app_bad_id").set({
      id: "app_bad_id",
      creatorId: ALICE_UID,
      creatorName: "Alice Studio",
      title: "Bad App",
      tagline: "Has id field",
      description: "Should fail schema validation",
      category: "Tools",
      downloadUrl: "https://example.com/bad",
      versionName: "1.0",
      visibility: "public",
      downloadCount: 0,
      likeCount: 0,
      accentIndex: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("apps").doc("app_bad_count").set({
      creatorId: ALICE_UID,
      creatorName: "Alice Studio",
      title: "Inflated App",
      tagline: "Starts with 999 downloads",
      description: "Should fail zero-counter requirement",
      category: "Tools",
      downloadUrl: "https://example.com/inflated",
      versionName: "1.0",
      visibility: "public",
      downloadCount: 999,
      likeCount: 0,
      accentIndex: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("apps").doc("app_future").set({
      creatorId: ALICE_UID,
      creatorName: "Alice Studio",
      title: "Future App",
      tagline: "Clock skew attack",
      description: "Should fail timestamp upper bound",
      category: "Tools",
      downloadUrl: "https://example.com/future",
      versionName: "1.0",
      visibility: "public",
      downloadCount: 0,
      likeCount: 0,
      accentIndex: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(
    bobDb.collection("apps").where("visibility", "==", "public").get()
  );

  await assertSucceeds(
    bobDb.collection("apps").doc("app_1").update({
      downloadCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("apps").doc("app_1").update({
      likeCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    bobDb.collection("apps").doc("app_1").update({
      downloadCount: 51,
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(
    bobDb.collection("apps").doc("app_1").update({
      title: "Hijacked Title",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(bobDb.collection("apps").doc("app_1").delete());

  await assertSucceeds(
    aliceDb.collection("apps").doc("app_private").set({
      creatorId: ALICE_UID,
      creatorName: "Alice Studio",
      title: "Secret Draft",
      tagline: "Private app draft",
      description: "Only Alice should read or query this draft",
      category: "Tools",
      downloadUrl: "https://example.com/draft",
      versionName: "0.1",
      visibility: "private",
      downloadCount: 0,
      likeCount: 0,
      accentIndex: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(bobDb.collection("apps").doc("app_private").get());
  await assertFails(bobDb.collection("apps").get());
  await assertSucceeds(aliceDb.collection("apps").where("creatorId", "==", ALICE_UID).get());
});

test("TubeVideo: creator publish, viewer +1 view/like increment, and private isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_1").set({
      creatorId: ALICE_UID,
      channelName: "Toon Galaxy",
      title: "Chotu Robot Adventure Ep 1",
      description: "Fun animated cartoon short for everyone to watch.",
      category: "Cartoons",
      language: "Marwadi / Rajasthani",
      videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
      durationLabel: "09:56",
      visibility: "public",
      viewCount: 0,
      likeCount: 0,
      accentIndex: 2,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    bobDb.collection("videos").where("visibility", "==", "public").get()
  );
  await assertSucceeds(
    bobDb.collection("videos").doc("vid_1").update({
      viewCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    bobDb.collection("videos").doc("vid_1").update({
      viewCount: 11,
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(bobDb.collection("videos").doc("vid_1").delete());

  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_1").update({
      title: "Chotu Robot Adventure HD",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertSucceeds(aliceDb.collection("videos").doc("vid_1").delete());
});

test("Owner Control Center: only verified admin can edit app_config/live & moderate reports/content", async () => {
  const ownerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true })
    .firestore();
  const unverifiedOwnerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: false })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true })
    .firestore();
  const now = new Date(Date.now() - 1000);

  // Regular user & unverified email CANNOT write app_config/live
  await assertFails(
    aliceDb.collection("app_config").doc("live").set({
      bannerText: "Hacked banner",
      bannerEnabled: true,
      maintenanceMode: false,
      maintenanceMessage: "",
      featuredLabel: "Featured",
      blockedIds: [],
      updatedBy: ALICE_UID,
      updatedAt: now,
    })
  );
  await assertFails(
    unverifiedOwnerDb.collection("app_config").doc("live").set({
      bannerText: "Unverified banner",
      bannerEnabled: true,
      maintenanceMode: false,
      maintenanceMessage: "",
      featuredLabel: "Featured",
      blockedIds: [],
      updatedBy: OWNER_UID,
      updatedAt: now,
    })
  );

  // Verified owner CAN write app_config/live; regular users CAN read app_config/live
  await assertSucceeds(
    ownerDb.collection("app_config").doc("live").set({
      bannerText: "Welcome to Tube Store Live!",
      bannerEnabled: true,
      maintenanceMode: false,
      maintenanceMessage: "Maintenance in progress",
      featuredLabel: "Top Picks Today",
      blockedIds: ["copied_app_99"],
      updatedBy: OWNER_UID,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("app_config").doc("live").get());
  await assertFails(bobDb.collection("app_config").get());

  // Regular user submits an anti-copy moderation report
  await assertSucceeds(
    bobDb.collection("moderation_reports").doc("rep_1").set({
      reporterId: BOB_UID,
      reporterName: "Bob",
      targetId: "copied_app_99",
      targetType: "app",
      targetTitle: "Copied Editor",
      reason: "Re-uploaded my original editor without permission",
      status: "open",
      resolutionNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can read own report, Alice CANNOT list/read Bob's report, Bob CANNOT self-resolve
  await assertSucceeds(bobDb.collection("moderation_reports").where("reporterId", "==", BOB_UID).get());
  await assertFails(aliceDb.collection("moderation_reports").doc("rep_1").get());
  await assertFails(aliceDb.collection("moderation_reports").get());
  await assertFails(
    bobDb.collection("moderation_reports").doc("rep_1").update({
      status: "resolved",
      resolutionNote: "Self resolved",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Verified owner CAN list all reports, resolve rep_1, and remove copied apps/videos
  await assertSucceeds(ownerDb.collection("moderation_reports").get());
  await assertSucceeds(
    ownerDb.collection("moderation_reports").doc("rep_1").update({
      status: "resolved",
      resolutionNote: "Blocked & removed copy",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  await assertSucceeds(
    aliceDb.collection("apps").doc("copied_app_99").set({
      creatorId: ALICE_UID,
      creatorName: "CopyCat",
      title: "Copied Editor",
      tagline: "Copied listing",
      description: "Copied listing to be removed by owner",
      category: "Tools",
      downloadUrl: "https://example.com/copy",
      versionName: "1.0.0",
      visibility: "public",
      downloadCount: 0,
      likeCount: 0,
      accentIndex: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(ownerDb.collection("apps").doc("copied_app_99").delete());
});
