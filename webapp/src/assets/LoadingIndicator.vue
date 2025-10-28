<template>
  <svg
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    role="status"
    aria-label="Loading..."
    class="text-seafoam-500"
  >
    <g>
      <circle cx="12" cy="12" r="9" class="stroke-tides-600" stroke-width="2" />
      <mask id="slice" fill="white">
        <path
          d="M22 12C22 14.1118 21.3315 16.1694 20.0902 17.8779C18.8489 19.5863 17.0986 20.858 15.0902 21.5106L14.4721 19.6085C16.0789 19.0864 17.4791 18.0691 18.4721 16.7023C19.4652 15.3355 20 13.6894 20 12L22 12Z"
        />
      </mask>
      <path
        d="M22 12C22 14.1118 21.3315 16.1694 20.0902 17.8779C18.8489 19.5863 17.0986 20.858 15.0902 21.5106L14.4721 19.6085C16.0789 19.0864 17.4791 18.0691 18.4721 16.7023C19.4652 15.3355 20 13.6894 20 12L22 12Z"
        stroke="currentColor"
        stroke-width="4"
        mask="url(#slice)"
      />
      <animateTransform
        attributeName="transform"
        type="rotate"
        from="0 12 12"
        to="360 12 12"
        dur="0.9s"
        repeatCount="indefinite"
      />
    </g>
  </svg>
</template>
