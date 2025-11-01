<template>
  <svg
    width="100%"
    height="15"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <defs>
      <pattern id="slashesPattern" patternUnits="userSpaceOnUse" width="16" height="15">
        <line x1="0"  y1="15" x2="8"  y2="0"  class="stroke-navy-700" stroke-width="2" />
        <line x1="4"  y1="15" x2="12" y2="0"  class="stroke-navy-100" stroke-width="2" />
      </pattern>
    </defs>
    <rect fill="url(#slashesPattern)" width="100%" height="100%" />
  </svg>
</template>

<script setup lang="ts"></script>
