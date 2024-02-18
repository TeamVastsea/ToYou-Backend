use std::sync::Arc;
use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;
use lazy_regex::regex;
use sea_orm::ActiveValue::Set;
use sea_orm::{ActiveModelTrait, NotSet};
use serde::{Deserialize, Serialize};
use crate::ServerState;
use crate::service::user::password::generate_password_hash;

use crate::service::user::phone::verify_sms;

pub async fn register_user(State(state): State<Arc<ServerState>>, Json(request): Json<RegisterRequest>) -> Result<String, (StatusCode, String)> {
    let phone = request.phone;
    if !verify_sms(request.code, &phone).await {
        return Err((StatusCode::BAD_REQUEST, "Invalid code.".to_string()));
    }

    if !is_valid_password(&request.password) {
        return Err((StatusCode::BAD_REQUEST, "Invalid password.".to_string()));
    }

    let user = crate::model::user::ActiveModel {
        id: NotSet,
        username: Set(request.username),
        password: Set(generate_password_hash(&request.password)),
        phone: Set(phone),
        email: Set(None),
        available: NotSet,
        level: Set(Vec::new()),
        create_time: NotSet,
        update_time: NotSet,
    };

    user.insert(&state.db).await.unwrap();

    Ok("".to_string())
}

//Since regex crate does not support lookbehind, we have to use a workaround to check the password.
pub fn is_valid_password(password: &str) -> bool {
    let length = password.len();
    if length < 5 || length > 20 {
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
    code: i32,
}

#[test]
fn test_verify_password() {
    assert_eq!(is_valid_password("123456"), false);
    assert_eq!(is_valid_password("hhhhh"), false);
    assert_eq!(is_valid_password("ZZZZZDSDDDS"), false);
    assert_eq!(is_valid_password("......"), false);
    assert_eq!(is_valid_password("Za1."), false);
    assert_eq!(is_valid_password("ZHCccccc"), true);
    assert_eq!(is_valid_password("zsg1234567"), true);
    assert_eq!(is_valid_password("12348."), true);
    assert_eq!(is_valid_password("zsg.."), true);
    assert_eq!(is_valid_password("GHG.."), true);
}