use std::sync::Arc;

use axum::extract::{Query, State};
use axum::http::{HeaderMap, StatusCode};
use axum::Json;
use axum::response::IntoResponse;
use sea_orm::{ColumnTrait, EntityTrait, Order, PaginatorTrait, QueryFilter, QueryOrder};
use serde::{Deserialize, Serialize};

use crate::model::prelude::UserImage;
use crate::ServerState;
use crate::service::user::login::login_by_token;

pub async fn list_picture(State(state): State<Arc<ServerState>>, header_map: HeaderMap, Query(query): Query<ListPictureRequest>) -> impl IntoResponse {
    if query.size > 100 {
        return Err((StatusCode::BAD_REQUEST, "Size too large.".to_string()));
    }
    let user_id = login_by_token(&state.db, header_map).await;
    if user_id.is_none() {
        return Err((StatusCode::UNAUTHORIZED, "Invalid token.".to_string()));
    }

    let pictures = UserImage::find()
        .filter(crate::model::user_image::Column::UserId.eq(user_id.unwrap().id))
        .filter(crate::model::user_image::Column::FolderId.eq(query.dir))
        .order_by(crate::model::user_image::Column::CreateTime, Order::Desc)
        .paginate(&state.db, query.size);

    let response = ListPictureResponse {
        pages: pictures.num_pages().await.unwrap(),
        total: pictures.num_items().await.unwrap(),
        records: pictures.fetch_page(query.current).await.unwrap(),
    };

    Ok(Json(response))
}

#[derive(Deserialize)]
pub struct ListPictureRequest {
    dir: i32,
    current: u64,
    size: u64,
}

#[derive(Serialize)]
pub struct ListPictureResponse {
    pages: u64,
    total: u64,
    records: Vec<crate::model::user_image::Model>,
}