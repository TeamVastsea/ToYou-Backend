use axum::extract::Query;
use axum::http::HeaderMap;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel};
use sea_orm::ActiveValue::Set;
use serde::Deserialize;
use tracing::{error, info};

use crate::DATABASE;
use crate::model::prelude::{Folder, Image, UserImage};
use crate::service::error::ErrorMessage;
use crate::service::picture::file::remove_file;

pub async fn delete_picture(Query(picture_id): Query<DeletePictureRequest>, headers: HeaderMap) -> Result<String, ErrorMessage> {
    info!("delete picture: {}", picture_id.image_id);

    let user = crate::service::user::login::login_by_token(headers).await
        .ok_or(ErrorMessage::InvalidToken)?;

    let user_picture = UserImage::find_by_id(picture_id.image_id)
        .one(&*crate::DATABASE)
        .await
        .unwrap()
        .ok_or(ErrorMessage::NotFound)?;

    if user_picture.user_id != user.id {
        return Err(ErrorMessage::PermissionDenied);
    }

    let size = Image::find_by_id(&user_picture.image_id)
        .one(&*crate::DATABASE)
        .await
        .unwrap()
        .unwrap()
        .size;
    let (mut parent, mut depth) = minus_size_to_folder(&*DATABASE, user_picture.folder_id, size).await;

    while let Some(a) = parent {
        let (new_parent, new_depth) = minus_size_to_folder(&*DATABASE, a, size).await;

        if new_depth != depth - 1 {
            error!("Invalid depth: {} in {} (indexed from depth {})", new_depth, a, depth);
            minus_size_to_folder(&*DATABASE, user.root, size).await;
            break;
        }

        parent = new_parent;
        depth = new_depth;
    }

    remove_file(&user_picture.image_id).await;
    user_picture.into_active_model().delete(&*crate::DATABASE).await.unwrap();

    Ok("".to_string())
}

async fn minus_size_to_folder(db: &sea_orm::DatabaseConnection, folder_id: i64, size: f64) -> (Option<i64>, i16) {
    let folder = Folder::find_by_id(folder_id)
        .one(db)
        .await
        .unwrap()
        .unwrap();
    let new_size = if folder.size - size < 0.0 { 0.0 } else { folder.size - size };
    let mut active_folder = folder.clone().into_active_model();
    active_folder.size = Set(new_size);
    active_folder.save(db).await.unwrap();

    (folder.parent, folder.depth)
}

#[derive(Deserialize)]
pub struct DeletePictureRequest {
    pub image_id: i64,
}