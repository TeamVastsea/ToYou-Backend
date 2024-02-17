use std::sync::Arc;

use axum::body::Bytes;
use axum::extract::{Multipart, State};
use axum::http::StatusCode;
use sea_orm::ActiveValue::Set;
use sea_orm::{ActiveModelTrait, ColumnTrait, EntityTrait, IntoActiveModel, NotSet, QueryFilter};
use tracing::debug;

use migration::UnOper::Not;

use crate::model::prelude::{Image, UserImage};
use crate::service::picture::file::save_file;
use crate::ServerState;

pub async fn post_picture(
    State(state): State<Arc<ServerState>>,
    mut multipart: Multipart,
) -> Result<String, (StatusCode, String)> {
    let mut file: Option<Bytes> = None;
    let mut file_name: Option<String> = None;
    let mut user_id: Option<i32> = None;
    let mut resource_type = None;

    while let Some(field) = multipart.next_field().await.unwrap() {
        let field_file_name = field.file_name().map(|a| a.to_string());
        let file_type = field.content_type().map(|a| a.to_string());
        let name = field.name().unwrap().to_string();
        let data = match field.bytes().await {
            Ok(a) => a,
            Err(err) => {
                return if &err.body_text() == "failed to read stream" {
                    Err((StatusCode::BAD_REQUEST, "File too large.".to_string()))
                } else {
                    Err((
                        StatusCode::INTERNAL_SERVER_ERROR,
                        err.body_text().to_string(),
                    ))
                };
            }
        };

        match name.as_str() {
            "file" => {
                if !file_type.clone().unwrap().starts_with("image/") {
                    return Err((
                        StatusCode::BAD_REQUEST,
                        "Invalid file type: ".to_string() + &file_type.clone().unwrap(),
                    ));
                }
                file = Some(data);
                file_name = Some(field_file_name.unwrap());
                resource_type = Some(file_type.clone().unwrap());
            }
            "token" => {
                let code = String::from_utf8(data.to_vec()).unwrap();
                user_id = Some(0);
                // let (ok, id) = get_pid(code.clone()).await;
                // if ok {
                //     pid = Some(id);
                //     auth_code = Some(code);
                // } else {
                //     return Err((StatusCode::BAD_REQUEST, "Invalid code.".to_string()));
                // }
            }
            "name" => {
                let name = String::from_utf8(data.to_vec()).unwrap();
                file_name = Some(name);
            }
            a => {
                debug!("Unknown field: {}", a);
            }
        }
    }

    if file.is_none() || file_name.is_none() || user_id.is_none() || resource_type.is_none() {
        return Err((StatusCode::BAD_REQUEST, "Missing field.".to_string()));
    }
    let file = file.unwrap();
    let file_name = file_name.unwrap();

    if UserImage::find()
        .filter(crate::model::user_image::Column::FileName.eq(&file_name))
        .one(&state.db)
        .await
        .unwrap()
        .is_some()
    {
        return Err((StatusCode::CONFLICT, "File already exists.".to_string()));
    }

    let id = save_file(&file).await.unwrap();
    if Image::find_by_id(&id)
        .one(&state.db)
        .await
        .unwrap()
        .is_none()
    {
        let image = crate::model::image::ActiveModel {
            id: Set(id.clone()),
            used: Set(1),
            size: Set((file.len() / 1024usize) as i32),
            create_time: NotSet,
        };
        image.insert(&state.db).await.unwrap();
    } else {
        let image = Image::find_by_id(&id)
            .one(&state.db)
            .await
            .unwrap()
            .unwrap();
        let mut image = image.into_active_model();
        image.used = Set(image.used.unwrap() + 1);
        image.save(&state.db).await.unwrap();
    }

    let user_image = crate::model::user_image::ActiveModel {
        id: NotSet,
        image_id: Set(id.clone()),
        user_id: Set(user_id.unwrap()),
        file_name: Set(file_name),
        folder_id: Set(0),
        create_time: NotSet,
        update_time: NotSet,
    };
    user_image.insert(&state.db).await.unwrap();

    Ok(id)
}
