const RUNWAY_BASE_URL = "https://api.dev.runwayml.com/v1";
const RUNWAY_VERSION = "2024-11-06";

function getMissingCredentials() {
  const missing = [];
  if (!process.env.RUNWAY_API_KEY) missing.push("RUNWAY_API_KEY");
  return missing;
}

function mapRatio(aspectRatio) {
  switch (aspectRatio) {
    case "9:16":
      return "720:1280";
    case "1:1":
      return "960:960";
    default:
      return "1280:720";
  }
}

async function startGeneration(params) {
  const missing = getMissingCredentials();
  if (missing.length > 0) {
    const err = new Error(`Runway AI credentials missing: ${missing.join(", ")}`);
    err.code = "API_CONFIGURATION_MISSING";
    err.missingKeys = missing;
    throw err;
  }

  if (!params.imageBase64 && !params.imageUrl) {
    throw new Error("Runway Gen-3/Gen-4 image_to_video requires a reference image (select one or generate with AI Image Generator first).");
  }

  const promptImage =
    params.imageUrl ||
    `data:${params.imageMimeType || "image/jpeg"};base64,${params.imageBase64}`;

  const body = {
    model: "gen4_turbo",
    promptImage,
    promptText: `${params.prompt}. Style: ${params.visualStyle}. Camera: ${params.cameraMovement}.`,
    ratio: mapRatio(params.aspectRatio),
    duration: Number(params.durationSeconds) >= 10 ? 10 : 5,
  };

  const response = await fetch(`${RUNWAY_BASE_URL}/image_to_video`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${process.env.RUNWAY_API_KEY}`,
      "X-Runway-Version": RUNWAY_VERSION,
    },
    body: JSON.stringify(body),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || `Runway API HTTP ${response.status}`);
  }

  return { remoteJobId: data.id };
}

async function checkStatus(taskId) {
  const response = await fetch(`${RUNWAY_BASE_URL}/tasks/${encodeURIComponent(taskId)}`, {
    headers: {
      Authorization: `Bearer ${process.env.RUNWAY_API_KEY}`,
      "X-Runway-Version": RUNWAY_VERSION,
    },
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || `Runway status HTTP ${response.status}`);
  }

  if (data.status === "SUCCEEDED") {
    return {
      status: "completed",
      progress: 100,
      videoUrl: Array.isArray(data.output) ? data.output[0] : null,
    };
  }
  if (data.status === "FAILED" || data.status === "CANCELLED") {
    return {
      status: "failed",
      progress: 0,
      errorMessage: data.failure || "Runway task failed",
    };
  }
  return {
    status: "processing",
    progress: Math.round((data.progress || 0.45) * 100),
  };
}

module.exports = { getMissingCredentials, startGeneration, checkStatus };
