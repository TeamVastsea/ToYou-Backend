use std::fs::OpenOptions;
use std::io::{Read, Write};

use chrono::{DateTime, Local};
use serde::{Deserialize, Serialize};
use serde_inline_default::serde_inline_default;
use tokio::fs;

#[serde_inline_default]
#[derive(Serialize, Deserialize, Clone)]
pub struct Config {
    #[serde(default = "generate_connection_setting")]
    pub connection: ConnectionConfig,
    #[serde_inline_default(String::from("info"))]
    pub trace_level: String,
}

#[serde_inline_default]
#[derive(Serialize, Deserialize, Clone)]
pub struct ConnectionConfig {
    #[serde_inline_default(String::from("0.0.0.0:7890"))]
    pub server_addr: String,
    #[serde_inline_default(String::from("postgresql://root:root@127.0.0.1:5432/toyou"))]
    pub db_uri: String,
    #[serde_inline_default(false)]
    pub tls: bool,
    #[serde_inline_default(String::from("./cert.crt"))]
    pub ssl_cert: String,
    #[serde_inline_default(String::from("./private.key"))]
    pub ssl_key: String,
    #[serde_inline_default(2)]
    pub max_body_size: usize,
}

fn generate_connection_setting() -> ConnectionConfig {
    ConnectionConfig {
        server_addr: "0.0.0.0:7890".to_string(),
        db_uri: "postgresql://root:root@127.0.0.1:5432/toyou".to_string(),
        tls: false,
        ssl_cert: "./cert.crt".to_string(),
        ssl_key: "./private.key".to_string(),
        max_body_size: 2,
    }
}

impl Config {
    pub fn new() -> Self {
        let mut raw_config = String::new();
        let mut file = OpenOptions::new()
            .read(true)
            .write(true)
            .create(true)
            .open("config.toml")
            .expect("Cannot open 'config.toml'");
        file.read_to_string(&mut raw_config).unwrap();

        let config: Config = toml::from_str(&raw_config).unwrap();

        config.save();

        config
    }

    pub fn save(&self) {
        let config_str = toml::to_string_pretty(self).unwrap();

        let mut file = OpenOptions::new()
            .write(true)
            .truncate(true)
            .open("config.toml")
            .expect("Cannot open 'config.toml'");
        file.write_all(config_str.as_bytes()).unwrap();
    }
}

pub async fn rename_log(now: DateTime<Local>) {
    fs::create_dir_all("logs").await.unwrap();
    let file_name = format!("logs/{}-least.log", now.format("%Y-%m-%d"));
    if fs::try_exists(file_name.clone()).await.unwrap() {
        let mut new_name = file_name.clone();
        let mut file_name_offset = 0;
        while fs::try_exists(new_name.clone()).await.unwrap() {
            file_name_offset += 1;
            new_name = format!("logs/{}-{file_name_offset}.log", now.format("%Y-%m-%d"));
        }

        fs::rename(file_name.clone(), new_name).await.unwrap();
    }
}