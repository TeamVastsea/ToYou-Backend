use std::sync::Arc;
use axum::{http, Router};
use axum::body::Body;
use axum::extract::DefaultBodyLimit;
use axum::http::Request;
use axum::routing::{get, post};
use axum_server::tls_rustls::RustlsConfig;
use chrono::Local;
use sea_orm::{ConnectOptions, Database, DatabaseConnection};
use tower_http::catch_panic::CatchPanicLayer;
use tower_http::trace::TraceLayer;
use tracing::{debug, info, Span, warn};
use tracing::log::LevelFilter;
use tracing_appender::non_blocking;
use tracing_appender::rolling::{RollingFileAppender, Rotation};
use tracing_subscriber::{EnvFilter, fmt, Registry};
use tracing_subscriber::fmt::time::ChronoLocal;
use tracing_subscriber::layer::SubscriberExt;
use tracing_subscriber::util::SubscriberInitExt;

use migration::{Migrator, MigratorTrait};

use crate::config::{Config, rename_log};
use crate::service::picture::upload::post_picture;

mod config;
mod model;
mod service;

#[tokio::main]
async fn main() {
    let config = Config::new();

    let env_filter = EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new(&config.trace_level));
    let file_appender = RollingFileAppender::builder()
        .rotation(Rotation::NEVER)
        .filename_suffix(format!("logs/{}-least.log", Local::now().format("%Y-%m-%d")))
        .build("").unwrap();
    let (non_blocking_appender, _guard) = non_blocking(file_appender);

    rename_log(Local::now()).await;
    let formatting_layer = fmt::layer()
        .with_writer(std::io::stderr)
        .with_timer(ChronoLocal::new("%Y-%m-%d %H:%M:%S%.f(%:z)".to_string()));
    let file_layer = fmt::layer()
        .with_timer(ChronoLocal::new("%Y-%m-%d %H:%M:%S%.f(%:z)".to_string()))
        .with_ansi(false)
        .with_writer(non_blocking_appender);
    Registry::default()
        .with(env_filter)
        .with(formatting_layer)
        .with(file_layer)
        .init();


    let mut opt = ConnectOptions::new(&config.connection.db_uri);
    opt.sqlx_logging(true);
    opt.sqlx_logging_level(LevelFilter::Debug);
    let db = Database::connect(opt).await.unwrap();

    Migrator::up(&db, None).await.unwrap();

    let trace_layer = TraceLayer::new_for_http()
        .on_request(|request: &Request<Body>, _span: &Span| {
            debug!("{} {}", request.method(), request.uri().path());
        })
        .on_response(|response: &http::Response<Body>, _latency: std::time::Duration, _span: &Span| {
            debug!("{}", response.status());
        });

    let app_state = Arc::new(ServerState {
        config: config.clone(),
        db,
    });

    let app = Router::new()
        .route("/picture", post(post_picture))
        .route("/ping", get(ping))
        .with_state(app_state)
        .layer(trace_layer)
        .layer(CatchPanicLayer::new())
        .layer(DefaultBodyLimit::max(config.connection.max_body_size * 1024 * 1024));
    let addr = config.connection.server_addr.parse().unwrap();
    info!("Listening: {addr}");

    if config.connection.tls {
        debug!("HTTPS enabled.");
        let tls_config = RustlsConfig::from_pem_file(config.connection.ssl_cert, config.connection.ssl_key).await.unwrap();
        axum_server::bind_rustls(addr, tls_config).serve(app.into_make_service()).await.unwrap();
    } else {
        warn!("HTTPS disabled.");
        axum_server::bind(addr).serve(app.into_make_service()).await.unwrap();
    }
    println!("Hello, world!");
}

struct ServerState {
    pub config: Config,
    pub db: DatabaseConnection,
}

async fn ping() -> String {
    "{\"version\" : \"0.1.0\"}".to_string()
}
