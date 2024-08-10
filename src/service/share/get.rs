use std::collections::HashMap;
use std::str::FromStr;

use axum::extract::{Path, Query};
use axum::http::{HeaderMap, HeaderName, HeaderValue};
use sea_orm::{ColumnTrait, EntityTrait, PaginatorTrait, QueryFilter};
use sea_orm::prelude::Uuid;
use serde::{Deserialize, Serialize};

use crate::DATABASE;
use crate::extractor::auth::AuthUser;
use crate::model::prelude::{Folder, Share, User, UserImage};
use crate::model::share::ShareInfo;
use crate::service::error::ErrorMessage;
use crate::service::picture::compress::ImageFile;
use crate::service::share::content::ContentType;
use crate::service::user::level::ShareLevel;
use crate::service::user::password::verify_password;

pub async fn check_share_password(Query(query): Query<HashMap<String, String>>) -> Result<String, ErrorMessage> {
    let id = query.get("id").ok_or(ErrorMessage::InvalidParams("id".to_string()))?;
    let id = Uuid::from_str(id).map_err(|_| { ErrorMessage::InvalidParams("id".to_string()) })?;
    let share = Share::find_by_id(id).one(&*DATABASE).await.unwrap().ok_or(ErrorMessage::NotFound)?;

    Ok(share.password.is_some().to_string())
}

pub async fn get_share_info(Query(query): Query<HashMap<String, String>>, Path(id): Path<String>) -> Result<String, ErrorMessage> {
    let id = Uuid::from_str(&id).map_err(|_| { ErrorMessage::InvalidParams("id".to_string()) })?;
    let share = Share::find_by_id(id).one(&*DATABASE).await.unwrap().ok_or(ErrorMessage::NotFound)?;
    if share.password.is_some() {
        let password = query.get("password").ok_or(ErrorMessage::LoginFailed)?;
        if !verify_password(password, &share.password.unwrap()) {
            return Err(ErrorMessage::LoginFailed);
        }
    }
    let user = User::find_by_id(share.user_id).one(&*DATABASE).await.unwrap().ok_or(ErrorMessage::NotFound)?;

    let share_info = ShareInfo {
        id: share.id,
        content: share.content,
        user_name: user.username,
        mode: share.mode,
        create_time: share.create_time,
        valid_time: share.valid_time,
    };

    Ok(serde_json::to_string(&share_info).unwrap())
}

pub async fn get_share_image(Query(query): Query<GetShareImageRequest>) -> Result<(HeaderMap, Vec<u8>), ErrorMessage> {
    let id = Uuid::from_str(&query.id).map_err(|_| { ErrorMessage::InvalidParams("id".to_string()) })?;
    let share = Share::find_by_id(id).one(&*DATABASE).await.unwrap().ok_or(ErrorMessage::NotFound)?;
    if share.password.is_some() {
        let password = query.password.ok_or(ErrorMessage::LoginFailed)?;
        if !verify_password(&password, &share.password.unwrap()) {
            return Err(ErrorMessage::LoginFailed);
        }
    }

    let content_id = ContentType::try_from(query.content.as_str()).map_err(|_| { ErrorMessage::InvalidParams("content".to_string()) })?;
    let image = match content_id {
        ContentType::Folder(_) => { return Err(ErrorMessage::InvalidParams("folder".to_string())); }
        ContentType::Picture(id) => { UserImage::find_by_id(id).one(&*DATABASE).await.unwrap().ok_or(ErrorMessage::NotFound)? }
    };

    if !share.content.contains(&query.content) && !share.content.contains(&("f".to_string() + &image.folder_id.to_string())) {
        return Err(ErrorMessage::PermissionDenied);
    }

    let mut file = ImageFile::new(image.image_id.as_str()).await.unwrap();
    file.compress_by_level(ShareLevel::try_from(share.mode).unwrap());

    let mut headers = HeaderMap::new();
    headers.insert(
        HeaderName::from_static("content-type"),
        HeaderValue::from_str("image/png").unwrap(),
    );

    Ok((headers, file.encode().unwrap()))
}

pub async fn get_share_folder(Query(query): Query<GetShareFolderRequest>) -> Result<String, ErrorMessage> {
    let id = Uuid::from_str(&query.id).map_err(|_| { ErrorMessage::InvalidParams("id".to_string()) })?;
    let share = Share::find_by_id(id).one(&*DATABASE).await.unwrap().ok_or(ErrorMessage::NotFound)?;

    if query.size > 100 {
        return Err(ErrorMessage::SizeTooLarge);
    }

    if share.password.is_some() {
        let password = query.password.ok_or(ErrorMessage::LoginFailed)?;
        if !verify_password(&password, &share.password.unwrap()) {
            return Err(ErrorMessage::LoginFailed);
        }
    }

    let content_id = ContentType::try_from(query.content.as_str()).map_err(|_| { ErrorMessage::InvalidParams("content".to_string()) })?;
    let folder = match content_id {
        ContentType::Folder(id) => { id }
        ContentType::Picture(_) => { return Err(ErrorMessage::InvalidParams("picture".to_string())); }
    };

    if !share.content.contains(&query.content) {
        return Err(ErrorMessage::PermissionDenied);
    }

    let folder = Folder::find_by_id(folder).one(&*DATABASE).await.unwrap().ok_or(ErrorMessage::NotFound)?;
    let images = UserImage::find()
        .filter(crate::model::user_image::Column::FolderId.eq(folder.id))
        .paginate(&*DATABASE, query.size);

    let response = GetShareFolderContentResponse {
        folder,
        content: images.fetch_page(query.current).await.unwrap(),
        pages: images.num_pages().await.unwrap(),
        total: images.num_items().await.unwrap(),
    };

    Ok(serde_json::to_string(&response).unwrap())
}

pub async fn list_all_share(AuthUser(user): AuthUser, Query(query): Query<ListShareRequest>) -> Result<String, ErrorMessage> {
    if query.size > 100 {
        return Err(ErrorMessage::SizeTooLarge);
    }

    let shares = Share::find()
        .filter(crate::model::share::Column::UserId.eq(user.id)).paginate(&*DATABASE, query.size);

    let response = ListPictureResponse {
        pages: shares.num_pages().await.unwrap(),
        total: shares.num_items().await.unwrap(),
        records: shares.fetch_page(query.current).await.unwrap().iter().map(|e| {
            ShareInfo {
                id: e.id,
                content: e.content.clone(),
                user_name: user.username.clone(),
                mode: e.mode,
                create_time: e.create_time,
                valid_time: e.valid_time,
            }
        }).collect(),
    };

    Ok(serde_json::to_string(&response).unwrap())
}

#[derive(Deserialize)]
pub struct ListShareRequest {
    current: u64,
    size: u64,
}

#[derive(Serialize)]
pub struct ListPictureResponse {
    pages: u64,
    total: u64,
    records: Vec<ShareInfo>,
}

#[derive(Deserialize)]
pub struct GetShareImageRequest {
    id: String,
    password: Option<String>,
    content: String,
}

#[derive(Deserialize)]
pub struct GetShareFolderRequest {
    id: String,
    password: Option<String>,
    content: String,
    size: u64,
    current: u64,
}

#[derive(Serialize)]
pub struct GetShareFolderContentResponse {
    folder: crate::model::folder::Model,
    content: Vec<crate::model::user_image::Model>,
    pages: u64,
    total: u64,
}