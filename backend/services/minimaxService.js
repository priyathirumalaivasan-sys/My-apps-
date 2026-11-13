const MINIMAX_BASE_URL = "https://api.minimax.io/v1";

function getMissingCredentials() {
  const missing = [];
  if (!process.env.MINIMAX_API_KEY) missing.push("MINIMAX_API_KEY");
  return missing;
}

async function startGeneration(params) {
  const missing = getMissingCredentials();
  if (missing.length > 0) {
    const err = new Error(`MiniMax Hailuo AI credentials missing: ${missing.join(", ")}`);
    err.code = "API_CONFIGURATION_MISSING";
    err.missingKeys = missing;
    throw err;
  }

  const body = {
    model: params.imageBase64 ? "I2V-01-Director" : "T2V-01-Director",
    prompt: `${params.prompt}. Visual style: ${params.visualStyle}. Camera movement: ${params.cameraMovement}.`,
    prompt_optimizer: true,
  };

  if (params.imageBase64) {
    body.first_frame_image = `data:${params.imageMimeType || "image/jpeg"};base64,${params.imageBase64}`;
  }

  const response = await fetch(`${MINIMAX_BASE_URL}/video_generation`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${process.env.MINIMAX_API_KEY}`,
    },
    body: JSON.stringify(body),
  });

  const data = await response.json();
  if (!response.ok || (data.base_resp && data.base_resp.status_code !== 0)) {
    throw new Error(data.base_resp?.status_msg || `MiniMax API HTTP ${response.status}`);
  }

  return { remoteJobId: data.task_id };
}

async function checkStatus(remoteJobId) {
  const queryUrl = `${MINIMAX_BASE_URL}/query/video_generation?task_id=${encodeURIComponent(remoteJobId)}`;
  const response = await fetch(queryUrl, {
    headers: { Authorization: `Bearer ${process.env.MINIMAX_API_KEY}` },
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(`MiniMax status HTTP ${response.status}`);
  }

  if (data.status === "Success" && data.file_id) {
    const fileRes = await fetch(
      `${MINIMAX_BASE_URL}/files/retrieve?file_id=${encodeURIComponent(data.file_id)}`,
      { headers: { Authorization: `Bearer ${process.env.MINIMAX_API_KEY}` } }
    );
    const fileData = await fileRes.json();
    return {
      status: "completed",
      progress: 100,
      videoUrl: fileData.file?.download_url || null,
    };
  }

  if (data.status === "Fail") {
    return {
      status: "failed",
      progress: 0,
      errorMessage: data.base_resp?.status_msg || "MiniMax generation failed",
    };
  }

  return {
    status: "processing",
    progress: data.status === "Preparing" ? 25 : 65,
  };
}

module.exports = { getMissingCredentials, startGeneration, checkStatus };
