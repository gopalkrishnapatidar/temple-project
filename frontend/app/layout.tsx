import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Temple Digital Services Platform",
  description: "Digital services foundation for temple operations.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <header className="site-header">
          <p className="site-brand">Temple Digital Services Platform</p>
        </header>
        {children}
        <footer className="site-footer">
          <p>Foundation release for future temple digital services.</p>
        </footer>
      </body>
    </html>
  );
}
