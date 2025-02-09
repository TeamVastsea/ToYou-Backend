use std::iter::once;

use axum::{http, Router};
use axum::extract::DefaultBodyLimit;
use axum::http::HeaderValue;
use axum::routing::{get, post};
use axum_server::tls_rustls::RustlsConfig;
use chrono::Local;
use lazy_static::lazy_static;
use sea_orm::{ConnectOptions, Database, DatabaseConnection};
use serde_json::json;
use shadow_rs::shadow;
use tower_http::catch_panic::CatchPanicLayer;
use tower_http::classify::StatusInRangeAsFailures;
use tower_http::cors::CorsLayer;
use tower_http::sensitive_headers::SetSensitiveHeadersLayer;
use tower_http::trace::TraceLayer;
use tracing::{debug, info, warn};
use tracing::log::LevelFilter;
use tracing_appender::non_blocking;
use tracing_appender::rolling::{RollingFileAppender, Rotation};
use tracing_subscriber::{EnvFilter, fmt, Registry};
use tracing_subscriber::fmt::time::ChronoLocal;
use tracing_subscriber::layer::SubscriberExt;
use tracing_subscriber::util::SubscriberInitExt;

use migration::{Migrator, MigratorTrait};

use crate::config::{Config, rename_log};
use crate::service::folder::create::create_folder;
use crate::service::folder::get::get_folder_info;
use crate::service::folder::rename::rename_folder;
use crate::service::picture::delete::delete_picture;
use crate::service::picture::get::{get_picture_preview, list_picture};
use crate::service::picture::rename::rename_picture;
use crate::service::picture::upload::post_picture;
use crate::service::share::create::create_share;
use crate::service::share::get::{check_share_password, get_share_folder, get_share_image, get_share_info, list_all_share};
use crate::service::trade::wechat::creat_wechat_pay;
use crate::service::trade::wechat::recall::wechat_pay_recall;
use crate::service::user::login::login_user;
use crate::service::user::phone::{get_sms, get_user_phone};
use crate::service::user::register::register_user;

mod config;
mod model;
mod service;
mod extractor;

lazy_static! {
    static ref CONFIG: Config = Config::new();
    static ref DATABASE: DatabaseConnection = {
        let mut opt = ConnectOptions::new(&CONFIG.connection.db_uri);
        opt.sqlx_logging(true);
        opt.sqlx_logging_level(LevelFilter::Info);
        futures::executor::block_on(Database::connect(opt)).unwrap_or_else(|e| {
            panic!("Failed to connect to database '{}': {}", CONFIG.connection.db_uri, e)
        })
    };
}

#[tokio::main]
async fn main() {
    let env_filter =
        EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new(&CONFIG.trace_level));
    let file_appender = RollingFileAppender::builder()
        .rotation(Rotation::NEVER)
        .filename_suffix(format!(
            "logs/{}-least.log",
            Local::now().format("%Y-%m-%d")
        ))
        .build("")
        .unwrap();
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

    Migrator::up(&*DATABASE, None).await.unwrap();
    let origins = CONFIG.connection.origins.clone().iter().map(|x| x.parse().unwrap()).collect::<Vec<HeaderValue>>();

    let app = Router::new()
        .route("/user", post(register_user).get(login_user))
        .route("/user/phone/:id", get(get_user_phone))
        .route("/user/code/phone", get(get_sms))
        .route("/picture", post(post_picture).get(list_picture).patch(rename_picture).delete(delete_picture))
        .route("/picture/preview", get(get_picture_preview))
        .route("/folder", post(create_folder).get(get_folder_info).patch(rename_folder))
        .route("/share", post(create_share).get(list_all_share))
        .route("/share/:id", get(get_share_info))
        .route("/share/password", get(check_share_password))
        .route("/share/image", get(get_share_image))
        .route("/share/folder", get(get_share_folder))
        .route("/pay/wechat", post(creat_wechat_pay))
        .route("/pay/wechat/callback", post(wechat_pay_recall))
        .route("/ping", get(ping))
        .layer(TraceLayer::new(
            StatusInRangeAsFailures::new(400..=599).into_make_classifier()
        ))
        .layer(CorsLayer::very_permissive().allow_origin(origins))
        .layer(CatchPanicLayer::new())
        .layer(DefaultBodyLimit::max(
            CONFIG.connection.max_body_size * 1024 * 1024,
        ))
        .layer(SetSensitiveHeadersLayer::new(once(http::HeaderName::from_lowercase(b"token").unwrap())));
    let addr = CONFIG.connection.server_addr.parse().unwrap();
    info!("Listening: {addr}");

    if CONFIG.connection.tls {
        debug!("HTTPS enabled.");
        let tls_config =
            RustlsConfig::from_pem_file(&CONFIG.connection.ssl_cert, &CONFIG.connection.ssl_key)
                .await
                .unwrap();
        axum_server::bind_rustls(addr, tls_config)
            .serve(app.into_make_service())
            .await
            .unwrap();
    } else {
        warn!("HTTPS disabled.");
        axum_server::bind(addr)
            .serve(app.into_make_service())
            .await
            .unwrap();
    }
    println!("Hello, world!");
}

async fn ping() -> String {
    shadow!(build);
    json!({"version": 2, "build_time": build::BUILD_TIME, "commit": build::SHORT_COMMIT, "rust_version": build::RUST_VERSION}).to_string()
}
