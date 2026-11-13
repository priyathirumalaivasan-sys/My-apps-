const express = require("express");
const multer = require("multer");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const { Readable } = require("stream");

const klingService = require("../services/klingService");
const minimaxService = require("../services/minimaxService");
const veoService = require("../services/veoService");
const runwayService = require("../services/runwayService");
const { requireFirebaseAuth } = require("../middleware/auth");

const router = express.Router();

const uploadDir = path.join(__dirname, "..", "uploads");
fs.mkdirSync(uploadDir, { recursive: true });

const upload = multer({
  dest: uploadDir,
  limits: { fileSize: 25 * 1024 * 1024 }, // 25 MB limit
});

// In-memory job store (can be backed by Firestore / Redis in multi-instance production)
const jobs = new Map();

function getProviderService(modelProvider) {
  switch (modelProvider) {
    case "Kling AI":
      return klingService;
    case "MiniMax Hailuo AI":
      return minimaxService;
    case "Google Veo":
      return veoService;
    case "Runway AI":
      return runwayService;
    default:
      return null;
  }
}

router.get("/health", (req, res) => {
  const missingEnvByProvider = {
    "Kling AI": klingService.getMissingCredentials(),
    "MiniMax Hailuo AI": minimaxService.getMissingCredentials(),
    "Google Veo": veoService.getMissingCredentials(),
    "Runway AI": runwayService.getMissingCredentials(),
  };

  const providers = Object.fromEntries(
    Object.entries(missingEnvByProvider).map(([k, v]) => [k, v.length === 0])
  );

  res.json({
    ok: true,
    service: "vasan-ai-studio-backend",
    providers,
    missingEnvByProvider,
  });
});

router.post(
  "/videos/generate",
  requireFirebaseAuth,
  upload.single("referenceMedia"),
  async (req, res) => {
    const file = req.file;
    try {
      const {
        modelProvider,
        generationMode = "Text to Video",
        prompt,
        negativePrompt = "",
        durationSeconds = 5,
        aspectRatio = "16:9",
        resolution = "1080p",
        fps = 24,
        cameraMovement = "Static",
        motionIntensity = 5,
        visualStyle = "Cinematic",
      } = req.body;

      if (!prompt || !modelProvider) {
        return res.status(400).json({
          code: "INVALID_REQUEST",
          message: "modelProvider and prompt are required.",
        });
      }

      const service = getProviderService(modelProvider);
      if (!service) {
        return res.status(400).json({
          code: "UNSUPPORTED_PROVIDER",
          message: `Unsupported modelProvider: ${modelProvider}`,
        });
      }

      const missingKeys = service.getMissingCredentials();
      if (missingKeys.length > 0) {
        return res.status(503).json({
          code: "API_CONFIGURATION_MISSING",
          message: `${modelProvider} API credentials are not configured on the Express backend.`,
          missingKeys,
        });
      }

      let imageBase64 = null;
      let imageMimeType = null;
      if (file) {
        const buffer = fs.readFileSync(file.path);
        imageBase64 = buffer.toString("base64");
        imageMimeType = file.mimetype || "image/jpeg";
      }

      const started = await service.startGeneration({
        generationMode,
        prompt,
        negativePrompt,
        durationSeconds: Number(durationSeconds),
        aspectRatio,
        resolution,
        fps: Number(fps),
        cameraMovement,
        motionIntensity: Number(motionIntensity),
        visualStyle,
        imageBase64,
        imageMimeType,
      });

      const jobId = crypto.randomUUID();
      const jobRecord = {
        jobId,
        uid: req.user.uid,
        modelProvider,
        remoteJobId: started.remoteJobId,
        taskType: started.taskType,
        status: "processing",
        progress: 15,
        videoUrl: null,
        errorMessage: null,
        createdAt: Date.now(),
      };
      jobs.set(jobId, jobRecord);

      return res.status(202).json({
        jobId,
        remoteJobId: started.remoteJobId,
        status: "processing",
        progress: 15,
      });
    } catch (err) {
      if (err.code === "API_CONFIGURATION_MISSING") {
        return res.status(503).json({
          code: err.code,
          message: err.message,
          missingKeys: err.missingKeys || [],
        });
      }
      return res.status(500).json({
        code: "GENERATION_FAILED",
        message: err.message || "Video generation request failed.",
      });
    } finally {
      if (file && fs.existsSync(file.path)) {
        fs.unlink(file.path, () => {});
      }
    }
  }
);

router.get("/videos/status/:jobId", requireFirebaseAuth, async (req, res) => {
  const job = jobs.get(req.params.jobId);
  if (!job) {
    return res.status(404).json({
      code: "JOB_NOT_FOUND",
      message: "Video generation job not found.",
    });
  }
  if (job.uid !== req.user.uid) {
    return res.status(403).json({
      code: "FORBIDDEN",
      message: "You do not own this video generation job.",
    });
  }

  if (job.status === "completed" || job.status === "failed") {
    return res.json({
      jobId: job.jobId,
      status: job.status,
      progress: job.progress,
      videoUrl: job.videoUrl,
      downloadUrl: job.videoUrl ? `/api/videos/download/${job.jobId}` : null,
      errorMessage: job.errorMessage,
    });
  }

  try {
    const service = getProviderService(job.modelProvider);
    const result = await service.checkStatus(job.remoteJobId, job.taskType);
    job.status = result.status;
    job.progress = result.progress;
    job.videoUrl = result.videoUrl || job.videoUrl;
    job.errorMessage = result.errorMessage || null;
    jobs.set(job.jobId, job);

    const baseUrl = process.env.PUBLIC_BASE_URL || `${req.protocol}://${req.get("host")}`;
    return res.json({
      jobId: job.jobId,
      status: job.status,
      progress: job.progress,
      videoUrl: job.videoUrl,
      downloadUrl: job.videoUrl ? `${baseUrl}/api/videos/download/${job.jobId}` : null,
      errorMessage: job.errorMessage,
    });
  } catch (err) {
    return res.status(500).json({
      code: "STATUS_CHECK_FAILED",
      message: err.message,
    });
  }
});

router.get("/videos/download/:jobId", requireFirebaseAuth, async (req, res) => {
  const job = jobs.get(req.params.jobId);
  if (!job || job.uid !== req.user.uid) {
    return res.status(404).json({ code: "NOT_FOUND", message: "Video not found." });
  }
  if (!job.videoUrl) {
    return res.status(409).json({ code: "NOT_READY", message: "Video is not ready for download." });
  }

  try {
    const upstream = await fetch(job.videoUrl);
    if (!upstream.ok || !upstream.body) {
      return res.status(502).json({ code: "UPSTREAM_ERROR", message: "Failed to fetch generated MP4." });
    }
    res.setHeader("Content-Type", "video/mp4");
    res.setHeader("Content-Disposition", `attachment; filename="vasan-studio-${job.jobId}.mp4"`);
    Readable.fromWeb(upstream.body).pipe(res);
  } catch (err) {
    res.status(500).json({ code: "DOWNLOAD_FAILED", message: err.message });
  }
});

module.exports = router;
