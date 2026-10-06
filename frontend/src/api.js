export class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}

export async function api(path, options = {}) {
  const timeout = options.timeout ?? 240000;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);
  try {
    const response = await fetch(`/api${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options.headers },
      signal: controller.signal,
    });
    const text = await response.text();
    let data;
    try { data = JSON.parse(text); } catch {
      throw new ApiError('The backend returned an unexpected response. Check that Spring Boot is running on port 8081.', response.status);
    }
    if (!response.ok) throw new ApiError(data.message || 'The request could not be completed.', response.status);
    return data;
  } catch (error) {
    if (error.name === 'AbortError') throw new ApiError('The local model took too long. Try a shorter question, or check the Ollama console.', 408);
    if (error instanceof ApiError) throw error;
    throw new ApiError('Cannot reach the backend. Start Spring Boot on port 8081, then try again.', 0);
  } finally { clearTimeout(timer); }
}
