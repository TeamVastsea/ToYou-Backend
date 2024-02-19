use std::sync::Arc;

use axum::extract::{Query, State};
use axum::http::{header, HeaderMap, StatusCode};
use axum::response::IntoResponse;
use rand::Rng;
use sea_orm::{ColumnTrait, DatabaseConnection, EntityTrait, QueryFilter};
use serde::Deserialize;
use serde_inline_default::serde_inline_default;
use tracing::log::debug;

use crate::model::prelude::User;
use crate::ServerState;
use crate::service::TOKEN_CACHE;
use crate::service::user::password::verify_password;

pub async fn login_user(State(state): State<Arc<ServerState>>, headers: HeaderMap, Query(request): Query<LoginRequest>) -> impl IntoResponse {
    println!("{headers:?}");
    if headers.contains_key("token") {
        println!("Token: {}", headers.get("token").unwrap().to_str().unwrap());
        let token = headers.get("token").unwrap().to_str().unwrap();
        let user = login_by_token(state.db.clone(), token).await?;
        let headers = HeaderMap::new();

        return Ok((headers, user));
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

    Ok((headers, serde_json::to_string(&user).unwrap()))
}

async fn login_by_token(db: DatabaseConnection, token: &str) -> Result<String, (StatusCode, String)> {
    let uid = TOKEN_CACHE.get(token).await.unwrap();
    let user = User::find().filter(crate::model::user::Column::Id.eq(uid)).one(&db).await.unwrap();
    if user.is_none() {
        return Err((StatusCode::UNAUTHORIZED, "Invalid token.".to_string()));
    }
    let user = user.unwrap();
    Ok(serde_json::to_string(&user).unwrap())
}

#[serde_inline_default]
#[derive(Deserialize)]
pub struct LoginRequest {
    #[serde_inline_default(String::from(""))]
    account: String,
    #[serde_inline_default(String::from(""))]
    password: String,
}