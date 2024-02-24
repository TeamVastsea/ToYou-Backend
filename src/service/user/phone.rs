use std::collections::HashMap;
use std::sync::Arc;
use std::time::Duration;

use axum::extract::{Path, Query, State};
use axum::http::StatusCode;
use lazy_regex::regex;
use lazy_static::lazy_static;
use moka::future::Cache;
use rand::Rng;
use sea_orm::{ColumnTrait, EntityTrait, QueryFilter};
use tracing::log::debug;

use crate::model::prelude::User;
use crate::ServerState;

lazy_static! {
    static ref CODE_CACHE: Cache<i32, String> = Cache::builder()
        .time_to_live(Duration::from_secs(60 * 5))
        .build();

    static ref HISTORY_CACHE: Cache<String, ()> = Cache::builder()
        .time_to_live(Duration::from_secs(60))
        .build();
    
    static ref SMS_CLIENT: reqwest::Client = reqwest::Client::new();
}

pub async fn get_user_phone(State(state): State<Arc<ServerState>>, Path(phone): Path<String>) -> String {
    debug!("Get user phone: {}", phone);
    let res = User::find().filter(crate::model::user::Column::Phone.eq(phone)).all(&state.db).await.unwrap();
    (!res.is_empty()).to_string()
}

pub async fn get_sms(State(state): State<Arc<ServerState>>, Query(params): Query<HashMap<String, String>>) -> Result<String, (StatusCode, String)> {
    if !params.contains_key("phone") {
        return Err((StatusCode::BAD_REQUEST, "Invalid phone.".to_string()));
    }
    let phone = params.get("phone").unwrap();
    if !regex!(r"^1[3-9]\d{9}$").is_match(phone) {
        return Err((StatusCode::BAD_REQUEST, "Invalid phone.".to_string()));
    }

    if HISTORY_CACHE.get(phone).await.is_some() {
        return Err((StatusCode::TOO_MANY_REQUESTS, "Request too frequent.".to_string()));
    }
    let code = rand::thread_rng().gen_range(100000..999999);
    let phone = params.get("phone").unwrap();
    CODE_CACHE.insert(code, phone.clone()).await;

    lsys_lib_sms::AliSms::branch_send(
        SMS_CLIENT.clone(),
        "",
        &state.config.aliyun.app_key,
        &state.config.aliyun.app_secret,
        &state.config.aliyun.sign_name,
        &state.config.aliyun.template_code,
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