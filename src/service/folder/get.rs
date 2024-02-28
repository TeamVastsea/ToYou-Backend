use std::collections::HashMap;
use std::sync::Arc;
use axum::extract::{Query, State};
use axum::http::{HeaderMap, StatusCode};
use sea_orm::EntityTrait;
use crate::model::prelude::Folder;
use crate::ServerState;
use crate::service::user::login::login_by_token;

pub async fn get_folder_info(State(state): State<Arc<ServerState>>, headers: HeaderMap, Query(query): Query<HashMap<String, String>>) -> Result<String, (StatusCode, String)> {
    let user = login_by_token(&state.db, headers).await
        .ok_or((StatusCode::UNAUTHORIZED, "Invalid token.".to_string()))?;
    let query_id: i64 = query.get("id")
        .ok_or((StatusCode::BAD_REQUEST, "Invalid folder id.".to_string()))?
        .parse().map_err(|_| (StatusCode::BAD_REQUEST, "Invalid folder id.".to_string()))?;
    let folder = Folder::find_by_id(query_id).one(&state.db).await.unwrap()
        .ok_or((StatusCode::BAD_REQUEST, "Invalid folder id.".to_string()))?;
    
    if !folder.user_id == user.id { 
        return Err((StatusCode::FORBIDDEN, "Permission denied.".to_string()));
    }
    
    Ok(serde_json::to_string(&folder).unwrap())
}