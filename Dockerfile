# cargo-chef and the Rust toolchain
FROM lukemathwalker/cargo-chef AS chef
WORKDIR /app

# prepare recipe
FROM chef AS planner
COPY . .
RUN cargo chef prepare --recipe-path recipe.json

# build artifact
FROM chef AS builder
COPY --from=planner /app/recipe.json recipe.json
## build dependencies
RUN cargo chef cook --release --recipe-path recipe.json
## build application
COPY . .
RUN cargo build --release

# build slim image
FROM debian:bookworm-slim AS runtime
WORKDIR /app
COPY --from=builder /app/target/release/toyou-backend /usr/app/toyou-backend

RUN \
  apt-get update && \
  apt-get install -y curl vim openssl

RUN \
  apt-get clean && \
  apt-get autoclean && \
  rm -rf /var/lib/apt/lists/*

  
WORKDIR /usr/app

CMD ["/usr/app/toyou-backend"]
