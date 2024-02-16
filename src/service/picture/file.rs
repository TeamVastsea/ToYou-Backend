use std::path::PathBuf;
use base64::Engine;
use base64::prelude::BASE64_URL_SAFE_NO_PAD;
use tokio::fs::try_exists;

/// Save a file to disk and generate the id of the file
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
pub async fn save_file(file_content: impl AsRef<[u8]>) -> Option<String> {

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
    tokio::fs::create_dir_all(&path.parent().unwrap()).await.unwrap();
    tokio::fs::write(&path, file_content.as_ref()).await.unwrap();

    Some(id)
}