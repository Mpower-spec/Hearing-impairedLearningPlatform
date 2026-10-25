<template>
  <div class="ambient-backdrop" aria-hidden="true">
    <div class="ambient-aurora ambient-aurora--cyan"></div>
    <div class="ambient-aurora ambient-aurora--mint"></div>
    <div class="ambient-aurora ambient-aurora--amber"></div>
    <div class="ambient-grid"></div>
    <div class="ambient-ring ambient-ring--one"></div>
    <div class="ambient-ring ambient-ring--two"></div>
    <div class="ambient-particles">
      <span v-for="particle in 14" :key="particle" :style="{ left: `${(particle * 61) % 100}%`, top: `${(particle * 37) % 100}%`, width: `${2 + particle % 3}px`, height: `${2 + particle % 3}px`, animationDelay: `${particle * 0.16}s`, animationDuration: `${7 + particle * 0.55}s` }"></span>
    </div>
    <div class="ambient-wave" aria-hidden="true">
      <span v-for="bar in 32" :key="bar" :style="{ height: `${8 + (bar * 7) % 36}px`, animationDelay: `${bar * 0.05}s` }"></span>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.ambient-backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(7, 29, 44, 0.08), transparent 32%),
    radial-gradient(circle at 50% 10%, rgba(255, 255, 255, 0.82), transparent 36rem);
}
.ambient-aurora,
.ambient-ring,
.ambient-wave,
.ambient-particles {
  position: absolute;
}
.ambient-aurora {
  width: 42vw;
  height: 42vw;
  min-width: 480px;
  min-height: 480px;
  border-radius: 50%;
  filter: blur(66px);
  opacity: 0.42;
  will-change: transform;
}
.ambient-aurora--cyan {
  top: -24vw;
  left: -12vw;
  background: radial-gradient(circle, rgba(40, 189, 214, 0.72), transparent 68%);
  animation: aurora-cyan 19s ease-in-out infinite alternate;
}
.ambient-aurora--mint {
  top: 22vh;
  right: -22vw;
  background: radial-gradient(circle, rgba(57, 198, 164, 0.62), transparent 70%);
  animation: aurora-mint 24s ease-in-out infinite alternate;
}
.ambient-aurora--amber {
  bottom: -28vw;
  left: 28vw;
  width: 38vw;
  height: 38vw;
  min-width: 420px;
  min-height: 420px;
  background: radial-gradient(circle, rgba(242, 184, 70, 0.4), transparent 68%);
  animation: aurora-amber 22s ease-in-out infinite alternate;
}
.ambient-grid {
  position: absolute;
  inset: 0;
  opacity: 0.26;
  background-image:
    linear-gradient(rgba(20, 99, 125, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(20, 99, 125, 0.08) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: linear-gradient(to bottom, transparent, black 16%, black 72%, transparent);
}
.ambient-ring {
  border: 1px solid rgba(10, 104, 133, 0.15);
  border-radius: 50%;
}
.ambient-ring--one {
  width: 52vw;
  height: 52vw;
  top: 18vh;
  left: 66vw;
  animation: ring-drift 18s ease-in-out infinite alternate;
}
.ambient-ring--two {
  width: 34vw;
  height: 34vw;
  left: -10vw;
  bottom: 4vh;
  border-style: dashed;
  animation: ring-drift 24s ease-in-out infinite alternate-reverse;
}
.ambient-particles { inset: 0; }
.ambient-particles span {
  position: absolute;
  border-radius: 50%;
  background: rgba(31, 150, 159, 0.46);
  box-shadow: 0 0 12px rgba(31, 150, 159, 0.36);
  animation-name: particle-float;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
  animation-direction: alternate;
}
.ambient-wave {
  left: 8%;
  right: 8%;
  bottom: 22px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  opacity: 0.2;
  mask-image: linear-gradient(90deg, transparent, black 20%, black 80%, transparent);
}
.ambient-wave span {
  width: 5px;
  border-radius: 999px;
  background: linear-gradient(180deg, #28a7b8, #f0b429);
  animation: ambient-wave 2.2s ease-in-out infinite alternate;
}
@keyframes aurora-cyan { to { transform: translate3d(8vw, 5vh, 0) scale(1.16); } }
@keyframes aurora-mint { to { transform: translate3d(-9vw, -7vh, 0) scale(0.9); } }
@keyframes aurora-amber { to { transform: translate3d(7vw, -5vh, 0) scale(1.12); } }
@keyframes ring-drift { to { transform: translate3d(-4vw, 4vh, 0) rotate(12deg); } }
@keyframes particle-float { to { transform: translate3d(18px, -28px, 0); opacity: 0.15; } }
@keyframes ambient-wave { from { transform: scaleY(0.3); opacity: 0.3; } to { transform: scaleY(1); opacity: 1; } }
@media (max-width: 767px) {
  .ambient-aurora { opacity: 0.3; filter: blur(48px); }
  .ambient-grid { background-size: 34px 34px; opacity: 0.18; }
  .ambient-ring--one { display: none; }
  .ambient-wave { left: 2%; right: 2%; gap: 6px; }
}
</style>