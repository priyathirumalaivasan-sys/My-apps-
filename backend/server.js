require("dotenv").config();
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const morgan = require("morgan");
const rateLimit = require("express-rate-limit");

const videoRoutes = require("./routes/videos");

const app = express();
const PORT = process.env.PORT || 3000;

app.use(helmet());
app.use(cors());
app.use(morgan("combined"));
app.use(express.json({ limit: "10mb" }));

const apiLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  standardHeaders: true,
  legacyHeaders: false,
});

app.use("/api", apiLimiter, videoRoutes);

app.get("/", (req, res) => {
  res.json({
    name: "Vasan AI Studio Backend",
    tagline: "Imagine. Create. Animate.",
    endpoints: ["/api/health", "/api/videos/generate", "/api/videos/status/:jobId", "/api/videos/download/:jobId"],
  });
});

app.listen(PORT, () => {
  console.log(`Vasan AI Studio backend listening on port ${PORT}`);
});
