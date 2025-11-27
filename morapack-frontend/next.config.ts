import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  devIndicators: {
    appIsrStatus: false,
    buildActivity: false,
  },
  eslint: {
    ignoreDuringBuilds: true,
  },
};

export default nextConfig;
