use std::sync::Arc;
use std::time::Duration;

use axum::extract::{Query, State};
use axum::http::{HeaderMap, StatusCode};
use axum::response::IntoResponse;
use lazy_static::lazy_static;
use moka::future::Cache;
use rand::Rng;
use sea_orm::{ColumnTrait, DatabaseConnection, EntityTrait, QueryFilter};
use serde::Deserialize;
use serde_inline_default::serde_inline_default;
use tracing::log::debug;

use crate::model::prelude::{Folder, User};
use crate::model::user::UserExtended;
use crate::ServerState;
use crate::service::user::level::{LevelInfo};
use crate::service::user::password::verify_password;

lazy_static!{
    static ref TOKEN_CACHE: Cache<String, i32> = Cache::builder()
        .time_to_idle(Duration::from_secs(60 * 60 * 24 * 7)) //if the key is not accessed for 7 days, it will be removed
        .build();
}



pub async fn login_user(State(state): State<Arc<ServerState>>, headers: HeaderMap, Query(request): Query<LoginRequest>) -> impl IntoResponse {
    if headers.contains_key("token") {
        debug!("Token: {}", headers.get("token").unwrap().to_str().unwrap());
        let user = login_by_token(&state.db, headers).await;
        if user.is_none() {
            return Err((StatusCode::UNAUTHORIZED, "Invalid token.".to_string()));
        }
        let user = user.unwrap();
        let headers = HeaderMap::new();

        if request.extended { //extended information includes the user's level and used space
            let levels: Vec<Vec<i64>> = user.level.iter().map(|level| serde_json::from_str(level).unwrap()).collect();
            let levels: Vec<LevelInfo> = levels.into_iter().map(|level| LevelInfo::try_from(level).unwrap()).collect();
            let max_level = levels.into_iter().max().unwrap_or_else(|| LevelInfo::get_free_level());

            let root_size = Folder::find_by_id(user.root).one(&state.db).await.unwrap().unwrap().size;
            let user = UserExtended {
                id: user.id,
                username: user.username,
                phone: user.phone,
                email: user.email,
                available: user.available,
                level: max_level,
                root: user.root,
                used_space: root_size,
                create_time: user.create_time,
                update_time: user.update_time,
            };

            return Ok((headers, serde_json::to_string(&user).unwrap()));
        }

        return Ok((headers, serde_json::to_string(&user).unwrap()));
    }

    let user = User::find().filter(crate::model::user::Column::Phone.eq(request.account)).one(&state.db).await.unwrap();
    if user.is_none() {
        return Err((StatusCode::UNAUTHORIZED, "Invalid username or password.".to_string()));
    }
    let user = user.unwrap();

    if !verify_password(&request.password, &user.password) {
        return Err((StatusCode::UNAUTHORIZED, "Invalid username or password.".to_string()));
    }

    // Generate a random token
    let token: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(30)
        .map(char::from)
        .collect();

    TOKEN_CACHE.insert(token.clone(), user.id).await;

    debug!("Token: {token}");
    let mut headers = HeaderMap::new();
    headers.insert("token", token.parse().unwrap());


    if request.extended { //extended information includes the user's level and used space
        let levels: Vec<Vec<i64>> = user.level.iter().map(|level| serde_json::from_str(level).unwrap()).collect();
        let levels: Vec<LevelInfo> = levels.into_iter().map(|level| LevelInfo::try_from(level).unwrap()).collect();
        let max_level = levels.into_iter().max().unwrap_or_else(|| LevelInfo::get_free_level());

        let root_size = Folder::find_by_id(user.root).one(&state.db).await.unwrap().unwrap().size;
        let user = UserExtended {
            id: user.id,
            username: user.username,
            phone: user.phone,
            email: user.email,
            available: user.available,
            level: max_level,
            root: user.root,
            used_space: root_size,
            create_time: user.create_time,
            update_time: user.update_time,
        };

        return Ok((headers, serde_json::to_string(&user).unwrap()));
    }

    Ok((headers, serde_json::to_string(&user).unwrap()))
}

pub async fn login_by_token(db: &DatabaseConnection, header: HeaderMap) -> Option<crate::model::user::Model> {
    if !header.contains_key("token") { return None; }
    let token = header.get("token").unwrap().to_str().unwrap();
    let uid = TOKEN_CACHE.get(token).await;
    if uid.is_none() {
        return None;
    }
    User::find().filter(crate::model::user::Column::Id.eq(uid.unwrap())).one(db).await.unwrap()
}

#[serde_inline_default]
#[derive(Deserialize)]
pub struct LoginRequest {
    #[serde_inline_default(String::from(""))]
    account: String,
    #[serde_inline_default(String::from(""))]
    password: String,
    #[serde_inline_default(false)]
    extended: bool,
}