use std::collections::HashMap;

use axum::extract::Query;
use sea_orm::EntityTrait;

use crate::DATABASE;
use crate::extractor::auth::AuthUser;
use crate::model::prelude::Folder;
use crate::service::error::ErrorMessage;

pub async fn get_folder_info(AuthUser(user): AuthUser, Query(query): Query<HashMap<String, String>>) -> Result<String, ErrorMessage> {
    let query_id: i64 = query.get("id")
        .ok_or(ErrorMessage::InvalidParams("id".to_string()))?
        .parse().map_err(|_| ErrorMessage::InvalidParams("folder_id".to_string()))?;
    let folder = Folder::find_by_id(query_id).one(&*DATABASE).await.unwrap()
        .ok_or(ErrorMessage::InvalidParams("folder_id".to_string()))?;

    if !folder.user_id == user.id {
        return Err(ErrorMessage::PermissionDenied);
    }

    Ok(serde_json::to_string(&folder).unwrap())
}