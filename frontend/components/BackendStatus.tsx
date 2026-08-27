import { fetchPing } from "@/lib/api";

export async function BackendStatus() {
  const result = await fetchPing();

  if (!result.ok) {
    return (
      <section className="status-card status-card-error" aria-live="polite">
        <h2>Backend Status</h2>
        <p className="status-label status-label-error" role="alert">
          {result.error}
        </p>
      </section>
    );
  }

  return (
    <section className="status-card status-card-success" aria-live="polite">
      <h2>Backend Status</h2>
      <dl className="status-details">
        <div>
          <dt>Status</dt>
          <dd>{result.data.status}</dd>
        </div>
        <div>
          <dt>Message</dt>
          <dd>{result.data.message}</dd>
        </div>
      </dl>
    </section>
  );
}
