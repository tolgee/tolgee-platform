# Src: https://github.com/gitpod-io/workspace-images/tree/main/chunks/lang-java
FROM gitpod/workspace-java-25@sha256:c21f0967499c211e732fe7aa1f6919cd54a8ff9309ae118f1f4f8c9196d031e6

# Merge workspace-node image to workspace-java-25
# Src: https://github.com/gitpod-io/workspace-images/tree/main/chunks/lang-node
COPY --from=gitpod/workspace-node@sha256:08e175d076c1197774438c8a26cc978a9cde5f96795ccd0ebce0c2b6f5035170 / /
ENV NODE_VERSION=21.2.0
ENV PNPM_HOME=/home/gitpod/.pnpm
ENV PATH=/home/gitpod/.nvm/versions/node/v${NODE_VERSION}/bin:/home/gitpod/.yarn/bin:${PNPM_HOME}:$PATH
