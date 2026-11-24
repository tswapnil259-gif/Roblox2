import express from "express";
import rateLimit from "express-rate-limit";

const app = express();
const PORT = process.env.PORT || 3000;
const RELAY_TOKEN = process.env.RELAY_TOKEN || "";

app.use(express.json({ limit: "1mb" }));
app.use(
  rateLimit({
    windowMs: 60 * 1000,
    max: 60,
    standardHeaders: true,
    legacyHeaders: false,
  })
);

function requireRelayAuth(req, res, next) {
  if (!RELAY_TOKEN) return next();
  const auth = req.headers.authorization || "";
  if (auth !== `Bearer ${RELAY_TOKEN}`) {
    return res.status(401).json({ error: { message: "Invalid relay bearer token" } });
  }
  next();
}

function getProviderConfig(provider) {
  switch (provider) {
    case "OPENAI":
      return {
        type: "openai",
        url: "https://api.openai.com/v1/chat/completions",
        key: process.env.OPENAI_API_KEY || "",
      };
    case "DEEPSEEK":
      return {
        type: "openai",
        url: "https://api.deepseek.com/chat/completions",
        key: process.env.DEEPSEEK_API_KEY || "",
      };
    case "OPENROUTER":
      return {
        type: "openai",
        url: "https://openrouter.ai/api/v1/chat/completions",
        key: process.env.OPENROUTER_API_KEY || "",
      };
    case "ANTHROPIC":
      return {
        type: "anthropic",
        url: "https://api.anthropic.com/v1/messages",
        key: process.env.ANTHROPIC_API_KEY || "",
      };
    default:
      return null;
  }
}

app.get("/api/health", requireRelayAuth, (_req, res) => {
  res.json({
    ok: true,
    configuredProviders: {
      OPENAI: Boolean(process.env.OPENAI_API_KEY),
      ANTHROPIC: Boolean(process.env.ANTHROPIC_API_KEY),
      DEEPSEEK: Boolean(process.env.DEEPSEEK_API_KEY),
      OPENROUTER: Boolean(process.env.OPENROUTER_API_KEY),
    },
  });
});

app.post("/api/chat", requireRelayAuth, async (req, res) => {
  const { provider, model, systemPrompt = "", messages = [] } = req.body || {};
  const cfg = getProviderConfig(provider);
  if (!cfg) {
    return res.status(400).json({ error: { message: `Unknown provider: ${provider}` } });
  }
  if (!cfg.key) {
    return res.status(401).json({ error: { message: `${provider} API key not configured on relay` } });
  }

  try {
    if (cfg.type === "anthropic") {
      const normalized = [];
      for (const turn of messages) {
        const role = turn.role === "assistant" ? "assistant" : "user";
        if (normalized.length === 0 && role !== "user") continue;
        if (normalized.length > 0 && normalized[normalized.length - 1].role === role) {
          normalized[normalized.length - 1].content += `\n\n${turn.content}`;
        } else {
          normalized.push({ role, content: String(turn.content || "") });
        }
      }

      const upstream = await fetch(cfg.url, {
        method: "POST",
        headers: {
          "x-api-key": cfg.key,
          "anthropic-version": "2023-06-01",
          "content-type": "application/json",
        },
        body: JSON.stringify({
          model,
          max_tokens: 4096,
          ...(systemPrompt ? { system: systemPrompt } : {}),
          messages: normalized,
        }),
      });

      const data = await upstream.json();
      if (!upstream.ok) return res.status(upstream.status).json(data);

      const content = (data.content || [])
        .filter((b) => b.type === "text")
        .map((b) => b.text)
        .join("\n");
      return res.json({ content });
    }

    const openAiMessages = [
      ...(systemPrompt ? [{ role: "system", content: systemPrompt }] : []),
      ...messages.map((m) => ({
        role: m.role === "assistant" ? "assistant" : "user",
        content: String(m.content || ""),
      })),
    ];

    const headers = {
      Authorization: `Bearer ${cfg.key}`,
      "Content-Type": "application/json",
    };
    if (provider === "OPENROUTER") {
      headers["X-Title"] = "Roblox AI Studio Assistant";
    }

    const upstream = await fetch(cfg.url, {
      method: "POST",
      headers,
      body: JSON.stringify({ model, messages: openAiMessages }),
    });

    const data = await upstream.json();
    if (!upstream.ok) return res.status(upstream.status).json(data);

    const content = data?.choices?.[0]?.message?.content || "";
    return res.json({ content });
  } catch (err) {
    return res.status(502).json({ error: { message: err.message || "Relay upstream failure" } });
  }
});

app.listen(PORT, () => {
  console.log(`Roblox AI Studio relay listening on port ${PORT}`);
});
