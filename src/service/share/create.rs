use axum::Json;
use chrono::{Days, Utc};
use sea_orm::{ActiveModelTrait, NotSet};
use sea_orm::ActiveValue::Set;
use serde::Deserialize;

use crate::DATABASE;
use crate::extractor::auth::AuthUser;
use crate::service::error::ErrorMessage;
use crate::service::share::content::ContentType;
use crate::service::user::level::LevelInfo;
use crate::service::user::password::generate_password_hash;

pub async fn create_share(AuthUser(user): AuthUser, Json(body): Json<CreateShareRequest>) -> Result<String, ErrorMessage> {
    for content in body.content.iter() {
        if !ContentType::verify(content) {
            return Err(ErrorMessage::InvalidParams(format!("content {}", content)));
        }
    }

    let level: LevelInfo = user.level.into_iter()
        .map(|e| {
            let raw: Vec<i64> = serde_json::from_str(&e).unwrap();
            LevelInfo::try_from(raw).unwrap_or_else(|_| LevelInfo::get_free_level())
        }).max().unwrap_or_else(|| LevelInfo::get_free_level());
    let max_level = level.level.get_max_share_level();
    let share_level = body.mode as u8;

    if share_level > max_level as u8 {
        return Err(ErrorMessage::PermissionDenied);
    }
    let password = match body.password {
        None => { None }
        Some(a) => { Some(generate_password_hash(&a)) }
    };

    let share = crate::model::share::ActiveModel {
        id: NotSet,
        content: Set(body.content),
        password: Set(password),
        user_id: Set(user.id),
        mode: Set(share_level as i16),
        create_time: NotSet,
        valid_time: Set(Utc::now().checked_add_days(Days::new(7)).unwrap().naive_local()),
    };

    return Ok(share.insert(&*DATABASE).await.unwrap().id.to_string());
}

#[derive(Deserialize)]
pub struct CreateShareRequest {
    mode: i16,
    password: Option<String>,
    content: Vec<String>,
}