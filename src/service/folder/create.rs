use std::sync::Arc;

use axum::extract::State;
use axum::http::{HeaderMap, StatusCode};
use axum::Json;
use axum::response::IntoResponse;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel, NotSet};
use sea_orm::ActiveValue::Set;
use serde::Deserialize;

use crate::model::prelude::Folder;
use crate::ServerState;
use crate::service::error::ErrorMessage;
use crate::service::user::login::login_by_token;

pub async fn create_folder(State(state): State<Arc<ServerState>>, header_map: HeaderMap, Json(query): Json<CreateFolderRequest>) -> Result<(), ErrorMessage> {
    let user = login_by_token(&state.db, header_map).await.ok_or(ErrorMessage::InvalidToken)?;
    
    let parent = Folder::find_by_id(query.parent).one(&state.db).await.unwrap()
        .ok_or(ErrorMessage::InvalidParams("parent".to_string()))?;
    
    let folder = crate::model::folder::ActiveModel{
        id: NotSet,
        name: Set(query.name),
        parent: Set(Some(query.parent)),
        child: NotSet,
        user_id: Set(user.id),  
        size: Set(0.0),
        depth: Set(parent.depth + 1),
        create_time: NotSet,
        update_time: NotSet,
    };
    let child = folder.insert(&state.db).await.unwrap();
    let mut child_id = match &parent.child {
        None => { Vec::new() }
        Some(a) => { a.clone() }
    };
    child_id.push(child.id);
    let mut parent = parent.into_active_model();
    parent.child = Set(Some(child_id));
    parent.save(&state.db).await.unwrap();
    
    Ok(())
}

#[derive(Deserialize)]
pub struct CreateFolderRequest {
    parent: i64,
    name: String,
}