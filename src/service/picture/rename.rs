use axum::extract::Query;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel};
use sea_orm::ActiveValue::Set;
use serde::Deserialize;

use crate::DATABASE;
use crate::extractor::auth::AuthUser;
use crate::model::prelude::UserImage;
use crate::service::error::ErrorMessage;

pub async fn rename_picture(AuthUser(user): AuthUser, Query(query): Query<RenamePictureRequest>) -> Result<String, ErrorMessage> {
    let picture = UserImage::find_by_id(query.id).one(&*DATABASE).await.unwrap().ok_or(ErrorMessage::NotFound)?;
    if picture.user_id != user.id {
        return Err(ErrorMessage::PermissionDenied);
    }

    let mut picture = picture.into_active_model();
    picture.file_name = Set(query.name);
    picture.save(&*DATABASE).await.unwrap();

    Ok("".to_string())
}

#[derive(Deserialize)]
pub struct RenamePictureRequest {
    pub id: i64,
    pub name: String,
}