export type PingResponse = {
  status: string;
  message: string;
};

export type PingResult =
  | { ok: true; data: PingResponse }
  | { ok: false; error: string };
