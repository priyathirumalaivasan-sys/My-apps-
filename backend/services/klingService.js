const jwt = require("jsonwebtoken");

const KLING_BASE_URL = "https://api.klingai.com";

function getMissingCredentials() {
  const missing = [];
  if (!process.env.KLING_ACCESS_KEY) missing.push("KLING_ACCESS_KEY");
  if (!process.env.KLING_SECRET_KEY) missing.push("KLING_SECRET_KEY");
  return missing;
}

function createKlingJwt() {
  const now = Math.floor(Date.now() / 1000);
  return jwt.sign(
    {
      iss: process.env.KLING_ACCESS_KEY,
      exp: now + 1800,
      nbf: now - 5,
    },
    process.env.KLING_SECRET_KEY,
    { algorithm: "HS256", header: { alg: "HS256", typ: "JWT" } }
  );
}

async function startGeneration(params) {
  const missing = getMissingCredentials();
  if (missing.length > 0) {
    const err = new Error(`Kling AI credentials missing: ${missing.join(", ")}`);
    err.code = "API_CONFIGURATION_MISSING";
    err.missingKeys = missing;
    throw err;
  }

  const token = createKlingJwt();
  const isImageToVideo = Boolean(params.imageBase64);
  const endpoint = isImageToVideo
    ? `${KLING_BASE_URL}/v1/videos/image2video`
    : `${KLING_BASE_URL}/v1/videos/text2video`;

  const duration = [5, 10].includes(Number(params.durationSeconds))
    ? String(params.durationSeconds)
    : "5";

  const body = {
    model_name: "kling-v1-6",
    prompt: `${params.prompt} | Style: ${params.visualStyle} | Camera: ${params.cameraMovement}`,
    negative_prompt: params.negativePrompt || "",
    cfg_scale: Math.min(1, Math.max(0.1, Number(params.motionIntensity || 5) / 10)),
    mode: params.resolution === "1080p" ? "pro" : "std",
    aspect_ratio: params.aspectRatio || "16:9",
    duration,
  };

  if (isImageToVideo) {
    body.image = params.imageBase64;
  }

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  });

  const data = await response.json();
  if (!response.ok || data.code !== 0) {
    throw new Error(data.message || `Kling API HTTP ${response.status}`);
  }

  return {
    remoteJobId: data.data?.task_id,
    taskType: isImageToVideo ? "image2video" : "text2video",
  };
}

async function checkStatus(remoteJobId, taskType = "text2video") {
  const token = createKlingJwt();
  const endpoint = `${KLING_BASE_URL}/v1/videos/${taskType}/${remoteJobId}`;
  const response = await fetch(endpoint, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const data = await response.json();
  if (!response.ok || data.code !== 0) {
    throw new Error(data.message || `Kling status HTTP ${response.status}`);
  }

  const taskStatus = data.data?.task_status;
  if (taskStatus === "succeed") {
    const videoUrl = data.data?.task_result?.videos?.[0]?.url || null;
    return { status: "completed", progress: 100, videoUrl };
  }
  if (taskStatus === "failed") {
    return {
      status: "failed",
      progress: 0,
      errorMessage: data.data?.task_status_msg || "Kling generation failed",
    };
  }
  return {
    status: "processing",
    progress: taskStatus === "submitted" ? 20 : 60,
  };
}

module.exports = { getMissingCredentials, startGeneration, checkStatus };
