use std::fs::OpenOptions;
use std::io::{Read, Write};

use chrono::{DateTime, Local};
use serde::{Deserialize, Serialize};
use serde_inline_default::serde_inline_default;
use tokio::fs;

#[serde_inline_default]
#[derive(Serialize, Deserialize, Clone)]
pub struct Config {
    #[serde_inline_default(String::from("info"))]
    pub trace_level: String,
    #[serde(default = "generate_connection_setting")]
    pub connection: ConnectionConfig,
    #[serde(default = "generate_aliyun_setting")]
    pub aliyun: AliyunConfig,
    #[serde(default = "generate_wechat_setting")]
    pub wechat: WeChatConfig,
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
    #[serde_inline_default(vec!["http://localhost:3000".into(), "https://itoyou.cc".into()])]
    pub origins: Vec<String>,
}

#[serde_inline_default]
#[derive(Serialize, Deserialize, Clone)]
pub struct AliyunConfig {
    #[serde_inline_default(String::from("LTAI5tD8ZcM52sTr3ubxxxxx"))]
    pub app_key: String,
    #[serde_inline_default(String::from("alCw4PXzoPM6KFbAgaM1UGC0O2xxxx"))]
    pub app_secret: String,
    #[serde_inline_default(String::from("SMS_xxxxxxxx"))]
    pub template_code: String,
    #[serde_inline_default(String::from("ToYou"))]
    pub sign_name: String,
}

#[serde_inline_default]
#[derive(Serialize, Deserialize, Clone)]
pub struct WeChatConfig {
    #[serde_inline_default(String::from("appid"))]
    pub app_id: String,
    #[serde_inline_default(String::from("mch"))]
    pub mch_id: String,
    #[serde_inline_default(String::from("serial"))]
    pub serial: String,
    #[serde_inline_default(String::from("key"))]
    pub key: String,
    #[serde_inline_default(String::from("./wx_private.pem"))]
    pub private_key: String,
    #[serde_inline_default(String::from("./wx_public.pem"))]
    pub public_key: String,
    #[serde_inline_default(String::from("https://api.toyou.cc/wechat/callback"))]
    pub call_back_url: String
}

fn generate_connection_setting() -> ConnectionConfig {
    ConnectionConfig {
        server_addr: "0.0.0.0:7890".to_string(),
        db_uri: "postgresql://root:root@127.0.0.1:5432/toyou".to_string(),
        tls: false,
        ssl_cert: "./cert.crt".to_string(),
        ssl_key: "./private.key".to_string(),
        max_body_size: 2,
        origins: vec!["http://localhost:3000".into(), "https://itoyou.cc".into()],
    }
}

fn generate_wechat_setting() -> WeChatConfig {
    WeChatConfig {
        app_id: "appid".to_string(),
        mch_id: "mch".to_string(),
        serial: "serial".to_string(),
        key: "key".to_string(),
        private_key: "./wx_private.pem".to_string(),
        public_key: "./wx_public.pem".to_string(),
        call_back_url: "https://api.toyou.cc/wechat/callback".to_string(),
    }
}

fn generate_aliyun_setting() -> AliyunConfig {
    AliyunConfig {
        app_key: "LTAI5tD8ZcM52sTr3ubxxxxx".to_string(),
        app_secret: "alCw4PXzoPM6KFbAgaM1UGC0O2xxxx".to_string(),
        template_code: "SMS_xxxxxxxx".to_string(),
        sign_name: "ToYou".to_string(),
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
