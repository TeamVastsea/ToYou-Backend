use axum::Json;
use lazy_regex::regex;
use sea_orm::{ActiveModelTrait, IntoActiveModel, NotSet};
use sea_orm::ActiveValue::Set;
use serde::Deserialize;

use crate::DATABASE;
use crate::service::error::ErrorMessage;
use crate::service::user::password::generate_password_hash;
use crate::service::user::phone::verify_sms;

pub async fn register_user(Json(request): Json<RegisterRequest>) -> Result<String, ErrorMessage> {
    let phone = request.phone;

    if !is_valid_password(&request.password) {
        return Err(ErrorMessage::InvalidParams("password".to_string()));
    }
    if !verify_sms(request.code.parse().unwrap(), &phone).await {
        return Err(ErrorMessage::InvalidParams("code".to_string()));
    }

    let user_root = crate::model::folder::ActiveModel {
        id: NotSet,
        name: Set("".to_string()),
        parent: Set(None),
        child: Set(None),
        user_id: Set(0),
        size: Set(0.0),
        depth: NotSet,
        create_time: NotSet,
        update_time: NotSet,
    };
    let user_root = user_root.insert(&*DATABASE).await.unwrap();

    let user = crate::model::user::ActiveModel {
        id: NotSet,
        username: Set(request.username),
        password: Set(generate_password_hash(&request.password)),
        phone: Set(phone),
        email: Set(None),
        available: NotSet,
        level: Set(Vec::new()),
        root: Set(user_root.id),
        create_time: NotSet,
        update_time: NotSet,
    };

    let user = user.insert(&*DATABASE).await.unwrap();
    let mut user_root = user_root.into_active_model();
    user_root.user_id = Set(user.id);
    user_root.update(&*DATABASE).await.unwrap();

    Ok("success".to_string())
}

//Since regex crate does not support lookbehind, we have to use a workaround to check the password.
pub fn is_valid_password(password: &str) -> bool {
    let length = password.len();
    if !(5..=20).contains(&length) {
        return false;
    }

    let uppercase = regex!(r"[A-Z]");
    let lowercase = regex!(r"[a-z]");
    let number = regex!(r"[0-9]");
    let special = regex!(r"\W");

    let mut conditions_met = 0;

    if uppercase.is_match(password) {
        conditions_met += 1;
    }
    if lowercase.is_match(password) {
        conditions_met += 1;
    }
    if number.is_match(password) {
        conditions_met += 1;
    }
    if special.is_match(password) {
        conditions_met += 1;
    }

    conditions_met >= 2
}

#[derive(Deserialize)]
pub struct RegisterRequest {
    password: String,
    username: String,
    phone: String,
    code: String,
}

#[test]
fn test_verify_password() {
    assert!(!is_valid_password("123456"));
    assert!(!is_valid_password("hhhhh"));
    assert!(!is_valid_password("ZZZZZDSDDDS"));
    assert!(!is_valid_password("......"));
    assert!(!is_valid_password("Za1."));
    assert!(is_valid_password("ZHCccccc"));
    assert!(is_valid_password("zsg1234567"));
    assert!(is_valid_password("12348."));
    assert!(is_valid_password("zsg.."));
    assert!(is_valid_password("GHG.."));
    println!("Password Verify Test passed");
}