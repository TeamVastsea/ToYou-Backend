use axum::Json;
use chrono::NaiveDate;
use serde::Deserialize;

use crate::extractor::auth::AuthUser;
use crate::service::error::ErrorMessage;
use crate::service::trade::wechat::start::start_wechat;
use crate::service::user::level::Level;

mod start;
pub mod recall;

pub async fn creat_wechat_pay(AuthUser(user): AuthUser, Json(request): Json<CreatePayRequest>) -> Result<String, ErrorMessage> {
    let (result, code) = start_wechat(user.id, request.level, request.period, request.start_date).await;
    if !result {
        return Err(ErrorMessage::Other(code));
    }

    Ok(code)
}

#[derive(Deserialize)]
pub struct CreatePayRequest {
    level: Level,
    period: i32,
    start_date: NaiveDate,
}