use std::sync::Arc;
use axum::extract::{Query, State};
use axum::http::HeaderMap;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel};
use sea_orm::ActiveValue::Set;
use serde::Deserialize;
use crate::model::prelude::UserImage;
use crate::ServerState;
use crate::service::error::ErrorMessage;

pub async fn rename_picture(State(state): State<Arc<ServerState>>, header_map: HeaderMap, Query(query): Query<RenamePictureRequest>) -> Result<String, ErrorMessage> {
    let user = crate::service::user::login::login_by_token(&state.db, header_map).await
        .ok_or(ErrorMessage::InvalidToken)?;
    
    let picture = UserImage::find_by_id(query.id).one(&state.db).await.unwrap().ok_or(ErrorMessage::NotFound)?;
    if picture.user_id != user.id { 
        return Err(ErrorMessage::PermissionDenied);
    }
    
    let mut picture = picture.into_active_model();
    picture.file_name = Set(query.name);
    picture.save(&state.db).await.unwrap();
    
    Ok("".to_string())
}

#[derive(Deserialize)]
pub struct RenamePictureRequest {
    pub id: i64,
    pub name: String,
}