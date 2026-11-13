const admin = require("firebase-admin");

let firebaseInitialized = false;

function initFirebaseAdmin() {
  if (firebaseInitialized) return;
  if (admin.apps.length === 0) {
    const projectId = process.env.FIREBASE_PROJECT_ID;
    admin.initializeApp(projectId ? { projectId } : undefined);
  }
  firebaseInitialized = true;
}

async function requireFirebaseAuth(req, res, next) {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    return res.status(401).json({
      code: "UNAUTHORIZED",
      message: "Missing Firebase ID token in Authorization header.",
    });
  }

  const idToken = authHeader.slice(7).trim();
  if (!idToken) {
    return res.status(401).json({
      code: "UNAUTHORIZED",
      message: "Empty Bearer token.",
    });
  }

  try {
    initFirebaseAdmin();
    const decoded = await admin.auth().verifyIdToken(idToken);
    req.user = decoded;
    return next();
  } catch (err) {
    return res.status(401).json({
      code: "INVALID_TOKEN",
      message: `Firebase ID token verification failed: ${err.message}`,
    });
  }
}

module.exports = { requireFirebaseAuth };
