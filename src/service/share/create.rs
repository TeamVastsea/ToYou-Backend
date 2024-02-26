use std::cmp::min;
use std::sync::Arc;

use axum::extract::State;
use axum::http::{HeaderMap, StatusCode};
use axum::Json;
use axum::response::IntoResponse;
use chrono::{Days, Utc};
use sea_orm::{ActiveModelTrait, NotSet};
use sea_orm::ActiveValue::Set;
use serde::Deserialize;

use crate::ServerState;
use crate::service::share::content::ContentType;
use crate::service::user::level::LevelInfo;
use crate::service::user::login::login_by_token;

pub async fn create_share(State(state): State<Arc<ServerState>>, headers: HeaderMap, Json(body): Json<CreateShareRequest>) -> impl IntoResponse {
    let user_id = login_by_token(&state.db, headers).await;
    if user_id.is_none() {
        return Err((StatusCode::UNAUTHORIZED, "Invalid token.".to_string()));
    }
    let user_id = user_id.unwrap();
    
    for content in body.content.iter() {
        if !ContentType::verify(content) {
            return Err((StatusCode::BAD_REQUEST, "Invalid content type.".to_string()));
        }
    }

    let level: LevelInfo = user_id.level.into_iter()
        .map(|e| {
            let raw: Vec<i64> = serde_json::from_str(&e).unwrap();
            LevelInfo::try_from(raw).unwrap_or_else(|e| LevelInfo::get_free_level())
        }).max().unwrap_or_else(|| LevelInfo::get_free_level());
    let max_level = level.get_max_share_level();
    let share_level = min(max_level as u8, body.mode as u8);
    
    let share = crate::model::share::ActiveModel {
        id: NotSet,
        content: Set(body.content),
        password: Set(body.password),
        user_id: Set(user_id.id),
        mode: Set(share_level as i16),
        create_time: NotSet,
        valid_time: Set(Utc::now().checked_add_days(Days::new(7)).unwrap().naive_local()),
    };
    
    return Ok(share.insert(&state.db).await.unwrap().id.to_string())
}

#[derive(Deserialize)]
pub struct CreateShareRequest {
    mode: i16,
    password: Option<String>,
    content: Vec<String>
}