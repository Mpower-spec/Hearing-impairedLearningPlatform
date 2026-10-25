<template>
  <div class="ambient-backdrop" aria-hidden="true">
    <div class="ambient-aurora ambient-aurora--cyan"></div>
    <div class="ambient-aurora ambient-aurora--mint"></div>
    <div class="ambient-aurora ambient-aurora--amber"></div>
    <div class="ambient-grid"></div>
    <div class="ambient-milkyway"></div>
    <div class="ambient-moon"></div>
    <div class="ambient-stars">
      <span
        v-for="star in 48"
        :key="star"
        :style="{
          left: `${(star * 37) % 100}%`,
          top: `${(star * 53) % 100}%`,
          width: `${1 + star % 3}px`,
          height: `${1 + star % 3}px`,
          opacity: `${0.24 + (star % 5) * 0.12}`,
          animationDelay: `${star * 0.19}s`,
          animationDuration: `${2.6 + (star % 7) * 0.45}s`
        }"
      ></span>
    </div>
    <div class="ambient-meteors">
      <span
        v-for="meteor in 18"
        :key="meteor"
        :style="{
          left: `${-8 + (meteor * 19) % 104}%`,
          top: `${-14 + (meteor * 31) % 58}%`,
          width: `${86 + (meteor % 4) * 38}px`,
          animationDelay: `${meteor * 1.35}s`,
          animationDuration: `${5.4 + (meteor % 5) * 1.15}s`
        }"
      ></span>
    </div>
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
.ambient-milkyway,
.ambient-moon,
.ambient-stars,
.ambient-meteors {
  position: absolute;
  pointer-events: none;
}
.ambient-milkyway {
  width: 125vw;
  height: 48vh;
  left: -14vw;
  top: 2vh;
  border-radius: 50%;
  background:
    radial-gradient(ellipse at 46% 52%, rgba(94, 177, 221, 0.2), transparent 48%),
    radial-gradient(ellipse at 62% 48%, rgba(174, 130, 221, 0.13), transparent 52%),
    radial-gradient(ellipse at 28% 48%, rgba(70, 222, 203, 0.1), transparent 48%);
  filter: blur(30px);
  opacity: 0.72;
  transform: rotate(-17deg);
  animation: milkyway-breathe 16s ease-in-out infinite alternate;
}
.ambient-moon {
  width: 150px;
  height: 150px;
  right: 9%;
  top: 7%;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 32%, rgba(226, 250, 255, 0.6), rgba(136, 220, 237, 0.18) 48%, transparent 72%);
  box-shadow: 0 0 80px rgba(106, 220, 235, 0.17);
  filter: blur(1px);
  opacity: 0.72;
  animation: moon-breathe 8s ease-in-out infinite alternate;
}
.ambient-stars { inset: 0; }
.ambient-stars span {
  position: absolute;
  border-radius: 50%;
  background: #e9ffff;
  box-shadow: 0 0 8px rgba(164, 242, 255, 0.9), 0 0 18px rgba(89, 201, 225, 0.4);
  animation-name: star-twinkle;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
  animation-direction: alternate;
}
.ambient-meteors { inset: 0; overflow: hidden; }
.ambient-meteors span {
  position: absolute;
  height: 1.5px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent 0%, rgba(83, 195, 229, 0.08) 24%, rgba(159, 239, 255, 0.74) 82%, #ffffff 100%);
  box-shadow: 0 0 8px rgba(136, 231, 255, 0.44);
  opacity: 0;
  transform-origin: right center;
  animation-name: meteor-flight;
  animation-timing-function: cubic-bezier(0.12, 0.56, 0.28, 1);
  animation-iteration-count: infinite;
  will-change: transform, opacity;
}
.ambient-meteors span::after {
  content: "";
  position: absolute;
  right: -2px;
  top: 50%;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 0 8px #bdf7ff, 0 0 16px rgba(96, 220, 255, 0.9);
  transform: translateY(-50%);
}
@keyframes star-twinkle {
  0% { transform: scale(0.65); opacity: 0.18; }
  100% { transform: scale(1.28); opacity: 0.95; }
}
@keyframes meteor-flight {
  0%, 71% { opacity: 0; transform: translate3d(-14vw, -8vh, 0) rotate(32deg) scaleX(0.28); }
  73% { opacity: 0.9; }
  86% { opacity: 0.95; }
  100% { opacity: 0; transform: translate3d(54vw, 36vh, 0) rotate(32deg) scaleX(1); }
}
@keyframes milkyway-breathe { to { opacity: 0.48; transform: rotate(-15deg) scale(1.06); } }
@keyframes moon-breathe { to { opacity: 0.48; transform: scale(1.06); } }
@media (max-width: 767px) {
  .ambient-milkyway { opacity: 0.5; filter: blur(24px); }
  .ambient-moon { width: 100px; height: 100px; right: 4%; top: 5%; opacity: 0.46; }
  .ambient-stars span:nth-child(2n),
  .ambient-meteors span:nth-child(2n) { display: none; }
}
</style>