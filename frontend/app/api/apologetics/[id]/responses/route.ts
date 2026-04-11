import { NextRequest, NextResponse } from "next/server";

const BACKEND_URL = process.env.BACKEND_URL || "http://localhost:8080";
const TIMEOUT_MS = 120_000; // 2 minutes for LLM calls

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const body = await request.text();
  const authHeader = request.headers.get("authorization");

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const res = await fetch(`${BACKEND_URL}/apologetics/${id}/responses`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(authHeader ? { Authorization: authHeader } : {}),
      },
      body,
      signal: controller.signal,
    });
    clearTimeout(timeout);
    const data = await res.text();
    return new NextResponse(data, {
      status: res.status,
      headers: { "Content-Type": "application/json" },
    });
  } catch (e: unknown) {
    clearTimeout(timeout);
    const message = e instanceof Error ? e.message : "Request failed";
    if (message.includes("abort")) {
      return NextResponse.json({ error: "Request timeout — LLM processing took too long" }, { status: 504 });
    }
    return NextResponse.json({ error: message }, { status: 502 });
  }
}
