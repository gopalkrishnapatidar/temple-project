import { Suspense } from "react";
import { BackendStatus } from "@/components/BackendStatus";
import { BackendStatusLoading } from "@/components/BackendStatusLoading";

export default function HomePage() {
  return (
    <main className="page">
      <section className="hero">
        <h1>Temple Digital Services Platform</h1>
        <p>
          A production-oriented foundation for temple browsing, events, darshan
          bookings, donations, and account services.
        </p>
      </section>

      <Suspense fallback={<BackendStatusLoading />}>
        <BackendStatus />
      </Suspense>
    </main>
  );
}
