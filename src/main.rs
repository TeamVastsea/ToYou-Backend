mod config;
mod data_struct;

fn init_log() -> () {
    let init = tracing_subscriber::fmt()
        .with_max_level(tracing::Level::DEBUG)
        .try_init();
    if init.is_err() {
        tracing::warn!("Failed to initialize logger");
    }
}

fn main() {
    init_log();
}
