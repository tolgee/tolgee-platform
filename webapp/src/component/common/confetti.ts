type ConfettiBit = {
  x: number;
  y: number;
  vx: number;
  vy: number;
  w: number;
  h: number;
  rot: number;
  vr: number;
  color: string;
};

/** Colours are passed in so this knows nothing about any particular palette. */
export function runConfetti(
  canvas: HTMLCanvasElement,
  colors: string[],
  durationMs: number
): () => void {
  const dpr = window.devicePixelRatio || 1;
  const width = canvas.offsetWidth;
  const height = canvas.offsetHeight;
  canvas.width = width * dpr;
  canvas.height = height * dpr;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    return () => undefined;
  }
  ctx.scale(dpr, dpr);

  const bits: ConfettiBit[] = Array.from({ length: 110 }, () => ({
    x: width / 2 + (Math.random() - 0.5) * width * 0.5,
    y: height * 0.44 + (Math.random() - 0.5) * 30,
    vx: (Math.random() - 0.5) * 7,
    vy: -Math.random() * 9 - 3,
    w: 4 + Math.random() * 5,
    h: 6 + Math.random() * 7,
    rot: Math.random() * Math.PI,
    vr: (Math.random() - 0.5) * 0.3,
    color: colors[Math.floor(Math.random() * colors.length)],
  }));

  let raf = 0;
  const start = performance.now();
  const frame = (now: number) => {
    const elapsed = now - start;
    ctx.clearRect(0, 0, width, height);
    bits.forEach((b) => {
      b.vy += 0.22;
      b.vx *= 0.995;
      b.x += b.vx;
      b.y += b.vy;
      b.rot += b.vr;
      ctx.save();
      ctx.translate(b.x, b.y);
      ctx.rotate(b.rot);
      ctx.globalAlpha = Math.max(0, 1 - elapsed / durationMs);
      ctx.fillStyle = b.color;
      ctx.fillRect(-b.w / 2, -b.h / 2, b.w, b.h);
      ctx.restore();
    });
    if (elapsed < durationMs) {
      raf = window.requestAnimationFrame(frame);
    }
  };
  raf = window.requestAnimationFrame(frame);

  return () => window.cancelAnimationFrame(raf);
}
