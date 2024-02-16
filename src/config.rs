use std::fs::OpenOptions;
use std::io::{Read, Write};

use serde::{Deserialize, Serialize};
use serde_inline_default::serde_inline_default;

#[derive(Serialize, Deserialize)]
pub struct Config {
    #[serde(default = "generate_connection_setting")]
    pub connection: ConnectionConfig,
}

#[serde_inline_default]
#[derive(Serialize, Deserialize)]
pub struct ConnectionConfig {
    #[serde_inline_default(String::from("0.0.0.0:7890"))]
    pub server_uri: String,
    #[serde_inline_default(String::from("postgresql://root:root@127.0.0.1:5432/toyou"))]
    pub db_uri: String,
}

fn generate_connection_setting() -> ConnectionConfig {
    ConnectionConfig {
        server_uri: "0.0.0.0:7890".to_string(),
        db_uri: "postgresql://root:root@127.0.0.1:5432/toyou".to_string(),
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
