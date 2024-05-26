use axum::http::HeaderMap;
use axum::Json;
use chrono::{DateTime, NaiveDateTime};
use serde::{Deserialize, Serialize};
use crate::service::error::ErrorMessage;
use crate::service::trade::wechat::start::start_wechat;
use crate::service::user::level::Level;
use crate::service::user::login::login_by_token;

mod start;
pub mod recall;

pub async fn creat_wechat_pay(headers: HeaderMap, Json(request): Json<CreatePayRequest>) -> Result<String, ErrorMessage> {
    let user = login_by_token(headers).await.ok_or(ErrorMessage::InvalidToken)?;
    
    let (result, code) = start_wechat(user.id, request.level, request.period, request.start_date.and_utc()).await;
    if !result {
        return Err(ErrorMessage::Other(code));
    }
    
    Ok(code)
}

#[derive(Deserialize)]
pub struct CreatePayRequest {
    level: Level,
    period: i32,
    start_date: NaiveDateTime
}