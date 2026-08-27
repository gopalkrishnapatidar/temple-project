export function BackendStatusLoading() {
  return (
    <section className="status-card status-card-loading" aria-live="polite">
      <h2>Backend Status</h2>
      <p className="status-label">Checking backend connectivity...</p>
    </section>
  );
}
