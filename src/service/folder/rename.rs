use axum::extract::Query;
use axum::http::HeaderMap;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel};
use sea_orm::ActiveValue::Set;
use serde::Deserialize;

use crate::DATABASE;
use crate::model::prelude::Folder;
use crate::service::error::ErrorMessage;

pub async fn rename_folder(header_map: HeaderMap, Query(query): Query<RenameFolderRequest>) -> Result<String, ErrorMessage> {
    let user = crate::service::user::login::login_by_token(header_map).await
        .ok_or(ErrorMessage::InvalidToken)?;

    let folder = Folder::find_by_id(query.id).one(&*DATABASE).await.unwrap().ok_or(ErrorMessage::NotFound)?;
    if folder.user_id != user.id || folder.id == user.root {
        return Err(ErrorMessage::PermissionDenied);
    }
    let mut folder = folder.into_active_model();
    folder.name = Set(query.name);
    folder.save(&*DATABASE).await.unwrap();

    Ok("".to_string())
}

#[derive(Deserialize)]
pub struct RenameFolderRequest {
    pub id: i64,
    pub name: String,
}
