use std::collections::HashMap;

use axum::extract::Query;
use axum::http::HeaderMap;
use sea_orm::EntityTrait;

use crate::DATABASE;
use crate::model::prelude::Folder;
use crate::service::error::ErrorMessage;
use crate::service::user::login::login_by_token;

pub async fn get_folder_info(headers: HeaderMap, Query(query): Query<HashMap<String, String>>) -> Result<String, ErrorMessage> {
    let user = login_by_token(headers).await
        .ok_or(ErrorMessage::InvalidToken)?;
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