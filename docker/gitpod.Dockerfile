# Src: https://github.com/gitpod-io/workspace-images/tree/main/chunks/lang-java
FROM gitpod/workspace-java-17@sha256:9afcbfd630163f00cab4bceab2d041e028c5b774019a40cb533f17d7ffa98350

# Merge workspace-node image to workspace-java-17
# Src: https://github.com/gitpod-io/workspace-images/tree/main/chunks/lang-node
COPY --from=gitpod/workspace-node@sha256:08e175d076c1197774438c8a26cc978a9cde5f96795ccd0ebce0c2b6f5035170 / /
ENV NODE_VERSION=21.2.0
ENV PNPM_HOME=/home/gitpod/.pnpm
ENV PATH=/home/gitpod/.nvm/versions/node/v${NODE_VERSION}/bin:/home/gitpod/.yarn/bin:${PNPM_HOME}:$PATH
