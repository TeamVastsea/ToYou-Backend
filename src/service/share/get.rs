use std::collections::HashMap;
use std::str::FromStr;
use std::sync::Arc;

use axum::extract::{Query, State};
use axum::http::{HeaderMap, HeaderName, HeaderValue, StatusCode};
use sea_orm::EntityTrait;
use sea_orm::prelude::Uuid;
use serde::Deserialize;

use crate::model::prelude::{Share, User, UserImage};
use crate::model::share::ShareInfo;
use crate::ServerState;
use crate::service::picture::compress::ImageFile;
use crate::service::share::content::ContentType;
use crate::service::user::level::ShareLevel;
use crate::service::user::password::verify_password;

pub async fn check_share_password(State(state): State<Arc<ServerState>>, Query(query): Query<HashMap<String, String>>) -> Result<String, (StatusCode, String)> {
    let id = query.get("id").ok_or((StatusCode::BAD_REQUEST, "Invalid id.".to_string()))?;
    let id = Uuid::from_str(id).map_err(|_| { (StatusCode::BAD_REQUEST, "Invalid id.".to_string()) })?;
    let share = Share::find_by_id(id).one(&state.db).await.unwrap().ok_or((StatusCode::BAD_REQUEST, "Invalid id.".to_string()))?;

    return Ok(share.password.is_some().to_string());
}

pub async fn get_share_info(State(state): State<Arc<ServerState>>, Query(query): Query<HashMap<String, String>>) -> Result<String, (StatusCode, String)> {
    let id = query.get("id").ok_or((StatusCode::BAD_REQUEST, "Invalid id.".to_string()))?;
    let id = Uuid::from_str(id).map_err(|_| { (StatusCode::BAD_REQUEST, "Invalid id.".to_string()) })?;
    let share = Share::find_by_id(id).one(&state.db).await.unwrap().ok_or((StatusCode::BAD_REQUEST, "Invalid id.".to_string()))?;
    if share.password.is_some() {
        let password = query.get("password").ok_or((StatusCode::BAD_REQUEST, "Invalid password.".to_string()))?;
        if !verify_password(password, &share.password.unwrap()) {
            return Err((StatusCode::UNAUTHORIZED, "Invalid password.".to_string()));
        }
    }
    let user = User::find_by_id(share.user_id).one(&state.db).await.unwrap().ok_or((StatusCode::BAD_REQUEST, "Invalid user.".to_string()))?;

    let share_info = ShareInfo {
        id: share.id,
        content: share.content,
        user_name: user.username,
        mode: share.mode,
        create_time: share.create_time,
        valid_time: share.valid_time,
    };

    Ok(serde_json::to_string(&share_info).unwrap())
}

pub async fn get_share_image(State(state): State<Arc<ServerState>>, Query(query): Query<GetShareImageRequest>) -> Result<(HeaderMap, Vec<u8>), (StatusCode, String)> {
    let id = Uuid::from_str(&query.id).map_err(|_| { (StatusCode::BAD_REQUEST, "Invalid id.".to_string()) })?;
    let share = Share::find_by_id(id).one(&state.db).await.unwrap().ok_or((StatusCode::BAD_REQUEST, "Invalid id.".to_string()))?;
    if share.password.is_some() {
        let password = query.password.ok_or((StatusCode::BAD_REQUEST, "Invalid password.".to_string()))?;
        if !verify_password(&password, &share.password.unwrap()) {
            return Err((StatusCode::UNAUTHORIZED, "Invalid password.".to_string()));
        }
    }

    if !share.content.contains(&query.content) {
        return Err((StatusCode::BAD_REQUEST, "Invalid content.".to_string()));
    }
    let content_id = ContentType::try_from(query.content.as_str()).map_err(|_| { (StatusCode::BAD_REQUEST, "Invalid content.".to_string()) })?;
    let image = match content_id {
        ContentType::Folder(_) => { return Err((StatusCode::BAD_REQUEST, "This api is image only.".to_string())); }
        ContentType::Picture(id) => { UserImage::find_by_id(id).one(&state.db).await.unwrap().ok_or((StatusCode::NOT_FOUND, "Image not fount.".to_string()))? }
    };

    let mut file = ImageFile::new(image.image_id.as_str()).await.unwrap();
    file.compress_by_level(ShareLevel::try_from(share.mode).unwrap());

    let mut headers = HeaderMap::new();
    headers.insert(
        HeaderName::from_static("content-type"),
        HeaderValue::from_str("image/png").unwrap(),
    );

    Ok((headers, file.encode().unwrap()))
}

#[derive(Deserialize)]
pub struct GetShareImageRequest {
    pub id: String,
    pub password: Option<String>,
    pub content: String,
}