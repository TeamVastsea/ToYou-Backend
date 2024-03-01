use std::path::PathBuf;

use base64::Engine;
use base64::prelude::BASE64_URL_SAFE_NO_PAD;
use image::DynamicImage;
use image::io::Reader as ImageReader;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel, NotSet};
use sea_orm::ActiveValue::Set;
use tokio::fs::try_exists;

use crate::DATABASE;
use crate::model::prelude::Image;

/// Writw a file to disk and generate the id of the file
///
/// # Arguments
///
/// * `file_content`: The content of file
///
/// returns: Option<String>: Some(id) if the file is saved successfully, None if error occurs
///
/// # Examples
///
/// ```
/// let file_content = fs::read("path/to/file").await.unwrap();
/// let file_id = save_file(file_content).await;
/// println!("{file_id:?}");
/// ```
async fn write_file(file_content: impl AsRef<[u8]>) -> Option<String> {
    let mut hasher = blake3::Hasher::new();
    hasher.update(file_content.as_ref());
    hasher.update(&file_content.as_ref().len().to_le_bytes());
    let hasher = hasher.finalize();

    let id = format!("{}", BASE64_URL_SAFE_NO_PAD.encode(hasher.as_bytes()));
    let id = (&id[1..]).to_string();

    let mut path = PathBuf::from("./files");
    path.push((&id[0..2]).to_string().to_ascii_lowercase());
    path.push((&id).to_string());

    if try_exists(&path).await.unwrap() {
        return Some(id);
    }
    tokio::fs::create_dir_all(&path.parent().unwrap())
        .await
        .unwrap();
    tokio::fs::write(&path, file_content.as_ref())
        .await
        .unwrap();

    Some(id)
}


/// Read a file from disk
/// 
/// # Arguments 
/// 
/// * `file_id`: The id of the file
/// 
/// returns: Option<DynamicImage>: Some(DynamicImage) if the file is read successfully, None if not exist or format not recognized
///   
/// # Examples 
/// 
/// ```
/// let file = read_image("test").unwrap();
/// ```
pub async fn read_image(file_id: &str) -> Option<DynamicImage> {
    let mut path = PathBuf::from("./files");
    path.push((&file_id[0..2]).to_string().to_ascii_lowercase());
    path.push((&file_id).to_string());

    if !try_exists(&path).await.unwrap() {
        return None;
    }
    
    let file = ImageReader::open(path);
    if file.is_err() {
        return None;
    }
    let file = file.unwrap().with_guessed_format();
    if file.is_err() {
        return None;
    }
    let file = file.unwrap().decode();
    if file.is_err() { 
        return None;
    }
    
    Some(file.unwrap())
}

/// Save a file to disk and update the database
/// 
/// # Arguments 
/// 
/// * `db`: The database connection
/// * `file_content`: The content of file
/// 
/// returns: String:  the id of the file
/// 
/// # Examples 
/// 
/// ```
/// let file_content = fs::read("path/to/file").await.unwrap();
/// let file_id = save_file(db, file_content).await;
/// ```
pub async fn save_file(file_content: impl AsRef<[u8]>) -> String {
    let id = write_file(&file_content).await.unwrap();

    if Image::find_by_id(&id).one(&*DATABASE).await.unwrap().is_none() {
        let image = crate::model::image::ActiveModel {
            id: Set(id.clone()),
            used: Set(1),
            size: Set(file_content.as_ref().len() as f64 / 1024f64),
            create_time: NotSet,
        };
        image.insert(&*DATABASE).await.unwrap();
    } else {
        let image = Image::find_by_id(&id)
            .one(&*DATABASE)
            .await
            .unwrap()
            .unwrap();
        let mut image = image.into_active_model();
        image.used = Set(image.used.unwrap() + 1);
        image.save(&*DATABASE).await.unwrap();
    }
    
    return id;
}