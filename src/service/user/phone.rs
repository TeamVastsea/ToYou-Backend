use std::collections::HashMap;
use std::time::Duration;

use axum::extract::{Path, Query};
use lazy_regex::regex;
use lazy_static::lazy_static;
use moka::future::Cache;
use rand::Rng;
use sea_orm::{ColumnTrait, EntityTrait, QueryFilter};
use tracing::log::debug;

use crate::{CONFIG, DATABASE};
use crate::model::prelude::User;
use crate::service::error::ErrorMessage;

lazy_static! {
    static ref CODE_CACHE: Cache<i32, String> = Cache::builder()
        .time_to_live(Duration::from_secs(60 * 10))
        .build();

    static ref HISTORY_CACHE: Cache<String, ()> = Cache::builder()
        .time_to_live(Duration::from_secs(60))
        .build();
    
    static ref SMS_CLIENT: reqwest::Client = reqwest::Client::new();
}

pub async fn get_user_phone(Path(phone): Path<String>) -> String {
    debug!("Get user phone: {}", phone);
    let res = User::find().filter(crate::model::user::Column::Phone.eq(phone)).all(&*DATABASE).await.unwrap();
    (!res.is_empty()).to_string()
}

pub async fn get_sms(Query(params): Query<HashMap<String, String>>) -> Result<String, ErrorMessage> {
    if !params.contains_key("phone") {
        return Err(ErrorMessage::InvalidParams("phone".to_string()));
    }
    let phone = params.get("phone").unwrap();
    if !regex!(r"^1[3-9]\d{9}$").is_match(phone) {
        return Err(ErrorMessage::InvalidParams("phone".to_string()));
    }

    if HISTORY_CACHE.get(phone).await.is_some() {
        return Err(ErrorMessage::TooFrequent);
    }
    let code = rand::thread_rng().gen_range(100000..999999);
    CODE_CACHE.insert(code, phone.clone()).await;

    lsys_lib_sms::AliSms::branch_send(
        SMS_CLIENT.clone(),
        "",
        &CONFIG.aliyun.app_key,
        &CONFIG.aliyun.app_secret,
        &CONFIG.aliyun.sign_name,
        &CONFIG.aliyun.template_code,
        (r#"{"code":"#.to_string() + &code.to_string() + r#"}"#).as_str(),
        &[phone],
        "",
        "",
    ).await.unwrap();

    debug!("Send SMS to {}: {}", phone, code);
    HISTORY_CACHE.insert(phone.clone(), ()).await;

    Ok("{\"cd\": 60000}".to_string())
}

pub async fn verify_sms(code: i32, phone: &str) -> bool {
    let cached_phone = CODE_CACHE.get(&code).await;

    if cached_phone.is_none() {
        return false;
    }
    if cached_phone.unwrap() != phone {
        return false;
    }

    CODE_CACHE.invalidate(&code).await;
    true
}