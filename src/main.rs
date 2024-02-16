use crate::config::Config;
use migration::{Migrator, MigratorTrait};
use sea_orm::{ConnectOptions, Database};
use tracing::log::LevelFilter;

mod config;
mod model;

fn init_log() -> () {
    let init = tracing_subscriber::fmt()
        .with_max_level(tracing::Level::DEBUG)
        .try_init();
    if init.is_err() {
        tracing::warn!("Failed to initialize logger");
    }
}

#[tokio::main]
async fn main() {
    let config = Config::new();
    init_log();

    let mut opt = ConnectOptions::new(config.connection.db_uri);
    opt.sqlx_logging(true);
    opt.sqlx_logging_level(LevelFilter::Debug);
    let db = Database::connect(opt).await.unwrap();

    Migrator::up(&db, None).await.unwrap();
}
