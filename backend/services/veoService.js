const GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

function getMissingCredentials() {
  const missing = [];
  if (!process.env.GOOGLE_VEO_API_KEY) missing.push("GOOGLE_VEO_API_KEY");
  return missing;
}

async function startGeneration(params) {
  const missing = getMissingCredentials();
  if (missing.length > 0) {
    const err = new Error(`Google Veo credentials missing: ${missing.join(", ")}`);
    err.code = "API_CONFIGURATION_MISSING";
    err.missingKeys = missing;
    throw err;
  }

  const instance = {
    prompt: `${params.prompt}. Visual style: ${params.visualStyle}. Camera movement: ${params.cameraMovement}.`,
  };

  if (params.imageBase64) {
    instance.image = {
      bytesBase64Encoded: params.imageBase64,
      mimeType: params.imageMimeType || "image/jpeg",
    };
  }

  const parameters = {
    aspectRatio: params.aspectRatio || "16:9",
    durationSeconds: Math.min(8, Math.max(5, Number(params.durationSeconds || 5))),
    negativePrompt: params.negativePrompt || undefined,
  };

  const response = await fetch(
    `${GEMINI_BASE_URL}/models/veo-2.0-generate-001:predictLongRunning`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": process.env.GOOGLE_VEO_API_KEY,
      },
      body: JSON.stringify({ instances: [instance], parameters }),
    }
  );

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error?.message || `Google Veo API HTTP ${response.status}`);
  }

  return { remoteJobId: data.name };
}

async function checkStatus(operationName) {
  const response = await fetch(`${GEMINI_BASE_URL}/${operationName}`, {
    headers: { "x-goog-api-key": process.env.GOOGLE_VEO_API_KEY },
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error?.message || `Google Veo status HTTP ${response.status}`);
  }

  if (data.error) {
    return {
      status: "failed",
      progress: 0,
      errorMessage: data.error.message || "Google Veo generation failed",
    };
  }

  if (data.done) {
    const uri =
      data.response?.generateVideoResponse?.generatedSamples?.[0]?.video?.uri ||
      data.response?.videos?.[0]?.uri ||
      null;
    const downloadProxyUrl = uri
      ? `${uri}${uri.includes("?") ? "&" : "?"}key=${encodeURIComponent(process.env.GOOGLE_VEO_API_KEY)}`
      : null;
    return {
      status: "completed",
      progress: 100,
      videoUrl: downloadProxyUrl,
    };
  }

  return { status: "processing", progress: 55 };
}

module.exports = { getMissingCredentials, startGeneration, checkStatus };
