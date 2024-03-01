use axum::extract::Query;
use axum::http::{HeaderMap, HeaderName, HeaderValue};
use axum::Json;
use axum::response::IntoResponse;
use sea_orm::{ColumnTrait, EntityTrait, Order, PaginatorTrait, QueryFilter, QueryOrder};
use serde::{Deserialize, Serialize};

use crate::DATABASE;
use crate::model::prelude::UserImage;
use crate::service::error::ErrorMessage;
use crate::service::picture::compress::ImageFile;
use crate::service::user::login::login_by_token;

pub async fn list_picture(header_map: HeaderMap, Query(query): Query<ListPictureRequest>) -> impl IntoResponse {
    if query.size > 100 {
        return Err(ErrorMessage::SizeTooLarge);
    }
    let user = login_by_token(header_map).await
        .ok_or(ErrorMessage::InvalidToken)?;

    let pictures = UserImage::find()
        .filter(crate::model::user_image::Column::UserId.eq(user.id))
        .filter(crate::model::user_image::Column::FolderId.eq(query.dir))
        .order_by(crate::model::user_image::Column::CreateTime, Order::Desc)
        .paginate(&*DATABASE, query.size);

    let response = ListPictureResponse {
        pages: pictures.num_pages().await.unwrap(),
        total: pictures.num_items().await.unwrap(),
        records: pictures.fetch_page(query.current).await.unwrap(),
    };

    Ok(Json(response))
}

pub async fn get_picture_preview(header_map: HeaderMap, Query(query): Query<PictureGetPreviewRequest>)
                                 -> Result<(HeaderMap, Vec<u8>), ErrorMessage> {
    let user = login_by_token(header_map).await
        .ok_or(ErrorMessage::InvalidToken)?;

    // let level: LevelInfo = user.level.into_iter()
    //     .map(|e| {
    //         let raw: Vec<i64> = serde_json::from_str(&e).unwrap();
    //         LevelInfo::try_from(raw).unwrap_or_else(|e| LevelInfo::get_free_level())
    //     }).max().unwrap_or_else(|| LevelInfo::get_free_level());
    // let max_level = level.get_max_share_level();
    // let share_level = min(max_level as u8, query.mode);
    // TODO: implement share level

    let picture = UserImage::find()
        .filter(crate::model::user_image::Column::UserId.eq(user.id))
        .filter(crate::model::user_image::Column::Id.eq(query.id))
        .one(&*DATABASE)
        .await.unwrap().ok_or(ErrorMessage::NotFound)?;

    let picture = ImageFile::new(&picture.image_id).await.unwrap();

    let mut headers = HeaderMap::new();
    headers.insert(
        HeaderName::from_static("content-type"),
        HeaderValue::from_str("image/png").unwrap(),
    );

    Ok((headers, picture.encode_preview().unwrap()))
}

#[derive(Deserialize)]
pub struct PictureGetPreviewRequest {
    id: i64,
    mode: u8,
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