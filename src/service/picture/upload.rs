use axum::body::Bytes;
use axum::extract::Multipart;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel, NotSet};
use sea_orm::ActiveValue::Set;
use tracing::{debug, error};

use crate::DATABASE;
use crate::extractor::auth::AuthUser;
use crate::model::prelude::Folder;
use crate::service::error::ErrorMessage;
use crate::service::picture::file::save_file;

pub async fn post_picture(AuthUser(user): AuthUser, mut multipart: Multipart) -> Result<String, ErrorMessage> {
    let mut file: Option<Bytes> = None;
    let mut file_name: Option<String> = None;
    let mut resource_type = None;
    let mut dir = user.root;

    while let Some(field) = multipart.next_field().await.unwrap() {
        let field_file_name = field.file_name().map(|a| a.to_string());
        let file_type = field.content_type().map(|a| a.to_string());
        let name = field.name().unwrap().to_string();
        let data = match field.bytes().await {
            Ok(a) => a,
            Err(err) => {
                return if &err.body_text() == "failed to read stream" {
                    Err(ErrorMessage::SizeTooLarge)
                } else {
                    Err(ErrorMessage::Other(err.body_text().to_string()))
                };
            }
        };

        match name.as_str() {
            "file" => {
                if !file_type.clone().unwrap().starts_with("image/") {
                    return Err(ErrorMessage::InvalidParams(format!("file type {}", file_type.unwrap())));
                }
                file = Some(data);
                file_name = Some(field_file_name.unwrap());
                resource_type = Some(file_type.clone().unwrap());
            }
            "name" => {
                let name = String::from_utf8(data.to_vec()).unwrap();
                file_name = Some(name);
            }
            "dir" => {
                let dir_id = String::from_utf8(data.to_vec()).unwrap();
                dir = dir_id.parse().unwrap();
            }
            a => {
                debug!("Unknown field: {}", a);
            }
        }
    }

    if file.is_none() || file_name.is_none() || resource_type.is_none() {
        return Err(ErrorMessage::InvalidParams("Missing field.".to_string()));
    }
    let file = file.unwrap();
    let file_name = file_name.unwrap();

    // if UserImage::find()
    //     .filter(crate::model::user_image::Column::FileName.eq(&file_name))
    //     .filter(crate::model::user_image::Column::UserId.eq(user.id))
    //     .one(&*DATABASE)
    //     .await
    //     .unwrap()
    //     .is_some()
    // {
    //     return Err((StatusCode::CONFLICT, "File already exists.".to_string()));
    // }

    let id = save_file(&file).await;

    let user_image = crate::model::user_image::ActiveModel {
        id: NotSet,
        image_id: Set(id.clone()),
        user_id: Set(user.id),
        file_name: Set(file_name),
        folder_id: Set(dir),
        create_time: NotSet,
        update_time: NotSet,
    };
    let user_image = user_image.insert(&*DATABASE).await.unwrap();

    let size = file.len() as f64 / 1024.0; // KB

    // ----------------------     ------------------------------
    // | id | depth | parent|     | id | new_depth | new_parent|
    // ----------------------  => ------------------------------
    // | a  |   3   |   b   |     | b  |     2     |     c     |
    // ----------------------     ------------------------------
    let (mut parent, mut depth) = add_size_to_folder(&*DATABASE, dir, size).await;

    while let Some(a) = parent {
        let (new_parent, new_depth) = add_size_to_folder(&*DATABASE, a, size).await;

        if new_depth != depth - 1 {
            error!("Invalid depth: {} in {} (indexed from depth {})", new_depth, a, depth);
            add_size_to_folder(&*DATABASE, user.root, size).await;
            break;
        }

        parent = new_parent;
        depth = new_depth;
    }

    Ok(user_image.id.to_string())
}

async fn add_size_to_folder(db: &sea_orm::DatabaseConnection, folder_id: i64, size: f64) -> (Option<i64>, i16) {
    let folder = Folder::find_by_id(folder_id)
        .one(db)
        .await
        .unwrap()
        .unwrap();
    let mut active_folder = folder.clone().into_active_model();
    active_folder.size = Set(folder.size + size);
    active_folder.save(db).await.unwrap();

    (folder.parent, folder.depth)
}
